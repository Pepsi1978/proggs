#!/bin/bash
# Startet den Watcher im Hintergrund
# Der Watcher ueberwacht, ob Claude laeuft und startet/stoppt das Overlay automatisch

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Virtuelle Umgebung aktivieren
if [ -d ".venv" ]; then
    source .venv/bin/activate
fi

# Watcher im Hintergrund starten
nohup python3 src/watcher.py > /dev/null 2>&1 &
echo "Watcher gestartet (PID: $!)"
