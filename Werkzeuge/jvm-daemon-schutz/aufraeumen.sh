#!/bin/bash
# JDK-Schutz für Gradle-Builds (macOS). Nach einem Homebrew-JDK-Update läuft ein alter Kotlin-Compile-Daemon
# auf dem gelöschten JDK weiter und bricht jeden Build ab („no class roots are found in the JDK path“).
#   aufraeumen.sh            Gradle-Init-Skript und launchd-Wächter aktuell halten, dann veraltete Daemons beenden
#                            (Updater-Zentrale nach jedem Homebrew-Update; jederzeit gefahrlos von Hand).
#   aufraeumen.sh --waechter Nur prüfen, jetzt und nach 30 s noch einmal (vom launchd-Wächter; brew löscht den
#                            alten JDK-Ordner erst nach dem Neuverlinken).
# Endet immer mit 0: Ein Fehler hier darf kein Update als gescheitert melden.
ORDNER="$(cd "$(dirname "$0")" && pwd)"
NAME="de.frank.jdk-schutz"
PLIST="$HOME/Library/LaunchAgents/$NAME.plist"

java_finden() {
    local kandidat
    for kandidat in "$(/usr/libexec/java_home 2>/dev/null)" "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
                    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"; do
        [ -n "$kandidat" ] && [ -x "$kandidat/bin/java" ] && { echo "$kandidat/bin/java"; return; }
    done
}

pruefen() {
    local java
    java="$(java_finden)"
    if [ -z "$java" ]; then
        echo "JDK-Schutz: kein JDK gefunden, Prüfung übersprungen."
        return
    fi
    "$java" "$ORDNER/VeralteteDaemons.java" 2>&1 || echo "JDK-Schutz: Prüfung fehlgeschlagen ($java)."
}

if [ "$1" = "--waechter" ]; then
    echo "$(date '+%d.%m.%Y %H:%M:%S') Wächter ausgelöst"
    pruefen
    sleep 30
    pruefen
    exit 0
fi

# Gradle-Init-Skript (prüft vor jedem Build ohne Configuration-Cache-Treffer).
ZIEL="$HOME/.gradle/init.d"
if mkdir -p "$ZIEL" 2>/dev/null && cp "$ORDNER/veraltete-jvm-daemons.gradle" "$ZIEL/veraltete-jvm-daemons.gradle.tmp" 2>/dev/null \
    && mv -f "$ZIEL/veraltete-jvm-daemons.gradle.tmp" "$ZIEL/veraltete-jvm-daemons.gradle"; then
    echo "JDK-Schutz: Gradle-Init-Skript ist aktuell ($ZIEL)."
else
    echo "JDK-Schutz: Gradle-Init-Skript ließ sich nicht nach $ZIEL kopieren."
fi

# launchd-Wächter: reagiert auf jedes Homebrew-JDK-Update, auch von Hand im Terminal. /opt/homebrew/opt ändert
# sich beim Neuverlinken, die openjdk-Ordner im Cellar beim Löschen der alten Version.
if [ -d /opt/homebrew/opt ]; then
    pfade="    <string>/opt/homebrew/opt</string>"
    for d in /opt/homebrew/Cellar/openjdk*; do
        [ -d "$d" ] && pfade="$pfade
    <string>$d</string>"
    done
    neu="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">
<plist version=\"1.0\">
<dict>
  <key>Label</key><string>$NAME</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/bash</string>
    <string>$ORDNER/aufraeumen.sh</string>
    <string>--waechter</string>
  </array>
  <key>WatchPaths</key>
  <array>
$pfade
  </array>
  <key>RunAtLoad</key><false/>
  <key>StandardOutPath</key><string>$HOME/Library/Logs/jdk-schutz.log</string>
  <key>StandardErrorPath</key><string>$HOME/Library/Logs/jdk-schutz.log</string>
</dict>
</plist>"
    if [ "$(cat "$PLIST" 2>/dev/null)" != "$neu" ]; then
        mkdir -p "$(dirname "$PLIST")"
        printf '%s\n' "$neu" > "$PLIST.tmp" && mv -f "$PLIST.tmp" "$PLIST"
        launchctl bootout "gui/$(id -u)/$NAME" 2>/dev/null
        if launchctl bootstrap "gui/$(id -u)" "$PLIST" 2>/dev/null; then
            echo "JDK-Schutz: launchd-Wächter eingerichtet."
        else
            echo "JDK-Schutz: launchd-Wächter ließ sich nicht laden ($PLIST)."
        fi
    else
        echo "JDK-Schutz: launchd-Wächter ist aktuell."
    fi
fi

pruefen
exit 0
