# auto-install-git-hooks.ps1 (Gemini) — Automatischer Git-Hook-Installer
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }

$SourceHooksDir = "$RepoRoot\Gemini-Setup\hooks"
$GitHooksDir = "$RepoRoot\.git\hooks"

Write-Host "=== 💡 Gemini Git-Hook Auto-Installer ===" -ForegroundColor Cyan

if (-not (Test-Path $GitHooksDir)) {
    Write-Host "❌ .git Verzeichnis nicht gefunden (kein Git-Repo?)" -ForegroundColor Red
    exit 1
}

# Liste der zu installierenden Hooks
# Falls die Datei im Repo eine Endung hat (z.B. post-merge.sh), 
# wird sie im .git Ordner ohne Endung gespeichert.
$HooksToInstall = @(
    "post-merge"
)

foreach ($hookName in $HooksToInstall) {
    $sourceFile = Join-Path $SourceHooksDir $hookName
    $targetFile = Join-Path $GitHooksDir $hookName

    if (Test-Path $sourceFile) {
        Write-Host "  Installing: $hookName..." -ForegroundColor Gray
        Copy-Item $sourceFile $targetFile -Force
        Write-Host "  ✅ $hookName erfolgreich installiert." -ForegroundColor Green
    } else {
        Write-Host "  ⚠️ Hook-Quelle fehlt: $sourceFile" -ForegroundColor Yellow
    }
}

Write-Host "`n✨ Git-Schutzmechanismen sind aktiv." -ForegroundColor Green
