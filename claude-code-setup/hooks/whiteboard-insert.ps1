# whiteboard-insert.ps1 — Shared helper for section-based whiteboard insertion
# Dot-source this in any hook that needs to write to MEMORY.md:
#   . "$PSScriptRoot/whiteboard-insert.ps1"
#   Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $myEntry
#
# This replaces Add-Content (which appends at file-end — FORBIDDEN by whiteboard rules).

function Insert-WhiteboardEntry {
    param(
        [string]$Section,
        [string]$Entry
    )

    $memoryFile = Join-Path $env:USERPROFILE "proggs" ".claude" "agent-memory" "shared" "MEMORY.md"
    if (-not (Test-Path $memoryFile)) { return }

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
        if ($placeholderIdx -ge 0) {
            # Replace placeholder
            $newLines[$placeholderIdx] = $Entry
        } else {
            # Insert at end of section
            $newLines.Insert($insertIdx, $Entry)
        }
        Set-Content -Path $memoryFile -Value $newLines.ToArray() -Encoding UTF8 -ErrorAction Stop
    }
}
