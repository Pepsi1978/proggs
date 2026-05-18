#!/usr/bin/env bash
# check-roentgen-output.sh — Sucht nach vorhandenen app-roentgen-Outputs an Standard-Standorten.
#
# Wird vom Rechtssicherheits-Skill in Schritt 1.5 (Roentgen-Integration) aufgerufen.
# Gibt Pfade aller gefundenen Outputs als Pseudo-JSON aus, plus Status-Code:
#   0 = Outputs gefunden
#   1 = Keine Outputs gefunden — Skill soll Roentgen vorschlagen
#   2 = Ungueltiger Aufruf
#
# Verwendung: bash check-roentgen-output.sh <APP_DIR>

set -u

APP_DIR="${1:-}"

if [ -z "$APP_DIR" ]; then
    echo "Verwendung: $0 <APP_DIR>" >&2
    exit 2
fi

if [ ! -d "$APP_DIR" ]; then
    echo "Fehler: '$APP_DIR' ist kein Verzeichnis." >&2
    exit 2
fi

APP_NAME=$(basename "$APP_DIR")
PARENT_DIR=$(dirname "$APP_DIR")

echo "{"
echo "  \"app_dir\": \"$APP_DIR\","
echo "  \"app_name\": \"$APP_NAME\","

FOUND=0

# Suchstandorte fuer Roentgen-Outputs
declare -a paths=()

# 1. Direkt im App-Verzeichnis
for f in "$APP_DIR"/app-roentgen-*.md "$APP_DIR"/app-roentgen-export.json; do
    if [ -f "$f" ]; then
        paths+=("$f")
        FOUND=1
    fi
done

# 2. Eltern-Verzeichnis mit App-Name-Prefix
for f in "$PARENT_DIR/$APP_NAME-app-roentgen-AUDIT-"*.md; do
    if [ -f "$f" ]; then
        paths+=("$f")
        FOUND=1
    fi
done

# 3. Workspace-Root / WORKSPACE/APP_NAME/
# Aufloesung in 3 Stufen (von hoch zu niedrig prior):
#   a) Umgebungsvariable WORKSPACE_ROOT, falls explizit gesetzt
#   b) Eltern-Verzeichnis von APP_DIR (`realpath "$APP_DIR/.."`) — funktioniert plattformneutral
#   c) Fallback $HOME/proggs (Franks Standard-Setup) — nur wenn (a) und (b) fehlschlagen
WORKSPACE_ROOT="${WORKSPACE_ROOT:-$(realpath "$APP_DIR/.." 2>/dev/null || echo "$HOME/proggs")}"
if [ -d "$WORKSPACE_ROOT/$APP_NAME" ]; then
    for f in "$WORKSPACE_ROOT/$APP_NAME"/app-roentgen-*.md "$WORKSPACE_ROOT/$APP_NAME"/app-roentgen-export.json; do
        if [ -f "$f" ]; then
            paths+=("$f")
            FOUND=1
        fi
    done
fi

# Dedup: gleichen realpath nur einmal ausgeben. Wichtig wenn APP_DIR == WORKSPACE_ROOT/APP_NAME.
declare -a unique_paths=()
declare -A seen=()
for p in "${paths[@]}"; do
    canonical=$(realpath "$p" 2>/dev/null || echo "$p")
    if [ -z "${seen[$canonical]+x}" ]; then
        seen[$canonical]=1
        unique_paths+=("$p")
    fi
done
paths=("${unique_paths[@]}")

echo "  \"roentgen_outputs\": ["
for ((i=0; i<${#paths[@]}; i++)); do
    if [ $i -lt $((${#paths[@]} - 1)) ]; then
        echo "    \"${paths[$i]}\","
    else
        echo "    \"${paths[$i]}\""
    fi
done
echo "  ],"

# Aelter als letzter App-Commit?
STALE=""
if [ $FOUND -eq 1 ] && [ -d "$APP_DIR/.git" ] || [ -d "$PARENT_DIR/.git" ]; then
    LAST_COMMIT_TS=$(cd "$APP_DIR" 2>/dev/null && git log -1 --format=%at 2>/dev/null || \
                      cd "$PARENT_DIR" 2>/dev/null && git log -1 --format=%at 2>/dev/null || echo "")
    if [ -n "$LAST_COMMIT_TS" ]; then
        for p in "${paths[@]}"; do
            OUT_TS=$(stat -c %Y "$p" 2>/dev/null || stat -f %m "$p" 2>/dev/null || echo "")
            if [ -n "$OUT_TS" ] && [ "$OUT_TS" -lt "$LAST_COMMIT_TS" ]; then
                STALE="true"
            fi
        done
    fi
fi

if [ -z "$STALE" ]; then
    STALE="false"
fi

echo "  \"stale\": $STALE,"
echo "  \"found\": $([ $FOUND -eq 1 ] && echo true || echo false)"
echo "}"

if [ $FOUND -eq 1 ]; then
    exit 0
else
    exit 1
fi
