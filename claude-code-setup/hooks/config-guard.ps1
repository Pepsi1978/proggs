# Config Guard: Verifies protected settings after any config change
# Hook event: PostToolUse (Edit|Write on settings.json), ConfigChange, Stop
# Platform: Windows (PowerShell 7+)
#
# ROBUSTNESS: Entire script wrapped in try/catch. Any failure → exit 0 (allow).
#
# Output: JSON for hook compatibility.
#   Block:   {"error":"CONFIG-GUARD: BLOCKIERT — ..."}
#   Warning: plain text (PostToolUse context)
#   OK:      no output

$ErrorActionPreference = 'SilentlyContinue'

try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

# Read hook input from stdin to check if the edited file is settings.json
try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Length -lt 5) { exit 0 }
    $hookJson = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
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

# Normalize path separators for comparison (backslash → forward slash)
$normalizedPath = $filePath -replace '\\', '/'
if ($normalizedPath -notmatch '\.claude/settings(\.local)?\.json$') {
    exit 0
}

# ================================================
# DEFENSE: Auto-remove allow lists from ALL settings files
# ================================================
# Claude Code auto-creates allow entries when the user approves a tool.
# These act as whitelists and break bypassPermissions. Remove immediately.

function Remove-AllowListSilent {
    param([string]$Path)
    try {
        if (Test-Path $Path) {
            $raw = Get-Content $Path -Raw -Encoding UTF8
            $parsed = $raw | ConvertFrom-Json
            if ($null -ne $parsed.permissions.allow) {
                $hash = $raw | ConvertFrom-Json -AsHashtable
                $hash["permissions"].Remove("allow")
                $tmpFile = "$Path.tmp"
                ($hash | ConvertTo-Json -Depth 20) | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
                Move-Item -Path $tmpFile -Destination $Path -Force
                Hook-Log "AUTO-FIX: removed allow list from $(Split-Path $Path -Leaf)"
                return $true
            }
        }
    } catch {}
    return $false
}

$settingsPath2 = Join-Path $env:USERPROFILE ".claude" "settings.json"
$localSettingsPath2 = Join-Path $env:USERPROFILE ".claude" "settings.local.json"

$allowFixed = $false
if (Remove-AllowListSilent -Path $settingsPath2) { $allowFixed = $true }
if (Remove-AllowListSilent -Path $localSettingsPath2) { $allowFixed = $true }

# Also clean project-level settings
try {
    $projectsDir2 = Join-Path $env:USERPROFILE ".claude" "projects"
    if (Test-Path $projectsDir2) {
        foreach ($pd in (Get-ChildItem $projectsDir2 -Directory)) {
            if (Remove-AllowListSilent -Path (Join-Path $pd.FullName "settings.json")) { $allowFixed = $true }
            if (Remove-AllowListSilent -Path (Join-Path $pd.FullName "settings.local.json")) { $allowFixed = $true }
        }
    }
} catch {}

# Now check the edited file for other violations
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
    if ($null -ne $defMode -and $defMode -ne '' -and $defMode -ne 'bypassPermissions') {
        $blocks += "defaultMode=$defMode (MUSS 'bypassPermissions' sein — Benutzer-Regel)"
    }
}

# effortLevel: User-controlled — only warn if set to something unexpected, never block.
# The user can switch between "high" and "medium" freely via /effort or settings.
$eff = $data.effortLevel
$validEfforts = @('high', 'medium', 'low')
if ($null -ne $eff -and $eff -ne '' -and $eff -notin $validEfforts) {
    $blocks += "effortLevel=$eff (ungueltiger Wert — erlaubt: high, medium, low)"
}

$envData = $data.env
if ($envData) {
    # CLAUDE_CODE_EFFORT_LEVEL in env — same as above, user-controlled
    $envEff = $envData.CLAUDE_CODE_EFFORT_LEVEL
    if ($null -ne $envEff -and $envEff -ne '' -and $envEff -notin $validEfforts) {
        $blocks += "CLAUDE_CODE_EFFORT_LEVEL=$envEff (ungueltiger Wert)"
    }

    # SUBAGENT_MODEL: BLOCK if changed from sonnet (critical for cost/quality)
    $subModel = $envData.CLAUDE_CODE_SUBAGENT_MODEL
    if ($null -ne $subModel -and $subModel -ne '' -and $subModel -ne 'sonnet') {
        $blocks += "CLAUDE_CODE_SUBAGENT_MODEL=$subModel (erwartet: sonnet)"
    }

    # AUTOCOMPACT: BLOCK if below 95 (value "0" is a real value — must not be falsy-skipped)
    $acp = $envData.CLAUDE_AUTOCOMPACT_PCT_OVERRIDE
    if ($null -ne $acp -and $acp -ne '' -and [int]$acp -lt 95) {
        $blocks += "AUTOCOMPACT=$acp (minimum: 95)"
    }
}

if ($blocks.Count -gt 0) {
    $blockMsg = $blocks -join "; "
    Hook-LogError "BLOCKED — protected settings changed: $blockMsg"
    # Log to whiteboard — wrapped in try/catch so exit 2 is always reached
    try {
        $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: config-guard.ps1 — Settings-Aenderung blockiert: $blockMsg"
        Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    } catch { }
    # Output valid JSON for hook compatibility
    $jsonOutput = @{ error = "CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: $blockMsg" } | ConvertTo-Json -Compress
    Write-Output $jsonOutput
    exit 2
}

Hook-Log "all protected settings intact"
exit 0
