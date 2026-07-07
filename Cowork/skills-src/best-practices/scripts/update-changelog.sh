#!/usr/bin/env bash
# update-changelog.sh — best-practices Skill (Cowork-Fassung)
#
# macOS/Linux/Cowork-VM-Gegenstueck zu update-changelog.ps1.
# Holt den offiziellen Claude-Code-Changelog VERBATIM und pflegt
# best-practices/_changelog-archiv.md:
#   - Erstlauf (oder --first-run): komplette Datei neu aufbauen
#   - sonst inkrementell: NUR Versionen neuer als _state.json.last_version oben anhaengen.
# Jede Version wird mit ihrem npm-Publish-Datum angereichert (## X.Y.Z — YYYY-MM-DD).
# Pure bash + awk (POSIX, kein Python, kein gawk-spezifisches match).
#
# Cowork-DELTA: Der Default-Zielordner ist RELATIV zum aktuellen Arbeitsordner
# (./best-practices), NICHT ein fester ~/proggs-Pfad — sonst schreibt das Script am
# verbundenen Cowork-Ordner vorbei. Aus dem Arbeitsordner-Root aufrufen (CWD = Arbeitsordner).
#
# Aufruf:
#   bash scripts/update-changelog.sh                    # inkrementell
#   bash scripts/update-changelog.sh --first-run         # komplette Neuanlage
#   bash scripts/update-changelog.sh --data-dir <pfad>   # anderer Zielordner

set -euo pipefail

DATA_DIR="$(pwd)/best-practices"   # Cowork: relativ zum Arbeitsordner (CWD)
FIRST_RUN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --first-run) FIRST_RUN=1 ;;
    --data-dir) shift; DATA_DIR="$1" ;;
    *) echo "Unbekanntes Argument: $1" >&2; exit 2 ;;
  esac
  shift
done

CHANGELOG_URL="https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md"
ARCHIVE="${DATA_DIR}/_changelog-archiv.md"
STATE="${DATA_DIR}/_state.json"
TODAY="$(date +%Y-%m-%d)"
DASH="—"
mkdir -p "$DATA_DIR"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
RAW="${TMP}/changelog.md"
NPM="${TMP}/npm-time.json"
MAP="${TMP}/datemap.txt"

# --- 1. last_version lesen ---
LAST_VERSION=""
if [ "$FIRST_RUN" -eq 0 ] && [ -f "$STATE" ]; then
  LAST_VERSION="$(grep -oE '"last_version"[[:space:]]*:[[:space:]]*"[^"]*"' "$STATE" | head -1 | sed -E 's/.*"([^"]*)"$/\1/')" || true
fi
if [ -z "$LAST_VERSION" ] || [ ! -f "$ARCHIVE" ]; then FIRST_RUN=1; fi

# --- 2. Changelog verbatim ---
curl -fsSL "$CHANGELOG_URL" -o "$RAW"

# --- 3. npm-Datums-Map (npm view -> Fallback Registry) ---
if ! npm view '@anthropic-ai/claude-code' time --json > "$NPM" 2>/dev/null || [ ! -s "$NPM" ]; then
  curl -fsSL "https://registry.npmjs.org/@anthropic-ai%2Fclaude-code" 2>/dev/null | tr ',' '\n' > "$NPM" || echo '{}' > "$NPM"
fi
grep -oE '"[0-9]+\.[0-9]+\.[0-9]+"[[:space:]]*:[[:space:]]*"[0-9]{4}-[0-9]{2}-[0-9]{2}' "$NPM" \
  | sed -E 's/^"([0-9.]+)"[[:space:]]*:[[:space:]]*"([0-9-]+)$/\1 \2/' | sort -u > "$MAP" || true

# --- 4. neueste Version aus der Quelle ---
NEWEST="$(grep -m1 -oE '^## [0-9]+\.[0-9]+\.[0-9]+' "$RAW" | sed -E 's/^## //')"
if [ -z "$NEWEST" ]; then echo "Keine Versions-Header in der Quelle gefunden — Abbruch." >&2; exit 1; fi

# --- 5. Kopf-Block ---
make_head() {
  cat <<EOF
# Claude-Code Changelog-Archiv (vollstaendig, verbatim)

> Quelle: https://github.com/anthropics/claude-code/blob/main/CHANGELOG.md
> Raw: ${CHANGELOG_URL}
> Aktualisiert: ${TODAY} — WORTWOERTLICH, ungekuerzt (kein WebFetch, keine Zusammenfassung).
> Datum je Version: npm-Publish-Zeitstempel (@anthropic-ai/claude-code) = faktisches Release-Datum.
> Aktualisierung inkrementell: neue Versionen kommen oben dazu, alte bleiben unangetastet.

---
EOF
}

# awk: Versions-Header mit Datum anreichern. mode=full | incr (incr stoppt am last_version-Header)
enrich() {
  local mode="$1"; local last="$2"
  awk -v mode="$mode" -v last="$last" -v dash="$DASH" -v mapfile="$MAP" '
    FILENAME==mapfile { d[$1]=$2; next }
    {
      if ($0 ~ /^## [0-9]+\.[0-9]+\.[0-9]+[ \t]*$/) {
        ver=$2
        if (mode=="incr" && ver==last) { stop=1 }
        if (stop) next
        date=(ver in d)?d[ver]:"Datum unbekannt"
        print "## " ver " " dash " " date
      } else {
        if (stop) next
        print
      }
    }' "$MAP" "$RAW"
}

if [ "$FIRST_RUN" -eq 1 ]; then
  # --- 6a. komplette Neuanlage ---
  { make_head; echo; enrich "full" ""; } > "$ARCHIVE"
  MODE="FirstRun (Voll-Aufbau)"
  ADDED="$(grep -cE '^## [0-9]+\.[0-9]+\.[0-9]+' "$ARCHIVE" || true)"
else
  # --- 6b. inkrementell ---
  if ! grep -qE "^## ${LAST_VERSION//./\\.}[[:space:]]*\$" "$RAW"; then
    echo "last_version '$LAST_VERSION' nicht in der Quelle gefunden — mit --first-run neu aufbauen." >&2
    exit 1
  fi
  NEWBLOCKS="${TMP}/new.md"
  enrich "incr" "$LAST_VERSION" > "$NEWBLOCKS"
  ADDED="$(grep -cE '^## [0-9]+\.[0-9]+\.[0-9]+' "$NEWBLOCKS" || true)"
  if [ "${ADDED:-0}" -eq 0 ]; then
    MODE="inkrementell (nichts Neues)"
  else
    HN="$(grep -n -m1 -E '^## [0-9]+\.[0-9]+\.[0-9]+' "$ARCHIVE" | cut -d: -f1)"
    if [ -z "$HN" ]; then echo "Im bestehenden Archiv kein Versions-Header gefunden." >&2; exit 1; fi
    {
      sed -n "1,$((HN-1))p" "$ARCHIVE"
      cat "$NEWBLOCKS"
      echo
      sed -n "${HN},\$p" "$ARCHIVE"
    } > "${TMP}/merged.md"
    mv "${TMP}/merged.md" "$ARCHIVE"
    MODE="inkrementell (+${ADDED} neue Version(en))"
  fi
fi

# --- 7. _state.json ---
printf '{\n  "last_version": "%s",\n  "last_checked": "%s"\n}\n' "$NEWEST" "$TODAY" > "$STATE"

# --- 8. Verifikation ---
HDRS="$(grep -cE '^## [0-9]+\.[0-9]+\.[0-9]+' "$ARCHIVE" || true)"
DUP="$(grep -cE "^## ${NEWEST//./\\.} " "$ARCHIVE" || true)"
NEWEST_DATE="$(grep -E "^${NEWEST//./\\.} " "$MAP" | head -1 | awk '{print $2}')"
echo "Modus:                  $MODE"
echo "Neueste Version:        $NEWEST (${NEWEST_DATE:-Datum unbekannt})"
echo "Neue Versionen:         ${ADDED:-0}"
echo "Versions-Header gesamt: $HDRS"
[ "${DUP:-0}" -ne 1 ] && echo "WARNUNG: neueste Version erscheint ${DUP}-mal (Duplikat?)" >&2 || true
exit 0
