param(
    [string]$HelperPath = (Join-Path $PSScriptRoot "..\hooks\codex-git-multi-session-lock.ps1")
)

$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function New-TestRepo {
    $root = Join-Path ([System.IO.Path]::GetTempPath()) ("codex-lock-test-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $root | Out-Null
    & git -C $root init | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "git init failed for $root"
    }
    Set-Content -Path (Join-Path $root "foo.txt") -Value "hello" -Encoding UTF8
    return $root
}

function Get-LockPath {
    param([string]$Repo)
    $common = (& git -C $Repo rev-parse --git-common-dir).Trim()
    if (-not [System.IO.Path]::IsPathRooted($common)) {
        $common = [System.IO.Path]::GetFullPath((Join-Path $Repo $common))
    }
    return (Join-Path $common "claude-multi-session.lock")
}

function Invoke-LockHelper {
    param(
        [string]$Repo,
        [string]$Command,
        [string]$SessionId = "codex-test-session"
    )
    $payload = [ordered]@{
        session_id = $SessionId
        cwd = $Repo
        hook_event_name = "PreToolUse"
        tool_name = "Bash"
        tool_input = [ordered]@{
            command = $Command
            cwd = $Repo
        }
    } | ConvertTo-Json -Depth 6 -Compress

    $output = $payload | & pwsh -NoProfile -ExecutionPolicy Bypass -File $HelperPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "helper exited with $LASTEXITCODE for command '$Command': $output"
    }
    return $output
}

function Read-LockJson {
    param([string]$LockPath)
    $raw = [System.IO.File]::ReadAllText($LockPath)
    return $raw | ConvertFrom-Json -ErrorAction Stop
}

$repo = New-TestRepo
$lock = Get-LockPath -Repo $repo

try {
    Assert-True (Test-Path $HelperPath) "helper missing: $HelperPath"

    Invoke-LockHelper -Repo $repo -Command "echo hello" -SessionId "codex-no-lock" | Out-Null
    Assert-True (-not (Test-Path $lock)) "read-only shell command created a lock"

    Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-functional" | Out-Null
    Assert-True (Test-Path $lock) "git add did not create lock"
    $first = Read-LockJson -LockPath $lock
    Assert-True ($first.sessionId -eq "codex-functional") "unexpected sessionId after acquire"
    Assert-True ($first.command -eq "git add foo.txt") "unexpected command in lock"
    Assert-True ($first.repo -and (Test-Path $first.repo)) "repo field missing or invalid"

    Start-Sleep -Milliseconds 50
    Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-functional" | Out-Null
    $second = Read-LockJson -LockPath $lock
    Assert-True ($second.sessionId -eq "codex-functional") "self-refresh changed sessionId"
    Assert-True ($second.acquired -ne $first.acquired) "self-refresh did not update acquired timestamp"

    Remove-Item -LiteralPath $lock -Force
    $jobs = for ($i = 0; $i -lt 10; $i++) {
        $payload = [ordered]@{
            session_id = "codex-stress"
            cwd = $repo
            hook_event_name = "PreToolUse"
            tool_name = "Bash"
            tool_input = [ordered]@{
                command = "git add foo.txt"
                cwd = $repo
            }
        } | ConvertTo-Json -Depth 6 -Compress
        Start-Job -ScriptBlock {
            param($helper, $stdin)
            $stdin | pwsh -NoProfile -ExecutionPolicy Bypass -File $helper
            return $LASTEXITCODE
        } -ArgumentList $HelperPath, $payload
    }
    $jobResults = $jobs | Wait-Job | Receive-Job
    $jobs | Remove-Job
    Assert-True (($jobResults | Where-Object { $_ -ne 0 }).Count -eq 0) "one or more stress workers failed"
    $stress = Read-LockJson -LockPath $lock
    Assert-True ($stress.sessionId -eq "codex-stress") "stress lock has wrong sessionId"

    $deadPid = 999999
    $deadLock = [ordered]@{
        sessionId = "claude-dead"
        acquired = [DateTimeOffset]::UtcNow.ToString("o")
        pid = $deadPid
        command = "git push"
        repo = (Split-Path $lock -Parent)
    } | ConvertTo-Json -Compress
    Set-Content -Path $lock -Value $deadLock -NoNewline -Encoding UTF8
    $staleWatch = [Diagnostics.Stopwatch]::StartNew()
    Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-stale" | Out-Null
    $staleWatch.Stop()
    $stale = Read-LockJson -LockPath $lock
    Assert-True ($stale.sessionId -eq "codex-stale") "dead-pid stale lock was not taken over"
    Assert-True ($staleWatch.Elapsed.TotalSeconds -lt 2) "dead-pid stale takeover waited too long"

    Set-Content -Path $lock -Value "{not-json" -NoNewline -Encoding UTF8
    Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-corrupt" | Out-Null
    $corrupt = Read-LockJson -LockPath $lock
    Assert-True ($corrupt.sessionId -eq "codex-corrupt") "corrupt lock was not recovered"

    $timeoutProcess = Start-Process pwsh -WindowStyle Hidden -PassThru -ArgumentList @(
        "-NoProfile",
        "-Command",
        "Start-Sleep -Seconds 5"
    )
    try {
        $timeoutLock = [ordered]@{
            sessionId = "claude-timeout"
            acquired = [DateTimeOffset]::UtcNow.ToString("o")
            pid = $timeoutProcess.Id
            command = "git push"
            repo = (Split-Path $lock -Parent)
        } | ConvertTo-Json -Compress
        Set-Content -Path $lock -Value $timeoutLock -NoNewline -Encoding UTF8
        $oldMaxWait = $env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS
        $env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS = "2"
        $timeoutWatch = [Diagnostics.Stopwatch]::StartNew()
        Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-timeout" | Out-Null
        $timeoutWatch.Stop()
        if ($null -eq $oldMaxWait) {
            Remove-Item Env:\CODEX_GIT_LOCK_MAX_WAIT_SECONDS -ErrorAction SilentlyContinue
        } else {
            $env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS = $oldMaxWait
        }
        $timeout = Read-LockJson -LockPath $lock
        Assert-True ($timeout.sessionId -eq "claude-timeout") "timeout fail-open should not replace active foreign lock"
        Assert-True ($timeoutWatch.Elapsed.TotalSeconds -ge 2) "timeout fail-open returned too early"
        Assert-True ($timeoutWatch.Elapsed.TotalSeconds -lt 5) "timeout fail-open waited too long"
    } finally {
        if ($null -ne $oldMaxWait) {
            $env:CODEX_GIT_LOCK_MAX_WAIT_SECONDS = $oldMaxWait
        } else {
            Remove-Item Env:\CODEX_GIT_LOCK_MAX_WAIT_SECONDS -ErrorAction SilentlyContinue
        }
        if (-not $timeoutProcess.HasExited) {
            Stop-Process -Id $timeoutProcess.Id -Force
        }
    }

    $sleepProcess = Start-Process pwsh -WindowStyle Hidden -PassThru -ArgumentList @(
        "-NoProfile",
        "-Command",
        "Start-Sleep -Seconds 3"
    )
    try {
        $activeLock = [ordered]@{
            sessionId = "claude-active"
            acquired = [DateTimeOffset]::UtcNow.ToString("o")
            pid = $sleepProcess.Id
            command = "git push"
            repo = (Split-Path $lock -Parent)
        } | ConvertTo-Json -Compress
        Set-Content -Path $lock -Value $activeLock -NoNewline -Encoding UTF8
        $waitWatch = [Diagnostics.Stopwatch]::StartNew()
        Invoke-LockHelper -Repo $repo -Command "git add foo.txt" -SessionId "codex-after-claude" | Out-Null
        $waitWatch.Stop()
        $afterClaude = Read-LockJson -LockPath $lock
        Assert-True ($afterClaude.sessionId -eq "codex-after-claude") "Codex did not acquire after active foreign lock"
        Assert-True ($waitWatch.Elapsed.TotalSeconds -ge 2) "Codex did not wait for active foreign lock"
    } finally {
        if (-not $sleepProcess.HasExited) {
            Stop-Process -Id $sleepProcess.Id -Force
        }
    }

    Write-Output "PASS functional/no-lock/self-refresh/stress/stale/corrupt/timeout/cross-format"
} finally {
    if (Test-Path $repo) {
        Remove-Item -LiteralPath $repo -Recurse -Force
    }
}
