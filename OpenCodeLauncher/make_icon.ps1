# Erzeugt app.ico fuer OpenCodeLauncher (dunkles Feld + lila Sterne, zum Fluent-Look passend).
Add-Type -AssemblyName System.Drawing

$size = 256
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.Clear([System.Drawing.Color]::Transparent)

# Hintergrund: abgerundetes dunkles Rechteck mit weichem Lila-Verlauf.
$pad = 14
$inner = $size - (2 * $pad)
$rect = New-Object System.Drawing.Rectangle($pad, $pad, $inner, $inner)
$radius = 48
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddArc($rect.X, $rect.Y, $radius, $radius, 180, 90)
$path.AddArc($rect.Right - $radius, $rect.Y, $radius, $radius, 270, 90)
$path.AddArc($rect.Right - $radius, $rect.Bottom - $radius, $radius, $radius, 0, 90)
$path.AddArc($rect.X, $rect.Bottom - $radius, $radius, $radius, 90, 90)
$path.CloseFigure()

$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, ([System.Drawing.Color]::FromArgb(255, 30, 30, 38)), ([System.Drawing.Color]::FromArgb(255, 44, 44, 56)), 90)
$g.FillPath($brush, $path)

# aeusserer Akzent-Ring (lila, halbtransparent).
$pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(120, 91, 91, 214), 3)
$g.DrawPath($pen, $path)

# Zentraler Stern "Spark" (acht-zackig) in lila.
function DrawStar($cx, $cy, $outer, $inner, $color) {
    $pts = New-Object System.Drawing.PointF[] 16
    for ($i = 0; $i -lt 16; $i += 2) {
        $a = [Math]::PI/8 * $i - [Math]::PI/2
        $pts[$i]   = [System.Drawing.PointF]::new($cx + $outer * [Math]::Cos($a), $cy + $outer * [Math]::Sin($a))
        $a2 = $a + [Math]::PI/8
        $pts[$i+1] = [System.Drawing.PointF]::new($cx + $inner * [Math]::Cos($a2), $cy + $inner * [Math]::Sin($a2))
    }
    $sb = New-Object System.Drawing.SolidBrush $color
    $g.FillPolygon($sb, $pts)
}

# grosser Hauptstern (lila), kleiner Akzentstern oben rechts (heller).
DrawStar 128 128 78 32 ([System.Drawing.Color]::FromArgb(255, 140, 140, 240))
DrawStar 178 88 26 11 ([System.Drawing.Color]::FromArgb(255, 200, 200, 255))

# dezenter Glanzpunkt oben links (weicher weisser Kreis).
$glow = New-Object System.Drawing.Drawing2D.GraphicsPath
$glow.AddEllipse(70, 60, 40, 40)
$gb = New-Object System.Drawing.Drawing2D.PathGradientBrush($glow)
$gb.CenterColor = [System.Drawing.Color]::FromArgb(90, 255, 255, 255)
$gb.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
$g.FillPath($gb, $glow)

$g.Dispose()

# Bitmap -> Icon mit mehreren Aufloesungen (256, 128, 64, 48, 32, 16) speichern.
$icoPath = Join-Path $PSScriptRoot "app.ico"
$tmpPng = [System.IO.Path]::GetTempFileName() + ".png"
$bmp.Save($tmpPng, [System.Drawing.Imaging.ImageFormat]::Png)

# Mehrere Groessen in eine .ico packen (ICO-Format manuell).
$sizes = @(256, 128, 64, 48, 32, 16)
$dir = [System.IO.Path]::GetTempPath() + "ocl_ico"
if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
New-Item -ItemType Directory -Path $dir | Out-Null

$images = @()
foreach ($s in $sizes) {
    $b = New-Object System.Drawing.Bitmap($s, $s, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g2 = [System.Drawing.Graphics]::FromImage($b)
    $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g2.DrawImage($bmp, 0, 0, $s, $s)
    $g2.Dispose()
    $p = Join-Path $dir "$s.png"
    $b.Save($p, [System.Drawing.Imaging.ImageFormat]::Png)
    $images += [PSCustomObject]@{ Size = $s; Path = $p }
    $b.Dispose()
}

# ICO-Datei zusammenbauen (Header + je Eintrag + Bilddaten).
$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter $fs
# ICONDIR-Header
$bw.Write([UInt16]0)      # reserved
$bw.Write([UInt16]1)      # type = icon
$bw.Write([UInt16]$images.Count)  # count
# ICONDIRENTRY je Bild
$offset = 6 + 16 * $images.Count
$entries = @()
foreach ($img in $images) {
    $bytes = [System.IO.File]::ReadAllBytes($img.Path)
    $w = if ($img.Size -eq 256) { 0 } else { $img.Size }
    $bw.Write([Byte]$w)
    $bw.Write([Byte]$w)
    $bw.Write([Byte]0)   # colors
    $bw.Write([Byte]0)   # reserved
    $bw.Write([UInt16]1) # planes
    $bw.Write([UInt16]32) # bpp
    $bw.Write([UInt32]$bytes.Length)
    $bw.Write([UInt32]$offset)
    $entries += [PSCustomObject]@{ Bytes = $bytes; Offset = $offset }
    $offset += $bytes.Length
}
foreach ($e in $entries) { $bw.Write($e.Bytes) }
$bw.Dispose()
$fs.Dispose()

Write-Output "Icon geschrieben: $icoPath ($((Get-Item $icoPath).Length) Bytes)"
Remove-Item $dir -Recurse -Force
Remove-Item $tmpPng -Force
