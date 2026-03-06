# Claude Overlay macOS

Ein Python-Overlay fuer die **macOS Claude Desktop App** und die **Codex Desktop App** mit zwei Buttons:

- **Mikrofon-Button** (Mic): Nimmt deine Sprache auf, schickt das Audio an die **Groq Whisper API** (Transkription), dann optional an die **Gemini API** (Intentionserkennung + hochwertiges Deutsch) und fuegt den verbesserten Text automatisch in das Claude-Eingabefeld ein.
- **Besen-Button**: Leert das gesamte Eingabefeld in der Claude Desktop App.

**Zusatzfunktionen:**

- Der **Watcher** (`watcher.py`) ueberwacht, ob die Claude Desktop App laeuft.
- Wenn Claude gestartet wird: Das Overlay startet automatisch.
- Wenn Claude geschlossen wird: Das Overlay beendet sich automatisch.
- Das Overlay ist **rahmenlos** (kein Fenstertitel) und kann per **Rechtsklick + Ziehen** (oder Ctrl+Klick + Ziehen) frei verschoben werden.
- Das Overlay **blendet sich automatisch aus**, wenn eine andere App als Claude oder Codex im Vordergrund ist, und erscheint wieder, sobald Claude/Codex aktiv wird.
- Waehrend der Aufnahme **pulsiert** der Mikrofon-Button rot.
- Lange Aufnahmen (>20 MB) werden automatisch in Teile aufgesplittet und stueckweise transkribiert.
- Bei API-Fehlern (429/500/503) wird automatisch mit Backoff erneut versucht.
- **Gemini-Toggle** (kleines "G" oben links): Gemini-Textverbesserung an/aus schalten.

---

## Inhaltsverzeichnis

1. [Was macht dieses Tool?](#1-was-macht-dieses-tool)
2. [Voraussetzungen](#2-voraussetzungen)
3. [Schritt 1: Python installieren](#3-schritt-1-python-installieren)
4. [Schritt 2: Xcode Command Line Tools installieren](#4-schritt-2-xcode-command-line-tools-installieren)
5. [Schritt 3: Repository herunterladen](#5-schritt-3-repository-herunterladen)
6. [Schritt 4: Virtuelle Umgebung erstellen](#6-schritt-4-virtuelle-umgebung-erstellen)
7. [Schritt 5: Abhaengigkeiten installieren](#7-schritt-5-abhaengigkeiten-installieren)
8. [Schritt 6: API-Keys besorgen und eintragen](#8-schritt-6-api-keys-besorgen-und-eintragen)
9. [Schritt 7: Berechtigungen einrichten](#9-schritt-7-berechtigungen-einrichten)
10. [Schritt 8: Tool starten](#10-schritt-8-tool-starten)
11. [Schritt 9: Autostart einrichten (optional)](#11-schritt-9-autostart-einrichten-optional)
12. [Benutzung](#12-benutzung)
13. [Log-Dateien und Fehlerdiagnose](#13-log-dateien-und-fehlerdiagnose)
14. [Fehlerbehebung / FAQ](#14-fehlerbehebung--faq)
15. [Projektstruktur](#15-projektstruktur)
16. [Deinstallation](#16-deinstallation)

---

## 1. Was macht dieses Tool?

Wenn du mit der Claude Desktop App oder der Codex Desktop App auf deinem Mac arbeitest, moechtest du vielleicht manchmal **per Sprache** Eingaben machen, anstatt alles zu tippen. Dieses Tool:

1. **Hoert dir zu** (Mikrofon-Aufnahme)
2. **Wandelt deine Sprache in Text um** (Groq Whisper API - schnelle und praezise Spracherkennung)
3. **Verbessert den Text** (Gemini API - erkennt deine Absichten und formuliert sie in gutem Deutsch)
4. **Fuegt den fertigen Text ein** (automatisch in das Eingabefeld von Claude oder Codex)

Das spart Zeit und sorgt dafuer, dass deine Spracheingaben sauber und klar formuliert ankommen. Das Overlay blendet sich automatisch aus, wenn du zu einer anderen App wechselst, und erscheint wieder, sobald Claude oder Codex im Vordergrund ist.

---

## 2. Voraussetzungen

Bevor du loslegst, brauchst du folgendes auf deinem Mac:

| Was | Warum |
|---|---|
| **macOS 12 (Monterey) oder neuer** | Das Tool nutzt AppleScript und macOS-spezifische Funktionen |
| **Python 3.11 oder neuer** | Die Programmiersprache, in der das Tool geschrieben ist |
| **Claude Desktop App** oder **Codex Desktop App** | Die App, in die das Overlay den Text einfuegt |
| **Internetverbindung** | Fuer die API-Aufrufe (Groq Whisper + Gemini) |
| **Ein Mikrofon** | Zum Aufnehmen deiner Sprache (eingebaut oder extern) |
| **Groq API-Key** | Zugang zur Sprach-Transkription (von Groq Cloud) |
| **Gemini API-Key** (optional) | Zugang zur Textverbesserung (von Google). Ohne Gemini wird der Rohtext direkt eingefuegt |

---

## 3. Schritt 1: Python installieren

### Was ist Python?

Python ist eine Programmiersprache. Dein Mac kann Python-Programme nur ausfuehren, wenn Python installiert ist. macOS hat zwar eine aeltere Version vorinstalliert, aber wir brauchen eine aktuelle.

### So installierst du Python:

1. Oeffne deinen Browser und gehe zu: **https://www.python.org/downloads/macos/**

2. Klicke auf den grossen gelben Button **"Download Python 3.1x.x"** (die neueste Version).

3. Oeffne die heruntergeladene `.pkg`-Datei und folge dem Installer.

4. Klicke dich durch den Installer (Standard-Einstellungen sind in Ordnung).

### Installation pruefen:

Oeffne das **Terminal** (Spotlight: `Cmd + Leertaste`, dann "Terminal" eingeben):

```
python3 --version
```

Du solltest so etwas sehen wie: `Python 3.12.5` (oder neuer).

> **Hinweis:** Auf macOS heisst der Befehl `python3` (nicht `python`).

---

## 4. Schritt 2: Xcode Command Line Tools installieren

### Was sind die Xcode Command Line Tools?

Diese Tools enthalten grundlegende Entwicklerwerkzeuge (Compiler, Git, etc.), die manche Python-Pakete zum Installieren brauchen.

### So installierst du sie:

```
xcode-select --install
```

> **Was passiert?** Es erscheint ein Dialog, der dich fragt, ob du die Command Line Tools installieren moechtest. Klicke auf **"Installieren"** und warte, bis es fertig ist.

Falls du die Meldung `xcode-select: error: command line tools are already installed` bekommst, sind sie bereits installiert und du kannst weitermachen.

---

## 5. Schritt 3: Repository herunterladen

Es gibt zwei Wege, den Code auf deinen Mac zu bekommen:

### Variante A: Mit Git klonen (empfohlen)

Ins Dokumente-Verzeichnis wechseln:

```
cd ~/Documents
```

Repository klonen:

```
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
```

In den Projektordner wechseln:

```
cd "tampermonkey-skripte/Claude Overlay macOS"
```

> **Was passiert hier?**
> - `cd ~/Documents` - Wechselt in deinen Dokumente-Ordner
> - `git clone ...` - Laedt das komplette Repository von GitHub herunter
> - `cd "Claude Overlay macOS"` - Wechselt in den Projektordner

### Variante B: Als ZIP herunterladen (ohne Git)

1. Gehe zu: **https://github.com/Pepsi1978/tampermonkey-skripte**
2. Klicke auf den gruenen Button **"Code"** > **"Download ZIP"**
3. Entpacke die ZIP-Datei (Doppelklick im Finder)
4. Oeffne Terminal und navigiere in den Ordner:

```
cd ~/Downloads/tampermonkey-skripte-main/Claude\ Overlay\ macOS
```

---

## 6. Schritt 4: Virtuelle Umgebung erstellen

### Was ist eine virtuelle Umgebung (venv)?

Stell dir vor, Python hat einen grossen "Werkzeugkasten" (Pakete/Bibliotheken). Wenn du fuer verschiedene Projekte unterschiedliche Werkzeuge brauchst, kann es zu Konflikten kommen. Eine **virtuelle Umgebung** ist wie ein eigener, isolierter Werkzeugkasten nur fuer dieses Projekt.

### So erstellst du eine virtuelle Umgebung:

Stelle sicher, dass du im Ordner `Claude Overlay macOS` bist:

```
cd ~/Documents/tampermonkey-skripte/Claude\ Overlay\ macOS
```

Dann erstelle die virtuelle Umgebung:

```
python3 -m venv .venv
```

> **Was passiert hier?** Es wird ein Ordner `.venv` mit einer isolierten Python-Kopie erstellt.

Jetzt aktiviere die Umgebung:

```
source .venv/bin/activate
```

> **Was passiert hier?**
> - Nach der Aktivierung siehst du `(.venv)` am Anfang deiner Kommandozeile
> - Ab jetzt installiert `pip install` Pakete NUR in diesen isolierten Bereich
> - Du musst die Umgebung jedes Mal aktivieren, wenn du ein neues Terminal oeffnest

**Wichtig:** Wenn du `(.venv)` am Anfang der Zeile siehst, bist du in der virtuellen Umgebung.

---

## 7. Schritt 5: Abhaengigkeiten installieren

### Was sind Abhaengigkeiten?

Abhaengigkeiten (Dependencies) sind externe Pakete/Bibliotheken, die unser Tool braucht:

| Paket | Wofuer |
|---|---|
| `sounddevice` | Zugriff auf dein Mikrofon (Audio aufnehmen) |
| `soundfile` | Audio als WAV-Datei speichern |
| `numpy` | Mathematische Berechnungen fuer Audio-Daten |
| `requests` | HTTP-Anfragen an die APIs senden (Groq + Gemini) |
| `psutil` | Laufende Prozesse pruefen (ist Claude gestartet?) |

### So installierst du sie:

Stelle sicher, dass die virtuelle Umgebung aktiv ist (du siehst `(.venv)`), dann:

pip aktualisieren:

```
python3 -m pip install --upgrade pip
```

Abhaengigkeiten installieren:

```
pip install -r requirements.txt
```

> Das kann ein paar Minuten dauern. Warte, bis alles fertig ist (keine Fehlermeldungen in rot).

---

## 8. Schritt 6: API-Keys besorgen und eintragen

Das Tool braucht mindestens einen API-Key (Zugangsschluessel), um die Cloud-Dienste nutzen zu koennen.

### 8.1 Groq API-Key (fuer Sprach-Transkription) - Pflicht

1. Gehe zu: **https://console.groq.com/**
2. Erstelle ein Konto oder melde dich an
3. Navigiere zu **"API Keys"**
4. Klicke auf **"Create API Key"**
5. Kopiere den Schluessel (er beginnt mit `gsk_...`)

> **Was ist ein API-Key?** Ein API-Key ist wie ein Passwort, das deinem Tool erlaubt, den Dienst zu nutzen. Halte ihn geheim!

### 8.2 Gemini API-Key (fuer Textverbesserung) - Optional

1. Gehe zu: **https://aistudio.google.com/apikey**
2. Melde dich mit deinem Google-Konto an
3. Klicke auf **"Create API Key"**
4. Waehle ein Google Cloud Projekt (oder erstelle ein neues)
5. Kopiere den Schluessel

> **Hinweis:** Ohne Gemini-Key wird die Textverbesserung uebersprungen und der Rohtext direkt eingefuegt.

### 8.3 Keys in die .env-Datei eintragen

Vorlage kopieren:

```
cp .env.example .env
```

Datei bearbeiten:

```
nano .env
```

Trage deine Keys ein:

```
GROQ_API_KEY=gsk_dein-schluessel-hier
GEMINI_API_KEY=dein-gemini-schluessel-hier
```

Speichere die Datei (`Ctrl + O`, Enter, dann `Ctrl + X` zum Beenden von nano).

> **Wichtig:** Die `.env`-Datei wird NICHT auf GitHub hochgeladen (sie steht in `.gitignore`). Deine Keys bleiben lokal auf deinem Mac.

### 8.4 Optionale Einstellungen

In der `.env`-Datei kannst du auch folgendes anpassen:

| Variable | Standard | Beschreibung |
|---|---|---|
| `WHISPER_MODEL` | `whisper-large-v3` | Welches Whisper-Modell Groq nutzen soll |
| `WHISPER_LANG` | `de` | Sprache der Aufnahme (ISO 639-1 Code) |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini-Modell (auch `gemini-3.1-flash-lite-preview` moeglich) |
| `GEMINI_THINKING_LEVEL` | `MEDIUM` | Thinking-Level, nur fuer Gemini 3.x (LOW/MEDIUM/HIGH) |
| `AUDIO_SAMPLE_RATE` | `16000` | Audio-Abtastrate in Hz (16000 ist Standard fuer Sprache) |
| `AUDIO_CHANNELS` | `1` | Mono (1) oder Stereo (2) |
| `CLAUDE_PROCESS_NAMES` | `Claude,Claude Desktop` | Name(n) des Claude-Prozesses (kommagetrennt) |
| `OVERLAY_TARGET_PROCESS_NAMES` | `Claude,Claude Desktop,...` | Alle Apps, bei denen das Overlay sichtbar sein soll |

---

## 9. Schritt 7: Berechtigungen einrichten

macOS verlangt ausdrueckliche Berechtigungen fuer Mikrofon und Bedienungshilfen. Ohne diese Berechtigungen funktioniert das Overlay nicht.

### 9.1 Mikrofonzugriff

Beim ersten Start des Overlays fragt macOS automatisch nach der Mikrofonberechtigung. Klicke auf **"OK"** / **"Erlauben"**.

Falls du die Berechtigung versehentlich abgelehnt hast:

1. Oeffne **Systemeinstellungen** > **Datenschutz & Sicherheit** > **Mikrofon**
2. Aktiviere den Schalter neben **Terminal** (oder **Python**)

### 9.2 Bedienungshilfen (Accessibility)

Das Tool nutzt AppleScript, um Tastenanschlaege zu simulieren (Cmd+V zum Einfuegen). Dafuer braucht Terminal die Berechtigung fuer **Bedienungshilfen**:

1. Oeffne **Systemeinstellungen** > **Datenschutz & Sicherheit** > **Bedienungshilfen**
2. Klicke auf das **Schloss-Symbol** (unten links) und gib dein Passwort ein
3. Klicke auf **"+"** und fuege **Terminal** (oder **iTerm**) hinzu
4. Stelle sicher, dass der Schalter neben Terminal **aktiviert** ist

> **Warum?** Ohne diese Berechtigung kann das Tool keine Tastenanschlaege an Claude senden und der Text kann nicht eingefuegt werden.

---

## 10. Schritt 8: Tool starten

Es gibt mehrere Wege, das Tool zu starten:

### Variante A: Shell-Skript (empfohlen)

```
bash start_watcher.sh
```

> **Was passiert?**
> - Der Watcher startet im Hintergrund
> - Er prueft alle 2 Sekunden, ob Claude laeuft
> - Sobald du die Claude Desktop App oeffnest, erscheint das Overlay
> - Wenn du Claude schliesst, verschwindet das Overlay

### Variante B: Debug-Modus (zum Testen)

Wenn du Fehlermeldungen live sehen moechtest:

```
bash start_overlay.sh
```

> Dieser Modus zeigt alle Ausgaben und Fehlermeldungen direkt im Terminal. Druecke `Ctrl + C` zum Beenden.

### Variante C: Manuell starten

Virtuelle Umgebung aktivieren:

```
source .venv/bin/activate
```

Watcher starten:

```
python3 src/watcher.py
```

---

## 11. Schritt 9: Autostart einrichten (optional)

Damit der Watcher nach jedem Login automatisch startet:

### Autostart installieren:

```
bash install_autostart.sh
```

> **Was passiert?**
> - Es wird ein macOS **LaunchAgent** erstellt (unter `~/Library/LaunchAgents/`)
> - Der LaunchAgent startet den Watcher automatisch bei jedem Login
> - Komplett unsichtbar im Hintergrund
> - Du musst nichts mehr manuell starten

### Autostart entfernen:

```
bash uninstall_autostart.sh
```

---

## 12. Benutzung

### Mikrofon-Button (unterer Button)

1. **Einmal klicken** = Aufnahme starten (Button wird **rot**, pulsiert)
2. **Sprich** deinen Text ins Mikrofon
3. **Nochmal klicken** = Aufnahme stoppen (Button wird **orange** = Verarbeitung)
4. Das Tool sendet dein Audio an:
   - **Groq Whisper** (Sprache zu Text)
   - **Gemini** (Text verbessern + Intentionen erkennen) - falls konfiguriert
5. Der verbesserte Text wird automatisch in das Claude-Eingabefeld eingefuegt
6. Button wird **gruen** = Erfolg!

> **Tipp:** Auch lange Aufnahmen sind moeglich. Dateien ueber 20 MB werden automatisch in Teile aufgesplittet.

### Besen-Button (oberer Button)

- **Einmal klicken** = Leert das komplette Eingabefeld in Claude
- Nuetzlich, wenn du den Text loeschen und neu anfangen moechtest

### Gemini-Toggle (kleines "G" oben links)

- **Einmal klicken** = Gemini an/aus schalten
- **Gruen** = Gemini aktiv (Textverbesserung an)
- **Grau** = Gemini deaktiviert (Rohtext wird direkt eingefuegt)

### Overlay verschieben

- **Rechtsklick + Ziehen** oder **Ctrl + Klick + Ziehen** = Overlay verschieben

### Overlay schliessen

- **Escape-Taste** druecken, waehrend das Overlay fokussiert ist
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

## 13. Log-Dateien und Fehlerdiagnose

Das Tool schreibt Diagnose-Informationen in Log-Dateien im Projektordner:

| Datei | Inhalt |
|---|---|
| `watcher.log` | Watcher-Aktivitaet: Claude-Erkennung, Overlay-Start/Stop, Crash-Erkennung |
| `overlay.log` | Overlay-Aktivitaet: Config-Werte, Aufnahme, API-Aufrufe, Fehler |
| `failed_transcripts/` | Gesicherte Transkripte bei API-Fehlern (Text geht nicht verloren) |

> **Tipp:** Bei Problemen zuerst `overlay.log` pruefen. Log-Dateien anzeigen:

```
cat overlay.log
```

---

## 14. Fehlerbehebung / FAQ

### "python3 wird nicht erkannt" / "command not found"

Python ist nicht installiert. Installiere Python von **https://www.python.org/downloads/macos/** und starte das Terminal neu.

### "Keine Audiodaten aufgenommen"

- Pruefe, ob dein Mikrofon angeschlossen und aktiviert ist
- Gehe zu **Systemeinstellungen > Ton > Eingabe** und pruefe, ob das richtige Mikrofon ausgewaehlt ist
- Stelle sicher, dass Terminal die Mikrofon-Berechtigung hat (**Systemeinstellungen > Datenschutz & Sicherheit > Mikrofon**)

### "Claude-Fenster wurde nicht gefunden"

- Stelle sicher, dass die Claude Desktop App geoeffnet ist
- Falls deine App einen anderen Namen hat, passe `CLAUDE_PROCESS_NAMES` in der `.env` an

### Text wird nicht eingefuegt

- Stelle sicher, dass Terminal die **Bedienungshilfen-Berechtigung** hat
- Gehe zu **Systemeinstellungen > Datenschutz & Sicherheit > Bedienungshilfen**
- Fuege Terminal hinzu und aktiviere den Schalter

### "Fehlende Umgebungsvariablen: GROQ_API_KEY"

- Oeffne die `.env`-Datei und stelle sicher, dass du deinen API-Key eingetragen hast
- Pruefe, dass kein Leerzeichen vor oder nach dem `=` steht
- Stelle sicher, dass die `.env`-Datei im Ordner `Claude Overlay macOS` liegt (nicht in `src/`)

### "API-Fehler / 401 Unauthorized"

- Dein API-Key ist ungueltig oder abgelaufen
- Erstelle einen neuen Key auf der jeweiligen Plattform

### "API-Fehler / 429 Too Many Requests"

- Du hast zu viele Anfragen in kurzer Zeit gesendet
- Das Tool versucht automatisch bis zu 3-5 Mal erneut (mit steigender Wartezeit)
- Warte ein paar Minuten und versuche es erneut

### Das Overlay erscheint nicht

- Stelle sicher, dass der Watcher laeuft (`bash start_watcher.sh`)
- Oeffne die Claude Desktop App
- Pruefe im Aktivitaetsmonitor, ob `python3` oder `Python` laeuft
- Pruefe `watcher.log` auf Fehlermeldungen

### Das Overlay reagiert nicht auf Klicks

- Stelle sicher, dass du die **linke** Maustaste zum Klicken verwendest
- Rechtsklick (oder Ctrl+Klick) ist fuer Drag (Verschieben)

---

## 15. Projektstruktur

```
Claude Overlay macOS/
  .env.example              # Vorlage fuer API-Keys
  .env                      # Deine echten API-Keys (wird nicht hochgeladen)
  .gitignore                # Dateien, die Git ignorieren soll
  requirements.txt          # Liste der Python-Abhaengigkeiten
  README.md                 # Diese Anleitung
  start_watcher.sh          # Startet den Watcher im Hintergrund
  start_overlay.sh          # Startet das Overlay direkt (Debug-Modus)
  stop_watcher.sh           # Stoppt den laufenden Watcher und Overlay
  install_autostart.sh      # Richtet macOS-Autostart ein (LaunchAgent)
  uninstall_autostart.sh    # Entfernt macOS-Autostart
  watcher.log               # Log-Datei des Watchers (wird automatisch erstellt)
  overlay.log               # Log-Datei des Overlays (wird automatisch erstellt)
  failed_transcripts/       # Gesicherte Transkripte bei API-Fehlern
  src/
    config.py               # Laedt .env-Datei und stellt Einstellungen bereit
    audio_capture.py         # Nimmt Audio vom Mikrofon auf und speichert als WAV
    api_clients.py           # Kommuniziert mit Groq Whisper + Gemini API
    claude_window.py         # Findet die Claude-App und fuegt Text ein/loescht (AppleScript)
    overlay_app.py           # Die sichtbare Overlay-Oberflaeche (Buttons, Farben)
    watcher.py               # Ueberwacht, ob Claude laeuft; startet/stoppt Overlay
```

---

## 16. Deinstallation

### Autostart entfernen:

```
bash uninstall_autostart.sh
```

### Watcher/Overlay beenden:

```
bash stop_watcher.sh
```

Oder manuell im Aktivitaetsmonitor (`Cmd + Leertaste` > "Aktivitaetsmonitor") nach `python3` suchen und den Prozess beenden.

### Dateien loeschen:

Loesche einfach den Ordner `Claude Overlay macOS` (oder das gesamte Repository, wenn du es nicht mehr brauchst).

### Virtuelle Umgebung loeschen:

Die virtuelle Umgebung ist im Ordner `.venv` innerhalb des Projektordners. Wenn du den Projektordner loeschst, ist sie automatisch mit weg.
