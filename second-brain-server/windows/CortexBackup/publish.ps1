# publish.ps1 — baut Cortex Backup als EINE self-contained .exe und legt eine Desktop-Verknuepfung an.
#
# Aufruf:  pwsh -ExecutionPolicy Bypass -File publish.ps1
# Ergebnis: publish\CortexBackup.exe  (laeuft ohne .NET-Installation) + Verknuepfung auf dem Desktop.
# WPF: KEIN Trimming/AOT (bugs/desktop/dotnet-csharp.md 1.5). Single-File self-contained (1.13/13.1).
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

Write-Host "Baue Cortex Backup (self-contained single-file)…" -ForegroundColor Cyan
dotnet publish -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true `
    -o "$here\publish"
if ($LASTEXITCODE -ne 0) { throw "dotnet publish fehlgeschlagen" }

$exe = Join-Path $here 'publish\CortexBackup.exe'
if (-not (Test-Path $exe)) { throw "EXE nicht gefunden: $exe" }

# Restore-Anleitung neben die EXE legen (der Wiederherstellen-Knopf oeffnet sie von dort).
$readme = Join-Path (Split-Path $here -Parent) 'BACKUP-RESTORE-README.md'
if (Test-Path $readme) { Copy-Item $readme (Join-Path $here 'publish\BACKUP-RESTORE-README.md') -Force }

# Desktop-Verknuepfung mit Icon anlegen (WScript.Shell COM).
$desktop = [Environment]::GetFolderPath('Desktop')
$lnkPath = Join-Path $desktop 'Cortex Backup.lnk'
$ico = Join-Path $here 'cortex.ico'
$ws = New-Object -ComObject WScript.Shell
$lnk = $ws.CreateShortcut($lnkPath)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = (Split-Path $exe -Parent)
if (Test-Path $ico) { $lnk.IconLocation = $ico }
$lnk.Description = 'Cortex — Komplett-Backup des zweiten Gehirns auf diesen PC'
$lnk.Save()

Write-Host ""
Write-Host "FERTIG:" -ForegroundColor Green
Write-Host "  EXE:          $exe"
Write-Host "  Desktop-Icon: $lnkPath"
Write-Host "Doppelklick auf das Desktop-Icon 'Cortex Backup' startet das Tool." -ForegroundColor Cyan
