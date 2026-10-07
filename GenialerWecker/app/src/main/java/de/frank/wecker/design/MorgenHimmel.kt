package de.frank.wecker.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.wecker.rememberResumed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Morgenruhes Himmel über dem bewegten Morgenlicht: dieselbe Tageszeit wie die Schlafszene im Kopf
 * ([Tageslauf]). Ist die Person tagsüber unterwegs, steht eine große, strahlende Sonne am Himmel —
 * mit langsam kreisenden Lichtstrahlen, Korona und Linsenreflexen —, und Schmetterlinge flattern
 * umher. Abends sinkt die Sonne rot glühend hinter den unteren Rand, der Himmel dunkelt ein, der Mond
 * geht auf und strahlt in weichen, atmenden Lichtringen, Sterne funkeln, eine Fledermaus kreist.
 *
 * Eine einzige Zeichenfläche; die Zeit wird nur im Zeichenblock gelesen, rund 30-mal pro Sekunde.
 * Bei reduzierter Bewegung steht ein ruhiges Nachtbild, passend zum Standbild der Szene.
 */
@Composable
internal fun MorgenHimmel(modifier: Modifier, dunkel: Boolean) {
    val reduziert = LocalBewegungReduziert.current
    val zeit = rememberTageszeit(rememberResumed() && !reduziert)
    val sterne = remember {
        val zufall = java.util.Random(46)
        List(46) { Stern(zufall.nextFloat(), zufall.nextFloat() * 0.75f, 0.6f + zufall.nextFloat() * 1.4f, zufall.nextFloat() * 6.28f, 1 + zufall.nextInt(4)) }
    }
    val farben = if (dunkel) HimmelsFarben.DUNKEL else HimmelsFarben.HELL
    Canvas(modifier) {
        val t = if (reduziert) Tageslauf.STANDBILD else zeit.value
        zeichneHimmel(t, farben, sterne)
    }
}

private class Stern(val x: Float, val y: Float, val r: Float, val phase: Float, val takt: Int)

private class HimmelsFarben(
    val nacht: Color, val nachtAlpha: Float, val abendrot: Color, val abendAlpha: Float,
    val sonnenKern: Color, val sonne: Color, val sonnenGlut: Color, val sonnenAlpha: Float,
    val mond: Color, val mondSchein: Color, val mondAlpha: Float, val stern: Color,
    val falter: List<Pair<Color, Color>>, val fledermaus: Color, val fledermausKante: Color,
    /** Hell zeichnet das Licht deckend, dunkel addiert es — so leuchtet es auf jedem Grund. */
    val licht: BlendMode,
) {
    companion object {
        val DUNKEL = HimmelsFarben(
            nacht = Color(0xFF040817), nachtAlpha = 0.45f, abendrot = Color(0xFFFF7A3C), abendAlpha = 0.16f,
            sonnenKern = Color(0xFFFFF6DC), sonne = Color(0xFFFFC65C), sonnenGlut = Color(0xFFFF6A2A), sonnenAlpha = 1f,
            mond = Color(0xFFF3E7C4), mondSchein = Color(0xFFBFD3FF), mondAlpha = 1f, stern = Color(0xFFFFF4E0),
            falter = listOf(Color(0xFFFFB24A) to Color(0xFF7A3E12), Color(0xFF9FC3FF) to Color(0xFF2E4D80), Color(0xFFF6D98B) to Color(0xFF8A5A1C)),
            fledermaus = Color(0xFF151927), fledermausKante = Color(0xFFBFD3FF),
            licht = BlendMode.Plus,
        )
        val HELL = HimmelsFarben(
            nacht = Color(0xFF26355F), nachtAlpha = 0.20f, abendrot = Color(0xFFFF8A4C), abendAlpha = 0.14f,
            sonnenKern = Color(0xFFFFFBEF), sonne = Color(0xFFFFC65C), sonnenGlut = Color(0xFFFF8A3C), sonnenAlpha = 0.85f,
            mond = Color(0xFFFFF8E4), mondSchein = Color(0xFFE6EEFF), mondAlpha = 0.95f, stern = Color(0xFFFFFFFF),
            falter = listOf(Color(0xFFF08A24) to Color(0xFF5A2A08), Color(0xFF4F86D9) to Color(0xFF1C3360), Color(0xFFE0B53C) to Color(0xFF6B4410)),
            fledermaus = Color(0xFF2B3046), fledermausKante = Color(0xFFFFFFFF),
            licht = BlendMode.SrcOver,
        )
    }
}

private fun DrawScope.zeichneHimmel(t: Float, f: HimmelsFarben, sterne: List<Stern>) {
    val w = size.width
    val h = size.height
    val tag = Tageslauf.tag(t)
    val nacht = 1f - tag
    val mond = Tageslauf.mond(t)
    val daemmerung = Tageslauf.daemmerung(t).coerceIn(0f, 1f)

    // 1. Der Himmel dunkelt ein: Nachtblau über allem, am oberen Rand tiefer.
    if (nacht > 0.005f) {
        drawRect(Brush.verticalGradient(listOf(f.nacht.copy(alpha = f.nachtAlpha * nacht), f.nacht.copy(alpha = f.nachtAlpha * nacht * .55f))))
    }
    // 2. Abend- und Morgenrot steigt vom unteren Rand auf.
    if (daemmerung > 0.005f) {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, f.abendrot.copy(alpha = f.abendAlpha * daemmerung)),
            startY = h * 0.35f, endY = h), blendMode = f.licht)
    }

    // 3. Sterne gehen mit der Nacht auf und funkeln.
    if (nacht > 0.05f) sterne.forEach { s ->
        val funkeln = 0.5f + 0.5f * Tageslauf.welle(t, 20 * s.takt, s.phase)
        val a = nacht * (0.25f + 0.75f * funkeln * funkeln)
        val c = Offset(s.x * w, s.y * h)
        val r = s.r * density
        drawCircle(Brush.radialGradient(listOf(f.stern.copy(alpha = a * .55f), Color.Transparent), center = c, radius = r * 4f), r * 4f, c)
        drawCircle(f.stern.copy(alpha = a), r * .7f, c)
    }

    // 4. Der Mond auf seinem Bogen, mit atmenden Lichtringen.
    if (mond > 0.01f) {
        val b = Tageslauf.mondBogen(t)
        val c = Offset(w * (0.12f + 0.62f * b), h * (1.06f - 0.80f * sin(PI.toFloat() * b)))
        mondZeichnen(c, w, t, mond, f)
    }

    // 5. Die Sonne auf ihrem Bogen: tagsüber hoch, abends sinkend und rot, morgens aufgehend.
    val bogen = Tageslauf.sonnenBogen(t)
    val hoehe = sin(PI.toFloat() * bogen)
    val sonnenPos = Offset(w * (0.08f + 0.84f * bogen), h * (1.07f - 0.86f * hoehe))
    if (tag > 0.01f && sonnenPos.y < h * 1.3f) sonneZeichnen(sonnenPos, w, h, t, tag, (1f - hoehe * 1.6f).coerceIn(0f, 1f), f)

    // 6. Tags Schmetterlinge, nachts eine Fledermaus.
    if (tag > 0.05f) repeat(3) { i -> schmetterling(i, t, w, h, tag, f) }
    if (nacht > 0.05f) fledermausFlug(t, w, h, nacht * max(mond, .4f), f)
}

private fun DrawScope.sonneZeichnen(c: Offset, w: Float, h: Float, t: Float, tag: Float, tief: Float, f: HimmelsFarben) {
    val a = tag * f.sonnenAlpha
    // Je tiefer die Sonne steht, desto röter und größer glüht sie.
    val farbe = lerp(f.sonne, f.sonnenGlut, tief)
    val r = w * (0.075f + 0.02f * tief)
    val atem = 0.85f + 0.15f * Tageslauf.welle(t, 8)

    // Großer Hof, weit über den Bildschirm.
    drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = .34f * a * atem), farbe.copy(alpha = .10f * a), Color.Transparent),
        center = c, radius = w * 0.95f), w * 0.95f, c, blendMode = f.licht)

    // Lichtstrahlen, die langsam um die Sonne kreisen (14 Stück, ganzzahlig nahtlos über den Zyklus).
    val dreh = t / Tageslauf.ZYKLUS * (360f / 14f) * 6f
    val lang = max(w, h) * 0.95f
    rotate(dreh, pivot = c) {
        repeat(14) { i ->
            val breite = if (i % 2 == 0) 4.2f else 2.4f
            val puls = 0.55f + 0.45f * Tageslauf.welle(t, 3 + i % 4, i * 1.3f)
            val winkel = (i * 360f / 14f) * PI.toFloat() / 180f
            val halb = breite * PI.toFloat() / 360f
            val strahl = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + cos(winkel - halb) * lang, c.y + sin(winkel - halb) * lang)
                lineTo(c.x + cos(winkel + halb) * lang, c.y + sin(winkel + halb) * lang)
                close()
            }
            drawPath(strahl, Brush.radialGradient(listOf(farbe.copy(alpha = .16f * a * puls), farbe.copy(alpha = .05f * a * puls), Color.Transparent),
                center = c, radius = lang), blendMode = f.licht)
        }
    }

    // Korona und ein feiner, pulsierender Lichtring.
    drawCircle(Brush.radialGradient(listOf(f.sonnenKern.copy(alpha = .55f * a), farbe.copy(alpha = .35f * a), Color.Transparent),
        center = c, radius = r * 2.6f), r * 2.6f, c, blendMode = f.licht)
    val ring = r * (1.9f + 0.25f * Tageslauf.welle(t, 6))
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, farbe.copy(alpha = .22f * a), Color.Transparent),
        center = c, radius = ring * 1.15f), ring * 1.15f, c, blendMode = f.licht)
    // Die Scheibe selbst.
    drawCircle(Brush.radialGradient(listOf(f.sonnenKern.copy(alpha = a), lerp(f.sonnenKern, farbe, .55f).copy(alpha = a), farbe.copy(alpha = a)),
        center = c - Offset(r * .25f, r * .25f), radius = r * 1.25f), r, c)

    // Linsenreflexe auf der Linie von der Sonne durch die Bildmitte.
    val mitte = Offset(w / 2f, h / 2f)
    listOf(0.55f to 0.035f, 0.85f to 0.06f, 1.2f to 0.022f, 1.45f to 0.09f).forEachIndexed { i, (wo, groesse) ->
        val p = c + (mitte - c) * wo
        val rr = w * groesse
        drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = (.10f - i * .015f) * a), farbe.copy(alpha = .03f * a), Color.Transparent),
            center = p, radius = rr), rr, p, blendMode = f.licht)
    }
}

private fun DrawScope.mondZeichnen(c: Offset, w: Float, t: Float, sicht: Float, f: HimmelsFarben) {
    val a = sicht * f.mondAlpha
    val r = w * 0.065f
    // Weiter, kühler Schein.
    drawCircle(Brush.radialGradient(listOf(f.mondSchein.copy(alpha = .26f * a), f.mondSchein.copy(alpha = .07f * a), Color.Transparent),
        center = c, radius = w * 0.75f), w * 0.75f, c, blendMode = f.licht)
    // Drei Lichtringe wandern langsam nach außen und verblassen — der Mond „strahlt“.
    repeat(3) { i ->
        val lauf = ((t / Tageslauf.ZYKLUS * 8f) + i / 3f) % 1f
        val rr = r * (1.4f + lauf * 4.5f)
        val ra = (1f - lauf) * sin(PI.toFloat() * lauf) * .22f * a
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, f.mondSchein.copy(alpha = ra), Color.Transparent),
            center = c, radius = rr), rr, c, blendMode = f.licht)
    }
    drawCircle(Brush.radialGradient(listOf(f.mond.copy(alpha = .55f * a), Color.Transparent), center = c, radius = r * 2.2f),
        r * 2.2f, c, blendMode = f.licht)
    // Die Scheibe mit Licht von oben links und ein paar Kratern.
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = a), f.mond.copy(alpha = a), lerp(f.mond, Color(0xFFB9AE90), .5f).copy(alpha = a)),
        center = c - Offset(r * .35f, r * .35f), radius = r * 1.5f), r, c)
    listOf(Triple(-0.3f, -0.2f, 0.22f), Triple(0.28f, 0.1f, 0.16f), Triple(-0.05f, 0.38f, 0.12f), Triple(0.35f, -0.35f, 0.09f)).forEach { (dx, dy, rr) ->
        drawCircle(Color(0xFF9C927A).copy(alpha = .22f * a), r * rr, c + Offset(r * dx, r * dy))
    }
}

/** Ein Schmetterling auf einer weiten, ruhigen Bahn; die Flügel schlagen etwa viermal pro Sekunde. */
private fun DrawScope.schmetterling(i: Int, t: Float, w: Float, h: Float, tag: Float, f: HimmelsFarben) {
    val bahnX = listOf(1, 2, 1)[i]
    val bahnY = listOf(2, 3, 3)[i]
    val phase = i * 2.1f
    val x = w * (0.5f + 0.40f * Tageslauf.welle(t, bahnX, phase) + 0.03f * Tageslauf.welle(t, 23 + i, phase))
    val y = h * (0.50f + 0.34f * Tageslauf.welle(t, bahnY, phase * 1.7f) + 0.02f * Tageslauf.welle(t, 31 + i * 2, phase))
    // Flugrichtung aus der Ableitung der waagerechten Bahn: Er schaut, wohin er fliegt.
    val nachRechts = cos(2f * PI.toFloat() * (bahnX * t / Tageslauf.ZYKLUS) + phase) >= 0f
    val schlag = abs(Tageslauf.welle(t, 184 + i * 9, phase))
    val g = (11f + i * 2.5f) * density
    val (hell, dunkel) = f.falter[i % f.falter.size]
    val a = tag
    translate(x, y) {
        scale(if (nachRechts) 1f else -1f, 1f, pivot = Offset.Zero) {
            rotate(-12f, pivot = Offset.Zero) {
                // Die Flügel klappen über die Breite zusammen und auf.
                scale(0.25f + 0.75f * schlag, 1f, pivot = Offset.Zero) {
                    listOf(-1f, 1f).forEach { seite ->
                        drawOval(Brush.radialGradient(listOf(hell.copy(alpha = a), dunkel.copy(alpha = a)), center = Offset(seite * g * .55f, -g * .45f), radius = g),
                            topLeft = Offset(if (seite < 0) -g * 1.1f else 0f, -g * 1.0f), size = Size(g * 1.1f, g * 0.95f))
                        drawOval(Brush.radialGradient(listOf(hell.copy(alpha = a * .9f), dunkel.copy(alpha = a)), center = Offset(seite * g * .4f, g * .3f), radius = g * .7f),
                            topLeft = Offset(if (seite < 0) -g * .8f else 0f, -g * 0.05f), size = Size(g * .8f, g * .7f))
                        drawCircle(Color.White.copy(alpha = .55f * a), g * .12f, Offset(seite * g * .6f, -g * .55f))
                    }
                }
                drawOval(dunkel.copy(alpha = a), topLeft = Offset(-g * .09f, -g * .7f), size = Size(g * .18f, g * 1.3f))
                drawLine(dunkel.copy(alpha = a), Offset(0f, -g * .65f), Offset(g * .25f, -g * 1.05f), g * .05f)
                drawLine(dunkel.copy(alpha = a), Offset(0f, -g * .65f), Offset(-g * .2f, -g * 1.05f), g * .05f)
            }
        }
    }
}

/** Eine Fledermaus kreist in weiten Bögen durch die obere Bildhälfte, mit hastigem Flügelschlag. */
private fun DrawScope.fledermausFlug(t: Float, w: Float, h: Float, sicht: Float, f: HimmelsFarben) {
    val x = w * (0.5f + 0.44f * Tageslauf.welle(t, 3, 0.7f))
    val y = h * (0.30f + 0.12f * Tageslauf.welle(t, 5, 1.9f) + 0.025f * Tageslauf.welle(t, 41))
    val schlag = Tageslauf.welle(t, 276)
    val g = 16f * density
    val c = Offset(x, y)
    // Ein Hauch Mondlicht auf der Kontur, damit sie sich vom Nachthimmel abhebt.
    drawCircle(Brush.radialGradient(listOf(f.fledermausKante.copy(alpha = .16f * sicht), Color.Transparent), center = c, radius = g * 2.6f),
        g * 2.6f, c, blendMode = f.licht)
    fledermaus(c, g, schlag, f.fledermaus.copy(alpha = sicht))
}
