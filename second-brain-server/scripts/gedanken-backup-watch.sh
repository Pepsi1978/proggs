#!/usr/bin/env bash
# gedanken-backup-watch.sh — beobachtet das Z-Laufwerk (Samba-Freigabe "gedanken") und stoesst
# bei JEDEM Schreiben (neuer Logbuch-Eintrag, neuer Snapshot, neue Datei) das Google-Drive-Backup an.
# Gebuendelt (debounce): nach der letzten Aenderung wird DEBOUNCE Sekunden gewartet, dann EIN Sync —
# so wird bei vielen schnellen Schreibvorgaengen nicht dutzendfach synchronisiert.
#
# Laeuft als systemd-Dienst (Restart=always). Eigene Steuer-/Status-Dotfiles sind ausgeschlossen,
# damit das Schreiben des Status nicht den Watcher erneut ausloest (kein Loop).

SRC="/srv/samba/gedanken"
BACKUP="/opt/second-brain/scripts/gedanken-gdrive-backup.sh"
DEBOUNCE="${BACKUP_DEBOUNCE:-20}"

# inotifywait vorhanden? Fehlt inotify-tools, scheitert der Watcher sofort — und systemd
# (Restart=always, RestartSec=10) startet ihn STUMM alle 10 s neu (Log-Flut + CPU-Last, ohne dass
# jemand die Ursache sieht). Darum: klare Meldung ins journald, 5 min pausieren (Restart-Sturm
# daempfen), dann kontrolliert raus.
if ! command -v inotifywait >/dev/null 2>&1; then
  echo "FEHLER: inotifywait fehlt — Paket 'inotify-tools' installieren (apt-get install -y inotify-tools). Watcher pausiert 300s." >&2
  sleep 300
  exit 1
fi

# Warten, bis der Mount/Ordner da ist (z.B. nach Reboot)
while [ ! -d "$SRC" ]; do sleep 5; done

# inotifywait: rekursiv, nur Schreib-/Erstell-/Verschiebe-Ereignisse, eigene Dotfiles ausgeschlossen.
inotifywait -m -r -q \
  -e close_write -e create -e moved_to -e delete -e move \
  --exclude '(\.gdrive-backup-status\.json|\.backup-trigger|\.restore-trigger|\.swp$|~$)' \
  "$SRC" | while read -r _line; do
    # Debounce: nach einer Aenderung kurz sammeln; weitere Events innerhalb DEBOUNCE "verschlucken".
    end=$(( $(date +%s) + DEBOUNCE ))
    while [ "$(date +%s)" -lt "$end" ]; do
      if read -t 1 -r _more; then end=$(( $(date +%s) + DEBOUNCE )); fi
    done
    bash "$BACKUP" >/dev/null 2>&1 || true
  done
