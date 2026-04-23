# Generates PromptBoard's AppIcon.ico + the MSIX tile/logo PNGs from scratch.
# Run once when the brand colors or letter change, commit the outputs.
# Usage:   pwsh -File scripts/generate-icon.ps1
#
# The design is intentionally simple: a pastel-blue filled circle on a
# transparent background with a bold white "P" in the middle. This matches
# the pastel-color palette the app uses for category headers.

param(
    [string]$OutputDir = (Join-Path $PSScriptRoot "..\PromptBoard.App\Assets")
)

Add-Type -AssemblyName System.Drawing

function New-RoundIconBitmap {
    param(
        [int]$Size,
        [string]$Letter = "P"
    )

    $bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    # Pastel blue (#8BB9E0) with a soft vertical gradient for depth.
    $outer = [System.Drawing.Color]::FromArgb(255, 139, 185, 224)
    $inner = [System.Drawing.Color]::FromArgb(255, 106, 156, 201)
    $rect = New-Object System.Drawing.RectangleF(0, 0, $Size, $Size)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        $outer,
        $inner,
        [System.Drawing.Drawing2D.LinearGradientMode]::Vertical)

    # Filled circle centered in the canvas.
    $padding = [math]::Max(1, [int]($Size * 0.04))
    $g.FillEllipse($brush, $padding, $padding, $Size - 2 * $padding, $Size - 2 * $padding)

    # Centered bold white "P".
    $fontSize = $Size * 0.58
    $font = New-Object System.Drawing.Font("Segoe UI", $fontSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $textBrush = [System.Drawing.Brushes]::White
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    # Slight vertical nudge upward to balance the open bowl of the 'P'.
    $rectText = New-Object System.Drawing.RectangleF(0, [single](-$Size * 0.03), $Size, $Size)
    $g.DrawString($Letter, $font, $textBrush, $rectText, $format)

    $font.Dispose()
    $brush.Dispose()
    $g.Dispose()
    return $bmp
}

function Save-Png {
    param([System.Drawing.Bitmap]$Bitmap, [string]$Path)
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function New-IcoFile {
    param([System.Collections.Generic.List[System.Drawing.Bitmap]]$Bitmaps, [string]$Path)

    # Encode each size as a standalone PNG in memory, then write the
    # multi-image ICO container ourselves (GDI+ has no ICO writer).
    $streams = New-Object System.Collections.Generic.List[byte[]]
    foreach ($bmp in $Bitmaps) {
        $ms = New-Object System.IO.MemoryStream
        $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $streams.Add($ms.ToArray()) | Out-Null
        $ms.Dispose()
    }

    $fs = [System.IO.File]::Create($Path)
    $writer = New-Object System.IO.BinaryWriter($fs)
    try {
        # ICONDIR: reserved=0, type=1 (icon), count
        $writer.Write([uint16]0)
        $writer.Write([uint16]1)
        $writer.Write([uint16]$Bitmaps.Count)

        $offset = 6 + (16 * $Bitmaps.Count)
        for ($i = 0; $i -lt $Bitmaps.Count; $i++) {
            $bmp = $Bitmaps[$i]
            $payload = $streams[$i]
            # ICONDIRENTRY
            $width = if ($bmp.Width -ge 256) { 0 } else { [byte]$bmp.Width }
            $height = if ($bmp.Height -ge 256) { 0 } else { [byte]$bmp.Height }
            $writer.Write([byte]$width)
            $writer.Write([byte]$height)
            $writer.Write([byte]0)   # color palette (0 = no palette)
            $writer.Write([byte]0)   # reserved
            $writer.Write([uint16]1) # color planes
            $writer.Write([uint16]32) # bits per pixel
            $writer.Write([uint32]$payload.Length)
            $writer.Write([uint32]$offset)
            $offset += $payload.Length
        }

        foreach ($payload in $streams) {
            $writer.Write($payload)
        }
    }
    finally {
        $writer.Dispose()
        $fs.Dispose()
    }
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

# Multi-resolution ICO for the EXE/window icon.
$icoSizes = @(16, 24, 32, 48, 64, 128, 256)
$icoBitmaps = New-Object System.Collections.Generic.List[System.Drawing.Bitmap]
foreach ($size in $icoSizes) {
    $icoBitmaps.Add((New-RoundIconBitmap -Size $size)) | Out-Null
}

$icoPath = Join-Path $OutputDir "AppIcon.ico"
New-IcoFile -Bitmaps $icoBitmaps -Path $icoPath
Write-Host "Wrote $icoPath"

# MSIX / Start-menu tile images (keep names matching csproj + manifest).
$tiles = @{
    "Square44x44Logo.scale-200.png" = 88
    "Square44x44Logo.targetsize-24_altform-unplated.png" = 24
    "Square150x150Logo.scale-200.png" = 300
    "Wide310x150Logo.scale-200.png" = 300   # source, aspect handled by tile container
    "StoreLogo.png" = 50
    "LockScreenLogo.scale-200.png" = 48
    "SplashScreen.scale-200.png" = 620
}
foreach ($name in $tiles.Keys) {
    $size = $tiles[$name]
    $bmp = New-RoundIconBitmap -Size $size
    Save-Png -Bitmap $bmp -Path (Join-Path $OutputDir $name)
    $bmp.Dispose()
}

foreach ($b in $icoBitmaps) { $b.Dispose() }

Write-Host "Icon assets updated in $OutputDir"
