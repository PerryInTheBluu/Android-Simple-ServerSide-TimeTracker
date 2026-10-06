"""Tests for Home Assistant Assist / voice processing and timer name lookup."""
import os
import tempfile

os.environ["DATABASE_URL"] = "sqlite://"
os.environ["SECRET_KEY_FILE"] = os.path.join(tempfile.mkdtemp(), "secret_assist.key")

from fastapi.testclient import TestClient

from app.db import Base, engine, SessionLocal, User, Activity, new_id
from app.main import app
from app.security import hash_password


def setup_module():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    user = User(id="user_assist_1", username="ha_user", password_hash=hash_password("pw123"))
    db.add(user)
    db.add(Activity(id="act_thermo", user_id="user_assist_1", name="Thermodynamik", color="orange"))
    db.add(Activity(id="act_pause", user_id="user_assist_1", name="Pause", color="yellow"))
    db.commit()
    db.close()


def get_token(client: TestClient) -> str:
    res = client.post("/api/auth/login", json={"username": "ha_user", "password": "pw123"})
    assert res.status_code == 200
    return res.json()["access_token"]


def test_timer_start_by_name_and_fuzzy():
    client = TestClient(app)
    token = get_token(client)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Start by exact name
    res = client.post("/api/timer/start", json={"activity_name": "Pause"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["running"] is True
    assert data["entry"]["activity"]["name"] == "Pause"

    # 2. Start by prefix/fuzzy name "Thermo"
    res = client.post("/api/timer/start", json={"activity_name": "Thermo"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["running"] is True
    assert data["entry"]["activity"]["name"] == "Thermodynamik"

    # Stop timer
    res = client.post("/api/timer/stop", headers=headers)
    assert res.status_code == 200


def test_assist_nlp_flow():
    client = TestClient(app)
    token = get_token(client)
    headers = {"Authorization": f"Bearer {token}"}

    # Make sure idle first
    client.post("/api/timer/stop", headers=headers)

    # 1. Query while idle
    res = client.post("/api/assist/process", json={"text": "Was läuft gerade?"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["intent"] == "status"
    assert data["running"] is False
    assert "keine Zeiterfassung" in data["response"]

    # 2. Start via natural voice sentence: "Tracke bitte Thermodynamik"
    res = client.post("/api/assist/process", json={"text": "Tracke bitte Thermodynamik"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["intent"] == "start"
    assert data["running"] is True
    assert data["activity_name"] == "Thermodynamik"
    assert "Tracking für Thermodynamik gestartet" in data["response"]

    # 3. GET /api/assist/status for Home Assistant REST sensor
    res = client.get("/api/assist/status", headers=headers)
    assert res.status_code == 200
    status_data = res.json()
    assert status_data["running"] is True
    assert status_data["state"] == "tracking"
    assert status_data["activity"] == "Thermodynamik"

    # 4. Query status while tracking
    res = client.post("/api/assist/process", json={"text": "Wie lange läuft das schon?"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["intent"] == "status"
    assert data["running"] is True
    assert "Thermodynamik" in data["response"]

    # 5. Stop via voice: "Stoppe das Tracking"
    res = client.post("/api/assist/process", json={"text": "Stoppe das Tracking"}, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["intent"] == "stop"
    assert data["running"] is False
    assert "beendet" in data["response"]

    # 6. Verify idle status again
    res = client.get("/api/assist/status", headers=headers)
    assert res.status_code == 200
    assert res.json()["running"] is False
    assert res.json()["state"] == "idle"
