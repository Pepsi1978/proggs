# Erzeugt app.ico: abgerundete Kachel mit Verlauf (Violett -> Blau), Glanzlicht, feinem Innenrand,
# Kreisbogen als Aktualisieren-Geste und einem Pfeil nach oben mit weichem Schatten.
# Einmalig noetig; die .exe bettet das Icon danach ueber ApplicationIcon ein.
Add-Type -AssemblyName System.Drawing

function New-RoundedPath {
    param([single]$x, [single]$y, [single]$w, [single]$h, [single]$r)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

# Gezeichnet wird einmal gross (512) und danach sauber heruntergerechnet - das gibt an den
# Kanten deutlich bessere Ergebnisse als direktes Zeichnen in 16 oder 32 Pixeln.
$gross = 512
$meister = New-Object System.Drawing.Bitmap($gross, $gross)
$g = [System.Drawing.Graphics]::FromImage($meister)
$g.SmoothingMode = 'AntiAlias'
$g.InterpolationMode = 'HighQualityBicubic'
$g.PixelOffsetMode = 'HighQuality'
$g.Clear([System.Drawing.Color]::Transparent)

$rand = $gross * 0.045
$kachel = $gross - (2 * $rand)
$radius = $gross * 0.23

# --- Schlagschatten unter der Kachel ---
$schattenPfad = New-RoundedPath ($rand) ($rand + $gross * 0.02) $kachel $kachel $radius
$schattenPinsel = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70, 20, 12, 60))
$g.FillPath($schattenPinsel, $schattenPfad)

# --- Kachel mit diagonalem Verlauf ---
$pfad = New-RoundedPath $rand $rand $kachel $kachel $radius
$flaeche = New-Object System.Drawing.Rectangle([int]$rand, [int]$rand, [int]$kachel, [int]$kachel)
$verlauf = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $flaeche,
    [System.Drawing.Color]::FromArgb(142, 92, 255),
    [System.Drawing.Color]::FromArgb(56, 150, 255),
    50.0)
$g.FillPath($verlauf, $pfad)

# --- Glanzlicht oben: schmaler heller Bogen, gibt der Flaeche Tiefe ---
$glanz = New-Object System.Drawing.Drawing2D.GraphicsPath
$glanz.AddEllipse($rand - $gross * 0.10, $rand - $gross * 0.42, $kachel + $gross * 0.20, $kachel * 0.82)
$alterClip = $g.Clip
$g.SetClip($pfad)
$glanzPinsel = New-Object System.Drawing.Drawing2D.PathGradientBrush($glanz)
$glanzPinsel.CenterColor = [System.Drawing.Color]::FromArgb(115, 255, 255, 255)
$glanzPinsel.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
$g.FillPath($glanzPinsel, $glanz)
$g.Clip = $alterClip

# --- Feiner Innenrand, damit die Kachel auf hellem Grund nicht ausfranst ---
$innen = New-RoundedPath ($rand + 2) ($rand + 2) ($kachel - 4) ($kachel - 4) ($radius - 2)
$innenStift = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(90, 255, 255, 255)), ([single]3)
$g.DrawPath($innenStift, $innen)

# --- Kreisbogen als Aktualisieren-Geste, offen oben rechts ---
$bogenRand = $gross * 0.225
$bogenGroesse = $gross - (2 * $bogenRand)
$bogenStift = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(205, 255, 255, 255)), ([single]($gross * 0.075))
$bogenStift.StartCap = 'Round'
$bogenStift.EndCap = 'Round'
# Offen nach oben rechts - genau dort setzt der Pfeil an, so lesen Ring und Pfeil als eine Geste.
$g.DrawArc($bogenStift, $bogenRand, $bogenRand, $bogenGroesse, $bogenGroesse, 20, 295)

# --- Pfeil nach oben, mit weichem Schatten fuer Plastizitaet ---
$w = $gross
# Schlanker gehalten als der Ring, damit beide Formen nebeneinander bestehen und das Zeichen
# auch bei 16 Pixeln noch als Pfeil im Kreis lesbar bleibt.
$punkte = @(
    (New-Object System.Drawing.PointF([single]($w * 0.500), [single]($w * 0.255))),
    (New-Object System.Drawing.PointF([single]($w * 0.665), [single]($w * 0.440))),
    (New-Object System.Drawing.PointF([single]($w * 0.577), [single]($w * 0.440))),
    (New-Object System.Drawing.PointF([single]($w * 0.577), [single]($w * 0.735))),
    (New-Object System.Drawing.PointF([single]($w * 0.423), [single]($w * 0.735))),
    (New-Object System.Drawing.PointF([single]($w * 0.423), [single]($w * 0.440))),
    (New-Object System.Drawing.PointF([single]($w * 0.335), [single]($w * 0.440)))
)

$pfeilSchatten = New-Object System.Drawing.Drawing2D.GraphicsPath
$verschoben = $punkte | ForEach-Object {
    New-Object System.Drawing.PointF([single]($_.X), [single]($_.Y + $w * 0.018))
}
$pfeilSchatten.AddPolygon([System.Drawing.PointF[]]$verschoben)
$schattenStift = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(55, 24, 10, 70))
$g.FillPath($schattenStift, $pfeilSchatten)

$pfeil = New-Object System.Drawing.Drawing2D.GraphicsPath
$pfeil.AddPolygon([System.Drawing.PointF[]]$punkte)
$g.FillPath([System.Drawing.Brushes]::White, $pfeil)

# Vorschau zum Anschauen ablegen (die .ico selbst laesst sich schlecht direkt betrachten).
$vorschau = Join-Path $PSScriptRoot 'icon-vorschau.png'
$meister.Save($vorschau, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()

# --- Alle Groessen aus dem Meisterbild herunterrechnen ---
$sizes = @(16, 24, 32, 48, 64, 128, 256)
$pngStreams = @()
foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $gs = [System.Drawing.Graphics]::FromImage($bmp)
    $gs.InterpolationMode = 'HighQualityBicubic'
    $gs.SmoothingMode = 'AntiAlias'
    $gs.PixelOffsetMode = 'HighQuality'
    $gs.Clear([System.Drawing.Color]::Transparent)
    $gs.DrawImage($meister, (New-Object System.Drawing.Rectangle(0, 0, $size, $size)))
    $gs.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngStreams += , $ms.ToArray()
    $bmp.Dispose()
}
$meister.Dispose()

# ICO-Container von Hand schreiben (PNG-komprimierte Eintraege, ab Vista unterstuetzt).
$out = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter($out)
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$sizes.Count)

$offset = 6 + (16 * $sizes.Count)
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = $sizes[$i]
    $bw.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
    $bw.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
    $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Write([uint16]1); $bw.Write([uint16]32)
    $bw.Write([uint32]$pngStreams[$i].Length)
    $bw.Write([uint32]$offset)
    $offset += $pngStreams[$i].Length
}
foreach ($png in $pngStreams) { $bw.Write($png) }
$bw.Flush()

$ziel = Join-Path $PSScriptRoot 'app.ico'
[System.IO.File]::WriteAllBytes($ziel, $out.ToArray())
$bw.Dispose(); $out.Dispose()

Write-Output "Icon erstellt: $ziel  ($($sizes -join ', ') Pixel)"
