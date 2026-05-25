# Overlay-Animation — Portierungs-Spec (Windows → macOS)

> **Zweck:** Diese Datei beschreibt die auf Windows fertig gebaute und von Frank als
> "wirklich perfekt, richtig geiler Effekt" bestaetigte Overlay-Animation, damit das
> macOS-Gegenstueck (Swift/AppKit) sie **1:1 genauso** nachbaut. NUR Spezifikation —
> hier wird kein Code gebaut. Gebaut wird, wenn Frank am Mac sitzt.
>
> **Windows-Quelle:** `TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs`,
> Commits `#1080`–`#1085` (Stand 2026-05-25).
> **Zugehoeriges Learning (Windows-Details):**
> `~/.claude/projects/.../memory/reference_wpf_overlay_smooth_animation.md`.

---

## 1. Was die Animation tut (gewuenschtes End-Verhalten, plattformunabhaengig)

Das Overlay ist eine abgerundete, halbtransparente Pille, die ueber dem Terminal
schwebt. Sie hat zwei Achsen von Zustandswechseln, die BEIDE weich animiert sind:

### A) Orientierungswechsel: vertikale Saeule (oben) ↔ horizontale Leiste (unten)
- **Runter (→ Leiste):** Die Saeule blendet **komplett aus**, das Overlay wechselt
  **unsichtbar** zur Leisten-Form, erscheint an der **oberen Linie** (Saeulen-Oberkante)
  und **rutscht von dort glatt nach unten** an seinen Platz.
- **Hoch (→ Saeule):** Die Leiste **rutscht erst glatt nach oben** (gleiche Strecke,
  Gegenrichtung) bis zur **Saeulen-Oberkante**, blendet dort **komplett aus**, wechselt
  **unsichtbar** zur Saeulen-Form und **blendet an der normalen/gemerkten Position
  wieder ein**.
- Beide Richtungen sind exaktes Spiegelbild → "genauso schoen hoch wie runter".

### B) Auto-Hide: volle Form (alle Buttons) ↔ kompakte Mic-Pille
- **Einklappen (voll → Mic):** Die volle Form **blendet komplett aus**, schrumpft
  **unsichtbar** auf die Mic-Pille (der Mic bleibt an seiner Bildschirmposition) und
  die **Mic-Pille blendet weich wieder ein**.
- **Ausklappen (Mic → voll):** Mic-Pille blendet aus → unsichtbar zur vollen Form +
  gemerkte Position → volle Form blendet weich ein.
- Gilt fuer **beide Orientierungen** (vertikal + horizontal), gleicher Beam-Effekt.

---

## 2. Die drei Kern-Prinzipien (NICHT verhandelbar — das ist der ganze Trick)

1. **Bewege NIE die hohe schmale Saeule.** Jede sichtbare BEWEGUNG (Rutschen) passiert
   ausschliesslich in der **flachen Leisten-Form**. Die Saeule erscheint/verschwindet nur
   per Ein-/Ausblenden — sie wird nie als hohes Fenster verschoben. (Auf Windows ruckelte
   ein bewegtes hohes transparentes Fenster; auf macOS weniger kritisch, aber zur Identitaet
   des Verhaltens trotzdem so bauen.)

2. **Der Formwechsel passiert IMMER bei Opacity 0.** Erst komplett ausblenden, dann im
   unsichtbaren Zustand Form/Groesse/Position aendern, dann wieder einblenden. So ist der
   harte Sprung nie sichtbar — er wird zum bewussten "Beam/Teleport".

3. **"Im Viereck bleiben."** Beim Wieder-Einblenden landet die Zielform an ihrer
   **normalen oder per Diskette gemerkten Position** — NICHT am Bildschirmrand. Auf keinen
   Fall hoeher als die kanonische Oberkante erscheinen lassen (sonst "vergroessert man das
   Viereck nach oben" und es ruckelt die Differenz). Konstante: Saeulen-Oberkante =
   Arbeitsbereich-Oberkante + `VerticalTopOffset` (Windows-Wert: 57 pt; gemerkte Position
   hat Vorrang).

---

## 3. Timings & Easing (gleiche Werte wie Windows)

| Phase | Dauer | Easing |
|-------|-------|--------|
| Ausblenden (BeamFadeOut), Opacity 1→0 | **~240 ms** | EaseInOut (weich) |
| Einblenden (BeamFadeIn), Opacity 0→1 | **~380 ms** (bewusst laenger) | EaseInOut (weich) |
| Rutschen (flache Form, senkrecht) | streckenabhaengig, ~**340–600 ms** | Smootherstep (`t³(t(6t−15)+10)`) |

- Einblenden ist absichtlich laenger als Ausblenden → ruhiger, weicher "Reinbeam".
- Rutsch-Tempo hoch und runter sollten gleich wirken (gleiche Dauer-Formel/Untergrenze).

---

## 4. Windows → macOS (Swift/AppKit) Entsprechungen

| Windows (C#/WPF) | macOS (Swift/AppKit) |
|------------------|----------------------|
| `Window` mit `AllowsTransparency=True`, `WindowStyle=None`, Topmost, `WS_EX_NOACTIVATE` | `NSPanel` (borderless, `.nonactivatingPanel`), `isOpaque=false`, `backgroundColor=.clear`, `level = .statusBar`/`.floating`, `ignoresMouseEvents` je nach Bedarf |
| Fenster verschieben: `SetWindowPos` pro Frame **+ `DwmFlush()`** (vsync-sync gegen Tearing ueber Text) | `NSWindow.setFrameOrigin(_:)` / `setFrame(_:display:)`. **macOS komponiert NSWindow-Moves bereits ueber den WindowServer** → i.d.R. glatt OHNE DwmFlush-Trick. Fuer perfekt gleichmaessige Frames: `CVDisplayLink` (vsync-getaktet) und pro Tick `setFrameOrigin` mit Smootherstep-Interpolation. Alternativ `NSAnimationContext` + `window.animator().setFrame(...)`. |
| Opacity-Fade: `DoubleAnimation` auf `UIElement.Opacity` (Beam-Crossfade) | `NSAnimationContext.runAnimationGroup { ctx in ctx.duration = 0.24; ctx.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut); view.animator().alphaValue = 0 }` mit Completion-Handler. (Fenster-weit: `window.animator().alphaValue`; pro Ansicht: `contentView.alphaValue` / Layer `opacity`.) |
| Form-Ansichten `HorizontalView` / `FullView` / `CollapsedView` (umgehaengt) | Drei `NSView`-Layouts in derselben Panel-`contentView`, per `isHidden` umgeschaltet — oder drei Container-Subviews. Gleiche Logik wie Windows. |
| `SizeToContent.WidthAndHeight` (Leiste misst sich selbst) | `NSView` `fittingSize` / Auto-Layout `intrinsicContentSize`, dann `window.setContentSize(...)`. |
| Smootherstep-Easing fuer Slide | Gleiche Formel im CVDisplayLink-Tick, oder `CAMediaTimingFunction(controlPoints:)` annaehern. |
| `VerticalTopOffset = 57` (Saeulen-Oberkante unter Arbeitsbereich-Oberkante) | Gleiche Konstante. **ACHTUNG Koordinaten:** macOS hat **Y-Ursprung unten-links** (Y waechst nach OBEN), Windows oben-links (Y waechst nach UNTEN). "Obere Kante" und "nach unten fallen" muessen in der Y-Mathematik **invertiert** werden. `NSScreen.visibleFrame` liefert die Arbeitsflaeche (ohne Menue-Leiste/Dock). |
| `_savedHorizontalPos` / `_savedVerticalPos` (Diskette-Positionen, pro Orientierung) | Gleiches Konzept; in `UserDefaults` oder dem bestehenden Settings-Store. Beim Beam-Einblenden Vorrang vor der kanonischen Position. |
| Generations-Zaehler `_collapseBeamGen` (Re-Entrancy bei Hover) | Gleiches Muster: `Int`-Zaehler, im Completion-Handler pruefen ob noch aktuell, sonst abbrechen. |
| `DispatcherTimer` (Collapse-Timer 2s/5s) | `Timer` / `DispatchQueue.main.asyncAfter`. |

---

## 5. Ablauf-Pseudocode (identisch fuer beide Plattformen)

### Orientierung RUNTER (→ Leiste)
```
beamFadeOut(saeuleView):              // 240ms, alpha 1→0
  morph → Leisten-Form (unsichtbar)
  leiste.frame = (finalX, oberkanteY) // oben an der Saeulen-Oberkante
  beamFadeIn(leisteView):             // 380ms, alpha 0→1
    slideTo(finalX, untenY)           // glattes Rutschen nach unten
```

### Orientierung HOCH (→ Saeule)
```
slideTo(currentX, oberkanteY):        // erst glatt hochrutschen (flache Form)
  beamFadeOut(leisteView):            // 240ms
    morph → Saeulen-Form (unsichtbar)
    saeule.frame = (gemerkteOderKanonischePos)   // "im Viereck", an Normalposition
    beamFadeIn(saeuleView)            // 380ms — KEIN weiteres Rutschen
```

### Einklappen (voll → Mic)  /  Ausklappen (Mic → voll)
```
einklappen:
  merke expandierte Position
  miscZielGeometrie = Mic-Bildschirmposition (vor dem Ausblenden gemessen)
  beamFadeOut(vollView):
    shrink → Mic-Pille (unsichtbar), frame = miscZielGeometrie
    beamFadeIn(micView)

ausklappen:
  beamFadeOut(micView):
    grow → volle Form (unsichtbar), frame = gemerkte expandierte Position (ABSOLUT)
    beamFadeIn(vollView)
```
- **Position absolut merken/wiederherstellen**, nicht relativ verschieben — sonst driftet
  es bei ueberlappenden Uebergaengen.

---

## 6. Was NICHT funktioniert hat (auf Windows verworfen — auf macOS gar nicht erst versuchen)
- Die hohe Saeule pro Frame VERSCHIEBEN (Content-Transform/Window-Move): ruckelte.
  → Loesung: Saeule nie bewegen, nur ein-/ausblenden.
- Harter Form-Sprung ohne Ausblenden: sichtbarer Ruck. → Loesung: Formwechsel bei Opacity 0.
- Zielform am Bildschirmrand (zu hoch) einblenden und dann runterfallen: ruckelte die
  Differenz, fuehlte sich "ausserhalb des Vierecks" an. → Loesung: an Normalposition einblenden.
- Feste, krumme Bildrate erzwingen (z.B. 50 fps bei 60/120 Hz): Beating/Ruckeln.
  → Loesung: vsync-gebunden laufen lassen (macOS: CVDisplayLink oder Default-Animator).

---

## 7. Checkliste fuer den macOS-Bau (wenn Frank am Mac ist)
- [ ] Drei View-Zustaende: vertikale Saeule, horizontale Leiste, Mic-Pille.
- [ ] `beamFadeOut`/`beamFadeIn`-Helfer (alpha, 240/380 ms, easeInEaseOut, Completion).
- [ ] Glattes senkrechtes Rutschen NUR in flacher Leisten-Form (CVDisplayLink/Animator).
- [ ] Orientierung: runter = ausblenden→morph→einblenden oben→runterrutschen;
      hoch = hochrutschen→ausblenden→morph→einblenden an Normalposition.
- [ ] Auto-Hide: ein-/ausklappen mit demselben Beam, beide Orientierungen.
- [ ] Y-Koordinaten invertiert (macOS unten-links), `NSScreen.visibleFrame`.
- [ ] Positionen absolut merken/wiederherstellen; Diskette-Positionen mit Vorrang.
- [ ] Generations-Guard gegen Hover-Ueberlappung.
- [ ] Eingeklappte Mic-Pille = **echter zentrierter Kreis** (nicht Kapsel): quadratisch,
      CornerRadius = halbe Kantenlaenge, Mic mittig drin. Auf Windows: 64×64-Border,
      CornerRadius 32, zentriert im 96×64-Fenster. macOS: runde `NSView`/Layer
      (`cornerRadius = bounds.width/2`), zentriert.
- [ ] Gegen weisser Terminal-Schrift testen — muss flackerfrei sein.
