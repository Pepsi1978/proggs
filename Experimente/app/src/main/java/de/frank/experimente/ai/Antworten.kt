package de.frank.experimente.ai

import de.frank.experimente.data.local.Stufe
import org.json.JSONArray
import org.json.JSONObject

/** Ein Vorschlag, wie die KI ihn geliefert hat — noch ohne Datenbank-Kennung. */
data class RohVorschlag(
    val titel: String,
    val beschreibung: String,
    val tage: Int,
    val stufe: Stufe,
    val aufgabenJeTag: List<List<String>>,
    val vonMerkliste: Boolean,
)

class AntwortException(message: String) : Exception(message)

object Antworten {

    private fun json(roh: String): JSONObject {
        val text = roh.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { JSONObject(text) }.getOrElse {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }
    }

    private fun stufe(wert: String): Stufe = when (wert.trim().lowercase()) {
        "leicht" -> Stufe.LEICHT
        "fordernd" -> Stufe.FORDERND
        else -> Stufe.MITTEL
    }

    private fun textliste(array: JSONArray?): List<String> =
        (0 until (array?.length() ?: 0))
            .mapNotNull { array?.optString(it)?.trim()?.takeIf(String::isNotEmpty) }

    fun vorschlaege(roh: String): List<RohVorschlag> {
        val liste = json(roh).optJSONArray("vorschlaege")
            ?: throw AntwortException("Die Antwort war unbrauchbar.")
        val ergebnis = (0 until liste.length()).mapNotNull { i ->
            val v = liste.optJSONObject(i) ?: return@mapNotNull null
            val titel = v.optString("titel").trim()
            if (titel.isEmpty()) return@mapNotNull null

            val proTag = v.optJSONArray("aufgaben_je_tag")
            val aufgaben = (0 until (proTag?.length() ?: 0))
                .map { textliste(proTag?.optJSONArray(it)) }
                .filter { it.isNotEmpty() }

            RohVorschlag(
                titel = titel,
                beschreibung = v.optString("beschreibung").trim(),
                // Die Tageszahl richtet sich nach dem, was wirklich an Aufgaben da ist —
                // sonst zeigt die App „Tag 2 von 3“ für einen Tag ohne Schritte.
                tage = aufgaben.size.coerceAtLeast(1),
                stufe = stufe(v.optString("stufe")),
                aufgabenJeTag = aufgaben.ifEmpty { listOf(listOf(titel)) },
                vonMerkliste = v.optBoolean("von_merkliste", false),
            )
        }
        if (ergebnis.isEmpty()) throw AntwortException("Die Antwort war unbrauchbar.")
        return ergebnis
    }

    fun gespraech(roh: String): String =
        json(roh).optString("antwort").trim().ifEmpty {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }

    fun auswertung(roh: String): String =
        json(roh).optString("auswertung").trim().ifEmpty {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }

    fun logbuch(roh: String): String =
        json(roh).optString("eintrag").trim().ifEmpty {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }

    fun verdichtung(roh: String): String =
        json(roh).optString("verdichtet").trim().ifEmpty {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }

    fun erkenntnisse(roh: String): List<String> =
        textliste(json(roh).optJSONArray("erkenntnisse"))

    fun eigeneIdee(roh: String): RohVorschlag {
        val v = json(roh)
        val titel = v.optString("titel").trim().ifEmpty {
            throw AntwortException("Die Antwort war unbrauchbar.")
        }
        val proTag = v.optJSONArray("aufgaben_je_tag")
        val aufgaben = (0 until (proTag?.length() ?: 0))
            .map { textliste(proTag?.optJSONArray(it)) }
            .filter { it.isNotEmpty() }
        return RohVorschlag(
            titel = titel,
            beschreibung = v.optString("beschreibung").trim(),
            tage = aufgaben.size.coerceAtLeast(1),
            stufe = stufe(v.optString("stufe")),
            aufgabenJeTag = aufgaben.ifEmpty { listOf(listOf(titel)) },
            vonMerkliste = false,
        )
    }

    /** Aufgaben je Tag ↔ JSON, für die Spalte `aufgabenJson`. */
    fun aufgabenZuJson(aufgabenJeTag: List<List<String>>): String {
        val aussen = JSONArray()
        aufgabenJeTag.forEach { tag -> aussen.put(JSONArray(tag)) }
        return aussen.toString()
    }

    fun aufgabenAusJson(roh: String): List<List<String>> {
        val aussen = runCatching { JSONArray(roh) }.getOrNull() ?: return emptyList()
        return (0 until aussen.length()).map { textliste(aussen.optJSONArray(it)) }
    }
}
