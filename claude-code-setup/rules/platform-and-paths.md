# Plattform, Pfade & Dateisicherheit (KRITISCH)

> Konsolidiert aus: workspace-directory, codex-directory-forbidden,
> python-paths-windows, path-verify-after-shell-updates,
> folder-rename-safety, semantic-search-isolation, no-sed-on-json

---

## 1. Arbeitsverzeichnis: IMMER ~/proggs/

| Plattform | Pfad |
|-----------|------|
| Windows | `C:\Users\barwa\proggs` |
| macOS | `/Users/barwa/proggs` |

Das Home-Verzeichnis (`~`) ist NICHT das Arbeitsverzeichnis. Das Repository
`Pepsi1978/proggs` liegt in `~/proggs/`. Alle Projekte, Git-Operationen → dorthin.

**Was beim Session-Start passieren muss:**
1. Pruefen: Ist das aktuelle Verzeichnis `~/proggs/`?
2. Wenn NEIN: Sofort melden und alle Operationen auf `~/proggs/` beziehen

**Technische Absicherung:** Windows Terminal ist konfiguriert mit `startingDirectory: C:\Users\barwa\proggs`.

---

## 2. ~/Codex/ ist GESPERRT

| Plattform | Pfad |
|-----------|------|
| Windows | `C:\Users\barwa\Codex\` |
| macOS | `/Users/barwa/Codex/` |

Separater Klon des gleichen Repos, der **ausschliesslich von Codex im Terminal** genutzt wird.

**Was NIEMALS passieren darf:**
- ❌ Dateien in `~/Codex/` lesen, schreiben oder bearbeiten
- ❌ `cd ~/Codex` ausfuehren
- ❌ Git-Operationen in diesem Verzeichnis
- ❌ Pfade die auf `/Codex/` oder `\Codex\` zeigen verwenden

---

## 3. Python-Pfade auf Windows: NIEMALS /c/Users/...

Git Bash uebersetzt Windows-Pfade in Unix-Stil (`C:\Users\barwa` → `/c/Users/barwa`).
Python versteht dieses Format NICHT → `FileNotFoundError`.

```python
# FALSCH:
with open('/c/Users/barwa/.claude/settings.json') as f: ...

# RICHTIG:
import os
path = os.path.expanduser('~/.claude/settings.json')
with open(path, 'r', encoding='utf-8') as f: ...
```

**Faustregel:**
- Bash/Git Bash: `/c/Users/...` funktioniert
- Python, Node.js, PowerShell: `expanduser()` oder native Windows-Pfade

---

## 4. PATH-Verifikation nach Shell-Updates

**Regel:** Nach JEDEM Shell/Terminal-Update MUSS PATH geprueft werden.

### Wann Pflicht

- Nach `brew upgrade` (besonders node, python, deno, powershell)
- Nach `npm update -g` (besonders claude-code CLI)
- Nach `rustup update`, `winget upgrade`, manuellen Tool-Updates, OS-Updates

### Verifikations-Befehle

```bash
# macOS/Linux:
bash ~/.claude/hooks/path-verify.sh --fix

# Windows:
pwsh ~/.claude/hooks/path-verify.ps1 -Fix
```

### Was geprueft wird

**Tools:** git, node, npm, bun, python3, rustc, cargo, go, claude, swift (macOS),
kotlin, gradle, deno, ollama (macOS), claudewatch

**Verzeichnisse (macOS):** /opt/homebrew/bin, ~/.cargo/bin, ~/go/bin, ~/.bun/bin
**Verzeichnisse (Windows):** %USERPROFILE%\bin, .local\bin, .bun\bin, .cargo\bin, AppData\Roaming\npm, go\bin, Android SDK

**Pflicht-Reihenfolge bei Updates:**
1. Alle Aufgaben abschliessen, committen, pushen
2. Benutzer warnen ("Terminal-Neustart noetig")
3. Shell-Updates durchfuehren
4. SOFORT danach: path-verify --fix
5. Ergebnis dem Benutzer zeigen

---

## 5. Ordner-Umbenennung: Sicher durchfuehren

Bei Ordnern die von externen Prozessen genutzt werden:

1. Gradle Daemon, adb und andere Tools VORHER stoppen
2. Auf Windows: `cmd.exe //c "ren"` statt `git mv`
3. SOFORT `.gitattributes` LFS-Regeln aktualisieren (sonst 100MB-Rejection)

---

## 6. Semantische Suche: Plattform-Isolation

Die semantische Suche (code-search MCP, `.mcp.json`, Reindex-Hook) wird auf jedem
System **eigenstaendig** verwaltet.

### Was NIEMALS passieren darf

- macOS-Aenderungen duerfen Windows NICHT beeinflussen (und umgekehrt)
- Keine "plattformuebergreifenden" Fixes oder Vereinheitlichungen
- `.mcp.json` NIEMALS automatisch aendern (nur der Benutzer)

### Betroffene Dateien (HANDS OFF fuer jeweils andere Plattform)

| Datei | Beschreibung |
|-------|-------------|
| `~/proggs/.mcp.json` | Projekt-MCP (plattformspezifisch!) |
| `claude-code-setup/mcp-macos.json` | macOS-Referenz |
| `claude-code-setup/mcp-windows.json` | Windows-Referenz |
| `~/.claude/hooks/reindex-codebase.sh/.ps1` | Plattform-spezifische Hooks |
| `~/proggs/.code-search/` | Lokaler Index (nie synchronisieren) |

> Vorfall 20./21.03.2026: `.mcp.json` auf Windows "plattformuebergreifend" gemacht →
> macOS kaputt (MCP-Server brauchen unter macOS absolute Pfade).

---

## 7. JSON-Dateien: Kein sed, kein awk — NUR sichere Methoden

**Regel:** JSON-Dateien duerfen NIEMALS mit `sed`, `awk`, `echo >>` oder Bash-Textersetzung
bearbeitet werden.

### Erlaubte Methoden

1. **Edit-Tool** (bevorzugt) — praezise Ersetzungen
2. **Write-Tool** — komplette Neuschreibung nach Read
3. **Python `json`-Modul**:
   ```python
   python3 -c "import json; d=json.load(open('PFAD')); d['key']='value'; json.dump(d, open('PFAD','w'), indent=2)"
   ```

### Pflicht: Validierung nach JEDER Aenderung

```bash
python3 -c "import json; json.load(open('PFAD')); print('OK')"
```
Kein Commit ohne bestandene Validierung.

> Vorfall 2026-03-27: `sed` hat unescapte Anfuehrungszeichen in settings.json eingefuegt →
> JSON zerstoert → Claude Code startete nicht richtig.

---

## 8. Hook-Entwicklung: Einschraenkungen & Exit-Code-Sicherheit (KRITISCH)

### Keine Prompt-Hooks bei SessionStart

`type: "prompt"` Hooks duerfen NIEMALS im `SessionStart` Event verwendet werden
(kein ToolUseContext vorhanden). Alternative: `type: "command"` verwenden.

**Wo Prompt-Hooks funktionieren:** PreToolUse, PostToolUse, PostToolUseFailure,
PreCompact, PostCompact, Stop, StopFailure, SubagentStop, InstructionsLoaded ✅
**Wo NICHT:** SessionStart ❌, SessionEnd ❌

### Plugin-Hook-Skripte: Execute-Permission

Plugins koennen `.sh`-Dateien ohne `+x` installieren.
`plugin-health-check.sh` repariert das automatisch.
Bei Problemen: `find ~/.claude/plugins -name "*.sh" ! -perm -u+x` → `chmod +x`
Windows `.ps1`: `pwsh -ExecutionPolicy Bypass -File "path-to-script.ps1"`

### Debug-Methode bei Hook-Fehlern (30 Sekunden statt 20 Minuten)

```bash
# macOS/Linux:
echo "test" | claude -p --debug-file /tmp/hook-debug.txt --system-prompt "Reply with just OK"
grep "Hook.*error" /tmp/hook-debug.txt

# Windows:
echo "test" | claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"
Select-String -Pattern "Hook.*error|hook.*fail" -Path $env:TEMP\hook-debug.txt
```

### Die 3 Hook-Kategorien (PFLICHT-Wissen vor jedem Hook-Edit)

| Kategorie | Erkennung | Exit-Code-Regel |
|-----------|-----------|-----------------|
| **Dot-Sourced-Bibliotheken** | Andere Hooks laden sie mit `. "$PSScriptRoot/..."` | **NIEMALS `exit` verwenden** (beendet den Aufrufer!) |
| **Blocker/Guard-Hooks** | Enthalten `exit 1`/`exit 2` fuer Fehlerfaelle | `exit 1/2` bei Verstoss, `exit 0` nur am Ende als Default |
| **Standalone Non-Blocking** | Direkt aufgerufen, blockieren nichts | **MUSS `exit 0` am Ende haben** |

**Aktuelle Bibliotheken:** `hook-log.ps1`, `whiteboard-insert.ps1`
**Aktuelle Blocker:** `config-guard.ps1`, `safety-gate.ps1`, `agent-resource-lock.ps1`

### Pflicht VOR jedem Hook-Edit

1. `grep -rn 'dateiname.ps1' ~/.claude/hooks/` — wird die Datei per Dot-Source geladen?
2. Hat der Hook `exit 1`/`exit 2`? → Blocker, vorsichtig mit exit 0
3. Keins von beidem? → Standalone, `exit 0` am Ende Pflicht

> Vorfall 2026-04-04: `exit 0` blind zu 5 Hooks hinzugefuegt. 3 davon waren Bibliotheken →
> 15 Hooks still abgeschaltet. Poka-Yoke: `hook-exit0-guard.ps1` Pre-Commit-Hook prueft das jetzt.
