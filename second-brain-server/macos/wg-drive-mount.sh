#!/bin/bash
# wg-drive-mount.sh — macOS-Pendant zu windows/wg-drive-mount.ps1
# Bindet die Gehirn-Laufwerke gedanken (Z) und daten (Y) im Finder ein, sobald der
# WireGuard-Tunnel steht und der SMB-Port 445 von 10.8.0.1 erreichbar ist.
#
# Laeuft als LaunchAgent (Login + alle 5 Min) im NICHT-erhoehten Benutzer-Kontext:
# -> die Laufwerke erscheinen in Franks Finder unter "Speicherorte", OHNE sudo.
# Mountet ueber automountd (osascript "mount volume") -> legt /Volumes/<share> selbst an,
# braucht also KEINE Root-Rechte fuer die Mountpunkte.
#
# Credentials kommen aus ~/SK/second-brain/samba.env (AUSSERHALB des Repos,
# secrets-in-sk-folder-Regel) — User frankmac (eigener Mac-Zugang, Windows bleibt unberuehrt).
set +e

SERVER="10.8.0.1"
SHARES="gedanken daten"
ENVFILE="$HOME/SK/second-brain/samba.env"
LOG="$HOME/Library/Logs/wg-drive-mount.log"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S')  $*" >>"$LOG" 2>/dev/null; }

# Log-Rotation (simpel): ab ~512 KB neu anfangen
if [ -f "$LOG" ] && [ "$(wc -c <"$LOG" 2>/dev/null || echo 0)" -gt 524288 ]; then
  mv -f "$LOG" "$LOG.1" 2>/dev/null
fi

# Credentials laden
if [ ! -f "$ENVFILE" ]; then
  log "FEHLER: $ENVFILE fehlt — kann nicht mounten."
  exit 0
fi
U=$(grep '^SAMBA_USER=' "$ENVFILE" | cut -d= -f2)
P=$(grep '^SAMBA_PASS=' "$ENVFILE" | cut -d= -f2-)
if [ -z "$U" ] || [ -z "$P" ]; then
  log "FEHLER: SAMBA_USER/SAMBA_PASS leer in $ENVFILE."
  exit 0
fi

# Gate: genau den SMB-Port 445 testen (NICHT einen fremden Dienst-Port — Almanach §5/§9).
# Tunnel braucht nach dem Boot ein paar Sekunden; LaunchAgent ruft uns ohnehin alle 5 Min erneut.
if ! nc -z -G 4 "$SERVER" 445 >/dev/null 2>&1; then
  log "SMB 445 ($SERVER) nicht erreichbar — Tunnel noch unten? Abbruch (Retry beim naechsten Lauf)."
  exit 0
fi

for share in $SHARES; do
  # Schon sauber gemountet? -> in Ruhe lassen (kein Blinken)
  if mount | grep -q "@${SERVER}/${share} on "; then
    log "${share}: bereits gemountet — ok"
    continue
  fi
  # Toten/leeren Mountpunkt vom Vorlauf aufraeumen (sonst 'File exists')
  if [ -d "/Volumes/${share}" ] && ! mount | grep -q " /Volumes/${share} "; then
    rmdir "/Volumes/${share}" 2>/dev/null
  fi
  # Mounten via automountd (kein sudo, erscheint im Finder)
  if /usr/bin/osascript -e "mount volume \"smb://${U}:${P}@${SERVER}/${share}\"" >/dev/null 2>&1; then
    log "${share}: gemountet (/Volumes/${share})"
  else
    log "${share}: Mount FEHLGESCHLAGEN"
  fi
done
exit 0
