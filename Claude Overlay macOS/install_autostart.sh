#!/bin/bash
# Richtet einen macOS LaunchAgent ein, der den Watcher automatisch beim Login startet

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PLIST_NAME="com.claude-overlay.watcher"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_NAME}.plist"

# Python-Pfad ermitteln
if [ -d "$SCRIPT_DIR/.venv" ]; then
    PYTHON_PATH="$SCRIPT_DIR/.venv/bin/python3"
else
    PYTHON_PATH="$(which python3)"
fi

if [ ! -f "$PYTHON_PATH" ]; then
    echo "FEHLER: Python3 nicht gefunden!"
    echo "Bitte installiere Python3 oder erstelle eine virtuelle Umgebung (.venv)"
    exit 1
fi

# LaunchAgents-Ordner erstellen falls noetig
mkdir -p "$HOME/Library/LaunchAgents"

# Plist-Datei erstellen
cat > "$PLIST_PATH" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>${PLIST_NAME}</string>
    <key>ProgramArguments</key>
    <array>
        <string>${PYTHON_PATH}</string>
        <string>${SCRIPT_DIR}/src/watcher.py</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${SCRIPT_DIR}</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <false/>
    <key>StandardOutPath</key>
    <string>${SCRIPT_DIR}/watcher.log</string>
    <key>StandardErrorPath</key>
    <string>${SCRIPT_DIR}/watcher.log</string>
</dict>
</plist>
EOF

echo "LaunchAgent erstellt: $PLIST_PATH"

# LaunchAgent laden
launchctl unload "$PLIST_PATH" 2>/dev/null
launchctl load "$PLIST_PATH"

echo "LaunchAgent geladen. Der Watcher startet jetzt automatisch beim Login."
echo ""
echo "Um den Autostart zu entfernen, fuehre aus:"
echo "  bash uninstall_autostart.sh"
