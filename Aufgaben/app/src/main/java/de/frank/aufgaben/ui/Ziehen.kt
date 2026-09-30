package de.frank.aufgaben.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import de.frank.aufgaben.data.Aufgabe
import kotlin.math.abs
import kotlin.math.roundToInt

/** Maße einer Zeitleiste in Fensterkoordinaten. */
class LeistenMass(val bereich: Rect, val startY: Float, val stundePx: Float, val vonMin: Int, val bisMin: Int)

/**
 * Zustand einer Ziehgeste über der ganzen Liste. Die Fingerposition wird nur in Zeichenebenen gelesen
 * (graphicsLayer), damit das Ziehen nichts neu aufbaut — nur Ziel und Uhrzeit ändern sich stufenweise.
 */
class ZiehZustand {
    var aufgabe by mutableStateOf<Aufgabe?>(null); private set
    val finger = mutableStateOf(Offset.Zero)
    var griff = Offset.Zero; private set
    var groesse by mutableStateOf(IntSize.Zero); private set
    var hoverZiel by mutableStateOf<String?>(null); private set
    /** Tag und Minuten, wenn die Karte über einer Zeitleiste schwebt. */
    var hoverZeit by mutableStateOf<Pair<Long, Int>?>(null); private set

    private val ziele = HashMap<String, Pair<Rect, Ziel>>()
    private val leisten = HashMap<Long, LeistenMass>()
    private var bewegt = false
    private var startFinger = Offset.Zero
    var letztesEnde = 0L; private set
    var beiAblage: (Aufgabe, Ziel) -> Unit = { _, _ -> }

    /** Bereich, in dem am Rand automatisch gescrollt wird (zwischen den Ablageleisten). */
    var scrollOben = 0f
    var scrollUnten = 0f

    fun registriere(schluessel: String, bereich: Rect, ziel: Ziel) { ziele[schluessel] = bereich to ziel }
    fun registriereLeiste(tag: Long, mass: LeistenMass) { leisten[tag] = mass }
    fun entferne(schluessel: String) { ziele.remove(schluessel) }
    fun entferneLeiste(tag: Long) { leisten.remove(tag) }

    fun start(a: Aufgabe, fingerImFenster: Offset, griffInKarte: Offset, kartenGroesse: IntSize) {
        aufgabe = a
        griff = griffInKarte
        groesse = kartenGroesse
        finger.value = fingerImFenster
        startFinger = fingerImFenster
        bewegt = false
        aktualisiere()
    }

    fun ziehe(delta: Offset) {
        finger.value += delta
        if (!bewegt && (finger.value - startFinger).getDistance() > 12f) bewegt = true
        aktualisiere()
    }

    fun aktualisiere() {
        if (aufgabe == null) return
        val p = finger.value
        // Ablage-Chips haben Vorrang, dann die Zeitleisten, dann die Bereiche.
        val chip = ziele.entries.firstOrNull { it.key.startsWith("chip_") && it.value.first.contains(p) }
        if (chip != null) { setze(chip.key, null); return }
        val leiste = leisten.entries.firstOrNull { it.value.bereich.contains(p) }
        if (leiste != null) {
            val m = leiste.value
            val kartenOben = p.y - griff.y
            val roh = m.vonMin + (kartenOben - m.startY) / m.stundePx * 60f
            val min = ((roh / 15f).roundToInt() * 15).coerceIn(m.vonMin, m.bisMin)
            setze(null, leiste.key to min)
            return
        }
        val sektion = ziele.entries.firstOrNull { !it.key.startsWith("chip_") && it.value.first.contains(p) }
        setze(sektion?.key, null)
    }

    private fun setze(ziel: String?, zeit: Pair<Long, Int>?) {
        if (hoverZiel != ziel) hoverZiel = ziel
        if (hoverZeit != zeit) hoverZeit = zeit
    }

    fun ende() {
        val a = aufgabe
        val ziel = hoverZeit?.let { Ziel.Tag(it.first, it.second) } ?: hoverZiel?.let { ziele[it]?.second }
        val warBewegt = bewegt
        zuruecksetzen()
        if (a != null && warBewegt && ziel != null) beiAblage(a, ziel)
    }

    fun abbruch() = zuruecksetzen()

    private fun zuruecksetzen() {
        aufgabe = null
        hoverZiel = null
        hoverZeit = null
        letztesEnde = System.currentTimeMillis()
    }

    /** Ein Tipp direkt nach dem Loslassen ist kein Tipp, sondern das Ende der Ziehgeste. */
    fun tippErlaubt(): Boolean = aufgabe == null && System.currentTimeMillis() - letztesEnde > 350
}

/** Macht eine Karte per langem Druck ziehbar. */
fun Modifier.ziehbar(a: Aufgabe, zustand: ZiehZustand): Modifier = composed {
    val aktuell by rememberUpdatedState(a)
    val haptik = LocalHapticFeedback.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    var groesse by remember { mutableStateOf(IntSize.Zero) }
    onGloballyPositioned { pos = it.positionInRoot(); groesse = it.size }
        .pointerInput(a.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { lokal ->
                    haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                    zustand.start(aktuell, pos + lokal, lokal, groesse)
                },
                onDrag = { change, delta -> change.consume(); zustand.ziehe(delta) },
                onDragEnd = { zustand.ende() },
                onDragCancel = { zustand.abbruch() },
            )
        }
}

/** Meldet eine Fläche als Ablageziel an. */
@Composable
fun Modifier.ablageZiel(zustand: ZiehZustand, schluessel: String, ziel: Ziel): Modifier {
    DisposableEffect(schluessel) { onDispose { zustand.entferne(schluessel) } }
    val z by rememberUpdatedState(ziel)
    return onGloballyPositioned { zustand.registriere(schluessel, it.boundsInRoot(), z) }
}

internal fun staerkeAmRand(y: Float, oben: Float, unten: Float, zone: Float): Float = when {
    y in oben..(oben + zone) -> -(1f - (y - oben) / zone)
    y in (unten - zone)..unten -> (y - (unten - zone)) / zone
    else -> 0f
}.let { it * abs(it) }
