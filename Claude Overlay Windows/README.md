# Claude Overlay Windows

Ein Python-Overlay fuer die **Windows Claude Code Desktop App** mit zwei Buttons:

- **Mikrofon-Button** (Mic): Nimmt deine Sprache auf, schickt das Audio an die **Grok Whisper API** (Transkription), dann an die **Gemini API** (Intentionserkennung + hochwertiges Deutsch) und fuegt den verbesserten Text automatisch in das Claude-Eingabefeld ein.
- **X-Button**: Leert das gesamte Eingabefeld in der Claude Desktop App.

**Zusatzfunktionen:**

- Der **Watcher** (`watcher.py`) ueberwacht, ob die Claude Desktop App laeuft.
- Wenn Claude gestartet wird: Das Overlay startet automatisch.
- Wenn Claude geschlossen wird: Das Overlay beendet sich automatisch.
- Das Overlay ist **rahmenlos** (kein Fenstertitel), **immer im Vordergrund** und kann per **Rechtsklick + Ziehen** frei verschoben werden.
- Waehrend der Aufnahme **pulsiert** der Mikrofon-Button rot.

---

## Inhaltsverzeichnis

1. [Was macht dieses Tool?](#1-was-macht-dieses-tool)
2. [Voraussetzungen](#2-voraussetzungen)
3. [Schritt 1: Python installieren](#3-schritt-1-python-installieren)
4. [Schritt 2: Git installieren (optional)](#4-schritt-2-git-installieren-optional)
5. [Schritt 3: Repository herunterladen](#5-schritt-3-repository-herunterladen)
6. [Schritt 4: Virtuelle Umgebung erstellen](#6-schritt-4-virtuelle-umgebung-erstellen)
7. [Schritt 5: Abhaengigkeiten installieren](#7-schritt-5-abhaengigkeiten-installieren)
8. [Schritt 6: API-Keys besorgen und eintragen](#8-schritt-6-api-keys-besorgen-und-eintragen)
9. [Schritt 7: Tool starten](#9-schritt-7-tool-starten)
10. [Schritt 8: Windows-Autostart einrichten (optional)](#10-schritt-8-windows-autostart-einrichten-optional)
11. [Benutzung](#11-benutzung)
12. [Fehlerbehebung / FAQ](#12-fehlerbehebung--faq)
13. [Projektstruktur](#13-projektstruktur)
14. [Deinstallation](#14-deinstallation)

---

## 1. Was macht dieses Tool?

Wenn du mit der Claude Desktop App arbeitest, moechtest du vielleicht manchmal **per Sprache** Eingaben machen, anstatt alles zu tippen. Dieses Tool:

1. **Hoert dir zu** (Mikrofon-Aufnahme)
2. **Wandelt deine Sprache in Text um** (Grok Whisper API - eine sehr gute Spracherkennung von xAI)
3. **Verbessert den Text** (Gemini API - erkennt deine Absichten und formuliert sie in gutem Deutsch)
4. **Fuegt den fertigen Text ein** (automatisch in das Claude-Eingabefeld)

Das spart Zeit und sorgt dafuer, dass deine Spracheingaben sauber und klar formuliert bei Claude ankommen.

---

## 2. Voraussetzungen

Bevor du loslegst, brauchst du folgendes auf deinem Windows-Rechner:

| Was | Warum |
|---|---|
| **Windows 10 oder 11** | Das Tool nutzt Windows-spezifische Funktionen (Fenstererkennung, UI-Automation) |
| **Python 3.11 oder neuer** | Die Programmiersprache, in der das Tool geschrieben ist. Python fuehrt den Code aus |
| **Claude Desktop App** | Die App, in die das Overlay den Text einfuegt |
| **Internetverbindung** | Fuer die API-Aufrufe (Grok Whisper + Gemini) |
| **Ein Mikrofon** | Zum Aufnehmen deiner Sprache (eingebaut oder extern) |
| **Grok API-Key** | Zugang zur Sprach-Transkription (von xAI / Grok) |
| **Gemini API-Key** | Zugang zur Textverbesserung (von Google) |

---

## 3. Schritt 1: Python installieren

### Was ist Python?

Python ist eine Programmiersprache. Dein Computer kann Python-Programme nur ausfuehren, wenn Python installiert ist - aehnlich wie du einen PDF-Reader brauchst, um PDFs zu oeffnen.

### So installierst du Python:

1. Oeffne deinen Browser und gehe zu: **https://www.python.org/downloads/**

2. Klicke auf den grossen gelben Button **"Download Python 3.1x.x"** (die neueste Version).

3. **WICHTIG:** Wenn der Installer startet, setze **unbedingt** den Haken bei:
   ```
   [x] Add python.exe to PATH
   ```
   > **Warum?** PATH ist eine Liste von Ordnern, in denen Windows nach Programmen sucht. Wenn Python nicht im PATH ist, kann Windows den Befehl `python` nicht finden und du bekommst Fehler wie "python wird nicht erkannt".

4. Klicke dann auf **"Install Now"**.

5. Warte, bis die Installation abgeschlossen ist, und klicke auf **"Close"**.

### Installation pruefen:

Oeffne die **Windows PowerShell** (druecke `Win + X` und waehle "Terminal" oder "PowerShell"):

```powershell
python --version
```

Du solltest so etwas sehen wie: `Python 3.12.5` (oder neuer).

Falls stattdessen der Microsoft Store aufgeht oder "python nicht erkannt" kommt:
- Starte den Python-Installer erneut
- Waehle **"Modify"** und stelle sicher, dass der PATH-Haken gesetzt ist
- Starte danach PowerShell/Terminal NEU

---

## 4. Schritt 2: Git installieren (optional)

### Was ist Git?

Git ist ein Versionsverwaltungs-Tool. Entwickler nutzen es, um Code-Aenderungen zu verfolgen und Code von GitHub herunterzuladen. Du brauchst Git nur, wenn du das Repository per Kommandozeile **klonen** moechtest. Alternativ kannst du den Code auch als ZIP herunterladen (siehe Schritt 3).

### So installierst du Git:

1. Gehe zu: **https://git-scm.com/download/win**
2. Lade den **64-bit Git for Windows Setup** herunter
3. Fuehre den Installer aus - du kannst alle Standard-Einstellungen beibehalten (einfach "Next" klicken)
4. Bei der Frage zum Standard-Editor kannst du **"Use Notepad"** waehlen (einfacher als Vim)

### Installation pruefen:

```powershell
git --version
```

Erwartete Ausgabe: `git version 2.x.x`

---

## 5. Schritt 3: Repository herunterladen

Es gibt zwei Wege, den Code auf deinen Rechner zu bekommen:

### Variante A: Mit Git klonen (empfohlen)

```powershell
cd $HOME\Documents
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd "tampermonkey-skripte\Claude Overlay Windows"
```

> **Was passiert hier?**
> - `cd $HOME\Documents` - Wechselt in deinen Dokumente-Ordner
> - `git clone ...` - Laedt das komplette Repository von GitHub herunter
> - `cd "Claude Overlay Windows"` - Wechselt in den Projektordner

### Variante B: Als ZIP herunterladen (ohne Git)

1. Gehe zu: **https://github.com/Pepsi1978/tampermonkey-skripte**
2. Klicke auf den gruenen Button **"Code"** > **"Download ZIP"**
3. Entpacke die ZIP-Datei (Rechtsklick > "Alle extrahieren")
4. Oeffne PowerShell und navigiere in den Ordner:

```powershell
cd "$HOME\Documents\tampermonkey-skripte-main\Claude Overlay Windows"
```

---

## 6. Schritt 4: Virtuelle Umgebung erstellen

### Was ist eine virtuelle Umgebung (venv)?

Stell dir vor, Python hat einen grossen "Werkzeugkasten" (Pakete/Bibliotheken). Wenn du fuer verschiedene Projekte unterschiedliche Werkzeuge brauchst, kann es zu Konflikten kommen. Eine **virtuelle Umgebung** ist wie ein eigener, isolierter Werkzeugkasten nur fuer dieses Projekt. So stoeren sich verschiedene Projekte nicht gegenseitig.

### So erstellst du eine virtuelle Umgebung:

Stelle sicher, dass du im Ordner `Claude Overlay Windows` bist:

```powershell
cd "$HOME\Documents\tampermonkey-skripte\Claude Overlay Windows"
```

Dann erstelle und aktiviere die virtuelle Umgebung:

```powershell
python -m venv .venv
```

> **Was passiert hier?**
> - `python -m venv` - Ruft das eingebaute venv-Modul auf
> - `.venv` - Erstellt einen Ordner namens ".venv" mit einer isolierten Python-Kopie

Jetzt aktiviere die Umgebung:

> **Wichtig vorab - PowerShell-Skripte erlauben:**
> Windows blockiert standardmaessig das Ausfuehren von PowerShell-Skripten (auch lokale wie `activate`).
> Das musst du einmalig erlauben - tippe diesen Befehl und bestatige mit **`Y`**:
>
> ```powershell
> Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
> ```
>
> **Warum?** `RemoteSigned` ist ein sicherer Kompromiss: Lokale Skripte (wie unser `activate`) duerfen laufen, aber aus dem Internet heruntergeladene Skripte muessen signiert sein. Diese Einstellung gilt nur fuer deinen Benutzer, nicht systemweit.

```powershell
.\.venv\Scripts\activate
```

> **Was passiert hier?**
> - Nach der Aktivierung siehst du `(.venv)` am Anfang deiner Kommandozeile
> - Ab jetzt installiert `pip install` Pakete NUR in diesen isolierten Bereich
> - Du musst die Umgebung jedes Mal aktivieren, wenn du ein neues Terminal oeffnest

**Wichtig:** Wenn du `(.venv)` am Anfang der Zeile siehst, bist du in der virtuellen Umgebung. Falls nicht, fuehre den `activate`-Befehl erneut aus.

---

## 7. Schritt 5: Abhaengigkeiten installieren

### Was sind Abhaengigkeiten?

Abhaengigkeiten (Dependencies) sind externe Pakete/Bibliotheken, die unser Tool braucht. Zum Beispiel:

| Paket | Wofuer |
|---|---|
| `sounddevice` | Zugriff auf dein Mikrofon (Audio aufnehmen) |
| `soundfile` | Audio als WAV-Datei speichern |
| `numpy` | Mathematische Berechnungen fuer Audio-Daten |
| `requests` | HTTP-Anfragen an die APIs senden (Grok + Gemini) |
| `psutil` | Laufende Prozesse pruefen (ist Claude gestartet?) |
| `pywinauto` | Windows-Fenster finden und fernsteuern |
| `pywin32` | Grundlegende Windows-Funktionen fuer pywinauto |
| `pyperclip` | Text in die Zwischenablage kopieren |

### So installierst du sie:

Stelle sicher, dass die virtuelle Umgebung aktiv ist (du siehst `(.venv)`), dann:

```powershell
python -m pip install --upgrade pip
pip install -r requirements.txt
```

> **Was passiert hier?**
> - `python -m pip install --upgrade pip` - Aktualisiert pip (den Paketmanager) auf die neueste Version
> - `pip install -r requirements.txt` - Installiert alle Pakete, die in der Datei `requirements.txt` aufgelistet sind

Das kann ein paar Minuten dauern, da manche Pakete gross sind. Warte, bis alles fertig ist (keine Fehlermeldungen in rot).

---

## 8. Schritt 6: API-Keys besorgen und eintragen

Das Tool braucht zwei API-Keys (Zugangsschluessel), um die Cloud-Dienste nutzen zu koennen.

### 8.1 Grok API-Key (fuer Sprach-Transkription)

Der Grok API-Key kommt von **xAI** (dem Unternehmen hinter Grok/X):

1. Gehe zu: **https://console.x.ai/**
2. Erstelle ein Konto oder melde dich an
3. Navigiere zu **"API Keys"**
4. Klicke auf **"Create API Key"**
5. Kopiere den Schluessel (er beginnt oft mit `xai-...`)

> **Was ist ein API-Key?** Ein API-Key ist wie ein Passwort, das deinem Tool erlaubt, den Dienst zu nutzen. Halte ihn geheim und teile ihn mit niemandem!

### 8.2 Gemini API-Key (fuer Textverbesserung)

Der Gemini API-Key kommt von **Google**:

1. Gehe zu: **https://aistudio.google.com/apikey**
2. Melde dich mit deinem Google-Konto an
3. Klicke auf **"Create API Key"**
4. Waehle ein Google Cloud Projekt (oder erstelle ein neues)
5. Kopiere den Schluessel

### 8.3 Keys in die .env-Datei eintragen

Kopiere die Vorlage und oeffne sie zum Bearbeiten:

```powershell
copy .env.example .env
notepad .env
```

Trage deine Keys ein:

```
GROK_API_KEY=xai-dein-schluessel-hier
GEMINI_API_KEY=dein-gemini-schluessel-hier
```

Speichere die Datei (`Strg + S`) und schliesse Notepad.

> **Wichtig:** Die `.env`-Datei wird NICHT auf GitHub hochgeladen (sie steht in `.gitignore`). Deine Keys bleiben lokal auf deinem Rechner.

### 8.4 Optionale Einstellungen

In der `.env`-Datei kannst du auch folgendes anpassen:

| Variable | Standard | Beschreibung |
|---|---|---|
| `GROK_WHISPER_MODEL` | `grok-2-whisper-1` | Welches Whisper-Modell Grok nutzen soll |
| `GEMINI_MODEL` | `gemini-3.1-flash-lite-preview` | Welches Gemini-Modell genutzt wird |
| `GEMINI_THINKING_LEVEL` | `medium` | Thinking-Level für Gemini (none/low/medium/high) |
| `AUDIO_SAMPLE_RATE` | `16000` | Audio-Abtastrate in Hz (16000 ist Standard fuer Sprache) |
| `AUDIO_CHANNELS` | `1` | Mono (1) oder Stereo (2) |
| `CLAUDE_PROCESS_NAMES` | `Claude.exe,...` | Name(n) des Claude-Prozesses (kommagetrennt) |

---

## 9. Schritt 7: Tool starten

Es gibt zwei Wege, das Tool zu starten:

### Variante A: Watcher-Modus (empfohlen)

Der Watcher laeuft im Hintergrund und startet/stoppt das Overlay automatisch:

```powershell
.\start_watcher.bat
```

Oder per Doppelklick auf `start_watcher.bat` im Datei-Explorer.

> **Was passiert?**
> - Der Watcher prueft alle 2 Sekunden, ob Claude laeuft
> - Sobald du die Claude Desktop App oeffnest, erscheint das Overlay
> - Wenn du Claude schliesst, verschwindet das Overlay
> - Der Watcher laeuft unsichtbar im Hintergrund (kein Fenster)

### Variante B: Debug-Modus (zum Testen)

Wenn du Fehlermeldungen sehen moechtest:

```powershell
.\start_overlay_debug.bat
```

> Dieser Modus zeigt ein Konsolenfenster mit Ausgaben und Fehlermeldungen. Nuetzlich zum Debuggen.

**Hinweis:** Im Debug-Modus muss Claude bereits laufen, sonst beendet sich das Overlay sofort.

---

## 10. Schritt 8: Windows-Autostart einrichten (optional)

Damit der Watcher nach jedem Windows-Login automatisch startet:

```powershell
powershell -ExecutionPolicy Bypass -File .\install_autostart.ps1
```

> **Was passiert?**
> - Das Skript erstellt eine Verknuepfung im Windows-Autostart-Ordner
> - Nach dem naechsten Neustart/Login laeuft der Watcher automatisch
> - Du musst nichts mehr manuell starten

### Autostart wieder entfernen:

```powershell
powershell -ExecutionPolicy Bypass -File .\uninstall_autostart.ps1
```

> **Was bedeutet `-ExecutionPolicy Bypass`?** Windows blockiert standardmaessig PowerShell-Skripte aus Sicherheitsgruenden. `Bypass` erlaubt die einmalige Ausfuehrung dieses Skripts, ohne die globale Sicherheitsrichtlinie zu aendern.

---

## 11. Benutzung

### Mikrofon-Button (linker Button)

1. **Einmal klicken** = Aufnahme starten (Button wird **rot**, pulsiert)
2. **Sprich** deinen Text ins Mikrofon
3. **Nochmal klicken** = Aufnahme stoppen (Button wird **orange** = Verarbeitung)
4. Das Tool sendet dein Audio an:
   - **Grok Whisper** (Sprache zu Text)
   - **Gemini** (Text verbessern + Intentionen erkennen)
5. Der verbesserte Text wird automatisch in das Claude-Eingabefeld eingefuegt
6. Button wird **gruen** = Erfolg!

### X-Button (rechter Button)

- **Einmal klicken** = Leert das komplette Eingabefeld in Claude
- Nuetzlich, wenn du den Text loeschen und neu anfangen moechtest

### Overlay verschieben

- **Rechtsklick + Ziehen** = Overlay an eine andere Stelle auf dem Bildschirm verschieben

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

## 12. Fehlerbehebung / FAQ

### "python wird nicht erkannt"

Python ist nicht im PATH. Installiere Python erneut und setze den Haken bei **"Add python.exe to PATH"**. Starte danach das Terminal neu.

### "pip wird nicht erkannt"

Versuche stattdessen:
```powershell
python -m pip install -r requirements.txt
```

### "Keine Audiodaten aufgenommen"

- Pruefe, ob dein Mikrofon angeschlossen und aktiviert ist
- Gehe zu **Windows-Einstellungen > System > Sound > Eingabe** und pruefe, ob das richtige Mikrofon ausgewaehlt ist
- Teste dein Mikrofon mit der Windows-Sprachaufnahme-App

### "Claude-Fenster wurde nicht gefunden"

- Stelle sicher, dass die Claude Desktop App geoeffnet ist
- Falls deine Claude-App einen anderen Fenstertitel hat, passe `CLAUDE_PROCESS_NAMES` in der `.env` an

### "Fehlende Umgebungsvariablen: GROK_API_KEY"

- Oeffne die `.env`-Datei und stelle sicher, dass du deinen API-Key eingetragen hast
- Pruefe, dass kein Leerzeichen vor oder nach dem `=` steht
- Stelle sicher, dass die `.env`-Datei im Ordner `Claude Overlay Windows` liegt (nicht in `src/`)

### "API-Fehler / 401 Unauthorized"

- Dein API-Key ist ungueltig oder abgelaufen
- Erstelle einen neuen Key auf der jeweiligen Plattform

### "API-Fehler / 429 Too Many Requests"

- Du hast zu viele Anfragen in kurzer Zeit gesendet
- Warte ein paar Minuten und versuche es erneut
- Pruefe dein API-Kontingent auf der jeweiligen Plattform

### Das Overlay erscheint nicht

- Stelle sicher, dass der Watcher laeuft (`start_watcher.bat`)
- Oeffne die Claude Desktop App
- Pruefe im Task-Manager, ob `pythonw.exe` laeuft

### Das Overlay reagiert nicht auf Klicks

- Stelle sicher, dass du den **linken** Maustaste zum Klicken verwendest
- Rechtsklick ist fuer Drag (Verschieben)
- Falls das Overlay im Processing-Zustand (orange) haengt, beende es ueber den Task-Manager und starte neu

---

## 13. Projektstruktur

```
Claude Overlay Windows/
  .env.example            # Vorlage fuer API-Keys
  .env                    # Deine echten API-Keys (wird nicht hochgeladen)
  .gitignore              # Dateien, die Git ignorieren soll
  requirements.txt        # Liste der Python-Abhaengigkeiten
  start_watcher.bat       # Startet den Hintergrund-Watcher
  start_overlay_debug.bat # Startet das Overlay im Debug-Modus
  install_autostart.ps1   # Richtet Windows-Autostart ein
  uninstall_autostart.ps1 # Entfernt Windows-Autostart
  README.md               # Diese Anleitung
  src/
    config.py             # Laedt .env-Datei und stellt Einstellungen bereit
    audio_capture.py      # Nimmt Audio vom Mikrofon auf und speichert als WAV
    api_clients.py        # Kommuniziert mit Grok Whisper + Gemini API
    claude_window.py      # Findet das Claude-Fenster und fuegt Text ein/loescht
    overlay_app.py        # Die sichtbare Overlay-Oberflaeche (Buttons, Farben)
    watcher.py            # Ueberwacht, ob Claude laeuft; startet/stoppt Overlay
```

---

## 14. Deinstallation

### Autostart entfernen:

```powershell
powershell -ExecutionPolicy Bypass -File .\uninstall_autostart.ps1
```

### Watcher/Overlay beenden:

Oeffne den Task-Manager (`Strg + Shift + Esc`), suche nach `pythonw.exe` oder `python.exe` und beende den Prozess.

### Dateien loeschen:

Loesche einfach den Ordner `Claude Overlay Windows` (oder das gesamte Repository, wenn du es nicht mehr brauchst).

### Virtuelle Umgebung loeschen:

Die virtuelle Umgebung ist im Ordner `.venv` innerhalb des Projektordners. Wenn du den Projektordner loeschst, ist sie automatisch mit weg.
