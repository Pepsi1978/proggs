# check-rule-parity.ps1 (Gemini) ÔÇö Pruefung der Regel-Konsistenz
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }

$FilesToCheck = @(
    "$RepoRoot\Gemini-Setup\rules\global.md",
    "$RepoRoot\Gemini-Setup\rules\SUPERINTELLIGENZ.md",
    "$RepoRoot\Gemini-Setup\agent-memory\shared\MEMORY.md",
    "$RepoRoot\AGENTS.md"
)

$KeyConcepts = @(
    @{ name = "Oberste Direktive"; pattern = "Oberste[n]*\s+Direktive" },
    @{ name = "8 Intelligenz-Dimensionen"; pattern = "8\s+Intelligenz-Dimensionen|Wissensbreite,\s+Wissenstiefe" },
    @{ name = "Vorschlags-Regel (Einfache Sprache)"; pattern = "einfacher\s+Sprache|Vorschlags-Regel|max\.\s+6\s+Zeilen" },
    @{ name = "Compound Effect"; pattern = "Compound\s+Effect|Compound\s+Intelligence\s+Effect" }
)

Write-Host "=== ­ƒÆí Gemini Rule Parity Check ===" -ForegroundColor Cyan
$AllOk = $true

foreach ($concept in $KeyConcepts) {
    Write-Host "`nPruefe Konzept: $($concept.name)" -ForegroundColor Yellow
    foreach ($file in $FilesToCheck) {
        if (-not (Test-Path $file)) {
            Write-Host "  ÔÜá´©Å Datei fehlt: $file" -ForegroundColor Red
            $AllOk = $false
            continue
        }
        
        $content = Get-Content $file -Raw
        if ($content -match $concept.pattern) {
            Write-Host "  Ô£à OK: $(Split-Path $file -Leaf)" -ForegroundColor Green
        } else {
            Write-Host "  ÔØî FEHLT: $(Split-Path $file -Leaf)" -ForegroundColor Red
            $AllOk = $false
        }
    }
}

if ($AllOk) {
    Write-Host "`nÔ£à Alle Regeln sind konsistent verteilt." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nÔÜá´©Å Inkonsistenzen gefunden! Bitte Regeln synchronisieren." -ForegroundColor Red
    exit 1
}
