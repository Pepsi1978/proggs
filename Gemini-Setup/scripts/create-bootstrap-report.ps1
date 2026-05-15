# create-bootstrap-report.ps1 (Gemini)
$RepoRoot = "C:\Users\barwa\GeminiCLI"
Set-Location -Path $RepoRoot

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "   GEMINI CLI SYSTEM BOOTSTRAP REPORT" -ForegroundColor Cyan
Write-Host "   Generiert am: $(Get-Date -Format 'dd.MM.yyyy HH:mm:ss')" -ForegroundColor Gray
Write-Host "===============================================================" -ForegroundColor Cyan

$GlobalHealth = $true

# 1. Runtime
Write-Host ""
Write-Host "[1] Laufzeit-Umgebung" -ForegroundColor White
$pwshCheck = powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/check-pwsh-availability.ps1"
if ($LASTEXITCODE -eq 0) {
    Write-Host "  OK: PowerShell 7+ (pwsh) ist verfuegbar." -ForegroundColor Green
} else {
    Write-Host "  FAIL: PowerShell 7+ fehlt oder ist fehlerhaft." -ForegroundColor Red
    $GlobalHealth = $false
}

# 2. Files
Write-Host ""
Write-Host "[2] Setup-Integritaet" -ForegroundColor White
$required = @("AGENTS.md", ".mcp.json", "Gemini-Setup/rules/global.md", "Gemini-Setup/agent-memory/shared/MEMORY.md")
foreach ($f in $required) {
    if (Test-Path $f) {
        Write-Host "  OK: $f gefunden." -ForegroundColor Green
    } else {
        Write-Host "  FAIL: $f fehlt!" -ForegroundColor Red
        $GlobalHealth = $false
    }
}

# 3. Hooks
Write-Host ""
Write-Host "[3] Git-Schutzmechanismen" -ForegroundColor White
if (Test-Path ".git/hooks/post-merge") {
    Write-Host "  OK: Post-Merge Hook (MCP-Reparatur) ist installiert." -ForegroundColor Green
} else {
    Write-Host "  WARN: Post-Merge Hook fehlt." -ForegroundColor Yellow
}

# 4. Search
Write-Host ""
Write-Host "[4] Semantische Code-Suche (Ollama)" -ForegroundColor White
if (Test-Path "Gemini-Setup/scripts/check-code-search-health.mjs") {
    $searchHealth = node "Gemini-Setup/scripts/check-code-search-health.mjs" | Out-String
    if ($searchHealth -match "Such-Status: ok") {
        Write-Host "  OK: Suche ist aktiv und gesund." -ForegroundColor Green
    } else {
        Write-Host "  FAIL: Suche meldet Probleme (Ollama gestartet?)" -ForegroundColor Red
        $GlobalHealth = $false
    }
} else {
    Write-Host "  WARN: check-code-search-health.mjs nicht gefunden." -ForegroundColor Yellow
}

# 5. Bridges
Write-Host ""
Write-Host "[5] Bruecken-Status (Cross-CLI)" -ForegroundColor White
if (Test-Path "Gemini-Setup/PORTING-LIST.md") {
    Write-Host "  OK: Eigener Briefkasten (Export) ist bereit." -ForegroundColor Green
}
if (Test-Path "claude-code-setup/PORTING-LIST.md") {
    Write-Host "  OK: Claudes Briefkasten ist erreichbar." -ForegroundColor Green
} else {
    Write-Host "  INFO: Claudes Briefkasten ist leer/nicht da." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
if ($GlobalHealth) {
    Write-Host "   SYSTEMSTATUS: OPTIMAL" -ForegroundColor Green
    Write-Host "   Alle Intelligenz-Hebel sind scharfgeschaltet." -ForegroundColor Gray
} else {
    Write-Host "   SYSTEMSTATUS: EINGESCHRAENKT" -ForegroundColor Yellow
    Write-Host "   Bitte die oben genannten Fehler beheben." -ForegroundColor Gray
}
Write-Host "===============================================================" -ForegroundColor Cyan
