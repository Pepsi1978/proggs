# Shows a short Windows notification (bottom right). Usage: meldung.ps1 -Text "..."
param([string]$Text = 'Spielmodus')
Add-Type -AssemblyName System.Windows.Forms
$n = New-Object System.Windows.Forms.NotifyIcon
$n.Icon = [System.Drawing.SystemIcons]::Information
$n.Text = 'Spielmodus'
$n.Visible = $true
$n.ShowBalloonTip(5000, 'Spielmodus', $Text, 'Info')
# Pump messages so the balloon is delivered reliably, then remove the tray icon.
$ende = (Get-Date).AddSeconds(6)
while ((Get-Date) -lt $ende) { [System.Windows.Forms.Application]::DoEvents(); Start-Sleep -Milliseconds 100 }
$n.Visible = $false
$n.Dispose()
