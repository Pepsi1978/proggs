#!/usr/bin/env bash
# session-backup-midturn: Mid-Turn-Schwester von session-backup-nudge.
#   Faengt Kontext-Spruenge >=80% INNERHALB eines langen Turns (z.B. Researcher-Schwarm),
#   wo der Stop-Hook (nur ZWISCHEN Turns) strukturell nicht hinschaut.
# Runs as PostToolUse hook, matcher "Task|Agent" (NICHT Bash: Regression #55889 droppt Kontext).
# Teilt ctx-Datei UND Marker (ctx-nudged-<sid>) mit session-backup-nudge -> kein Doppel-Feuern.
# PostToolUse: IMMER exit 0; Kontext via nested hookSpecificOutput.additionalContext.
# Kein 'set -e' (if-Guards + garantiertes exit 0). JSON per python3 (kein jq-Zwang, claude-hooks.md 13.2).
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
trap 'hook_log_warn "session-backup-midturn: error at line $LINENO" 2>/dev/null || true; exit 0' ERR

stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi

# Input-Guard: PostToolUse braucht tool_name
tool_name=$(printf '%s' "$stdin_input" | python3 -c 'import sys,json
try:
    print(json.load(sys.stdin).get("tool_name","") or "")
except Exception:
    print("")' 2>/dev/null || echo "")
if [ -z "$tool_name" ]; then exit 0; fi

sid=$(printf '%s' "$stdin_input" | python3 -c 'import sys,json
try:
    print(json.load(sys.stdin).get("session_id","") or "")
except Exception:
    print("")' 2>/dev/null || echo "")
case "$sid" in
    ''|*[!a-zA-Z0-9_-]*) exit 0 ;;
esac

state_dir="$HOME/.claude/state"
# Geteilter Marker mit session-backup-nudge: nur EINMAL pro Session
marker="$state_dir/ctx-nudged-$sid"
if [ -f "$marker" ]; then exit 0; fi

ctx_file="$state_dir/ctx-$sid"
if [ ! -f "$ctx_file" ]; then exit 0; fi

pct=$(tr -cd '0-9' < "$ctx_file" 2>/dev/null || echo "")
if [ -z "$pct" ]; then exit 0; fi
if [ "$pct" -lt 80 ] 2>/dev/null; then exit 0; fi

echo "$pct" > "$marker" 2>/dev/null || true

reason="KONTEXT BEI ${pct}% MITTEN IM TURN (Schwelle 80%, grosse Komprimierung droht bei 100%). Dieser Hinweis kommt nur EINMAL pro Session. Ein einzelner langer Turn (z.B. Researcher-Schwarm) kann den Kontext schlagartig anheben. Wenn die aktuelle Aufgabe an einem sinnvollen Punkt steht (kein offener Multiple-Choice, keine Rueckfrage an den Benutzer offen): Schliesse den aktuellen Teilschritt ab und fuehre dann den Skill 'session' mit Unterbefehl 'backup' aus, zeige danach den Disketten-Marker (drei Disketten vorne, drei Ausrufezeichen hinten, je eine dicke Linie drueber und drunter) und anschliessend eine FETTE Warnung mit Warndreieck, dass der Benutzer JETZT eine neue Session starten muss (/clear, danach 'session restore'). Wenn gerade ein kritischer Schritt laeuft oder du auf eine Antwort des Benutzers wartest: erst fertig machen bzw. abwarten, das Backup direkt danach."

out=$(printf '%s' "$reason" | python3 -c 'import sys,json; print(json.dumps({"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":sys.stdin.read()}}))' 2>/dev/null || echo "")
if [ -n "$out" ]; then echo "$out"; fi

exit 0
