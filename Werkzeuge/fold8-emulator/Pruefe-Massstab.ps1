# Prueft nach, ob der Emulator beim Drehen wirklich massstabsgetreu bleibt.
#
# Geprueft werden bei JEDER Drehung ZWEI Dinge - Zahlen allein genuegen nicht:
#   1. GROESSE   - stimmt das Fenster auf den Millimeter mit dem echten Geraet ueberein?
#   2. LAGE      - haben Fenster und Anzeige dieselbe Ausrichtung? Sonst zeichnet der
#                  Emulator den Inhalt um 90 Grad GEKIPPT (Text liegt seitwaerts),
#                  waehrend die reinen Masse noch richtig aussehen.
#
# Aufruf:  .\Pruefe-Massstab.ps1              # 10 Drehungen ueber den Dreh-Knopf
#          .\Pruefe-Massstab.ps1 -Runden 20   # laenger pruefen
#          .\Pruefe-Massstab.ps1 -Sensor      # ueber den Lagesensor, links und rechts

param(
  [int]$Runden = 10,
  [switch]$Sensor,
  [int]$WartenSek = 4
)

Add-Type @"
using System;using System.Runtime.InteropServices;
public class PM {
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("shcore.dll")] public static extern int SetProcessDpiAwareness(int v);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L,T,R,B; }
}
"@
# PFLICHT: ohne das liefert GetClientRect skalierte statt echter Pixel - die
# Pruefung saehe dann richtig aus, obwohl das Fenster physisch falsch ist.
try { [void][PM]::SetProcessDpiAwareness(2) } catch { }
Add-Type -AssemblyName System.Windows.Forms

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$INNEN_PPI = 403.6; $COVER_PPI = 424.3
$lagen = @("0:9.81:0", "9.81:0:0", "0:-9.81:0", "-9.81:0:0")

# Monitor-ppi aus dem EDID - dieselbe Quelle wie im Setz-Skript
$schirm = [System.Windows.Forms.Screen]::PrimaryScreen
$edid = Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorBasicDisplayParams -ErrorAction SilentlyContinue | Select-Object -First 1
if ($edid -and $edid.MaxHorizontalImageSize -gt 0) {
  $diagZoll = [math]::Sqrt([math]::Pow($edid.MaxHorizontalImageSize,2)+[math]::Pow($edid.MaxVerticalImageSize,2))/2.54
  $MON = [math]::Sqrt([math]::Pow($schirm.Bounds.Width,2)+[math]::Pow($schirm.Bounds.Height,2)) / $diagZoll
} else { $MON = 242.9 }

function Fenstermass {
  $pr = Get-Process | Where-Object { $_.MainWindowTitle -like "*Android Emulator*" } | Select-Object -First 1
  if (-not $pr) { return $null }
  $r = New-Object PM+RECT; [void][PM]::GetClientRect($pr.MainWindowHandle, [ref]$r)
  return @{ B = $r.R-$r.L; H = $r.B-$r.T }
}
function Anzeige {
  $t = (& $adb -s emulator-5554 shell "dumpsys window displays" 2>$null) -join " "
  if ($t -match "cur=(\d+)x(\d+)") { return @{ B=[int]$Matches[1]; H=[int]$Matches[2] } }
  return $null
}
function GeraetePpi($cur) {
  $lang = [math]::Max($cur.B,$cur.H); $kurz = [math]::Min($cur.B,$cur.H)
  if ($lang -eq 1972 -and $kurz -eq 1248) { return $COVER_PPI }
  return $INNEN_PPI
}

$fehler = 0
function Pruefe($name) {
  $cur = $null; $f = $null
  for ($v = 0; $v -lt 5 -and (-not $cur -or -not $f); $v++) {
    $cur = Anzeige; $f = Fenstermass
    if (-not $cur -or -not $f) { Start-Sleep -Milliseconds 600 }
  }
  if (-not $cur -or -not $f) { Write-Host "$name : keine Messung moeglich" -ForegroundColor Red; $script:fehler++; return }
  $ppi = GeraetePpi $cur
  $sollB = [int][math]::Round($cur.B * ($MON/$ppi)); $sollH = [int][math]::Round($cur.H * ($MON/$ppi))
  $groesseOk = ([math]::Abs($f.B-$sollB) -le 6 -and [math]::Abs($f.H-$sollH) -le 6)
  $lageOk = (($f.B -gt $f.H) -eq ($cur.B -gt $cur.H))
  $urteil = if (-not $lageOk) { "GEKIPPT" } elseif (-not $groesseOk) { "GROESSE FALSCH" } else { "OK" }
  Write-Host ("  {0,-12} Anzeige {1}x{2}  Fenster {3}x{4}  ={5} x {6} cm   {7}" -f `
    $name, $cur.B, $cur.H, $f.B, $f.H, [math]::Round($f.B/$MON*2.54,2), [math]::Round($f.H/$MON*2.54,2), $urteil) `
    -ForegroundColor $(if ($urteil -eq "OK") { "Green" } else { "Red" })
  if ($urteil -ne "OK") { $script:fehler++ }
}

if (-not (& $adb devices | Where-Object { $_ -match "^emulator-\d+\s+device" })) {
  Write-Host "Kein Emulator gefunden. Erst .\Start-Fold8.ps1 aufrufen." -ForegroundColor Red
  exit 1
}
$waechter = @(Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" | Where-Object { $_.CommandLine -like "*Ueberwachen*" })
if ($waechter.Count -eq 0) {
  Write-Host "Achtung: Es laeuft kein Groessen-Waechter - ohne ihn wird die Pruefung fehlschlagen." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Monitor: $([math]::Round($MON,1)) ppi   -   Toleranz 6 px (rund 0,6 mm)" -ForegroundColor DarkGray
Write-Host ""

$anzahl = 0
if ($Sensor) {
  $i = 0
  Write-Host "$Runden Drehungen nach links (Lagesensor):" -ForegroundColor Cyan
  for ($n = 1; $n -le $Runden; $n++) {
    $i = ($i + 3) % 4
    & $adb -s emulator-5554 emu sensor set acceleration $lagen[$i] | Out-Null
    Start-Sleep -Seconds $WartenSek; Pruefe "links $n"; $anzahl++
  }
  Write-Host "$Runden Drehungen nach rechts (Lagesensor):" -ForegroundColor Cyan
  for ($n = 1; $n -le $Runden; $n++) {
    $i = ($i + 1) % 4
    & $adb -s emulator-5554 emu sensor set acceleration $lagen[$i] | Out-Null
    Start-Sleep -Seconds $WartenSek; Pruefe "rechts $n"; $anzahl++
  }
  & $adb -s emulator-5554 emu sensor set acceleration 0:9.81:0 | Out-Null
} else {
  Write-Host "$Runden Drehungen ueber den Dreh-Knopf:" -ForegroundColor Cyan
  for ($n = 1; $n -le $Runden; $n++) {
    & $adb -s emulator-5554 emu rotate | Out-Null
    Start-Sleep -Seconds $WartenSek; Pruefe "drehung $n"; $anzahl++
  }
}

Write-Host ""
if ($fehler -eq 0) {
  Write-Host "  ALLE $anzahl PRUEFUNGEN BESTANDEN - nie gekippt, immer 1:1" -ForegroundColor Green
} else {
  Write-Host "  $fehler von $anzahl FEHLGESCHLAGEN" -ForegroundColor Red
  Write-Host "  Laeuft der Groessen-Waechter? Sonst: .\Start-Fold8.ps1" -ForegroundColor Yellow
}
Write-Host ""
