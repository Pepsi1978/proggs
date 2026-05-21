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

# Dependency-Check (Wave 3 Hardening, Direktive #3 — Loop 2 K2):
# python3 ist Pflicht-Tool fuer JSON-Parsing. Wenn fehlend: blockierender Hook
# MUSS FAIL-CLOSED (exit 2) — sonst koennen alle Schreibversuche an App-Dateien
# im audit-only-Modus durchgehen.
if ! command -v python3 >/dev/null 2>&1; then
  echo "[finale] BLOCKIERT: python3 nicht im PATH — Hook kann JSON-Input nicht parsen." >&2
  echo "[finale] Bitte python3 installieren (Linux: apt install python3 / macOS: brew install python / Windows: python.org Installer)." >&2
  echo "[finale] Bis dahin werden ALLE Edit/Write/MultiEdit-Versuche blockiert (Fail-Closed Sicherheits-Default)." >&2
  exit 2
fi

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
    # 4-Stufen-Fallback: realpath (Linux/Git-Bash) -> readlink -f (Linux/Git-Bash) ->
    # python3 (macOS BSD ohne coreutils, Wave 3 Hardening 2026-05-21) -> echo
    # Letzter Fallback ist UNSICHER (gibt unkanonischen Pfad zurueck), aber besser
    # als Abbruch — auf macOS ohne realpath/readlink -f braucht python3 als Brueckentechnik.
    real_path="$(realpath "$file_path" 2>/dev/null \
      || readlink -f "$file_path" 2>/dev/null \
      || python3 -c "import os,sys; print(os.path.realpath(sys.argv[1]))" "$file_path" 2>/dev/null \
      || echo "$file_path")"
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

# Stale-Lock-Check (Wave 6 Hardening 2026-05-21 — Lock-Format-Mismatch behoben):
# Frueher (Wave 3): Check auf orchestratorPid + kill -0. Wave 5 hat das Lock-Format
# in orchestrator.md auf sessionToken umgestellt (LLM-Agenten haben keine stabile
# OS-PID) — Hooks lasen aber weiter orchestratorPid -> Stale-Check war tot ->
# Locks blieben nach Crash fuer immer aktiv.
# Wave 6: nur Timestamp-basierter Stale-Check (kein PID-Check mehr).
# Plus: Negative lock_age durch NTP-Korrektur/VM-Resume auf 0 clippen.
now="$(date +%s 2>/dev/null || echo 0)"
lock_mtime="$(stat -c %Y "$lock_found" 2>/dev/null || stat -f %m "$lock_found" 2>/dev/null || echo 0)"
lock_age=$((now - lock_mtime))
# Negative lock_age (NTP-Korrektur, VM-Resume kann Zeit zurueckdrehen) auf 0 clippen
[ "$lock_age" -lt 0 ] && lock_age=0
# Stale-Schwelle 1800 Sek = 30 Min (passt zu typischen audit-Laeufen bis 25 Min)
if [ "$lock_age" -gt 1800 ]; then
  echo "[finale] WARNUNG: Stale-Lock erkannt ($lock_age Sek alt, ueber 30-Min-Schwelle)." >&2
  echo "[finale] Vorheriger Orchestrator-Lauf wahrscheinlich gecrasht." >&2
  echo "[finale] Lock wird ignoriert. Manuell loeschen: rm \"$lock_found\"" >&2
  exit 0
fi

# Lock aktiv UND Datei nicht in .android-shield/ -> blockieren
echo "[finale] BLOCKIERT: Audit-Only-Modus aktiv (Lock: $lock_found)" >&2
echo "[finale] Datei: $file_path" >&2
echo "[finale] Im Audit-Only-Modus duerfen nur Dateien unter .android-shield/ beschrieben werden." >&2
echo "[finale] Fuer Fixes: /finale:fix-only oder /finale:run starten — dort wird der Lock nicht gesetzt." >&2
exit 2
