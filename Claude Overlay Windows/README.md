# Claude Overlay Windows

Python-Overlay fuer die **Windows Claude Code Desktop App** mit zwei Buttons:

- `Mic`: nimmt Sprache auf, schickt Audio an **Grok Whisper** (Transkription), dann an **Gemini** (Intentionserkennung + hochwertiges Deutsch), und fuegt den verbesserten Text in Claude ein.
- `X`: leert das Eingabefeld in Claude (`Ctrl+A`, `Backspace`).

Zusatzfunktion:

- Der `watcher.py` erkennt, ob Claude laeuft.
- Wenn Claude startet: Overlay startet automatisch.
- Wenn Claude geschlossen wird: Overlay beendet sich automatisch.

## Voraussetzungen (Windows)

- Windows 10/11
- Python 3.11+
- Claude Desktop App installiert

## Installation auf einem anderen Windows-Rechner

1. Repository klonen:

```powershell
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd "tampermonkey-skripte\Claude Overlay Windows"
```

2. Virtuelle Umgebung erstellen und aktivieren:

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\activate
```

3. Abhaengigkeiten installieren:

```powershell
python -m pip install --upgrade pip
pip install -r requirements.txt
```

4. `.env` erstellen und API-Keys eintragen:

```powershell
copy .env.example .env
notepad .env
```

Mindestens setzen:

- `GROK_API_KEY=...`
- `GEMINI_API_KEY=...`

5. Watcher starten:

```powershell
start_watcher.bat
```

6. Claude Desktop App oeffnen. Das Overlay startet automatisch und beendet sich wieder, wenn Claude geschlossen wird.

## Installation

1. In den Ordner wechseln:

```powershell
cd "Claude Overlay Windows"
```

2. Abhaengigkeiten installieren:

```powershell
python -m pip install -r requirements.txt
```

3. `.env` anlegen (von `.env.example` kopieren):

```powershell
copy .env.example .env
```

4. API-Keys in `.env` setzen:

- `GROK_API_KEY=...`
- `GEMINI_API_KEY=...`

Optional:

- `GROK_WHISPER_MODEL` (Default: `grok-2-whisper-1`)
- `GEMINI_MODEL` (Default: `gemini-2.0-flash`)
- `CLAUDE_PROCESS_NAMES` falls dein Prozessname anders ist

## Starten

### A) Auto-Watcher (empfohlen)

```powershell
start_watcher.bat
```

Der Watcher laeuft im Hintergrund und startet/stoppt das Overlay anhand des Claude-Prozesses.

### B) Overlay direkt starten (Debug)

```powershell
start_overlay_debug.bat
```

## Windows-Autostart einrichten

Damit der Watcher nach Login immer verfuegbar ist:

```powershell
powershell -ExecutionPolicy Bypass -File .\install_autostart.ps1
```

Autostart entfernen:

```powershell
powershell -ExecutionPolicy Bypass -File .\uninstall_autostart.ps1
```

## Hinweise

- Das Einfuegen/Loeschen basiert auf UI-Automation (`pywinauto`) und Tastaturaktionen.
- Falls Claude-UI sich aendert, kann die Fokuslogik in `src/claude_window.py` angepasst werden.
- Das Overlay nutzt bei `Mic` eine Toggle-Logik: einmal klicken startet Aufnahme, erneut klicken stoppt + verarbeitet.

## Projektstruktur

- `src/config.py` - `.env`-Laden, Settings
- `src/audio_capture.py` - Audioaufnahme
- `src/api_clients.py` - Grok Whisper + Gemini API
- `src/claude_window.py` - Claude-Fensterautomation
- `src/overlay_app.py` - Overlay UI
- `src/watcher.py` - Prozessbeobachter fuer Auto-Start/-Stop
