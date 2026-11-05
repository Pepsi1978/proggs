#!/usr/bin/env bash
# Liest Logcat vom Handy schnell und gefiltert, auch über WLAN/Tunnel.
#
# Warum: `adb logcat -d | grep …` zieht den GANZEN Puffer (oft 10–50 MB) über die Leitung und filtert erst
# hier. Über den Tunnel dauert das Minuten und endet im Zeitlimit. Dieses Skript filtert AUF DEM HANDY
# (Zeitfenster, Prozess, Tags, Muster, Zeilenzahl) und überträgt nur das Ergebnis. Harte Zeitgrenze, nie hängend.
#
# Aufruf:
#   logcat-lesen.sh [--paket de.frank.app] [--tags "TagA,TagB"] [--muster "regex"] [--minuten 60]
#                   [--zeilen 300] [--puffer main,system,crash] [--serial IP:5555] [--zeitlimit 60]
# Beispiele:
#   logcat-lesen.sh --tags "WeckerLifecycle,WeckerPlayback" --minuten 120
#   logcat-lesen.sh --muster "genialerwecker|FATAL" --minuten 30
#   logcat-lesen.sh --puffer crash --minuten 1440          # Abstürze des letzten Tages
#   logcat-lesen.sh --paket de.frank.jarvis --minuten 10   # nur der laufende Prozess der App
# Letzte Zeile: LOGCAT_STATUS=ok|leer|zeitlimit|fehler (<Grund>)
set -u
PAKET=""; TAGS=""; MUSTER=""; MINUTEN=60; ZEILEN=300; PUFFER="main,system,crash"; SERIAL=""; LIMIT=60
while [ $# -gt 0 ]; do
  case "$1" in
    --paket) PAKET="$2"; shift 2;;
    --tags) TAGS="$2"; shift 2;;
    --muster) MUSTER="$2"; shift 2;;
    --minuten) MINUTEN="$2"; shift 2;;
    --zeilen) ZEILEN="$2"; shift 2;;
    --puffer) PUFFER="$2"; shift 2;;
    --serial) SERIAL="$2"; shift 2;;
    --zeitlimit) LIMIT="$2"; shift 2;;
    -h|--help) sed -n 2,18p "$0"; exit 0;;
    *) echo "Unbekannter Schalter: $1"; echo "LOGCAT_STATUS=fehler (Aufruf)"; exit 2;;
  esac
done
case "$MINUTEN$ZEILEN$LIMIT" in *[!0-9]*) echo "LOGCAT_STATUS=fehler (--minuten/--zeilen/--zeitlimit nur Zahlen)"; exit 2;; esac
HIER="$(cd "$(dirname "$0")" && pwd)"

# Gerät wählen: WLAN/Tunnel (IP:5555) vor Kabel; sonst einmal verbinden.
waehle() { adb devices 2>/dev/null | awk 'NR>1 && $2=="device"{print $1}' | sort -t: -k2 -r | head -1; }
if [ -z "$SERIAL" ]; then
  SERIAL="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" && $1 ~ /:5555$/ {print $1; exit}')"
  [ -z "$SERIAL" ] && { timeout 60 bash "$HIER/../adb-wlan/adb-wlan.sh" >/dev/null 2>&1; SERIAL="$(waehle)"; }
fi
[ -z "$SERIAL" ] && { echo "LOGCAT_STATUS=fehler (kein Handy erreichbar)"; exit 1; }

# Einzelne Anführungszeichen für die Handy-Shell sicher einbetten.
q() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"; }

FILTER=""
if [ -n "$TAGS" ]; then
  for t in ${TAGS//,/ }; do FILTER="$FILTER $(q "$t:V")"; done
  FILTER="$FILTER '*:S'"
fi
PUFFERARG=""; for b in ${PUFFER//,/ }; do PUFFERARG="$PUFFERARG -b $b"; done

REMOTE="T=\$(date -d @\$(( \$(date +%s) - $MINUTEN*60 )) '+%m-%d %H:%M:%S.000');"
if [ -n "$PAKET" ]; then
  REMOTE="$REMOTE P=\$(pidof $(q "$PAKET") | cut -d' ' -f1); if [ -z \"\$P\" ]; then echo 'HINWEIS: $PAKET läuft gerade nicht, Filter nur über Tags/Muster.' >&2; PID=''; else PID=\"--pid=\$P\"; fi;"
else
  REMOTE="$REMOTE PID='';"
fi
REMOTE="$REMOTE logcat -d -v time$PUFFERARG -T \"\$T\" \$PID$FILTER"
[ -n "$MUSTER" ] && REMOTE="$REMOTE | grep -iE $(q "$MUSTER")"
REMOTE="$REMOTE | grep -v 'adbd service requested' | tail -n $ZEILEN"

AUS="$(timeout "$LIMIT" adb -s "$SERIAL" shell "$REMOTE" 2>&1)"; RC=$?
if [ $RC -eq 124 ]; then
  printf '%s\n' "$AUS"
  echo "LOGCAT_STATUS=zeitlimit (nach ${LIMIT}s; Zeitfenster/Filter enger wählen)"; exit 1
fi
printf '%s\n' "$AUS"
if printf '%s' "$AUS" | grep -qE '^[0-9]{2}-[0-9]{2} '; then echo "LOGCAT_STATUS=ok ($SERIAL, letzte $MINUTEN min)"
elif [ $RC -ne 0 ] && [ -n "$AUS" ] && ! printf '%s' "$AUS" | grep -q '^HINWEIS'; then echo "LOGCAT_STATUS=fehler (adb rc=$RC)"; exit 1
else echo "LOGCAT_STATUS=leer (keine passenden Zeilen in den letzten $MINUTEN min; Puffer evtl. schon überschrieben)"; fi
