# LBV-Termin-Bot (Hamburg)

Sucht auf **lbv-termine.de** (Online-Terminvergabe des Landesbetriebs Verkehr Hamburg)
regelmäßig nach freien Terminen und bucht den ersten, der zu deinen Wünschen passt
(Dienstleistung, Standort, Zeitraum, Wochentage, Uhrzeit).

## Einrichten (einmalig)

```bash
cd lbv-termin-bot
python3 -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
playwright install chromium
cp config.example.yaml config.yaml                   # dann config.yaml ausfüllen
```

In `config.yaml` die Kategorie und Dienstleistung **genau so** eintragen, wie sie auf
lbv-termine.de stehen (ein eindeutiger Teil des Textes reicht).

## Benutzen

```bash
python lbv_bot.py --dry-run --once   # erst testen: füllt das Formular aus, schickt NICHT ab
python lbv_bot.py                    # sucht alle paar Minuten und bucht, sobald etwas frei ist
```

- Der Browser ist sichtbar, damit du zusehen und notfalls ein Captcha lösen kannst.
- Nach der Buchung kommt meist eine E-Mail mit Bestätigungslink – **den musst du selbst klicken**,
  sonst verfällt der Termin.
- Optional Push aufs Handy: App **ntfy** installieren, ein geheimes Thema abonnieren und
  bei `ntfy_thema` eintragen.
- Der Bot fragt höchstens einmal pro Minute (Standard: alle 3 Min.). Bitte nicht schneller
  stellen, und nur für dich selbst buchen.

## Wenn etwas hakt

Das Skript klickt sich über die sichtbaren Texte durch die Seite. Ändert der LBV das
Layout, kann ein Schritt scheitern. Dann liegen Screenshot + HTML im Ordner `debug/` –
damit lässt sich der Schritt schnell anpassen.

## Testen ohne echten Server

```bash
python test/mock_server.py &                         # nachgebaute Terminseite auf Port 8765
# in einer Kopie der config: url: http://127.0.0.1:8765/frontend/index.php
python lbv_bot.py --config test.yaml --once
```
