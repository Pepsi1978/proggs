#!/bin/bash
# wg-drive-mount.sh — macOS-Pendant zu windows/wg-drive-mount.ps1
# Bindet die Gehirn-Laufwerke gedanken (Z) und daten (Y) im Finder ein, sobald der
# WireGuard-Tunnel steht und der SMB-Port 445 von 10.8.0.1 erreichbar ist.
#
# RESILIENZ (Direktive #3, 2026-06-29): Nach einem kurzen Tunnel-Aussetzer (z.B.
# Telekom-IP-Wechsel/Reconnect der Internetleitung — der VPS war beim Vorfall am
# 2026-06-29 21:19 nachweislich kerngesund, sar belegte CPU idle 98%, eth0 0 Fehler)
# wirft macOS die SMB-Mounts ab ODER laesst einen TOTEN, haengenden Mount zurueck.
# Frueher meldete das Skript dann blind "bereits gemountet — ok" und die Laufwerke
# blieben bis zu 5 Min weg. JETZT:
#   1. Stale-Erkennung: aktive Leseprobe mit HARTEM Timeout (perl-alarm, kein 'timeout'
#      auf macOS). Ein toter Mount wird erkannt -> zwangsweise geloest + sofort neu gemountet.
#   2. Observability: ist der Tunnel unten, wird die oeffentliche IP mitgeloggt -> beim
#      naechsten Vorfall ist beweisbar, ob die Telekom-IP gewechselt hat.
#
# Laeuft als LaunchAgent (Login + periodisch) im NICHT-erhoehten Benutzer-Kontext:
# -> die Laufwerke erscheinen in Franks Finder unter "Speicherorte", OHNE sudo.
# Mountet ueber automountd (osascript "mount volume") -> legt /Volumes/<share> selbst an,
# braucht also KEINE Root-Rechte fuer die Mountpunkte. (mount_smbfs scheidet bewusst aus:
# es braeuchte sudo fuer mkdir in /Volumes -- /Volumes gehoert root -- und erschiene nicht
# automatisch im Finder. Das waere eine Regression.) Die SMB-Haertung (soft mounts, kurze
# Timeouts, damit ein toter Mount nicht ewig haengt) kommt aus nsmb.conf, nicht aus dem
# Mount-Befehl -- so bleibt der osascript-Weg (no-sudo + Finder) unveraendert erhalten.
#
# Credentials kommen aus ~/SK/second-brain/samba.env (AUSSERHALB des Repos,
# secrets-in-sk-folder-Regel) — User frankmac (eigener Mac-Zugang, Windows bleibt unberuehrt).
set +e

SERVER="10.8.0.1"
SHARES="gedanken daten"
ENVFILE="$HOME/SK/second-brain/samba.env"
LOG="$HOME/Library/Logs/wg-drive-mount.log"
PROBE_TIMEOUT=4   # Sekunden — hartes Timeout fuer die Leseprobe eines (evtl. toten) Mounts
export PROBE_TIMEOUT

log() { echo "$(date '+%Y-%m-%d %H:%M:%S')  $*" >>"$LOG" 2>/dev/null; }

# Log-Rotation (simpel): ab ~512 KB neu anfangen
if [ -f "$LOG" ] && [ "$(wc -c <"$LOG" 2>/dev/null || echo 0)" -gt 524288 ]; then
  mv -f "$LOG" "$LOG.1" 2>/dev/null
fi

# --- Lebt der Mountpunkt WIRKLICH? Ein toter SMB-Mount haengt beim Zugriff sonst ewig. ---
# perl ist auf macOS immer vorhanden; alarm() erzwingt ein hartes Timeout.
# exit 0 = lesbar, exit 1 = tot/Timeout/nicht oeffenbar.
mount_alive() {
  perl -e '
    eval {
      local $SIG{ALRM} = sub { die "timeout\n" };
      alarm($ENV{PROBE_TIMEOUT} || 4);
      opendir(my $dh, $ARGV[0]) or die "noopen\n";
      readdir($dh);
      closedir($dh);
      alarm(0);
    };
    exit($@ ? 1 : 0);
  ' "$1"
}

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
# Tunnel braucht nach dem Boot ein paar Sekunden; LaunchAgent ruft uns ohnehin periodisch erneut.
if ! nc -z -G 4 "$SERVER" 445 >/dev/null 2>&1; then
  # Observability (observability-first): bei Aussetzer die oeffentliche IP festhalten ->
  # beim naechsten Vorfall sieht man, ob die Telekom-IP/das NAT gewechselt hat.
  PUBIP=$(curl -s --max-time 4 https://api.ipify.org 2>/dev/null || echo "?")
  log "SMB 445 ($SERVER) nicht erreichbar — Tunnel unten? (oeffentliche IP jetzt: ${PUBIP}). Abbruch (Retry beim naechsten Lauf)."
  exit 0
fi

for share in $SHARES; do
  MP="/Volumes/${share}"
  if mount | grep -q "@${SERVER}/${share} on "; then
    # Laut Kernel gemountet — aber LEBT er noch? (toter Mount nach Tunnel-Aussetzer)
    if mount_alive "$MP"; then
      log "${share}: bereits gemountet und lesbar — ok"
      continue
    fi
    # Toter/haengender Mount -> zwangsweise loesen, dann unten neu aufbauen.
    log "${share}: Mount vorhanden aber TOT (Leseprobe >${PROBE_TIMEOUT}s) — loese + remounte."
    diskutil unmount force "$MP" >/dev/null 2>&1 || umount -f "$MP" >/dev/null 2>&1
  fi
  # Toten/leeren Mountpunkt vom Vorlauf aufraeumen (sonst 'File exists')
  if [ -d "$MP" ] && ! mount | grep -q " ${MP} "; then
    rmdir "$MP" 2>/dev/null
  fi
  # Mounten via automountd (kein sudo, erscheint im Finder)
  if /usr/bin/osascript -e "mount volume \"smb://${U}:${P}@${SERVER}/${share}\"" >/dev/null 2>&1; then
    log "${share}: gemountet (${MP})"
  else
    log "${share}: Mount FEHLGESCHLAGEN"
  fi
done
exit 0
