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

# Die tatsaechliche Displaygroesse vom laufenden Emulator lesen - ein Skin kann
# sie ueberschreiben (Samsungs Fold8-Skin dreht z.B. auf 2448x1848 quer).
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adb) {
  $wm = (& $adb -s emulator-5554 shell wm size 2>$null) -join " "
  if ($wm -match "(\d+)x(\d+)") {
    $liveB = [int]$Matches[1]; $liveH = [int]$Matches[2]
    # Passt die Auflaesung zum Cover- oder zum Innendisplay (Orientierung egal)?
    $lang = [math]::Max($liveB, $liveH); $kurz = [math]::Min($liveB, $liveH)
    if ($lang -eq [math]::Max($COVER_PX_B,$COVER_PX_H) -and $kurz -eq [math]::Min($COVER_PX_B,$COVER_PX_H)) {
      $ppi = $COVER_PPI; $was = "Cover-Display"
    } elseif ($lang -eq [math]::Max($INNEN_PX_B,$INNEN_PX_H) -and $kurz -eq [math]::Min($INNEN_PX_B,$INNEN_PX_H)) {
      $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)"
    }
    $pxB = $liveB; $pxH = $liveH
  }
}

$faktor = ($MONITOR_PPI / $ppi) * $Zoom

# Liegt ein Geraeterahmen (Skin) an, umfasst der Fensterinhalt den ganzen Rahmen,
# nicht nur das Display. Dann muss der Rahmen skaliert werden, damit das DISPLAY
# darin die Originalgroesse hat.
$avdName = if ($Cover) { "Fold8_Cover" } else { "Fold8" }
$cfg = Join-Path $env:USERPROFILE ".android\avd\$avdName.avd\config.ini"
$rahmenB = 0; $rahmenH = 0; $skinDispB = 0
if (Test-Path $cfg) {
  $skinPfad = (Get-Content $cfg | Where-Object { $_ -match "^skin\.path=" }) -replace "^skin\.path=", ""
  $layoutDatei = if ($skinPfad) { Join-Path $skinPfad "layout" } else { $null }
  if ($layoutDatei -and (Test-Path $layoutDatei)) {
    $lay = Get-Content $layoutDatei -Raw
    # display { width X height Y }  -> Displaygroesse laut Skin
    if ($lay -match "display\s*\{[^}]*?width\s+(\d+)[^}]*?height\s+(\d+)") { $skinDispB = [int]$Matches[1] }
    # layouts { portrait { width X height Y -> Gesamtgroesse des Rahmens
    if ($lay -match "layouts\s*\{\s*\w+\s*\{\s*width\s+(\d+)\s+height\s+(\d+)") {
      $rahmenB = [int]$Matches[1]; $rahmenH = [int]$Matches[2]
    }
  }
}

if ($rahmenB -gt 0 -and $skinDispB -gt 0) {
  # Faktor so waehlen, dass das Display im Rahmen die Originalgroesse erreicht
  $zielB = [int][math]::Round($rahmenB * $faktor)
  $zielH = [int][math]::Round($rahmenH * $faktor)
  $displayB = [int][math]::Round($skinDispB * $faktor)
  Write-Host ""
  Write-Host "  Geraeterahmen erkannt ($rahmenB x $rahmenH) - Display darin: $displayB px breit" -ForegroundColor DarkGray
} else {
  $zielB = [int][math]::Round($pxB * $faktor)
  $zielH = [int][math]::Round($pxH * $faktor)
}

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

# Position mitsetzen, damit das Fenster garantiert vollstaendig sichtbar ist.
# Der Emulator merkt sich seine letzte Position und startet sonst gern halb
# ausserhalb des Bildschirms - dann ist die Titelleiste weg und man kann ihn
# nicht mehr verschieben.
$arbeit = $screen.WorkingArea
$vollB = $zielB + $randB
$vollH = $zielH + $randH
# Mittig setzen: so bleibt ringsum Platz - auch rechts fuer die seitliche
# Bedienleiste des Emulators, die sonst aus dem Bildschirm faellt.
$posX = $arbeit.Left + [int](($arbeit.Right - $arbeit.Left - $vollB) / 2)
$posY = $arbeit.Top  + [int](($arbeit.Bottom - $arbeit.Top - $vollH) / 2)
if ($posX -lt $arbeit.Left) { $posX = $arbeit.Left }
if ($posY -lt $arbeit.Top)  { $posY = $arbeit.Top }

# 0x0004 = SWP_NOZORDER
[void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $posX, $posY, $vollB, $vollH, 0x0004)
Start-Sleep -Milliseconds 400

# Nachmessen und einmal korrigieren (Anzeigeskalierung kann dazwischenfunken)
[void][Fenster]::GetClientRect($h, [ref]$c)
$istB = $c.R - $c.L; $istH = $c.B - $c.T
if ([math]::Abs($istB - $zielB) -gt 2) {
  $korrB = $vollB + ($zielB - $istB)
  $korrH = $vollH + ($zielH - $istH)
  $posX = $arbeit.Left + [int](($arbeit.Right - $arbeit.Left - $korrB) / 2)
  $posY = $arbeit.Top  + [int](($arbeit.Bottom - $arbeit.Top - $korrH) / 2)
  if ($posX -lt $arbeit.Left) { $posX = $arbeit.Left }
  if ($posY -lt $arbeit.Top)  { $posY = $arbeit.Top }
  [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $posX, $posY, $korrB, $korrH, 0x0004)
  Start-Sleep -Milliseconds 300
  [void][Fenster]::GetClientRect($h, [ref]$c)
  $istB = $c.R - $c.L; $istH = $c.B - $c.T
}

# Letzte Sicherung: passt das Fenster ueberhaupt in den Arbeitsbereich?
$w2 = New-Object Fenster+RECT
[void][Fenster]::GetWindowRect($h, [ref]$w2)
if ($w2.T -lt $arbeit.Top -or $w2.L -lt $arbeit.Left -or $w2.B -gt $arbeit.Bottom -or $w2.R -gt $arbeit.Right) {
  $nx = [math]::Max($arbeit.Left, [math]::Min($posX, $arbeit.Right - ($w2.R - $w2.L)))
  $ny = [math]::Max($arbeit.Top,  [math]::Min($posY, $arbeit.Bottom - ($w2.B - $w2.T)))
  # 0x0001 = SWP_NOSIZE, 0x0004 = SWP_NOZORDER
  [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $nx, $ny, 0, 0, 0x0005)
  if (($w2.B - $w2.T) -gt ($arbeit.Bottom - $arbeit.Top)) {
    Write-Host ""
    Write-Host "  Hinweis: In Originalgroesse ist das Fenster hoeher als der sichtbare" -ForegroundColor Yellow
    Write-Host "  Bereich. Mit -Zoom 0.8 wird es kleiner, aber dann nicht mehr massstabsgetreu." -ForegroundColor Yellow
  }
}

$cmB = [math]::Round($istB / $MONITOR_PPI * 2.54, 2)
$cmH = [math]::Round($istH / $MONITOR_PPI * 2.54, 2)
Write-Host ""
Write-Host "  Fertig: Bildbereich $istB x $istH Pixel  =  $cmB x $cmH cm auf dem Monitor" -ForegroundColor Green
Write-Host "  ($($p.MainWindowTitle))" -ForegroundColor DarkGray
Write-Host ""
