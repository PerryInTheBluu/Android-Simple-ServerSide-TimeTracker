# Caddy Reverse Proxy & Static Hosting Setup

Diese Anleitung beschreibt die einmalige Einrichtung von Caddy auf dem Proxmox LXC Container (CT), um:
1. Den FastAPI-Docker-Container (`127.0.0.1:8080`) sicher über das Tailnet per HTTPS bereitzustellen.
2. Das statische Web-Dashboard (`website/`) performant direkt über Caddy mit Gzip/Zstandard auszuliefern.

---

## 1. Architektur-Übersicht

```
[ Tailscale Clients (S20, Tab S7+, Laptop) ]
                    │
                    ▼ (HTTPS: time.ts.piusdischinger.com)
     ┌──────────────────────────────┐
     │ Caddy Webserver (CT / Host)  │
     └──────────────┬───────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
    `/api/*`               `/*` (Web-App)
[ Docker: FastAPI:8080 ]   [ /var/www/timetracker/index.html ]
```

---

## 2. Einmalige Caddy-Konfiguration

1. Öffne die Caddyfile auf dem CT:
   ```bash
   sudo nano /etc/caddy/Caddyfile
   ```

2. Füge den folgenden Block ein (oder ersetze den bestehenden Eintrag für `time.ts.piusdischinger.com`):
   ```caddyfile
   time.ts.piusdischinger.com {
       tls {
           get_certificate tailscale
       }

       encode gzip zstd

       handle /api/* {
           reverse_proxy 127.0.0.1:8080 {
               header_up X-Real-IP {remote_host}
               header_up X-Forwarded-For {remote_host}
               header_up X-Forwarded-Proto {scheme}
           }
       }

       handle {
           root * /var/www/timetracker
           file_server
           try_files {path} /index.html
       }

       header {
           X-Content-Type-Options nosniff
           X-Frame-Options SAMEORIGIN
           Referrer-Policy strict-origin-when-cross-origin
       }
   }
   ```

3. Konfiguration prüfen:
   ```bash
   caddy validate --config /etc/caddy/Caddyfile
   ```

4. Caddy neu laden:
   ```bash
   sudo systemctl reload caddy
   ```

---

## 3. Website-Dateien bereitstellen / aktualisieren

Führe einfach das mitgelieferte Deployment-Skript aus:
```bash
./server/caddy/deploy_website.sh
```

Alternativ manuell:
```bash
sudo mkdir -p /var/www/timetracker
sudo cp website/index.html /var/www/timetracker/
sudo chown -R caddy:caddy /var/www/timetracker
sudo chmod -R 755 /var/www/timetracker
```

---

## 4. Docker Container starten (falls noch nicht aktiv)

Im Verzeichnis `server/`:
```bash
cd server
docker compose up -d
```

---

## 5. Verifikation

- **Web-Dashboard**: Im Browser `https://time.ts.piusdischinger.com` aufrufen. Das Dashboard öffnet sich sofort ohne Ladeverzögerung.
- **API Health**: Im Browser oder per curl:
  ```bash
  curl -k https://time.ts.piusdischinger.com/api/docs
  ```
- **Android App Sync**: In den App-Einstellungen unter *Synchronisierung* die URL `https://time.ts.piusdischinger.com` verwenden.
