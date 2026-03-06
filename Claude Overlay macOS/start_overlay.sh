#!/bin/bash
# Startet das Overlay direkt (ohne Watcher)
# Nuetzlich zum Testen und Debuggen

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Virtuelle Umgebung aktivieren
if [ -d ".venv" ]; then
    source .venv/bin/activate
fi

# Overlay direkt starten (mit Konsolenausgabe)
python3 src/overlay_app.py
