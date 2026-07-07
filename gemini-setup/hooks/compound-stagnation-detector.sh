#!/usr/bin/env bash
# compound-stagnation-detector.sh — SessionStart Hook
# Event: SessionStart
# Type: command
# Platform: macOS/Linux
#
# Zweck: Warnt bei >14 Tagen ohne neuen Compound Effect.
# Quelle: /self-improve v5.19 Stufe 4 (2026-05-10), Linse "Eleganz" + R6 Verifiable-Outcome-Gate.
# Hintergrund: Compound Effect alle 7-10 Tage ist gesund. Pause >14 Tage = strukturelles Problem
# (z.B. Detektor kaputt, Trigger fehlt). Frank merkt Stagnation nicht — Hook macht sie sichtbar.
#
# Defense in Depth:
# - Jeder Fehler non-fatal, Hook beendet immer mit exit 0
# - Liest nur, schreibt nichts (keine Side-Effects)
# - Ergaenzt invariant-check.sh (der prueft Stale-OFFEN; dieser prueft Stagnation)

set +e

forschung="$HOME/proggs/Forschung.md"
[ ! -f "$forschung" ] && exit 0

# Compound Effect Erfolge Sektion isolieren
section=$(awk '/## Compound Effect Erfolge/{flag=1; next} flag' "$forschung" 2>/dev/null)
[ -z "$section" ] && exit 0

# Letztes Datum suchen
latest_date=$(echo "$section" | grep -oE '###[[:space:]]*\[[0-9]{4}-[0-9]{2}-[0-9]{2}\]' | tail -1 | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}')
[ -z "$latest_date" ] && exit 0

# Tage berechnen (Cross-Platform: macOS BSD date vs. Linux GNU date)
if [ "$(uname)" = "Darwin" ]; then
    latest_ts=$(date -j -f "%Y-%m-%d" "$latest_date" "+%s" 2>/dev/null)
else
    latest_ts=$(date -d "$latest_date" "+%s" 2>/dev/null)
fi
[ -z "$latest_ts" ] && exit 0

now_ts=$(date "+%s")
days_since=$(( (now_ts - latest_ts) / 86400 ))

if [ "$days_since" -gt 14 ]; then
    cat <<EOF
{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"COMPOUND-STAGNATION: Letzter dokumentierter Compound Effect ist $days_since Tage her ($latest_date). Schwelle: 14 Tage. Direktive #2 (Selbstbeobachtung) sagt: Stagnation = strukturelles Problem. Empfehlung: /self-improve in dieser Session triggern, oder dokumentieren WARUM keine Verbesserung ablief."}}
EOF
fi

exit 0
