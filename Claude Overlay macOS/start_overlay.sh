#!/bin/bash
# Startet das Overlay direkt (ohne Watcher).
# Claude muss bereits laufen.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Virtuelle Umgebung aktivieren (falls vorhanden)
if [ -d ".venv/bin" ]; then
    source .venv/bin/activate
fi

python src/overlay_app.py
