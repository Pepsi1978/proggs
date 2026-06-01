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
trap 'hook_log_warn "bug-almanac-index: Error at line $LINENO"' ERR

# Spam-Schutz-Marker des Guard-Hooks zuruecksetzen -> frische Erinnerung pro Session.
rm -f "${TMPDIR:-/tmp}"/bug-almanac-seen-*.flag 2>/dev/null || true

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

    ctx="BUG-ALMANACH-SYSTEM aktiv. Vorhandene Almanache in bugs/: $list. Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...): bugs/README.md pruefen und den passenden Almanach ZUERST lesen. Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann den Skill 'bug-almanach-recherche' STARTEN (das ist der vorgeschriebene Weg, NICHT selbst ad hoc recherchieren). Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump)."
    sysmsg="Bug-Almanach: $count Almanach(e) aktiv."

    python3 -c "import json,sys; print(json.dumps({'systemMessage': sys.argv[1], 'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': sys.argv[2]}}))" "$sysmsg" "$ctx"
fi

exit 0
