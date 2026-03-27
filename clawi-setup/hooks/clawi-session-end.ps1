# clawi-session-end.ps1 - Automatischer Backup-Hook für Clawi
# Dieser Hook wird am Ende jeder Session ausgeführt, um Identität und Gedächtnis zu sichern.

$ErrorActionPreference = "SilentlyContinue"

# Unicode-Konstanten für Emojis (Windows-Resilienz)
$Dino = [char]::ConvertFromUtf32(0x1F996)

# Pfade
$RepoDir = "C:\Users\barwa\Clawi"
$SyncScript = Join-Path $RepoDir "clawi-setup\sync-clawi.ps1"
$StateFile = Join-Path $RepoDir "clawi-setup\hooks\sync-state.json"

Write-Host "$Dino Clawi Auto-Backup gestartet..." -ForegroundColor Cyan

# 0. Gedächtnis-Staubsauger ausführen (letzte Beobachtungen sammeln)
$CollectorScript = Join-Path $RepoDir "clawi-setup\hooks\clawi-observation-collector.ps1"
if (Test-Path $CollectorScript) {
    powershell -NoProfile -File $CollectorScript
}

# 1. Identität vom lokalen Workspace in den Repo-Ordner kopieren
if (Test-Path $SyncScript) {
    powershell -NoProfile -File $SyncScript -Mode push
}

# 2. Änderungen im Repo committen und pushen
Set-Location $RepoDir
if (Test-Path ".git") {
    git add clawi-setup/ Forschung/
    $dateStr = Get-Date -Format "yyyy-MM-dd HH:mm"
    git commit -m "#AUTO - Clawi Identity & Memory Sync ($dateStr)"
    git push origin main
    
    # 3. Status-Update
    if (Test-Path $StateFile) {
        $state = Get-Content $StateFile | ConvertFrom-Json
        $state.last_push_timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
        $state | ConvertTo-Json | Out-File $StateFile -Encoding utf8
    }
}

Write-Host "OK. Clawi Identitaet erfolgreich zu GitHub gesichert." -ForegroundColor Green
