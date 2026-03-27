# clawi-session-start.ps1 - Automatischer Pull-Hook für Clawi
# Dieser Hook wird am Anfang jeder Session ausgeführt, um die neueste Identität zu laden.

$ErrorActionPreference = "SilentlyContinue"

# Pfade
$RepoDir = "C:\Users\barwa\Clawi"
$SyncScript = Join-Path $RepoDir "clawi-setup\sync-clawi.ps1"

Write-Host "🦖 Clawi Auto-Sync gestartet..." -ForegroundColor Cyan

# 1. Neueste Änderungen von GitHub holen
Set-Location $RepoDir
if (Test-Path ".git") {
    git pull origin main
}

# 2. Identität vom Repo-Ordner in den lokalen Workspace laden
if (Test-Path $SyncScript) {
    powershell -NoProfile -File $SyncScript -Mode pull
}

Write-Host "✅ Clawi ist auf dem neuesten Stand." -ForegroundColor Green
