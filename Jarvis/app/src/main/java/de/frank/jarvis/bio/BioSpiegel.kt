package de.frank.jarvis.bio

import android.content.Context
import android.net.Uri
import de.frank.jarvis.bio.data.local.AppDatabase
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Spiegel der Biomarker vom Handy aufs Tablet, über den Geräteabgleich-Speicher des Jarvis-Servers (Dokument
 * `appdaten/biomarker`, die Datenbank als Datei nach ihrer Prüfsumme).
 *
 * Nur in EINE Richtung: Das Handy ist der einzige Abrufer (der Whoop-Zugang verträgt nur einen, Zepp und Health Connect gibt es
 * nur dort) und lädt nach jeder Änderung einen Schnappschuss hoch; das Tablet spielt ihn ein, wenn einer seiner Auslöser
 * feuert (Biomarker-Bereich öffnen, Aktualisieren, Tagesauswertung) und beim Start. Zugangsdaten reisen nie mit, nur Messwerte,
 * Abrufstand und Kartenreihenfolge. Nie während eines Sprachgesprächs.
 */
object BioSpiegel {
    private const val ART = "appdaten"
    private const val ID = "biomarker"
    private val sperre = Mutex()
    private var anstehend: Job? = null
    private val netz by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).build() }
    private val JSON = "application/json".toMediaType()

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("bio_spiegel", Context.MODE_PRIVATE)

    private fun basis(c: Context): String? {
        val e = Einstellungen.get(c)
        if (!e.geraeteAbgleichAn || e.serverHost.isBlank() || e.serverToken.isBlank()) return null
        return "https://${e.serverHost}/j/${e.geheimnis}/abgleich"
    }

    /** Beim Start des Dienstes: Das Handy lädt einen noch nicht gespiegelten Stand hoch, das Tablet holt den neuesten. */
    fun starte(context: Context) {
        val app = context.applicationContext
        if (Geraet.istTablet) BioModul.bereich.launch { einfrierenAufDemTablet(app); holen(app) } else geaendert(app, "Start")
    }

    /**
     * Tablet: Ist dort Entropie Reductor installiert, soll es ebenfalls nichts mehr abrufen und keinen Whoop-Token mehr tauschen
     * (sonst gäbe es zwei Whoop-Abrufer). Einmalig, sobald seine Fassung die Übergabe kennt.
     */
    private fun einfrierenAufDemTablet(app: Context) {
        val p = prefs(app)
        if (p.getBoolean("er_eingefroren", false)) return
        for (adresse in listOf("content://de.frank.entropyreducer.jarvis", "content://de.frank.entropyreducer.debug.jarvis")) {
            val antwort = runCatching {
                app.contentResolver.call(Uri.parse(adresse), "uebergabe_nur_einfrieren", null, android.os.Bundle())?.getString("json")?.let(::JSONObject)
            }.getOrNull() ?: continue
            if (antwort.optBoolean("ok")) {
                p.edit().putBoolean("er_eingefroren", true).apply()
                Logbuch.system("Biomarker", "Entropie Reductor auf dem Tablet ruft keine Biomarker mehr ab")
            }
            return
        }
    }

    /** Daten haben sich geändert (nur Handy): gesammelt nach einer kurzen Pause hochladen. */
    fun geaendert(context: Context, grund: String) {
        if (Geraet.istTablet) return
        val app = context.applicationContext
        if (Einstellungen.get(app).bioUebernommenMs == 0L) return
        synchronized(this) {
            anstehend?.cancel()
            anstehend = BioModul.bereich.launch {
                delay(15_000)
                while (de.frank.jarvis.speech.GespraechsFokus.sprachmodus) delay(30_000)
                // Ohne Netz (unterwegs, Tunnel weg) bis zu dreimal im Abstand von fünf Minuten neu versuchen.
                for (versuch in 1..4) {
                    val fehler = runCatching { hochladen(app) }.exceptionOrNull() ?: break
                    if (fehler !is IOException) { Logbuch.fehler("Biomarker-Spiegel", fehler, grund); break }
                    if (versuch == 4) { Logbuch.fehler("Biomarker-Spiegel", fehler, "$grund: nach vier Versuchen kein Netz, nächster Anstoß holt es nach"); break }
                    delay(5 * 60_000L)
                }
            }
        }
    }

    private suspend fun hochladen(app: Context) = sperre.withLock {
        val basis = basis(app) ?: return@withLock
        val datei = File(app.cacheDir, "bio_spiegel.db")
        try {
            datei.delete()
            BioModul.datenbank.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf(datei.absolutePath))
            val sha = sha256(datei)
            if (prefs(app).getString("hochgeladen", "") == sha) return@withLock
            netz.newCall(Request.Builder().url("$basis/blob/$sha").put(datei.asRequestBody("application/octet-stream".toMediaType())).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Spiegel hochladen: Server ${r.code}")
            }
            val e = BioModul.einstellungen
            val stand = JSONObject()
                .put("biomarker_sync_status_json", e.biomarkerSyncStatusFlow.first())
                .put("whoop_abruf_stand_json", e.whoopAbrufStandFlow.first().alsJson())
                .put("last_whoop_sync_ms", e.lastWhoopSyncMsFlow.first())
                .put("last_refresh_footer_text", e.lastRefreshFooterFlow.first())
                .put("last_refresh_footer_at_ms", e.lastRefreshFooterAtMsFlow.first())
            val karten = org.json.JSONArray(BioModul.kartenReihenfolge.rawSavedOrder.first())
            val daten = JSONObject().put("text_sha", sha).put("inhalt", sha).put("groesse", datei.length()).put("version", 1)
                .put("stand", stand).put("karten", karten).put("zeit", System.currentTimeMillis())
            val rumpf = JSONObject().put("daten", daten).put("geaendert", System.currentTimeMillis()).put("geloescht", false).put("von", Geraet.ablageName).toString()
            netz.newCall(Request.Builder().url("$basis/doc/$ART/$ID").put(rumpf.toRequestBody(JSON)).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Spiegel melden: Server ${r.code}")
            }
            prefs(app).edit().putString("hochgeladen", sha).apply()
            Logbuch.system("Biomarker-Spiegel", "Stand fürs Tablet hochgeladen (${datei.length() / 1024} KB)")
        } finally {
            datei.delete()
        }
    }

    /** Tablet: den neuesten Stand des Handys holen und einspielen. Ohne Netz bleibt der vorige Stand, mit seinem Datum. */
    suspend fun holen(context: Context) = sperre.withLock {
        val app = context.applicationContext
        if (!Geraet.istTablet) return@withLock
        val basis = basis(app) ?: return@withLock
        BioModul.einrichten(app)
        val p = prefs(app)
        var seit = p.getLong("seit", 0L)
        var neu: JSONObject? = null
        try {
            while (true) {
                val antwort = netz.newCall(Request.Builder().url("$basis/aenderungen?seit=$seit&geraet=${Uri.encode(Geraet.ablageName)}").build()).execute().use { r ->
                    if (!r.isSuccessful) throw IOException("Server ${r.code}")
                    JSONObject(r.body?.string().orEmpty())
                }
                // Neu aufgesetzter Speicher am Server: Der gemerkte Zeiger gilt nicht mehr, von vorn lesen.
                val instanz = antwort.optString("instanz")
                if (seit != 0L && (antwort.optBoolean("neuaufbau") || (p.getString("instanz", "").orEmpty().isNotEmpty() && p.getString("instanz", "") != instanz))) {
                    seit = 0L; p.edit().putString("instanz", instanz).putString("eingespielt", "").apply(); continue
                }
                p.edit().putString("instanz", instanz).apply()
                val docs = antwort.optJSONArray("docs")
                for (i in 0 until (docs?.length() ?: 0)) {
                    val d = docs!!.getJSONObject(i)
                    if (d.optString("art") == ART && d.optString("id") == ID && !d.optBoolean("geloescht")) neu = d
                }
                seit = antwort.optLong("stand")
                if (!antwort.optBoolean("mehr")) break
            }
        } catch (e: IOException) {
            return@withLock
        }
        val doc = neu ?: run { p.edit().putLong("seit", seit).apply(); return@withLock }
        val daten = doc.optJSONObject("daten") ?: return@withLock
        val sha = daten.optString("text_sha")
        if (sha.isEmpty() || sha == p.getString("eingespielt", "")) { p.edit().putLong("seit", seit).apply(); return@withLock }
        while (de.frank.jarvis.speech.GespraechsFokus.sprachmodus) delay(30_000)
        val datei = File(app.cacheDir, "bio_spiegel_fern.db")
        try {
            netz.newCall(Request.Builder().url("$basis/blob/$sha").build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Spiegel holen: Server ${r.code}")
                datei.outputStream().use { aus -> r.body!!.byteStream().copyTo(aus) }
            }
            if (sha256(datei) != sha) { Logbuch.fehler("Biomarker-Spiegel", null, "Der Stand vom Handy kam beschädigt an."); return@withLock }
            ersetze(datei)
            daten.optJSONObject("stand")?.let { BioModul.einstellungen.uebernimm(it) }
            daten.optJSONArray("karten")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() } }
                ?.takeIf { it.isNotEmpty() }?.let { BioModul.kartenReihenfolge.saveOrder(it) }
            p.edit().putString("eingespielt", sha).putLong("seit", seit).putLong("zeit", daten.optLong("zeit")).putString("von", doc.optString("von")).apply()
            BioAbruf.neuGelesen(app)
            Logbuch.system("Biomarker-Spiegel", "Stand vom Handy eingespielt (${datei.length() / 1024} KB)")
        } catch (e: IOException) {
            // Netz weg: nächster Auslöser.
        } finally {
            datei.delete()
        }
    }

    private fun ersetze(datei: File) { BioTabellenKopie.kopiere(datei) }

    /** Für das Tablet: von wann der eingespielte Stand des Handys ist. */
    fun standText(context: Context): String {
        val zeit = prefs(context).getLong("zeit", 0L)
        if (zeit == 0L) return "noch kein Stand vom Handy eingespielt"
        val z = java.time.Instant.ofEpochMilli(zeit).atZone(java.time.ZoneId.systemDefault())
        return "Stand vom Handy, %02d.%02d. um %02d:%02d Uhr".format(z.dayOfMonth, z.monthValue, z.hour, z.minute)
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ein -> val puffer = ByteArray(1 shl 16); while (true) { val n = ein.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
