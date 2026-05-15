#!/usr/bin/env pwsh
# Memory Watchdog ÔÇö SubagentStop Hook (v1.0)
# Checks if a senior agent wrote back to MEMORY.md after finishing.
# If not: logs a warning to MEMORY.md and outputs a notification.
#
# Senior agents that MUST write back: code-reviewer, tester, architect, debugger, optimizer
# Other agents (coder, researcher, etc.) are exempt.

param()
. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

# Write to the REPO copy (~/proggs/.claude/) ÔÇö this is the authoritative whiteboard
# that gets committed. The ~/.claude/ copy is kept in sync by the commit workflow.
$memoryFile = Join-Path $env:USERPROFILE "proggs" ".claude" "agent-memory" "shared" "MEMORY.md"
$counterFile = Join-Path $env:TEMP "claude-writeback-counter.txt"

# Count SubagentStop events since last MEMORY.md write
# This avoids the false-negative problem where a non-senior agent finishes
# right after a senior agent modified MEMORY.md ÔÇö we track cumulative misses instead.

# Acquire a file-based lock for the counter file to prevent concurrent read-modify-write
$lockFile = "$counterFile.lock"
$lockAcquired = $false
for ($retry = 0; $retry -lt 10; $retry++) {
    try {
        [System.IO.File]::Open($lockFile, 'CreateNew', 'Write').Close()
        $lockAcquired = $true
        break
    } catch {
        Start-Sleep -Milliseconds 100
    }
}

try {
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
        # Reset counter ÔÇö someone wrote to MEMORY.md recently
        Set-Content -Path $counterFile -Value "0" -Force
        Write-Output "MEMORY_WATCHDOG: Write-back detected ÔÇö counter reset"
        exit 0
    }

    # Increment miss counter
    $missCount++
    Set-Content -Path $counterFile -Value "$missCount" -Force

    # Alert after 3+ consecutive misses (was 5, briefly 2 ÔÇö 3 avoids false positives
    # from parallel coder/researcher agents while still catching failures 40% faster)
    if ($missCount -ge 3) {
        $date = Get-Date -Format "yyyy-MM-dd HH:mm"
        # Use section-based insertion (Add-Content to file-end is FORBIDDEN)
        # Standard format: ### date ÔÇö Hook: name ÔÇö message
        $entry = "### $date ÔÇö Hook: memory-watchdog.ps1 ÔÇö Write-Back nicht erfolgt ($missCount aufeinanderfolgende Agents) ÔÇö Status: AUTO-LOGGED"
        Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
        # Reset counter after logging
        Set-Content -Path $counterFile -Value "0" -Force
        Write-Output "MEMORY_WATCHDOG: $missCount consecutive misses ÔÇö logged to MEMORY.md"
    } else {
        Write-Output "MEMORY_WATCHDOG: No write-back ($missCount/3 misses)"
    }
} finally {
    # Always release the lock ÔÇö even on crash or early exit
    if ($lockAcquired) { Remove-Item $lockFile -Force -ErrorAction SilentlyContinue }
}

exit 0
