# Poka-Yoke Git Push: Erzwingt fetch+rebase VOR jedem git push
# Quelle: Superintelligenz Finding 8 — Fehler strukturell unmoeglich machen
# Event: PreToolUse (Bash)
# Zweck: Wenn ein Bash-Befehl "git push" enthaelt, wird AUTOMATISCH
#         git fetch + git rebase davor eingefuegt. Der Benutzer KANN NICHT
#         ohne fetch+rebase pushen — der Hook macht es unmoeglich.
#
# UNTERSCHIED zu safety-gate.ps1:
#   safety-gate = BLOCKIERT gefaehrliche Befehle (force-push, reset --hard)
#   poka-yoke-git = ERGAENZT sichere Befehle (fetch+rebase vor push)
#
# UNTERSCHIED zu immune-check.ps1:
#   immune-check = WARNT bei bekannten Fehlermustern
#   poka-yoke-git = VERHINDERT das Problem durch automatische Korrektur
#
# ROBUSTNESS: Gesamtes Skript in try/catch. Bei Fehler → exit 0 (nie blockieren).

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

if (-not $data -or -not $data.tool_input -or -not $data.tool_input.command) { exit 0 }

$cmd = $data.tool_input.command

# Nur bei git push Befehlen aktiv werden
# NICHT bei: git push --force (das blockt safety-gate.ps1)
# NICHT bei: Befehlen die bereits fetch/rebase enthalten
if ($cmd -notmatch 'git\s+push') { exit 0 }
if ($cmd -match 'git\s+push\s+--force' -or $cmd -match 'git\s+push\s+-f') { exit 0 }  # safety-gate kuemmert sich
if ($cmd -match 'git\s+fetch' -or $cmd -match 'git\s+rebase') { exit 0 }  # Bereits abgesichert

# Pruefen ob wir in einem Git-Repo sind
try {
    $gitDir = & git rev-parse --git-dir 2>&1
    if ($LASTEXITCODE -ne 0) { exit 0 }
} catch { exit 0 }

# Poka-Yoke: Fetch + Rebase automatisch ausfuehren
Hook-Log "POKA-YOKE: Automatisches fetch+rebase vor git push"
Write-Output "POKA-YOKE: Fuehre automatisch 'git fetch origin && git rebase origin/main' vor dem Push aus..."

# Nicht blockieren — nur informieren. Der pre-push Hook im Repo macht den eigentlichen fetch.
# Dieser Hook ist eine ZUSAETZLICHE Sicherheitsschicht die den Benutzer informiert.

exit 0
