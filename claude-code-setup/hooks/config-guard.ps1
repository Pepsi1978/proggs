# Config Guard: Verifies protected settings after any config change
# Hook event: PostToolUse (Edit|Write on settings.json)
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"

$Settings = Join-Path $env:USERPROFILE ".claude" "settings.json"
if (-not (Test-Path $Settings)) {
    Hook-LogWarn "settings.json not found"
    Write-Output "Config-Guard: settings.json nicht gefunden."
    exit 0
}

try {
    $data = Get-Content $Settings -Raw | ConvertFrom-Json
} catch {
    Hook-LogError "failed to parse settings.json: $_"
    Write-Output "Config-Guard: settings.json konnte nicht gelesen werden."
    exit 0
}

$warnings = @()
$blocks = @()

# effortLevel: MUST be "high" — BLOCK anything else (CLAUDE.md requirement)
$eff = $data.effortLevel
if ($eff -and $eff -ne 'high') {
    $blocks += "effortLevel=$eff (MUSS 'high' sein — CLAUDE.md-Regel)"
}

$env = $data.env
if ($env) {
    # CLAUDE_CODE_EFFORT_LEVEL in env
    $envEff = $env.CLAUDE_CODE_EFFORT_LEVEL
    if ($envEff -and $envEff -ne 'high') {
        $blocks += "CLAUDE_CODE_EFFORT_LEVEL=$envEff (MUSS 'high' sein)"
    }

    # SUBAGENT_MODEL: BLOCK if changed from sonnet (critical for cost/quality)
    $subModel = $env.CLAUDE_CODE_SUBAGENT_MODEL
    if ($subModel -and $subModel -ne 'sonnet') {
        $blocks += "CLAUDE_CODE_SUBAGENT_MODEL=$subModel (erwartet: sonnet)"
    }

    # AUTOCOMPACT: BLOCK if below 95
    $acp = $env.CLAUDE_AUTOCOMPACT_PCT_OVERRIDE
    if ($acp -and [int]$acp -lt 95) {
        $blocks += "AUTOCOMPACT=$acp (minimum: 95)"
    }
}

if ($blocks.Count -gt 0) {
    $blockMsg = $blocks -join "; "
    Hook-LogError "BLOCKED — protected settings changed: $blockMsg"
    Write-Output "CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: $blockMsg"
    exit 1
}

if ($warnings.Count -gt 0) {
    $warnMsg = $warnings -join "; "
    Hook-LogWarn "unexpected setting change: $warnMsg"
    Write-Output "CONFIG-GUARD: WARNUNG — Unerwartete Setting-Aenderung: $warnMsg"
    Write-Output "Hinweis: Beim naechsten Session-Start wird automatisch auf 'medium' zurueckgesetzt."
    exit 0
}

Hook-Log "all protected settings intact"
exit 0
