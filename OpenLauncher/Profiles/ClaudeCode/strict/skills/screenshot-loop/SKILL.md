---
name: screenshot-loop
disallowed-tools:
  - AskUserQuestion
description: Macht periodisch automatisch Screenshots vom angeschlossenen Android-Geraet (Galaxy S23 Ultra, Galaxy Tab S9, Galaxy Fold 6) und vibriert dabei das Geraet als Bestaetigung — Frank navigiert manuell durch die App, der Skill knipst was gerade auf dem Bildschirm ist. Standard-Intervall 2,7 Sekunden, aber per Trigger-Phrase ueberschreibbar (siehe unten). Nutze diesen Skill IMMER und SOFORT wenn der Benutzer sagt "starte Screenshot-Loop", "Screenshot-Loop starten", "starte den Screenshot-Loop", "starte den Screenshot-Loop mit 3 Sekunden", "mit 4 Sekunden", "alle 5 Sekunden", "mach alle paar Sekunden ein Bild", "automatische Screenshots", "Polling-Screenshots", "Auto-Screenshot-Skript starten", oder eine aehnliche Phrase die auf einen periodischen Screenshot-Modus hindeutet. Auch triggern wenn der Benutzer sinngemaess "ich navigiere durch die App, du machst alle paar Sekunden ein Bild" sagt — auch wenn das exakte Wort "Loop" nicht faellt. Nicht triggern bei einzelnen Screenshot-Anfragen ("mach ein Bild", "zeig mir den Screen jetzt") — dafuer reicht ein einzelner Aufruf von ~/proggs/scripts/screenshot.sh.
---

# Screenshot-Loop

Macht im periodischen Takt automatisch Screenshots vom Android-Geraet, mit Vibration als haptisches Feedback. Frank scrollt/navigiert durch die App, der Skill knipst dabei kontinuierlich.

## Wann anwenden

Wenn der Benutzer signalisiert dass er **mehrere Bildschirme** der App systematisch dokumentieren will und dafuer einen automatischen periodischen Modus moechte. Trigger-Beispiele aus der `description` oben — beim ersten passenden Trigger SOFORT loslegen, keine zusaetzliche Rueckfrage.

## Intervall aus dem Trigger lesen (PFLICHT)

Bevor du den Loop startest, **bestimme das Intervall**:

| Trigger-Variante | Intervall |
|------------------|-----------|
| `starte den Screenshot-Loop` (ohne Zahl) | **2.7 Sekunden** (Default) |
| `starte den Screenshot-Loop mit 3 Sekunden` | 3.0 |
| `starte den Screenshot-Loop mit 1,5 Sekunden` | 1.5 (Komma in Zahl in Punkt umwandeln) |
| `mach alle 4 Sekunden ein Bild` | 4.0 |
| `Screenshot-Loop alle 5 sek` | 5.0 |

**Regex-Hinweis fuer dich:** Suche im Benutzer-Prompt nach einer Zahl die direkt vor "Sekunden", "sek", "s" oder als Standalone-Zahl im Trigger steht. Akzeptiere `,` und `.` als Dezimalzeichen, gib im Bash-Befehl aber IMMER `.` weil `sleep` nur Punkt versteht. Zahl muss positiv und kleiner 60 sein (Sanity-Check — ueber 60 Sekunden ist vermutlich Tippfehler).

Wenn keine Zahl im Prompt: 2.7 Sekunden Default.

## Was der Skill garantiert

1. **Direkter Loop-Start** ohne Pre-Flight-Vibrations-Test. Frank ist sowieso in der App und wartet auf das erste Bild — die erste echte Vibration ist die Bestaetigung dass alles funktioniert. Wenn Vibration im echten Loop fehlschlaegt, sieht Frank das am fehlenden Feedback und sagt "stop".
2. **Festes Intervall** im Loop: Screenshot + Vibration synchron, dann INTERVAL Sekunden Pause, dann naechster.
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

## Konkrete Umsetzung

Diesen Bash-Block ausfuehren — die Variable `INTERVAL` MUSST du an die aus dem Trigger gelesene Zahl anpassen (siehe oben), Default `2.7`:

```bash
DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
if [ -z "$DEVICE" ]; then
  echo "FEHLER: Kein Android-Geraet verbunden. Pruefen mit: adb devices" >&2
  exit 3
fi

INTERVAL=2.7   # <-- aus dem Benutzer-Prompt anpassen falls Zahl drin steht

OUTDIR="$HOME/Pictures/Claude Screenshots"
mkdir -p "$OUTDIR"
SCREENSHOT_SCRIPT="$HOME/proggs/scripts/screenshot.sh"

echo "Screenshot-Loop laeuft auf $DEVICE — Intervall ${INTERVAL}s, max 5 Min oder 150 Bilder. ESC zum Abbrechen."

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
  bash "$SCREENSHOT_SCRIPT" -s "$DEVICE" -n "auto_$NUM" >/dev/null 2>&1 \
    && adb -s "$DEVICE" shell cmd vibrator_manager synced oneshot 250 >/dev/null 2>&1
  echo "  [$NUM] OK"
  sleep "$INTERVAL"
done

echo
echo "$COUNT Screenshots gemacht, gespeichert in $OUTDIR"
```

## Warum kein Pre-Flight-Test mehr

Frank-Praezisierung 2026-05-09: "Bitte ohne den ersten Vibrationstest einfach los starten, weil ich bin ja sowieso schon in der App drin. Ich brauche dann nur, wenn ich die erste Vibration merke, dann funktioniert das alles top." — die erste echte Loop-Vibration ist die Bestaetigung. Eine Vor-Vibration waere eine zusaetzliche Wartezeit ohne neuen Informationsgewinn.

## Warum Foreground statt Hintergrund

Frank moechte ESC-Abbruch. Das geht nur wenn der Bash-Block im Foreground laeuft — ESC unterbricht dann den laufenden Tool-Call und der Loop endet sofort. Im Hintergrund waere ESC nicht direkt mappbar (man muesste den PID killen), das ist umstaendlicher fuer Franks Workflow.

Der Trade-off: Waehrend der Loop laeuft (max 5 Min), kann ich nichts anderes tun. Das ist Absicht — Frank ist in der App-Tour fokussiert, Multitasking ist nicht gewollt.

## Was NICHT in diesem Skill ist

- **Theme-Wechsel**: Frank tippt den Mond/Sonne-Icon in der App selbst an. Apps mit eigenem 3-State-Toggle (SYSTEM/LIGHT/DARK in DataStore) ignorieren `adb shell cmd uimode night`.
- **Navigation**: Frank steuert manuell durch die App. Der Skill macht keine Tap-Logik.
- **App-Erkennung**: Egal welche App offen ist — Screenshot capture die ganze Bildschirm-Bildflaeche.

## Voraussetzung

`~/proggs/scripts/screenshot.sh` muss existieren und ausfuehrbar sein. Falls nicht: einmal kurz pruefen mit `ls -la ~/proggs/scripts/screenshot.sh`. Wenn das Skript fehlt, bauen wir es zuerst (siehe Memory `feedback_screenshot_method.md`) — aber das sollte nie der Fall sein, weil das Repo-committed ist.

## Bei Fehlern

- `Kein Geraet`: Frank das Telefon einstecken lassen, dann Skill nochmal triggern.
- `Erste Vibration kommt nicht`: Frank wird "stop" sagen — dann kurz pruefen ob Telefon im Schlafmodus ist (Bildschirm aufwecken) oder ob eine andere ADB-Permission fehlt.
- `Screenshot fehlgeschlagen`: einzelner Fehler bricht den Loop NICHT ab. Wenn alle Screenshots fehlschlagen, im Bilanz-Output erkennt Frank dass nichts angekommen ist.
