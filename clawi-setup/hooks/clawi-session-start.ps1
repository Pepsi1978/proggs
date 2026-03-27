# clawi-session-start.ps1 - Automatischer Pull-Hook für Clawi
# Dieser Hook wird am Anfang jeder Session ausgeführt, um die neueste Identität zu laden.

$ErrorActionPreference = "SilentlyContinue"

# Unicode-Konstanten für Emojis (Windows-Resilienz)
$Dino = [char]::ConvertFromUtf32(0x1F996)

# Pfade
$RepoDir = "C:\Users\barwa\Clawi"
$SyncScript = Join-Path $RepoDir "clawi-setup\sync-clawi.ps1"
$StateFile = Join-Path $RepoDir "clawi-setup\hooks\sync-state.json"

Write-Host "$Dino Clawi Auto-Sync gestartet..." -ForegroundColor Cyan

# 1. Neueste Änderungen von GitHub holen
Set-Location $RepoDir
if (Test-Path ".git") {
    # Stash changes in clawi-setup just in case
    git stash push clawi-setup/ -m "Auto-sync stash"
    git pull origin main --rebase
    git stash pop
}

# 2. Identität vom Repo-Ordner in den lokalen Workspace laden
if (Test-Path $SyncScript) {
    powershell -NoProfile -File $SyncScript -Mode pull
    
    # 3. Status-Update
    if (Test-Path $StateFile) {
        $state = Get-Content $StateFile | ConvertFrom-Json
        $state.last_pull_timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
        $state | ConvertTo-Json | Out-File $StateFile -Encoding utf8
    }
}

Write-Host "OK. Clawi ist auf dem neuesten Stand." -ForegroundColor Green
