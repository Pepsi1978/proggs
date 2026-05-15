# check-for-duplicate-suggestions.ps1 (Gemini) ÔÇö Verhindert doppelte Vorschlaege (Fuzzy Edition)
param (
    [string]$SuggestionId,
    [string]$Summary = ""
)

$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }

$WhiteboardPath = "$RepoRoot\Gemini-Setup\agent-memory\shared\MEMORY.md"
$LedgerPath = "$RepoRoot\Gemini-Setup\state\implemented-intelligence-suggestions.json"

# 1. Exakte ID-Pruefung (Whiteboard & Ledger)
if (Test-Path $WhiteboardPath) {
    $wbContent = Get-Content $WhiteboardPath -Raw
    if ($wbContent -match [regex]::Escape($SuggestionId)) { exit 1 }
}
if (Test-Path $LedgerPath) {
    $ledgerContent = Get-Content $LedgerPath -Raw
    if ($ledgerContent -match [regex]::Escape($SuggestionId)) { exit 2 }
}

# 2. Schlaue Inhalts-Pruefung (Fuzzy)
if ($Summary) {
    # Extrahiere Kernbegriffe (laenger als 4 Zeichen, keine Standardfuellwoerter)
    $Keywords = $Summary.Split(' ') | Where-Object { $_.Length -gt 4 } | ForEach-Object { $_.Trim(',.;') }
    
    if ($Keywords) {
        $SearchPattern = ($Keywords | ForEach-Object { [regex]::Escape($_) }) -join '|'
        
        # Pruefe im Whiteboard nach Haeufung von Kernbegriffen
        if (Test-Path $WhiteboardPath) {
            $Matches = [regex]::Matches($wbContent, $SearchPattern, "IgnoreCase")
            if ($Matches.Count -gt 3) { exit 3 } # Zu viele Treffer -> Wahrscheinlich Dublette
        }
    }
}

exit 0 # Sauber
