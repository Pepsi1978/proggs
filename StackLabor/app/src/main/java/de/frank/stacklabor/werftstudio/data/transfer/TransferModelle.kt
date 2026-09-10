package de.frank.stacklabor.werftstudio.data.transfer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferDokument(
    @SerialName("format") val format: String,
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("exportiert_am") val exportiertAmEpochMillis: Long? = null,
    val mittel: List<MittelTransfer> = emptyList(),
    val wirkstoffkomponenten: List<WirkstoffkomponenteTransfer> = emptyList(),
    val stacks: List<StackTransfer> = emptyList(),
    val eintraege: List<StackEintragTransfer> = emptyList(),
    val dosen: List<DosisTransfer> = emptyList(),
    @SerialName("alternations_partner") val alternationsPartner: List<AlternationsPartnerTransfer> = emptyList(),
    val ziele: List<ZielTransfer> = emptyList(),
    @SerialName("stack_ziele") val stackZiele: List<StackZielTransfer> = emptyList(),
    val fragen: List<FrageTransfer> = emptyList(),
    val bewertungen: List<BewertungTransfer> = emptyList(),
    val bewertungszellen: List<BewertungszelleTransfer> = emptyList(),
    val konkurrenzen: List<KonkurrenzTransfer> = emptyList(),
    val antworten: List<AntwortTransfer> = emptyList(),
    val sortieransichten: List<SortieransichtTransfer> = emptyList(),
)

@Serializable
data class MittelTransfer(
    val id: String,
    val name: String,
    val loeslichkeit: String,
    val darreichungsform: String,
    val hersteller: String = "",
    val durchfallrisiko: Boolean = false,
    val beistoffe: String = "",
    @SerialName("darreichungsform_text") val darreichungsformText: String? = null,
    @SerialName("loeslichkeit_ki_ermittelt") val loeslichkeitKiErmittelt: Boolean = false,
)

@Serializable
data class WirkstoffkomponenteTransfer(
    val id: String,
    @SerialName("mittel_id") val mittelId: String,
    val name: String,
    val menge: String? = null,
    val einheit: String? = null,
)

@Serializable
data class StackTransfer(
    val id: String,
    val name: String,
    val zeitpunkt: String = "",
    @SerialName("einnahme_hinweis") val einnahmeHinweis: String = "",
    val sortierung: Int,
    val gesperrt: Boolean = false,
)

@Serializable
data class StackEintragTransfer(
    val id: String,
    @SerialName("stack_id") val stackId: String,
    @SerialName("mittel_id") val mittelId: String,
    @SerialName("frequenz_typ") val frequenzTyp: String = "TAEGLICH",
    @SerialName("alle_n_tage") val alleNTage: Int? = null,
    val aktiv: Boolean = true,
    val reihenfolge: Int,
    @SerialName("gruppe_id") val gruppeId: String? = null,
    val zusatztext: String = "",
    @SerialName("offener_hinweis") val offenerHinweis: String? = null,
    @SerialName("frequenz_text") val frequenzText: String? = null,
)

@Serializable
data class DosisTransfer(
    @SerialName("stack_eintrag_id") val stackEintragId: String,
    val variante: String,
    val stueckzahl: String,
    @SerialName("menge_je_stueck") val mengeJeStueck: String? = null,
    val einheit: String? = null,
    @SerialName("einheit_text") val einheitText: String? = null,
)

@Serializable
data class AlternationsPartnerTransfer(
    @SerialName("stack_eintrag_id") val stackEintragId: String,
    @SerialName("partner_mittel_id") val partnerMittelId: String,
)

@Serializable
data class ZielTransfer(val id: String, val text: String)

@Serializable
data class StackZielTransfer(
    @SerialName("stack_id") val stackId: String,
    @SerialName("ziel_id") val zielId: String,
    val rang: Int,
)

@Serializable
data class FrageTransfer(
    val id: String,
    @SerialName("stack_id") val stackId: String,
    val text: String,
)

@Serializable
data class BewertungTransfer(
    val id: String,
    val bereich: String,
    @SerialName("stack_id") val stackId: String? = null,
    @SerialName("zeitpunkt_epoch_millis") val zeitpunktEpochMillis: Long,
    val modell: String,
    val denkstufe: String,
    val pruefsumme: String,
    val gesamt: String = "",
    val hinweise: List<String> = emptyList(),
)

@Serializable
data class BewertungszelleTransfer(
    @SerialName("bewertung_id") val bewertungId: String,
    @SerialName("mittel_id") val mittelId: String,
    @SerialName("ziel_id") val zielId: String,
    val wirkung: String,
    val staerke: Int,
    val grund: String,
)

@Serializable
data class KonkurrenzTransfer(
    @SerialName("bewertung_id") val bewertungId: String,
    @SerialName("mittel_a_id") val mittelAId: String,
    @SerialName("mittel_b_id") val mittelBId: String,
    val art: String,
    val schwere: Int,
    val grund: String,
)

@Serializable
data class AntwortTransfer(
    @SerialName("bewertung_id") val bewertungId: String,
    @SerialName("frage_id") val frageId: String,
    val text: String,
)

@Serializable
data class SortieransichtTransfer(
    @SerialName("stack_id") val stackId: String,
    val ansicht: String,
    @SerialName("manuelle_reihenfolge") val manuelleReihenfolge: Boolean = true,
)
