package de.frank.stacklabor.werftstudio.domain

import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class PruefsummenKomponente(
    val id: String,
    val name: String,
    val menge: BigDecimal?,
    val einheit: Einheit?,
)

data class PruefsummenMittel(
    val id: String,
    val loeslichkeit: Loeslichkeit,
    val darreichungsform: Darreichungsform,
    val komponenten: List<PruefsummenKomponente>,
)

data class PruefsummenDosis(
    val variante: DosisVariante,
    val stueckzahl: BigDecimal,
    val mengeJeStueck: BigDecimal?,
    val einheit: Einheit?,
)

data class PruefsummenEintrag(
    val id: String,
    val mittel: PruefsummenMittel,
    val frequenzTyp: FrequenzTyp,
    val alleNTage: Int?,
    val gruppeId: String?,
    val zusatztext: String,
    val dosen: List<PruefsummenDosis>,
    val alternationsPartnerMittelIds: List<String>,
)

data class PruefsummenFrage(
    val id: String,
    val text: String,
)

data class StackPruefsummenInhalt(
    val zeitpunkt: String,
    val einnahmeHinweis: String,
    val dosisVariante: DosisVariante,
    val eintraege: List<PruefsummenEintrag>,
    val aktiveZielIds: List<String>,
    val fragen: List<PruefsummenFrage>,
)

object Pruefsumme {
    fun berechnen(inhalt: StackPruefsummenInhalt): String {
        val kanonisch = buildString {
            token("stacklabor-pruefsumme-v1")
            token(inhalt.zeitpunkt)
            token(inhalt.einnahmeHinweis)
            val hatZweiteVariante = inhalt.eintraege.any { eintrag ->
                eintrag.dosen.any { it.variante == DosisVariante.DIENST }
            }
            token(if (hatZweiteVariante) inhalt.dosisVariante.code.toString() else "-")

            inhalt.eintraege.sortedBy { it.id }.forEach { eintrag ->
                token(eintrag.id)
                token(eintrag.mittel.id)
                token(eintrag.mittel.loeslichkeit.code.toString())
                token(eintrag.mittel.darreichungsform.code.toString())
                token(eintrag.frequenzTyp.code.toString())
                token(eintrag.alleNTage?.toString().orEmpty())
                token(eintrag.gruppeId.orEmpty())
                token(eintrag.zusatztext)
                val frei = eintrag.dosen.singleOrNull { it.variante == DosisVariante.FREI }
                val gewaehlt = eintrag.dosen.singleOrNull { it.variante == inhalt.dosisVariante } ?: frei
                checkNotNull(gewaehlt) { "Eintrag ${eintrag.id} hat keine freie Dosis." }
                token(decimal(gewaehlt.stueckzahl))
                token(gewaehlt.mengeJeStueck?.let(::decimal).orEmpty())
                token(gewaehlt.einheit?.code?.toString().orEmpty())
                eintrag.alternationsPartnerMittelIds.sorted().forEach { token(it) }
                eintrag.mittel.komponenten.sortedBy { it.id }.forEach { komponente ->
                    token(komponente.id)
                    token(komponente.name)
                    token(komponente.menge?.let(::decimal).orEmpty())
                    token(komponente.einheit?.code?.toString().orEmpty())
                }
            }
            inhalt.aktiveZielIds.sorted().forEach { token(it) }
            inhalt.fragen.sortedBy { it.id }.forEach { frage ->
                token(frage.id)
                token(frage.text)
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(kanonisch.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    private fun StringBuilder.token(value: String) {
        append(value.toByteArray(StandardCharsets.UTF_8).size)
        append(':')
        append(value)
        append('|')
    }

    private fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
}
