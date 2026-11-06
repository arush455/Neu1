"""
Nachgebaute Mini-Terminseite zum Testen des Bots (ohne den echten LBV-Server).
Start:  python test/mock_server.py   → http://127.0.0.1:8765/frontend/index.php
"""
import datetime as dt
import os
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

BOOKED = []
TODAY = dt.date.today()
FREE_DAYS = [TODAY + dt.timedelta(days=n) for n in (3, 9, 16)]
if os.environ.get("MOCK_HEUTE"):  # zum Testen von --heute
    FREE_DAYS.insert(0, TODAY)


def page(body):
    return f"""<!doctype html><html lang=de><head><meta charset=utf-8><title>TERMINVEREINBARUNG</title></head>
<body><div id=cookie><button onclick="this.parentNode.remove()">Akzeptieren</button></div>{body}</body></html>"""


class H(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def send(self, html):
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(html.encode())

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query).items()}
        step = q.get("step", "start")
        if step == "start":
            self.send(page("""<h1>Terminvereinbarung</h1>
              <a href="?step=service&cat=fs">Führerschein</a> <a href="?step=service&cat=kfz">Zulassung</a>"""))
        elif step == "service":
            self.send(page("""<form action="" method=get><input type=hidden name=step value=loc>
              <ul><li><span>Umschreibung ausländischer Führerschein</span>
                  <button type=button onclick="n.value=+n.value+1">+</button><input id=n name=anzahl value=0 readonly></li>
                <li><span>Abholung eines bestellten EU-Kartenführerscheins</span>
                  <button type=button onclick="k.value=+k.value+1">+</button><input id=k name=anzahl3 value=0 readonly></li>
                <li><span>Tausch eines deutschen Führerscheins in einen befristeten EU-Kartenführerschein</span>
                  <button type=button onclick="m.value=+m.value+1">+</button><input id=m name=anzahl2 value=0 readonly></li></ul>
              <input type=submit value="Weiter"></form>"""))
        elif step == "loc":
            self.send(page("""<h2>Standort wählen</h2>
              <a href="?step=cal&loc=west">LBV West (Schnackenburgallee)</a><br>
              <a href="?step=cal&loc=mitte">LBV Mitte (Süderstraße)</a>"""))
        elif step == "cal":
            if q.get("loc") == "west":
                self.send(page("<p>Derzeit keine freien Termine verfügbar.</p>"))
                return
            sel = q.get("day")
            cells = "".join(
                f'<a href="?step=cal&loc=mitte&day={d.isoformat()}" title="{d:%d.%m.%Y}">{d.day}</a> ' if d in FREE_DAYS
                else f'<span class="disabled">{d.day}</span> '
                for d in (TODAY + dt.timedelta(days=i) for i in range(0, 21))
            )
            times = ""
            if sel:
                times = f"<h3>Termine am {dt.date.fromisoformat(sel):%d.%m.%Y}</h3>" + "".join(
                    f'<a href="?step=form&day={sel}&t={t}">{t}</a> ' for t in ("07:30", "10:15", "14:45"))
            self.send(page(f"<table><caption>Kalender</caption><tr><td>{cells}</td></tr></table>{times}"))
        elif step == "form":
            self.send(page(f"""<h2>Ihre Daten – {q['day']} {q['t']}</h2>
              <form action="" method=get><input type=hidden name=step value=done>
              <input type=hidden name=slot value="{q['day']} {q['t']}">
              <label>Vorname <input name=vorname required></label>
              <label>Nachname <input name=nachname required></label>
              <label>E-Mail <input type=email name=email required></label>
              <label>E-Mail wiederholen <input type=email name=email2 required></label>
              <label>Telefon <input name=telefon></label>
              <label><input type=checkbox name=ds required> Ich habe die Datenschutzhinweise gelesen</label>
              <input type=submit value="Termin reservieren"></form>"""))
        elif step == "done":
            BOOKED.append(q)
            ok = q.get("email") == q.get("email2") and q.get("ds") and q.get("vorname")
            self.send(page("<h2>Vielen Dank! Ihr Termin wurde reserviert.</h2>" if ok else "<p>Fehler im Formular</p>"))
            print("BUCHUNG:", q, flush=True)


if __name__ == "__main__":
    HTTPServer(("127.0.0.1", 8765), H).serve_forever()
