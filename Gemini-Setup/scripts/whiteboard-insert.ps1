# whiteboard-insert.ps1 — Shared helper for section-based whiteboard insertion
# Dot-source this in any hook that needs to write to MEMORY.md:
#   . "$PSScriptRoot/whiteboard-insert.ps1"
#   Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $myEntry
#
# This replaces Add-Content (which appends at file-end — FORBIDDEN by whiteboard rules).

# Replace-WhiteboardEntry — Removes ALL lines matching a pattern within a section, then inserts the new entry.
# Usage: Replace-WhiteboardEntry -Section "Systemzustand" -MatchPattern "Pending Admin Updates" -Entry "- **Pending Admin Updates (5):** foo,bar"
# This prevents duplicate accumulation (e.g. pending-admin-updates hook writing a new line every session).
function Replace-WhiteboardEntry {
    param(
        [string]$Section,
        [string]$MatchPattern,
        [string]$Entry
    )

    $memoryFile = Join-Path $env:USERPROFILE "proggs" ".Gemini" "agent-memory" "shared" "MEMORY.md"
    if (-not (Test-Path $memoryFile)) { return }

    $mutexName = "Global\GeminiWhiteboardMutex"
    $mutex = [System.Threading.Mutex]::new($false, $mutexName)
    $acquired = $false
    try {
        $acquired = $mutex.WaitOne(5000)
        if (-not $acquired) {
            if (Get-Command Hook-LogWarn -ErrorAction SilentlyContinue) {
                Hook-LogWarn "Replace-WhiteboardEntry: mutex timeout — skipping write"
            }
            return
        }

        try {
            $lines = Get-Content $memoryFile -Encoding UTF8 -ErrorAction Stop
            $sectionHeader = "## $Section"

            # Find the section
            $sectionIdx = -1
            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i].TrimEnd() -match "^## $([regex]::Escape($Section))") {
                    $sectionIdx = $i
                    break
                }
            }
            if ($sectionIdx -lt 0) { return }

            # Find section end
            $sectionEnd = $lines.Count
            for ($j = $sectionIdx + 1; $j -lt $lines.Count; $j++) {
                if ($lines[$j] -match '^## ' -or $lines[$j].TrimEnd() -eq '---') {
                    $sectionEnd = $j
                    break
                }
            }

            # Remove matching lines within section, collect non-matching lines
            $newLines = [System.Collections.ArrayList]::new()
            $insertPoint = $sectionEnd
            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($i -gt $sectionIdx -and $i -lt $sectionEnd -and $lines[$i] -match [regex]::Escape($MatchPattern)) {
                    continue  # Skip matching lines
                }
                [void]$newLines.Add($lines[$i])
            }

            # Find new insert point (end of section in cleaned list)
            $newSectionEnd = $newLines.Count
            for ($i = 0; $i -lt $newLines.Count; $i++) {
                if ($newLines[$i].TrimEnd() -match "^## $([regex]::Escape($Section))") {
                    for ($j = $i + 1; $j -lt $newLines.Count; $j++) {
                        if ($newLines[$j] -match '^## ' -or $newLines[$j].TrimEnd() -eq '---') {
                            $newSectionEnd = $j
                            break
                        }
                    }
                    break
                }
            }

            $newLines.Insert($newSectionEnd, $Entry)
            Set-Content -Path $memoryFile -Value $newLines.ToArray() -Encoding UTF8 -ErrorAction Stop
        } catch {
            if (Get-Command Hook-LogWarn -ErrorAction SilentlyContinue) {
                Hook-LogWarn "Replace-WhiteboardEntry failed for section '$Section': $_"
            }
        }
    } finally {
        if ($acquired) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
}

function Insert-WhiteboardEntry {
    param(
        [string]$Section,
        [string]$Entry
    )

    $memoryFile = Join-Path $env:USERPROFILE "proggs" ".Gemini" "agent-memory" "shared" "MEMORY.md"
    if (-not (Test-Path $memoryFile)) { return }

    # Acquire named mutex to prevent concurrent read-modify-write on MEMORY.md
    $mutexName = "Global\GeminiWhiteboardMutex"
    $mutex = [System.Threading.Mutex]::new($false, $mutexName)
    $acquired = $false
    try {
        $acquired = $mutex.WaitOne(5000)  # 5 second timeout
        if (-not $acquired) {
            if (Get-Command Hook-LogWarn -ErrorAction SilentlyContinue) {
                Hook-LogWarn "Insert-WhiteboardEntry: mutex timeout — skipping write"
            }
            return
        }

        try {
        $lines = Get-Content $memoryFile -Encoding UTF8 -ErrorAction Stop
        $sectionHeader = "## $Section"
        $placeholder = "_Noch keine Eintraege._"

        # Find the section header
        $sectionIdx = -1
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i].TrimEnd() -match "^## $([regex]::Escape($Section))") {
                $sectionIdx = $i
                break
            }
        }

        if ($sectionIdx -ge 0) {
            # Find placeholder or insertion point within this section
            $insertIdx = $sectionIdx + 1
            $placeholderIdx = -1
            for ($j = $sectionIdx + 1; $j -lt $lines.Count; $j++) {
                if ($lines[$j] -match '^## ') { break }
                if ($lines[$j] -match '^---') { break }
                if ($lines[$j].Trim() -eq $placeholder) {
                    $placeholderIdx = $j
                    break
                }
                $insertIdx = $j + 1
            }

            $newLines = [System.Collections.ArrayList]@($lines)
            # Split multi-line entries so each line is inserted separately
            # This prevents a single $Entry containing \n from corrupting the file
            $entryLines = $Entry -split "`r?`n"
            if ($placeholderIdx -ge 0) {
                # Replace placeholder with first line, insert remaining lines after it
                $newLines[$placeholderIdx] = $entryLines[0]
                for ($j = 1; $j -lt $entryLines.Count; $j++) {
                    $newLines.Insert($placeholderIdx + $j, $entryLines[$j])
                }
            } else {
                # Insert all lines at end of section
                for ($j = 0; $j -lt $entryLines.Count; $j++) {
                    $newLines.Insert($insertIdx + $j, $entryLines[$j])
                }
            }
            Set-Content -Path $memoryFile -Value $newLines.ToArray() -Encoding UTF8 -ErrorAction Stop
        }
        } catch {
            # Log error to hook-log if available, otherwise silently fail
            # This prevents callers from crashing when MEMORY.md is locked or disk is full
            if (Get-Command Hook-LogWarn -ErrorAction SilentlyContinue) {
                Hook-LogWarn "Insert-WhiteboardEntry failed for section '$Section': $_"
            }
        }
    } finally {
        if ($acquired) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
}

