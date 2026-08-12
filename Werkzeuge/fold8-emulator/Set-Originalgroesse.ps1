# Setzt das Emulator-Fenster so, dass das Handy-Bild auf dem Monitor
# exakt so gross ist wie das echte Display in der Hand (1:1 in Zentimetern).
#
# Rechenweg:  Fensterbreite [Monitorpixel] = Geraetebreite [Geraetepixel] * (Monitor-ppi / Geraete-ppi)
#
# Aufruf:  .\Set-Originalgroesse.ps1                 -> Innendisplay, Originalgroesse
#          .\Set-Originalgroesse.ps1 -Zoom 1.5       -> 150 Prozent
#          .\Set-Originalgroesse.ps1 -Cover          -> Cover-Display-Fenster

param(
  [double]$Zoom = 1.0,
  [switch]$Cover,
  [string]$Fenster = ""
)

# --- Windows-Anzeigeskalierung abschalten (MUSS als Allererstes kommen,
#     sonst meldet der Monitor skalierte statt echter Pixel) --------------
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Fenster {
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int w, int hgt, uint flags);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("shcore.dll")] public static extern int SetProcessDpiAwareness(int v);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
}
"@
try { [void][Fenster]::SetProcessDpiAwareness(2) } catch { }

# --- Geraetewerte, am echten Fold 8 (SM-F971B) gemessen -----------------
$INNEN_PX_B = 1848; $INNEN_PX_H = 2448; $INNEN_PPI = 403.6   # 7,6 Zoll
$COVER_PX_B = 1248; $COVER_PX_H = 1972; $COVER_PPI = 424.3   # 5,5 Zoll

# --- Monitorwerte aus dem EDID -----------------------------------------
Add-Type -AssemblyName System.Windows.Forms
$screen = [System.Windows.Forms.Screen]::PrimaryScreen
$edid = Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorBasicDisplayParams -ErrorAction SilentlyContinue | Select-Object -First 1

if ($edid -and $edid.MaxHorizontalImageSize -gt 0) {
  $diagZoll = [math]::Sqrt([math]::Pow($edid.MaxHorizontalImageSize,2) + [math]::Pow($edid.MaxVerticalImageSize,2)) / 2.54
  $diagPx   = [math]::Sqrt([math]::Pow($screen.Bounds.Width,2) + [math]::Pow($screen.Bounds.Height,2))
  $MONITOR_PPI = $diagPx / $diagZoll
} else {
  $MONITOR_PPI = 242.9   # Rueckfallwert fuer diesen Rechner
  Write-Host "EDID nicht lesbar - nutze Rueckfallwert $MONITOR_PPI ppi" -ForegroundColor Yellow
}

if ($Cover) { $pxB = $COVER_PX_B; $pxH = $COVER_PX_H; $ppi = $COVER_PPI; $was = "Cover-Display" }
else        { $pxB = $INNEN_PX_B; $pxH = $INNEN_PX_H; $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)" }

$faktor = ($MONITOR_PPI / $ppi) * $Zoom
$zielB = [int][math]::Round($pxB * $faktor)
$zielH = [int][math]::Round($pxH * $faktor)

Write-Host ""
Write-Host "  $was" -ForegroundColor Cyan
Write-Host "  Monitor: $([math]::Round($MONITOR_PPI,1)) ppi   Geraet: $ppi ppi   Massstab: $([math]::Round($faktor,4))" -ForegroundColor DarkGray
Write-Host "  Zielgroesse Bildbereich: $zielB x $zielH Pixel" -ForegroundColor DarkGray
if ($Zoom -ne 1.0) { Write-Host "  Zoom: $($Zoom * 100) Prozent" -ForegroundColor Yellow }
else { Write-Host "  Originalgroesse - so gross wie das Handy in der Hand" -ForegroundColor Green }

# --- Fenster finden und setzen ------------------------------------------
$muster = if ($Fenster -ne "") { $Fenster } else { "Android Emulator" }
$p = Get-Process | Where-Object { $_.MainWindowTitle -like "*$muster*" } | Select-Object -First 1
if (-not $p) {
  Write-Host ""
  Write-Host "  Kein Emulator-Fenster gefunden. Erst den Emulator starten." -ForegroundColor Red
  exit 1
}
$h = $p.MainWindowHandle

# Minimierte Fenster haben keine messbare Groesse -> erst wiederherstellen
if ([Fenster]::IsIconic($h)) {
  [void][Fenster]::ShowWindow($h, 9)   # 9 = SW_RESTORE
  Start-Sleep -Milliseconds 600
}

# Rahmen und Titelleiste ermitteln, damit der INNENbereich exakt passt
$w = New-Object Fenster+RECT; $c = New-Object Fenster+RECT
[void][Fenster]::GetWindowRect($h, [ref]$w)
[void][Fenster]::GetClientRect($h, [ref]$c)
$randB = ($w.R - $w.L) - ($c.R - $c.L)
$randH = ($w.B - $w.T) - ($c.B - $c.T)

# 0x0004 = SWP_NOZORDER, 0x0002 = SWP_NOMOVE
[void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, 0, 0, $zielB + $randB, $zielH + $randH, 0x0006)
Start-Sleep -Milliseconds 400

# Nachmessen und einmal korrigieren (Anzeigeskalierung kann dazwischenfunken)
[void][Fenster]::GetClientRect($h, [ref]$c)
$istB = $c.R - $c.L; $istH = $c.B - $c.T
if ([math]::Abs($istB - $zielB) -gt 2) {
  $korrB = $zielB + $randB + ($zielB - $istB)
  $korrH = $zielH + $randH + ($zielH - $istH)
  [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, 0, 0, $korrB, $korrH, 0x0006)
  Start-Sleep -Milliseconds 300
  [void][Fenster]::GetClientRect($h, [ref]$c)
  $istB = $c.R - $c.L; $istH = $c.B - $c.T
}

$cmB = [math]::Round($istB / $MONITOR_PPI * 2.54, 2)
$cmH = [math]::Round($istH / $MONITOR_PPI * 2.54, 2)
Write-Host ""
Write-Host "  Fertig: Bildbereich $istB x $istH Pixel  =  $cmB x $cmH cm auf dem Monitor" -ForegroundColor Green
Write-Host "  ($($p.MainWindowTitle))" -ForegroundColor DarkGray
Write-Host ""
