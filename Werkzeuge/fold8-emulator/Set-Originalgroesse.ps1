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
# Er startet immer bildschirmfuellend. Darum wird nachtraeglich korrigiert - so
# frueh wie moeglich, sobald das Fenster existiert.
#
# WARUM DAUERHAFT (-Ueberwachen)? Der gezeigte Bildschirm aendert sich im Betrieb:
# aufklappen (andere AVD, 1248x1972 -> 1848x2448), drehen (hoch -> quer). Wird die
# Groesse nur EINMAL beim Start gesetzt, stimmt sie danach nicht mehr. Der
# Ueberwachungsmodus liest den Zustand laufend und fuehrt das Fenster nach, damit
# es IMMER 1:1 bleibt.
#
# Aufruf:  .\Set-Originalgroesse.ps1                 -> Originalgroesse, mittig
#          .\Set-Originalgroesse.ps1 -Zoom 1.5       -> 150 Prozent
#          .\Set-Originalgroesse.ps1 -Cover          -> Massstab fuers Cover-Display
#          .\Set-Originalgroesse.ps1 -Warten 60      -> bis zu 60 s auf das Fenster warten
#          .\Set-Originalgroesse.ps1 -Ueberwachen    -> mitlaufen und dauerhaft 1:1 halten

param(
  [double]$Zoom = 1.0,
  [switch]$Cover,
  [string]$Fenster = "",
  [int]$Warten = 0,
  [switch]$Leise,
  [switch]$Ueberwachen,
  [int]$Intervall = 1
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
  [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr h);
  [DllImport("shcore.dll")] public static extern int SetProcessDpiAwareness(int v);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
}
"@
try { [void][Fenster]::SetProcessDpiAwareness(2) } catch { }

function Schreib($text, $farbe) { if (-not $Leise) { Write-Host $text -ForegroundColor $farbe } }

# --- Geraetewerte, am echten Fold 8 (SM-F971B) gemessen -----------------
$INNEN_PX_B = 1848; $INNEN_PX_H = 2448; $INNEN_PPI = 403.6   # 7,6 Zoll
$COVER_PX_B = 1248; $COVER_PX_H = 1972; $COVER_PPI = 424.3   # 5,5 Zoll

Add-Type -AssemblyName System.Windows.Forms
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# --- Monitor-ppi aus dem EDID -------------------------------------------
# Gemerkt pro Bildschirm: Die EDID-Abfrage kostet mehrere hundert Millisekunden.
# Im Ueberwachungsmodus wuerde sie bei jeder Drehung erneut anfallen und die
# Korrektur spuerbar verzoegern - der Monitor aendert sich derweil nie.
$script:PpiSpeicher = @{}
function Hole-MonitorPpi($schirm) {
  $name = if ($schirm) { $schirm.DeviceName } else { "?" }
  if ($script:PpiSpeicher.ContainsKey($name)) { return $script:PpiSpeicher[$name] }
  $edid = Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorBasicDisplayParams -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($edid -and $edid.MaxHorizontalImageSize -gt 0) {
    $diagZoll = [math]::Sqrt([math]::Pow($edid.MaxHorizontalImageSize,2) + [math]::Pow($edid.MaxVerticalImageSize,2)) / 2.54
    $diagPx   = [math]::Sqrt([math]::Pow($schirm.Bounds.Width,2) + [math]::Pow($schirm.Bounds.Height,2))
    $wert = $diagPx / $diagZoll
    $script:PpiSpeicher[$name] = $wert
    return $wert
  }
  Schreib "EDID nicht lesbar - nutze Rueckfallwert 242.9 ppi" Yellow
  $script:PpiSpeicher[$name] = 242.9
  return 242.9   # Rueckfallwert fuer diesen Rechner
}

# --- Emulator-Fenster suchen --------------------------------------------
function Hole-Fenster([int]$wartenSek) {
  $muster = if ($Fenster -ne "") { $Fenster } else { "Android Emulator" }
  $grenze = if ($wartenSek -gt 0) { $wartenSek * 5 } else { 1 }
  for ($i = 0; $i -lt $grenze; $i++) {
    $p = Get-Process | Where-Object { $_.MainWindowTitle -like "*$muster*" } | Select-Object -First 1
    if ($p) {
      $h = $p.MainWindowHandle
      if ([Fenster]::IsIconic($h)) { [void][Fenster]::ShowWindow($h, 9); Start-Sleep -Milliseconds 500 }
      return $h
    }
    Start-Sleep -Milliseconds 200
  }
  return [IntPtr]::Zero
}

# --- Was zeigt der Emulator GERADE? -------------------------------------
# Live gefragt, nie geraten: welche AVD, welche Flaeche, welche Drehung.
# "cur=" aus "dumpsys window displays" ist die tatsaechlich bespielte Flaeche
# und dreht im Querformat mit; mRotation dient als zweite Quelle, falls cur
# noch nicht nachgezogen hat.
function Hole-Zustand {
  if (-not (Test-Path $adb)) { return $null }
  $avd = ((& $adb -s emulator-5554 emu avd name 2>$null) | Select-Object -First 1) -replace "\s",""
  if (-not $avd -or $avd -eq "") { return $null }

  $anzeige = (& $adb -s emulator-5554 shell "dumpsys window displays" 2>$null) -join " "
  $b = 0; $h = 0
  if ($anzeige -match "cur=(\d+)x(\d+)") { $b = [int]$Matches[1]; $h = [int]$Matches[2] }
  if ($b -eq 0) {
    $wm = (& $adb -s emulator-5554 shell wm size 2>$null) -join " "
    if ($wm -match "(\d+)x(\d+)") { $b = [int]$Matches[1]; $h = [int]$Matches[2] }
  }
  if ($b -eq 0) { return $null }

  $rot = 0
  $rotText = (& $adb -s emulator-5554 shell "dumpsys window" 2>$null | Select-String -Pattern "^\s*mRotation=(\d)" | Select-Object -First 1)
  if ($rotText -and $rotText.Matches.Count -gt 0) { $rot = [int]$rotText.Matches[0].Groups[1].Value }
  # Quer gedreht, aber die Flaeche steht noch hochkant: dann selbst tauschen.
  if (($rot -eq 1 -or $rot -eq 3) -and $h -gt $b) { $t = $b; $b = $h; $h = $t }

  # Geraete-ppi anhand der langen Kante - unabhaengig von der Drehung
  $lang = [math]::Max($b, $h); $kurz = [math]::Min($b, $h)
  if ($lang -eq $COVER_PX_H -and $kurz -eq $COVER_PX_B) {
    $ppi = $COVER_PPI; $was = "Cover-Display (zugeklappt)"
  } elseif ($lang -eq $INNEN_PX_H -and $kurz -eq $INNEN_PX_B) {
    $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)"
  } elseif ($avd -match "Cover") {
    $ppi = $COVER_PPI; $was = "Cover-Display (zugeklappt)"
  } else {
    $ppi = $INNEN_PPI; $was = "Innendisplay (aufgeklappt)"
  }
  if ($rot -eq 1 -or $rot -eq 3) { $was = "$was, quer" }

  return @{ Avd = $avd; B = $b; H = $h; Rot = $rot; Ppi = $ppi; Was = $was }
}

# --- Zustand ohne laufenden Emulator: aus den Parametern ----------------
function Hole-Ersatzzustand {
  if ($Cover) { return @{ Avd = "Fold8_Cover"; B = $COVER_PX_B; H = $COVER_PX_H; Rot = 0; Ppi = $COVER_PPI; Was = "Cover-Display (zugeklappt)" } }
  return @{ Avd = "Fold8"; B = $INNEN_PX_B; H = $INNEN_PX_H; Rot = 0; Ppi = $INNEN_PPI; Was = "Innendisplay (aufgeklappt)" }
}

# --- Geraeterahmen (Skin) beruecksichtigen, falls einer aktiv ist --------
function Hole-Rahmenmasse($avdName) {
  $cfg = Join-Path $env:USERPROFILE ".android\avd\$avdName.avd\config.ini"
  if (-not (Test-Path $cfg)) { return @(0, 0) }
  $skinPfad = (Get-Content $cfg | Where-Object { $_ -match "^skin\.path=" }) -replace "^skin\.path=", ""
  if (-not $skinPfad) { return @(0, 0) }
  $layoutDatei = Join-Path $skinPfad "layout"
  if (-not (Test-Path $layoutDatei)) { return @(0, 0) }
  $lay = Get-Content $layoutDatei -Raw
  if ($lay -match "layouts\s*\{\s*\w+\s*\{\s*width\s+(\d+)\s+height\s+(\d+)") {
    return @([int]$Matches[1], [int]$Matches[2])
  }
  return @(0, 0)
}

# --- Fenster auf Originalgroesse bringen --------------------------------
# $mittig = $false laesst das Fenster stehen, wo es ist (der Benutzer darf es
# verschieben) und korrigiert nur die Groesse.
function Setze-Groesse($h, $zustand, [bool]$mittig) {
  if (-not [Fenster]::IsWindow($h)) { return $null }

  $screen = [System.Windows.Forms.Screen]::FromHandle($h)
  if (-not $screen) { $screen = [System.Windows.Forms.Screen]::PrimaryScreen }
  $monitorPpi = Hole-MonitorPpi $screen
  $faktor = ($monitorPpi / $zustand.Ppi) * $Zoom

  $rahmen = Hole-Rahmenmasse $zustand.Avd
  if ($rahmen[0] -gt 0) {
    $zielB = [int][math]::Round($rahmen[0] * $faktor)
    $zielH = [int][math]::Round($rahmen[1] * $faktor)
  } else {
    $zielB = [int][math]::Round($zustand.B * $faktor)
    $zielH = [int][math]::Round($zustand.H * $faktor)
  }

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

  # 0x0004 = SWP_NOZORDER, 0x0002 = SWP_NOMOVE (Position unangetastet lassen)
  if ($mittig) {
    $pos = Mittig $vollB $vollH
    [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $pos[0], $pos[1], $vollB, $vollH, 0x0004)
  } else {
    [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, 0, 0, $vollB, $vollH, 0x0006)
  }
  Start-Sleep -Milliseconds 350

  # Genau EINE Nachkorrektur - der Rahmen kann sich beim Skalieren aendern
  [void][Fenster]::GetClientRect($h, [ref]$c)
  $istB = $c.R - $c.L; $istH = $c.B - $c.T
  if ([math]::Abs($istB - $zielB) -gt 2 -or [math]::Abs($istH - $zielH) -gt 2) {
    $korrB = $vollB + ($zielB - $istB)
    $korrH = $vollH + ($zielH - $istH)
    if ($mittig) {
      $pos = Mittig $korrB $korrH
      [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, $pos[0], $pos[1], $korrB, $korrH, 0x0004)
    } else {
      [void][Fenster]::SetWindowPos($h, [IntPtr]::Zero, 0, 0, $korrB, $korrH, 0x0006)
    }
    Start-Sleep -Milliseconds 250
    [void][Fenster]::GetClientRect($h, [ref]$c)
    $istB = $c.R - $c.L; $istH = $c.B - $c.T
  }

  return @{ IstB = $istB; IstH = $istH; ZielB = $zielB; ZielH = $zielH; MonitorPpi = $monitorPpi }
}

function Melde($zustand, $ergebnis) {
  if (-not $ergebnis) { return }
  $cmB = [math]::Round($ergebnis.IstB / $ergebnis.MonitorPpi * 2.54, 2)
  $cmH = [math]::Round($ergebnis.IstH / $ergebnis.MonitorPpi * 2.54, 2)
  $sollCmB = [math]::Round($zustand.B / $zustand.Ppi * 2.54 * $Zoom, 2)
  $sollCmH = [math]::Round($zustand.H / $zustand.Ppi * 2.54 * $Zoom, 2)
  Schreib "" White
  Schreib "  $($zustand.Was)  -  $($zustand.B)x$($zustand.H) px bei $($zustand.Ppi) ppi" Cyan
  if ($Zoom -ne 1.0) { Schreib "  Zoom $($Zoom * 100) Prozent" Yellow }
  Schreib "  Fenster: $($ergebnis.IstB) x $($ergebnis.IstH) px  =  $cmB x $cmH cm   (Soll $sollCmB x $sollCmH cm)" Green
  $abwB = [math]::Abs($cmB - $sollCmB)
  if ($abwB -gt 0.15) {
    Schreib "  Abweichung $([math]::Round($abwB,2)) cm - das Fenster passt vermutlich nicht auf den Bildschirm." Yellow
  }
  Schreib "" White
}

# ========================= Hauptteil =====================================

$h = Hole-Fenster $Warten
if ($h -eq [IntPtr]::Zero) {
  Schreib "" White
  Schreib "  Kein Emulator-Fenster gefunden. Erst den Emulator starten." Red
  exit 1
}

$zustand = Hole-Zustand
if (-not $zustand) { $zustand = Hole-Ersatzzustand }
$ergebnis = Setze-Groesse $h $zustand $true
Melde $zustand $ergebnis

if (-not $Ueberwachen) { exit 0 }

# --- Ueberwachungsmodus: 1:1 dauerhaft halten ---------------------------
# Reagiert auf: Aufklappen/Zuklappen (AVD-Wechsel), Drehen, Emulator-Neustart
# und auf ein von aussen veraendertes Fenster. Endet, wenn der Emulator weg ist.
#
# NUR EINER GLEICHZEITIG (Poka-Yoke Stufe 3): Zwei Waechter wuerden sich
# gegenseitig ueberstimmen und das Fenster sichtbar hin- und herspringen lassen.
# Der Mutex macht einen zweiten Waechter unmoeglich - er beendet sich sofort.
$mutex = New-Object System.Threading.Mutex($false, "Local\Fold8GroessenWaechter")
$allein = $false
try { $allein = $mutex.WaitOne(0) } catch { $allein = $false }
if (-not $allein) {
  Schreib "  Es laeuft bereits ein Groessen-Waechter - dieser beendet sich." DarkGray
  exit 0
}

Schreib "  Groessen-Waechter laeuft - haelt das Fenster bei jeder Aenderung 1:1." DarkGray

# Der Sollwert haengt IMMER am gerade gemeldeten Zustand - er wird nie
# fortgeschrieben. Dadurch ist es egal, wie oft und in welche Richtung gedreht
# wird: nach jeder Drehung wird neu aus "cur=BxH" gerechnet.
$letzterSchluessel = "$($zustand.Avd)|$($zustand.B)x$($zustand.H)|$($zustand.Rot)"
$fehlt = 0
$kandidat = ""; $kandidatZaehler = 0
$korrekturZeiten = @()
$pauseBis = Get-Date
$ruheBis = Get-Date
$schiefSeit = $null; $schiefGemeldet = $false
$sprungFrei = Get-Date; $spruenge = 0
$takt = [math]::Max(150, [int]($Intervall * 250))   # Standard 250 ms - flink genug fuers Drehen

try {
  while ($true) {
    Start-Sleep -Milliseconds $takt

    # Fenster weg (Emulator beendet oder neu gestartet)? Neu suchen.
    if (-not [Fenster]::IsWindow($h)) {
      $h = Hole-Fenster 0
      if ($h -eq [IntPtr]::Zero) {
        $fehlt++
        if ($fehlt -ge 75) { Schreib "  Emulator beendet - Waechter steigt aus." DarkGray; break }
        continue
      }
      $letzterSchluessel = ""   # neues Fenster: auf jeden Fall neu setzen
    }
    $fehlt = 0

    # Direkt nach einem Eingriff kurz nicht messen - sonst wird das Fenster
    # mitten im Neuzeichnen erfasst und der Waechter regelt gegen sich selbst.
    if ((Get-Date) -lt $ruheBis) { continue }

    $jetzt = Hole-Zustand
    if (-not $jetzt) { continue }

    # ---- LAGE-ABGLEICH: laeuft in JEDEM Durchlauf, vor allem anderen --------
    # Der Emulator dreht seinen Fensterrahmen und Android seine Anzeige - aber
    # nicht gleichzeitig, und in einer Lage gar nicht gemeinsam. Passen beide
    # nicht zusammen, darf die Groesse NICHT gesetzt werden: ein Hochformat-Mass
    # auf einem querstehenden Rahmen (oder umgekehrt) laesst den Emulator den
    # Inhalt um 90 Grad GEKIPPT zeichnen - der Text liegt dann seitwaerts.
    # Erkennung: das unangetastete Fenster hat immer das Verhaeltnis des
    # Emulator-Rahmens, Android meldet seines ueber "cur=BxH".
    # Diese Pruefung MUSS in jedem Durchlauf laufen, nicht nur beim Wechsel:
    # der Emulator stellt sein Fenster erst kurz NACH der Android-Meldung um.
    $c0 = New-Object Fenster+RECT
    [void][Fenster]::GetClientRect($h, [ref]$c0)
    $c0B = $c0.R - $c0.L; $c0H = $c0.B - $c0.T
    if ($c0B -gt 50 -and $c0H -gt 50 -and (($c0B -gt $c0H) -ne ($jetzt.B -gt $jetzt.H))) {
      if ($schiefSeit -eq $null) { $schiefSeit = Get-Date }

      # Waehrend einer Drehung stehen beide kurz schief - das ist normal und geht
      # von selbst vorbei. Bleibt es laenger als zwei Sekunden schief, ist es die
      # 180-Grad-Lage: Der Emulator stellt seinen Rahmen hochkant, Android kennt
      # kein Hochformat kopfueber und bleibt quer. Diese Lage ist unbrauchbar -
      # der Inhalt wird darin IMMER gekippt gezeichnet, mit jeder Fenstergroesse.
      # Dann eine Stufe weiterdrehen, damit die naechste brauchbare Lage kommt.
      if (((Get-Date) - $schiefSeit).TotalSeconds -ge 1.2 -and (Get-Date) -gt $sprungFrei) {
        if ($spruenge -ge 2) {
          if (-not $schiefGemeldet) {
            Schreib "  Emulator-Rahmen und Anzeige bleiben quer zueinander - bitte im Emulator einmal drehen." Yellow
            $schiefGemeldet = $true
          }
          continue
        }
        Schreib "  Unbrauchbare Lage (Rahmen und Anzeige stehen quer zueinander) - eine Stufe weiter." DarkGray
        & $adb -s emulator-5554 emu rotate 2>$null | Out-Null
        $spruenge++
        $sprungFrei = (Get-Date).AddSeconds(6)   # nie zwei Spruenge kurz hintereinander
        $ruheBis = (Get-Date).AddMilliseconds(800)
        $schiefSeit = $null
        $letzterSchluessel = ""                  # nach dem Sprung neu bewerten
      }
      continue            # in dieser Lage NIEMALS die Groesse setzen
    }
    $schiefSeit = $null; $schiefGemeldet = $false; $spruenge = 0

    $schluessel = "$($jetzt.Avd)|$($jetzt.B)x$($jetzt.H)|$($jetzt.Rot)"
    if ($schluessel -ne $letzterSchluessel) {
      # Entprellen: Beim Drehen und Klappen meldet Android Zwischenzustaende.
      # Erst wenn derselbe neue Zustand zweimal hintereinander gemessen wurde,
      # ist er echt - sonst loest jeder Zwischenschritt ein Setzen aus.
      if ($schluessel -eq $kandidat) { $kandidatZaehler++ } else { $kandidat = $schluessel; $kandidatZaehler = 1 }
      if ($kandidatZaehler -lt 2) { continue }

      $ergebnis = Setze-Groesse $h $jetzt $true
      Melde $jetzt $ergebnis
      $letzterSchluessel = $schluessel
      $korrekturZeiten = @()
      $pauseBis = Get-Date          # neue Lage - Nachfuehrung ist wieder frei
      $ruheBis = (Get-Date).AddMilliseconds(1200)
      continue
    }

    # Gleicher Zustand: nachziehen, wenn die Groesse verstellt wurde (der
    # Emulator aendert sie beim Drehen selbst). Position dabei in Ruhe lassen.
    if ((Get-Date) -lt $pauseBis) { continue }

    $c = New-Object Fenster+RECT
    [void][Fenster]::GetClientRect($h, [ref]$c)
    $istB = $c.R - $c.L; $istH = $c.B - $c.T
    if ($istB -lt 50 -or $istH -lt 50) { continue }   # minimiert oder in Bewegung
    if ($ergebnis -and ([math]::Abs($istB - $ergebnis.ZielB) -gt 6 -or [math]::Abs($istH - $ergebnis.ZielH) -gt 6)) {
      $ergebnis = Setze-Groesse $h $jetzt $false
      $ruheBis = (Get-Date).AddMilliseconds(1200)

      # Schutz vor Endlos-Ping-Pong, falls der Emulator dagegenhaelt: Bei sechs
      # Korrekturen in zehn Sekunden vier Sekunden Ruhe. Die Sperre loest sich
      # von SELBST wieder - sie darf sich niemals dauerhaft festfahren, sonst
      # bliebe das Fenster nach mehrfachem Drehen fuer immer falsch. Sechs statt
      # vier, damit schnelles Hin- und Herdrehen sie nicht faelschlich ausloest.
      $korrekturZeiten += (Get-Date)
      $korrekturZeiten = @($korrekturZeiten | Where-Object { $_ -gt (Get-Date).AddSeconds(-10) })
      if ($korrekturZeiten.Count -ge 6) {
        $pauseBis = (Get-Date).AddSeconds(4)
        $korrekturZeiten = @()
        Schreib "  Der Emulator setzt die Groesse staendig zurueck - Nachfuehrung pausiert 4 Sekunden." Yellow
      }
    }
  }
}
finally {
  try { $mutex.ReleaseMutex() } catch { }
  try { $mutex.Dispose() } catch { }
}
