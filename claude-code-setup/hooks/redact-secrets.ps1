# redact-secrets.ps1 — PreToolUse hook: block Write/Edit if content contains secrets
# Hook event: PreToolUse (Write, Edit)
# Platform: Windows (PowerShell 7+)
#
# Strategy:
#   1. Intercept Write/Edit tool calls targeting sensitive files
#   2. Check content for known secret patterns
#   3. If secrets found: BLOCK the write and print a clear warning
#      Claude sees the block message and must redact before retrying.
#
# ROBUSTNESS: Entire script in try/catch. Any failure → exit 0 (allow).
# A broken hook must NEVER block legitimate work.

$ErrorActionPreference = 'SilentlyContinue'

try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Trim().Length -lt 5) { exit 0 }
    $json = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

$toolName  = $json.tool_name
$toolInput = $json.tool_input

# Only intercept Write and Edit tool calls
if ($toolName -notin @('Write', 'Edit')) { exit 0 }

# ── Determine file path and content ─────────────────────────────────────────
$filePath = ''
$content  = ''

if ($toolName -eq 'Write') {
    $filePath = $toolInput.file_path
    $content  = $toolInput.content
} elseif ($toolName -eq 'Edit') {
    $filePath = $toolInput.file_path
    # Check both old and new content in an Edit
    $content  = "$($toolInput.old_string)`n$($toolInput.new_string)"
}

if (-not $filePath -or -not $content) { exit 0 }

# ── Scope: only check sensitive config files ─────────────────────────────────
# We only guard files that are known to contain secrets (settings, config)
# and that end up in the git repo (claude-code-setup/ files).
$sensitivePatterns = @(
    'settings-reference\.json$',
    'settings\.json$',
    'settings\.local\.json$',
    'claude-code-setup[/\\].*\.json$',
    '\.claude[/\\].*settings.*\.json$'
)

$isSensitiveFile = $false
foreach ($pattern in $sensitivePatterns) {
    if ($filePath -match $pattern) {
        $isSensitiveFile = $true
        break
    }
}

if (-not $isSensitiveFile) { exit 0 }

# ── Secret patterns to detect ────────────────────────────────────────────────
$secretPatterns = @(
    'ghp_[A-Za-z0-9]{36,}',           # GitHub Personal Access Token
    'gho_[A-Za-z0-9]{36,}',           # GitHub OAuth Token
    'github_pat_[A-Za-z0-9_]{36,}',   # GitHub Fine-Grained PAT
    'ghs_[A-Za-z0-9]{36,}',           # GitHub App token
    'ghr_[A-Za-z0-9]{36,}',           # GitHub refresh token
    'sk-[A-Za-z0-9]{40,}',            # OpenAI / generic API key
    'xoxb-[0-9]+-[A-Za-z0-9-]+',      # Slack bot token
    'xoxp-[0-9]+-[A-Za-z0-9-]+',      # Slack user token
    'Bearer [A-Za-z0-9\-_.~+/]{40,}'  # Generic Bearer token
)

$foundSecrets = @()
foreach ($pattern in $secretPatterns) {
    $matches = [regex]::Matches($content, $pattern)
    foreach ($m in $matches) {
        $val = $m.Value
        # Truncate for display (don't show the full secret in logs)
        $preview = if ($val.Length -gt 16) { $val.Substring(0, 12) + '...' } else { $val }
        $foundSecrets += $preview
    }
}

if ($foundSecrets.Count -eq 0) { exit 0 }

# ── Block the write and explain ──────────────────────────────────────────────
$filename = Split-Path $filePath -Leaf
$preview  = $foundSecrets -join ', '

# Output a JSON block response that Claude sees as a hook error.
# This prevents the Write/Edit from executing.
$output = @{
    decision = 'block'
    reason   = "WARNUNG: Secret erkannt in '$filename' — Write blockiert.`nGefundene Muster: $preview`nBitte redacte die Secrets vor dem Schreiben mit: '<REDACTED — set locally>'`nAlternativ: python `"`$USERPROFILE/.claude/scripts/redact-secrets.py`" `"$filePath`""
} | ConvertTo-Json -Compress

Write-Output $output
exit 0
