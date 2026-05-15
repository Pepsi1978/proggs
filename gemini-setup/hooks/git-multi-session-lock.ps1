# git-multi-session-lock.ps1: Lock-Mechanismus fuer parallele Gemini-Sessions am gleichen Git-Repo
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# Atomarer Lock (Direktive #3, Resilient Bugfixing, Loop 3 + Perf-Loop 3):
#   Nutzt [System.IO.File]::Open mit FileMode.CreateNew. Das ist auf NTFS atomar
#   zwischen Prozessen — nur EINE Session kann gleichzeitig die Datei erstellen.
#   Plus: Cleanup-Pfad raeumt halb-geschriebene Dateien auf BEVOR sie andere
#   Sessions in Endlos-Loops fuehren koennen.
#
# Performance-Optimierungen (Perf-Loop 1+2+3):
#   - hook-log lazy-loaded NACH skip-checks (spart 2-3ms je skip)
#   - [System.IO.File]::ReadAllText statt Get-Content -Raw (10× schneller)
#   - [DateTime]::UtcNow statt Get-Date (>100× schneller)
#   - git -C $cwd statt Push/Pop-Location (spart 1-3ms + 1 Subprocess-Init)
#   - JSON-Skeleton einmal aussen gebaut, nur acquired-Timestamp in Loop ersetzt
#
# Defense-in-Depth-Schichten:
#   1. Praevention: atomares CreateNew + temp-Write-then-rename Pattern
#   2. Reaktiv: Korrupte/leere Lock-Files werden bei Read-Failure SOFORT geloescht
#   3. Selbstheilend: Stale-Lock-Takeover nach TTL ODER wenn lockender Prozess tot
#   4. Worktree-aware: Lock liegt im --git-common-dir (geteilt zwischen Worktrees)
#
# Lock-Lifetime: 180 Sekunden TTL (langlebig genug fuer grosse Pushes).
# Platform: Windows (PowerShell 7+)

$ErrorActionPreference = 'SilentlyContinue'

# Perf-Loop 1.3: Inline-Pattern statt Funktion (function-Definition kostet ~0.5ms je Aufruf).
# hook-log wird erst NACH skip-checks geladen (95% der Aufrufe sind skip).

$hookInput = [Console]::In.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($hookInput)) { exit 0 }

# Welche git-Subkommandos sind "Schreibebene"?
# tag\s+\S → nur 'git tag <name>' (Schreib), nicht 'git tag' (Listen-Read)
# branch\s+(-[Dd]|--delete|--force|--move|-m\s+\S|-M\s+\S) → loeschen/umbenennen/force
$gitWritePattern = '\bgit\s+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag\s+\S|branch\s+(-[Dd]|--delete|--force|--move|-m\s+\S|-M\s+\S)|worktree\s+(add|remove|move|prune)|submodule\s+(update|add|deinit))\b'

# Perf-Loop 2: Schneller regex-Check auf raw input BEVOR ConvertFrom-Json.
# 95% der Aufrufe sind non-git-Bash — dort sparen wir den ConvertFrom-Json
# (~3-10ms je skip). Plus zusaetzlich: wenn raw match auf input matched, machen
# wir noch den finalen exact-match auf dem geparsten command-Feld.
if ($hookInput -notmatch $gitWritePattern) { exit 0 }

# Now we know it likely contains a git-write command — full JSON parse
try {
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

if (-not $data -or -not $data.tool_input -or -not $data.tool_input.command) { exit 0 }

$cmd = $data.tool_input.command

# Final-Check auf dem exakten command-Feld (statt nur input-blob).
# Verhindert false-positives wenn 'git commit' in Argument/Pfad vorkommt
# aber nicht der eigentliche command ist.
if ($cmd -notmatch $gitWritePattern) { exit 0 }

# === Ab hier: write-path. hook-log laden (inline statt Funktion). ===
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

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

# Git-Common-Dir ermitteln (worktree-aware — Lock teilt sich zwischen allen Worktrees)
# Perf-Loop 1: git -C $cwd statt Push-Location/Pop-Location (spart 1-3ms)
$gitCommonDir = $null
try {
    $gitCommonDir = & git -C $cwd rev-parse --git-common-dir 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $gitCommonDir) {
        try { Hook-Log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" } catch { }
        exit 0
    }
    $gitCommonDir = $gitCommonDir.Trim()
    # Relative Pfade (z.B. ".git") absolut machen relativ zu cwd
    if (-not [System.IO.Path]::IsPathRooted($gitCommonDir)) {
        $gitCommonDir = [System.IO.Path]::GetFullPath((Join-Path $cwd $gitCommonDir))
    }
} catch {
    exit 0
}

# Perf-Loop 3: [Directory]::Exists statt Test-Path (kein cmdlet-Overhead).
if (-not [System.IO.Directory]::Exists($gitCommonDir)) { exit 0 }

# Perf-Loop 3: Direct string concat statt Join-Path (cmdlet → ~1ms saving).
$lockfile = "$gitCommonDir\Gemini-multi-session.lock"

# Session-ID — primaer aus gemini-setup-Env. Fallback: stabile Parent-PID
# (Hook-PID variiert per Aufruf, Parent-PID = gemini-setup-Prozess = stabil pro Session)
$mySessionId = if ($env:Gemini_CODE_SESSION_ID) {
    $env:Gemini_CODE_SESSION_ID
} elseif ($env:Gemini_SESSION_ID) {
    $env:Gemini_SESSION_ID
} else {
    $parentPid = $null
    try {
        $parentPid = (Get-CimInstance Win32_Process -Filter "ProcessId=$PID" -ErrorAction Stop).ParentProcessId
    } catch { }
    if ($parentPid) { "ppid-$parentPid" } else { "pid-$PID" }
}

$maxWaitSec = 120
$staleThresholdSec = 180  # erhoeht von 90 auf 180 — grosse Pushes koennen >90s dauern
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$invariantCulture = [System.Globalization.CultureInfo]::InvariantCulture
$globalWaited = 0
$corruptRetryCount = 0
$maxCorruptRetries = 5  # harte Grenze gegen Endlos-Loop bei korruptem File

# Perf-Loop 1: Statt JSON in jeder Iteration neu zu erzeugen (ConvertTo-Json hat
# Pipeline-Overhead), bauen wir einmal das "Skeleton" und ersetzen nur den Timestamp.
$cmdShort = if ($cmd.Length -gt 200) { $cmd.Substring(0, 200) } else { $cmd }
# Escape command fuer JSON-Einbettung
$cmdShortJson = $cmdShort.Replace('\', '\\').Replace('"', '\"').Replace("`r", '\r').Replace("`n", '\n').Replace("`t", '\t')
$mySessionIdJson = $mySessionId.Replace('\', '\\').Replace('"', '\"')
$gitCommonDirJson = $gitCommonDir.Replace('\', '\\').Replace('"', '\"')
# acquired wird per Iteration eingesetzt — ISO-8601 mit InvariantCulture
function New-LockJsonFast {
    $ts = [DateTime]::UtcNow.ToString("o", $invariantCulture)
    return "{`"sessionId`":`"$mySessionIdJson`",`"acquired`":`"$ts`",`"pid`":$PID,`"command`":`"$cmdShortJson`",`"repo`":`"$gitCommonDirJson`"}"
}

function Write-LockAtomic {
    param($path, $jsonContent, $utf8NoBom)
    # FileMode.CreateNew = atomar fail-if-exists auf NTFS
    # Cleanup: bei Write-Failure NACH erfolgreichem Create raeumen wir auf,
    # sonst bleibt eine leere Datei zurueck und fuettert den Korrupt-Loop
    $stream = $null
    $createdFile = $false
    try {
        $stream = [System.IO.File]::Open(
            $path,
            [System.IO.FileMode]::CreateNew,
            [System.IO.FileAccess]::Write,
            [System.IO.FileShare]::None
        )
        $createdFile = $true
        $bytes = $utf8NoBom.GetBytes($jsonContent)
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Flush()
        $stream.Dispose()
        $stream = $null
        return $true
    } catch {
        if ($stream) {
            try { $stream.Dispose() } catch { }
            $stream = $null
        }
        if ($createdFile) {
            try { Remove-Item $path -Force -ErrorAction Stop } catch { }
        }
        return $false
    } finally {
        if ($stream) { try { $stream.Dispose() } catch { } }
    }
}

function Write-LockOverwrite {
    param($path, $jsonContent, $utf8NoBom)
    # Self-Refresh: temp + atomic File.Move (atomar auf NTFS via ReplaceFile/MoveFileEx).
    # Debug-Loop 2 Fix: WriteAllText direkt hatte FileShare.Read — concurrent reader
    # einer anderen Session konnte partial content sehen, als korrupt klassifizieren
    # und unseren Lock LOESCHEN. Atomic-rename hat dieses Fenster nicht (target wird
    # erst nach komplettem temp-write sichtbar).
    # Direktive 3 Schicht 1 (Praevention) MUSS bleiben — Schicht 2 (Korrupt-Cleanup)
    # allein ist nicht genug.
    $tempPath = "$path.tmp.$PID.$(Get-Random)"
    try {
        [System.IO.File]::WriteAllText($tempPath, $jsonContent, $utf8NoBom)
        [System.IO.File]::Move($tempPath, $path, $true)
        return $true
    } catch {
        try { if ([System.IO.File]::Exists($tempPath)) { [System.IO.File]::Delete($tempPath) } } catch { }
        return $false
    }
}

function Test-ProcessAlive {
    # Liveness-Check — wenn lockender Prozess tot ist, ist der Lock stale
    # unabhaengig vom Timestamp. Verhindert dass abgestuerzte Sessions andere blockieren.
    param([int]$processId)
    if (-not $processId -or $processId -le 0) { return $false }
    try {
        $null = Get-Process -Id $processId -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

# --- Atomare Lock-Aquise ---
while ($globalWaited -lt $maxWaitSec) {
    # Versuch 1: atomar CreateNew (klappt nur wenn keine Lock-Datei existiert)
    $newJson = New-LockJsonFast
    if (Write-LockAtomic -path $lockfile -jsonContent $newJson -utf8NoBom $utf8NoBom) {
        try { Hook-Log "git-multi-session-lock: Lock atomar erworben fuer cmd '$cmdShort'" } catch { }
        exit 0
    }

    # Versuch 2: Datei existiert. Lies sie und entscheide.
    # Perf-Loop 1: [System.IO.File]::ReadAllText statt Get-Content -Raw (10× schneller)
    $lockData = $null
    $readFailed = $false
    try {
        $raw = [System.IO.File]::ReadAllText($lockfile, $utf8NoBom)
        if ([string]::IsNullOrWhiteSpace($raw)) {
            $readFailed = $true
        } else {
            $lockData = $raw | ConvertFrom-Json -ErrorAction Stop
        }
    } catch {
        $readFailed = $true
    }

    # Korrupte/leere Datei SOFORT loeschen, nicht in Loop laufen lassen
    if ($readFailed -or -not $lockData) {
        $corruptRetryCount++
        if ($corruptRetryCount -gt $maxCorruptRetries) {
            try { Hook-LogWarn "git-multi-session-lock: $maxCorruptRetries Korrupt-Retries — gebe auf, fahre ohne Lock fort" } catch { }
            exit 0
        }
        try { Hook-LogWarn "git-multi-session-lock: Korrupter/leerer Lock — loesche (Retry $corruptRetryCount/$maxCorruptRetries)" } catch { }
        try { Remove-Item $lockfile -Force -ErrorAction Stop } catch {
            Start-Sleep -Milliseconds 100
        }
        continue
    }

    # Stale-Check mit InvariantCulture (kulturresistent)
    # Perf-Loop 1: [DateTime]::UtcNow statt Get-Date (>100× schneller)
    $age = 999.0
    try {
        $acquired = [System.DateTimeOffset]::Parse($lockData.acquired, $invariantCulture).UtcDateTime
        $age = ([DateTime]::UtcNow - $acquired).TotalSeconds
    } catch { $age = 999.0 }

    # PID-Liveness-Check — wenn lockender Prozess tot, ist Lock stale egal wie alt.
    # Nur fuer FREMDE Sessions — die eigene Session ueberlebt sich selbst per Definition.
    $lockPid = $null
    try { $lockPid = [int]$lockData.pid } catch { }
    $pidIsAlive = if ($lockPid) { Test-ProcessAlive -processId $lockPid } else { $true }

    $isStale = ($age -gt $staleThresholdSec) -or (-not $pidIsAlive -and $lockData.sessionId -ne $mySessionId)

    if ($isStale) {
        $reason = if ($age -gt $staleThresholdSec) { "${age}s alt" } else { "PID $lockPid tot" }
        try { Hook-LogWarn "git-multi-session-lock: Stale Lock ($reason) von $($lockData.sessionId) - delete + retry" } catch { }
        try { Remove-Item $lockfile -Force -ErrorAction Stop } catch {
            Start-Sleep -Milliseconds 100
        }
        $corruptRetryCount = 0
        continue
    }

    # Eigener Lock: refresh
    if ($lockData.sessionId -eq $mySessionId) {
        if (Write-LockOverwrite -path $lockfile -jsonContent $newJson -utf8NoBom $utf8NoBom) {
            try { Hook-Log "git-multi-session-lock: Lock refreshed fuer cmd '$cmdShort'" } catch { }
        } else {
            try { Hook-LogWarn "git-multi-session-lock: Lock-Refresh fehlgeschlagen, fahre fort" } catch { }
        }
        exit 0
    }

    # Fremder aktiver Lock: warten
    if ($globalWaited -eq 0) {
        $info = "Session $($lockData.sessionId) (PID $($lockData.pid), seit $([int]$age)s)"
        [Console]::Error.WriteLine("git-multi-session-lock: Andere Gemini-Session blockiert git ($info). Warte...")
        try { Hook-Log "git-multi-session-lock: Warte auf $info" } catch { }
    } elseif (($globalWaited % 10) -eq 0) {
        [Console]::Error.WriteLine("git-multi-session-lock: Warte weiter (${globalWaited}s)...")
    }

    Start-Sleep -Seconds 2
    $globalWaited += 2
    $corruptRetryCount = 0
}

# Timeout erreicht — fail-open (besser ohne Lock als haengende Session)
try { Hook-LogWarn "git-multi-session-lock: Timeout nach ${globalWaited}s, fahre ohne Lock fort" } catch { }
[Console]::Error.WriteLine("WARNUNG: Multi-Session-Lock-Timeout nach ${globalWaited}s. Andere Session haengt vermutlich.")

exit 0

