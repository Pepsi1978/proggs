# reindex-codebase.ps1 (Gemini) — Hintergrund-Indexierung
$RepoRoot = "C:\Users\barwa\GeminiCLI"
$ScriptPath = "$RepoRoot\Gemini-Setup\scripts\code-search-mcp-client.mjs"

Write-Host "[$(Get-Date)] Starte Hintergrund-Reindex..."
Start-Process -FilePath "bun" -ArgumentList "run", "$RepoRoot\mcp-code-search\src\reindex.ts", "$RepoRoot" -WindowStyle Hidden

Write-Host "✅ Reindex-Job im Hintergrund gestartet."
