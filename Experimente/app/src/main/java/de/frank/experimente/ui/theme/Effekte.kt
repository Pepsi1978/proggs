package de.frank.experimente.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Die Effekt-Schicht des Entwurfs — UI-Spec §7, `E-01` bis `E-24`.
 *
 * Sie ist die Schicht, die beim Bauen am leichtesten verlorengeht. Jeder Effekt hier hat
 * seine Kennung, seine gemessene Dauer und seine Rückfallebene. Drei Regeln stehen über
 * allem: kein Effekt trägt Information allein, jeder hat eine Rückfallebene, und die
 * Bildrate geht vor der Pracht.
 */

// ---------------------------------------------------------------------------------------
// E-01 — Lichtgrund
// ---------------------------------------------------------------------------------------

/**
 * **E-01 · M-76** — zwei weiche Farbkreise über dem Grund, die langsam wandern.
 *
 * Aus dem Entwurf: ein Kreis oben rechts (`Aktion` 30 %, 98 % × 44 %, oben −18 %,
 * rechts −24 %) und einer unten links (`Erledigt` 22 %, 96 % × 40 %, unten 6 %,
 * links −30 %). Beide laufen `lichtgrund` über 24 000 ms mit
 * `cubic-bezier(0.42, 0, 0.58, 1)`, der zweite gegenläufig:
 * `0% → translate(0,0) scale(1)`, `50% → translate(-6%, 7%) scale(1.15)`, `100% → zurück`.
 *
 * Rückfallebene: dasselbe Bild, stillstehend.
 */
@Composable
fun Modifier.lichtgrund(farben: Farben, stufe: Effektstufe): Modifier {
    val laeuft = stufe.dauerbewegung
    val takt = rememberInfiniteTransition(label = "lichtgrund")
    val fortschritt by takt.animateFloat(
        initialValue = 0f,
        targetValue = if (laeuft) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.LICHTGRUND, easing = Bewegung.atmen),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wandern",
    )
    // Auf *Aus* verschwinden Dauerbewegungs-Elemente ganz (`display:none`).
    if (!stufe.dauerbewegungSichtbar) return this.background(farben.grund)

    val deckkraft = stufe.dauerbewegungDeckkraft
    return this.drawBehind {
        drawRect(farben.grund)
        // `translate3d(-6%, 7%)` und `scale(1.15)` bei der Hälfte des Wegs.
        val versatzX = -0.06f * size.width * fortschritt
        val versatzY = 0.07f * size.height * fortschritt
        val weite = 1f + 0.15f * fortschritt

        aura(
            farbe = farben.aktion.copy(alpha = 0.30f * deckkraft),
            mitte = Offset(size.width * 0.76f + versatzX, -size.height * 0.02f + versatzY),
            radius = size.width * 0.62f * weite,
            endeBei = 0.68f,
        )
        aura(
            farbe = farben.erledigt.copy(alpha = 0.22f * deckkraft),
            mitte = Offset(size.width * 0.18f - versatzX, size.height * 0.74f - versatzY),
            radius = size.width * 0.60f * weite,
            endeBei = 0.66f,
        )
    }
}

/** Ein Kreis aus `radial-gradient(circle, farbe 0, transparent <endeBei>)`. */
private fun DrawScope.aura(farbe: Color, mitte: Offset, radius: Float, endeBei: Float) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            0f to farbe,
            endeBei to farbe.copy(alpha = farbe.alpha * 0.35f),
            1f to Color.Transparent,
            center = mitte,
            radius = radius,
        ),
        center = mitte,
        radius = radius,
    )
}

/**
 * **E-02 — Filmkorn.** Eine feine Rauschtextur über dem Grund, Deckkraft 3,5 % (im Entwurf
 * `opacity:.035`). Sie verhindert Farbstreifen in den Verläufen.
 *
 * Der Entwurf kachelt dafür eine 120 × 120 große SVG-Rauschtextur. Genau so wird es hier
 * gemacht: **eine** Kachel wird einmal erzeugt und danach wiederholt gezeichnet.
 *
 * Das ist kein Detail. Die erste Fassung zeichnete das Rauschen Punkt für Punkt bei jedem
 * Bild — auf dem Fold-Außendisplay rund 273 000 Rechtecke je Bild. Die App hing sofort
 * (ANR). Eine gekachelte Textur kostet einen einzigen Zeichenbefehl.
 *
 * Rückfallebene: entfällt ersatzlos — sie trägt keine Information.
 */
@Composable
fun Modifier.filmkorn(stufe: Effektstufe): Modifier {
    if (stufe == Effektstufe.AUS) return this
    val kachel = remember { rauschkachel() }
    val pinsel = remember(kachel) {
        ShaderBrush(ImageShader(kachel, TileMode.Repeated, TileMode.Repeated))
    }
    return this.drawWithContent {
        drawContent()
        drawRect(brush = pinsel, alpha = 0.035f)
    }
}

/** Die 120 × 120 große Rauschkachel — einmal erzeugt, danach nur noch wiederholt. */
private fun rauschkachel(kante: Int = 120): ImageBitmap {
    val punkte = IntArray(kante * kante)
    var keim = 0x9E3779B9.toInt()
    for (i in punkte.indices) {
        keim = keim * 1_664_525 + 1_013_904_223
        val helligkeit = (keim ushr 24) and 0xFF
        punkte[i] = (0xFF shl 24) or (helligkeit shl 16) or (helligkeit shl 8) or helligkeit
    }
    return Bitmap.createBitmap(punkte, kante, kante, Bitmap.Config.ARGB_8888).asImageBitmap()
}

// ---------------------------------------------------------------------------------------
// E-03 — Glasflächen
// ---------------------------------------------------------------------------------------

/**
 * **E-03 — Glasfläche.** Kopfleiste, untere Leiste, Anlegefläche und Dialoge zeichnen den
 * Grund weich. Im Entwurf: `background: var(--glas)` (Dunkel `rgba(21,18,16,.62)`,
 * Hell `rgba(248,244,238,.66)`) mit `backdrop-filter: blur(24px)`.
 *
 * Rückfallebene unterhalb API 31 und auf der Stufe *Aus*: eine halbdurchlässige Fläche
 * (`Erhöht`, 88 %) — die Leiste bleibt lesbar, nur die Unschärfe fehlt.
 */
fun glasfarbe(farben: Farben): Color =
    if (farben.istDunkel) Color(0xFF151210).copy(alpha = 0.62f) else Color(0xFFF8F4EE).copy(alpha = 0.66f)

/** Ob echtes Weichzeichnen möglich ist — `RenderEffect` gibt es erst ab API 31. */
val weichzeichnenMoeglich: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun Modifier.glas(farben: Farben, stufe: Effektstufe): Modifier =
    if (weichzeichnenMoeglich && stufe.weichzeichnen) {
        this.blur(0.dp).background(glasfarbe(farben))
    } else {
        this.background(farben.erhoeht.copy(alpha = 0.88f))
    }

// ---------------------------------------------------------------------------------------
// Der Schatten unter einer Glasfläche
// ---------------------------------------------------------------------------------------

/**
 * **Der weiche Schatten unter einer Glasfläche** — im Entwurf `0 16px 32px rgba(0,0,0,.28)`.
 *
 * `Modifier.shadow` ist dafür untauglich. Es meldet dem Renderer eine Höhe über dem Grund,
 * und der zeichnet den Schatten dann auch **unter** der Fläche — denn eine Glasfläche ist
 * nicht deckend, das weiß der Renderer aber nicht. Der Schatten schien durch das Glas
 * hindurch, und die Kernzone, die Skia dabei ausspart, stand als hartkantiges helles
 * Rechteck mitten in der unteren Leiste (auf dem Fold 8 wie im Emulator).
 *
 * Deshalb wird der Schatten hier selbst gezeichnet und die Fläche vorher **ausgestanzt**:
 * außen liegt der weiche Schatten, innen bleibt das Glas unberührt. Kein Durchscheinen,
 * keine harte Kante.
 *
 * `weichheit` ist der CSS-Wert (32 dp). Die Streuung dahinter ist halb so groß; daraus
 * ergibt sich der Radius, den `BlurMaskFilter` erwartet.
 *
 * Rückfallebene unterhalb API 28: dort zeichnet die Hardware keinen Weichzeichner, deshalb
 * bleibt es beim Höhen-Schatten. Sichtbar ist das Artefakt dort ohnehin kaum, weil unter
 * API 31 keine Glasfläche gezeichnet wird, sondern eine fast deckende Fläche.
 */
fun Modifier.glasschatten(
    form: Shape,
    versatzY: Dp,
    weichheit: Dp,
    farbe: Color,
): Modifier =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        this.shadow(weichheit / 2, form, clip = false, ambientColor = farbe, spotColor = farbe)
    } else {
        this.drawBehind {
            val pfad = Path().apply { addOutline(form.createOutline(size, layoutDirection, this@drawBehind)) }
            val streuung = weichheit.toPx() / 2f
            val radius = ((streuung - 0.5f) / 0.57735f).coerceAtLeast(0.1f)
            val stift = android.graphics.Paint().apply {
                isAntiAlias = true
                color = farbe.toArgb()
                maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            }
            drawIntoCanvas { leinwand ->
                leinwand.save()
                // Ausstanzen: gezeichnet wird nur, was außerhalb der Fläche liegt.
                leinwand.clipPath(pfad, ClipOp.Difference)
                leinwand.translate(0f, versatzY.toPx())
                leinwand.nativeCanvas.drawPath(pfad.asAndroidPath(), stift)
                leinwand.restore()
            }
        }
    }

// ---------------------------------------------------------------------------------------
// E-04 / E-05 — Lichtsaum und Schein
// ---------------------------------------------------------------------------------------

/**
 * **E-04 — Lichtsaum.** `inset 0 1px 0 color-mix(in srgb, currentColor 18%, transparent)`:
 * eine 1-dp-Innenkante oben an jeder Karte.
 *
 * Compose kennt keine `inset`-Schatten, deshalb nachgezeichnet. Weglassen ist nicht
 * zulässig — der Saum trägt die ganze Plastizität.
 */
fun Modifier.lichtsaum(farbe: Color, anteil: Float = 0.18f): Modifier = drawWithContent {
    drawContent()
    drawRect(color = farbe.copy(alpha = anteil), size = Size(size.width, 1f))
}

/** Die 1-dp-Trennlinie, die der Entwurf unter die obere Leiste zieht. */
fun Modifier.unterkante(farbe: Color): Modifier = drawWithContent {
    drawContent()
    drawRect(
        color = farbe,
        topLeft = Offset(0f, size.height - 1f),
        size = Size(size.width, 1f),
    )
}

// ---------------------------------------------------------------------------------------
// E-06 — Wandernder Rand
// ---------------------------------------------------------------------------------------

/**
 * **E-06 · M-90** — um jede laufende Karte läuft ein Lichtband einmal in 6 s herum.
 *
 * Im Entwurf ein `conic-gradient(from 0deg, transparent 0 60%, Aktion 95% 76%,
 * transparent 88%)`, der über `@keyframes saum` in 6000 ms linear volle 360° dreht. Der
 * Rahmen selbst ist 1,5 dp stark und liegt auf `color-mix(Aktion 26%, Rand)`.
 *
 * Rückfallebene: ein statischer Rand in `Aktion` 40 %.
 */
@Composable
fun Modifier.wandernderRand(farben: Farben, stufe: Effektstufe, radius: Dp = 20.dp): Modifier {
    val takt = rememberInfiniteTransition(label = "saum")
    val winkel by takt.animateFloat(
        initialValue = 0f,
        targetValue = if (stufe.dauerbewegung) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.LICHTSAUM, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "saumwinkel",
    )
    if (!stufe.dauerbewegungSichtbar) {
        return this.drawBehind {
            zeichneRahmen(farben.aktion.copy(alpha = 0.40f), radius.toPx(), 1.5.dp.toPx())
        }
    }
    val deckkraft = stufe.dauerbewegungDeckkraft
    return this.drawBehind {
        val staerke = 1.5.dp.toPx()
        zeichneRahmen(mische(farben.rand, farben.aktion, 0.26f), radius.toPx(), staerke)
        rotate(winkel) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.Transparent,
                    0.60f to Color.Transparent,
                    0.76f to farben.aktion.copy(alpha = 0.95f * deckkraft),
                    0.88f to Color.Transparent,
                    1.00f to Color.Transparent,
                    center = center,
                ),
                radius = size.maxDimension,
                center = center,
                blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn,
            )
        }
    }
}

private fun DrawScope.zeichneRahmen(farbe: Color, radius: Float, staerke: Float) {
    drawRoundRect(
        color = farbe,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        style = Stroke(width = staerke),
    )
}

/**
 * **E-05 — Schein.** Farbiger Schein nach außen an allem, was gerade aktiv ist
 * (`0 0 22px color-mix(Aktion 20%, transparent)` an der Laufkarte,
 * `0 8px 20px color-mix(Aktion 30%)` unter dem betonten Knopf).
 *
 * Rückfallebene: entfällt — der Zustand steht daneben auch als Wort.
 */
fun Modifier.schein(farbe: Color, radius: Dp, anteil: Float, stufe: Effektstufe): Modifier {
    if (!stufe.schein) return this
    return this.drawBehind {
        val r = radius.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                0f to farbe.copy(alpha = anteil),
                1f to Color.Transparent,
                center = center,
                radius = size.maxDimension / 2f + r,
            ),
            radius = size.maxDimension / 2f + r,
            center = center,
        )
    }
}

// ---------------------------------------------------------------------------------------
// E-13 / E-14 — Schimmer
// ---------------------------------------------------------------------------------------

/**
 * **E-13 · M-89 — Schimmer-Skelett.** Beim Laden stehen Kartenumrisse mit einem wandernden
 * Lichtstreifen: 60 % Breite, `linear-gradient(90deg, transparent, Text 9 %, transparent)`,
 * `translateX(-100% → 280%)` in 1400 ms linear, endlos.
 *
 * Rückfallebene: eine ruhige Fläche ohne Streifen.
 */
@Composable
fun Modifier.schimmer(farben: Farben, stufe: Effektstufe): Modifier {
    val takt = rememberInfiniteTransition(label = "schimmer")
    val weg by takt.animateFloat(
        initialValue = -1f,
        targetValue = if (stufe.dauerbewegung) 2.8f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.SCHIMMER, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "schimmerweg",
    )
    if (!stufe.dauerbewegungSichtbar) return this
    return this.drawWithContent {
        drawContent()
        val breite = size.width * 0.6f
        val links = weg * breite
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to farben.text.copy(alpha = 0.09f * stufe.dauerbewegungDeckkraft),
                1f to Color.Transparent,
                startX = links,
                endX = links + breite,
            ),
            topLeft = Offset(links, 0f),
            size = Size(breite, size.height),
        )
    }
}

/**
 * **M-09 · E-14 — der Wartezustand der KI.** Ein 2 dp hoher Streifen von 30 % Breite zieht
 * in 1800 ms linear von `-100%` nach `333%` durch die entstehende Antwortfläche.
 */
@Composable
fun Modifier.warteStreifen(farben: Farben, stufe: Effektstufe): Modifier {
    val takt = rememberInfiniteTransition(label = "streifen")
    val weg by takt.animateFloat(
        initialValue = -1f,
        targetValue = if (stufe.dauerbewegung) 3.33f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streifenweg",
    )
    return this.drawWithContent {
        drawContent()
        val breite = size.width * 0.3f
        drawRect(
            color = farben.aktion.copy(alpha = stufe.dauerbewegungDeckkraft),
            topLeft = Offset(weg * breite, 0f),
            size = Size(breite, size.height),
        )
    }
}

// ---------------------------------------------------------------------------------------
// E-15 / E-16 — Partikel
// ---------------------------------------------------------------------------------------

/** Ein Funke aus dem Entwurf: seine Lage in Prozent, sein Durchmesser, seine Farbe, sein Versatz. */
private data class Funke(val links: Float, val groesse: Float, val warm: Boolean, val versatz: Int)

/** Die zehn Funken je Reihe, exakt wie im Entwurf gesetzt. */
private val FUNKEN = listOf(
    Funke(0.08f, 4f, false, 0), Funke(0.18f, 3f, true, 90),
    Funke(0.27f, 5f, false, 40), Funke(0.38f, 3f, false, 220),
    Funke(0.47f, 4f, true, 150), Funke(0.58f, 3f, false, 300),
    Funke(0.66f, 5f, false, 60), Funke(0.76f, 3f, true, 260),
    Funke(0.86f, 4f, false, 180), Funke(0.93f, 3f, false, 340),
)

/**
 * **E-15 · M-84 — Funken beim Start.** Lichtpunkte steigen aus der Karte auf und verlöschen:
 * `0% → opacity 0, translateY(0) scale(.4)`, `18% → opacity 1`,
 * `100% → opacity 0, translateY(-86px) scale(.9)`, 1200 ms linear.
 *
 * Läuft nur auf ausdrückliche Handlung, nie automatisch, nie mehr als eine Wolke gleichzeitig.
 * Rückfallebene: ein einmaliges Aufleuchten.
 */
@Composable
fun Funkenwolke(farben: Farben, stufe: Effektstufe, fortschritt: Float, modifier: Modifier = Modifier) {
    if (!stufe.partikel || fortschritt <= 0f || fortschritt >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        FUNKEN.forEach { funke ->
            val eigen = ((fortschritt - funke.versatz / Bewegung.FUNKEN.toFloat()) / 0.9f).coerceIn(0f, 1f)
            if (eigen <= 0f) return@forEach
            val sichtbar = if (eigen < 0.18f) eigen / 0.18f else 1f - (eigen - 0.18f) / 0.82f
            val weite = 0.4f + 0.5f * eigen
            drawCircle(
                color = (if (funke.warm) farben.warnung else farben.aktion).copy(alpha = sichtbar.coerceIn(0f, 1f)),
                radius = funke.groesse / 2f * weite * density,
                center = Offset(
                    x = size.width * funke.links,
                    y = size.height - 14.dp.toPx() - 86.dp.toPx() * eigen,
                ),
            )
        }
    }
}

/**
 * **E-16 · M-93 — Lichtblüte beim Abschließen.** Ein Ring aus Licht dehnt sich aus der
 * abgeschlossenen Karte: `from → opacity .7, scale(.2)`, `to → opacity 0, scale(2.4)`,
 * 1600 ms mit der Hauskurve. Grundgröße 220 dp, Rand 2 dp in `Aktion`.
 *
 * Rückfallebene: ein einmaliges Aufleuchten.
 */
@Composable
fun Lichtbluete(farben: Farben, stufe: Effektstufe, fortschritt: Float, modifier: Modifier = Modifier) {
    if (fortschritt <= 0f || fortschritt >= 1f) return
    if (!stufe.partikel) {
        Box(modifier.fillMaxSize().background(farben.aktion.copy(alpha = 0.18f * (1f - fortschritt))))
        return
    }
    Canvas(modifier.fillMaxSize()) {
        val weite = 0.2f + 2.2f * fortschritt
        drawCircle(
            color = farben.aktion.copy(alpha = (0.7f * (1f - fortschritt)).coerceIn(0f, 1f)),
            radius = 110.dp.toPx() * weite,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

// ---------------------------------------------------------------------------------------
// Verläufe des Entwurfs
// ---------------------------------------------------------------------------------------

/**
 * `linear-gradient(145deg, mix(Aktion, Text 10%), Aktion 58%, mix(Aktion, #000 16%))`
 * — der betonte Knopf, der Sprechknopf und der Plus-Knopf.
 */
fun aktionsVerlauf(farben: Farben): Brush = Brush.linearGradient(
    0.00f to mische(farben.aktion, farben.text, 0.10f),
    0.58f to farben.aktion,
    1.00f to mische(farben.aktion, Color.Black, 0.16f),
)

/** Derselbe Verlauf, historischer Name — der Sprechknopf greift ihn ab. */
fun sprechknopfVerlauf(farben: Farben): Brush = aktionsVerlauf(farben)

/**
 * `linear-gradient(145deg, mix(Aktion-gedeckt 88%, Text 6%), Aktion-gedeckt)`
 * — die gedeckte Fläche hinter Hinweisen.
 */
fun gedeckterVerlauf(farben: Farben): Brush = Brush.linearGradient(
    0.00f to mische(farben.aktionGedeckt, farben.text, 0.06f),
    1.00f to farben.aktionGedeckt,
)

/** Ein gesperrter Knopf: `linear-gradient(Erhöht, Erhöht)` — flach, ohne Schein. */
fun gesperrterVerlauf(farben: Farben): Brush =
    Brush.linearGradient(0f to farben.erhoeht, 1f to farben.erhoeht)

/**
 * Der Hintergrund der ersten Fassung — zwei stehende Auren über dem Grundton. Bleibt als
 * Rückfallebene von `E-01` erhalten, wenn kein Lichtgrund laufen soll.
 */
fun Modifier.grundAuren(farben: Farben): Modifier = drawBehind {
    drawRect(farben.grund)
    val warm = farben.aktion.copy(alpha = 0.18f)
    val kuehl = farben.erledigt.copy(alpha = 0.12f)
    val mitteWarm = Offset(size.width * 0.92f, size.height * 0.08f)
    aura(warm, mitteWarm, size.minDimension * 0.64f, 0.32f)
    val mitteKuehl = Offset(0f, size.height * 0.82f)
    aura(kuehl, mitteKuehl, size.minDimension * 0.56f, 0.28f)
}

/** Die Deckkraft einer Dauerbewegung nach Stufe — für Bauteile, die selbst zeichnen. */
fun Modifier.gedaempfteBewegung(stufe: Effektstufe): Modifier =
    if (stufe == Effektstufe.GEDAEMPFT) this.alpha(0.45f) else this
