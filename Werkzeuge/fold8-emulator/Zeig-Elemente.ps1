# Macht einen Screenshot des laufenden Geraets (Emulator ODER echtes Fold),
# liest die Bedienelemente aus und zeichnet nummerierte Rahmen darueber.
#
# Danach genuegt: "Nummer 7 soll anders aussehen" - die Nummer verweist
# eindeutig auf ein Element mit Position, Text und Resource-Id.
#
# Aufruf:  .\Zeig-Elemente.ps1                    -> laufender Emulator
#          .\Zeig-Elemente.ps1 -Geraet R3GL7073MLM -> echtes Fold 8
#          .\Zeig-Elemente.ps1 -NurKlickbare      -> nur bedienbare Elemente

param(
  [string]$Geraet = "",
  [switch]$NurKlickbare,
  [string]$Ziel = ""
)

# adb schreibt Erfolgsmeldungen auf die Fehlerausgabe - darum nicht abbrechen,
# sondern jedes Zwischenergebnis selbst pruefen.
$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Host "adb nicht gefunden: $adb" -ForegroundColor Red; exit 1 }

# --- Geraet bestimmen ----------------------------------------------------
if ($Geraet -eq "") {
  $zeilen = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }
  if (-not $zeilen) { Write-Host "Kein Geraet verbunden." -ForegroundColor Red; exit 1 }
  # Emulator bevorzugen, sonst das erste
  $emu = $zeilen | Where-Object { $_ -match "^emulator-" } | Select-Object -First 1
  if (-not $emu) { $emu = @($zeilen)[0] }
  $Geraet = ($emu -split "\s+")[0]
}
Write-Host "Geraet: $Geraet" -ForegroundColor Cyan

if ($Ziel -eq "") {
  $Ziel = Join-Path ([System.IO.Path]::GetTempPath()) "fold8-elemente"
}
New-Item -ItemType Directory -Force -Path $Ziel | Out-Null
$bildDatei  = Join-Path $Ziel "screenshot.png"
$zielDatei  = Join-Path $Ziel "elemente.png"
$listeDatei = Join-Path $Ziel "elemente.txt"

# --- Screenshot + UI-Hierarchie holen ------------------------------------
Write-Host "Screenshot holen..." -ForegroundColor DarkGray
# Umweg ueber das Geraet: Binaerdaten ueberleben die PowerShell-Pipe nicht
& $adb -s $Geraet shell "screencap -p /sdcard/shot.png" | Out-Null
if (Test-Path $bildDatei) { Remove-Item $bildDatei -Force }
& $adb -s $Geraet pull /sdcard/shot.png $bildDatei 2>&1 | Out-Null
if (-not (Test-Path $bildDatei) -or (Get-Item $bildDatei).Length -lt 1000) {
  Write-Host "Screenshot fehlgeschlagen." -ForegroundColor Red; exit 1
}

Write-Host "Bedienelemente auslesen..." -ForegroundColor DarkGray
& $adb -s $Geraet shell "uiautomator dump /sdcard/ui.xml" | Out-Null
$xmlText = (& $adb -s $Geraet shell "cat /sdcard/ui.xml") -join ""
try { $xml = [xml]$xmlText } catch { Write-Host "UI-Hierarchie nicht lesbar." -ForegroundColor Red; exit 1 }

# --- Elemente sammeln ----------------------------------------------------
$elemente = @()
$knoten = $xml.SelectNodes("//node")
foreach ($n in $knoten) {
  $b = $n.bounds
  if (-not ($b -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')) { continue }
  $x1=[int]$Matches[1]; $y1=[int]$Matches[2]; $x2=[int]$Matches[3]; $y2=[int]$Matches[4]
  $breite = $x2-$x1; $hoehe = $y2-$y1
  if ($breite -lt 20 -or $hoehe -lt 20) { continue }

  $klickbar = ($n.clickable -eq "true")
  if ($NurKlickbare -and -not $klickbar) { continue }

  # Container ueberspringen, die den ganzen Bildschirm fuellen und nichts aussagen
  $hatInhalt = ($n.text -ne "") -or ($n.'content-desc' -ne "") -or ($n.'resource-id' -ne "")
  if (-not $hatInhalt -and -not $klickbar) { continue }

  $elemente += [PSCustomObject]@{
    X1 = $x1; Y1 = $y1; X2 = $x2; Y2 = $y2
    Flaeche  = $breite * $hoehe
    Text     = $n.text
    Beschr   = $n.'content-desc'
    Id       = ($n.'resource-id' -replace '^.*/', '')
    Klasse   = ($n.class -replace '^.*\.', '')
    Klickbar = $klickbar
  }
}

# Alle Textknoten getrennt merken - sie liefern gleich die Beschriftung
# fuer bedienbare Zeilen, die selbst keinen eigenen Text tragen.
$alleTexte = $elemente | Where-Object { $_.Text -ne "" }

# Doppelte wegwerfen: Android verschachtelt oft mehrere Knoten deckungsgleich.
# Von jedem Rechteck bleibt der aussagekraeftigste Knoten uebrig.
$gesehen = @{}
$eindeutig = @()
$vorsortiert = $elemente | Sort-Object @{Expression={[int](-not $_.Klickbar)}}, @{Expression={[int](($_.Text -eq "") -and ($_.Beschr -eq ""))}}
foreach ($e in $vorsortiert) {
  $schluessel = "$($e.X1),$($e.Y1),$($e.X2),$($e.Y2)"
  if ($gesehen.ContainsKey($schluessel)) { continue }
  $gesehen[$schluessel] = $true
  $eindeutig += $e
}

# Bedienbares zuerst, dann Beschriftetes, jeweils die spezifischsten (kleinsten)
$klick = $eindeutig | Where-Object { $_.Klickbar } | Sort-Object Flaeche
$rest  = $eindeutig | Where-Object { -not $_.Klickbar -and (($_.Text -ne "") -or ($_.Beschr -ne "")) } | Sort-Object Flaeche
$elemente = @($klick) + @($rest) | Select-Object -First 35

if ($elemente.Count -eq 0) {
  Write-Host "Keine benennbaren Elemente gefunden." -ForegroundColor Yellow
  Write-Host "Bei reinen Compose-Oberflaechen hilft Modifier.testTag(...) oder contentDescription." -ForegroundColor Yellow
  exit 0
}

# --- Rahmen einzeichnen --------------------------------------------------
Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Image]::FromFile($bildDatei)
$neu = New-Object System.Drawing.Bitmap($bmp.Width, $bmp.Height)
$g = [System.Drawing.Graphics]::FromImage($neu)
$g.DrawImage($bmp, 0, 0, $bmp.Width, $bmp.Height)
$bmp.Dispose()
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

$dickeRahmen = [math]::Max(3, [int]($neu.Width / 400))
$schriftGr   = [math]::Max(20, [int]($neu.Width / 45))
$schrift = New-Object System.Drawing.Font("Segoe UI", $schriftGr, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$stiftKlick = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 220, 40, 40), $dickeRahmen)
$stiftAnzeige = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(200, 40, 120, 220), $dickeRahmen)
$fuellRot  = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 220, 40, 40))
$fuellBlau = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 40, 120, 220))
$weiss = [System.Drawing.Brushes]::White

$liste = @()
$nr = 0
foreach ($e in $elemente) {
  $nr++
  $stift = if ($e.Klickbar) { $stiftKlick } else { $stiftAnzeige }
  $fuell = if ($e.Klickbar) { $fuellRot } else { $fuellBlau }
  $g.DrawRectangle($stift, $e.X1, $e.Y1, ($e.X2-$e.X1), ($e.Y2-$e.Y1))

  # Nummernplakette oben links am Element
  $txt = "$nr"
  $masse = $g.MeasureString($txt, $schrift)
  $pw = [math]::Max($masse.Width + 12, $schriftGr * 1.4)
  $ph = $masse.Height + 6
  $px = [math]::Max(0, $e.X1)
  $py = [math]::Max(0, $e.Y1 - $ph)
  if ($py -lt 0) { $py = $e.Y1 }
  $g.FillRectangle($fuell, $px, $py, $pw, $ph)
  $g.DrawString($txt, $schrift, $weiss, ($px + 6), ($py + 2))

  $bez = if ($e.Text) { $e.Text } elseif ($e.Beschr) { $e.Beschr } else { "" }
  if ($bez -eq "") {
    # Bedienbare Zeilen tragen ihren Text oft erst im Kindelement -
    # den innenliegenden Text uebernehmen, damit die Liste lesbar bleibt
    $innen = $alleTexte | Where-Object {
      $_.X1 -ge $e.X1 -and $_.Y1 -ge $e.Y1 -and $_.X2 -le $e.X2 -and $_.Y2 -le $e.Y2
    } | Sort-Object Flaeche -Descending | Select-Object -First 1
    if ($innen) { $bez = $innen.Text }
  }
  if ($bez -eq "") { $bez = if ($e.Id) { $e.Id } else { $e.Klasse } }
  $art = if ($e.Klickbar) { "bedienbar" } else { "Anzeige" }
  $liste += "{0,3}  {1,-40} {2,-12} {3}  [{4},{5}]-[{6},{7}]" -f $nr, $bez, $art, $e.Klasse, $e.X1, $e.Y1, $e.X2, $e.Y2
}

$g.Dispose()
$neu.Save($zielDatei, [System.Drawing.Imaging.ImageFormat]::Png)
$neu.Dispose()

$liste | Set-Content -Path $listeDatei -Encoding UTF8

Write-Host ""
Write-Host "  $nr Elemente markiert" -ForegroundColor Green
Write-Host "  Rot = bedienbar, Blau = reine Anzeige" -ForegroundColor DarkGray
Write-Host "  Bild:  $zielDatei" -ForegroundColor Cyan
Write-Host "  Liste: $listeDatei" -ForegroundColor Cyan
Write-Host ""
$liste | Select-Object -First 25 | ForEach-Object { Write-Host "  $_" }
if ($nr -gt 25) { Write-Host "  ... $($nr - 25) weitere in der Liste" -ForegroundColor DarkGray }
Write-Host ""
