# Session Scorer — Quantitative Session-Metrics Tracker
# Event: SessionEnd (fires when Claude Code session ends)
# Type: command (runs as background script)
# Platform: Windows (PowerShell 7+)
#
# PURPOSE: Collects quantitative metrics from the session and appends them
# to ~/.claude/session-scores.jsonl for trend analysis.
#
# METRICS COLLECTED:
# - Turn count (from intent-anker turn counter)
# - Hook error count (from hook logs)
# - Session duration (from goal file timestamp)
# - Commit count (from git log)
#
# ROBUSTNESS: Non-critical. Any failure → exit 0 silently.
# Missing data → use 0 or "unknown" as default.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

$ScoresFile = Join-Path $env:USERPROFILE ".claude" "session-scores.jsonl"

# Ensure directory exists
$scoresDir = Split-Path $ScoresFile
if (-not (Test-Path $scoresDir)) {
    New-Item -ItemType Directory -Path $scoresDir -Force | Out-Null
}

# 1. Turn count
$CounterFile = Join-Path $env:TEMP "claude-turn-counter.txt"
$turns = 0
if (Test-Path $CounterFile) {
    try { $turns = [int](Get-Content $CounterFile -Raw).Trim() } catch { $turns = 0 }
}

# Skip scoring for trivial sessions
if ($turns -lt 3) {
    exit 0
}

# 2. Session goal
$GoalFile = Join-Path $env:TEMP "claude-session-goal.txt"
$goal = "unknown"
if (Test-Path $GoalFile) {
    try {
        $goalRaw = (Get-Content $GoalFile -Raw).Trim()
        # Truncate to 100 chars, remove newlines — Python handles JSON escaping
        $goal = $goalRaw.Substring(0, [Math]::Min(100, $goalRaw.Length))
        $goal = $goal -replace '[\r\n]+', ' '
    } catch { $goal = "unknown" }
}

# 3. Hook errors in current session (last 2 hours, not entire day)
$LogDate = Get-Date -Format "yyyy-MM-dd"
$LogFile = Join-Path $env:USERPROFILE ".claude" "logs" "hooks" "$LogDate.log"
$hookErrors = 0
if (Test-Path $LogFile) {
    try {
        $cutoff = (Get-Date).AddHours(-2).ToString("HH:mm:ss")
        $lines = Get-Content $LogFile -ErrorAction Stop
        foreach ($line in $lines) {
            # Case-sensitive match (-cmatch), exclude "0 errors" false positives
            if ($line -cmatch "^\[(\d{2}:\d{2}:\d{2})\].*\b(ERROR|FEHLER)\b" -and $line -notmatch "0 errors") {
                if ($Matches[1] -ge $cutoff) { $hookErrors++ }
            }
        }
    } catch { $hookErrors = 0 }
}

# 4. Commits in last 2 hours
$commitCount = 0
try {
    $commits = git -C "$env:USERPROFILE/proggs" log --oneline --since="2 hours ago" 2>$null
    if ($commits) {
        $commitCount = @($commits).Count
    }
} catch { $commitCount = 0 }

# 5. Session duration (from goal file creation time)
$durationMin = 0
if (Test-Path $GoalFile) {
    try {
        $goalAge = (Get-Date) - (Get-Item $GoalFile).LastWriteTime
        # Only count if <4 hours (otherwise it's a stale file from previous session)
        if ($goalAge.TotalHours -lt 4) {
            $durationMin = [Math]::Round($goalAge.TotalMinutes)
        }
    } catch { $durationMin = 0 }
}

# 6. Build and append JSON line via Python (guarantees correct escaping)
# Find Python: try python3, python, then full Windows path
$pythonCmd = $null
foreach ($candidate in @("python3", "python", "$env:LOCALAPPDATA\Programs\Python\Python313\python.exe", "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe")) {
    try {
        $null = & $candidate --version 2>$null
        if ($LASTEXITCODE -eq 0) { $pythonCmd = $candidate; break }
    } catch { }
}

$timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ"

if ($pythonCmd) {
    try {
        # Python reads goal file DIRECTLY — no PowerShell string processing
        & $pythonCmd -c @"
import json, os, pathlib
goal_path = os.path.join(os.environ.get('TEMP', '/tmp'), 'claude-session-goal.txt')
goal = 'unknown'
if os.path.exists(goal_path):
    raw = pathlib.Path(goal_path).read_text(encoding='utf-8', errors='replace').strip()
    goal = raw[:100].replace('\n', ' ').replace('\r', '')
data = {
    'date': '$timestamp',
    'turns': $turns,
    'hook_errors': $hookErrors,
    'commits': $commitCount,
    'duration_min': $durationMin,
    'goal': goal
}
scores = r'$ScoresFile'
line = json.dumps(data, ensure_ascii=False)
with open(scores, 'a', encoding='utf-8') as f:
    f.write(line + '\n')
"@
        Hook-Log "Session score written: turns=$turns errors=$hookErrors commits=$commitCount"
    } catch {
        Hook-LogError "Failed to write session score via Python: $_"
    }
} else {
    # Fallback: PowerShell-only JSON (strips problematic chars)
    $goalClean = $goal -replace '[\\"]', '_'
    $jsonLine = "{`"date`":`"$timestamp`",`"turns`":$turns,`"hook_errors`":$hookErrors,`"commits`":$commitCount,`"duration_min`":$durationMin,`"goal`":`"$goalClean`"}"
    try {
        Add-Content -Path $ScoresFile -Value $jsonLine -Encoding UTF8
        Hook-Log "Session score written (PS fallback): turns=$turns"
    } catch {
        Hook-LogError "Failed to write session score: $_"
    }
}

exit 0
