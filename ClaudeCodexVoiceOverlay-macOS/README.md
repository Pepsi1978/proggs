# ClaudeCodexVoiceOverlay

Natives macOS-Overlay für Spracheingabe in **Claude Desktop** (`com.anthropic.claudefordesktop`) und **Codex** (`com.openai.codex`). Nutzt Groq Whisper für Speech-to-Text und optional Gemini für Textkorrektur.

## Features

- Schwebendes Overlay mit 3 Buttons: **X** (Eingabe löschen), **Mic** (Spracheingabe), **G** (Gemini-Toggle)
- Automatisches Ein-/Ausblenden wenn Claude Desktop oder Codex aktiv/inaktiv ist
- Kein Dock-Icon, kein Cmd+Tab-Eintrag (LSUIElement)
- Menüleisten-Icon zum Anzeigen/Beenden
- Koexistenz mit TerminalVoiceOverlay möglich (beide laufen gleichzeitig)

## Voraussetzungen

- macOS 13.0 oder höher
- Xcode Command Line Tools (für `swiftc`)

Xcode Command Line Tools installieren (falls nicht vorhanden):

```
xcode-select --install
```

## API-Keys

Du benötigst mindestens einen Groq API-Key für die Spracherkennung. Optional einen Gemini API-Key für Textkorrektur.

- Groq API-Key: https://console.groq.com/keys
- Gemini API-Key: https://aistudio.google.com/apikey

## Installation

Ins Projektverzeichnis wechseln:

```
cd ~/proggs/ClaudeCodexVoiceOverlay
```

`.env`-Datei anlegen (entweder im Projektverzeichnis oder unter `~/.config/ClaudeCodexVoiceOverlay/.env`):

```
cp .env.example .env
```

API-Keys in der `.env`-Datei eintragen:

```
nano .env
```

App bauen:

```
./build.sh
```

## App starten

```
open build/ClaudeCodexVoiceOverlay.app
```

## Berechtigungen

Beim ersten Start werden zwei Berechtigungen abgefragt:

1. **Mikrofon**: Für die Sprachaufnahme - im macOS-Dialog erlauben
2. **Accessibility**: Für Tastatureingabe (Cmd+V, Cmd+A+Backspace) - unter Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen die App hinzufügen

## Autostart (LaunchAgent)

LaunchAgent installieren, damit die App automatisch beim Login startet:

```
launchctl load ~/Library/LaunchAgents/com.frank.claudecodexvoiceoverlay.plist
```

LaunchAgent deaktivieren:

```
launchctl unload ~/Library/LaunchAgents/com.frank.claudecodexvoiceoverlay.plist
```

## Bedienung

| Button | Funktion |
|--------|----------|
| **X** | Löscht die aktuelle Eingabe im Textfeld (Cmd+A + Backspace) |
| **Mic** | 1x klicken = Aufnahme starten (rot pulsierend), erneut klicken = stoppen + transkribieren |
| **G** | Gemini-Korrektur an (grün) / aus (rot) umschalten |

### Button-Farben (Mic)

- **Dunkelgrau**: Bereit
- **Rot pulsierend**: Aufnahme läuft
- **Orange**: Verarbeitung (Transkription/Korrektur)
- **Grün**: Text erfolgreich eingefügt (reset nach 3s)

## Datenfluss

```
Mic klicken -> Aufnahme (rot pulsierend)
    |
Mic erneut klicken -> Stopp -> WAV-Datei
    |
Groq Whisper API -> Rohtext (orange)
    |
[Wenn G=AN] Gemini API -> korrigierter Text
    |
Clipboard + Cmd+V -> Text in Eingabefeld eingefügt (grün)
```

## Unterschiede zu TerminalVoiceOverlay

| Feature | TerminalVoiceOverlay | ClaudeCodexVoiceOverlay |
|---------|---------------------|------------------------|
| Ziel-Apps | Terminal.app | Claude Desktop, Codex |
| X-Button | Ctrl+U (Shell-Befehl) | Cmd+A + Backspace (Electron) |
| Bundle-ID | com.frank.TerminalVoiceOverlay | com.frank.ClaudeCodexVoiceOverlay |
