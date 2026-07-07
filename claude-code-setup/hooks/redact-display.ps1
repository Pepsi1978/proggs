# redact-display.ps1 — MessageDisplay hook: redact secrets from the DISPLAYED assistant text
# Hook event: MessageDisplay (added in Claude Code v2.1.152)
# Platform: Windows (PowerShell 7+)
#
# Strategy:
#   1. Read the assistant message_text from stdin (MessageDisplay event)
#   2. If it contains known secret patterns, replace them with <REDACTED>
#   3. Emit hookSpecificOutput.displayContent so ONLY the on-screen display changes.
#      Per the MessageDisplay contract the transcript file and Claude's own context
#      keep the ORIGINAL text — this hook never alters what Claude sees.
#
# Companion to redact-secrets.ps1 (PreToolUse, guards file writes). This one guards
# the visible answer text — e.g. a token Claude echoed back into its reply.
#
# ROBUSTNESS: entire body in try/catch. Any error, empty input, or no match → exit 0
# WITHOUT emitting displayContent → the ORIGINAL text is shown unchanged. A broken
# hook must NEVER blank or mangle the user's view.

$ErrorActionPreference = 'SilentlyContinue'

try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Trim().Length -lt 5) { exit 0 }
    $json = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

# Input-Guard: nur auf echte MessageDisplay-Events reagieren
if ($json.hook_event_name -ne 'MessageDisplay') { exit 0 }

$text = $json.message_text
if (-not $text) { exit 0 }

$patterns = @(
    'ghp_[A-Za-z0-9]{36,}',          # GitHub Personal Access Token
    'gho_[A-Za-z0-9]{36,}',          # GitHub OAuth Token
    'ghu_[A-Za-z0-9]{36,}',          # GitHub User-to-Server Token
    'ghs_[A-Za-z0-9]{36,}',          # GitHub Server-to-Server Token
    'github_pat_[A-Za-z0-9_]{36,}',  # GitHub Fine-Grained PAT
    'sk-ant-[A-Za-z0-9_-]{20,}',     # Anthropic API Key
    'sk-[A-Za-z0-9]{40,}',           # OpenAI-style API Key
    'xoxb-[0-9]+-[A-Za-z0-9-]+',     # Slack Bot Token
    'AIza[A-Za-z0-9_-]{30,}'         # Google API Key
)

try {
    $redacted = $text
    foreach ($p in $patterns) {
        $redacted = [regex]::Replace($redacted, $p, '<REDACTED>')
    }

    # Kein Treffer -> Original anzeigen (kein displayContent ausgeben)
    if ($redacted -eq $text) { exit 0 }

    $out = @{
        hookSpecificOutput = @{
            hookEventName  = 'MessageDisplay'
            displayContent = $redacted
        }
    }
    $out | ConvertTo-Json -Depth 5 -Compress
} catch {
    exit 0
}

exit 0
