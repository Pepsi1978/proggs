package de.frank.jarvis.listen

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Collator
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

/** Eine Liste wie in TickTick: Name, Art, Farbe und eigene Ansicht (Sortierung, Gruppierung, Erledigte zeigen). */
data class Liste(
    val id: Long,
    val name: String,
    val art: String = ListenArt.SONSTIGES.kennung,
    val farbe: Int = 0,
    val sortierung: String = Sortierung.MANUELL.kennung,
    val gruppierung: String = Gruppierung.KEINE.kennung,
    val erledigteZeigen: Boolean = true,
    val pos: Double = 0.0,
    val erstellt: Long = System.currentTimeMillis(),
    val geaendert: Long = System.currentTimeMillis(),
    val geloescht: Boolean = false,
    /** Eigene Abschnitte in ihrer Reihenfolge (TickTick „Sections“), etwa „Obst“, „Drogerie“. */
    val abschnitte: List<String> = emptyList(),
)

/** Ein Eintrag; [eltern] = 0 heißt oberste Ebene, sonst ist er Unterpunkt dieses Eintrags. */
data class Eintrag(
    val id: Long,
    val liste: Long,
    val titel: String,
    val eltern: Long = 0L,
    val notiz: String = "",
    /** 0 keine, 1 niedrig, 2 mittel, 3 hoch (wie TickTick). */
    val prioritaet: Int = 0,
    /** JJJJ-MM-TT oder leer. */
    val faellig: String = "",
    val tags: List<String> = emptyList(),
    val erledigt: Boolean = false,
    val erledigtAm: Long = 0L,
    val pos: Double = 0.0,
    val erstellt: Long = System.currentTimeMillis(),
    val geaendert: Long = System.currentTimeMillis(),
    val geloescht: Boolean = false,
    /** Angeheftet: steht immer oben, egal wie sortiert wird. */
    val angeheftet: Boolean = false,
    /** Name des Abschnitts (nur oberste Ebene), leer = ohne Abschnitt. */
    val abschnitt: String = "",
)

enum class ListenArt(val kennung: String, val anzeige: String, val symbol: String) {
    EINKAUF("einkauf", "Einkauf", "🛒"), AUFGABEN("aufgaben", "Aufgaben", "✅"), CHECKLISTE("checkliste", "Checkliste", "📋"),
    PACKLISTE("packliste", "Packliste", "🎒"), IDEEN("ideen", "Ideen", "💡"), SONSTIGES("sonstiges", "Sonstiges", "📝");
    companion object { fun von(k: String) = entries.firstOrNull { it.kennung == k } ?: SONSTIGES }
}

enum class Sortierung(val kennung: String, val anzeige: String) {
    MANUELL("manuell", "Eigene Reihenfolge"), FAELLIG("faellig", "Fälligkeit"), PRIORITAET("prioritaet", "Priorität"),
    TITEL("titel", "Titel A–Z"), TAG("tag", "Tag"), ERSTELLT("erstellt", "Neueste zuerst"), AELTESTE("aelteste", "Älteste zuerst"),
    GEAENDERT("geaendert", "Zuletzt geändert");
    companion object { fun von(k: String) = entries.firstOrNull { it.kennung == k } ?: MANUELL }
}

enum class Gruppierung(val kennung: String, val anzeige: String) {
    KEINE("keine", "Keine"), ABSCHNITT("abschnitt", "Abschnitte"), PRIORITAET("prioritaet", "Priorität"), FAELLIG("faellig", "Fälligkeit"),
    TAG("tag", "Tag"), ERSTELLT("erstellt", "Erstellt am");
    companion object { fun von(k: String) = entries.firstOrNull { it.kennung == k } ?: KEINE }
}

object Prioritaet {
    val namen = listOf("keine", "niedrig", "mittel", "hoch")
    fun name(p: Int) = namen.getOrElse(p) { "keine" }
    fun von(text: String): Int? = when (text.trim().lowercase(Locale.GERMAN)) {
        "", "keine", "ohne", "0" -> 0
        "niedrig", "gering", "low", "1" -> 1
        "mittel", "normal", "medium", "2" -> 2
        "hoch", "wichtig", "dringend", "high", "3" -> 3
        else -> null
    }
}

/** Eine Zeile der sortierten Ansicht: Gruppenkopf ([kopf]) oder Eintrag mit Einrücktiefe und Fortschritt seiner Unterpunkte. */
data class Zeile(val eintrag: Eintrag? = null, val tiefe: Int = 0, val kinderErledigt: Int = 0, val kinderGesamt: Int = 0, val kopf: String? = null, val kopfAnzahl: Int = 0)

data class ListenStand(val listen: List<Liste> = emptyList(), val eintraege: List<Eintrag> = emptyList()) {
    fun eintraegeVon(liste: Long) = eintraege.filter { it.liste == liste }
    fun offen(liste: Long) = eintraege.count { it.liste == liste && !it.erledigt }
}

/**
 * Franks Listen in Jarvis: je Liste und je Eintrag eine kleine JSON-Datei unter `filesDir/listen/` (`l_<id>.json`, `e_<id>.json`).
 * So gleicht [de.frank.jarvis.abgleich.GedaechtnisAbgleich] Handy und Tablet je Eintrag ab: Fügt das Tablet etwas hinzu, während das
 * Handy abhakt, geht nichts verloren. Gelöscht wird nie die Datei, sondern als Grabstein markiert (sonst holte der Abgleich sie zurück).
 *
 * Jeder Schreiber (Oberfläche, Werkzeuge, Widget, Abgleich) geht durch [schreibe]: Datei, [stand], Widget und Abgleich in einem Zug.
 */
object Listen {
    const val ORDNER = "listen"
    /** Höchste Einrücktiefe (0 = oberste Ebene): fünf Ebenen wie in TickTick. */
    private const val MAX_TIEFE = 4
    private val sperre = Any()
    @Volatile private var geladen = false
    private val listen = HashMap<Long, Liste>()
    private val eintraege = HashMap<Long, Eintrag>()
    private val _stand = MutableStateFlow(ListenStand())
    val stand: StateFlow<ListenStand> = _stand.asStateFlow()

    private fun ordner(app: Context) = File(app.filesDir, ORDNER).apply { mkdirs() }

    fun neueId(): Long = System.currentTimeMillis() * 1000 + Random.nextInt(1000)

    /** Lädt beim ersten Gebrauch. Listen entstehen nur auf Wunsch (App, Sprache); „Füge … meiner Einkaufsliste hinzu“ legt sie bei Bedarf an. */
    fun sicher(context: Context): ListenStand {
        if (geladen) return _stand.value
        synchronized(sperre) {
            if (!geladen) {
                einlesen(context.applicationContext)
                geladen = true
            }
        }
        return _stand.value
    }

    /** Nach dem Abgleich: alles neu einlesen und das Widget nachziehen. */
    fun neuLaden(context: Context) {
        val app = context.applicationContext
        synchronized(sperre) { einlesen(app); geladen = true }
        ListenWidget.aktualisieren(app)
        gedaechtnisAuffrischen(app, "Geräteabgleich")
    }

    /** Live-Gedächtnis: Bereich „Listen“ neu lesen, damit „Was steht auf der Einkaufsliste?“ den Stand von eben kennt. */
    private fun gedaechtnisAuffrischen(app: Context, anlass: String) {
        runCatching { de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(de.frank.jarvis.gedaechtnis.Bereich.LISTEN), anlass) }
    }

    /** Für den Abgleich: Schreiben von außen unter derselben Sperre wie die eigenen Änderungen. */
    fun vonAussen(block: () -> Unit) = synchronized(sperre) { block() }

    private fun einlesen(app: Context) {
        listen.clear(); eintraege.clear()
        ordner(app).listFiles { f -> f.isFile && f.name.endsWith(".json") }?.forEach { f ->
            runCatching {
                val j = JSONObject(f.readText())
                when {
                    f.name.startsWith("l_") -> listeAus(j).let { listen[it.id] = it }
                    f.name.startsWith("e_") -> eintragAus(j).let { eintraege[it.id] = it }
                }
            }.onFailure { Logbuch.fehler("Listen", it, "${f.name} unlesbar") }
        }
        veroeffentlichen()
    }

    private fun veroeffentlichen() {
        val l = listen.values.filter { !it.geloescht }.sortedWith(compareBy({ it.pos }, { it.erstellt }))
        val ids = l.map { it.id }.toSet()
        _stand.value = ListenStand(l, eintraege.values.filter { !it.geloescht && it.liste in ids })
    }

    // ---------------------------------------------------------------- Schreiben

    private fun datei(app: Context, l: Liste) = File(ordner(app), "l_${l.id}.json")
    private fun datei(app: Context, e: Eintrag) = File(ordner(app), "e_${e.id}.json")

    private fun atomar(ziel: File, text: String) {
        val teil = File(ziel.path + ".teil")
        teil.writeText(text)
        if (!teil.renameTo(ziel)) { ziel.delete(); teil.renameTo(ziel) }
    }

    /** Ein Bedienschritt: [block] ändert Listen und Einträge über die übergebenen Funktionen; danach Dateien, Stand, Widget, Abgleich. */
    private fun <T> schreibe(context: Context, block: Schreiber.() -> T): T {
        val app = context.applicationContext
        sicher(app)
        val ergebnis: T
        synchronized(sperre) {
            val s = Schreiber()
            ergebnis = s.block()
            val jetzt = System.currentTimeMillis()
            s.listenNeu.values.forEach { l -> val n = l.copy(geaendert = jetzt); listen[n.id] = n; atomar(datei(app, n), listeJson(n).toString()) }
            s.eintraegeNeu.values.forEach { e -> val n = e.copy(geaendert = jetzt); eintraege[n.id] = n; atomar(datei(app, n), eintragJson(n).toString()) }
            veroeffentlichen()
        }
        ListenWidget.aktualisieren(app)
        runCatching { de.frank.jarvis.abgleich.GedaechtnisAbgleich.anstossen() }
        gedaechtnisAuffrischen(app, "Listen geändert")
        return ergebnis
    }

    class Schreiber internal constructor() {
        internal val listenNeu = LinkedHashMap<Long, Liste>()
        internal val eintraegeNeu = LinkedHashMap<Long, Eintrag>()
        fun liste(id: Long): Liste? = listenNeu[id] ?: listen[id]?.takeIf { !it.geloescht }
        fun eintrag(id: Long): Eintrag? = eintraegeNeu[id] ?: eintraege[id]?.takeIf { !it.geloescht }
        fun alleEintraege(liste: Long): List<Eintrag> = (eintraege.values.filter { it.liste == liste && it.id !in eintraegeNeu } + eintraegeNeu.values.filter { it.liste == liste }).filter { !it.geloescht }
        fun setze(l: Liste) { listenNeu[l.id] = l }
        fun setze(e: Eintrag) { eintraegeNeu[e.id] = e }
        fun nachkommen(id: Long, liste: Long): List<Eintrag> {
            val alle = alleEintraege(liste)
            val aus = ArrayList<Eintrag>()
            fun sammle(v: Long) { alle.filter { it.eltern == v }.forEach { aus += it; sammle(it.id) } }
            sammle(id)
            return aus
        }
    }

    // ---------------------------------------------------------------- Listen

    fun listeAnlegen(context: Context, name: String, art: String = ListenArt.SONSTIGES.kennung, farbe: Int = -1): Liste = schreibe(context) {
        val pos = (listen.values.filter { !it.geloescht }.maxOfOrNull { it.pos } ?: 0.0) + 1.0
        val a = ListenArt.von(art)
        Liste(neueId(), name.trim(), a.kennung, if (farbe >= 0) farbe else (listen.size % FARBEN), pos = pos,
            erledigteZeigen = true).also { setze(it) }
    }

    fun listeAendern(context: Context, id: Long, aendern: (Liste) -> Liste): Liste? = schreibe(context) {
        liste(id)?.let(aendern)?.also { setze(it) }
    }

    /** Liste samt allen Einträgen als gelöscht markieren. */
    fun listeLoeschen(context: Context, id: Long): Boolean = schreibe(context) {
        val l = liste(id) ?: return@schreibe false
        setze(l.copy(geloescht = true))
        alleEintraege(id).forEach { setze(it.copy(geloescht = true)) }
        true
    }

    /** Reihenfolge der Listen (für die Auswahl in App und Widget). */
    fun listenOrdnen(context: Context, ids: List<Long>) = schreibe(context) {
        ids.forEachIndexed { i, id -> liste(id)?.let { if (it.pos != i + 1.0) setze(it.copy(pos = i + 1.0)) } }
    }

    // ---------------------------------------------------------------- Einträge

    fun hinzufuegen(
        context: Context, liste: Long, titel: String, eltern: Long = 0L, prioritaet: Int = 0, notiz: String = "", faellig: String = "", tags: List<String> = emptyList(),
        abschnitt: String = "",
    ): Eintrag? = schreibe(context) {
        if (abschnitt.isNotBlank()) liste(liste)?.let { l -> if (abschnitt.trim() !in l.abschnitte) setze(l.copy(abschnitte = l.abschnitte + abschnitt.trim())) }
        if (liste(liste) == null) return@schreibe null
        val tiefe = generateSequence(eltern.takeIf { it != 0L }?.let { eintrag(it) }) { e -> e.eltern.takeIf { it != 0L }?.let { eintrag(it) } }.count()
        val vater = if (tiefe > MAX_TIEFE) eintrag(eltern)?.eltern ?: 0L else eltern
        val geschwister = alleEintraege(liste).filter { it.eltern == vater }
        val pos = (geschwister.maxOfOrNull { it.pos } ?: 0.0) + 1.0
        Eintrag(neueId(), liste, titel.trim(), vater, notiz.trim(), prioritaet.coerceIn(0, 3), faellig, tags.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }, pos = pos,
            abschnitt = if (vater == 0L) abschnitt.trim() else "").also { setze(it) }
    }

    fun aendern(context: Context, id: Long, aendern: (Eintrag) -> Eintrag): Eintrag? = schreibe(context) {
        eintrag(id)?.let(aendern)?.also { setze(it) }
    }

    /**
     * Abhaken wie in TickTick: Ein erledigter Eintrag nimmt alle Unterpunkte mit. Wird ein Unterpunkt wieder geöffnet, öffnen sich
     * auch seine erledigten Oberpunkte (sonst stünde ein offener Punkt unter einem erledigten).
     */
    fun abhaken(context: Context, id: Long, erledigt: Boolean): Eintrag? = schreibe(context) {
        val e = eintrag(id) ?: return@schreibe null
        val jetzt = System.currentTimeMillis()
        setze(e.copy(erledigt = erledigt, erledigtAm = if (erledigt) jetzt else 0L))
        if (erledigt) {
            nachkommen(id, e.liste).filter { !it.erledigt }.forEach { setze(it.copy(erledigt = true, erledigtAm = jetzt)) }
            // Wie TickTick: Sind alle Unterpunkte erledigt, ist es auch der Oberpunkt.
            var v = e.eltern
            while (v != 0L) {
                val p = eintrag(v) ?: break
                if (p.erledigt || alleEintraege(e.liste).any { it.eltern == v && !it.erledigt }) break
                setze(p.copy(erledigt = true, erledigtAm = jetzt)); v = p.eltern
            }
        } else {
            var v = e.eltern
            while (v != 0L) { val p = eintrag(v) ?: break; if (p.erledigt) setze(p.copy(erledigt = false, erledigtAm = 0L)); v = p.eltern }
        }
        eintrag(id)
    }

    fun loeschen(context: Context, id: Long): Eintrag? = schreibe(context) {
        val e = eintrag(id) ?: return@schreibe null
        setze(e.copy(geloescht = true))
        nachkommen(id, e.liste).forEach { setze(it.copy(geloescht = true)) }
        e
    }

    /** Alle erledigten Einträge einer Liste löschen; gibt die Anzahl zurück. */
    fun erledigteLoeschen(context: Context, liste: Long): Int = schreibe(context) {
        val weg = alleEintraege(liste).filter { it.erledigt }
        weg.forEach { e -> setze(e.copy(geloescht = true)); nachkommen(e.id, liste).forEach { setze(it.copy(geloescht = true)) } }
        weg.size
    }

    /** In eine andere Liste verschieben, mit allen Unterpunkten; dort oben auf der obersten Ebene. */
    fun verschieben(context: Context, id: Long, ziel: Long): Eintrag? = schreibe(context) {
        val e = eintrag(id) ?: return@schreibe null
        if (liste(ziel) == null) return@schreibe null
        val kinder = nachkommen(id, e.liste)
        val pos = (alleEintraege(ziel).filter { it.eltern == 0L }.maxOfOrNull { it.pos } ?: 0.0) + 1.0
        setze(e.copy(liste = ziel, eltern = 0L, pos = pos))
        kinder.forEach { setze(it.copy(liste = ziel)) }
        eintrag(id)
    }

    /** Unterpunkt eines anderen Eintrags derselben Liste machen (0 = oberste Ebene). */
    fun einhaengen(context: Context, id: Long, eltern: Long): Eintrag? = schreibe(context) {
        val e = eintrag(id) ?: return@schreibe null
        if (eltern == id || (eltern != 0L && nachkommen(id, e.liste).any { it.id == eltern })) return@schreibe null
        val pos = (alleEintraege(e.liste).filter { it.eltern == eltern }.maxOfOrNull { it.pos } ?: 0.0) + 1.0
        setze(e.copy(eltern = eltern, pos = pos))
        eintrag(id)
    }

    /** Eigene Reihenfolge: [ids] sind Geschwister in der neuen Reihenfolge. */
    fun ordnen(context: Context, ids: List<Long>) = schreibe(context) {
        ids.forEachIndexed { i, id -> eintrag(id)?.let { if (it.pos != i + 1.0) setze(it.copy(pos = i + 1.0)) } }
    }

    // ---------------------------------------------------------------- Ansicht

    private val kollator: Collator = Collator.getInstance(Locale.GERMAN).apply { strength = Collator.SECONDARY }

    private fun vergleicher(s: Sortierung): Comparator<Eintrag> {
        val manuell = compareBy<Eintrag>({ it.pos }, { it.erstellt })
        val datum = Comparator<Eintrag> { a, b -> when { a.faellig == b.faellig -> 0; a.faellig.isEmpty() -> 1; b.faellig.isEmpty() -> -1; else -> a.faellig.compareTo(b.faellig) } }
        val prio = compareByDescending<Eintrag> { it.prioritaet }
        val titel = Comparator<Eintrag> { a, b -> kollator.compare(a.titel, b.titel) }
        return when (s) {
            Sortierung.MANUELL -> manuell
            Sortierung.FAELLIG -> datum.then(prio).then(manuell)
            Sortierung.PRIORITAET -> prio.then(datum).then(manuell)
            Sortierung.TITEL -> titel.then(manuell)
            Sortierung.TAG -> Comparator<Eintrag> { a, b ->
                val x = a.tags.minWithOrNull(kollator); val y = b.tags.minWithOrNull(kollator)
                when { x == y -> 0; x == null -> 1; y == null -> -1; else -> kollator.compare(x, y) }
            }.then(prio).then(manuell)
            Sortierung.ERSTELLT -> compareByDescending<Eintrag> { it.erstellt }
            Sortierung.AELTESTE -> compareBy<Eintrag> { it.erstellt }
            Sortierung.GEAENDERT -> compareByDescending<Eintrag> { it.geaendert }
        }.let { v -> compareByDescending<Eintrag> { it.angeheftet }.then(v) }
    }

    private fun faelligGruppe(f: String, heute: LocalDate = LocalDate.now()): Pair<Int, String> {
        val d = runCatching { LocalDate.parse(f) }.getOrNull() ?: return 9 to "Ohne Datum"
        return when {
            d.isBefore(heute) -> 0 to "Überfällig"
            d == heute -> 1 to "Heute"
            d == heute.plusDays(1) -> 2 to "Morgen"
            !d.isAfter(heute.plusDays(7)) -> 3 to "Nächste 7 Tage"
            else -> 4 to "Später"
        }
    }

    /**
     * Die Einträge einer Liste so, wie Frank sie sieht: offene oben (sortiert und gegebenenfalls gruppiert wie in den Einstellungen der
     * Liste), Unterpunkte direkt unter ihrem Oberpunkt eingerückt, erledigte Oberpunkte unten unter „Erledigt“.
     * [erledigteZeigen] = null nimmt die Einstellung der Liste.
     */
    fun ansicht(stand: ListenStand, liste: Liste, erledigteZeigen: Boolean? = null, gruppen: Boolean = true): List<Zeile> {
        val alle = stand.eintraegeVon(liste.id)
        val ids = alle.map { it.id }.toSet()
        // Unterpunkte, deren Oberpunkt fehlt (gelöscht auf dem anderen Gerät), rücken nach oben statt zu verschwinden.
        val kinder = alle.groupBy { if (it.eltern != 0L && it.eltern in ids) it.eltern else 0L }
        val vgl = vergleicher(Sortierung.von(liste.sortierung))
        val zeigen = erledigteZeigen ?: liste.erledigteZeigen
        val aus = ArrayList<Zeile>()
        fun zeile(e: Eintrag, tiefe: Int) {
            val k = kinder[e.id].orEmpty()
            aus += Zeile(e, tiefe, k.count { it.erledigt }, k.size)
            // Unterpunkte: offene zuerst, dann erledigte (nur wenn erledigte gezeigt werden).
            k.filter { !it.erledigt }.sortedWith(vgl).forEach { zeile(it, tiefe + 1) }
            if (zeigen || e.erledigt) k.filter { it.erledigt }.sortedWith(vgl).forEach { zeile(it, tiefe + 1) }
        }
        val oben = kinder[0L].orEmpty()
        val offen = oben.filter { !it.erledigt }.sortedWith(vgl)
        when (if (gruppen) Gruppierung.von(liste.gruppierung) else Gruppierung.KEINE) {
            Gruppierung.KEINE -> offen.forEach { zeile(it, 0) }
            Gruppierung.ABSCHNITT -> {
                val reihe = liste.abschnitte
                offen.groupBy { it.abschnitt.takeIf { a -> a in reihe }.orEmpty() }.toSortedMap(compareBy { a -> if (a.isEmpty()) -1 else reihe.indexOf(a) }).forEach { (a, l) ->
                    aus += Zeile(kopf = a.ifEmpty { "Ohne Abschnitt" }, kopfAnzahl = l.size); l.forEach { zeile(it, 0) }
                }
            }
            Gruppierung.ERSTELLT -> offen.groupBy { java.time.Instant.ofEpochMilli(it.erstellt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                .toSortedMap(reverseOrder()).forEach { (d, l) ->
                    aus += Zeile(kopf = "Erstellt " + d.format(java.time.format.DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)), kopfAnzahl = l.size); l.forEach { zeile(it, 0) }
                }
            Gruppierung.PRIORITAET -> offen.groupBy { it.prioritaet }.toSortedMap(reverseOrder()).forEach { (p, l) ->
                aus += Zeile(kopf = if (p == 0) "Ohne Priorität" else "Priorität ${Prioritaet.name(p)}", kopfAnzahl = l.size); l.forEach { zeile(it, 0) }
            }
            Gruppierung.FAELLIG -> offen.groupBy { faelligGruppe(it.faellig) }.toSortedMap(compareBy { it.first }).forEach { (g, l) ->
                aus += Zeile(kopf = g.second, kopfAnzahl = l.size); l.forEach { zeile(it, 0) }
            }
            Gruppierung.TAG -> {
                val mit = offen.flatMap { e -> e.tags.ifEmpty { listOf("") }.map { it to e } }.groupBy({ it.first }, { it.second })
                mit.toSortedMap { a, b -> when { a == b -> 0; a.isEmpty() -> 1; b.isEmpty() -> -1; else -> kollator.compare(a, b) } }.forEach { (t, l) ->
                    aus += Zeile(kopf = if (t.isEmpty()) "Ohne Tag" else "#$t", kopfAnzahl = l.size); l.forEach { zeile(it, 0) }
                }
            }
        }
        val fertig = oben.filter { it.erledigt }.sortedByDescending { it.erledigtAm }
        if (zeigen && fertig.isNotEmpty()) {
            aus += Zeile(kopf = "Erledigt", kopfAnzahl = fertig.size)
            fertig.forEach { zeile(it, 0) }
        }
        return aus
    }

    // ---------------------------------------------------------------- Namen auflösen (Sprache)

    /** Ergebnis der Suche nach einer Liste: genau eine, keine oder mehrere passende. */
    sealed class Treffer {
        data class Eine(val liste: Liste) : Treffer()
        data class Mehrere(val listen: List<Liste>) : Treffer()
        data object Keine : Treffer()
    }

    private val FUELL = Regex("\\b(meine[rnms]?|die|der|das|den|dem|zur|zum|auf|in|liste|listen)\\b")

    fun glatt(text: String): String = text.lowercase(Locale.GERMAN)
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .replace(Regex("[^a-z0-9 ]"), " ").replace("liste", " ").replace(FUELL, " ").replace(Regex("\\s+"), " ").trim()

    /** Wörter, an denen die Art einer Liste erkennbar ist („Einkaufsliste“ → Einkauf). */
    private val ART_WOERTER = mapOf(
        ListenArt.EINKAUF to listOf("einkauf", "einkaufen", "shopping", "supermarkt", "besorg"),
        ListenArt.PACKLISTE to listOf("pack", "koffer", "urlaub", "reise"),
        ListenArt.IDEEN to listOf("idee"),
        ListenArt.AUFGABEN to listOf("aufgabe", "todo", "to do", "erledig"),
        ListenArt.CHECKLISTE to listOf("check"),
    )

    fun artAusName(name: String): ListenArt? { val g = glatt(name); return ART_WOERTER.entries.firstOrNull { (_, w) -> w.any { g.contains(it) } }?.key }

    /** Findet die gemeinte Liste: erst gleicher Name, dann Teilwort, dann die einzige Liste der erkennbaren Art. */
    fun finde(stand: ListenStand, name: String): Treffer {
        val alle = stand.listen
        if (alle.isEmpty()) return Treffer.Keine
        name.trim().toLongOrNull()?.let { id -> alle.firstOrNull { it.id == id }?.let { return Treffer.Eine(it) } }
        val g = glatt(name)
        if (g.isEmpty()) return if (alle.size == 1) Treffer.Eine(alle[0]) else Treffer.Mehrere(alle)
        fun ergebnis(l: List<Liste>) = when (l.size) { 0 -> null; 1 -> Treffer.Eine(l[0]); else -> Treffer.Mehrere(l) }
        ergebnis(alle.filter { glatt(it.name) == g })?.let { return it }
        ergebnis(alle.filter { glatt(it.name).replace(" ", "") == g.replace(" ", "") })?.let { return it }
        ergebnis(alle.filter { val n = glatt(it.name); n.isNotEmpty() && (n.contains(g) || g.contains(n)) })?.let { return it }
        artAusName(name)?.let { art -> ergebnis(alle.filter { it.art == art.kennung })?.let { return it } }
        return Treffer.Keine
    }

    /** Findet einen Eintrag einer Liste nach Kennung oder Titel (gleicher Titel vor Teilwort; offene vor erledigten). */
    fun findeEintrag(stand: ListenStand, liste: Long, text: String): List<Eintrag> {
        val alle = stand.eintraegeVon(liste)
        text.trim().toLongOrNull()?.let { id -> alle.firstOrNull { it.id == id }?.let { return listOf(it) } }
        val g = glatt(text).replace(" ", "")
        if (g.isEmpty()) return emptyList()
        fun n(e: Eintrag) = glatt(e.titel).replace(" ", "")
        val gleich = alle.filter { n(it) == g }
        val teil = gleich.ifEmpty { alle.filter { n(it).isNotEmpty() && (n(it).contains(g) || g.contains(n(it))) } }
        return teil.sortedBy { it.erledigt }.let { l -> if (l.any { !it.erledigt } && l.size > 1) l.filter { !it.erledigt } else l }
    }

    /** Schnelleingabe wie in TickTick: „Milch !hoch #kühl“ → Titel „Milch“, Priorität hoch, Tag „kühl“ (!1 bis !3 oder Wort). */
    data class Schnell(val titel: String, val prioritaet: Int, val tags: List<String>)

    fun schnell(text: String): Schnell {
        var prio = 0
        val tags = ArrayList<String>()
        val rest = text.split(Regex("\\s+")).filter { w ->
            when {
                w.length > 1 && w.startsWith("!") -> { val p = w.drop(1); (p.toIntOrNull()?.takeIf { it in 0..3 } ?: Prioritaet.von(p))?.let { prio = it; false } ?: true }
                w.length > 1 && w.startsWith("#") -> { tags += w.drop(1); false }
                else -> true
            }
        }
        return Schnell(rest.joinToString(" ").trim().ifEmpty { text.trim() }, prio, tags)
    }

    // ---------------------------------------------------------------- JSON

    const val FARBEN = 8

    private fun listeJson(l: Liste) = JSONObject().put("id", l.id).put("name", l.name).put("art", l.art).put("farbe", l.farbe)
        .put("sortierung", l.sortierung).put("gruppierung", l.gruppierung).put("erledigte_zeigen", l.erledigteZeigen).put("pos", l.pos)
        .put("erstellt", l.erstellt).put("geaendert", l.geaendert).put("geloescht", l.geloescht).put("abschnitte", JSONArray(l.abschnitte))

    private fun listeAus(j: JSONObject) = Liste(
        j.getLong("id"), j.optString("name"), j.optString("art", "sonstiges"), j.optInt("farbe"), j.optString("sortierung", "manuell"),
        j.optString("gruppierung", "keine"), j.optBoolean("erledigte_zeigen", true), j.optDouble("pos", 0.0), j.optLong("erstellt"), j.optLong("geaendert"), j.optBoolean("geloescht"),
        j.optJSONArray("abschnitte")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
    )

    private fun eintragJson(e: Eintrag) = JSONObject().put("id", e.id).put("liste", e.liste).put("titel", e.titel).put("eltern", e.eltern)
        .put("notiz", e.notiz).put("prioritaet", e.prioritaet).put("faellig", e.faellig).put("tags", JSONArray(e.tags)).put("erledigt", e.erledigt)
        .put("erledigt_am", e.erledigtAm).put("pos", e.pos).put("erstellt", e.erstellt).put("geaendert", e.geaendert).put("geloescht", e.geloescht)
        .put("angeheftet", e.angeheftet).put("abschnitt", e.abschnitt)

    private fun eintragAus(j: JSONObject) = Eintrag(
        j.getLong("id"), j.getLong("liste"), j.optString("titel"), j.optLong("eltern"), j.optString("notiz"), j.optInt("prioritaet"), j.optString("faellig"),
        j.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
        j.optBoolean("erledigt"), j.optLong("erledigt_am"), j.optDouble("pos", 0.0), j.optLong("erstellt"), j.optLong("geaendert"), j.optBoolean("geloescht"),
        j.optBoolean("angeheftet"), j.optString("abschnitt"),
    )
}
