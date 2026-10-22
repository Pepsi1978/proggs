package de.frank.genialeideen.bridge

import android.content.Context
import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.text.UmlautKorrektur
import de.frank.genialeideen.ui.IdeenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Glättet Text und Titel einer über Jarvis angelegten Idee im Hintergrund, wie „Korrektur“ und der
 * Titelvorschlag im Erfassen-Bildschirm.
 *
 * Der Auftrag steht vorher in einer Warteschlange auf der Platte: Beendet Android die App, bevor die KI
 * geantwortet hat, holt der nächste Start ([nachholen]) die Glättung nach, statt sie zu verlieren.
 */
object JarvisGlaettung {
    private const val DATEI = "jarvis_glaettung"
    private const val SCHLUESSEL = "auftraege"
    private const val MAX_VERSUCHE = 3

    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val laufend = mutableSetOf<Long>()

    fun starte(c: AppContainer, context: Context, id: Long, roh: String, titelFehlt: Boolean) {
        if (!c.codexAuthManager.kannDenken(de.frank.genialeideen.auth.CodexModel.fromLabel(c.settings.model))) return
        aendere(context) { it.put(JSONObject().put("id", id).put("roh", roh).put("titelFehlt", titelFehlt).put("versuche", 0)) }
        fuehreAus(c, context, id, roh, titelFehlt)
    }

    /** Beim App-Start: liegen gebliebene Glättungen erneut anstoßen. */
    fun nachholen(c: AppContainer, context: Context) {
        if (!c.codexAuthManager.kannDenken(de.frank.genialeideen.auth.CodexModel.fromLabel(c.settings.model))) return
        val offen = lies(context)
        for (n in 0 until offen.length()) {
            val a = offen.getJSONObject(n)
            fuehreAus(c, context, a.getLong("id"), a.getString("roh"), a.optBoolean("titelFehlt"))
        }
    }

    private fun fuehreAus(c: AppContainer, context: Context, id: Long, roh: String, titelFehlt: Boolean) {
        synchronized(laufend) { if (!laufend.add(id)) return }
        nebenbei.launch {
            val geschafft = runCatching { glaette(c, id, roh, titelFehlt) }.isSuccess
            synchronized(laufend) { laufend.remove(id) }
            aendere(context) { liste ->
                val neu = JSONArray()
                for (n in 0 until liste.length()) {
                    val a = liste.getJSONObject(n)
                    if (a.getLong("id") != id) { neu.put(a); continue }
                    val versuche = a.optInt("versuche") + 1
                    // Ein Netzfehler darf es beim nächsten Start noch einmal versuchen, aber nicht ewig.
                    if (!geschafft && versuche < MAX_VERSUCHE) neu.put(a.put("versuche", versuche))
                }
                neu
            }
        }
    }

    private suspend fun glaette(c: AppContainer, id: Long, roh: String, titelFehlt: Boolean) {
        val modell = CodexModel.fromLabel(c.settings.model)
        val stufe = ReasoningEffort.fromLabel(c.settings.reasoning)
        val sauber = UmlautKorrektur.korrigiere(
            c.codexAuthManager.streamChat(IdeenViewModel.KORREKTUR, listOf(ChatTurn("user", roh)), modell, stufe).trim(),
        )
        var titel: String? = null
        if (titelFehlt) {
            titel = dreiWoerter(UmlautKorrektur.korrigiere(
                c.codexAuthManager.streamChat(IdeenViewModel.TITEL, listOf(ChatTurn("user", roh.take(4000))), modell, stufe),
            )).takeIf { it.isNotBlank() }
        }
        val repo = c.ideenRepository
        val idee = repo.lade(id) ?: return
        // Nur überschreiben, wenn der Text seit dem Anlegen unverändert ist — sonst gewinnt die Bearbeitung von Hand.
        if (idee.text == roh && sauber.isNotBlank()) {
            c.database.ideenDao().aktualisieren(idee.copy(text = sauber, titel = titel ?: idee.titel, originalText = roh, geaendertAm = System.currentTimeMillis()))
        } else if (titel != null && idee.titel == ersatzTitel(roh)) {
            repo.aendere(idee, titel, idee.text)
        }
    }

    fun dreiWoerter(roh: String): String = roh.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim()
        .trim('"', '\'', '„', '“', '”', '«', '»').split(Regex("\\s+")).filter(String::isNotBlank).take(3).joinToString(" ")
        .trimEnd('.', ',', ';', ':', '!').trim()

    fun ersatzTitel(text: String): String = dreiWoerter(text).ifBlank { "Neue Idee" }

    private fun lies(context: Context): JSONArray =
        runCatching { JSONArray(context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).getString(SCHLUESSEL, "[]")) }.getOrElse { JSONArray() }

    private fun aendere(context: Context, aenderung: (JSONArray) -> JSONArray) = synchronized(this) {
        val neu = aenderung(lies(context))
        context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, neu.toString()).commit()
    }
}
