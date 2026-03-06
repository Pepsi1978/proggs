#!/bin/bash
# Stoppt den laufenden Watcher und das Overlay

echo "Suche laufende Watcher- und Overlay-Prozesse..."

# Watcher-Prozesse finden und beenden
WATCHER_PIDS=$(pgrep -f "watcher.py" 2>/dev/null)
OVERLAY_PIDS=$(pgrep -f "overlay_app.py" 2>/dev/null)

if [ -n "$WATCHER_PIDS" ]; then
    echo "Beende Watcher (PIDs: $WATCHER_PIDS)"
    kill $WATCHER_PIDS 2>/dev/null
fi

if [ -n "$OVERLAY_PIDS" ]; then
    echo "Beende Overlay (PIDs: $OVERLAY_PIDS)"
    kill $OVERLAY_PIDS 2>/dev/null
fi

if [ -z "$WATCHER_PIDS" ] && [ -z "$OVERLAY_PIDS" ]; then
    echo "Keine laufenden Prozesse gefunden."
else
    echo "Alle Prozesse beendet."
fi
