#!/bin/bash
# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Ein Overlay über jeder App bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Ohne Antwort wird nach 30 Sekunden installiert. Die APK liegt schon vor der Frage in einer Installationssitzung am
# Handy, damit „Sofort“ wirklich sofort installiert: Nach der Antwort fehlt nur noch das Abschließen der Sitzung.
# Jarvis verlangt seinen Pflicht-Teil (assets/freigabe-teil.apk in der APK): Nacktes adb install lehnt Android deshalb
# mit INSTALL_FAILED_MISSING_SPLIT ab, nur dieses Skript und UpdateStation installieren ihn mit und stellen die Frage.
# Aufruf: jarvis-install.sh <apk> [-s <gerät>]
# Kann das Overlay nicht erscheinen, wird nicht installiert.
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert oder fehler.
set -u
PAKET=de.frank.jarvis
FENSTER=$PAKET/.ui.InstallFrageDienst
ANTWORT=/sdcard/Android/data/$PAKET/files/install-antwort.txt
# Am Handy immer nur ein Lauf: Zwischendatei, Antwortdatei und Overlay gibt es dort je nur einmal, auch über mehrere Rechner hinweg.
SPERRE=/data/local/tmp/jarvis-install.sperre
SPERRE_FRIST_S=900
SPERRE_MEIN=""
ZWISCHENLAGER=/data/local/tmp/jarvis-install.apk
TEIL_IM_APK=assets/freigabe-teil.apk
# Frist des Overlays plus Reserve; sie zählt ab seiner Meldung „offen“, damit jeder Tipp im sichtbaren Fenster noch gilt.
WARTEZEIT_S=33
MELDEFRIST_S=20
WLAN_SKRIPT="$(cd "$(dirname "$0")/.." && pwd)/adb-wlan/adb-wlan.sh"
# Delta-Übertragung: nur geänderte Einträge durch den Tunnel, Rest aus der installierten APK am Handy.
source "$(cd "$(dirname "$0")/.." && pwd)/apk-delta/delta-push.sh"

ende() { echo "JARVIS_INSTALL_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }

APK=""; GERAET=""
while [ $# -gt 0 ]; do
  case "$1" in
    -s) GERAET="${2:-}"; shift 2 ;;
    *) APK="$1"; shift ;;
  esac
done
[ -f "$APK" ] || ende fehler "APK nicht gefunden: $APK"

for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -n "${ADB_BIN:-}" ] || ende fehler "adb nicht gefunden"

# WLAN oder Tunnel (IP:Port) vor Kabel; ohne Verbindung einmal das WLAN-Werkzeug fragen.
waehle_geraet() {
  local netz
  netz=$("$ADB_BIN" devices | awk '$2=="device" && $1 ~ /:[0-9]+$/ {print $1; exit}')
  [ -n "$netz" ] && { echo "$netz"; return; }
  "$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}
verbinde() {
  if [ -n "$GERAET" ] && [ "$("$ADB_BIN" -s "$GERAET" get-state 2>/dev/null)" = device ]; then return 0; fi
  GERAET=""
  GERAET=$(waehle_geraet)
  [ -n "$GERAET" ] && return 0
  "$WLAN_SKRIPT" >/dev/null 2>&1
  GERAET=$(waehle_geraet)
  [ -n "$GERAET" ]
}
handy() { "$ADB_BIN" -s "$GERAET" shell "$@" 2>&1 | tr -d '\r'; }
# Über den langsamen Tunnel spart lz4 rund ein Sechstel der Übertragungszeit (unkomprimierte .so und Dex-Daten).
# Ältere adb ohne -z: einfach ohne Kompression.
schiebe() { "$ADB_BIN" -s "$GERAET" push -z lz4 "$1" "$2" >/dev/null 2>&1 || "$ADB_BIN" -s "$GERAET" push "$1" "$2" >/dev/null 2>&1; }

SITZUNG=""
ABGESCHLOSSEN=""
SERIE=""
TEIL_DATEI=""
# Auch bei Abbruch: Die Sitzung hält sonst die ganze APK am Handy, bis Android sie Tage später entsorgt.
aufraeumen() {
  # Nach einem Verbindungsabriss erst neu verbinden; auf einem anderen Gerät gehört uns dort nichts.
  [ -n "$SERIE" ] || { [ -n "$TEIL_DATEI" ] && rm -f "$TEIL_DATEI"; return 0; }
  if { [ -n "$GERAET" ] || verbinde; } && [ "$(handy getprop ro.serialno)" = "$SERIE" ] && [ -n "$SPERRE_MEIN" ]; then
    [ -n "$SITZUNG" ] && [ -z "$ABGESCHLOSSEN" ] && handy pm install-abandon "$SITZUNG" >/dev/null
    handy rm -f "$ZWISCHENLAGER" "$ANTWORT" >/dev/null
    handy rmdir "$SPERRE" >/dev/null
  fi
  [ -n "$TEIL_DATEI" ] && rm -f "$TEIL_DATEI"
}
trap aufraeumen EXIT
trap 'exit 130' INT TERM

nimm_sperre() {
  local bis alter jetzt
  bis=$(( $(date +%s) + SPERRE_FRIST_S + 60 ))
  while [ -n "$(handy mkdir "$SPERRE")" ]; do
    # Ein abgestürzter Lauf hinterlässt die Sperre: Älter als jeder echte Lauf heißt verwaist.
    alter=$(handy stat -c %Y "$SPERRE")
    jetzt=$(handy date +%s)
    # Gerade freigegeben oder Lesefehler: Dann ist hier nichts verwaist.
    case "$alter$jetzt" in ''|*[!0-9]*) alter=$jetzt ;; esac
    case "$jetzt" in ''|*[!0-9]*) alter=0; jetzt=0 ;; esac
    [ $(( jetzt - alter )) -gt "$SPERRE_FRIST_S" ] && { handy rmdir "$SPERRE" >/dev/null; continue; }
    [ "$(date +%s)" -ge "$bis" ] && ende fehler "Eine andere Jarvis-Installation blockiert das Handy"
    [ -z "${SPERRE_GEMELDET:-}" ] && { echo "Eine andere Jarvis-Installation läuft, warte ..."; SPERRE_GEMELDET=1; }
    sleep 3
    verbinde || ende fehler "Handy nicht erreichbar"
  done
  SPERRE_MEIN=1
  handy rm -f '/data/local/tmp/jarvis-install*.apk' >/dev/null
}
bereite_vor() {
  echo "Lade die APK vorab aufs Handy über $GERAET ..."
  delta_push "$ADB_BIN" "$GERAET" "$APK" "$ZWISCHENLAGER" "$PAKET" || ende fehler "APK ließ sich nicht aufs Handy laden"
  local groesse ausgabe teil_groesse=0
  groesse=$(wc -c < "$APK" | tr -d ' ')
  # Ältere Jarvis-APKs haben den Teil noch nicht und brauchen ihn auch nicht.
  if unzip -l "$APK" "$TEIL_IM_APK" >/dev/null 2>&1; then
    TEIL_DATEI=$(mktemp) || ende fehler "Keine Temp-Datei für den Pflicht-Teil"
    unzip -p "$APK" "$TEIL_IM_APK" > "$TEIL_DATEI" || ende fehler "Pflicht-Teil ließ sich nicht aus der APK holen"
    teil_groesse=$(wc -c < "$TEIL_DATEI" | tr -d ' ')
  fi
  ausgabe=$(handy pm install-create -r -S "$(( groesse + teil_groesse ))")
  SITZUNG=$(echo "$ausgabe" | sed -n 's/.*\[\([0-9]*\)\].*/\1/p')
  [ -n "$SITZUNG" ] || ende fehler "Installationssitzung ließ sich nicht anlegen: $ausgabe"
  ausgabe=$(handy pm install-write -S "$groesse" "$SITZUNG" base.apk "$ZWISCHENLAGER")
  handy rm -f "$ZWISCHENLAGER" >/dev/null
  echo "$ausgabe" | grep -q '^Success' || ende fehler "APK ließ sich nicht in die Sitzung schreiben: $ausgabe"
  if [ -n "$TEIL_DATEI" ]; then
    schiebe "$TEIL_DATEI" "$ZWISCHENLAGER" || ende fehler "Pflicht-Teil ließ sich nicht aufs Handy laden"
    ausgabe=$(handy pm install-write -S "$teil_groesse" "$SITZUNG" freigabe.apk "$ZWISCHENLAGER")
    handy rm -f "$ZWISCHENLAGER" >/dev/null
    echo "$ausgabe" | grep -q '^Success' || ende fehler "Pflicht-Teil ließ sich nicht in die Sitzung schreiben: $ausgabe"
  fi
}
installiere() {
  verbinde || ende fehler "Handy nicht erreichbar"
  # Nach einem Verbindungsabriss kann die Adresse wechseln (WLAN, Tunnel); die Sitzung gilt aber nur auf diesem Handy.
  [ "$(handy getprop ro.serialno)" = "$SERIE" ] || ende fehler "anderes Gerät verbunden, Sitzung gehört zu $SERIE"
  local ausgabe
  ausgabe=$(handy pm install-commit "$SITZUNG")
  echo "$ausgabe" | tail -1
  if echo "$ausgabe" | grep -q '^Success'; then
    ABGESCHLOSSEN=1
    ende installiert "$GERAET"
  fi
  ende fehler "Installation gescheitert: $(echo "$ausgabe" | tail -1)"
}

verbinde || ende fehler "Handy nicht erreichbar"
SERIE=$(handy getprop ro.serialno)
nimm_sperre
bereite_vor

KENNUNG="$(date +%s)-$$"
handy rm -f "$ANTWORT" >/dev/null
START=$(handy am start-foreground-service -n "$FENSTER" --es kennung "$KENNUNG")
if echo "$START" | grep -q "Not found"; then
  # Die installierte Version kennt das Overlay noch nicht: nur dann ohne Rückfrage.
  echo "Installierte Jarvis-Version hat das Overlay noch nicht, installiere ohne Rückfrage."
  installiere
fi
echo "$START" | grep -q "^Error" && ende fehler "Overlay ließ sich nicht öffnen: $(echo "$START" | grep -m1 '^Error')"

MELDUNG_BIS=$(( $(date +%s) + MELDEFRIST_S ))
BIS=""
WAHL=""
while :; do
  JETZT=$(date +%s)
  [ -n "$BIS" ] && [ "$JETZT" -ge "$BIS" ] && break
  [ -z "$BIS" ] && [ "$JETZT" -ge "$MELDUNG_BIS" ] && ende fehler "Overlay hat sich nicht gemeldet, nicht installiert"
  ZEILE=$(handy cat "$ANTWORT")
  case "$ZEILE" in
    "$KENNUNG offen")
      if [ -z "$BIS" ]; then
        BIS=$(( JETZT + WARTEZEIT_S ))
        echo "Overlay geöffnet, warte auf Antwort ..."
      fi ;;
    "$KENNUNG fehler") ende fehler "Overlay konnte nicht erscheinen, nicht installiert" ;;
    "$KENNUNG "*) WAHL="${ZEILE#* }"; break ;;
  esac
  sleep 0.2
done

case "$WAHL" in
  "") echo "Keine Antwort: installiere jetzt." ;;
  0) echo "Gewählt: Sofort" ;;
  [1-5]) echo "Gewählt: in $WAHL Minute(n), Installation um $(date -v+"${WAHL}"M +%H:%M 2>/dev/null || date -d "+$WAHL minutes" +%H:%M) Uhr"; sleep $(( WAHL * 60 )) ;;
  *) ende fehler "unbekannte Antwort: $WAHL" ;;
esac
installiere
