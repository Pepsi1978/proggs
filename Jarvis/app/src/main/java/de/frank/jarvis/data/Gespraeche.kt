package de.frank.jarvis.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Ein gespeichertes Gespräch, wie es die Seitenleiste zeigt. */
data class Sitzung(val id: String, val titel: String, val erstellt: Long, val geaendert: Long, val anzahl: Int)

/**
 * Eine Nachricht im Speicher: von Frank oder von Jarvis. [art]: „antwort“ (normal), „fehler“ (Fehlermeldung statt
 * Antwort) oder „abgebrochen“ (Franks Frage blieb unbeantwortet). [kontext] sieht nur das Modell im Verlauf,
 * etwa was in einer abgebrochenen Runde schon erledigt war.
 */
data class GespeicherteNachricht(val vonMir: Boolean, val text: String, val art: String = "antwort", val kontext: String = "", val anhaenge: List<String> = emptyList(), val bewertung: Int = 0)

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

    /** Alle Gespräche, das zuletzt geführte zuerst. */
    fun alle(context: Context): List<Sitzung> =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { f ->
            runCatching {
                val j = lies(f)
                Sitzung(j.getString("id"), j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), j.optJSONArray("nachrichten")?.length() ?: 0)
            }.onFailure { Log.w(TAG, "Gespräch ${f.name} nicht lesbar", it) }.getOrNull()
        }.sortedByDescending { it.geaendert }

    fun laden(context: Context, id: String): Pair<Sitzung, List<GespeicherteNachricht>>? = runCatching {
        val j = lies(datei(context, id))
        val a = j.optJSONArray("nachrichten") ?: JSONArray()
        val nachrichten = (0 until a.length()).map {
            a.getJSONObject(it).let { n ->
                val anh = n.optJSONArray("anhaenge")?.let { l -> (0 until l.length()).map { k -> l.getString(k) } }.orEmpty()
                GespeicherteNachricht(n.optBoolean("mir"), n.optString("text"), n.optString("art", "antwort"), n.optString("kontext"), anh, n.optInt("bewertung"))
            }
        }
        Sitzung(id, j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), nachrichten.size) to nachrichten
    }.getOrNull()

    /**
     * Speichert das ganze Gespräch. Ein von Hand vergebener Titel bleibt erhalten. Liefert den neuen Eintrag für die
     * Seitenleiste (null = nichts geschrieben). Ein älterer Stand mit weniger Nachrichten überschreibt nie einen neueren.
     * [kuerzenUm]: So viele Nachrichten hat Frank bewusst entfernt (etwa die verworfene Antwort bei „Erneut versuchen“).
     */
    fun speichern(context: Context, id: String, erstellt: Long, nachrichten: List<GespeicherteNachricht>, kuerzenUm: Int = 0): Sitzung? {
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
                val json = JSONObject()
                    .put("id", id)
                    .put("titel", titel)
                    .put("eigenerTitel", eigenerTitel)
                    .put("kiTitel", kiTitel)
                    .put("kiTitelBei", alt?.optInt("kiTitelBei") ?: 0)
                    .put("erstellt", erstellt)
                    .put("geaendert", jetzt)
                    .put("nachrichten", JSONArray().apply {
                        nachrichten.forEach { n ->
                            put(JSONObject().put("mir", n.vonMir).put("text", n.text).apply {
                                // Nur Abweichungen speichern: Alte Dateien bleiben gültig, neue bleiben klein.
                                if (n.art != "antwort") put("art", n.art)
                                if (n.kontext.isNotEmpty()) put("kontext", n.kontext)
                                if (n.anhaenge.isNotEmpty()) put("anhaenge", JSONArray(n.anhaenge))
                                if (n.bewertung != 0) put("bewertung", n.bewertung)
                            })
                        }
                    })
                schreiben(d, json)
                Sitzung(id, titel, erstellt, jetzt, nachrichten.size)
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
            Sitzung(id, titel, j.optLong("erstellt"), j.optLong("geaendert"), j.optJSONArray("nachrichten")?.length() ?: 0)
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
                liste.put(JSONObject().put("mir", n.vonMir).put("text", n.text).apply {
                    if (n.art != "antwort") put("art", n.art)
                    if (n.kontext.isNotEmpty()) put("kontext", n.kontext)
                    if (n.anhaenge.isNotEmpty()) put("anhaenge", JSONArray(n.anhaenge))
                })
                j.put("geaendert", System.currentTimeMillis())
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

    /** Trägt bei einer Rückfrage (art „rueckfrage“, kontext = JSON mit frage_id) die Antwort ein, in der Datei. */
    fun rueckfrageBeantwortet(context: Context, id: String, frageId: String, antwort: String) {
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val j = lies(d)
                val liste = j.optJSONArray("nachrichten") ?: return@runCatching
                for (i in liste.length() - 1 downTo 0) {
                    val n = liste.getJSONObject(i)
                    if (n.optString("art") != "rueckfrage") continue
                    val kontext = runCatching { JSONObject(n.optString("kontext")) }.getOrNull() ?: continue
                    if (kontext.optString("frage_id") != frageId) continue
                    n.put("kontext", kontext.put("beantwortet", true).put("antwort", antwort).toString())
                    schreiben(d, j)
                    return@runCatching
                }
            }.onFailure { Log.w(TAG, "Rückfrage nicht als beantwortet markiert", it) }
        }
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
    private fun schreiben(d: File, json: JSONObject) {
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
