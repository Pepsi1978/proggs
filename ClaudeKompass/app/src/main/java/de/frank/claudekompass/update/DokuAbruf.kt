package de.frank.claudekompass.update

import de.frank.claudekompass.network.awaitAntwort
import de.frank.claudekompass.network.beendeSanft
import de.frank.claudekompass.observability.KompassLog
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
            .header("User-Agent", "ClaudeKompass/1.0 (Android)")
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
        const val URL_BEFEHLE = "https://code.claude.com/docs/en/commands.md"
        const val URL_EINSTELLUNGEN = "https://code.claude.com/docs/en/settings-reference.md"
        const val URL_VARIABLEN = "https://code.claude.com/docs/en/env-vars.md"

        /**
         * Das Änderungsprotokoll kommt aus dem Quellverzeichnis statt von der Doku-Seite.
         * Dort steht es vollständig und unverkürzt — auf der Webseite wird es ab einer
         * gewissen Länge abgeschnitten, und genau die alten Einträge wären für die Frage
         * „seit wann gibt es das?" die wichtigen.
         */
        const val URL_CHANGELOG =
            "https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md"
    }
}
