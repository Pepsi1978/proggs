# TerminalVoiceOverlay — Windows

Ein schwebendes WPF-Overlay, das sich automatisch am Rand des aktiven Terminals einblendet. Per Knopfdruck wird Sprache aufgenommen, via **Groq Whisper** in Text umgewandelt und direkt ins Terminal eingefuegt. Optional korrigiert **Google Gemini** den transkribierten Text vor dem Einfuegen — optimiert fuer Programmier-Anweisungen an KI-Coding-Tools wie Claude Code.

---

## Features

- **Voice-to-Text**: Mikrofon-Button druecken, sprechen, nochmal druecken — der transkribierte Text wird direkt ins Terminal eingefuegt
- **Gemini-Korrektur** (optional): Verbessert Grammatik und erkennt falsch transkribierte englische Fachbegriffe — optimiert fuer Programmier-Kontext
- **Whisper-Undo**: Falls die Gemini-Korrektur nicht gefaellt, kann der originale Whisper-Rohtext per W-Button eingefuegt werden
- **Auto-Enter**: Optionaler Toggle, der den Text nach dem Einfuegen automatisch absendet (Enter-Taste)
- **Audio-Feedback**: Kurzer Ton beim Start der Aufnahme, Doppel-Ton beim Stopp — kein Hinschauen noetig
- **Leerzeichen zwischen Eingaben**: Bei mehreren aufeinanderfolgenden Spracheingaben wird automatisch ein Leerzeichen eingefuegt
- **Automatische Sichtbarkeit**: Das Overlay erscheint nur, wenn ein Terminal-Fenster aktiv ist, und versteckt sich automatisch
- **Multi-Monitor**: Das Overlay positioniert sich immer auf dem Monitor des aktiven Terminals
- **Zeile loeschen**: X-Button loescht die aktuelle Zeile im Terminal
- **System-Tray**: Laeuft unauffaellig im Hintergrund mit Tray-Icon
- **Watcher**: Optionaler Watcher startet das Overlay automatisch neu, falls es sich beendet
- **Autostart**: Kann sich beim Windows-Login automatisch starten

---

## Voraussetzungen

### 1. Git

Git wird benoetigt, um das Repository von GitHub herunterzuladen.

Download: **https://git-scm.com/download/win**

> Lade den **64-bit Git for Windows Setup** Installer herunter und fuehre ihn aus. Die Standardeinstellungen bei der Installation koennen beibehalten werden.

Nach der Installation pruefen — oeffne ein neues Terminal und fuehre aus:

```powershell
git --version
```

> Es muss eine Versionsnummer wie `git version 2.x.x` angezeigt werden.

### 2. .NET 8 SDK

Das Projekt ist eine WPF-App, die mit .NET 8 gebaut wird. Du brauchst das SDK zum Kompilieren.

Download: **https://dotnet.microsoft.com/download/dotnet/8.0**

> Lade den **SDK-Installer** herunter (nicht nur die Runtime). Waehle **Windows x64**.

Nach der Installation pruefen — oeffne ein **neues** Terminal und fuehre aus:

```powershell
dotnet --version
```

> Es muss eine Versionsnummer wie `8.0.x` angezeigt werden. Falls der Befehl nicht erkannt wird, starte das Terminal neu.

### 3. Groq API-Key (erforderlich)

Groq stellt die Whisper-API fuer die Spracherkennung bereit. Du brauchst einen kostenlosen API-Key.

1. Gehe zu: **https://console.groq.com/**
2. Erstelle ein Konto oder melde dich an
3. Gehe zu **API Keys** und erstelle einen neuen Key
4. Kopiere den Key — du brauchst ihn gleich fuer die `.env`-Datei

### 4. Gemini API-Key (optional, aber empfohlen)

Gemini korrigiert den transkribierten Text — speziell optimiert fuer Programmier-Anweisungen. Ohne Gemini-Key wird der Rohtext von Whisper direkt eingefuegt.

1. Gehe zu: **https://aistudio.google.com/apikey**
2. Melde dich mit deinem Google-Konto an
3. Erstelle einen API-Key und kopiere ihn

---

## Installation

### 1. Repository klonen

Oeffne ein Terminal (Windows Terminal oder PowerShell) und wechsle in den Ordner, in dem du das Projekt speichern moechtest:

```powershell
cd $HOME\Documents
```

Klone das Repository von GitHub:

```powershell
git clone https://github.com/Pepsi1978/proggs.git
```

Wechsle in den Projektordner:

```powershell
cd proggs\TerminalVoiceOverlay-Windows
```

Falls du das Repository bereits hast, hole den aktuellen Stand:

```powershell
cd $HOME\Documents\proggs
```

```powershell
git pull origin main
```

```powershell
cd TerminalVoiceOverlay-Windows
```

### 2. Projekt kompilieren

Das Skript `publish.ps1` baut eine einzelne, ausfuehrbare `.exe`-Datei. Diese ist self-contained — das bedeutet, auf dem Zielrechner muss kein .NET installiert sein, um die fertige `.exe` auszufuehren:

```powershell
pwsh -File publish.ps1
```

> Die fertige `TerminalVoiceOverlay.exe` liegt danach im Ordner `publish/`. Die Datei ist ca. 60-80 MB gross, da die komplette .NET-Runtime enthalten ist.

**Hinweis:** Falls `pwsh` nicht gefunden wird, nutze stattdessen:

```powershell
powershell -File publish.ps1
```

### 3. Konfiguration (.env-Datei)

Erstelle die `.env`-Datei aus der Vorlage:

```powershell
copy .env.example publish\.env
```

Oeffne die Datei zum Bearbeiten:

```powershell
notepad publish\.env
```

Trage mindestens deinen **Groq API-Key** ein. Die wichtigsten Einstellungen:

| Variable | Pflicht | Beschreibung |
|---|---|---|
| `GROQ_API_KEY` | Ja | Dein Groq API-Key fuer Whisper Speech-to-Text |
| `WHISPER_MODEL` | Nein | Whisper-Modell (Standard: `whisper-large-v3-turbo`) |
| `WHISPER_LANG` | Nein | Sprache der Aufnahme (Standard: `de` fuer Deutsch) |
| `GEMINI_API_KEY` | Nein | Google Gemini API-Key fuer Textkorrektur |
| `GEMINI_MODEL` | Nein | Gemini-Modell (Standard: `gemini-3.1-flash-lite`) |
| `GEMINI_THINKING_LEVEL` | Nein | Thinking-Level fuer Gemini 3.x: `LOW`, `MEDIUM` oder `HIGH` |
| `TERMINAL_PROCESS_NAMES` | Nein | Kommagetrennte Terminal-Prozessnamen (Standard: `WindowsTerminal,pwsh,powershell`) |

> **Wichtig:** Die `.env`-Datei enthaelt deine geheimen API-Keys. Sie darf **niemals** auf GitHub hochgeladen werden.

Die `.env`-Datei wird in folgender Reihenfolge gesucht:

1. Neben der `.exe` (empfohlen)
2. Im aktuellen Arbeitsverzeichnis
3. In `%USERPROFILE%\.env`
4. In `%APPDATA%\TerminalVoiceOverlay\.env`

### 4. Starten

Starte das Overlay:

```powershell
.\publish\TerminalVoiceOverlay.exe
```

Das Overlay erscheint automatisch am rechten Rand, sobald ein Terminal-Fenster (Windows Terminal, PowerShell) aktiv ist.

> **Hinweis:** Falls Windows Smart App Control oder ein Virenscanner die `.exe` beim ersten Start blockiert, musst du die Datei manuell erlauben (Rechtsklick → „Trotzdem ausfuehren" oder eine Ausnahme im Virenscanner hinzufuegen).

---

## Autostart einrichten

Damit das Overlay bei jedem Windows-Login automatisch startet:

```powershell
pwsh -File install_autostart.ps1
```

> Das Skript registriert den **Watcher** mit höchsten Privilegien im Aufgabenplaner. Der Watcher überwacht alle 3 Sekunden, ob das Overlay läuft, und startet es automatisch neu, falls es sich beendet hat. Die einmalige Einrichtung benötigt eine administrative PowerShell; danach startet die Aufgabe bei jeder Anmeldung ohne erneute UAC-Abfrage.

Um den Autostart wieder zu entfernen:

```powershell
pwsh -File uninstall_autostart.ps1
```

---

## Bedienung

Das Overlay zeigt fuenf runde Buttons:

| Button | Funktion |
|---|---|
| **X** | Loescht die aktuelle Zeile im Terminal (simuliert End + Backspace) |
| **Mikrofon** 🎤 | Aufnahme starten/stoppen — transkribierter Text wird ins Terminal eingefuegt |
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
| Kurzer hoher Ton (880Hz) | Aufnahme gestartet |
| Absteigender Doppel-Ton (660Hz → 440Hz) | Aufnahme gestoppt |

### Workflow

1. Terminal-Fenster oeffnen (Windows Terminal, PowerShell)
2. Das Overlay erscheint automatisch am rechten Bildschirmrand
3. Mikrofon-Button klicken — Aufnahme startet (Button pulsiert rot, kurzer Ton)
4. Sprechen
5. Mikrofon-Button nochmal klicken — Aufnahme stoppt (Doppel-Ton)
6. Text wird transkribiert, optional von Gemini korrigiert, und ins Terminal eingefuegt
7. Falls Gemini-Korrektur nicht gefaellt: W-Button druecken — ersetzt durch Whisper-Rohtext
8. Falls Auto-Enter (⏎) aktiviert ist, wird der Text automatisch abgesendet

### Mehrere Eingaben hintereinander

Bei aufeinanderfolgenden Spracheingaben wird automatisch ein Leerzeichen zwischen den Texten eingefuegt, damit keine Woerter zusammenkleben. Das Leerzeichen wird zurueckgesetzt, wenn:
- Die Zeile geloescht wird (X-Button)
- Auto-Enter aktiv ist (nach dem Enter beginnt eine neue Zeile)

---

## Projektstruktur

```
TerminalVoiceOverlay-Windows/
  .env.example          — Vorlage fuer die Konfiguration
  App.xaml / App.xaml.cs — WPF-Anwendung mit System-Tray-Icon
  TerminalVoiceOverlay.csproj — Projektdatei (.NET 8, WPF)
  app.manifest           — Windows-Manifest
  Models/
    RecordingState.cs    — Enum fuer Aufnahmezustaende
  NativeMethods/
    Win32.cs             — Win32-API-Aufrufe (SetForegroundWindow, SendInput, etc.)
  Services/
    AudioRecorder.cs     — Mikrofon-Aufnahme via NAudio
    Config.cs            — .env-Datei laden und parsen
    GeminiClient.cs      — Google Gemini API-Client fuer Textkorrektur (Programmier-Kontext)
    GroqWhisperClient.cs — Groq Whisper API-Client fuer Transkription
    TerminalController.cs — Text ins Terminal einfuegen, Zeile loeschen, Auto-Enter
    TerminalWatcher.cs   — Erkennt aktive Terminal-Fenster
  Views/
    OverlayWindow.xaml   — Overlay-UI (fuenf runde Buttons)
    OverlayWindow.xaml.cs — Overlay-Logik
  publish.ps1            — Build-Skript (erzeugt einzelne .exe)
  install_autostart.ps1  — Richtet Autostart mit Watcher ein
  uninstall_autostart.ps1 — Entfernt den Autostart
  watcher.vbs            — Ueberwacht das Overlay und startet es bei Bedarf neu
```

---

## Schwester-Projekte

Dieses Projekt ist Teil einer Familie von Voice-Overlay-Apps. Alle teilen die gleiche Architektur und ca. 80% des Codes:

| Projekt | Plattform | Ziel-Apps | Sprache |
|---|---|---|---|
| TerminalVoiceOverlay-macOS | macOS | Terminal.app, iTerm2, Warp | Swift / AppKit |
| ClaudeCodexVoiceOverlay-macOS | macOS | Claude Desktop, Codex | Swift / AppKit |
| **TerminalVoiceOverlay-Windows** | Windows | Windows Terminal, PowerShell | C# / WPF |
| ClaudeVoiceOverlay-Windows | Windows | Claude Desktop, Codex | C# / WPF |

**Wichtig:** Bei Aenderungen an einem Projekt muessen die Schwester-Projekte ebenfalls aktualisiert werden, da sie den gleichen Code fuer Groq Whisper, Gemini, Audio-Aufnahme und UI-Logik verwenden. Nur die Ziel-App-Erkennung und Tastatureingabe unterscheiden sich.

### Architektur-Mapping Windows ↔ macOS

| Windows (C#) | macOS (Swift) | Funktion |
|---|---|---|
| OverlayWindow.xaml.cs | AppDelegate.swift | Zentrale App-Logik |
| OverlayWindow.xaml | OverlayPanel.swift | UI (fuenf runde Buttons) |
| TerminalController.cs | TerminalController.swift | Tastatureingabe (Ctrl+V / Cmd+V) |
| AudioRecorder.cs | AudioRecorder.swift | Mikrofon-Aufnahme (NAudio / AVAudioEngine) |
| GroqWhisperClient.cs | GroqWhisperClient.swift | Groq Whisper API |
| GeminiClient.cs | GeminiClient.swift | Gemini API |
| Config.cs | Config.swift | .env laden und parsen |
| TerminalWatcher.cs | AppWatcher.swift | Fenster-Erkennung |
| Win32.cs | (CGEvent direkt) | Native API-Aufrufe |
| publish.ps1 | build.sh | Build-Script |

### Fuer Entwickler: Aenderungen von macOS portieren

Wenn die macOS-Version (Swift) aktualisiert wurde, muessen folgende Schritte fuer Windows (C#) durchgefuehrt werden:

1. **GroqWhisperClient / GeminiClient**: API-Logik ist fast identisch. Aenderungen an Retry-Logik, Timeouts oder Endpunkten 1:1 uebernehmen.
2. **Config**: Neue .env-Variablen muessen in beiden Versionen hinzugefuegt werden.
3. **UI-Logik (AppDelegate ↔ OverlayWindow)**: Button-Handler, State-Management und Fehlerbehandlung sind funktional identisch. C# verwendet `async/await` statt Swift-Closures.
4. **Tastatureingabe**: macOS verwendet `CGEvent`, Windows verwendet `keybd_event`. Die Logik unterscheidet sich, aber das Verhalten soll identisch sein.
5. **Fenster-Erkennung**: macOS verwendet `NSWorkspace` Notifications, Windows verwendet `SetWinEventHook`. Neue Ziel-Apps muessen in beiden Versionen ergaenzt werden.

---

## Letzte Aenderungen (2026-03-12)

- Die macOS-Versionen (Swift) haben folgende Fixes erhalten, die bereits in den Windows-Versionen (C#) korrekt implementiert waren:
  - Retry-Logik verwendet async Task.Delay statt Thread.Sleep (C# war bereits korrekt)
  - IDisposable/Dispose fuer sauberes Aufraemen (C# war bereits korrekt)
  - Async/await fuer Thread-Safety (C# war bereits korrekt)
- Fix: Zwischenablage wird vor dem Einfuegen gesichert und danach wiederhergestellt
- Fix: Auto-Enter Sleep von 500ms auf 300ms reduziert

---

## Haeufige Probleme

| Problem | Loesung |
|---|---|
| `dotnet wird nicht erkannt` | .NET 8 SDK installieren von https://dotnet.microsoft.com/download/dotnet/8.0 und Terminal neu starten |
| `git wird nicht erkannt` | Git installieren von https://git-scm.com/download/win und Terminal neu starten |
| `pwsh wird nicht erkannt` | PowerShell 7 installieren oder stattdessen `powershell -File publish.ps1` verwenden |
| `.exe` wird von Windows blockiert | Smart App Control oder Virenscanner blockiert die unsignierte `.exe`. Rechtsklick → „Trotzdem ausfuehren" oder Ausnahme im Virenscanner hinzufuegen |
| Overlay erscheint nicht | Pruefen ob ein Terminal-Fenster (Windows Terminal / PowerShell) im Vordergrund ist. Andere Terminals muessen ggf. in `TERMINAL_PROCESS_NAMES` ergaenzt werden |
| Transkription schlaegt fehl | `GROQ_API_KEY` in der `.env` pruefen. Ist der Key gueltig? Ist die `.env` neben der `.exe`? |
| Gemini-Korrektur funktioniert nicht | `GEMINI_API_KEY` pruefen. Ohne Key wird Gemini automatisch deaktiviert (kein Fehler) |
| Text wird nicht eingefuegt | Das Terminal-Fenster muss im Vordergrund sein. Das Overlay aktiviert das Terminal vor dem Einfuegen automatisch |
| Watcher startet nicht | Pruefen ob `watcher.vbs` und `publish\TerminalVoiceOverlay.exe` im selben Verzeichnis liegen |
| Mehrere Overlay-Instanzen | Der Watcher hat eine Doppelstart-Sperre. Falls trotzdem mehrere laufen: Task-Manager oeffnen und `TerminalVoiceOverlay.exe` beenden |
| Kein Audio-Feedback | Der System-Lautsprecher muss aktiviert sein. `Console.Beep` nutzt den internen PC-Speaker |

---

## Technologie

- **C# / WPF** (.NET 8) — Desktop-UI mit transparentem, nicht-aktivierendem Overlay
- **NAudio** — Mikrofon-Aufnahme (WAV)
- **Groq Whisper API** — Speech-to-Text
- **Google Gemini API** — Textkorrektur fuer Programmier-Kontext (optional)
- **Win32 API** — Fenster-Erkennung, Vordergrund-Steuerung, Tastatureingabe-Simulation
