# Setzt das Emulator-Fenster so, dass das Handy-Bild auf dem Monitor exakt so
# gross ist wie das echte Display in der Hand (1:1 in Zentimetern), und stellt es
# mittig auf den Bildschirm.
#
# Rechenweg:  Fensterbreite [Monitorpixel] = Geraetebreite [Geraetepixel] * (Monitor-ppi / Geraete-ppi)
#
# WARUM NACHTRAEGLICH? Der Android-Emulator kennt keine Startgroesse:
#   -scale            ist seit Emulator 2.0 abgeschafft und wird ignoriert
#   -window-size      gilt nur fuer Fuchsia
#   emulator-user.ini wird beim Beenden mit scale = -1 ueberschrieben
# Er startet immer bildschirmfuellend. Darum wird EINMAL korrigiert - so frueh wie
# moeglich (sobald das Fenster existiert), nicht mehrfach.
#
# Aufruf:  .\Set-Originalgroesse.ps1                 -> Originalgroesse, mittig
#          .\Set-Originalgroesse.ps1 -Zoom 1.5       -> 150 Prozent
#          .\Set-Originalgroesse.ps1 -Cover          -> Massstab fuers Cover-Display
#          .\Set-Originalgroesse.ps1 -Warten 60      -> bis zu 60 s auf das Fenster warten

param(
  [double]$Zoom = 1.0,
  [switch]$Cover,
  [string]$Fenster = "",
  [int]$Warten = 0,
  [switch]$Leise
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
  [DllImport("shcore.dll")] public static extern int SetProcessDpiAwareness(int v);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
}
"@
try { [void][Fenster]::SetProcessDpiAwareness(2) } catch { }

function Schreib($text, $farbe) { if (-not $Leise) { Write-Host $text -ForegroundColor $farbe } }

# --- Geraetewerte, am echten Fold 8 (SM-F971B) gemessen -----------------
$INNEN_PX_B = 1848; $INNEN_PX_H = 2448; $INNEN_PPI = 403.6   # 7,6 Zoll
$COVER_PX_B = 1248; $COVER_PX_H = 1972; $COVER_PPI = 424.3   # 5,5 Zoll

# --- Monitorwerte aus dem EDID -----------------------------------------
Add-Type -AssemblyName System.Windows.Forms
# Vorlaeufig der Hauptmonitor; sobald das Fenster bekannt ist, wird auf den
# Monitor umgestellt, auf dem es tatsaechlich liegt (Mehrmonitor-Betrieb).
$screen = [System.Windows.Forms.Screen]::PrimaryScreen
$edid = Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorBasicDisplayParams -ErrorAction SilentlyContinue | Select-Object -First 1

if ($edid -and $edid.MaxHorizontalImageSize -gt 0) {
  $diagZoll = [math]::Sqrt([math]::Pow($edid.MaxHorizontalImageSize,2) + [math]::Pow($edid.MaxVerticalImageSize,2)) / 2.54
  $diagPx   = [math]::Sqrt([math]::Pow($screen.Bounds.Width,2) + [math]::Pow($screen.Bounds.Height,2))
  $MONITOR_PPI = $diagPx / $diagZoll
} else {
  $MONITOR_PPI = 242.9   # Rueckfallwert fuer diesen Rechner
  Schreib "EDID nicht lesbar - nutze Rueckfallwert $MONITOR_PPI ppi" Yellow
}

# --- Auf das Fenster warten (optional) ----------------------------------
$muster = if ($Fenster -ne "") { $Fenster } else { "Android Emulator" }
$p = $null
$grenze = if ($Warten -gt 0) { $Warten * 5 } else { 1 }
for ($i = 0; $i -lt $grenze; $i++) {
  $p = Get-Process | Where-Object { $_.MainWindowTitle -like "*$muster*" } | Select-Object -First 1
  if ($p) { break }
  Start-Sleep -Milliseconds 200
}
if (-not $p) {
  Schreib "" White
  Schreib "  Kein Emulator-Fenster gefunden. Erst den Emulator starten." Red
  exit 1
}
$h = $p.MainWindowHandle
if ([Fenster]::IsIconic($h)) { [void][Fenster]::ShowWindow($h, 9); Start-Sleep -Milliseconds 500 }

# Auf den Monitor umstellen, auf dem das Fenster wirklich liegt. Mit dem
# Hauptmonitor zu rechnen waere falsch, sobald ein zweiter Bildschirm im Spiel
# ist - dann stimmte der Massstab nicht mehr.
$screenFenster = [System.Windows.Forms.Screen]::FromHandle($h)
if ($screenFenster -and $screenFenster.DeviceName -ne $screen.DeviceName) {
  $verhaeltnis = [math]::Sqrt([math]::Pow($screenFenster.Bounds.Width,2) + [math]::Pow($screenFenster.Bounds.Height,2)) /
                 [math]::Sqrt([math]::Pow($screen.Bounds.Width,2) + [math]::Pow($screen.Bounds.Height,2))
  $MONITOR_PPI = $MONITOR_PPI * $verhaeltnis
  Schreib "  Fenster liegt auf $($screenFenster.DeviceName) - Massstab angepasst (Diagonale geschaetzt)." Yellow
  $screen = $screenFenster
}

# --- Welches Display zeigt der Emulator gerade? -------------------------
if ($Cover) { $pxB = $COVER_PX_B; $pxH = $COVER_PX_H; $ppi = $COVER_PPI; $was = "Cover-Display" }
else        { $pxB = $INNEN_PX_B; $pxH = $INNEN_PX_H; $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)" }

# Live nachfragen - ein Skin oder eine andere AVD kann die Masse aendern
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adb) {
  $wm = (& $adb -s emulator-5554 shell wm size 2>$null) -join " "
  if ($wm -match "(\d+)x(\d+)") {
    $liveB = [int]$Matches[1]; $liveH = [int]$Matches[2]
    $lang = [math]::Max($liveB, $liveH); $kurz = [math]::Min($liveB, $liveH)
    if ($lang -eq $COVER_PX_H -and $kurz -eq $COVER_PX_B) {
      $ppi = $COVER_PPI; $was = "Cover-Display"
    } elseif ($lang -eq $INNEN_PX_H -and $kurz -eq $INNEN_PX_B) {
      $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)"
    }
    $pxB = $liveB; $pxH = $liveH
  }
}

$faktor = ($MONITOR_PPI / $ppi) * $Zoom

# --- Geraeterahmen (Skin) beruecksichtigen, falls einer aktiv ist --------
$avdName = if ($Cover) { "Fold8_Cover" } else { "Fold8" }
$cfg = Join-Path $env:USERPROFILE ".android\avd\$avdName.avd\config.ini"
$rahmenB = 0; $rahmenH = 0
if (Test-Path $cfg) {
  $skinPfad = (Get-Content $cfg | Where-Object { $_ -match "^skin\.path=" }) -replace "^skin\.path=", ""
  $layoutDatei = if ($skinPfad) { Join-Path $skinPfad "layout" } else { $null }
  if ($layoutDatei -and (Test-Path $layoutDatei)) {
    $lay = Get-Content $layoutDatei -Raw
    if ($lay -match "layouts\s*\{\s*\w+\s*\{\s*width\s+(\d+)\s+height\s+(\d+)") {
      $rahmenB = [int]$Matches[1]; $rahmenH = [int]$Matches[2]
    }
  }
}
if ($rahmenB -gt 0) {
  $zielB = [int][math]::Round($rahmenB * $faktor)
  $zielH = [int][math]::Round($rahmenH * $faktor)
} else {
  $zielB = [int][math]::Round($pxB * $faktor)
  $zielH = [int][math]::Round($pxH * $faktor)
}

# --- Setzen: Groesse UND Position in einem Zug --------------------------
$w = New-Object Fenster+RECT; $c = New-Object Fenster+RECT
[void][Fenster]::GetWindowRect($h, [ref]$w)
[void][Fenster]::GetClientRect($h, [ref]$c)
$randB = ($w.R - $w.L) - ($c.R - $c.L)
$randH = ($w.B - $w.T) - ($c.B - $c.T)

$arbeit = $screen.WorkingArea
$vollB = $zielB + $randB
$vollH = $zielH + $randH

function Mittig($breite, $hoehe) {
  $x = $arbeit.Left + [int](($arbeit.Right - $arbeit.Left - $breite) / 2)
  $y = $arbeit.Top  + [int](($arbeit.Bottom - $arbeit.Top - $hoehe) / 2)
  if ($x -lt $arbeit.Left) { $x = $arbeit.Left }
  if ($y -lt $arbeit.Top)  { $y = $arbeit.Top }
  return @($x, $y)
}

$pos = Mittig $vollB $vollH
# 0x0004 = SWP_NOZORDER
[void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $pos[0], $pos[1], $vollB, $vollH, 0x0004)
Start-Sleep -Milliseconds 350

# Genau EINE Nachkorrektur - der Rahmen kann sich beim Skalieren aendern
[void][Fenster]::GetClientRect($h, [ref]$c)
$istB = $c.R - $c.L; $istH = $c.B - $c.T
if ([math]::Abs($istB - $zielB) -gt 2 -or [math]::Abs($istH - $zielH) -gt 2) {
  $korrB = $vollB + ($zielB - $istB)
  $korrH = $vollH + ($zielH - $istH)
  $pos = Mittig $korrB $korrH
  [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $pos[0], $pos[1], $korrB, $korrH, 0x0004)
  Start-Sleep -Milliseconds 250
  [void][Fenster]::GetClientRect($h, [ref]$c)
  $istB = $c.R - $c.L; $istH = $c.B - $c.T
}

$cmB = [math]::Round($istB / $MONITOR_PPI * 2.54, 2)
$cmH = [math]::Round($istH / $MONITOR_PPI * 2.54, 2)
$sollCmB = [math]::Round($pxB / $ppi * 2.54 * $Zoom, 2)
$sollCmH = [math]::Round($pxH / $ppi * 2.54 * $Zoom, 2)

Schreib "" White
Schreib "  $was  -  $($pxB)x$($pxH) px bei $ppi ppi" Cyan
if ($Zoom -ne 1.0) { Schreib "  Zoom $($Zoom * 100) Prozent" Yellow }
Schreib "  Fenster: $istB x $istH px  =  $cmB x $cmH cm   (Soll $sollCmB x $sollCmH cm)" Green

$abwB = [math]::Abs($cmB - $sollCmB)
if ($abwB -gt 0.15) {
  Schreib "  Abweichung $([math]::Round($abwB,2)) cm - das Fenster passt vermutlich nicht auf den Bildschirm." Yellow
}
Schreib "" White
