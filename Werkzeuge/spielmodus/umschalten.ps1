# Double-click toggle: game mode on/off (no admin rights, via aus.flag), then a visible status window.
# A second click within 3 s does not toggle back, it only shows the current state again.
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$flag  = Join-Path $daten 'aus.flag'
$zuletzt = Join-Path $daten 'umschalten.zeit'
$log   = Join-Path $daten 'waechter.log'
New-Item -ItemType Directory -Force $daten | Out-Null

$textAn  = 'Jetzt AN. Mit Steam pausieren Store, Xbox und Google Drive. Im Spiel haben nur das Spiel, Steam, Chrome und das Heimnetz Internet.'
$textAus = 'Jetzt AUS. Store, Xbox, Google Drive und Internet laufen ganz normal.'

# Only one toggle at a time: overlapping launches (fast repeated clicks) must not both toggle.
$mutex = [Threading.Mutex]::new($false, 'Local\Spielmodus-Umschalten')
if (-not $mutex.WaitOne(0)) { exit }

$prell = (Test-Path $zuletzt) -and (((Get-Date) - (Get-Item $zuletzt).LastWriteTime).TotalSeconds -lt 3)
if ($prell) {
    $istAus = Test-Path $flag
    & (Join-Path $PSScriptRoot 'meldung.ps1') -Text ($(if ($istAus) { $textAus } else { $textAn }) + ' (unverändert)') -Farbe $(if ($istAus) { 'aus' } else { 'an' })
    exit
}

if (Test-Path $flag) {
    Remove-Item $flag -Force
    $text = $textAn; $farbe = 'an'
} else {
    New-Item -ItemType File $flag -Force | Out-Null
    $text = $textAus; $farbe = 'aus'
}
Set-Content $zuletzt (Get-Date -Format o)
$mutex.ReleaseMutex()
try { Add-Content $log ('{0:dd.MM.yyyy HH:mm:ss}  Von Hand umgeschaltet: Spielmodus {1}' -f (Get-Date), $(if ($farbe -eq 'an') { 'AN' } else { 'AUS' })) -Encoding utf8 } catch { }
& (Join-Path $PSScriptRoot 'meldung.ps1') -Text $text -Farbe $farbe
