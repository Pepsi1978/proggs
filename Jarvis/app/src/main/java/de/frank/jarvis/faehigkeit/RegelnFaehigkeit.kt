package de.frank.jarvis.faehigkeit

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Eine Regel, die Frank für die Zukunft festgelegt hat. */
data class Regel(val id: Int, val text: String, val stand: String)

/**
 * Die Regeldatei von Jarvis: was Frank dauerhaft wichtig ist („in Zukunft immer …“). Jarvis schreibt sie selbst,
 * sobald er im Gesagten eine solche Vorgabe erkennt, und legt sie jedem Modell als Kontext bei: dem Plugin
 * (Anleitung und Werkzeug-Ergebnisse), dem eigenen Chat, den Agenten und der Tagesauswertung.
 * Im Code stehen deshalb keine einzelnen Regeln, nur dieser Speicher.
 */
object Regeln {
    private const val HOECHSTENS = 40
    const val ZEICHEN = 400
    private const val LAENGSTENS = 1500

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "regeln.json")

    @Synchronized
    fun alle(context: Context): List<Regel> = runCatching {
        val liste = JSONObject(datei(context).readText()).getJSONArray("regeln")
        (0 until liste.length()).map { liste.getJSONObject(it).let { r -> Regel(r.getInt("id"), r.getString("text"), r.optString("stand")) } }
    }.getOrDefault(emptyList())

    private fun schreibe(context: Context, regeln: List<Regel>) {
        val inhalt = JSONObject().put("format", 1)
            .put("regeln", JSONArray(regeln.map { JSONObject().put("id", it.id).put("text", it.text).put("stand", it.stand) })).toString(2)
        // Erst daneben schreiben, dann umbenennen: Eine halbe Datei würde alle Regeln kosten.
        val neu = File(datei(context).path + ".neu")
        neu.writeText(inhalt)
        if (!neu.renameTo(datei(context))) { datei(context).writeText(inhalt); neu.delete() }
    }

    /** Legt eine Regel an oder ersetzt mit [id] den Text einer vorhandenen. null = diese id gibt es nicht oder die Datei ist voll. */
    @Synchronized
    fun speichere(context: Context, text: String, id: Int? = null): Regel? {
        val bisher = alle(context)
        // Eingesprochenes darf länger sein als die Vorgabe an die Modelle; die KI-Korrektur strafft es danach.
        val sauber = text.replace(Regex("\\s+"), " ").trim().take(LAENGSTENS)
        val heute = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val regel = if (id != null) {
            if (bisher.none { it.id == id }) return null
            Regel(id, sauber, heute)
        } else {
            if (bisher.size >= HOECHSTENS) return null
            Regel((bisher.maxOfOrNull { it.id } ?: 0) + 1, sauber, heute)
        }
        schreibe(context, bisher.filter { it.id != regel.id } + regel)
        return regel
    }

    @Synchronized
    fun loesche(context: Context, id: Int): Regel? {
        val bisher = alle(context)
        val weg = bisher.firstOrNull { it.id == id } ?: return null
        schreibe(context, bisher - weg)
        return weg
    }

    /** Stellt einen früheren Stand wieder her (Rückgängig in der App). */
    @Synchronized
    fun setze(context: Context, regeln: List<Regel>) = schreibe(context, regeln)

    fun suche(context: Context, wort: String): List<Regel> = wort.trim().lowercase(Locale.GERMAN).let { w -> alle(context).filter { w in it.text.lowercase(Locale.GERMAN) } }

    fun liste(regeln: List<Regel>): String = regeln.sortedBy { it.id }.joinToString("\n") { "[${it.id}] ${it.text}" }

    /** Der Block, der jedem Modell beiliegt. Leer, solange Frank nichts festgelegt hat. */
    fun alsKontext(context: Context): String = alle(context).takeIf { it.isNotEmpty() }?.let {
        "FRANKS REGELN (von Frank selbst festgelegt; sie gelten immer und gehen allgemeinen Vorgaben vor, wo sie ihnen widersprechen; nicht vorlesen, nur einhalten):\n" + liste(it) + "\n"
    }.orEmpty()
}

/** Die Werkzeuge zur Regeldatei: lesen, merken oder ändern, löschen. */
class RegelnFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "regeln"
    override val name = "Franks Regeln"
    override val beschreibung = "Was Frank für die Zukunft festlegt, als Regel merken, lesen, ändern und löschen. Jarvis beachtet die Regeln bei allem."
    override val hinweise =
        "Jarvis lernt dazu: Sagt Frank, wie er etwas künftig haben möchte, ist das eine Regel – auch beiläufig und ohne „merk dir“. Erkennungszeichen: " +
            "„immer“, „nie“, „in Zukunft“, „künftig“, „ab jetzt“, „jedes Mal“, „ich lege Wert darauf“, „mir ist wichtig“, oder er bemängelt, wie eine Antwort aufgebaut war. " +
            "Rufe dann von dir aus regel_speichern auf, ohne nachzufragen, und sag Frank danach, was genau du dir gemerkt hast und wie du es künftig machst, damit er es berichtigen kann.Keine Regel ist ein einmaliger Wunsch nur für diese Antwort. " +
            "Gibt es zum selben Thema schon eine Regel, ändere sie (id), statt eine zweite anzulegen. Die geltenden Regeln stehen unter FRANKS REGELN; halte sie bei jeder Antwort ein."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "regeln_lesen",
            titel = "Regeln lesen",
            beschreibung = "Jarvis: liest die Regeln, die Frank für die Zukunft festgelegt hat (seine Regeldatei). Nutze es für „Welche Regeln hast du dir gemerkt?“, " +
                "„Was weißt du darüber, wie ich etwas haben will?“ und bevor du eine Regel änderst oder löschst.",
            schema = schema(),
            nurLesen = true,
        ) { _ -> Ergebnis(Regeln.alle(context).takeIf { it.isNotEmpty() }?.let { "Franks Regeln:\n" + Regeln.liste(it) } ?: "Frank hat noch keine Regeln festgelegt.") },
        Werkzeug(
            name = "regel_speichern",
            titel = "Regel merken oder ändern",
            beschreibung = "Jarvis: merkt sich dauerhaft, wie Frank etwas künftig haben möchte. Rufe es von dir aus auf, sobald Frank eine Vorgabe für die Zukunft äußert, auch beiläufig: " +
                "„in Zukunft möchte ich …“, „nenn mir bei Schlafwerten immer auch …“, „mach das nie wieder so“, „ich lege Wert darauf, dass …“, „ab jetzt …“. " +
                "Jarvis legt die Regel ab und beachtet sie danach in jedem Gespräch, bei seinen Agenten und in der Tagesauswertung. " +
                "NEU ohne id; ÄNDERN mit id, wenn es zum selben Thema schon eine Regel gibt (dann den vollständigen neuen Text übergeben). Nicht für einmalige Wünsche.",
            schema = schema(
                "text" to text("Die Regel als vollständige, für sich allein verständliche Anweisung an Jarvis in ein bis zwei Sätzen: wann sie gilt und was dann zu tun ist. " +
                    "In Franks Sinn formuliert, Hörfehler der Spracherkennung bereinigt, höchstens ${Regeln.ZEICHEN} Zeichen. " +
                    "Beispiel: „Bei Fragen nach dem Schlaf immer auch HRV, Ruhepuls und Schlafdauer nennen.“"),
                "id" to zahl("Nur beim Ändern: id der vorhandenen Regel aus FRANKS REGELN oder regeln_lesen."),
                pflicht = listOf("text"),
            ),
            nurLesen = false,
        ) { a ->
            val inhalt = a.optString("text").trim()
            if (inhalt.length < 10) return@Werkzeug Ergebnis("Die Regel fehlt oder ist zu kurz, um allein verständlich zu sein.", fehler = true)
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else null
            val regel = Regeln.speichere(context, inhalt, nummer)
                ?: return@Werkzeug Ergebnis(if (nummer != null) "Eine Regel mit der id $nummer gibt es nicht. Lies die Regeln mit regeln_lesen." else "Die Regeldatei ist voll. Fasse ähnliche Regeln zusammen oder lösche überholte, dann noch einmal.", fehler = true)
            Ergebnis((if (nummer != null) "Regel geändert" else "Regel gemerkt") + ": [${regel.id}] ${regel.text}\n" +
                "BESTÄTIGE ES FRANK JETZT: Sag ihm in ein bis zwei Sätzen, was du dir gemerkt hast und wie du es künftig machst, mit dem Inhalt der Regel, nicht nur „gemerkt“ " +
                "(zum Beispiel „Okay, ich habe mir gemerkt: … In Zukunft nenne ich dir das immer so.“). So kann er sofort berichtigen. " +
                "Berichtigt er, rufe regel_speichern mit id ${regel.id} und dem vollständigen neuen Text auf.\nAlle Regeln jetzt:\n" + Regeln.liste(Regeln.alle(context)) +
                "\nWiderspricht eine ältere Regel der neuen oder sagt sie dasselbe, lösche die ältere mit regel_loeschen.")
        },
        Werkzeug(
            name = "regel_loeschen",
            titel = "Regel löschen",
            beschreibung = "Jarvis: löscht eine Regel aus Franks Regeldatei, wenn Frank sie nicht mehr will („vergiss die Regel mit …“, „das gilt nicht mehr“) oder eine neue sie ersetzt hat. Über id ODER Suchwort.",
            schema = schema("id" to zahl("id der Regel aus FRANKS REGELN oder regeln_lesen."), "suche" to text("Statt id: Wort aus dem Text der Regel.")),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else {
                val treffer = if (a.gesetzt("suche")) Regeln.suche(context, a.optString("suche")) else return@Werkzeug Ergebnis("Es fehlt die id oder ein Suchwort für die Regel.", fehler = true)
                when (treffer.size) {
                    0 -> return@Werkzeug Ergebnis("Keine Regel gefunden, die zu „${a.optString("suche")}“ passt.", fehler = true)
                    1 -> treffer.first().id
                    else -> return@Werkzeug Ergebnis("Mehrere Regeln passen. Frage Frank, welche gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + Regeln.liste(treffer), fehler = true)
                }
            }
            Regeln.loesche(context, nummer)?.let { Ergebnis("Gelöscht: „${it.text}“.") } ?: Ergebnis("Eine Regel mit der id $nummer gibt es nicht.", fehler = true)
        },
    )
}
