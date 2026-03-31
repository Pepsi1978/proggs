# Hyperagent Stop-Hook — Metacognitive Analysis Prompt Injection
# Event: Stop (fires when Claude finishes a response)
# Type: prompt (injects text into Claude's context)
# Platform: Windows (PowerShell 7+)
#
# PURPOSE: Reminds Claude to perform metacognitive analysis before completing.
# This is the "heartbeat" of the Hyperagent system — it fires on every Stop event.
#
# ROBUSTNESS: Non-critical hook. Any failure → exit 0 with empty output.
# An empty output means no prompt injection — Claude continues normally.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

# Read turn counter to determine if analysis is needed
$CounterFile = Join-Path $env:TEMP "claude-turn-counter.txt"
$turn = 0
if (Test-Path $CounterFile) {
    try { $turn = [int](Get-Content $CounterFile -Raw).Trim() } catch { $turn = 0 }
}

# Skip metacognitive analysis for trivial sessions (<5 turns)
if ($turn -lt 5) {
    exit 0
}

# Read session goal for context
$GoalFile = Join-Path $env:TEMP "claude-session-goal.txt"
$goal = ""
if (Test-Path $GoalFile) {
    try { $goal = (Get-Content $GoalFile -Raw).Trim() } catch { $goal = "" }
}

# Check if goal file is stale (older than 2 hours = different session)
if (Test-Path $GoalFile) {
    $goalAge = (Get-Date) - (Get-Item $GoalFile).LastWriteTime
    if ($goalAge.TotalHours -gt 2) {
        # Stale goal — don't inject analysis for a previous session
        exit 0
    }
}

# Read today's hook error count as a signal
$LogDate = Get-Date -Format "yyyy-MM-dd"
$LogFile = Join-Path $env:USERPROFILE ".claude" "logs" "hooks" "$LogDate.log"
$errorCount = 0
if (Test-Path $LogFile) {
    try {
        $cutoff = (Get-Date).AddHours(-2).ToString("HH:mm:ss")
        $lines = Get-Content $LogFile -ErrorAction Stop
        foreach ($line in $lines) {
            # Case-sensitive match (-cmatch), exclude "0 errors" false positives
            if ($line -cmatch "^\[(\d{2}:\d{2}:\d{2})\].*\b(ERROR|FAIL|FEHLER)\b" -and $line -notmatch "0 errors") {
                if ($Matches[1] -ge $cutoff) { $errorCount++ }
            }
        }
    } catch { $errorCount = 0 }
}

# Determine analysis depth based on session complexity
$depth = "leicht"  # default: inline analysis
if ($turn -gt 20 -or $errorCount -gt 3) {
    $depth = "voll"  # spawn hyperagent sub-agent
}

# Build the metacognitive prompt
$prompt = @"
[METACOGNITIVE MONITORING — Hyperagent-Pattern]
Session-Turns: $turn | Hook-Errors: $errorCount | Analyse-Tiefe: $depth
"@

if ($goal) {
    $prompt += "`nUrspruengliches Ziel: $goal"
}

$prompt += @"

Bevor du diese Antwort abschliesst, fuehre eine kurze metacognitive Analyse durch:
1. **Intent-Check**: Habe ich das urspruengliche Ziel des Benutzers erreicht?
2. **Retry-Check**: Gab es unnoetige Wiederholungen (>2x gleicher Versuch)?
3. **Korrektur-Check**: Hat der Benutzer mich korrigiert? Falls ja: Sollte das eine Regel werden?
4. **Lern-Check**: Habe ich etwas Neues gelernt das persistiert werden sollte?

Fasse die Ergebnisse als Intelligenz-Vorschlaege am Ende deiner Antwort zusammen.
Bei Analyse-Tiefe 'voll': Erwaege den Hyperagent-Sub-Agent zu spawnen fuer tiefere Analyse.
"@

# Output the prompt (Claude Code reads stdout from prompt hooks)
Write-Output $prompt

exit 0
