# clawi-session-end.ps1 - Automatischer Backup-Hook für Clawi
# Dieser Hook wird am Ende jeder Session ausgeführt, um Identität und Gedächtnis zu sichern.

$ErrorActionPreference = "SilentlyContinue"

# Pfade
$RepoDir = "C:\Users\barwa\Clawi"
$SyncScript = Join-Path $RepoDir "clawi-setup\sync-clawi.ps1"

Write-Host "🦖 Clawi Auto-Backup gestartet..." -ForegroundColor Cyan

# 1. Identität vom lokalen Workspace in den Repo-Ordner kopieren
if (Test-Path $SyncScript) {
    powershell -NoProfile -File $SyncScript -Mode push
}

# 2. Änderungen im Repo committen und pushen
Set-Location $RepoDir
if (Test-Path ".git") {
    git add clawi-setup/
    $date = Get-Date -Format "yyyy-MM-dd HH:mm"
    git commit -m "#AUTO - Clawi Identity & Memory Sync ($date)"
    git push origin main
}

Write-Host "✅ Clawi Identität erfolgreich zu GitHub gesichert." -ForegroundColor Green
