#!/usr/bin/env bash
# full-restore.sh — spielt ein KOMPLETT-Backup (cortex-full-*.tar.gz) auf einem Server ein.
#
# Zweck: Server bei Totalausfall schnell wieder GENAU wie vorher aufsetzen. Auf einem FRISCHEN
# Ubuntu (Docker + docker compose installiert, WireGuard ggf. eingerichtet) reicht dieses Skript +
# das Archiv, um das ganze zweite Gehirn (inkl. aller Eintraege) wiederherzustellen.
#
# ABLAUF:
#   1. Archiv auspacken.
#   2. /opt/second-brain wiederherstellen (compose.yaml, .env, agent-data = 3 Prompts/config/categories, …).
#   3. Caddy named volumes wiederherstellen (interne CA/Zertifikate).
#   4. docker compose up -d  (baut die Images, startet den Stack).
#   5. Qdrant-Gehirn aus dem Snapshot wiederherstellen (Snapshot-Upload-API).
#
# Aufruf:  sudo bash full-restore.sh /pfad/zu/cortex-full-JJJJ-MM-TT_HHMMSS.tar.gz
# WICHTIG: Restore ist DESTRUKTIV (ueberschreibt vorhandene Daten am Ziel). Bewusst mit "JA" bestaetigen.
# Qdrant-Restore: Content-Type bei -F NICHT explizit setzen (curl generiert die boundary; qdrant.md §3/§7).
set -euo pipefail

ARCHIVE="${1:-}"
APP_DIR="${SB_APP_DIR:-/opt/second-brain}"
QDRANT_URL="${SB_QDRANT_URL:-http://127.0.0.1:6333}"
COLLECTION="${SB_COLLECTION:-brain__e2}"                        # muss zur compose.yaml passen (Vorfall 2026-07-10: alter Default 'brain' = geloeschte Collection)
ENTITY_COLLECTION="${SB_ENTITY_COLLECTION:-brain_entities__e2}"  # Entity-Register
SAMBA_DIR="${SB_SAMBA_DIR:-/srv/samba}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cortex-restore.XXXXXX")"
log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
cleanup() { rm -rf "$WORK" 2>/dev/null || true; }
trap cleanup EXIT

[ -n "$ARCHIVE" ] && [ -f "$ARCHIVE" ] || { echo "Aufruf: sudo bash full-restore.sh <cortex-full-*.tar.gz>"; exit 1; }

echo "============================================================"
echo " Cortex KOMPLETT-RESTORE"
echo "   Archiv: $ARCHIVE"
echo "   Ziel:   $APP_DIR  (wird ueberschrieben!)"
echo "============================================================"
printf 'Wirklich wiederherstellen? Tippe JA: '
read -r ans
[ "$ans" = "JA" ] || { echo "Abgebrochen."; exit 0; }

# Docker vorhanden?
command -v docker >/dev/null 2>&1 || { echo "FEHLER: docker fehlt. Erst Docker + 'docker compose' installieren."; exit 1; }
DC="docker compose"; $DC version >/dev/null 2>&1 || DC="docker-compose"

# 1) Auspacken
log "Entpacke Archiv…"
tar xzf "$ARCHIVE" -C "$WORK"
[ -f "$WORK/manifest.txt" ] && { echo "--- Manifest ---"; cat "$WORK/manifest.txt"; echo "----------------"; }

# 2) /opt/second-brain wiederherstellen
log "Stelle $APP_DIR wieder her…"
mkdir -p "$APP_DIR"
if [ -f "$WORK/opt-second-brain.tar.gz" ]; then
  tar xzf "$WORK/opt-second-brain.tar.gz" -C "$APP_DIR"
  log "Anwendungsdateien + .env + agent-data wiederhergestellt"
else
  log "WARN: opt-second-brain.tar.gz fehlt im Archiv — bitte Repo manuell nach $APP_DIR klonen + .env ergaenzen"
fi
# qdrant-data-Fallback (nur falls KEIN API-Snapshot im Archiv war)
if [ -f "$WORK/qdrant-data-fallback.tar.gz" ] && [ ! -f "$WORK/qdrant-brain.snapshot" ]; then
  log "Spiele qdrant-data-Fallback ein…"; tar xzf "$WORK/qdrant-data-fallback.tar.gz" -C "$APP_DIR"
fi

# 2b) Samba-Platten (Logbuch + daten) wiederherstellen
if [ -f "$WORK/samba.tar.gz" ]; then
  log "Stelle Samba-Platten (Logbuch + daten) wieder her…"
  mkdir -p "$SAMBA_DIR"
  tar xzf "$WORK/samba.tar.gz" -C "$SAMBA_DIR" && log "Samba wiederhergestellt"
fi

# 3) Caddy named volumes wiederherstellen
PROJ="$(basename "$APP_DIR")"
restore_volume() {
  local file="$1" base="$2"
  local vol="${PROJ}_${base}"
  [ -f "$file" ] || { log "Info: $base nicht im Archiv (uebersprungen)"; return; }
  docker volume create "$vol" >/dev/null 2>&1 || true
  docker run --rm -v "$vol":/v -v "$WORK":/in alpine:3 sh -c "cd /v && tar xzf /in/$(basename "$file")" >/dev/null 2>&1 \
    && log "Volume $vol wiederhergestellt" || log "WARN: Volume $vol konnte nicht wiederhergestellt werden"
}
restore_volume "$WORK/caddy_data.tar.gz" caddy_data
restore_volume "$WORK/caddy_config.tar.gz" caddy_config

# 4) Stack starten
log "Starte den Stack (docker compose up -d --build)…"
( cd "$APP_DIR" && $DC up -d --build )

# 5) Qdrant-Gehirn aus dem Snapshot wiederherstellen
if [ -f "$WORK/qdrant-brain.snapshot" ]; then
  if [ -f "$APP_DIR/.env" ]; then set -a; . "$APP_DIR/.env"; set +a; fi
  QKEY="${QDRANT_API_KEY:-}"
  log "Warte auf Qdrant…"
  for _ in $(seq 1 30); do curl -fsS --max-time 60 "$QDRANT_URL/healthz" >/dev/null 2>&1 && break; sleep 2; done
  log "Spiele Qdrant-Snapshot ein (Collection $COLLECTION)…"
  # KEIN explizites Content-Type — curl baut die multipart-boundary selbst (qdrant.md §3/§7).
  if curl -fsS --max-time 600 -X POST "$QDRANT_URL/collections/$COLLECTION/snapshots/upload?priority=snapshot" \
        ${QKEY:+-H "api-key: $QKEY"} -F "snapshot=@$WORK/qdrant-brain.snapshot" >/dev/null 2>&1; then
    log "Qdrant-Gehirn wiederhergestellt."
  else
    log "WARN: Qdrant-Snapshot-Upload fehlgeschlagen — Stack laeuft, aber das Gehirn muss ggf. manuell eingespielt werden (siehe bugs/server/qdrant.md §3)."
  fi
else
  log "Info: kein Qdrant-Snapshot im Archiv (Fallback qdrant-data wurde — falls vorhanden — bereits eingespielt)."
fi

# 5b) Entity-Register aus dem Snapshot wiederherstellen (in aelteren Archiven nicht enthalten)
if [ -f "$WORK/qdrant-brain-entities.snapshot" ]; then
  if [ -f "$APP_DIR/.env" ]; then set -a; . "$APP_DIR/.env"; set +a; fi
  QKEY="${QDRANT_API_KEY:-}"   # eigenes Laden: Block 5 koennte uebersprungen worden sein (set -u)
  log "Spiele Entity-Snapshot ein (Collection $ENTITY_COLLECTION)…"
  if curl -fsS --max-time 600 -X POST "$QDRANT_URL/collections/$ENTITY_COLLECTION/snapshots/upload?priority=snapshot" \
        ${QKEY:+-H "api-key: $QKEY"} -F "snapshot=@$WORK/qdrant-brain-entities.snapshot" >/dev/null 2>&1; then
    log "Entity-Register wiederhergestellt."
  else
    log "WARN: Entity-Snapshot-Upload fehlgeschlagen — brain-api legt die Collection leer neu an (Verknuepfungen fehlen dann)."
  fi
else
  log "Info: kein Entity-Snapshot im Archiv (aelteres Backup) — brain-api legt die Collection leer neu an."
fi

log "RESTORE FERTIG. Pruefe: docker compose ps  +  das Dashboard ueber den WireGuard-Tunnel."
