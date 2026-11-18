# Removes the game mode completely (elevates itself). Use via CLI only.
# Driver and Wi-Fi adapter settings stay; old driver: %LOCALAPPDATA%\Spielmodus\treiber-sicherung-*.
$id = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process (Get-Process -Id $PID).Path -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$fehler = @()

Stop-ScheduledTask -TaskName 'Spielmodus-Waechter' -ErrorAction SilentlyContinue
Unregister-ScheduledTask -TaskName 'Spielmodus-Waechter' -Confirm:$false -ErrorAction SilentlyContinue
Get-CimInstance Win32_Process -Filter "Name='pwsh.exe'" | Where-Object { $_.CommandLine -match 'spielmodus\\waechter\.ps1' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

# Firewall: outbound back to the saved default (never stay on Block), remove all game-mode rules
$fwSicher = Join-Path $daten 'firewall.json'
$fwv = $null
if (Test-Path $fwSicher) { try { $fwv = Get-Content $fwSicher -Raw | ConvertFrom-Json } catch { } }
foreach ($pr in @('Domain', 'Private', 'Public')) {
    $w = if ($fwv -and $fwv.$pr -and $fwv.$pr -ne 'Block') { $fwv.$pr } else { 'NotConfigured' }
    try { Set-NetFirewallProfile -Name $pr -DefaultOutboundAction $w -ErrorAction Stop } catch { $fehler += "Firewall ${pr}: $($_.Exception.Message)" }
}
Remove-NetFirewallRule -Group 'Spielmodus' -ErrorAction SilentlyContinue
Remove-NetFirewallRule -Group 'Spielmodus-Erlaubt' -ErrorAction SilentlyContinue
Remove-Item $fwSicher -ErrorAction SilentlyContinue

# InstallService back to its original start type (never 'Disabled')
$typ = 'Manual'
$sicher = Join-Path $daten 'starttypen.json'
if (Test-Path $sicher) { try { $t = (Get-Content $sicher -Raw | ConvertFrom-Json).InstallService; if ($t -and $t -ne 'Disabled') { $typ = $t } } catch { } }
try { Set-Service InstallService -StartupType $typ -ErrorAction Stop } catch { $fehler += "InstallService: $($_.Exception.Message)" }

# Restore saved original values; remove values that did not exist before
$v = $null
$vorherDatei = Join-Path $daten 'vorher.json'
if (Test-Path $vorherDatei) { try { $v = Get-Content $vorherDatei -Raw | ConvertFrom-Json } catch { } }
function Restore-Wert([string]$key, [string]$name, $wert) {
    try {
        if ($null -eq $wert) { Remove-ItemProperty $key -Name $name -ErrorAction SilentlyContinue }
        else { if (-not (Test-Path $key)) { New-Item $key -Force | Out-Null }; Set-ItemProperty $key -Name $name -Value ([int]$wert) -Type DWord -ErrorAction Stop }
    } catch { $script:fehler += "${name}: $($_.Exception.Message)" }
}
Restore-Wert 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' 'AutoDownload' $v.StoreAutoDownload
Restore-Wert 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' 'DOPercentageMaxBackgroundBandwidth' $v.DoBackground
Restore-Wert 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' 'DOPercentageMaxForegroundBandwidth' $v.DoForeground
if ($v) {
    Restore-Wert 'HKCU:\System\GameConfigStore' 'GameDVR_Enabled' $v.GameDVR_Enabled
    Restore-Wert 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' 'AppCaptureEnabled' $v.AppCaptureEnabled
}

$desk = [Environment]::GetFolderPath('Desktop')
Remove-Item (Join-Path $desk 'Spielmodus an-aus.lnk'), (Join-Path $desk 'Spielmodus zurueck.lnk') -ErrorAction SilentlyContinue
Remove-Item (Join-Path $daten 'aus.flag'), $sicher, $vorherDatei -ErrorAction SilentlyContinue

Add-Type -AssemblyName System.Windows.Forms
$text = if ($fehler) { "Spielmodus entfernt, aber mit Fehlern:`n" + ($fehler -join "`n") } else { 'Spielmodus entfernt: Store, Firewall, Google Drive, Übermittlungsoptimierung und Game-Bar-Aufnahme sind wieder wie vorher.' }
[System.Windows.Forms.MessageBox]::Show($text, 'Spielmodus') | Out-Null
