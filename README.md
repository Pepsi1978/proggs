# proggs

Dieses Repository enthaelt Voice-Overlay-Tools (macOS + Windows), Browser-Erweiterungen und das plattformuebergreifende Claude Code Setup.

---

## Claude Code Setup (Neuen Rechner einrichten)

Das **Wichtigste zuerst**: Wenn du Claude Code auf einem neuen Rechner einrichten willst, brauchst du nur **3 Befehle**. Das Setup-Skript installiert automatisch alle 32 Plugins, 4 Custom Skills, Dev-Tools (Bun, Deno, Rust, Docker, .NET), MCP-Server und den Parry Security-Scanner.

### macOS / Linux

```bash
git clone https://github.com/Pepsi1978/proggs.git
cd proggs
bash claude-code-setup/setup.sh
```

### Windows (PowerShell)

```powershell
git clone https://github.com/Pepsi1978/proggs.git
cd proggs
powershell claude-code-setup/setup.ps1
```

### Was wird installiert?

| Kategorie | Inhalt |
|-----------|--------|
| **Plugins** | 32 Stueck (26 offizielle + 7 Superpowers-Marketplace) |
| **Custom Skills** | auto-verify-iterate, cross-platform, undo-changes |
| **Dev-Tools** | Bun, Deno, Rust/Cargo, Docker, .NET SDK |
| **MCP Server** | Sequential Thinking (Problemzerlegung) |
| **Security** | Parry (Prompt-Injection-Scanner) |
| **Features** | Agent Teams (experimentell), Episodic Memory |

> **Hinweis:** Das Setup erkennt bereits installierte Tools und ueberspringt sie. Du kannst es also jederzeit erneut ausfuehren, um fehlende Komponenten nachzuinstallieren.

> **Automatisch:** Claude Code prueft beim Start automatisch die `claude-code-setup/manifest.json` und weist auf fehlende Plugins hin (siehe CLAUDE.md).

---

## Repository auf einem neuen Rechner einrichten / synchronisieren

Diese Anleitung erklaert, wie du dieses Repository auf einem **neuen Windows-Rechner** einrichtest oder auf einem **bestehenden Rechner** den aktuellen Stand holst.

---

### Situation A: Neuer Rechner (Ersteinrichtung)

Wenn du das Repository zum ersten Mal auf einem neuen PC einrichten moechtest:

#### 1. Git installieren

Git ist ein Versionsverwaltungs-Tool, mit dem du Code von GitHub herunterladen und synchronisieren kannst.

1. Gehe zu: **https://git-scm.com/download/win**
2. Lade den **64-bit Git for Windows Setup** herunter
3. Fuehre den Installer aus (Standardeinstellungen beibehalten, einfach "Next" klicken)

#### 2. Node.js installieren

Node.js wird fuer Claude Code und viele Plugins benoetigt.

1. Gehe zu: **https://nodejs.org/**
2. Lade die **LTS-Version** herunter und installiere sie

#### 3. Claude Code installieren

```powershell
npm install -g @anthropic-ai/claude-code
```

#### 4. Repository klonen und Setup ausfuehren

```powershell
cd $HOME
git clone https://github.com/Pepsi1978/proggs.git
cd proggs
powershell claude-code-setup/setup.ps1
```

> **Was passiert?** Das Setup-Skript installiert automatisch alle Plugins, Dev-Tools, MCP-Server und Custom Skills. Siehe oben fuer Details.

#### 5. API-Keys eintragen

Fuer Voice-Overlay-Projekte: Kopiere `.env.example` zu `.env` und trage deine API-Keys ein (GROQ_API_KEY, optional GEMINI_API_KEY).

> **Wichtig:** Die `.env`-Datei wird NICHT auf GitHub hochgeladen (steht in `.gitignore`). Du musst deine Keys auf jedem Rechner neu eintragen.

---


### Situation B: Bestehender Rechner (aktuellen Stand holen)

Wenn du das Repository bereits auf dem Rechner hast und nur die **neuesten Aenderungen** uebernehmen moechtest:

#### 1. In den Projektordner wechseln

```powershell
cd "$HOME\Documents\proggs"
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
