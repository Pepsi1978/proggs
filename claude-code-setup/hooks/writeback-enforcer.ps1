# writeback-enforcer.ps1 — SubagentStop Hook for Write-Back Enforcement (C1)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md
# into the CORRECT SECTION (not just appended at the end).
# v2.1: Fixed CRLF handling — uses regex instead of IndexOf for reliable section detection.
#
# Sentinel file format: $env:TEMP/agent-writeback-{agentname}.json
# Content: { "agent": "name", "timestamp": "ISO8601", "findings": "one-line summary" }

$sentinelDir = $env:TEMP
$sentinelPattern = "agent-writeback-*.json"
# Write to the REPO copy (~/proggs/.claude/) — authoritative whiteboard
$memoryFile = Join-Path $env:USERPROFILE "proggs" ".claude" "agent-memory" "shared" "MEMORY.md"

# Guard: exit gracefully if whiteboard doesn't exist
if (-not (Test-Path $memoryFile)) { exit 0 }

# Map agent names to their target sections in MEMORY.md
$sectionMap = @{
    "code-reviewer"       = "## Erkenntnisse aus Code Reviews"
    "batch-reviewer"      = "## Erkenntnisse aus Code Reviews"
    "mar-reviewer"        = "## Erkenntnisse aus Code Reviews"
    "tester"              = "## Erkenntnisse aus Tests"
    "architect"           = "## Architektur-Entscheidungen"
    "challenger"          = "## Architektur-Entscheidungen"
    "debugger"            = "## Debugging-Muster"
    "optimizer"           = "## Performance & Optimierung"
    "ui-polisher"         = "## UI/UX-Patterns"
    "researcher"          = "## Forschung & Intelligence"
    "intelligence-researcher" = "## Forschung & Intelligence"
    "evolution-analyst"   = "## Systemzustand (aktuell)"
    "quality-gate"        = "## Erkenntnisse aus Tests"
    "env-checker"         = "## Systemzustand (aktuell)"
}

# Find all sentinel files
$sentinelFiles = Get-ChildItem -Path $sentinelDir -Filter $sentinelPattern -ErrorAction SilentlyContinue

if ($sentinelFiles.Count -gt 0) {
    # Read the full whiteboard content as array of lines (preserves original line endings)
    $lines = Get-Content $memoryFile -Encoding UTF8 -ErrorAction Stop
    $placeholder = "_Noch keine Eintraege._"

    foreach ($f in $sentinelFiles) {
        try {
            $raw = Get-Content $f.FullName -Raw -ErrorAction Stop
            $data = $raw | ConvertFrom-Json -ErrorAction Stop
            $ts = Get-Date -Format "yyyy-MM-dd HH:mm"
            $agentName = if ($data.agent) { $data.agent } else { "unknown" }
            $findings = if ($data.findings) { $data.findings } else { "(no findings)" }

            $entry = "- **[$ts] $agentName**: $findings"

            # Find the correct section to insert into
            $targetSection = $sectionMap[$agentName]
            if (-not $targetSection) {
                $targetSection = "## Erkenntnisse aus Code Reviews"
            }

            # Find the section header line index
            $sectionIdx = -1
            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i].TrimEnd() -eq $targetSection) {
                    $sectionIdx = $i
                    break
                }
            }

            if ($sectionIdx -ge 0) {
                # Find placeholder within this section (before next ## heading)
                $placeholderIdx = -1
                $insertIdx = $sectionIdx + 1
                for ($j = $sectionIdx + 1; $j -lt $lines.Count; $j++) {
                    if ($lines[$j] -match '^## ') { break }  # Next section
                    if ($lines[$j] -match '^---') { break }   # Section separator
                    if ($lines[$j].Trim() -eq $placeholder) {
                        $placeholderIdx = $j
                        break
                    }
                    $insertIdx = $j + 1
                }

                if ($placeholderIdx -ge 0) {
                    # Replace placeholder with entry
                    $lines[$placeholderIdx] = $entry
                } else {
                    # Insert before next section (at insertIdx)
                    $newLines = [System.Collections.ArrayList]@($lines)
                    $newLines.Insert($insertIdx, $entry)
                    $lines = $newLines.ToArray()
                }
            } else {
                # Section not found — append at end as last resort
                $lines += $entry
            }

            # Remove processed sentinel file
            Remove-Item $f.FullName -ErrorAction SilentlyContinue

            Write-Output "WriteBack-Enforcer: Merged $agentName findings into '$targetSection'"
        }
        catch {
            Write-Output "WriteBack-Enforcer: Error processing $($f.Name): $_"
        }
    }

    # Write the updated content back (line-by-line preserves original encoding)
    Set-Content -Path $memoryFile -Value $lines -Encoding UTF8 -ErrorAction Stop
}
# If no sentinel files exist, that's OK — not every subagent is a senior agent.
# The memory-watchdog.ps1 handles the "should have written but didn't" case.
