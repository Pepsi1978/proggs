#!/usr/bin/env bash
# brain-backup.sh — taegliches Backup des "zweiten Gehirns" (Qdrant-Collection 'brain').
#
# Was es tut (Observability-First: nichts stirbt still, alles ins Log):
#   1. Erstellt via Qdrant-Snapshot-API einen vollstaendigen Snapshot der Collection 'brain'
#      (alle Eintraege + Vektoren + Payload in EINER Datei).
#   2. Laedt den Snapshot per API herunter nach $DEST (= /srv/samba/gedanken/qdrant-snapshot,
#      also Z:\qdrant-snapshot auf Franks PC). Damit liegt das Backup auf der Samba-Platte.
#   3. Loescht den Qdrant-INTERNEN Snapshot wieder (er liegt im Container unter /qdrant/snapshots,
#      ist NICHT gemountet -> wuerde sich sonst nur im Container ansammeln).
#   4. Rotation: behaelt die letzten $KEEP Snapshots, loescht aeltere.
#
# WICHTIG (ehrliche Einordnung): $DEST liegt PHYSISCH auf dem VPS. Dieses Backup schuetzt vor
# Datenfehlern (versehentliches Loeschen, kaputte Collection, fehlerhaftes Update) — NICHT vor
# einem VPS-Totalverlust. Echtes Offsite = separates Server-Disaster-Recovery (Backup auf Franks PC).
#
# Cron (Server, 4 Uhr nachts Europe/Berlin): siehe Installations-Hinweis am Dateiende.
# Wiederherstellen: bugs/server/qdrant.md §3/§7 (Restore: Content-Type multipart NICHT explizit setzen).
set -euo pipefail

QDRANT_URL="http://127.0.0.1:6333"
COLLECTION="brain"
DEST="/srv/samba/gedanken/qdrant-snapshot"
KEEP=14                       # 14 taegliche Snapshots aufbewahren (je ~3 MB -> winzig)
LOG="/opt/second-brain/brain-logs/brain-backup.jsonl"
OWNER_UID=1000                # frank (Samba) — damit die Datei ueber Z: lesbar/verwaltbar ist

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >> "$LOG" 2>/dev/null || true; }
fail() { log "FEHLER: $*"; echo "brain-backup FEHLER: $*" >&2; exit 1; }

# API-Key aus der .env (nie im Skript hartkodiert)
if [ -f /opt/second-brain/.env ]; then set -a; . /opt/second-brain/.env; set +a; fi
KEY="${QDRANT_API_KEY:-}"
[ -n "$KEY" ] || fail "QDRANT_API_KEY fehlt (/opt/second-brain/.env)"

mkdir -p "$DEST" "$(dirname "$LOG")" 2>/dev/null || true

# 1) Snapshot erstellen
resp=$(curl -fsS -X POST "$QDRANT_URL/collections/$COLLECTION/snapshots" -H "api-key: $KEY") \
    || fail "Snapshot-Erstellung fehlgeschlagen"
name=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin)['result']['name'])") \
    || fail "Snapshot-Name nicht lesbar: $resp"
apisize=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin)['result']['size'])" 2>/dev/null || echo 0)

# 2) Herunterladen nach DEST (Datums-Name; mehrfach am Tag = idempotent ueberschrieben)
stamp=$(date '+%Y-%m-%d')
out="$DEST/brain-$stamp.snapshot"
curl -fsS "$QDRANT_URL/collections/$COLLECTION/snapshots/$name" -H "api-key: $KEY" -o "$out" \
    || fail "Download fehlgeschlagen ($name)"

# 3) Integritaet: heruntergeladene Datei muss plausibel gross sein (vs. API-Groesse)
dl=$(stat -c%s "$out" 2>/dev/null || echo 0)
if [ "$dl" -lt 1000 ]; then rm -f "$out"; fail "heruntergeladene Datei zu klein ($dl bytes) — verworfen"; fi
chown "$OWNER_UID:$OWNER_UID" "$out" 2>/dev/null || true

# 4) Qdrant-internen Snapshot loeschen (kein Ansammeln im Container)
curl -fsS -X DELETE "$QDRANT_URL/collections/$COLLECTION/snapshots/$name" -H "api-key: $KEY" >/dev/null 2>&1 \
    || log "WARN: interner Snapshot $name nicht geloescht"

# 5) Rotation: nur die letzten KEEP behalten (nach Aenderungszeit sortiert)
removed=0
while IFS= read -r f; do
    [ -n "$f" ] || continue
    rm -f "$f" && removed=$((removed+1)) && log "rotiert (geloescht): $(basename "$f")"
done < <(ls -1t "$DEST"/brain-*.snapshot 2>/dev/null | tail -n +$((KEEP+1)))

count=$(ls -1 "$DEST"/brain-*.snapshot 2>/dev/null | wc -l)
log "OK: $(basename "$out") ($dl bytes, API meldete $apisize) gesichert; $count Snapshot(s) im Ordner (KEEP=$KEEP, $removed rotiert)"
exit 0

# ── Installation (einmalig, als root auf dem VPS) ───────────────────────────
#   chmod +x /opt/second-brain/scripts/brain-backup.sh
#   crontab:  CRON_TZ=Europe/Berlin
#             0 4 * * *  /opt/second-brain/scripts/brain-backup.sh
