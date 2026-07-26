@echo off
setlocal

rem Legt die Desktop-Verknuepfung fuer den OpenLauncher an - Doppelklick genuegt.
rem
rem Bewusst eigenstaendig: Diese Datei setzt WEDER ein aktuelles Repo NOCH einen Build voraus. Sie
rem sucht die vorhandene OpenLauncher.exe an den ueblichen Orten und verknuepft die erste, die sie
rem findet. Damit funktioniert sie auch dann, wenn nur die fertige .exe heruntergeladen wurde.
rem
rem Fuer den kompletten Weg (Profile uebernehmen, alten Ordner entfernen, Release bauen, verknuepfen)
rem gibt es "OpenLauncher einrichten.cmd" im selben Ordner.

title OpenLauncher - Verknuepfung anlegen

where pwsh >nul 2>&1
if %ERRORLEVEL% equ 0 (set "PS=pwsh") else (set "PS=powershell")

%PS% -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$kandidaten = @(" ^
  "  (Join-Path $HOME 'proggs\OpenLauncher\bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.exe')," ^
  "  (Join-Path $PSScriptRoot 'OpenLauncher.exe')," ^
  "  (Join-Path ([Environment]::GetFolderPath('Desktop')) 'OpenLauncher.exe')," ^
  "  (Join-Path $HOME 'Downloads\OpenLauncher.exe')," ^
  "  (Join-Path $HOME 'proggs\OpenLauncher\OpenLauncher.exe')" ^
  ");" ^
  "$exe = $kandidaten ^| Where-Object { Test-Path -LiteralPath $_ } ^| Select-Object -First 1;" ^
  "if (-not $exe) {" ^
  "  Write-Host 'Es wurde keine OpenLauncher.exe gefunden. Gesucht wurde an diesen Orten:' -ForegroundColor Yellow;" ^
  "  $kandidaten ^| ForEach-Object { Write-Host \"  - $_\" };" ^
  "  Write-Host '';" ^
  "  Write-Host 'Lege die heruntergeladene OpenLauncher.exe an einen dieser Orte (zum Beispiel auf den Desktop) und starte diese Datei erneut.' -ForegroundColor Yellow;" ^
  "  exit 1" ^
  "};" ^
  "$exe = (Resolve-Path -LiteralPath $exe).Path;" ^
  "$lnk = Join-Path ([Environment]::GetFolderPath('Desktop')) 'OpenLauncher.lnk';" ^
  "$shell = New-Object -ComObject WScript.Shell;" ^
  "$k = $shell.CreateShortcut($lnk);" ^
  "$k.TargetPath = $exe;" ^
  "$k.WorkingDirectory = (Split-Path -Parent $exe);" ^
  "$k.IconLocation = \"$exe,0\";" ^
  "$k.Description = 'OpenLauncher - Modell + Profil fuer OpenCode und Claude Code auswaehlen und starten';" ^
  "$k.WindowStyle = 1;" ^
  "$k.Save();" ^
  "Write-Host '';" ^
  "Write-Host 'Verknuepfung angelegt:' -ForegroundColor Green;" ^
  "Write-Host \"  $lnk\";" ^
  "Write-Host \"  -^> Ziel: $exe\";"

set "ERGEBNIS=%ERRORLEVEL%"
echo.
if %ERGEBNIS% neq 0 (
    echo Es wurde KEINE Verknuepfung angelegt. Der Grund steht oben.
) else (
    echo Fertig. Die Verknuepfung "OpenLauncher" liegt jetzt auf dem Desktop.
)
echo.
rem Fenster offen halten: Nach einem Doppelklick waere die Meldung sonst sofort weg.
pause
exit /b %ERGEBNIS%
