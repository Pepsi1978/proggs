# Safety Gate: Block dangerous commands before execution
# Hook event: PreToolUse (Bash)
# Platform: Windows (PowerShell 7+)
#
# ROBUSTNESS: Entire script wrapped in try/catch. Any failure → exit 0 (allow).
# A broken safety-gate must never block normal work.

$ErrorActionPreference = 'SilentlyContinue'

try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Length -lt 5) { exit 0 }
    $json = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

$toolName = $json.tool_name
$toolInput = $json.tool_input

# Only check Bash commands
if ($toolName -ne 'Bash') { exit 0 }

$cmd = $toolInput.command
if (-not $cmd) { exit 0 }

# Dangerous patterns to block.
# NOTE: Patterns are intentionally NOT anchored to line start (no ^\s*).
# Anchoring to ^ would allow bypassing via: something && git reset --hard
$dangerous = @(
    'rm\s+-rf\s+[/~]',                # rm -rf / or ~
    'git\s+push\s+--force\s+.*main',  # force push to main
    'git\s+reset\s+--hard',           # hard reset (unanchored — catches: cmd && git reset --hard)
    'git\s+restore\s+\.',             # git restore . (unanchored — catches chained commands)
    'git\s+branch\s+-D',              # git branch -D (unanchored — catches chained commands)
    '(?i)DROP\s+TABLE',               # SQL drop table (case-insensitive)
    '(?i)DROP\s+DATABASE',            # SQL drop database (case-insensitive)
    '(?i)TRUNCATE\s+TABLE',           # SQL truncate (case-insensitive)
    'format\s+[A-Z]:',                # format drive
    'del\s+/[sS]\s+/[qQ]\s+C:',      # delete C: drive
    'Remove-Item\s+-Recurse.*C:\\',   # PowerShell recursive delete C:
    'git\s+init',                     # block creating new repos — only Pepsi1978/proggs allowed (unanchored)
    'gh\s+repo\s+create',             # block creating GitHub repos (unanchored)
    'git\s+remote\s+add'              # block adding new remotes — only Pepsi1978/proggs allowed (unanchored)
)

# Shell update patterns — WARNING only (exit 0), not blocking
# Warn when shell tools are updated mid-session (can kill running terminals)
$shellUpdates = @(
    'npm\s+install\s+-g\s+@google/gemini-cli',  # global Gemini-CLI install during session
    'winget\s+upgrade\s+Git\.Git',              # git update during session
    'rustup\s+update'                           # rustup update during session
)

foreach ($pattern in $dangerous) {
    if ($cmd -match $pattern) {
        Hook-LogError "BLOCKED dangerous command: $pattern — cmd: $($cmd.Substring(0, [Math]::Min(100, $cmd.Length)))"
        # Log to whiteboard — wrapped in try/catch so exit 2 is ALWAYS reached
        try {
            $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: safety-gate.ps1 — Befehl blockiert: $pattern"
            Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
        } catch { }
        @{ error = "BLOCKED: Dangerous command detected — $pattern" } | ConvertTo-Json -Compress | Write-Output
        exit 2
    }
}

# Shell update warning — allow but warn user
foreach ($pattern in $shellUpdates) {
    if ($cmd -match $pattern) {
        Hook-LogWarn "Shell-update detected mid-session: $pattern"
        Write-Output "WARNING (safety-gate): Shell/Tool-Update erkannt. Laut Gemini-Regeln muessen Shell-Updates NACH Abschluss aller Aufgaben erfolgen — sie koennen alle offenen Terminals killen. Bitte erst alle Aufgaben beenden und committen, dann updaten."
        exit 0  # allow, but user has been warned
    }
}

# settings.json write-via-Bash warning — allow but warn
if ($cmd -match '>\s*.*settings\.json' -or $cmd -match 'echo.*>.*settings\.json' -or $cmd -match 'cat.*>.*settings\.json') {
    Hook-LogWarn "Bash command writing to settings.json detected"
    Write-Output "WARNING (safety-gate): Schreibzugriff auf settings.json per Bash erkannt."
    exit 0
}

exit 0
