# Plattform, Pfade & Dateisicherheit (KRITISCH)

## 1. Arbeitsverzeichnis: IMMER ~/proggs/
Windows `C:\Users\barwa\proggs`, macOS `/Users/barwa/proggs`. Home ist NICHT das Arbeitsverzeichnis.
Beim Session-Start pruefen; falsch → melden, alles auf ~/proggs beziehen.

## 2. ~/Codex/ ist GESPERRT
`~/Codex/` — separater Klon NUR fuer Codex. NIEMALS dort lesen/schreiben/`cd`/Git.

## 3. Python-Pfade auf Windows: NIEMALS /c/Users/...
Git Bash uebersetzt `C:\Users\barwa` → `/c/Users/barwa`; Python versteht das nicht (`FileNotFoundError`).
Python/Node/PowerShell: `os.path.expanduser()` oder native Pfade. Bash: `/c/Users/...` ok.

## 4. PATH-Verifikation nach Shell-Updates (PFLICHT)
Nach `brew upgrade`/`npm update -g`/`rustup`/`winget upgrade`/OS-Update: `path-verify.sh --fix` (macOS) /
`path-verify.ps1 -Fix` (Windows). Reihenfolge: erst alles committen+pushen → Benutzer warnen → Update → path-verify.

## 5. Ordner-Umbenennung sicher
Gradle-Daemon/adb vorher stoppen · Windows `cmd.exe //c "ren"` statt `git mv` · sofort `.gitattributes`
LFS-Regeln aktualisieren (sonst 100MB-Rejection).

## 6. .mcp.json: Plattform-Isolation
`.mcp.json` ist pro System eigenstaendig — macOS-Aenderungen duerfen Windows nicht beeinflussen. NIEMALS
automatisch "vereinheitlichen" (macOS braucht absolute Pfade). HANDS OFF: `~/proggs/.mcp.json`, `mcp-{macos,windows}.json`.

## 7. JSON: kein sed/awk
Nur Edit/Write-Tool oder Python `json`-Modul. Nach JEDER Aenderung validieren:
`python3 -c "import json; json.load(open('PFAD'))"`. Kein Commit ohne bestandene Validierung.

## 8. Hook-Entwicklung: Exit-Code-Sicherheit
Keine Prompt-Hooks bei SessionStart/SessionEnd (`type:"command"`). 3 Kategorien: Dot-sourced Bibliothek →
NIEMALS `exit`; Blocker/Guard → `exit 1/2` bei Verstoss, sonst `exit 0`; Standalone → MUSS `exit 0` am
Ende. Vor Hook-Edit: dot-sourced? + auf `exit 1/2` pruefen.
