#!/usr/bin/env bash
# pretooluse-bash.sh
#
# Blockiert destruktive Bash-Commands die das Plugin-Output-Verzeichnis
# (.android-shield/) oder Android-Ressourcen-Dateien (strings.xml,
# AndroidManifest.xml, build.gradle.kts) gefaehrden wuerden.
#
# Verbessert vom 2026-05-21 (Direktive #3, Task 6):
# - Pattern-Liste erweitert: deckt jetzt auch cat>, tee (mit -a), cp/mv mit
#   geschuetzter Ziel-Datei und dd of= ab
# - Geschuetzte Dateien jetzt: strings.xml, AndroidManifest.xml, build.gradle,
#   build.gradle.kts (vorher nur strings.xml)
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

# Bash-Command aus JSON extrahieren
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

# Geschuetzte Dateinamen als Regex-Alternativ-Pattern.
# build.gradle.kts steht VOR build.gradle damit der laengere Match zuerst greift.
PROTECTED='(strings\.xml|AndroidManifest\.xml|build\.gradle\.kts|build\.gradle)'

block_reason=""

# Pattern 1 — rm -rf auf .android-shield/ oder res/
if printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\.android-shield(/|\b)'; then
  block_reason="rm -rf auf .android-shield/ — das wuerde alle Plugin-Reports und das audit-log loeschen"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres/values'; then
  block_reason="rm -rf auf res/values — das wuerde Lokalisierungs-Dateien zerstoeren"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres(/|\b)'; then
  block_reason="rm -rf auf res/ — das wuerde alle App-Ressourcen zerstoeren"
fi

# Pattern 2 — Shell-Umleitung (> oder >>) in geschuetzte Datei
# Deckt ab: echo > X, cat > X, python -c "..." > X, irgendwas >> X, etc.
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q ">>?[[:space:]]*[^|&]*$PROTECTED(\b|$)"; then
    block_reason="Shell-Umleitung in eine geschuetzte Datei (strings.xml/AndroidManifest.xml/build.gradle) — destructiver Komplett-Overwrite. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 3 — tee (mit oder ohne -a) in geschuetzte Datei
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "tee[[:space:]]+(-a[[:space:]]+)?[^|&]*$PROTECTED(\b|$)"; then
    block_reason="tee in eine geschuetzte Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 4 — cp / mv mit geschuetzter Ziel-Datei (letztes Argument)
# Heuristik: erkennt wenn der Befehl mit cp/mv anfaengt UND mit einer geschuetzten Datei endet
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "(^|[[:space:];|&])(cp|mv)[[:space:]]+[^|&;]+[[:space:]][^|&;[:space:]]*$PROTECTED([[:space:];|&]|$)"; then
    block_reason="cp/mv mit geschuetzter Ziel-Datei — destructiver Overwrite. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 5 — dd of= mit geschuetzter Datei
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "dd[[:space:]].*of=[^[:space:]]*$PROTECTED(\b|$)"; then
    block_reason="dd of= mit geschuetzter Ziel-Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 6 — find ... -delete im Plugin-Output
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q 'find[[:space:]]+.*\.android-shield.*-delete'; then
    block_reason="find -delete auf .android-shield/ — destructiv. Audit-Log gehoert append-only."
  fi
fi

if [ -n "$block_reason" ]; then
  echo "[finale] BLOCKIERT: $block_reason" >&2
  echo "[finale] Befehl: $cmd" >&2
  echo "[finale] Wenn du das wirklich willst, fuehre den Befehl ausserhalb des Plugin-Scopes aus." >&2
  exit 2
fi

exit 0
