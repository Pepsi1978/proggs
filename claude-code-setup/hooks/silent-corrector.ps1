# silent-corrector.ps1 — PreToolUse Hook: Blocks known dangerous patterns
# Windows-Portierung von silent-corrector.sh (macOS)
# Intercepts Bash commands that violate project rules, with precise matching
# to avoid false positives on commands that merely MENTION the patterns.
#
# Corrections implemented:
#   1. cd into ~/Codex/ → BLOCK (most dangerous Codex operation)
#   2. sed as PRIMARY command on JSON → BLOCK (no-sed-on-json rule)
#
# Intentionally NOT implemented:
#   3. git push without fetch+rebase — handled by poka-yoke-git-push.ps1
#   4. Deep string analysis — would be too fragile, rules cover edge cases
#
# Hook protocol (PreToolUse):
#   - Receives tool invocation as JSON on stdin
#   - exit 0 = allow unchanged
#   - exit 2 + JSON stdout = block with reason
#
# Defense in Depth (Directive #3):
#   - $ErrorActionPreference = SilentlyContinue: never abort on error
#   - Fail-open: if uncertain, ALLOW (rules catch edge cases)
#   - ALWAYS exit 0 on unhandled paths
#   - Pattern matching via PowerShell regex for precision
#
# Source: Direktiven-Recherche 2026-04-06, Erkenntnis 3
# Poka-Yoke Stufe 2: Prevents the most dangerous known patterns

$ErrorActionPreference = 'SilentlyContinue'

$logFile = Join-Path $env:TEMP "silent-corrector.log"
$ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

function Log-Event {
    param([string]$Type, [string]$Message)
    try {
        Add-Content -Path $logFile -Value "[$ts] ${Type}: $Message" -Encoding UTF8
    } catch {}
}

# Read stdin
try {
    $input_text = $input | Out-String
    if (-not $input_text -or $input_text.Trim().Length -eq 0) {
        exit 0
    }

    $data = $input_text | ConvertFrom-Json
    if (-not $data) { exit 0 }

    $toolName = $data.tool_name
    if ($toolName -ne "Bash") { exit 0 }

    $cmd = $data.tool_input.command
    if (-not $cmd) { exit 0 }

    # Split command into sub-commands by && ; ||
    $parts = $cmd -split '\s*(?:&&|;|\|\|)\s*'

    foreach ($part in $parts) {
        $stripped = $part.Trim()
        if (-not $stripped) { continue }

        # CHECK 1: cd into Codex directory
        if ($stripped -match '^cd\s+.*[~/\\]Codex[/\\]?') {
            Log-Event "BLOCKED Codex-path" "command operated on ~/Codex/"
            Write-Output '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt (codex-directory-forbidden.md). Verwende ~/proggs/ statt ~/Codex/."}'
            exit 2
        }

        # CHECK 2: Direct file operations on Codex paths
        $fileOps = '^(ls|cat|rm|mv|cp|touch|mkdir|chmod|head|tail|wc|stat|file|readlink)\s+'
        if ($stripped -match $fileOps -and $stripped -match '[~/\\]Codex[/\\]') {
            Log-Event "BLOCKED Codex-path" "file operation on ~/Codex/"
            Write-Output '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt (codex-directory-forbidden.md). Verwende ~/proggs/ statt ~/Codex/."}'
            exit 2
        }

        # CHECK 3: sed as PRIMARY command targeting a .json file
        if ($stripped -match '^sed\s+' -and $stripped -match '\.json\b') {
            Log-Event "BLOCKED sed-on-JSON" "sed command targeting .json file"
            Write-Output '{"reason":"BLOCKIERT: sed auf JSON verboten (no-sed-on-json.md). Benutze Edit-Tool oder python3 json.load/dump."}'
            exit 2
        }
    }

    # All checks passed — allow through
    exit 0

} catch {
    # Fail-open: any error means allow through
    exit 0
}

# Fallback — should never reach here
exit 0
