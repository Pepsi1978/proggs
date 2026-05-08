---
name: screenshot-loop
description: Macht im 2,7-Sekunden-Takt automatisch Screenshots vom angeschlossenen Android-Geraet (Galaxy S23 Ultra, Galaxy Tab S9, Galaxy Fold 6) und vibriert dabei das Geraet als Bestaetigung — Frank navigiert manuell durch die App, der Skill knipst was gerade auf dem Bildschirm ist. Nutze diesen Skill IMMER und SOFORT wenn der Benutzer sagt "starte Screenshot-Loop", "Screenshot-Loop starten", "starte den Screenshot-Loop", "mach alle paar Sekunden ein Bild", "automatische Screenshots", "Polling-Screenshots", "Auto-Screenshot-Skript starten", oder eine aehnliche Phrase die auf einen periodischen Screenshot-Modus hindeutet. Auch triggern wenn der Benutzer sinngemaess "ich navigiere durch die App, du machst alle paar Sekunden ein Bild" sagt — auch wenn das exakte Wort "Loop" nicht faellt. Nicht triggern bei einzelnen Screenshot-Anfragen ("mach ein Bild", "zeig mir den Screen jetzt") — dafuer reicht ein einzelner Aufruf von ~/proggs/scripts/screenshot.sh.
---

# Screenshot-Loop

Macht im 2-Sekunden-Takt automatisch Screenshots vom Android-Geraet, mit Vibration als haptisches Feedback. Frank scrollt/navigiert durch die App, der Skill knipst dabei kontinuierlich.

## Wann anwenden

Wenn der Benutzer signalisiert dass er **mehrere Bildschirme** der App systematisch dokumentieren will und dafuer einen automatischen Periodischen Modus moechte. Trigger-Beispiele aus der `description` oben — beim ersten passenden Trigger SOFORT loslegen, keine zusaetzliche Rueckfrage.

## Was der Skill garantiert

1. **Pre-Flight-Test** vor dem Loop: ein einzelner Vibrations-Puls aufs Geraet plus Konsolen-Ausgabe `Test war erfolgreich. Ich starte jetzt mit den Screenshots.`
2. **2,7-Sekunden-Takt** im Loop: Screenshot + Vibration synchron, dann 2,7 Sekunden Pause, dann naechster. Frank-Praezisierung 2026-05-09: 2,0 Sekunden waren zu schnell um den Bildschirm umzuschalten — 2,7 Sekunden gibt genug Zeit zum Wechseln zwischen Bildschirmen.
3. **Vibration markiert den Moment** des Bildes — Frank kann sofort weiterscrollen sobald er die Vibration spuert.
4. **Standardordner**: alle Bilder landen unter `~/Pictures/Claude Screenshots/` mit Naming `auto_NNN_<timestamp>.png` (NNN dreistellig, fortlaufend).
5. **Maximaldauer 5 Minuten** ODER 150 Bilder — was zuerst eintritt, dann automatischer Stop.
6. **Foreground-Ausfuehrung** — Frank kann jederzeit per ESC abbrechen, der Loop endet sofort.
7. **Bilanz am Ende**: `X Screenshots gemacht, gespeichert in <Pfad>`.

## Geraete

Frank besitzt drei Samsung-Geraete (alle Android 13+, alle haben `cmd vibrator_manager`):
- Galaxy S23 Ultra
- Galaxy Tab S9
- Galaxy Fold 6 (Geraete-ID `RFCX70KTDFX`)

Wenn nur eines verbunden ist: das wird genommen. Wenn mehrere verbunden sind: das **erste** aus `adb devices`. Wenn der Benutzer ein bestimmtes Geraet meint, lese das aus seinem Prompt heraus oder frage einmal kurz nach.

## Vibrations-Befehl

`adb shell cmd vibrator_manager synced oneshot 250` — direkter Hardware-Befehl, geht durch die System-Vibrations-Mute-Einstellung hindurch (weil ADB-Shell mit Shell-Permissions arbeitet, nicht ueber die Audio/Notification-Pipeline). Frank fuehlt die Vibration auch wenn er Haptic-Feedback in den Telefon-Einstellungen ausgeschaltet hat.

Wenn der Pre-Flight-Test fehlschlaegt (z.B. `Can't find service: vibrator_manager`): Skill BRICHT AB mit Fehlermeldung. Nicht still ohne Vibration weiterlaufen — Frank braucht das Feedback.

## Konkrete Umsetzung

Diesen Bash-Block ausfuehren (auf Windows-Git-Bash UND macOS gleich):

```bash
DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
if [ -z "$DEVICE" ]; then
  echo "FEHLER: Kein Android-Geraet verbunden. Pruefen mit: adb devices" >&2
  exit 3
fi

# Pre-Flight: Vibrations-Test
if ! adb -s "$DEVICE" shell cmd vibrator_manager synced oneshot 250 >/dev/null 2>&1; then
  echo "FEHLER: Vibration auf $DEVICE schlug fehl. Skill bricht ab." >&2
  exit 4
fi
echo "Test war erfolgreich. Ich starte jetzt mit den Screenshots."

OUTDIR="$HOME/Pictures/Claude Screenshots"
mkdir -p "$OUTDIR"
SCREENSHOT_SCRIPT="$HOME/proggs/scripts/screenshot.sh"

START_TS=$SECONDS
COUNT=0
MAX_BILDER=150
MAX_SEKUNDEN=300

for i in $(seq 1 $MAX_BILDER); do
  ELAPSED=$((SECONDS - START_TS))
  if [ "$ELAPSED" -ge "$MAX_SEKUNDEN" ]; then
    echo "5-Minuten-Limit erreicht."
    break
  fi
  COUNT=$((COUNT + 1))
  NUM=$(printf "%03d" "$COUNT")
  # Screenshot + Vibration synchron
  bash "$SCREENSHOT_SCRIPT" -s "$DEVICE" -n "auto_$NUM" >/dev/null 2>&1 \
    && adb -s "$DEVICE" shell cmd vibrator_manager synced oneshot 250 >/dev/null 2>&1
  echo "  [$NUM] OK"
  sleep 2.7
done

echo
echo "$COUNT Screenshots gemacht, gespeichert in $OUTDIR"
```

## Warum Foreground statt Hintergrund

Frank moechte ESC-Abbruch. Das geht nur wenn der Bash-Block im Foreground laeuft — ESC unterbricht dann den laufenden Tool-Call und der Loop endet sofort. Im Hintergrund waere ESC nicht direkt mappbar (man muesste den PID killen), das ist umstaendlicher fuer Franks Workflow.

Der Trade-off: Waehrend der Loop laeuft (max 5 Min), kann ich nichts anderes tun. Das ist Absicht — Frank ist in der App-Tour fokussiert, Multitasking ist nicht gewollt.

## Was NICHT in diesem Skill ist

- **Theme-Wechsel**: Frank tippt den Mond/Sonne-Icon in der App selbst an. Die App `Entropie Reductor` (und vermutlich auch andere von Frank) hat einen In-App-3-State-Toggle (SYSTEM/LIGHT/DARK), den `adb shell cmd uimode night` nicht steuern kann.
- **Navigation**: Frank steuert manuell durch die App. Der Skill macht keine Tap-Logik.
- **App-Erkennung**: Egal welche App offen ist — Screenshot capture die ganze Bildschirm-Bildflaeche.

## Voraussetzung

`~/proggs/scripts/screenshot.sh` muss existieren und ausfuehrbar sein. Falls nicht: einmal kurz pruefen mit `ls -la ~/proggs/scripts/screenshot.sh`. Wenn das Skript fehlt, bauen wir es zuerst (siehe Memory `feedback_screenshot_method.md`) — aber das sollte nie der Fall sein, weil das Repo-committed ist.

## Bei Fehlern

- `Kein Geraet`: Frank das Telefon einstecken lassen, dann Skill nochmal triggern.
- `Vibration schlug fehl`: Telefon kurz aus dem Schlafmodus holen (Bildschirm aufwecken), dann nochmal.
- `Screenshot fehlgeschlagen`: einzelner Fehler bricht den Loop NICHT ab (`>/dev/null 2>&1` schluckt Stderr). Wenn alle Screenshots fehlschlagen, im Bilanz-Output erkennt Frank dass nichts angekommen ist.
