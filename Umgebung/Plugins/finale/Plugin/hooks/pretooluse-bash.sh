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

# set -eu: -e laesst Fehler propagieren, -u faengt unbound variables.
# Hardening FIN-029 (2026-05-21) — vorher nur set -u.
set -eu

# Dependency-Check (Wave 3 Hardening, Direktive #3 — Loop 2 K2):
# python3 ist Pflicht-Tool fuer JSON-Parsing. Wenn python3 nicht im PATH ist,
# kann der Hook den JSON-Input nicht lesen UND damit destruktive Befehle nicht
# erkennen. Ein blockierender Hook MUSS in diesem Fall FAIL-CLOSED (exit 2),
# nicht FAIL-OPEN (exit 0 durchlassen) — sonst kein Schutz auf python3-losen Systemen.
if ! command -v python3 >/dev/null 2>&1; then
  echo "[finale] BLOCKIERT: python3 nicht im PATH — Hook kann JSON-Input nicht parsen." >&2
  echo "[finale] Bitte python3 installieren (Linux: apt install python3 / macOS: brew install python / Windows: python.org Installer)." >&2
  echo "[finale] Bis dahin werden ALLE Bash-Befehle blockiert (Fail-Closed Sicherheits-Default)." >&2
  exit 2
fi

# Hook-Input einlesen (Claude Code schickt JSON mit { tool_input: { command: "..." } }).
# DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin akzeptieren. Bei
# groesserem Input wuerde `cat` den Speicher fluten — head -c begrenzt hart.
stdin_input="$(head -c 524288 2>/dev/null || true)"
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
# PATH_PREFIX matcht optionalen Pfad-Anteil VOR dem Dateinamen — bewusst nur Token
# die in `/` enden (= echte Pfad-Trenner). Verhindert dass `xstrings.xml` durch
# Substring-Match getroffen wird.
PATH_PREFIX='([^[:space:]|&;]*/)?'
# RBOUND fordert dass nach dem Dateinamen ein klarer Token-Ende-Marker kommt:
# Whitespace, ;, |, &, oder Ende. KEIN `\b` weil das vor `.bak`/.tmp` etc. matched
# und damit `strings.xml.bak` faelschlich blockiert wurde (Debug-Loop 2 Wave 8 2026-05-21).
RBOUND='([[:space:]]|;|\||&|$)'

block_reason=""

# Pattern 1 — rm -rf auf .android-shield/ oder res/
if printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\.android-shield(/|\b)'; then
  block_reason="rm -rf auf .android-shield/ — das wuerde alle Plugin-Reports und das audit-log loeschen"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres/values'; then
  block_reason="rm -rf auf res/values — das wuerde Lokalisierungs-Dateien zerstoeren"
elif printf '%s' "$cmd" | grep -E -q 'rm[[:space:]]+-r[fF][[:space:]]+.*\bres(/|\b)'; then
  block_reason="rm -rf auf res/ — das wuerde alle App-Ressourcen zerstoeren"
fi

# Pattern 2 — Shell-Umleitung (> oder >>) in geschuetzte Datei.
# Wave 8 Fix (2026-05-21): vorher matched `> strings.xml.bak` (\b vor `.`) und
# `> xstrings.xml` (Substring-Match). Jetzt mit PATH_PREFIX + RBOUND exakt.
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q ">>?[[:space:]]+${PATH_PREFIX}${PROTECTED}${RBOUND}"; then
    block_reason="Shell-Umleitung in eine geschuetzte Datei (strings.xml/AndroidManifest.xml/build.gradle) — destructiver Komplett-Overwrite. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 3 — tee (mit oder ohne -a) in geschuetzte Datei
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "tee[[:space:]]+(-a[[:space:]]+)?${PATH_PREFIX}${PROTECTED}${RBOUND}"; then
    block_reason="tee in eine geschuetzte Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 4 — cp / mv mit geschuetzter Ziel-Datei (letztes Argument)
# Wave 8 Fix: gleiche Boundary-Logik wie Pattern 2 — verhindert false-positive
# bei `cp foo xstrings.xml` oder `cp foo strings.xml.bak`.
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "(^|[[:space:];|&])(cp|mv)[[:space:]]+[^|&;]+[[:space:]]${PATH_PREFIX}${PROTECTED}${RBOUND}"; then
    block_reason="cp/mv mit geschuetzter Ziel-Datei — destructiver Overwrite. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 5 — dd of= mit geschuetzter Datei
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q "dd[[:space:]].*of=${PATH_PREFIX}${PROTECTED}${RBOUND}"; then
    block_reason="dd of= mit geschuetzter Ziel-Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
  fi
fi

# Pattern 6 — find ... -delete im Plugin-Output
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q 'find[[:space:]]+.*\.android-shield.*-delete'; then
    block_reason="find -delete auf .android-shield/ — destructiv. Audit-Log gehoert append-only."
  fi
fi

# Pattern 7 — Scripting-Sprache mit Code-Argument (-c/-e/--eval) UND geschuetztem
# Dateinamen im Befehl. Schliesst Bypass via `python3 -c "open('strings.xml','w')..."`,
# `node -e "...writeFileSync..."`, `perl -e "open('>strings.xml')..."` etc.
# Konservativ: false positives akzeptabel (Read-Only-Skripte werden auch blockiert);
# false negatives waeren das ernstere Risiko. Hardening C1 2026-05-21 (Direktive #3).
if [ -z "$block_reason" ]; then
  if printf '%s' "$cmd" | grep -E -q '(^|[[:space:];|&])(python3?|node|nodejs|perl|ruby|sh|pwsh|bash)[[:space:]]+(-[cei]|--eval|--exec|--command)' && \
     printf '%s' "$cmd" | grep -E -q "$PROTECTED"; then
    block_reason="Scripting-Sprache (python/node/perl/etc.) mit Code-Argument das eine geschuetzte Datei referenziert. Auch indirekte Schreibversuche via Scripting werden blockiert. Nutze Edit/Write ueber den fix-applier."
  fi
fi

if [ -n "$block_reason" ]; then
  echo "[finale] BLOCKIERT: $block_reason" >&2
  echo "[finale] Befehl: $cmd" >&2
  echo "[finale] Wenn du das wirklich willst, fuehre den Befehl ausserhalb des Plugin-Scopes aus." >&2
  exit 2
fi

exit 0
