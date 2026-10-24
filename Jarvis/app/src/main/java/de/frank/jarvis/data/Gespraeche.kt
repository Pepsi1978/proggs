package de.frank.jarvis.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import de.frank.jarvis.faehigkeit.ProgrammierChat
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ein gespeichertes Gespräch, wie es die Seitenleiste zeigt. [projekt]: id des Projekts, leer = normales Gespräch.
 * [programmierung]: Gespräch eines Programmierauftrags; es steht im Reiter „Programmierung“, nicht unter „Gespräche“.
 */
/** [auftragStart]: wann der erste Programmierauftrag dieses Gesprächs abgeschickt wurde (0 = unbekannt); einmal gesetzt, bleibt er. */
data class Sitzung(val id: String, val titel: String, val erstellt: Long, val geaendert: Long, val anzahl: Int, val projekt: String = "", val programmierung: Boolean = false,
                   val auftragStart: Long = 0L)

/**
 * Eine Nachricht im Speicher: von Frank oder von Jarvis. [art]: „antwort“ (normal), „fehler“ (Fehlermeldung statt
 * Antwort) oder „abgebrochen“ (Franks Frage blieb unbeantwortet). [kontext] sieht nur das Modell im Verlauf,
 * etwa was in einer abgebrochenen Runde schon erledigt war.
 */
data class GespeicherteNachricht(val vonMir: Boolean, val text: String, val art: String = "antwort", val kontext: String = "", val anhaenge: List<String> = emptyList(), val bewertung: Int = 0, val tokens: Long = 0)

/**
 * Jedes Gespräch mit Jarvis als eigene Datei unter `files/gespraeche/<id>.json`, damit Frank alte Gespräche in der
 * Seitenleiste wiederfindet und weiterführen kann. Klein und lesbar: Titel, Zeitpunkte und die Nachrichten.
 */
object Gespraeche {
    private const val TAG = "Gespraeche"

    /** Speichern, Umbenennen und Löschen nacheinander: Zwei Sicherungen desselben Gesprächs dürfen sich nie überholen. */
    private val sperre = Any()

    private fun ordner(context: Context) = File(context.applicationContext.filesDir, "gespraeche").apply { mkdirs() }
    private fun datei(context: Context, id: String) = File(ordner(context), "$id.json")

    fun neueId(): String = System.currentTimeMillis().toString(36) + (1000..9999).random()

    /** Liest eine Gesprächsdatei; eine nach Absturz übrig gebliebene Sicherung (.bak) greift dabei von selbst. */
    private fun lies(d: File): JSONObject = JSONObject(String(AtomicFile(d).readFully(), Charsets.UTF_8))

    /**
     * Nachrichtenarten, die nur ein Programmierauftrag ins Gespräch schreibt (Statusmeldung, Abschlussbericht, Rückfrage) und
     * die Modellwahl-Karte: Schon der noch nicht gestartete Auftrag gehört unter „Programmierung“, egal welcher Reiter offen war.
     */
    private val PROGRAMMIER_ARTEN = setOf(ProgrammierChat.ART_STATUS, ProgrammierChat.ART_BERICHT, "rueckfrage", ProgrammierChat.ART_MODELLWAHL)

    /**
     * Gehört das Gespräch zu einem Programmierauftrag? Die gespeicherte Markierung, sonst die Spuren eines Auftrags: der
     * Auftrag als erste Nachricht (eigenes Auftrags-Gespräch) oder Meldungen der Sitzung. So werden auch alte Gespräche
     * erkannt, deren Auftrag längst aus dem Auftragsbuch gefallen ist.
     */
    fun istProgrammierung(j: JSONObject): Boolean {
        if (j.optBoolean("programmierung")) return true
        if (istAuftragsgespraech(j)) return true
        val a = j.optJSONArray("nachrichten") ?: return false
        return (0 until a.length()).any { a.optJSONObject(it)?.optString("art") in PROGRAMMIER_ARTEN }
    }

    /** Eigenes Auftragsgespräch: Die erste Nachricht ist der Auftrag selbst, angelegt im Augenblick des Abschickens. */
    private fun istAuftragsgespraech(j: JSONObject): Boolean =
        j.optJSONArray("nachrichten")?.optJSONObject(0)?.let { it.optBoolean("mir") && ProgrammierChat.istAuftragsVorspann(it.optString("text")) } == true

    /** Der früheste bekannte Auftragsstart (0 = keiner): Ein späterer Auftrag oder Nachtrag ersetzt nie einen früheren. */
    internal fun fruehesterStart(vararg starts: Long): Long = starts.filter { it > 0 }.minOrNull() ?: 0L

    /** Der Start, der statt [gespeichert] ins Gespräch gehört, oder null, wenn [gespeichert] schon der früheste ist. */
    internal fun neuerStart(gespeichert: Long, bekannt: Long): Long? = fruehesterStart(gespeichert, bekannt).takeIf { it > 0 && it != gespeichert }

    /** Je Gespräch der früheste Start aus (Gespräch, Start)-Paaren des Auftragsbuchs; 0 = nur Einträge ohne Start. */
    internal fun fruehesteStarts(eintraege: List<Pair<String, Long>>): Map<String, Long> = HashMap<String, Long>().apply {
        eintraege.forEach { (g, s) -> if (g.isNotEmpty()) merge(g, s) { a, b -> fruehesterStart(a, b) } }
    }

    private fun sitzungAus(j: JSONObject, id: String = j.getString("id")) =
        Sitzung(id, j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), j.optJSONArray("nachrichten")?.length() ?: 0, j.optString("projekt"), istProgrammierung(j),
            j.optLong("auftragStart"))

    /**
     * Alle Gespräche, das zuletzt geführte zuerst. [mitAuftrag]: Gespräche, denen das Auftragsbuch einen Programmierauftrag
     * zuordnet, mit dem Start ihres ersten Auftrags (0 = unbekannt). Fehlt ihnen oder einem anders erkannten Programmiergespräch
     * die Markierung oder der Start, wird beides einmalig in die Datei geschrieben: Es bleibt dann auch, wenn der Auftrag
     * später aus dem Auftragsbuch fällt.
     */
    fun alle(context: Context, mitAuftrag: Map<String, Long> = emptyMap()): List<Sitzung> =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { f ->
            runCatching {
                val j = lies(f)
                val s = sitzungAus(j)
                val buch = mitAuftrag[s.id]
                // Noch ohne gespeicherten Start (ältere Aufträge): Ein eigenes Auftragsgespräch entstand beim Abschicken, das hilft,
                // wenn der erste Auftrag schon aus dem Auftragsbuch gefallen ist. Sonst nur Starts aus dem Auftragsbuch, nie erfunden.
                val start = if (buch == null) 0L else fruehesterStart(buch, if (s.auftragStart == 0L && istAuftragsgespraech(j)) s.erstellt else 0L)
                val neu = neuerStart(s.auftragStart, start)
                if ((!j.optBoolean("programmierung") && (s.programmierung || buch != null)) || neu != null) markieren(context, s.id, start)
                val mitStart = if (neu != null) s.copy(auftragStart = neu) else s
                if (buch != null) mitStart.copy(programmierung = true) else mitStart
            }.onFailure { Log.w(TAG, "Gespräch ${f.name} nicht lesbar", it) }.getOrNull()
        }.sortedByDescending { it.geaendert }

    /**
     * Markiert ein gespeichertes Gespräch dauerhaft als Programmiergespräch. Gibt es die Datei (noch) nicht, passiert nichts.
     * [auftragStart]: Start eines Auftrags; übernommen nur, wenn er früher ist als der gespeicherte (der früheste bleibt).
     */
    fun markieren(context: Context, id: String, auftragStart: Long = 0L) {
        if (id.isEmpty()) return
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                if (!d.exists() && !File(d.path + ".bak").exists()) return@runCatching
                val j = lies(d)
                val start = neuerStart(j.optLong("auftragStart"), auftragStart)
                if (!j.optBoolean("programmierung") || start != null) {
                    j.put("programmierung", true)
                    start?.let { j.put("auftragStart", it) }
                    schreiben(d, j)
                }
            }.onFailure { Log.w(TAG, "Gespräch $id nicht als Programmierung markiert", it) }
        }
    }

    fun laden(context: Context, id: String): Pair<Sitzung, List<GespeicherteNachricht>>? = runCatching {
        val j = lies(datei(context, id))
        val a = j.optJSONArray("nachrichten") ?: JSONArray()
        val nachrichten = (0 until a.length()).map {
            a.getJSONObject(it).let { n ->
                val anh = n.optJSONArray("anhaenge")?.let { l -> (0 until l.length()).map { k -> l.getString(k) } }.orEmpty()
                GespeicherteNachricht(n.optBoolean("mir"), n.optString("text"), n.optString("art", "antwort"), n.optString("kontext"), anh, n.optInt("bewertung"), n.optLong("tokens"))
            }
        }
        sitzungAus(j, id) to nachrichten
    }.getOrNull()

    /**
     * Speichert das ganze Gespräch. Ein von Hand vergebener Titel bleibt erhalten. Liefert den neuen Eintrag für die
     * Seitenleiste (null = nichts geschrieben). Ein älterer Stand mit weniger Nachrichten überschreibt nie einen neueren.
     * [kuerzenUm]: So viele Nachrichten hat Frank bewusst entfernt (etwa die verworfene Antwort bei „Erneut versuchen“).
     * [projekt]: gilt nur beim ersten Speichern; danach bleibt das Gespräch in seinem Projekt. [programmierung]: Gespräch eines
     * Programmierauftrags; einmal gesetzt (oder an den Meldungen erkannt), bleibt die Markierung bei jedem weiteren Speichern.
     */
    fun speichern(context: Context, id: String, erstellt: Long, nachrichten: List<GespeicherteNachricht>, kuerzenUm: Int = 0, projekt: String = "", programmierung: Boolean = false): Sitzung? {
        if (nachrichten.isEmpty()) return null
        return synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val alt = if (d.exists() || File(d.path + ".bak").exists()) runCatching { lies(d) }.getOrNull() else null
                val bisher = alt?.optJSONArray("nachrichten")?.length() ?: 0
                if (nachrichten.size + kuerzenUm < bisher) {
                    Log.w(TAG, "Älterer Stand von $id verworfen ($bisher > ${nachrichten.size})")
                    return@runCatching null
                }
                val eigenerTitel = alt?.optBoolean("eigenerTitel") == true
                val kiTitel = alt?.optString("kiTitel").orEmpty()
                val titel = if (eigenerTitel) alt!!.optString("titel") else kiTitel.ifBlank { titelAus(nachrichten) }
                val jetzt = System.currentTimeMillis()
                val zuProjekt = if (alt != null) alt.optString("projekt") else projekt
                val json = JSONObject()
                    .put("id", id)
                    .put("titel", titel)
                    .put("eigenerTitel", eigenerTitel)
                    .put("kiTitel", kiTitel)
                    .put("kiTitelBei", alt?.optInt("kiTitelBei") ?: 0)
                    .put("erstellt", erstellt)
                    .put("geaendert", jetzt)
                    .apply { if (zuProjekt.isNotEmpty()) put("projekt", zuProjekt) }
                    .apply { if (programmierung || alt?.optBoolean("programmierung") == true) put("programmierung", true) }
                    // Der Start des ersten Auftrags bleibt bei jedem Speichern stehen.
                    .apply { alt?.optLong("auftragStart")?.takeIf { it > 0 }?.let { put("auftragStart", it) } }
                    .put("nachrichten", JSONArray().apply {
                        nachrichten.forEach { n ->
                            put(JSONObject().put("mir", n.vonMir).put("text", n.text).apply {
                                // Nur Abweichungen speichern: Alte Dateien bleiben gültig, neue bleiben klein.
                                if (n.art != "antwort") put("art", n.art)
                                if (n.kontext.isNotEmpty()) put("kontext", n.kontext)
                                if (n.anhaenge.isNotEmpty()) put("anhaenge", JSONArray(n.anhaenge))
                                if (n.bewertung != 0) put("bewertung", n.bewertung)
                                if (n.tokens > 0) put("tokens", n.tokens)
                            })
                        }
                    })
                if (!json.optBoolean("programmierung") && istProgrammierung(json)) json.put("programmierung", true)
                schreiben(d, json)
                Sitzung(id, titel, erstellt, jetzt, nachrichten.size, zuProjekt, json.optBoolean("programmierung"), json.optLong("auftragStart"))
            }.onFailure { Log.w(TAG, "Gespräch nicht gespeichert", it) }.getOrNull()
        }
    }

    fun umbenennen(context: Context, id: String, titel: String) {
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val j = lies(d).put("titel", titel.trim()).put("eigenerTitel", true)
                schreiben(d, j)
            }.onFailure { Log.w(TAG, "Gespräch nicht umbenannt", it) }
        }
    }

    /**
     * Braucht das Gespräch (neu) eine KI-Überschrift? Ja nach der ersten Antwort und danach alle sechs Nachrichten,
     * damit sie mit dem Gespräch mitwächst, höchstens bis 20 Nachrichten. Ein von Hand vergebener Titel bleibt immer.
     */
    fun brauchtKiTitel(context: Context, id: String, anzahl: Int): Boolean = synchronized(sperre) {
        runCatching {
            val j = lies(datei(context, id))
            if (j.optBoolean("eigenerTitel")) return@runCatching false
            val bei = j.optInt("kiTitelBei")
            anzahl >= 2 && (bei == 0 || (anzahl >= bei + 6 && bei < 20))
        }.getOrDefault(false)
    }

    /** Übernimmt die KI-Überschrift, außer Frank hat inzwischen selbst umbenannt. Liefert den neuen Eintrag. */
    fun setzeKiTitel(context: Context, id: String, titel: String, bei: Int): Sitzung? = synchronized(sperre) {
        runCatching {
            val d = datei(context, id)
            val j = lies(d)
            if (j.optBoolean("eigenerTitel")) return@runCatching null
            j.put("titel", titel).put("kiTitel", titel).put("kiTitelBei", bei)
            schreiben(d, j)
            sitzungAus(j, id)
        }.onFailure { Log.w(TAG, "KI-Titel nicht gespeichert", it) }.getOrNull()
    }

    /**
     * Hängt eine Nachricht an ein gespeichertes Gespräch, das gerade nicht in der App offen ist (Meldungen eines
     * Programmierauftrags). Gibt es das Gespräch nicht mehr, passiert nichts.
     */
    fun anhaengen(context: Context, id: String, n: GespeicherteNachricht) {
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                if (!d.exists() && !File(d.path + ".bak").exists()) return@runCatching
                val j = lies(d)
                val liste = j.optJSONArray("nachrichten") ?: JSONArray().also { j.put("nachrichten", it) }
                // Dieselbe Rückfrage (frage_id) nur einmal: Der Hook wiederholt Meldungen, und der Abgleich mit dem Server reicht nach.
                if (n.art == "rueckfrage" && rueckfrageIndex(liste, rueckfrageId(n.kontext)) >= 0) return@runCatching
                // Dieselbe Modellwahl-Karte (wahl_id) nur einmal: Die App trägt fehlende Karten selbst nach.
                if (n.art == ProgrammierChat.ART_MODELLWAHL && wahlId(n.kontext).let { w -> w.isNotEmpty() &&
                        (0 until liste.length()).any { liste.optJSONObject(it)?.let { m -> m.optString("art") == n.art && wahlId(m.optString("kontext")) == w } == true } }) return@runCatching
                liste.put(JSONObject().put("mir", n.vonMir).put("text", n.text).apply {
                    if (n.art != "antwort") put("art", n.art)
                    if (n.kontext.isNotEmpty()) put("kontext", n.kontext)
                    if (n.anhaenge.isNotEmpty()) put("anhaenge", JSONArray(n.anhaenge))
                    if (n.tokens > 0) put("tokens", n.tokens)
                })
                j.put("geaendert", System.currentTimeMillis())
                if (n.art in PROGRAMMIER_ARTEN) j.put("programmierung", true)
                schreiben(d, j)
            }.onFailure { Log.w(TAG, "Nachricht nicht angehängt", it) }
        }
    }

    /** Hängt an die letzte Nachricht, wenn sie von Frank ist, einen Kontext (was in einer abgebrochenen Runde schon erledigt war), in der Datei. */
    fun letzteFrageKontext(context: Context, id: String, kontext: String) {
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val j = lies(d)
                val liste = j.optJSONArray("nachrichten") ?: return@runCatching
                // Die letzte Frage, nicht die letzte Nachricht: Statusmeldungen eines Programmierauftrags können dahinter stehen.
                val n = (liste.length() - 1 downTo 0).mapNotNull { liste.optJSONObject(it) }.firstOrNull { it.optBoolean("mir") } ?: return@runCatching
                n.put("kontext", kontext)
                schreiben(d, j)
            }.onFailure { Log.w(TAG, "Kontext der Frage nicht gesetzt", it) }
        }
    }

    /** Die frage_id im Kontext einer Rückfrage, leer ohne. */
    fun rueckfrageId(kontext: String): String = runCatching { JSONObject(kontext).optString("frage_id") }.getOrDefault("")

    /** Die wahl_id im Kontext einer Modellwahl-Karte, leer ohne. */
    fun wahlId(kontext: String): String = runCatching { JSONObject(kontext).optString("wahl_id") }.getOrDefault("")

    private fun rueckfrageIndex(liste: JSONArray, frageId: String): Int {
        if (frageId.isEmpty()) return -1
        for (i in liste.length() - 1 downTo 0) {
            val n = liste.optJSONObject(i) ?: continue
            if (n.optString("art") == "rueckfrage" && rueckfrageId(n.optString("kontext")) == frageId) return i
        }
        return -1
    }

    /**
     * Ändert den Kontext einer Rückfrage (art „rueckfrage“, kontext = JSON mit frage_id) in der Datei: Stand, Antwort, Entwurf.
     * [aenderung] liefert false, wenn sich nichts ändern soll. Ergebnis: true = die Rückfrage steht in diesem Gespräch.
     */
    fun rueckfrageAendern(context: Context, id: String, frageId: String, aenderung: (JSONObject) -> Boolean): Boolean = synchronized(sperre) {
        runCatching {
            val d = datei(context, id)
            if (!d.exists() && !File(d.path + ".bak").exists()) return@runCatching false
            val j = lies(d)
            val liste = j.optJSONArray("nachrichten") ?: return@runCatching false
            val i = rueckfrageIndex(liste, frageId)
            if (i < 0) return@runCatching false
            val n = liste.getJSONObject(i)
            val kontext = runCatching { JSONObject(n.optString("kontext")) }.getOrNull() ?: return@runCatching true
            if (aenderung(kontext)) {
                n.put("kontext", kontext.toString())
                schreiben(d, j)
            }
            true
        }.onFailure { Log.w(TAG, "Rückfrage nicht geändert", it) }.getOrDefault(false)
    }

    /** Der Kontext einer Rückfrage in der Datei, oder null. */
    fun rueckfrageKontext(context: Context, id: String, frageId: String): JSONObject? = synchronized(sperre) {
        runCatching {
            val liste = lies(datei(context, id)).optJSONArray("nachrichten") ?: return@runCatching null
            val i = rueckfrageIndex(liste, frageId)
            if (i < 0) null else JSONObject(liste.getJSONObject(i).optString("kontext"))
        }.getOrNull()
    }

    fun loeschen(context: Context, id: String) {
        synchronized(sperre) {
            // Die Anhänge des Gesprächs gehören nur ihm: mit löschen, sonst wüchse der Speicher mit jedem Foto.
            runCatching {
                val anhaenge = Anhaenge.ordner(context).canonicalPath
                val a = lies(datei(context, id)).optJSONArray("nachrichten") ?: JSONArray()
                for (i in 0 until a.length()) {
                    val l = a.optJSONObject(i)?.optJSONArray("anhaenge") ?: continue
                    for (k in 0 until l.length()) File(l.optString(k)).takeIf { it.canonicalPath.startsWith(anhaenge) }?.delete()
                }
            }
            AtomicFile(datei(context, id)).delete()
        }
    }

    /** Atomar mit fsync: Nach Absturz oder leerem Akku bleibt der alte oder der neue Stand, nie eine leere Datei. */
    internal fun schreiben(d: File, json: JSONObject) {
        val af = AtomicFile(d)
        val aus = af.startWrite()
        try {
            aus.write(json.toString().toByteArray(Charsets.UTF_8))
            af.finishWrite(aus)
        } catch (e: Exception) {
            af.failWrite(aus)
            throw e
        }
    }

    /** Titel aus Franks erster Frage: eine Zeile, höchstens 60 Zeichen. */
    private fun titelAus(nachrichten: List<GespeicherteNachricht>): String {
        val erste = nachrichten.firstOrNull { it.vonMir }?.text ?: nachrichten.first().text
        val zeile = erste.replace(Regex("\\s+"), " ").trim()
        return if (zeile.length <= 60) zeile else zeile.take(57).substringBeforeLast(' ', zeile.take(57)) + " …"
    }
}
