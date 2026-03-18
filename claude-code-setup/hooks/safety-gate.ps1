# Safety Gate: Block dangerous commands before execution
# Hook event: PreToolUse (Bash)
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"

$input = [Console]::In.ReadToEnd()
try {
    $json = $input | ConvertFrom-Json
} catch {
    exit 0
}

$toolName = $json.tool_name
$toolInput = $json.tool_input

# Only check Bash commands
if ($toolName -ne 'Bash') { exit 0 }

$cmd = $toolInput.command
if (-not $cmd) { exit 0 }

# Dangerous patterns to block
$dangerous = @(
    'rm\s+-rf\s+[/~]',                # rm -rf / or ~
    'git\s+push\s+--force\s+.*main',  # force push to main
    'git\s+reset\s+--hard',            # hard reset
    'git\s+restore\s+\.',              # git restore . (discard all changes)
    'git\s+branch\s+-D',              # git branch -D (force-delete branch)
    '(?i)DROP\s+TABLE',               # SQL drop table (case-insensitive)
    '(?i)DROP\s+DATABASE',            # SQL drop database (case-insensitive)
    '(?i)TRUNCATE\s+TABLE',           # SQL truncate (case-insensitive)
    'format\s+[A-Z]:',                # format drive
    'del\s+/[sS]\s+/[qQ]\s+C:',      # delete C: drive
    'Remove-Item\s+-Recurse.*C:\\'    # PowerShell recursive delete C:
)

foreach ($pattern in $dangerous) {
    if ($cmd -match $pattern) {
        Hook-LogError "BLOCKED dangerous command: $pattern — cmd: $($cmd.Substring(0, [Math]::Min(100, $cmd.Length)))"
        Write-Output "{`"error`":`"BLOCKED: Dangerous command detected — $pattern`"}"
        exit 2
    }
}
exit 0
