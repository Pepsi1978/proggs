# check-for-stale-rules.ps1 (Gemini) ÔÇö Suche nach Resten archivierter Regeln
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
$ArchiveDir = "$RepoRoot\Gemini-Setup\agent-memory\archive\rules"

Write-Host "=== ­ƒÆí Gemini Stale Rule Cleanup-Check ===" -ForegroundColor Cyan

if (-not (Test-Path $ArchiveDir)) {
    Write-Host "Ô£à Kein Regel-Archiv gefunden (nichts zu tun)." -ForegroundColor Green
    exit 0
}

$ArchiveFiles = Get-ChildItem $ArchiveDir -Filter "*.md" | Where-Object { $_.Name -ne "README.md" }
if ($ArchiveFiles.Count -eq 0) {
    Write-Host "Ô£à Archiv ist leer (keine veralteten Regeln bekannt)." -ForegroundColor Green
    exit 0
}

$PatternsToSearch = @()
foreach ($file in $ArchiveFiles) {
    # Zeilenweise einlesen fuer bessere Kodierungs-Kompatibilitaet
    $lines = Get-Content $file.FullName
    foreach ($line in $lines) {
        if ($line -like "*Stale-Patterns:*") {
            $rawList = $line -replace ".*Stale-Patterns:\*\*\s*", ""
            $rawPatterns = $rawList.Replace('"', '').Split(',')
            foreach ($p in $rawPatterns) {
                $trimmed = $p.Trim()
                if ($trimmed) { $PatternsToSearch += $trimmed }
            }
        }
    }
}

if ($PatternsToSearch.Count -eq 0) {
    Write-Host "ÔÜá´©Å  Keine Suchmuster im Archiv definiert." -ForegroundColor Yellow
    exit 0
}

Write-Host "`nSuche nach folgenden 'Geister-Begriffen': $($PatternsToSearch -join ', ')" -ForegroundColor Gray
$FoundAny = $false

foreach ($pattern in $PatternsToSearch) {
    Write-Host "  Pruefe: $pattern..." -ForegroundColor Gray
    # Wir nutzen git grep fuer Geschwindigkeit und ignorieren das Archiv und State-Files
    $Results = git grep -i "$pattern" -- . ":(exclude)Gemini-Setup/agent-memory/archive/*" ":(exclude)**/state/*.json"
    
    if ($Results) {
        Write-Host "  ÔØî FUNDE fuer '$pattern':" -ForegroundColor Red
        foreach ($line in $Results) {
            Write-Host "    $line" -ForegroundColor DarkGray
        }
        $FoundAny = $true
    }
}

if ($FoundAny) {
    Write-Host "`nÔÜá´©Å  Inkonsistenz: Es wurden noch Reste archivierter Regeln gefunden!" -ForegroundColor Red
    Write-Host "  Bitte die oben genannten Dateien manuell bereinigen."
    exit 1
} else {
    Write-Host "`nÔ£¿ Das System ist frei von Geister-Begriffen aus dem Archiv." -ForegroundColor Green
    exit 0
}
