# One-time setup (elevates itself). Steps:
#  0. Copy the scripts to %ProgramFiles%\Spielmodus (admin-only) - the elevated tasks run ONLY from
#     there, never from the user-writable git checkout (would be a UAC bypass)
#  1. Turn off Microsoft Store auto-updates (policy), cap Delivery Optimization, no peer uploads
#  2. Turn off Game Bar background recording (causes the ms-gamingoverlay popup when Game Bar is missing)
#  3. Scheduled tasks "Spielmodus-Waechter" (at logon, highest privileges, hidden) and
#     "Spielmodus-Notfall" (SYSTEM, at boot: restores a leftover game firewall); stops an old watcher first
#  4. Desktop shortcut "Spielmodus an-aus" (removal only via CLI: zurueck.ps1)
#  5. Optional: update the Intel Wi-Fi driver (-TreiberZip), old driver is backed up first
#  6. Wi-Fi: prefer 5 GHz, lowest roaming aggressiveness (adapter restart only if something changed)
# Original values are stored in %LOCALAPPDATA%\Spielmodus\vorher.json for zurueck.ps1.
param([string]$TreiberZip)

$id = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $a = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    # Resolve before elevating: the elevated process starts in System32, a relative path would break.
    if ($TreiberZip) { $a += " -TreiberZip `"$((Resolve-Path -LiteralPath $TreiberZip -ErrorAction SilentlyContinue).Path ?? $TreiberZip)`"" }
    Start-Process (Get-Process -Id $PID).Path -Verb RunAs -ArgumentList $a
    exit
}

$ErrorActionPreference = 'Continue'
$hier  = $PSScriptRoot
$pwsh  = (Get-Process -Id $PID).Path
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$ziel  = Join-Path $env:ProgramFiles 'Spielmodus'
New-Item -ItemType Directory -Force $daten | Out-Null
$log = Join-Path $daten 'einrichten.log'
function L([string]$t) { Add-Content $log ('{0:HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8 }
L '=== Einrichtung gestartet ==='

# Remember original values once (for zurueck.ps1); add keys introduced by newer versions
$vorherDatei = Join-Path $daten 'vorher.json'
$doKey = 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization'
if (-not (Test-Path $vorherDatei)) {
    $v = [ordered]@{
        GameDVR_Enabled   = (Get-ItemProperty 'HKCU:\System\GameConfigStore' -ErrorAction SilentlyContinue).GameDVR_Enabled
        AppCaptureEnabled = (Get-ItemProperty 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' -ErrorAction SilentlyContinue).AppCaptureEnabled
        StoreAutoDownload = (Get-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' -ErrorAction SilentlyContinue).AutoDownload
        DoBackground      = (Get-ItemProperty $doKey -ErrorAction SilentlyContinue).DOPercentageMaxBackgroundBandwidth
        DoForeground      = (Get-ItemProperty $doKey -ErrorAction SilentlyContinue).DOPercentageMaxForegroundBandwidth
        DoDownloadMode    = (Get-ItemProperty $doKey -ErrorAction SilentlyContinue).DODownloadMode
    }
    $v | ConvertTo-Json | Set-Content $vorherDatei -Encoding utf8
    L "Ursprungswerte gesichert: $($v | ConvertTo-Json -Compress)"
} else {
    $v = Get-Content $vorherDatei -Raw | ConvertFrom-Json
    if ($v.PSObject.Properties.Name -notcontains 'DoDownloadMode') {
        $v | Add-Member -NotePropertyName DoDownloadMode -NotePropertyValue ((Get-ItemProperty $doKey -ErrorAction SilentlyContinue).DODownloadMode)
        $v | ConvertTo-Json | Set-Content $vorherDatei -Encoding utf8
        L "Ursprungswert ergänzt: DoDownloadMode=$($v.DoDownloadMode)"
    }
}

function Set-Dword([string]$key, [string]$name, [int]$wert) {
    if (-not (Test-Path $key)) { New-Item $key -Force | Out-Null }
    Set-ItemProperty $key -Name $name -Value $wert -Type DWord
}

# 0. Admin-only copy of the scripts + prebuilt status-window class
try {
    New-Item -ItemType Directory -Force $ziel | Out-Null
    Copy-Item (Join-Path $hier '*.ps1') $ziel -Force -ErrorAction Stop
    & $pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ziel 'meldung.ps1') -Bauen (Join-Path $ziel 'meldung.dll') *> $null
    L "Skripte nach $ziel kopiert (meldung.dll: $(Test-Path (Join-Path $ziel 'meldung.dll')))"
} catch { L "FEHLER Kopie nach ${ziel}: $($_.Exception.Message)" }

# 1. Store auto-updates off, Delivery Optimization capped and without peer uploads (HTTP only)
try {
    Set-Dword 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' 'AutoDownload' 2
    Set-Dword $doKey 'DOPercentageMaxBackgroundBandwidth' 10
    Set-Dword $doKey 'DOPercentageMaxForegroundBandwidth' 50
    Set-Dword $doKey 'DODownloadMode' 0
    L 'Store AutoDownload=2, Übermittlungsoptimierung Hintergrund 10 %, Vordergrund 50 %, kein Hochladen an andere PCs'
} catch { L "FEHLER Richtlinien: $($_.Exception.Message)" }

# 2. Game Bar background recording off
try {
    Set-Dword 'HKCU:\System\GameConfigStore' 'GameDVR_Enabled' 0
    Set-Dword 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' 'AppCaptureEnabled' 0
    L 'Game-Bar-Aufnahme aus'
} catch { L "FEHLER Game Bar: $($_.Exception.Message)" }

# 3. Scheduled tasks (stop an old watcher first so only one instance runs; pwsh or Windows PowerShell)
try {
    Stop-ScheduledTask -TaskName 'Spielmodus-Waechter' -ErrorAction SilentlyContinue
    Get-CimInstance Win32_Process -Filter "Name='pwsh.exe' OR Name='powershell.exe'" | Where-Object { $_.CommandLine -match 'spielmodus\\waechter\.ps1' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    $arg  = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$ziel\waechter.ps1`""
    $akt  = New-ScheduledTaskAction -Execute "$env:WINDIR\System32\conhost.exe" -Argument $arg
    $trig = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
    $pri  = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Highest
    $set  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew -Priority 7
    Register-ScheduledTask -TaskName 'Spielmodus-Waechter' -Action $akt -Trigger $trig -Principal $pri -Settings $set -Force -ErrorAction Stop | Out-Null
    L 'Aufgabe Spielmodus-Waechter registriert'

    # Boot safety net: SYSTEM, before any logon; restores a leftover game firewall (PC never stays offline)
    $nAkt = New-ScheduledTaskAction -Execute $pwsh -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$ziel\notfall.ps1`" -Daten `"$daten`""
    $nTrig = New-ScheduledTaskTrigger -AtStartup
    $nPri = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    $nSet = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
    Register-ScheduledTask -TaskName 'Spielmodus-Notfall' -Action $nAkt -Trigger $nTrig -Principal $nPri -Settings $nSet -Force -ErrorAction Stop | Out-Null
    L 'Aufgabe Spielmodus-Notfall (Systemstart, SYSTEM) registriert'

    Start-ScheduledTask -TaskName 'Spielmodus-Waechter'
    L 'Wächter gestartet'
} catch { L "FEHLER Aufgabe: $($_.Exception.Message)" }

# 4. Desktop shortcut
try {
    $desk = [Environment]::GetFolderPath('Desktop')
    Remove-Item (Join-Path $desk 'Spielmodus zurueck.lnk') -ErrorAction SilentlyContinue
    $sh = New-Object -ComObject WScript.Shell
    $v = $sh.CreateShortcut((Join-Path $desk 'Spielmodus an-aus.lnk'))
    $v.TargetPath = "$env:WINDIR\System32\conhost.exe"
    $v.Arguments = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$ziel\umschalten.ps1`""
    $v.IconLocation = "$env:WINDIR\System32\imageres.dll,101"; $v.Save()
    L "Verknüpfung 'Spielmodus an-aus' auf $desk angelegt"
} catch { L "FEHLER Verknüpfung: $($_.Exception.Message)" }

# Wi-Fi adapter by medium type (802.11), not by display name ("WLAN" may become "WLAN 2" after a driver change)
. (Join-Path $hier 'gemeinsam.ps1')
$wlan = Get-WlanName
function Wait-WlanUp([int]$sek) {
    for ($i = 0; $i -lt $sek; $i++) { $n = Get-WlanName; if ($n -and (Get-NetAdapter -Name $n -ErrorAction SilentlyContinue).Status -eq 'Up') { return $true }; Start-Sleep 1 }
    return $false
}

# 5. Driver (optional)
if ($TreiberZip -and (Test-Path -LiteralPath $TreiberZip)) {
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
        $zielTr = Join-Path $daten 'treiber-neu'
        if (Test-Path $zielTr) { Remove-Item $zielTr -Recurse -Force }
        Expand-Archive -LiteralPath $TreiberZip $zielTr -Force
        $infs = @(Get-ChildItem $zielTr -Recurse -Filter 'netwtw*.inf')
        $ok = $infs.Count -gt 0
        foreach ($i in $infs) {
            # Check exactly the catalog the INF names, and the publisher exactly (not "contains Intel")
            $catName = ([regex]::Match((Get-Content -LiteralPath $i.FullName -Raw), '(?im)^\s*CatalogFile(\.NTamd64)?\s*=\s*(\S+)')).Groups[2].Value
            $cat = if ($catName) { Join-Path $i.DirectoryName $catName } else { $null }
            $sig = if ($cat -and (Test-Path -LiteralPath $cat)) { Get-AuthenticodeSignature -LiteralPath $cat } else { $null }
            $sub = if ($sig) { $sig.SignerCertificate.Subject } else { '' }
            if (-not $sig -or $sig.Status -ne 'Valid' -or ($sub -notmatch '^CN=Microsoft Windows Hardware Compatibility Publisher,' -and $sub -notmatch '(^|, )O=Intel Corporation(,|$)')) { $ok = $false; L "Signatur ungültig: $($i.FullName) ($catName, $sub)" }
        }
        if ($ok) {
            foreach ($i in $infs) { $out = pnputil /add-driver $i.FullName /install 2>&1; L ("pnputil $($i.Name): " + (($out | Select-Object -Last 3) -join ' / ')) }
            Start-Sleep 5
            [void](Wait-WlanUp 60)
            $wlan = Get-WlanName
        } else { L 'Treiber NICHT installiert (Signaturprüfung)' }
    } catch { L "FEHLER Treiber: $($_.Exception.Message)" }
} elseif ($TreiberZip) { L "Treiber-ZIP nicht gefunden: $TreiberZip" }
else { L 'Kein Treiber-ZIP übergeben, Treiber-Schritt übersprungen' }

# 6. Wi-Fi properties (by registry value). Restart ONLY if a value really changed - a rerun during a
#    match must not drop the connection.
if ($wlan) {
    $fehler = @(); $geaendert = $false
    foreach ($p in @(@('RoamingPreferredBandType', 2), @('RoamAggressiveness', 0))) {
        $ist = (Get-NetAdapterAdvancedProperty -Name $wlan -RegistryKeyword $p[0] -ErrorAction SilentlyContinue).RegistryValue
        if ("$ist" -eq "$($p[1])") { continue }
        try { Set-NetAdapterAdvancedProperty -Name $wlan -RegistryKeyword $p[0] -RegistryValue $p[1] -NoRestart -ErrorAction Stop; $geaendert = $true }
        catch { $fehler += "$($p[0]): $($_.Exception.Message)" }
    }
    if ($fehler) { L ("FEHLER WLAN-Eigenschaften: " + ($fehler -join ' | ')) }
    if ($geaendert) {
        Restart-NetAdapter -Name $wlan -Confirm:$false -ErrorAction SilentlyContinue
        if (-not (Wait-WlanUp 40)) { Enable-NetAdapter -Name $wlan -Confirm:$false -ErrorAction SilentlyContinue; [void](Wait-WlanUp 20) }
        Start-Sleep 6
        $ssid = (netsh wlan show interfaces | Select-String '^\s+SSID\s+:\s+(.+)$').Matches.Groups[1].Value
        if ($ssid -ne 'Tenda 5 Ghz' -and ((netsh wlan show profiles) -match 'Tenda 5 Ghz') -and ($ssid -like 'Tenda*' -or -not $ssid)) {
            netsh wlan connect name="Tenda 5 Ghz" interface="$wlan" | Out-Null
            Start-Sleep 8
        }
    } else { L 'WLAN-Eigenschaften schon richtig, kein Adapter-Neustart' }
    $drv = (Get-NetAdapter -Name $wlan -ErrorAction SilentlyContinue).DriverVersion
    $pp = Get-NetAdapterAdvancedProperty -Name $wlan -RegistryKeyword RoamingPreferredBandType, RoamAggressiveness -ErrorAction SilentlyContinue
    L "Ergebnis ($wlan): Treiber $drv | $(($pp | ForEach-Object { "$($_.DisplayName)=$($_.DisplayValue)" }) -join ' | ')"
    L "WLAN: $((netsh wlan show interfaces | Select-String 'SSID\s+:|Kanal|Empfangsrate') -join ' ')"
} else { L 'Kein WLAN-Adapter gefunden, WLAN-Schritt übersprungen' }
L '=== FERTIG ==='
