#!/usr/bin/env bash
# harness-mirror.sh — Spiegel-Werkzeug fuer die Claude-Code-Harness
#
# Vergleicht (check) oder synchronisiert (sync) die AKTIVEN eigenen Komponenten
# in ~/.claude/ gegen die beiden Repo-Spiegelorte:
#   1) ~/proggs/claude-code-setup/  (granularer Cross-Platform-Spiegel)
#   2) ~/proggs/Umgebung/           (transportable Komplett-Umgebung)
#
# Umgesetzt fuer die Regel ~/.claude/rules/harness-mirror-on-change.md.
# Bewusst KEINE Observability-Schicht (JSON-Logging/Sonden): reines Diff-/Kopier-
# Hilfsskript mit klarer Konsolenausgabe — volle Sonden waeren Over-Engineering.
#
# Nutzung:
#   bash harness-mirror.sh check    # nur anzeigen, was fehlt/veraltet/extra ist (Default)
#   bash harness-mirror.sh sync     # aktiven Stand 1:1 in beide Spiegel kopieren (additiv)
#
# Hinweise:
#   - __pycache__ und *.pyc werden immer ausgeschlossen; der leere Skill-Ordner
#     "learned" wird ignoriert.
#   - sync ist ADDITIV (kopiert/aktualisiert, loescht NICHTS). EXTRA-Dateien im
#     Spiegel werden im check-Modus gemeldet und sind bewusst manuell zu entscheiden.
#   - Plugins (eigene vs. externe) werden NICHT automatisch verglichen — am Ende nur
#     ein Hinweis, weil ~/.claude/plugins/ ueberwiegend FREMDE Plugins enthaelt.

set -u

MODE="${1:-check}"
PROGGS="${HOME}/proggs"
CLAUDE_HOME="${HOME}/.claude"
SETUP="${PROGGS}/claude-code-setup"
UMGEBUNG="${PROGGS}/Umgebung"

# label | aktiv | spiegel-1 (claude-code-setup) | spiegel-2 (Umgebung, leer = keiner)
COMPONENTS=(
  "skills|${CLAUDE_HOME}/skills|${SETUP}/skills|${UMGEBUNG}/Skills"
  "hooks|${CLAUDE_HOME}/hooks|${SETUP}/hooks|${UMGEBUNG}/Hooks"
  "agents|${CLAUDE_HOME}/agents|${SETUP}/agents|"
  "commands|${CLAUDE_HOME}/commands|${SETUP}/commands|"
  "rules|${CLAUDE_HOME}/rules|${SETUP}/rules|"
)

EXCLUDES=(-x '__pycache__' -x '*.pyc' -x 'learned' -x '*.bak' -x '*.reset-bak-*')
total_missing=0
total_stale=0
total_extra=0

compare_dir() {
  # $1 aktiv, $2 spiegel, $3 anzeige-label
  local active="$1" mirror="$2" what="$3"
  if [ ! -d "$active" ]; then
    printf '   (aktiv fehlt: %s — uebersprungen)\n' "$active"
    return
  fi
  if [ ! -d "$mirror" ]; then
    printf '   \033[33mSPIEGEL FEHLT KOMPLETT:\033[0m %s\n' "$mirror"
    total_missing=$((total_missing + 1))
    return
  fi
  local line miss=0 stale=0 extra=0
  while IFS= read -r line; do
    case "$line" in
      "Only in ${active}"*|"Only in ${active}/"*)
        printf '   \033[31mFEHLT  \033[0m %s\n' "${line#Only in }"; miss=$((miss + 1)) ;;
      "Only in ${mirror}"*|"Only in ${mirror}/"*)
        printf '   \033[36mEXTRA  \033[0m %s\n' "${line#Only in }"; extra=$((extra + 1)) ;;
      Files\ *\ differ)
        printf '   \033[33mVERALTET\033[0m %s\n' "$line"; stale=$((stale + 1)) ;;
    esac
  done < <(diff -rq "${EXCLUDES[@]}" "$active" "$mirror" 2>/dev/null)
  if [ "$miss" -eq 0 ] && [ "$stale" -eq 0 ] && [ "$extra" -eq 0 ]; then
    printf '   \033[32m1:1 aktuell\033[0m (%s)\n' "$what"
  fi
  total_missing=$((total_missing + miss))
  total_stale=$((total_stale + stale))
  total_extra=$((total_extra + extra))
}

sync_dir() {
  # $1 aktiv -> $2 spiegel (additiv, keine Loeschung)
  local active="$1" mirror="$2"
  [ -d "$active" ] || { printf '   (aktiv fehlt: %s — uebersprungen)\n' "$active"; return; }
  mkdir -p "$mirror"
  if command -v rsync >/dev/null 2>&1; then
    rsync -a --exclude='__pycache__' --exclude='*.pyc' --exclude='learned' \
      --exclude='*.bak' --exclude='*.reset-bak-*' "$active"/ "$mirror"/
  else
    cp -R "$active"/. "$mirror"/ 2>/dev/null || true
    find "$mirror" -type d -name '__pycache__' -prune -exec rm -rf {} + 2>/dev/null || true
    find "$mirror" -type f \( -name '*.pyc' -o -name '*.bak' -o -name '*.reset-bak-*' \) -delete 2>/dev/null || true
  fi
  printf '   \033[32mgespiegelt\033[0m %s -> %s\n' "$active" "$mirror"
}

printf '=== harness-mirror (%s) ===\n' "$MODE"
for entry in "${COMPONENTS[@]}"; do
  IFS='|' read -r label active m1 m2 <<< "$entry"
  printf '\n[%s]\n' "$label"
  if [ "$MODE" = "sync" ]; then
    sync_dir "$active" "$m1"
    [ -n "$m2" ] && sync_dir "$active" "$m2"
  else
    printf ' -> claude-code-setup:\n'
    compare_dir "$active" "$m1" "$label"
    if [ -n "$m2" ]; then
      printf ' -> Umgebung:\n'
      compare_dir "$active" "$m2" "$label"
    fi
  fi
done

printf '\n[plugins]\n'
printf '   Hinweis: Eigene Plugins manuell pruefen (claude-code-setup/Plugins, Umgebung/Plugins).\n'
printf '   ~/.claude/plugins/ enthaelt ueberwiegend FREMDE Plugins — kein Auto-Vergleich.\n'

if [ "$MODE" = "check" ]; then
  printf '\n=== Zusammenfassung ===\n'
  printf '   FEHLT (im Spiegel nicht vorhanden): %d\n' "$total_missing"
  printf '   VERALTET (Inhalt weicht ab):        %d\n' "$total_stale"
  printf '   EXTRA (nur im Spiegel):             %d\n' "$total_extra"
  if [ "$total_missing" -eq 0 ] && [ "$total_stale" -eq 0 ]; then
    printf '   \033[32mAlle Spiegel sind aktuell.\033[0m\n'
  else
    printf '   \033[33mZum Nachziehen: bash harness-mirror.sh sync\033[0m\n'
  fi
fi
