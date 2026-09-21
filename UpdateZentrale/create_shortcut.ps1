# Erstellt die Desktop-Verknuepfung fuer die UpdateZentrale.
# Das Icon steckt bereits in der .exe (ApplicationIcon im csproj), darum reicht die exe als
# Icon-Quelle.

$projektDir = $PSScriptRoot
$exe = Join-Path $projektDir "bin\Release\net8.0-windows10.0.19041.0\win-x64\UpdateZentrale.exe"
$desktopDir = [Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)
$lnkPath = Join-Path $desktopDir "UpdateZentrale.lnk"

if (-not (Test-Path $exe)) {
    Write-Error "EXE nicht gefunden: $exe  --  zuerst 'dotnet build -c Release' ausfuehren."
    exit 1
}

$shell = New-Object -ComObject WScript.Shell
$lnk = $shell.CreateShortcut($lnkPath)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $projektDir
$lnk.IconLocation = "$exe,0"
$lnk.Description = "UpdateZentrale - alle Werkzeuge pruefen und aktualisieren"
$lnk.WindowStyle = 1
$lnk.Save()

# Dieselbe Verknuepfung ins Startmenue, damit die Zentrale auch ueber die Suche startet.
$startMenuDir = Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::ApplicationData)) "Microsoft\Windows\Start Menu\Programs"
$startLnk = Join-Path $startMenuDir "UpdateZentrale.lnk"
Copy-Item -Path $lnkPath -Destination $startLnk -Force

# Windows merkt sich Verknuepfungs-Icons pro Pfad; ohne das Auffrischen bleibt nach einer
# Icon-Aenderung das alte Bild stehen.
try { & "$env:SystemRoot\System32\ie4uinit.exe" -show } catch { }

Write-Output "Verknuepfung erstellt: $lnkPath"
Write-Output "Verknuepfung erstellt: $startLnk"
Write-Output "  -> Ziel: $exe"
