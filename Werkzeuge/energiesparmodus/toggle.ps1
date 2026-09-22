# Schaltet den Windows-Energiesparmodus um (an <-> aus).
# An:  Akku-Schwelle 100 % (Akku und Netz) -> Energiesparmodus greift sofort.
# Aus: Schwelle 0 % -> Energiesparmodus bleibt aus, auch bei niedrigem Akku.
# Der Zustand wird aus der eigenen Schwelle gelesen, nicht aus Windows,
# weil Windows den Status verzögert oder gar nicht meldet.
$zeile = powercfg /q SCHEME_CURRENT SUB_ENERGYSAVER ESBATTTHRESHOLD |
    Select-String 'Gleichstromeinstellung|DC Power Setting Index' | Select-Object -First 1
$istAn = "$zeile" -match '0x00000064'

if ($istAn) { $wert = 0;   $text = 'Energiesparmodus AUS' }
else        { $wert = 100; $text = 'Energiesparmodus AN' }

powercfg /setdcvalueindex SCHEME_CURRENT SUB_ENERGYSAVER ESBATTTHRESHOLD $wert
powercfg /setacvalueindex SCHEME_CURRENT SUB_ENERGYSAVER ESBATTTHRESHOLD $wert
powercfg /setactive SCHEME_CURRENT

Add-Type -AssemblyName System.Windows.Forms
$n = New-Object System.Windows.Forms.NotifyIcon
$n.Icon = [System.Drawing.SystemIcons]::Information
$n.Visible = $true
$n.ShowBalloonTip(2500, 'Energie', $text, 'Info')
Start-Sleep -Seconds 3
$n.Dispose()
