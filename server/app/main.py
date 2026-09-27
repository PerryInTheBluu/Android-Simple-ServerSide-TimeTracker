"""Single-user self-hosted time tracker server.

Runs behind an existing Caddy reverse proxy inside a private tailnet.
No telemetry, no third-party services, no public ports by itself.
"""
import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import api_router
from app.security import RequestContextMiddleware

app = FastAPI(
    title="Simple Time Tracker Server",
    version="1.0.0",
    docs_url="/api/docs",
    openapi_url="/api/openapi.json",
    redoc_url=None,
)

app.add_middleware(RequestContextMiddleware)

allowed_origins = os.environ.get("CORS_ORIGINS", "")
if allowed_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[o.strip() for o in allowed_origins.split(",") if o.strip()],
        allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type"],
    )

app.include_router(api_router, prefix="/api")
