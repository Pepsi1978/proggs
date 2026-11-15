package de.frank.jarvis.gedaechtnis

import android.content.Context
import android.util.Log
import de.frank.jarvis.faehigkeit.Entscheidungen
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.Faehigkeit
import de.frank.jarvis.faehigkeit.Programmierregeln
import de.frank.jarvis.faehigkeit.Regeln
import de.frank.jarvis.faehigkeit.StilRegeln
import de.frank.jarvis.faehigkeit.UeberFrank
import de.frank.jarvis.faehigkeit.Werkzeug
import de.frank.jarvis.faehigkeit.Ziele
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Das Live-Gedächtnis als Verzeichnis: Für jede Frage in Jarvis' eigenem Chat und im Sprachgespräch wird nur geladen, was
 * die Frage braucht, statt des ganzen Gesamtbilds. Die Auswahl läuft lokal in Millisekunden, ohne eigenen Modellaufruf:
 *
 * 1. IMMER: Franks Regeln, Stil, Ziele, Entscheidungen und die kleinen Lagebereiche (Dienst, Termine, Erinnerungen …).
 * 2. Stichworte je Bereich ([profil]) und von Jarvis gelernte Wörter ([gelernt]) wählen die passenden Bereiche.
 * 3. Passt nichts Bestimmtes (Plaudern, „was soll ich jetzt tun?“), kommt der STANDARD-Satz dazu.
 * 4. Alles Übrige steht als Verzeichnis mit Kennung und Größe im Kontext; das Modell holt es mit `gedaechtnis_laden` nach.
 * 5. Lernen: Was das Modell nachladen musste, wird für die Wörter dieser Frage künftig vorab geladen. Was über gelernte Wörter
 *    geladen, laut Modell (`genutzt` im Antwort-JSON) aber nicht gebraucht wurde, verliert an Gewicht. Immer-Teile nie.
 * „Benutze dein gesamtes Live-Gedächtnis“ ([GANZ]) lädt wie früher alles.
 *
 * Neue Bereiche („Garage“): ein Eintrag im Enum [Bereich] und ein `when`-Zweig in [profil]. Der Compiler verlangt den Zweig,
 * ein neuer Bereich kann also nicht vergessen werden; ohne Stichworte steht er im Verzeichnis und lernt seine Wörter selbst.
 * Der MCP-Weg (jarvis_kontext für ChatGPT und Programmier-Sitzungen) bleibt davon unberührt.
 */
object KontextWahl {
    private const val TAG = "JarvisKontextWahl"

    enum class Rolle { IMMER, STANDARD, WAHL }

    class Profil(val rolle: Rolle, val stichworte: List<String> = emptyList(), val begleiter: List<Bereich> = emptyList())

    /** Jeder Bereich des Live-Gedächtnisses mit Rolle und Stichworten. Kleinbuchstaben; „=“ am Ende heißt ganzes Wort. */
    fun profil(b: Bereich): Profil = when (b) {
        Bereich.GEMERKT -> Profil(Rolle.IMMER)
        Bereich.RAHMEN -> Profil(Rolle.IMMER)
        Bereich.TERMINE -> Profil(Rolle.IMMER)
        Bereich.ERINNERUNGEN -> Profil(Rolle.IMMER)
        Bereich.LISTEN -> Profil(Rolle.IMMER)
        Bereich.VERAENDERUNGEN -> Profil(Rolle.IMMER)
        Bereich.WECKER -> Profil(Rolle.IMMER)
        Bereich.AUFGABEN -> Profil(Rolle.STANDARD, listOf("aufgabe", "todo", "to-do", "erledig", "abhak", "hake ", "entropie", "priorität", "anstehen", "steht an", "planen", "planung"), listOf(Bereich.ZIELE))
        Bereich.AUSWERTUNG -> Profil(Rolle.STANDARD, listOf("auswertung", "guten morgen", "wie geht es mir", "wie geht's mir", "einschätzung", "tagesplan", "wie war mein tag"))
        Bereich.GESPRAECHE -> Profil(Rolle.STANDARD, listOf("gespräch", "gesprochen", "vorhin", "letztens", "worüber", "besprochen", "erzählt", "unterhalten", "hatten wir"))
        Bereich.ZIELE -> Profil(Rolle.STANDARD, listOf("ziel", "fortschritt", "vorsatz", "vorsätze"))
        Bereich.STACKS -> Profil(Rolle.WAHL, listOf("stack", "nahrungserg", "supplement", "vitamin", "mineral", "magnesium", "omega", "kreatin", "zink", "einnahme", "einnehm", "kapsel", "dosis", "präparat", "mittel="))
        Bereich.BIODATEN -> Profil(Rolle.WAHL, listOf("schlaf", "geschlafen", "hrv", "herzraten", "erholung", "erholt", "recovery", "ruhepuls", "puls", "whoop", "oura", "gewicht", "waage", "wiege", "körperfett", "biomarker", "biodaten", "fitness", "vo2", "strain", "belast", "müde", "energie", "gesundheit", "temperatur", "blutdruck", "krank", "körper"), listOf(Bereich.TRAININGS, Bereich.AUSWERTUNG))
        Bereich.TRAININGS -> Profil(Rolle.WAHL, listOf("training", "trainier", "lauf=", "laufen=", "laufrunde", "gelaufen", "joggen", "sport", "kilometer", "pace", "intervall", "workout", "radfahr", "schwimm", "waldlauf", "traillauf"), listOf(Bereich.BIODATEN, Bereich.WETTER))
        Bereich.KONTINGENTE -> Profil(Rolle.WAHL, listOf("kontingent", "limit", "token", "guthaben", "codex", "chatgpt", "claude", "anthropic", "opus", "sonnet", "gemini", "kosten"))
        Bereich.WETTER -> Profil(Rolle.WAHL, listOf("wetter", "regen=", "regnen", "regnet", "sonne", "sonnig", "grad=", "draußen", "jacke", "schirm", "schnee", "wind=", "gewitter", "spazier", "frost", "glätte"))
        Bereich.RUECKBLICK -> Profil(Rolle.WAHL, listOf("letzte tage", "letzten tage", "letzte woche", "letzten woche", "rückblick", "gestern", "vorgestern", "vergangen"))
        Bereich.ABLAGE -> Profil(Rolle.WAHL, listOf("ablage", "datei", "dokument", "pdf", "bild=", "bilder", "foto", "gespeichert", "anhang"))
        Bereich.MAIL -> Profil(Rolle.WAHL, listOf("mail=", "mails", "e-mail", "email", "posteingang", "newsletter", "bestellung", "post="))
        Bereich.WISSEN -> Profil(Rolle.WAHL, listOf("wissen", "datenbank", "artikel", "nachschlag", "studie"))
        Bereich.IDEEN -> Profil(Rolle.WAHL, listOf("idee"))
        Bereich.TAGEBUCH -> Profil(Rolle.WAHL, listOf("tagebuch", "journal", "eintrag", "einträge", "notiert", "gefühlt", "stimmung", "reflexion", "gedanken"))
    }

    /** Teile des Gemerkten, die nur bei Bedarf dazukommen (Regeln, Stil, Ziele und Entscheidungen stehen immer darin). */
    enum class Merkteil(val kennung: String, val titel: String, val rolle: Rolle, val stichworte: List<String>, val text: (Context) -> String) {
        UEBER_FRANK("ueber_frank", "Über Frank", Rolle.STANDARD,
            listOf("über mich", "weißt du", "kennst du", "mein auto", "meine familie", "vorliebe", "lieblings", "geburtstag", "adresse", "wohne", "ich mag", "mein haus", "meine wohnung"),
            { UeberFrank.alsKontext(it) }),
        LANGZEIT("langzeit", "Langzeitkontext", Rolle.STANDARD,
            listOf("über mich", "langzeit", "lebenslauf", "lebensziel", "mein leben", "biografie"),
            { Langzeitkontext.alsKontext(it) }),
        PROGRAMMIERREGELN("programmierregeln", "Programmierregeln", Rolle.WAHL,
            listOf("programmier", "programm=", "entwickler", "code=", "coden", "bug", "commit", "claude code", "nachtrag", "übergib", "implement", "feature", "repo=", "repository", "android", "kotlin", "swift",
                "app=", "session", "sitzung", "bauen", "einbauen", "baue ", "funktion in", "fehler in"),
            { Programmierregeln.alsKontext(it) }),
    }

    /** Der immer geladene Kern des Gemerkten. Ohne Symbiose: Sie regelt nur ChatGPT, Jarvis' eigenes Modell braucht sie nicht. */
    private fun kernGemerkt(context: Context): String =
        listOf(Regeln, StilRegeln, Ziele, Entscheidungen).joinToString("") { it.alsKontext(context) }

    /** Ein Teil des Gemerkten zu seiner Kennung. */
    fun merkteil(kennung: String): Merkteil? = Merkteil.entries.firstOrNull { it.kennung == kennung }

    /** Gibt es diese Kennung (Bereich außer GEMERKT oder Teil des Gemerkten)? Nur solche lernt und lädt Jarvis. */
    private fun gueltig(kennung: String): Boolean = merkteil(kennung) != null || Bereich.von(kennung)?.let { it != Bereich.GEMERKT } == true

    /** „Benutze dein gesamtes Live-Gedächtnis“ und ähnlich (auch „Life-Gedächtnis“ aus der Mitschrift): dann wie früher alles. */
    private val GANZ = Regex(
        "(gesamte[nmsr]?|ganze[nmsr]?|komplette[nmsr]?|volle[nmsr]?|vollständige[nmsr]?)\\s+((live|life|leif|laiv)[- ]?)?(gedächtnis|gesamtbild)" +
            "|(gesamte[nmsr]?|ganze[nmsr]?|komplette[nmsr]?|volle[nmsr]?)\\s+(live|life|leif|laiv)[- ]?kontext|alles,? was du (über mich )?weißt|alles aus deinem gedächtnis",
        RegexOption.IGNORE_CASE,
    )
    /** Klare Übergabe an den Programmierer: dann nur Immer-Kern und Programmierregeln, egal welche Fachwörter im Auftrag stehen. */
    private val UEBERGABE = Regex(
        "(an|für) den programmierer|zum programmierer|programmier(auftrag|nachtrag)|lass (das|es|ihn) programmieren|programmier(e|t)? (das|mir)|übergib .{0,40}programmier|schick .{0,60}programmier",
        RegexOption.IGNORE_CASE,
    )
    /**
     * Ab diesem Gewicht eines einzelnen Wortes bleibt ein Bereich weg: Er war bei Fragen mit diesem Wort mindestens dreimal
     * unnötig. Gilt nur für Bereiche aus dem Standard-Satz oder als Begleiter, nie für direkt genannte oder im Gespräch nachgeladene.
     */
    private const val WEGLASSEN = -3

    /** Allgemeine Lagefragen ohne Fachwort: dann kommt der Standard-Satz dazu. */
    private val LAGE = listOf("sinnvoll", "empfiehl", "empfehl", "vorschlag", "was soll ich", "was mache ich", "wie sieht mein", "überblick", "meine lage", "was steht an", "was liegt an", "tipp=", "tipps", "wie geht es mir")

    private val STOPP = setOf(
        "bitte", "kannst", "könntest", "würdest", "mal", "dass", "diese", "dieser", "dieses", "einen", "einem", "einer", "eine", "nicht", "noch", "schon", "wieder",
        "jarvis", "danke", "genau", "einfach", "etwas", "irgendwie", "vielleicht", "wirklich", "immer", "heute", "morgen", "gerade", "machen", "haben", "werden",
        "sollen", "wollen", "können", "müssen", "welche", "welcher", "welches", "warum", "wieso", "weshalb", "worum", "darüber", "davon", "dafür", "damit",
        "meine", "meinen", "meinem", "meiner", "deine", "deinen", "deinem", "unsere", "sagen", "zeigen", "schau", "schauen", "nochmal", "ungefähr", "anders",
        "sowas", "sodass", "wenn", "aber", "oder", "und", "auch", "über", "unter", "nach", "zwischen", "letzte", "letzten", "ersten", "neuen", "neue",
        "jetzt", "brauche", "brauchst", "einmal", "sollte", "solltest", "zuerst", "vorher", "nachher", "später", "eigentlich", "trotzdem", "allerdings",
        "gerne", "denke", "glaube", "meinst", "hätte", "wäre", "würde", "könnte", "müsste", "dürfte", "bisschen", "wenig", "viele", "vieles", "alles", "allem",
        "andere", "anderen", "weiter", "weitere", "gleich", "ganze", "ganzen", "dieser", "jemand", "nichts", "deshalb", "darum", "dabei", "dazu", "seine", "ihrer",
    )

    /** Die Auswahl für eine Frage. [gelernteTreffer] = Kennung → Wörter, die sie über das Gelernte gewählt haben (für das Abstufen). */
    data class Wahl(
        val voll: Boolean,
        val bereiche: Set<Bereich>,
        val teile: Set<Merkteil>,
        val gelernteTreffer: Map<String, Set<String>>,
        val woerter: List<String>,
        val grund: String,
        /** Direkt genannt oder im Gespräch schon nachgeladen: Diese Kennungen stuft das Lernen nicht ab. */
        val geschuetzt: Set<String> = emptySet(),
    ) {
        val kennungen: Set<String> get() = bereiche.mapTo(LinkedHashSet()) { it.kennung } + teile.map { it.kennung }
        /** Für die Anzeige unter der Antwort: die Titel der geladenen Bereiche (ohne die kleinen Immer-Bereiche). */
        fun titel(): List<String> = if (voll) listOf("gesamtes Live-Gedächtnis") else
            listOf("Gemerktes") + bereiche.filter { profil(it).rolle != Rolle.IMMER }.sortedBy { it.ordinal }.map { it.titel } + teile.sortedBy { it.ordinal }.map { it.titel }
    }

    // ---------------------------------------------------------------- Gelerntes

    private val ablauf = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sperre = Any()
    private val dateiSperre = Any()
    /** Wort → (Kennung → Gewicht). Ab [SCHWELLE] lädt das Wort den Bereich vorab, ab [WEGLASSEN] bleibt er weg. */
    @Volatile private var gelernt: MutableMap<String, MutableMap<String, Int>>? = null
    /** Je Gespräch, was das Modell schon nachgeladen hat: Eine Anschlussfrage bekommt es gleich mit. */
    private val nachgeladenJeGespraech = object : LinkedHashMap<String, MutableSet<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MutableSet<String>>?) = size > 30
    }
    /** Zweimal nachgeladen bei Fragen mit diesem Wort, dann lädt es den Bereich künftig vorab (einmal ist Zufall). */
    private const val SCHWELLE = 2
    private const val HOECHSTGEWICHT = 10
    private const val HOECHSTENS_WOERTER = 3_000

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "kontextwahl.json")

    private fun gelernt(context: Context): MutableMap<String, MutableMap<String, Int>> = gelernt ?: synchronized(sperre) {
        gelernt ?: runCatching {
            val j = JSONObject(datei(context).readText()).optJSONObject("woerter") ?: JSONObject()
            val m = HashMap<String, MutableMap<String, Int>>()
            j.keys().forEach { w ->
                val b = j.getJSONObject(w)
                m[w] = HashMap<String, Int>().also { z -> b.keys().forEach { k -> if (gueltig(k)) z[k] = b.optInt(k) } }
            }
            m as MutableMap<String, MutableMap<String, Int>>
        }.getOrElse { HashMap() }.also { gelernt = it }
    }

    /** Vorab im Hintergrund laden, damit die erste Frage nach dem Start nicht auf die Datei wartet. */
    fun vorwaermen(context: Context) { val app = context.applicationContext; if (gelernt == null) ablauf.launch { gelernt(app) } }

    private fun speichere(context: Context) {
        val g = gelernt ?: return
        // Eine Sperre für Schnappschuss und Schreiben: Zwei gleichzeitige Lernläufe schrieben sonst dieselbe .neu-Datei.
        synchronized(dateiSperre) {
            val json = synchronized(sperre) {
                JSONObject().put("format", 1).put("woerter", JSONObject().apply { g.forEach { (w, m) -> put(w, JSONObject(m as Map<*, *>)) } }).toString()
            }
            runCatching {
                val f = datei(context)
                val neu = File(f.path + ".neu")
                neu.writeText(json)
                if (!neu.renameTo(f)) { f.writeText(json); neu.delete() }
            }.onFailure { Log.w(TAG, "Kontextwahl nicht gespeichert", it) }
        }
    }

    /** Die bedeutsamen Wörter einer Frage (klein, ab fünf Buchstaben, ohne Füllwörter). */
    fun woerter(text: String): List<String> =
        Regex("[\\p{L}][\\p{L}-]{4,}").findAll(text.lowercase(Locale.GERMAN)).map { it.value.trim('-') }
            .filter { it.length >= 5 && it !in STOPP }.distinct().take(16).toList()

    /** Kurze und Ganzwort-Stichworte als Regex, einmal gebaut. */
    private val muster = java.util.concurrent.ConcurrentHashMap<String, Regex>()

    private fun trifft(text: String, stichwort: String): Boolean {
        if (stichwort.endsWith("=")) return muster.getOrPut(stichwort) { Regex("(?<![\\p{L}])" + Regex.escape(stichwort.dropLast(1)) + "(?![\\p{L}])") }.containsMatchIn(text)
        // Lange Stichworte auch mitten in zusammengesetzten Wörtern („Lauftraining“), kurze nur am Wortanfang („lauf“ nicht in „Verlauf“).
        if (stichwort.length >= 6 || stichwort.contains(' ')) return text.contains(stichwort)
        return muster.getOrPut(stichwort) { Regex("(?<![\\p{L}])" + Regex.escape(stichwort)) }.containsMatchIn(text)
    }

    // ---------------------------------------------------------------- Auswahl

    /**
     * Wählt die Bereiche für [frage], nur Franks eigene Worte (ohne Anhänge und Hinweise der App). [vorher] = seine vorige
     * Nachricht (Anschlussfragen wie „und morgen?“ erben so deren Fachwörter). [gespraech] = Kennung des Gesprächs für das
     * bereits Nachgeladene. [programmierung] = die App weiß schon, dass es ein Programmierauftrag ist.
     */
    fun waehle(context: Context, frage: String, vorher: String?, gespraech: String?, programmierung: Boolean = false): Wahl {
        if (GANZ.containsMatchIn(frage)) return Wahl(true, Bereich.entries.toSet(), Merkteil.entries.toSet(), emptyMap(), emptyList(), "gesamtes Gedächtnis verlangt")
        val jetzt = frage.lowercase(Locale.GERMAN)
        val immer = Bereich.entries.filter { profil(it).rolle == Rolle.IMMER }
        if (programmierung || UEBERGABE.containsMatchIn(jetzt)) return Wahl(false, immer.toSet(), setOf(Merkteil.PROGRAMMIERREGELN), emptyMap(), emptyList(), "Programmierauftrag")
        val davor = vorher.orEmpty().lowercase(Locale.GERMAN).take(3_000)
        val bereiche = LinkedHashSet<Bereich>(immer)
        val teile = LinkedHashSet<Merkteil>()
        val geschuetzt = HashSet<String>()
        val gruende = mutableListOf<String>()
        fun fach(text: String): Boolean {
            var gefunden = false
            Bereich.entries.forEach { b ->
                val p = profil(b)
                if (p.rolle != Rolle.IMMER && p.stichworte.any { trifft(text, it) }) { gefunden = true; bereiche += b; geschuetzt += b.kennung; bereiche += p.begleiter }
            }
            Merkteil.entries.forEach { t -> if (t.stichworte.any { trifft(text, it) }) { gefunden = true; teile += t; geschuetzt += t.kennung } }
            return gefunden
        }
        val eigen = fach(jetzt)
        if (eigen) gruende += "Stichworte"
        // Der Verlauf zählt nur, wenn die Frage selbst nichts Bestimmtes nennt (Anschlussfrage wie „und morgen?“), sonst nach Themenwechsel zu viel.
        if (!eigen && davor.isNotBlank() && fach(davor)) gruende += "Gesprächsverlauf"
        // Gelernt: Wörter, bei denen das Modell früher einen Bereich nachladen musste (nur gültige Kennungen außerhalb des Immer-Kerns).
        val worte = woerter(frage)
        val treffer = HashMap<String, MutableSet<String>>()
        val g = gelernt(context)
        synchronized(sperre) {
            worte.forEach { w -> g[w]?.forEach { (k, gewicht) -> if (gewicht >= SCHWELLE && gueltig(k) && Bereich.von(k)?.let { profil(it).rolle == Rolle.IMMER } != true) treffer.getOrPut(k) { HashSet() } += w } }
        }
        treffer.keys.forEach { k -> Bereich.von(k)?.let { bereiche += it } ?: merkteil(k)?.let { teile += it } }
        if (treffer.isNotEmpty()) gruende += "gelernt"
        gespraech?.let { id -> synchronized(nachgeladenJeGespraech) { nachgeladenJeGespraech[id]?.toList() }?.forEach { k ->
            geschuetzt += k
            Bereich.von(k)?.let { bereiche += it } ?: merkteil(k)?.let { teile += it }
        } }
        // Nichts Bestimmtes in der Frage selbst oder eine allgemeine Lagefrage: der Standard-Satz.
        if ((!eigen && treffer.isEmpty()) || LAGE.any { trifft(jetzt, it) }) {
            Bereich.entries.filter { profil(it).rolle == Rolle.STANDARD }.forEach { bereiche += it }
            Merkteil.entries.filter { it.rolle == Rolle.STANDARD }.forEach { teile += it }
            gruende += "Standard"
        }
        // Gelerntes Weglassen: nur Mitgenommenes (Standard, Begleiter), das bei einem Wort dieser Frage wiederholt unnötig war.
        val weg = HashSet<String>()
        synchronized(sperre) { worte.forEach { w -> g[w]?.forEach { (k, gewicht) -> if (gewicht <= WEGLASSEN && k !in geschuetzt && k !in treffer) weg += k } } }
        if (weg.isNotEmpty()) {
            bereiche.removeAll { it.kennung in weg && profil(it).rolle != Rolle.IMMER }
            teile.removeAll { it.kennung in weg }
            gruende += "gelernt weggelassen: " + weg.joinToString()
        }
        return Wahl(false, bereiche, teile, treffer, worte, gruende.joinToString(", "), geschuetzt)
    }

    /**
     * Nach der Antwort, im Hintergrund: [nachgeladen] = was das Modell mit gedaechtnis_laden holen musste, [genutzt] = was es
     * laut Antwort wirklich gebraucht hat (null = keine Angabe, dann wird nichts abgestuft).
     */
    fun lerne(context: Context, wahl: Wahl, gespraech: String?, nachgeladen: Set<String>, genutzt: Set<String>?) {
        if (wahl.voll) return
        val app = context.applicationContext
        val gueltigNachgeladen = nachgeladen.filterTo(LinkedHashSet()) { gueltig(it) }
        if (gespraech != null && gueltigNachgeladen.isNotEmpty()) synchronized(nachgeladenJeGespraech) { nachgeladenJeGespraech.getOrPut(gespraech) { HashSet() } += gueltigNachgeladen }
        if (wahl.woerter.isEmpty()) return
        val fehlten = gueltigNachgeladen - wahl.kennungen
        val unnoetig = if (genutzt == null) emptyMap() else wahl.gelernteTreffer.filterKeys { it !in genutzt }
        val bestaetigt = if (genutzt == null) emptyMap() else wahl.gelernteTreffer.filterKeys { it in genutzt }
        // Mitgenommenes (nicht direkt genannt, nicht im Gespräch nachgeladen), das nicht gebraucht wurde, verliert für die Wörter dieser Frage an Gewicht.
        val wahlweise = wahl.bereiche.filter { profil(it).rolle != Rolle.IMMER }.map { it.kennung } + wahl.teile.map { it.kennung }
        val ungebraucht = if (genutzt == null) emptyList() else wahlweise.filter { it !in genutzt && it !in wahl.gelernteTreffer && it !in wahl.geschuetzt }
        val gebraucht = if (genutzt == null) emptyList() else wahlweise.filter { it in genutzt }
        if (fehlten.isEmpty() && unnoetig.isEmpty() && bestaetigt.isEmpty() && ungebraucht.isEmpty() && gebraucht.isEmpty()) return
        ablauf.launch {
            val g = gelernt(app)
            synchronized(sperre) {
                fun aendere(w: String, k: String, um: Int) {
                    val m = g.getOrPut(w) { HashMap() }
                    val neu = ((m[k] ?: 0) + um).coerceIn(-HOECHSTGEWICHT, HOECHSTGEWICHT)
                    if (neu == 0) m.remove(k) else m[k] = neu
                    if (m.isEmpty()) g.remove(w)
                }
                fehlten.forEach { k -> wahl.woerter.forEach { w -> aendere(w, k, 1) } }
                unnoetig.forEach { (k, ws) -> ws.forEach { w -> aendere(w, k, -1) } }
                bestaetigt.forEach { (k, ws) -> ws.forEach { w -> aendere(w, k, 1) } }
                ungebraucht.forEach { k -> wahl.woerter.forEach { w -> aendere(w, k, -1) } }
                // Doch gebraucht: ein früheres Weglass-Gewicht wieder abbauen (nie über 0 hinaus, Vorabladen lernt nur das Nachladen).
                gebraucht.forEach { k -> wahl.woerter.forEach { w -> if ((g[w]?.get(k) ?: 0) < 0) aendere(w, k, 1) } }
                if (g.size > HOECHSTENS_WOERTER) g.keys.sortedBy { w -> g[w]?.values?.sumOf { kotlin.math.abs(it) } ?: 0 }.take(g.size - HOECHSTENS_WOERTER).forEach { g.remove(it) }
            }
            speichere(app)
            Logbuch.gedaechtnis("Kontextwahl gelernt", "fehlten ${fehlten.joinToString().ifEmpty { "–" }}; unnötig ${(unnoetig.keys + ungebraucht).joinToString().ifEmpty { "–" }}; bestätigt ${bestaetigt.keys.joinToString().ifEmpty { "–" }}")
        }
    }

    // ---------------------------------------------------------------- Kontext und Nachladen

    /**
     * Das Gesamtbild für [wahl]. Vor dem [trenner] (zwischengespeichert) nur der Kern des Gemerkten, der bei jeder Frage
     * gleich ist; gewählte Teile des Gemerkten stehen dahinter, sonst wechselte der Zwischenspeicher bei jeder Frage.
     */
    suspend fun kontext(context: Context, wahl: Wahl, wartenMs: Long, trenner: String): String {
        val app = context.applicationContext
        val teile = wahl.teile.sortedBy { it.ordinal }.joinToString("") { it.text(app) }
        val verzeichnis = Merkteil.entries.filter { it !in wahl.teile }.map { t -> "- ${t.kennung}: ${t.titel} (aus dem Gemerkten)" }
        return Lagebild.gezielt(app, wahl.bereiche, kernGemerkt(app), teile, verzeichnis, wahl.kennungen, wartenMs, trenner)
    }

    /** Der lesbare Titel zu einer Kennung (Bereich oder Teil des Gemerkten), sonst die Kennung selbst. */
    fun titelVon(kennung: String): String = Bereich.von(kennung)?.titel ?: merkteil(kennung)?.titel ?: kennung

    /** Die gültigen Kennungen in [kennungen], klein geschrieben. */
    fun gueltige(kennungen: Collection<String>): Set<String> = kennungen.map { it.trim().lowercase(Locale.GERMAN) }.filterTo(LinkedHashSet()) { gueltig(it) }

    /** Lädt Bereiche oder Teile des Gemerkten vollständig nach (für das Werkzeug gedaechtnis_laden). */
    suspend fun lade(context: Context, kennungen: List<String>): String {
        val app = context.applicationContext
        val roh = kennungen.map { it.trim().lowercase(Locale.GERMAN) }.filter { it.isNotEmpty() }.distinct()
        val sauber = roh.filter { gueltig(it) }
        val unbekannt = roh - sauber.toSet()
        val teile = sauber.mapNotNull { merkteil(it) }
        val rest = sauber.filter { merkteil(it) == null }
        val moeglich = (Bereich.entries.filter { it != Bereich.GEMERKT }.map { it.kennung } + Merkteil.entries.map { it.kennung }).joinToString()
        return buildString {
            teile.forEach { append(it.text(app).ifBlank { "${it.titel}: (leer)\n" }).append('\n') }
            if (rest.isNotEmpty()) append(Lagebild.bereiche(app, rest)).append('\n')
            if (unbekannt.isNotEmpty()) append("\nUnbekannte Kennungen: ").append(unbekannt.joinToString()).append(". Möglich: ").append(moeglich).append('.')
        }.trim().ifEmpty { "Keine Kennung angegeben. Möglich: $moeglich." }
    }

    /**
     * Das Werkzeug zum Nachladen, nur für Jarvis' eigenes Modell (nicht im Register, also nie im Plugin).
     * [beiLaden] bekommt die geladenen Kennungen und die Zeichenzahl (für Lernen und Tokenanzeige).
     */
    fun faehigkeit(context: Context, beiLaden: (Set<String>, Int) -> Unit): Faehigkeit = object : Faehigkeit {
        override val id = "gedaechtnis_wahl"
        override val name = "Live-Gedächtnis (Verzeichnis)"
        override val beschreibung = "Lädt weitere Bereiche des Live-Gedächtnisses nach."
        override val hinweise = "Das Live-Gedächtnis ist für jede Frage gezielt geladen (GELADEN im Gesamtbild); alles andere steht unter WEITERE BEREICHE mit Kennung. " +
            "Braucht die Frage etwas daraus, lade es mit gedaechtnis_laden, bevor du antwortest; rate nie, was in einem nicht geladenen Bereich steht. " +
            "Lade nur, was die Frage wirklich braucht. Bei der Antwort an Frank beginnt das JSON immer mit \"antwort\"; dahinter hängst du das Feld \"genutzt\" an: die Kennungen der " +
            "geladenen Gedächtnis-Bereiche, die du für diese Antwort tatsächlich gebraucht hast, etwa {\"antwort\": \"…\", \"genutzt\": [\"biodaten\", \"wetter\"]}; keine: []."
        override val imPlugin = false
        override val nurImGespraech = true
        override fun stoerung(): String? = null
        override val werkzeuge = listOf(
            Werkzeug(
                name = "gedaechtnis_laden",
                titel = "Live-Gedächtnis nachladen",
                beschreibung = "Lädt Bereiche des Live-Gedächtnisses vollständig, die für diese Frage nicht vorab geladen sind (Kennungen aus WEITERE BEREICHE). Mehrere auf einmal möglich.",
                schema = JSONObject().put("type", "object").put("required", JSONArray().put("bereiche")).put("properties", JSONObject()
                    .put("bereiche", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")).put("description", "Kennungen, z. B. [\"tagebuch\", \"biodaten\"]"))),
                nurLesen = true,
                taetigkeit = "schaut in seinem Gedächtnis nach",
            ) { a ->
                val liste = a.optJSONArray("bereiche")?.let { arr -> (0 until arr.length()).map { arr.optString(it) } } ?: listOfNotNull(a.optString("bereich").takeIf { it.isNotBlank() })
                val text = lade(context, liste)
                beiLaden(gueltige(liste), text.length)
                Ergebnis(text)
            },
        )
    }
}
