"""Server configuration from environment variables."""
import os


class Config:
    DB_HOST = os.environ.get("DB_HOST", "localhost")
    DB_PORT = int(os.environ.get("DB_PORT", "5432"))
    DB_NAME = os.environ.get("DB_NAME", "timetracker")
    DB_USER = os.environ.get("DB_USER", "timetracker")
    DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
    DATABASE_URL = os.environ.get(
        "DATABASE_URL",
        f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}",
    )

    LISTEN_HOST = os.environ.get("LISTEN_HOST", "127.0.0.1")
    LISTEN_PORT = int(os.environ.get("LISTEN_PORT", "8080"))

    TOKEN_TTL_HOURS = int(os.environ.get("TOKEN_TTL_HOURS", "24"))
    REFRESH_TOKEN_TTL_DAYS = int(os.environ.get("REFRESH_TOKEN_TTL_DAYS", "30"))
    API_TOKEN_TTL_DAYS = int(os.environ.get("API_TOKEN_TTL_DAYS", "3650"))

    SECRET_KEY_FILE = os.environ.get("SECRET_KEY_FILE", "/data/secret.key")
    DATA_DIR = os.environ.get("DATA_DIR", "/data")

    DEFAULT_TIMEZONE = os.environ.get("DEFAULT_TIMEZONE", "Europe/Berlin")
