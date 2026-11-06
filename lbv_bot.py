#!/usr/bin/env python3
"""
LBV-Termin-Bot: sucht auf lbv-termine.de (LBV Hamburg) nach freien Terminen
und bucht den ersten, der zu deinen Wünschen passt.

Benutzung:
    python lbv_bot.py                 # suchen und buchen (Endlosschleife)
    python lbv_bot.py --dry-run       # suchen, aber vor dem Absenden stoppen
    python lbv_bot.py --once          # nur ein Durchlauf
    python lbv_bot.py --config andere.yaml

Alle Einstellungen stehen in config.yaml (Vorlage: config.example.yaml).
"""
from __future__ import annotations

import argparse
import datetime as dt
import os
import random
import re
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

import yaml
from playwright.sync_api import Page, TimeoutError as PWTimeout, sync_playwright

HERE = Path(__file__).resolve().parent
DEBUG_DIR = HERE / "debug"
MIN_INTERVAL = 60  # Sekunden: nicht häufiger fragen, um den Server zu schonen

NEXT_WORDS = r"^\s*(weiter|fortfahren|weiter zur terminauswahl|weiter zur standortauswahl|next|ok|auswählen)\s*[>»]?\s*$"
SUBMIT_WORDS = r"(termin\s*)?(verbindlich\s*)?(buchen|reservieren|absenden|bestätigen|termin vereinbaren)"
NO_SLOT_WORDS = r"(keine (freien )?termine|kein (freier )?termin|derzeit keine|nicht verfügbar|ausgebucht)"
SUCCESS_WORDS = r"(erfolgreich|vielen dank|bestätigungs|vorgangsnummer|wurde (für sie )?reserviert|e-mail (wurde )?(an|versendet|verschickt))"
CAPTCHA_HINT = r"(captcha|sicherheitsabfrage|ich bin kein roboter)"


# --------------------------------------------------------------------------- #
# Konfiguration
# --------------------------------------------------------------------------- #
@dataclass
class Config:
    url: str
    category: str
    services: list[str]  # Alternativen, werden der Reihe nach probiert
    count: int
    locations: list[str]
    earliest: dt.date | None
    latest: dt.date | None
    weekdays: set[int]
    time_from: dt.time
    time_to: dt.time
    person: dict
    interval: int
    headless: bool
    max_calendar_pages: int
    only_today: bool
    ntfy_topic: str | None
    raw: dict = field(default_factory=dict)


def _date(v):
    if not v:
        return None
    if isinstance(v, dt.date):
        return v
    return dt.date.fromisoformat(str(v))


def _time(v, default):
    if not v:
        return default
    h, m = str(v).split(":")
    return dt.time(int(h), int(m))


WEEKDAY_NAMES = {"mo": 0, "di": 1, "mi": 2, "do": 3, "fr": 4, "sa": 5, "so": 6}


def load_config(path: Path) -> Config:
    if not path.exists():
        sys.exit(f"Konfiguration {path} fehlt. Kopiere config.example.yaml nach config.yaml und trage deine Daten ein.")
    c = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    days = c.get("wochentage") or ["mo", "di", "mi", "do", "fr", "sa"]
    weekdays = {WEEKDAY_NAMES[d.strip().lower()[:2]] for d in days}
    locs = c.get("standorte") or []
    if isinstance(locs, str):
        locs = [locs]
    return Config(
        url=c.get("url", "https://www.lbv-termine.de/frontend/index.php"),
        category=c.get("kategorie") or "",
        services=[c["dienstleistung"]] if isinstance(c["dienstleistung"], str) else list(c["dienstleistung"]),
        count=int(c.get("anzahl", 1)),
        locations=locs,
        earliest=_date(c.get("frühestens") or c.get("fruehestens")),
        latest=_date(c.get("spätestens") or c.get("spaetestens")),
        weekdays=weekdays,
        time_from=_time(c.get("uhrzeit_von"), dt.time(0, 0)),
        time_to=_time(c.get("uhrzeit_bis"), dt.time(23, 59)),
        person=c.get("person") or {},
        interval=max(MIN_INTERVAL, int(c.get("intervall_sekunden", 180))),
        headless=bool(c.get("unsichtbar", False)),
        max_calendar_pages=int(c.get("max_kalenderseiten", 4)),
        ntfy_topic=c.get("ntfy_thema"),
        only_today=bool(c.get("nur_heute", False)),
        raw=c,
    )


# --------------------------------------------------------------------------- #
# Hilfsfunktionen
# --------------------------------------------------------------------------- #
def log(msg: str) -> None:
    print(f"[{dt.datetime.now():%H:%M:%S}] {msg}", flush=True)


def snapshot(page: Page, name: str) -> None:
    """Speichert Screenshot + HTML, damit man Probleme nachvollziehen kann."""
    DEBUG_DIR.mkdir(exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    base = DEBUG_DIR / f"{stamp}-{name}"
    try:
        page.screenshot(path=f"{base}.png", full_page=True)
        Path(f"{base}.html").write_text(page.content(), encoding="utf-8")
        log(f"  Debug gespeichert: {base}.png")
    except Exception:
        pass


def notify(cfg: Config, title: str, text: str) -> None:
    print("\a", end="", flush=True)  # Piepton im Terminal
    log(f"*** {title}: {text}")
    if cfg.ntfy_topic:
        try:
            import urllib.request

            req = urllib.request.Request(
                f"https://ntfy.sh/{cfg.ntfy_topic}",
                data=text.encode("utf-8"),
                headers={"Title": title.encode("utf-8").decode("latin-1", "ignore")},
            )
            urllib.request.urlopen(req, timeout=10)
        except Exception as e:
            log(f"  Push-Nachricht fehlgeschlagen: {e}")


def page_text(page: Page) -> str:
    try:
        return page.inner_text("body")
    except Exception:
        return ""


def settle(page: Page) -> None:
    try:
        page.wait_for_load_state("networkidle", timeout=8000)
    except PWTimeout:
        pass
    page.wait_for_timeout(400)


def click_text(page: Page, text: str | re.Pattern, *, exact: bool = False, timeout: int = 4000) -> bool:
    """Klickt das erste sichtbare Element (Button, Link, Label, Text) mit diesem Text."""
    pattern = text if isinstance(text, re.Pattern) else (
        re.compile(rf"^\s*{re.escape(text)}\s*$", re.I) if exact else re.compile(re.escape(text), re.I)
    )
    candidates = [
        page.get_by_role("button", name=pattern),
        page.get_by_role("link", name=pattern),
        page.get_by_role("tab", name=pattern),
        page.get_by_label(pattern),
        page.get_by_text(pattern),
    ]
    # input[type=submit] mit passendem value
    for inp in page.locator("input[type=submit], input[type=button]").all():
        try:
            if inp.is_visible() and pattern.search(inp.get_attribute("value") or ""):
                inp.click(timeout=timeout)
                return True
        except Exception:
            continue
    for loc in candidates:
        try:
            for i in range(min(loc.count(), 5)):
                el = loc.nth(i)
                if el.is_visible() and el.is_enabled():
                    el.click(timeout=timeout)
                    return True
        except Exception:
            continue
    return False


def click_next(page: Page) -> bool:
    return click_text(page, re.compile(NEXT_WORDS, re.I))


def dismiss_banners(page: Page) -> None:
    for word in ("Alle akzeptieren", "Akzeptieren", "Einverstanden", "Verstanden", "Schließen", "OK"):
        try:
            btn = page.get_by_role("button", name=re.compile(rf"^\s*{word}\s*$", re.I))
            if btn.count() and btn.first.is_visible():
                btn.first.click(timeout=1500)
                page.wait_for_timeout(300)
        except Exception:
            pass


def wait_for_captcha(page: Page, cfg: Config) -> None:
    """Falls ein Captcha auftaucht: Bescheid geben und warten, bis du es löst."""
    has_frame = page.locator("iframe[src*='captcha'], iframe[title*='captcha' i], .g-recaptcha, .h-captcha, .frc-captcha").count() > 0
    if not has_frame and not re.search(CAPTCHA_HINT, page_text(page), re.I):
        return
    if cfg.headless:
        log("  Captcha gefunden, aber Browser ist unsichtbar. Setze 'unsichtbar: false' in config.yaml.")
        return
    notify(cfg, "LBV-Bot: Captcha", "Bitte im Browserfenster das Captcha lösen (max. 5 Minuten).")
    deadline = time.time() + 300
    while time.time() < deadline:
        page.wait_for_timeout(2000)
        if not re.search(CAPTCHA_HINT, page_text(page), re.I):
            return


# --------------------------------------------------------------------------- #
# Schritte der Buchung
# --------------------------------------------------------------------------- #
MARK_SERVICE_JS = r"""
([needle, count]) => {
  const norm = s => (s || '').replace(/\s+/g, ' ').trim().toLowerCase();
  const want = norm(needle);
  // kleinstes sichtbares Element, das den Dienstleistungstext enthält
  let best = null;
  for (const el of document.querySelectorAll('body *')) {
    if (!el.offsetParent && el.tagName !== 'BODY') continue;
    const t = norm(el.innerText);
    if (!t.includes(want)) continue;
    if (!best || t.length < norm(best.innerText).length) best = el;
  }
  if (!best) return 'not-found';
  // im Umfeld nach einem Bedienelement suchen
  let node = best;
  for (let depth = 0; depth < 6 && node; depth++, node = node.parentElement) {
    const sel = node.querySelector('select');
    if (sel) { sel.setAttribute('data-lbvbot', 'x'); return 'select'; }
    const num = node.querySelector('input[type=number], input[type=text][name*=anzahl i], input[type=text][id*=anzahl i]');
    if (num) { num.setAttribute('data-lbvbot', 'x'); return 'number'; }
    const plus = [...node.querySelectorAll('button, a, input[type=button], span[role=button]')]
      .find(b => /^\s*(\+|plus|erhöhen)\s*$/i.test(b.innerText || b.value || b.getAttribute('aria-label') || '')
             || /plus|increase|erhoehen/i.test(b.className || ''));
    if (plus) { plus.setAttribute('data-lbvbot', 'x'); return 'plus'; }
    const box = node.querySelector('input[type=checkbox], input[type=radio]');
    if (box) { box.setAttribute('data-lbvbot', 'x'); return 'check'; }
    // Karten-Layout: eigener Button in der Karte, z. B. "zum Vor-Ort-Termin"
    const btns = [...node.querySelectorAll('button, a, input[type=submit], input[type=button]')]
      .filter(b => b.offsetParent && !b.contains(best));
    const btn = btns.find(b => /termin|auswählen|buchen|weiter/i.test(b.innerText || b.value || '')) || (btns.length === 1 ? btns[0] : null);
    if (btn) { btn.setAttribute('data-lbvbot', 'x'); return 'button'; }
  }
  best.setAttribute('data-lbvbot', 'x');
  return 'text';
}
"""


def choose_category(page: Page, cfg: Config) -> None:
    if cfg.category and re.search(re.escape(cfg.category), page_text(page), re.I):
        if click_text(page, cfg.category):
            settle(page)


def choose_service(page: Page, cfg: Config) -> None:
    page.evaluate("() => document.querySelectorAll('[data-lbvbot]').forEach(e => e.removeAttribute('data-lbvbot'))")
    kind = "not-found"
    for name in cfg.services:
        kind = page.evaluate(MARK_SERVICE_JS, [name, cfg.count])
        if kind != "not-found":
            log(f"  Dienstleistung '{name}'")
            break
    target = page.locator("[data-lbvbot]").first
    log(f"  Dienstleistung gefunden ({kind})")
    if kind == "not-found":
        snapshot(page, "dienstleistung-nicht-gefunden")
        raise RuntimeError(f"Dienstleistung {cfg.services} nicht gefunden. Schreibweise in config.yaml prüfen.")
    if kind == "select":
        try:
            target.select_option(str(cfg.count))
        except Exception:
            target.select_option(index=cfg.count)
    elif kind == "number":
        target.fill(str(cfg.count))
        target.dispatch_event("change")
    elif kind == "plus":
        for _ in range(cfg.count):
            target.click()
            page.wait_for_timeout(200)
    elif kind == "check":
        target.check()
    elif kind == "button":
        target.click()
    else:
        target.click()
    settle(page)
    click_next(page)
    settle(page)


def choose_location(page: Page, cfg: Config, location: str | None) -> None:
    dismiss_banners(page)
    if location:
        if not click_text(page, location):
            log(f"  Standort '{location}' hier nicht als Auswahl gefunden – evtl. kommt er später.")
        settle(page)
    # Manche Seiten brauchen nach der Standortwahl noch "Weiter"
    if not find_times(page) and not find_days(page):
        if click_next(page):
            settle(page)


DATE_RE = re.compile(r"(\d{1,2})\.(\d{1,2})\.(\d{2,4})")
DATE_ISO_RE = re.compile(r"(\d{4})-(\d{2})-(\d{2})")


def parse_date(s: str) -> dt.date | None:
    m = DATE_ISO_RE.search(s or "")
    if m:
        return dt.date(int(m[1]), int(m[2]), int(m[3]))
    m = DATE_RE.search(s or "")
    if m:
        y = int(m[3])
        y = y + 2000 if y < 100 else y
        try:
            return dt.date(y, int(m[2]), int(m[1]))
        except ValueError:
            return None
    return None


CLICKABLE = "a, button, input[type=submit], input[type=button], input[type=radio], label, td[onclick], div[onclick], span[onclick], [role=button], [role=gridcell], [role=option]"
DISABLED_CLASS = re.compile(r"(disabled|inactive|inaktiv|unavailable|belegt|nicht|past|closed|full|voll)", re.I)


def _is_free(el) -> bool:
    try:
        if not el.is_visible() or not el.is_enabled():
            return False
        cls = (el.get_attribute("class") or "") + " " + (el.get_attribute("aria-disabled") or "")
        return not DISABLED_CLASS.search(cls) and "true" not in (el.get_attribute("aria-disabled") or "")
    except Exception:
        return False


def _label(el) -> str:
    parts = []
    for attr in ("value", "title", "aria-label", "data-date", "data-datum", "data-day", "datetime"):
        try:
            v = el.get_attribute(attr)
            if v:
                parts.append(v)
        except Exception:
            pass
    try:
        parts.append(el.inner_text(timeout=500))
    except Exception:
        pass
    return " ".join(parts)


def find_times(page: Page) -> list:
    """Alle anklickbaren Uhrzeit-Elemente (z. B. '08:15')."""
    out = []
    for el in page.locator(CLICKABLE).all():
        lbl = _label(el)
        first = lbl.strip().splitlines()[0] if lbl.strip() else ""
        m = re.search(r"(?<![\d.:])(\d{1,2}):(\d{2})(?![\d.:])", first)
        if m and len(first) <= 40 and int(m[1]) < 24 and _is_free(el):
            out.append((dt.time(int(m[1]), int(m[2])), lbl, el))
    return out


def find_days(page: Page) -> list:
    """Anklickbare Tage im Kalender (Zahl 1–31 oder ein Datum)."""
    out = []
    for el in page.locator(CLICKABLE).all():
        lbl = _label(el)
        txt = lbl.strip()
        if not txt:
            continue
        date = parse_date(txt)
        try:
            is_daynum = re.fullmatch(r"\d{1,2}", el.inner_text(timeout=500).strip()) is not None
        except Exception:
            is_daynum = False
        if (date or is_daynum) and not re.search(r"(?<![\d.:])\d{1,2}:\d{2}(?![\d.:])", txt) and _is_free(el):
            out.append((date, lbl, el))
    return out


def current_date_on_page(page: Page) -> dt.date | None:
    for sel in ("h1, h2, h3, h4, legend, caption, .datum, .date, [class*=selected], [aria-selected=true]",):
        try:
            for el in page.locator(sel).all()[:30]:
                d = parse_date(el.inner_text(timeout=500))
                if d:
                    return d
        except Exception:
            pass
    return None


def date_ok(cfg: Config, d: dt.date | None) -> bool:
    if cfg.only_today:
        # Datum wird bei jedem Versuch neu bestimmt, also auch nach Mitternacht richtig
        return d is None or d == dt.date.today()
    if d is None:
        return True  # unbekannt – wir prüfen später nochmal
    if cfg.earliest and d < cfg.earliest:
        return False
    if cfg.latest and d > cfg.latest:
        return False
    return d.weekday() in cfg.weekdays


def time_ok(cfg: Config, t: dt.time) -> bool:
    return cfg.time_from <= t <= cfg.time_to


def pick_slot(page: Page, cfg: Config) -> tuple[dt.date | None, dt.time] | None:
    """Sucht im Kalender einen passenden Termin und klickt ihn an."""
    for cal_page in range(cfg.max_calendar_pages):
        if re.search(NO_SLOT_WORDS, page_text(page), re.I) and not find_times(page) and not find_days(page):
            return None

        # 1) Uhrzeiten direkt sichtbar?
        day = current_date_on_page(page)
        for t, lbl, el in find_times(page):
            d = parse_date(lbl) or day
            if cfg.only_today and d is None:
                continue
            if date_ok(cfg, d) and time_ok(cfg, t):
                el.click()
                settle(page)
                return d, t

        # 2) Tage durchklicken
        days = find_days(page)
        tried = 0
        for idx in range(len(days)):
            days = find_days(page)  # nach jedem Klick neu einlesen
            if idx >= len(days):
                break
            d, lbl, el = days[idx]
            if not date_ok(cfg, d):
                continue
            # Kalenderfeld ohne Datum (nur Tageszahl): bei nur_heute muss die Zahl stimmen
            day_num = (lbl.strip().split() or [""])[-1]
            is_today_cell = d == dt.date.today() or day_num == str(dt.date.today().day)
            if cfg.only_today and not is_today_cell:
                continue
            tried += 1
            try:
                el.click()
            except Exception:
                continue
            settle(page)
            d = d or current_date_on_page(page)
            if cfg.only_today and d is None and is_today_cell:
                d = dt.date.today()
            if not date_ok(cfg, d) or (cfg.only_today and d is None):
                continue
            for t, tlbl, tel in find_times(page):
                if time_ok(cfg, t):
                    tel.click()
                    settle(page)
                    return d, t
            if tried > 40:
                break

        # 3) nächste Woche / nächsten Monat
        if cfg.only_today:
            break
        if not click_text(page, re.compile(r"(nächste|naechste|vor|später|weiter)\s*(woche|monat|seite|termine)?\s*[>»]?$|^[>»›]+$", re.I)):
            break
        settle(page)
    return None


FIELD_MAP = [
    (r"anrede|salutation", "anrede"),
    (r"vorname|first", "vorname"),
    (r"nachname|familienname|last|^name$|surname", "nachname"),
    (r"e-?mail.*(wiederhol|bestätig|repeat|confirm)", "email"),
    (r"e-?mail", "email"),
    (r"telefon|phone|mobil|handy", "telefon"),
    (r"geburts|birth", "geburtsdatum"),
    (r"straße|strasse|street", "strasse"),
    (r"plz|postleitzahl|zip", "plz"),
    (r"\bort\b|stadt|city|wohnort", "ort"),
    (r"kennzeichen", "kennzeichen"),
]


def fill_form(page: Page, cfg: Config) -> None:
    p = {k: str(v) for k, v in (cfg.person or {}).items() if v not in (None, "")}
    for el in page.locator("input:visible, select:visible, textarea:visible").all():
        typ = (el.get_attribute("type") or "").lower()
        if typ in ("hidden", "submit", "button", "checkbox", "radio"):
            continue
        hint = " ".join(filter(None, [
            el.get_attribute("name"), el.get_attribute("id"), el.get_attribute("placeholder"),
            el.get_attribute("aria-label"), _label_for(page, el),
        ])).lower()
        for pattern, key in FIELD_MAP:
            if re.search(pattern, hint) and key in p:
                try:
                    if el.evaluate("e => e.tagName") == "SELECT":
                        el.select_option(label=re.compile(re.escape(p[key]), re.I))
                    elif not el.input_value():
                        el.fill(p[key])
                except Exception:
                    pass
                break
    # Pflicht-Häkchen (Datenschutz, Einwilligung)
    for box in page.locator("input[type=checkbox]:visible").all():
        hint = (_label_for(page, box) + " " + (box.get_attribute("name") or "")).lower()
        if re.search(r"datenschutz|einwillig|akzeptier|zustimm|gelesen|privacy|agb|bestätig", hint) or box.get_attribute("required") is not None:
            try:
                box.check()
            except Exception:
                pass


def _label_for(page: Page, el) -> str:
    try:
        return el.evaluate(
            """e => {
              if (e.labels && e.labels.length) return [...e.labels].map(l => l.innerText).join(' ');
              const p = e.closest('label, .form-group, tr, li, div');
              return p ? p.innerText.slice(0, 120) : '';
            }"""
        ) or ""
    except Exception:
        return ""


def submit(page: Page) -> bool:
    if click_text(page, re.compile(SUBMIT_WORDS, re.I)):
        settle(page)
        return True
    return False


# --------------------------------------------------------------------------- #
# Ablauf
# --------------------------------------------------------------------------- #
def attempt(page: Page, cfg: Config, location: str | None, dry_run: bool) -> bool:
    log(f"Suche Termin{' in ' + location if location else ''} …")
    page.goto(cfg.url, wait_until="domcontentloaded")
    settle(page)
    dismiss_banners(page)
    choose_category(page, cfg)
    choose_service(page, cfg)
    choose_location(page, cfg, location)

    slot = pick_slot(page, cfg)
    if not slot:
        log("  Kein passender Termin frei.")
        return False

    d, t = slot
    when = f"{d:%d.%m.%Y} " if d else ""
    notify(cfg, "LBV-Bot: Termin gefunden", f"{when}{t:%H:%M}{' · ' + location if location else ''}")

    # Manche Seiten zeigen nach der Uhrzeit noch eine Zusammenfassung mit "Weiter"
    for _ in range(2):
        if page.locator("input[type=email]:visible, input[name*=mail i]:visible").count():
            break
        if not click_next(page):
            break
        settle(page)

    wait_for_captcha(page, cfg)
    fill_form(page, cfg)
    snapshot(page, "formular")

    if dry_run:
        log("  Testlauf: Formular ausgefüllt, NICHT abgeschickt. Browser bleibt 60 s offen.")
        page.wait_for_timeout(60000)
        return True

    for _ in range(3):  # Formular → evtl. Zusammenfassung → endgültig buchen
        if not submit(page):
            break
        wait_for_captcha(page, cfg)
        if re.search(SUCCESS_WORDS, page_text(page), re.I):
            snapshot(page, "gebucht")
            notify(cfg, "LBV-Bot: Termin gebucht!",
                   f"{when}{t:%H:%M}. Prüfe dein E-Mail-Postfach – oft musst du den Termin dort noch per Link bestätigen.")
            return True
        fill_form(page, cfg)

    snapshot(page, "buchung-unklar")
    notify(cfg, "LBV-Bot: Bitte prüfen", "Termin ausgewählt, aber Buchung nicht eindeutig bestätigt. Siehe debug/-Ordner.")
    return True  # lieber stoppen als doppelt buchen


def main() -> None:
    ap = argparse.ArgumentParser(description="Sucht und bucht Termine beim LBV Hamburg.")
    ap.add_argument("--config", default=str(HERE / "config.yaml"))
    ap.add_argument("--dry-run", action="store_true", help="nicht absenden, nur bis zum Formular")
    ap.add_argument("--once", action="store_true", help="nur ein Durchlauf")
    ap.add_argument("--heute", action="store_true", help="nur Termine von heute buchen")
    args = ap.parse_args()
    cfg = load_config(Path(args.config))
    if args.heute:
        cfg.only_today = True

    log(f"Suche '{cfg.services[0]}' | Standorte: {', '.join(cfg.locations) or 'egal'} | " +
        ("NUR HEUTE" if cfg.only_today else f"{cfg.earliest or 'ab sofort'} bis {cfg.latest or 'offen'}") + f" | {cfg.time_from:%H:%M}–{cfg.time_to:%H:%M}")

    with sync_playwright() as pw:
        launch = {"headless": cfg.headless}
        if os.environ.get("LBV_CHROMIUM"):
            launch["executable_path"] = os.environ["LBV_CHROMIUM"]
        browser = pw.chromium.launch(**launch)
        ctx = browser.new_context(locale="de-DE", timezone_id="Europe/Berlin")
        page = ctx.new_page()
        page.set_default_timeout(15000)
        try:
            while True:
                for loc in (cfg.locations or [None]):
                    try:
                        if attempt(page, cfg, loc, args.dry_run):
                            return
                    except Exception as e:
                        log(f"  Fehler: {e}")
                        snapshot(page, "fehler")
                if args.once:
                    return
                wait = cfg.interval + random.randint(0, 30)
                log(f"Nächster Versuch in {wait} s (Abbrechen mit Strg+C).")
                time.sleep(wait)
        except KeyboardInterrupt:
            log("Beendet.")
        finally:
            browser.close()


if __name__ == "__main__":
    main()
