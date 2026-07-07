# ClaudeCodexVoiceOverlay — macOS

Ein schwebendes macOS-Overlay fuer Spracheingabe in **Claude Desktop** (`com.anthropic.claudefordesktop`) und **Codex** (`com.openai.codex`). Per Knopfdruck wird Sprache aufgenommen, via **Groq Whisper** in Text umgewandelt und direkt in die App eingefuegt. Optional korrigiert **Google Gemini** den transkribierten Text vor dem Einfuegen — optimiert fuer Programmier-Anweisungen an KI-Coding-Tools wie Claude Code.

---

## Features

- **Voice-to-Text**: Mikrofon-Button druecken, sprechen, nochmal druecken — der transkribierte Text wird direkt eingefuegt
- **Gemini-Korrektur** (optional): Verbessert Grammatik und erkennt falsch transkribierte englische Fachbegriffe — optimiert fuer Programmier-Kontext
- **Whisper-Undo**: Falls die Gemini-Korrektur nicht gefaellt, kann der originale Whisper-Rohtext per W-Button eingefuegt werden
- **Auto-Enter**: Optionaler Toggle, der den Text nach dem Einfuegen automatisch absendet (Return-Taste)
- **Audio-Feedback**: Systemton beim Start und Stopp der Aufnahme — kein Hinschauen noetig
- **Leerzeichen zwischen Eingaben**: Bei mehreren aufeinanderfolgenden Spracheingaben wird automatisch ein Leerzeichen eingefuegt
- **Automatische Sichtbarkeit**: Das Overlay erscheint nur, wenn Claude Desktop oder Codex aktiv ist, und versteckt sich automatisch
- **Zeile loeschen**: X-Button loescht die aktuelle Eingabe im Textfeld (Cmd+A + Backspace)
- **Kein Dock-Icon**: Kein Dock-Icon, kein Cmd+Tab-Eintrag (LSUIElement)
- **Menueleisten-Icon**: Zum Anzeigen und Beenden der App
- **Koexistenz**: Kann gleichzeitig mit TerminalVoiceOverlay laufen

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

Das Overlay zeigt fuenf runde Buttons:

| Button | Funktion |
|---|---|
| **X** | Loescht die aktuelle Eingabe im Textfeld (Cmd+A + Backspace) |
| **Mikrofon** 🎤 | Aufnahme starten/stoppen — transkribierter Text wird eingefuegt |
| **W** | Whisper-Undo: Ersetzt den Gemini-korrigierten Text durch den originalen Whisper-Rohtext |
| **G** | Gemini-Korrektur an/aus (gruen = an, grau = aus) |
| **⏎** | Auto-Enter an/aus (gruen = an, grau = aus) — sendet Text nach dem Einfuegen automatisch ab |

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
    main.swift              — Einstiegspunkt der App
    AppDelegate.swift       — Zentrale App-Logik, Zustandsmanagement
    OverlayPanel.swift      — Overlay-UI (fuenf runde Buttons, Pulse-Animation)
    InputController.swift   — Tastatureingabe-Simulation (Cmd+V, Cmd+A, Return)
    AudioRecorder.swift     — Mikrofon-Aufnahme via AVAudioEngine (WAV)
    GroqWhisperClient.swift — Groq Whisper API-Client fuer Transkription
    GeminiClient.swift      — Google Gemini API-Client fuer Textkorrektur (Programmier-Kontext)
    Config.swift            — .env-Datei laden und parsen
    AppWatcher.swift        — Erkennt aktive Ziel-Apps (Claude Desktop, Codex)
    ErrorDescriptions.swift  — Benutzerfreundliche Fehlermeldungen fuer API-Fehler
```

---

## Schwester-Projekte

Dieses Projekt ist Teil einer Familie von Voice-Overlay-Apps. Alle teilen die gleiche Architektur und ca. 80% des Codes:

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

## Letzte Aenderungen (2026-03-12)

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
