# consolidate-whiteboard-sections.ps1 (Gemini) ÔÇö Raeumt das Whiteboard auf
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
$WhiteboardPath = "$RepoRoot\Gemini-Setup\agent-memory\shared\MEMORY.md"

Write-Host "=== ­ƒÆí Gemini Whiteboard Consolidator ===" -ForegroundColor Cyan

if (-not (Test-Path $WhiteboardPath)) {
    Write-Host "ÔØî Whiteboard nicht gefunden." -ForegroundColor Red
    exit 1
}

$Content = Get-Content $WhiteboardPath -Raw
$Sections = @{}
$CurrentSection = "Header"
$Lines = $Content -split "`n"

# 1. Zerlege Whiteboard in Sektionen
foreach ($line in $Lines) {
    if ($line -match "^##\s+(.*)") {
        $CurrentSection = $Matches[1].Trim()
        if (-not $Sections.ContainsKey($CurrentSection)) {
            $Sections[$CurrentSection] = @()
        }
    } else {
        if (-not $Sections.ContainsKey($CurrentSection)) { $Sections[$CurrentSection] = @() }
        $Sections[$CurrentSection] += $line
    }
}

# 2. Baue Whiteboard neu zusammen (sortiert und konsolidiert)
$NewContent = ""
# Header zuerst
$NewContent += ($Sections["Header"] -join "`n").Trim() + "`n`n"

$OrderedSections = @(
    "Oberste Direktive",
    "Workspace Orchestration",
    "Systemzustand",
    "Offene Fehler & Probleme",
    "Performance & Optimierung",
    "Architektur-Entscheidungen",
    "Debugging-Muster",
    "Forschung & Intelligence",
    "Erkenntnisse aus Code Reviews",
    "Erkenntnisse aus Tests",
    "UI/UX-Patterns",
    "Regeln & Konventionen"
)

foreach ($sec in $OrderedSections) {
    if ($Sections.ContainsKey($sec)) {
        $NewContent += "## $sec`n"
        # Entferne Platzhalter-Texte wenn echte Eintraege da sind
        $FilteredLines = $Sections[$sec] | Where-Object { $_ -notmatch "_Noch keine Eintraege._" }
        if ($FilteredLines.Count -eq 0 -or ($FilteredLines -join "").Trim() -eq "") {
            $NewContent += "_Noch keine Eintraege._`n`n"
        } else {
            $NewContent += ($FilteredLines -join "`n").Trim() + "`n`n"
        }
        $Sections.Remove($sec)
    }
}

# Verbleibende (unbekannte) Sektionen am Ende anf├╝gen
foreach ($sec in $Sections.Keys) {
    if ($sec -ne "Header") {
        $NewContent += "## $sec`n"
        $NewContent += ($Sections[$sec] -join "`n").Trim() + "`n`n"
    }
}

# 3. Speichern
Set-Content $WhiteboardPath $NewContent.Trim() -Encoding UTF8
Write-Host "Ô£à Whiteboard konsolidiert und Sektionen sortiert." -ForegroundColor Green
