package de.frank.stacklabor.ui.bausteine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.frank.stacklabor.ui.theme.Bewegungen
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * PLAN (M-01 … M-06, M-18)
 * ------------------------------------------------------------------------------------
 * Waehrend des Ziehens bleibt die Datenliste UNVERAENDERT. Der Zustand kennt nur zwei
 * Zahlen: den Startindex und den laufenden Zielindex. Daraus leitet er alles ab — den
 * Ausweich-Versatz jeder Nachbarzeile (M-02) und die live laufende Nummer (M-03). Erst
 * beim Einrasten (M-04) wird die neue Reihenfolge ein einziges Mal nach aussen gemeldet.
 *
 * Warum kein Live-Umbau der Datenliste: Wer bei jedem Grenzuebertritt umsortiert, muss
 * den Versatz des gezogenen Elements im selben Zug gegenrechnen — das flackert, und die
 * 12-ms-Staffelung aus M-02 laesst sich damit gar nicht bauen.
 *
 * Die Fingerposition im Sichtfenster ist immer `startOffsetPx + versatz`. Beim
 * automatischen Weiterrollen (M-05) wandert die Zeile mit dem Inhalt; genau der gerollte
 * Betrag wird auf den Versatz addiert, damit diese Summe konstant bleibt.
 */

/** M-05: Randzone, in der das automatische Weiterrollen anspringt. */
private val RANDZONE = 64.dp

/** M-05: Hoechstgeschwindigkeit am inneren Rand der Zone, in dp je Sekunde. */
private const val TEMPO_MAX = 900f

/** M-05: Verweildauer, bevor gerollt wird — ein kurzes Zittern loest nichts aus. */
private const val VERWEILEN_MS = 120L

/** M-02: Versatz je Zeile, damit die Nachbarn als Welle ausweichen. */
private const val AUSWEICH_VERSATZ_MS = 12

/** M-02: Mehr als zehn Stufen Verzoegerung wirken wie ein Nachzuegler. */
private const val AUSWEICH_STUFEN_MAX = 10

/** M-01: Die Zeile waechst beim Aufnehmen auf 1,04. */
private const val AUFNAHME_SKALIERUNG = 0.04f

/** M-01: Die Tiefe geht von 1 dp auf 8 dp. */
private const val AUFNAHME_TIEFE_DP = 7f

/** M-03: Y-Versatz, mit dem die neue Ziffer hereinlaeuft. */
private val ZIFFER_VERSATZ = 6.dp

/**
 * Der Zustand einer Zieh-Sitzung in einer `LazyColumn`.
 *
 * Er haelt bewusst keine Daten, sondern nur Positionen: So kann derselbe Baustein die
 * Ziele auf B-12 und die Mittel auf B-02 in der Ansicht „Einnahme" tragen (M-18).
 *
 * **Voraussetzung:** Die `LazyColumn` enthaelt ausschliesslich die ziehbaren Zeilen
 * (kein Kopf- und kein Fussbaustein) — die Anzahl kommt aus `totalItemsCount`.
 */
@Stable
class ZiehZustand internal constructor(
    private val listenZustand: LazyListState,
    private val bereich: CoroutineScope,
    private val haptik: HapticFeedback,
    private val dichte: Density,
) {
    internal var reduziert: Boolean = false
    internal var aufVerschieben: (Int, Int) -> Unit = { _, _ -> }
    internal var aufAbgelegt: (List<Int>) -> Unit = {}

    /** Der Index der Zeile, die gerade am Finger haengt — `null`, wenn nichts gezogen wird. */
    var gezogenerIndex by mutableStateOf<Int?>(null)
        private set

    /** Die Stelle, an der die Zeile im Augenblick landen wuerde. Grundlage von M-02 und M-03. */
    var zielIndex by mutableStateOf<Int?>(null)
        private set

    val istAmZiehen: Boolean get() = gezogenerIndex != null

    private var versatzRoh by mutableFloatStateOf(0f)

    /** Die Y-Verschiebung der gezogenen Zeile gegenueber ihrer Ausgangsposition, in px. */
    val versatz: Float get() = versatzRoh

    /** Fortschritt des Aufnehmens (M-01): 0 = liegt, 1 = angehoben. */
    private var aufnahme by mutableFloatStateOf(0f)

    internal val skalierung: Float get() = 1f + AUFNAHME_SKALIERUNG * aufnahme
    internal val tiefeDp: Float get() = 1f + AUFNAHME_TIEFE_DP * aufnahme

    private var startOffsetPx = 0f
    private var zeilenHoehePx = 0f

    private var rollTempo = 0f
    private var rollAuftrag: Job? = null

    /**
     * Die Original-Indizes in der Ordnung, die gerade zu sehen ist (M-03).
     * Ohne laufendes Ziehen ist das schlicht 0, 1, 2, …
     */
    val sichtbareReihenfolge: List<Int>
        get() {
            val anzahl = listenZustand.layoutInfo.totalItemsCount
            val liste = MutableList(anzahl) { it }
            val start = gezogenerIndex ?: return liste
            val ziel = zielIndex ?: return liste
            if (start !in 0 until anzahl) return liste
            liste.removeAt(start)
            liste.add(ziel.coerceIn(0, liste.size), start)
            return liste
        }

    /**
     * Die Nummer, die an der Zeile mit diesem Original-Index stehen muss — 1-basiert.
     *
     * Sie wird direkt gerechnet statt in `sichtbareReihenfolge` nachgeschlagen, damit
     * eine lange Liste beim Ziehen nicht je Bild quadratisch viel Arbeit macht.
     */
    fun nummerFuer(index: Int): Int {
        val start = gezogenerIndex ?: return index + 1
        val ziel = zielIndex ?: return index + 1
        return when {
            index == start -> ziel + 1
            ziel < start && index in ziel until start -> index + 2
            ziel > start && index in (start + 1)..ziel -> index
            else -> index + 1
        }
    }

    /** M-02: Um wie viel diese Zeile ausweichen muss, in px. */
    internal fun ausweichZiel(index: Int): Float {
        val start = gezogenerIndex ?: return 0f
        val ziel = zielIndex ?: return 0f
        return when {
            index == start -> 0f
            ziel < start && index in ziel until start -> zeilenHoehePx
            ziel > start && index in (start + 1)..ziel -> -zeilenHoehePx
            else -> 0f
        }
    }

    /** M-02: Die Welle laeuft vom gezogenen Element aus — je Zeile 12 ms spaeter. */
    internal fun ausweichVerzug(index: Int): Int {
        val start = gezogenerIndex ?: return 0
        return abs(index - start).coerceAtMost(AUSWEICH_STUFEN_MAX) * AUSWEICH_VERSATZ_MS
    }

    /** M-01: Aufnehmen — die Zeile hebt ab und meldet sich haptisch. */
    internal fun beginne(index: Int) {
        if (istAmZiehen) return
        val info = listenZustand.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        startOffsetPx = info.offset.toFloat()
        zeilenHoehePx = info.size.toFloat()
        versatzRoh = 0f
        gezogenerIndex = index
        zielIndex = index
        haptik.performHapticFeedback(HapticFeedbackType.LongPress)
        bereich.launch {
            animate(aufnahme, 1f, animationSpec = spez(Bewegungen.aufnehmen())) { wert, _ -> aufnahme = wert }
        }
    }

    /** Der Finger bewegt sich: Versatz fortschreiben, Zielstelle und Rollen nachziehen. */
    internal fun ziehe(dy: Float) {
        if (!istAmZiehen) return
        versatzRoh += dy
        aktualisiereZiel()
        aktualisiereRollen()
    }

    /** M-04 bzw. M-06: Loslassen — einrasten oder zurueckfliegen. */
    internal fun lasseLos() {
        val start = gezogenerIndex ?: return
        haltRollen()
        val info = listenZustand.layoutInfo
        val mitte = fingerMitte()
        // M-06: ueber den Listenrand hinaus losgelassen — es wird nichts umgestellt.
        if (mitte < info.viewportStartOffset || mitte > info.viewportEndOffset) {
            melde(start, start)
            flieg(0f, spez(Bewegungen.wechsel()), null)
            return
        }
        haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val ziel = zielIndex ?: start
        val reihenfolge = sichtbareReihenfolge
        flieg(
            zielVersatz = (ziel - start) * zeilenHoehePx,
            spez = spez(Bewegungen.einrasten()),
            meldung = if (ziel == start) null else reihenfolge,
        )
    }

    /** Das System hat die Geste einkassiert — Rueckflug wie M-06. */
    internal fun brichAb() {
        val start = gezogenerIndex ?: return
        haltRollen()
        melde(start, start)
        flieg(0f, spez(Bewegungen.wechsel()), null)
    }

    /**
     * Der letzte Flug: Versatz und Aufnahme laufen gemeinsam aus, danach wird der
     * Zustand geraeumt und die neue Reihenfolge gemeldet.
     *
     * DECISION: Gemeldet wird erst NACH dem Flug. Die Zeile steht dann optisch schon an
     * ihrer neuen Stelle — der Datenumbau im selben Bild ist deshalb unsichtbar.
     */
    private fun flieg(zielVersatz: Float, spez: AnimationSpec<Float>, meldung: List<Int>?) {
        bereich.launch {
            coroutineScope {
                launch {
                    animate(aufnahme, 0f, animationSpec = spez(Bewegungen.einrasten())) { wert, _ ->
                        aufnahme = wert
                    }
                }
                animate(versatzRoh, zielVersatz, animationSpec = spez) { wert, _ -> versatzRoh = wert }
            }
            raeume()
            meldung?.let { aufAbgelegt(it) }
        }
    }

    private fun raeume() {
        gezogenerIndex = null
        zielIndex = null
        versatzRoh = 0f
        aufnahme = 0f
    }

    private fun fingerMitte(): Float = startOffsetPx + versatzRoh + zeilenHoehePx / 2f

    private fun melde(start: Int, neu: Int) {
        if (zielIndex != neu) {
            zielIndex = neu
            aufVerschieben(start, neu)
        }
    }

    private fun aktualisiereZiel() {
        val start = gezogenerIndex ?: return
        val info = listenZustand.layoutInfo
        val anzahl = info.totalItemsCount
        if (anzahl < 2) return
        val mitte = fingerMitte()
        val treffer = info.visibleItemsInfo.firstOrNull { mitte >= it.offset && mitte < it.offset + it.size }
        val neu = when {
            treffer != null -> treffer.index
            mitte < info.viewportStartOffset -> info.visibleItemsInfo.firstOrNull()?.index ?: 0
            else -> info.visibleItemsInfo.lastOrNull()?.index ?: (anzahl - 1)
        }
        melde(start, neu.coerceIn(0, anzahl - 1))
    }

    /** M-05: Tempo aus der Eindringtiefe in die Randzone — linear von 0 auf 900 dp/s. */
    private fun berechneTempo(): Float {
        val info = listenZustand.layoutInfo
        val zonePx = RANDZONE.value * dichte.density
        if (zonePx <= 0f) return 0f
        val mitte = fingerMitte()
        val oben = info.viewportStartOffset + zonePx
        val unten = info.viewportEndOffset - zonePx
        return when {
            mitte < oben && listenZustand.canScrollBackward ->
                -TEMPO_MAX * ((oben - mitte) / zonePx).coerceIn(0f, 1f)

            mitte > unten && listenZustand.canScrollForward ->
                TEMPO_MAX * ((mitte - unten) / zonePx).coerceIn(0f, 1f)

            else -> 0f
        }
    }

    private fun aktualisiereRollen() {
        rollTempo = berechneTempo()
        if (rollTempo == 0f) {
            haltRollen()
        } else if (rollAuftrag == null) {
            rollAuftrag = bereich.launch { rolle() }
        }
    }

    private fun haltRollen() {
        rollTempo = 0f
        rollAuftrag?.cancel()
        rollAuftrag = null
    }

    private suspend fun rolle() {
        // Die 120 ms sind Eingabe-Entprellung, keine Animation — sie bleiben auch bei
        // reduzierter Bewegung stehen.
        delay(VERWEILEN_MS)
        var letzte = withFrameNanos { it }
        while (istAmZiehen) {
            val jetzt = withFrameNanos { it }
            val sekunden = (jetzt - letzte) / 1_000_000_000f
            letzte = jetzt
            rollTempo = berechneTempo()
            if (rollTempo == 0f) break
            val verbraucht = listenZustand.scrollBy(rollTempo * sekunden * dichte.density)
            if (verbraucht == 0f) break
            // Der Inhalt wandert, der Finger nicht: Der Versatz haelt die Zeile am Finger.
            versatzRoh += verbraucht
            aktualisiereZiel()
        }
        rollAuftrag = null
    }

    /** Bei reduzierter Bewegung faellt alles ausser M-02 auf 0 ms. */
    private fun spez(normal: AnimationSpec<Float>): AnimationSpec<Float> =
        if (reduziert) Bewegungen.sofort() else normal
}

/**
 * Erzeugt den Zieh-Zustand fuer eine `LazyColumn`.
 *
 * @param aufVerschieben wird bei jedem Positionswechsel gemeldet, waehrend der Finger noch liegt.
 * @param aufAbgelegt bekommt beim Einrasten die neue Ordnung als Original-Indizes.
 */
@Composable
fun rememberZiehZustand(
    listenZustand: LazyListState,
    aufVerschieben: (von: Int, nach: Int) -> Unit = { _, _ -> },
    aufAbgelegt: (reihenfolge: List<Int>) -> Unit,
): ZiehZustand {
    val bereich = rememberCoroutineScope()
    val haptik = LocalHapticFeedback.current
    val dichte = LocalDensity.current
    val reduziert = LocalBewegungReduziert.current
    val zustand = remember(listenZustand, dichte) { ZiehZustand(listenZustand, bereich, haptik, dichte) }
    SideEffect {
        zustand.reduziert = reduziert
        zustand.aufVerschieben = aufVerschieben
        zustand.aufAbgelegt = aufAbgelegt
    }
    return zustand
}

/**
 * Legt eine Listenzeile unter den Zieh-Zustand: die gezogene Zeile haengt am Finger
 * (M-01/M-04), alle anderen weichen aus (M-02).
 *
 * @param schluessel die Identitaet des Eintrags. Wechselt sie, faengt der Ausweich-Versatz
 *   bei 0 an, statt eine Bewegung aus der vorigen Ordnung nachzuschleppen.
 */
@Composable
fun Modifier.ziehbar(zustand: ZiehZustand, index: Int, schluessel: Any): Modifier {
    val amZiehen = zustand.gezogenerIndex == index
    val ziel = zustand.ausweichZiel(index)
    val verzug = zustand.ausweichVerzug(index)
    val amZiehenIrgendwo = zustand.istAmZiehen
    val ausweichen = remember(schluessel) { Animatable(0f) }

    // M-02 behaelt seine 220 ms auch bei reduzierter Bewegung: Ohne das Ausweichen sieht
    // Frank nicht, wohin die Zeile faellt.
    LaunchedEffect(ziel, verzug, amZiehenIrgendwo) {
        if (!amZiehenIrgendwo) {
            // Nach dem Ablegen steht die neue Ordnung bereits in den Daten — ein
            // Zurueckgleiten waere dieselbe Bewegung ein zweites Mal.
            ausweichen.snapTo(0f)
        } else {
            ausweichen.animateTo(ziel, tween(Dauer.ANTWORT, delayMillis = verzug, easing = Kurven.leit))
        }
    }

    return this
        .zIndex(if (amZiehen) 1f else 0f)
        .graphicsLayer {
            if (zustand.gezogenerIndex == index) {
                translationY = zustand.versatz
                scaleX = zustand.skalierung
                scaleY = zustand.skalierung
                shadowElevation = zustand.tiefeDp * density
                clip = false
            } else {
                // DECISION: Ohne laufendes Ziehen zaehlt hart 0 — der Umbau der Liste und
                // das Zuruecksetzen der Animation liegen sonst ein Bild auseinander, und
                // genau dieses eine Bild sieht man als Zucken.
                translationY = if (zustand.istAmZiehen) ausweichen.value else 0f
            }
        }
}

/**
 * Die Flaeche, an der gezogen wird: langes Druecken 300 ms, danach folgt die Zeile dem
 * Finger.
 *
 * Die Erkennung ist absichtlich handgeschrieben — `detectDragGesturesAfterLongPress`
 * haengt an der System-Schwelle (500 ms), M-01 verlangt aber 300 ms.
 */
@Composable
fun Modifier.ziehGriff(zustand: ZiehZustand, index: Int): Modifier =
    this.pointerInput(zustand, index) {
        awaitEachGesture {
            val runter = awaitFirstDown(requireUnconsumed = false)
            val langGedrueckt = try {
                withTimeout(Dauer.LANG_DRUECKEN) { waitForUpOrCancellation() }
                false
            } catch (_: PointerEventTimeoutCancellationException) {
                true
            }
            if (!langGedrueckt) return@awaitEachGesture
            zustand.beginne(index)
            if (!zustand.istAmZiehen) return@awaitEachGesture
            val sauber = drag(runter.id) { aenderung ->
                zustand.ziehe(aenderung.positionChange().y)
                aenderung.consume()
            }
            if (sauber) zustand.lasseLos() else zustand.brichAb()
        }
    }

/**
 * **M-03 — Nummern laufen live mit.**
 *
 * Die Ziffer wechselt, waehrend der Finger noch liegt: 120 ms linear, dazu 6 dp
 * Y-Versatz in Laufrichtung. Das ist der Sinn des ganzen Bildschirms — Frank sieht die
 * neue Prioritaet, bevor er sich entscheidet.
 */
@Composable
fun LaufendeZiffer(nummer: Int, stil: TextStyle, farbe: Color, modifier: Modifier = Modifier) {
    val dauer = if (LocalBewegungReduziert.current) 0 else Dauer.NUMMER
    val versatzPx = with(LocalDensity.current) { ZIFFER_VERSATZ.roundToPx() }
    AnimatedContent(
        targetState = nummer,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val hoch = targetState > initialState
            val blende = tween<Float>(dauer, easing = Kurven.linear)
            val lauf = tween<IntOffset>(dauer, easing = Kurven.linear)
            (slideInVertically(lauf) { if (hoch) versatzPx else -versatzPx } + fadeIn(blende))
                .togetherWith(
                    slideOutVertically(lauf) { if (hoch) -versatzPx else versatzPx } + fadeOut(blende),
                )
                .using(SizeTransform(clip = false))
        },
        label = "nummer",
    ) { wert ->
        Text(wert.toString(), style = stil, color = farbe)
    }
}
