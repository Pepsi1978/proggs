# Erzeugt aus einer Messung den Compose-Code eines Bildschirms.
#
# Warum es das gibt: Solange ein Modell 150 gemessene Elemente liest und daraus von Hand
# Kotlin schreibt, geht bei jedem Durchgang etwas anderes verloren — mal ein Schatten, mal
# eine Grossschreibung, mal eine Anordnung. Das ist der Grund, warum ein Entwurf nie „in
# einem Zug" ankommt. Dieses Skript uebersetzt stattdessen mechanisch: jedes Element mit
# seinem gemessenen Kasten und seinen gemessenen Werten, ohne Auslegung.
#
#   .\bildschirm-erzeugen.ps1 -Messdatei <...>\messung\<erscheinung>\<name>.json `
#                             -Ziel <App>\...\ui\erzeugt\<Name>Erzeugt.kt `
#                             -Paket de.frank.experimente.ui.erzeugt
#
# Das Ergebnis ist die **Rohfassung**: masshaltig und vollstaendig, aber ohne Verhalten.
# Stufe 3 verdrahtet danach Funktionen und Zustaende — sie zeichnet nichts nach.

param(
    [Parameter(Mandatory = $true)][string]$Messdatei,
    [Parameter(Mandatory = $true)][string]$Ziel,
    [Parameter(Mandatory = $true)][string]$Paket
)

$ErrorActionPreference = "Stop"
$m = Get-Content $Messdatei -Raw | ConvertFrom-Json

# --- Farb- und Werthilfen ---------------------------------------------------------------
function AlsCompose([string]$css) {
    if (-not $css) { return $null }
    if ($css -match 'rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)\s*(?:[,/]\s*([\d.]+))?\s*\)') {
        $r = [int]$Matches[1]; $g = [int]$Matches[2]; $b = [int]$Matches[3]
        $a = if ($Matches[4]) { [double]$Matches[4] } else { 1.0 }
        $hex = "0xFF{0:X2}{1:X2}{2:X2}" -f $r, $g, $b
        $wert = if ($a -lt 1) { "Color($hex).copy(alpha = ${a}f)" } else { "Color($hex)" }
        return $wert
    }
    # color(srgb 0.12 0.10 0.09 / 0.82)
    if ($css -match 'color\(srgb\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)(?:\s*/\s*([\d.]+))?\s*\)') {
        $r = [int][Math]::Round([double]$Matches[1] * 255)
        $g = [int][Math]::Round([double]$Matches[2] * 255)
        $b = [int][Math]::Round([double]$Matches[3] * 255)
        $a = if ($Matches[4]) { [double]$Matches[4] } else { 1.0 }
        $hex = "0xFF{0:X2}{1:X2}{2:X2}" -f $r, $g, $b
        $wert = if ($a -lt 1) { "Color($hex).copy(alpha = ${a}f)" } else { "Color($hex)" }
        return $wert
    }
    return $null
}
function Px([string]$v) { if ($v -match '^([\d.]+)px$') { [double]$Matches[1] } else { $null } }

# --- Kopf -------------------------------------------------------------------------------
$name = ([IO.Path]::GetFileNameWithoutExtension($Messdatei) -replace '[^A-Za-z0-9]', '')
$name = $name.Substring(0,1).ToUpper() + $name.Substring(1)

$sb = [Text.StringBuilder]::new()
[void]$sb.AppendLine("package $Paket")
[void]$sb.AppendLine(@"

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ERZEUGT — nicht von Hand aendern.
 *
 * Quelle: ``$([IO.Path]::GetFileName($Messdatei))`` aus ``Specs/<App>/v2/messung/``.
 * Neu erzeugen: ``design-umsetzer/references/bildschirm-erzeugen.ps1``.
 *
 * Jedes Element steht an seiner gemessenen Stelle, mit seinen gemessenen Werten. Wer hier
 * etwas verschiebt, verschiebt es gegen die Vorlage — dann stimmt die Vorlage nicht mehr.
 */
@Composable
fun $name`Erzeugt(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
"@)

$gezeichnet = 0
foreach ($e in ($m.elemente | Sort-Object { $_.kasten.y }, { $_.kasten.x })) {
    if ($e.kasten.breite -le 0 -or $e.kasten.hoehe -le 0) { continue }
    $s = $e.stil
    $flaeche = AlsCompose $s.backgroundColor
    $randF   = AlsCompose $s.borderTopColor
    $randB   = Px $s.borderTopWidth
    $radius  = Px $s.borderTopLeftRadius
    $hatFlaeche = $flaeche -or ($randB -and $randB -gt 0)
    if (-not $hatFlaeche -and -not $e.text) { continue }

    $kennung = if ($e.klassen) { ($e.klassen -split ' ')[0] } else { $e.tag }
    [void]$sb.AppendLine("        // $kennung")
    $mod = "Modifier.offset(x = $($e.kasten.x)f.dp, y = $($e.kasten.y)f.dp)" +
           ".size(width = $($e.kasten.breite)f.dp, height = $($e.kasten.hoehe)f.dp)"
    if ($radius -and $radius -gt 0) {
        $r = if ($radius -gt 999) { "RoundedCornerShape(percent = 50)" } else { "RoundedCornerShape(${radius}f.dp)" }
        $mod += ".clip($r)"
    }
    if ($flaeche) { $mod += ".background($flaeche)" }
    if ($randB -and $randB -gt 0 -and $randF) {
        $formArg = if ($radius -and $radius -gt 0) {
            if ($radius -gt 999) { ", RoundedCornerShape(percent = 50)" } else { ", RoundedCornerShape(${radius}f.dp)" }
        } else { "" }
        $mod += ".border(${randB}f.dp, $randF$formArg)"
    }

    if ($e.text) {
        $txt = $e.text -replace '"', '\"'
        $gr = (Px $s.fontSize) ?? 16
        $zh = (Px $s.lineHeight) ?? ($gr * 1.5)
        $gew = if ($s.fontWeight) { $s.fontWeight } else { "400" }
        $farbe = (AlsCompose $s.color) ?? "Color.Unspecified"
        $gross = if ($s.textTransform -eq "uppercase") { ".uppercase()" } else { "" }
        [void]$sb.AppendLine("        Box($mod) {")
        [void]$sb.AppendLine("            Text(")
        [void]$sb.AppendLine("                text = `"$txt`"$gross,")
        [void]$sb.AppendLine("                style = TextStyle(")
        [void]$sb.AppendLine("                    fontSize = ${gr}f.sp, lineHeight = ${zh}f.sp,")
        [void]$sb.AppendLine("                    fontWeight = FontWeight($gew), color = $farbe,")
        [void]$sb.AppendLine("                ),")
        [void]$sb.AppendLine("            )")
        [void]$sb.AppendLine("        }")
    } else {
        [void]$sb.AppendLine("        Box($mod)")
    }
    $gezeichnet++
}

[void]$sb.AppendLine("    }")
[void]$sb.AppendLine("}")

New-Item -ItemType Directory -Force -Path (Split-Path $Ziel) | Out-Null
[IO.File]::WriteAllText($Ziel, $sb.ToString(), [Text.UTF8Encoding]::new($false))
Write-Host "$gezeichnet Elemente nach $Ziel geschrieben (von $($m.elemente.Count) gemessenen)."
