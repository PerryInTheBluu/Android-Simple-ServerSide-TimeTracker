# Home Assistant Sprachsteuerung (Assist) & Sensor-Integration

Dieses Paket integriert den TimeTracker nahtlos in Home Assistant:
1. **Sprachsteuerung via Assist**: Sag einfach „Tracke Pause“, „Starte Thermodynamik“, „Was läuft gerade?“ oder „Stoppe Tracking“ zu deinem Home Assistant Sprachassistenten (Voice PE, Atom Echo, Smartphone, Dashboard).
2. **Fuzzy-Match & TTS-Antwort**: Der Server erkennt auch Tippfehler, unvollständige Namen und Synonyme („Thermo“ ➔ „Thermodynamik“) und antwortet per Text-to-Speech mit der genauen Bestätigung.
3. **Live-Sensoren**: Zeigt den aktuellen Tracking-Zustand, die aktive Aktivität und die Laufzeit in Home Assistant an (für Dashboards und Automatisierungen).

---

## 1. Voraussetzungen

- Home Assistant mit aktiviertem Assist (Sprachassistent).
- Ein API-Token deines TimeTracker-Servers (aus der App oder der Web-App).

---

## 2. Einrichtung in 3 einfachen Schritten

### Schritt 1: Secret in Home Assistant eintragen
Öffne `secrets.yaml` in deiner Home Assistant Konfiguration und ergänze:
```yaml
timetracker_token: "Bearer DEIN_LOKALER_API_TOKEN_HIER"
```

*(Ersetze `DEIN_LOKALER_API_TOKEN_HIER` durch deinen API-Token)*

### Schritt 2: Dateien in Home Assistant kopieren

Kopiere die beiden Dateien aus diesem Ordner in deine Home Assistant Konfiguration:
- `homeassistant/custom_sentences/de/timetracker.yaml` ➔ `/config/custom_sentences/de/timetracker.yaml`
- `homeassistant/packages/timetracker.yaml` ➔ `/config/packages/timetracker.yaml`

*(Falls in deiner `configuration.yaml` noch keine Packages aktiv sind, füge dort einmalig ein:)*
```yaml
homeassistant:
  packages: !include_dir_named packages
```

### Schritt 3: Home Assistant neu laden
In Home Assistant unter **Entwicklerwerkzeuge** ➔ **YAML-Konfiguration neu laden**:
- *Satz-Trigger neu laden* (oder Assist-Konfiguration)
- *YAML-Konfiguration neu laden* (für Packages & REST-Sensoren)

---

## 3. Verfügbare Sprachbefehle

| Gesprochener Satz | Server-Aktion | Antwortbeispiel |
|---|---|---|
| „Tracke Pause“ | Startet Timer für *Pause* | „Tracking für Pause gestartet.“ |
| „Starte Thermodynamik“ / „Tracke Thermo“ | Fuzzy-Match auf *Thermodynamik* | „Tracking für Thermodynamik gestartet.“ |
| „Stoppe Tracking“ / „Tracking beenden“ | Stoppt den aktuell aktiven Timer | „Tracking für Thermodynamik nach 42 Minuten beendet.“ |
| „Was läuft gerade?“ / „Status Zeiterfassung“ | Prüft laufenden Timer | „Aktuell läuft seit 15 Minuten die Aktivität Pause.“ |
| „Was läuft gerade?“ *(wenn nichts läuft)* | Prüft Status | „Aktuell läuft keine Zeiterfassung.“ |

---

## 4. Verfügbare Entitäten in Home Assistant

- `sensor.timetracker_status`: Zustand (`tracking` oder `idle`)
- `sensor.timetracker_active_activity`: Name der aktuell aktiven Aktivität (z. B. `Pause`, `Thermodynamik`, `Keine`)
- `sensor.timetracker_duration_minutes`: Bisherige Laufzeit in Minuten

---

## 5. MQTT Autodiscovery & WebSocket Event Push

Neben dem klassischen REST-Polling (alle 30s) unterstützt der TimeTracker-Server jetzt auch echten Echtzeit-Push:

### A) MQTT Autodiscovery
Wenn du einen MQTT-Broker (z. B. Mosquitto Add-on in Home Assistant) nutzt:
1. Rufe die Autodiscovery-Konfigurationen ab: `GET /api/assist/mqtt_discovery`
2. Konfiguriere in der Umgebung des TimeTracker-Servers:
   - `MQTT_HOST`: IP/Hostname deines MQTT-Brokers (z. B. `192.168.1.10` oder Tailnet-IP)
   - `MQTT_PORT`: 1883
   - `MQTT_USER`: Optionaler Benutzername
   - `MQTT_PASSWORD`: Optionales Passwort
3. Der Server publiziert Statusänderungen sofort auf Topic `timetracker/state` mit retained Status, und Home Assistant registriert die Sensoren automatisch!

### B) WebSocket Event-Push (`/api/ws`)
Web-Clients, Dashboards und Drittanwendungen können sich direkt per WebSocket verbinden:
- URL: `wss://time.ts.piusdischinger.com/api/ws?token=DEIN_TOKEN`
- Empfängt sofort beim Verbinden das Event `connected` mit dem aktuellen Timer-Status.
- Sendet Live-Events `timer_started`, `timer_stopped` und `sync_completed` ohne jede Verzögerung.
