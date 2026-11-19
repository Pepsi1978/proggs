# Baut Jarvis Windows HP, installiert nach %LOCALAPPDATA%\Programs\JarvisWindowsHP, richtet den Wecker ein und startet die App.
# Aufruf: pwsh -File publish.ps1                 → bauen, installieren, starten (HP-Laptop)
#         pwsh -File publish.ps1 -Geraet power   → dasselbe für den Power-PC: Ordner JarvisWindowsPower, Aufgabe JarvisWindowsWeckerPower,
#                                                  Verknüpfung "Jarvis Windows Power-PC.lnk", geraet.json neben der EXE
#         pwsh -File publish.ps1 -NurBauen       → nur bauen
param([switch]$NurBauen, [ValidateSet('hp', 'power')][string]$Geraet = 'hp')
$ErrorActionPreference = 'Stop'
$projekt = $PSScriptRoot
if ($Geraet -eq 'power') {
    $ordnerName = 'JarvisWindowsPower'; $aufgabe = 'JarvisWindowsWeckerPower'; $lnkName = 'Jarvis Windows Power-PC.lnk'
} else {
    $ordnerName = 'JarvisWindowsHP'; $aufgabe = 'JarvisWindowsWecker'; $lnkName = 'Jarvis Windows HP.lnk'
}
$ziel = Join-Path $env:LOCALAPPDATA "Programs\$ordnerName"
$aus = Join-Path $projekt 'publish'

dotnet publish (Join-Path $projekt 'JarvisWindows.csproj') -c Release -r win-x64 --self-contained true -o $aus --nologo -v q
if ($LASTEXITCODE -ne 0) { throw 'Bau fehlgeschlagen' }
Copy-Item (Join-Path $projekt 'tools\hp-wecker.ps1') (Join-Path $aus 'hp-wecker.ps1') -Force
# Identität für App und Wecker. Ohne -Geraet keine Datei (HP wie bisher); robocopy /MIR entfernt sie dann auch im Ziel.
$geraetDatei = Join-Path $aus 'geraet.json'
if ($Geraet -eq 'hp') { Remove-Item -LiteralPath $geraetDatei -Force -ErrorAction SilentlyContinue }
else { [System.IO.File]::WriteAllText($geraetDatei, "{`"geraet`":`"$Geraet`"}", [System.Text.UTF8Encoding]::new($false)) }
if ($NurBauen) { Write-Output "Gebaut: $aus"; return }

# Fenster schließen versteckt die App nur. Sauber beendet sie erst --beenden: Einstellungen, Zugang und Gespräche kommen dann noch auf die Platte.
# Nur die Fassung aus diesem Zielordner: Läuft auf demselben Rechner eine andere Identität, bleibt sie unberührt.
function App-Prozesse([string]$ordner) {
    $exePfad = Join-Path $ordner 'JarvisWindows.exe'
    @(Get-Process JarvisWindows -ErrorAction SilentlyContinue | Where-Object { $_.Path -and $_.Path -ieq $exePfad })
}
function Wecker-Prozesse([string]$ordner) {
    $muster = '*' + (Join-Path $ordner 'hp-wecker.ps1') + '*'
    @(Get-CimInstance Win32_Process -Filter "Name='pwsh.exe' OR Name='powershell.exe'" | Where-Object { "$($_.CommandLine)" -like $muster })
}
function Beende-App([string]$ordner = $ziel) {
    $laufend = App-Prozesse $ordner
    if ($laufend.Count -eq 0) { return }
    $alt = Join-Path $ordner 'JarvisWindows.exe'
    if (Test-Path $alt) {
        $bote = Start-Process -FilePath $alt -ArgumentList '--beenden' -PassThru
        $null = $bote.WaitForExit(5000)
    }
    foreach ($p in $laufend) {
        if (-not $p.WaitForExit(10000)) { Stop-Process -Id $p.Id -Force; $null = $p.WaitForExit(3000) }
    }
}
# Auf dem Power-PC soll nur die Power-Identität laufen: Eine vorhandene HP-Installation (Ordner JarvisWindowsHP, Aufgabe
# JarvisWindowsWecker, HP-Verknüpfungen) wird beendet und entfernt. Daten unter %LOCALAPPDATA%\JarvisWindows bleiben.
if ($Geraet -eq 'power') {
    $hpOrdner = Join-Path $env:LOCALAPPDATA 'Programs\JarvisWindowsHP'
    $hpLnk = 'Jarvis Windows HP.lnk'
    $hpLnks = @((Join-Path ([Environment]::GetFolderPath('Programs')) $hpLnk), (Join-Path ([Environment]::GetFolderPath('Desktop')) $hpLnk))
    $hpAufgabe = Get-ScheduledTask -TaskName 'JarvisWindowsWecker' -ErrorAction SilentlyContinue
    if ((Test-Path -LiteralPath $hpOrdner) -or $hpAufgabe -or ($hpLnks | Where-Object { Test-Path -LiteralPath $_ })) {
        Write-Warning 'HP-Installation auf diesem Rechner gefunden: Auf dem Power-PC läuft nur Jarvis Windows Power-PC. Die HP-Fassung wird beendet und entfernt.'
        if ($hpAufgabe) { Unregister-ScheduledTask -TaskName 'JarvisWindowsWecker' -Confirm:$false }
        Wecker-Prozesse $hpOrdner | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        if (Test-Path -LiteralPath $hpOrdner) { Beende-App $hpOrdner }
        $hpLnks | Where-Object { Test-Path -LiteralPath $_ } | ForEach-Object { Remove-Item -LiteralPath $_ -Force }
        if (Test-Path -LiteralPath $hpOrdner) {
            try { Remove-Item -LiteralPath $hpOrdner -Recurse -Force; Write-Output "Entfernt: $hpOrdner" }
            catch { Write-Warning "HP-Ordner ließ sich nicht ganz löschen ($($_.Exception.Message)); Aufgabe und Verknüpfungen sind weg, er startet nicht mehr." }
        }
    }
}

New-Item -ItemType Directory -Force $ziel | Out-Null
$marke = Join-Path $env:LOCALAPPDATA ('JarvisWindows\installiert-gerade' + $(if ($Geraet -eq 'hp') { '' } else { "-$Geraet" }))
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
$startmenue = Join-Path ([Environment]::GetFolderPath('Programs')) $lnkName
$sh = New-Object -ComObject WScript.Shell
$lnk = $sh.CreateShortcut($startmenue)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $ziel
$lnk.Save()
$desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) $lnkName
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
Register-ScheduledTask -TaskName $aufgabe -Action $aktion -Trigger $ausloeser -Settings $regeln -Principal $haupt -Force | Out-Null
# Neu starten, damit die neue Fassung gilt. Stop-ScheduledTask allein verwaiste die pwsh (siehe Jarvis-PC-Brücke); darum den Prozess selbst beenden.
# Nur den Wecker dieses Zielordners: Der Wecker einer anderen Identität läuft weiter.
Wecker-Prozesse $ziel | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
for ($i = 0; $i -lt 10 -and (Get-ScheduledTask -TaskName $aufgabe).State -eq 'Running'; $i++) { Start-Sleep -Milliseconds 500 }
Start-ScheduledTask -TaskName $aufgabe

Start-Process -FilePath $exe
Write-Output "Installiert und gestartet: $exe"
