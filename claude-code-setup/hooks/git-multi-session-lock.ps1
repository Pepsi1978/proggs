# git-multi-session-lock.ps1: Lock-Mechanismus fuer parallele Claude-Sessions am gleichen Git-Repo
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# Atomarer Lock (Direktive #3, Resilient Bugfixing, Loop 3):
#   Nutzt [System.IO.File]::Open mit FileMode.CreateNew. Das ist auf NTFS atomar
#   zwischen Prozessen — nur EINE Session kann gleichzeitig die Datei erstellen.
#   Plus: Cleanup-Pfad raeumt halb-geschriebene Dateien auf BEVOR sie andere
#   Sessions in Endlos-Loops fuehren koennen.
#
# Defense-in-Depth-Schichten:
#   1. Praevention: atomares CreateNew + temp-Write-then-rename Pattern
#   2. Reaktiv: Korrupte/leere Lock-Files werden bei Read-Failure SOFORT geloescht
#   3. Selbstheilend: Stale-Lock-Takeover nach TTL ODER wenn lockender Prozess tot
#   4. Worktree-aware: Lock liegt im --git-common-dir (geteilt zwischen Worktrees)
#
# Logik:
#   1. Pruefen ob Befehl eine git-Schreibebene ist
#   2. Git-Common-Dir via 'git rev-parse --git-common-dir' (worktree-safe)
#   3. Outer loop bis $maxWaitSec:
#      a. Versuche atomar CreateNew → falls geklappt: Lock gehoert uns
#      b. Falls Datei existiert: lies sie, pruefe stale/mine/pid-tot
#      c. korrupt/leer → loeschen, retry CreateNew
#      d. stale-by-time ODER stale-by-pid → loeschen, retry CreateNew
#      e. mine → ueberschreiben (Refresh)
#      f. fremd-aktiv → warte 2s
#
# Lock-Lifetime: 180 Sekunden TTL (langlebig genug fuer grosse Pushes).
# Platform: Windows (PowerShell 7+)

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
# tag\s+\S → nur 'git tag <name>' (Schreib), nicht 'git tag' (Listen-Read)
# branch\s+(-[Dd]|--delete|--force) → branch loeschen oder umbenennen
$gitWritePattern = '\bgit\s+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag\s+\S|branch\s+(-[Dd]|--delete|--force|--move|-m\s+\S|-M\s+\S)|worktree\s+(add|remove|move|prune)|submodule\s+(update|add|deinit))\b'
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

# Git-Common-Dir ermitteln (worktree-aware — Lock teilt sich zwischen allen Worktrees)
$gitCommonDir = $null
try {
    Push-Location $cwd -ErrorAction Stop
    $gitCommonDir = & git rev-parse --git-common-dir 2>$null
    $gitExitCode = $LASTEXITCODE
    Pop-Location
    if ($gitExitCode -ne 0 -or -not $gitCommonDir) {
        try { Hook-Log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" } catch { }
        exit 0
    }
    $gitCommonDir = $gitCommonDir.Trim()
    # Relative Pfade (z.B. ".git") absolut machen relativ zu cwd
    if (-not [System.IO.Path]::IsPathRooted($gitCommonDir)) {
        $gitCommonDir = [System.IO.Path]::GetFullPath((Join-Path $cwd $gitCommonDir))
    }
} catch {
    try { Pop-Location } catch { }
    exit 0
}

if (-not (Test-Path $gitCommonDir)) { exit 0 }

$lockfile = Join-Path $gitCommonDir "claude-multi-session.lock"

# Session-ID — primaer aus Claude-Code-Env. Fallback: stabile Parent-PID
# (Hook-PID variiert per Aufruf, Parent-PID = Claude-Code-Prozess = stabil pro Session)
$mySessionId = if ($env:CLAUDE_CODE_SESSION_ID) {
    $env:CLAUDE_CODE_SESSION_ID
} elseif ($env:CLAUDE_SESSION_ID) {
    $env:CLAUDE_SESSION_ID
} else {
    $parentPid = $null
    try {
        $parentPid = (Get-CimInstance Win32_Process -Filter "ProcessId=$PID" -ErrorAction Stop).ParentProcessId
    } catch { }
    if ($parentPid) { "ppid-$parentPid" } else { "pid-$PID" }
}

$maxWaitSec = 120
$staleThresholdSec = 180  # Loop 3: erhoeht von 90 auf 180 — grosse Pushes koennen >90s dauern
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$invariantCulture = [System.Globalization.CultureInfo]::InvariantCulture
$globalWaited = 0
$corruptRetryCount = 0
$maxCorruptRetries = 5  # Loop 1: harte Grenze gegen Endlos-Loop bei korruptem File

function New-LockJson {
    param($cmdShort, $repoRoot, $mySessionId)
    $lockObj = [ordered]@{
        sessionId = $mySessionId
        acquired = (Get-Date).ToUniversalTime().ToString("o", [System.Globalization.CultureInfo]::InvariantCulture)
        pid = $PID
        command = $cmdShort
        repo = $repoRoot
    }
    return ($lockObj | ConvertTo-Json -Compress)
}

function Write-LockAtomic {
    param($path, $jsonContent, $utf8NoBom)
    # FileMode.CreateNew = atomar fail-if-exists auf NTFS
    # Loop 1 Fix: bei Write-Failure NACH erfolgreichem Create raeumen wir auf,
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
        # Cleanup: wenn wir die Datei erfolgreich angelegt aber das Schreiben gescheitert ist
        if ($stream) {
            try { $stream.Dispose() } catch { }
            $stream = $null
        }
        if ($createdFile) {
            # Wir haben sie angelegt, also gehoert sie uns — sicher zu loeschen
            try { Remove-Item $path -Force -ErrorAction Stop } catch { }
        }
        return $false
    } finally {
        if ($stream) { try { $stream.Dispose() } catch { } }
    }
}

function Write-LockOverwrite {
    param($path, $jsonContent, $utf8NoBom)
    # Fuer Self-Refresh (eigener Lock): temp-write + atomic rename
    # Loop 1 Fix: WriteAllText war NICHT atomar — bei Crash mitten im Schreiben
    # blieb leere Datei zurueck. Jetzt: temp + Move (atomic rename auf NTFS).
    $tempPath = "$path.tmp.$PID.$(Get-Random)"
    try {
        [System.IO.File]::WriteAllText($tempPath, $jsonContent, $utf8NoBom)
        # File.Move mit overwrite=$true (NetCore 3+) — atomarer Replace
        [System.IO.File]::Move($tempPath, $path, $true)
        return $true
    } catch {
        try { if (Test-Path $tempPath) { Remove-Item $tempPath -Force -ErrorAction Stop } } catch { }
        return $false
    }
}

function Test-ProcessAlive {
    # Loop 3: Liveness-Check — wenn lockender Prozess tot ist, ist der Lock stale
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

$cmdShort = if ($cmd.Length -gt 200) { $cmd.Substring(0, 200) } else { $cmd }

# --- Atomare Lock-Aquise ---
while ($globalWaited -lt $maxWaitSec) {
    # Versuch 1: atomar CreateNew (klappt nur wenn keine Lock-Datei existiert)
    $newJson = New-LockJson -cmdShort $cmdShort -repoRoot $gitCommonDir -mySessionId $mySessionId
    if (Write-LockAtomic -path $lockfile -jsonContent $newJson -utf8NoBom $utf8NoBom) {
        try { Hook-Log "git-multi-session-lock: Lock atomar erworben fuer cmd '$cmdShort'" } catch { }
        exit 0
    }

    # Versuch 2: Datei existiert. Lies sie und entscheide.
    $lockData = $null
    $readFailed = $false
    try {
        $raw = Get-Content $lockfile -Raw -ErrorAction Stop
        if ([string]::IsNullOrWhiteSpace($raw)) {
            $readFailed = $true
        } else {
            $lockData = $raw | ConvertFrom-Json -ErrorAction Stop
        }
    } catch {
        $readFailed = $true
    }

    # Loop 1 Fix: Korrupte/leere Datei SOFORT loeschen, nicht in Loop laufen lassen
    if ($readFailed -or -not $lockData) {
        $corruptRetryCount++
        if ($corruptRetryCount -gt $maxCorruptRetries) {
            try { Hook-LogWarn "git-multi-session-lock: $maxCorruptRetries Korrupt-Retries — gebe auf, fahre ohne Lock fort" } catch { }
            exit 0
        }
        try { Hook-LogWarn "git-multi-session-lock: Korrupter/leerer Lock — loesche (Retry $corruptRetryCount/$maxCorruptRetries)" } catch { }
        try { Remove-Item $lockfile -Force -ErrorAction Stop } catch {
            # Andere Session war schneller — OK
            Start-Sleep -Milliseconds 100
        }
        continue
    }

    # Stale-Check mit InvariantCulture (kulturresistent)
    $age = 999.0
    try {
        $acquired = [System.DateTimeOffset]::Parse($lockData.acquired, $invariantCulture).UtcDateTime
        $age = ((Get-Date).ToUniversalTime() - $acquired).TotalSeconds
    } catch { $age = 999.0 }

    # Loop 3: PID-Liveness-Check — wenn lockender Prozess tot, ist Lock stale
    # egal wie alt. ABER: nur wenn Lock-PID auf DIESER Maschine ist (gleiche Hostname-Annahme).
    # Da Locks repo-lokal sind und Repos selten ueber Netzlaufwerke geteilt werden, OK.
    $lockPid = $null
    try { $lockPid = [int]$lockData.pid } catch { }
    $pidIsAlive = if ($lockPid) { Test-ProcessAlive -processId $lockPid } else { $true }  # unbekannt = vorsichtig "alive" annehmen

    $isStale = ($age -gt $staleThresholdSec) -or (-not $pidIsAlive -and $lockData.sessionId -ne $mySessionId)

    if ($isStale) {
        $reason = if ($age -gt $staleThresholdSec) { "${age}s alt" } else { "PID $lockPid tot" }
        try { Hook-LogWarn "git-multi-session-lock: Stale Lock ($reason) von $($lockData.sessionId) - delete + retry" } catch { }
        try { Remove-Item $lockfile -Force -ErrorAction Stop } catch {
            # Konnte stale-lock nicht loeschen (andere Session hat ihn vielleicht schon ersetzt)
            Start-Sleep -Milliseconds 100
        }
        $corruptRetryCount = 0  # Reset bei legitimer Stale-Detection
        continue  # zurueck zu CreateNew
    }

    # Eigener Lock: ueberschreiben (Refresh)
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
        [Console]::Error.WriteLine("git-multi-session-lock: Andere Claude-Session blockiert git ($info). Warte...")
        try { Hook-Log "git-multi-session-lock: Warte auf $info" } catch { }
    } elseif (($globalWaited % 10) -eq 0) {
        [Console]::Error.WriteLine("git-multi-session-lock: Warte weiter (${globalWaited}s)...")
    }

    Start-Sleep -Seconds 2
    $globalWaited += 2
    $corruptRetryCount = 0  # Reset wenn wir auf normalen fremden Lock warten
}

# Timeout erreicht — fail-open (besser ohne Lock als haengende Session)
try { Hook-LogWarn "git-multi-session-lock: Timeout nach ${globalWaited}s, fahre ohne Lock fort" } catch { }
[Console]::Error.WriteLine("WARNUNG: Multi-Session-Lock-Timeout nach ${globalWaited}s. Andere Session haengt vermutlich.")

exit 0
