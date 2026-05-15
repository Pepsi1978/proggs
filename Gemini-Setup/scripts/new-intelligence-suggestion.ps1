# new-intelligence-suggestion.ps1 (Gemini) ÔÇö Gefuehrter Prozess fuer Intelligenz-Vorschlaee
param (
    [string]$State,
    [switch]$Json
)

$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
$RegisterScript = "$RepoRoot\Gemini-Setup\scripts\register-intelligence-suggestion.mjs"

$Fields = @(
    @{ key = "id"; prompt = "Vorschlags-ID (z.B. SUGG-2026-03-24-name)" },
    @{ key = "summary"; prompt = "Kurzfassung (Einfache Sprache)" },
    @{ key = "proposal"; prompt = "Originaler Vorschlags-Text" },
    @{ key = "context"; prompt = "Kontext fuer andere CLIs (Warum ist das nuetzlich?)" },
    @{ key = "suggested-because"; prompt = "Warum wurde es vorgeschlagen?" },
    @{ key = "implemented-because"; prompt = "Warum wurde es umgesetzt?" },
    @{ key = "implementation"; prompt = "Wie wurde es konkret umgesetzt? (Dateien/Logik)" },
    @{ key = "bridge-value"; prompt = "Wert fuer die Bruecke (Hebel)" },
    @{ key = "adoption-guidance"; prompt = "Portierungshinweis fuer Claude/Codex" },
    @{ key = "resilience-summary"; prompt = "Resilienz-Check (Warum bleibt das stabil?)" }
)

$EntryData = @{}

Write-Host "=== ­ƒÆí Gefuehrter Intelligenz-Vorschlag (Gemini CLI) ===" -ForegroundColor Cyan
Write-Host "Bereite diesen Vorschlag so vor, dass andere CLIs ihn leicht portieren koennen.`n" -ForegroundColor Gray

foreach ($field in $Fields) {
    $val = ""
    while ([string]::IsNullOrWhiteSpace($val)) {
        $val = Read-Host "$($field.prompt)"
    }
    $EntryData[$field.key] = $val

    # Proaktiver Check nach ID und Kurzfassung (Summary)
    if ($field.key -eq "summary") {
        powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$RepoRoot\Gemini-Setup\scripts\check-for-duplicate-suggestions.ps1" -SuggestionId $EntryData["id"] -Summary $EntryData["summary"]
        $checkStatus = $LASTEXITCODE
        
        if ($checkStatus -ne 0) {
            Write-Host "`nÔÜá´©Å  ACHTUNG: Ein aehnlicher oder identischer Vorschlag wurde bereits gefunden!" -ForegroundColor Yellow
            if ($checkStatus -eq 1) { Write-Host "  -> ID bereits im Whiteboard vorhanden." -ForegroundColor Gray }
            if ($checkStatus -eq 2) { Write-Host "  -> ID bereits im Ledger (erfolgreich umgesetzt)." -ForegroundColor Gray }
            if ($checkStatus -eq 3) { Write-Host "  -> Inhaltlich sehr aehnlich zu einem bestehenden Eintrag." -ForegroundColor Gray }
            
            $Choice = Read-Host "`nMoechtest du trotzdem fortfahren? (j/n)"
            if ($Choice -ne 'j') {
                Write-Host "Abbruch durch Benutzer." -ForegroundColor Red
                exit 0
            }
        }
    }
}

$Arguments = @("run", "$RegisterScript", "add")
foreach ($key in $EntryData.Keys) {
    $Arguments += "--$key"
    $Arguments += $EntryData[$key]
}

if ($State) {
    $Arguments += "--state"
    $Arguments += $State
}

Write-Host "`nRegistriere Intelligenz-Vorschlag fuer die Bruecken..." -ForegroundColor Yellow
& bun @Arguments
