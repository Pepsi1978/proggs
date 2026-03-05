# tampermonkey-skripte

Dieses Repository enthaelt verschiedene Tampermonkey-Skripte und Tools (z.B. das Claude Overlay fuer Windows und macOS).

---

## Repository auf einem neuen Rechner einrichten / synchronisieren

Diese Anleitung erklaert, wie du dieses Repository auf einem **neuen Windows-Rechner** einrichtest oder auf einem **bestehenden Rechner** den aktuellen Stand holst.

---

### Situation A: Neuer Rechner (Ersteinrichtung)

Wenn du das Repository zum ersten Mal auf einem neuen PC einrichten moechtest:

#### 1. Python installieren

Python ist die Programmiersprache, in der viele Tools in diesem Repository geschrieben sind.

1. Gehe zu: **https://www.python.org/downloads/**
2. Klicke auf **"Download Python 3.1x.x"**
3. **WICHTIG:** Setze im Installer den Haken bei **`Add python.exe to PATH`**
4. Klicke auf **"Install Now"**

> **Warum PATH?** Damit Windows den Befehl `python` in der Kommandozeile findet.

#### 2. Git installieren

Git ist ein Versionsverwaltungs-Tool, mit dem du Code von GitHub herunterladen und synchronisieren kannst.

1. Gehe zu: **https://git-scm.com/download/win**
2. Lade den **64-bit Git for Windows Setup** herunter
3. Fuehre den Installer aus (Standardeinstellungen beibehalten, einfach "Next" klicken)

#### 3. Installation pruefen

Oeffne **Windows PowerShell** (`Win + X` > "Terminal" oder "PowerShell"):

```powershell
python --version
git --version
```

> Beide Befehle muessen eine Versionsnummer anzeigen. Falls nicht, starte PowerShell nach der Installation neu.

#### 4. Repository klonen

```powershell
cd $HOME\Documents
git clone https://github.com/Pepsi1978/tampermonkey-skripte.git
cd tampermonkey-skripte
```

> **Was passiert?** `git clone` laedt das komplette Repository (alle Dateien und die gesamte Versionshistorie) von GitHub auf deinen Rechner herunter.

#### 5. Projekt einrichten (am Beispiel Claude Overlay Windows)

```powershell
cd "Claude Overlay Windows"
python -m venv .venv
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

> **Warum eine virtuelle Umgebung (.venv)?** Sie isoliert die Python-Pakete dieses Projekts von anderen Projekten auf deinem Rechner. Die `.venv` wird NICHT im Repository gespeichert und muss auf jedem Rechner neu erstellt werden.

#### 6. API-Keys eintragen

```powershell
copy .env.example .env
notepad .env
```

Trage deine API-Keys ein (GROQ_API_KEY, optional GEMINI_API_KEY) und speichere die Datei.

> **Wichtig:** Die `.env`-Datei wird NICHT auf GitHub hochgeladen (steht in `.gitignore`). Du musst deine Keys auf jedem Rechner neu eintragen.

---

### Situation B: Bestehender Rechner (aktuellen Stand holen)

Wenn du das Repository bereits auf dem Rechner hast und nur die **neuesten Aenderungen** uebernehmen moechtest:

#### 1. In den Projektordner wechseln

```powershell
cd "$HOME\Documents\tampermonkey-skripte"
```

#### 2. Aenderungen vom Server holen

```powershell
git pull origin main
```

> **Was passiert?**
> - `git pull` laedt alle neuen Commits von GitHub herunter und wendet sie auf deine lokale Kopie an
> - `origin` ist der Name des GitHub-Servers (Standardname beim Klonen)
> - `main` ist der Hauptbranch des Repositories

#### 3. Pruefen, was sich geaendert hat

```powershell
git log --oneline -10
```

> Zeigt die letzten 10 Commits mit Kurzbeschreibung an.

#### 4. Eventuell neue Abhaengigkeiten installieren

Falls sich die `requirements.txt` eines Projekts geaendert hat:

```powershell
cd "Claude Overlay Windows"
.\.venv\Scripts\activate
pip install -r requirements.txt
```

> **Tipp:** Im Zweifel einfach ausfuehren - bereits installierte Pakete werden uebersprungen.

---

### Kurzreferenz: Die wichtigsten Git-Befehle

| Befehl | Was er tut |
|---|---|
| `git clone <url>` | Repository zum ersten Mal herunterladen |
| `git pull origin main` | Neueste Aenderungen vom Server holen und anwenden |
| `git status` | Zeigt, ob du lokale Aenderungen hast (geaenderte/neue Dateien) |
| `git log --oneline -10` | Letzte 10 Commits anzeigen (Kurzform) |
| `git diff` | Zeigt deine lokalen Aenderungen im Detail an |
| `git add .` | Alle lokalen Aenderungen fuer einen Commit vormerken |
| `git commit -m "Beschreibung"` | Aenderungen als neuen Commit speichern |
| `git push origin main` | Lokale Commits auf den Server (GitHub) hochladen |

---

### Typischer Workflow zwischen zwei Rechnern

```
Rechner A (Aenderungen machen):
  git add .
  git commit -m "Beschreibung der Aenderung"
  git push origin main

Rechner B (Aenderungen uebernehmen):
  git pull origin main
```

> **Wichtig:** Fuehre `git pull` immer **vor** deinen eigenen Aenderungen aus, damit es keine Konflikte gibt. Falls doch ein Konflikt entsteht (beide Rechner haben die gleiche Datei geaendert), zeigt Git dir die betroffenen Stellen an - du musst dann manuell entscheiden, welche Version behalten wird.

---

### Haeufige Probleme

| Problem | Loesung |
|---|---|
| `python wird nicht erkannt` | Python neu installieren mit Haken bei "Add to PATH", dann PowerShell neu starten |
| `git wird nicht erkannt` | Git installieren von https://git-scm.com/download/win, dann PowerShell neu starten |
| `Activate.ps1 kann nicht geladen werden` | Einmalig ausfuehren: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` |
| `git pull` zeigt Konflikte | Konflikte in den betroffenen Dateien manuell loesen, dann `git add .` und `git commit` |
| `pip install` schlaegt fehl | Pruefen ob `.venv` aktiviert ist (steht `(.venv)` am Zeilenanfang?) |
