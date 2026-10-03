"""Smoke tests for the time tracker server API.

Uses an in-memory SQLite database so the tests run anywhere
(the production deployment uses PostgreSQL via docker-compose).
"""
import os
import tempfile

os.environ["DATABASE_URL"] = "sqlite://"
os.environ["SECRET_KEY_FILE"] = os.path.join(tempfile.mkdtemp(), "secret.key")

from fastapi.testclient import TestClient  # noqa: E402

from app.db import Base, engine, SessionLocal, User, new_id  # noqa: E402
from app.main import app  # noqa: E402
from app.security import hash_password  # noqa: E402


def seed_user() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    db.add(User(id=new_id(), username="test", password_hash=hash_password("password123")))
    db.commit()
    db.close()


def login(client: TestClient) -> str:
    response = client.post("/api/auth/login", json={"username": "test", "password": "password123"})
    assert response.status_code == 200, response.text
    return response.json()["access_token"]


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def test_full_flow():
    seed_user()
    client = TestClient(app)

    token = login(client)

    # Activity lifecycle.
    response = client.post(
        "/api/activities",
        json={"name": "Thermodynamik", "color": "3", "icon": "ic_school_24px"},
        headers=auth_headers(token),
    )
    assert response.status_code == 200, response.text
    activity = response.json()

    response = client.get("/api/activities", headers=auth_headers(token))
    assert response.status_code == 200
    assert len(response.json()) == 1

    # Archive hides activity from the default list.
    response = client.post(f"/api/activities/{activity['id']}/archive", headers=auth_headers(token))
    assert response.status_code == 200
    response = client.get("/api/activities", headers=auth_headers(token))
    assert len(response.json()) == 0
    response = client.get("/api/activities?include_archived=true", headers=auth_headers(token))
    assert len(response.json()) == 1

    # Restore.
    response = client.post(f"/api/activities/{activity['id']}/restore", headers=auth_headers(token))
    assert response.status_code == 200

    # Time entry.
    entry_payload = {
        "activity_id": activity["id"],
        "started_at": "2026-01-01T08:00:00+00:00",
        "ended_at": "2026-01-01T09:00:00+00:00",
        "duration_seconds": 3600,
    }
    response = client.post("/api/time-entries", json=entry_payload, headers=auth_headers(token))
    assert response.status_code == 200, response.text
    entry = response.json()

    response = client.get("/api/time-entries", headers=auth_headers(token))
    assert len(response.json()) == 1

    # Dashboard day summary.
    response = client.get("/api/dashboard/day?date=2026-01-01", headers=auth_headers(token))
    assert response.status_code == 200
    assert response.json()["total_seconds"] == 3600

    # Sync push with a newer version updates, older version conflicts.
    newer = dict(entry_payload)
    newer["id"] = entry["id"]
    newer["comment"] = "updated"
    newer["updated_at"] = "2099-01-02T00:00:00+00:00"
    response = client.post(
        "/api/sync/push",
        json={"items": [{"entity_type": "time_entry", "data": newer}]},
        headers=auth_headers(token),
    )
    assert response.status_code == 200
    assert response.json()["applied"] == 1

    older = dict(newer)
    older["comment"] = "stale"
    older["updated_at"] = "2000-01-01T00:00:00+00:00"
    response = client.post(
        "/api/sync/push",
        json={"items": [{"entity_type": "time_entry", "data": older}]},
        headers=auth_headers(token),
    )
    assert response.status_code == 200
    conflicts = response.json()["conflicts"]
    assert len(conflicts) == 1
    assert conflicts[0]["resolution"] == "server_kept_newer"

    # Conflict log is visible.
    response = client.get("/api/sync/conflicts", headers=auth_headers(token))
    assert response.status_code == 200
    assert len(response.json()) == 1

    # Sync pull returns everything.
    response = client.get("/api/sync/pull", headers=auth_headers(token))
    assert response.status_code == 200
    assert len(response.json()["time_entries"]) == 1

    # Soft delete keeps the row retrievable via pull.
    response = client.delete(f"/api/time-entries/{entry['id']}", headers=auth_headers(token))
    assert response.status_code == 200
    response = client.get("/api/time-entries", headers=auth_headers(token))
    assert len(response.json()) == 0
    response = client.get("/api/sync/pull", headers=auth_headers(token))
    pulled = response.json()["time_entries"]
    assert len(pulled) == 1
    assert pulled[0]["deleted_at"] is not None

    # Export json.
    response = client.get("/api/export?format=json", headers=auth_headers(token))
    assert response.status_code == 200
    assert "activities" in response.json()

    # Export csv.
    response = client.get("/api/export?format=csv", headers=auth_headers(token))
    assert response.status_code == 200
    assert "activity_id" in response.text

    # Import Simple Time Tracker CSV rows.
    response = client.post(
        "/api/import/simple-time-tracker",
        json={
            "rows": [
                {
                    "activity": "Lernen",
                    "started_at": "2026-01-01T10:00:00+00:00",
                    "ended_at": "2026-01-01T11:00:00+00:00",
                },
            ],
        },
        headers=auth_headers(token),
    )
    assert response.status_code == 200, response.text
    assert response.json()["created"] == 1


def test_auth_requires_token():
    client = TestClient(app)
    response = client.get("/api/activities")
    assert response.status_code == 401


def test_login_rate_limit():
    seed = os.environ.get("RATE_LIMIT_TEST", "0")
    if seed != "1":
        return
    client = TestClient(app)
    for _ in range(12):
        client.post("/api/auth/login", json={"username": "x", "password": "y"})
    response = client.post("/api/auth/login", json={"username": "x", "password": "y"})
    assert response.status_code == 429


def ensure_seed_user() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    if db.query(User).filter(User.username == "test").first() is None:
        db.add(User(id=new_id(), username="test", password_hash=hash_password("password123")))
        db.commit()
    db.close()


def push_item(client: TestClient, token: str, entity_type: str, data: dict) -> dict:
    response = client.post(
        "/api/sync/push",
        json={"items": [{"entity_type": entity_type, "data": data}]},
        headers=auth_headers(token),
    )
    assert response.status_code == 200, response.text
    return response.json()


def test_sync_push_tombstone_deletes_known_entry():
    ensure_seed_user()
    client = TestClient(app)
    token = login(client)

    push_item(
        client,
        token,
        "activity",
        {"id": "a1", "name": "Lernen", "updated_at": "2026-01-01T00:00:00+00:00"},
    )
    push_item(
        client,
        token,
        "time_entry",
        {
            "id": "e1",
            "activity_id": "a1",
            "started_at": "2026-01-01T08:00:00+00:00",
            "ended_at": "2026-01-01T09:00:00+00:00",
            "duration_seconds": 3600,
            "updated_at": "2026-01-01T00:00:00+00:00",
        },
    )

    result = push_item(
        client,
        token,
        "time_entry",
        {
            "id": "e1",
            "updated_at": "2026-01-02T00:00:00+00:00",
            "deleted_at": "2026-01-02T00:00:00+00:00",
        },
    )
    assert result["applied"] == 1
    assert result["conflicts"] == []

    pulled = client.get("/api/sync/pull", headers=auth_headers(token)).json()
    entry = next(e for e in pulled["time_entries"] if e["id"] == "e1")
    assert entry["deleted_at"] is not None


def test_sync_push_tombstone_unknown_entities_is_noop():
    ensure_seed_user()
    client = TestClient(app)
    token = login(client)

    result = client.post(
        "/api/sync/push",
        json={
            "items": [
                {
                    "entity_type": "time_entry",
                    "data": {
                        "id": "e-unknown",
                        "updated_at": "2026-01-02T00:00:00+00:00",
                        "deleted_at": "2026-01-02T00:00:00+00:00",
                    },
                },
                {
                    "entity_type": "activity",
                    "data": {
                        "id": "a-unknown",
                        "updated_at": "2026-01-02T00:00:00+00:00",
                        "deleted_at": "2026-01-02T00:00:00+00:00",
                    },
                },
            ]
        },
        headers=auth_headers(token),
    )
    assert result.status_code == 200, result.text
    assert result.json()["applied"] == 2
    assert result.json()["conflicts"] == []


def test_sync_push_tombstone_deletes_known_activity():
    ensure_seed_user()
    client = TestClient(app)
    token = login(client)

    push_item(
        client,
        token,
        "activity",
        {"id": "a2", "name": "Vorlesung", "updated_at": "2026-01-01T00:00:00+00:00"},
    )

    result = push_item(
        client,
        token,
        "activity",
        {
            "id": "a2",
            "updated_at": "2026-01-02T00:00:00+00:00",
            "deleted_at": "2026-01-02T00:00:00+00:00",
        },
    )
    assert result["applied"] == 1

    pulled = client.get("/api/sync/pull", headers=auth_headers(token)).json()
    activity = next(a for a in pulled["activities"] if a["id"] == "a2")
    assert activity["deleted_at"] is not None


def test_sync_push_tombstone_stale_entry_keeps_server_state():
    ensure_seed_user()
    client = TestClient(app)
    token = login(client)

    push_item(
        client,
        token,
        "activity",
        {"id": "a3", "name": "Tutorium", "updated_at": "2026-01-01T00:00:00+00:00"},
    )
    push_item(
        client,
        token,
        "time_entry",
        {
            "id": "e3",
            "activity_id": "a3",
            "started_at": "2026-01-01T08:00:00+00:00",
            "ended_at": "2026-01-01T09:00:00+00:00",
            "updated_at": "2026-01-05T00:00:00+00:00",
        },
    )

    # A stale tombstone loses against the newer server version.
    result = push_item(
        client,
        token,
        "time_entry",
        {
            "id": "e3",
            "updated_at": "2026-01-02T00:00:00+00:00",
            "deleted_at": "2026-01-02T00:00:00+00:00",
        },
    )
    assert result["applied"] == 0
    assert result["conflicts"][0]["resolution"] == "server_kept_newer"

    pulled = client.get("/api/sync/pull", headers=auth_headers(token)).json()
    entry = next(e for e in pulled["time_entries"] if e["id"] == "e3")
    assert entry["deleted_at"] is None


def test_sync_push_entry_without_activity_id_is_invalid():
    ensure_seed_user()
    client = TestClient(app)
    token = login(client)

    result = push_item(
        client,
        token,
        "time_entry",
        {"id": "e4", "started_at": "2026-01-01T08:00:00+00:00"},
    )
    assert result["applied"] == 0
    assert result["conflicts"][0]["resolution"] == "invalid"
