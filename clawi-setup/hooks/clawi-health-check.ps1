# clawi-health-check.ps1 - Verpasst-Check für Cron-Jobs
# Prüft, ob geplante Sync-Jobs verpasst wurden und holt sie nach.

$ErrorActionPreference = "SilentlyContinue"

# Unicode-Konstanten für Emojis (Windows-Resilienz)
$Warn = [char]::ConvertFromUtf32(0x26A0)

$RepoDir = "C:\Users\barwa\Clawi"
$StateFile = Join-Path $RepoDir "clawi-setup\hooks\sync-state.json"
$StartHook = Join-Path $RepoDir "clawi-setup\hooks\clawi-session-start.ps1"
$EndHook = Join-Path $RepoDir "clawi-setup\hooks\clawi-session-end.ps1"

if (-not (Test-Path $StateFile)) { exit 0 }

$state = Get-Content $StateFile | ConvertFrom-Json
$now = [DateTimeOffset]::Now
$today = Get-Date -Hour 0 -Minute 0 -Second 0

# 1. Verpassten Pull-Check (Morgens 08:00)
$pullDeadline = $today.AddHours($state.scheduled_pull_hour)
if ($now.DateTime -gt $pullDeadline -and ([DateTimeOffset]::FromUnixTimeSeconds($state.last_pull_timestamp).DateTime -lt $pullDeadline)) {
    Write-Host "$Warn Verpasster Morgen-Sync erkannt. Starte Nachholung..." -ForegroundColor Yellow
    powershell -NoProfile -File $StartHook
}

# 2. Verpassten Backup-Check (Mitternacht 00:00)
$yesterdayBackup = $today.AddDays(-1).AddHours($state.scheduled_backup_hour)
if ($now.DateTime -gt $today -and ([DateTimeOffset]::FromUnixTimeSeconds($state.last_push_timestamp).DateTime -lt $yesterdayBackup)) {
    Write-Host "$Warn Verpasstes Mitternachts-Backup erkannt. Starte Nachholung..." -ForegroundColor Yellow
    powershell -NoProfile -File $EndHook
}

exit 0
