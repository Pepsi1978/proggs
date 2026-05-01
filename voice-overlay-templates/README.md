# Voice Overlay Templates

Diese Datei ist ein Template fuer die Voice-Overlay-Apps
(`TerminalVoiceOverlay-Windows`, `ClaudeVoiceOverlay-Windows`,
`TerminalVoiceOverlay-macOS`, `ClaudeCodexVoiceOverlay-macOS`).

## Was die Datei tut

| Datei | Zweck |
|-------|-------|
| `gemini-correction-prompt.txt` | Gemini-System-Prompt fuer den G-Button. Wenn der Benutzer "G" druckt, geht der Whisper-Output durch Gemini mit diesem Prompt — Gemini korrigiert Whisper-Phonetik-Fehler (z.B. "Cloud Code" → "Claude Code"). Hier sollen kuenftig themenspezifische Profile gepflegt werden, nicht nur Programmier-Begriffe. |

## Wo die Live-Datei liegt

NICHT hier im Repo (das ist nur ein Template). Die echte, aktive Datei
liegt ausserhalb des Repos im SK-Ordner:

- `~/SK/VoiceOverlays/gemini-correction-prompt.txt`

Auf einem frischen Rechner wird sie beim ersten `bash build.sh`
(macOS) automatisch dorthin kopiert. Auf Windows muss der Benutzer
sie einmalig per Hand anlegen oder vom anderen Rechner kopieren.

## Hinweis zu voice-prompt.txt (entfernt am 2026-05-01)

Die frueher hier liegende `voice-prompt.txt` (Whisper-Vokabel-Hint) wird
nicht mehr verwendet. Whisper bekommt das Audio ohne `prompt`-Parameter,
damit der Original-Whisper-Output unverfaelscht weitergegeben werden kann.
Themenspezifische Sprach-/Stilprofile passieren ab jetzt erst in der
Gemini-Stufe (siehe `gemini-correction-prompt.txt`). Eine eventuell schon
existierende Datei in `~/SK/VoiceOverlays/voice-prompt.txt` darf liegen
bleiben — sie wird einfach nicht mehr gelesen.

## Anpassen

Anpassen passiert in der Live-Datei in `~/SK/VoiceOverlays/`, NICHT hier.
Das Template im Repo dient nur als Fallback fuer frische Installationen.

Wenn du den Default fuer ALLE zukuenftigen frischen Installationen aendern
willst, aktualisiere zusaetzlich das Template hier. Dann committen.
