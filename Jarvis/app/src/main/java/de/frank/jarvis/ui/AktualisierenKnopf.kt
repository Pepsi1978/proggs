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
    var druckSeit = 0L
    var losAm = 0L

    /** True when the last press lasted at least [grenzeMs] (long press for dragging): the following tap must do nothing. */
    fun warLang(grenzeMs: Long): Boolean = losAm - druckSeit >= grenzeMs

    fun imSchutzkreis(radiusPx: Float): Boolean {
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
 * Kein `lange` an antippen: Ein eigener Long-Click verbraucht den langen Druck, dann startet das Ziehen der Startliste nie
 * (Fehler vom 19.11.2026, Biomarker und Raumklima ließen sich nicht verschieben). Den Tipp nach langem Druck unterdrückt [KnopfSchutz.warLang].
 */
@Composable
fun Modifier.antippenMitSchutz(schutz: KnopfSchutz, imSchutz: () -> Unit, aktion: () -> Unit): Modifier {
    val dichte = LocalDensity.current
    val langMs = LocalViewConfiguration.current.longPressTimeoutMillis
    return onPlaced { schutz.karte = it }.pointerInput(schutz) {
        awaitPointerEventScope {
            while (true) {
                val changes = awaitPointerEvent(PointerEventPass.Initial).changes
                changes.firstOrNull { it.changedToDown() }?.let { schutz.druck = it.position; schutz.druckSeit = it.uptimeMillis }
                changes.firstOrNull { it.changedToUp() }?.let { schutz.losAm = it.uptimeMillis }
            }
        }
    }.antippen(unterdruecken = { schutz.warLang(langMs) }) {
        if (schutz.imSchutzkreis(with(dichte) { SCHUTZ_RADIUS.toPx() })) imSchutz() else aktion()
    }
}

/** Runder Aktualisieren-Knopf oben rechts in einer Startkarte; leuchtet, solange [laeuft]. Mit [schutz] meldet er seine Lage für den Schutzkreis. */
@Composable
fun AktualisierenKnopf(laeuft: Boolean, beschreibung: String, beschreibungLaeuft: String, schutz: KnopfSchutz? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).then(if (schutz != null) Modifier.onPlaced { schutz.knopf = it } else Modifier)
            .glas(f, 22.dp, erhoeht = 0.5f, toenung = if (laeuft) f.primaer else null).antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Refresh, if (laeuft) beschreibungLaeuft else beschreibung, tint = if (laeuft) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
    }
}
