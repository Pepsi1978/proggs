# mirror-check.ps1 — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.
# Adapted from MIRROR-2026-03-25-MAC-002 + MIRROR-2026-03-25-MAC-016 by import agent on 2026-03-26

$ErrorActionPreference = "SilentlyContinue"

$Ledger = Join-Path $env:USERPROFILE "proggs\claude-code-setup\mirror-ledger.md"

# Bail if ledger doesn't exist yet
if (-not (Test-Path $Ledger)) { exit 0 }

$platform = "windows"
$cli = "gemini"

# Count PENDING entries for this platform/cli (defensive — null-safe)
$pattern = "APPLIED: ${platform}/${cli}=PENDING"
$count = (Select-String -Pattern ([regex]::Escape($pattern)) -Path $Ledger -SimpleMatch -ErrorAction SilentlyContinue | Measure-Object).Count
if ($null -eq $count) { $count = 0 }

if ($count -gt 0) {
    Write-Output "Mirror-Bridge: $count ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
}

exit 0
