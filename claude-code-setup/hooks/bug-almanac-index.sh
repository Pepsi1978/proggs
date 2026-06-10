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
    # Almanache liegen in Kategorie-Unterordnern (bugs/<kategorie>/<file>.md). Rekursiv sammeln und
    # als relativen Pfad ab bugs/ anzeigen; alles direkt in bugs/ (README/SYSTEM/OFFENE-*) hat keinen
    # '/' im Relativpfad und faellt damit automatisch raus.
    while IFS= read -r f; do
        [ -e "$f" ] || continue
        rel="${f#"$BUGS_DIR"/}"
        case "$rel" in
            */*) list="${list:+$list, }$rel"; count=$((count + 1));;
        esac
    done < <(find "$BUGS_DIR" -name '*.md' -type f 2>/dev/null | sort)
    [ -n "$list" ] || list="(noch keine)"

    ctx="BUG-ALMANACH-SYSTEM aktiv (Digest-Modell, 3 Stufen - siehe bugs/SYSTEM.md Paragraf 11). Vorhandene Almanache in bugs/: $list. STUFE A (vor der Arbeit): Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...) NUR den Kurzcheck lesen: Read auf bugs/<kategorie>/<bereich>.md mit limit=80 (die Kurzcheck-Sektion oben in der Datei), DANACH ebenso die zugehoerige best-practices/projekt-code/<kategorie>/best-practices-<bereich>.md mit limit=80. Der bug-almanac-guard BLOCKIERT Edit/Write bis beides gelesen ist (Reihenfolge erst Almanach, dann Best Practices; 1x pro Bereich/Session). STUFE B (bei JEDEM Fehler im Bereich): SOFORT den VOLLTEXT des Almanachs lesen (Read ohne limit) - ab dem ersten Fehler reicht der Kurzcheck nicht mehr. STUFE C (Hochrisiko-Bereiche r8, firebase-billing, claude-hooks, claude-config): schon VOR der Arbeit den VOLLTEXT lesen - der Guard erzwingt das. Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann den Skill 'bug-almanach-recherche' STARTEN (das ist der vorgeschriebene Weg, NICHT selbst ad hoc recherchieren). Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump). Notaus bei Fehlalarm: leere Datei bug-almanac-disable.flag im TEMP-Verzeichnis anlegen."
    sysmsg="Bug-Almanach: $count Almanach(e) aktiv."

    python3 -c "import json,sys; print(json.dumps({'systemMessage': sys.argv[1], 'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': sys.argv[2]}}))" "$sysmsg" "$ctx"
fi

exit 0
