-- Initial schema for the time tracker server.
-- Version 1: users, tokens, activities, time_entries, goals, sync/conflict logs.

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR PRIMARY KEY,
    username VARCHAR UNIQUE NOT NULL,
    password_hash VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL REFERENCES users(id),
    token_hash VARCHAR UNIQUE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS api_tokens (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL REFERENCES users(id),
    name VARCHAR NOT NULL DEFAULT 'default',
    token_hash VARCHAR UNIQUE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_api_tokens_user_id ON api_tokens(user_id);

CREATE TABLE IF NOT EXISTS activities (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    color VARCHAR NOT NULL DEFAULT '',
    icon VARCHAR NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    parent_activity_id VARCHAR REFERENCES activities(id),
    category VARCHAR NOT NULL DEFAULT '',
    goal_seconds_per_week INTEGER,
    goal_seconds_total INTEGER,
    goal_days_per_month INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_activities_updated_at ON activities(updated_at);

CREATE TABLE IF NOT EXISTS time_entries (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    activity_id VARCHAR NOT NULL REFERENCES activities(id),
    parent_activity_ids VARCHAR NOT NULL DEFAULT '',
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    comment TEXT NOT NULL DEFAULT '',
    tags TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sync_status VARCHAR NOT NULL DEFAULT 'synced',
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_time_entries_updated_at ON time_entries(updated_at);
CREATE INDEX IF NOT EXISTS ix_time_entries_user_started ON time_entries(user_id, started_at);

CREATE TABLE IF NOT EXISTS goals (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    activity_id VARCHAR NOT NULL REFERENCES activities(id),
    goal_type VARCHAR NOT NULL DEFAULT 'seconds_per_week',
    goal_value DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_goals_updated_at ON goals(updated_at);

CREATE TABLE IF NOT EXISTS sync_log (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    entity_type VARCHAR NOT NULL,
    entity_id VARCHAR NOT NULL,
    action VARCHAR NOT NULL,
    detail TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_sync_log_user_id ON sync_log(user_id);

CREATE TABLE IF NOT EXISTS conflict_log (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    entity_type VARCHAR NOT NULL,
    entity_id VARCHAR NOT NULL,
    resolution VARCHAR NOT NULL,
    detail TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_conflict_log_user_id ON conflict_log(user_id);
