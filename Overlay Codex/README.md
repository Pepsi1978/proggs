# Codex Desktop Voice Input

Schwebender Mikrofon-Button als Overlay über der Codex Desktop App.
Klick auf den Button startet die Sprachaufnahme, ein zweiter Klick stoppt sie.
Der gesprochene Text wird per Whisper (lokal!) transkribiert und automatisch
ins Eingabefeld der Codex Desktop App eingefügt.

## Features

- **Lokale Transkription** – kein Cloud-Service, alles läuft auf deinem Rechner (Whisper)
- **Always-on-Top Overlay** – schwebt über der Codex Desktop App
- **Frei verschiebbar** – per Rechtsklick (Windows) / Ctrl+Click (macOS) ziehen
- **Globaler Hotkey** – Strg+Shift+M (Windows) / Cmd+Shift+M (macOS)
- **Automatische Texteingabe** – transkribierter Text wird direkt ins Codex-Eingabefeld eingefügt
- **Visuelle Status-Anzeige** – Idle (grau), Aufnahme (rot pulsierend), Verarbeitung (orange)

## Voraussetzungen

- Python 3.9+
- Mikrofon
- Codex Desktop App (geöffnet)

## Installation

### Windows

```bash
# Repository klonen
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd tampermonkey-skripte/Overlay\ Codex

# Virtuelle Umgebung erstellen (empfohlen)
python -m venv venv
venv\Scripts\activate

# Abhängigkeiten installieren
pip install -r requirements-windows.txt
```

### macOS

```bash
# Repository klonen
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd tampermonkey-skripte/Overlay\ Codex

# Virtuelle Umgebung erstellen (empfohlen)
python3 -m venv venv
source venv/bin/activate

# Abhängigkeiten installieren
pip install -r requirements-macos.txt

# Tkinter für macOS (falls nicht vorhanden)
brew install python-tk
```

**macOS Berechtigungen:** Beim ersten Start wirst du nach Mikrofon-Zugriff und
Bedienungshilfen-Berechtigung gefragt (Systemeinstellungen > Datenschutz & Sicherheit).

## Nutzung

### Windows

```bash
python voice_overlay_windows.py
```

### macOS

```bash
python voice_overlay_macos.py
```

### Optionen

```
--model {tiny,base,small,medium,large}   Whisper Modell-Größe (Standard: base)
--lang LANG                               Sprachcode (Standard: de)
```

**Beispiele:**

```bash
# Größeres Modell für bessere Erkennung
python voice_overlay_windows.py --model small

# Englische Spracheingabe
python voice_overlay_macos.py --lang en

# Schnellstes Modell (weniger genau)
python voice_overlay_windows.py --model tiny
```

## Bedienung

| Aktion | Windows | macOS |
|---|---|---|
| Aufnahme starten/stoppen | Klick auf Button | Klick auf Button |
| Globaler Hotkey | Strg+Shift+M | Cmd+Shift+M |
| Button verschieben | Rechtsklick + Ziehen | Ctrl+Click + Ziehen |
| Beenden | Escape | Escape |

## Whisper Modell-Größen

| Modell | RAM | Geschwindigkeit | Qualität |
|---|---|---|---|
| tiny | ~1 GB | Sehr schnell | Grundlegend |
| **base** | ~1 GB | **Schnell** | **Gut (Standard)** |
| small | ~2 GB | Mittel | Sehr gut |
| medium | ~5 GB | Langsam | Exzellent |
| large | ~10 GB | Sehr langsam | Beste |

Für Deutsch empfehle ich mindestens `base`, für beste Ergebnisse `small` oder `medium`.

## Troubleshooting

### "Kein Mikrofon gefunden"
- Prüfe ob dein Mikrofon vom System erkannt wird
- Windows: Einstellungen > System > Sound > Eingabe
- macOS: Systemeinstellungen > Ton > Eingabe

### "Kein Text erkannt"
- Sprich deutlicher oder näher am Mikrofon
- Versuche ein größeres Modell: `--model small`
- Prüfe die Sprache: `--lang de` für Deutsch

### macOS: "Bedienungshilfen-Zugriff verweigert"
- Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen
- Python/Terminal zu den erlaubten Apps hinzufügen

### Windows: Hotkey funktioniert nicht
- Das `keyboard`-Paket benötigt auf manchen Systemen Admin-Rechte
- Alternative: Nutze einfach den Button-Klick

## Architektur

```
Overlay Codex/
├── voice_core.py              # Shared: Whisper-Integration, AudioRecorder
├── voice_overlay_windows.py   # Windows-spezifisches Overlay (tkinter)
├── voice_overlay_macos.py     # macOS-spezifisches Overlay (tkinter + AppleScript)
├── requirements-windows.txt   # Windows Abhängigkeiten
├── requirements-macos.txt     # macOS Abhängigkeiten
└── README.md
```
