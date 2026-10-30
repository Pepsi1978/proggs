#!/bin/bash
# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Ein Overlay über jeder App bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Ohne Antwort wird nach 30 Sekunden installiert. Die APK liegt schon vor der Frage in einer Installationssitzung am
# Handy, damit „Sofort“ wirklich sofort installiert: Nach der Antwort fehlt nur noch das Abschließen der Sitzung.
# Aufruf: jarvis-install.sh <apk> [-s <gerät>]
# Kann das Overlay nicht erscheinen, wird nicht installiert.
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert oder fehler.
set -u
PAKET=de.frank.jarvis
FENSTER=$PAKET/.ui.InstallFrageDienst
ANTWORT=/sdcard/Android/data/$PAKET/files/install-antwort.txt
ZWISCHENLAGER=/data/local/tmp/jarvis-install.apk
# Frist des Overlays plus Reserve; sie zählt ab seiner Meldung „offen“, damit jeder Tipp im sichtbaren Fenster noch gilt.
WARTEZEIT_S=33
MELDEFRIST_S=20
WLAN_SKRIPT="$(cd "$(dirname "$0")/.." && pwd)/adb-wlan/adb-wlan.sh"

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

SITZUNG=""
ABGESCHLOSSEN=""
SERIE=""
# Auch bei Abbruch: Die Sitzung hält sonst die ganze APK am Handy, bis Android sie Tage später entsorgt.
aufraeumen() {
  [ -n "$GERAET" ] || return 0
  [ -n "$SITZUNG" ] && [ -z "$ABGESCHLOSSEN" ] && handy pm install-abandon "$SITZUNG" >/dev/null
  handy rm -f "$ZWISCHENLAGER" "$ANTWORT" >/dev/null
}
trap aufraeumen EXIT
trap 'exit 130' INT TERM

bereite_vor() {
  echo "Lade die APK vorab aufs Handy über $GERAET ..."
  "$ADB_BIN" -s "$GERAET" push "$APK" "$ZWISCHENLAGER" >/dev/null 2>&1 || ende fehler "APK ließ sich nicht aufs Handy laden"
  local groesse ausgabe
  groesse=$(wc -c < "$APK" | tr -d ' ')
  ausgabe=$(handy pm install-create -r -S "$groesse")
  SITZUNG=$(echo "$ausgabe" | sed -n 's/.*\[\([0-9]*\)\].*/\1/p')
  [ -n "$SITZUNG" ] || ende fehler "Installationssitzung ließ sich nicht anlegen: $ausgabe"
  ausgabe=$(handy pm install-write -S "$groesse" "$SITZUNG" base.apk "$ZWISCHENLAGER")
  handy rm -f "$ZWISCHENLAGER" >/dev/null
  echo "$ausgabe" | grep -q '^Success' || ende fehler "APK ließ sich nicht in die Sitzung schreiben: $ausgabe"
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
