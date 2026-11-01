# Baut Jarvis Windows HP, installiert nach %LOCALAPPDATA%\Programs\JarvisWindowsHP, richtet den Wecker ein und startet die App.
# Aufruf: pwsh -File publish.ps1            → bauen, installieren, starten
#         pwsh -File publish.ps1 -NurBauen  → nur bauen
param([switch]$NurBauen)
$ErrorActionPreference = 'Stop'
$projekt = $PSScriptRoot
$ziel = Join-Path $env:LOCALAPPDATA 'Programs\JarvisWindowsHP'
$aus = Join-Path $projekt 'publish'

dotnet publish (Join-Path $projekt 'JarvisWindows.csproj') -c Release -r win-x64 --self-contained true -o $aus --nologo -v q
if ($LASTEXITCODE -ne 0) { throw 'Bau fehlgeschlagen' }
Copy-Item (Join-Path $projekt 'tools\hp-wecker.ps1') (Join-Path $aus 'hp-wecker.ps1') -Force
if ($NurBauen) { Write-Output "Gebaut: $aus"; return }

# Fenster schließen versteckt die App nur. Sauber beendet sie erst --beenden: Einstellungen, Zugang und Gespräche kommen dann noch auf die Platte.
function Beende-App {
    $laufend = @(Get-Process JarvisWindows -ErrorAction SilentlyContinue)
    if ($laufend.Count -eq 0) { return }
    $alt = Join-Path $ziel 'JarvisWindows.exe'
    if (Test-Path $alt) {
        $bote = Start-Process -FilePath $alt -ArgumentList '--beenden' -PassThru
        $null = $bote.WaitForExit(5000)
    }
    foreach ($p in $laufend) {
        if (-not $p.WaitForExit(10000)) { Stop-Process -Id $p.Id -Force; $null = $p.WaitForExit(3000) }
    }
}
New-Item -ItemType Directory -Force $ziel | Out-Null
$marke = Join-Path $env:LOCALAPPDATA 'JarvisWindows\installiert-gerade'
New-Item -ItemType Directory -Force (Split-Path $marke) | Out-Null
Set-Content -Path $marke -Value (Get-Date -Format o)
# Der Wecker oder Frank kann die App während des Kopierens neu starten; sie sperrt dann ihre Dateien. Robocopy wartet von sich aus fast ewig (/R:1000000), darum kurze Versuche und neu beenden.
try {
    for ($versuch = 1; $versuch -le 5; $versuch++) {
        Beende-App
        robocopy $aus $ziel /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
        if ($LASTEXITCODE -lt 8) { break }
        if ($versuch -eq 5) { throw "Kopieren fehlgeschlagen ($LASTEXITCODE)" }
    }
} finally {
    Remove-Item $marke -Force -ErrorAction SilentlyContinue
}
$global:LASTEXITCODE = 0

$exe = Join-Path $ziel 'JarvisWindows.exe'
$startmenue = Join-Path ([Environment]::GetFolderPath('Programs')) 'Jarvis Windows HP.lnk'
$sh = New-Object -ComObject WScript.Shell
$lnk = $sh.CreateShortcut($startmenue)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $ziel
$lnk.Save()
$desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Jarvis Windows HP.lnk'
$lnk = $sh.CreateShortcut($desktop)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $ziel
$lnk.Save()

# Wecker: hält den HP beim Relay als verbunden und startet die App, wenn das Handy es verlangt.
# Aus dem Installationsordner: Ein git pull oder ein verschobenes Repo ändert so kein laufendes Skript.
$wecker = Join-Path $ziel 'hp-wecker.ps1'
$pwsh = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell).Source }
$conhost = Join-Path $env:SystemRoot 'System32\conhost.exe'
$aktion = New-ScheduledTaskAction -Execute $conhost -Argument "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$wecker`""
$benutzer = "$env:USERDOMAIN\$env:USERNAME"
# Wächter wie bei der Jarvis-PC-Brücke: Stirbt der Wecker, wäre der HP sonst bis zur nächsten Anmeldung für das Handy nicht verbunden. Läuft er schon, beendet sich der neue sofort (Sperre im Skript).
$ausloeser = @(
    (New-ScheduledTaskTrigger -AtLogOn -User $benutzer),
    (New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1))
)
$regeln = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit ([TimeSpan]::Zero) `
    -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1)
$haupt = New-ScheduledTaskPrincipal -UserId $benutzer -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'JarvisWindowsWecker' -Action $aktion -Trigger $ausloeser -Settings $regeln -Principal $haupt -Force | Out-Null
# Neu starten, damit die neue Fassung gilt. Stop-ScheduledTask allein verwaiste die pwsh (siehe Jarvis-PC-Brücke); darum den Prozess selbst beenden.
Get-CimInstance Win32_Process -Filter "Name='pwsh.exe' OR Name='powershell.exe'" | Where-Object { $_.CommandLine -like '*hp-wecker.ps1*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
for ($i = 0; $i -lt 10 -and (Get-ScheduledTask -TaskName 'JarvisWindowsWecker').State -eq 'Running'; $i++) { Start-Sleep -Milliseconds 500 }
Start-ScheduledTask -TaskName 'JarvisWindowsWecker'

Start-Process -FilePath $exe
Write-Output "Installiert und gestartet: $exe"
