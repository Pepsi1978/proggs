# codex-git-wrapper.ps1
# Holds the shared Claude-compatible Git lock for the lifetime of the real git process.

$ErrorActionPreference = "Stop"
$GitArgs = [string[]]@($args)

function ConvertFrom-GitBashPath {
    param([string]$PathValue)
    if ($PathValue -match '^/([a-zA-Z])/(.*)$') {
        return "$($Matches[1].ToUpper()):\$($Matches[2] -replace '/', '\')"
    }
    return $PathValue
}

function Resolve-RealGit {
    if ($env:CODEX_GIT_LOCK_REAL_GIT -and [System.IO.File]::Exists($env:CODEX_GIT_LOCK_REAL_GIT)) {
        return $env:CODEX_GIT_LOCK_REAL_GIT
    }

    $commonCandidates = @(
        "C:\Program Files\Git\cmd\git.exe",
        "C:\Program Files\Git\bin\git.exe"
    )
    foreach ($candidate in $commonCandidates) {
        if ([System.IO.File]::Exists($candidate)) {
            return $candidate
        }
    }

    try {
        $cmdInfo = Get-Command git.exe -ErrorAction Stop
        if ($cmdInfo.Source) {
            return $cmdInfo.Source
        }
    } catch { }

    return "git.exe"
}

function Get-StableSessionId {
    if ($env:CODEX_GIT_LOCK_SESSION_ID) {
        return $env:CODEX_GIT_LOCK_SESSION_ID
    }
    if ($env:CODEX_SESSION_ID) {
        return $env:CODEX_SESSION_ID
    }
    return "pid-$PID"
}

function Join-DisplayArgs {
    param([string[]]$ArgumentList)
    $parts = foreach ($arg in $ArgumentList) {
        if ($arg -match '[\s"]') {
            '"' + ($arg -replace '"', '\"') + '"'
        } else {
            $arg
        }
    }
    return ($parts -join " ")
}

function Get-GitInvocationInfo {
    param([string[]]$ArgumentList)

    $cwdValue = (Get-Location).Path
    $subcommand = ""
    $subcommandIndex = -1
    $i = 0

    while ($i -lt $ArgumentList.Count) {
        $arg = $ArgumentList[$i]
        if ($arg -eq "-C") {
            $i++
            if ($i -lt $ArgumentList.Count -and -not [string]::IsNullOrWhiteSpace($ArgumentList[$i])) {
                $cwdValue = ConvertFrom-GitBashPath -PathValue $ArgumentList[$i]
            }
            $i++
            continue
        }
        if ($arg -in @("--git-dir", "--work-tree", "--namespace", "-c", "--config-env")) {
            $i += 2
            continue
        }
        if ($arg -match '^--(git-dir|work-tree|namespace)=') {
            $i++
            continue
        }
        if ($arg -in @("--no-pager", "--paginate", "--bare", "--version", "--help")) {
            $i++
            continue
        }
        if ($arg -like "-*") {
            $i++
            continue
        }

        $subcommand = $arg
        $subcommandIndex = $i
        break
    }

    $remaining = @()
    if ($subcommandIndex -ge 0 -and $subcommandIndex + 1 -lt $ArgumentList.Count) {
        $remaining = $ArgumentList[($subcommandIndex + 1)..($ArgumentList.Count - 1)]
    }

    [pscustomobject]@{
        Cwd = [System.IO.Path]::GetFullPath($cwdValue)
        Subcommand = $subcommand
        Remaining = [string[]]$remaining
    }
}

function Test-GitWriteOperation {
    param(
        [string]$Subcommand,
        [string[]]$Remaining
    )

    switch ($Subcommand) {
        { $_ -in @("add", "commit", "push", "fetch", "rebase", "pull", "reset", "restore", "checkout", "switch", "rm", "mv", "clean", "gc", "maintenance", "stash", "am", "cherry-pick", "revert", "merge") } {
            return $true
        }
        "tag" {
            return ($Remaining.Count -gt 0)
        }
        "branch" {
            if ($Remaining.Count -eq 0) { return $false }
            $first = $Remaining[0]
            if ($first -in @("-D", "-d", "--delete", "--force", "--move")) { return $true }
            if ($first -in @("-m", "-M") -and $Remaining.Count -gt 1) { return $true }
            return $false
        }
        "remote" {
            return ($Remaining.Count -gt 0 -and $Remaining[0] -in @("add", "remove", "rm", "rename", "set-url", "prune", "update"))
        }
        "worktree" {
            return ($Remaining.Count -gt 0 -and $Remaining[0] -in @("add", "remove", "move", "prune"))
        }
        "submodule" {
            return ($Remaining.Count -gt 0 -and $Remaining[0] -in @("update", "add", "deinit"))
        }
        default {
            return $false
        }
    }
}

function Get-GitCommonDir {
    param(
        [string]$RealGit,
        [string]$Cwd
    )

    $gitCommonDir = (& $RealGit -C $Cwd rev-parse --git-common-dir 2>$null)
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($gitCommonDir)) {
        return $null
    }

    $gitCommonDir = $gitCommonDir.Trim()
    if (-not [System.IO.Path]::IsPathRooted($gitCommonDir)) {
        $gitCommonDir = [System.IO.Path]::GetFullPath((Join-Path $Cwd $gitCommonDir))
    }

    if (-not [System.IO.Directory]::Exists($gitCommonDir)) {
        return $null
    }

    return $gitCommonDir
}

function New-LockJson {
    param(
        [string]$Session,
        [string]$CommandText,
        [string]$Repo,
        [int]$OwnerPid
    )

    $shortCommand = $CommandText
    if ($shortCommand.Length -gt 200) {
        $shortCommand = $shortCommand.Substring(0, 200)
    }

    return ([ordered]@{
        sessionId = $Session
        acquired = [DateTimeOffset]::UtcNow.ToString("o", [Globalization.CultureInfo]::InvariantCulture)
        pid = $OwnerPid
        command = $shortCommand
        repo = $Repo
    } | ConvertTo-Json -Compress)
}

function Write-LockCreateNew {
    param(
        [string]$Path,
        [string]$Json,
        [System.Text.Encoding]$Encoding
    )

    $stream = $null
    $created = $false
    try {
        $stream = [System.IO.File]::Open(
            $Path,
            [System.IO.FileMode]::CreateNew,
            [System.IO.FileAccess]::Write,
            [System.IO.FileShare]::None
        )
        $created = $true
        $bytes = $Encoding.GetBytes($Json)
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Flush()
        return $true
    } catch {
        if ($created) {
            try { [System.IO.File]::Delete($Path) } catch { }
        }
        return $false
    } finally {
        if ($stream) {
            try { $stream.Dispose() } catch { }
        }
    }
}

function Write-LockRefresh {
    param(
        [string]$Path,
        [string]$Json,
        [System.Text.Encoding]$Encoding
    )

    $tempPath = "$Path.tmp.$PID.$([Guid]::NewGuid().ToString('N'))"
    try {
        [System.IO.File]::WriteAllText($tempPath, $Json, $Encoding)
        [System.IO.File]::Move($tempPath, $Path, $true)
        return $true
    } catch {
        try {
            if ([System.IO.File]::Exists($tempPath)) {
                [System.IO.File]::Delete($tempPath)
            }
        } catch { }
        return $false
    }
}

function Test-ProcessAlive {
    param([int]$ProcessId)

    if ($ProcessId -le 0) {
        return $false
    }

    try {
        $null = Get-Process -Id $ProcessId -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Acquire-GitLock {
    param(
        [string]$GitCommonDir,
        [string]$SessionId,
        [string]$CommandText,
        [int]$OwnerPid
    )

    $lockPath = Join-Path $GitCommonDir "claude-multi-session.lock"
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    $culture = [Globalization.CultureInfo]::InvariantCulture
    $maxWaitSec = 120
    if ($env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS -match '^\d+$') {
        $maxWaitSec = [int]$env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS
    }
    $staleThresholdSec = 180
    $maxCorruptRetries = 5
    $corruptRetries = 0
    $waited = 0

    while ($waited -lt $maxWaitSec) {
        $newJson = New-LockJson -Session $SessionId -CommandText $CommandText -Repo $GitCommonDir -OwnerPid $OwnerPid

        if (Write-LockCreateNew -Path $lockPath -Json $newJson -Encoding $utf8NoBom) {
            return $lockPath
        }

        $lockData = $null
        $readFailed = $false
        try {
            $raw = [System.IO.File]::ReadAllText($lockPath, $utf8NoBom)
            if ([string]::IsNullOrWhiteSpace($raw)) {
                $readFailed = $true
            } else {
                $lockData = $raw | ConvertFrom-Json -ErrorAction Stop
            }
        } catch {
            $readFailed = $true
        }

        if ($readFailed -or -not $lockData -or -not $lockData.sessionId) {
            $corruptRetries++
            if ($corruptRetries -gt $maxCorruptRetries) {
                return $lockPath
            }
            try { [System.IO.File]::Delete($lockPath) } catch { Start-Sleep -Milliseconds 100 }
            Start-Sleep -Milliseconds 100
            continue
        }

        $age = 999.0
        try {
            $acquired = [DateTimeOffset]::Parse([string]$lockData.acquired, $culture).UtcDateTime
            $age = ([DateTime]::UtcNow - $acquired).TotalSeconds
        } catch {
            $age = 999.0
        }

        $lockPid = 0
        try { $lockPid = [int]$lockData.pid } catch { $lockPid = 0 }
        $isOwnLock = ([string]$lockData.sessionId -eq $SessionId)
        $pidAlive = if ($isOwnLock) { $true } else { Test-ProcessAlive -ProcessId $lockPid }
        $isStale = ($age -gt $staleThresholdSec) -or (-not $pidAlive)

        if ($isStale) {
            try { [System.IO.File]::Delete($lockPath) } catch { Start-Sleep -Milliseconds 100 }
            $corruptRetries = 0
            Start-Sleep -Milliseconds 100
            continue
        }

        if ($isOwnLock) {
            $null = Write-LockRefresh -Path $lockPath -Json $newJson -Encoding $utf8NoBom
            return $lockPath
        }

        if ($waited -eq 0) {
            [Console]::Error.WriteLine("codex-git-wrapper: another session holds git lock (session $($lockData.sessionId), pid $lockPid, age $([int]$age)s). Waiting...")
        } elseif (($waited % 10) -eq 0) {
            [Console]::Error.WriteLine("codex-git-wrapper: still waiting (${waited}s)...")
        }

        Start-Sleep -Seconds 2
        $waited += 2
        $corruptRetries = 0
    }

    [Console]::Error.WriteLine("WARNING: codex-git-wrapper timeout after ${waited}s; continuing without lock.")
    return $lockPath
}

function Release-OwnLock {
    param(
        [string]$LockPath,
        [string]$SessionId,
        [int]$OwnerPid
    )

    if ([string]::IsNullOrWhiteSpace($LockPath) -or -not [System.IO.File]::Exists($LockPath)) {
        return
    }

    try {
        $raw = [System.IO.File]::ReadAllText($LockPath)
        if ([string]::IsNullOrWhiteSpace($raw)) { return }
        $sessionNeedle = '"sessionId":"' + $SessionId.Replace('\', '\\').Replace('"', '\"') + '"'
        $pidNeedle = '"pid":' + [string]$OwnerPid
        if ($raw.Contains($sessionNeedle) -and $raw.Contains($pidNeedle)) {
            [System.IO.File]::Delete($LockPath)
        }
    } catch { }
}

try {
    if ($GitArgs.Count -gt 0 -and $GitArgs[0] -eq "--") {
        $GitArgs = if ($GitArgs.Count -gt 1) { $GitArgs[1..($GitArgs.Count - 1)] } else { @() }
    }

    $realGit = Resolve-RealGit
    if ($env:CODEX_GIT_LOCK_BYPASS -eq "1") {
        & $realGit @GitArgs
        exit $LASTEXITCODE
    }

    $info = Get-GitInvocationInfo -ArgumentList $GitArgs
    $isWrite = Test-GitWriteOperation -Subcommand $info.Subcommand -Remaining $info.Remaining

    if (-not $isWrite) {
        & $realGit @GitArgs
        exit $LASTEXITCODE
    }

    $gitCommonDir = Get-GitCommonDir -RealGit $realGit -Cwd $info.Cwd
    if ([string]::IsNullOrWhiteSpace($gitCommonDir)) {
        & $realGit @GitArgs
        exit $LASTEXITCODE
    }
    $lockPath = Join-Path $gitCommonDir "claude-multi-session.lock"

    $sessionId = Get-StableSessionId
    $normalizedCommand = "git $($info.Subcommand)"
    if ($info.Remaining.Count -gt 0) {
        $normalizedCommand = "$normalizedCommand $(Join-DisplayArgs -ArgumentList $info.Remaining)"
    }

    $oldOwnerPid = $env:CODEX_GIT_LOCK_OWNER_PID
    $oldRealGit = $env:CODEX_GIT_LOCK_REAL_GIT
    $oldBypass = $env:CODEX_GIT_LOCK_BYPASS
    $env:CODEX_GIT_LOCK_OWNER_PID = [string]$PID
    $env:CODEX_GIT_LOCK_REAL_GIT = $realGit

    try {
        $lockPath = Acquire-GitLock -GitCommonDir $gitCommonDir -SessionId $sessionId -CommandText $normalizedCommand -OwnerPid $PID
    } catch {
        [Console]::Error.WriteLine("codex-git-wrapper: lock acquire failed-open: $($_.Exception.Message)")
    }

    try {
        $env:CODEX_GIT_LOCK_BYPASS = "1"
        & $realGit @GitArgs
        $gitExitCode = $LASTEXITCODE
    } finally {
        if ($null -eq $oldBypass) {
            Remove-Item Env:\CODEX_GIT_LOCK_BYPASS -ErrorAction SilentlyContinue
        } else {
            $env:CODEX_GIT_LOCK_BYPASS = $oldBypass
        }
        Release-OwnLock -LockPath $lockPath -SessionId $sessionId -OwnerPid $PID
        if ($null -eq $oldOwnerPid) {
            Remove-Item Env:\CODEX_GIT_LOCK_OWNER_PID -ErrorAction SilentlyContinue
        } else {
            $env:CODEX_GIT_LOCK_OWNER_PID = $oldOwnerPid
        }
        if ($null -eq $oldRealGit) {
            Remove-Item Env:\CODEX_GIT_LOCK_REAL_GIT -ErrorAction SilentlyContinue
        } else {
            $env:CODEX_GIT_LOCK_REAL_GIT = $oldRealGit
        }
    }

    exit $gitExitCode
} catch {
    [Console]::Error.WriteLine("codex-git-wrapper: unexpected failure; running real git fail-open: $($_.Exception.Message)")
    try {
        $realGit = Resolve-RealGit
        $env:CODEX_GIT_LOCK_BYPASS = "1"
        & $realGit @GitArgs
        exit $LASTEXITCODE
    } catch {
        exit 1
    }
}
