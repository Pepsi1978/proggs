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

function Get-ParentProcessId {
    try {
        return (Get-CimInstance Win32_Process -Filter "ProcessId=$PID" -ErrorAction Stop).ParentProcessId
    } catch {
        return $null
    }
}

function Get-StableSessionId {
    if ($env:CODEX_GIT_LOCK_SESSION_ID) {
        return $env:CODEX_GIT_LOCK_SESSION_ID
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
        { $_ -in @("add", "commit", "push", "fetch", "rebase", "pull", "reset", "restore", "stash", "am", "cherry-pick", "revert", "merge") } {
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

function Get-LockPath {
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

    return (Join-Path $gitCommonDir "claude-multi-session.lock")
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
        $data = $raw | ConvertFrom-Json -ErrorAction Stop
        if ($data.sessionId -eq $SessionId -and [int]$data.pid -eq $OwnerPid) {
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

    $helperPath = Join-Path $PSScriptRoot "codex-git-multi-session-lock.ps1"
    if (-not [System.IO.File]::Exists($helperPath)) {
        & $realGit @GitArgs
        exit $LASTEXITCODE
    }

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
        & pwsh -NoProfile -ExecutionPolicy Bypass -File $helperPath -Command $normalizedCommand -Cwd $info.Cwd -SessionId $sessionId -OwnerPid $PID | Out-Null
    } catch {
        [Console]::Error.WriteLine("codex-git-wrapper: lock helper failed-open: $($_.Exception.Message)")
    }

    $lockPath = Get-LockPath -RealGit $realGit -Cwd $info.Cwd
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
