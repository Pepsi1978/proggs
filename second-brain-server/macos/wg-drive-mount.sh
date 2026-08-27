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
# secrets-in-sk-folder-Regel). Aktueller Mac-Zugang: SAMBA_USER=frank (steht wie frankmac in
# valid users beider Freigaben) — siehe README.md, Abschnitt Voraussetzungen.
set +e

SERVER="10.8.0.1"
SHARES="gedanken daten"
ENVFILE="$HOME/SK/second-brain/samba.env"
LOG="$HOME/Library/Logs/wg-drive-mount.log"
PROBE_TIMEOUT=4   # Sekunden — hartes Timeout fuer die Leseprobe eines (evtl. toten) Mounts
export PROBE_TIMEOUT

log() { echo "$(date '+%Y-%m-%d %H:%M:%S')  $*" >>"$LOG" 2>/dev/null; }

# --- Einfach-Lauf erzwingen (2026-08-27): LaunchAgent und ein manueller Aufruf koennen sich
# ueberschneiden -> beide mounten denselben Share und macOS legt /Volumes/<share>-1 an
# (Doppelmount). mkdir ist atomar; ein alter Lock (>10 Min, z.B. nach Absturz) wird gebrochen.
LOCKDIR="$HOME/Library/Caches/wg-drive-mount.lock"
if ! mkdir "$LOCKDIR" 2>/dev/null; then
  if [ -n "$(find "$LOCKDIR" -maxdepth 0 -mmin +10 2>/dev/null)" ]; then
    rmdir "$LOCKDIR" 2>/dev/null; mkdir "$LOCKDIR" 2>/dev/null || { log "Lock belegt — Abbruch."; exit 0; }
  else
    log "Laeuft bereits (Lock) — Abbruch, kein Doppelmount."
    exit 0
  fi
fi
trap 'rmdir "$LOCKDIR" 2>/dev/null' EXIT

# Reine-Bash-URL-Kodierung (Percent-Encoding, KEINE externen Deps). SMB-User/Passwort mit
# Sonderzeichen (@ : / % ...) wuerden die smb://-URL sonst zerbrechen — ein '@' im Passwort z.B.
# beendet den Credential-Teil vorzeitig. LC_ALL=C -> byteweise (auch UTF-8 sauber kodiert).
urlencode() {
  local LC_ALL=C str="$1" i c out=""
  for (( i=0; i<${#str}; i++ )); do
    c="${str:i:1}"
    case "$c" in
      [a-zA-Z0-9._~-]) out="$out$c" ;;
      *) out="$out$(printf '%%%02X' "'$c")" ;;
    esac
  done
  printf '%s' "$out"
}

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
# Fuer die smb://-URL kodieren (Sonderzeichen sicher), Roh-Werte bleiben fuer Vergleiche/Logs unberuehrt.
UENC="$(urlencode "$U")"
PENC="$(urlencode "$P")"

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
  if /usr/bin/osascript -e "mount volume \"smb://${UENC}:${PENC}@${SERVER}/${share}\"" >/dev/null 2>&1; then
    log "${share}: gemountet (${MP})"
  else
    log "${share}: Mount FEHLGESCHLAGEN"
  fi
done
exit 0
