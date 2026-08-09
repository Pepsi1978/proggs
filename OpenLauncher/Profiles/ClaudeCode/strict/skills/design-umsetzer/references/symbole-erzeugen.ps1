# Erzeugt aus der Messung eine Kotlin-Datei mit den Symbolen des Entwurfs.
#
# Warum: Ein Symbol aus einer Standardsammlung (`Icons.Outlined.*`) hat andere Proportionen,
# andere Strichstaerken und andere Ecken als das gezeichnete. Damit ist der Bildschirm
# sichtbar ein anderer. Die Messung enthaelt die echten Umrisse — dieses Skript giesst sie
# in `ImageVector`s, ohne dass jemand einen Pfad abtippt.
#
#   .\symbole-erzeugen.ps1 -Messung <Spec-v2>\messung\<erscheinung> `
#                          -Ziel    <App>\src\main\java\<paket>\ui\theme\Symbole.kt `
#                          -Paket   de.frank.experimente.ui.theme

param(
    [Parameter(Mandatory = $true)][string]$Messung,
    [Parameter(Mandatory = $true)][string]$Ziel,
    [Parameter(Mandatory = $true)][string]$Paket
)

$ErrorActionPreference = "Stop"

# Aus einer Beschriftung einen gueltigen Kotlin-Namen machen.
function AlsName([string]$text) {
    $t = $text -replace 'ä','ae' -replace 'ö','oe' -replace 'ü','ue' -replace 'ß','ss'
    $t = $t -replace 'Ä','Ae' -replace 'Ö','Oe' -replace 'Ü','Ue'
    $teile = ($t -replace '[^A-Za-z0-9 ]', ' ') -split ' +' | Where-Object { $_ }
    (($teile | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1) }) -join '')
}

$symbole = [ordered]@{}

Get-ChildItem $Messung -Filter *.json | ForEach-Object {
    $o = Get-Content $_.FullName -Raw | ConvertFrom-Json
    foreach ($e in $o.elemente) {
        if (-not $e.symbol) { continue }
        $pfade = @($e.symbol | Where-Object { $_.d })
        if (-not $pfade) { continue }
        $schluessel = ($pfade | ForEach-Object { $_.d }) -join '|'
        if ($symbole.Contains($schluessel)) { continue }

        # Der naechstgelegene Vorfahr mit Beschriftung oder Text gibt den Namen.
        $eltern = $o.elemente |
            Where-Object { $e.pfad.StartsWith($_.pfad) -and ($_.beschriftung -or $_.text) } |
            Sort-Object { $_.pfad.Length } -Descending | Select-Object -First 1
        $roh = if ($eltern.beschriftung) { $eltern.beschriftung } elseif ($eltern.text) { $eltern.text } else { "Symbol" }
        $name = AlsName $roh
        $i = 2; $endgueltig = $name
        while ($symbole.Values.Name -contains $endgueltig) { $endgueltig = "$name$i"; $i++ }

        $sicht = if ($e.sichtfeld) { $e.sichtfeld } else { "0 0 24 24" }
        $masse = $sicht -split ' +'
        $symbole[$schluessel] = [pscustomobject]@{
            Name = $endgueltig; Quelle = $roh
            Breite = $masse[2]; Hoehe = $masse[3]
            Pfade = ($pfade | ForEach-Object { $_.d })
        }
    }
}

$sb = [Text.StringBuilder]::new()
[void]$sb.AppendLine("package $Paket")
[void]$sb.AppendLine(@'

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Die Symbole des Entwurfs — erzeugt aus der Messung, nicht von Hand abgetippt und nicht
 * aus einer Standardsammlung ersetzt. Wer hier etwas aendert, aendert es an der falschen
 * Stelle: Quelle ist `Specs/<App>/v2/messung/`.
 *
 * Neu erzeugen: `design-umsetzer/references/symbole-erzeugen.ps1`
 */
private fun ausPfaden(name: String, breite: Float, hoehe: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = breite.dp, defaultHeight = hoehe.dp,
        viewportWidth = breite, viewportHeight = hoehe,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

object Symbole {
'@)

foreach ($s in $symbole.Values) {
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("    /** $($s.Quelle) */")
    [void]$sb.AppendLine("    val $($s.Name): ImageVector by lazy {")
    [void]$sb.AppendLine("        ausPfaden(")
    [void]$sb.AppendLine("            `"$($s.Name)`", $($s.Breite)f, $($s.Hoehe)f,")
    foreach ($p in $s.Pfade) {
        $escaped = $p -replace '\\', '\\' -replace '"', '\"'
        [void]$sb.AppendLine("            `"$escaped`",")
    }
    [void]$sb.AppendLine("        )")
    [void]$sb.AppendLine("    }")
}
[void]$sb.AppendLine("}")

New-Item -ItemType Directory -Force -Path (Split-Path $Ziel) | Out-Null
[IO.File]::WriteAllText($Ziel, $sb.ToString(), [Text.UTF8Encoding]::new($false))
Write-Host "$($symbole.Count) Symbole nach $Ziel geschrieben."
$symbole.Values | ForEach-Object { "  {0,-28} {1}" -f $_.Name, $_.Quelle }
