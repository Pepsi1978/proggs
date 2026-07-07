# screenshot-to-clipboard.ps1
# Macht einen Vollbild-Screenshot und legt ihn in die Windows-Zwischenablage.
# Gedacht fuer eine Stream-Deck-Taste: danach in der CLI Strg+V druecken,
# um den Screenshot als Bild einzufuegen.
#
# Aufruf (Stream Deck "Oeffnen"-Aktion):
#   powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\Users\barwa\proggs\streamdeck\screenshot-to-clipboard.ps1"
#
# Standard: nur der Hauptbildschirm. Fuer ALLE Monitore zusammen:
#   ... -File "...\screenshot-to-clipboard.ps1" -All

param(
    [switch]$All  # -All erfasst alle Monitore zusammen statt nur den Hauptbildschirm
)

try {
    Add-Type -AssemblyName System.Windows.Forms, System.Drawing -ErrorAction Stop

    if ($All) {
        # Alle Monitore zusammen (virtueller Bildschirm)
        $bounds = [System.Windows.Forms.SystemInformation]::VirtualScreen
    } else {
        # Nur der Hauptbildschirm
        $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    }

    $bmp = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
    [System.Windows.Forms.Clipboard]::SetImage($bmp)
}
catch {
    # Fehler nicht still verschlucken: in eine Log-Datei schreiben, damit man
    # nachvollziehen kann, warum mal kein Screenshot in der Zwischenablage landete.
    $logDir = Join-Path $env:LOCALAPPDATA 'streamdeck-screenshot'
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    $line = '{0} ERROR {1}' -f (Get-Date -Format o), $_.Exception.Message
    Add-Content -Path (Join-Path $logDir 'screenshot.log') -Value $line -Encoding UTF8
    exit 1
}
finally {
    if ($g)   { $g.Dispose() }
    if ($bmp) { $bmp.Dispose() }
}
