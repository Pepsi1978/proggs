# Doppelklick-Schalter: Spielmodus-Waechter an/aus (ohne Adminrechte, ueber aus.flag).
$daten = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$flag  = Join-Path $daten 'aus.flag'
New-Item -ItemType Directory -Force $daten | Out-Null
if (Test-Path $flag) { Remove-Item $flag -Force; $text = 'Spielmodus AN: Store und Xbox-Downloads werden gesperrt, sobald Steam oder Call of Duty laeuft.' }
else { New-Item -ItemType File $flag -Force | Out-Null; $text = 'Spielmodus AUS: Store und Gaming Services laufen wieder normal.' }

Add-Type -AssemblyName System.Windows.Forms
$n = New-Object System.Windows.Forms.NotifyIcon
$n.Icon = [System.Drawing.SystemIcons]::Information
$n.Visible = $true
$n.ShowBalloonTip(4000, 'Spielmodus', $text, 'Info')
Start-Sleep -Seconds 5
$n.Dispose()
