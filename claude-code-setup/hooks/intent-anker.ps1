# Intent-Anker — Saves the original session goal for drift prevention
# UserPromptSubmit hook: Turn 1 saves goal, counter tracks session length
# Platform: Windows (PowerShell 7+)
#
# ROBUSTNESS: Non-critical hook. Any failure → exit 0 silently.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

$GoalFile = Join-Path $env:TEMP "claude-session-goal.txt"
$CounterFile = Join-Path $env:TEMP "claude-turn-counter.txt"

# Read JSON input from stdin (Claude Code sends hook context)
$json = $null
try {
    $hookInput = [Console]::In.ReadToEnd()
    $json = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    Hook-LogWarn "Failed to parse hook JSON input: $_"
    $json = $null
}

# Increment turn counter
$turn = 0
if (Test-Path $CounterFile) {
    try { $turn = [int](Get-Content $CounterFile -Raw).Trim() } catch { $turn = 0 }
}

# Reset counter if goal file is stale (older than 2 hours = new session)
# This prevents the counter from carrying over across separate Claude Code sessions
$goalFile = Join-Path $env:TEMP "claude-session-goal.txt"
if (Test-Path $goalFile) {
    $goalAge = (Get-Date) - (Get-Item $goalFile).LastWriteTime
    if ($goalAge.TotalHours -gt 2) {
        # Goal is stale — this is a new session, reset counter
        $turn = 0
    }
}

$turn++
Set-Content -Path $CounterFile -Value $turn -NoNewline

# On turn 1: extract and save the user's prompt as session goal
if ($turn -eq 1 -and $json) {
    $goal = ""
    if ($json.prompt) {
        $goal = if ($json.prompt -is [string]) { $json.prompt } else { $json.prompt.content }
    } elseif ($json.message) {
        $goal = if ($json.message -is [string]) { $json.message } else { $json.message.content }
    }
    if ($goal -and $goal.Length -gt 0) {
        $goal = $goal.Substring(0, [Math]::Min(300, $goal.Length))
        Set-Content -Path $GoalFile -Value $goal
    }
}

# Every 5 turns: write a reminder marker file
# Drift onset peaks at turns 4-7 (arxiv 2510.07777), so 5-turn interval catches it early
if (($turn % 5) -eq 0 -and (Test-Path $GoalFile)) {
    $reminderFile = Join-Path $env:TEMP "claude-intent-reminder.txt"
    Set-Content -Path $reminderFile -Value $turn -NoNewline
}

exit 0
