# codex-git-multi-session-lock.ps1
# Codex PreToolUse:Bash lock for shared Git write operations.
# Uses the Claude-compatible lock file:
#   <git-common-dir>/claude-multi-session.lock

param(
    [string]$Command,
    [string]$Cwd,
    [string]$SessionId
)

$ErrorActionPreference = "Stop"

function Hook-Log {
    param([string]$Message)
}

function Hook-LogWarn {
    param([string]$Message)
}

function ConvertFrom-GitBashPath {
    param([string]$PathValue)
    if ($PathValue -match '^/([a-zA-Z])/(.*)$') {
        return "$($Matches[1].ToUpper()):\$($Matches[2] -replace '/', '\')"
    }
    return $PathValue
}

function Get-ParentProcessId {
    try {
        return (Get-CimInstance Win32_Process -Filter "ProcessId=$PID" -ErrorAction Stop).ParentProcessId
    } catch {
        return $null
    }
}

function Get-CodexSessionId {
    param($HookData, [string]$ExplicitSessionId)

    if ($ExplicitSessionId) {
        return $ExplicitSessionId
    }

    if ($HookData -and ($HookData.PSObject.Properties.Name -contains "session_id") -and $HookData.session_id) {
        return [string]$HookData.session_id
    }

    if ($env:CODEX_SESSION_ID) {
        return $env:CODEX_SESSION_ID
    }

    $parentPid = Get-ParentProcessId
    if ($parentPid) {
        return "ppid-$parentPid"
    }
    return "pid-$PID"
}

function New-LockJson {
    param(
        [string]$Session,
        [string]$CommandText,
        [string]$Repo
    )

    $shortCommand = $CommandText
    if ($shortCommand.Length -gt 200) {
        $shortCommand = $shortCommand.Substring(0, 200)
    }

    return ([ordered]@{
        sessionId = $Session
        acquired = [DateTimeOffset]::UtcNow.ToString("o", [Globalization.CultureInfo]::InvariantCulture)
        pid = $PID
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

try {
    $gitWritePattern = '\bgit\s+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag\s+\S|branch\s+(-[Dd]|--delete|--force|--move|-m\s+\S|-M\s+\S)|worktree\s+(add|remove|move|prune)|submodule\s+(update|add|deinit))\b'

    $hookInput = [Console]::In.ReadToEnd()
    $hookData = $null
    $cmd = $Command
    $cwdValue = $Cwd

    if (-not [string]::IsNullOrWhiteSpace($hookInput)) {
        if ($hookInput -notmatch $gitWritePattern) {
            exit 0
        }

        try {
            $hookData = $hookInput | ConvertFrom-Json -ErrorAction Stop
        } catch {
            exit 0
        }

        if ($hookData.tool_name -and $hookData.tool_name -ne "Bash") {
            exit 0
        }

        if ($hookData.tool_input -and ($hookData.tool_input.PSObject.Properties.Name -contains "command")) {
            $cmd = [string]$hookData.tool_input.command
        }

        if ($hookData.tool_input -and ($hookData.tool_input.PSObject.Properties.Name -contains "cwd") -and $hookData.tool_input.cwd) {
            $cwdValue = [string]$hookData.tool_input.cwd
        } elseif (($hookData.PSObject.Properties.Name -contains "cwd") -and $hookData.cwd) {
            $cwdValue = [string]$hookData.cwd
        }
    }

    if ([string]::IsNullOrWhiteSpace($cmd)) {
        exit 0
    }

    if ($cmd -notmatch $gitWritePattern) {
        exit 0
    }

    try { . "$PSScriptRoot\hook-log.ps1" } catch { }

    if ([string]::IsNullOrWhiteSpace($cwdValue)) {
        $cwdValue = (Get-Location).Path
    }
    $cwdValue = ConvertFrom-GitBashPath -PathValue $cwdValue

    if (-not [System.IO.Directory]::Exists($cwdValue)) {
        exit 0
    }

    $gitCommonDir = (& git -C $cwdValue rev-parse --git-common-dir 2>$null)
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($gitCommonDir)) {
        Hook-Log "codex-git-multi-session-lock: no git repo at $cwdValue"
        exit 0
    }

    $gitCommonDir = $gitCommonDir.Trim()
    if (-not [System.IO.Path]::IsPathRooted($gitCommonDir)) {
        $gitCommonDir = [System.IO.Path]::GetFullPath((Join-Path $cwdValue $gitCommonDir))
    }

    if (-not [System.IO.Directory]::Exists($gitCommonDir)) {
        exit 0
    }

    $lockFile = Join-Path $gitCommonDir "claude-multi-session.lock"
    $mySessionId = Get-CodexSessionId -HookData $hookData -ExplicitSessionId $SessionId
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
        $newJson = New-LockJson -Session $mySessionId -CommandText $cmd -Repo $gitCommonDir

        if (Write-LockCreateNew -Path $lockFile -Json $newJson -Encoding $utf8NoBom) {
            Hook-Log "codex-git-multi-session-lock: acquired for '$cmd'"
            exit 0
        }

        $lockData = $null
        $readFailed = $false
        try {
            $raw = [System.IO.File]::ReadAllText($lockFile, $utf8NoBom)
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
                Hook-LogWarn "codex-git-multi-session-lock: corrupt retry limit reached; fail-open"
                exit 0
            }
            try { [System.IO.File]::Delete($lockFile) } catch { Start-Sleep -Milliseconds 100 }
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
        $isOwnLock = ([string]$lockData.sessionId -eq $mySessionId)
        $pidAlive = if ($isOwnLock) { $true } else { Test-ProcessAlive -ProcessId $lockPid }
        $isStale = ($age -gt $staleThresholdSec) -or (-not $pidAlive)

        if ($isStale) {
            Hook-LogWarn "codex-git-multi-session-lock: stale lock from $($lockData.sessionId); delete and retry"
            try { [System.IO.File]::Delete($lockFile) } catch { Start-Sleep -Milliseconds 100 }
            $corruptRetries = 0
            Start-Sleep -Milliseconds 100
            continue
        }

        if ($isOwnLock) {
            if (Write-LockRefresh -Path $lockFile -Json $newJson -Encoding $utf8NoBom) {
                Hook-Log "codex-git-multi-session-lock: refreshed for '$cmd'"
            } else {
                Hook-LogWarn "codex-git-multi-session-lock: refresh failed; fail-open"
            }
            exit 0
        }

        if ($waited -eq 0) {
            [Console]::Error.WriteLine("codex-git-multi-session-lock: another session holds git lock (session $($lockData.sessionId), pid $lockPid, age $([int]$age)s). Waiting...")
            Hook-Log "codex-git-multi-session-lock: waiting for $($lockData.sessionId)"
        } elseif (($waited % 10) -eq 0) {
            [Console]::Error.WriteLine("codex-git-multi-session-lock: still waiting (${waited}s)...")
        }

        Start-Sleep -Seconds 2
        $waited += 2
        $corruptRetries = 0
    }

    Hook-LogWarn "codex-git-multi-session-lock: timeout after ${waited}s; fail-open"
    [Console]::Error.WriteLine("WARNING: codex-git-multi-session-lock timeout after ${waited}s; continuing without lock.")
    exit 0
} catch {
    try { Hook-LogWarn "codex-git-multi-session-lock: fail-open after error: $($_.Exception.Message)" } catch { }
    exit 0
}

exit 0
