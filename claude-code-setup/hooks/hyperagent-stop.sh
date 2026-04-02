#!/bin/bash
# Hyperagent Stop-Hook — Metacognitive Analysis Prompt Injection
# Event: Stop (fires when Claude finishes a response)
# Type: prompt (injects text into Claude's context)
# Platform: macOS/Linux
#
# PURPOSE: Reminds Claude to perform metacognitive analysis before completing.
# This is the "heartbeat" of the Hyperagent system — it fires on every Stop event.
#
# ROBUSTNESS: Non-critical hook. Any failure → exit 0 with empty output.
# An empty output means no prompt injection — Claude continues normally.

set +e  # Don't exit on error — this hook must always exit 0

# Read turn counter
COUNTER_FILE="${TMPDIR:-/tmp}/claude-turn-counter.txt"
turn=0
if [ -f "$COUNTER_FILE" ]; then
    turn=$(cat "$COUNTER_FILE" 2>/dev/null | tr -d '[:space:]')
    turn=${turn:-0}
    # Validate: must be numeric, otherwise reset to 0
    case "$turn" in
        ''|*[!0-9]*) turn=0 ;;
    esac
fi

# Skip for trivial sessions (<5 turns)
if [ "$turn" -lt 5 ] 2>/dev/null; then
    exit 0
fi

# Read session goal
GOAL_FILE="${TMPDIR:-/tmp}/claude-session-goal.txt"
goal=""
if [ -f "$GOAL_FILE" ]; then
    goal=$(cat "$GOAL_FILE" 2>/dev/null | head -c 300)

    # Check if goal is stale (older than 2 hours)
    if [ "$(uname)" = "Darwin" ]; then
        goal_age=$(( $(date +%s) - $(stat -f %m "$GOAL_FILE" 2>/dev/null || echo 0) ))
    else
        goal_age=$(( $(date +%s) - $(stat -c %Y "$GOAL_FILE" 2>/dev/null || echo 0) ))
    fi
    if [ "$goal_age" -gt 7200 ] 2>/dev/null; then
        goal=""  # Goal is stale — clear it but DON'T skip the hook
    fi
fi

# Count today's hook errors
LOG_DATE=$(date +%Y-%m-%d)
LOG_FILE="$HOME/.claude/logs/hooks/$LOG_DATE.log"
error_count=0
if [ -f "$LOG_FILE" ]; then
    # Only count errors from last 2 hours (current session), not entire day
    cutoff=$(date -v-2H +%H:%M:%S 2>/dev/null || date -d "2 hours ago" +%H:%M:%S 2>/dev/null || echo "00:00:00")
    error_count=$(awk -v cutoff="$cutoff" '/^\[([0-9]{2}:[0-9]{2}:[0-9]{2})\].*ERROR|FAIL/ { match($0, /\[([0-9]{2}:[0-9]{2}:[0-9]{2})\]/, t); if (t[1] >= cutoff) c++ } END { print c+0 }' "$LOG_FILE" 2>/dev/null || echo 0)
fi

# Determine analysis depth
depth="leicht"
if [ "$turn" -gt 12 ] 2>/dev/null || [ "$error_count" -gt 2 ] 2>/dev/null; then
    depth="voll"
fi

# Build and output the metacognitive prompt
cat <<PROMPT
[METACOGNITIVE MONITORING — Hyperagent-Pattern]
Session-Turns: $turn | Hook-Errors: $error_count | Analyse-Tiefe: $depth
PROMPT

if [ -n "$goal" ]; then
    echo "Urspruengliches Ziel: $goal"
fi

cat <<'PROMPT'

Bevor du diese Antwort abschliesst, fuehre eine kurze metacognitive Analyse durch:
1. **Intent-Check**: Habe ich das urspruengliche Ziel des Benutzers erreicht?
2. **Retry-Check**: Gab es unnoetige Wiederholungen (>2x gleicher Versuch)?
3. **Korrektur-Check**: Hat der Benutzer mich korrigiert? Falls ja: Sollte das eine Regel werden?
4. **Lern-Check**: Habe ich etwas Neues gelernt das persistiert werden sollte?

Fasse die Ergebnisse als Intelligenz-Vorschlaege am Ende deiner Antwort zusammen.
Bei Analyse-Tiefe 'voll': Erwaege den Hyperagent-Sub-Agent zu spawnen fuer tiefere Analyse.
PROMPT

exit 0
