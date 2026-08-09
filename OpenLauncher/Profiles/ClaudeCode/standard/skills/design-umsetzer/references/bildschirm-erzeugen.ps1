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

# `box-shadow` kann mehrere Lagen tragen. Compose kennt nur eine je Modifier und keine
# inset-Schatten — also: aeussere Lagen als gestapelte `.shadow(...)`, der inset-Lichtsaum
# oben als 1 dp Linie. Weglassen ist nicht zulaessig, er traegt die Plastizitaet.
function Schatten([string]$css, [string]$form) {
    if (-not $css) { return @() }
    $lagen = [regex]::Split($css, ',(?![^()]*\))')
    $raus = @()
    foreach ($lage in $lagen) {
        $l = $lage.Trim()
        if (-not $l) { continue }
        $farbe = AlsCompose $l
        if (-not $farbe) { continue }
        $zahlen = [regex]::Matches($l, '(-?[\d.]+)px') | ForEach-Object { [double]$_.Groups[1].Value }
        if ($zahlen.Count -lt 3) { continue }
        if ($l -match 'inset') {
            # nur der helle Saum oben interessiert; seitliche insets traegt der Rand
            if ($zahlen[1] -gt 0) { $raus += "KANTE:$farbe" }
            continue
        }
        $unschaerfe = $zahlen[2]
        $hoehe = [Math]::Max(1, [Math]::Round($unschaerfe / 2))
        $formArg = if ($form) { ", shape = $form" } else { "" }
        $raus += ".shadow(elevation = ${hoehe}f.dp$formArg, clip = false, ambientColor = $farbe, spotColor = $farbe)"
    }
    return $raus
}

# `linear-gradient(145deg, A, B 58%, C)` und `radial-gradient(...)`
function Verlauf([string]$css) {
    if (-not $css -or $css -notmatch 'gradient') { return $null }
    $farben = [regex]::Matches($css, '(rgba?\([^)]*\)|color\(srgb[^)]*\))') |
        ForEach-Object { AlsCompose $_.Value } | Where-Object { $_ }
    if ($farben.Count -lt 2) { return $null }
    $stellen = [regex]::Matches($css, '\)\s+([\d.]+)%') | ForEach-Object { [double]$_.Groups[1].Value / 100 }
    $teile = @()
    for ($i = 0; $i -lt $farben.Count; $i++) {
        $anteil = if ($i -eq 0) { 0.0 }
                  elseif ($i -eq $farben.Count - 1) { 1.0 }
                  elseif ($i - 1 -lt $stellen.Count) { $stellen[$i - 1] }
                  else { [Math]::Round($i / ($farben.Count - 1), 2) }
        $teile += "${anteil}f to $($farben[$i])"
    }
    if ($css -match 'radial-gradient') {
        return "Brush.radialGradient(listOf(" + (($farben) -join ', ') + "))"
    }
    return "Brush.linearGradient(" + ($teile -join ', ') + ")"
}

# --- Kopf -------------------------------------------------------------------------------
$name = ([IO.Path]::GetFileNameWithoutExtension($Messdatei) -replace '[^A-Za-z0-9]', '')
# Bildschirmdateien heissen oft "1-heute" — ein Bezeichner darf aber nicht mit einer Ziffer
# beginnen. Die Nummer bleibt erhalten, sie wandert nur nach hinten.
if ($name -match '^(\d+)(.*)$') { $name = $Matches[2] + $Matches[1] }
if (-not $name) { $name = "Bildschirm" }
$name = $name.Substring(0,1).ToUpper() + $name.Substring(1)

# Wie hoch ist der Entwurf wirklich? Das unterste Element bestimmt den Scroll-Bereich —
# ohne ihn waere bei einem 1742 dp hohen Bildschirm alles unterhalb des Sichtfensters
# unerreichbar.
$gesamthoehe = [Math]::Ceiling(
    (($m.elemente | ForEach-Object { $_.kasten.y + $_.kasten.hoehe }) | Measure-Object -Maximum).Maximum
)
$sichtfenster = [Math]::Ceiling(($m.elemente | Select-Object -First 1).kasten.hoehe)

$sb = [Text.StringBuilder]::new()
[void]$sb.AppendLine("package $Paket")
[void]$sb.AppendLine(@"

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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
private fun ausPfaden(breite: Float, hoehe: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = "symbol", defaultWidth = breite.dp, defaultHeight = hoehe.dp,
        viewportWidth = breite, viewportHeight = hoehe,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

@Composable
fun $name`Erzeugt(modifier: Modifier = Modifier) {
    // Der Entwurf ist $gesamthoehe dp hoch, das Sichtfenster nur $sichtfenster dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(${gesamthoehe}f.dp)) {
"@)

$gezeichnet = 0
$zustaende = 0
# Ein verstecktes Element versteckt auch alles darunter.
$versteckteAeste = @($m.elemente | Where-Object { $_.versteckt } | ForEach-Object { $_.pfad + "/" })
foreach ($e in ($m.elemente | Sort-Object { $_.kasten.y }, { $_.kasten.x })) {
    if ($e.kasten.breite -le 0 -or $e.kasten.hoehe -le 0) { continue }
    $s = $e.stil
    $flaeche = AlsCompose $s.backgroundColor
    $randF   = AlsCompose $s.borderTopColor
    $randB   = Px $s.borderTopWidth
    $radius  = Px $s.borderTopLeftRadius
    $hatFlaeche = $flaeche -or ($randB -and $randB -gt 0) -or $s.backgroundImage -or $s.boxShadow -or $e.symbol
    if (-not $hatFlaeche -and -not $e.text) { continue }

    # Versteckte Elemente sind ZUSTAENDE (Ladezustand, Fehlerkarte, Antwort, Transkript).
    # Sie wurden fuer die Messung aufgedeckt, damit ihre Anordnung bekannt ist — gebaut
    # werden sie hinter einer Bedingung, sonst steht der ganze Bildschirm gleichzeitig da.
    $inZustand = $e.versteckt -or ($versteckteAeste | Where-Object { $e.pfad.StartsWith($_) })
    if ($inZustand) { $zustaende++; continue }

    $kennung = if ($e.klassen) { ($e.klassen -split ' ')[0] } else { $e.tag }
    [void]$sb.AppendLine("        // $kennung")
    $mod = "Modifier.offset(x = $($e.kasten.x)f.dp, y = $($e.kasten.y)f.dp)" +
           ".size(width = $($e.kasten.breite)f.dp, height = $($e.kasten.hoehe)f.dp)"

    $form = if ($radius -and $radius -gt 0) {
        if ($radius -gt 999) { "RoundedCornerShape(percent = 50)" } else { "RoundedCornerShape(${radius}f.dp)" }
    } else { $null }

    # Schatten stehen VOR clip/background — sonst zeichnen sie ueber die Flaeche.
    $kante = $null
    foreach ($teil in (Schatten $s.boxShadow $form)) {
        if ($teil -like "KANTE:*") { $kante = $teil.Substring(6) } else { $mod += $teil }
    }
    if ($form) { $mod += ".clip($form)" }

    # Verlauf schlaegt Volltonflaeche — im Entwurf traegt er die Plastizitaet.
    $verlauf = Verlauf $s.backgroundImage
    if ($verlauf) { $mod += ".background($verlauf)" }
    elseif ($flaeche) { $mod += ".background($flaeche)" }
    if ($randB -and $randB -gt 0 -and $randF) {
        $formArg = if ($radius -and $radius -gt 0) {
            if ($radius -gt 999) { ", RoundedCornerShape(percent = 50)" } else { ", RoundedCornerShape(${radius}f.dp)" }
        } else { "" }
        $mod += ".border(${randB}f.dp, $randF$formArg)"
    }

    if ($e.symbol) {
        # Das gezeichnete Symbol, nicht ein aehnliches aus einer Standardsammlung.
        $farbe = (AlsCompose $s.color) ?? "Color.Unspecified"
        $sicht = if ($e.sichtfeld) { $e.sichtfeld -split ' +' } else { @(0,0,24,24) }
        [void]$sb.AppendLine("        Box($mod) {")
        [void]$sb.AppendLine("            Icon(")
        [void]$sb.AppendLine("                imageVector = ausPfaden($($sicht[2])f, $($sicht[3])f,")
        foreach ($p in ($e.symbol | Where-Object { $_.d })) {
            $d = $p.d -replace '\\', '\\' -replace '"', '\"'
            [void]$sb.AppendLine("                    `"$d`",")
        }
        [void]$sb.AppendLine("                ),")
        [void]$sb.AppendLine("                contentDescription = null, tint = $farbe,")
        [void]$sb.AppendLine("                modifier = Modifier.fillMaxSize(),")
        [void]$sb.AppendLine("            )")
        [void]$sb.AppendLine("        }")
    } elseif ($e.text) {
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
        if ($kante) {
            [void]$sb.AppendLine("        Box($mod) { Box(Modifier.fillMaxWidth().height(1f.dp).background($kante)) }")
        } else {
            [void]$sb.AppendLine("        Box($mod)")
        }
    }
    $gezeichnet++
}

[void]$sb.AppendLine("        }")
[void]$sb.AppendLine("    }")
[void]$sb.AppendLine("}")

New-Item -ItemType Directory -Force -Path (Split-Path $Ziel) | Out-Null
[IO.File]::WriteAllText($Ziel, $sb.ToString(), [Text.UTF8Encoding]::new($false))
Write-Host "$gezeichnet Elemente nach $Ziel geschrieben (von $($m.elemente.Count) gemessenen; $zustaende Zustands-Elemente ausgelassen)."
