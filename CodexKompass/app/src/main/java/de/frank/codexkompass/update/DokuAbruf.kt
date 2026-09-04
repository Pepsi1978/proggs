package de.frank.codexkompass.update

import de.frank.codexkompass.network.awaitAntwort
import de.frank.codexkompass.network.beendeSanft
import de.frank.codexkompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class DokuFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/**
 * Holt die offiziellen Unterlagen, gegen die aktualisiert wird.
 *
 * Bewusst die Markdown-Fassungen: Sie sind maschinell auswertbar, klein und ändern sich
 * seltener in ihrer Form als die gestaltete Webseite. Eine Auswertung des HTML wäre bei der
 * ersten Umgestaltung der Seite kaputt.
 */
class DokuAbruf {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun hole(adresse: String): String = withContext(Dispatchers.IO) {
        val anfrage = Request.Builder()
            .url(adresse)
            .header("Accept", "text/plain, text/markdown, */*")
            .header("User-Agent", "CodexKompass/1.0 (Android)")
            .build()
        client.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) {
                throw DokuFehler("Die Unterlage konnte nicht geladen werden (${antwort.code}): $adresse")
            }
            val inhalt = antwort.body?.string().orEmpty()
            if (inhalt.isBlank()) throw DokuFehler("Die geladene Unterlage war leer: $adresse")
            KompassLog.info(
                "DokuAbruf",
                "hole",
                "Unterlage geladen",
                mapOf("adresse" to adresse, "zeichen" to inhalt.length),
            )
            inhalt
        }
    }

    fun beende() = client.beendeSanft("DokuAbruf")

    companion object {
        const val URL_BEFEHLE = "https://learn.chatgpt.com/docs/developer-commands.md?surface=cli"
        const val URL_CHANGELOG = "https://learn.chatgpt.com/docs/changelog"
    }
}
