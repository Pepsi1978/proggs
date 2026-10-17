#!/bin/bash
# Richtet die Jarvis-Mac-Brücke ein (Gegenstück zu einrichten.ps1): LaunchAgent de.frank.jarvis.pc-bruecke startet sie bei
# jeder Anmeldung und sofort neu, falls sie endet (KeepAlive ersetzt den Minuten-Wächter der Windows-Aufgabe).
# Ohne sudo, nur für den angemeldeten Benutzer. Mehrfach ausführbar.
set -euo pipefail
HIER="$(cd "$(dirname "$0")" && pwd)"
SKRIPT="$HIER/jarvis_pc_bruecke.py"
NAME="de.frank.jarvis.pc-bruecke"
PLIST="$HOME/Library/LaunchAgents/$NAME.plist"
LOGS="$HOME/Library/Logs/JarvisPcBruecke"
PYTHON="$(command -v python3 || true)"
[ -x /opt/homebrew/bin/python3 ] && PYTHON=/opt/homebrew/bin/python3
[ -n "$PYTHON" ] || { echo "python3 fehlt (brew install python)"; exit 1; }
mkdir -p "$LOGS" "$HOME/Library/LaunchAgents"

cat > "$PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$NAME</string>
    <key>ProgramArguments</key>
    <array>
        <string>$PYTHON</string>
        <string>$SKRIPT</string>
    </array>
    <key>WorkingDirectory</key><string>$HOME/proggs</string>
    <key>RunAtLoad</key><true/>
    <key>KeepAlive</key><true/>
    <key>ThrottleInterval</key><integer>30</integer>
    <key>ProcessType</key><string>Interactive</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>PATH</key><string>/opt/homebrew/bin:/opt/homebrew/sbin:$HOME/.local/bin:/opt/homebrew/share/android-commandlinetools/platform-tools:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
        <key>PYTHONUNBUFFERED</key><string>1</string>
    </dict>
    <key>StandardOutPath</key><string>$LOGS/launchd.log</string>
    <key>StandardErrorPath</key><string>$LOGS/launchd.log</string>
</dict>
</plist>
PLIST

launchctl bootout "gui/$(id -u)/$NAME" 2>/dev/null || true
launchctl bootstrap "gui/$(id -u)" "$PLIST"
launchctl kickstart -k "gui/$(id -u)/$NAME"
echo "$NAME eingerichtet und gestartet. Protokoll: $LOGS/bruecke.log"
[ -f "$HOME/SK/Jarvis/relay.properties" ] || echo "ACHTUNG: ~/SK/Jarvis/relay.properties fehlt, die Brücke wartet, bis sie da ist."
