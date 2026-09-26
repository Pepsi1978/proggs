# update-zentrale.ps1 -- Selbst-Update der UpdateZentrale (Windows).
#
# Gegenstueck zu UpdaterZentrale-macOS/update-zentrale.sh. Ablauf:
#   1. Ja/Nein-Fenster (240 s, ohne Klick gilt "Nein") -- ohne Ja passiert nichts,
#   2. in einen Zwischenordner bauen: die laufende UpdateZentrale.exe ist gesperrt, ein Build in
#      bin\Release wuerde am Kopieren scheitern,
#   3. einen abgekoppelten Helfer starten, der die laufende UpdateZentrale sauber schliesst, den
#      Zwischenstand nach bin\Release kopiert und die neue Fassung startet. Abgekoppelt, weil die
#      App nicht auf ein Skript warten kann, das sie selbst beendet.
#
# Statuszeile fuer die App: UPDATEZENTRALE_UPDATE_STATUS=started|cancelled|no-answer
# Aufruf:  pwsh -File "$env:USERPROFILE\proggs\UpdateZentrale\update-zentrale.ps1"
$ErrorActionPreference = 'Stop'

$projektDir = $PSScriptRoot
$zielDir    = Join-Path $projektDir 'bin\Release\net8.0-windows10.0.19041.0\win-x64'
$zwischen   = Join-Path $projektDir 'obj\selbstupdate'
$exe        = Join-Path $zielDir 'UpdateZentrale.exe'

$antwort = (New-Object -ComObject WScript.Shell).Popup(
    "Die UpdateZentrale wird jetzt neu gebaut, geschlossen und in der neuen Fassung gestartet.`n`nJetzt aktualisieren?",
    240, 'UpdateZentrale aktualisieren', 4 + 32 + 4096)
if ($antwort -eq -1) { Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=no-answer'; exit 0 }
if ($antwort -ne 6)  { Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=cancelled'; exit 0 }

if (Test-Path $zwischen) { Remove-Item $zwischen -Recurse -Force }
dotnet build (Join-Path $projektDir 'UpdateZentrale.csproj') -c Release -o $zwischen
if ($LASTEXITCODE -ne 0) { Write-Error "Build fehlgeschlagen (Exit $LASTEXITCODE)."; exit 1 }

$helfer = @"
Start-Sleep -Seconds 3
Get-Process UpdateZentrale -ErrorAction SilentlyContinue | ForEach-Object { `$null = `$_.CloseMainWindow() }
for (`$i = 0; `$i -lt 60 -and (Get-Process UpdateZentrale -ErrorAction SilentlyContinue); `$i++) { Start-Sleep -Milliseconds 500 }
Get-Process UpdateZentrale -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
robocopy '$zwischen' '$zielDir' /E /NFL /NDL /NJH /NJS | Out-Null
Start-Process '$exe'
"@
$shell = (Get-Process -Id $PID).Path
$helferArgs = @{ FilePath = $shell; WindowStyle = 'Hidden'; ArgumentList = @('-NoProfile', '-Command', $helfer) }

# Laeuft die UpdateZentrale als Administrator, kann ein Helfer ohne Adminrechte ihr Fenster weder
# schliessen (UIPI) noch den Prozess beenden -- die gesperrte exe bliebe stehen und das Update
# verpuffte still. Der Pfad eines erhoehten Prozesses ist von hier aus nicht lesbar: daran erkannt.
$istAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
$laeuftErhoeht = @(Get-Process UpdateZentrale -ErrorAction SilentlyContinue | Where-Object { -not $_.Path }).Count -gt 0
if ($laeuftErhoeht -and -not $istAdmin) { $helferArgs.Verb = 'RunAs' }

Start-Process @helferArgs

Write-Output 'Die UpdateZentrale startet sich in wenigen Sekunden neu.'
Write-Output 'UPDATEZENTRALE_UPDATE_STATUS=started'
