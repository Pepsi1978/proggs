package de.frank.stacklabor.ui.bausteine

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.delay

/*
 * PLAN — Die Wartezustände als eigene Bausteine (M-11, M-12, M-13) samt Pegel (M-15).
 *
 *  1. Sie liegen in `bausteine/`, weil B-02, B-07 und B-09 dieselben Zustände zeigen —
 *     eine Auswertung läuft immer für den ganzen Bildschirm, nie nur für eine Zeile.
 *  2. Jeder Wert stammt aus `03-MOTION-SPEC.md`; nirgends steht eine bequeme Standardkurve
 *     anstelle des gemessenen Wertes. Was die Messung nicht hergibt (die Schwingdauer der
 *     Pegelbalken), ist unten ausdrücklich als gewählt gekennzeichnet.
 *  3. „Bewegung reduzieren“ schaltet den Schimmer auf eine ruhige Fläche und das Pulsieren
 *     auf einen statischen Graustand — die Aussage „gilt gerade nicht“ bleibt erhalten.
 *     Der streamende Text ist Inhalt und erscheint dann sofort vollständig.
 *
 * Die Bezeichner folgen der Schreibweise des übrigen Quelltextes (ae/oe/ue); Kommentare,
 * angezeigte Texte und Beschreibungen tragen echte Umlaute.
 */

private val LEERRAUM = Regex("\\s+")

/** Die sechs Sätze der Fortschrittserzählung — wörtlich aus dem Entwurf (M-13). */
private val ERZAEHLUNG = listOf(
    "stelle den Stack zusammen …",
    "prüfe Wechselwirkungen …",
    "gewichte Ziel 1–4 …",
    "summiere die Wirkstoffmengen …",
    "formuliere die Begründungen …",
    "ordne die Ampeln zu …",
)

/** Wechseltakt der Erzählung und die Geduldsgrenze aus `03-MOTION-SPEC.md` §7. */
private const val ERZAEHLER_TAKT_MS = 3500L
private const val GEDULD_MS = 45_000L
private const val GEDULD_SATZ = "Das dauert länger als gewöhnlich."

/** Der Lichtstreifen wandert von −140 % auf 240 %, seine Breite sind 40 % (M-11). */
private const val SCHIMMER_START = -1.4f
private const val SCHIMMER_ENDE = 2.4f
private const val SCHIMMER_BREITE = 0.4f

/** Deckkraft des entsättigten Pulsierens: 1 → 0,45 → 1 in 1600 ms (M-12). */
private const val PULS_HELL = 1f
private const val PULS_DUNKEL = 0.45f

/** Die drei Höhen des gemessenen Pegel-Keyframes: 4 → 14 → 5 dp (M-15). */
private const val PEGEL_RUHE = 4f
private const val PEGEL_VOLL = 14f
private const val PEGEL_AUSKLANG = 5f

/**
 * Schwingdauer und Versatz der drei Balken. Die Messung nennt für `pegel` keine Dauer
 * („— / endlos“), nur die drei Höhen; beides ist deshalb hier gewählt und nicht gemessen.
 */
private const val PEGEL_DAUER_MS = 620
private const val PEGEL_VERSATZ_MS = 90

/** Das Wort steigt beim Erscheinen um 6 dp auf (M-13). */
private val WORT_AUFSTIEG = 6.dp

/**
 * **M-11 — Warte-Schimmer.** Ein Platzhalter, über den ein Lichtstreifen wandert:
 * X von −140 % auf 240 %, **1400 ms linear, endlos**, Streifenbreite 40 %.
 *
 * Bei reduzierter Bewegung bleibt die ruhige Fläche stehen — der Platz ist weiterhin belegt,
 * nur ohne Wanderung.
 */
@Composable
fun SchimmerFlaeche(modifier: Modifier = Modifier) {
    val grundton = Farben.rand.copy(alpha = if (Farben.istHell) 0.9f else 0.6f)
    val form = RoundedCornerShape(4.dp)

    if (LocalBewegungReduziert.current) {
        Box(modifier.clip(form).background(grundton))
        return
    }

    // Auf Hell trägt Weiß den Streifen, auf Dunkel der helle Textton — sonst bliebe er unsichtbar.
    val licht = (if (Farben.istHell) Color.White else Farben.text).copy(alpha = 0.55f)
    val takt = rememberInfiniteTransition(label = "schimmer")
    val lauf by takt.animateFloat(
        initialValue = SCHIMMER_START,
        targetValue = SCHIMMER_ENDE,
        animationSpec = infiniteRepeatable(
            tween(Dauer.SCHIMMER, easing = Kurven.linear), RepeatMode.Restart,
        ),
        label = "schimmerLauf",
    )
    Box(
        modifier
            .clip(form)
            .background(grundton)
            .drawBehind {
                val anfang = size.width * lauf
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, licht, Color.Transparent),
                        startX = anfang,
                        endX = anfang + size.width * SCHIMMER_BREITE,
                    ),
                )
            },
    )
}

/**
 * **M-12 — entsättigt und pulsierend.** Solange [aktiv] gilt, verliert der Inhalt seine Farbe
 * und atmet: Deckkraft 1 → 0,45 → 1 in **1600 ms, endlos**.
 *
 * Das ist der wichtigste Wartezustand: Er sagt, dass die angezeigten Farben **gerade nicht
 * gelten**. Bei reduzierter Bewegung bleibt der Graustand statisch stehen — die Aussage geht
 * dadurch nicht verloren, nur die Bewegung.
 */
@Composable
fun Modifier.entsaettigtPulsierend(aktiv: Boolean): Modifier {
    if (!aktiv) return this

    val deckkraft = if (LocalBewegungReduziert.current) {
        PULS_HELL
    } else {
        val takt = rememberInfiniteTransition(label = "m12")
        takt.animateFloat(
            initialValue = PULS_HELL,
            targetValue = PULS_DUNKEL,
            // Hin und zurück ergeben zusammen die gemessenen 1600 ms.
            animationSpec = infiniteRepeatable(
                tween(Dauer.WARTE_PULS / 2, easing = Kurven.atem), RepeatMode.Reverse,
            ),
            label = "m12Deckkraft",
        ).value
    }

    // DECISION: Entsättigt wird über eine Farbmatrix in einer eigenen Ebene, nicht über einen
    // RenderEffect — den gibt es erst ab Android 12, die App läuft ab Android 8.
    val graustufe = remember {
        Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
    }
    return this
        .graphicsLayer { alpha = deckkraft }
        .drawWithContent {
            drawIntoCanvas { leinwand ->
                leinwand.saveLayer(Rect(Offset.Zero, size), graustufe)
                drawContent()
                leinwand.restore()
            }
        }
}

/**
 * **M-13 — Fortschrittserzählung.** Solange [laeuft] gilt, wechselt alle 3,5 Sekunden einer der
 * sechs Sätze; die Überblendung dauert **240 ms**. Nach 45 Sekunden kommt „Das dauert länger als
 * gewöhnlich.“ dazu — abgebrochen wird deshalb nichts.
 */
@Composable
fun FortschrittsErzaehler(laeuft: Boolean, modifier: Modifier = Modifier) {
    if (!laeuft) return

    var schritt by remember { mutableIntStateOf(0) }
    var dauertLaenger by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ERZAEHLER_TAKT_MS)
            schritt = (schritt + 1) % ERZAEHLUNG.size
        }
    }
    LaunchedEffect(Unit) {
        delay(GEDULD_MS)
        dauertLaenger = true
    }

    val wechsel = if (LocalBewegungReduziert.current) 0 else Dauer.ERZAEHLER_WECHSEL
    Column(modifier) {
        Crossfade(
            targetState = schritt,
            animationSpec = tween(wechsel, easing = Kurven.zustand),
            label = "erzaehler",
        ) { stelle ->
            Text(ERZAEHLUNG[stelle], style = Schrift.grund, color = Farben.textSchwach)
        }
        if (dauertLaenger) {
            Text(
                GEDULD_SATZ,
                style = Schrift.klein,
                color = Farben.textSchwach,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * **M-13 — streamender Antworttext.** Der Text baut sich **wortweise** auf: je Wort Einblenden
 * und 6 dp Aufwärtsbewegung in **180 ms**, Abstand zwischen zwei Wörtern **60 ms**.
 *
 * Bei reduzierter Bewegung steht der Text sofort vollständig da — er ist Inhalt, keine
 * Animation, und darf deshalb nie zurückgehalten werden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamenderText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    if (LocalBewegungReduziert.current) {
        Text(text, style = style, color = Farben.text, modifier = modifier)
        return
    }

    val woerter = remember(text) { text.split(LEERRAUM).filter { it.isNotEmpty() } }
    var sichtbar by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        sichtbar = 0
        while (sichtbar < woerter.size) {
            delay(Dauer.WORT_ABSTAND)
            sichtbar++
        }
    }

    // DECISION: Jedes Wort ist ein eigener Text in einer FlowRow — nur so kann sich das einzelne
    // Wort bewegen. Die Wortlücke wird gemessen (Breite von „n n“ minus „nn“), damit der
    // Fließtext genauso dicht steht wie ein gewöhnlicher Absatz.
    val messer = rememberTextMeasurer()
    val dichte = LocalDensity.current
    val luecke = remember(style, dichte) {
        val ohne = messer.measure("nn", style).size.width
        val mit = messer.measure("n n", style).size.width
        with(dichte) { (mit - ohne).coerceAtLeast(1).toDp() }
    }
    val aufstieg = with(dichte) { WORT_AUFSTIEG.toPx() }

    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(luecke)) {
        woerter.forEachIndexed { nummer, wort ->
            val eingang by animateFloatAsState(
                targetValue = if (nummer < sichtbar) 1f else 0f,
                animationSpec = tween(Dauer.WORT, easing = Kurven.leit),
                label = "wort",
            )
            Text(
                wort,
                style = style,
                color = Farben.text,
                modifier = Modifier.graphicsLayer {
                    alpha = eingang
                    translationY = (1f - eingang) * aufstieg
                },
            )
        }
    }
}

/**
 * **M-15 — die drei Pegelbalken.** Sie schwingen durch die gemessenen Höhen 4 → 14 → 5 dp;
 * wie weit sie ausschlagen, bestimmt [pegel] (0 = Ruhe bei 4 dp, 1 = voller Ausschlag).
 *
 * Bei reduzierter Bewegung stehen die Balken still und zeigen allein den Pegelwert.
 */
@Composable
fun PegelBalken(pegel: Float, modifier: Modifier = Modifier) {
    val staerke = pegel.coerceIn(0f, 1f)
    val takt = if (LocalBewegungReduziert.current) null else {
        rememberInfiniteTransition(label = "pegel")
    }

    Row(
        modifier.height(PEGEL_VOLL.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(3) { nummer ->
            val schwung = if (takt == null) {
                PEGEL_VOLL
            } else {
                takt.animateFloat(
                    initialValue = PEGEL_RUHE,
                    targetValue = PEGEL_AUSKLANG,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = PEGEL_DAUER_MS
                            PEGEL_RUHE at 0 using Kurven.atem
                            PEGEL_VOLL at PEGEL_DAUER_MS / 2 using Kurven.atem
                            PEGEL_AUSKLANG at PEGEL_DAUER_MS
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(nummer * PEGEL_VERSATZ_MS),
                    ),
                    label = "pegelBalken",
                ).value
            }

            // Ohne Pegel bleibt der Balken auf der Ruhehöhe stehen, statt weiterzuschwingen.
            val hoehe = PEGEL_RUHE + (schwung - PEGEL_RUHE) * staerke
            Box(
                Modifier
                    .width(3.dp)
                    .height(hoehe.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Farben.akzent),
            )
        }
    }
}
