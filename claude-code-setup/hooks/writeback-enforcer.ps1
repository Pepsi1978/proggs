# writeback-enforcer.ps1 — SubagentStop Hook for Write-Back Enforcement (C1)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md
# into the CORRECT SECTION (not just appended at the end).
# v2.2: Added subagent-failure + tool-failure to sectionMap (K10).
#       Added section-field override: sentinel JSON may contain "section" to override the map.
#       Fixed CRLF handling — uses regex instead of IndexOf for reliable section detection.
#
# Sentinel file format: $env:TEMP/agent-writeback-{agentname}.json
# Content: { "agent": "name", "timestamp": "ISO8601", "findings": "one-line summary" }
# Optional: add "section": "Offene Fehler & Probleme" to route to a specific section.

. "$PSScriptRoot/hook-log.ps1"

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
    "evolution-analyst"   = "## Systemzustand"
    "quality-gate"        = "## Erkenntnisse aus Tests"
    "env-checker"         = "## Systemzustand"
    "coder"               = "## Erkenntnisse aus Code Reviews"
    # K10: failure sentinels from SubagentStop and PostToolUseFailure hooks
    "subagent-failure"    = "## Offene Fehler & Probleme"
    "tool-failure"        = "## Offene Fehler & Probleme"
    # v4: intelligence checker and learning loop sentinels
    "intelligence-checker" = "## Meta-Intelligenz & Selbstverbesserung"
    "learning-loop"       = "## Meta-Intelligenz & Selbstverbesserung"
}

# Find all sentinel files
$sentinelFiles = Get-ChildItem -Path $sentinelDir -Filter $sentinelPattern -ErrorAction SilentlyContinue

if ($sentinelFiles.Count -gt 0) {
    # Acquire named mutex to prevent concurrent read-modify-write on MEMORY.md
    $mutexName = "Global\ClaudeWhiteboardMutex"
    $mutex = [System.Threading.Mutex]::new($false, $mutexName)
    $acquired = $false
    try {
        $acquired = $mutex.WaitOne(5000)  # 5 second timeout
        if (-not $acquired) {
            Write-Output "WriteBack-Enforcer: mutex timeout — skipping write"
            exit 0  # Transient condition — not a real error
        }

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

                # K10: If the sentinel contains a "section" field, use it as override.
                # This allows quality-gate (3 sentinels) and failure hooks to route correctly.
                $sectionOverride = if ($data.section) { $data.section } else { $null }
                if ($sectionOverride) {
                    $targetSection = "## $sectionOverride"
                    # Avoid double-## if the value already starts with ##
                    if ($sectionOverride -like '##*') { $targetSection = $sectionOverride }
                } else {
                    $targetSection = $sectionMap[$agentName]
                    if (-not $targetSection) {
                        $targetSection = "## Erkenntnisse aus Code Reviews"
                    }
                }

                # Find the section header line index
                $sectionIdx = -1
                for ($i = 0; $i -lt $lines.Count; $i++) {
                    if ($lines[$i].TrimEnd() -match "^$([regex]::Escape($targetSection))") {
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
                    Hook-LogError "WriteBack-Enforcer: section '$targetSection' not found for agent '$agentName' — appending at end as last resort"
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
    } finally {
        if ($acquired) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
}
# If no sentinel files exist, that's OK — not every subagent is a senior agent.
# The memory-watchdog.ps1 handles the "should have written but didn't" case.

exit 0
