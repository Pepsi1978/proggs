#!/usr/bin/env bash
# gedanken-gdrive-connect.sh — verbindet rclone mit Google Drive aus einem OAuth-Token, das das
# Dashboard liefert (Frank erzeugt es einmalig per `rclone authorize "drive"` auf seinem PC und fuegt
# es im Cockpit ein). Schreibt die rclone.conf direkt (deterministisch, kein fragiles config-create),
# loescht das Token-File SOFORT wieder (kein Secret-Rest), testet die Verbindung und stoesst ein
# erstes Backup an. Wird von der systemd-.path-Unit ausgeloest, wenn das Dashboard das Token ablegt.

CONTROL="${BACKUP_CONTROL_DIR:-/opt/second-brain/backup-control}"
TOKENFILE="$CONTROL/.rclone-token"
RCLONE_CONF="${RCLONE_CONF:-/root/.config/rclone/rclone.conf}"
REMOTE_NAME="${GDRIVE_REMOTE_NAME:-gdrive}"
STATUS="$CONTROL/.gdrive-backup-status.json"
LOG="/opt/second-brain/brain-logs/gedanken-gdrive-backup.jsonl"

mkdir -p "$(dirname "$RCLONE_CONF")" "$(dirname "$LOG")" "$CONTROL" 2>/dev/null || true

logj() { printf '{"ts":"%s","module":"gedanken-gdrive-connect","msg":"%s"}\n' \
         "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" >> "$LOG" 2>/dev/null || true; }
write_status() {  # state, detail
  printf '{"ts":"%s","state":"%s","detail":"%s","files":"","remote":"%s:CortexBackup"}\n' \
         "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" "$2" "$REMOTE_NAME" > "$STATUS" 2>/dev/null || true
  chmod 644 "$STATUS" 2>/dev/null || true
}

[ -f "$TOKENFILE" ] || { logj "kein Token-File"; exit 0; }
TOKEN="$(cat "$TOKENFILE" 2>/dev/null)"
rm -f "$TOKENFILE" 2>/dev/null || true     # Secret sofort entfernen (nur transient im Steuer-Ordner)

# Token plausibel? (rclone-authorize liefert JSON mit access_token + refresh_token)
if ! printf '%s' "$TOKEN" | grep -q 'access_token'; then
  write_status "fehler" "Token ungueltig — bitte die komplette Ausgabe von 'rclone authorize drive' einfuegen"
  logj "Token ungueltig"; exit 0
fi

# rclone.conf direkt schreiben (deterministisch). Built-in Client-ID reicht fuer privaten Gebrauch.
umask 077
{
  printf '[%s]\n' "$REMOTE_NAME"
  printf 'type = drive\n'
  printf 'scope = drive\n'
  printf 'token = %s\n' "$TOKEN"
} > "$RCLONE_CONF"
chmod 600 "$RCLONE_CONF" 2>/dev/null || true

# Verbindung testen (Wurzel auflisten) — bei Erfolg: Status ok + erstes Backup
if timeout 30 rclone --config "$RCLONE_CONF" lsd "${REMOTE_NAME}:" >/dev/null 2>&1; then
  write_status "ok" "Mit Google Drive verbunden — erstes Backup laeuft"
  logj "verbunden OK"
  /opt/second-brain/scripts/gedanken-gdrive-backup.sh >/dev/null 2>&1 || true
else
  write_status "fehler" "Verbunden, aber Zugriff fehlgeschlagen (Token abgelaufen/Scope?). Bitte neu verbinden."
  logj "lsd-Test FEHLER"
fi
exit 0
