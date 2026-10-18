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

/**
 * Ein Eintrag einer Merkdatei: eine Regel, ein Ziel oder eine Notiz über Frank. [quelle] = woher er kam: leer = von Frank
 * selbst (Einstellungen, jarvis_merken) oder aus der Zeit vor der Herkunftskennung; „Gespräch“ oder „Tagebuch“ = von einem
 * Lernlauf angelegt. Nur gelernte Einträge darf ein Lernlauf von sich aus wieder entfernen.
 */
data class Regel(val id: Int, val text: String, val stand: String, val quelle: String = "") {
    val gelernt: Boolean get() = quelle.isNotEmpty()
}

/**
 * Eine Datei, die Jarvis selbst pflegt und jedem Modell als Kontext beilegt: dem Plugin (Anleitung und
 * Werkzeug-Ergebnisse), dem eigenen Chat, den Agenten und der Tagesauswertung. Es gibt vier davon:
 * [Regeln] (wie Jarvis arbeiten soll), [Ziele] (was Frank erreichen will), [UeberFrank] (was Jarvis über Frank weiß) und
 * [Programmierregeln] (wie Jarvis Programmieraufträge steuert und den Programmierer instruiert; reiner Jarvis-Kontext, geht nie an Programmier-Sitzungen).
 * Im Code stehen deshalb keine einzelnen Regeln, Ziele oder Angaben über Frank, nur dieser Speicher.
 *
 * - ids werden nie wiederverwendet (Zähler `naechsteId` in der Datei): Eine veraltete id trifft nie einen fremden Eintrag.
 * - Geschrieben wird atomar über [AtomicFile] (mit fsync und Sicherung).
 * - Ist die Datei vorhanden, aber unlesbar, wird sie als `<name>.defekt-<zeit>` gesichert, weggeräumt, und Jarvis beginnt
 *   leer neu (Hinweis in [alsKontext] und [neuBegonnen]). Nur wenn das Sichern scheitert, bleibt die Datei gesperrt;
 *   sonst würde das nächste Merken alle Einträge unwiederbringlich überschreiben.
 */
open class Merkdatei(
    private val dateiname: String,
    private val hoechstens: Int,
    private val kopf: String,
    /** Einträge, mit denen die Datei beim allerersten Lesen angelegt wird. Eine vorhandene (auch leere) Datei bleibt unberührt. */
    private val vorgabe: List<String> = emptyList(),
) {
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
            // Wurde die Datei nach einem Defekt weggeräumt, nie still die Vorgabe darüberlegen: Franks Fassung liegt in der
            // Sicherung, und die Warnung (neuBegonnen) soll bleiben, bis er selbst etwas schreibt.
            val gesichert = f.parentFile?.listFiles { d -> d.name.startsWith("$dateiname.defekt-") }?.isNotEmpty() == true
            // Nach einem Defekt zählen die ids hinter denen der Sicherung weiter: Eine alte id (etwa noch im Kontext von ChatGPT)
            // träfe sonst einen neuen, fremden Eintrag.
            // Zwischengespeichert wie bei der Vorgabe (fehlende Datei: Zeit und Länge 0), sonst läse jedes Lesen die Sicherungen neu.
            if (gesichert) return Stand(emptyList(), naechsteAusSicherungen(f)).also { zwischen = Triple(0L, 0L, it) }
            if (vorgabe.isEmpty()) return Stand(emptyList(), 1)
            // Erste Nutzung: mit der Vorgabe anlegen (ids 1 bis n in ihrer Reihenfolge). Klappt das Schreiben nicht, gilt sie für
            // diesen Lauf trotzdem, ohne es bei jedem Lesen erneut zu versuchen (fehlende Datei: Zeit und Länge 0).
            val eintraege = vorgabe.mapIndexed { i, t -> Regel(i + 1, saeubere(t), heute()) }
            val stand = Stand(eintraege, eintraege.size + 1)
            if (!schreibe(context, Stand(emptyList(), 1), eintraege)) {
                Logbuch.gedaechtnis("Merkdatei-Vorgabe nicht geschrieben", dateiname)
                zwischen = Triple(0L, 0L, stand)
            }
            return stand
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
                liste.optJSONObject(i)?.let { r -> Regel(r.optInt("id", 0), r.optString("text").trim(), r.optString("stand"), r.optString("quelle")) }?.takeIf { it.id > 0 && it.text.isNotEmpty() }
            }
            Stand(eintraege, maxOf(json.optInt("naechsteId", 0), (eintraege.maxOfOrNull { it.id } ?: 0) + 1))
        } catch (e: Exception) {
            return defekt(f, e)
        }
        zwischen = Triple(f.lastModified(), f.length(), stand)
        return stand
    }

    /**
     * Die erste freie id nach allen gesicherten (defekten) Fassungen dieser Datei. Gelesen wird roh mit einem Muster, weil die
     * Sicherungen ja gerade kein gültiges JSON sein müssen. Mindestens 1.
     */
    private fun naechsteAusSicherungen(f: File): Int {
        val muster = Regex("\"(?:id|naechsteId)\"\\s*:\\s*(\\d{1,9})")
        val hoechste = f.parentFile?.listFiles { d -> d.name.startsWith("$dateiname.defekt-") }.orEmpty().maxOfOrNull { s ->
            runCatching { muster.findAll(s.readText()).maxOfOrNull { it.groupValues[1].toInt() } }.getOrNull() ?: 0
        } ?: 0
        return hoechste + 1
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
            return Stand(emptyList(), naechsteAusSicherungen(f))
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
            .put("regeln", JSONArray(eintraege.map { JSONObject().put("id", it.id).put("text", it.text).put("stand", it.stand).apply { if (it.quelle.isNotEmpty()) put("quelle", it.quelle) } })).toString(2)
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
     * [quelle] = Lernlauf, der ihn anlegt („Gespräch“, „Tagebuch“); leer = Frank selbst. Ersetzt ein Lernlauf einen Eintrag
     * von Frank, bleibt er Franks Eintrag; ersetzt Frank einen gelernten, gilt er als von Frank bestätigt.
     * null = diese id gibt es nicht, die Datei ist voll oder defekt (siehe [defekt]).
     */
    @Synchronized
    fun speichere(context: Context, text: String, id: Int? = null, quelle: String = ""): Regel? {
        val vorher = lies(context)
        if (vorher.defekt != null) return null
        val bisher = vorher.eintraege
        // Eingesprochenes darf länger sein als die Vorgabe an die Modelle; die KI-Korrektur strafft es danach.
        val sauber = saeubere(text)
        val nummer = id?.takeIf { it > 0 }
        val eintrag = if (nummer != null) {
            val alt = bisher.firstOrNull { it.id == nummer } ?: return null
            Regel(nummer, sauber, heute(), if (alt.gelernt) quelle else "")
        } else {
            if (bisher.size >= hoechstens) return null
            Regel(vorher.naechsteId, sauber, heute(), quelle)
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
        val eintrag = Regel(id, saeubere(neu), heute(), alt.quelle)
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

    /** Setzt die Herkunft eines vorhandenen Eintrags (Nachtrag aus dem Aktivitätsprotokoll); Text und Stand bleiben. */
    @Synchronized
    fun setzeQuelle(context: Context, id: Int, quelle: String): Boolean {
        val vorher = lies(context)
        val alt = vorher.eintraege.firstOrNull { it.id == id } ?: return false
        if (alt.quelle == quelle) return true
        return schreibe(context, vorher, vorher.eintraege.map { if (it.id == id) it.copy(quelle = quelle) else it })
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

    fun liste(eintraege: List<Regel>): String = eintraege.sortedBy { it.id }.joinToString("\n") { "[$kennzeichen${it.id}] ${it.text}" }

    /** Vorsatz vor den ids im Kontext; leer bei den Dateien, die über jarvis_merken mit ihrer id bearbeitet werden. */
    protected open val kennzeichen: String = ""

    /** Die Überschrift des Blocks; eine Datei kann sie aus ihren Einträgen ableiten (Stand, Fassung). */
    protected open fun kopfText(eintraege: List<Regel>): String = kopf

    /** Der Block, der jedem Modell beiliegt. Leer, solange nichts drinsteht; bei einer defekten Datei die Warnung. */
    fun alsKontext(context: Context): String {
        val s = lies(context)
        val kopf = kopfText(s.eintraege)
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
        /** Ab diesem Anteil gemeinsamer Wörter gilt ein neuer Eintrag als mögliche Dublette oder Korrektur eines alten. */
        const val AEHNLICH = 0.6
        private val FUELL = setOf(
            "frank", "franks", "jarvis", "der", "die", "das", "den", "dem", "des", "und", "oder", "ist", "sind", "ein", "eine", "einen", "einem", "einer",
            "mit", "bei", "für", "von", "vom", "zum", "zur", "auf", "aus", "nicht", "immer", "soll", "sollen", "wenn", "dann", "auch", "noch", "nur", "dass",
            "hat", "haben", "wird", "werden", "sich", "ihm", "ihn", "sein", "seine", "seinen", "seiner", "will", "ich", "mir", "mich", "wie", "was", "als", "über", "nach", "vor", "bis",
        )

        /** Die tragenden Wörter eines Eintrags (ohne Füllwörter und sehr kurze Wörter) für den Dublettenvergleich. */
        fun woerter(text: String): Set<String> = text.lowercase(Locale.GERMAN).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 && it !in FUELL }.toSet()

        /** Anteil gemeinsamer tragender Wörter (0 bis 1). */
        fun aehnlichkeit(a: String, b: String): Double {
            val x = woerter(a)
            val y = woerter(b)
            if (x.isEmpty() || y.isEmpty()) return 0.0
            return (x intersect y).size.toDouble() / (x union y).size
        }

        /** Gleich bis auf Leerraum, Groß/Klein und Satzzeichen am Ende: eine Wiederholung desselben Eintrags. */
        fun gleich(a: String, b: String): Boolean {
            fun n(t: String) = t.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim().trimEnd('.', '!', '?', ' ')
            return n(a) == n(b)
        }

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

/**
 * Franks Programmierregeln: ausschließlich Kontext für Jarvis selbst, damit er Programmieraufgaben richtig steuert und den
 * Programmierer passend instruiert. Sie gehen nie automatisch an eine Programmier-Session (PC, Mac, Cloud): nicht im Auftrag
 * ([ProgrammierFaehigkeit]), nicht in der Plugin-Anleitung, nicht in nachgereichten Gedächtnis-Blöcken und nicht im
 * Live-Gedächtnis (jarvis_kontext), weil all das auch die Programmier-Sitzungen über MCP lesen. Jarvis' eigene Modelle
 * bekommen sie dazu ([merkKontext], [JarvisAgent]); ChatGPT liest sie bei Bedarf mit jarvis_gemerktes_lesen.
 * Kein Lernlauf legt hier etwas an; Einträge kommen nur von Frank (Einstellungen oder jarvis_merken).
 */
object Programmierregeln : Merkdatei(
    "programmierregeln.json", 40,
    "FRANKS PROGRAMMIERREGELN (von Frank selbst festgelegt; ausschließlich Kontext für Jarvis: wie du Programmieraufträge steuerst, formulierst " +
        "und den Programmierer instruierst; nutze sie dafür, schicke den Regeltext aber nie an Programmierer, Programmier-Sitzungen oder PC/Mac mit; nicht ungefragt vorlesen):",
)

/**
 * Franks Antwort-Stil: wie Jarvis' Antworten aussehen sollen (Länge, Gliederung, Ton, Symbole). Entsteht aus Franks
 * Daumen-Bewertungen ([de.frank.jarvis.data.Stil]) und aus seinen eigenen Angaben; Frank bearbeitet ihn in den Einstellungen.
 */
object StilRegeln : Merkdatei(
    "stil.json", 40,
    "FRANKS ANTWORT-STIL (aus seinen Daumen-Bewertungen gelernt und von ihm bearbeitbar; gilt für Form, Länge, Gliederung und Ton deiner Antworten und geht den allgemeinen Formatvorgaben vor; nicht ungefragt vorlesen):",
)

/**
 * Franks ausdrückliche Entscheidungen („wir machen es so“, „ich habe mich für … entschieden“), damit ein neues Gespräch
 * nicht wieder bei null anfängt. Nur was Frank selbst beschlossen hat; Vorschläge und offene Überlegungen gehören nicht hierher.
 */
object Entscheidungen : Merkdatei(
    "entscheidungen.json", 80,
    "FRANKS ENTSCHEIDUNGEN (von Frank ausdrücklich getroffen, mit Datum; gelten, bis er sie ändert; frühere Entscheidungen gezielt mit gedaechtnis_suchen; nicht ungefragt aufzählen):",
)

/** Die Arten von Gemerktem, mit Datei, Wort und dem, was die Modelle dazu wissen müssen. */
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
    ),
    PROGRAMMIERREGEL(
        "programmierregel", Programmierregeln, "Programmierregel",
        "eine vollständige, für sich allein verständliche Anweisung an Jarvis in ein bis zwei Sätzen, wie er mit Programmieraufträgen umgeht oder den Programmierer instruiert: wann sie gilt und was dann zu tun ist („Bei Aufträgen an Android-Apps im Auftrag immer die Paketkennung nennen.“)",
        "Sag ihm in ein bis zwei Sätzen, welche Programmierregel du dir gemerkt hast; du richtest künftige Programmieraufträge danach aus, ohne den Regeltext an den Programmierer zu schicken.",
    ),
    STIL(
        "stil", StilRegeln, "Stilregel",
        "eine kurze Anweisung, wie deine Antworten aussehen sollen: Länge, Gliederung, Ton oder Symbole („Bei Statusmeldungen jede Sache in eine eigene Zeile mit ✅ davor.“, „Antworten zu Biowerten mit kleiner Überschrift je Messgröße.“)",
        "Sag ihm in einem Satz, welche Stilregel du dir gemerkt hast; ab jetzt antwortest du so.",
    ),
    ENTSCHEIDUNG(
        "entscheidung", Entscheidungen, "Entscheidung",
        "ein für sich allein verständlicher Satz, was Frank ausdrücklich entschieden hat, worum es geht und mit Datum, wenn es zählt („Frank hat am 17.10.2026 entschieden, ChatGPT als Gesprächsoberfläche zu nutzen und Jarvis als Gedächtnis im Hintergrund.“)",
        "Sag ihm in einem Satz, welche Entscheidung du festgehalten hast.",
    );

    companion object {
        fun von(kennung: String): Merkart? = entries.firstOrNull { it.kennung == kennung.trim().lowercase(Locale.GERMAN) }
    }
}

/**
 * Die Zusammenarbeit von ChatGPT Live und Jarvis: dieselben Regeln, die Frank 1:1 bei ChatGPT unter Personalisierung →
 * Benutzerdefinierte Anweisungen einträgt (die App kopiert sie ihm mit [fuerChatGpt]). Sie steht in jedem Kontext (Plugin, eigener
 * Chat, Agenten), nicht aber beim Schreiben der Tagesauswertung (die ist für Frank persönlich) und nicht in Nachträgen an Werkzeug-Ergebnisse. Bewusst keine [Merkart]: Kein
 * Modell soll die Regeln über jarvis_merken umschreiben können, sonst wiche die Kopie bei ChatGPT still ab; bearbeitet wird in
 * der App. Die Reihenfolge der ids ist die Priorität (neue Einträge kommen ans Ende).
 * [fassung] ist eine kurze Prüfsumme der Regeln; sie steht in beiden Fassungen, damit ChatGPT eine veraltete Kopie erkennt.
 */
object Symbiose : Merkdatei("symbiose.json", 30, "", SYMBIOSE_VORGABE) {
    /** So lang darf der Text für ChatGPT höchstens sein (Feld der benutzerdefinierten Anweisungen). */
    const val GRENZE = 4_000
    private val TAG_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Vier Zeichen Prüfsumme über die Regeln (ohne Überschrift), gleich in beiden Fassungen. */
    fun fassung(eintraege: List<Regel>): String {
        val crc = java.util.zip.CRC32()
        crc.update(liste(eintraege).toByteArray(Charsets.UTF_8))
        return "%04X".format(crc.value and 0xFFFF)
    }

    /** Der jüngste Änderungstag der Regeln. */
    fun stand(eintraege: List<Regel>): String =
        eintraege.mapNotNull { runCatching { LocalDate.parse(it.stand, TAG_FORMAT) }.getOrNull() }.maxOrNull()?.format(TAG_FORMAT).orEmpty()

    /** Die Überschrift der Fassung für ChatGPT. */
    fun titel(eintraege: List<Regel>): String =
        "JARVIS-SYMBIOSE · Stand ${stand(eintraege)} · Fassung ${fassung(eintraege)} · gilt nur im Sprachmodus; im Textmodus gelten meine übrigen Anweisungen."

    /** Ids als [S1], [S2] …: nie zu verwechseln mit den ids von FRANKS REGELN, die jarvis_merken ändert. */
    override val kennzeichen: String = "S"

    /** Der Text für ChatGPT aus gegebenen Einträgen (ohne Dateizugriff, etwa für die Oberfläche). */
    fun text(eintraege: List<Regel>): String = titel(eintraege) + "\n" + liste(eintraege)

    /** Der Text, den Frank in ChatGPT einfügt: Überschrift und Regeln, genau wie im Kontext von Jarvis. */
    fun fuerChatGpt(context: Context): String = text(alle(context))

    internal fun kopfTextFuerTest(eintraege: List<Regel>): String = kopfText(eintraege)

    override fun kopfText(eintraege: List<Regel>): String =
        "SYMBIOSE CHATGPT LIVE ⇄ JARVIS · Stand ${stand(eintraege)} · Fassung ${fassung(eintraege)} (von Frank festgelegt, nach Wichtigkeit geordnet; dieselben Regeln stehen 1:1 bei ChatGPT unter " +
            "Einstellungen → Personalisierung → Benutzerdefinierte Anweisungen. „Du“ meint darin ChatGPT Live in der ChatGPT-App, „Jarvis“ die Jarvis-App mit ihren Werkzeugen. " +
            "Bist du Jarvis selbst oder einer seiner Agenten, weißt du daraus, wie ChatGPT dich über MCP anspricht und was es von dir erwartet; die Grundsätze zu Ehrlichkeit, Gedächtnis " +
            "und Hintergrundarbeit gelten für dich sinngemäß, Werkzeuge, die du nicht hast, rufst du nicht auf. Die Regeln [S…] ändert nur Frank in der App, nie über jarvis_merken oder jarvis_vergessen; „Korrektur über die id“ darin meint die Einträge der anderen Dateien. Nicht ungefragt vorlesen):"
}

/** Die erste Fassung der Symbiose-Regeln (Stand 17.10.2026). Danach gilt, was Frank in der App daraus macht. */
internal val SYMBIOSE_VORGABE = listOf(
    "Rollen: Du bist ChatGPT Live, mein Gesprächspartner. Jarvis ist meine App auf dem Handy, angebunden als Konnektor „Jarvis“: mein dauerhaftes Gedächtnis, Zugang zu meinen Apps und Daten, " +
        "ausführende Stelle, Hintergrund-Agenten und Übergabe an den Programmierer. Du sprichst, denkst mit und formulierst; Jarvis weiß, speichert und führt aus.",
    "Start: Vor deiner ersten Antwort in jedem Gespräch rufst du jarvis_kontext auf, nach etwa einer Stunde erneut. Darin stehen meine Regeln, Ziele, Entscheidungen, " +
        "Tageslage und offene Aufträge, je mit Datenstand. Verlass dich nicht auf alte Chats. Details lädst du gezielt nach: jarvis_kontext mit bereiche, gedaechtnis_suchen für Früheres und frühere Entscheidungen.",
    "Wer macht was: Alles zu meinen Daten, Apps, Terminen, Gesundheit, Gedächtnis und Aufträgen läuft über Jarvis, auch wenn ich „Jarvis“ nicht sage. Sage ich „nicht über Jarvis“, machst du es selbst. " +
        "Allgemeinwissen und Gespräch übernimmst du selbst, mit meinem Kontext im Blick.",
    "Ehrlichkeit: Erledigt ist etwas erst, wenn das Werkzeug es bestätigt; „angenommen“ heißt nicht fertig. Scheitert etwas, sag es in einem Satz. Kam keine Antwort, prüfe mit dem passenden " +
        "Lese-Werkzeug, statt blind zu wiederholen. Daten mit altem Stand nennst du als alt.",
    "Gedächtnis: Sage ich etwas Dauerhaftes, speichere es sofort mit jarvis_merken, auch ohne „merk dir“: info (Tatsache über mich), regel („ab jetzt“, „immer“, „nie“), ziel, entscheidung " +
        "(„ich habe entschieden“), stil (wie du antworten sollst). Korrektur: alten Eintrag über seine id ersetzen. Nur was ich sage oder bestätige: deine Vorschläge erst nach meinem Ja, " +
        "offene Gedanken mit idee_speichern, nie als Aufgabe.",
    "Lange Arbeit: Recherchen und alles über eine Minute gibst du mit agent_starten an einen Jarvis-Agenten und redest weiter. Fertiges holst du mit vorgaenge, sagst es mir an passender Stelle " +
        "und bestätigst dann mit vorgaenge (aktion mitgeteilt); ebenso bei „VORGANG FERTIG“ an einem Ergebnis. Jarvis kann dich nicht von sich aus ansprechen.",
    "Programmierer: „Schicke bitte folgenden Auftrag an den Programmierer“, „schick das an den Programmierer“ oder „lass das programmieren“ heißt: Auftrag mit programmieren an Claude Code; " +
        "Jarvis legt dafür eine eigene Programmier-Session an. Nenne die App und schreib den Auftrag vollständig aus dem Gespräch: Ziel, was sich ändern soll, woran man erkennt, dass es fertig ist. " +
        "Unklare App: kurz fragen. Rückfragen der Sitzung beantwortest du mit programmier_antwort, den Stand holst du mit vorgaenge.",
    "Auslöser: „Erstelle eine Aufgabe …“, „erinnere mich …“, „hake ab …“ → Aufgaben-App (aufgabe_anlegen). „Ich habe eine Idee …“ → Geniale Ideen (idee_speichern). Termin, Dienst, " +
        "„wann muss ich arbeiten“ → kalender_eintragen, dienstplan_lesen. „Stell den Wecker …“ → wecker_stellen. „Timer …“, „Stoppuhr …“ → timer, stoppuhr. Schlaf, HRV, Erholung, Training → " +
        "biomarker_auswertung, trainings_lesen. Wetter, „wie lange zur Arbeit“ → wetter_lesen, fahrzeit_lesen. Tagebuch → tagebuch_lesen. Nahrungsergänzung → stacks_lesen; Epigenetik → " +
        "epigenetik_lesen. Mail → mail_lesen, mail_senden. „Leg das in die Ablage“, Bild → ablage_schreiben, bild_erzeugen. „Guten Morgen“, Tagesauswertung → tagesauswertung_lesen. " +
        "Jarvis-Einstellung → einstellungen. „Bist du da?“ → jarvis_status.",
    "Sprechen: kurz und natürlich; nie Kennungen oder Werkzeugnamen vorlesen. Datum und Uhrzeit nimmst du von Jarvis. Fehlt Tag oder Uhrzeit oder ist etwas mehrdeutig, frag kurz nach. " +
        "Dienst, Schlafzeiten, Wetter und Erholung nennst du von dir aus, wenn sie zählen. Mehrere Dinge in einem Satz = mehrere Aufrufe.",
    "Abgleich: Nennt jarvis_kontext unter SYMBIOSE eine andere Fassung als oben, sag mir einmal, dass ich diesen Text in der Jarvis-App neu kopieren soll; bis dahin gilt die Fassung aus Jarvis.",
)

/**
 * Alle Merkdateien in der Reihenfolge, in der sie im Kontext stehen. Die Symbiose zuletzt: Manche Programme kürzen die Anleitung
 * des Plugins, dann sollen Franks eigene Regeln ankommen; ChatGPT hat die Symbiose ohnehin in seinen Anweisungen.
 */
val merkdateien: List<Merkdatei> get() = Merkart.entries.map { it.datei } + Symbiose

/** Alle Dateien als ein Block für den Kontext von Jarvis' eigenen Modellen (Chat, Agenten, Tagesauswertung). */
fun merkKontext(context: Context): String = merkdateien.joinToString("") { it.alsKontext(context) }

/**
 * Alles ohne die [Programmierregeln]: für jeden Text, der das Handy über das Plugin verlässt (Anleitung, Werkzeug-Ergebnisse,
 * Live-Gedächtnis in jarvis_kontext). Auch Programmier-Sitzungen lesen das über MCP; der Relay unterscheidet sie nicht von ChatGPT.
 */
fun merkKontextOhneProgrammierregeln(context: Context, ohneSymbiose: Boolean = false): String =
    merkKontextAusser(merkdateien, ohneSymbiose).joinToString("") { it.alsKontext(context) }

/** Die Auswahl dazu, rein und ohne Dateizugriff (auch für die Tests). */
fun merkKontextAusser(dateien: List<Merkdatei>, ohneSymbiose: Boolean): List<Merkdatei> =
    dateien.filter { it !== Programmierregeln && !(ohneSymbiose && it === Symbiose) }

/** Dasselbe ohne die Symbiose: für Nachträge an einzelne Werkzeug-Ergebnisse, die den Startkontext schon haben. */
fun merkKontextOhneSymbiose(context: Context): String = merkdateien.filter { it !== Symbiose }.joinToString("") { it.alsKontext(context) }

/** Das Gedächtnis von Jarvis: vier Dateien, drei Werkzeuge (merken, vergessen, lesen). */
class RegelnFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "regeln"
    override val name = "Jarvis' Gedächtnis"
    override val beschreibung = "Franks Regeln, seine Ziele, was Jarvis über ihn weiß und seine Programmierregeln: merken, ändern, löschen. Jarvis pflegt das selbst und hat es bei allem im Blick."
    override val hinweise =
        "Du führst sechs Dateien über Frank und pflegst sie selbst mit jarvis_merken und jarvis_vergessen. " +
            "STIL = wie deine Antworten aussehen sollen (Länge, Gliederung, Ton, Symbole), wenn Frank das sagt („antworte mir kürzer“, „mach bei so etwas immer Stichpunkte“); " +
            "die Datei wächst außerdem von selbst aus seinen Daumen-Bewertungen. " +
            "REGEL = wie du arbeiten oder antworten sollst („immer“, „nie“, „in Zukunft“, „ab jetzt“, oder Frank bemängelt eine Antwort), auch wie du mit Programmieraufträgen umgehst. " +
            "PROGRAMMIERREGEL = wie du (Jarvis) Programmieraufträge steuerst, formulierst und den Programmierer instruierst; reiner Jarvis-Kontext, der nie an Programmierer mitgeht; " +
            "lege sie nur an, wenn Frank sie ausdrücklich als Programmierregel will, und nie doppelt zu einer Regel, die schon unter FRANKS REGELN steht. " +
            "ZIEL = was Frank erreichen will („ich will meine VO2max steigern“). " +
            "INFO = eine dauerhafte Tatsache über ihn: Vorlieben, Abneigungen, was ihm wichtig ist, Besitz, persönliche Daten, Gewohnheiten, Familie, Gesundheit. " +
            "ENTSCHEIDUNG = was Frank ausdrücklich beschlossen hat. Merke nur, was Frank selbst sagt oder bestätigt; deine Vorschläge erst nach seinem Ja, " +
            "offene Gedanken ohne Beschluss als Idee (idee_speichern), nie als Aufgabe oder als bereits Erledigtes. " +
            "Erkennst du so etwas in dem, was Frank sagt, auch nebenbei, merke es dir von selbst und ohne nachzufragen und sag ihm kurz, was du dir gemerkt hast. " +
            "Halte die Dateien wahr: Gehört Neues zu einem vorhandenen Eintrag oder widerspricht es ihm (notiert ist „geht bei Regen nicht laufen“, jetzt sagt er „im Regen laufen ist doch gut, das mache ich öfter“), " +
            "ersetze diesen Eintrag über seine id, statt einen zweiten anzulegen; was gar nicht mehr gilt (Ziel erreicht, Auto verkauft), löschst du. " +
            "Nichts Flüchtiges merken (Stimmung von heute, einmalige Wünsche, einzelne Termine). " +
            "Hängt ein Eintrag an einem Begriff aus einer von Franks Apps, dessen genauen Wert du nicht kennst (er will „eine grüne Erholung bei Whoop“: ab wie viel Prozent ist sie grün?), " +
            "schlag den Wert zuerst mit repo_lesen im Code der App nach und trag ihn mit ein („… also mindestens so viel Prozent“). Findest du ihn nicht, merk dir den Eintrag in Franks Worten. " +
            "Was gilt, steht unter FRANKS REGELN, FRANKS ZIELE, WAS JARVIS ÜBER FRANK WEISS, FRANKS PROGRAMMIERREGELN und FRANKS ANTWORT-STIL: Regeln und Stil hältst du ein, Ziele und Wissen beziehst du von dir aus ein, " +
            "Programmierregeln nutzt du beim Formulieren und Begleiten von Programmieraufträgen; sie gehen nie automatisch mit, und du schreibst den Regeltext auch nicht in den Auftrag. " +
            "Stehen sie nicht in deinem Kontext (Plugin), lies sie vor einem Programmierauftrag mit jarvis_gemerktes_lesen."

    override fun stoerung(): String? = null

    private val arten = Merkart.entries.map { it.kennung }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "jarvis_merken",
            titel = "Merken oder ändern (Regel, Ziel, Info, Entscheidung, Programmierregel, Stil)",
            beschreibung = "Jarvis: merkt sich dauerhaft etwas über Frank oder ändert einen vorhandenen Eintrag. Nur was Frank selbst gesagt oder ausdrücklich bestätigt hat; " +
                "eigene Vorschläge erst nach seinem Ja, offene Überlegungen ohne Beschluss mit idee_speichern (nie als Aufgabe oder als schon Erledigtes). " +
                "entscheidung = was Frank ausdrücklich beschlossen hat („wir machen es so“, „ich nehme …“). Weitere Arten: " +
                "stil = wie Jarvis' Antworten aussehen sollen („antworte mir kürzer“, „bei so etwas immer Stichpunkte“); " +
                "regel = wie Jarvis künftig arbeiten oder antworten soll („nenn mir bei Schlafwerten immer auch …“, „mach das nie wieder so“, „ab jetzt …“); " +
                "ziel = was Frank erreichen will („ich will meine VO2max und HRV steigern und den Ruhepuls senken“); " +
                "info = eine dauerhafte Tatsache über Frank („ich fahre einen …“, „ich bin am … geboren“, „bei Regen gehe ich nicht laufen“, „die Drohne … habe ich mir gekauft“); " +
                "programmierregel = wie Jarvis Programmieraufträge steuert und den Programmierer instruiert, nur wenn Frank es ausdrücklich als Programmierregel will (reiner Jarvis-Kontext, geht nie an den Programmierer mit). " +
                "Rufe es von dir aus auf, sobald Frank so etwas sagt, auch nebenbei und ohne „merk dir“. Mehrere Dinge in einem Satz = mehrere Aufrufe. " +
                "Nennt Frank dabei einen Begriff aus einer seiner Apps, dessen genauen Wert du nicht kennst (zum Beispiel „grüne Erholung“), schlag ihn vorher mit repo_lesen im Code der App nach und nimm den Wert in den Text auf. " +
                "PFLEGE: Steht zum selben Thema schon ein Eintrag da oder widerspricht das Neue einem alten, übergib dessen id und den vollständigen neuen Text; der alte wird ersetzt. " +
                "Nicht für Flüchtiges (Stimmung von heute, einmalige Wünsche, einzelne Termine). Jarvis hat alles Gemerkte danach in jedem Gespräch, bei seinen Agenten und in der Tagesauswertung vor Augen.",
            schema = schema(
                "art" to text(arten.joinToString(", ") + ".", arten),
                "text" to text("Der Eintrag, in Franks Sinn formuliert, Hörfehler der Spracherkennung bereinigt, höchstens ${Merkdatei.ZEICHEN} Zeichen. " +
                    Merkart.entries.joinToString(" ") { "Bei ${it.kennung}: ${it.textHinweis}." }),
                "id" to zahl("Nur beim Ändern oder Ersetzen: id des vorhandenen Eintrags dieser Art (die Zahl in eckigen Klammern davor)."),
                "quelle" to text("frank (Vorgabe) = Frank hat es selbst gesagt oder ausdrücklich bestätigt; vorschlag = eigener Vorschlag oder Deutung von dir, den Frank noch nicht bestätigt hat (wird dann nicht gemerkt).", listOf("frank", "vorschlag")),
                "neu_erzwingen" to schalter("Nur nach dem Hinweis „sehr ähnlicher Eintrag“: true = trotzdem als eigenen Eintrag anlegen, weil es wirklich etwas anderes ist."),
                pflicht = listOf("art", "text"),
            ),
            nurLesen = false,
        ) { a ->
            val art = Merkart.von(a.optString("art")) ?: return@Werkzeug Ergebnis("art fehlt: " + arten.joinToString(", ") + ".", fehler = true)
            val inhalt = a.optString("text").trim()
            if (inhalt.length < 10) return@Werkzeug Ergebnis("Der Text fehlt oder ist zu kurz, um allein verständlich zu sein.", fehler = true)
            // id 0 heißt bei vielen Modellen „nicht angegeben“: dann neu anlegen.
            val nummer = if (a.has("id") && !a.isNull("id")) a.optInt("id", 0).takeIf { it > 0 } else null
            // Nur Franks eigene oder von ihm bestätigte Angaben: Ein Vorschlag des Assistenten darf nicht als Franks Regel, Ziel oder Entscheidung erscheinen.
            if (a.optString("quelle").equals("vorschlag", ignoreCase = true)) return@Werkzeug Ergebnis(
                "Nicht gemerkt: Das ist ein Vorschlag, keine Angabe von Frank. Frag ihn kurz („Soll ich mir merken, dass …?“) und rufe jarvis_merken erst nach seinem Ja mit quelle frank auf. " +
                    "Eine offene Überlegung ohne Beschluss gehört mit idee_speichern in Geniale Ideen, nicht ins Gedächtnis und nicht als Aufgabe.", fehler = true,
            )
            art.datei.defekt(context)?.let { return@Werkzeug Ergebnis("Nicht gemerkt. $it Sag Frank das.", fehler = true) }
            val laenge = inhalt.replace(Regex("\\s+"), " ").length
            if (laenge > Merkdatei.LAENGSTENS) return@Werkzeug Ergebnis("Zu lang (${laenge} von höchstens ${Merkdatei.LAENGSTENS} Zeichen), nichts gemerkt. Fasse den Eintrag auf etwa ${Merkdatei.ZEICHEN} Zeichen zusammen und rufe jarvis_merken noch einmal auf; mehrere Dinge = mehrere Einträge.", fehler = true)
            if (nummer == null) {
                val vorhanden = art.datei.alle(context)
                // Dieselbe Angabe noch einmal (Wiederholung nach Verbindungsabbruch, oder Frank sagt es erneut): nichts doppelt anlegen.
                vorhanden.firstOrNull { Merkdatei.gleich(it.text, inhalt) }?.let { return@Werkzeug Ergebnis("War schon gemerkt, nichts doppelt angelegt: [${it.id}] ${it.text}") }
                // Sehr ähnlich: meist eine Korrektur oder neue Fassung desselben Eintrags. Ohne neu_erzwingen nicht anlegen.
                val aehnlich = vorhanden.map { it to Merkdatei.aehnlichkeit(it.text, inhalt) }.filter { it.second >= Merkdatei.AEHNLICH }.sortedByDescending { it.second }.map { it.first }
                if (aehnlich.isNotEmpty() && !a.optBoolean("neu_erzwingen")) return@Werkzeug Ergebnis(
                    "Nicht angelegt: Es gibt schon einen sehr ähnlichen Eintrag der Art ${art.kennung}:\n" + art.datei.liste(aehnlich.take(3)) + "\n" +
                        "Ist das Neue eine Korrektur oder neue Fassung davon, rufe jarvis_merken mit art ${art.kennung}, id ${aehnlich.first().id} und dem vollständigen neuen Text auf (der alte wird ersetzt). " +
                        "Ist es wirklich etwas anderes, rufe es noch einmal mit neu_erzwingen true auf.", fehler = true,
                )
            }
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
            titel = "Gemerktes löschen (Regel, Ziel, Info, Programmierregel)",
            beschreibung = "Jarvis: löscht einen Eintrag, der nicht mehr gilt und nicht durch eine neue Fassung ersetzt wird: eine Regel, die Frank nicht mehr will („das gilt nicht mehr“), " +
                "ein erreichtes oder aufgegebenes Ziel, eine Tatsache, die nicht mehr stimmt („das Auto habe ich verkauft“), oder ein doppelter Eintrag. Über id ODER Suchwort. " +
                "Hat sich etwas nur geändert, nimm stattdessen jarvis_merken mit der id.",
            schema = schema(
                "art" to text(arten.joinToString(", ") + ".", arten),
                "id" to zahl("id des Eintrags (die Zahl in eckigen Klammern davor)."),
                "suche" to text("Statt id: Wort aus dem Text des Eintrags."),
                pflicht = listOf("art"),
            ),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            val art = Merkart.von(a.optString("art")) ?: return@Werkzeug Ergebnis("art fehlt: " + arten.joinToString(", ") + ".", fehler = true)
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
            titel = "Gemerktes lesen (Regeln, Ziele, Infos, Programmierregeln)",
            beschreibung = "Jarvis: liest alles, was Jarvis sich über Frank gemerkt hat: seine Regeln, seine Ziele, die Notizen über ihn und seine Programmierregeln, jeweils mit id. " +
                "Die Programmierregeln stehen nicht im Plugin-Kontext: Lies sie hier, bevor du einen Programmierauftrag formulierst, und richte den Auftrag danach aus, ohne ihren Text mitzuschicken. " +
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
