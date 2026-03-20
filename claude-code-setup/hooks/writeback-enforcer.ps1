# writeback-enforcer.ps1 — SubagentStop Hook for Write-Back Enforcement (C1)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md
# into the CORRECT SECTION (not just appended at the end).
#
# Sentinel file format: $env:TEMP/agent-writeback-{agentname}.json
# Content: { "agent": "name", "timestamp": "ISO8601", "findings": "one-line summary" }

$sentinelDir = $env:TEMP
$sentinelPattern = "agent-writeback-*.json"
# Write to the REPO copy (~/proggs/.claude/) — authoritative whiteboard
$memoryFile = Join-Path $env:USERPROFILE "proggs\.claude\agent-memory\shared\MEMORY.md"

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
}

# Find all sentinel files
$sentinelFiles = Get-ChildItem -Path $sentinelDir -Filter $sentinelPattern -ErrorAction SilentlyContinue

if ($sentinelFiles.Count -gt 0) {
    # Read the full whiteboard content
    $content = Get-Content $memoryFile -Raw -ErrorAction Stop

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
                # Unknown agent — append under Erkenntnisse aus Code Reviews as fallback
                $targetSection = "## Erkenntnisse aus Code Reviews"
            }

            # Find the section and insert after the placeholder or last entry
            $placeholder = "_Noch keine Eintraege._"
            $sectionIdx = $content.IndexOf($targetSection)

            if ($sectionIdx -ge 0) {
                # Find the placeholder within this section
                $searchStart = $sectionIdx + $targetSection.Length
                $placeholderIdx = $content.IndexOf($placeholder, $searchStart)
                # Check that placeholder is within this section (before next ## heading)
                $nextSectionIdx = $content.IndexOf("`n## ", $searchStart + 1)

                if ($placeholderIdx -ge 0 -and ($nextSectionIdx -lt 0 -or $placeholderIdx -lt $nextSectionIdx)) {
                    # Replace placeholder with the entry
                    $content = $content.Remove($placeholderIdx, $placeholder.Length).Insert($placeholderIdx, $entry)
                } else {
                    # No placeholder — insert before the next section (or section separator)
                    if ($nextSectionIdx -ge 0) {
                        $content = $content.Insert($nextSectionIdx, "$entry`n")
                    } else {
                        # Fallback: append at end
                        $content += "`n$entry"
                    }
                }
            } else {
                # Section not found — append at end as last resort
                $content += "`n$entry"
            }

            # Remove processed sentinel file
            Remove-Item $f.FullName -ErrorAction SilentlyContinue

            Write-Output "WriteBack-Enforcer: Merged $agentName findings into '$targetSection'"
        }
        catch {
            Write-Output "WriteBack-Enforcer: Error processing $($f.Name): $_"
        }
    }

    # Write the updated content back
    Set-Content -Path $memoryFile -Value $content -Encoding UTF8 -NoNewline -ErrorAction Stop
}
# If no sentinel files exist, that's OK — not every subagent is a senior agent.
# The memory-watchdog.ps1 (existing hook) handles the "should have written but didn't" case.
