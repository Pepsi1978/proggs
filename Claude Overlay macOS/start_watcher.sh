#!/bin/bash
# Startet den Watcher im Hintergrund.
# Der Watcher ueberwacht, ob Claude laeuft, und startet/stoppt das Overlay automatisch.
# Dank PID-Lock wird nie eine zweite Instanz gestartet.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Pruefen ob bereits eine Instanz laeuft
if [ -f "watcher.pid" ]; then
    OLD_PID=$(cat "watcher.pid")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "Watcher laeuft bereits (PID: $OLD_PID). Keine zweite Instanz noetig."
        exit 0
    fi
    echo "Verwaiste PID-Datei gefunden, wird bereinigt."
    rm -f "watcher.pid"
fi

# Virtuelle Umgebung aktivieren (falls vorhanden)
if [ -d ".venv/bin" ]; then
    source .venv/bin/activate
fi

python3 src/watcher.py &
echo "Watcher gestartet (PID: $!)"
echo "Log: $SCRIPT_DIR/watcher.log"
