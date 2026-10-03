-- Version 2: generic storage for synced categories and record tags.
-- The server treats the payload as opaque json in `data`; the app owns the
-- field layout, so new app side fields need no server migration.

CREATE TABLE IF NOT EXISTS sync_categories (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    data TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_sync_categories_updated_at ON sync_categories(updated_at);
CREATE INDEX IF NOT EXISTS ix_sync_categories_user_id ON sync_categories(user_id);

CREATE TABLE IF NOT EXISTS sync_tags (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    data TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_sync_tags_updated_at ON sync_tags(updated_at);
CREATE INDEX IF NOT EXISTS ix_sync_tags_user_id ON sync_tags(user_id);
