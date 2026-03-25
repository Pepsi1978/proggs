# mirror-check.ps1 — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.

$ErrorActionPreference = "SilentlyContinue"

$Ledger = Join-Path $env:USERPROFILE "proggs\claude-code-setup\mirror-ledger.md"

# Bail if ledger doesn't exist yet
if (-not (Test-Path $Ledger)) { exit 0 }

$platform = "windows"
$cli = "claude-code"

# Count PENDING entries for this platform/cli
$pattern = "APPLIED: ${platform}/${cli}=PENDING"
$count = (Select-String -Pattern ([regex]::Escape($pattern)) -Path $Ledger -SimpleMatch | Measure-Object).Count

if ($count -gt 0) {
    Write-Output "Mirror-Bridge: $count ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
}

exit 0
