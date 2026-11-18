# Shows a clearly visible status window (top-most, bottom right, closes after 5 s or on click).
# Windows 11 often swallows NotifyIcon balloons, so a small own window is used instead.
# Usage: meldung.ps1 -Text "..." [-Farbe an|aus|info]
param([string]$Text = 'Spielmodus', [string]$Farbe = 'info')
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()

$hinter = switch ($Farbe) {
    'an'    { [System.Drawing.Color]::FromArgb(22, 128, 61) }
    'aus'   { [System.Drawing.Color]::FromArgb(75, 85, 99) }
    default { [System.Drawing.Color]::FromArgb(30, 64, 175) }
}

$f = New-Object System.Windows.Forms.Form
$f.FormBorderStyle = 'None'
$f.ShowInTaskbar = $false
$f.TopMost = $true
$f.StartPosition = 'Manual'
$f.BackColor = $hinter
$f.Width = 460
$f.Height = 130

$titel = New-Object System.Windows.Forms.Label
$titel.Text = 'Spielmodus'
$titel.ForeColor = [System.Drawing.Color]::White
$titel.Font = New-Object System.Drawing.Font('Segoe UI Semibold', 14)
$titel.SetBounds(18, 12, 420, 30)

$inhalt = New-Object System.Windows.Forms.Label
$inhalt.Text = $Text
$inhalt.ForeColor = [System.Drawing.Color]::White
$inhalt.Font = New-Object System.Drawing.Font('Segoe UI', 11)
$inhalt.SetBounds(18, 46, 425, 76)

$f.Controls.AddRange(@($titel, $inhalt))
$bereich = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea
$f.Location = New-Object System.Drawing.Point(($bereich.Right - $f.Width - 16), ($bereich.Bottom - $f.Height - 16))

$schliessen = { $f.Close() }
$f.Add_Click($schliessen); $titel.Add_Click($schliessen); $inhalt.Add_Click($schliessen)
$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 5000
$timer.Add_Tick($schliessen)
$f.Add_Shown({ $timer.Start(); $f.Activate() })
[System.Windows.Forms.Application]::Run($f)
$timer.Dispose()
