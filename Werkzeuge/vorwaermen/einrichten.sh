#!/bin/bash
# Richtet das Vorwärmen am Mac ein (Gegenstück zu einrichten.ps1), mehrfach ausführbar, ohne sudo:
# 1. Räumt eine frühere globale Leerlauf-Grenze aus ~/.gradle/gradle.properties (24 h gilt nur für den vorgewärmten Daemon).
# 2. LaunchAgent de.frank.vorwaermen: bei der Anmeldung (startet den Daemon), alle 5 Minuten zur vollen Fünferminute und nach
#    dem Aufwachen sofort (launchd holt einen verpassten StartCalendarInterval-Lauf nach dem Aufwachen einmal nach).
#    Jeder Lauf spult das Repo vor (nur Fast-Forward) und baut geänderte Apps von Handy/Tablet nach.
# AbandonProcessGroup: Der per `gradlew help` gestartete Daemon gehört zur Prozessgruppe des Laufs; ohne den Schlüssel
# beendete launchd ihn am Ende des Laufs mit (wie wireguard-go im Almanach samba-wireguard).
set -euo pipefail
HIER="$(cd "$(dirname "$0")" && pwd)"
NAME="de.frank.vorwaermen"
PLIST="$HOME/Library/LaunchAgents/$NAME.plist"
LOGS="$HOME/Library/Logs/vorwaermen"
mkdir -p "$LOGS" "$HOME/Library/LaunchAgents" "$HOME/.gradle"

PROPS="$HOME/.gradle/gradle.properties"
if [ -f "$PROPS" ] && grep -qE '^\s*org\.gradle\.daemon\.idletimeout\s*=' "$PROPS"; then
    grep -vE '^\s*org\.gradle\.daemon\.idletimeout\s*=' "$PROPS" > "$PROPS.neu" || true
    mv "$PROPS.neu" "$PROPS"
fi

PYTHON="$(command -v python3 || true)"
[ -x /opt/homebrew/bin/python3 ] && PYTHON=/opt/homebrew/bin/python3
[ -n "$PYTHON" ] || { echo "python3 fehlt (brew install python)"; exit 1; }
# Dieselbe Java wie in den Sitzungen (stabiler opt-Pfad, nie der Cellar-Pfad: der bricht nach jedem brew upgrade, siehe
# Gedächtnis jdk-update-kotlin-daemon), sonst passte der warme Daemon nicht zu den Sitzungsbauten.
JAVA="${JAVA_HOME:-}"
case "$JAVA" in ""|*/Cellar/*) JAVA="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ;; esac
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"

TAKT=""
for m in 0 5 10 15 20 25 30 35 40 45 50 55; do TAKT="$TAKT        <dict><key>Minute</key><integer>$m</integer></dict>
"; done

cat > "$PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$NAME</string>
    <key>ProgramArguments</key>
    <array>
        <string>$PYTHON</string>
        <string>$HIER/vorwaermen.py</string>
        <string>lauf</string>
    </array>
    <key>WorkingDirectory</key><string>$HOME/proggs</string>
    <key>RunAtLoad</key><true/>
    <key>StartCalendarInterval</key>
    <array>
$TAKT    </array>
    <key>AbandonProcessGroup</key><true/>
    <key>ProcessType</key><string>Interactive</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>JAVA_HOME</key><string>$JAVA</string>
        <key>ANDROID_HOME</key><string>$SDK</string>
        <key>ANDROID_SDK_ROOT</key><string>$SDK</string>
        <key>PATH</key><string>$JAVA/bin:$SDK/platform-tools:/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
        <key>LANG</key><string>de_DE.UTF-8</string>
    </dict>
    <key>StandardOutPath</key><string>$LOGS/launchd.log</string>
    <key>StandardErrorPath</key><string>$LOGS/launchd.log</string>
</dict>
</plist>
PLIST

# bootout kehrt zurück, bevor launchd den Job ganz abgebaut hat; ein sofortiges bootstrap scheitert dann mit „Bootstrap failed: 5“.
launchctl bootout "gui/$(id -u)/$NAME" 2>/dev/null || true
for _ in $(seq 1 20); do launchctl print "gui/$(id -u)/$NAME" >/dev/null 2>&1 || break; sleep 0.5; done
GELADEN=0
for _ in 1 2 3 4 5; do
    if launchctl bootstrap "gui/$(id -u)" "$PLIST" 2>/dev/null; then GELADEN=1; break; fi
    sleep 1
done
[ $GELADEN -eq 1 ] || { echo "FEHLER: launchctl bootstrap $PLIST ist fehlgeschlagen"; exit 1; }
echo "VORWAERMEN_EINRICHTEN=ok ($NAME, alle 5 Minuten, Log ~/.openlauncher/vorwaermen/log.txt)"
