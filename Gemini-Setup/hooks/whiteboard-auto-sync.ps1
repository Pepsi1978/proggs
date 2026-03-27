# whiteboard-auto-sync.ps1 — SessionStart: Provide key context from MEMORY.md
# Part of the Gemini CLI intelligence system.

$ErrorActionPreference = "SilentlyContinue"

$Whiteboard = Join-Path $env:USERPROFILE "GeminiCLI\proggs\Gemini-Setup\agent-memory\shared\MEMORY.md"

if (-not (Test-Path $Whiteboard)) { exit 0 }

# Extract the "Offene Fehler & Probleme" section
$content = Get-Content $Whiteboard -Raw
$pattern = "(?s)## Offene Fehler & Probleme\s*(.*?)(?=\n##|$)"
if ($content -match $pattern) {
    $issues = $Matches[1].Trim()
    if ($issues) {
        Write-Output "--- WHITEBOARD CONTEXT (Offene Probleme) ---"
        # Take the first 3 issues (lines starting with ###)
        $issues -split "\n" | Where-Object { $_ -match "###" } | Select-Object -First 3
        Write-Output "-------------------------------------------"
    }
}

exit 0
