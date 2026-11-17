# One-time setup (elevates itself). Steps:
#  1. Turn off Microsoft Store auto-updates (policy) and cap Delivery Optimization bandwidth
#  2. Turn off Game Bar background recording (causes the ms-gamingoverlay popup when Game Bar is missing)
#  3. Scheduled task "Spielmodus-Waechter" (at logon, highest privileges, hidden); stops an old watcher first
#  4. Desktop shortcut "Spielmodus an-aus" (removal only via CLI: zurueck.ps1)
#  5. Optional: update the Intel Wi-Fi driver (-TreiberZip), old driver is backed up first
#  6. Wi-Fi: prefer 5 GHz, lowest roaming aggressiveness (one adapter restart)
# Original values are stored in %LOCALAPPDATA%\Spielmodus\vorher.json for zurueck.ps1.
param([string]$TreiberZip)

$id = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $a = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    if ($TreiberZip) { $a += " -TreiberZip `"$TreiberZip`"" }
    Start-Process (Get-Process -Id $PID).Path -Verb RunAs -ArgumentList $a
    exit
}

$ErrorActionPreference = 'Continue'
$hier  = $PSScriptRoot
$pwsh  = (Get-Process -Id $PID).Path
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
New-Item -ItemType Directory -Force $daten | Out-Null
$log = Join-Path $daten 'einrichten.log'
function L([string]$t) { Add-Content $log ('{0:HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8 }
L '=== Einrichtung gestartet ==='

# Remember original values once (for zurueck.ps1)
$vorherDatei = Join-Path $daten 'vorher.json'
if (-not (Test-Path $vorherDatei)) {
    $v = [ordered]@{
        GameDVR_Enabled   = (Get-ItemProperty 'HKCU:\System\GameConfigStore' -ErrorAction SilentlyContinue).GameDVR_Enabled
        AppCaptureEnabled = (Get-ItemProperty 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' -ErrorAction SilentlyContinue).AppCaptureEnabled
        StoreAutoDownload = (Get-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' -ErrorAction SilentlyContinue).AutoDownload
        DoBackground      = (Get-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' -ErrorAction SilentlyContinue).DOPercentageMaxBackgroundBandwidth
        DoForeground      = (Get-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' -ErrorAction SilentlyContinue).DOPercentageMaxForegroundBandwidth
    }
    $v | ConvertTo-Json | Set-Content $vorherDatei -Encoding utf8
    L "Ursprungswerte gesichert: $($v | ConvertTo-Json -Compress)"
}

function Set-Dword([string]$key, [string]$name, [int]$wert) {
    if (-not (Test-Path $key)) { New-Item $key -Force | Out-Null }
    Set-ItemProperty $key -Name $name -Value $wert -Type DWord
}

# 1. Store auto-updates off, Delivery Optimization capped
try {
    Set-Dword 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' 'AutoDownload' 2
    Set-Dword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' 'DOPercentageMaxBackgroundBandwidth' 10
    Set-Dword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' 'DOPercentageMaxForegroundBandwidth' 50
    L 'Store AutoDownload=2, Übermittlungsoptimierung Hintergrund 10 %, Vordergrund 50 %'
} catch { L "FEHLER Richtlinien: $($_.Exception.Message)" }

# 2. Game Bar background recording off
try {
    Set-Dword 'HKCU:\System\GameConfigStore' 'GameDVR_Enabled' 0
    Set-Dword 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' 'AppCaptureEnabled' 0
    L 'Game-Bar-Aufnahme aus'
} catch { L "FEHLER Game Bar: $($_.Exception.Message)" }

# 3. Scheduled task (stop an old watcher first so only one instance runs)
try {
    Stop-ScheduledTask -TaskName 'Spielmodus-Waechter' -ErrorAction SilentlyContinue
    Get-CimInstance Win32_Process -Filter "Name='pwsh.exe'" | Where-Object { $_.CommandLine -match 'spielmodus\\waechter\.ps1' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    $arg  = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$hier\waechter.ps1`""
    $akt  = New-ScheduledTaskAction -Execute "$env:WINDIR\System32\conhost.exe" -Argument $arg
    $trig = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
    $pri  = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Highest
    $set  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew -Priority 7
    Register-ScheduledTask -TaskName 'Spielmodus-Waechter' -Action $akt -Trigger $trig -Principal $pri -Settings $set -Force -ErrorAction Stop | Out-Null
    Start-ScheduledTask -TaskName 'Spielmodus-Waechter'
    L 'Aufgabe Spielmodus-Waechter registriert und gestartet'
} catch { L "FEHLER Aufgabe: $($_.Exception.Message)" }

# 4. Desktop shortcut
try {
    $desk = [Environment]::GetFolderPath('Desktop')
    Remove-Item (Join-Path $desk 'Spielmodus zurueck.lnk') -ErrorAction SilentlyContinue
    $sh = New-Object -ComObject WScript.Shell
    $v = $sh.CreateShortcut((Join-Path $desk 'Spielmodus an-aus.lnk'))
    $v.TargetPath = "$env:WINDIR\System32\conhost.exe"
    $v.Arguments = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$hier\umschalten.ps1`""
    $v.IconLocation = "$env:WINDIR\System32\imageres.dll,101"; $v.Save()
    L "Verknüpfung 'Spielmodus an-aus' auf $desk angelegt"
} catch { L "FEHLER Verknüpfung: $($_.Exception.Message)" }

function Wait-WlanUp([int]$sek) {
    for ($i = 0; $i -lt $sek; $i++) { if ((Get-NetAdapter -Name WLAN -ErrorAction SilentlyContinue).Status -eq 'Up') { return $true }; Start-Sleep 1 }
    return $false
}

# 5. Driver (optional)
if ($TreiberZip -and (Test-Path $TreiberZip)) {
    try {
        $dev = Get-PnpDevice -Class Net -PresentOnly | Where-Object FriendlyName -match 'AX201' | Select-Object -First 1
        $inf = if ($dev) { (Get-PnpDeviceProperty -InstanceId $dev.InstanceId -KeyName DEVPKEY_Device_DriverInfPath).Data } else { $null }
        $ver = if ($dev) { (Get-PnpDeviceProperty -InstanceId $dev.InstanceId -KeyName DEVPKEY_Device_DriverVersion).Data } else { $null }
        if (-not $inf) { throw 'AX201 oder dessen Treiberdatei nicht gefunden, Treiber-Schritt abgebrochen' }
        $bak = Join-Path $daten "treiber-sicherung-$ver"
        New-Item -ItemType Directory -Force $bak | Out-Null
        pnputil /export-driver $inf $bak | Out-Null
        if ($LASTEXITCODE -ne 0 -or -not (Get-ChildItem $bak -Filter *.inf -ErrorAction SilentlyContinue)) { throw "Sicherung von $inf fehlgeschlagen (Exit $LASTEXITCODE), Treiber-Schritt abgebrochen" }
        L "Alter Treiber $ver ($inf) gesichert nach $bak"
        $ziel = Join-Path $daten 'treiber-neu'
        if (Test-Path $ziel) { Remove-Item $ziel -Recurse -Force }
        Expand-Archive $TreiberZip $ziel -Force
        $infs = @(Get-ChildItem $ziel -Recurse -Filter 'netwtw*.inf')
        $ok = $infs.Count -gt 0
        foreach ($i in $infs) {
            $cat = Get-ChildItem $i.DirectoryName -Filter *.cat | Select-Object -First 1
            $sig = if ($cat) { Get-AuthenticodeSignature $cat.FullName } else { $null }
            if (-not $sig -or $sig.Status -ne 'Valid' -or $sig.SignerCertificate.Subject -notmatch 'Microsoft Windows Hardware Compatibility Publisher|Intel') { $ok = $false; L "Signatur ungültig: $($i.FullName)" }
        }
        if ($ok) {
            foreach ($i in $infs) { $out = pnputil /add-driver $i.FullName /install 2>&1; L ("pnputil $($i.Name): " + (($out | Select-Object -Last 3) -join ' / ')) }
            Start-Sleep 5
            [void](Wait-WlanUp 60)
        } else { L 'Treiber NICHT installiert (Signaturprüfung)' }
    } catch { L "FEHLER Treiber: $($_.Exception.Message)" }
} else { L 'Kein Treiber-ZIP übergeben, Treiber-Schritt übersprungen' }

# 6. Wi-Fi properties (by registry value, independent of display text), one restart
$fehler = @()
foreach ($p in @(@('RoamingPreferredBandType', 2), @('RoamAggressiveness', 0))) {
    try { Set-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword $p[0] -RegistryValue $p[1] -NoRestart -ErrorAction Stop }
    catch { $fehler += "$($p[0]): $($_.Exception.Message)" }
}
if ($fehler) { L ("FEHLER WLAN-Eigenschaften: " + ($fehler -join ' | ')) }
Restart-NetAdapter -Name WLAN -Confirm:$false -ErrorAction SilentlyContinue
[void](Wait-WlanUp 40)
Start-Sleep 6
$ssid = (netsh wlan show interfaces | Select-String '^\s+SSID\s+:\s+(.+)$').Matches.Groups[1].Value
if ($ssid -ne 'Tenda 5 Ghz' -and ((netsh wlan show profiles) -match 'Tenda 5 Ghz') -and ($ssid -like 'Tenda*' -or -not $ssid)) {
    netsh wlan connect name="Tenda 5 Ghz" interface="WLAN" | Out-Null
    Start-Sleep 8
}

$drv = (Get-NetAdapter -Name WLAN -ErrorAction SilentlyContinue).DriverVersion
$pp = Get-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamingPreferredBandType, RoamAggressiveness -ErrorAction SilentlyContinue
L "Ergebnis: Treiber $drv | $(($pp | ForEach-Object { "$($_.DisplayName)=$($_.DisplayValue)" }) -join ' | ')"
L "WLAN: $((netsh wlan show interfaces | Select-String 'SSID\s+:|Kanal|Empfangsrate') -join ' ')"
L '=== FERTIG ==='
