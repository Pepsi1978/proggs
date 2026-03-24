# plugin-health-check.ps1
$RepoRoot = "C:\Users\barwa\GeminiCLI"
$LogFile = "$RepoRoot/Gemini-Setup/logs/plugin-health.log"
if (-not (Test-Path (Split-Path $LogFile))) { New-Item -ItemType Directory -Path (Split-Path $LogFile) -Force }

Write-Host "[$(Get-Date)] Starte Plugin-Check (PS7)..."
"[$(Get-Date)] Starte Plugin-Check (PS7)..." | Out-File -FilePath $LogFile -Append

# CHECK 1: Code-Search MCP
Write-Host "Prüfe Code-Search MCP..."
node "$RepoRoot/Gemini-Setup/scripts/check-code-search-health.mjs" | Out-File -FilePath $LogFile -Append
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Code-Search MCP ist gesund." -ForegroundColor Green
} else {
    Write-Host "⚠️ Code-Search MCP hat Probleme." -ForegroundColor Yellow
}

# CHECK 2: Ollama
try {
    $response = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method Get -TimeoutSec 2
    Write-Host "✅ Ollama ist erreichbar." -ForegroundColor Green
} catch {
    Write-Host "⚠️ Ollama ist nicht aktiv." -ForegroundColor Yellow
}
