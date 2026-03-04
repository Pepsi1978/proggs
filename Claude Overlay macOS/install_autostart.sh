#!/bin/bash
# Richtet den macOS LaunchAgent ein, damit der Watcher beim Login automatisch startet.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PLIST_NAME="com.claude-overlay.watcher"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_NAME}.plist"
PYTHON_PATH="$SCRIPT_DIR/.venv/bin/python"
WATCHER_SCRIPT="$SCRIPT_DIR/src/watcher.py"

# Pruefen ob Python in der venv existiert
if [ ! -f "$PYTHON_PATH" ]; then
    PYTHON_PATH=$(which python3)
    if [ -z "$PYTHON_PATH" ]; then
        echo "Fehler: Python nicht gefunden. Erstelle zuerst die virtuelle Umgebung."
        exit 1
    fi
    echo "Hinweis: Verwende System-Python: $PYTHON_PATH"
fi

# LaunchAgents-Ordner erstellen (falls noetig)
mkdir -p "$HOME/Library/LaunchAgents"

# Bestehenden Agent stoppen (falls vorhanden)
if launchctl list | grep -q "$PLIST_NAME"; then
    launchctl unload "$PLIST_PATH" 2>/dev/null
    echo "Bestehender LaunchAgent gestoppt."
fi

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
        <string>${WATCHER_SCRIPT}</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${SCRIPT_DIR}</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <false/>
    <key>StandardOutPath</key>
    <string>${SCRIPT_DIR}/watcher_stdout.log</string>
    <key>StandardErrorPath</key>
    <string>${SCRIPT_DIR}/watcher_stderr.log</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>PATH</key>
        <string>/usr/local/bin:/usr/bin:/bin:/opt/homebrew/bin</string>
    </dict>
</dict>
</plist>
EOF

# LaunchAgent laden
launchctl load "$PLIST_PATH"

echo ""
echo "=== Autostart eingerichtet ==="
echo "LaunchAgent: $PLIST_PATH"
echo "Python:      $PYTHON_PATH"
echo "Watcher:     $WATCHER_SCRIPT"
echo ""
echo "Der Watcher startet jetzt automatisch bei jedem Login."
echo "Zum Entfernen: bash uninstall_autostart.sh"
