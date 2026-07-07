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

# SessionStart-source ermitteln (startup/clear/resume/compact). Bewaehrtes Muster aus session-guard.
# Marker-Cleanup wird NUR bei einem KONTEXT-ERHALTENDEN Reset (compact/resume) UEBERSPRUNGEN, sonst
# (startup/clear/unbekannt) wie bisher geloescht. Grund (Frank-Wunsch 2026-06-22): SessionStart feuert
# auch bei Auto-Compact/Resume (gleiche Arbeitsphase, nur Kontext komprimiert) - dort die "gelesen"-
# Marker zu loeschen liess den Guard mitten im Arbeitsfluss erneut blocken (nervte mehrfach). FAIL-OPEN.
session_source=""
stdin_input=$(cat 2>/dev/null || echo "")
if [ -n "$stdin_input" ]; then
    session_source=$(echo "$stdin_input" | python3 -c "import sys,json
try:
    d=json.load(sys.stdin); print(d.get('source','') or '')
except Exception:
    print('')" 2>/dev/null || echo "")
fi

# Alle Guard-Marker zuruecksetzen -> frische Erinnerung + frischer Lese-Zwang pro echter Session.
# Deckt seen-* (Spam-Schutz), read-*/full-*/bp-read-* ("gelesen"-Marker) und disable (Notaus) ab.
# Der Notaus gilt damit nur fuer die Session in der er gesetzt wurde (Schutz ist danach wieder an).
case "$session_source" in
    compact|resume) : ;;  # Kontext-Erhalt -> "gelesen"-Marker behalten (Frank-Wunsch 2026-06-22)
    *) rm -f "${TMPDIR:-/tmp}"/bug-almanac-*.flag 2>/dev/null || true ;;
esac

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
    done < <(find "$BUGS_DIR" -name '*.md' -type f ! -name '*-kurzcheck.md' 2>/dev/null | sort)
    [ -n "$list" ] || list="(noch keine)"

    ctx="BUG-ALMANACH-SYSTEM aktiv (Digest-Modell, 3 Stufen - siehe bugs/SYSTEM.md Paragraf 11). Vorhandene Almanache in bugs/: $list. STUFE A (vor der Arbeit): Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...) NUR den Kurzcheck lesen: Read auf bugs/<kategorie>/<bereich>.md mit limit=80 (die Kurzcheck-Sektion oben in der Datei), DANACH ebenso die zugehoerige best-practices/projekt-code/<kategorie>/best-practices-<bereich>.md mit limit=80. Der bug-almanac-guard BLOCKIERT Edit/Write bis beides gelesen ist (Reihenfolge erst Almanach, dann Best Practices; 1x pro Bereich/Session). STUFE B (bei JEDEM Fehler im Bereich): SOFORT den VOLLTEXT des Almanachs lesen (Read ohne limit) - ab dem ersten Fehler reicht der Kurzcheck nicht mehr. STUFE C (Hochrisiko-Bereiche r8, firebase-billing, claude-hooks, claude-config): schon VOR der Arbeit den VOLLTEXT lesen - der Guard erzwingt das. Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann den Skill 'bug-almanach-recherche' STARTEN (das ist der vorgeschriebene Weg, NICHT selbst ad hoc recherchieren). Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump). Notaus bei Fehlalarm: leere Datei bug-almanac-disable.flag im TEMP-Verzeichnis anlegen."
    # -- Almanach-Trigger-Sonde: Reminder zum woechentlichen Auswerten (Direktive #2) --
    # Zaehlt block-Ereignisse (state/bug-almanac-triggers.jsonl + .jsonl.1) per grep (schnell).
    # state/almanach-last-review.txt: Zeile 1 Datum, Zeile 2 Block-Zahl der letzten Auswertung
    # (vom Skill almanach-trigger-auswertung gesetzt) -> "neue seit letzter Auswertung". Nur bei >0.
    reminder=""
    STATE_DIR="$HOME/.claude/state"
    total_blocks=0
    for tf in "$STATE_DIR/bug-almanac-triggers.jsonl.1" "$STATE_DIR/bug-almanac-triggers.jsonl"; do
        [ -f "$tf" ] || continue
        c=$(grep -c '"event":"block"' "$tf" 2>/dev/null || true); c=${c:-0}
        total_blocks=$((total_blocks + c))
    done
    review_file="$STATE_DIR/almanach-last-review.txt"
    prev_count=0; review_date=""
    if [ -f "$review_file" ]; then
        review_date=$(sed -n '1p' "$review_file" 2>/dev/null | tr -d '[:space:]' || true)
        prev_count=$(sed -n '2p' "$review_file" 2>/dev/null | tr -d '[:space:]' || true)
        [ -z "$prev_count" ] && prev_count=0
    fi
    new_blocks=$((total_blocks - prev_count))
    [ "$new_blocks" -lt 0 ] && new_blocks=$total_blocks
    if [ "$new_blocks" -gt 0 ]; then
        if [ -n "$review_date" ]; then
            days=$(python3 -c "import datetime,sys
try:
    print((datetime.date.today()-datetime.date.fromisoformat(sys.argv[1])).days)
except Exception:
    print('')" "$review_date" 2>/dev/null || true)
            if [ -n "$days" ]; then since=" (seit letzter Auswertung vor $days Tagen)"; else since=" (seit letzter Auswertung)"; fi
        else
            since=" (noch nie ausgewertet)"
        fi
        reminder=" | Almanach-Sonde: $new_blocks Unterbrechung(en)$since - sag 'werte die Almanach-Trigger aus'."
    fi
    sysmsg="Bug-Almanach: $count Almanach(e) aktiv.$reminder"

    python3 -c "import json,sys; print(json.dumps({'systemMessage': sys.argv[1], 'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': sys.argv[2]}}))" "$sysmsg" "$ctx"
fi

exit 0
