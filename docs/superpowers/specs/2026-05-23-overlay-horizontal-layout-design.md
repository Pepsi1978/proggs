# Design: Horizontale Layout-Variante für die Voice Overlays

- **Datum:** 2026-05-23
- **Status:** Freigegeben (Benutzer am 2026-05-23: "bau erstmal einfach, debug später")
- **Betroffene Projekte:** TerminalVoiceOverlay-Windows (Hauptziel), ClaudeVoiceOverlay-Windows (Folgeschritt, simpler)

---

## 1. Ziel

Das Overlay soll optional **horizontal** statt vertikal dargestellt werden — gleiche Symbole,
gleiche Funktionen, nur die Achse gedreht. Umschaltbar per Einstellung **und** per neuem
⇄-Button direkt im Overlay neben dem Stern. Rein optisch, keine Funktionsänderung.

## 2. Layout (horizontal, rechts → links)

Eine Symbol-Reihe; Profil-Zahlen pro Sektion direkt **darunter** (90°-Drehung des vertikalen
Layouts). Reihenfolge von **rechts nach links**:

```
 rechts ◄─────────────────────────────────────────────── links
  ★  ⇄ │ 🎤 BTW │ W G │ ✕ │ Copy Paste │ Screenshot Insert │ ⏎
        1 2 3    4 5    6     7 8          9 10
```

- Ganz rechts der **Stern** (etwas kleiner), daneben der neue **⇄-Umschalter**, dann leicht
  nach links versetzt der Rest.
- Profil-Zahlen sitzen unter ihrer jeweiligen Sektion (gruppiert, nicht als gleichmäßige Reihe).
- Farben, Icons, Tooltips bleiben 1:1 wie vertikal.

## 3. Bildschirmposition & Popups

- Horizontale Leiste am **unteren** Rand des Terminal-Monitors.
- Promptboard-/Eingabe-Fenster öffnen **nach oben** (über der Leiste) — statt wie vertikal
  links daneben.

## 4. Auto-Hide (horizontal)

- Eingeklappt bleibt nur die Mic-Pille an gleicher Bildschirmposition; die Leiste schrumpft
  **horizontal** (statt vertikal). Hover klappt auf. Gleiche 2 s/5 s-Timer-Logik.

## 5. Umschalten zwischen vertikal/horizontal

- **In-Overlay:** neuer ⇄-Button neben dem Stern (in **beiden** Layouts). Klick → sofort
  umschalten + persistieren. Der Stern wird dafür etwas kleiner und rückt leicht nach links.
- **Einstellungen:** Schalter „Ausrichtung: vertikal / horizontal".
- **Persistenz:**
  - Terminal-App: `AppSettings.Orientation` (string "vertical"/"horizontal", Default "vertical"),
    DB + idempotente `ALTER TABLE`-Migration — exakt wie das AutoHide-Feld.
  - Claude-App: hat **keine** DB → kleine JSON-Datei in `%LOCALAPPDATA%\ClaudeVoiceOverlay\`
    (sonst ginge der ⇄-Schalter beim Neustart verloren).

## 6. Architektur — ein Satz Buttons, umgehängt (reparenting)

Es gibt **keine** doppelten Button-Instanzen. Die bestehenden benannten Buttons (MicButton,
BtwButton, …, Profile1..10) werden zur Laufzeit zwischen dem vertikalen Container (`FullView`,
unverändert) und einem neuen horizontalen Container (`HorizontalView`) **umgehängt**.

- Vorteil: die gesamte Zustands-Logik (SetMicState, SetActiveProfile, Toggle-Farben,
  Aufnahme/Waveform) bleibt **unverändert** — es sind dieselben Instanzen.
- `ApplyOrientation(horizontal)`: hängt Buttons in die Ziel-Container, schaltet Sichtbarkeit
  von FullView/HorizontalView, setzt Fenstergröße + Position passend.
- Beim Zurückschalten werden Buttons an ihre ursprüngliche Stelle (Parent + Index gemerkt)
  zurückgehängt.
- Die **vertikale XAML bleibt der Standard und wird nicht destruktiv angefasst** — horizontal
  ist additiv und opt-in. Bei Bugs im Horizontalen schaltet der Benutzer zurück auf vertikal
  (unberührt, Fallback).

## 7. Umsetzungsreihenfolge (sicher, additiv, je committet)

1. `AppSettings.Orientation` + DB-Migration (Terminal) — unsichtbar, kein Risiko. ← **Fundament**
2. `HorizontalView`-Container in XAML (leer) + `ApplyOrientation()`-Gerüst.
3. Reparenting der Buttons in die horizontale Anordnung (Symbole + Zahlen pro Sektion).
4. Position unten + Popup-Richtung oben + horizontaler Collapse.
5. ⇄-Toggle-Button (beide Layouts) + Settings-Schalter (Terminal).
6. Claude-App: analog, simpler (keine Profil-Zahlen), JSON-Persistenz.

Jeder Schritt einzeln gebaut + committet; die vertikale Variante bleibt durchgehend nutzbar.

## 8. Nicht im Umfang (YAGNI)

- macOS-Overlays (separater Schritt, eigene Spec/To-Do).
- Keine Funktionsänderung — nur Anordnung/Position/Sichtbarkeit.
- Keine frei drehbaren Zwischenwinkel — nur vertikal ODER horizontal.
