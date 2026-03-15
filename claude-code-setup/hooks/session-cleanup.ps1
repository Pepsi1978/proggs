# Session Cleanup: Clean up temp files when Claude Code exits
# Hook event: SessionEnd
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"

$cleaned = 0

# Clean Claude temp directory (files older than 2 hours)
$claudeTemp = Join-Path $env:TEMP "claude"
if (Test-Path $claudeTemp) {
    $oldFiles = Get-ChildItem $claudeTemp -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-2) }
    foreach ($file in $oldFiles) {
        try { Remove-Item $file.FullName -Force -ErrorAction Stop; $cleaned++ } catch { }
    }
    # Remove empty directories
    Get-ChildItem $claudeTemp -Directory -Recurse -ErrorAction SilentlyContinue |
        Where-Object { (Get-ChildItem $_.FullName -ErrorAction SilentlyContinue).Count -eq 0 } |
        ForEach-Object { try { Remove-Item $_.FullName -Force } catch { } }
}

# Clean node compile cache (files older than 1 day)
$nodeCache = Join-Path $env:TEMP "node-compile-cache"
if (Test-Path $nodeCache) {
    $oldNodeFiles = Get-ChildItem $nodeCache -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-1) }
    foreach ($file in $oldNodeFiles) {
        try { Remove-Item $file.FullName -Force -ErrorAction Stop; $cleaned++ } catch { }
    }
}

# Clean old hook log files (already handled by hook-log.ps1, but belt-and-suspenders)
$hookLogs = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
if (Test-Path $hookLogs) {
    Get-ChildItem $hookLogs -Filter "*.log" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        ForEach-Object { try { Remove-Item $_.FullName -Force; $cleaned++ } catch { } }
}

# Log remaining disk space
try {
    $freeGB = [math]::Round((Get-PSDrive C).Free / 1GB, 1)
    Hook-Log "cleanup done: $cleaned files removed, ${freeGB}GB free"
} catch {
    Hook-Log "cleanup done: $cleaned files removed"
}

exit 0
