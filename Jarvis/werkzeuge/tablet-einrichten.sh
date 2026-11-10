#!/usr/bin/env bash
# Richtet „Jarvis Tablet“ (Galaxy Tab S9) als Zwilling des Handys ein: Erstbefüllung per adb, einmalig oder zum Nachziehen.
# Voraussetzung: Auf beiden Geräten ist dieselbe Jarvis-Version (ab 1.8.604) installiert, die angebundenen Apps in gleicher
# Version (alles Debug-Bauten, deshalb geht run-as), beide Geräte sind per adb erreichbar (Werkzeuge/adb-wlan/geraete.json).
#
# Schritte (einzeln wählbar mit --nur <schritt>, sonst alle in dieser Reihenfolge):
#   rechte      Berechtigungen und Sonderzugriffe für Jarvis auf dem Tablet (Mikrofon, Kalender, Standort, Overlay, Akku-Ausnahme)
#   umzug       alle Einstellungen und Zugangsschlüssel vom Handy (data/Umzug.kt); die Datei reist per run-as von Gerät zu Gerät
#               und liegt nie auf dem Rechner. Nicht dabei: Plugin-Geheimnis, ChatGPT-Anmeldung, Anthropic-Sitzungen.
#   gedaechtnis Jarvis' Gedächtnis und Gespräche (Merkdateien, Langzeitkontext, Tagebuch-Kurzfassungen, Auswertungen, Gespräche).
#               Die Ablage, Prompts und Erinnerungen kommen nicht per Kopie: Die holt sich das Tablet selbst vom Server.
#   apps        Daten der angebundenen Apps (Aufgaben, Ziele, Geniale Ideen, Epigenetik, Stack-Labor, Genialer Wecker):
#               Datenbanken, Dateien und unverschlüsselte Einstellungen. Verschlüsselte Speicher (Schlüssel, Anmeldungen)
#               bleiben weg, sie wären auf dem Tablet unlesbar.
# Schutz: gedaechtnis und apps überschreiben den Stand des Tablets. Hat das Tablet dort schon Daten, bricht der Schritt ab,
# außer mit --ueberschreiben. Die Richtung ist immer Handy → Tablet.
# Letzte Zeile TABLET_EINRICHTEN_STATUS: fertig oder fehler.
set -u
export MSYS_NO_PATHCONV=1
HIER="$(cd "$(dirname "$0")" && pwd)"
GERAETE="$HIER/../../Werkzeuge/adb-wlan/geraete.json"
JARVIS=de.frank.jarvis
NUR=""; UEBERSCHREIBEN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --nur) NUR="${2:-}"; [ $# -gt 1 ] && shift ;;
    --ueberschreiben) UEBERSCHREIBEN=1 ;;
    -h|--help) sed -n 2,18p "$0"; exit 0 ;;
    *) echo "Unbekannter Schalter: $1"; echo "TABLET_EINRICHTEN_STATUS=fehler (Aufruf)"; exit 2 ;;
  esac
  shift
done
ende() { echo "TABLET_EINRICHTEN_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }

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
[ -n "$H" ] || ende fehler "Handy nicht per adb verbunden"
[ -n "$T" ] || ende fehler "Tablet nicht per adb verbunden"
echo "Handy: $H   Tablet: $T"
h() { "$ADB" -s "$H" shell "$@" </dev/null 2>&1 | tr -d '\r'; }
t() { "$ADB" -s "$T" shell "$@" </dev/null 2>&1 | tr -d '\r'; }
version() { "$ADB" -s "$1" shell "dumpsys package $2 | grep -m1 versionCode=" </dev/null 2>/dev/null | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p'; }
will() { [ -z "$NUR" ] || [ "$NUR" = "$1" ]; }

gleiche_version() {
  local vh vt; vh=$(version "$H" "$1"); vt=$(version "$T" "$1")
  [ -n "$vh" ] && [ "$vh" = "$vt" ] && return 0
  echo "  $1: Versionen verschieden oder nicht installiert (Handy ${vh:-–}, Tablet ${vt:-–}) – übersprungen"; return 1
}

if will rechte; then
  echo "== Rechte für Jarvis auf dem Tablet"
  for r in POST_NOTIFICATIONS READ_CALENDAR WRITE_CALENDAR RECORD_AUDIO ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACCESS_BACKGROUND_LOCATION; do
    t pm grant $JARVIS android.permission.$r >/dev/null
  done
  t appops set $JARVIS SYSTEM_ALERT_WINDOW allow >/dev/null
  t appops set $JARVIS USE_FULL_SCREEN_INTENT allow >/dev/null
  t dumpsys deviceidle whitelist +$JARVIS >/dev/null
  # Die angebundenen Apps: Benachrichtigungen, und der Wecker darf über anderen Apps erscheinen
  for p in de.frank.aufgaben de.frank.ziele de.frank.genialeideen de.frank.epigenetik de.frank.stacklabor.werftstudio de.frank.genialerwecker de.frank.updatestation; do
    t pm grant $p android.permission.POST_NOTIFICATIONS >/dev/null
  done
  t appops set de.frank.genialerwecker SYSTEM_ALERT_WINDOW allow >/dev/null
  t appops set de.frank.genialerwecker USE_FULL_SCREEN_INTENT allow >/dev/null
  t dumpsys deviceidle whitelist +de.frank.genialerwecker >/dev/null
  echo "  gesetzt"
fi

if will umzug; then
  echo "== Einstellungs-Umzug Handy → Tablet"
  gleiche_version $JARVIS || ende fehler "Jarvis muss auf beiden Geräten in derselben Version laufen"
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
  echo "  $GROESSE Bytes übergeben; das Tablet übernimmt sie beim nächsten Start von Jarvis"
fi

if will gedaechtnis; then
  echo "== Jarvis' Gedächtnis und Gespräche Handy → Tablet"
  if [ $UEBERSCHREIBEN -eq 0 ] && [ -n "$(t run-as $JARVIS sh -c "'ls files/regeln.json files/gespraeche 2>/dev/null | head -1'")" ]; then
    echo "  Das Tablet hat schon ein Gedächtnis – übersprungen (--ueberschreiben erzwingt)"
  else
    TEILE="regeln.json ziele.json ueber_frank.json entscheidungen.json symbiose.json merk_verlauf.jsonl gespraechsnotizen.json gespraeche_gelernt.json langzeitkontext.txt langzeitkontext-vorfassung.txt tagebuch_kurz tagebuch_verdichtet tagesauswertung tagesauswertung_archiv lernberichte.json lern_pruefungen.json lern_aenderungen.jsonl funktionen stil projekte.json gespraeche auftragstexte agenten"
    t am force-stop $JARVIS >/dev/null
    "$ADB" -s "$H" exec-out run-as $JARVIS sh -c "cd files && tar c \$(ls -d $TEILE 2>/dev/null)" </dev/null | "$ADB" -s "$T" exec-in run-as $JARVIS sh -c 'mkdir -p files && cd files && tar x'
    echo "  übertragen: $(t run-as $JARVIS sh -c "'ls files | wc -l'" | tr -d '[:space:]') Einträge im App-Speicher des Tablets, $(t run-as $JARVIS sh -c "'ls files/gespraeche 2>/dev/null | wc -l'" | tr -d '[:space:]') Gespräche"
  fi
fi

if will apps; then
  echo "== Daten der angebundenen Apps Handy → Tablet"
  for p in de.frank.aufgaben de.frank.ziele de.frank.genialeideen de.frank.epigenetik de.frank.stacklabor.werftstudio de.frank.genialerwecker; do
    gleiche_version $p || continue
    if [ $UEBERSCHREIBEN -eq 0 ] && [ -n "$(t run-as $p sh -c "'ls databases 2>/dev/null | head -1; ls /data/user_de/0/$p/shared_prefs 2>/dev/null | head -1'")" ]; then
      echo "  $p: Das Tablet hat schon Daten – übersprungen (--ueberschreiben erzwingt)"; continue
    fi
    t am force-stop $p >/dev/null
    # Unverschlüsselte Einstellungen: alles ohne den Schlüsselsatz der verschlüsselten Speicher (der wäre am Tablet unlesbar).
    PREFS=$(h run-as $p sh -c "'cd shared_prefs 2>/dev/null && grep -L __androidx_security_crypto_encrypted_prefs *.xml 2>/dev/null'" | grep -v 'ActivityThread' | sed 's|^|shared_prefs/|' | tr '\n' ' ')
    "$ADB" -s "$H" exec-out run-as $p sh -c "tar c \$(ls -d databases files $PREFS 2>/dev/null)" </dev/null | "$ADB" -s "$T" exec-in run-as $p sh -c 'tar x'
    # Geräteschutz-Speicher (Weckerliste): eigener Ordner außerhalb des normalen App-Speichers
    if [ -n "$(h run-as $p sh -c "'ls /data/user_de/0/$p/shared_prefs 2>/dev/null | head -1'")" ]; then
      "$ADB" -s "$H" exec-out run-as $p sh -c "cd /data/user_de/0/$p && tar c shared_prefs \$(ls -d files 2>/dev/null)" </dev/null | "$ADB" -s "$T" exec-in run-as $p sh -c "cd /data/user_de/0/$p && tar x"
    fi
    echo "  $p: übertragen ($(t run-as $p sh -c "'du -sk databases files 2>/dev/null | tr \"\\n\" \" \"'"))"
  done
fi
ende fertig
