# sync-rule-parity.ps1 (Gemini) ÔÇö Automatischer Abgleich der Regeln
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }

$FilesToCheck = @(
    "$RepoRoot\Gemini-Setup\rules\global.md",
    "$RepoRoot\Gemini-Setup\rules\SUPERINTELLIGENZ.md",
    "$RepoRoot\Gemini-Setup\agent-memory\shared\MEMORY.md",
    "$RepoRoot\AGENTS.md"
)

# Definition der "Master"-Snippets fuer die Synchronisation
$RulesToSync = @(
    @{ 
        name = "Oberste Direktive"; 
        pattern = "Oberste[n]*\s+Direktive"; 
        snippet = "- Gemini CLI verfolgt als oberstes Ziel, seine Programmierintelligenz und die Intelligenz seiner gesamten Programmierumgebung von Session zu Session zu steigern."
    },
    @{ 
        name = "8 Intelligenz-Dimensionen"; 
        pattern = "8\s+Intelligenz-Dimensionen|Wissensbreite,\s+Wissenstiefe"; 
        snippet = "- Die 8 Intelligenz-Dimensionen: Wissensbreite, Wissenstiefe, Geschwindigkeit, Qualitaet, Autonomie, Voraussicht, Kreativitaet, Meta-Intelligenz."
    },
    @{ 
        name = "Vorschlags-Regel"; 
        pattern = "einfacher\s+Sprache|Vorschlags-Regel|max\.\s+6\s+Zeilen"; 
        snippet = "- Vorschlags-Regel: Intelligenz-Vorschlaege muessen in einfacher Sprache (max. 6 Zeilen) erklaert werden, inklusive der Begruendung fuer den Intelligenz-Gewinn."
    }
)

Write-Host "=== ­ƒÆí Gemini Rule Sync Helper ===" -ForegroundColor Cyan

foreach ($file in $FilesToCheck) {
    if (-not (Test-Path $file)) {
        Write-Host "ÔÜá´©Å  Datei fehlt: $file" -ForegroundColor Yellow
        continue
    }

    $content = Get-Content $file -Raw
    $modified = $false
    Write-Host "`nPruefe $file..." -ForegroundColor Gray

    foreach ($rule in $RulesToSync) {
        if ($content -notmatch $rule.pattern) {
            Write-Host "  Ô×ò Ergaenze: $($rule.name)" -ForegroundColor Green
            # Suche nach Sektionen zum Anhaengen oder einfach am Ende
            if ($content -match "## Regeln & Konventionen") {
                $content = $content -replace "## Regeln & Konventionen", "## Regeln & Konventionen`n$($rule.snippet)"
            } elseif ($content -match "## Oberste Direktiven") {
                 $content = $content -replace "## Oberste Direktiven.*", "$&`n$($rule.snippet)"
            } else {
                $content += "`n$($rule.snippet)"
            }
            $modified = $true
        }
    }

    if ($modified) {
        Set-Content $file $content -Encoding UTF8
        Write-Host "  Ô£à Datei aktualisiert." -ForegroundColor Green
    } else {
        Write-Host "  Ô£¿ Bereits synchron." -ForegroundColor DarkGray
    }
}

Write-Host "`n=== Synchronisation abgeschlossen ===" -ForegroundColor Cyan
