package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/** Radius des Schutzkreises um einen Knopf in einer antippbaren Karte, gemessen von der Knopfmitte (Knopf 22 dp + Daumenrand; Frank, 15.11.2026). */
internal val SCHUTZ_RADIUS = 72.dp

/** Lage von Karte und Knopf und die letzte Druckstelle (in Kartenkoordinaten); kein State, nur beim Tippen gelesen. */
class KnopfSchutz {
    var karte: LayoutCoordinates? = null
    var knopf: LayoutCoordinates? = null
    var druck: Offset? = null
        private set
    private var zeiger: PointerId? = null
    private var druckSeit = 0L
    private var losAm = 0L

    // Values belong to exactly one physical gesture: set on the first finger's down, cleared once a click used them.
    // An accessibility or keyboard click has no pointer gesture and therefore finds nothing stale here.
    private var frisch = false

    internal fun gedrueckt(id: PointerId, position: Offset, zeit: Long) {
        zeiger = id; druck = position; druckSeit = zeit; losAm = zeit; frisch = true
    }

    internal fun losgelassen(id: PointerId, zeit: Long) { if (id == zeiger) losAm = zeit }

    /** Marks the current gesture as used, so a later non-pointer click neither gets suppressed nor routed by an old position. */
    fun erledigt() { frisch = false; druck = null; zeiger = null }

    /**
     * True when the current gesture was a long press (the card was lifted for dragging): the following click must do nothing.
     * Consumes the gesture when it returns true.
     */
    fun nachLangemDruck(grenzeMs: Long): Boolean =
        (frisch && losAm - druckSeit >= grenzeMs).also { if (it) erledigt() }

    fun imSchutzkreis(radiusPx: Float): Boolean {
        if (!frisch) return false
        val k = karte ?: return false
        val b = knopf ?: return false
        val p = druck ?: return false
        if (!k.isAttached || !b.isAttached) return false
        val mitte = k.localPositionOf(b, Offset(b.size.width / 2f, b.size.height / 2f))
        return (p - mitte).getDistance() <= radiusPx
    }
}

/**
 * Antippbare Startkarte mit Schutzkreis um ihren Knopf: Ein Tipp im Kreis führt [imSchutz] aus (meist dasselbe wie der Knopf),
 * jeder andere [aktion]. Die Druckstelle wird im Initial-Durchlauf nur gemerkt, nicht verbraucht; Haptik, Druckeffekt und TalkBack
 * kommen von antippen. Langes Drücken (Karte verschieben) löst nichts aus.
 * No `lange` on antippen: an own long click consumes the long press and the start list never begins dragging
 * (bug of 19.11.2026: Biomarker and Raumklima cards could not be moved). The click after a long press is suppressed via
 * [KnopfSchutz.nachLangemDruck]; children (tiles, buttons) use the same [schutz], since this card records every press inside it.
 */
@Composable
fun Modifier.antippenMitSchutz(schutz: KnopfSchutz, imSchutz: () -> Unit, aktion: () -> Unit): Modifier {
    val dichte = LocalDensity.current
    val langMs = LocalViewConfiguration.current.longPressTimeoutMillis
    return druckMerken(schutz).antippen(unterdruecken = { schutz.nachLangemDruck(langMs) }) {
        if (schutz.imSchutzkreis(with(dichte) { SCHUTZ_RADIUS.toPx() })) imSchutz() else aktion()
        schutz.erledigt()
    }
}

/**
 * Records position and duration of every press inside this element for [schutz] (Initial pass, nothing consumed).
 * Only the first finger of a gesture counts, so a second finger cannot mix up duration or position.
 */
fun Modifier.druckMerken(schutz: KnopfSchutz): Modifier = onPlaced { schutz.karte = it }.pointerInput(schutz) {
    awaitPointerEventScope {
        while (true) {
            val changes = awaitPointerEvent(PointerEventPass.Initial).changes
            changes.firstOrNull { it.changedToDown() }?.let { neu ->
                if (changes.none { it.id != neu.id && it.pressed }) schutz.gedrueckt(neu.id, neu.position, neu.uptimeMillis)
            }
            changes.forEach { if (it.changedToUp()) schutz.losgelassen(it.id, it.uptimeMillis) }
        }
    }
}

/** Runder Aktualisieren-Knopf oben rechts in einer Startkarte; leuchtet, solange [laeuft]. Mit [schutz] meldet er seine Lage für den Schutzkreis. */
@Composable
fun AktualisierenKnopf(laeuft: Boolean, beschreibung: String, beschreibungLaeuft: String, schutz: KnopfSchutz? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val langMs = LocalViewConfiguration.current.longPressTimeoutMillis
    // With schutz the button sits inside a draggable card: a long press lifts the card and must not refresh on release.
    val antippenModifier = if (schutz != null) Modifier.antippen(unterdruecken = { schutz.nachLangemDruck(langMs) }) { aktion(); schutz.erledigt() }
        else Modifier.antippen(aktion = aktion)
    Box(
        Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).then(if (schutz != null) Modifier.onPlaced { schutz.knopf = it } else Modifier)
            .glas(f, 22.dp, erhoeht = 0.5f, toenung = if (laeuft) f.primaer else null).then(antippenModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Refresh, if (laeuft) beschreibungLaeuft else beschreibung, tint = if (laeuft) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
    }
}
