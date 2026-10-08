package de.frank.jarvis.agent

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Ablage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Ein Agent: ein Name und eine Rolle, also die Anweisung, wie er an Aufträge herangeht. */
data class AgentenBauplan(val name: String, val rolle: String, val vorgegeben: Boolean = false)

data class AgentenLauf(val agent: String, val auftrag: String, val seit: Long, val schritt: String = "")

/**
 * Die Agenten von Jarvis. Ein Agent ist das eigene Modell von Jarvis mit einer festen Rolle; er arbeitet einen
 * Auftrag selbstständig in mehreren Schritten ab (Werkzeuge der angebundenen Apps, Internet-Suche, Ablage) und
 * legt sein Ergebnis als Datei in die Ablage. Jarvis kann neue Agenten anlegen: Es genügt eine Rolle in Worten.
 *
 * Ein Lauf dauert Minuten und läuft deshalb im Hintergrund über den Dienst, nie innerhalb eines Plugin-Aufrufs.
 */
object Agenten {
    private const val TAG = "JarvisAgenten"
    private val _laeufe = MutableStateFlow<List<AgentenLauf>>(emptyList())
    val laeufe: StateFlow<List<AgentenLauf>> = _laeufe.asStateFlow()

    private val VORGABEN = listOf(
        AgentenBauplan(
            "Recherche",
            "Du bist ein gründlicher Recherche-Agent. Kläre zuerst in Gedanken, welche Teilfragen der Auftrag enthält. Suche dann mit mehreren unterschiedlichen Anfragen im Internet, " +
                "lies die wichtigsten Seiten selbst nach und prüfe Angaben gegeneinander. Schreibe am Ende einen gegliederten Bericht: Kurzfassung in drei bis fünf Sätzen, " +
                "dann die Ergebnisse nach Teilfragen, dann eine klare Empfehlung, dann offene Punkte und die Quellen mit Adresse. Trenne Belegtes von Vermutetem.",
            vorgegeben = true,
        ),
        AgentenBauplan(
            "Machbarkeit",
            "Du prüfst, wie sich eine Idee umsetzen lässt. Lies zuerst die Idee (falls sie in Geniale Ideen steht) und kläre Ziel und Nutzen. Recherchiere dann, welche Wege es gibt, " +
                "was sie kosten, was schon existiert und wo die Risiken liegen. Schreibe einen Bericht: Kurzfassung, Umsetzungswege im Vergleich, empfohlener Weg mit den ersten drei Schritten, " +
                "Aufwand grob geschätzt, Risiken, Quellen.",
            vorgegeben = true,
        ),
    )

    private fun ordner(context: Context) = File(context.filesDir, "agenten").apply { mkdirs() }
    private fun datei(context: Context, name: String) = File(ordner(context), name.lowercase(Locale.GERMAN).replace(Regex("[^a-z0-9äöüß]+"), "-").trim('-') + ".json")

    fun alle(context: Context): List<AgentenBauplan> {
        val eigene = ordner(context).listFiles { f -> f.name.endsWith(".json") }?.mapNotNull { f ->
            runCatching { JSONObject(f.readText()).let { AgentenBauplan(it.getString("name"), it.getString("rolle")) } }.getOrNull()
        }.orEmpty()
        // Ein eigener Agent mit dem Namen einer Vorgabe ersetzt sie.
        return VORGABEN.filter { v -> eigene.none { it.name.equals(v.name, ignoreCase = true) } } + eigene.sortedBy { it.name.lowercase(Locale.GERMAN) }
    }

    fun finde(context: Context, name: String): AgentenBauplan? {
        val gesucht = name.trim().lowercase(Locale.GERMAN)
        val liste = alle(context)
        return liste.firstOrNull { it.name.lowercase(Locale.GERMAN) == gesucht } ?: liste.filter { gesucht in it.name.lowercase(Locale.GERMAN) }.singleOrNull()
    }

    fun speichere(context: Context, name: String, rolle: String): AgentenBauplan {
        val plan = AgentenBauplan(name.trim().take(40), rolle.trim())
        datei(context, plan.name).writeText(JSONObject().put("name", plan.name).put("rolle", plan.rolle).toString())
        return plan
    }

    /** Löscht einen selbst angelegten Agenten. Vorgaben bleiben. */
    fun loesche(context: Context, name: String): Boolean = datei(context, name).let { it.exists() && it.delete() }

    /** Führt einen Auftrag aus und legt das Ergebnis in die Ablage. Rückgabe: Titel der Datei oder null. */
    suspend fun fuehreAus(context: Context, plan: AgentenBauplan, auftrag: String): String? {
        val app = context.applicationContext
        val lauf = AgentenLauf(plan.name, auftrag, System.currentTimeMillis())
        _laeufe.value = _laeufe.value + lauf
        // Der Titel steht vorab fest: Dateien, die der Agent unterwegs erzeugt (Bilder, PDFs, Downloads), landen im selben Eintrag wie sein Bericht.
        val titel = "${plan.name} – ${auftrag.replace(Regex("\\s+"), " ").take(50).trim()} – ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M. HH.mm"))}"
        try {
            val rolle = "DEINE ROLLE ALS AGENT „${plan.name}“: ${plan.rolle}\n" +
                "Arbeite den Auftrag selbstständig ab, ohne Rückfragen. Dein letzter Schritt ist die Antwort mit dem vollständigen Bericht als Text; " +
                "der Bericht wird als Datei gespeichert und kann länger sein. Schreibe ihn in gutem Deutsch mit Überschriften.\n" +
                "Dein Ablage-Eintrag heißt „$titel“. Dateien (bild_erzeugen, ablage_datei_speichern, ablage_schreiben mit als_pdf) legst du mit genau diesem Titel ab, " +
                "dann gehören sie zu deinem Bericht. Nenne im Bericht nur Dateien, deren Speicherung ein Werkzeug-Ergebnis bestätigt hat."
            val bericht = JarvisAgent(app).versuche(
                auftrag = auftrag, zeitlimitMs = 12 * 60_000L, maxSchritte = 14, rolle = rolle, mitInternet = true, ablageTitel = titel,
                beiSchritt = { schritt -> _laeufe.value = _laeufe.value.map { if (it === lauf || it.seit == lauf.seit) it.copy(schritt = schritt) else it } },
            )
            if (bericht.isNullOrBlank()) {
                Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Kein Ergebnis für: ${auftrag.take(120)}", ok = false)
                return null
            }
            val datei = Ablage.schreibe(app, titel, "# ${plan.name}: ${auftrag.trim()}\n\nErstellt am ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm 'Uhr'"))} von Jarvis.\n\n$bericht")
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Fertig, abgelegt als „${datei.nameWithoutExtension}“.")
            return datei.nameWithoutExtension
        } catch (e: Exception) {
            Log.e(TAG, "Agent fehlgeschlagen", e)
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}", ok = false)
            return null
        } finally {
            _laeufe.value = _laeufe.value.filter { it.seit != lauf.seit }
        }
    }
}
