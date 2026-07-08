# Plattform, Pfade & Dateisicherheit (KRITISCH)

## 1. Arbeitsverzeichnis: IMMER ~/proggs/

Windows `C:\Users\barwa\proggs`, macOS `/Users/barwa/proggs`. Das Home-Verzeichnis ist NICHT das
Arbeitsverzeichnis. Beim Session-Start pruefen; falls pwd falsch: melden, alle Operationen auf ~/proggs beziehen.

## 2. ~/Codex/ ist GESPERRT

`C:\Users\barwa\Codex\` bzw. `/Users/barwa/Codex/` — separater Klon, NUR fuer Codex. NIEMALS dort
lesen/schreiben/`cd`/Git, keine Pfade auf `/Codex/`.

## 3. Python-Pfade auf Windows: NIEMALS /c/Users/...

Git Bash uebersetzt `C:\Users\barwa` → `/c/Users/barwa`; Python versteht das nicht (`FileNotFoundError`).

```python
import os
path = os.path.expanduser('~/.claude/settings.json')   # RICHTIG
```

Bash: `/c/Users/...` ok. Python/Node/PowerShell: `expanduser()` oder native Windows-Pfade.

## 4. PATH-Verifikation nach Shell-Updates (PFLICHT)

Nach `brew upgrade`/`npm update -g`/`rustup update`/`winget upgrade`/OS-Update:
`bash ~/.claude/hooks/path-verify.sh --fix` (macOS) bzw. `pwsh ~/.claude/hooks/path-verify.ps1 -Fix` (Windows).
Reihenfolge bei Updates: alle Aufgaben committen+pushen → Benutzer warnen (Terminal-Neustart) → Update →
SOFORT path-verify → Ergebnis zeigen.

## 5. Ordner-Umbenennung sicher

Gradle-Daemon/adb vorher stoppen · Windows `cmd.exe //c "ren"` statt `git mv` · sofort `.gitattributes`
LFS-Regeln aktualisieren (sonst 100MB-Rejection).

## 6. `.mcp.json`: Plattform-Isolation

`.mcp.json` ist pro System eigenstaendig — macOS-Aenderungen duerfen Windows NICHT beeinflussen (und
umgekehrt). `.mcp.json` NIEMALS automatisch aendern/"vereinheitlichen" (macOS braucht absolute Pfade).
HANDS OFF fuer die andere Plattform: `~/proggs/.mcp.json`, `mcp-macos.json`/`mcp-windows.json`.

## 7. JSON: kein sed/awk

Nur Edit-Tool, Write-Tool (nach Read) oder Python `json`-Modul. Nach JEDER Aenderung validieren:
```bash
python3 -c "import json; json.load(open('PFAD')); print('OK')"
```
Kein Commit ohne bestandene Validierung. (Vorfall: `sed` fuegte unescapte Quotes in settings.json → JSON kaputt.)

## 8. Hook-Entwicklung: Exit-Code-Sicherheit

- **Keine Prompt-Hooks (`type: "prompt"`) bei SessionStart/SessionEnd** (kein ToolUseContext) — `type: "command"` nutzen.
- **3 Hook-Kategorien (vor jedem Hook-Edit pruefen):** Dot-sourced Bibliothek (per `. "$PSScriptRoot/..."` geladen) → **NIEMALS `exit`** (killt Aufrufer); Blocker/Guard (hat `exit 1`/`exit 2`) → exit 1/2 bei Verstoss, exit 0 nur als Default am Ende; Standalone non-blocking → **MUSS `exit 0` am Ende**.
- Vor Hook-Edit: `grep -rn 'dateiname' ~/.claude/hooks/` (dot-sourced?) + auf `exit 1/2` pruefen.
  (Vorfall: `exit 0` blind zu 5 Hooks → 3 waren Bibliotheken → 15 Hooks still abgeschaltet.)
- Windows `.ps1`: `pwsh -ExecutionPolicy Bypass -File "..."`. Plugin-`.sh` ggf. `chmod +x`.
