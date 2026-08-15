package de.frank.stacklabor.werftstudio.domain.model

import java.math.BigDecimal

enum class Loeslichkeit(val code: Int) {
    WASSER(0),
    BEIDES(1),
    FETT(2),
}

enum class Darreichungsform(val code: Int) {
    KAPSEL(0),
    TABLETTE(1),
    LOEFFEL(2),
    TASSE(3),
    PULVER(4),
    TROPFEN(5),
    SONSTIGE(6),
}

enum class Einheit(val code: Int) {
    MG(0),
    UG(1),
    G(2),
    ML(3),
    IE(4),
    STUECK(5),
    TASSE(6),
}

enum class DosisVariante(val code: Int) {
    FREI(0),
    DIENST(1),
}

enum class FrequenzTyp(val code: Int) {
    TAEGLICH(0),
    ALLE_N_TAGE(1),
}

enum class BewertungsBereich(val code: Int) {
    STACK(0),
    TAG(1),
}

enum class Wirkung(val code: Int) {
    STUETZT(0),
    STOERT(1),
}

enum class KonkurrenzArt(val code: Int) {
    AUFNAHME(0),
    WIRKUNG(1),
    ZEITPUNKT(2),
}

enum class Sortieransicht(val code: Int) {
    LOESLICHKEIT(0),
    EINNAHME(1),
}

enum class Ampel {
    GRUEN,
    GELB,
    ROT,
    GRAU,
}

data class Wirkstoffkomponente(
    val id: String,
    val mittelId: String,
    val name: String,
    val menge: BigDecimal?,
    val einheit: Einheit?,
)

data class Mittel(
    val id: String,
    val name: String,
    val loeslichkeit: Loeslichkeit = Loeslichkeit.WASSER,
    val darreichungsform: Darreichungsform = Darreichungsform.KAPSEL,
    val hersteller: String = "",
    val durchfallrisiko: Boolean = false,
    val beistoffe: String = "",
    val wirkstoffkomponenten: List<Wirkstoffkomponente> = emptyList(),
    val darreichungsformText: String? = null,
)

data class Stack(
    val id: String,
    val name: String,
    val zeitpunkt: String = "",
    val einnahmeHinweis: String = "",
    val sortierung: Int,
)

data class StackEintragDosis(
    val stackEintragId: String,
    val variante: DosisVariante,
    val stueckzahl: BigDecimal,
    val mengeJeStueck: BigDecimal?,
    val einheit: Einheit?,
    val einheitText: String? = null,
)

data class StackEintrag(
    val id: String,
    val stackId: String,
    val mittelId: String,
    val frequenzTyp: FrequenzTyp = FrequenzTyp.TAEGLICH,
    val alleNTage: Int? = null,
    val aktiv: Boolean = true,
    val reihenfolge: Int,
    val gruppeId: String? = null,
    val zusatztext: String = "",
    val offenerHinweis: String? = null,
    val dosen: List<StackEintragDosis>,
    val alternationsPartnerMittelIds: List<String> = emptyList(),
    val frequenzText: String? = null,
)

data class Ziel(
    val id: String,
    val text: String,
)

data class StackZiel(
    val stackId: String,
    val zielId: String,
    val rang: Int,
)

data class EigeneFrage(
    val id: String,
    val stackId: String,
    val text: String,
)

data class Bewertung(
    val id: String,
    val bereich: BewertungsBereich,
    val stackId: String?,
    val zeitpunktEpochMillis: Long,
    val modell: String,
    val denkstufe: String,
    val pruefsumme: String,
    val gesamt: String = "",
    val hinweise: List<String> = emptyList(),
)

data class Bewertungszelle(
    val bewertungId: String,
    val mittelId: String,
    val zielId: String,
    val wirkung: Wirkung,
    val staerke: Int,
    val grund: String,
)

data class Konkurrenz(
    val bewertungId: String,
    val mittelAId: String,
    val mittelBId: String,
    val art: KonkurrenzArt,
    val schwere: Int,
    val grund: String,
)

data class FrageAntwort(
    val bewertungId: String,
    val frageId: String,
    val text: String,
)

data class BewertungMitDetails(
    val bewertung: Bewertung,
    val zellen: List<Bewertungszelle>,
    val konkurrenzen: List<Konkurrenz>,
    val antworten: List<FrageAntwort>,
)

data class StackInhalt(
    val stack: Stack,
    val mittel: List<Mittel>,
    val eintraege: List<StackEintrag>,
    val ziele: List<StackZiel>,
    val fragen: List<EigeneFrage>,
)
