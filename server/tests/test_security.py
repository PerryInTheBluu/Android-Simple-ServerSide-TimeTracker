"""Regression tests for password hashing.

passlib 1.7.4 is incompatible with bcrypt >= 5.0: bcrypt 5 removed the
silent truncation of secrets longer than 72 bytes and passlib's backend
self-check then raises ValueError on every hash() call. These tests fail
with an incompatible bcrypt version and pass with the pinned one.
"""
import os
import tempfile

os.environ["DATABASE_URL"] = "sqlite://"
os.environ["SECRET_KEY_FILE"] = os.path.join(tempfile.mkdtemp(), "secret.key")

from app.security import hash_password, verify_password  # noqa: E402


def test_password_roundtrip():
    password = "password123"

    password_hash = hash_password(password)

    assert password_hash.startswith("$2")
    assert verify_password(password, password_hash)
    assert not verify_password("wrong-password", password_hash)


def test_password_longer_than_72_bytes_does_not_crash():
    password = "x" * 100

    password_hash = hash_password(password)

    assert verify_password(password, password_hash)
