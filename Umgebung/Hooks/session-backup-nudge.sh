#!/usr/bin/env bash
# session-backup-nudge: Stupst Claude bei >=88% Kontext einmal pro Session an,
#   ein 'session backup' zu machen, BEVOR die grosse Komprimierung (100%) zuschlaegt.
# Runs as Stop hook.
# stdout -> JSON {"decision":"block","reason":...} (reason geht als Anweisung an Claude)
# Platform: macOS/Linux
#
# Der exakte Kontext-Prozentsatz kommt NICHT im Stop-Input — er wird von der
# Statusline (statusline.sh/.ps1) pro Session nach ~/.claude/state/ctx-<sid> geschrieben.
# Schleifenschutz: (1) stop_hook_active==true -> raus; (2) Nudge-Marker pro Session.
#
# Bewusst KEIN 'set -e': die Guard-Logik nutzt viele Bedingungen, deren non-zero
# Exit unter set -e das Skript faelschlich beenden wuerde. Stattdessen if-Guards
# und garantiertes exit 0 am Ende.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
trap 'hook_log_warn "session-backup-nudge: error at line $LINENO" 2>/dev/null || true' ERR

stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi

# Schleifenschutz 1 + session_id in EINEM jq-Aufruf (Performance: 1 jq statt 2 spart auf
# Windows ~50-80ms pro Stop). jq gibt beide Werte als ZWEI ZEILEN aus, sicher per Newline
# getrennt. Ein leerer join-Trenner klebte die Werte zusammen und machte sid leer; mit
# Newline-Trennung kann das nicht passieren. Faellt jq aus, bleiben beide leer -> exit 0.
# Schleifenschutz 1: Claude macht bereits aufgrund eines Stop-Hooks weiter.
# Bewusst 2 jq via $() statt read: Command-Substitution entfernt das trailing CR der
# Windows-jq-Ausgabe automatisch; read wuerde das CR behalten und die sid-Validierung brechen.
# (Performance-Optimierung "1 jq statt 2" verworfen: marginal ~50ms, aber CR-fehleranfaellig.)
stop_active=$(printf '%s' "$stdin_input" | jq -r '.stop_hook_active // false' 2>/dev/null || echo "false")
if [ "$stop_active" = "true" ]; then exit 0; fi

sid=$(printf '%s' "$stdin_input" | jq -r '.session_id // empty' 2>/dev/null || echo "")
case "$sid" in
    ''|*[!a-zA-Z0-9_-]*) exit 0 ;;
esac

state_dir="$HOME/.claude/state"
ctx_file="$state_dir/ctx-$sid"
if [ ! -f "$ctx_file" ]; then exit 0; fi

pct=$(tr -cd '0-9' < "$ctx_file" 2>/dev/null || echo "")
if [ -z "$pct" ]; then exit 0; fi
if [ "$pct" -lt 88 ] 2>/dev/null; then exit 0; fi

# Schleifenschutz 2: nur EINMAL pro Session anstupsen
marker="$state_dir/ctx-nudged-$sid"
if [ -f "$marker" ]; then exit 0; fi
echo "$pct" > "$marker" 2>/dev/null || true

reason="KONTEXT BEI ${pct}% (Schwelle 88%, grosse Komprimierung droht bei 100%). Dieser Hinweis kommt nur EINMAL pro Session. Wenn die aktuelle Aufgabe VOLLSTAENDIG abgeschlossen ist (kein offener Multiple-Choice, keine Rueckfrage an den Benutzer offen): Fuehre JETZT den Skill 'session' mit Unterbefehl 'backup' aus, zeige danach den Disketten-Marker (drei Disketten vorne, drei Ausrufezeichen hinten, je eine dicke Linie drueber und drunter) und anschliessend eine FETTE Warnung mit Warndreieck, dass der Benutzer JETZT eine neue Session starten muss (/clear, danach 'session restore'). Wenn die Aufgabe noch laeuft oder du auf eine Antwort des Benutzers wartest: erst fertig machen bzw. abwarten, das Backup danach."

out=$(jq -nc --arg r "$reason" '{decision:"block", reason:$r}' 2>/dev/null || echo "")
if [ -n "$out" ]; then echo "$out"; fi

exit 0
