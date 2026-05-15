# post-reindex-summary.ps1 (Gemini) ÔÇö Status-Zusammenfassung nach Reindex
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
$McpClient = "$RepoRoot\Gemini-Setup\scripts\code-search-mcp-client.mjs"

try {
    # Hole aktuellen Status via MCP-Client
    $StatusJson = node "$McpClient" status --workspace "$RepoRoot" --json | ConvertFrom-Json -ErrorAction Stop
    $StatusText = $StatusJson.statusText

    # Extrahiere Stats (Files/Chunks)
    $Files = if ($StatusText -match 'Files indexed: (\d+)') { $Matches[1] } else { "?" }
    $Chunks = if ($StatusText -match 'Code chunks: (\d+)') { $Matches[1] } else { "?" }
    
    $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
    $Entry = "### $Timestamp ÔÇö Code-Suche Index aktualisiert: $Files Dateien, $Chunks Chunks."

    # In Whiteboard schreiben
    . "$RepoRoot\Gemini-Setup\scripts\whiteboard-insert.ps1"
    Insert-WhiteboardEntry -Section "Performance & Optimierung" -Entry $Entry

    Write-Host "Ô£à Index-Zusammenfassung im Whiteboard aktualisiert." -ForegroundColor Green
} catch {
    Write-Host "ÔÜá´©Å Konnte Index-Zusammenfassung nicht aktualisieren: $($_.Exception.Message)" -ForegroundColor Yellow
}
