#!/usr/bin/env bash
# Richtet „Jarvis Tablet“ (Galaxy Tab S9) als Zwilling des Handys ein, einmalig oder zum Nachziehen.
# Voraussetzung: Auf beiden Geräten ist dieselbe Jarvis-Version (ab 1.8.604) installiert (Debug-Bau, deshalb geht run-as),
# beide Geräte sind per adb erreichbar (Werkzeuge/adb-wlan/geraete.json).
#
# Schritte (einzeln wählbar mit --nur <schritt>, sonst beide):
#   rechte   Berechtigungen und Sonderzugriffe für Jarvis und die angebundenen Apps auf dem Tablet
#            (Mikrofon, Kalender, Standort, Overlay, Benachrichtigungen, Akku-Ausnahme)
#   umzug    alle Einstellungen und Zugangsschlüssel vom Handy (data/Umzug.kt) und der Zugang zur Fernsteuerung der Rechner;
#            die Dateien reisen per run-as von Gerät zu Gerät und liegen nie auf dem Rechner. Nicht dabei: Plugin-Geheimnis,
#            ChatGPT-Anmeldung, Anthropic-Sitzungen. Die vier Tablet-Schalter (Auswertung, Lernen, Losfahren, Weckwort) bleiben,
#            wie sie am Tablet stehen.
#
# Daten kopiert dieses Skript nicht mehr: Jarvis' Gedächtnis und Gespräche sowie die Daten von Aufgaben, Ziele, Geniale Ideen,
# Epigenetik und Stack-Labor holt sich das Tablet über den Geräteabgleich selbst vom Handy (README „Jarvis Tablet“), sobald
# Jarvis dort läuft. Eine Dateikopie laufender Datenbanken wäre dagegen unsicher (halb geschriebene Stände), und der Geniale
# Wecker soll nur am Handy klingeln.
# Letzte Zeile TABLET_EINRICHTEN_STATUS: fertig oder fehler.
set -u
export MSYS_NO_PATHCONV=1
HIER="$(cd "$(dirname "$0")" && pwd)"
GERAETE="$HIER/../../Werkzeuge/adb-wlan/geraete.json"
JARVIS=de.frank.jarvis
NUR=""
while [ $# -gt 0 ]; do
  case "$1" in
    --nur) NUR="${2:-}"; [ $# -gt 1 ] && shift ;;
    -h|--help) sed -n 2,18p "$0"; exit 0 ;;
    *) echo "Unbekannter Schalter: $1"; echo "TABLET_EINRICHTEN_STATUS=fehler (Aufruf)"; exit 2 ;;
  esac
  shift
done
ende() { echo "TABLET_EINRICHTEN_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }
case "$NUR" in ""|rechte|umzug) ;; *) ende fehler "unbekannter Schritt '$NUR' (rechte, umzug)" ;; esac

for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "${LOCALAPPDATA:-}/Android/Sdk/platform-tools/adb.exe" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB="$K"; break; }
done
[ -n "${ADB:-}" ] || ende fehler "adb nicht gefunden"

serie() { awk -v g="\"$1\":" '$1==g {drin=1; next} drin && /}/ {exit} drin && $1=="\"serial\":" {gsub(/[",\r]/,"",$2); print $2; exit}' "$GERAETE"; }
# Adresse des verbundenen Geräts mit dieser Seriennummer, Netz vor Kabel
adresse() {
  local s kabel=""
  for s in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    [ "$("$ADB" -s "$s" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '[:space:]')" = "$1" ] || continue
    case "$s" in *:[0-9]*) echo "$s"; return ;; esac
    [ -z "$kabel" ] && kabel="$s"
  done
  [ -n "$kabel" ] && echo "$kabel"
}
H=$(adresse "$(serie handy)"); T=$(adresse "$(serie tablet)")
[ -n "$T" ] || ende fehler "Tablet nicht per adb verbunden"
echo "Handy: ${H:-nicht verbunden}   Tablet: $T"
h() { "$ADB" -s "$H" shell "$@" </dev/null 2>&1 | tr -d '\r'; }
t() { "$ADB" -s "$T" shell "$@" </dev/null 2>&1 | tr -d '\r'; }
version() { "$ADB" -s "$1" shell "dumpsys package $2 | grep -m1 versionCode=" </dev/null 2>/dev/null | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p'; }
will() { [ -z "$NUR" ] || [ "$NUR" = "$1" ]; }

if will rechte; then
  echo "== Rechte für Jarvis auf dem Tablet"
  for r in POST_NOTIFICATIONS READ_CALENDAR WRITE_CALENDAR RECORD_AUDIO ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACCESS_BACKGROUND_LOCATION; do
    t pm grant $JARVIS android.permission.$r >/dev/null
  done
  t appops set $JARVIS SYSTEM_ALERT_WINDOW allow >/dev/null
  t appops set $JARVIS USE_FULL_SCREEN_INTENT allow >/dev/null
  t dumpsys deviceidle whitelist +$JARVIS >/dev/null
  for p in de.frank.aufgaben de.frank.ziele de.frank.genialeideen de.frank.epigenetik de.frank.stacklabor.werftstudio de.frank.genialerwecker de.frank.updatestation de.frank.entropyreducer.debug; do
    t pm grant $p android.permission.POST_NOTIFICATIONS >/dev/null
  done
  echo "  gesetzt"
fi

if will umzug; then
  echo "== Einstellungs-Umzug Handy → Tablet"
  [ -n "$H" ] || ende fehler "Handy nicht per adb verbunden"
  VH=$(version "$H" $JARVIS); VT=$(version "$T" $JARVIS)
  [ -n "$VH" ] && [ "$VH" = "$VT" ] || ende fehler "Jarvis muss auf beiden Geräten in derselben Version laufen (Handy ${VH:-–}, Tablet ${VT:-–})"
  h run-as $JARVIS rm -f files/umzug-export.json >/dev/null
  h am broadcast -a de.frank.jarvis.EINSTELLUNGEN_UMZUG -p $JARVIS >/dev/null
  for _ in $(seq 1 40); do [ "$(h run-as $JARVIS sh -c "'ls files/umzug-export.json 2>/dev/null'")" = "files/umzug-export.json" ] && break; sleep 0.5; done
  GROESSE=$(h run-as $JARVIS sh -c "'wc -c < files/umzug-export.json 2>/dev/null'" | tr -d '[:space:]')
  case "$GROESSE" in ''|*[!0-9]*|0) ende fehler "Das Handy hat keinen Export geschrieben (Jarvis ab 1.8.604 nötig)" ;; esac
  # Das Tablet muss die Datei beim nächsten App-Start finden: Jarvis dort vorher beenden.
  t am force-stop $JARVIS >/dev/null
  "$ADB" -s "$H" exec-out run-as $JARVIS cat files/umzug-export.json </dev/null | "$ADB" -s "$T" exec-in run-as $JARVIS sh -c 'mkdir -p files && cat > files/umzug.json'
  h run-as $JARVIS rm -f files/umzug-export.json >/dev/null
  ANGEKOMMEN=$(t run-as $JARVIS sh -c "'wc -c < files/umzug.json 2>/dev/null'" | tr -d '[:space:]')
  [ "$ANGEKOMMEN" = "$GROESSE" ] || { t run-as $JARVIS rm -f files/umzug.json >/dev/null; ende fehler "Umzugsdatei kam unvollständig an ($ANGEKOMMEN statt $GROESSE Bytes)"; }
  echo "  $GROESSE Bytes übergeben"
  # Zugang zur Fernsteuerung der Rechner (Mac-/Windows-Bildschirm, Server-Konsole): liegt als Datei im App-Speicher, nicht in den Einstellungen.
  if [ -n "$(h run-as $JARVIS sh -c "'ls files/fernsteuerung-token.txt 2>/dev/null'")" ]; then
    "$ADB" -s "$H" exec-out run-as $JARVIS cat files/fernsteuerung-token.txt </dev/null | "$ADB" -s "$T" exec-in run-as $JARVIS sh -c 'cat > files/fernsteuerung-token.txt'
    echo "  Fernsteuerungs-Zugang übergeben (die Rechner erscheinen am Tablet, sobald dort WireGuard läuft)"
  fi
  # Jarvis wieder starten: Er übernimmt die Datei beim Start; der Prozessstart stellt Dienst und Verbindung her.
  t am broadcast -a de.frank.jarvis.AUFWACHEN -p $JARVIS >/dev/null
  echo "  Jarvis am Tablet neu gestartet; Gedächtnis und App-Daten kommen über den Geräteabgleich"
fi
ende fertig
