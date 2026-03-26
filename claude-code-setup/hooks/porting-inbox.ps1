# DEPRECATED: Replaced by mirror-check.ps1 + import agent (Universal Mirror Bridge, 2026-03-25)
# This hook is no longer registered in settings.json.
# Kept for reference only — remove after 2026-04-25.
# Adapted from MIRROR-2026-03-25-MAC-006 by import agent on 2026-03-26
exit 0
# --- Original code below (inactive) ---

# porting-inbox.ps1 — Session-Start Hook: Reads PORTING-LIST.md from other CLIs
# Extracts new suggestions into the whiteboard as TODOs
# Part of the Universal Bridge Protocol (cross-CLI intelligence sharing)
# Platform: Windows (PowerShell 7+)
# ROBUSTNESS: Non-critical hook. Any failure -> exit 0 silently.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

$RepoRoot = Join-Path $env:USERPROFILE "proggs"
$WhiteboardFile = Join-Path $RepoRoot ".claude" "agent-memory" "shared" "MEMORY.md"
$LedgerFile = Join-Path $env:USERPROFILE ".claude" "state" "porting-ledger.json"

# Ensure ledger exists
if (-not (Test-Path $LedgerFile)) {
    $dir = Split-Path $LedgerFile -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    '{"ported":[]}' | Out-File -FilePath $LedgerFile -Encoding utf8
}

# Read ledger (list of already-ported suggestion IDs)
try {
    $ledger = Get-Content $LedgerFile -Raw -Encoding UTF8 | ConvertFrom-Json
} catch {
    $ledger = @{ ported = @() }
}
$portedIds = @($ledger.ported)

# Define partner porting lists
$partners = @(
    @{ name = "Gemini"; path = Join-Path $RepoRoot "Gemini-Setup" "PORTING-LIST.md" },
    @{ name = "Codex"; path = Join-Path $RepoRoot "codex-setup" "PORTING-LIST.md" }
)

$newCount = 0

foreach ($partner in $partners) {
    if (-not (Test-Path $partner.path)) { continue }

    $content = Get-Content $partner.path -Raw -Encoding UTF8
    if (-not $content) { continue }

    # Extract suggestion blocks: ### [ID] followed by content until next ### or ## or end
    $matches = [regex]::Matches($content, '###\s+\[(.*?)\]([\s\S]*?)(?=###|## |\z)')

    foreach ($m in $matches) {
        $id = $m.Groups[1].Value.Trim()
        $body = $m.Groups[2].Value.Trim()

        # Duplicate check 1: Exact ID match in ledger
        if ($portedIds -contains $id) { continue }

        # Duplicate check 2: Check if ID already mentioned in whiteboard (fuzzy)
        if (Test-Path $WhiteboardFile) {
            $wbContent = Get-Content $WhiteboardFile -Raw -Encoding UTF8
            if ($wbContent -match [regex]::Escape($id)) { continue }

            # Duplicate check 3: Fuzzy — check if first line of body is already in whiteboard
            $firstLine = ($body -split "`n")[0].Trim()
            if ($firstLine.Length -gt 20) {
                $shortMatch = $firstLine.Substring(0, [Math]::Min(40, $firstLine.Length))
                if ($wbContent -match [regex]::Escape($shortMatch)) { continue }
            }
        }

        # New suggestion — write to whiteboard
        $ts = Get-Date -Format "yyyy-MM-dd HH:mm"
        $entry = "### $ts — Portierungs-Vorschlag von $($partner.name): [$id]`n$body"
        try {
            Insert-WhiteboardEntry -Section "Forschung & Intelligence" -Entry $entry
            $newCount++
            $portedIds += $id
            Hook-Log "Porting inbox: new suggestion [$id] from $($partner.name)"
        } catch {
            Hook-LogWarn "Porting inbox: failed to insert [$id]: $_"
        }
    }
}

# Update ledger
try {
    $ledger.ported = $portedIds
    $ledger | ConvertTo-Json -Depth 3 | Out-File -FilePath $LedgerFile -Encoding utf8 -Force
} catch { }

if ($newCount -gt 0) {
    Write-Output "Porting-Inbox: $newCount neue Vorschlaege von Partner-CLIs ins Whiteboard uebernommen."
} else {
    Hook-Log "Porting inbox: no new suggestions"
}

exit 0
