package de.frank.newskompass.data

import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.observability.KompassLog
import java.io.File
import java.time.YearMonth
import org.json.JSONObject

/**
 * Gespeicherter Rückblick eines abgeschlossenen Monats — nur eine Ableitung aus den
 * Originalausgaben, nie ihr Ersatz. Passt der Fingerabdruck der Quellausgaben nicht mehr, hat
 * sich das Verfahren geändert oder ist die Datei beschädigt, entsteht er neu aus den Originalen.
 */
object RueckblickCache {

    const val FORMAT = 1

    /** Hochzählen, sobald Auswahl oder Zusammenfassung im Rückblick anders rechnen. */
    const val VERFAHREN = 1

    enum class Zustand { FEHLT, VERALTET, BESCHAEDIGT }

    /** Liefert den gespeicherten Rückblick oder den Grund, warum er nicht taugt. */
    fun lies(ordner: File, monat: YearMonth, fingerabdruck: String): Pair<Ausgabe?, Zustand?> {
        val datei = datei(ordner, monat)
        if (!datei.exists()) return null to Zustand.FEHLT
        return try {
            val j = JSONObject(datei.readText())
            if (j.getInt("format") != FORMAT || j.getInt("verfahren") != VERFAHREN || j.getString("fingerabdruck") != fingerabdruck) {
                null to Zustand.VERALTET
            } else {
                val inhalt = j.getString("inhalt")
                if (SicheresSchreiben.sha256(inhalt) != j.getString("pruefsumme")) {
                    null to Zustand.BESCHAEDIGT
                } else {
                    AusgabenSpeicher.ausJson(JSONObject(inhalt)) to null
                }
            }
        } catch (fehler: Exception) {
            KompassLog.warn("RueckblickCache", "lies", "Rückblick unlesbar", mapOf("monat" to monat.toString(), "grund" to fehler.javaClass.simpleName))
            null to Zustand.BESCHAEDIGT
        }
    }

    fun schreibe(ordner: File, monat: YearMonth, fingerabdruck: String, rueckblick: Ausgabe) {
        ordner.mkdirs()
        val inhalt = AusgabenSpeicher.zuJson(rueckblick).toString()
        val json = JSONObject()
            .put("format", FORMAT)
            .put("verfahren", VERFAHREN)
            .put("monat", monat.toString())
            .put("fingerabdruck", fingerabdruck)
            .put("erzeugtUm", System.currentTimeMillis())
            .put("pruefsumme", SicheresSchreiben.sha256(inhalt))
            .put("inhalt", inhalt)
        SicheresSchreiben.schreibe(datei(ordner, monat), json.toString()) { text ->
            val j = JSONObject(text)
            val gelesen = j.getString("inhalt")
            require(SicheresSchreiben.sha256(gelesen) == j.getString("pruefsumme")) { "Prüfsumme weicht ab" }
            AusgabenSpeicher.ausJson(JSONObject(gelesen))
        }
    }

    /** Fingerabdruck der Quellausgaben: welche Ausgaben mit welchem Dateistand eingeflossen sind. */
    fun fingerabdruck(quellen: List<AusgabenEintrag>): String =
        SicheresSchreiben.sha256(quellen.sortedBy { it.id }.joinToString("\n") { "${it.id}:${it.stand}" })

    private fun datei(ordner: File, monat: YearMonth) = File(ordner, "$monat.json")
}
