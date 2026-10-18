package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ein Hintergrundauftrag an einen Agenten, mit stabiler Kennung. Überlebt Verbindungsabbruch, App-Neustart und ein
 * neues ChatGPT-Gespräch: Das Ergebnis bleibt dem ursprünglichen Auftrag zugeordnet.
 * [ergebnis] = Titel des Ablage-Eintrags, nur wenn Frank die Ablage verlangt hat ([ablage]) oder es später übernommen wurde;
 * sonst steht der Bericht vorläufig unter [vorlaeufig] (nicht in der Ablage, siehe [de.frank.jarvis.ablage.Vorlaeufig]).
 * [gespraech] = das Jarvis-Gespräch, aus dem der Auftrag kam; dorthin kommt das Ergebnis. Leer bei Aufträgen aus ChatGPT.
 * [pdf] = Frank hat ausdrücklich ein PDF verlangt (agent_starten mit als_pdf). Unabhängig von [ablage]: Ein PDF-Wunsch allein legt nichts ab.
 * [abgerufen] = ein Werkzeug hat das Ergebnis ausgeliefert; [mitgeteilt] = ChatGPT hat bestätigt, es Frank gesagt zu haben.
 */
data class Vorgang(
    val id: String,
    val agent: String,
    val auftrag: String,
    val erstellt: Long,
    val stand: String,
    val schritt: String = "",
    val geaendert: Long = erstellt,
    val gestartet: Long = 0L,
    val beendet: Long = 0L,
    val ergebnis: String = "",
    val grund: String = "",
    val abgerufen: Long = 0L,
    val mitgeteilt: Long = 0L,
    val ablage: Boolean = false,
    val gespraech: String = "",
    val vorlaeufig: String = "",
    val pdf: Boolean = false,
) {
    val offen: Boolean get() = stand == ANGENOMMEN || stand == LAEUFT
    /** Fertig (oder gescheitert), aber Frank hat es laut ChatGPT noch nicht erfahren. */
    val neu: Boolean get() = !offen && mitgeteilt == 0L

    fun alsJson(): JSONObject = JSONObject().put("id", id).put("agent", agent).put("auftrag", auftrag).put("erstellt", erstellt).put("stand", stand)
        .put("schritt", schritt).put("geaendert", geaendert).put("gestartet", gestartet).put("beendet", beendet).put("ergebnis", ergebnis)
        .put("grund", grund).put("abgerufen", abgerufen).put("mitgeteilt", mitgeteilt)
        .put("ablage", ablage).put("gespraech", gespraech).put("vorlaeufig", vorlaeufig).put("pdf", pdf)

    companion object {
        const val ANGENOMMEN = "angenommen"
        const val LAEUFT = "laeuft"
        const val FERTIG = "fertig"
        const val FEHLGESCHLAGEN = "fehlgeschlagen"
        const val ABGEBROCHEN = "abgebrochen"

        fun aus(j: JSONObject) = Vorgang(
            j.optString("id"), j.optString("agent"), j.optString("auftrag"), j.optLong("erstellt"), j.optString("stand", FEHLGESCHLAGEN),
            j.optString("schritt"), j.optLong("geaendert"), j.optLong("gestartet"), j.optLong("beendet"), j.optString("ergebnis"),
            j.optString("grund"), j.optLong("abgerufen"), j.optLong("mitgeteilt"),
            // Alte Einträge (vor 1.8.179) legten immer in der Ablage ab.
            j.optBoolean("ablage", true), j.optString("gespraech"), j.optString("vorlaeufig"),
            // Ohne Feld (vor 1.8.184) kein PDF: Ein PDF entsteht nur noch auf ausdrücklichen Wunsch, nie aus einer Wortsuche im Auftrag.
            j.optBoolean("pdf", false),
        )

        fun standText(stand: String): String = when (stand) {
            ANGENOMMEN -> "angenommen, startet gleich"
            LAEUFT -> "läuft"
            FERTIG -> "fertig"
            FEHLGESCHLAGEN -> "fehlgeschlagen"
            ABGEBROCHEN -> "abgebrochen"
            else -> stand
        }
    }
}

/** Ergebnis von [VorgangsBuch.annehmen]. */
sealed class Annahme {
    data class Neu(val vorgang: Vorgang) : Annahme()
    /** Derselbe Auftrag ist schon angenommen (Wiederholung nach Verbindungsabbruch): nichts neu gestartet. */
    data class Doppelt(val vorgang: Vorgang) : Annahme()
    object KeinPlatz : Annahme()
}

/**
 * Das Buch der Hintergrundaufträge in einer JSON-Datei (atomar geschrieben: erst eine Nebendatei, dann umbenannt).
 * Ohne Android-Abhängigkeit, damit es sich ohne Gerät prüfen lässt; [Vorgaenge] bindet es an die App.
 *
 * Beim ersten Lesen in einem Prozess gelten alle noch offenen Einträge als abgebrochen: Ihr Lauf gehörte zu einem
 * früheren Prozess und lebt nicht mehr. Fortsetzen lässt er sich nicht, aber der Eintrag tut nicht so, als liefe er.
 */
class VorgangsBuch(private val datei: File, private val uhr: () -> Long = System::currentTimeMillis, private val melde: (String, String) -> Unit = { _, _ -> }) {
    private var liste: MutableList<Vorgang>? = null

    @Synchronized
    private fun alle(): MutableList<Vorgang> = liste ?: run {
        val gelesen = runCatching {
            if (!datei.exists()) emptyList() else JSONArray(datei.readText()).let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(Vorgang::aus) } }
        }.getOrElse { melde("Vorgänge: Datei unlesbar", it.message ?: it.javaClass.simpleName); emptyList() }.toMutableList()
        val jetzt = uhr()
        var verwaist = 0
        gelesen.replaceAll { v ->
            if (!v.offen) v else { verwaist++; v.copy(stand = Vorgang.ABGEBROCHEN, grund = "Jarvis wurde neu gestartet, während der Auftrag lief; er lässt sich mit agent_starten neu beauftragen", geaendert = jetzt, beendet = jetzt) }
        }
        liste = gelesen
        if (verwaist > 0) { melde("Vorgänge nach Neustart abgebrochen", "$verwaist offene"); schreibe() }
        gelesen
    }

    private fun schreibe() {
        val l = liste ?: return
        // Ältestes fällt heraus; Offene und noch nicht Mitgeteilte zuletzt.
        while (l.size > HOECHSTENS) {
            val weg = l.indexOfFirst { !it.offen && !it.neu }.takeIf { it >= 0 } ?: l.indexOfFirst { !it.offen }.takeIf { it >= 0 } ?: 0
            l.removeAt(weg)
        }
        runCatching {
            datei.parentFile?.mkdirs()
            val neu = File(datei.path + ".neu")
            neu.writeText(JSONArray(l.map { it.alsJson() }).toString())
            if (!neu.renameTo(datei)) { datei.delete(); check(neu.renameTo(datei)) { "Umbenennen gescheitert" } }
        }.onFailure { melde("Vorgänge: schreiben gescheitert", it.message ?: it.javaClass.simpleName) }
    }

    /** Lädt die Datei (und räumt Verwaistes ab), ohne etwas zu verändern. */
    @Synchronized
    fun lade() { alle() }

    /**
     * Nimmt einen Auftrag an. Steht derselbe Auftrag (gleicher Agent, gleicher Text bis auf Leerraum und Groß/Klein)
     * noch offen oder wurde er vor weniger als [DOPPELT_MS] angenommen, kommt dieser Eintrag zurück und nichts startet neu.
     * [reserviere] wird nur für einen wirklich neuen Auftrag gerufen (unter derselben Sperre, also ohne Wettlauf).
     */
    @Synchronized
    fun annehmen(agent: String, auftrag: String, ablage: Boolean = false, gespraech: String = "", pdf: Boolean = false, reserviere: () -> Boolean): Annahme {
        val l = aufgeraeumt()
        val jetzt = uhr()
        val schluessel = normal(agent) + "|" + normal(auftrag)
        l.lastOrNull { normal(it.agent) + "|" + normal(it.auftrag) == schluessel && (it.offen || (it.stand != Vorgang.FEHLGESCHLAGEN && it.stand != Vorgang.ABGEBROCHEN && jetzt - it.erstellt < DOPPELT_MS)) }
            ?.let { return Annahme.Doppelt(it) }
        if (!reserviere()) return Annahme.KeinPlatz
        val v = Vorgang(neueId(l), agent, auftrag.trim(), jetzt, Vorgang.ANGENOMMEN, ablage = ablage, gespraech = gespraech, pdf = pdf)
        l += v
        schreibe()
        melde("Vorgang ${v.id} angenommen", agent)
        return Annahme.Neu(v)
    }

    /** Ändert einen Eintrag. Ein beendeter Eintrag bleibt beendet (ein spätes „läuft“ überschreibt kein „fertig“). */
    @Synchronized
    fun aendere(id: String, aenderung: (Vorgang) -> Vorgang): Vorgang? {
        val l = alle()
        val i = l.indexOfFirst { it.id.equals(id, ignoreCase = true) }.takeIf { it >= 0 } ?: return null
        val alt = l[i]
        val neu = aenderung(alt).let { if (!alt.offen && it.offen) alt else it }
        if (neu == alt) return alt
        val jetzt = uhr()
        l[i] = neu.copy(geaendert = jetzt)
        schreibe()
        if (neu.stand != alt.stand) {
            val dauer = if (neu.offen) "" else " nach ${(jetzt - alt.erstellt) / 1000} s"
            melde("Vorgang $id: ${alt.stand} → ${neu.stand}$dauer", neu.grund.take(200))
        }
        return l[i]
    }

    fun gestartet(id: String) = aendere(id) { it.copy(stand = Vorgang.LAEUFT, gestartet = uhr()) }
    fun schritt(id: String, text: String) = aendere(id) { if (it.offen) it.copy(stand = Vorgang.LAEUFT, schritt = text.take(160)) else it }
    fun beende(id: String, stand: String, ergebnis: String?, grund: String?, vorlaeufig: String? = null) =
        aendere(id) { if (it.offen) it.copy(stand = stand, ergebnis = ergebnis.orEmpty(), grund = grund.orEmpty(), beendet = uhr(), schritt = "", vorlaeufig = vorlaeufig.orEmpty()) else it }

    @Synchronized
    fun abgerufen(ids: Collection<String>) { val jetzt = uhr(); ids.forEach { id -> aendere(id) { if (!it.offen && it.abgerufen == 0L) it.copy(abgerufen = jetzt) else it } } }

    /** Bestätigt, dass Frank das Ergebnis erfahren hat. Rückgabe: die tatsächlich bestätigten (nur beendete). */
    @Synchronized
    fun mitgeteilt(ids: Collection<String>): List<Vorgang> { val jetzt = uhr(); return ids.mapNotNull { id -> aendere(id) { if (!it.offen && it.mitgeteilt == 0L) it.copy(mitgeteilt = jetzt) else it }?.takeIf { !it.offen } } }

    @Synchronized
    fun finde(id: String): Vorgang? = aufgeraeumt().firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

    /** Offene und neu beendete Vorgänge (neueste zuerst); mit [alle] auch die schon mitgeteilten der letzten [TAGE] Tage. */
    @Synchronized
    fun liste(alle: Boolean = false): List<Vorgang> {
        val grenze = uhr() - TAGE * 24 * 3_600_000L
        return aufgeraeumt().filter { it.offen || (it.geaendert >= grenze && (alle || it.neu)) }.sortedByDescending { it.erstellt }
    }

    /** Ein angenommener Auftrag, dessen Lauf nie begann (Dienst startete nicht), gilt nach [START_MS] als gescheitert. */
    private fun aufgeraeumt(): MutableList<Vorgang> {
        val l = alle()
        val jetzt = uhr()
        l.filter { (it.stand == Vorgang.ANGENOMMEN && jetzt - it.erstellt > START_MS) || (it.stand == Vorgang.LAEUFT && jetzt - it.geaendert > STILL_MS) }.forEach { v ->
            aendere(v.id) { it.copy(stand = Vorgang.FEHLGESCHLAGEN, grund = if (it.stand == Vorgang.ANGENOMMEN) "Der Agent ist nicht angelaufen" else "Seit über ${STILL_MS / 60_000} Minuten keine Rückmeldung", beendet = jetzt) }
        }
        return l
    }

    private fun neueId(l: List<Vorgang>): String {
        val zeichen = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val zufall = java.security.SecureRandom()
        while (true) {
            val id = "V" + (1..4).map { zeichen[zufall.nextInt(zeichen.length)] }.joinToString("")
            if (l.none { it.id == id }) return id
        }
    }

    companion object {
        const val HOECHSTENS = 60
        const val TAGE = 7L
        const val DOPPELT_MS = 10 * 60_000L
        const val START_MS = 3 * 60_000L
        /** Ein Agent arbeitet höchstens 25 Minuten und meldet jeden Schritt; so lange Stille heißt: der Lauf ist tot. */
        const val STILL_MS = 40 * 60_000L
        fun normal(text: String): String = text.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim()
        private val ZEIT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M. HH:mm", Locale.GERMAN)
        fun zeit(millis: Long): String = ZEIT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }
}

/** Das Vorgangsbuch der App (`filesDir/vorgaenge.json`). */
object Vorgaenge {
    @Volatile private var buch: VorgangsBuch? = null

    fun buch(context: Context): VorgangsBuch = buch ?: synchronized(this) {
        buch ?: VorgangsBuch(File(context.applicationContext.filesDir, "vorgaenge.json"), melde = { was, details -> Logbuch.system(was, details) }).also { buch = it }
    }

    /** Eine Zeile für Listen: Kennung, Agent, Stand, Auftrag kurz, Zeit, Ergebnis oder Grund. */
    fun zeile(v: Vorgang): String = buildString {
        append("[").append(v.id).append("] ").append(v.agent).append(": ").append(Vorgang.standText(v.stand))
        if (v.offen && v.schritt.isNotEmpty()) append(" (").append(v.schritt).append(")")
        append(" · „").append(v.auftrag.replace(Regex("\\s+"), " ").take(90)).append("“ · angenommen ").append(VorgangsBuch.zeit(v.erstellt))
        if (v.beendet > 0) append(", beendet ").append(VorgangsBuch.zeit(v.beendet))
        if (v.ergebnis.isNotEmpty()) append(" · Ergebnis in der Ablage: „").append(v.ergebnis).append("“")
        else if (v.vorlaeufig.isNotEmpty()) append(" · Ergebnis bereit (NICHT in der Ablage; Frank wollte keine Ablage").append(if (v.gespraech.isNotEmpty()) ", steht im Jarvis-Chat" else "").append(")")
        if (v.grund.isNotEmpty()) append(" · ").append(if (v.stand == Vorgang.FERTIG) "Hinweis: " else "Grund: ").append(v.grund.take(200))
        if (!v.offen) append(if (v.mitgeteilt > 0) " · Frank mitgeteilt" else if (v.abgerufen > 0) " · abgerufen, noch nicht als mitgeteilt bestätigt" else " · Frank noch nicht mitgeteilt")
    }
}
