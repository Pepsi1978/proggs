package de.frank.jarvis.faehigkeit

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag einer Merkdatei: eine Regel oder eine Notiz über Frank. */
data class Regel(val id: Int, val text: String, val stand: String)

/**
 * Eine Datei, die Jarvis selbst pflegt und jedem Modell als Kontext beilegt: dem Plugin (Anleitung und
 * Werkzeug-Ergebnisse), dem eigenen Chat, den Agenten und der Tagesauswertung. Es gibt zwei davon:
 * [Regeln] (wie Jarvis arbeiten soll) und [UeberFrank] (was Jarvis über Frank weiß).
 * Im Code stehen deshalb keine einzelnen Regeln oder Angaben über Frank, nur dieser Speicher.
 */
open class Merkdatei(private val dateiname: String, private val hoechstens: Int, private val kopf: String) {
    private fun datei(context: Context) = File(context.applicationContext.filesDir, dateiname)

    @Synchronized
    fun alle(context: Context): List<Regel> = runCatching {
        val liste = JSONObject(datei(context).readText()).getJSONArray("regeln")
        (0 until liste.length()).map { liste.getJSONObject(it).let { r -> Regel(r.getInt("id"), r.getString("text"), r.optString("stand")) } }
    }.getOrDefault(emptyList())

    private fun schreibe(context: Context, eintraege: List<Regel>) {
        val inhalt = JSONObject().put("format", 1)
            .put("regeln", JSONArray(eintraege.map { JSONObject().put("id", it.id).put("text", it.text).put("stand", it.stand) })).toString(2)
        // Erst daneben schreiben, dann umbenennen: Eine halbe Datei würde alle Einträge kosten.
        val neu = File(datei(context).path + ".neu")
        neu.writeText(inhalt)
        if (!neu.renameTo(datei(context))) { datei(context).writeText(inhalt); neu.delete() }
    }

    /** Legt einen Eintrag an oder ersetzt mit [id] den Text eines vorhandenen. null = diese id gibt es nicht oder die Datei ist voll. */
    @Synchronized
    fun speichere(context: Context, text: String, id: Int? = null): Regel? {
        val bisher = alle(context)
        // Eingesprochenes darf länger sein als die Vorgabe an die Modelle; die KI-Korrektur strafft es danach.
        val sauber = text.replace(Regex("\\s+"), " ").trim().take(LAENGSTENS)
        val heute = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val eintrag = if (id != null) {
            if (bisher.none { it.id == id }) return null
            Regel(id, sauber, heute)
        } else {
            if (bisher.size >= hoechstens) return null
            Regel((bisher.maxOfOrNull { it.id } ?: 0) + 1, sauber, heute)
        }
        schreibe(context, bisher.filter { it.id != eintrag.id } + eintrag)
        return eintrag
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
    fun setze(context: Context, eintraege: List<Regel>) = schreibe(context, eintraege)

    fun suche(context: Context, wort: String): List<Regel> = wort.trim().lowercase(Locale.GERMAN).let { w -> alle(context).filter { w in it.text.lowercase(Locale.GERMAN) } }

    fun liste(eintraege: List<Regel>): String = eintraege.sortedBy { it.id }.joinToString("\n") { "[${it.id}] ${it.text}" }

    /** Der Block, der jedem Modell beiliegt. Leer, solange nichts drinsteht. */
    fun alsKontext(context: Context): String = alle(context).takeIf { it.isNotEmpty() }?.let { kopf + "\n" + liste(it) + "\n" }.orEmpty()

    companion object {
        /** Länge, die die Modelle für einen Eintrag anstreben sollen. */
        const val ZEICHEN = 400
        private const val LAENGSTENS = 1500
    }
}

/** Die Regeldatei: was Frank dauerhaft vorgibt („in Zukunft immer …“). */
object Regeln : Merkdatei(
    "regeln.json", 40,
    "FRANKS REGELN (von Frank selbst festgelegt; sie gelten immer und gehen allgemeinen Vorgaben vor, wo sie ihnen widersprechen; nicht vorlesen, nur einhalten):",
)

/** Die Datei über Frank: was Jarvis über ihn erfahren hat (Vorlieben, Besitz, Daten, was ihm wichtig ist). */
object UeberFrank : Merkdatei(
    "ueber_frank.json", 120,
    "WAS JARVIS ÜBER FRANK WEISS (aus Franks eigenen Angaben; beziehe es von dir aus ein, wo es passt; nicht aufzählen und nicht vorlesen):",
)

/** Beide Dateien als ein Block für den Kontext eines Modells. */
fun merkKontext(context: Context): String = Regeln.alsKontext(context) + UeberFrank.alsKontext(context)

/** Die Werkzeuge zu beiden Dateien: lesen, Regel merken oder löschen, Notiz über Frank merken oder löschen. */
class RegelnFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "regeln"
    override val name = "Franks Regeln und Wissen über Frank"
    override val beschreibung = "Was Frank für die Zukunft festlegt, als Regel merken und was Jarvis über Frank erfährt, als Notiz. Jarvis hat beides bei allem im Blick."
    override val hinweise =
        "Jarvis lernt dazu und führt zwei Dateien. " +
            "REGEL = wie du arbeiten oder antworten sollst: Sagt Frank, wie er etwas künftig haben möchte („immer“, „nie“, „in Zukunft“, „ab jetzt“, „ich lege Wert darauf“, " +
            "oder er bemängelt, wie eine Antwort aufgebaut war), rufe regel_speichern auf. " +
            "INFO ÜBER FRANK = eine dauerhafte Tatsache über ihn selbst: Vorlieben und Abneigungen („bei Regen gehe ich nicht laufen“), was ihm wichtig ist, was er besitzt (Auto, Drohne, Geräte), " +
            "persönliche Daten (Geburtstag), Gewohnheiten, Familie, Gesundheit. Erwähnt er so etwas, auch nebenbei, rufe frank_info_speichern auf. " +
            "Beides von dir aus und ohne nachzufragen, auch wenn er nicht „merk dir“ sagt. Sag ihm danach kurz, was du dir gemerkt hast, damit er es berichtigen kann. " +
            "Nichts Flüchtiges speichern (Stimmung von heute, einmalige Wünsche, einzelne Termine) und nichts, was schon dasteht; hat sich etwas geändert, ändere den vorhandenen Eintrag (id). " +
            "Was gilt, steht unter FRANKS REGELN und WAS JARVIS ÜBER FRANK WEISS: Halte die Regeln ein und beziehe das Wissen über Frank von dir aus in Antworten und Empfehlungen ein."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "regeln_lesen",
            titel = "Regeln und Wissen über Frank lesen",
            beschreibung = "Jarvis: liest beide Dateien, die Jarvis über Frank führt: die Regeln, die Frank für die Zukunft festgelegt hat, und die Notizen darüber, was Jarvis über Frank weiß. " +
                "Nutze es für „Welche Regeln hast du dir gemerkt?“, „Was weißt du über mich?“ und bevor du einen Eintrag änderst oder löschst.",
            schema = schema(),
            nurLesen = true,
        ) { _ ->
            Ergebnis(
                (Regeln.alle(context).takeIf { it.isNotEmpty() }?.let { "Franks Regeln:\n" + Regeln.liste(it) } ?: "Frank hat noch keine Regeln festgelegt.") + "\n\n" +
                    (UeberFrank.alle(context).takeIf { it.isNotEmpty() }?.let { "Was Jarvis über Frank weiß:\n" + UeberFrank.liste(it) } ?: "Über Frank ist noch nichts notiert."),
            )
        },
        speichern(
            Regeln, "regel_speichern", "Regel merken oder ändern", "Regel",
            "Jarvis: merkt sich dauerhaft, wie Frank etwas künftig haben möchte. Rufe es von dir aus auf, sobald Frank eine Vorgabe für die Zukunft äußert, auch beiläufig: " +
                "„in Zukunft möchte ich …“, „nenn mir bei Schlafwerten immer auch …“, „mach das nie wieder so“, „ich lege Wert darauf, dass …“, „ab jetzt …“. " +
                "Jarvis legt die Regel ab und beachtet sie danach in jedem Gespräch, bei seinen Agenten und in der Tagesauswertung. " +
                "NEU ohne id; ÄNDERN mit id, wenn es zum selben Thema schon eine Regel gibt (dann den vollständigen neuen Text übergeben). Nicht für einmalige Wünsche und nicht für Tatsachen über Frank (dafür frank_info_speichern).",
            "Die Regel als vollständige, für sich allein verständliche Anweisung an Jarvis in ein bis zwei Sätzen: wann sie gilt und was dann zu tun ist. " +
                "In Franks Sinn formuliert, Hörfehler der Spracherkennung bereinigt, höchstens ${Merkdatei.ZEICHEN} Zeichen. " +
                "Beispiel: „Bei Fragen nach dem Schlaf immer auch HRV, Ruhepuls und Schlafdauer nennen.“",
            "Sag ihm in ein bis zwei Sätzen, was du dir gemerkt hast und wie du es künftig machst, mit dem Inhalt der Regel, nicht nur „gemerkt“ " +
                "(zum Beispiel „Okay, ich habe mir gemerkt: … In Zukunft nenne ich dir das immer so.“).",
        ),
        loeschen(Regeln, "regel_loeschen", "Regel löschen", "Regel", "Jarvis: löscht eine Regel aus Franks Regeldatei, wenn Frank sie nicht mehr will („vergiss die Regel mit …“, „das gilt nicht mehr“) oder eine neue sie ersetzt hat. Über id ODER Suchwort."),
        speichern(
            UeberFrank, "frank_info_speichern", "Notiz über Frank merken oder ändern", "Notiz",
            "Jarvis: notiert dauerhaft etwas, das Jarvis über Frank erfahren hat. Rufe es von dir aus auf, sobald Frank etwas Bleibendes über sich selbst sagt, auch nebenbei: " +
                "eine Vorliebe oder Abneigung („es regnet, heute gehe ich nicht laufen“ → „Frank geht bei Regen nicht gern laufen.“), was ihm wichtig ist („HRV, VO2max und Ruhepuls sind meine wichtigsten Werte“), " +
                "was er besitzt („ich habe mir die Drohne … gekauft“, „ich sitze in meinem Toyota …“), persönliche Daten („ich bin am … geboren“), Gewohnheiten, Familie, Gesundheit. " +
                "Jarvis hat die Notizen danach in jedem Gespräch, bei seinen Agenten und in der Tagesauswertung vor Augen. " +
                "NEU ohne id; ÄNDERN mit id, wenn es dazu schon eine Notiz gibt und sich etwas geändert hat. Nichts Flüchtiges (Stimmung von heute, einzelne Termine) und keine Vorgaben, wie Jarvis arbeiten soll (dafür regel_speichern).",
            "Die Notiz als ein kurzer, für sich allein verständlicher Satz über Frank in der dritten Person, mit den genauen Namen und Zahlen, die er genannt hat. " +
                "Beispiele: „Frank fährt einen Toyota C-HR.“, „Frank ist am 3. August 1978 geboren.“, „Frank geht bei Regen nicht gern im Wald laufen.“",
            "Sag ihm in einem kurzen Satz, was du dir über ihn notiert hast (zum Beispiel „Ich habe mir notiert: …“), ohne daraus ein Thema zu machen, und beantworte dann, worum es ihm eigentlich ging.",
        ),
        loeschen(UeberFrank, "frank_info_loeschen", "Notiz über Frank löschen", "Notiz", "Jarvis: löscht eine Notiz über Frank, wenn sie nicht mehr stimmt („das Auto habe ich verkauft“, „vergiss das“) und nicht durch eine neue Fassung ersetzt wird. Über id ODER Suchwort."),
    )

    private fun speichern(datei: Merkdatei, name: String, titel: String, wort: String, beschreibung: String, textHinweis: String, bestaetigung: String) = Werkzeug(
        name = name,
        titel = titel,
        beschreibung = beschreibung,
        schema = schema(
            "text" to text(textHinweis),
            "id" to zahl("Nur beim Ändern: id des vorhandenen Eintrags (steht in eckigen Klammern davor)."),
            pflicht = listOf("text"),
        ),
        nurLesen = false,
    ) { a ->
        val inhalt = a.optString("text").trim()
        if (inhalt.length < 10) return@Werkzeug Ergebnis("Der Text fehlt oder ist zu kurz, um allein verständlich zu sein.", fehler = true)
        val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else null
        val eintrag = datei.speichere(context, inhalt, nummer)
            ?: return@Werkzeug Ergebnis(if (nummer != null) "Einen Eintrag mit der id $nummer gibt es dort nicht. Lies nach mit regeln_lesen." else "Die Datei ist voll. Fasse ähnliche Einträge zusammen oder lösche überholte, dann noch einmal.", fehler = true)
        Ergebnis("$wort ${if (nummer != null) "geändert" else "gemerkt"}: [${eintrag.id}] ${eintrag.text}\n" +
            "BESTÄTIGE ES FRANK JETZT: $bestaetigung So kann er sofort berichtigen. Berichtigt er, rufe $name mit id ${eintrag.id} und dem vollständigen neuen Text auf.\n" +
            "Alle Einträge jetzt:\n" + datei.liste(datei.alle(context)) + "\nSagt ein älterer Eintrag dasselbe oder widerspricht er dem neuen, lösche den älteren.")
    }

    private fun loeschen(datei: Merkdatei, name: String, titel: String, wort: String, beschreibung: String) = Werkzeug(
        name = name,
        titel = titel,
        beschreibung = beschreibung,
        schema = schema("id" to zahl("id des Eintrags (steht in eckigen Klammern davor)."), "suche" to text("Statt id: Wort aus dem Text des Eintrags.")),
        nurLesen = false,
        loeschend = true,
    ) { a ->
        val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else {
            val treffer = if (a.gesetzt("suche")) datei.suche(context, a.optString("suche")) else return@Werkzeug Ergebnis("Es fehlt die id oder ein Suchwort.", fehler = true)
            when (treffer.size) {
                0 -> return@Werkzeug Ergebnis("Kein Eintrag gefunden, der zu „${a.optString("suche")}“ passt.", fehler = true)
                1 -> treffer.first().id
                else -> return@Werkzeug Ergebnis("Mehrere Einträge passen. Frage Frank, welcher gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + datei.liste(treffer), fehler = true)
            }
        }
        datei.loesche(context, nummer)?.let { Ergebnis("$wort gelöscht: „${it.text}“.") } ?: Ergebnis("Einen Eintrag mit der id $nummer gibt es dort nicht.", fehler = true)
    }
}
