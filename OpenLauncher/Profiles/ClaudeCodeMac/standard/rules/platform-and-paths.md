# Plattform, Pfade & Dateisicherheit (KRITISCH)

## 1. Arbeitsverzeichnis ~/proggs/
Windows `/Users/frank/proggs`, macOS `/Users/barwa/proggs` (nicht Home). Falsch -> melden.

## 2. ~/Codex/ ist GESPERRT
`~/Codex/` = Codex-only-Klon. NIEMALS dort lesen/schreiben/`cd`/Git.

## 3. Python-Pfade auf Windows
Git Bash: `C:\..` -> `/c/Users/..`; Python bricht (`FileNotFoundError`). Nutze
`os.path.expanduser()`/native Pfade; Bash: `/c/Users/...` ok.

## 4. PATH-Verifikation nach Shell-/OS-Updates (PFLICHT)
`path-verify.sh --fix` (macOS) / `path-verify.ps1 -Fix` (Windows).
Erst pushen -> warnen -> Update -> path-verify.

## 5. Ordner-Umbenennung sicher
Gradle-Daemon/adb stoppen; Windows `cmd.exe //c "ren"` statt `git mv`; `.gitattributes` LFS-Regeln
(sonst 100MB-Rejection).

## 6. .mcp.json: Plattform-Isolation
Pro System eigenstaendig; NIEMALS automatisch "vereinheitlichen" (macOS: absolute Pfade). HANDS OFF:
`~/proggs/.mcp.json`, `mcp-{macos,windows}.json`.

## 7. JSON: kein sed/awk
Nur Edit/Write-Tool oder Python `json`-Modul; nach jeder Aenderung
`python3 -c "import json; json.load(open('PFAD'))"`. Kein Commit ohne Validierung.

## 8. Hook-Entwicklung: Exit-Code-Sicherheit
Keine Prompt-Hooks bei SessionStart/SessionEnd (`type:"command"`). Dot-sourced Lib -> NIEMALS `exit`;
Blocker/Guard -> `exit 1/2` bei Verstoss, sonst `exit 0`; Standalone -> MUSS `exit 0`. Vor Hook-Edit:
dot-sourced? + auf `exit 1/2` pruefen.
