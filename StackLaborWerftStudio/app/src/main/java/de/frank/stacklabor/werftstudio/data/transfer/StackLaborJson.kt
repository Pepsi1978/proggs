package de.frank.stacklabor.werftstudio.data.transfer

import java.math.BigDecimal
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class StackLaborJson(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
        prettyPrint = true
    },
) {
    fun kodieren(dokument: TransferDokument): String {
        validiere(dokument)
        return json.encodeToString(dokument)
    }

    fun dekodieren(text: String): TransferDokument {
        val root = try {
            json.decodeFromString<JsonObject>(text)
        } catch (exception: SerializationException) {
            throw ImportFormatException("Die Datei ist kein gueltiges JSON.", exception)
        }
        val version = root["schema_version"]?.jsonPrimitive?.intOrNull
            ?: throw ImportFormatException("schema_version fehlt.")
        if (version > AKTUELLE_SCHEMA_VERSION) {
            throw NeuereSchemaVersionException(version, AKTUELLE_SCHEMA_VERSION)
        }
        val migriert = migriere(root, version)
        val dokument = try {
            json.decodeFromString<TransferDokument>(migriert.toString())
        } catch (exception: SerializationException) {
            throw ImportFormatException("Das StackLabor-Format ist unvollstaendig.", exception)
        }
        validiere(dokument)
        return dokument
    }

    fun validiere(dokument: TransferDokument) {
        requireImport(dokument.schemaVersion == AKTUELLE_SCHEMA_VERSION, "Ungueltige schema_version.")
        requireImport(dokument.format in ERLAUBTE_FORMATE, "Unbekanntes StackLabor-Format: ${dokument.format}")
        requireUnique(dokument.mittel.map { it.id }, "mittel.id")
        requireUnique(dokument.wirkstoffkomponenten.map { it.id }, "wirkstoffkomponente.id")
        requireUnique(dokument.stacks.map { it.id }, "stack.id")
        requireUnique(dokument.eintraege.map { it.id }, "stack_eintrag.id")
        requireUnique(dokument.ziele.map { it.id }, "ziel.id")
        requireUnique(dokument.ziele.map { it.text }, "ziel.text")
        requireUnique(dokument.fragen.map { it.id }, "eigene_frage.id")
        requireUnique(dokument.bewertungen.map { it.id }, "bewertung.id")

        val mittelIds = dokument.mittel.mapTo(mutableSetOf()) { it.id }
        val stackIds = dokument.stacks.mapTo(mutableSetOf()) { it.id }
        val eintragIds = dokument.eintraege.mapTo(mutableSetOf()) { it.id }
        val zielIds = dokument.ziele.mapTo(mutableSetOf()) { it.id }
        val frageIds = dokument.fragen.mapTo(mutableSetOf()) { it.id }
        val bewertungIds = dokument.bewertungen.mapTo(mutableSetOf()) { it.id }

        dokument.mittel.forEach {
            requireImport(it.id.isNotBlank() && it.name.isNotBlank(), "Mittel brauchen ID und Name.")
            requireImport(it.name.length <= 80, "Mittelname ist laenger als 80 Zeichen: ${it.id}")
            requireImport(it.loeslichkeit in LOESLICHKEITEN, "Ungueltige Loeslichkeit: ${it.loeslichkeit}")
            requireImport(it.darreichungsform in DARREICHUNGSFORMEN, "Ungueltige Darreichungsform: ${it.darreichungsform}")
            requireImport(it.darreichungsformText == null || it.darreichungsformText.isNotBlank() && it.darreichungsformText.length <= 60, "Ungueltiger Darreichungsformtext: ${it.id}")
        }
        dokument.stacks.forEach {
            requireImport(it.id.isNotBlank() && it.name.isNotBlank(), "Stacks brauchen ID und Name.")
            requireImport(it.name.length <= 60, "Stackname ist laenger als 60 Zeichen: ${it.id}")
            requireImport(it.zeitpunkt.length <= 120 && it.einnahmeHinweis.length <= 120, "Stacktexte sind zu lang: ${it.id}")
        }
        dokument.wirkstoffkomponenten.forEach {
            requireImport(it.mittelId in mittelIds, "Komponente verweist auf unbekanntes Mittel: ${it.id}")
            it.menge?.let { menge -> requireImport(decimal(menge) > BigDecimal.ZERO, "Komponentenmenge muss positiv sein: ${it.id}") }
            requireImport((it.menge == null) == (it.einheit == null), "Komponentenmenge und Einheit muessen gemeinsam gesetzt sein: ${it.id}")
            requireImport(it.einheit == null || it.einheit in EINHEITEN, "Ungueltige Komponenten-Einheit: ${it.einheit}")
        }
        dokument.eintraege.forEach {
            requireImport(it.stackId in stackIds, "Eintrag verweist auf unbekannten Stack: ${it.id}")
            requireImport(it.mittelId in mittelIds, "Eintrag verweist auf unbekanntes Mittel: ${it.id}")
            requireImport(it.reihenfolge >= 1, "Eintragsposition muss mindestens 1 sein: ${it.id}")
            requireImport(it.zusatztext.length <= 300, "Zusatztext ist zu lang: ${it.id}")
            requireImport(it.frequenzTyp in FREQUENZTYPEN, "Ungueltiger Frequenztyp: ${it.frequenzTyp}")
            requireImport(it.frequenzText == null || it.frequenzText.isNotBlank() && it.frequenzText.length <= 60, "Ungueltiger Frequenztext: ${it.id}")
            if (it.frequenzTyp == "ALLE_N_TAGE") {
                requireImport((it.alleNTage ?: 0) >= 2, "alle_n_tage muss mindestens 2 sein: ${it.id}")
            }
        }
        requireUnique(dokument.eintraege.map { it.stackId to it.mittelId }, "stack_eintrag(stack_id,mittel_id)")
        dokument.dosen.forEach {
            requireImport(it.stackEintragId in eintragIds, "Dosis verweist auf unbekannten Eintrag: ${it.stackEintragId}")
            requireImport(decimal(it.stueckzahl) > BigDecimal.ZERO, "Stueckzahl muss positiv sein: ${it.stackEintragId}")
            it.mengeJeStueck?.let { menge -> requireImport(decimal(menge) > BigDecimal.ZERO, "Dosis muss positiv sein: ${it.stackEintragId}") }
            val hatEinheit = !it.einheitText.isNullOrBlank() || it.einheit != null
            requireImport(it.mengeJeStueck == null || hatEinheit, "Zu einer Menge gehoert eine Einheit: ${it.stackEintragId}")
            requireImport(it.einheitText == null || it.einheitText.isNotBlank() && it.einheitText.length <= 30, "Ungueltiger Einheitentext: ${it.stackEintragId}")
            requireImport(it.variante in DOSIS_VARIANTEN, "Ungueltige Dosisvariante: ${it.variante}")
            requireImport(it.einheit == null || it.einheit in EINHEITEN, "Ungueltige Einheit: ${it.einheit}")
        }
        requireUnique(dokument.dosen.map { it.stackEintragId to it.variante }, "stack_eintrag_dosis")
        eintragIds.forEach { id ->
            requireImport(dokument.dosen.any { it.stackEintragId == id && it.variante == "FREI" }, "Freie Dosis fehlt: $id")
        }
        dokument.alternationsPartner.forEach {
            requireImport(it.stackEintragId in eintragIds, "Alternationspartner verweist auf unbekannten Eintrag.")
            requireImport(it.partnerMittelId in mittelIds, "Alternationspartner verweist auf unbekanntes Mittel.")
        }
        requireUnique(dokument.alternationsPartner.map { it.stackEintragId to it.partnerMittelId }, "alternations_partner")
        dokument.stackZiele.forEach {
            requireImport(it.stackId in stackIds && it.zielId in zielIds, "Stack-Ziel verweist auf unbekannte Daten.")
            requireImport(it.rang >= 1, "Zielrang muss mindestens 1 sein.")
        }
        requireUnique(dokument.stackZiele.map { it.stackId to it.zielId }, "stack_ziel")
        dokument.fragen.forEach {
            requireImport(it.stackId in stackIds, "Frage verweist auf unbekannten Stack: ${it.id}")
            requireImport(it.text.isNotBlank() && it.text.length <= 300, "Ungueltiger Fragetext: ${it.id}")
        }
        dokument.bewertungen.forEach {
            requireImport(it.stackId == null || it.stackId in stackIds, "Bewertung verweist auf unbekannten Stack: ${it.id}")
            requireImport(it.bereich in BEREICHE, "Ungueltiger Bewertungsbereich: ${it.bereich}")
            requireImport((it.bereich == "STACK") == (it.stackId != null), "Bewertungsbereich und stack_id passen nicht: ${it.id}")
        }
        dokument.bewertungszellen.forEach {
            requireImport(it.bewertungId in bewertungIds && it.mittelId in mittelIds && it.zielId in zielIds, "Bewertungszelle verweist auf unbekannte Daten.")
            requireImport(it.wirkung in WIRKUNGEN, "Ungueltige Wirkung: ${it.wirkung}")
            requireImport(it.staerke in 1..3, "Bewertungsstaerke muss 1 bis 3 sein.")
            requireImport(it.grund.length <= 140, "Bewertungsgrund ist laenger als 140 Zeichen.")
        }
        requireUnique(dokument.bewertungszellen.map { Triple(it.bewertungId, it.mittelId, it.zielId) }, "bewertungszelle")
        dokument.konkurrenzen.forEach {
            requireImport(it.bewertungId in bewertungIds && it.mittelAId in mittelIds && it.mittelBId in mittelIds, "Konkurrenz verweist auf unbekannte Daten.")
            requireImport(it.art in KONKURRENZ_ARTEN, "Ungueltige Konkurrenzart: ${it.art}")
            requireImport(it.mittelAId != it.mittelBId && it.schwere in 1..3, "Ungueltige Konkurrenz.")
        }
        dokument.antworten.forEach {
            requireImport(it.bewertungId in bewertungIds && it.frageId in frageIds, "Antwort verweist auf unbekannte Daten.")
        }
        requireUnique(
            dokument.konkurrenzen.map { listOf(it.bewertungId, it.mittelAId, it.mittelBId, it.art) },
            "konkurrenz",
        )
        requireUnique(dokument.antworten.map { it.bewertungId to it.frageId }, "frage_antwort")
        dokument.sortieransichten.forEach {
            requireImport(it.stackId in stackIds, "Sortieransicht verweist auf unbekannten Stack.")
            requireImport(it.ansicht in SORTIERANSICHTEN, "Ungueltige Sortieransicht: ${it.ansicht}")
        }
        requireUnique(dokument.sortieransichten.map { it.stackId }, "stack_sortieransicht")
    }

    private fun migriere(root: JsonObject, version: Int): JsonObject {
        var current = root
        var currentVersion = version
        while (currentVersion < AKTUELLE_SCHEMA_VERSION) {
            current = when (currentVersion) {
                0 -> migriereV0NachV1(current)
                1 -> migriereV1NachV2(current)
                2 -> migriereV2NachV3(current)
                else -> throw ImportFormatException("Kein Migrationspfad ab schema_version $currentVersion.")
            }
            currentVersion++
        }
        return current
    }

    private fun migriereV0NachV1(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply {
            put("schema_version", JsonPrimitive(1))
            putIfAbsent("wirkstoffkomponenten", kotlinx.serialization.json.JsonArray(emptyList()))
            putIfAbsent("dosen", kotlinx.serialization.json.JsonArray(emptyList()))
            putIfAbsent("alternations_partner", kotlinx.serialization.json.JsonArray(emptyList()))
            putIfAbsent("sortieransichten", kotlinx.serialization.json.JsonArray(emptyList()))
        },
    )

    private fun migriereV1NachV2(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply { put("schema_version", JsonPrimitive(2)) },
    )

    private fun migriereV2NachV3(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply { put("schema_version", JsonPrimitive(3)) },
    )

    private fun requireUnique(values: List<Any>, feld: String) {
        requireImport(values.size == values.toSet().size, "Doppelte Werte in $feld.")
    }

    private fun requireImport(condition: Boolean, message: String) {
        if (!condition) throw ImportFormatException(message)
    }

    private fun decimal(value: String): BigDecimal = try {
        BigDecimal(value)
    } catch (exception: NumberFormatException) {
        throw ImportFormatException("Ungueltige Dezimalzahl: $value", exception)
    }

    companion object {
        const val AKTUELLE_SCHEMA_VERSION: Int = 3
        const val EXPORT_FORMAT: String = "stacklabor_export"
        const val SEED_FORMAT: String = "stacklabor_startbestand"
        private val ERLAUBTE_FORMATE = setOf(EXPORT_FORMAT, SEED_FORMAT)
        private val LOESLICHKEITEN = setOf("WASSER", "BEIDES", "FETT")
        private val DARREICHUNGSFORMEN = setOf("KAPSEL", "TABLETTE", "LOEFFEL", "TASSE", "PULVER", "TROPFEN", "SONSTIGE")
        private val EINHEITEN = setOf("MG", "UG", "G", "ML", "IE", "STUECK", "TASSE")
        private val DOSIS_VARIANTEN = setOf("FREI", "DIENST")
        private val FREQUENZTYPEN = setOf("TAEGLICH", "ALLE_N_TAGE")
        private val BEREICHE = setOf("STACK", "TAG")
        private val WIRKUNGEN = setOf("STUETZT", "STOERT")
        private val KONKURRENZ_ARTEN = setOf("AUFNAHME", "WIRKUNG", "ZEITPUNKT")
        private val SORTIERANSICHTEN = setOf("LOESLICHKEIT", "EINNAHME")
    }
}

open class ImportFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class NeuereSchemaVersionException(
    val dateiVersion: Int,
    val appVersion: Int,
) : ImportFormatException("Die Datei hat schema_version $dateiVersion, diese App versteht nur bis $appVersion.")
