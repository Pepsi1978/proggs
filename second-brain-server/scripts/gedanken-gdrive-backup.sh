#!/usr/bin/env bash
# gedanken-gdrive-backup.sh — spiegelt das komplette Z-Laufwerk (Samba-Freigabe "gedanken":
# Logbuch + qdrant-snapshot + alles Neue) nach Google Drive. ECHTER Spiegel (sync), aber CRASH-SICHER.
#
# Frank-Wunsch 2026-06-24: ein echter Sink/Spiegel (nichts summiert sich auf, was nicht auf Z liegt,
# ist auch nicht im Backup; Snapshots bleiben bei 14 wie auf Z). ABER: ein Server-Crash / leeres Z
# darf das Backup NIEMALS loeschen — sonst ist das Backup genau dann weg, wenn man es braucht.
#
# DREI SCHUTZSCHICHTEN:
#   1) Quelle-Bereit-Check (Hauptschutz): ist Z eingehaengt UND nicht leer (Pflichtordner mit Inhalt)?
#      Wenn nicht -> ABBRUCH, Backup wird NICHT angefasst. (Faengt Crash/Mount-weg ab.)
#   2) Loesch-Obergrenze (--max-delete): rclone bricht ab, wenn es mehr als N Dateien loeschen wuerde.
#   3) Google-Drive-Papierkorb (rclone-Default): Geloeschtes ist ~30 Tage wiederherstellbar.
#
# Schreibt Status nach $STATUS (vom Dashboard gelesen). Stirbt nie hart (immer exit 0 mit Status).
# Observability-First: JSON-Status + Log.

SRC="/srv/samba/gedanken"
REMOTE="${GDRIVE_REMOTE:-gdrive:CortexBackup}"
RCLONE_CONF="${RCLONE_CONF:-/root/.config/rclone/rclone.conf}"
STATUS="$SRC/.gdrive-backup-status.json"
LOCK="/run/gedanken-gdrive-backup.lock"
LOG="/opt/second-brain/brain-logs/gedanken-gdrive-backup.jsonl"
MAX_DELETE="${GDRIVE_MAX_DELETE:-25}"     # Schicht 2: nie mehr als so viele Dateien pro Lauf loeschen
OWNER_UID="${BACKUP_OWNER_UID:-1000}"     # frank (Samba) — Status-Datei ueber Z: lesbar
# Dateien, die NICHT mitgespiegelt werden (eigene Steuer-/Status-Dateien)
EXCLUDES=(--exclude ".gdrive-backup-status.json" --exclude ".backup-trigger" --exclude ".restore-trigger")

mkdir -p "$(dirname "$LOG")" 2>/dev/null || true

logj() { printf '{"ts":"%s","module":"gedanken-gdrive-backup","msg":"%s"}\n' \
         "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" >> "$LOG" 2>/dev/null || true; }

write_status() {  # state, detail, [files]
  local state="$1" detail="$2" files="${3:-}" ts
  ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '{"ts":"%s","state":"%s","detail":"%s","files":"%s","remote":"%s"}\n' \
         "$ts" "$state" "$detail" "$files" "$REMOTE" > "$STATUS" 2>/dev/null || true
  chown "$OWNER_UID:$OWNER_UID" "$STATUS" 2>/dev/null || true
}

# Nur eine Instanz gleichzeitig (Watcher + Timer + Button koennen sich ueberlappen)
exec 9>"$LOCK" 2>/dev/null || true
if command -v flock >/dev/null 2>&1 && ! flock -n 9; then
  logj "laeuft bereits, uebersprungen"; exit 0
fi

# rclone vorhanden?
if ! command -v rclone >/dev/null 2>&1; then
  write_status "fehler" "rclone nicht installiert"; logj "rclone fehlt"; exit 0
fi

# Konfiguriert? (Remote in rclone.conf vorhanden) — sonst "nicht eingerichtet" (Frank muss Browser-Login machen)
RNAME="${REMOTE%%:*}"
if ! rclone --config "$RCLONE_CONF" listremotes 2>/dev/null | grep -q "^${RNAME}:$"; then
  write_status "nicht_eingerichtet" "Google Drive noch nicht verbunden (einmaliger Login fehlt)"
  logj "remote $RNAME nicht konfiguriert"; exit 0
fi

# ── SCHICHT 1: Quelle-Bereit-Check ──────────────────────────────────────────
# Z muss eingehaengt sein UND die Pflichtordner mit Inhalt haben. Sonst: NICHT anfassen.
ready=1
[ -d "$SRC" ] || ready=0
[ -d "$SRC/Logbuch" ] || ready=0
[ -d "$SRC/qdrant-snapshot" ] || ready=0
# mind. EINE echte Nutzdatei (ohne unsere Steuer-Dotfiles) muss existieren
realfiles=$(find "$SRC" -type f ! -name ".gdrive-backup-status.json" ! -name ".backup-trigger" \
            ! -name ".restore-trigger" 2>/dev/null | head -1 | wc -l)
[ "${realfiles:-0}" -ge 1 ] || ready=0
if [ "$ready" -ne 1 ]; then
  write_status "uebersprungen" "Quelle nicht bereit (Z leer oder nicht eingehaengt) — Backup unangetastet"
  logj "Quelle nicht bereit -> SCHUTZ: kein Sync"; exit 0
fi

write_status "laeuft" "Spiegelung laeuft..."
logj "Sync start $SRC -> $REMOTE (max-delete=$MAX_DELETE)"

# ── SCHICHT 2 + 3: echter Spiegel, Loesch-Obergrenze, Drive-Papierkorb (Default an) ──
out=$(rclone --config "$RCLONE_CONF" sync "$SRC" "$REMOTE" \
        "${EXCLUDES[@]}" \
        --max-delete "$MAX_DELETE" \
        --create-empty-src-dirs \
        --transfers 4 --checkers 8 \
        --log-level INFO 2>&1)
rc=$?

n=$(find "$SRC" -type f ! -name ".gdrive-backup-status.json" ! -name ".backup-trigger" \
    ! -name ".restore-trigger" 2>/dev/null | wc -l)

if [ "$rc" -eq 0 ]; then
  write_status "ok" "Spiegel aktuell" "$n"
  logj "Sync OK ($n Dateien)"
else
  # rc!=0: u.a. wenn die Loesch-Obergrenze gegriffen hat (Schutz) -> Backup bleibt unangetastet
  detail="Sync fehlgeschlagen (rc=$rc) — evtl. Loesch-Obergrenze (Schutz) oder Netzfehler"
  write_status "fehler" "$detail" "$n"
  logj "Sync FEHLER rc=$rc: $(printf '%s' "$out" | tail -3 | tr '\n' ' ')"
fi
exit 0
