#!/bin/bash
# Startet den Watcher im Hintergrund.
# Der Watcher ueberwacht, ob Claude laeuft, und startet/stoppt das Overlay automatisch.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Virtuelle Umgebung aktivieren (falls vorhanden)
if [ -d ".venv/bin" ]; then
    source .venv/bin/activate
fi

python src/watcher.py &
echo "Watcher gestartet (PID: $!)"
echo "Log: $SCRIPT_DIR/watcher.log"
