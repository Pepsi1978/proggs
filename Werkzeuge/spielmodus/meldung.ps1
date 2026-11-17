# Zeigt eine kurze Windows-Mitteilung (unten rechts). Aufruf: meldung.ps1 -Text "..."
param([string]$Text = 'Spielmodus')
Add-Type -AssemblyName System.Windows.Forms
$n = New-Object System.Windows.Forms.NotifyIcon
$n.Icon = [System.Drawing.SystemIcons]::Information
$n.Visible = $true
$n.ShowBalloonTip(5000, 'Spielmodus', $Text, 'Info')
Start-Sleep -Seconds 6
$n.Dispose()
