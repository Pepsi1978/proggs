#!/usr/bin/env pwsh
# Memory Watchdog — SubagentStop Hook (v1.0)
# Checks if a senior agent wrote back to MEMORY.md after finishing.
# If not: logs a warning to MEMORY.md and outputs a notification.
#
# Senior agents that MUST write back: code-reviewer, tester, architect, debugger, optimizer
# Other agents (coder, researcher, etc.) are exempt.

param()

$memoryFile = Join-Path $env:USERPROFILE ".claude" "agent-memory" "shared" "MEMORY.md"
$whiteboardFile = Join-Path $env:USERPROFILE ".claude" "agent-memory" "shared" "MEMORY.md"
$counterFile = Join-Path $env:TEMP "claude-writeback-counter.txt"

# Count SubagentStop events since last MEMORY.md write
# This avoids the false-negative problem where a non-senior agent finishes
# right after a senior agent modified MEMORY.md — we track cumulative misses instead.
$missCount = 0
if (Test-Path $counterFile) {
    $missCount = [int](Get-Content $counterFile -ErrorAction SilentlyContinue)
}

# Check if MEMORY.md was modified in the last 3 minutes
$recentWrite = $false
if (Test-Path $memoryFile) {
    $lastWrite = (Get-Item $memoryFile).LastWriteTime
    $threshold = (Get-Date).AddMinutes(-3)
    if ($lastWrite -gt $threshold) {
        $recentWrite = $true
    }
}

if ($recentWrite) {
    # Reset counter — someone wrote to MEMORY.md recently
    Set-Content -Path $counterFile -Value "0" -Force
    Write-Output "MEMORY_WATCHDOG: Write-back detected — counter reset"
    exit 0
}

# Increment miss counter
$missCount++
Set-Content -Path $counterFile -Value "$missCount" -Force

# Only alert after 5+ consecutive misses (avoids false positives from coder/researcher agents)
if ($missCount -ge 5) {
    $date = Get-Date -Format "yyyy-MM-dd HH:mm"
    $warning = @"

### [$date] Agent: Write-Back nicht erfolgt ($missCount aufeinanderfolgende Agents)
- **Symptom**: $missCount Agents nacheinander haben MEMORY.md nicht aktualisiert
- **Root Cause**: Mandatory Write-Back wird von Agents ignoriert
- **Prevention**: memory-watchdog.ps1 (5-Strike-Regel, SubagentStop)
- **Status**: AUTO-LOGGED
"@

    if (Test-Path $whiteboardFile) {
        Add-Content -Path $whiteboardFile -Value $warning -Encoding UTF8
    }
    # Reset counter after logging
    Set-Content -Path $counterFile -Value "0" -Force
    Write-Output "MEMORY_WATCHDOG: $missCount consecutive misses — logged to MEMORY.md"
} else {
    Write-Output "MEMORY_WATCHDOG: No write-back ($missCount/5 misses)"
}
exit 0
