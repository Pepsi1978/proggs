#!/usr/bin/env bash
# pretooluse-bash.sh
#
# Blockiert destruktive Bash-Commands die das Plugin-Output-Verzeichnis
# (.android-shield/) oder den Android-Ressourcen-Ordner (res/) gefaehrden
# wuerden. Lest die Bash-Command-Eingabe aus stdin (Claude Code Hook-Konvention)
# und entscheidet ob der Befehl durchgelassen oder blockiert wird.
#
# Verhalten:
# - Gefaehrlicher Befehl -> Exit 2 (Block), klare Begruendung auf stderr
# - Harmloser Befehl -> Exit 0, keine Ausgabe

set -u

# Hook-Input einlesen (Claude Code schickt JSON mit { tool_input: { command: "..." } })
stdin_input="$(cat 2>/dev/null || true)"
if [ -z "$stdin_input" ]; then
  exit 0
fi

# Robust extrahieren ohne jq-Pflicht. Wir suchen "command":"..." im JSON-Blob.
# Bei Fehler stillschweigend durchlassen — wir wollen nichts kaputtmachen.
cmd="$(printf '%s' "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {}) or {}
    c = ti.get('command', '') or ''
    print(c)
except Exception:
    print('')
" 2>/dev/null || true)"

if [ -z "$cmd" ]; then
  exit 0
fi

# Patterns die destruktiv und gefaehrlich sind in unserem Scope.
# Beachte: wir greifen NUR wenn der Pfad .android-shield/ oder res/values*/strings.xml
# (oder ein leerer/absoluter rm -rf auf das App-Root) betroffen ist.

block_reason=""

# 1. rm -rf auf .android-shield/ oder res/
if printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\.android-shield(/|\b)'; then
  block_reason="rm -rf auf .android-shield/ — das wuerde alle Plugin-Reports und das audit-log loeschen"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres/values'; then
  block_reason="rm -rf auf res/values — das wuerde Lokalisierungs-Dateien zerstoeren"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres(/|\b)'; then
  block_reason="rm -rf auf res/ — das wuerde alle App-Ressourcen zerstoeren"
fi

# 2. > strings.xml (Umleitung in eine strings.xml = komplettes Ueberschreiben)
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q '>[[:space:]]*[^|&]*strings\.xml(\b|$)'; then
    block_reason="Direkte Shell-Umleitung in eine strings.xml — destructiver Komplett-Overwrite. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# 3. find ... -delete im Plugin-Output
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q 'find[[:space:]]+.*\.android-shield.*-delete'; then
    block_reason="find -delete auf .android-shield/ — destructiv. Audit-Log gehoert append-only."
  fi
fi

# 4. git reset --hard / git clean -fdx im App-Root waehrend Plugin laeuft
#    Wir koennen den Kontext nicht zuverlaessig erkennen — daher nur warnen, nicht blocken.
#    (Auskommentiert, um keine generischen Workflows zu stoeren.)

if [ -n "$block_reason" ]; then
  echo "[finale] BLOCKIERT: $block_reason" >&2
  echo "[finale] Befehl: $cmd" >&2
  echo "[finale] Wenn du das wirklich willst, fuehre den Befehl ausserhalb des Plugin-Scopes aus." >&2
  exit 2
fi

exit 0
