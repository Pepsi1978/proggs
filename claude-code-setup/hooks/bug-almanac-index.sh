#!/usr/bin/env bash
# bug-almanac-index: Blendet bei Session-Start die Liste der vorhandenen Bug-Almanache ein.
# Schicht 1 ("Praesenz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as SessionStart hook (type: command).
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
#         systemMessage -> sichtbare Meldung fuer den Benutzer.
# Platform: macOS/Linux

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
trap 'hook_log_warn "bug-almanac-index: Error at line $LINENO"; exit 0' ERR

# Alle Guard-Marker zuruecksetzen -> frische Erinnerung + frischer Lese-Zwang pro Session.
# Deckt seen-* (Spam-Schutz), read-* (Stufe-2-"gelesen"-Marker) und disable (Notaus) ab.
# Der Notaus gilt damit nur fuer die Session in der er gesetzt wurde (Schutz ist danach wieder an).
rm -f "${TMPDIR:-/tmp}"/bug-almanac-*.flag 2>/dev/null || true

BUGS_DIR="$HOME/proggs/bugs"
if [ -d "$BUGS_DIR" ]; then
    list=""
    count=0
    for f in "$BUGS_DIR"/*.md; do
        [ -e "$f" ] || continue
        base="$(basename "$f")"
        if [ "$base" != "README.md" ] && [ "$base" != "SYSTEM.md" ]; then
            list="${list:+$list, }$base"
            count=$((count + 1))
        fi
    done
    [ -n "$list" ] || list="(noch keine)"

    ctx="BUG-ALMANACH-SYSTEM aktiv (Stufe 2 = ERZWINGUNG). Vorhandene Almanache in bugs/: $list. Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...): bugs/README.md pruefen und den passenden Almanach ZUERST mit dem Read-Tool lesen. WICHTIG: Existiert ein Almanach fuer den Bereich, BLOCKIERT der bug-almanac-guard Edit/Write so lange, bis du die Almanach-Datei in dieser Session per Read geoeffnet hast (Lesen gibt den Bereich frei, gilt pro Bereich 1x/Session). Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann den Skill 'bug-almanach-recherche' STARTEN (das ist der vorgeschriebene Weg, NICHT selbst ad hoc recherchieren). Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump). Notaus bei Fehlalarm: leere Datei bug-almanac-disable.flag im TEMP-Verzeichnis anlegen."
    sysmsg="Bug-Almanach: $count Almanach(e) aktiv."

    python3 -c "import json,sys; print(json.dumps({'systemMessage': sys.argv[1], 'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': sys.argv[2]}}))" "$sysmsg" "$ctx"
fi

exit 0
