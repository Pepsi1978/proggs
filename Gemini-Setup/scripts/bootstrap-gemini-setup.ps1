# bootstrap-gemini-setup.ps1
$RepoRoot = "C:\Users\barwa\GeminiCLI"
cd $RepoRoot

Write-Host "=== Gemini CLI Bootstrap (Windows/PS7) ==="
Write-Host "Workspace: $RepoRoot"

Write-Host "[1/2] Validiere Gemini-Setup-Struktur..."
pwsh Gemini-Setup/scripts/validate-gemini-setup.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Error "❌ Validierung fehlgeschlagen."
    exit 1
}

Write-Host "[2/2] Generiere Bootstrap-Report..."
node Gemini-Setup/scripts/bootstrap-report.mjs

Write-Host "[3/4] Führe Cross-CLI Intelligenz-Audit aus..."
pwsh Gemini-Setup/scripts/session-start-audit.ps1

Write-Host "[4/4] Führe Plugin-Health-Check aus..."
pwsh Gemini-Setup/scripts/plugin-health-check.ps1

Write-Host "[5/5] Starte inkrementelle Reindexierung (Hintergrund)..."
pwsh Gemini-Setup/hooks/reindex-codebase.ps1

Write-Host "---"
Write-Host "Gemini CLI Bootstrap abgeschlossen."
