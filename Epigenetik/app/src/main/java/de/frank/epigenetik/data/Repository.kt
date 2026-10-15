package de.frank.epigenetik.data

import android.content.Context
import de.frank.epigenetik.ki.Forscher
import de.frank.epigenetik.ki.jsonAus
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/** Zentrale für alle Recherchen: anlegen, finden, Ergebnis speichern. Wird von App, Worker und Jarvis-Brücke benutzt. */
class Repository private constructor(private val context: Context) {
    val dao = Datenbank.get(context).dao()

    val alle: Flow<List<Recherche>> = dao.alle()
    val wirkungen: Flow<List<SchalterWirkung>> = dao.alleWirkungen()

    fun beobachte(id: Long): Flow<Recherche?> = dao.beobachte(id)

    /**
     * Legt eine Recherche an und reiht sie ein. Ist das Gleiche schon fertig recherchiert, kommt das vorhandene
     * Ergebnis zurück (außer [neu] = true); läuft es gerade, wird nichts doppelt gestartet.
     */
    suspend fun anlegen(typ: Typ, name: String, kategorie: Kategorie? = null, quelle: String = "app", neu: Boolean = false): Recherche {
        val sauber = name.trim().replace(Regex("\\s+"), " ").trimEnd('.', '?', '!').ifBlank { throw IllegalArgumentException("Der Name fehlt.") }
        val vorhanden = when (typ) {
            Typ.STACK -> dao.alleJetzt().firstOrNull { it.typEnum == Typ.STACK }
            Typ.MITTEL -> findeMittel(sauber)
            else -> dao.finde(typ.name, normalisiere(sauber))
        }
        if (vorhanden != null) {
            if (vorhanden.offen) return vorhanden
            if (vorhanden.fertig && !neu && typ != Typ.STACK) return vorhanden
            val zurueck = vorhanden.copy(status = Status.WARTET.name, fehler = "", aktualisiert = System.currentTimeMillis(), gelesen = false, quelle = quelle)
            dao.aendern(zurueck)
            Forscher.einreihen(context, zurueck.id)
            return zurueck
        }
        val r = Recherche(
            typ = typ.name,
            kategorie = (kategorie ?: if (typ == Typ.MITTEL) Kategorie.SUPPLEMENT else null)?.name.orEmpty(),
            name = sauber.replaceFirstChar { it.uppercase() },
            schluessel = normalisiere(sauber),
            quelle = quelle,
        )
        val id = dao.einfuegen(r)
        Forscher.einreihen(context, id)
        return r.copy(id = id)
    }

    /** Findet ein Mittel über Name, Aliase oder ähnliche Schreibweise. */
    suspend fun findeMittel(name: String): Recherche? {
        val k = normalisiere(name)
        if (k.isBlank()) return null
        val mittel = dao.alleJetzt().filter { it.typEnum == Typ.MITTEL }
        return mittel.firstOrNull { it.schluessel == k }
            ?: mittel.firstOrNull { m -> m.aliase.split('|').any { it == k } }
            ?: mittel.firstOrNull { m -> aehnlich(m.schluessel, k) || m.aliase.split('|').any { aehnlich(it, k) } }
    }

    /** Sucht über alle Arten (für Jarvis): erst Mittel, dann Wirkungen, Ziele, Fragen. */
    suspend fun findeIrgendwas(name: String, typ: Typ? = null): Recherche? {
        if (typ == null || typ == Typ.MITTEL) findeMittel(name)?.let { return it }
        val k = normalisiere(name)
        val kandidaten = dao.alleJetzt().filter { typ == null || it.typEnum == typ }
        return kandidaten.firstOrNull { it.schluessel == k }
            ?: kandidaten.firstOrNull { aehnlich(it.schluessel, k) }
            ?: kandidaten.firstOrNull { k.length >= 6 && (it.schluessel.contains(k) || k.contains(it.schluessel)) }
    }

    /** Speichert das Ergebnis einer fertigen Recherche und verknüpft die Schalter. */
    suspend fun ergebnisSpeichern(id: Long, json: String) {
        val r = dao.hole(id) ?: return
        val o = jsonAus(json)
        val jetzt = System.currentTimeMillis()
        var neu = r.copy(json = o.toString(), status = Status.FERTIG.name, fehler = "", aktualisiert = jetzt, gelesen = false)
        val wirkungen = mutableListOf<SchalterWirkung>()
        when (r.typEnum) {
            Typ.MITTEL -> {
                val kiName = o.optString("name").trim()
                val aliase = (o.optJSONArray("aliase").strings() + r.name + kiName).map(::normalisiere).filter { it.isNotBlank() }.toSet()
                neu = neu.copy(
                    name = kiName.ifBlank { r.name },
                    schluessel = normalisiere(kiName.ifBlank { r.name }),
                    aliase = aliase.joinToString("|"),
                    kategorie = Kategorie.von(o.optString("kategorie", r.kategorie)).name,
                    kurz = o.optString("kurz"),
                    potenzial = o.optInt("potenzial", 0).coerceIn(0, 100),
                    evidenz = o.optString("evidenzstufe"),
                )
                o.optJSONArray("schalter").objekte().forEach { s ->
                    val sch = Schalterkatalog.zu(s.optString("id")) ?: return@forEach
                    wirkungen += SchalterWirkung(
                        rechercheId = id, schalter = sch.id, richtung = s.optString("richtung", "moduliert"),
                        staerke = s.optInt("staerke", 0).coerceIn(0, 100), evidenz = s.optString("evidenz"), mechanismus = s.optString("mechanismus"),
                    )
                }
            }
            Typ.WIRKUNG, Typ.ZIEL -> neu = neu.copy(
                kurz = o.optString("fazit").ifBlank { o.optString("erklaerung").ifBlank { o.optString("beschreibung") } },
                potenzial = 0,
            )
            Typ.STACK -> neu = neu.copy(kurz = o.optString("zusammenfassung"), potenzial = o.optInt("bewertung", 0).coerceIn(0, 100))
            Typ.FRAGE -> neu = neu.copy(kurz = o.optString("kurz"))
        }
        dao.ergebnisSpeichern(neu, wirkungen.distinctBy { it.schalter })
    }

    suspend fun setzeFavorit(r: Recherche, an: Boolean) = dao.aendern(r.copy(favorit = an))
    suspend fun setzeStack(r: Recherche, an: Boolean) = dao.aendern(r.copy(imStack = an))
    suspend fun setzeNotiz(r: Recherche, notiz: String) = dao.aendern(r.copy(notiz = notiz))
    suspend fun loeschen(id: Long) { Forscher.abbrechen(context, id); dao.allesLoeschen(id) }
    suspend fun gelesen(id: Long) = dao.gelesen(id)

    /** Nach einem Neustart: alles, was noch offen war, wieder einreihen (laufende Arbeiten bleiben unberührt). */
    suspend fun offeneFortsetzen() {
        dao.offene().forEach { Forscher.einreihen(context, it.id) }
    }

    companion object {
        @Volatile private var instanz: Repository? = null
        fun get(context: Context): Repository = instanz ?: synchronized(this) {
            instanz ?: Repository(context.applicationContext).also { instanz = it }
        }
    }
}

fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }.filter { it.isNotBlank() }
fun JSONArray?.objekte(): List<JSONObject> = if (this == null) emptyList() else List(length()) { optJSONObject(it) }.filterNotNull()

/**
 * Tippfehler-tolerant gleich („Ashwaganda“ ↔ „Ashwagandha“), aber nie bei kurzen Unterscheidungen
 * wie „Vitamin D“ ↔ „Vitamin K“ oder „B6“ ↔ „B12“.
 */
fun aehnlich(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    if (a == b) return true
    if (a.any { it.isDigit() } || b.any { it.isDigit() }) return false
    val la = a.substringAfterLast(' ')
    val lb = b.substringAfterLast(' ')
    if (la != lb && (la.length <= 3 || lb.length <= 3)) return false
    val n = minOf(a.length, b.length)
    val grenze = when { n >= 10 -> 2; n >= 7 -> 1; else -> 0 }
    return grenze > 0 && abstand(a, b) <= grenze
}

/** Levenshtein-Abstand. */
fun abstand(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 3) return 99
    var vorher = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val jetzt = IntArray(b.length + 1)
        jetzt[0] = i
        for (j in 1..b.length) {
            jetzt[j] = minOf(vorher[j] + 1, jetzt[j - 1] + 1, vorher[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        vorher = jetzt
    }
    return vorher[b.length]
}
