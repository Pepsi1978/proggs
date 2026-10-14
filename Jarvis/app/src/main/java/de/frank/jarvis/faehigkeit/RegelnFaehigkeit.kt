package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.AtomicFile
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag einer Merkdatei: eine Regel, ein Ziel oder eine Notiz über Frank. */
data class Regel(val id: Int, val text: String, val stand: String)

/**
 * Eine Datei, die Jarvis selbst pflegt und jedem Modell als Kontext beilegt: dem Plugin (Anleitung und
 * Werkzeug-Ergebnisse), dem eigenen Chat, den Agenten und der Tagesauswertung. Es gibt drei davon:
 * [Regeln] (wie Jarvis arbeiten soll), [Ziele] (was Frank erreichen will) und [UeberFrank] (was Jarvis über Frank weiß).
 * Im Code stehen deshalb keine einzelnen Regeln, Ziele oder Angaben über Frank, nur dieser Speicher.
 *
 * - ids werden nie wiederverwendet (Zähler `naechsteId` in der Datei): Eine veraltete id trifft nie einen fremden Eintrag.
 * - Geschrieben wird atomar über [AtomicFile] (mit fsync und Sicherung).
 * - Ist die Datei vorhanden, aber unlesbar, wird sie als `<name>.defekt-<zeit>` gesichert, weggeräumt, und Jarvis beginnt
 *   leer neu (Hinweis in [alsKontext] und [neuBegonnen]). Nur wenn das Sichern scheitert, bleibt die Datei gesperrt;
 *   sonst würde das nächste Merken alle Einträge unwiederbringlich überschreiben.
 */
open class Merkdatei(private val dateiname: String, private val hoechstens: Int, private val kopf: String) {
    private fun datei(context: Context) = File(context.applicationContext.filesDir, dateiname)

    /** Inhalt der Datei. [defekt] = vorhanden, aber nicht lesbar (Grund), dann wird nichts geschrieben. */
    private class Stand(val eintraege: List<Regel>, val naechsteId: Int, val defekt: String? = null)

    /** Zuletzt gelesener oder geschriebener Stand samt Änderungszeit und Größe der Datei. */
    @Volatile private var zwischen: Triple<Long, Long, Stand>? = null
    /** Änderungszeit der defekten Datei, die schon gesichert ist (nicht bei jedem Lesen erneut kopieren). */
    @Volatile private var gesichert = 0L

    /**
     * Gesetzt, wenn eine beschädigte Datei gesichert und leer neu begonnen wurde (Text für Frank).
     * Steht im Kontext der Modelle, bis wieder etwas geschrieben wurde; die Oberfläche kann ihn anzeigen.
     */
    @Volatile var neuBegonnen: String? = null
        private set

    @Synchronized
    private fun lies(context: Context): Stand {
        val f = datei(context)
        zwischen?.let { (zeit, laenge, s) -> if (f.lastModified() == zeit && f.length() == laenge) return s }
        val text = try {
            String(AtomicFile(f).readFully(), Charsets.UTF_8)
        } catch (_: FileNotFoundException) {
            return Stand(emptyList(), 1)
        } catch (e: Exception) {
            return defekt(f, e)
        }
        val stand = if (text.isBlank()) {
            // Leer: Es gibt nichts zu retten, also nicht sperren, aber festhalten.
            Logbuch.gedaechtnis("Merkdatei leer", dateiname)
            Stand(emptyList(), 1)
        } else try {
            val json = JSONObject(text)
            val liste = json.getJSONArray("regeln")
            // Ein einzelner kaputter Eintrag kostet nur sich selbst, nicht die ganze Datei.
            val eintraege = (0 until liste.length()).mapNotNull { i ->
                liste.optJSONObject(i)?.let { r -> Regel(r.optInt("id", 0), r.optString("text").trim(), r.optString("stand")) }?.takeIf { it.id > 0 && it.text.isNotEmpty() }
            }
            Stand(eintraege, maxOf(json.optInt("naechsteId", 0), (eintraege.maxOfOrNull { it.id } ?: 0) + 1))
        } catch (e: Exception) {
            return defekt(f, e)
        }
        zwischen = Triple(f.lastModified(), f.length(), stand)
        return stand
    }

    private fun defekt(f: File, e: Exception): Stand {
        val kopie = File(f.path + ".defekt-" + f.lastModified())
        // Gesichert (oder schon da): die kaputte Datei wegräumen und leer neu beginnen; die Daten liegen in der Kopie.
        val sicher = kopie.exists() || runCatching { f.copyTo(kopie); true }.getOrElse { Logbuch.fehler("Merkdatei sichern", it, dateiname); false }
        if (sicher && runCatching { AtomicFile(f).delete() }.isSuccess && !f.exists()) {
            zwischen = null
            gesichert = 0L
            neuBegonnen = "$dateiname war beschädigt, gesichert als ${kopie.name}; neu begonnen."
            Logbuch.fehler("Merkdatei defekt", e, "$dateiname gesichert als ${kopie.name}; neu begonnen")
            return Stand(emptyList(), 1)
        }
        if (gesichert != f.lastModified()) {
            runCatching { if (!kopie.exists()) f.copyTo(kopie) }.onFailure { Logbuch.fehler("Merkdatei sichern", it, dateiname) }
            gesichert = f.lastModified()
            Logbuch.fehler("Merkdatei defekt", e, "$dateiname gesichert als ${kopie.name}")
        }
        return Stand(emptyList(), 1, defekt = "Die Datei $dateiname ist beschädigt und wurde als ${kopie.name} gesichert. Jarvis speichert dort nichts Neues, bis sie repariert ist.")
    }

    @Synchronized
    fun alle(context: Context): List<Regel> = lies(context).eintraege

    /** null = in Ordnung, sonst der Grund, warum diese Datei gerade nicht beschrieben wird. */
    fun defekt(context: Context): String? = lies(context).defekt

    /** true = die Datei hat die Höchstzahl an Einträgen erreicht; nur dann ist „voll“ der Grund für ein gescheitertes Anlegen. */
    fun voll(context: Context): Boolean = lies(context).eintraege.size >= hoechstens

    /** Schreibt atomar. false = nicht geschrieben (Grund im Logbuch). */
    private fun schreibe(context: Context, vorher: Stand, eintraege: List<Regel>): Boolean {
        if (vorher.defekt != null) return false
        val naechste = maxOf(vorher.naechsteId, (eintraege.maxOfOrNull { it.id } ?: 0) + 1)
        val inhalt = JSONObject().put("format", 1).put("naechsteId", naechste)
            .put("regeln", JSONArray(eintraege.map { JSONObject().put("id", it.id).put("text", it.text).put("stand", it.stand) })).toString(2)
        val f = datei(context)
        val atomar = AtomicFile(f)
        val aus = try { atomar.startWrite() } catch (e: Exception) { Logbuch.fehler("Merkdatei schreiben", e, dateiname); return false }
        try {
            aus.write(inhalt.toByteArray(Charsets.UTF_8))
            atomar.finishWrite(aus)
        } catch (e: Exception) {
            atomar.failWrite(aus)
            Logbuch.fehler("Merkdatei schreiben", e, dateiname)
            return false
        }
        zwischen = Triple(f.lastModified(), f.length(), Stand(eintraege, naechste))
        neuBegonnen = null
        // Erst nach dem gelungenen Schreiben: Daran erkennen Plugin und Oberfläche den neuen Stand.
        zuletztGeaendert = System.currentTimeMillis()
        _stand.update { it + 1 }
        return true
    }

    private fun heute(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    /** Leerraum glätten; nur über der harten Grenze an der letzten Satzgrenze kürzen (und das festhalten). */
    private fun saeubere(text: String): String {
        val sauber = text.replace(Regex("\\s+"), " ").trim()
        if (sauber.length <= HARTE_GRENZE) return sauber
        Logbuch.gedaechtnis("Merkeintrag gekürzt", "$dateiname: ${sauber.length} Zeichen")
        val stueck = sauber.take(HARTE_GRENZE)
        val satzEnde = stueck.lastIndexOfAny(charArrayOf('.', '!', '?'))
        return if (satzEnde > HARTE_GRENZE / 2) stueck.take(satzEnde + 1) else "$stueck …"
    }

    /**
     * Legt einen Eintrag an oder ersetzt mit [id] den Text eines vorhandenen. id 0 oder kleiner = neu anlegen.
     * null = diese id gibt es nicht, die Datei ist voll oder defekt (siehe [defekt]).
     */
    @Synchronized
    fun speichere(context: Context, text: String, id: Int? = null): Regel? {
        val vorher = lies(context)
        if (vorher.defekt != null) return null
        val bisher = vorher.eintraege
        // Eingesprochenes darf länger sein als die Vorgabe an die Modelle; die KI-Korrektur strafft es danach.
        val sauber = saeubere(text)
        val nummer = id?.takeIf { it > 0 }
        val eintrag = if (nummer != null) {
            if (bisher.none { it.id == nummer }) return null
            Regel(nummer, sauber, heute())
        } else {
            if (bisher.size >= hoechstens) return null
            Regel(vorher.naechsteId, sauber, heute())
        }
        val neu = if (nummer != null) bisher.map { if (it.id == nummer) eintrag else it } else bisher + eintrag
        return if (schreibe(context, vorher, neu)) eintrag else null
    }

    /**
     * Ersetzt den Text von [id] nur, wenn dort noch [erwartet] steht (etwa nach einer KI-Korrektur, die eine Weile lief).
     * null = Eintrag weg, inzwischen geändert, Datei defekt oder nicht geschrieben.
     */
    @Synchronized
    fun ersetzeWenn(context: Context, id: Int, erwartet: String, neu: String): Regel? {
        val vorher = lies(context)
        if (vorher.defekt != null) return null
        val alt = vorher.eintraege.firstOrNull { it.id == id } ?: return null
        if (alt.text != saeubere(erwartet)) return null
        val eintrag = Regel(id, saeubere(neu), heute())
        return if (schreibe(context, vorher, vorher.eintraege.map { if (it.id == id) eintrag else it })) eintrag else null
    }

    /**
     * Stellt einen einzelnen Eintrag mit seiner alten id wieder her (Rückgängig): ersetzt ihn, wenn es ihn noch gibt,
     * sonst kommt er zurück. Andere Einträge bleiben unberührt. null = Datei voll, defekt oder nicht geschrieben.
     */
    @Synchronized
    fun stelleWiederHer(context: Context, regel: Regel): Regel? {
        if (regel.id <= 0) return null
        val vorher = lies(context)
        if (vorher.defekt != null) return null
        val da = vorher.eintraege.any { it.id == regel.id }
        if (!da && vorher.eintraege.size >= hoechstens) return null
        val neu = if (da) vorher.eintraege.map { if (it.id == regel.id) regel else it } else vorher.eintraege + regel
        return if (schreibe(context, vorher, neu)) regel else null
    }

    @Synchronized
    fun loesche(context: Context, id: Int): Regel? {
        val vorher = lies(context)
        val weg = vorher.eintraege.firstOrNull { it.id == id } ?: return null
        return if (schreibe(context, vorher, vorher.eintraege - weg)) weg else null
    }

    /** Stellt einen früheren Stand wieder her (Rückgängig in der App). Der id-Zähler sinkt dabei nie. */
    @Synchronized
    fun setze(context: Context, eintraege: List<Regel>): Boolean = schreibe(context, lies(context), eintraege)

    fun suche(context: Context, wort: String): List<Regel> = wort.trim().lowercase(Locale.GERMAN).let { w -> alle(context).filter { w in it.text.lowercase(Locale.GERMAN) } }

    fun liste(eintraege: List<Regel>): String = eintraege.sortedBy { it.id }.joinToString("\n") { "[${it.id}] ${it.text}" }

    /** Der Block, der jedem Modell beiliegt. Leer, solange nichts drinsteht; bei einer defekten Datei die Warnung. */
    fun alsKontext(context: Context): String {
        val s = lies(context)
        s.defekt?.let { return "$kopf\nGEDÄCHTNIS-DATEI DEFEKT: $it Sag Frank das.\n" }
        val hinweis = neuBegonnen?.let { "GEDÄCHTNIS-DATEI ZURÜCKGESETZT: $it Frühere Einträge stehen nur noch in der Sicherung. Sag Frank das.\n" }.orEmpty()
        val eintraege = s.eintraege.takeIf { it.isNotEmpty() }?.let { liste(it) + "\n" }.orEmpty()
        return if (hinweis.isEmpty() && eintraege.isEmpty()) "" else "$kopf\n$hinweis$eintraege"
    }

    companion object {
        /** Länge, die die Modelle für einen Eintrag anstreben sollen. */
        const val ZEICHEN = 400
        /** Höchstlänge über das Werkzeug; längere Texte meldet jarvis_merken zurück, statt sie still zu kürzen. */
        const val LAENGSTENS = 1500
        /** Sicherung gegen ausufernde Diktate; erst darüber wird (an einer Satzgrenze) gekürzt. */
        private const val HARTE_GRENZE = 4000
        /** Wann zuletzt eine der Dateien geschrieben wurde; daran erkennt das Plugin, ob ein Programm den neuen Stand schon hat. */
        @Volatile var zuletztGeaendert = 0L
        private val _stand = MutableStateFlow(0L)
        /** Zählt jedes gelungene Schreiben einer der Dateien; die Oberfläche lädt darauf neu. */
        val stand: StateFlow<Long> = _stand.asStateFlow()
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
            // id 0 heißt bei vielen Modellen „nicht angegeben“: dann neu anlegen.
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id", 0).takeIf { it > 0 } else null
            art.datei.defekt(context)?.let { return@Werkzeug Ergebnis("Nicht gemerkt. $it Sag Frank das.", fehler = true) }
            val laenge = inhalt.replace(Regex("\\s+"), " ").length
            if (laenge > Merkdatei.LAENGSTENS) return@Werkzeug Ergebnis("Zu lang (${laenge} von höchstens ${Merkdatei.LAENGSTENS} Zeichen), nichts gemerkt. Fasse den Eintrag auf etwa ${Merkdatei.ZEICHEN} Zeichen zusammen und rufe jarvis_merken noch einmal auf; mehrere Dinge = mehrere Einträge.", fehler = true)
            val eintrag = art.datei.speichere(context, inhalt, nummer)
                ?: return@Werkzeug Ergebnis(
                    art.datei.defekt(context)?.let { "Nicht gemerkt. $it Sag Frank das." } ?: when {
                        nummer != null && art.datei.alle(context).none { it.id == nummer } -> "Einen Eintrag der Art ${art.kennung} mit der id $nummer gibt es nicht. Lies nach mit jarvis_gemerktes_lesen."
                        nummer == null && art.datei.voll(context) -> "Diese Datei ist voll. Fasse ähnliche Einträge zusammen oder lösche überholte, dann noch einmal."
                        else -> "Nicht gemerkt: Die Datei ließ sich gerade nicht schreiben (Grund im Protokoll). Versuche es noch einmal; klappt es wieder nicht, sag Frank das."
                    },
                    fehler = true,
                )
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
            // id 0 heißt bei vielen Modellen „nicht angegeben“: dann gilt das Suchwort.
            val nummer = (if (a.has("id") && !a.isNull("id")) a.optInt("id", 0).takeIf { it > 0 } else null) ?: run {
                val treffer = if (a.gesetzt("suche")) art.datei.suche(context, a.optString("suche")) else return@Werkzeug Ergebnis("Es fehlt die id oder ein Suchwort.", fehler = true)
                when (treffer.size) {
                    0 -> return@Werkzeug Ergebnis("Kein Eintrag der Art ${art.kennung} gefunden, der zu „${a.optString("suche")}“ passt.", fehler = true)
                    1 -> treffer.first().id
                    else -> return@Werkzeug Ergebnis("Mehrere Einträge passen. Frage Frank, welcher gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + art.datei.liste(treffer), fehler = true)
                }
            }
            art.datei.defekt(context)?.let { return@Werkzeug Ergebnis("Nicht gelöscht. $it Sag Frank das.", fehler = true) }
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
