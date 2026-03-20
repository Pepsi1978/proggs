# writeback-enforcer.ps1 — SubagentStop Hook for Write-Back Enforcement (C1)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md
# If no sentinel file found, logs a warning to MEMORY.md (Sektion "Offene Fehler & Probleme")
#
# Sentinel file format: $env:TEMP/agent-writeback-{agentname}.json
# Content: { "agent": "name", "timestamp": "ISO8601", "findings": "one-line summary" }

$sentinelDir = $env:TEMP
$sentinelPattern = "agent-writeback-*.json"
$memoryFile = Join-Path $env:USERPROFILE ".claude\agent-memory\shared\MEMORY.md"
$whiteboardFile = $memoryFile  # Same file — errors go to "Offene Fehler & Probleme" section

# Find all sentinel files
$sentinelFiles = Get-ChildItem -Path $sentinelDir -Filter $sentinelPattern -ErrorAction SilentlyContinue

if ($sentinelFiles.Count -gt 0) {
    foreach ($f in $sentinelFiles) {
        try {
            $raw = Get-Content $f.FullName -Raw -ErrorAction Stop
            $data = $raw | ConvertFrom-Json -ErrorAction Stop
            $ts = Get-Date -Format "yyyy-MM-dd HH:mm"
            $agentName = if ($data.agent) { $data.agent } else { "unknown" }
            $findings = if ($data.findings) { $data.findings } else { "(no findings)" }

            # Append to MEMORY.md under the appropriate section
            $entry = "`n- **[$ts] $agentName**: $findings"
            Add-Content -Path $memoryFile -Value $entry -ErrorAction Stop

            # Remove processed sentinel file
            Remove-Item $f.FullName -ErrorAction SilentlyContinue

            Write-Output "WriteBack-Enforcer: Merged findings from $agentName"
        }
        catch {
            Write-Output "WriteBack-Enforcer: Error processing $($f.Name): $_"
        }
    }
}
# If no sentinel files exist, that's OK — not every subagent is a senior agent.
# The memory-watchdog.ps1 (existing hook) handles the "should have written but didn't" case.
