#!/usr/bin/env bash
set -euo pipefail

# Deploy website static files to Caddy webroot
TARGET_DIR="${1:-/var/www/timetracker}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
WEBSITE_SRC="$REPO_ROOT/website"

echo "Deploying website files from: $WEBSITE_SRC"
echo "Target directory: $TARGET_DIR"

if [ ! -d "$WEBSITE_SRC" ]; then
    echo "Error: Website source directory not found at $WEBSITE_SRC" >&2
    exit 1
fi

sudo mkdir -p "$TARGET_DIR"
sudo cp "$WEBSITE_SRC/index.html" "$TARGET_DIR/"
if [ -f "$WEBSITE_SRC/style.css" ]; then
    sudo cp "$WEBSITE_SRC/style.css" "$TARGET_DIR/"
fi
if [ -f "$WEBSITE_SRC/script.js" ]; then
    sudo cp "$WEBSITE_SRC/script.js" "$TARGET_DIR/"
fi

# Ensure correct permissions for caddy user/group
if id "caddy" &>/dev/null; then
    sudo chown -R caddy:caddy "$TARGET_DIR"
fi
sudo chmod -R 755 "$TARGET_DIR"

echo "Website static files deployed successfully."

# Reload Caddy if service is active
if systemctl is-active --quiet caddy; then
    echo "Reloading Caddy..."
    sudo systemctl reload caddy
    echo "Caddy reloaded successfully."
else
    echo "Caddy service not running or not found. Please reload or start Caddy manually: 'sudo systemctl reload caddy'"
fi
