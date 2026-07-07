# Session Scorer — Quantitative Session-Metrics Tracker
# Event: SessionEnd (fires when Codex session ends)
# Type: command (runs as background script)
# Platform: Windows (PowerShell 7+)
#
# PURPOSE: Collects quantitative metrics from the session and appends them
# to ~/.codex/session-scores.jsonl for trend analysis.
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

$ScoresFile = Join-Path $env:USERPROFILE ".codex" "session-scores.jsonl"

# Ensure directory exists
$scoresDir = Split-Path $ScoresFile
if (-not (Test-Path $scoresDir)) {
    New-Item -ItemType Directory -Path $scoresDir -Force | Out-Null
}

# 1. Turn count
$CounterFile = Join-Path $env:TEMP "codex-turn-counter.txt"
$turns = 0
if (Test-Path $CounterFile) {
    try { $turns = [int](Get-Content $CounterFile -Raw).Trim() } catch { $turns = 0 }
}

# Skip scoring for trivial sessions
if ($turns -lt 3) {
    exit 0
}

# 2. Session goal
$GoalFile = Join-Path $env:TEMP "codex-session-goal.txt"
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
$LogFile = Join-Path $env:USERPROFILE ".codex" "logs" "hooks" "$LogDate.log"
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
goal_path = os.path.join(os.environ.get('TEMP', '/tmp'), 'codex-session-goal.txt')
goal = 'unknown'
if os.path.exists(goal_path):
    raw = pathlib.Path(goal_path).read_text(encoding='utf-8', errors='replace').strip()
    goal = raw[:100].replace('\n', ' ').replace('\r', '')
efficiency = min($commitCount / max($turns, 1) * 100, 40)
quality = max(0, 30 - $hookErrors * 10)
duration_bonus = min($durationMin / 3, 30)
iq_score = max(0, min(100, round(efficiency + quality + duration_bonus)))
data = {
    'date': '$timestamp',
    'turns': $turns,
    'hook_errors': $hookErrors,
    'commits': $commitCount,
    'duration_min': $durationMin,
    'goal': goal,
    'iq_score': iq_score
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
    $efficiency = [Math]::Min($commitCount / [Math]::Max($turns, 1) * 100, 40)
    $quality = [Math]::Max(0, 30 - $hookErrors * 10)
    $durationBonus = [Math]::Min($durationMin / 3, 30)
    $iqScore = [Math]::Max(0, [Math]::Min(100, [Math]::Round($efficiency + $quality + $durationBonus)))
    $jsonLine = "{`"date`":`"$timestamp`",`"turns`":$turns,`"hook_errors`":$hookErrors,`"commits`":$commitCount,`"duration_min`":$durationMin,`"goal`":`"$goalClean`",`"iq_score`":$iqScore}"
    try {
        Add-Content -Path $ScoresFile -Value $jsonLine -Encoding UTF8
        Hook-Log "Session score written (PS fallback): turns=$turns"
    } catch {
        Hook-LogError "Failed to write session score: $_"
    }
}

# 7. Session summary for the user (3 sentences, German, plain text)
$summaryParts = @()
if ($commitCount -gt 0) {
    $summaryParts += "$commitCount Commit(s) gepusht"
} else {
    $summaryParts += "Keine Commits"
}
if ($hookErrors -gt 0) {
    $summaryParts += "$hookErrors Hook-Fehler aufgetreten"
}
if ($durationMin -gt 0) {
    $summaryParts += "Dauer: ca. $durationMin Minuten"
}
$summaryLine = $summaryParts -join ", "
$summaryLine += ". IQ-Score: $iqScore/100."

$summaryPath = Join-Path $env:TEMP "codex-session-summary.txt"
try {
    Set-Content -Path $summaryPath -Value $summaryLine -Encoding UTF8 -NoNewline
} catch { }

# 8. Session cleanup (consolidated from session-cleanup.ps1, 2026-04-12)
# Clean temp files to prevent accumulation across sessions
$cleaned = 0

# Clean Codex temp directory (files older than 2 hours)
$claudeTemp = Join-Path $env:TEMP "codex"
if (Test-Path $claudeTemp) {
    Get-ChildItem $claudeTemp -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-2) } |
        ForEach-Object { try { Remove-Item $_.FullName -Force -ErrorAction Stop; $cleaned++ } catch { } }
    Get-ChildItem $claudeTemp -Directory -Recurse -ErrorAction SilentlyContinue |
        Where-Object { (Get-ChildItem $_.FullName -ErrorAction SilentlyContinue).Count -eq 0 } |
        ForEach-Object { try { Remove-Item $_.FullName -Force } catch { } }
}

# Clean node compile cache (files older than 1 day)
$nodeCache = Join-Path $env:TEMP "node-compile-cache"
if (Test-Path $nodeCache) {
    Get-ChildItem $nodeCache -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-1) } |
        ForEach-Object { try { Remove-Item $_.FullName -Force -ErrorAction Stop; $cleaned++ } catch { } }
}

# Intent-anker files: DO NOT DELETE (fixed 2026-04-12)
# These files are in $env:TEMP with fixed names — shared across ALL sessions.
# Deleting them here destroys the turn counter and goal for any OTHER running session.
# They are small text files (<1KB each) that get overwritten on next session start
# by intent-anker.ps1, so there is no accumulation problem.
# @("codex-session-goal.txt", "codex-turn-counter.txt", "codex-intent-reminder.txt")

# Clean old agent-writeback sentinel files
Get-ChildItem (Join-Path $env:TEMP "agent-writeback-*.json") -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-2) } |
    ForEach-Object { try { Remove-Item $_.FullName -Force; $cleaned++ } catch { } }

# Clean old hook log files (older than 14 days)
$hookLogs = Join-Path $env:USERPROFILE ".codex" "logs" "hooks"
if (Test-Path $hookLogs) {
    Get-ChildItem $hookLogs -Filter "*.log" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        ForEach-Object { try { Remove-Item $_.FullName -Force; $cleaned++ } catch { } }
}

Hook-Log "Session end: score written, $cleaned temp files cleaned"

exit 0
