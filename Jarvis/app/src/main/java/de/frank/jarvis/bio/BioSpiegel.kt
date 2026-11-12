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
 * nur dort) und lädt nach jeder Änderung einen Schnappschuss hoch; das Tablet schaut alle fünf Minuten nach und spielt ihn ein, zusätzlich bei seinen Auslösern
 * (Biomarker-Bereich öffnen, Aktualisieren, Tagesauswertung) und beim Start. Zugangsdaten reisen nie mit, nur Messwerte,
 * Abrufstand und Kartenreihenfolge. Nie während eines Sprachgesprächs.
 */
object BioSpiegel {
    private const val ART = "appdaten"
    private const val ID = "biomarker"
    private val sperre = Mutex()
    /** Tablet: zählt jedes Einspielen eines neuen Handy-Stands, damit offene Anzeigen (Startkarte, Abrufstand) sofort neu lesen. */
    private val _eingespielt = kotlinx.coroutines.flow.MutableStateFlow(0)
    val eingespielt: kotlinx.coroutines.flow.StateFlow<Int> = _eingespielt
    private var anstehend: Job? = null
    private val netz by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).build() }
    private val JSON = "application/json".toMediaType()

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("bio_spiegel", Context.MODE_PRIVATE)

    private fun basis(c: Context): String? {
        val e = Einstellungen.get(c)
        if (!e.geraeteAbgleichAn || e.serverHost.isBlank() || e.serverToken.isBlank()) return null
        return "https://${e.serverHost}/j/${e.geheimnis}/abgleich"
    }

    /** Tablet: die ständige Nachschau, ob das Handy einen neuen Stand hochgeladen hat. */
    private var nachschau: Job? = null
    /** Abstand der Nachschau auf dem Tablet. Die Frage an den Server ist billig; die Datenbank kommt nur, wenn sie sich geändert hat. */
    private const val NACHSCHAU_MS = 5 * 60_000L

    /**
     * Beim Start des Dienstes: Das Handy lädt einen noch nicht gespiegelten Stand hoch. Das Tablet holt den neuesten und schaut
     * danach alle fünf Minuten nach, damit jede Aktualisierung am Handy (Knopf, Sprache, 9 und 21 Uhr) von selbst ankommt, auch
     * wenn auf dem Tablet niemand etwas antippt und dort keine eigene Tagesauswertung läuft.
     */
    fun starte(context: Context) {
        val app = context.applicationContext
        if (!Geraet.istTablet) { geaendert(app, "Start"); return }
        synchronized(this) {
            if (nachschau?.isActive == true) return
            nachschau = BioModul.bereich.launch {
                einfrierenAufDemTablet(app)
                while (true) {
                    holen(app)
                    delay(NACHSCHAU_MS)
                }
            }
        }
    }

    /** Tablet: sofort nachschauen (etwa beim Öffnen des Biomarker-Bereichs). Ruft nie selbst bei den Quellen ab. */
    fun jetztNachschauen(context: Context) {
        if (!Geraet.istTablet) return
        val app = context.applicationContext
        BioModul.bereich.launch { holen(app) }
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
                    val fehler = try { hochladen(app); null } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (e: Exception) { e } ?: break
                    if (fehler !is IOException) { Logbuch.fehler("Biomarker-Spiegel", fehler, grund); break }
                    if (versuch == 4) { Logbuch.fehler("Biomarker-Spiegel", fehler, "$grund: nach vier Versuchen kein Netz, nächster Anstoß holt es nach"); break }
                    delay(5 * 60_000L)
                }
            }
        }
    }

    /** Der Stand, der neben den Messwerten mitreist: Abrufbericht, Whoop-Stand, letzte Abrufe je Quelle, Fußzeile. Nie Zugänge. */
    private suspend fun standJson(): JSONObject {
        val e = BioModul.einstellungen
        return JSONObject()
            .put("biomarker_sync_status_json", e.biomarkerSyncStatusFlow.first())
            .put("whoop_abruf_stand_json", e.whoopAbrufStandFlow.first().alsJson())
            .put("last_whoop_sync_ms", e.lastWhoopSyncMsFlow.first())
            .put("last_oura_sync_ms", e.lastOuraSyncMsFlow.first())
            .put("last_amazfit_sync_ms", e.lastAmazfitSyncMsFlow.first())
            .put("last_zepp_body_sync_ms", e.lastZeppBodySyncMsFlow.first())
            .put("whoop_luecken_geprueft_bis", e.whoopLueckenGeprueftBisFlow.first())
            .put("last_refresh_footer_text", e.lastRefreshFooterFlow.first())
            .put("last_refresh_footer_at_ms", e.lastRefreshFooterAtMsFlow.first())
    }

    /** Billiger Fingerabdruck der Datenbank (Zeilen und neueste Zeitstempel je Tabelle), damit nicht jeder Anstoß einen ganzen Schnappschuss baut. */
    private fun dbMarke(): String = runCatching {
        val db = BioModul.datenbank.openHelper.readableDatabase
        AppDatabase.TABELLEN.joinToString("|") { t -> db.query("SELECT COUNT(*), TOTAL(rowid) FROM `$t`").use { c -> c.moveToFirst(); "${c.getLong(0)}:${c.getDouble(1)}" } } +
            "|" + db.query("SELECT TOTAL(capturedAt) FROM biomarker_snapshots").use { c -> c.moveToFirst(); c.getDouble(0).toString() } +
            "|" + db.query("SELECT TOTAL(COALESCE(distanceMeters,0)) + TOTAL(COALESCE(calories,0)) FROM amazfit_workouts").use { c -> c.moveToFirst(); c.getDouble(0).toString() }
    }.getOrDefault(System.currentTimeMillis().toString())

    private suspend fun hochladen(app: Context) = sperre.withLock {
        val basis = basis(app) ?: return@withLock
        val p = prefs(app)
        val stand = standJson()
        val karten = org.json.JSONArray(BioModul.kartenReihenfolge.rawSavedOrder.first())
        val marke = dbMarke() + "#" + sha256Text(stand.toString() + karten.toString())
        if (p.getString("marke", "") == marke) return@withLock
        val datei = File(app.cacheDir, "bio_spiegel.db")
        try {
            datei.delete()
            BioModul.datenbank.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf(datei.absolutePath))
            val sha = sha256(datei)
            // Messwerte unverändert: kein neuer Blob, aber ein geänderter Abrufstand (etwa eine abgelaufene Anmeldung) geht trotzdem rüber.
            if (p.getString("hochgeladen", "") != sha) {
                netz.newCall(Request.Builder().url("$basis/blob/$sha").put(datei.asRequestBody("application/octet-stream".toMediaType())).build()).execute().use { r ->
                    if (!r.isSuccessful) throw IOException("Spiegel hochladen: Server ${r.code}")
                }
            }
            val inhalt = sha256Text(sha + stand.toString() + karten.toString())
            val daten = JSONObject().put("text_sha", sha).put("inhalt", inhalt).put("groesse", datei.length()).put("version", 1)
                .put("stand", stand).put("karten", karten).put("zeit", System.currentTimeMillis())
            val rumpf = JSONObject().put("daten", daten).put("geaendert", System.currentTimeMillis()).put("geloescht", false).put("von", Geraet.ablageName).toString()
            netz.newCall(Request.Builder().url("$basis/doc/$ART/$ID").put(rumpf.toRequestBody(JSON)).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Spiegel melden: Server ${r.code}")
            }
            p.edit().putString("hochgeladen", sha).putString("marke", marke).apply()
            Logbuch.system("Biomarker-Spiegel", "Stand fürs Tablet hochgeladen (${datei.length() / 1024} KB)")
        } finally {
            datei.delete()
        }
    }

    /** Tablet: den neuesten Stand des Handys holen und einspielen. Ohne Netz bleibt der vorige Stand, mit seinem Datum. Wirft nie. */
    suspend fun holen(context: Context) {
        try {
            holenGesperrt(context.applicationContext)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IOException) {
            // Netz weg: die nächste Nachschau holt es nach. Einmal je Ausfall ins Logbuch, damit ein hängender Spiegel auffällt.
            if (!netzFehlerGemeldet) { netzFehlerGemeldet = true; Logbuch.system("Biomarker-Spiegel", "Stand vom Handy gerade nicht erreichbar: ${e.message}") }
        } catch (e: Exception) {
            // Kaputter Stand, anderes Schema, unerwartete Antwort: im Logbuch, aber nie ein Absturz von Jarvis.
            Logbuch.fehler("Biomarker-Spiegel", e, "Stand vom Handy nicht eingespielt")
        }
    }

    @Volatile private var netzFehlerGemeldet = false

    private suspend fun holenGesperrt(app: Context) = sperre.withLock {
        if (!Geraet.istTablet) return@withLock
        val basis = basis(app) ?: return@withLock
        BioModul.einrichten(app)
        val p = prefs(app)
        var seit = p.getLong("seit", 0L)
        var neu: JSONObject? = null
        var runden = 0
        while (runden++ < 200) {
            val antwort = netz.newCall(Request.Builder().url("$basis/aenderungen?seit=$seit&geraet=${Uri.encode(Geraet.ablageName)}").build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Server ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            netzFehlerGemeldet = false
            // Neu aufgesetzter Speicher am Server: Der gemerkte Zeiger gilt nicht mehr, von vorn lesen.
            val instanz = antwort.optString("instanz")
            if (seit != 0L && (antwort.optBoolean("neuaufbau") || (p.getString("instanz", "").orEmpty().isNotEmpty() && p.getString("instanz", "") != instanz))) {
                seit = 0L; p.edit().putString("instanz", instanz).putString("eingespielt", "").putString("eingespielt_db", "").apply(); continue
            }
            p.edit().putString("instanz", instanz).apply()
            val docs = antwort.optJSONArray("docs")
            for (i in 0 until (docs?.length() ?: 0)) {
                val d = docs!!.getJSONObject(i)
                if (d.optString("art") == ART && d.optString("id") == ID && !d.optBoolean("geloescht")) neu = d
            }
            val weiter = antwort.optLong("stand")
            // Kein Fortschritt trotz „mehr“: abbrechen statt endlos zu fragen.
            if (!antwort.optBoolean("mehr") || weiter == seit) { seit = weiter; break }
            seit = weiter
        }
        val doc = neu ?: run { p.edit().putLong("seit", seit).apply(); return@withLock }
        val daten = doc.optJSONObject("daten") ?: return@withLock
        val sha = daten.optString("text_sha")
        val inhalt = daten.optString("inhalt").ifEmpty { sha }
        if (sha.isEmpty() || inhalt == p.getString("eingespielt", "")) { p.edit().putLong("seit", seit).apply(); return@withLock }
        while (de.frank.jarvis.speech.GespraechsFokus.sprachmodus) delay(30_000)
        // Messwerte nur neu einspielen, wenn sich die Datenbank geändert hat; der Abrufstand kommt immer mit.
        if (sha != p.getString("eingespielt_db", "")) {
            val datei = File(app.cacheDir, "bio_spiegel_fern.db")
            try {
                netz.newCall(Request.Builder().url("$basis/blob/$sha").build()).execute().use { r ->
                    if (!r.isSuccessful) throw IOException("Spiegel holen: Server ${r.code}")
                    val koerper = r.body ?: throw IOException("Spiegel holen: leere Antwort")
                    datei.outputStream().use { aus -> koerper.byteStream().copyTo(aus) }
                }
                if (sha256(datei) != sha) { Logbuch.fehler("Biomarker-Spiegel", null, "Der Stand vom Handy kam beschädigt an."); return@withLock }
                ersetze(datei)
                p.edit().putString("eingespielt_db", sha).apply()
                Logbuch.system("Biomarker-Spiegel", "Stand vom Handy eingespielt (${datei.length() / 1024} KB)")
            } finally {
                datei.delete()
            }
        }
        daten.optJSONObject("stand")?.let { BioModul.einstellungen.uebernimm(it) }
        BioModul.sync.vergiss()
        daten.optJSONArray("karten")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() } }
            ?.takeIf { it.isNotEmpty() }?.let { BioModul.kartenReihenfolge.saveOrder(it) }
        p.edit().putString("eingespielt", inhalt).putLong("seit", seit).putLong("zeit", daten.optLong("zeit")).putString("von", doc.optString("von")).apply()
        _eingespielt.value++
        BioAbruf.neuGelesen(app)
    }

    private fun sha256Text(t: String): String = MessageDigest.getInstance("SHA-256").digest(t.toByteArray()).joinToString("") { "%02x".format(it) }

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
