# Config Guard: Verifies protected settings after any config change
# Hook event: PostToolUse (Edit|Write on settings.json)
# Platform: Windows (PowerShell 7+)
#
# Output: JSON for hook compatibility.
#   Block:   {"error":"CONFIG-GUARD: BLOCKIERT — ..."}
#   Warning: plain text (PostToolUse context)
#   OK:      no output

. "$PSScriptRoot/hook-log.ps1"

# Read hook input from stdin to check if the edited file is settings.json
$hookInput = [Console]::In.ReadToEnd()
try {
    $hookJson = $hookInput | ConvertFrom-Json
} catch {
    # Could not parse hook input — not our concern, allow
    exit 0
}

# Only check when settings.json was edited
$toolInput = $hookJson.tool_input
if (-not $toolInput) { exit 0 }

$filePath = $toolInput.file_path
if (-not $filePath) {
    # Write tool uses file_path, Edit also uses file_path
    $filePath = $toolInput.path
}
if (-not $filePath) { exit 0 }

# Normalize path separators for comparison
$normalizedPath = $filePath -replace '\\', '/'
if ($normalizedPath -notmatch '\.claude/settings\.json$' -and $normalizedPath -notmatch '\.claude\\settings\.json$') {
    exit 0
}

$Settings = Join-Path $env:USERPROFILE ".claude" "settings.json"
if (-not (Test-Path $Settings)) {
    Hook-LogWarn "settings.json not found"
    exit 0
}

try {
    $data = Get-Content $Settings -Raw | ConvertFrom-Json
} catch {
    Hook-LogError "failed to parse settings.json: $_"
    exit 0
}

$blocks = @()

# defaultMode: MUST be "bypassPermissions" — BLOCK anything else (user requirement)
$perms = $data.permissions
if ($perms) {
    $defMode = $perms.defaultMode
    if ($defMode -and $defMode -ne 'bypassPermissions') {
        $blocks += "defaultMode=$defMode (MUSS 'bypassPermissions' sein — Benutzer-Regel)"
    }
}

# effortLevel: MUST be "high" — BLOCK anything else (CLAUDE.md requirement)
$eff = $data.effortLevel
if ($eff -and $eff -ne 'high') {
    $blocks += "effortLevel=$eff (MUSS 'high' sein — CLAUDE.md-Regel)"
}

$envData = $data.env
if ($envData) {
    # CLAUDE_CODE_EFFORT_LEVEL in env
    $envEff = $envData.CLAUDE_CODE_EFFORT_LEVEL
    if ($envEff -and $envEff -ne 'high') {
        $blocks += "CLAUDE_CODE_EFFORT_LEVEL=$envEff (MUSS 'high' sein)"
    }

    # SUBAGENT_MODEL: BLOCK if changed from sonnet (critical for cost/quality)
    $subModel = $envData.CLAUDE_CODE_SUBAGENT_MODEL
    if ($subModel -and $subModel -ne 'sonnet') {
        $blocks += "CLAUDE_CODE_SUBAGENT_MODEL=$subModel (erwartet: sonnet)"
    }

    # AUTOCOMPACT: BLOCK if below 95
    $acp = $envData.CLAUDE_AUTOCOMPACT_PCT_OVERRIDE
    if ($acp -and [int]$acp -lt 95) {
        $blocks += "AUTOCOMPACT=$acp (minimum: 95)"
    }
}

if ($blocks.Count -gt 0) {
    $blockMsg = $blocks -join "; "
    Hook-LogError "BLOCKED — protected settings changed: $blockMsg"
    # Output valid JSON for hook compatibility
    $jsonOutput = @{ error = "CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: $blockMsg" } | ConvertTo-Json -Compress
    Write-Output $jsonOutput
    exit 2
}

Hook-Log "all protected settings intact"
exit 0
