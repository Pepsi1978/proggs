# Shows a clearly visible status window (top-most, bottom right, closes after 5 s or on click).
# Windows 11 often swallows NotifyIcon balloons, so a small own window is used instead.
# The window never takes focus (WS_EX_NOACTIVATE), so a fullscreen game is not interrupted.
# Usage: meldung.ps1 -Text "..." [-Farbe an|aus|info]
param([string]$Text = 'Spielmodus', [string]$Farbe = 'info')
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()

if (-not ('Spielmodus.StillesFenster' -as [type])) {
    $refs = @([System.Windows.Forms.Form].Assembly.Location, [System.Drawing.Color].Assembly.Location,
        [System.ComponentModel.Component].Assembly.Location) +
        @([AppDomain]::CurrentDomain.GetAssemblies() | Where-Object { $_.GetName().Name -in 'System.Windows.Forms.Primitives', 'System.Drawing.Primitives', 'System.ComponentModel.Primitives', 'System.Runtime', 'System.Private.Windows.Core' } | ForEach-Object Location)
    Add-Type -ReferencedAssemblies ($refs | Sort-Object -Unique) -TypeDefinition @'
namespace Spielmodus {
    public class StillesFenster : System.Windows.Forms.Form {
        protected override bool ShowWithoutActivation { get { return true; } }
        protected override System.Windows.Forms.CreateParams CreateParams {
            get {
                var cp = base.CreateParams;
                cp.ExStyle |= 0x08000000 | 0x00000080 | 0x00000008; // NOACTIVATE | TOOLWINDOW | TOPMOST
                return cp;
            }
        }
    }
}
'@
}

$hinter = switch ($Farbe) {
    'an'    { [System.Drawing.Color]::FromArgb(22, 128, 61) }
    'aus'   { [System.Drawing.Color]::FromArgb(75, 85, 99) }
    default { [System.Drawing.Color]::FromArgb(30, 64, 175) }
}

$f = New-Object Spielmodus.StillesFenster
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
$f.Add_Shown({ $timer.Start() })
[System.Windows.Forms.Application]::Run($f)
$timer.Dispose()
