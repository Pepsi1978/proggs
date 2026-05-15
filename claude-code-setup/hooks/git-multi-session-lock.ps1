# git-multi-session-lock.ps1: Lock-Mechanismus fuer parallele Claude-Sessions am gleichen Git-Repo
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# UNTERSCHIED zu poka-yoke-git-push.ps1:
#   poka-yoke-git-push = informiert ueber fetch+rebase vor push
#   git-multi-session-lock = sequentialisiert git-Schreiboperationen aller Sessions
#
# Logik:
#   1. Pruefen ob Befehl eine git-Schreibebene ist (add/commit/push/reset/...)
#   2. Repo-Root via 'git rev-parse --show-toplevel' ermitteln
#   3. Lock-Datei: <repo>/.git/claude-multi-session.lock
#   4. Falls Lock von anderer Session: warten (max 120s, stale-takeover nach 90s)
#   5. Falls Lock von uns selbst: weiter (gleiche Sequenz)
#   6. Lock mit eigener SessionID schreiben, exit 0
#
# Lock-Lifetime: 90 Sekunden TTL. Bei jedem git-Befehl wird der Lock erneuert.
# Falls Frank's Session 90s nichts mit git macht, ist der Lock automatisch frei.
#
# Platform: Windows (PowerShell 7+)
# stdout -> AI context (system-reminder), stderr -> user terminal

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($hookInput)) { exit 0 }
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

if (-not $data -or -not $data.tool_input -or -not $data.tool_input.command) { exit 0 }

$cmd = $data.tool_input.command

# Welche git-Subkommandos sind "Schreibebene"?
$gitWritePattern = '\bgit\s+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag|branch\s+-[Dd])\b'
if ($cmd -notmatch $gitWritePattern) { exit 0 }

# Working Directory ermitteln (Tool-Input oder PWD)
$cwd = $null
if ($data.tool_input.PSObject.Properties.Name -contains 'cwd' -and $data.tool_input.cwd) {
    $cwd = $data.tool_input.cwd
} else {
    $cwd = (Get-Location).Path
}

# Git-Bash-Pfade in Windows-Pfade konvertieren (/c/... -> C:\...)
if ($cwd -match '^/([a-zA-Z])/(.*)$') {
    $cwd = "$($Matches[1].ToUpper()):\$($Matches[2] -replace '/', '\')"
}

# Repo-Root ermitteln
$repoRoot = $null
try {
    Push-Location $cwd -ErrorAction Stop
    $repoRoot = & git rev-parse --show-toplevel 2>$null
    Pop-Location
    if ($LASTEXITCODE -ne 0 -or -not $repoRoot) {
        try { Hook-Log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" } catch { }
        exit 0
    }
    $repoRoot = $repoRoot.Trim()
} catch {
    try { Pop-Location } catch { }
    exit 0
}

if (-not (Test-Path $repoRoot)) { exit 0 }

$lockfile = Join-Path $repoRoot ".git/claude-multi-session.lock"
$mySessionId = if ($env:CLAUDE_CODE_SESSION_ID) {
    $env:CLAUDE_CODE_SESSION_ID
} else {
    "$PID-$(Get-Date -Format 'yyyyMMddHHmmss')"
}

$maxWaitSec = 120
$staleThresholdSec = 90
$waited = 0
$lockOwnerInfo = ""

while (Test-Path $lockfile) {
    $lockData = $null
    try {
        $raw = Get-Content $lockfile -Raw -ErrorAction Stop
        $lockData = $raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        # Korrupte Lock-Datei, einfach ueberschreiben
        break
    }

    # Stale-Check
    try {
        $acquired = [DateTime]::Parse($lockData.acquired)
        $age = ((Get-Date) - $acquired).TotalSeconds
    } catch {
        $age = 999
    }
    if ($age -gt $staleThresholdSec) {
        try { Hook-LogWarn "git-multi-session-lock: Stale Lock (${age}s alt) von $($lockData.sessionId) wird ueberschrieben" } catch { }
        break
    }

    # Eigener Lock: einfach erneuern
    if ($lockData.sessionId -eq $mySessionId) { break }

    # Anderer Lock: warten
    if ($waited -eq 0) {
        $lockOwnerInfo = "Session $($lockData.sessionId) (PID $($lockData.pid), seit ${age}s)"
        [Console]::Error.WriteLine("git-multi-session-lock: Andere Claude-Session blockiert git gerade ($lockOwnerInfo). Warte...")
        try { Hook-Log "git-multi-session-lock: Warte auf $lockOwnerInfo" } catch { }
    }

    Start-Sleep -Seconds 2
    $waited += 2

    if ($waited -ge $maxWaitSec) {
        try { Hook-LogWarn "git-multi-session-lock: Timeout nach ${maxWaitSec}s, Lock von $($lockData.sessionId) ignoriert" } catch { }
        [Console]::Error.WriteLine("WARNUNG: Multi-Session-Lock-Timeout nach ${maxWaitSec}s. Andere Session haengt vermutlich. Fahre fort.")
        break
    }

    if (($waited % 10) -eq 0) {
        [Console]::Error.WriteLine("git-multi-session-lock: Warte weiter auf andere Session (${waited}s)...")
    }
}

# Lock schreiben (oder erneuern)
try {
    $cmdShort = if ($cmd.Length -gt 200) { $cmd.Substring(0, 200) } else { $cmd }
    $lockObj = [ordered]@{
        sessionId = $mySessionId
        acquired = (Get-Date).ToString("o")
        pid = $PID
        command = $cmdShort
        repo = $repoRoot
    }
    $lockJson = $lockObj | ConvertTo-Json -Compress
    Set-Content -Path $lockfile -Value $lockJson -Encoding UTF8 -Force -ErrorAction Stop
    try { Hook-Log "git-multi-session-lock: Lock gesetzt fuer cmd '$cmdShort'" } catch { }
} catch {
    try { Hook-LogWarn "git-multi-session-lock: Konnte Lock nicht schreiben: $_" } catch { }
}

exit 0
