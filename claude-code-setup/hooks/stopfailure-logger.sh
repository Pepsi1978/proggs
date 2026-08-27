#!/usr/bin/env bash
# stopfailure-logger.sh — Logs API failures to whiteboard with rate limiting
# Triggered by StopFailure hook event (Claude Code v2.1.78+)
# Uses whiteboard-insert.sh for section-based writing (echo >> is FORBIDDEN!)
# Rate limit: max 1 whiteboard entry per hour to prevent spam

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

# Rate limiting: check if we already logged a StopFailure recently (60 min)
RATE_LIMIT_FILE="$TMPDIR/claude-stopfailure-last.txt"
if [ ! -d "$TMPDIR" ]; then
    RATE_LIMIT_FILE="/tmp/claude-stopfailure-last.txt"
fi

if [ -f "$RATE_LIMIT_FILE" ]; then
    last_write=$(stat -f "%m" "$RATE_LIMIT_FILE" 2>/dev/null || stat -c "%Y" "$RATE_LIMIT_FILE" 2>/dev/null)
    if [ -n "$last_write" ]; then
        now=$(date +%s)
        elapsed=$(( (now - last_write) / 60 ))
        if [ "$elapsed" -lt 60 ]; then
            hook_log "StopFailure rate-limited (last logged ${elapsed}min ago)"
            echo "StopFailure rate-limited (last logged ${elapsed}min ago)"
            exit 0
        fi
    fi
fi

# Read stdin for error details (Claude Code pipes error info)
ERROR_INPUT=""
if [ -t 0 ]; then
    # stdin is a terminal (not redirected) — no error details available
    ERROR_INPUT="No error details available (stdin empty)"
else
    ERROR_INPUT=$(cat 2>/dev/null)
    if [ -z "$(echo "$ERROR_INPUT" | tr -d '[:space:]')" ]; then
        ERROR_INPUT="No error details available (stdin empty)"
    fi
fi

# Truncate long error messages (max 500 chars)
if [ "${#ERROR_INPUT}" -gt 500 ]; then
    ERROR_INPUT="${ERROR_INPUT:0:500}... (truncated)"
fi

# Update rate limit marker
echo "$TIMESTAMP" > "$RATE_LIMIT_FILE" 2>/dev/null || true

# Fehlerart aus dem Event-JSON bestimmen (Direktive #3, eigener Vorfall 2026-08-27).
# Vorher trug JEDER Eintrag den Titel "API/Rate-Limit Error" und den Fix-Vorschlag
# "API-Key pruefen" — auch bei "authentication_failed". Ein Anmeldeproblem sah im
# Whiteboard dadurch wie ein Rate-Limit aus und schickte die Ursachensuche in die
# falsche Richtung. python3 statt jq: jq fehlt oft und liefert dann still leere
# Werte, der Hook versagte lautlos (bugs/claude-tooling/claude-hooks.md 13.2).
# Drei Zeilen -> drei Variablen; faellt python3 aus, greift der neutrale Fallback.
ART_DATEN=$(printf '%s' "$ERROR_INPUT" | python3 -c '
import json, sys
try:
    fehler = str(json.load(sys.stdin).get("error", "")).strip().lower()
except Exception:
    fehler = ""

if any(w in fehler for w in ("auth", "login", "credential", "unauthorized", "401")):
    titel = "Nicht angemeldet (%s)" % (fehler or "authentication_failed")
    status = "OFFEN (Anmeldung fehlt - kein Rate-Limit)"
    vorschlag = ("Der Konfigurationsordner dieser Sitzung hat keine gueltige Anmeldung. "
                 "Abgleich starten: python3 ~/proggs/OpenLauncher/Profiles/hooks/claude-login-sync.py "
                 "- Hintergrund: bugs/claude-tooling/claude-config.md 3.9.")
elif any(w in fehler for w in ("rate", "limit", "quota", "429")):
    titel = "API/Rate-Limit Error"
    status = "TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)"
    vorschlag = "Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen."
else:
    titel = "API-Fehler (%s)" % (fehler or "unbekannt")
    status = "TRANSIENT (externer API-Fehler, kein Harness-Bug)"
    vorschlag = "Fehlerart in den Details unten pruefen und passend einordnen."

print(titel)
print(status)
print(vorschlag)
' 2>/dev/null)
TITEL=$(printf '%s' "$ART_DATEN" | sed -n '1p')
STATUS=$(printf '%s' "$ART_DATEN" | sed -n '2p')
VORSCHLAG=$(printf '%s' "$ART_DATEN" | sed -n '3p')
# Fallback, falls python3 fehlt oder nichts lieferte — nie mit leeren Feldern schreiben.
[ -n "$TITEL" ]     || TITEL="API-Fehler (Art nicht bestimmbar)"
[ -n "$STATUS" ]    || STATUS="TRANSIENT (externer API-Fehler, kein Harness-Bug)"
[ -n "$VORSCHLAG" ] || VORSCHLAG="Fehlerart in den Details unten pruefen und passend einordnen."

# Build whiteboard entry
# 2026-05-30: Status TRANSIENT statt OFFEN — ein externer API-/Rate-Limit-Fehler ist kein
# reparierbarer Harness-Bug. So blaeht er die OFFEN-Liste nicht auf und der invariant-check
# zaehlt ihn nicht als ungeloesten Fehler. Ein Anmeldefehler ist dagegen sehr wohl
# reparierbar und bleibt deshalb bewusst OFFEN.
ENTRY=$(cat << ENTRY_EOF

### $TIMESTAMP — StopFailure: $TITEL — Status: $STATUS
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch einen API-Fehler
**Details:** $ERROR_INPUT
**Fix-Vorschlag:** $VORSCHLAG
**Status:** $STATUS
ENTRY_EOF
)

# Use whiteboard-insert.sh (already sourced above)
WHITEBOARD="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
if [ -f "$WHITEBOARD" ]; then
    insert_whiteboard_entry "Offene Fehler & Probleme" "$ENTRY" || \
        echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required."
else
    echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required."
fi

echo "StopFailure logged to whiteboard at $TIMESTAMP"

exit 0
