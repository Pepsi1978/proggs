# Double-click toggle: game mode on/off (no admin rights, via aus.flag), then a notification.
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$flag  = Join-Path $daten 'aus.flag'
New-Item -ItemType Directory -Force $daten | Out-Null
if (Test-Path $flag) {
    Remove-Item $flag -Force
    $text = 'Spielmodus ist jetzt AN. Store und Xbox-Downloads werden gesperrt, solange Steam oder Call of Duty läuft.'
} else {
    New-Item -ItemType File $flag -Force | Out-Null
    $text = 'Spielmodus ist jetzt AUS. Store und Gaming Services laufen normal.'
}
& (Join-Path $PSScriptRoot 'meldung.ps1') -Text $text
