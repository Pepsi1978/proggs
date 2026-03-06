#!/bin/bash
# Entfernt den macOS LaunchAgent fuer den Claude Overlay Watcher

PLIST_NAME="com.claude-overlay.watcher"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_NAME}.plist"

if [ -f "$PLIST_PATH" ]; then
    launchctl unload "$PLIST_PATH" 2>/dev/null
    rm "$PLIST_PATH"
    echo "LaunchAgent entfernt: $PLIST_PATH"
    echo "Der Watcher startet nicht mehr automatisch beim Login."
else
    echo "Kein LaunchAgent gefunden unter: $PLIST_PATH"
    echo "Autostart war nicht eingerichtet."
fi
