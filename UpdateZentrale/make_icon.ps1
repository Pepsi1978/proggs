# Erzeugt app.ico: violetter Farbverlauf mit Aufwaerts-Pfeil (Update-Symbol).
# Einmalig noetig; die .exe bettet das Icon danach ueber ApplicationIcon ein.
Add-Type -AssemblyName System.Drawing

$sizes = @(16, 32, 48, 64, 128, 256)
$pngStreams = @()

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.Clear([System.Drawing.Color]::Transparent)

    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(124, 92, 255),
        [System.Drawing.Color]::FromArgb(77, 163, 255),
        45.0)

    $radius = [int]($size * 0.22)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc(0, 0, $d, $d, 180, 90)
    $path.AddArc($size - $d, 0, $d, $d, 270, 90)
    $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
    $path.AddArc(0, $size - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.FillPath($brush, $path)

    # Pfeil nach oben
    $w = $size
    $pfeil = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pts = @(
        (New-Object System.Drawing.PointF([single]($w * 0.50), [single]($w * 0.20))),
        (New-Object System.Drawing.PointF([single]($w * 0.76), [single]($w * 0.50))),
        (New-Object System.Drawing.PointF([single]($w * 0.61), [single]($w * 0.50))),
        (New-Object System.Drawing.PointF([single]($w * 0.61), [single]($w * 0.80))),
        (New-Object System.Drawing.PointF([single]($w * 0.39), [single]($w * 0.80))),
        (New-Object System.Drawing.PointF([single]($w * 0.39), [single]($w * 0.50))),
        (New-Object System.Drawing.PointF([single]($w * 0.24), [single]($w * 0.50)))
    )
    $pfeil.AddPolygon([System.Drawing.PointF[]]$pts)
    $g.FillPath([System.Drawing.Brushes]::White, $pfeil)

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngStreams += , $ms.ToArray()

    $g.Dispose(); $bmp.Dispose(); $brush.Dispose(); $path.Dispose(); $pfeil.Dispose()
}

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

Write-Output "Icon erstellt: $ziel"
