#!/usr/bin/env bash
# verify-skills.sh
#
# Phase-0-Tor-Waechter des finale Plugins.
# Prueft fuer alle vier Skill-Symlinks:
#   1. Symlink existiert physisch
#   2. Symlink ist aufloesbar
#   3. Zielverzeichnis existiert
#   4. Ziel/SKILL.md existiert und ist lesbar
# Berechnet sha256, mtime, version-Tag, references-Count pro Skill.
# Schreibt strukturiertes JSON nach stdout (fuer Orchestrator-Konsum).
# Schreibt menschenlesbare Diagnosen nach stderr.
# Exit-Code: 0 wenn alle Skills OK, 1 wenn mindestens ein Skill kaputt.
#
# Aufruf: bash scripts/verify-skills.sh [plugin-root]
# Default plugin-root = Verzeichnis ueber scripts/

set -u

# -----------------------------------------------------------------------------
# Konfiguration
# -----------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PLUGIN_ROOT="${1:-$(cd "$SCRIPT_DIR/.." && pwd)}"
SKILLS_DIR="$PLUGIN_ROOT/skills"

# Welche Skills gehoeren ins Plugin und welches ist ihr erwartetes echtes Ziel?
# Reihenfolge entscheidet ueber JSON-Reihenfolge.
# Bash 3.2 kompatibel (macOS-System-Default), daher zwei parallele indizierte Arrays
# statt assoziativem Array (declare -A).
SKILL_NAMES=(roentgen-skill rechtssicherheits-skill strings-skill uebersetzer-skill)
EXPECTED_BASENAMES=(app-roentgen rechtssicherheit string-extraktor übersetzung)

# Mapping-Funktion: Symlink-Name -> erwarteter Basename des Ziels
expected_basename_for() {
  local key="$1"
  local i
  for i in "${!SKILL_NAMES[@]}"; do
    if [ "${SKILL_NAMES[$i]}" = "$key" ]; then
      printf '%s' "${EXPECTED_BASENAMES[$i]}"
      return 0
    fi
  done
  printf ''
  return 1
}

# -----------------------------------------------------------------------------
# Hilfsfunktionen
# -----------------------------------------------------------------------------

log() { printf '[verify-skills] %s\n' "$*" >&2; }
err() { printf '[verify-skills] FEHLER: %s\n' "$*" >&2; }

# JSON-String-Escape (minimal, fuer Pfade/Hashes).
json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  s="${s//$'\r'/\\r}"
  s="${s//$'\t'/\\t}"
  printf '%s' "$s"
}

# Plattformneutraler sha256 (Linux: sha256sum, macOS: shasum -a 256, Windows Git Bash: sha256sum).
sha256_of() {
  local file="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  else
    echo "no-sha256-tool"
    return 1
  fi
}

# Plattformneutraler mtime in ISO-8601 (Linux: stat -c, macOS/BSD: stat -f).
mtime_of() {
  local file="$1"
  if stat -c '%y' "$file" >/dev/null 2>&1; then
    stat -c '%y' "$file" | awk '{printf "%sT%s%s", $1, $2, $3}'
  elif stat -f '%Sm' "$file" >/dev/null 2>&1; then
    stat -f '%Sm' -t '%Y-%m-%dT%H:%M:%S' "$file" 2>/dev/null
  else
    echo "unknown"
  fi
}

# Liest "version:" oder "Version X.Y" aus dem ersten 30 Zeilen einer SKILL.md.
# Liefert leeren String wenn nichts gefunden.
version_of() {
  local file="$1"
  local v
  v="$(head -n 30 "$file" 2>/dev/null | grep -iE '^(version[: ]|# *Version[: ])' | head -n 1 | sed -E 's/^[Vv]ersion[: ]+//; s/^# *Version[: ]+//' | tr -d '\r' | head -c 50)"
  printf '%s' "$v"
}

# Zaehlt Dateien im references/-Unterordner. 0 wenn nicht vorhanden.
references_count_of() {
  local skill_target="$1"
  if [ -d "$skill_target/references" ]; then
    find "$skill_target/references" -maxdepth 1 -type f 2>/dev/null | wc -l | tr -d '[:space:]'
  else
    echo 0
  fi
}

# -----------------------------------------------------------------------------
# Haupt-Pruefschleife
# -----------------------------------------------------------------------------

log "Plugin-Root: $PLUGIN_ROOT"
log "Skills-Verzeichnis: $SKILLS_DIR"

if [ ! -d "$SKILLS_DIR" ]; then
  err "Skills-Verzeichnis fehlt: $SKILLS_DIR"
  cat <<JSON
{
  "ok": false,
  "fatal": "skills-directory-missing",
  "skillsDir": "$(json_escape "$SKILLS_DIR")",
  "skills": []
}
JSON
  exit 1
fi

declare -i overall_ok=1
declare -a json_chunks=()

for name in "${SKILL_NAMES[@]}"; do
  link_path="$SKILLS_DIR/$name"
  expected_basename="$(expected_basename_for "$name")"
  expected_full="$HOME/.claude/skills/$expected_basename"

  status="ok"
  problem=""
  resolved=""
  target_exists="false"
  skill_md=""
  sha=""
  mtime=""
  version=""
  refs=0

  if [ ! -L "$link_path" ]; then
    if [ -e "$link_path" ]; then
      status="error"
      problem="not-a-symlink"
    else
      status="error"
      problem="symlink-missing"
    fi
  else
    resolved="$(readlink "$link_path" 2>/dev/null || true)"
    # Normalisiere /c/... vs C:\... fuer Vergleich (best-effort).
    resolved_norm="$(printf '%s' "$resolved" | sed 's:/$::')"
    expected_norm="$(printf '%s' "$expected_full" | sed 's:/$::')"
    expected_alt="/c/Users/${USER:-$USERNAME}/.claude/skills/$expected_basename"
    expected_alt_lower="$(printf '%s' "$expected_alt" | tr '[:upper:]' '[:lower:]')"
    resolved_lower="$(printf '%s' "$resolved_norm" | tr '[:upper:]' '[:lower:]')"

    if [ "$resolved_norm" = "$expected_norm" ] \
       || [ "$resolved_lower" = "$expected_alt_lower" ] \
       || [ "$(basename "$resolved_norm")" = "$expected_basename" ]; then
      :  # OK
    else
      status="warn"
      problem="symlink-points-elsewhere"
    fi

    if [ -d "$link_path/" ]; then
      target_exists="true"
      if [ -f "$link_path/SKILL.md" ]; then
        skill_md="$link_path/SKILL.md"
        sha="$(sha256_of "$skill_md" 2>/dev/null || echo "")"
        mtime="$(mtime_of "$skill_md")"
        version="$(version_of "$skill_md")"
        refs="$(references_count_of "$link_path")"
      else
        status="error"
        problem="skill-md-missing"
      fi
    else
      status="error"
      problem="target-directory-missing"
    fi
  fi

  if [ "$status" = "error" ]; then
    overall_ok=0
    err "Skill '$name': $problem (link=$link_path, expected target $expected_full)"
  elif [ "$status" = "warn" ]; then
    log "Skill '$name': WARN $problem (resolved=$resolved, expected=$expected_full)"
  else
    log "Skill '$name': OK (sha=${sha:0:12}..., mtime=$mtime, refs=$refs)"
  fi

  json_chunks+=("    {
      \"name\": \"$(json_escape "$name")\",
      \"status\": \"$(json_escape "$status")\",
      \"problem\": \"$(json_escape "$problem")\",
      \"linkPath\": \"$(json_escape "$link_path")\",
      \"expectedTarget\": \"$(json_escape "$expected_full")\",
      \"resolvedTarget\": \"$(json_escape "$resolved")\",
      \"targetExists\": $target_exists,
      \"skillMd\": \"$(json_escape "$skill_md")\",
      \"sha256\": \"$(json_escape "$sha")\",
      \"mtime\": \"$(json_escape "$mtime")\",
      \"versionTag\": \"$(json_escape "$version")\",
      \"referencesCount\": $refs
    }")
done

# -----------------------------------------------------------------------------
# JSON-Output (stdout)
# -----------------------------------------------------------------------------

joined=""
for i in "${!json_chunks[@]}"; do
  if [ $i -gt 0 ]; then joined+=$',\n'; fi
  joined+="${json_chunks[$i]}"
done

if [ $overall_ok -eq 1 ]; then ok_str=true; else ok_str=false; fi
ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cat <<JSON
{
  "ok": $ok_str,
  "timestamp": "$ts",
  "pluginRoot": "$(json_escape "$PLUGIN_ROOT")",
  "skillsDir": "$(json_escape "$SKILLS_DIR")",
  "skills": [
$joined
  ]
}
JSON

if [ $overall_ok -eq 1 ]; then
  log "Alle vier Skills OK."
  exit 0
else
  err "Mindestens ein Skill kaputt. Reparatur noetig vor Phase 1."
  exit 1
fi
