# validate-gemini-setup.ps1
$RepoRoot = "C:\Users\barwa\GeminiCLI"
cd $RepoRoot

Write-Host "=== Gemini Setup Validierung (PS7) ==="

$requiredFiles = @(
    "Gemini-Setup/rules/global.md",
    "Gemini-Setup/rules/SUPERINTELLIGENZ.md",
    "Gemini-Setup/rules/SELBSTBEOBACHTUNG.md",
    "Gemini-Setup/rules/RESILIENT_BUGFIXING.md",
    "Gemini-Setup/agent-memory/shared/MEMORY.md",
    "Gemini-Setup/bridges/bridge-registry.json",
    "Gemini-Setup/bridges/codex-delta-bridge.json",
    "Gemini-Setup/state/environment-fixes.json",
    "Gemini-Setup/state/implemented-intelligence-suggestions.json",
    "Gemini-Setup/scripts/bridge-registry.mjs",
    "Gemini-Setup/scripts/bootstrap-report.mjs"
)

$missing = 0
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        Write-Host "❌ Fehlt: $file" -ForegroundColor Red
        $missing++
    } else {
        Write-Host "✅ Vorhanden: $file" -ForegroundColor Green
    }
}

if ($missing -eq 0) {
    Write-Host "---"
    Write-Host "🚀 Alle Gemini-Setup-Komponenten sind vorhanden."
    exit 0
} else {
    Write-Host "---"
    Write-Host "⚠️ $missing Komponenten fehlen."
    exit 1
}
