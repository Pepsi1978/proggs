# ClaudeVoiceOverlay-Windows

Voice-to-Text Overlay fuer die **Claude Desktop App** und **Codex Desktop App** unter Windows.

Das Overlay erscheint automatisch am rechten Bildschirmrand, wenn Claude oder Codex im Vordergrund ist, und verschwindet bei anderen Anwendungen.

## Funktionen

- **Mikrofon-Button**: Sprache aufnehmen und als Text in Claude/Codex einfuegen (via Groq Whisper)
- **Gemini-Button (G)**: Textverbesserung durch Google Gemini an/aus schalten
- **X-Button**: Aktuelle Eingabe in Claude/Codex loeschen

## Voraussetzungen

### .NET 8 SDK

Wird benoetigt, um das Projekt zu kompilieren. Download:

https://dotnet.microsoft.com/download/dotnet/8.0

Nach der Installation pruefen:

```
dotnet --version
```

### Groq API-Key

Wird fuer die Spracherkennung (Whisper) benoetigt. Kostenlos erstellen unter:

https://console.groq.com/

### Gemini API-Key (optional)

Wird fuer die automatische Textverbesserung benoetigt. Kostenlos erstellen unter:

https://aistudio.google.com/apikey

## Installation

Repository klonen oder herunterladen:

```
git clone https://github.com/IHR-USERNAME/ClaudeVoiceOverlay-Windows.git
```

In das Projektverzeichnis wechseln:

```
cd ClaudeVoiceOverlay-Windows
```

Die `.env.example` Datei kopieren und als `.env` speichern:

```
copy .env.example .env
```

Die `.env` Datei oeffnen und die API-Keys eintragen:

```
notepad .env
```

## Build

Das Projekt als selbststaendige .exe kompilieren:

```
pwsh -File publish.ps1
```

Dies erzeugt `publish/ClaudeVoiceOverlay.exe` — eine einzelne, ausfuehrbare Datei.

## Starten

Die .exe direkt starten:

```
.\publish\ClaudeVoiceOverlay.exe
```

Das Overlay erscheint automatisch, sobald Claude oder Codex in den Vordergrund kommt.

## Autostart einrichten

Damit das Overlay bei jedem Windows-Start automatisch laeuft:

```
pwsh -File install_autostart.ps1
```

Der Watcher (`watcher.vbs`) startet bei der Windows-Anmeldung und haelt das Overlay am Laufen. Wenn das Overlay abstuerzt, wird es automatisch neu gestartet.

## Autostart entfernen

```
pwsh -File uninstall_autostart.ps1
```

## Bedienung

| Button | Funktion |
|--------|----------|
| **X** | Loescht die aktuelle Eingabe in Claude/Codex (Ctrl+A + Backspace) |
| **Mikrofon** | Klick = Aufnahme starten (rot pulsierend), erneut klicken = stoppen und Text einfuegen |
| **G** | Gemini-Textverbesserung an (gruen) / aus (rot) |

### Farben des Mikrofon-Buttons

- **Grau**: Bereit
- **Rot pulsierend**: Nimmt auf
- **Orange**: Verarbeitet (Transkription/Korrektur)
- **Gruen**: Erfolgreich eingefuegt
- **Rot (statisch)**: Fehler

## Konfiguration (.env)

| Variable | Beschreibung | Standard |
|----------|-------------|----------|
| `GROQ_API_KEY` | Groq API-Key (erforderlich) | — |
| `WHISPER_MODEL` | Whisper-Modell | `whisper-large-v3` |
| `WHISPER_LANG` | Sprache fuer Erkennung | `de` |
| `WHISPER_URL` | Groq API-Endpunkt | `https://api.groq.com/openai/v1/audio/transcriptions` |
| `GEMINI_API_KEY` | Gemini API-Key (optional) | — |
| `GEMINI_MODEL` | Gemini-Modell | `gemini-3.1-flash-lite-preview` |
| `GEMINI_THINKING_LEVEL` | Thinking-Level (LOW/MEDIUM/HIGH) | `MEDIUM` |
| `AUDIO_SAMPLE_RATE` | Abtastrate | `16000` |
| `AUDIO_CHANNELS` | Kanaele | `1` |
| `TARGET_PROCESS_NAMES` | Ziel-Prozesse (kommagetrennt) | `Claude,Codex` |

Die `.env` Datei wird in folgender Reihenfolge gesucht:

1. Neben der `.exe`
2. Im aktuellen Arbeitsverzeichnis
3. `%USERPROFILE%\.env`
4. `%APPDATA%\ClaudeVoiceOverlay\.env`

## Fehlerbehebung

### Overlay erscheint nicht

Pruefen ob Claude oder Codex wirklich im Vordergrund ist. Die Prozessnamen muessen mit `TARGET_PROCESS_NAMES` uebereinstimmen (Standard: `Claude,Codex`).

### Kein Ton / Mikrofon-Fehler

Sicherstellen, dass ein Mikrofon angeschlossen und in den Windows-Einstellungen als Standardgeraet aktiviert ist.

### API-Fehler

Die API-Keys in der `.env` pruefen. Bei 429-Fehlern (Rate Limit) wird automatisch erneut versucht.

### Text wird nicht eingefuegt

Claude/Codex muss im Vordergrund sein. Das Overlay setzt den Text in die Zwischenablage und sendet Ctrl+V. Falls das nicht funktioniert, pruefen ob die Zwischenablage von einem anderen Programm blockiert wird.
