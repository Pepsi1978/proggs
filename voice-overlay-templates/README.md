# Voice Overlay Templates

Diese beiden Dateien sind Templates fuer die Voice-Overlay-Apps
(`TerminalVoiceOverlay-Windows`, `ClaudeVoiceOverlay-Windows`,
`TerminalVoiceOverlay-macOS`, `ClaudeCodexVoiceOverlay-macOS`).

## Was die Dateien tun

| Datei | Zweck |
|-------|-------|
| `voice-prompt.txt` | Whisper-Vokabel-Hint (max 224 Tokens). Wird beim Transkribieren als `prompt`-Parameter an Groq mitgeschickt — Whisper biast dadurch englische Programmier-Begriffe richtig zu transkribieren. |
| `gemini-correction-prompt.txt` | Gemini-System-Prompt fuer den G-Button. Wenn der Benutzer "G" druckt, geht der Whisper-Output durch Gemini mit diesem Prompt — Gemini korrigiert Whisper-Phonetik-Fehler (z.B. "Cloud Code" → "Claude Code"). |

## Wo die Live-Dateien liegen

NICHT hier im Repo (das sind nur Templates). Die echten, aktiven Dateien
liegen ausserhalb des Repos im SK-Ordner:

- `~/SK/VoiceOverlays/voice-prompt.txt`
- `~/SK/VoiceOverlays/gemini-correction-prompt.txt`

Auf einem frischen Rechner werden sie beim ersten `bash build.sh`
(macOS) automatisch dorthin kopiert. Auf Windows muss der Benutzer
sie einmalig per Hand anlegen oder vom anderen Rechner kopieren.

## Anpassen

Anpassen passiert in der Live-Datei in `~/SK/VoiceOverlays/`, NICHT hier.
Die Templates im Repo dienen nur als Fallback fuer frische Installationen.

Wenn du den Default fuer ALLE zukuenftigen frischen Installationen aendern
willst, aktualisiere zusaetzlich die Templates hier. Dann committen.
