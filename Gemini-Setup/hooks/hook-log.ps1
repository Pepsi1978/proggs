# hook-log.ps1 — Error logging for Claude Code hooks (PowerShell 7)
# Dot-source this at the top of every hook script:
#   . "$PSScriptRoot/hook-log.ps1"
#
# Provides:
#   Hook-Log "message"        — log info message
#   Hook-LogError "message"   — log error with details
#   Hook-LogWarn "message"    — log warning
#   Automatic error trapping  — uncaught errors are logged on exit
#
# Logs to: ~/.claude/logs/hooks/YYYY-MM-DD.log
# Log rotation: files older than 14 days are auto-deleted

$script:_HookLogDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
$script:_HookLogName = if ($MyInvocation.ScriptName) {
    [System.IO.Path]::GetFileNameWithoutExtension($MyInvocation.ScriptName)
} else {
    "unknown-hook"
}
$script:_HookLogFile = Join-Path $script:_HookLogDir "$(Get-Date -Format 'yyyy-MM-dd').log"
$script:_HookStartTime = Get-Date

# Ensure log directory exists
if (-not (Test-Path $script:_HookLogDir)) {
    New-Item -ItemType Directory -Force -Path $script:_HookLogDir | Out-Null
}

# Clean up log files older than 14 days
try {
    Get-ChildItem -Path $script:_HookLogDir -Filter "*.log" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        Remove-Item -Force -ErrorAction SilentlyContinue
} catch { }

function Hook-Log {
    param([string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $line = "[$ts] $($script:_HookLogName): $Message"
    try { $line | Out-File -FilePath $script:_HookLogFile -Append -Encoding utf8 } catch { }
}

function Hook-LogError {
    param([string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $line = "[$ts] ERROR $($script:_HookLogName): $Message"
    try { $line | Out-File -FilePath $script:_HookLogFile -Append -Encoding utf8 } catch { }
}

function Hook-LogWarn {
    param([string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $line = "[$ts] WARN  $($script:_HookLogName): $Message"
    try { $line | Out-File -FilePath $script:_HookLogFile -Append -Encoding utf8 } catch { }
}

# Register automatic error logging on script exit.
# NOTE: -SupportEvent is intentionally NOT used here. The flag would hide the subscriber
# from Get-EventSubscriber (making it unremovable/undebuggable), and more importantly,
# the Action block runs in a child scope where Hook-LogError is not available.
# FIX K7: $script: scope is inaccessible inside event Action blocks — capture values into
# local variables first and reference them via $using: inside the Action scriptblock.
$capturedLogFile = $script:_HookLogFile
$capturedHookName = $script:_HookLogName
$null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action {
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        $ts = Get-Date -Format "HH:mm:ss"
        $logFile = $using:capturedLogFile
        $hookName = $using:capturedHookName
        try {
            "[$ts] ERROR $hookName`: unexpected exit code $LASTEXITCODE" |
                Out-File -FilePath $logFile -Append -Encoding utf8
        } catch { }
    }
} 2>$null

# Trap terminating errors and log them
trap {
    $errMsg = $_.Exception.Message -replace "`r?`n", " "
    $errLine = $_.InvocationInfo.ScriptLineNumber
    $errScript = if ($_.InvocationInfo.ScriptName) {
        [System.IO.Path]::GetFileName($_.InvocationInfo.ScriptName)
    } else { "unknown" }
    Hook-LogError "$errScript`:$errLine — $errMsg"
}

Hook-Log "started"
