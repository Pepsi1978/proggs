package de.frank.jarvis.ablage

import android.content.Context
import android.util.AtomicFile
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Ein eingestellter Erinnerungszeitpunkt. [minuten] vor dem Termin; bei [uhrzeit] (nur Termine ohne Uhrzeit) [minuten] / 1440 Tage vorher um diese Zeit. */
data class Ausloesung(val id: String, val minuten: Int, val uhrzeit: String? = null, val angelegt: Long = 0L)

/**
 * Eine Erinnerung. [zeile] = Listenzeile (etwa sechs Wörter), [kurz] = Kurzfassung zum Vorlesen, [text] = ausführlicher Inhalt.
 * [uhrzeit] null = Frank hat keine genannt; dann löst sie um [ohneUhrzeitUm] aus, angezeigt wird aber nur das Datum.
 * [hinweisUm]: allgemeine Erinnerungen ohne Datum: Jarvis hat für diesen Zeitpunkt einen erneuten Hinweis geplant.
 */
data class Erinnerung(
    val id: String,
    val allgemein: Boolean,
    val zeile: String,
    val kurz: String,
    val text: String,
    val datum: String? = null,
    val uhrzeit: String? = null,
    val ohneUhrzeitUm: String = "10:00",
    val form: String = FORM_MELDUNG,
    val ausloesungen: List<Ausloesung> = emptyList(),
    val erledigt: Long = 0L,
    val hinweisUm: Long = 0L,
    val zuletztErwaehnt: Long = 0L,
    val quelle: String = "",
    val erstellt: Long = 0L,
    val geaendert: Long = 0L,
) {
    val wecker get() = form == FORM_WECKER

    fun tag(): LocalDate? = datum?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Der Termin selbst, zum Ordnen und Rechnen; bei reinem Datum die Uhrzeit, zu der er auslöst. */
    fun termin(): Long? = tag()?.let { d -> ms(d.atTime(zeit(uhrzeit ?: ohneUhrzeitUm))) }

    fun zeitVon(a: Ausloesung): Long? {
        val d = tag() ?: return null
        return if (a.uhrzeit != null) ms(d.minusDays((a.minuten / 1440).toLong()).atTime(zeit(a.uhrzeit)))
        else ms(d.atTime(zeit(uhrzeit ?: ohneUhrzeitUm)).minusMinutes(a.minuten.toLong()))
    }

    /** Alle Auslösungen. Die Kennung trägt die Zeit: Wird der Termin verschoben, ist es eine neue Auslösung; bloßes Bearbeiten des Textes lässt Bestätigtes bestätigt. */
    fun termine(): List<Faellig> {
        if (allgemein) return if (hinweisUm > 0) listOf(Faellig(this, "${id}_h_${hinweisUm / 60_000}", hinweisUm, null, "Hinweis von Jarvis")) else emptyList()
        return ausloesungen.mapNotNull { a -> zeitVon(a)?.let { z -> Faellig(this, "${id}_${a.id}_${z / 60_000}", z, a, vorlaufText(a)) } }.sortedBy { it.zeit }
    }

    fun vorlaufText(a: Ausloesung): String = when {
        a.uhrzeit != null -> (if (a.minuten == 0) "Am Termintag" else tageText(a.minuten / 1440) + " vorher") + " um ${a.uhrzeit} Uhr"
        a.minuten == 0 -> if (uhrzeit != null) "Zum Termin" else "Am Termintag"
        a.minuten % 1440 == 0 -> tageText(a.minuten / 1440) + " vorher"
        a.minuten % 60 == 0 -> stundenText(a.minuten / 60) + " vorher"
        else -> if (a.minuten == 1) "1 Minute vorher" else "${a.minuten} Minuten vorher"
    }

    /** Erste Zeile des Eintrags: Datum und nur eine ausdrücklich genannte Uhrzeit. */
    fun datumAnzeige(): String = tag()?.let { d -> d.format(ANZEIGE) + (uhrzeit?.let { " · $it Uhr" } ?: "") } ?: "Ohne festen Termin"

    companion object {
        const val FORM_MELDUNG = "meldung"
        const val FORM_WECKER = "wecker"
        private val ANZEIGE = DateTimeFormatter.ofPattern("EE, d. MMMM yyyy", Locale.GERMAN)
        private fun tageText(n: Int) = if (n == 1) "1 Tag" else "$n Tage"
        private fun stundenText(n: Int) = if (n == 1) "1 Stunde" else "$n Stunden"
        private fun zeit(t: String) = runCatching { LocalTime.parse(t) }.getOrDefault(LocalTime.of(10, 0))
        private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/** Eine einzelne Auslösung einer Erinnerung. */
data class Faellig(val erinnerung: Erinnerung, val quittung: String, val zeit: Long, val ausloesung: Ausloesung?, val anzeige: String)

data class Quittung(val id: String, val zeit: Long, val von: String, val erinnerung: String)

/**
 * Der Reiter „Erinnerung“ der Ablage: eigene Datei `erinnerungen.json`, abgeglichen über [AblageSync].
 * Bestätigungen stehen als eigene Dokumente neben den Erinnerungen und kommen nur dazu: Keine Bearbeitung auf einem anderen Gerät kann sie überschreiben.
 */
object Erinnerungen {
    private const val DATEI = "erinnerungen.json"
    /** Älteres, nie Bestätigtes meldet sich nicht mehr von selbst, es bleibt in der Liste stehen. */
    const val VERPASST_NACH_MS = 7 * 24 * 3_600_000L

    private val _stand = MutableStateFlow(0L)
    val stand: StateFlow<Long> = _stand.asStateFlow()

    private class Stand(val liste: List<Erinnerung>, val quittungen: Map<String, Quittung>, val geloescht: Map<String, Long>, val defekt: Boolean = false)

    private var gelesen: Stand? = null
    private fun datei(c: Context) = File(c.applicationContext.filesDir, DATEI)

    @Synchronized private fun lies(c: Context): Stand {
        gelesen?.let { return it }
        return liesDatei(c).also { if (!it.defekt) gelesen = it }
    }

    private fun liesDatei(c: Context): Stand {
        val f = datei(c)
        val text = try { String(AtomicFile(f).readFully(), Charsets.UTF_8) } catch (_: FileNotFoundException) { return Stand(emptyList(), emptyMap(), emptyMap()) }
            catch (e: Exception) { Logbuch.fehler("Erinnerungen lesen", e); return Stand(emptyList(), emptyMap(), emptyMap(), defekt = true) }
        if (text.isBlank()) return Stand(emptyList(), emptyMap(), emptyMap())
        return try {
            val j = JSONObject(text)
            val a = j.getJSONArray("erinnerungen")
            val q = j.optJSONObject("quittungen") ?: JSONObject()
            val w = j.optJSONObject("geloescht") ?: JSONObject()
            Stand((0 until a.length()).map { i -> a.getJSONObject(i).let { aus(it.getString("id"), it, it.optLong("geaendert")) } },
                q.keys().asSequence().associateWith { k -> q.getJSONObject(k).let { Quittung(k, it.optLong("zeit"), it.optString("von"), it.optString("erinnerung")) } },
                w.keys().asSequence().associateWith { w.optLong(it) })
        } catch (e: Exception) {
            runCatching { f.copyTo(File(f.path + ".defekt-" + f.lastModified())) }
            Logbuch.fehler("Erinnerungen defekt", e)
            Stand(emptyList(), emptyMap(), emptyMap(), defekt = true)
        }
    }

    @Synchronized private fun schreibe(c: Context, s: Stand): Boolean {
        if (lies(c).defekt) return false
        val inhalt = JSONObject().put("format", 1)
            .put("erinnerungen", JSONArray(s.liste.map { alsJson(it).put("id", it.id).put("geaendert", it.geaendert) }))
            .put("quittungen", JSONObject().apply { s.quittungen.values.forEach { q -> put(q.id, JSONObject().put("zeit", q.zeit).put("von", q.von).put("erinnerung", q.erinnerung)) } })
            .put("geloescht", JSONObject().apply { s.geloescht.forEach { (k, v) -> put(k, v) } }).toString(2)
        val atomar = AtomicFile(datei(c))
        val aus = try { atomar.startWrite() } catch (e: Exception) { Logbuch.fehler("Erinnerungen schreiben", e); return false }
        return try {
            aus.write(inhalt.toByteArray(Charsets.UTF_8)); atomar.finishWrite(aus); gelesen = s; _stand.update { it + 1 }; true
        } catch (e: Exception) {
            atomar.failWrite(aus); Logbuch.fehler("Erinnerungen schreiben", e); false
        }
    }

    // ---- Lesen ----

    fun defekt(c: Context) = lies(c).defekt
    fun alle(c: Context): List<Erinnerung> = lies(c).liste
    fun eine(c: Context, id: String): Erinnerung? = lies(c).liste.firstOrNull { it.id == id }
    fun quittung(c: Context, id: String): Quittung? = lies(c).quittungen[id]
    fun quittungen(c: Context): Map<String, Quittung> = lies(c).quittungen
    fun offeneLoeschungen(c: Context): Map<String, Long> = lies(c).geloescht

    /** Datierte: das als Nächstes Kommende oben, danach das Vergangene, das Älteste ganz unten. Maßgeblich ist der Termin selbst. */
    fun datiert(c: Context, jetzt: Long = System.currentTimeMillis()): List<Erinnerung> {
        val d = alle(c).filter { !it.allgemein }
        val (kommend, vorbei) = d.partition { (it.termin() ?: 0L) >= jetzt && it.erledigt == 0L }
        return kommend.sortedBy { it.termin() } + vorbei.sortedByDescending { it.termin() }
    }

    fun allgemeine(c: Context): List<Erinnerung> = alle(c).filter { it.allgemein }.sortedWith(compareBy<Erinnerung> { it.erledigt != 0L }.thenByDescending { it.erstellt })

    /** Wird diese Auslösung noch von selbst gemeldet? Vor dem Anlegen vergangene Zeitpunkte nicht: Sie waren beim Einstellen schon vorbei. */
    fun meldbar(f: Faellig, jetzt: Long): Boolean = f.erinnerung.erledigt == 0L && jetzt - f.zeit < VERPASST_NACH_MS && (f.ausloesung == null || f.zeit >= f.ausloesung.angelegt - 60_000L)

    /** Offene, schon fällige Auslösungen, die noch niemand bestätigt hat. */
    fun faellig(c: Context, jetzt: Long = System.currentTimeMillis()): List<Faellig> {
        val q = lies(c).quittungen
        return alle(c).flatMap { it.termine() }.filter { it.zeit <= jetzt && it.quittung !in q && meldbar(it, jetzt) }.sortedBy { it.zeit }
    }

    fun naechste(c: Context, jetzt: Long = System.currentTimeMillis()): List<Faellig> {
        val q = lies(c).quittungen
        return alle(c).filter { it.erledigt == 0L }.flatMap { it.termine() }.filter { it.zeit > jetzt && it.quittung !in q }.sortedBy { it.zeit }
    }

    // ---- Ändern ----

    private fun neueId() = UUID.randomUUID().toString().replace("-", "").take(20)
    fun neueAusloesung(minuten: Int, uhrzeit: String? = null) = Ausloesung(UUID.randomUUID().toString().replace("-", "").take(8), minuten, uhrzeit, System.currentTimeMillis())

    /** Listenzeile aus dem Text, wenn keine angegeben ist: die ersten sechs Wörter. */
    fun zeileAus(text: String) = text.trim().split(Regex("\\s+")).take(6).joinToString(" ").trimEnd(',', ';', ':', '.')
    fun kurzAus(text: String) = text.trim().split(Regex("\\s+")).let { w -> w.take(35).joinToString(" ") + if (w.size > 35) " …" else "" }

    @Synchronized
    fun anlegen(c: Context, text: String, zeile: String, kurz: String, datum: String?, uhrzeit: String?, form: String, vorab: List<Ausloesung>, ohneUhrzeitUm: String): Erinnerung? {
        val sauber = text.trim()
        val s = lies(c)
        if (sauber.isEmpty() || s.defekt) return null
        val jetzt = System.currentTimeMillis()
        val allgemein = datum == null
        val ausl = if (allgemein) emptyList() else vorab.ifEmpty { listOf(neueAusloesung(0)) }
        val e = Erinnerung(neueId(), allgemein, zeile.trim().ifEmpty { zeileAus(sauber) }, kurz.trim().ifEmpty { kurzAus(sauber) }, sauber, datum, uhrzeit, ohneUhrzeitUm, form, ausl, quelle = AblageQuellen.HANDY, erstellt = jetzt, geaendert = jetzt)
        return e.takeIf { schreibe(c, Stand(s.liste + e, s.quittungen, s.geloescht)) }
    }

    /** Ändert eine Erinnerung; nie eine Bestätigung oder Erledigung, außer [aendern] setzt sie ausdrücklich. */
    @Synchronized
    fun aendere(c: Context, id: String, aendern: (Erinnerung) -> Erinnerung): Erinnerung? {
        val s = lies(c)
        val alt = s.liste.firstOrNull { it.id == id } ?: return null
        val neu = aendern(alt).copy(geaendert = maxOf(System.currentTimeMillis(), alt.geaendert + 1))
        return neu.takeIf { schreibe(c, Stand(s.liste.map { if (it.id == id) neu else it }, s.quittungen, s.geloescht)) }
    }

    @Synchronized
    fun loesche(c: Context, id: String): Boolean {
        val s = lies(c)
        if (s.liste.none { it.id == id }) return false
        return schreibe(c, Stand(s.liste.filter { it.id != id }, s.quittungen, s.geloescht + (id to System.currentTimeMillis())))
    }

    /** Bestätigt genau eine Auslösung. Die übrigen geplanten Auslösungen bleiben. */
    @Synchronized
    fun bestaetige(c: Context, f: Faellig, von: String = AblageQuellen.HANDY): Boolean {
        val s = lies(c)
        if (f.quittung in s.quittungen) return true
        return schreibe(c, Stand(s.liste, s.quittungen + (f.quittung to Quittung(f.quittung, System.currentTimeMillis(), von, f.erinnerung.id)), s.geloescht))
    }

    // ---- Abgleich (AblageSync) ----

    @Synchronized fun quittiereLoeschung(c: Context, id: String) { val s = lies(c); if (id in s.geloescht) schreibe(c, Stand(s.liste, s.quittungen, s.geloescht - id)) }

    @Synchronized fun setzeFern(c: Context, e: Erinnerung): Boolean {
        val s = lies(c)
        val da = s.liste.any { it.id == e.id }
        return schreibe(c, Stand(if (da) s.liste.map { if (it.id == e.id) e else it } else s.liste + e, s.quittungen, s.geloescht))
    }

    @Synchronized fun loescheFern(c: Context, id: String): Boolean { val s = lies(c); return schreibe(c, Stand(s.liste.filter { it.id != id }, s.quittungen, s.geloescht)) }

    @Synchronized fun quittungFern(c: Context, q: Quittung): Boolean { val s = lies(c); return q.id in s.quittungen || schreibe(c, Stand(s.liste, s.quittungen + (q.id to q), s.geloescht)) }

    /** Beide Seiten haben geändert: Der jüngere Inhalt gilt, eingestellte Auslösungen beider Seiten bleiben. */
    fun vereine(lokal: Erinnerung, fern: Erinnerung): Erinnerung {
        val neuer = if (fern.geaendert >= lokal.geaendert) fern else lokal
        val aelter = if (neuer === fern) lokal else fern
        return neuer.copy(ausloesungen = neuer.ausloesungen + aelter.ausloesungen.filter { a -> neuer.ausloesungen.none { it.id == a.id } },
            zuletztErwaehnt = maxOf(lokal.zuletztErwaehnt, fern.zuletztErwaehnt), hinweisUm = maxOf(lokal.hinweisUm, fern.hinweisUm), geaendert = maxOf(lokal.geaendert, fern.geaendert))
    }

    fun alsJson(e: Erinnerung): JSONObject = JSONObject().put("allgemein", e.allgemein).put("zeile", e.zeile).put("kurz", e.kurz).put("text", e.text)
        .put("datum", e.datum ?: JSONObject.NULL).put("uhrzeit", e.uhrzeit ?: JSONObject.NULL).put("ohne_uhrzeit_um", e.ohneUhrzeitUm).put("form", e.form)
        .put("ausloesungen", JSONArray(e.ausloesungen.map { JSONObject().put("id", it.id).put("minuten", it.minuten).put("uhrzeit", it.uhrzeit ?: JSONObject.NULL).put("angelegt", it.angelegt) }))
        .put("erledigt", e.erledigt).put("hinweis_um", e.hinweisUm).put("zuletzt_erwaehnt", e.zuletztErwaehnt).put("quelle", e.quelle.ifBlank { AblageQuellen.HANDY }).put("erstellt", e.erstellt)

    fun aus(id: String, w: JSONObject, geaendert: Long): Erinnerung {
        fun nullbar(o: JSONObject, k: String) = if (o.isNull(k)) null else o.optString(k).takeIf { it.isNotBlank() }
        val a = w.optJSONArray("ausloesungen") ?: JSONArray()
        return Erinnerung(
            id, w.optBoolean("allgemein"), w.optString("zeile"), w.optString("kurz"), w.optString("text"), nullbar(w, "datum"), nullbar(w, "uhrzeit"), w.optString("ohne_uhrzeit_um", "10:00"),
            w.optString("form", Erinnerung.FORM_MELDUNG),
            (0 until a.length()).map { i -> a.getJSONObject(i).let { Ausloesung(it.getString("id"), it.optInt("minuten"), nullbar(it, "uhrzeit"), it.optLong("angelegt")) } },
            w.optLong("erledigt"), w.optLong("hinweis_um"), w.optLong("zuletzt_erwaehnt"), w.optString("quelle"), w.optLong("erstellt"), geaendert,
        )
    }

    fun zeitText(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
}
