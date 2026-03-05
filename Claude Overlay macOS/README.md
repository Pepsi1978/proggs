# Claude Overlay macOS

Ein Python-Overlay fuer die **macOS Claude Desktop App** mit zwei Buttons:

- **Mikrofon-Button** (Mic): Nimmt deine Sprache auf, schickt das Audio an die **Groq Whisper API** (Transkription), dann optional an die **Gemini API** (Intentionserkennung + hochwertiges Deutsch) und fuegt den verbesserten Text automatisch in das Claude-Eingabefeld ein.
- **Radiergummi-Button**: Leert das gesamte Eingabefeld in der Claude Desktop App.

**Zusatzfunktionen:**

- Der **Watcher** (`watcher.py`) ueberwacht, ob die Claude Desktop App laeuft.
- Wenn Claude gestartet wird: Das Overlay startet automatisch.
- Wenn Claude geschlossen wird: Das Overlay beendet sich automatisch.
- Das Overlay ist **rahmenlos** (kein Fenstertitel, kein Dock-Icon), **immer im Vordergrund** und kann per **Rechtsklick + Ziehen** oder **Ctrl+Klick + Ziehen** frei verschoben werden.
- Der Watcher nutzt eine **PID-Lock-Datei**, sodass nie zwei Instanzen gleichzeitig laufen koennen.
- Waehrend der Aufnahme **pulsiert** der Mikrofon-Button rot.
- Lange Aufnahmen (>20 MB) werden automatisch in Teile aufgesplittet und stueckweise transkribiert.
- Bei API-Fehlern (429/500/503) wird automatisch mit Backoff erneut versucht.
- **Gemini-Toggle** (kleines "G" oben links): Textverbesserung ein-/ausschalten.
- **Beenden-Button** (kleines "X" oben rechts): Overlay schliessen.

---

## Inhaltsverzeichnis

1. [Was macht dieses Tool?](#1-was-macht-dieses-tool)
2. [Voraussetzungen](#2-voraussetzungen)
3. [Schritt 1: Python installieren](#3-schritt-1-python-installieren)
4. [Schritt 2: Repository herunterladen](#4-schritt-2-repository-herunterladen)
5. [Schritt 3: Virtuelle Umgebung erstellen](#5-schritt-3-virtuelle-umgebung-erstellen)
6. [Schritt 4: Abhaengigkeiten installieren](#6-schritt-4-abhaengigkeiten-installieren)
7. [Schritt 5: API-Keys besorgen und eintragen](#7-schritt-5-api-keys-besorgen-und-eintragen)
8. [Schritt 6: macOS-Berechtigungen einrichten](#8-schritt-6-macos-berechtigungen-einrichten)
9. [Schritt 7: Tool starten](#9-schritt-7-tool-starten)
10. [Schritt 8: Autostart einrichten (optional)](#10-schritt-8-autostart-einrichten-optional)
11. [Benutzung](#11-benutzung)
12. [Log-Dateien und Fehlerdiagnose](#12-log-dateien-und-fehlerdiagnose)
13. [Fehlerbehebung / FAQ](#13-fehlerbehebung--faq)
14. [Projektstruktur](#14-projektstruktur)
15. [Deinstallation](#15-deinstallation)

---

## 1. Was macht dieses Tool?

Wenn du mit der Claude Desktop App arbeitest, moechtest du vielleicht manchmal **per Sprache** Eingaben machen, anstatt alles zu tippen. Dieses Tool:

1. **Hoert dir zu** (Mikrofon-Aufnahme)
2. **Wandelt deine Sprache in Text um** (Groq Whisper API - schnelle und praezise Spracherkennung)
3. **Verbessert den Text** (Gemini API - erkennt deine Absichten und formuliert sie in gutem Deutsch)
4. **Fuegt den fertigen Text ein** (automatisch in das Claude-Eingabefeld)

Das spart Zeit und sorgt dafuer, dass deine Spracheingaben sauber und klar formuliert bei Claude ankommen.

---

## 2. Voraussetzungen

Bevor du loslegst, brauchst du folgendes auf deinem Mac:

| Was | Warum |
|---|---|
| **macOS 12 (Monterey) oder neuer** | Das Tool nutzt AppleScript fuer Fenstererkennung und Tastatur-Simulation |
| **Python 3.11 oder neuer** | Die Programmiersprache, in der das Tool geschrieben ist |
| **Claude Desktop App** | Die App, in die das Overlay den Text einfuegt |
| **Internetverbindung** | Fuer die API-Aufrufe (Groq Whisper + Gemini) |
| **Ein Mikrofon** | Zum Aufnehmen deiner Sprache (eingebaut oder extern) |
| **Groq API-Key** | Zugang zur Sprach-Transkription (von Groq Cloud) |
| **Gemini API-Key** (optional) | Zugang zur Textverbesserung (von Google). Ohne Gemini wird der Rohtext direkt eingefuegt |

---

## 3. Schritt 1: Python installieren

### Was ist Python?

Python ist eine Programmiersprache. Dein Mac kann Python-Programme nur ausfuehren, wenn Python installiert ist.

### So installierst du Python:

**Empfohlen: ueber Homebrew** (der Paketmanager fuer macOS):

1. Falls du Homebrew noch nicht hast, oeffne das **Terminal** (Programme > Dienstprogramme > Terminal) und fuehre aus:

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

   > **Was ist Homebrew?** Homebrew ist ein Paketmanager fuer macOS - aehnlich wie ein App Store fuer Kommandozeilen-Tools. Es macht die Installation von Entwickler-Tools viel einfacher.

2. Installiere Python:

   ```bash
   brew install python@3.12
   ```

**Alternativ: Direkt von python.org**

1. Gehe zu: **https://www.python.org/downloads/macos/**
2. Lade das neueste macOS-Installationspaket herunter
3. Oeffne die `.pkg`-Datei und folge dem Installer

### Installation pruefen:

```bash
python3 --version
```

Du solltest so etwas sehen wie: `Python 3.12.5` (oder neuer).

> **Hinweis:** Auf macOS heisst der Befehl `python3` (nicht `python`). Das liegt daran, dass macOS frueher ein altes Python 2 mitgeliefert hat.

---

## 4. Schritt 2: Repository herunterladen

### Variante A: Mit Git klonen (empfohlen)

Git ist auf den meisten Macs vorinstalliert. Oeffne das Terminal:

```bash
cd ~/Documents
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd "tampermonkey-skripte/Claude Overlay macOS"
```

> **Was passiert hier?**
> - `cd ~/Documents` - Wechselt in deinen Dokumente-Ordner
> - `git clone ...` - Laedt das komplette Repository von GitHub herunter
> - `cd "Claude Overlay macOS"` - Wechselt in den Projektordner

Falls Git nicht installiert ist, fragt macOS automatisch, ob du die Xcode Command Line Tools installieren moechtest - klicke auf "Installieren".

### Variante B: Als ZIP herunterladen (ohne Git)

1. Gehe zu: **https://github.com/Pepsi1978/tampermonkey-skripte**
2. Klicke auf den gruenen Button **"Code"** > **"Download ZIP"**
3. Entpacke die ZIP-Datei (Doppelklick)
4. Oeffne das Terminal und navigiere in den Ordner:

```bash
cd ~/Documents/tampermonkey-skripte-main/"Claude Overlay macOS"
```

---

## 5. Schritt 3: Virtuelle Umgebung erstellen

### Was ist eine virtuelle Umgebung (venv)?

Eine **virtuelle Umgebung** ist ein isolierter Bereich fuer Python-Pakete. So stoeren sich verschiedene Projekte nicht gegenseitig.

### So erstellst du eine virtuelle Umgebung:

Stelle sicher, dass du im Ordner `Claude Overlay macOS` bist:

```bash
cd ~/Documents/tampermonkey-skripte/"Claude Overlay macOS"
```

Dann erstelle und aktiviere die virtuelle Umgebung:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

> **Was passiert hier?**
> - `python3 -m venv .venv` - Erstellt einen Ordner `.venv` mit einer isolierten Python-Kopie
> - `source .venv/bin/activate` - Aktiviert die Umgebung (du siehst `(.venv)` am Anfang der Zeile)
> - Du musst die Umgebung jedes Mal aktivieren, wenn du ein neues Terminal oeffnest

---

## 6. Schritt 4: Abhaengigkeiten installieren

### Was sind Abhaengigkeiten?

Abhaengigkeiten (Dependencies) sind externe Pakete, die das Tool braucht:

| Paket | Wofuer |
|---|---|
| `sounddevice` | Zugriff auf dein Mikrofon (Audio aufnehmen) |
| `soundfile` | Audio als WAV-Datei speichern |
| `numpy` | Mathematische Berechnungen fuer Audio-Daten |
| `requests` | HTTP-Anfragen an die APIs senden (Groq + Gemini) |
| `psutil` | Laufende Prozesse pruefen (ist Claude gestartet?) |

> **Hinweis:** Im Gegensatz zur Windows-Version braucht die macOS-Version kein `pywinauto` oder `pywin32` - stattdessen nutzt sie das eingebaute AppleScript.

### So installierst du sie:

Stelle sicher, dass die virtuelle Umgebung aktiv ist (du siehst `(.venv)`), dann:

```bash
python3 -m pip install --upgrade pip
pip install -r requirements.txt
```

> **PortAudio benoetigt:** Falls `sounddevice` nicht installiert werden kann, installiere PortAudio zuerst:
> ```bash
> brew install portaudio
> ```

---

## 7. Schritt 5: API-Keys besorgen und eintragen

Das Tool braucht mindestens einen API-Key (Zugangsschluessel), um die Cloud-Dienste nutzen zu koennen.

### 7.1 Groq API-Key (fuer Sprach-Transkription) - Pflicht

1. Gehe zu: **https://console.groq.com/**
2. Erstelle ein Konto oder melde dich an
3. Navigiere zu **"API Keys"**
4. Klicke auf **"Create API Key"**
5. Kopiere den Schluessel (er beginnt mit `gsk_...`)

> **Was ist ein API-Key?** Ein API-Key ist wie ein Passwort, das deinem Tool erlaubt, den Dienst zu nutzen. Halte ihn geheim!

### 7.2 Gemini API-Key (fuer Textverbesserung) - Optional

1. Gehe zu: **https://aistudio.google.com/apikey**
2. Melde dich mit deinem Google-Konto an
3. Klicke auf **"Create API Key"**
4. Kopiere den Schluessel

> **Hinweis:** Ohne Gemini-Key wird die Textverbesserung uebersprungen und der Rohtext direkt eingefuegt.

### 7.3 Keys in die .env-Datei eintragen

Kopiere die Vorlage und oeffne sie zum Bearbeiten:

```bash
cp .env.example .env
nano .env
```

Trage deine Keys ein:

```
GROQ_API_KEY=gsk_dein-schluessel-hier
GEMINI_API_KEY=dein-gemini-schluessel-hier
```

Speichere mit `Ctrl + O`, dann `Enter`, dann `Ctrl + X` zum Beenden von nano.

> **Alternativ mit TextEdit:**
> ```bash
> open -a TextEdit .env
> ```

> **Wichtig:** Die `.env`-Datei wird NICHT auf GitHub hochgeladen. Deine Keys bleiben lokal.

### 7.4 Optionale Einstellungen

| Variable | Standard | Beschreibung |
|---|---|---|
| `WHISPER_MODEL` | `whisper-large-v3` | Welches Whisper-Modell Groq nutzen soll |
| `WHISPER_LANG` | `de` | Sprache der Aufnahme (ISO 639-1 Code) |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini-Modell |
| `GEMINI_THINKING_LEVEL` | `MEDIUM` | Thinking-Level fuer Gemini 3.x (LOW/MEDIUM/HIGH) |
| `AUDIO_SAMPLE_RATE` | `16000` | Audio-Abtastrate in Hz |
| `AUDIO_CHANNELS` | `1` | Mono (1) oder Stereo (2) |
| `CLAUDE_PROCESS_NAMES` | `Claude` | Name(n) des Claude-Prozesses (kommagetrennt) |

---

## 8. Schritt 6: macOS-Berechtigungen einrichten

macOS erfordert explizite Berechtigungen fuer Mikrofon- und Bedienungshilfen-Zugriff. **Ohne diese Berechtigungen funktioniert das Overlay nicht richtig.**

### 8.1 Mikrofon-Berechtigung

Beim ersten Start des Overlays fragt macOS automatisch nach der Mikrofon-Berechtigung. Klicke auf **"Erlauben"**.

Falls du die Berechtigung verpasst hast:
1. Oeffne **Systemeinstellungen** > **Datenschutz & Sicherheit** > **Mikrofon**
2. Aktiviere den Schalter fuer **Terminal** (oder die App, von der aus du das Tool startest)

### 8.2 Bedienungshilfen-Berechtigung

Das Tool braucht Zugriff auf die Bedienungshilfen, um Tastenkombinationen (Cmd+V, Cmd+A) in anderen Apps zu simulieren:

1. Oeffne **Systemeinstellungen** > **Datenschutz & Sicherheit** > **Bedienungshilfen**
2. Klicke auf das **Schloss-Symbol** unten links und gib dein Passwort ein
3. Klicke auf **"+"** und fuege **Terminal** hinzu (oder die App, von der aus du das Tool startest)

> **Warum?** macOS schuetzt dich davor, dass Programme heimlich Tasten druecken. Damit unser Tool Text in Claude einfuegen kann, braucht es diese Berechtigung.

---

## 9. Schritt 7: Tool starten

Es gibt mehrere Wege, das Tool zu starten:

### Variante A: Shell-Skript (empfohlen)

```bash
bash start_watcher.sh
```

> **Was passiert?**
> - Der Watcher prueft alle 2 Sekunden, ob Claude laeuft
> - Sobald du die Claude Desktop App oeffnest, erscheint das Overlay
> - Wenn du Claude schliesst, verschwindet das Overlay

### Variante B: Overlay direkt starten (zum Testen)

```bash
bash start_overlay.sh
```

> Startet das Overlay direkt. Claude muss bereits laufen. Nuetzlich zum Testen.

### Variante C: Manuell im Terminal (Debug-Modus)

```bash
cd ~/Documents/tampermonkey-skripte/"Claude Overlay macOS"
source .venv/bin/activate
python src/watcher.py
```

> Dieser Modus zeigt alle Ausgaben und Fehlermeldungen im Terminal. Nuetzlich zum Debuggen. Druecke `Ctrl + C` zum Beenden.

---

## 10. Schritt 8: Autostart einrichten (optional)

Damit der Watcher nach jedem Login automatisch startet:

### Autostart installieren:

```bash
bash install_autostart.sh
```

> **Was passiert?**
> - Es wird ein macOS **LaunchAgent** erstellt (unter `~/Library/LaunchAgents/`)
> - Der Watcher startet automatisch bei jedem Login
> - Sobald du Claude oeffnest, erscheint das Overlay
> - Sobald du Claude schliesst, verschwindet das Overlay
> - Dank **PID-Lock** werden keine doppelten Instanzen gestartet
> - Kein manuelles Starten mehr noetig

### Autostart entfernen:

```bash
bash uninstall_autostart.sh
```

---

## 11. Benutzung

### Mikrofon-Button (linker Button)

1. **Einmal klicken** = Aufnahme starten (Button wird **rot**, pulsiert)
2. **Sprich** deinen Text ins Mikrofon
3. **Nochmal klicken** = Aufnahme stoppen (Button wird **orange** = Verarbeitung)
4. Das Tool sendet dein Audio an:
   - **Groq Whisper** (Sprache zu Text)
   - **Gemini** (Text verbessern + Intentionen erkennen) - falls konfiguriert
5. Der verbesserte Text wird automatisch in das Claude-Eingabefeld eingefuegt
6. Button wird **gruen** = Erfolg!

### Radiergummi-Button (rechter Button)

- **Einmal klicken** = Leert das komplette Eingabefeld in Claude

### Gemini-Toggle (kleines "G" oben links)

- **Klicken** = Textverbesserung ein-/ausschalten
- **Gruen** = Gemini aktiv, **Grau** = Gemini aus
- Ohne Gemini wird der Rohtext direkt eingefuegt

### Overlay verschieben

- **Rechtsklick + Ziehen** = Overlay an eine andere Stelle verschieben
- **Ctrl+Klick + Ziehen** = Alternative zum Verschieben (fuer Trackpads ohne Rechtsklick)

### Overlay schliessen

- **Kleines "X"** oben rechts klicken
- **Escape-Taste** druecken
- Oder einfach Claude schliessen (der Watcher beendet das Overlay automatisch)

### Farb-Codes

| Farbe | Bedeutung |
|---|---|
| Dunkelgrau | Bereit (Idle) |
| Rot (pulsierend) | Aufnahme laeuft |
| Orange | Verarbeitung (API-Aufruf) |
| Gruen | Erfolg (Text eingefuegt / Feld geleert) |
| Rot (statisch) | Fehler aufgetreten |

---

## 12. Log-Dateien und Fehlerdiagnose

Das Tool schreibt Diagnose-Informationen in Log-Dateien im Projektordner:

| Datei | Inhalt |
|---|---|
| `watcher.log` | Watcher-Aktivitaet: Claude-Erkennung, Overlay-Start/Stop, Crash-Erkennung |
| `overlay.log` | Overlay-Aktivitaet: Config-Werte, Aufnahme, API-Aufrufe, Fehler |
| `failed_transcripts/` | Gesicherte Transkripte bei API-Fehlern (Text geht nicht verloren) |

> **Tipp:** Bei Problemen zuerst `overlay.log` pruefen - dort stehen die meisten Fehlermeldungen.

---

## 13. Fehlerbehebung / FAQ

### "python3 wird nicht erkannt"

Python ist nicht installiert. Installiere es ueber Homebrew (`brew install python@3.12`) oder von python.org.

### "No module named sounddevice"

PortAudio fehlt. Installiere es:
```bash
brew install portaudio
pip install sounddevice
```

### "Keine Audiodaten aufgenommen"

- Pruefe die Mikrofon-Berechtigung: **Systemeinstellungen > Datenschutz & Sicherheit > Mikrofon**
- Teste dein Mikrofon mit der QuickTime Player Audioaufnahme

### "Cmd+V funktioniert nicht / Text wird nicht eingefuegt"

- Pruefe die Bedienungshilfen-Berechtigung: **Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen**
- Stelle sicher, dass **Terminal** (oder deine Start-App) in der Liste ist und aktiviert

### "Claude nicht gefunden"

- Stelle sicher, dass die Claude Desktop App geoeffnet ist
- Falls deine Claude-App einen anderen Prozessnamen hat, passe `CLAUDE_PROCESS_NAMES` in der `.env` an

### "Fehlende Umgebungsvariablen: GROQ_API_KEY"

- Oeffne die `.env`-Datei und stelle sicher, dass du deinen API-Key eingetragen hast
- Pruefe, dass kein Leerzeichen vor oder nach dem `=` steht
- Die `.env`-Datei muss im Ordner `Claude Overlay macOS` liegen (nicht in `src/`)

### "API-Fehler / 401 Unauthorized"

- Dein API-Key ist ungueltig oder abgelaufen
- Erstelle einen neuen Key auf der jeweiligen Plattform

### "API-Fehler / 429 Too Many Requests"

- Du hast zu viele Anfragen in kurzer Zeit gesendet
- Das Tool versucht automatisch bis zu 3-5 Mal erneut
- Warte ein paar Minuten und versuche es erneut

### Das Overlay erscheint nicht

- Stelle sicher, dass der Watcher laeuft (`bash start_watcher.sh`)
- Oeffne die Claude Desktop App
- Pruefe im Aktivitaetsmonitor (Programme > Dienstprogramme), ob `python` laeuft
- Pruefe `watcher.log` auf Fehlermeldungen

### tkinter fehlt / ImportError

Falls tkinter nicht verfuegbar ist:
```bash
brew install python-tk@3.12
```

---

## 14. Projektstruktur

```
Claude Overlay macOS/
  .env.example              # Vorlage fuer API-Keys
  .env                      # Deine echten API-Keys (wird nicht hochgeladen)
  requirements.txt          # Liste der Python-Abhaengigkeiten
  README.md                 # Diese Anleitung
  start_watcher.sh          # Startet den Watcher im Hintergrund
  start_overlay.sh          # Startet das Overlay direkt (ohne Watcher)
  install_autostart.sh      # Richtet macOS-Autostart ein (LaunchAgent)
  uninstall_autostart.sh    # Entfernt macOS-Autostart
  watcher.pid               # PID-Lock-Datei (verhindert doppelte Instanzen)
  watcher.log               # Log-Datei des Watchers (wird automatisch erstellt)
  overlay.log               # Log-Datei des Overlays (wird automatisch erstellt)
  failed_transcripts/       # Gesicherte Transkripte bei API-Fehlern
  src/
    config.py               # Laedt .env-Datei und stellt Einstellungen bereit
    audio_capture.py         # Nimmt Audio vom Mikrofon auf und speichert als WAV
    api_clients.py           # Kommuniziert mit Groq Whisper + Gemini API
    claude_window.py         # Findet Claude via AppleScript, fuegt Text ein/loescht
    overlay_app.py           # Die sichtbare Overlay-Oberflaeche (Buttons, Farben)
    watcher.py               # Ueberwacht, ob Claude laeuft; startet/stoppt Overlay
```

---

## 15. Deinstallation

### Autostart entfernen:

```bash
bash uninstall_autostart.sh
```

### Watcher/Overlay beenden:

Oeffne den Aktivitaetsmonitor (Programme > Dienstprogramme > Aktivitaetsmonitor), suche nach `python` und beende den Prozess. Oder im Terminal:

```bash
pkill -f "watcher.py"
pkill -f "overlay_app.py"
```

### Dateien loeschen:

Loesche einfach den Ordner `Claude Overlay macOS` (oder das gesamte Repository).

### Virtuelle Umgebung loeschen:

Die virtuelle Umgebung ist im Ordner `.venv` innerhalb des Projektordners. Wenn du den Projektordner loeschst, ist sie automatisch mit weg.
