# Zeigefinger

## Klick-Auswahl fuer OpenCode

Der neue Standardweg spiegelt den laufenden Fold-8-Emulator in ein sauberes
`scrcpy`-Arbeitsfenster. Das eigentliche Emulatorfenster wird minimiert. Ein grosses, schwebendes
Bedienfeld links oben trennt zwei Zustaende:

- **Bedienen:** Klicks, Scrollrad und Tastatur gehen unveraendert an Android.
- **Auswaehlen:** Die App wird leicht getoent; der naechste Klick wird abgefangen und loest keine
  Aktion in Android aus.

Start fuer die Experimente-App:

```powershell
.\Start-Zeigefinger.ps1
```

Alternativ `Start-Zeigefinger.vbs` im Explorer doppelt anklicken. Dieser Starter oeffnet kein
zusaetzliches Konsolenfenster. Das Werkzeug erkennt sowohl OpenCode als auch Claude Code im
Windows-Terminal; damit funktioniert derselbe Ablauf auch nach einem Start von Opus ueber den
OpenLauncher. Gibt es mehrere echte Terminalfenster und ist das Ziel nicht eindeutig, wird die
Referenz sicher in die Zwischenablage gelegt statt in das falsche Fenster geschrieben.

Nach **Auswaehlen** und einem Klick friert das Werkzeug den genauen Kontext ein: Android-Koordinate,
aktueller Screen, UI-Baum, Screenshot, Element unter dem Zeiger, umgebende Elemente und moegliche
Kotlin-Codestellen. Die Daten landen als eindeutige JSON-Datei unter
`%TEMP%\opencode\zeigefinger\`.

Danach wechselt der Fokus zu OpenCode und folgende Referenz steht bereits in der Eingabe:

```text
Lies zuerst dieses Zeigefinger-Ziel und beziehe "dort" darauf: "...\auswahl-....json".
```

Nur noch die eigentliche Aenderung ergaenzen, beispielsweise `Mach dort die Schrift groesser.`,
und absenden. Falls kein OpenCode-Fenster eindeutig gefunden wird, liegt derselbe Text in der
Zwischenablage und kann mit `Strg+V` eingefuegt werden.

Das Schliessen der Leiste beendet die von ihr gestartete Spiegelung und stellt das Emulatorfenster
wieder her.

---

## Klassischer Hover-Modus

Zeigt im scrcpy-Spiegelbild auf ein Bedienelement und sagt dir, **wo im Quellcode** es steht.
Damit wird aus „der Knopf da oben rechts" eine konkrete Datei mit Zeilennummer — ohne Screenshot,
ohne Suchen, ohne Raten.

```
🔘  Backup aktualisieren
    Position 922,276   Bereich [891,245][954,308]
    Klickbarer Rahmen: (ohne Beschriftung)  [860,214][986,340]

    → app/src/main/java/.../presentation/dashboard1/TasksScreen.kt:413
      contentDescription = "Backup aktualisieren",
```

Danach reicht: „mach den orange und lass ihn stattdessen exportieren".

## Voraussetzungen

- Android-Gerät per USB, USB-Debugging an (`adb devices` zeigt es als `device`)
- `scrcpy` läuft und spiegelt das Gerät (`winget install Genymobile.scrcpy`)
- Python 3.10+ — keine zusätzlichen Pakete nötig

## Benutzung

**Dauerbetrieb (Windows).** Läuft mit, während du arbeitest:

```powershell
python zeigefinger.py --projekt C:\Users\barwa\proggs\EntropieReductor
```

Fahr mit der Maus im scrcpy-Fenster auf ein Element und **halt kurz still** (0,7 s). Kein Klick.
Beenden mit `Strg+C`.

**Einmalig, mit fester Koordinate.** Läuft auf jedem Betriebssystem:

```powershell
python zeigefinger.py --projekt <pfad> --punkt 922 276
```

| Schalter | Bedeutung | Standard |
|----------|-----------|----------|
| `--projekt` | Ordner mit dem Quellcode | aktueller Ordner |
| `--fenster` | Titel(teil) des scrcpy-Fensters | `Fold8Live` |
| `--serial` | ADB-Seriennummer | erstes echtes Gerät (Emulatoren werden übersprungen) |
| `--punkt X Y` | Einmal-Abfrage statt Maus | — |

## Warum Zeigen und nicht Klicken

Ein echter Klick schaltet die App weiter. Der UI-Baum, den das Werkzeug danach liest, würde
also schon den **nächsten** Bildschirm beschreiben — du zeigst auf „Einstellungen" und bekommst
den Code des Einstellungs-Bildschirms statt den des Knopfes. Deshalb reagiert das Werkzeug auf
ruhende Maus statt auf Klicks.

## Warum keine Klick-Erfassung auf dem Gerät

Der naheliegende Weg wäre `adb shell getevent` — auf dem Gerät mithören, wo getippt wird.
Gemessen am 12.08.2026 auf dem Galaxy Z Fold 8 (One UI 8): **funktioniert nicht.** Klicks aus
scrcpy und `input tap` werden über die InputManager-Schnittstelle eingespeist und laufen am
Kernel-Eingabegerät vorbei — `getevent` sieht sie nicht. Nur ein echter Finger auf dem Glas
erzeugt dort Ereignisse. Deshalb wird die Mausposition auf der Windows-Seite gelesen und
umgerechnet.

Auch `dumpsys input` hilft nicht: Die `RecentQueue` listet die letzten Ereignisse, aber **ohne
Koordinaten**.

## Fallstricke bei Foldables

- **Zwei Touchscreens, zwei Displays.** Das Fold 8 hat `sec_touchscreen` und `sec_touchscreen2`
  mit je Rohwerten 0–4095 — das sind keine Pixel. Das Werkzeug umgeht die Umrechnerei komplett,
  indem es die Mausposition im Fenster nimmt und über `wm size` skaliert.
- **`wm size` wird bei jeder Abfrage neu gelesen.** Klappst du das Gerät mitten in der Sitzung
  auf, stimmen die Koordinaten weiterhin (1248×1972 zugeklappt, 2448×1848 aufgeklappt).
- **Kein Letterboxing vorausgesetzt.** scrcpy zeigt das Bild maßstabsgetreu; gemessen 714×1129
  Fensterpixel bei 1248×1972 Gerätepixeln (Seitenverhältnis 0,6324 vs. 0,6329). Sollte scrcpy
  einmal mit schwarzen Rändern zeichnen, verschiebt sich der Treffer.

## Was das Werkzeug nicht kann

- **Inhalte aus der Datenbank** — „Tiefenreinigung der Wohnung" ist ein Datensatz, kein Code.
  Das Werkzeug sagt das ausdrücklich, statt einen falschen Treffer zu zeigen.
- **Elemente ohne Text und ohne `contentDescription`** — reine Grafik-Flächen in Compose haben
  keinen Anker zum Suchen. Hier hilft nur, das Element in Worten zu beschreiben.
- **macOS/Linux im Dauerbetrieb** — die Mausabfrage nutzt Win32. `--punkt` geht überall.

## Beobachtung

Jeder Lauf schreibt nach `%TEMP%\zeigefinger.jsonl` (eine JSON-Zeile pro Ereignis: Start,
aufgelöstes Element, verletzte Erwartungen). Bei Fehlverhalten ist das die erste Anlaufstelle.
