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
  gelb umrandet läuft gerade, dunkel ausgefallen/vorlesungsfrei). Überlappende Veranstaltungen
  werden unabhängig voneinander gewertet — zwei parallel nachgetragene
  Veranstaltungen zählen beide als besucht.
- **Stunden pro Fach**: getrackte Stunden je Aktivität im Zeitraum mit
  Fortschrittsbalken gegen das Stundenziel (inkl. ECTS-Schätzung,
  1 ECTS = 30 h).
- **Alle Aktivitäten**: Stunden und Anteil je Aktivität.
- **1-Klick Kacheln (Live-Tracking ohne Dropdowns)**:
  Alle aktiven Aktivitäten werden als übersichtliche Kacheln dargestellt.
  Ein Klick auf eine Kachel startet die Aktivität sofort (oder wechselt
  direkt dorthin, falls bereits ein Timer läuft). Ein Klick auf die
  laufende Kachel stoppt die Erfassung sofort (1-Klick Stop). Jede Kachel
  zeigt die heute getrackte Dauer des jeweiligen Fachs.
- **Max. 2-Klick Start mit Offset oder Notiz**:
  Vor dem Klick auf eine Kachel kann per Schnell-Chip ein Start-Offset
  gewählt werden (`Sofort`, `vor 5m`, `vor 15m`, `vor 30m`) sowie eine
  optionale Notiz eingegeben werden. Nach dem Start wird der Offset
  automatisch wieder auf `Sofort` zurückgesetzt.
- **Nachträgliche Offset-Korrektur & Notiz während des Laufs**:
  Bei laufendem Timer kann die Startzeit über Schnell-Knöpfe (`-15m`,
  `-5m`, `+5m`) direkt im Backend korrigiert und Notizen aktualisiert werden.
- **Heute-Timeline**:
  Direkt unter den Kacheln: Anzeige aller heutigen Zeiteinträge in
  chronologischer Reihenfolge, farbiger Tagesfortschrittsbalken mit
  Live-Pulsieren für aktive Timer, Gesamtdauer für heute und Möglichkeit,
  Fehleinträge direkt zu löschen.
- **Abhakbare Todos (Vorbereitung / Nachbereitung)**:
  Stundenplan-Todos können per Klick auf die Checkbox direkt als erledigt/offen
  markiert werden. Die Änderung wird via Sync-Push sofort an den Server
  übertragen und steht bei der nächsten Synchronisation auch in der Android-App bereit.
- **Sync-Status & Konflikt-Anzeige**:
  Zeigt den Status der Serversynchronisation an. Bei etwaigen Konflikten
  (z. B. gleichzeitige Bearbeitung auf Handy und Web) können die
  Auflösungsdetails per Klick eingesehen werden.

Anwesenheit wird wie in der App gewertet: ein Eintrag der verknüpften
Aktivität, der den Termin (ggf. mit Raum-/Zeit-Ausnahme) überlappt,
zählt als besucht.

## Test-Werkzeuge (Entwicklung)

- `test_page.js` — führt die Seiten-Logik headless mit einer `pull.json`
  Datei aus: `node test_page.js /pfad/zur/pull.json`
- `reference.py` — unabhängige Referenzrechnung derselben Kennzahlen zum
  Gegenprüfen: `python3 reference.py /pfad/zur/pull.json`
