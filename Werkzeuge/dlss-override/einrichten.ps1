# Sets the newest DLSS (driver override, Recommended preset) for CoD MW2 (2022) and turns on the DLSS indicator.
# Needs admin (one UAC prompt). No game files are touched.
param([string]$Nip = "$PSScriptRoot\mw2-dlss.nip", [switch]$KeinIndikator)
$ErrorActionPreference = 'Stop'
$npiDir = "$env:LOCALAPPDATA\nvidiaProfileInspector"
$npi = "$npiDir\nvidiaProfileInspector.exe"
$log = "$env:TEMP\dlss-override-einrichten.log"

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    $args2 = @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$PSCommandPath`"",'-Nip',"`"$Nip`"")
    if ($KeinIndikator) { $args2 += '-KeinIndikator' }
    Start-Process powershell -Verb RunAs -Wait -ArgumentList $args2
    if (Test-Path $log) { Get-Content $log }
    return
}

Start-Transcript -Path $log -Force | Out-Null
try {
    if (-not (Test-Path $npi)) {
        New-Item -ItemType Directory -Force $npiDir | Out-Null
        Invoke-WebRequest 'https://github.com/Orbmu2k/nvidiaProfileInspector/releases/download/v3.0.2.1/nvidiaProfileInspector.zip' -OutFile "$npiDir\npi.zip"
        Expand-Archive "$npiDir\npi.zip" $npiDir -Force
    }
    Start-Process $npi -ArgumentList @('-silent','-mergeImport',"`"$Nip`"") -Wait
    "IMPORT fertig"

    if (-not $KeinIndikator) {
        $k = 'HKLM:\SOFTWARE\NVIDIA Corporation\Global\NGXCore'
        if (-not (Test-Path $k)) { New-Item -Path $k | Out-Null }
        Set-ItemProperty -Path $k -Name ShowDlssIndicator -Value 0x400 -Type DWord
        "INDIKATOR an"
    }

    Get-ChildItem $npiDir -Filter 'CustomProfiles_*.nip' | Remove-Item -Force
    Start-Process $npi -ArgumentList '-exportCustomized' -Wait
    $exp = Get-ChildItem $npiDir -Filter 'CustomProfiles_*.nip' | Sort-Object LastWriteTime | Select-Object -Last 1
    $x = Get-Content $exp.FullName -Raw
    $ok = $x -match 'Modern Warfare 2 \(2022\)' -and $x -match '283385345' -and $x -match '6505105' -and $x -match '283385331'
    "VERIFIKATION: " + $(if ($ok) { 'OK' } else { 'FEHLT' })
} catch { "FEHLER: $_" } finally { Stop-Transcript | Out-Null }
