#!/usr/bin/env bash
# gedanken-gdrive-restore.sh — NOTFALL-Wiederherstellung: holt das Google-Drive-Backup zurueck
# auf das Z-Laufwerk (Samba-Freigabe "gedanken"). NUR im Notfall druecken (Server neu/leer).
#
# Bewusst SICHER: rclone COPY (additiv) statt sync -> es wird NICHTS lokal geloescht. Vorhandene
# lokale Dateien bleiben; fehlende werden aus dem Backup ergaenzt. So kann ein Restore nie Daten
# vernichten, nur ergaenzen. Schreibt Status nach $STATUS (vom Dashboard gelesen).

SRC="/srv/samba/gedanken"
REMOTE="${GDRIVE_REMOTE:-gdrive:CortexBackup}"
RCLONE_CONF="${RCLONE_CONF:-/root/.config/rclone/rclone.conf}"
STATUS="$SRC/.gdrive-backup-status.json"
LOCK="/run/gedanken-gdrive-backup.lock"
LOG="/opt/second-brain/brain-logs/gedanken-gdrive-backup.jsonl"
OWNER_UID="${BACKUP_OWNER_UID:-1000}"

mkdir -p "$(dirname "$LOG")" "$SRC" 2>/dev/null || true

logj() { printf '{"ts":"%s","module":"gedanken-gdrive-restore","msg":"%s"}\n' \
         "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" >> "$LOG" 2>/dev/null || true; }

write_status() {  # state, detail, [files]
  local state="$1" detail="$2" files="${3:-}" ts
  ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '{"ts":"%s","state":"%s","detail":"%s","files":"%s","remote":"%s"}\n' \
         "$ts" "$state" "$detail" "$files" "$REMOTE" > "$STATUS" 2>/dev/null || true
  chown "$OWNER_UID:$OWNER_UID" "$STATUS" 2>/dev/null || true
}

exec 9>"$LOCK" 2>/dev/null || true
if command -v flock >/dev/null 2>&1 && ! flock -n 9; then logj "Backup/Restore laeuft bereits"; exit 0; fi

if ! command -v rclone >/dev/null 2>&1; then write_status "fehler" "rclone nicht installiert"; exit 0; fi
RNAME="${REMOTE%%:*}"
if ! rclone --config "$RCLONE_CONF" listremotes 2>/dev/null | grep -q "^${RNAME}:$"; then
  write_status "nicht_eingerichtet" "Google Drive noch nicht verbunden"; exit 0
fi

write_status "restore_laeuft" "Wiederherstellung laeuft (additiv, loescht lokal nichts)..."
logj "Restore start $REMOTE -> $SRC (copy, additiv)"

out=$(rclone --config "$RCLONE_CONF" copy "$REMOTE" "$SRC" \
        --transfers 4 --checkers 8 --log-level INFO 2>&1)
rc=$?

# Wiederhergestellte Dateien dem Samba-Nutzer geben (ueber Z: nutzbar)
chown -R "$OWNER_UID:$OWNER_UID" "$SRC" 2>/dev/null || true
n=$(find "$SRC" -type f 2>/dev/null | wc -l)

if [ "$rc" -eq 0 ]; then
  write_status "restore_ok" "Wiederherstellung abgeschlossen (additiv)" "$n"
  logj "Restore OK ($n Dateien lokal)"
else
  write_status "fehler" "Wiederherstellung fehlgeschlagen (rc=$rc)" "$n"
  logj "Restore FEHLER rc=$rc: $(printf '%s' "$out" | tail -3 | tr '\n' ' ')"
fi
exit 0
