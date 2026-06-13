# ClaudeCodexVoiceOverlay — macOS

Ein schwebendes macOS-Overlay fuer Spracheingabe in **Claude Desktop** (`com.anthropic.claudefordesktop`) und **Codex** (`com.openai.codex`). Per Knopfdruck wird Sprache aufgenommen, via **Groq Whisper** in Text umgewandelt und direkt in die App eingefuegt. Optional korrigiert **Google Gemini** den transkribierten Text vor dem Einfuegen — optimiert fuer Programmier-Anweisungen an KI-Coding-Tools wie Claude Code.

---

> **Hinweis (2026-06):** Dieses Overlay wurde vollstaendig auf den
> Funktionsumfang des `TerminalVoiceOverlay-macOS` gebracht. Frueher hatte es
> nur fuenf Buttons; jetzt enthaelt es PromptBoard, Prompt-Historie, Gemini-
> Profile, Push-to-Talk, Waveform, Einstellungs-Dialog, Google-Drive-Backup und
> alle globalen Hotkeys. Der einzige Unterschied zum Terminal-Overlay ist die
> Ziel-App-Erkennung (Electron-Apps statt Terminals) und die Lösch-Tastenkombi.

## Features

### Sprache & Korrektur
- **Voice-to-Text**: Mikrofon-Button druecken, sprechen, nochmal druecken — der transkribierte Text wird direkt eingefuegt
- **Gemini-Korrektur** (optional): Verbessert Grammatik und erkennt falsch transkribierte englische Fachbegriffe — optimiert fuer Programmier-Kontext
- **Gemini-Profile 1–10**: Verschiedene Korrektur-Profile (1=Standard, 2=Programmierung, 3=Meta, 4–10 frei belegbar). Linksklick auf ein Profil-Kaestchen schickt die letzte Aufnahme erneut durch das gewaehlte Profil (Re-Correct), Rechtsklick wechselt nur das Profil
- **Whisper-Undo**: Falls die Gemini-Korrektur nicht gefaellt, kann der originale Whisper-Rohtext per W-Button eingefuegt werden
- **BTW-Button**: Separate Aufnahme fuer kurze Zwischenfragen (wird mit `/btw` eingeleitet)
- **Push-to-Talk**: Hotkey gedrueckt halten (≥500 ms) = Aufnahme laeuft solange gehalten; kurzer Tap = Start/Stop-Umschaltung
- **Waveform**: Live-Pegelanzeige (14 Balken) waehrend der Aufnahme
- **Auto-Enter**: Optionaler Toggle, der den Text nach dem Einfuegen automatisch absendet (Return-Taste)
- **Audio-Feedback**: Systemton beim Start und Stopp der Aufnahme — kein Hinschauen noetig

### PromptBoard (Prompt-Bibliothek)
- **PromptBoard**: Verwaltet Kategorien und gespeicherte Prompts in einer SQLite-Datenbank, andockbar neben dem Overlay (Stern-Button)
- **Always-On-Prompts**: Pre-/Post-Prompts, die automatisch vor/nach jedem Diktat mitgeschickt werden
- **Prompt-Eingabefenster**: Grosses Freitext-Feld zum Tippen/Editieren mit Gemini-Verbesserung und Aufgaben-Trenner (`;`)
- **Prompt-Historie**: Die letzten 100 Eingaben werden gespeichert (mit KI-generiertem 4-Wort-Titel), aeltere wandern in ein Markdown-Archiv
- **Hotkey-Prompts**: Prompts koennen Tasten zugewiesen werden (Cmd+1..9, Cmd+Opt+A..Z) und per Hotkey sofort eingefuegt werden
- **Google-Drive-Backup**: PromptBoard, Historie und Slots werden optional ueber Google Drive zwischen Geraeten synchronisiert

### Overlay & Bedienung
- **Copy/Paste/Screenshot**: Buttons zum Kopieren, Einfuegen, Bildschirmfoto aufnehmen und das letzte Foto einfuegen
- **Orientierung umschalten**: Vertikale Saeule ↔ horizontale Leiste (⇄-Button oder Cmd+Shift+O)
- **Auto-Hide / Collapsed-Pille**: Bei Inaktivitaet klappt das Overlay zu einer kleinen Mic-Pille zusammen
- **Position merken**: Disketten-Button speichert die Overlay-Position (pro Orientierung)
- **Automatische Sichtbarkeit**: Das Overlay erscheint nur, wenn Claude Desktop oder Codex aktiv ist, und versteckt sich automatisch
- **Zeile loeschen**: X-Button loescht die aktuelle Eingabe im Textfeld (Cmd+A + Backspace); gedrueckt halten loescht wiederholt
- **Stream-Deck-Anbindung**: HTTP-Server auf `127.0.0.1:5723` fuer Auto-Enter-Status/Toggle
- **Kein Dock-Icon**: Kein Dock-Icon, kein Cmd+Tab-Eintrag (LSUIElement)
- **Menueleisten-Icon**: Zum Anzeigen und Beenden der App
- **Koexistenz**: Kann gleichzeitig mit TerminalVoiceOverlay laufen (siehe Hinweis weiter unten zu Hotkey-/Port-Kollisionen)

---

## Voraussetzungen

### 1. Git

Git wird benoetigt, um das Repository von GitHub herunterzuladen.

Pruefen ob Git bereits installiert ist:

```
git --version
```

Falls nicht vorhanden, Xcode Command Line Tools installieren (enthaelt Git):

```
xcode-select --install
```

### 2. Xcode Command Line Tools

Werden fuer den Swift-Compiler (`swiftc`) benoetigt, um die App zu bauen:

```
xcode-select --install
```

> Falls bereits installiert, erscheint eine Meldung, dass die Tools schon vorhanden sind.

### 3. macOS 13.0 oder hoeher

Die App nutzt AppKit-APIs, die macOS 13.0 (Ventura) oder hoeher erfordern.

### 4. Groq API-Key (erforderlich)

Groq stellt die Whisper-API fuer die Spracherkennung bereit. Du brauchst einen kostenlosen API-Key.

1. Gehe zu: **https://console.groq.com/**
2. Erstelle ein Konto oder melde dich an
3. Gehe zu **API Keys** und erstelle einen neuen Key
4. Kopiere den Key — du brauchst ihn gleich fuer die `.env`-Datei

### 5. Gemini API-Key (optional, aber empfohlen)

Gemini korrigiert den transkribierten Text — speziell optimiert fuer Programmier-Anweisungen. Ohne Gemini-Key wird der Rohtext von Whisper direkt eingefuegt.

1. Gehe zu: **https://aistudio.google.com/apikey**
2. Melde dich mit deinem Google-Konto an
3. Erstelle einen API-Key und kopiere ihn

---

## Installation

### 1. Repository klonen

Oeffne ein Terminal und wechsle in den Ordner, in dem du das Projekt speichern moechtest:

```
cd ~/Documents
```

Klone das Repository von GitHub:

```
git clone https://github.com/Pepsi1978/proggs.git
```

Wechsle in den Projektordner:

```
cd proggs/ClaudeCodexVoiceOverlay-macOS
```

Falls du das Repository bereits hast, hole den aktuellen Stand:

```
cd ~/Documents/proggs
```

```
git pull origin main
```

```
cd ClaudeCodexVoiceOverlay-macOS
```

### 2. Konfiguration (.env-Datei)

Erstelle die `.env`-Datei aus der Vorlage:

```
cp .env.example .env
```

Oeffne die Datei zum Bearbeiten:

```
nano .env
```

Trage mindestens deinen **Groq API-Key** ein. Die wichtigsten Einstellungen:

| Variable | Pflicht | Beschreibung |
|---|---|---|
| `GROQ_API_KEY` | Ja | Dein Groq API-Key fuer Whisper Speech-to-Text |
| `GEMINI_API_KEY` | Nein | Google Gemini API-Key fuer Textkorrektur |

> **Wichtig:** Die `.env`-Datei enthaelt deine geheimen API-Keys. Sie darf **niemals** auf GitHub hochgeladen werden.

Die `.env`-Datei wird in folgender Reihenfolge gesucht:

1. Neben der App (im `.app`-Bundle)
2. Im Projektverzeichnis
3. In `~/.config/ClaudeCodexVoiceOverlay/.env`

### 3. App bauen

```
./build.sh
```

> Das Build-Skript kompiliert die Swift-Quelldateien und erstellt `build/ClaudeCodexVoiceOverlay.app`.

### 4. Starten

```
open build/ClaudeCodexVoiceOverlay.app
```

---

## Berechtigungen

Beim ersten Start werden zwei Berechtigungen abgefragt:

1. **Mikrofon**: Fuer die Sprachaufnahme — im macOS-Dialog erlauben
2. **Accessibility**: Fuer Tastatureingabe (Cmd+V, Cmd+A+Backspace) — unter Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen die App hinzufuegen

---

## Autostart (LaunchAgent)

LaunchAgent installieren, damit die App automatisch beim Login startet:

```
launchctl load ~/Library/LaunchAgents/com.frank.claudecodexvoiceoverlay.plist
```

LaunchAgent deaktivieren:

```
launchctl unload ~/Library/LaunchAgents/com.frank.claudecodexvoiceoverlay.plist
```

---

## Bedienung

Das Overlay zeigt (in der vertikalen Saeule) folgende Buttons:

| Button | Funktion |
|---|---|
| **★ Stern** | PromptBoard / Prompt-Eingabe oeffnen, Always-On-Prompts aktivieren |
| **⇄** | Orientierung umschalten (vertikal ↔ horizontal) |
| **Mikrofon** 🎤 | Aufnahme starten/stoppen — transkribierter Text wird eingefuegt |
| **BTW** | Zwischenfrage-Aufnahme (wird mit `/btw` eingeleitet) |
| **1–10** | Gemini-Profil waehlen. Linksklick = Re-Correct der letzten Aufnahme, Rechtsklick = nur Profil wechseln |
| **W** | Whisper-Undo: Ersetzt den Gemini-korrigierten Text durch den originalen Whisper-Rohtext |
| **G** | Gemini-Korrektur an/aus (gruen = an, grau = aus) |
| **X** | Loescht die aktuelle Eingabe (Cmd+A + Backspace); gedrueckt halten loescht wiederholt |
| **Copy** | Auswahl in der Ziel-App kopieren (Cmd+C) |
| **Paste** | Zwischenablage in die Ziel-App einfuegen (Cmd+V) |
| **Screenshot** | Bildschirmfoto aufnehmen (nach ~/Pictures/Screenshots) |
| **Insert-Screenshot** | Pfad des letzten Screenshots einfuegen |
| **⏎** | Auto-Enter an/aus (gruen = an, grau = aus) — sendet Text nach dem Einfuegen automatisch ab |
| **Diskette** | Aktuelle Overlay-Position merken (pro Orientierung); erneut = zuruecksetzen |

### Globale Hotkeys

Funktionieren systemweit, auch wenn das Overlay im Hintergrund ist (Carbon-API, keine Accessibility noetig):

| Hotkey | Funktion |
|---|---|
| **Cmd+Shift+R** | Aufnahme umschalten (Tap) / Push-to-Talk (≥500 ms halten) |
| **Cmd+Shift+S** | Screenshot aufnehmen |
| **Cmd+Shift+I** | Letzten Screenshot einfuegen |
| **Cmd+Shift+O** | Orientierung umschalten |
| **Cmd+Shift+C** | Collapsed-Pille umschalten |
| **Cmd+Shift+,** | Einstellungs-Dialog oeffnen |
| **Cmd+Shift+E** | Release-Bundle-Ordner im Finder oeffnen |
| **Cmd+1 … Cmd+9** | Prompt mit zugewiesener Nummer einfuegen |
| **Cmd+Opt+A … Z** | Prompt mit zugewiesenem Buchstaben einfuegen |

### Mikrofon-Farben

| Farbe | Bedeutung |
|---|---|
| Dunkelgrau | Bereit (Idle) |
| Rot (pulsierend) | Aufnahme laeuft |
| Orange | Verarbeitung (Transkription / Korrektur) |
| Gruen | Erfolgreich eingefuegt |

### Audio-Feedback

| Ton | Bedeutung |
|---|---|
| Systemton | Aufnahme gestartet |
| Systemton | Aufnahme gestoppt |

### Workflow

1. Claude Desktop oder Codex oeffnen
2. Das Overlay erscheint automatisch am rechten Bildschirmrand
3. Mikrofon-Button klicken — Aufnahme startet (Button pulsiert rot, Systemton)
4. Sprechen
5. Mikrofon-Button nochmal klicken — Aufnahme stoppt (Systemton)
6. Text wird transkribiert, optional von Gemini korrigiert, und ins Textfeld eingefuegt
7. Falls Gemini-Korrektur nicht gefaellt: W-Button druecken — ersetzt durch Whisper-Rohtext
8. Falls Auto-Enter (⏎) aktiviert ist, wird der Text automatisch abgesendet

### Mehrere Eingaben hintereinander

Bei aufeinanderfolgenden Spracheingaben wird automatisch ein Leerzeichen zwischen den Texten eingefuegt, damit keine Woerter zusammenkleben. Das Leerzeichen wird zurueckgesetzt, wenn:
- Die Zeile geloescht wird (X-Button)
- Auto-Enter aktiv ist (nach dem Enter beginnt eine neue Zeile)

---

## Projektstruktur

```
ClaudeCodexVoiceOverlay-macOS/
  .env.example              — Vorlage fuer die Konfiguration
  build.sh                  — Build-Skript (kompiliert die App)
  Info.plist                — App-Konfiguration (LSUIElement, Bundle-ID)
  ClaudeCodexVoiceOverlay/
    main.swift                  — Einstiegspunkt + Single-Instance-Guard
    AppDelegate.swift           — Zentrale App-Logik, Orchestrierung (definiert tvoDebug)
    OverlayPanel.swift          — Overlay-UI (Buttons, Sektionen, Pulse/Waveform)
    OverlayExtraButtons.swift   — Orientierungs-Toggle + Disketten-Button
    OverlayCollapsedMic.swift   — Collapsed-Mic-Pille + Beam-Crossfade
    OverlayOrientation.swift    — Orientierungs-/Positionslogik
    OverlayHorizontalLayout.swift — Horizontales Leisten-Layout
    OverlayGlideAnimation.swift — Glide-Animation beim Verschieben
    WaveformView.swift          — 14-Balken-Pegelanzeige
    InputController.swift       — Tastatureingabe fuer Electron (Cmd+V, Cmd+A+Backspace, Return)
    AppWatcher.swift            — Erkennt aktive Ziel-Apps (Claude Desktop, Codex)
    AudioRecorder.swift         — Mikrofon-Aufnahme via AVAudioEngine (WAV) + onLevel
    GroqWhisperClient.swift     — Groq Whisper API-Client fuer Transkription
    GeminiClient.swift          — Gemini-Client: Profile 1–10, Titel/Slot-Summary, Prompt-Engineer
    Config.swift                — .env-Datei laden und parsen
    ErrorDescriptions.swift     — Benutzerfreundliche Fehlermeldungen fuer API-Fehler
    PushToTalkController.swift  — Push-to-Talk Hold-Erkennung
    HotkeyRegistry.swift        — Globale Carbon-Hotkeys
    AutoHideController.swift    — Auto-Collapse zur Mic-Pille
    AutoEnterStatusServer.swift — HTTP-Server (127.0.0.1:5723) fuer Stream Deck
    VoiceServiceProvider.swift  — Geteilter Audio/STT/Gemini-Locator
    AlwaysOnPrefixService.swift — Pre/Post-Always-On-Prompt-Kette
    PromptBoardModels.swift     — Datenmodelle (Kategorie, Prompt, Settings)
    PromptBoardStore.swift      — SQLite-Persistenz fuer PromptBoard
    PromptHistoryStore.swift    — Prompt-Historie (100 aktiv + MD-Archiv)
    PromptSlotStore.swift       — Prompt-Zwischenspeicher-Slots
    GoogleDriveBackupService.swift — Backup/Sync ueber Google Drive appDataFolder
    PromptBoardPanel.swift      — PromptBoard-Hauptfenster
    PromptInputPanel.swift      — Grosses Freitext-Eingabefenster
    PromptHistoryPanel.swift    — Historie-Ansicht
    PromptBoardDialogs.swift    — Prompt-Edit-/Historie-Edit-Dialoge
    CommonDialogs.swift         — Bestaetigungs-/Text-Eingabe-Dialoge
    SettingsDialog.swift        — Einstellungs-Dialog
```

> **Datenspeicherung:** Alle lokalen Daten liegen unter
> `~/Library/Application Support/ClaudeCodexVoiceOverlay/` (PromptBoard-DB,
> `history/`, `slots/`) — bewusst getrennt vom TerminalVoiceOverlay, damit beide
> Apps kollisionsfrei nebeneinander laufen. Sollen beide Overlays dieselbe
> Prompt-Bibliothek teilen, koennen die Pfade in `PromptBoardStore`,
> `PromptHistoryStore` und `PromptSlotStore` auf ein gemeinsames Verzeichnis
> gezeigt werden.

> **Gleichzeitiger Betrieb mit TerminalVoiceOverlay:** Beide Apps duerfen
> parallel laufen (jedes Overlay zeigt sich nur ueber seinen eigenen Ziel-Apps).
> Zwei prozessweite Ressourcen kollidieren aber: die **globalen Hotkeys** (wer
> zuerst startet, gewinnt — die identischen Hotkeys der zweiten App feuern
> nicht) und der **AutoEnter-Port 5723** (der zweite Bind schlaegt fehl, wird
> aber sauber abgefangen — Stream Deck spricht mit der zuerst gestarteten App).

---

## Schwester-Projekte

Dieses Projekt ist Teil einer Familie von Voice-Overlay-Apps. Alle teilen die gleiche Architektur. Nach der Vollportierung (2026-06) teilt es ~95% des Codes mit `TerminalVoiceOverlay-macOS`:

| Projekt | Plattform | Ziel-Apps | Sprache |
|---|---|---|---|
| TerminalVoiceOverlay-macOS | macOS | Terminal.app, iTerm2, Warp | Swift / AppKit |
| **ClaudeCodexVoiceOverlay-macOS** | macOS | Claude Desktop, Codex | Swift / AppKit |
| TerminalVoiceOverlay-Windows | Windows | Windows Terminal, PowerShell | C# / WPF |
| ClaudeVoiceOverlay-Windows | Windows | Claude Desktop, Codex | C# / WPF |

**Wichtig:** Bei Aenderungen an einem Projekt muessen die Schwester-Projekte ebenfalls aktualisiert werden, da sie den gleichen Code fuer Groq Whisper, Gemini, Audio-Aufnahme und UI-Logik verwenden. Nur die Ziel-App-Erkennung und Tastatureingabe unterscheiden sich.

### Architektur-Mapping macOS ↔ Windows

| macOS (Swift) | Windows (C#) | Funktion |
|---|---|---|
| AppDelegate.swift | OverlayWindow.xaml.cs | Zentrale App-Logik |
| OverlayPanel.swift | OverlayWindow.xaml | UI (fuenf runde Buttons) |
| InputController.swift | AppController.cs | Tastatureingabe (Cmd+V / Ctrl+V) |
| AudioRecorder.swift | AudioRecorder.cs | Mikrofon-Aufnahme |
| GroqWhisperClient.swift | GroqWhisperClient.cs | Groq API |
| GeminiClient.swift | GeminiClient.cs | Gemini API |
| Config.swift | Config.cs | .env laden |
| AppWatcher.swift | AppWatcher.cs | Fenster-Erkennung |
| ErrorDescriptions.swift | (inline in OverlayWindow) | Fehlermeldungen |
| build.sh | publish.ps1 | Build-Script |

---

## Letzte Aenderungen

### 2026-06 — Vollportierung vom TerminalVoiceOverlay
- Kompletter Funktionsumfang des `TerminalVoiceOverlay-macOS` uebernommen: PromptBoard (SQLite), Prompt-Historie + Archiv, Prompt-Slots, Gemini-Profile 1–10 + Re-Correct, BTW-Aufnahme, Push-to-Talk, Waveform, Copy/Paste/Screenshot, Orientierungs-Umschaltung, Collapsed-Pille/Auto-Hide, Disketten-Positionsspeicher, Einstellungs-Dialog, Google-Drive-Backup, AutoEnter-HTTP-Server, globale Carbon-Hotkeys
- `InputController` um `clearAllInput` und einen robusten `activateTargetApp` (mit Fallback-Kette) erweitert; Lösch-Kombi bleibt Cmd+A+Backspace (Electron)
- `AppWatcher` auf die Terminal-Overlay-Struktur gebracht (statische `targetBundleIDs`/`isTargetApp`, Eigen-Prozess-Ignorierung) mit Electron-App-IDs
- Daten liegen app-spezifisch unter `~/Library/Application Support/ClaudeCodexVoiceOverlay/` (unabhaengig vom Terminal-Overlay)
- `build.sh` kompiliert jetzt alle 34 Dateien und linkt zusaetzlich Carbon, Network und sqlite3

### 2026-03-12

- Fix: Force-unwrap in Config.swift durch sicheren Optional-Zugriff ersetzt
- Fix: CoreFoundation takeRetainedValue → takeUnretainedValue (Crash-Verhinderung)
- Fix: deinit mit removeObserver in AppWatcher (Memory-Leak-Verhinderung)
- Fix: Thread-Safety — Groq-Callback auf Main Thread gewrappt
- Fix: Thread.sleep in Retry-Logik durch DispatchQueue.asyncAfter ersetzt (non-blocking)
- Fix: Zwischenablage wird vor dem Einfuegen gesichert und danach wiederhergestellt
- Fix: Debug-Logging in #if DEBUG gewrappt (Release-Build sauber)
- Fix: Auto-Enter Sleep von 500ms auf 300ms reduziert
- Fix: .data(using: .utf8)! durch Data(_.utf8) ersetzt (kein Force-unwrap)
- Refactoring: Fehlerbeschreibungen in eigene Datei ErrorDescriptions.swift ausgelagert

---

## Haeufige Probleme

| Problem | Loesung |
|---|---|
| `swiftc wird nicht erkannt` | Xcode Command Line Tools installieren: `xcode-select --install` |
| `git wird nicht erkannt` | Xcode Command Line Tools installieren (enthaelt Git) |
| Overlay erscheint nicht | Pruefen ob Claude Desktop oder Codex im Vordergrund ist |
| Mikrofon funktioniert nicht | Systemeinstellungen > Datenschutz & Sicherheit > Mikrofon — App erlauben |
| Text wird nicht eingefuegt | Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen — App hinzufuegen |
| Transkription schlaegt fehl | `GROQ_API_KEY` in der `.env` pruefen. Ist der Key gueltig? |
| Gemini-Korrektur funktioniert nicht | `GEMINI_API_KEY` pruefen. Ohne Key wird Gemini automatisch deaktiviert (kein Fehler) |
| App startet nicht nach Rebuild | Alte App beenden (Menueleiste > Beenden), dann neu starten |

---

## Technologie

- **Swift / AppKit** — Natives macOS-Overlay (NSPanel, floating, non-activating)
- **AVAudioEngine** — Mikrofon-Aufnahme (WAV)
- **Groq Whisper API** — Speech-to-Text
- **Google Gemini API** — Textkorrektur fuer Programmier-Kontext (optional)
- **CGEvent** — Tastatureingabe-Simulation (Cmd+V, Cmd+A, Return)
