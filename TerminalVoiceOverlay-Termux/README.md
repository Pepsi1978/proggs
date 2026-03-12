# Terminal Voice Overlay - Termux

Spracheingabe-Overlay fuer das Android Termux-Terminal. Sprich deine Programmier-Anweisungen, und sie werden automatisch transkribiert, korrigiert und in die Zwischenablage kopiert.

## Funktionen

- **Mikrofon-Aufnahme** ueber `termux-microphone-record` (16kHz Mono WAV)
- **Speech-to-Text** ueber Groq Whisper API (schnell und praezise)
- **Textkorrektur** ueber Google Gemini (optional, korrigiert Fachbegriffe)
- **Zwischenablage-Integration** ueber `termux-clipboard-set/get`
- **Benachrichtigungen** ueber `termux-notification` und `termux-toast`
- **Haptisches Feedback** ueber `termux-vibrate`

## Tastenbelegung

| Taste | Funktion |
|-------|----------|
| `m` | Mikrofon ein/aus (Aufnahme starten/stoppen) |
| `x` | Zeile loeschen / Aufnahme abbrechen |
| `w` | Whisper Undo (Rohtext statt Gemini-Korrektur) |
| `g` | Gemini AN/AUS umschalten |
| `e` | Auto-Enter AN/AUS umschalten |
| `q` | Beenden |

## Voraussetzungen

1. **Termux** (v0.118+) aus F-Droid
2. **Termux:API** App aus F-Droid (fuer Mikrofon, Zwischenablage, Benachrichtigungen)
3. **Node.js**: `pkg install nodejs`
4. **termux-api**: `pkg install termux-api`
5. **Mikrofon-Berechtigung** in den Android-Einstellungen fuer Termux erteilen

## Installation

```bash
cd ~/projects/proggs/TerminalVoiceOverlay-Termux

# Abhaengigkeiten und Build
bash build.sh

# API-Keys eintragen
cp .env.example .env
# Bearbeite .env und trage deine Keys ein:
#   GROQ_API_KEY=dein_key
#   GEMINI_API_KEY=dein_key (optional)
```

## Starten

```bash
node dist/main.js
# oder
voice-overlay  # wenn watcher.sh eingerichtet ist
```

## Auto-Start einrichten

Fuege folgende Zeile am Ende deiner `~/.bashrc` hinzu:

```bash
source ~/projects/proggs/TerminalVoiceOverlay-Termux/watcher.sh
```

Danach wird bei jedem Termux-Start der Befehl `voice-overlay` verfuegbar.

## API-Keys

| Variable | Pflicht | Beschreibung |
|----------|---------|-------------|
| `GROQ_API_KEY` | Ja | Groq API-Key fuer Whisper Speech-to-Text |
| `WHISPER_MODEL` | Nein | Whisper-Modell (Standard: `whisper-large-v3`) |
| `WHISPER_LANG` | Nein | Sprache (Standard: `de`) |
| `GEMINI_API_KEY` | Nein | Google Gemini API-Key fuer Textkorrektur |
| `GEMINI_MODEL` | Nein | Gemini-Modell (Standard: `gemini-3.1-flash-lite-preview`) |

## Ablauf

1. Druecke `m` um die Aufnahme zu starten
2. Sprich deine Anweisung
3. Druecke `m` erneut um die Aufnahme zu stoppen
4. Der Text wird automatisch:
   - Mit Whisper transkribiert
   - Optional mit Gemini korrigiert (Fachbegriffe, Grammatik)
   - In die Zwischenablage kopiert
   - Als Benachrichtigung angezeigt
5. Fuege den Text mit langem Druecken → Einfuegen ein

## Fehlerbehebung

| Problem | Loesung |
|---------|---------|
| Aufnahme startet nicht | Mikrofon-Berechtigung fuer Termux pruefen |
| Transkription schlaegt fehl | `GROQ_API_KEY` in `.env` pruefen |
| Gemini funktioniert nicht | `GEMINI_API_KEY` pruefen, ohne Key wird Gemini deaktiviert |
| `termux-microphone-record` nicht gefunden | `pkg install termux-api` und Termux:API App installieren |
