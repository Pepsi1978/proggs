# Macht den Spielmodus komplett rueckgaengig (fragt per UAC nach Adminrechten).
# Treiber und WLAN-Einstellungen bleiben; alter Treiber liegt in %LOCALAPPDATA%\Spielmodus\treiber-sicherung-*.
$id = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process (Get-Command pwsh).Source -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
Stop-ScheduledTask -TaskName 'Spielmodus-Waechter' -ErrorAction SilentlyContinue
Unregister-ScheduledTask -TaskName 'Spielmodus-Waechter' -Confirm:$false -ErrorAction SilentlyContinue
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'spielmodus\\waechter\.ps1' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
$sicher = Join-Path $daten 'starttypen.json'
$s = if (Test-Path $sicher) { Get-Content $sicher -Raw | ConvertFrom-Json } else { $null }
foreach ($d in 'GamingServices', 'GamingServicesNet', 'InstallService') {
    $typ = if ($s -and $s.$d) { $s.$d } elseif ($d -eq 'InstallService') { 'Manual' } else { 'Automatic' }
    Set-Service $d -StartupType $typ -ErrorAction SilentlyContinue
}
Start-Service GamingServices, GamingServicesNet -ErrorAction SilentlyContinue
Remove-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\WindowsStore' -Name AutoDownload -ErrorAction SilentlyContinue
Set-ItemProperty 'HKCU:\System\GameConfigStore' -Name GameDVR_Enabled -Value 1 -Type DWord
Set-ItemProperty 'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\GameDVR' -Name AppCaptureEnabled -Value 1 -Type DWord
$desk = [Environment]::GetFolderPath('Desktop')
Remove-Item (Join-Path $desk 'Spielmodus an-aus.lnk'), (Join-Path $desk 'Spielmodus zurueck.lnk') -ErrorAction SilentlyContinue
Add-Type -AssemblyName System.Windows.Forms
[System.Windows.Forms.MessageBox]::Show('Spielmodus entfernt: Store, Gaming Services und Game-Bar-Aufnahme sind wieder wie vorher.', 'Spielmodus') | Out-Null
