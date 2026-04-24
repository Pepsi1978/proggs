#!/usr/bin/env pwsh
# Memory Watchdog — SubagentStop Hook (v1.0)
# Checks if a senior agent wrote back to MEMORY.md after finishing.
# If not: logs a warning to MEMORY.md and outputs a notification.
#
# Senior agents that MUST write back: code-reviewer, tester, architect, debugger, optimizer
# Other agents (coder, researcher, etc.) are exempt.

param()
. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

# Input guard — only run on REAL SubagentStop events that have an agent_id.
# Prevents the 2026-04-15/18 endless-loop bug where the hook fired on every
# Stop event and spammed 50+ "Write-Back nicht erfolgt" entries per session.
# NEVER dot-sourced — exit 0 is safe.
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin -and $stdin.Trim() -ne "") {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.agent_id -or [string]::IsNullOrWhiteSpace($parsed.agent_id)) {
            exit 0
        }
    } else {
        exit 0
    }
} catch {
    exit 0
}

# Write to the REPO copy (~/.codex/) — this is the authoritative whiteboard
# that gets committed. The ~/.codex/ copy is kept in sync by the commit workflow.
$memoryFile = Join-Path $env:USERPROFILE "proggs" ".codex" "agent-memory" "shared" "MEMORY.md"
$counterFile = Join-Path $env:TEMP "codex-writeback-counter.txt"

# Count SubagentStop events since last MEMORY.md write
# This avoids the false-negative problem where a non-senior agent finishes
# right after a senior agent modified MEMORY.md — we track cumulative misses instead.

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
        # Reset counter — someone wrote to MEMORY.md recently
        Set-Content -Path $counterFile -Value "0" -Force
        Write-Output "MEMORY_WATCHDOG: Write-back detected — counter reset"
        exit 0
    }

    # Increment miss counter
    $missCount++
    Set-Content -Path $counterFile -Value "$missCount" -Force

    # Alert after 5+ consecutive misses (was 3 — raised to 5 to reduce log-spam on Windows
    # when parallel coder/researcher agents run in batches without needing to write MEMORY.md)
    # Deduplication: skip logging if the last entry of this kind is less than 60 minutes old.
    if ($missCount -ge 5) {
        $date = Get-Date -Format "yyyy-MM-dd HH:mm"
        $lastLogFile = Join-Path $env:TEMP "codex-writeback-last-log.txt"
        $shouldLog = $true
        if (Test-Path $lastLogFile) {
            try {
                $lastLog = [DateTime](Get-Content $lastLogFile -ErrorAction SilentlyContinue)
                if ((Get-Date) - $lastLog -lt (New-TimeSpan -Minutes 60)) {
                    $shouldLog = $false
                }
            } catch { }
        }
        if ($shouldLog) {
            $entry = "### $date — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt ($missCount aufeinanderfolgende Agents) — Status: AUTO-LOGGED"
            try { Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry } catch { }
            try { Set-Content -Path $lastLogFile -Value (Get-Date).ToString('o') -Force -ErrorAction Stop } catch { }
            Write-Output "MEMORY_WATCHDOG: $missCount consecutive misses — logged to MEMORY.md"
        } else {
            Write-Output "MEMORY_WATCHDOG: $missCount consecutive misses — dedup (last log <60min)"
        }
        # Reset counter whether logged or deduped
        try { Set-Content -Path $counterFile -Value "0" -Force -ErrorAction Stop } catch { }
    } else {
        Write-Output "MEMORY_WATCHDOG: No write-back ($missCount/5 misses)"
    }
} finally {
    # Always release the lock — even on crash or early exit
    if ($lockAcquired) { Remove-Item $lockFile -Force -ErrorAction SilentlyContinue }
}

exit 0
