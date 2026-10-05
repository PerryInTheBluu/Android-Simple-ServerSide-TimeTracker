# Stundenplan-Auswertung (Website)

Lokale Weboberfläche zur Analyse der Uni-Daten (Anwesenheit, Stunden pro
Fach, Vorbereitungen) auf großen Bildschirmen. Läuft komplett lokal auf
dem Laptop/PC, der Server muss dafür nicht angepasst werden.

## Start

    python3 serve.py

Dann im Browser öffnen: http://127.0.0.1:8765

- API-Token eintragen (gleiches Token wie in der App-Konfiguration),
  es wird nur lokal im Browser gespeichert.
- Zeitraum einstellen (Standard: ältester Eintrag bis heute) und
  „Laden" drücken.

Optional:

    python3 serve.py --port 9000 --server https://<eigene-server-url>

`serve.py` serviert die Seite und leitet `/api/…`-Anfragen an den
Time-Tracker-Server weiter — dadurch ist keine CORS-Freigabe auf dem
Server nötig. Nur Python-Standardbibliothek, keine Abhängigkeiten.

## Was gezeigt wird

- **Anwesenheit pro Veranstaltung**: erwartete / besuchte / verpasste /
  bevorstehende Termine je Stundenplan-Eintrag, mit Terminstreifen pro
  Datum (grün besucht, rot verpasst, grau umrandet bevorstehend,
  dunkel ausgefallen/vorlesungsfrei). Überlappende Veranstaltungen
  werden unabhängig voneinander gewertet — zwei parallel nachgetragene
  Veranstaltungen zählen beide als besucht.
- **Stunden pro Fach**: getrackte Stunden je Aktivität im Zeitraum mit
  Fortschrittsbalken gegen das Stundenziel (inkl. ECTS-Schätzung,
  1 ECTS = 30 h).
- **Alle Aktivitäten**: Stunden und Anteil je Aktivität.
- **Vorbereitung / Nachbereitung / Todos**: Erfüllungsquote je Art und
  Liste der offenen.
- **Getrackte Stunden pro Tag**: gestapeltes Balkendiagramm des
  Zeitraums mit Stunden-Achse — ein Segment je Aktivität in der Farbe
  der Aktivität (gleiche Palette wie in der App), Tooltip pro Tag mit
  Aufteilung; Einträge über Mitternacht werden sauber aufgeteilt.
  Die Aktivitäts- und Zieltabellen zeigen passende Farbpunkte.
- **CSV-Export**: lädt die Kennzahlen (Anwesenheit pro Veranstaltung,
  Stunden pro Tag, Gesamtsumme) als Excel-kompatible Semikolon-CSV mit
  BOM herunter.
- **Link kopieren**: erzeugt eine URL mit Token und Zeitraum im Hash
  (`#token=…&from=…&to=…`), die die Seite beim Öffen direkt lädt —
  praktisch als Lesezeichen. Der Token steht dabei in der URL, also
  nur auf eigenen Rechnern verwenden.

Anwesenheit wird wie in der App gewertet: ein Eintrag der verknüpften
Aktivität, der den Termin (ggf. mit Raum-/Zeit-Ausnahme) überlappt,
zählt als besucht.

## Test-Werkzeuge (Entwicklung)

- `test_page.js` — führt die Seiten-Logik headless mit einer `pull.json`
  Datei aus: `node test_page.js /pfad/zur/pull.json`
- `reference.py` — unabhängige Referenzrechnung derselben Kennzahlen zum
  Gegenprüfen: `python3 reference.py /pfad/zur/pull.json`
