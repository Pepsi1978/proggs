#!/bin/bash
# JDK-Schutz für Gradle-Builds (macOS): installiert das Gradle-Init-Skript und beendet sofort alle
# Gradle- und Kotlin-Daemons, deren JDK es nicht mehr gibt (nach einem Homebrew-JDK-Update).
# Aufgerufen von der Updater-Zentrale nach jedem Homebrew-Update; jederzeit gefahrlos von Hand.
# Endet immer mit 0: Ein Fehler hier darf kein Update als gescheitert melden.
ORDNER="$(cd "$(dirname "$0")" && pwd)"
ZIEL="$HOME/.gradle/init.d"

if mkdir -p "$ZIEL" 2>/dev/null && cp "$ORDNER/veraltete-jvm-daemons.gradle" "$ZIEL/veraltete-jvm-daemons.gradle.tmp" 2>/dev/null \
    && mv -f "$ZIEL/veraltete-jvm-daemons.gradle.tmp" "$ZIEL/veraltete-jvm-daemons.gradle"; then
    echo "JDK-Schutz: Gradle-Init-Skript ist aktuell ($ZIEL)."
else
    echo "JDK-Schutz: Gradle-Init-Skript ließ sich nicht nach $ZIEL kopieren."
fi

beendet=0
while read -r pid java; do
    [ -n "$java" ] || continue
    jdk="$(dirname "$(dirname "$java")")"
    if [ ! -e "$java" ] || [ ! -e "$jdk/lib/modules" ]; then
        if kill "$pid" 2>/dev/null; then
            echo "JDK-Schutz: veralteten Daemon $pid beendet (JDK $jdk existiert nicht mehr)."
            beendet=$((beendet + 1))
        fi
    fi
done < <(ps -axo pid=,command= 2>/dev/null | grep -E 'KotlinCompileDaemon|org\.gradle\.launcher\.daemon\.bootstrap\.GradleDaemon' | grep -v grep | awk '{print $1, $2}')

[ "$beendet" -eq 0 ] && echo "JDK-Schutz: keine veralteten Gradle-/Kotlin-Daemons."
exit 0
