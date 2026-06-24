#!/usr/bin/env bash
# full-backup-create.sh — KOMPLETT-Backup des zweiten Gehirns als EIN Archiv (server-seitig).
#
# Zweck (Frank-Wunsch 2026-06-24): ein server-UNABHAENGIGES Voll-Backup, mit dem der ganze Server
# bei Totalausfall schnell wieder GENAU wie vorher aufgesetzt werden kann. Dieses Skript laeuft auf
# dem VPS (per SSH von Franks PC angestossen, siehe windows/cortex-full-backup.ps1) und packt ALLES
# Unersetzliche in eine einzige .tar.gz-Datei. Franks PC zieht diese Datei danach per scp lokal.
#
# WAS GESICHERT WIRD (alles, was den Server reproduzierbar macht):
#   1. Qdrant-Gehirn  — frischer, KONSISTENTER Snapshot der Collection 'brain' ueber die Snapshot-API
#                       (kein Datei-Kopieren bei laufendem Qdrant -> immer konsistent).
#   2. /opt/second-brain — compose.yaml, .env (Secrets!), agent-data (3 Prompts + config + categories),
#                       Caddyfile, Dockerfiles, Skripte. OHNE qdrant-data (kommt als Snapshot),
#                       OHNE backups/ (kein Backup-im-Backup), OHNE .git / brain-logs (regenerierbar).
#   3. Caddy named volumes (caddy_data = interne CA/Zertifikate, caddy_config).
#   4. WireGuard-Config /etc/wireguard/wg0.conf (falls lesbar) — fuer den Tunnel-Zugang.
#   5. manifest.txt — Datum, Hostname, Versionen, Inhalt (fuer den Restore nachvollziehbar).
#
# Observability-First: nichts stirbt still — jeder Schritt loggt; ein Teil-Fehler bricht NICHT alles ab.
# Verwandt: brain-backup.sh (taeglicher Gehirn-Snapshot auf Z), bugs/server/qdrant.md §3/§7 (Restore).
set -euo pipefail

# ── Konfiguration (Defaults passen zum Stack; per Env ueberschreibbar) ───────────────────────────
APP_DIR="${SB_APP_DIR:-/opt/second-brain}"
QDRANT_URL="${SB_QDRANT_URL:-http://127.0.0.1:6333}"
COLLECTION="${SB_COLLECTION:-brain}"
OUT_DIR="${SB_BACKUP_OUT:-$APP_DIR/backups}"
WG_CONF="${SB_WG_CONF:-/etc/wireguard/wg0.conf}"
SAMBA_DIR="${SB_SAMBA_DIR:-/srv/samba}"   # Z=gedanken (Logbuch) + Y=daten (Franks Netzlaufwerke)
KEEP="${SB_BACKUP_KEEP:-7}"                 # so viele Voll-Backups serverseitig behalten (Platz sparen)
STAMP="$(date '+%Y-%m-%d_%H%M%S')"
HOST="$(hostname 2>/dev/null || echo unknown)"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cortex-backup.XXXXXX")"
ARCHIVE="$OUT_DIR/cortex-full-$STAMP.tar.gz"

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2; }
cleanup() { rm -rf "$WORK" 2>/dev/null || true; }
trap cleanup EXIT

mkdir -p "$OUT_DIR" "$WORK"

# API-Key aus der .env (NIE im Skript hartkodiert)
if [ -f "$APP_DIR/.env" ]; then set -a; . "$APP_DIR/.env"; set +a; fi
QKEY="${QDRANT_API_KEY:-}"

# ── 1) Qdrant-Snapshot (konsistent, ueber die API) ──────────────────────────────────────────────
qdrant_ok=0
if [ -n "$QKEY" ]; then
  if resp=$(curl -fsS -X POST "$QDRANT_URL/collections/$COLLECTION/snapshots" -H "api-key: $QKEY" 2>/dev/null); then
    name=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin)['result']['name'])" 2>/dev/null || echo "")
    if [ -n "$name" ]; then
      if curl -fsS "$QDRANT_URL/collections/$COLLECTION/snapshots/$name" -H "api-key: $QKEY" -o "$WORK/qdrant-brain.snapshot" 2>/dev/null; then
        sz=$(stat -c%s "$WORK/qdrant-brain.snapshot" 2>/dev/null || echo 0)
        if [ "$sz" -ge 1000 ]; then qdrant_ok=1; log "Qdrant-Snapshot OK ($sz bytes)"; else log "WARN: Qdrant-Snapshot zu klein ($sz) — verworfen"; rm -f "$WORK/qdrant-brain.snapshot"; fi
      else log "WARN: Qdrant-Snapshot-Download fehlgeschlagen"; fi
      # internen Snapshot wieder loeschen (kein Ansammeln im Container)
      curl -fsS -X DELETE "$QDRANT_URL/collections/$COLLECTION/snapshots/$name" -H "api-key: $QKEY" >/dev/null 2>&1 || true
    else log "WARN: Snapshot-Name nicht lesbar"; fi
  else log "WARN: Qdrant-Snapshot-Erstellung fehlgeschlagen (Qdrant erreichbar?)"; fi
else
  log "WARN: QDRANT_API_KEY fehlt — Qdrant-Snapshot uebersprungen"
fi
# Fallback: das qdrant-data-Verzeichnis mitnehmen, falls die API NICHT lieferte (kein Gehirn-Verlust).
if [ "$qdrant_ok" -eq 0 ] && [ -d "$APP_DIR/qdrant-data" ]; then
  log "Fallback: kopiere qdrant-data/ direkt (Snapshot-API lieferte nicht)"
  tar czf "$WORK/qdrant-data-fallback.tar.gz" -C "$APP_DIR" qdrant-data 2>/dev/null && log "qdrant-data-Fallback OK" || log "WARN: qdrant-data-Fallback fehlgeschlagen"
fi

# ── 2) /opt/second-brain ohne regenerierbare/grosse Teile ───────────────────────────────────────
if tar czf "$WORK/opt-second-brain.tar.gz" -C "$APP_DIR" \
      --exclude='./qdrant-data' --exclude='./backups' --exclude='./.git' \
      --exclude='./brain-logs' --exclude='./agent-logs' --exclude='./ollama-data' \
      --exclude='*/__pycache__' . 2>/dev/null; then
  log "opt-second-brain.tar.gz OK"
else
  log "WARN: opt-second-brain-Archiv unvollstaendig"
fi

# ── 2b) Samba-Platten (Z=gedanken inkl. Logbuch, Y=daten) — ohne den redundanten qdrant-snapshot ──
if [ -d "$SAMBA_DIR" ]; then
  if tar czf "$WORK/samba.tar.gz" -C "$SAMBA_DIR" --exclude='./gedanken/qdrant-snapshot' . 2>/dev/null; then
    log "samba.tar.gz OK (Logbuch + daten)"
  else
    log "WARN: samba-Archiv unvollstaendig"
  fi
else
  log "Info: $SAMBA_DIR nicht vorhanden (uebersprungen)"
fi

# ── 3) Caddy named volumes (interne CA/Zertifikate) ─────────────────────────────────────────────
backup_volume() {
  local vol="$1" out="$2"
  if docker volume inspect "$vol" >/dev/null 2>&1; then
    if docker run --rm -v "$vol":/v:ro -v "$WORK":/out alpine:3 tar czf "/out/$out" -C /v . >/dev/null 2>&1; then
      log "Volume $vol -> $out OK"
    else log "WARN: Volume $vol konnte nicht gesichert werden"; fi
  else log "Info: Volume $vol existiert nicht (uebersprungen)"; fi
}
# Compose-Projektname praefixt named volumes (z.B. 'second-brain_caddy_data'). Beide Schreibweisen testen.
PROJ="$(basename "$APP_DIR")"
for base in caddy_data caddy_config; do
  if docker volume inspect "${PROJ}_${base}" >/dev/null 2>&1; then backup_volume "${PROJ}_${base}" "${base}.tar.gz";
  else backup_volume "$base" "${base}.tar.gz"; fi
done

# ── 4) WireGuard-Config (Tunnel-Zugang) ─────────────────────────────────────────────────────────
if [ -r "$WG_CONF" ]; then cp "$WG_CONF" "$WORK/wg0.conf" && log "wg0.conf gesichert"; else log "Info: $WG_CONF nicht lesbar (uebersprungen — ggf. als root ausfuehren)"; fi

# ── 5) Manifest ─────────────────────────────────────────────────────────────────────────────────
{
  echo "Cortex (zweites Gehirn) — KOMPLETT-Backup"
  echo "erstellt:   $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "host:       $HOST"
  echo "app_dir:    $APP_DIR"
  echo "collection: $COLLECTION"
  echo "qdrant_snapshot: $([ "$qdrant_ok" -eq 1 ] && echo ja || echo 'nein (Fallback qdrant-data)')"
  echo "docker:     $(docker --version 2>/dev/null || echo '?')"
  echo "inhalt:"
  ( cd "$WORK" && ls -la )
} > "$WORK/manifest.txt"

# ── Alles in EIN Archiv ─────────────────────────────────────────────────────────────────────────
tar czf "$ARCHIVE" -C "$WORK" . 2>/dev/null || { log "FEHLER: konnte Gesamt-Archiv nicht schreiben"; exit 1; }
asz=$(stat -c%s "$ARCHIVE" 2>/dev/null || echo 0)
[ "$asz" -ge 2000 ] || { log "FEHLER: Gesamt-Archiv zu klein ($asz) — verworfen"; rm -f "$ARCHIVE"; exit 1; }
chown "${SB_OWNER_UID:-1000}:${SB_OWNER_UID:-1000}" "$ARCHIVE" 2>/dev/null || true

# ── Rotation (nur die letzten KEEP behalten) ────────────────────────────────────────────────────
removed=0
while IFS= read -r f; do [ -n "$f" ] || continue; rm -f "$f" && removed=$((removed+1)); done \
  < <(ls -1t "$OUT_DIR"/cortex-full-*.tar.gz 2>/dev/null | tail -n +$((KEEP+1)))

count=$(ls -1 "$OUT_DIR"/cortex-full-*.tar.gz 2>/dev/null | wc -l)
log "FERTIG: $ARCHIVE ($asz bytes); $count Voll-Backup(s) im Ordner (KEEP=$KEEP, $removed rotiert)"
# WICHTIG: die LETZTE stdout-Zeile ist NUR der Archiv-Pfad — die Windows-Seite liest ihn aus.
echo "$ARCHIVE"
