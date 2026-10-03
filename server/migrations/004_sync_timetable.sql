-- Version 4: generic storage for synced timetable entities (events,
-- overrides, free days, todos) and subject hour goals. The server treats
-- the payload as opaque json in `data`; the app owns the field layout,
-- so new app side fields need no server migration.

CREATE TABLE IF NOT EXISTS sync_timetable (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    entity_type VARCHAR NOT NULL,
    data TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_sync_timetable_updated_at ON sync_timetable(updated_at);
CREATE INDEX IF NOT EXISTS ix_sync_timetable_user_id ON sync_timetable(user_id);
CREATE INDEX IF NOT EXISTS ix_sync_timetable_type_id ON sync_timetable(entity_type, id);
