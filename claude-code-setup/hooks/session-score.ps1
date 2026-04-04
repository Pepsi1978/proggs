# Session Score: Automatic quality scoring at session end
# Runs as Stop hook — calculates a score based on session metrics
# Writes to ~/.claude/session-scores.jsonl for trend analysis
# Platform: Windows (PowerShell)

. "$PSScriptRoot/hook-log.ps1"

$ScoreFile = Join-Path $env:USERPROFILE ".claude" "session-scores.jsonl"
$RepoDir = Join-Path $env:USERPROFILE "proggs"

# Collect metrics
$score = 5  # Base score
$metrics = @{}

# 1. Check if commits were made today in the last 30 minutes
$recentCommits = 0
try {
    Set-Location $RepoDir
    $since = (Get-Date).AddMinutes(-60).ToString("yyyy-MM-ddTHH:mm:ss")
    $recentCommits = [int](git log --oneline --since="$since" 2>$null | Measure-Object -Line).Lines
    $metrics["commits"] = $recentCommits
} catch {
    $metrics["commits"] = 0
}

# 2. Score adjustments based on commits
if ($recentCommits -ge 3) { $score += 2 }      # Productive session
elseif ($recentCommits -ge 1) { $score += 1 }   # Some output
else { $score -= 1 }                              # No commits = less productive

# 3. Check if any tests failed (look for recent test output)
# Simple heuristic: if recent commits contain "fix" or "bug", slightly lower
$fixCommits = 0
try {
    $fixCommits = [int](git log --oneline --since="$since" --grep="fix\|bug\|hotfix" -i 2>$null | Measure-Object -Line).Lines
    $metrics["fix_commits"] = $fixCommits
} catch {
    $metrics["fix_commits"] = 0
}
if ($fixCommits -gt 0) { $score += 1 }  # Fixing bugs is good work

# 4. Check disk space (penalize if critically low)
try {
    $freeGB = [math]::Round((Get-PSDrive C).Free / 1GB, 1)
    $metrics["disk_free_gb"] = $freeGB
    if ($freeGB -lt 50) { $score -= 1 }
} catch {
    $metrics["disk_free_gb"] = -1
}

# Clamp score to 1-10
$score = [math]::Max(1, [math]::Min(10, $score))
$metrics["score"] = $score

# Build JSON line
$entry = @{
    date = (Get-Date).ToString("yyyy-MM-dd")
    time = (Get-Date).ToString("HH:mm:ss")
    score = $score
    commits = $metrics["commits"]
    fix_commits = $metrics["fix_commits"]
    disk_free_gb = $metrics["disk_free_gb"]
} | ConvertTo-Json -Compress

# Append to JSONL file
$entry | Out-File -FilePath $ScoreFile -Append -Encoding utf8

Hook-Log "score=$score commits=$recentCommits fixes=$fixCommits disk=${freeGB}GB"

# Output valid JSON for Claude Code hook validation
# Use Write-Host for log display (stderr), not Write-Output (stdout parsed as JSON)
Write-Host "Session-Score: $score/10 (Commits: $recentCommits, Fixes: $fixCommits)"

exit 0
