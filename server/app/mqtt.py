"""MQTT integration & Home Assistant Autodiscovery support for TimeTracker.

Zero-dependency MQTT v3.1.1 client for publishing discovery configurations
and live state updates to Home Assistant via Mosquitto or any MQTT broker.
"""
import json
import logging
import os
import socket
import struct
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)

# Default MQTT broker configuration from environment
MQTT_HOST = os.environ.get("MQTT_HOST", "127.0.0.1")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
MQTT_USER = os.environ.get("MQTT_USER", "")
MQTT_PASSWORD = os.environ.get("MQTT_PASSWORD", "")
MQTT_TOPIC_PREFIX = os.environ.get("MQTT_TOPIC_PREFIX", "timetracker")
HA_DISCOVERY_PREFIX = os.environ.get("HA_DISCOVERY_PREFIX", "homeassistant")


def get_ha_device_info() -> Dict[str, Any]:
    """Home Assistant device metadata block."""
    return {
        "identifiers": ["timetracker_server"],
        "name": "TimeTracker",
        "model": "ServerSide TimeTracker",
        "manufacturer": "Pius Dischinger",
        "sw_version": "1.60.0",
    }


def get_ha_discovery_payloads(
    topic_prefix: str = MQTT_TOPIC_PREFIX,
    discovery_prefix: str = HA_DISCOVERY_PREFIX,
) -> Dict[str, Dict[str, Any]]:
    """Returns all Home Assistant MQTT Autodiscovery topics and configurations."""
    state_topic = f"{topic_prefix}/state"
    device = get_ha_device_info()

    configs = {
        f"{discovery_prefix}/sensor/timetracker_status/config": {
            "name": "TimeTracker Status",
            "unique_id": "timetracker_sensor_status",
            "state_topic": state_topic,
            "value_template": "{{ value_json.state }}",
            "icon": "mdi:timer-outline",
            "device": device,
        },
        f"{discovery_prefix}/sensor/timetracker_activity/config": {
            "name": "TimeTracker Aktive Aktivität",
            "unique_id": "timetracker_sensor_activity",
            "state_topic": state_topic,
            "value_template": "{{ value_json.activity | default('Keine', true) }}",
            "icon": "mdi:briefcase-clock-outline",
            "device": device,
        },
        f"{discovery_prefix}/sensor/timetracker_duration_minutes/config": {
            "name": "TimeTracker Laufzeit Minuten",
            "unique_id": "timetracker_sensor_duration_minutes",
            "state_topic": state_topic,
            "value_template": "{{ value_json.duration_minutes | default(0, true) }}",
            "unit_of_measurement": "min",
            "icon": "mdi:clock-outline",
            "device": device,
        },
        f"{discovery_prefix}/sensor/timetracker_started_at/config": {
            "name": "TimeTracker Gestartet um",
            "unique_id": "timetracker_sensor_started_at",
            "state_topic": state_topic,
            "value_template": "{{ value_json.started_at }}",
            "device_class": "timestamp",
            "icon": "mdi:clock-start",
            "device": device,
        },
    }
    return configs


def build_status_payload(
    running: bool,
    activity_name: Optional[str] = None,
    duration_seconds: int = 0,
    started_at: Optional[str] = None,
    entry_id: Optional[str] = None,
) -> Dict[str, Any]:
    """Generates the standardized JSON state payload."""
    duration_min = round(duration_seconds / 60) if duration_seconds else 0
    return {
        "state": "tracking" if running else "idle",
        "running": running,
        "activity": activity_name if running else None,
        "duration_seconds": duration_seconds,
        "duration_minutes": duration_min,
        "started_at": started_at if running else None,
        "entry_id": entry_id if running else None,
    }


def _encode_remaining_length(length: int) -> bytes:
    encoded = bytearray()
    while True:
        digit = length % 128
        length //= 128
        if length > 0:
            digit |= 0x80
        encoded.append(digit)
        if length == 0:
            break
    return bytes(encoded)


def _encode_str(s: str) -> bytes:
    raw = s.encode("utf-8")
    return struct.pack("!H", len(raw)) + raw


def publish_mqtt_message(
    topic: str,
    payload: str,
    host: str = MQTT_HOST,
    port: int = MQTT_PORT,
    username: str = MQTT_USER,
    password: str = MQTT_PASSWORD,
    retain: bool = False,
    timeout: float = 2.0,
) -> bool:
    """Publishes a single MQTT message using raw socket (v3.1.1 QoS 0).

    Does not require any external paho-mqtt dependency.
    """
    try:
        sock = socket.create_connection((host, port), timeout=timeout)
        try:
            # 1. CONNECT packet
            flags = 0x02  # CleanSession
            if username:
                flags |= 0x80
            if password:
                flags |= 0x40

            var_header = b"\x00\x04MQTT\x04" + bytes([flags]) + struct.pack("!H", 60)
            payload_data = _encode_str("timetracker_pub")
            if username:
                payload_data += _encode_str(username)
            if password:
                payload_data += _encode_str(password)

            connect_pkt = (
                b"\x10"
                + _encode_remaining_length(len(var_header) + len(payload_data))
                + var_header
                + payload_data
            )
            sock.sendall(connect_pkt)

            # Read CONNACK (4 bytes: 0x20 0x02 0x00 0x00)
            connack = sock.recv(4)
            if len(connack) < 4 or connack[0] != 0x20 or connack[3] != 0x00:
                logger.warning("MQTT CONNACK failed or refused: %r", connack)
                return False

            # 2. PUBLISH packet (QoS 0)
            pub_flags = 0x30 | (0x01 if retain else 0x00)
            topic_bytes = _encode_str(topic)
            msg_bytes = payload.encode("utf-8")
            pub_pkt = (
                bytes([pub_flags])
                + _encode_remaining_length(len(topic_bytes) + len(msg_bytes))
                + topic_bytes
                + msg_bytes
            )
            sock.sendall(pub_pkt)

            # 3. DISCONNECT packet (0xe0 0x00)
            sock.sendall(b"\xe0\x00")
            return True
        finally:
            sock.close()
    except Exception as exc:
        logger.debug("Failed to publish MQTT message to %s:%d: %s", host, port, exc)
        return False


def publish_discovery_and_state(
    running: bool,
    activity_name: Optional[str] = None,
    duration_seconds: int = 0,
    started_at: Optional[str] = None,
    host: str = MQTT_HOST,
    port: int = MQTT_PORT,
    username: str = MQTT_USER,
    password: str = MQTT_PASSWORD,
    topic_prefix: str = MQTT_TOPIC_PREFIX,
) -> bool:
    """Helper to publish all HA autodiscovery configs and current state."""
    # Publish discovery configs (retained)
    for disc_topic, disc_conf in get_ha_discovery_payloads(topic_prefix=topic_prefix).items():
        publish_mqtt_message(
            topic=disc_topic,
            payload=json.dumps(disc_conf),
            host=host,
            port=port,
            username=username,
            password=password,
            retain=True,
        )

    # Publish state (retained)
    state = build_status_payload(
        running=running,
        activity_name=activity_name,
        duration_seconds=duration_seconds,
        started_at=started_at,
    )
    return publish_mqtt_message(
        topic=f"{topic_prefix}/state",
        payload=json.dumps(state),
        host=host,
        port=port,
        username=username,
        password=password,
        retain=True,
    )
