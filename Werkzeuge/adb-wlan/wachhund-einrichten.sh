#!/bin/bash
# Richtet den adb-WLAN-Wachhund ein (Gegenstück zu wachhund-einrichten.ps1): LaunchAgent de.frank.adb-wlan.wachhund
# ruft adb-wlan.sh --leise alle 2 Minuten und bei der Anmeldung auf. Der Wachhund startet den adb-Server nie neu.
# Ohne sudo. Mehrfach ausführbar.
set -euo pipefail
HIER="$(cd "$(dirname "$0")" && pwd)"
NAME="de.frank.adb-wlan.wachhund"
PLIST="$HOME/Library/LaunchAgents/$NAME.plist"
LOGS="$HOME/Library/Logs/adb-wlan"
mkdir -p "$LOGS" "$HOME/Library/LaunchAgents"
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
cat > "$PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$NAME</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>$HIER/adb-wlan.sh</string>
        <string>--leise</string>
    </array>
    <key>RunAtLoad</key><true/>
    <key>StartInterval</key><integer>120</integer>
    <key>EnvironmentVariables</key>
    <dict>
        <key>ANDROID_HOME</key><string>$SDK</string>
        <key>PATH</key><string>$SDK/platform-tools:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
    </dict>
    <key>StandardOutPath</key><string>$LOGS/wachhund.log</string>
    <key>StandardErrorPath</key><string>$LOGS/wachhund.log</string>
</dict>
</plist>
PLIST
launchctl bootout "gui/$(id -u)/$NAME" 2>/dev/null || true
launchctl bootstrap "gui/$(id -u)" "$PLIST"
echo "$NAME eingerichtet (alle 2 Minuten). Protokoll: $LOGS/adb-wlan.log"
