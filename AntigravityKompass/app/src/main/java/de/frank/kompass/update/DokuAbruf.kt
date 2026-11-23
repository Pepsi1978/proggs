package de.frank.kompass.update

import de.frank.kompass.network.awaitAntwort
import de.frank.kompass.network.beendeSanft
import de.frank.kompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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
            .header("User-Agent", "AntigravityKompass/1.0 (Android)")
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
        private const val DOKU = "https://antigravity.google/docs"
        /** Eine Seite für Befehle UND settings.json-Schlüssel (getrennte Tabellen). */
        const val URL_BEFEHLE = "$DOKU/cli/reference.md"
        const val URL_EINSTELLUNGEN = "$DOKU/cli/reference.md"
        /** Variablen stehen verstreut; die Settings-Seite ist der beste Fundort, ihr Ausfall ist verzichtbar. */
        const val URL_VARIABLEN = "$DOKU/settings.md"

        /**
         * Das Änderungsprotokoll kommt aus dem Quellverzeichnis statt von der Doku-Seite.
         * Dort steht es vollständig und unverkürzt — auf der Webseite wird es ab einer
         * gewissen Länge abgeschnitten, und genau die alten Einträge wären für die Frage
         * „seit wann gibt es das?" die wichtigen.
         */
        const val URL_CHANGELOG =
            "$DOKU/changelog.md"
    }
}
