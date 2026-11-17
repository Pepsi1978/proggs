# Einmalige Einrichtung (als Administrator, ein UAC-Klick):
#  1. Microsoft-Store-Automatikupdates per Richtlinie aus
#  2. Geplante Aufgabe "Spielmodus-Waechter" (bei Anmeldung, hoechste Rechte, unsichtbar)
#  3. Desktop-Verknuepfung "Spielmodus an-aus" (Entfernen nur per CLI ueber zurueck.ps1)
#  4. Intel-WLAN-Treiber aktualisieren (nur bei gueltiger Signatur), alter Treiber wird gesichert
#  5. WLAN: 5 GHz bevorzugen, Roaming-Dynamik am niedrigsten, mit "Tenda 5 Ghz" verbinden
param([string]$TreiberZip)

$ErrorActionPreference = 'Continue'
$hier  = $PSScriptRoot
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
New-Item -ItemType Directory -Force $daten | Out-Null
$log = Join-Path $daten 'einrichten.log'
function L([string]$t) { Add-Content $log ('{0:HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8 }
L '=== Einrichtung gestartet ==='

# 1. Store-Automatikupdates aus
try {
    $k = 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore'
    if (-not (Test-Path $k)) { New-Item $k | Out-Null }
    Set-ItemProperty $k -Name AutoDownload -Value 2 -Type DWord
    L 'Store-Richtlinie AutoDownload=2 gesetzt'
} catch { L "FEHLER Store-Richtlinie: $($_.Exception.Message)" }

# 2. Geplante Aufgabe
try {
    $pwsh = (Get-Command pwsh).Source
    $arg  = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$hier\waechter.ps1`""
    $akt  = New-ScheduledTaskAction -Execute "$env:WINDIR\System32\conhost.exe" -Argument $arg
    $trig = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
    $pri  = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Highest
    $set  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
    Register-ScheduledTask -TaskName 'Spielmodus-Waechter' -Action $akt -Trigger $trig -Principal $pri -Settings $set -Force | Out-Null
    Start-ScheduledTask -TaskName 'Spielmodus-Waechter'
    L 'Aufgabe Spielmodus-Waechter registriert und gestartet'
} catch { L "FEHLER Aufgabe: $($_.Exception.Message)" }

# 3. Desktop-Verknuepfungen
try {
    $desk = [Environment]::GetFolderPath('Desktop')
    $sh = New-Object -ComObject WScript.Shell
    $v = $sh.CreateShortcut((Join-Path $desk 'Spielmodus an-aus.lnk'))
    $v.TargetPath = "$env:WINDIR\System32\conhost.exe"
    $v.Arguments = "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$hier\umschalten.ps1`""
    $v.IconLocation = "$env:WINDIR\System32\imageres.dll,101"; $v.Save()
    L "Verknuepfungen auf $desk angelegt"
} catch { L "FEHLER Verknuepfungen: $($_.Exception.Message)" }

# 4. Treiber
if ($TreiberZip -and (Test-Path $TreiberZip)) {
    try {
        $alt = Get-CimInstance Win32_PnPSignedDriver | Where-Object DeviceName -match 'AX201' | Select-Object -First 1
        L "Alter Treiber: $($alt.DriverVersion) ($($alt.InfName))"
        $bak = Join-Path $daten "treiber-sicherung-$($alt.DriverVersion)"
        New-Item -ItemType Directory -Force $bak | Out-Null
        pnputil /export-driver $alt.InfName $bak | Out-Null
        L "Alter Treiber gesichert nach $bak"
        $ziel = Join-Path $daten 'treiber-neu'
        if (Test-Path $ziel) { Remove-Item $ziel -Recurse -Force }
        Expand-Archive $TreiberZip $ziel -Force
        $cats = Get-ChildItem $ziel -Recurse -Filter *.cat
        $schlecht = @($cats | Where-Object { (Get-AuthenticodeSignature $_.FullName).Status -ne 'Valid' })
        L "Kataloge: $($cats.Count), ungueltig signiert: $($schlecht.Count)"
        if ($cats.Count -gt 0 -and $schlecht.Count -eq 0) {
            $out = pnputil /add-driver (Join-Path $ziel '*.inf') /subdirs /install 2>&1
            L ("pnputil: " + (($out | Select-Object -Last 6) -join ' / '))
            for ($i = 0; $i -lt 60; $i++) { if ((Get-NetAdapter -Name WLAN -ErrorAction SilentlyContinue).Status -eq 'Up') { break }; Start-Sleep 1 }
        } else { L 'Treiber NICHT installiert (Signaturpruefung)' }
    } catch { L "FEHLER Treiber: $($_.Exception.Message)" }
} else { L 'Kein Treiber-ZIP uebergeben, Treiber-Schritt uebersprungen' }

# 5. WLAN-Einstellungen nach dem Treiber, ueber Registrierungswerte
try {
    Set-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamingPreferredBandType -RegistryValue 2 -ErrorAction Stop
    Set-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamAggressiveness -RegistryValue 0 -ErrorAction Stop
    L 'Bevorzugtes Band=5 GHz (2), Roaming-Dynamik=am niedrigsten (0) gesetzt'
} catch { L "FEHLER WLAN-Eigenschaften: $($_.Exception.Message)" }
for ($i = 0; $i -lt 40; $i++) { if ((Get-NetAdapter -Name WLAN -ErrorAction SilentlyContinue).Status -eq 'Up') { break }; Start-Sleep 1 }
netsh wlan connect name="Tenda 5 Ghz" interface="WLAN" | Out-Null
Start-Sleep 8

$neu = Get-CimInstance Win32_PnPSignedDriver | Where-Object DeviceName -match 'AX201' | Select-Object -First 1
$p = Get-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamingPreferredBandType, RoamAggressiveness
L "Ergebnis: Treiber $($neu.DriverVersion) | $(($p | ForEach-Object { "$($_.DisplayName)=$($_.DisplayValue)" }) -join ' | ')"
L "WLAN: $((netsh wlan show interfaces | Select-String 'SSID\s+:|Kanal|Empfangsrate') -join ' ')"
L '=== FERTIG ==='
