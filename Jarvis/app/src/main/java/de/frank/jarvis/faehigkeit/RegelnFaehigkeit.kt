package de.frank.jarvis.faehigkeit

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag einer Merkdatei: eine Regel, ein Ziel oder eine Notiz über Frank. */
data class Regel(val id: Int, val text: String, val stand: String)

/**
 * Eine Datei, die Jarvis selbst pflegt und jedem Modell als Kontext beilegt: dem Plugin (Anleitung und
 * Werkzeug-Ergebnisse), dem eigenen Chat, den Agenten und der Tagesauswertung. Es gibt drei davon:
 * [Regeln] (wie Jarvis arbeiten soll), [Ziele] (was Frank erreichen will) und [UeberFrank] (was Jarvis über Frank weiß).
 * Im Code stehen deshalb keine einzelnen Regeln, Ziele oder Angaben über Frank, nur dieser Speicher.
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
        zuletztGeaendert = System.currentTimeMillis()
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
        /** Wann zuletzt eine der Dateien geschrieben wurde; daran erkennt das Plugin, ob ein Programm den neuen Stand schon hat. */
        @Volatile var zuletztGeaendert = 0L
    }
}

/** Die Regeldatei: was Frank dauerhaft vorgibt („in Zukunft immer …“). */
object Regeln : Merkdatei(
    "regeln.json", 40,
    "FRANKS REGELN (von Frank selbst festgelegt; sie gelten immer und gehen allgemeinen Vorgaben vor, wo sie ihnen widersprechen; nicht ungefragt vorlesen, nur einhalten):",
)

/** Die Ziele-Datei: was Frank erreichen will. Eigener Bereich, damit Jarvis Vorschläge und Deutungen daran messen kann. */
object Ziele : Merkdatei(
    "ziele.json", 40,
    "FRANKS ZIELE (was er erreichen will; miss Deutungen und Vorschläge daran, wo es passt; nicht ungefragt aufzählen):",
)

/** Die Datei über Frank: was Jarvis über ihn erfahren hat (Vorlieben, Besitz, Daten, was ihm wichtig ist). */
object UeberFrank : Merkdatei(
    "ueber_frank.json", 120,
    "WAS JARVIS ÜBER FRANK WEISS (aus Franks eigenen Angaben; beziehe es von dir aus ein, wo es passt; nicht ungefragt aufzählen):",
)

/** Die drei Arten von Gemerktem, mit Datei, Wort und dem, was die Modelle dazu wissen müssen. */
enum class Merkart(val kennung: String, val datei: Merkdatei, val wort: String, val textHinweis: String, val bestaetigung: String) {
    REGEL(
        "regel", Regeln, "Regel",
        "eine vollständige, für sich allein verständliche Anweisung an Jarvis in ein bis zwei Sätzen: wann sie gilt und was dann zu tun ist („Bei Fragen nach dem Schlaf immer auch HRV, Ruhepuls und Schlafdauer nennen.“)",
        "Sag ihm in ein bis zwei Sätzen, was du dir gemerkt hast und wie du es künftig machst (zum Beispiel „Okay, ich habe mir gemerkt: … In Zukunft mache ich das so.“).",
    ),
    ZIEL(
        "ziel", Ziele, "Ziel",
        "ein kurzer Satz über das, was Frank erreichen will, mit seinen Zahlen und Fristen, wenn er welche nennt („Frank will seine VO2max steigern.“, „Frank will seinen Ruhepuls senken.“)",
        "Sag ihm in einem Satz, welches Ziel du dir gemerkt hast, und beantworte dann, worum es ihm eigentlich ging.",
    ),
    INFO(
        "info", UeberFrank, "Notiz",
        "ein kurzer, für sich allein verständlicher Satz über Frank in der dritten Person, mit den genauen Namen und Zahlen, die er genannt hat („Frank fährt einen Toyota C-HR.“, „Frank geht bei Regen nicht gern im Wald laufen.“)",
        "Sag ihm in einem kurzen Satz, was du dir über ihn notiert hast, ohne daraus ein Thema zu machen, und beantworte dann, worum es ihm eigentlich ging.",
    );

    companion object {
        fun von(kennung: String): Merkart? = entries.firstOrNull { it.kennung == kennung.trim().lowercase(Locale.GERMAN) }
    }
}

/** Alle drei Dateien als ein Block für den Kontext eines Modells. */
fun merkKontext(context: Context): String = Regeln.alsKontext(context) + Ziele.alsKontext(context) + UeberFrank.alsKontext(context)

/** Das Gedächtnis von Jarvis: drei Dateien, drei Werkzeuge (merken, vergessen, lesen). */
class RegelnFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "regeln"
    override val name = "Jarvis' Gedächtnis"
    override val beschreibung = "Franks Regeln, seine Ziele und was Jarvis über ihn weiß: merken, ändern, löschen. Jarvis pflegt das selbst und hat es bei allem im Blick."
    override val hinweise =
        "Du führst drei Dateien über Frank und pflegst sie selbst mit jarvis_merken und jarvis_vergessen. " +
            "REGEL = wie du arbeiten oder antworten sollst („immer“, „nie“, „in Zukunft“, „ab jetzt“, oder Frank bemängelt eine Antwort). " +
            "ZIEL = was Frank erreichen will („ich will meine VO2max steigern“). " +
            "INFO = eine dauerhafte Tatsache über ihn: Vorlieben, Abneigungen, was ihm wichtig ist, Besitz, persönliche Daten, Gewohnheiten, Familie, Gesundheit. " +
            "Erkennst du so etwas in dem, was Frank sagt, auch nebenbei, merke es dir von selbst und ohne nachzufragen und sag ihm kurz, was du dir gemerkt hast. " +
            "Halte die Dateien wahr: Gehört Neues zu einem vorhandenen Eintrag oder widerspricht es ihm (notiert ist „geht bei Regen nicht laufen“, jetzt sagt er „im Regen laufen ist doch gut, das mache ich öfter“), " +
            "ersetze diesen Eintrag über seine id, statt einen zweiten anzulegen; was gar nicht mehr gilt (Ziel erreicht, Auto verkauft), löschst du. " +
            "Nichts Flüchtiges merken (Stimmung von heute, einmalige Wünsche, einzelne Termine). " +
            "Hängt ein Eintrag an einem Begriff aus einer von Franks Apps, dessen genauen Wert du nicht kennst (er will „eine grüne Erholung bei Whoop“: ab wie viel Prozent ist sie grün?), " +
            "schlag den Wert zuerst mit repo_lesen im Code der App nach und trag ihn mit ein („… also mindestens so viel Prozent“). Findest du ihn nicht, merk dir den Eintrag in Franks Worten. " +
            "Was gilt, steht unter FRANKS REGELN, FRANKS ZIELE und WAS JARVIS ÜBER FRANK WEISS: Regeln hältst du ein, Ziele und Wissen beziehst du von dir aus ein."

    override fun stoerung(): String? = null

    private val arten = Merkart.entries.map { it.kennung }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "jarvis_merken",
            titel = "Merken oder ändern (Regel, Ziel, Info)",
            beschreibung = "Jarvis: merkt sich dauerhaft etwas über Frank oder ändert einen vorhandenen Eintrag. Drei Arten: " +
                "regel = wie Jarvis künftig arbeiten oder antworten soll („nenn mir bei Schlafwerten immer auch …“, „mach das nie wieder so“, „ab jetzt …“); " +
                "ziel = was Frank erreichen will („ich will meine VO2max und HRV steigern und den Ruhepuls senken“); " +
                "info = eine dauerhafte Tatsache über Frank („ich fahre einen …“, „ich bin am … geboren“, „bei Regen gehe ich nicht laufen“, „die Drohne … habe ich mir gekauft“). " +
                "Rufe es von dir aus auf, sobald Frank so etwas sagt, auch nebenbei und ohne „merk dir“. Mehrere Dinge in einem Satz = mehrere Aufrufe. " +
                "Nennt Frank dabei einen Begriff aus einer seiner Apps, dessen genauen Wert du nicht kennst (zum Beispiel „grüne Erholung“), schlag ihn vorher mit repo_lesen im Code der App nach und nimm den Wert in den Text auf. " +
                "PFLEGE: Steht zum selben Thema schon ein Eintrag da oder widerspricht das Neue einem alten, übergib dessen id und den vollständigen neuen Text; der alte wird ersetzt. " +
                "Nicht für Flüchtiges (Stimmung von heute, einmalige Wünsche, einzelne Termine). Jarvis hat alles Gemerkte danach in jedem Gespräch, bei seinen Agenten und in der Tagesauswertung vor Augen.",
            schema = schema(
                "art" to text("regel, ziel oder info.", arten),
                "text" to text("Der Eintrag, in Franks Sinn formuliert, Hörfehler der Spracherkennung bereinigt, höchstens ${Merkdatei.ZEICHEN} Zeichen. " +
                    Merkart.entries.joinToString(" ") { "Bei ${it.kennung}: ${it.textHinweis}." }),
                "id" to zahl("Nur beim Ändern oder Ersetzen: id des vorhandenen Eintrags dieser Art (die Zahl in eckigen Klammern davor)."),
                pflicht = listOf("art", "text"),
            ),
            nurLesen = false,
        ) { a ->
            val art = Merkart.von(a.optString("art")) ?: return@Werkzeug Ergebnis("art fehlt: regel, ziel oder info.", fehler = true)
            val inhalt = a.optString("text").trim()
            if (inhalt.length < 10) return@Werkzeug Ergebnis("Der Text fehlt oder ist zu kurz, um allein verständlich zu sein.", fehler = true)
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else null
            val eintrag = art.datei.speichere(context, inhalt, nummer)
                ?: return@Werkzeug Ergebnis(if (nummer != null) "Einen Eintrag der Art ${art.kennung} mit der id $nummer gibt es nicht. Lies nach mit jarvis_gemerktes_lesen." else "Diese Datei ist voll. Fasse ähnliche Einträge zusammen oder lösche überholte, dann noch einmal.", fehler = true)
            Ergebnis("${art.wort} ${if (nummer != null) "geändert" else "gemerkt"}: [${eintrag.id}] ${eintrag.text}\n" +
                "BESTÄTIGE ES FRANK JETZT: ${art.bestaetigung} So kann er sofort berichtigen. Berichtigt er, rufe jarvis_merken mit art ${art.kennung}, id ${eintrag.id} und dem vollständigen neuen Text auf.\n" +
                "Alle Einträge dieser Art jetzt:\n" + art.datei.liste(art.datei.alle(context)) + "\nSagt ein älterer Eintrag dasselbe oder widerspricht er dem neuen, lösche ihn mit jarvis_vergessen.")
        },
        Werkzeug(
            name = "jarvis_vergessen",
            titel = "Gemerktes löschen (Regel, Ziel, Info)",
            beschreibung = "Jarvis: löscht einen Eintrag, der nicht mehr gilt und nicht durch eine neue Fassung ersetzt wird: eine Regel, die Frank nicht mehr will („das gilt nicht mehr“), " +
                "ein erreichtes oder aufgegebenes Ziel, eine Tatsache, die nicht mehr stimmt („das Auto habe ich verkauft“), oder ein doppelter Eintrag. Über id ODER Suchwort. " +
                "Hat sich etwas nur geändert, nimm stattdessen jarvis_merken mit der id.",
            schema = schema(
                "art" to text("regel, ziel oder info.", arten),
                "id" to zahl("id des Eintrags (die Zahl in eckigen Klammern davor)."),
                "suche" to text("Statt id: Wort aus dem Text des Eintrags."),
                pflicht = listOf("art"),
            ),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            val art = Merkart.von(a.optString("art")) ?: return@Werkzeug Ergebnis("art fehlt: regel, ziel oder info.", fehler = true)
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id") else {
                val treffer = if (a.gesetzt("suche")) art.datei.suche(context, a.optString("suche")) else return@Werkzeug Ergebnis("Es fehlt die id oder ein Suchwort.", fehler = true)
                when (treffer.size) {
                    0 -> return@Werkzeug Ergebnis("Kein Eintrag der Art ${art.kennung} gefunden, der zu „${a.optString("suche")}“ passt.", fehler = true)
                    1 -> treffer.first().id
                    else -> return@Werkzeug Ergebnis("Mehrere Einträge passen. Frage Frank, welcher gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + art.datei.liste(treffer), fehler = true)
                }
            }
            art.datei.loesche(context, nummer)?.let { Ergebnis("${art.wort} gelöscht: „${it.text}“. Sag Frank in einem Satz, was du gestrichen hast.") }
                ?: Ergebnis("Einen Eintrag der Art ${art.kennung} mit der id $nummer gibt es nicht.", fehler = true)
        },
        Werkzeug(
            name = "jarvis_gemerktes_lesen",
            titel = "Gemerktes lesen (Regeln, Ziele, Infos)",
            beschreibung = "Jarvis: liest alles, was Jarvis sich über Frank gemerkt hat: seine Regeln, seine Ziele und die Notizen über ihn, jeweils mit id. " +
                "Nutze es für „Welche Regeln hast du dir gemerkt?“, „Welche Ziele habe ich?“, „Was weißt du über mich?“ und bevor du etwas änderst oder löschst, wenn du die ids nicht vor dir hast.",
            schema = schema(),
            nurLesen = true,
        ) { _ ->
            Ergebnis(Merkart.entries.joinToString("\n\n") { art ->
                art.datei.alle(context).takeIf { it.isNotEmpty() }?.let { "${art.wort} (art ${art.kennung}):\n" + art.datei.liste(it) } ?: "${art.wort} (art ${art.kennung}): noch nichts gemerkt."
            })
        },
    )
}
