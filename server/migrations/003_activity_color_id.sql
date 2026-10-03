-- Version 3: activity predefined color index.
-- The app stores predefined colors as a color id with an empty color
-- string; syncing only the string made every imported activity fall
-- back to the same color on other devices.

ALTER TABLE activities ADD COLUMN IF NOT EXISTS color_id INTEGER NOT NULL DEFAULT 0;
