#!/usr/bin/env bash
# audit-only-write-guard.sh
#
# PreToolUse-Hook fuer Edit|Write|MultiEdit. Blockiert Schreibversuche an
# App-Dateien wenn ein Audit-Only-Lauf aktiv ist. Aktiv = Lock-Datei
# `<app-root>/.android-shield/.audit-only.lock` existiert in einem
# Vorfahren-Verzeichnis des betroffenen Pfads.
#
# Schliesst die Sicherheitsluecke aus dem Validierungsbericht 2026-05-21:
# Vorher behauptete commands/audit-only.md dass ein Hook namens
# `block-app-writes-in-audit-only` Schreibversuche erzwingt — der Hook
# existierte aber nicht. Jetzt existiert er.
#
# Verhalten:
# - Schreibversuch an App-Datei + Lock aktiv -> Exit 2 (block)
# - Schreibversuch innerhalb .android-shield/ -> immer OK (Plugin-Output erlaubt)
# - Schreibversuch ohne Lock -> immer OK
# - Fehler im Hook -> Exit 0 (Session brechen ist schlimmer als Schutz aus)

# set -eu: -e laesst Fehler propagieren (statt still weiterzulaufen),
# -u faengt unbound variables. Die if/case/while-Strukturen unten deaktivieren
# -e im Bedingungs-Kontext automatisch, daher kein Konflikt mit der
# fail-safe-Logik (exit 0 am Ende). FIN-029 Hardening 2026-05-21.
set -eu

# Input-Guard: leerer stdin -> still durchwinken.
# DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin.
stdin_input="$(head -c 524288 2>/dev/null || true)"
if [ -z "$stdin_input" ]; then
  exit 0
fi

# Datei-Pfad aus dem Hook-Input extrahieren (Claude Code Hook-Konvention)
file_path="$(printf '%s' "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {}) or {}
    fp = ti.get('file_path') or ti.get('path') or ''
    print(fp)
except Exception:
    print('')
" 2>/dev/null || true)"

if [ -z "$file_path" ]; then
  exit 0
fi

# Path-Traversal-Schutz (C2 2026-05-21 Hardening): wenn der Pfad ../ oder ..\
# enthaelt, kanonisieren BEVOR der .android-shield/-Whitelist-Check greift.
# Verhindert Bypass wie /app/../.android-shield/../res/values/strings.xml
# (matched die Whitelist als Substring, ist aber tatsaechlich ein Schreibversuch
# in res/). Normaler Pfad ohne ../ wird nicht kanonisiert (Performance).
case "$file_path" in
  *../*|*..[\\/]*)
    real_path="$(realpath "$file_path" 2>/dev/null || readlink -f "$file_path" 2>/dev/null || echo "$file_path")"
    ;;
  *)
    real_path="$file_path"
    ;;
esac

# Schreiben innerhalb .android-shield/ ist immer erlaubt (Plugin-Output-Domain).
# Match auf KANONISIERTEM Pfad (real_path) — sonst Traversal-Bypass moeglich.
case "$real_path" in
  */.android-shield/*) exit 0 ;;
esac

# Suche aufwaerts nach Audit-Lock — von der betroffenen Datei zum Filesystem-Root.
# Nutzt den kanonisierten Pfad damit Traversal-Tricks die Lock-Suche nicht umgehen.
dir="$(dirname "$real_path")"
lock_found=""
depth=0
max_depth=20   # Schutz gegen Endlosschleife bei seltsamen Pfaden

while [ -n "$dir" ] && [ "$dir" != "/" ] && [ "$dir" != "." ] && [ $depth -lt $max_depth ]; do
  if [ -f "$dir/.android-shield/.audit-only.lock" ]; then
    lock_found="$dir/.android-shield/.audit-only.lock"
    break
  fi
  parent="$(dirname "$dir")"
  if [ "$parent" = "$dir" ]; then
    break   # Root erreicht
  fi
  dir="$parent"
  depth=$((depth + 1))
done

if [ -z "$lock_found" ]; then
  exit 0   # Kein Audit-Only-Lauf aktiv
fi

# Lock aktiv UND Datei nicht in .android-shield/ -> blockieren
echo "[finale] BLOCKIERT: Audit-Only-Modus aktiv (Lock: $lock_found)" >&2
echo "[finale] Datei: $file_path" >&2
echo "[finale] Im Audit-Only-Modus duerfen nur Dateien unter .android-shield/ beschrieben werden." >&2
echo "[finale] Fuer Fixes: /finale:fix-only oder /finale:run starten — dort wird der Lock nicht gesetzt." >&2
exit 2
