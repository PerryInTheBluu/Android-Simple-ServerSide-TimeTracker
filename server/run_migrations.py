"""Runs versioned SQL migrations from the migrations directory.

A schema_migrations table tracks applied versions.
Migrations run in filename order and are applied transactionally.
"""
import os
import re

from sqlalchemy import text

from app.db import engine

MIGRATIONS_DIR = os.path.join(os.path.dirname(__file__), "migrations")


def applied_versions(conn) -> set:
    conn.execute(
        text(
            "CREATE TABLE IF NOT EXISTS schema_migrations ("
            "version VARCHAR PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"
        )
    )
    conn.commit()
    rows = conn.execute(text("SELECT version FROM schema_migrations")).fetchall()
    return {row[0] for row in rows}


def run_migrations() -> None:
    files = sorted(f for f in os.listdir(MIGRATIONS_DIR) if re.match(r"^\d+_\w+\.sql$", f))
    with engine.begin() as outer:
        pass
    with engine.connect() as conn:
        done = applied_versions(conn)
        for filename in files:
            version = filename.rsplit(".", 1)[0]
            if version in done:
                continue
            print(f"Applying migration {version}")
            with open(os.path.join(MIGRATIONS_DIR, filename)) as f:
                sql = f.read()
            conn.execute(text(sql))
            conn.execute(
                text("INSERT INTO schema_migrations (version) VALUES (:v)"),
                {"v": version},
            )
            conn.commit()
            print(f"Applied {version}")
    print("Migrations up to date")


if __name__ == "__main__":
    run_migrations()
