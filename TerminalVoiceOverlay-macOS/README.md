# TerminalVoiceOverlay

Natives macOS-Overlay für Spracheingabe in Terminal.app. Nutzt Groq Whisper für Speech-to-Text und optional Gemini für Textkorrektur.

## Features

- Schwebendes Overlay mit 3 Buttons: **X** (Zeile löschen), **Mic** (Spracheingabe), **G** (Gemini-Toggle)
- Automatisches Ein-/Ausblenden wenn Terminal.app aktiv/inaktiv ist
- Kein Dock-Icon, kein Cmd+Tab-Eintrag (LSUIElement)
- Menüleisten-Icon zum Anzeigen/Beenden

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
cd ~/proggs/TerminalVoiceOverlay
```

`.env`-Datei anlegen (entweder im Projektverzeichnis oder unter `~/.config/TerminalVoiceOverlay/.env`):

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
open build/TerminalVoiceOverlay.app
```

## Berechtigungen

Beim ersten Start werden zwei Berechtigungen abgefragt:

1. **Mikrofon**: Für die Sprachaufnahme - im macOS-Dialog erlauben
2. **Accessibility**: Für Tastatureingabe (Cmd+V, Ctrl+U) - unter Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen die App hinzufügen

## Bedienung

| Button | Funktion |
|--------|----------|
| **X** | Löscht die aktuelle Eingabezeile im Terminal (Ctrl+U) |
| **Mic** | 1x klicken = Aufnahme starten (rot pulsierend), erneut klicken = stoppen + transkribieren |
| **G** | Gemini-Korrektur an (grün) / aus (rot) umschalten |

### Button-Farben (Mic)

- **Dunkelgrau**: Bereit
- **Rot pulsierend**: Aufnahme läuft
- **Orange**: Verarbeitung (Transkription/Korrektur)
- **Grün**: Text erfolgreich eingefügt (reset nach 3s)

## Datenfluss

```
Mic klicken → Aufnahme (rot pulsierend)
    ↓
Mic erneut klicken → Stopp → WAV-Datei
    ↓
Groq Whisper API → Rohtext (orange)
    ↓
[Wenn G=AN] Gemini API → korrigierter Text
    ↓
Clipboard + Cmd+V → Text in Terminal eingefügt (grün)
```
