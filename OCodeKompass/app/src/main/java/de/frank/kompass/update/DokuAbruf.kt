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
 * Bewusst die Markdown-Quelldateien aus dem Projektarchiv statt der gestalteten Webseite
 * opencode.ai: Sie sind maschinell auswertbar, klein und aendern ihre Form seltener. Eine
 * Auswertung des HTML waere bei der ersten Umgestaltung der Seite kaputt.
 *
 * Der Zweig heisst `dev`, nicht `main` — das ist der Standardzweig des Projekts. Ein Abruf von
 * `main` liefert 404, deshalb steht der Name hier ausgeschrieben und nicht als Vermutung.
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
            .header("User-Agent", "OCodeKompass/1.0 (Android)")
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
        private const val BASIS =
            "https://raw.githubusercontent.com/anomalyco/opencode/dev/packages/web/src/content/docs"

        /** Die Seite mit dem Abschnitt „Commands" — dort stehen alle eingebauten Slash-Befehle. */
        const val URL_BEFEHLE = "$BASIS/tui.mdx"

        /**
         * Das offizielle JSON-Schema von `opencode.json` — die Quelle der Einstellungen.
         *
         * Bis Fassung 0.6.8 zeigte diese Angabe auf `config.mdx`, und dorthin wurde sie nie
         * abgerufen: Der Abgleich kannte nur den Befehlsbereich. Beides ist jetzt behoben, und
         * zwar mit der besseren Quelle. `config.mdx` erklärt die Einstellungen an
         * Beispielblöcken; ein Beispiel zeigt aber, was eine Einstellung kann, nicht welche es
         * gibt. Das Schema ist die Datei, gegen die OpenCode selbst prüft — es trägt jeden
         * Schlüssel, die meisten mit Beschreibung, und ist dabei klein.
         */
        const val URL_CONFIG = "https://opencode.ai/config.json"

        /** Die Fassungen des Projekts; zwei Seiten, weil sehr haeufig veroeffentlicht wird. */
        const val URL_RELEASES_1 = "https://api.github.com/repos/anomalyco/opencode/releases?per_page=100&page=1"
        const val URL_RELEASES_2 = "https://api.github.com/repos/anomalyco/opencode/releases?per_page=100&page=2"
    }
}
