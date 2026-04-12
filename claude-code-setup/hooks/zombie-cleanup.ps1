# zombie-cleanup.ps1 — SessionStart hook: Kill truly orphaned node.exe processes
# Category: Standalone Non-Blocking — MUST end with exit 0
#
# Strategy: Only kill node.exe processes whose PARENT process no longer exists.
# This safely preserves MCP servers for ALL running sessions while cleaning up
# orphans from crashed/closed sessions.
#
# IMPORTANT: NEVER kill claude.exe processes — they manage their own lifecycle.
# Killing older claude.exe processes DESTROYS active sessions in other tabs.
# (Root cause of "session destruction" bug, fixed 2026-04-12)

$ErrorActionPreference = 'SilentlyContinue'

try {
    # Get all running claude.exe PIDs — these are ACTIVE session owners
    $activeClaudes = @{}
    Get-Process -Name 'claude' -ErrorAction SilentlyContinue | ForEach-Object {
        $activeClaudes[$_.Id] = $true
    }

    if ($activeClaudes.Count -eq 0) {
        # No claude processes at all — nothing to clean
        exit 0
    }

    # Find orphaned node.exe processes:
    # A node.exe is an orphan if its parent PID is NOT an active claude.exe process
    # AND the parent PID doesn't exist at all (parent crashed/exited)
    $zombieCount = 0
    $nodeProcs = Get-Process -Name 'node' -ErrorAction SilentlyContinue

    if ($nodeProcs) {
        foreach ($node in $nodeProcs) {
            try {
                # Get parent PID via CIM
                $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $($node.Id)" -ErrorAction Stop
                $parentPid = $cim.ParentProcessId

                # Check if parent is alive
                $parentAlive = Get-Process -Id $parentPid -ErrorAction SilentlyContinue

                if (-not $parentAlive) {
                    # Parent is GONE — this node.exe is a true orphan
                    Stop-Process -Id $node.Id -Force -ErrorAction SilentlyContinue
                    $zombieCount++
                }
                # If parent is alive (whether claude or something else), leave it alone
            } catch {
                # CIM query failed — skip this process (don't kill what we don't understand)
            }
        }
    }

    if ($zombieCount -gt 0) {
        Write-Host "Zombie-Cleanup: $zombieCount zombie processes killed"
    }
} catch {
    # Never block session start
}

exit 0
