package de.frank.stacklabor.werftstudio.bruecke

import de.frank.stacklabor.werftstudio.domain.model.Einheit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Eine Dosis, wie sie in `stack_eintrag_dosis` landet: [stueckzahl] × [mengeJeStueck] [einheit]. */
data class Dosis(val stueckzahl: BigDecimal, val mengeJeStueck: BigDecimal?, val einheit: Einheit?)

/**
 * Wandelt Jarvis' Angaben in eine Dosis um, ohne etwas dazuzuerfinden.
 *
 * - `menge` ist die Gesamtmenge je Einnahme, so wie Frank sie sagt („2 Gramm, zwei Kapseln“ = zusammen 2 g aus 2 Kapseln).
 *   Die App speichert Stückzahl × Menge je Stück, also 2 × 1 g. Geht die Teilung nicht glatt auf, wird nachgefragt.
 * - `menge_je_stueck` nur, wenn Frank ausdrücklich „je Kapsel“ sagt.
 * - Einheiten werden nie umgerechnet: 2 g bleiben 2 g, 500 mg bleiben 500 mg. „Gramm“ und „Milligramm“ werden sicher erkannt.
 */
object DosisEingabe {
    sealed interface Ergebnis {
        data class Gut(val dosis: Dosis) : Ergebnis
        data class Fehlt(val frage: String) : Ergebnis
    }

    fun lies(menge: String?, mengeJeStueck: String?, stueckzahl: String?, einheit: String?): Ergebnis {
        val gesamt = menge?.takeIf { it.isNotBlank() }?.let { zahl(it) ?: return Ergebnis.Fehlt("Die Menge „$it“ ist keine Zahl.") }
        val jeStueck = mengeJeStueck?.takeIf { it.isNotBlank() }?.let { zahl(it) ?: return Ergebnis.Fehlt("Die Menge je Stück „$it“ ist keine Zahl.") }
        val stueck = stueckzahl?.takeIf { it.isNotBlank() }?.let { zahl(it) ?: return Ergebnis.Fehlt("Die Stückzahl „$it“ ist keine Zahl.") }
        val e = einheit?.takeIf { it.isNotBlank() }?.let { einheit(it) ?: return Ergebnis.Fehlt("Die Einheit „$it“ kennt Stack-Labor nicht. Erlaubt: mg, g, µg, ml, IE, Stück, Tasse.") }
        if (listOfNotNull(gesamt, jeStueck, stueck).any { it.signum() <= 0 }) return Ergebnis.Fehlt("Mengen und Stückzahl müssen größer als null sein.")
        if (gesamt != null && jeStueck != null) return Ergebnis.Fehlt("Bitte entweder die Gesamtmenge oder die Menge je Stück angeben, nicht beides.")
        if ((gesamt != null || jeStueck != null) && e == null) return Ergebnis.Fehlt("Zur Menge fehlt die Einheit (etwa Gramm oder Milligramm). Bitte bei Frank nachfragen.")
        if (gesamt == null && jeStueck == null && stueck == null) return Ergebnis.Fehlt("Es fehlt die Dosis: Menge mit Einheit und/oder Stückzahl. Bitte bei Frank nachfragen.")
        val anzahl = stueck ?: BigDecimal.ONE
        val jeEines = when {
            jeStueck != null -> jeStueck
            gesamt != null -> teile(gesamt, anzahl)
                ?: return Ergebnis.Fehlt("${schoen(gesamt)} ${anzeige(e)} lassen sich nicht glatt auf ${schoen(anzahl)} Stück aufteilen. Bitte die Menge je Stück erfragen.")
            else -> null
        }
        return Ergebnis.Gut(Dosis(anzahl.normal(), jeEines?.normal(), if (jeEines == null) null else e))
    }

    /** Zahl aus „2“, „1,5“, „0.5“ oder einem Zahlwort („zwei“). */
    fun zahl(text: String): BigDecimal? {
        val t = text.trim().lowercase(Locale.GERMAN)
        ZAHLWOERTER[t]?.let { return BigDecimal(it) }
        return t.replace(',', '.').toBigDecimalOrNull()
    }

    fun einheit(text: String): Einheit? = EINHEITEN[text.trim().lowercase(Locale.GERMAN).removeSuffix(".")]

    fun anzeige(e: Einheit?): String = when (e) {
        Einheit.MG -> "mg"
        Einheit.UG -> "µg"
        Einheit.G -> "g"
        Einheit.ML -> "ml"
        Einheit.IE -> "IE"
        Einheit.STUECK -> "Stück"
        Einheit.TASSE -> "Tasse"
        null -> ""
    }

    /** Gesamtmenge in Milligramm, nur zum Vergleichen (g, mg, µg); andere Einheiten bleiben null. */
    fun inMilligramm(menge: BigDecimal, e: Einheit?): BigDecimal? = when (e) {
        Einheit.G -> menge.multiply(BigDecimal(1000))
        Einheit.MG -> menge
        Einheit.UG -> menge.divide(BigDecimal(1000))
        else -> null
    }

    fun schoen(z: BigDecimal): String = z.stripTrailingZeros().toPlainString().replace('.', ',')

    /** Glatte Teilung (höchstens vier Nachkommastellen), sonst null. */
    private fun teile(gesamt: BigDecimal, anzahl: BigDecimal): BigDecimal? = runCatching {
        gesamt.divide(anzahl).takeIf { it.stripTrailingZeros().scale() <= 4 }
    }.getOrElse { runCatching { gesamt.divide(anzahl, 4, RoundingMode.UNNECESSARY) }.getOrNull() }

    private fun BigDecimal.normal(): BigDecimal = stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }

    private val ZAHLWOERTER = mapOf(
        "ein" to "1", "eine" to "1", "eins" to "1", "einen" to "1", "zwei" to "2", "zwo" to "2", "drei" to "3", "vier" to "4",
        "fünf" to "5", "fuenf" to "5", "sechs" to "6", "sieben" to "7", "acht" to "8", "neun" to "9", "zehn" to "10",
        "halb" to "0.5", "halbe" to "0.5", "eineinhalb" to "1.5", "anderthalb" to "1.5",
    )

    private val EINHEITEN = mapOf(
        "g" to Einheit.G, "gr" to Einheit.G, "gramm" to Einheit.G,
        "mg" to Einheit.MG, "milligramm" to Einheit.MG,
        "µg" to Einheit.UG, "μg" to Einheit.UG, "ug" to Einheit.UG, "mcg" to Einheit.UG, "mikrogramm" to Einheit.UG,
        "ml" to Einheit.ML, "milliliter" to Einheit.ML,
        "ie" to Einheit.IE, "i.e" to Einheit.IE, "iu" to Einheit.IE, "einheiten" to Einheit.IE, "internationale einheiten" to Einheit.IE,
        "stück" to Einheit.STUECK, "stueck" to Einheit.STUECK,
        "tasse" to Einheit.TASSE, "tassen" to Einheit.TASSE,
    )
}
