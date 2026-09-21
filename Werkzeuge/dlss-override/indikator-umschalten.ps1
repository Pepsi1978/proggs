# Toggles the NVIDIA DLSS on-screen indicator (0x400 = on, 0 = off). One UAC prompt.
$k = 'HKLM:\SOFTWARE\NVIDIA Corporation\Global\NGXCore'
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) { Start-Process powershell -Verb RunAs -Wait -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$PSCommandPath`""); return }
$cur = (Get-ItemProperty -Path $k -Name ShowDlssIndicator -ErrorAction SilentlyContinue).ShowDlssIndicator
$neu = if ($cur -eq 0x400) { 0 } else { 0x400 }
if (-not (Test-Path $k)) { New-Item -Path $k | Out-Null }
Set-ItemProperty -Path $k -Name ShowDlssIndicator -Value $neu -Type DWord
Add-Type -AssemblyName PresentationFramework
[System.Windows.MessageBox]::Show("DLSS-Anzeige ist jetzt " + $(if ($neu) { 'AN' } else { 'AUS' }), 'DLSS-Indikator') | Out-Null
