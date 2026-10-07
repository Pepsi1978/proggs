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

/**
 * Maße einer Zeitleiste. Senkrecht in Inhaltskoordinaten (Fensterposition + Scrollstand beim Messen), damit sie beim
 * Randscrollen nicht einen Frame hinterherhinken: Die Fensterlage ergibt sich immer frisch aus `wert - scrollWert()`.
 */
class LeistenMass(val links: Float, val rechts: Float, val startImInhalt: Float, val stundePx: Float, val lueckePx: Float, val band: Zeitband) {
    val vonMin: Int get() = band.vonMin
    val bisMin: Int get() = band.bisMin
    /** Länge des Zeitbands (vonMin bis bisMin, Lücken zusammengerückt) in px. */
    val bandPx: Float = band.hoehe(stundePx, lueckePx)
    /** Minute an der Höhe [px] unter dem Bandanfang. */
    fun minute(px: Float): Float = band.minute(px, stundePx, lueckePx)
}

/**
 * Zustand einer Ziehgeste über der ganzen Liste. Die Fingerposition wird nur in Zeichenebenen gelesen
 * (graphicsLayer), damit das Ziehen nichts neu aufbaut — nur Ziel und Uhrzeit ändern sich stufenweise.
 *
 * Karte, Zeitlinie, Uhrzeit-Marke und Geisterblock hängen alle an [linieY] (eine einzige Quelle aus Finger und
 * Griff). Darum können sie nicht auseinanderlaufen — weder beim Randscrollen noch beim Wechsel der Uhrzeit.
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

    /** Aktueller Scrollstand der Liste in px — live, also schon nach dispatchRawDelta und vor dem nächsten Layout. */
    var scrollWert: () -> Float = { 0f }

    /** Abstand der Zeitlinie über der Kartenoberkante in px (1 mm). */
    var linieAbstand = 0f

    /** Fensterlage der Ebene, in der Karte und Zeitlinie gezeichnet werden (Fensterwert − ursprung = Zeichenwert). */
    var ursprung = Offset.Zero

    /** Spanne (von, bis in Minuten), die während des Ziehens auf allen Zeitleisten gilt; null = keine Angabe. */
    var ziehSpanne: Pair<Int, Int>? = null

    /** Scrollt die Liste sofort um px (dispatchRawDelta), noch im selben Frame wie der Ziehstart. */
    var scrolleUm: (Float) -> Unit = {}

    /** Höhe der Zeitlinie im Fenster: immer genau [linieAbstand] über der gezogenen Karte. Nur in Zeichenebenen lesen. */
    fun linieY(): Float = finger.value.y - griff.y - linieAbstand

    fun leiste(tag: Long): LeistenMass? = leisten[tag]

    fun registriere(schluessel: String, bereich: Rect, ziel: Ziel) { ziele[schluessel] = bereich to ziel }

    /** Meldet eine Zeitleiste mit ihrer Fensterlage an (aus onGloballyPositioned) und rechnet sie in Inhaltslage um. */
    fun registriereLeiste(tag: Long, links: Float, rechts: Float, startImFenster: Float, stundePx: Float, lueckePx: Float, band: Zeitband) {
        leisten[tag] = LeistenMass(links, rechts, startImFenster + scrollWert(), stundePx, lueckePx, band)
        // Ändert sich die Leiste während des Ziehens (z. B. Spanne beim Ziehstart), gilt die neue Uhrzeit sofort.
        if (aufgabe != null) aktualisiere()
    }
    fun entferne(schluessel: String) { ziele.remove(schluessel) }
    fun entferneLeiste(tag: Long) { leisten.remove(tag) }

    fun start(a: Aufgabe, fingerImFenster: Offset, griffInKarte: Offset, kartenGroesse: IntSize) {
        aufgabe = a
        griff = griffInKarte
        groesse = kartenGroesse
        finger.value = fingerImFenster
        startFinger = fingerImFenster
        bewegt = false
        spanneAusgleichen(fingerImFenster.y)
        aktualisiere()
    }

    /**
     * Mit Automatik oder zusammengerückten Lücken zeigt eine Zeitleiste ohne Ziehen nur einen Teil des Tages, beim Ziehen
     * die ganze eingestellte Spanne. Der Wechsel lässt Inhalt über dem Finger wachsen oder schrumpfen — die Leiste spränge
     * unter dem stillen Finger auf eine andere Uhrzeit. Darum wird im selben Frame genau um diese Höhe nachgescrollt, und
     * die Maße aller Leisten werden sofort auf das kommende Layout umgerechnet (nicht erst, wenn Compose sie neu meldet:
     * Bleibt eine Leiste dank Ausgleich an ihrer Fensterstelle, muss Compose sie gar nicht neu melden).
     */
    private fun spanneAusgleichen(fingerY: Float) {
        val (von, bis) = ziehSpanne ?: return
        val voll = Zeitband(von, bis)
        val y = fingerY + scrollWert() // Finger in Inhaltslage (altes Layout)
        var verschiebung = 0f // Höhenänderung aller Leisten darüber
        var d = 0f // Höhenänderung über dem Finger
        for ((tag, m) in leisten.entries.sortedBy { it.value.startImInhalt }) {
            val neuPx = voll.hoehe(m.stundePx, m.lueckePx)
            val innen = y - m.startImInhalt
            d += when {
                innen < 0f -> 0f // Leiste liegt unter dem Finger: verschiebt nichts darüber
                // Finger auf der Leiste: Die Minute unter dem Finger bleibt dort — nur der Teil darüber ändert seine Höhe.
                innen < m.bandPx -> (m.minute(innen) - von) / 60f * m.stundePx - innen
                else -> neuPx - m.bandPx // Leiste ganz darüber: ganze Höhenänderung
            }
            leisten[tag] = LeistenMass(m.links, m.rechts, m.startImInhalt + verschiebung, m.stundePx, m.lueckePx, voll)
            verschiebung += neuPx - m.bandPx
        }
        // Nahe dem Listenende kann dispatchRawDelta begrenzen (maxValue noch vom alten Layout): dann bleibt ein Restsprung,
        // die Uhrzeit stimmt trotzdem, weil sie aus den umgerechneten Maßen und dem echten Scrollstand entsteht.
        if (d != 0f) scrolleUm(d)
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
        // Eine Uhrzeit gilt nur, solange die Linie auf dem Zeitband liegt (nicht im Rand darüber oder darunter): Sonst
        // liefe die Karte weiter, während die Uhrzeit am Bandende stehen bliebe — Linie, Marke und Landeplatz wichen ab.
        val s = scrollWert()
        val y = linieY()
        val leiste = leisten.entries.firstOrNull { (_, m) ->
            val start = m.startImInhalt - s
            val rand = 7.5f / 60f * m.stundePx // halber Rasterschritt: erste und letzte Viertelstunde voll erreichbar
            p.x >= m.links && p.x < m.rechts && y >= start - rand && y <= start + m.bandPx + rand
        }
        if (leiste != null) {
            val m = leiste.value
            // Die Uhrzeit gilt dort, wo die Linie die Zeitleiste schneidet — dieselbe Höhe, an der Linie und Geisterblock liegen.
            val roh = m.minute(y - (m.startImInhalt - s))
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
        aktualisiere() // Ablage aus dem allerletzten Stand von Finger, Scroll und Layout, nie aus einem älteren.
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
