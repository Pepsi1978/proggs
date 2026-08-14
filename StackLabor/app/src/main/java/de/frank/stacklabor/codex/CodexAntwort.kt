package de.frank.stacklabor.codex

// PLAN (F-12): Gegenstück zum erzwungenen Antwortformat.
// 1. Die Datenklassen bilden das JSON aus der Spezifikation 1:1 ab (dünn besetzt: nur nicht-neutrale
//    Zellen werden gemeldet, alles Nichtgenannte gilt als neutral).
// 2. parseAntwort schneidet den JSON-Block zwischen erster { und letzter } heraus, damit auch eine
//    Antwort mit Text davor oder danach noch lesbar ist.
// 3. Gelesen wird eintragsweise: eine einzelne unlesbare Zelle wirft nicht die ganze Auswertung weg,
//    sie fehlt einfach — und fehlend heisst laut Spezifikation ohnehin neutral.
// 4. vereinigtMit führt die beiden Läufe zusammen, in die F-12 ab 13 Zielen zerlegt.

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Richtung einer Bewertungszelle. */
@Serializable
enum class Wirkung {
    @SerialName("stuetzt")
    STUETZT,

    @SerialName("stoert")
    STOERT,
}

/** Grund einer gemeldeten Konkurrenz zweier Mittel. */
@Serializable
enum class Konkurrenzart {
    @SerialName("aufnahme")
    AUFNAHME,

    @SerialName("wirkung")
    WIRKUNG,

    @SerialName("zeitpunkt")
    ZEITPUNKT,
}

/** Eine nicht-neutrale Zelle der Bewertungstabelle: Mittel [nem] wirkt auf Ziel [ziel]. */
@Serializable
data class AntwortZelle(
    val nem: String,
    val ziel: String,
    val wirkung: Wirkung,
    val staerke: Int = 1,
    val grund: String = "",
)

/** Zwei Mittel behindern sich. */
@Serializable
data class AntwortKonkurrenz(
    @SerialName("nem_a") val mittelA: String,
    @SerialName("nem_b") val mittelB: String,
    val art: Konkurrenzart,
    val schwere: Int = 1,
    val grund: String = "",
)

/** Antwort auf eine eigene Frage des Stacks. */
@Serializable
data class AntwortAufFrage(
    val frage: String,
    val text: String = "",
)

/** Die vollständige Codex-Antwort einer Auswertung (F-12/F-13). */
@Serializable
data class CodexAntwort(
    val zellen: List<AntwortZelle> = emptyList(),
    val konkurrenzen: List<AntwortKonkurrenz> = emptyList(),
    val antworten: List<AntwortAufFrage> = emptyList(),
    val gesamt: String = "",
    val hinweise: List<String> = emptyList(),
) {
    /** true, wenn ausser dem Fließtext nichts Verwertbares kam — die Ampeln bleiben dann grau. */
    val nurFliesstext: Boolean
        get() = zellen.isEmpty() && konkurrenzen.isEmpty() && antworten.isEmpty()
}

/** Die Antwort war nicht als JSON lesbar. Löst den einen Wiederholversuch aus (F-12). */
class AntwortFormatFehler(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

private val codexJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * Liest die Codex-Antwort.
 *
 * Verträgt Code-Zäune und versehentlichen Text um das JSON herum: gelesen wird der Abschnitt
 * zwischen der ersten `{` und der letzten `}`.
 */
fun parseAntwort(rohtext: String): Result<CodexAntwort> {
    val ausschnitt = schneideJsonAus(rohtext)
        ?: return Result.failure(AntwortFormatFehler("In der Antwort von Codex steckt kein JSON-Block."))
    val wurzel = runCatching { codexJson.parseToJsonElement(ausschnitt) as? JsonObject }.getOrNull()
        ?: return Result.failure(AntwortFormatFehler("Der JSON-Block der Codex-Antwort ließ sich nicht lesen."))

    val antwort = CodexAntwort(
        zellen = eintraege(wurzel, "zellen", AntwortZelle.serializer()).mapNotNull { it.bereinigt() },
        konkurrenzen = eintraege(wurzel, "konkurrenzen", AntwortKonkurrenz.serializer()).mapNotNull { it.bereinigt() },
        antworten = eintraege(wurzel, "antworten", AntwortAufFrage.serializer())
            .filter { it.frage.isNotBlank() }
            .map { it.copy(text = it.text.trim()) },
        gesamt = text(wurzel, "gesamt"),
        hinweise = texte(wurzel, "hinweise"),
    )
    if (antwort.gesamt.isBlank() && antwort.nurFliesstext) {
        return Result.failure(AntwortFormatFehler("Die Codex-Antwort enthielt weder Bewertungen noch einen Fließtext."))
    }
    return Result.success(antwort)
}

/**
 * Führt die Teilantworten der beiden Läufe zusammen, in die F-12 ab 13 aktiven Zielen zerlegt.
 * Doppelte Zellen und doppelte Konkurrenzen (auch mit vertauschten Mitteln) fallen weg.
 */
fun CodexAntwort.vereinigtMit(andere: CodexAntwort): CodexAntwort = CodexAntwort(
    zellen = (zellen + andere.zellen).distinctBy { it.nem to it.ziel },
    konkurrenzen = (konkurrenzen + andere.konkurrenzen).distinctBy { paarSchluessel(it) },
    antworten = (antworten + andere.antworten).distinctBy { it.frage },
    gesamt = listOf(gesamt, andere.gesamt).filter { it.isNotBlank() }.joinToString("\n\n"),
    hinweise = (hinweise + andere.hinweise).map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
)

/** Schneidet den JSON-Block aus einem Rohtext heraus; null, wenn keiner drin steckt. */
internal fun schneideJsonAus(rohtext: String): String? {
    val ohneZaun = rohtext.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val anfang = ohneZaun.indexOf('{')
    val ende = ohneZaun.lastIndexOf('}')
    if (anfang < 0 || ende <= anfang) return null
    return ohneZaun.substring(anfang, ende + 1)
}

private fun paarSchluessel(konkurrenz: AntwortKonkurrenz): String =
    listOf(konkurrenz.mittelA, konkurrenz.mittelB).sorted().joinToString("|")

private fun AntwortZelle.bereinigt(): AntwortZelle? {
    if (nem.isBlank() || ziel.isBlank()) return null
    return copy(
        nem = nem.trim(),
        ziel = ziel.trim(),
        staerke = staerke.coerceIn(1, 3),
        grund = grund.trim(),
    )
}

private fun AntwortKonkurrenz.bereinigt(): AntwortKonkurrenz? {
    if (mittelA.isBlank() || mittelB.isBlank() || mittelA.trim() == mittelB.trim()) return null
    return copy(
        mittelA = mittelA.trim(),
        mittelB = mittelB.trim(),
        schwere = schwere.coerceIn(1, 3),
        grund = grund.trim(),
    )
}

/**
 * Liest ein Feld eintragsweise. Ein unlesbarer Eintrag (etwa ein unbekannter Wirkungswert) wird
 * übersprungen, statt die ganze Antwort zu verwerfen — fehlende Zellen gelten als neutral.
 */
private fun <T> eintraege(wurzel: JsonObject, name: String, serialisierer: KSerializer<T>): List<T> {
    val feld = wurzel[name] as? JsonArray ?: return emptyList()
    return feld.mapNotNull { eintrag ->
        runCatching { codexJson.decodeFromJsonElement(serialisierer, eintrag) }.getOrNull()
    }
}

private fun text(wurzel: JsonObject, name: String): String {
    val wert = wurzel[name] as? JsonPrimitive ?: return ""
    if (!wert.isString) return ""
    return wert.content.trim()
}

private fun texte(wurzel: JsonObject, name: String): List<String> {
    val feld = wurzel[name] as? JsonArray ?: return emptyList()
    return feld.mapNotNull { eintrag ->
        (eintrag as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
    }
}
