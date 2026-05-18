#!/usr/bin/env bash
# session-start.sh
#
# Wird bei jedem Session-Start des finale-Plugins ausgefuehrt.
# Prueft NICHT-BLOCKIEREND ob die vier Skill-Symlinks aufloesbar sind und
# gibt eine kurze Diagnose nach stderr. Diese Diagnose erscheint im
# Session-Start-Bereich von Claude Code als Info-Block.
#
# Verhalten:
# - Tote Symlinks -> Warnung + Reparatur-Hinweis, aber Exit 0 (nicht blockieren)
# - Alle OK -> stille Erfolgsmeldung
# - Skript-Fehler -> stille Schluck-Logik, niemals die Session blockieren

set -u

PLUGIN_ROOT="${CLAUDE_PLUGIN_ROOT:-}"

if [ -z "$PLUGIN_ROOT" ] || [ ! -d "$PLUGIN_ROOT" ]; then
  # Kein Plugin-Root gesetzt -> wir koennen nichts pruefen, einfach durchwinken.
  exit 0
fi

VERIFY="$PLUGIN_ROOT/scripts/verify-skills.sh"
if [ ! -x "$VERIFY" ] && [ ! -r "$VERIFY" ]; then
  echo "[finale] Hinweis: verify-skills.sh nicht ausfuehrbar oder fehlt. Plugin koennte beschaedigt sein." >&2
  exit 0
fi

# Wir wollen nicht den Phase-0-Pre-Flight-Plan im SessionStart ausloesen — nur eine
# Kurzpruefung. Das Skript laeuft daher mit stdout->/dev/null, stderr wird
# auf eine 1-Zeilen-Zusammenfassung reduziert.
TMP_DIR="${TMPDIR:-/tmp}"
ERR_FILE="$TMP_DIR/.android-shield-verify-$$.err"

if bash "$VERIFY" "$PLUGIN_ROOT" >/dev/null 2>"$ERR_FILE"; then
  echo "[finale] Alle vier Skill-Symlinks OK." >&2
else
  echo "[finale] WARNUNG: mindestens ein Skill-Symlink ist defekt." >&2
  echo "[finale] Vor dem naechsten /finale:run reparieren. Details:" >&2
  # Zeige bis zu 10 Zeilen aus dem Stderr-Log
  if [ -s "$ERR_FILE" ]; then
    sed -n '1,10p' "$ERR_FILE" >&2
  fi
fi
rm -f "$ERR_FILE" 2>/dev/null || true

exit 0
