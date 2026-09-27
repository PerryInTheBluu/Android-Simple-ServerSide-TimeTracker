# Simple Time Tracker Server

Selbst gehosteter Single-User-Server für die Android-App.
Läuft im Heimnetz hinter dem vorhandenen Caddy Reverse Proxy und ist nur
über das Headscale/Tailscale-Tailnet (HTTPS, gültige Zertifikate) erreichbar.

**Kein** Internet-Exit Node, **keine** Cloud, **keine** Telemetrie.
Der Dienst öffnet selbst keine öffentlichen Ports.

## Deployment (Proxmox / Docker Compose)

```bash
cd server
cat > .env <<'EOF'
DB_PASSWORD=<starkes-passwort>
API_BIND_IP=<lan-ip-des-containers>
EOF
docker compose up -d --build
```

Der Dienst hört nur auf der gebundenen LAN-IP, Port 8080.
TLS-Terminierung übernimmt der vorhandene Caddy.

## Caddy (auf dem Host, tailnet-only)

```caddyfile
timetracker.example.ts.net {
    reverse_proxy <lan-ip>:8080
}
```

Caddy besorgt reguläre Zertifikate (z. B. über die Tailscale-DNS-Challenge
oder eine interne ACME-CA). Keine selbstsignierten Zertifikate nötig.

## Erster Start (Benutzer anlegen)

Solange kein Benutzer existiert, ist einmalig der Setup-Endpunkt aufrufbar:

```bash
curl -X POST https://timetracker.example.ts.net/api/auth/setup \
  -H 'Content-Type: application/json' \
  -d '{"username": "ich", "password": "mindestens-8-zeichen"}'
```

Danach Token für die App erzeugen:

```bash
curl -X POST https://timetracker.example.ts.net/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "ich", "password": "..."}'
```

`access_token` ist ein langlebiger, widerrufbarer API-Token (Default 10 Jahre),
in der App einzutragen. Widerrufbar über `/api/auth/logout` (revoked flag).

## API

OpenAPI: `GET /api/openapi.json`, Docs: `GET /api/docs`

- `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`
- `GET/POST /api/activities`, `PATCH /api/activities/{id}`,
  `POST /api/activities/{id}/archive`, `POST /api/activities/{id}/restore`
- `GET/POST /api/time-entries`, `PATCH /api/time-entries/{id}`, `DELETE /api/time-entries/{id}`
- `GET /api/dashboard/day?date=`, `GET /api/dashboard/summary?from=&to=`,
  `GET /api/dashboard/heatmap?from=&to=`
- `GET/POST /api/goals`, `PATCH /api/goals/{id}`
- `POST /api/sync/push`, `GET /api/sync/pull?since=`, `GET /api/sync/conflicts`
- `GET /api/export?format=json|csv`, `POST /api/import/simple-time-tracker`

Konfliktstrategie (Version 1, konservativ):
- IDs, `updated_at`, `deleted_at` überall.
- Gleiche ID: neuere `updated_at` gewinnt; Verlierer wird im
  `conflict_log` protokolliert und über `/api/sync/conflicts` sichtbar.
- Zeitblöcke mit unterschiedlicher ID werden zusammengeführt.

## Migrationen

Versionierte SQL-Dateien in `migrations/`, angewendet beim Containerstart
(`run_migrations.py`, Tabelle `schema_migrations`).

## Backups

- `db_data` (PostgreSQL) und `api_data` (Secret-Key) sind benannte Docker-Volumes
  und in jede Backup-Strategie (z. B. Proxmox Backup / vzdump) einbezogen.
- Zusätzlich: `GET /api/export?format=json` für vollständigen Datenexport.

## Tests

```bash
pip install -r requirements.txt pytest httpx
python -m pytest tests -q
```
