package de.frank.experimente.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Formen und Maße aus `02-UI-SPEC.md` §4 und §5. px des Entwurfs sind bei `density: 1`
 * unmittelbar dp.
 */
object Formen {
    val eingabefeld = RoundedCornerShape(14.dp)
    val karte = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(24.dp)
    val vollrund = RoundedCornerShape(percent = 50)

    /** `20px 20px 6px 20px` — die eigene Sprechblase (design.html:radius(7)). */
    val blaseIch = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)

    /** `20px 20px 20px 6px` — die Blase der KI (design.html:radius(8)). */
    val blaseKi = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
}

object Masse {
    /** Bezugsbreite des Entwurfs: 412 × 915 dp bei `density: 1`. */
    val entwurfsbreite = 412.dp

    val rand = 20.dp
    val luftKlein = 8.dp
    val luft = 12.dp
    val luftGross = 20.dp
    val sprechknopf = 88.dp
    val leiste = 64.dp
}
