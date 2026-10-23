# update-zentrale.ps1 -- Selbst-Update der UpdateZentrale (Windows).
#
# Gegenstueck zu UpdaterZentrale-macOS/update-zentrale.sh. Ablauf:
#   1. Ja/Nein-Fenster (240 s, ohne Klick gilt "Nein") -- ohne Ja passiert nichts,
#   2. in einen Zwischenordner bauen: die laufende UpdateZentrale.exe ist gesperrt, ein Build in
#      bin\Release wuerde am Kopieren scheitern,
#   3. den abgekoppelten Helfer werkzeuge\selbstupdate-helfer.ps1 starten: er wartet, bis keine
#      UpdateZentrale mehr laeuft, kopiert den Zwischenstand nach bin\Release und startet die neue
#      Fassung. Abgekoppelt, weil die App nicht auf ein Skript warten kann, das sie selbst beendet.
#
# -AppPid: die App uebergibt ihre Prozessnummer, wenn sie das Update selbst startet. Sie beendet sich
# dann nach ihrem Protokoll selbst, der Helfer wartet nur darauf. Ohne -AppPid (Start von aussen)
# schliesst der Helfer die App nach einer Sekunde.
#
# Statuszeile fuer die App: UPDATEZENTRALE_UPDATE_STATUS=started|cancelled|no-answer
# Aufruf:  pwsh -File "$env:USERPROFILE\proggs\UpdateZentrale\update-zentrale.ps1" [-AppPid <pid>]
param([int]$AppPid = 0)

$ErrorActionPreference = 'Stop'

$projektDir = $PSScriptRoot
$zielDir    = Join-Path $projektDir 'bin\Release\net8.0-windows10.0.19041.0\win-x64'
$zwischen   = Join-Path $projektDir 'obj\selbstupdate'
$exe        = Join-Path $zielDir 'UpdateZentrale.exe'
$helferSkript = Join-Path $projektDir 'werkzeuge\selbstupdate-helfer.ps1'
$helferLog  = Join-Path $env:LOCALAPPDATA 'UpdateZentrale\logs\selbstupdate.log'

$antwort = (New-Object -ComObject WScript.Shell).Popup(
    "Die UpdateZentrale wird jetzt neu gebaut, geschlossen und in der neuen Fassung gestartet.`n`nJetzt aktualisieren?",
    240, 'UpdateZentrale aktualisieren', 4 + 32 + 4096)
if ($antwort -eq -1) { Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=no-answer'; exit 0 }
if ($antwort -ne 6)  { Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=cancelled'; exit 0 }

if (Test-Path $zwischen) { Remove-Item $zwischen -Recurse -Force }
dotnet build (Join-Path $projektDir 'UpdateZentrale.csproj') -c Release -o $zwischen
if ($LASTEXITCODE -ne 0) { Write-Error "Build fehlgeschlagen (Exit $LASTEXITCODE)."; exit 1 }

New-Item -ItemType Directory -Path (Split-Path $helferLog) -Force | Out-Null
# Als Datei mit -File statt als langer -Command-Text: ein erhoehter Start (RunAs) reicht so nur kurze,
# sicher zitierte Argumente durch.
$argumente = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$helferSkript`"",
               '-Zwischen', "`"$zwischen`"", '-Ziel', "`"$zielDir`"", '-Exe', "`"$exe`"", '-Log', "`"$helferLog`"",
               '-AppPid', "$AppPid")
$shell = (Get-Process -Id $PID).Path
$helferArgs = @{ FilePath = $shell; WindowStyle = 'Hidden'; ArgumentList = $argumente }

# Laeuft die UpdateZentrale als Administrator, kann ein Helfer ohne Adminrechte ihr Fenster weder
# schliessen (UIPI) noch den Prozess beenden -- die gesperrte exe bliebe stehen und das Update
# verpuffte still. Der Pfad eines erhoehten Prozesses ist von hier aus nicht lesbar: daran erkannt.
$istAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
$laeuftErhoeht = @(Get-Process UpdateZentrale -ErrorAction SilentlyContinue | Where-Object { -not $_.Path }).Count -gt 0
if ($laeuftErhoeht -and -not $istAdmin) { $helferArgs.Verb = 'RunAs' }

Start-Process @helferArgs

Write-Output ('Die UpdateZentrale schliesst sich gleich und startet in der neuen Fassung neu (Protokoll: ' + $helferLog + ').')
Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=started'
