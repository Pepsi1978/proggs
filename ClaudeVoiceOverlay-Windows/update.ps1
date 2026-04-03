# update.ps1 — Baut neue Version und ersetzt die laufende .exe
# Ausfuehren: pwsh -File update.ps1
#
# Warum ein eigenes Skript?
# Das Overlay hat einen eingebauten Watchdog der es nach jedem Kill sofort
# wieder startet. Deshalb kann man die .exe nicht einfach ueberschreiben.
# Dieses Skript baut in ein temporaeres Verzeichnis, beendet beide Prozesse
# (Watchdog + Overlay), tauscht die .exe atomar aus und startet die neue Version.

$ErrorActionPreference = "Stop"
$publishDir = Join-Path $PSScriptRoot "publish"
$tempDir = Join-Path $PSScriptRoot "publish-update"
$exeName = "ClaudeVoiceOverlay.exe"
$exePath = Join-Path $publishDir $exeName
$tempExe = Join-Path $tempDir $exeName

# Step 1: Build into temporary directory
Write-Host "1/4 Baue neue Version..." -ForegroundColor Cyan
dotnet publish -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -o $tempDir

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build fehlgeschlagen!" -ForegroundColor Red
    exit 1
}

$newSize = [math]::Round((Get-Item $tempExe).Length / 1MB, 1)
Write-Host "   Neue Version gebaut ($newSize MB)" -ForegroundColor Green

# Step 2: Kill all overlay processes (watchdog + overlay)
Write-Host "2/4 Beende laufendes Overlay..." -ForegroundColor Cyan
$maxAttempts = 10
for ($i = 0; $i -lt $maxAttempts; $i++) {
    $procs = Get-Process -Name "ClaudeVoiceOverlay" -ErrorAction SilentlyContinue
    if (-not $procs) { break }
    $procs | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 300
}

# Verify all processes are gone
$remaining = Get-Process -Name "ClaudeVoiceOverlay" -ErrorAction SilentlyContinue
if ($remaining) {
    Write-Host "   WARNUNG: Prozess laesst sich nicht beenden!" -ForegroundColor Yellow
}

# Step 3: Swap the exe (rename old, copy new, delete old)
Write-Host "3/4 Ersetze .exe..." -ForegroundColor Cyan
$backupExe = "$exePath.old"
$swapped = $false

for ($i = 0; $i -lt $maxAttempts; $i++) {
    # Kill any respawned processes
    Stop-Process -Name "ClaudeVoiceOverlay" -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 200

    try {
        if (Test-Path $backupExe) { Remove-Item $backupExe -Force -ErrorAction Stop }
        if (Test-Path $exePath) {
            Rename-Item $exePath -NewName "$exeName.old" -Force -ErrorAction Stop
        }
        Copy-Item $tempExe $exePath -Force -ErrorAction Stop
        $swapped = $true
        Write-Host "   .exe ersetzt (Versuch $($i+1))" -ForegroundColor Green
        break
    } catch {
        Write-Host "   Versuch $($i+1): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

if (-not $swapped) {
    Write-Host "FEHLER: Konnte .exe nicht ersetzen nach $maxAttempts Versuchen!" -ForegroundColor Red
    exit 1
}

# Cleanup
Remove-Item $backupExe -Force -ErrorAction SilentlyContinue
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue

# Step 4: Start the new version
Write-Host "4/4 Starte neue Version..." -ForegroundColor Cyan
Start-Process $exePath
Start-Sleep -Seconds 2

$running = Get-Process -Name "ClaudeVoiceOverlay" -ErrorAction SilentlyContinue
if ($running) {
    Write-Host "`nUpdate erfolgreich! Neue Version laeuft (PID: $($running[0].Id))" -ForegroundColor Green
} else {
    Write-Host "`nWARNUNG: Overlay wurde gestartet, aber kein Prozess gefunden." -ForegroundColor Yellow
}
