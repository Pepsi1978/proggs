# Design: Auto-Hide / Hover-to-Expand für die Voice Overlays

- **Datum:** 2026-05-23
- **Status:** Freigegeben (vom Benutzer am 2026-05-23)
- **Betroffene Projekte:** TerminalVoiceOverlay (Windows + macOS), ClaudeVoiceOverlay/ClaudeCodexVoiceOverlay (Windows + macOS)

---

## 1. Problem

Das Voice Overlay ist ein dauerhaft sichtbarer, vertikaler Pill-Streifen (Windows: 96×612 px,
Topmost). Beim Mitlesen der CLI-Ausgabe liegt das Overlay teilweise über dem Text — der Benutzer
sieht zu wenig. Gewünscht ist ein Overlay, das sich nach Benutzung selbst aus dem Weg räumt und
nur bei Bedarf wieder erscheint.

## 2. Ziel

Das Overlay bekommt zwei Zustände:

- **Eingeklappt** — nur der Mikrofon-Button ist sichtbar.
- **Ausgeklappt** — der komplette Overlay-Streifen ist sichtbar und voll bedienbar.

Der Wechsel erfolgt automatisch über Maus-Hover und kurze Timer. Im eingeklappten Zustand ist
die CLI dahinter frei lesbar **und** anklickbar.

## 3. Umfang

Alle vier Apps bekommen identisches Verhalten:

| App | Plattform | Framework |
|-----|-----------|-----------|
| TerminalVoiceOverlay-Windows | Windows | WPF (C#) |
| ClaudeVoiceOverlay-Windows | Windows | WPF (C#) |
| TerminalVoiceOverlay-macOS | macOS | AppKit (Swift) |
| ClaudeCodexVoiceOverlay-macOS | macOS | AppKit (Swift) |

## 4. Fenster-Mechanismus (gewählter Ansatz)

**Ansatz A — Fenster verkleinern + verschieben.** Ein einziges Fenster pro App.

- **Einklappen:** Alle Sektionen außer dem Mic-Button werden ausgeblendet, das Fenster
  schrumpft auf die Höhe des Mic-Bereichs und wird gleichzeitig so verschoben, dass der
  Mic-Button **exakt an seiner bisherigen Bildschirm-Position** bleibt (kein Springen).
- **Ausklappen:** Fenstergröße und -position werden auf den vollen Streifen zurückgesetzt.

Begründung gegenüber Alternativen:
- *Zwei getrennte Fenster* würden Mic-/Aufnahme-Logik duplizieren und ständige Synchronisation
  erfordern — verworfen.
- *Großes Fenster, nur Inhalt ausblenden* würde die Maus weiter abfangen und die CLI darunter
  blockieren — verworfen (widerspricht dem Ziel).

### 4.1 Positions-Berechnung (Mic bleibt an gleicher Stelle)

Der Mic-Button sitzt nicht ganz oben, sondern unterhalb der ersten Sektion (Stern). Beim
Einklappen muss daher gelten:

```
neue_fenster_oberkante = alte_oberkante + offset_mic_im_vollen_layout
neue_fenster_hoehe      = hoehe_des_mic_bereichs
```

Beim Ausklappen wird die Verschiebung exakt rückgängig gemacht. Der `offset_mic_im_vollen_layout`
wird aus dem Layout ermittelt (Höhe Sektion 1 + Trennlinie + Padding bis zur Mic-Oberkante),
nicht hartkodiert geschätzt.

## 5. Zustandsautomat & Timer

Zwei Zustände: `Eingeklappt`, `Ausgeklappt`.

| Ereignis | Reaktion |
|----------|----------|
| Maus betritt den Mic-Button (Zustand Eingeklappt) | sofort → **Ausgeklappt** |
| Maus betritt das Overlay (egal welcher Bereich) | alle Einklapp-Timer abbrechen, bleibt **Ausgeklappt** |
| Maus ist über dem Overlay | bleibt offen, alles bedienbar, **kein Timer läuft** |
| Maus verlässt das Overlay — *seit Aufklappen wurde eine Funktion benutzt* | Timer **2 s** → Eingeklappt |
| Maus verlässt das Overlay — *nur gehovert, nichts benutzt* | Timer **5 s** → Eingeklappt |
| Aufnahme läuft gerade | bleibt **immer Ausgeklappt** (kein Einklappen während des Sprechens) |

### 5.1 Bestätigte Grundregel

„Solange die Maus über dem Overlay ist, bleibt es offen." Diese Regel hat Vorrang. Der 2-s-
bzw. 5-s-Timer startet **erst, wenn die Maus das Overlay verlässt** — nie währenddessen.
(Vom Benutzer am 2026-05-23 ausdrücklich bestätigt.)

### 5.2 Welcher Timer gilt (2 s vs. 5 s)

Ein Merker `benutztSeitAufklappen` steuert die Wahl:

- Bei jedem Aufklappen (Maus-Eintritt) → Merker = `false`.
- Sobald irgendeine Funktion ausgelöst wird → Merker = `true`.
- Beim Verlassen der Maus: Verzögerung = `benutztSeitAufklappen ? 2 s : 5 s`.

### 5.3 Was als „Funktion benutzt" zählt

Jede ausgelöste Aktion: Aufnahme (Mic), BTW/Zwischenfrage, W (Whisper), G (Gemini),
X (Zeile löschen), Kopieren, Einfügen, Screenshot, Screenshots einfügen, Auto-Enter,
Profil-Umschaltung, Promptboard/Stern. Kurz: jeder Button-Click und jede abgeschlossene Aufnahme.

## 6. Weitere Festlegungen

- **Start-Zustand:** beim App-Start **Eingeklappt** (nur Mic).
- **Animation:** weiches Ein-/Ausblenden ~150 ms (Fade/Resize), kein hartes Springen.
- **Einstellungs-Schalter:** neue Option **„Overlay automatisch einklappen"** (Standard: **an**).
  - `an` = beschriebenes Auto-Hide-Verhalten.
  - `aus` = bisheriges Immer-sichtbar-Overlay (voll ausgeklappt, keine Timer, keine Hover-Logik).
  - Wird über die bestehende Config-/Settings-Mechanik der App persistiert.
- **Timer-Werte:** 2 s und 5 s als feste Konstanten im Code (leicht änderbar). **Kein** eigenes
  Bedien-Element dafür (YAGNI — würde die Oberfläche überladen).

## 7. Randfälle

- **Hit-Testing eingeklappt:** Das geschrumpfte Fenster deckt nur den Mic-Button ab; der Rest
  des Bildschirms (CLI) bleibt voll klick- und lesbar.
- **Maus-über bei transparentem Fenster:** Hover/Leave wird über den sichtbaren, opaken
  Inhalt erkannt (vollständig transparente Pixel lösen ohnehin keine Maus-Ereignisse aus).
- **Resize-Flackern:** Da der Mic-Button beim Ein-/Ausklappen unter der Maus bleibt, darf kein
  unbeabsichtigtes `MouseLeave` ausgelöst werden, das sofort wieder einklappt. Die Hover-Region
  umfasst den Mic-Button in beiden Zuständen.
- **Aufnahme während eingeklappt:** Hover → Ausklappen → Mic klicken → Aufnahme; während der
  Aufnahme bleibt es offen, danach greift der 2-s-Timer (sobald Maus draußen).
- **Schalter `aus`:** keine Hover-/Timer-Logik aktiv, Fenster bleibt fest in voller Größe.

## 8. Nicht im Umfang (YAGNI)

- Keine konfigurierbaren Timer-Zeiten in der Oberfläche.
- Keine zusätzlichen Zustände (z. B. „halb ausgeklappt").
- Keine Änderung an Aufnahme-, Whisper-, Gemini- oder Terminal-Logik — nur Sichtbarkeit/Geometrie.

## 9. Voraussichtlich betroffene Dateien

**Windows (je Projekt, TerminalVoiceOverlay-Windows + ClaudeVoiceOverlay-Windows):**
- `Views/OverlayWindow.xaml` — ggf. benannte Bereiche für Ein-/Ausblenden.
- `Views/OverlayWindow.xaml.cs` — Hover-Handler, `DispatcherTimer`, Resize/Reposition, Zustandslogik.
- `Views/SettingsDialog.xaml` (+ `.cs`) — neuer Schalter.
- `Services/Config.cs` — Persistenz der neuen Option.

**macOS (je Projekt, TerminalVoiceOverlay-macOS + ClaudeCodexVoiceOverlay-macOS):**
- `OverlayPanel.swift` — `NSTrackingArea`, `Timer`, Resize/Reposition des `NSPanel`, Zustandslogik.
- `Config.swift` — Persistenz der neuen Option.
- `AppDelegate.swift` / Einstellungs-UI — neuer Schalter.

> Hinweis: Windows-Projekte sind ~80 % code-gleich (Sister-Projekt-Regel aus
> `TerminalVoiceOverlay-Windows/CLAUDE.md`). Änderungen an gemeinsamen Dateien müssen in beiden
> Projekten gespiegelt werden. Gleiches gilt sinngemäß für die beiden macOS-Projekte.
