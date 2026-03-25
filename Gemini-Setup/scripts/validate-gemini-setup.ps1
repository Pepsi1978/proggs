# validate-gemini-setup.ps1 (Gemini) — Gesamt-Validierung der Umgebung
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
cd $RepoRoot

Write-Host "=== 💡 Gemini Setup Validierung (PS7) ===" -ForegroundColor Cyan

# 1. Dateipruefung
$requiredFiles = @(
    "Gemini-Setup/rules/global.md",
    "Gemini-Setup/rules/SUPERINTELLIGENZ.md",
    "Gemini-Setup/rules/SELBSTBEOBACHTUNG.md",
    "Gemini-Setup/rules/RESILIENT_BUGFIXING.md",
    "Gemini-Setup/agent-memory/shared/MEMORY.md",
    "Gemini-Setup/bridges/bridge-registry.json",
    "Gemini-Setup/state/environment-fixes.json",
    "Gemini-Setup/state/implemented-intelligence-suggestions.json",
    "Gemini-Setup/scripts/check-rule-parity.ps1"
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

# 2. Regel-Konsistenz-Check
Write-Host "`n--- Starte Regel-Konsistenz-Pruefung ---" -ForegroundColor Yellow
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/check-rule-parity.ps1"
$parityExitCode = $LASTEXITCODE

# 3. Stale-Rule-Cleanup-Check (Geister-Begriffe)
Write-Host "`n--- Starte Cleanup-Pruefung (Stale Rules) ---" -ForegroundColor Yellow
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/check-for-stale-rules.ps1"
$staleExitCode = $LASTEXITCODE

# 4. Git-Hook Installation
Write-Host "`n--- Installiere/Pruefe Git-Hooks ---" -ForegroundColor Yellow
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/auto-install-git-hooks.ps1"
$hooksExitCode = $LASTEXITCODE

if ($parityExitCode -ne 0) {
    Write-Host "`n💡 Inkonsistenzen in den Regeln gefunden!" -ForegroundColor Yellow
    if ([Environment]::UserInteractive) {
        $Choice = Read-Host "Soll ich den Sync-Helper (sync-rule-parity.ps1) jetzt starten? (j/n)"
        if ($Choice -eq 'j') {
            powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/sync-rule-parity.ps1"
            # Nach Sync nochmals prüfen
            powershell.exe -NoProfile -ExecutionPolicy Bypass -File "Gemini-Setup/scripts/check-rule-parity.ps1"
            $parityExitCode = $LASTEXITCODE
        }
    }
}

if ($missing -eq 0 -and $parityExitCode -eq 0) {
    Write-Host "`n🚀 Alle Gemini-Setup-Komponenten sind vorhanden und konsistent." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️ Validierung fehlgeschlagen ($missing Dateien fehlen, Parity-Status: $parityExitCode)." -ForegroundColor Red
    exit 1
}
