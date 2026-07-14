#!/usr/bin/env bash
# brain-backup.sh — taegliches Backup des "zweiten Gehirns" (Qdrant-Collections).
#
# Was es tut (Observability-First: nichts stirbt still, alles ins Log):
#   1. Erstellt via Qdrant-Snapshot-API einen vollstaendigen Snapshot der Haupt-Collection
#      (alle Eintraege + Vektoren + Payload in EINER Datei) UND der Entity-Collection.
#   2. Laedt die Snapshots per API herunter nach $DEST (= /srv/samba/gedanken/qdrant-snapshot,
#      also Z:\qdrant-snapshot auf Franks PC). Damit liegt das Backup auf der Samba-Platte.
#   3. Loescht die Qdrant-INTERNEN Snapshots wieder (liegen im Container unter /qdrant/snapshots,
#      sind NICHT gemountet -> wuerden sich sonst nur im Container ansammeln).
#   4. Rotation: behaelt je Collection die letzten $KEEP Snapshots, loescht aeltere.
#
# Collections kommen aus der .env (SB_COLLECTION/SB_ENTITY_COLLECTION) oder nutzen die
# compose.yaml-Defaults brain__e2 / brain_entities__e2 (gemini-embedding-2 @ 3072).
# HINTERGRUND (Vorfall 2026-07-10): der alte hartcodierte Name 'brain' zeigte nach der
# Embedding-2-Migration (2026-07-08) auf eine GELOESCHTE Collection -> 404 -> zwei Naechte
# ohne Backup. Namen daher nie doppelt pflegen: .env-Override > Default hier.
#
# WICHTIG (ehrliche Einordnung): $DEST liegt PHYSISCH auf dem VPS. Dieses Backup schuetzt vor
# Datenfehlern (versehentliches Loeschen, kaputte Collection, fehlerhaftes Update) — NICHT vor
# einem VPS-Totalverlust. Echtes Offsite = separates Server-Disaster-Recovery (Backup auf Franks PC).
#
# Cron (Server, 4 Uhr nachts Europe/Berlin): siehe Installations-Hinweis am Dateiende.
# Wiederherstellen: bugs/server/qdrant.md §3/§7 (Restore: Content-Type multipart NICHT explizit setzen).
set -euo pipefail

QDRANT_URL="http://127.0.0.1:6333"
DEST="/srv/samba/gedanken/qdrant-snapshot"
KEEP=14                       # 14 taegliche Snapshots je Collection aufbewahren
LOG="/opt/second-brain/brain-logs/brain-backup.jsonl"
OWNER_UID=1000                # frank (Samba) — damit die Datei ueber Z: lesbar/verwaltbar ist

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >> "$LOG" 2>/dev/null || true; }
fail() { log "FEHLER: $*"; echo "brain-backup FEHLER: $*" >&2; exit 1; }

# API-Key + Collection-Namen aus der .env (nie im Skript hartkodiert)
if [ -f /opt/second-brain/.env ]; then set -a; . /opt/second-brain/.env; set +a; fi
KEY="${QDRANT_API_KEY:-}"
[ -n "$KEY" ] || fail "QDRANT_API_KEY fehlt (/opt/second-brain/.env)"
COLLECTION="${SB_COLLECTION:-brain__e2}"                       # Haupt-Gehirn (muss zur compose.yaml passen)
ENTITY_COLLECTION="${SB_ENTITY_COLLECTION:-brain_entities__e2}" # Entity-Register (sonst nach Restore weg)

mkdir -p "$DEST" "$(dirname "$LOG")" 2>/dev/null || true

# Sichert EINE Collection: Snapshot erstellen -> herunterladen -> intern loeschen -> rotieren.
# $1 = Collection-Name in Qdrant, $2 = Dateinamen-Prefix in $DEST (Rotation je Prefix getrennt).
snapshot_one() {
    local coll="$1" prefix="$2" resp name apisize stamp out dl removed count
    # 1) Snapshot erstellen
    resp=$(curl -fsS --max-time 600 -X POST "$QDRANT_URL/collections/$coll/snapshots" -H "api-key: $KEY") \
        || fail "Snapshot-Erstellung fehlgeschlagen ($coll)"
    name=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin)['result']['name'])") \
        || fail "Snapshot-Name nicht lesbar ($coll): $resp"
    apisize=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin)['result']['size'])" 2>/dev/null || echo 0)

    # 2) Herunterladen nach DEST (Datums-Name; mehrfach am Tag = idempotent ueberschrieben)
    stamp=$(date '+%Y-%m-%d')
    out="$DEST/$prefix-$stamp.snapshot"
    curl -fsS --max-time 600 "$QDRANT_URL/collections/$coll/snapshots/$name" -H "api-key: $KEY" -o "$out" \
        || fail "Download fehlgeschlagen ($coll/$name)"

    # 3) Integritaet: heruntergeladene Datei muss plausibel gross sein (vs. API-Groesse)
    dl=$(stat -c%s "$out" 2>/dev/null || echo 0)
    if [ "$dl" -lt 1000 ]; then rm -f "$out"; fail "heruntergeladene Datei zu klein ($dl bytes, $coll) — verworfen"; fi
    chown "$OWNER_UID:$OWNER_UID" "$out" 2>/dev/null || true

    # 4) Qdrant-internen Snapshot loeschen (kein Ansammeln im Container)
    curl -fsS --max-time 60 -X DELETE "$QDRANT_URL/collections/$coll/snapshots/$name" -H "api-key: $KEY" >/dev/null 2>&1 \
        || log "WARN: interner Snapshot $name ($coll) nicht geloescht"

    # 5) Rotation je Prefix: nur die letzten KEEP behalten (Datumsmuster, damit sich die
    #    Prefixe 'brain' und 'brain-entities' nicht gegenseitig wegrotieren)
    removed=0
    while IFS= read -r f; do
        [ -n "$f" ] || continue
        rm -f "$f" && removed=$((removed+1)) && log "rotiert (geloescht): $(basename "$f")"
    done < <(ls -1t "$DEST/$prefix"-????-??-??.snapshot 2>/dev/null | tail -n +$((KEEP+1)))

    count=$(ls -1 "$DEST/$prefix"-????-??-??.snapshot 2>/dev/null | wc -l)
    log "OK: $(basename "$out") ($dl bytes, API meldete $apisize, Collection $coll) gesichert; $count Snapshot(s) im Ordner (KEEP=$KEEP, $removed rotiert)"
}

snapshot_one "$COLLECTION" "brain"
snapshot_one "$ENTITY_COLLECTION" "brain-entities"

# Frisch erstellte Snapshots sofort ins Google-Drive-Backup spiegeln (best effort; eigenes Logging/Status)
/opt/second-brain/scripts/gedanken-gdrive-backup.sh >/dev/null 2>&1 || true

exit 0

# ── Installation (einmalig, als root auf dem VPS) ───────────────────────────
#   chmod +x /opt/second-brain/scripts/brain-backup.sh
#   crontab:  CRON_TZ=Europe/Berlin
#             0 4 * * *  /opt/second-brain/scripts/brain-backup.sh
