package de.frank.jarvis.staubsauger

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder

/**
 * Erweiterte Steuerung des M7 Pro über die Proscenic-Cloud für die Staubsauger-Karte: Erreichbarkeit, Karte mit Fahrweg,
 * Räume und Sperrzonen, Zeitpläne, Einstellungen (Saugstufe, Lautstärke, LED, automatische Staubsammlung) und Verlauf.
 * Endpunkte und Felder stammen aus der Proscenic-App 1.5.5 (Retrofit-Schnittstelle `RemoteService`, ausgelesen am 14.11.2026)
 * und einem Mitschnitt des Gateways: Leseanfragen (`cmd20001`, `cmd21002`, `cmd21004`, `21011`) antworten nur mit
 * `data: null`, die eigentliche Antwort kommt verschlüsselt über das TCP-Gateway.
 */
object ProscenicMehr {

    /** Aufbereitete Karte: Pixel 0 = Wand, 127 = unbekannt, 255 = Boden; Koordinaten der Cloud in Millimetern. */
    class Karte(val breite: Int, val hoehe: Int, val aufloesung: Double, val xMin: Double, val yMin: Double, val bild: Bitmap) {
        /** Millimeter → Bildpunkt (y zeigt nach oben, das Bild ist dafür gespiegelt). */
        fun px(xMm: Int): Float = ((xMm / 1000.0 - xMin) / aufloesung).toFloat()
        fun py(yMm: Int): Float = (hoehe - 1 - (yMm / 1000.0 - yMin) / aufloesung).toFloat()
        fun mmX(px: Float): Int = ((px * aufloesung + xMin) * 1000).toInt()
        fun mmY(py: Float): Int = (((hoehe - 1 - py) * aufloesung + yMin) * 1000).toInt()
    }

    /** Ein Raum oder eine Sperrzone aus 21004 (rechteckig, Ecken in Millimetern). */
    data class Zone(val json: JSONObject) {
        val sperre: Boolean get() = json.optString("active") == "forbid"
        val name: String get() = json.optString("tag")
        val ecken: List<Pair<Int, Int>> get() = json.optJSONArray("vertexs")?.let { a ->
            (0 until a.length()).mapNotNull { i -> a.optJSONArray(i)?.let { it.optInt(0) to it.optInt(1) } }
        }.orEmpty()
    }

    data class Zeitplan(val aktiv: Boolean, val startSek: Int, val tage: List<Int>, val stufe: String, val roh: JSONObject)

    data class Live(
        val modus: String?, val akku: Int?, val saugstufe: String?, val lautstaerke: Int?, val led: Boolean?, val staubFrequenz: Int?,
        val karte: Karte?, val pfad: List<Pair<Int, Int>>, val roboter: Pair<Int, Int>?, val station: Pair<Int, Int>?,
        val mapId: Long?, val zonen: List<Zone>, val zeitZone: Int, val zeitZoneSek: Int, val zeitplaene: List<Zeitplan>,
        /** Kamen Zeitpläne (21002) bzw. Räume und Sperrzonen (21004) wirklich vom Roboter? Nur dann darf Jarvis sie zurückschreiben. */
        val zeitplaeneGelesen: Boolean = false, val zonenGelesen: Boolean = false,
    ) {
        /**
         * Diese Karte mit dem Zustand (Modus, Akku, Einstellungen, Position) aus [neu]. Zeitpläne und Zonen kommen aus [neu], wenn
         * es sie frisch gelesen hat; die Gelesen-Marken gelten immer nur für den letzten Abruf, damit nie ein älterer Stand
         * als frisch zurückgeschrieben wird.
         */
        fun mitZustand(neu: Live) = copy(modus = neu.modus, akku = neu.akku, saugstufe = neu.saugstufe, lautstaerke = neu.lautstaerke,
            led = neu.led, staubFrequenz = neu.staubFrequenz, roboter = neu.roboter ?: roboter,
            zeitplaene = if (neu.zeitplaeneGelesen) neu.zeitplaene else zeitplaene,
            zeitZone = if (neu.zeitplaeneGelesen) neu.zeitZone else zeitZone, zeitZoneSek = if (neu.zeitplaeneGelesen) neu.zeitZoneSek else zeitZoneSek,
            zonen = if (neu.zonenGelesen) neu.zonen else zonen, mapId = if (neu.zonenGelesen) neu.mapId ?: mapId else mapId,
            zeitplaeneGelesen = neu.zeitplaeneGelesen, zonenGelesen = neu.zonenGelesen)
    }

    data class Lauf(val start: Long, val dauerSek: Int, val flaeche: Int)
    data class Verlauf(val laeufe: List<Lauf>, val gesamtMin: Int?, val gesamtFlaeche: Int?, val anzahl: Int?)

    private inline fun <T> mitSitzung(context: Context, block: (ProscenicCloud.Sitzung) -> T): T {
        val s = ProscenicCloud.sitzung(context, neu = false)
        return try { block(s) } catch (e: ProscenicCloud.TokenUngueltig) { block(ProscenicCloud.sitzung(context, neu = true)) }
    }

    private fun u(s: ProscenicCloud.Sitzung) = "?username=" + URLEncoder.encode(s.mail, "UTF-8")
    private val leerJson get() = "{}".toRequestBody("application/json;charset=UTF-8".toMediaType())
    private fun json(o: Any) = o.toString().toRequestBody("application/json;charset=UTF-8".toMediaType())

    /** Ist der Roboter gerade mit der Proscenic-Cloud verbunden? null = Cloud selbst nicht erreichbar. */
    suspend fun erreichbar(context: Context): Boolean? = withContext(Dispatchers.IO) {
        // online() meldet ein abgelaufenes Token; mitSitzung meldet dann neu an und fragt noch einmal.
        runCatching { mitSitzung(context) { s -> ProscenicCloud.online(s) } }.getOrNull()
    }

    /**
     * Holt über eine kurze Gateway-Sitzung den ganzen Stand: Status (20001), Karte (20002), Räume und Sperrzonen (21004),
     * Fahrweg (21011) und Zeitpläne (21002). [mitKarte] = false holt nur den Status.
     */
    suspend fun live(context: Context, mitKarte: Boolean = true): Live? = withContext(Dispatchers.IO) {
        runCatching {
            var mail = ""
            val daten = mitSitzung(context) { s ->
                mail = s.mail
                // Offline? Dann gar nicht erst 14 Sekunden aufs Gateway warten.
                if (ProscenicCloud.online(s) == false) null else lesen(s, mitKarte)
            } ?: return@runCatching null
            // Vollständig mit Karte gelesen (Status, Karte, Zeitpläne): die Rohdaten zwischenspeichern, damit die Karte beim
            // nächsten Aufklappen sofort da ist. Ein unvollständiger Lauf überschreibt den Speicher nicht.
            if (daten.keys.containsAll(listOf(20001, 20002, 21002))) kartenSpeichern(context, daten, mail)
            // Ein frischer Status kommt nur von einem verbundenen Roboter.
            if (daten.containsKey(20001)) SaugerStand.erreichbar = true
            bauen(daten)
        }.onFailure { Logbuch.netz("Proscenic: Live-Stand fehlt", it.toString().take(200)) }.getOrNull()
    }

    // ---- Zwischengespeicherte Karte ----

    private fun kartenDatei(context: Context) = java.io.File(context.applicationContext.filesDir, "staubsauger_karte.json")

    private fun kartenPrefs(context: Context) = context.applicationContext.getSharedPreferences("staubsauger", Context.MODE_PRIVATE)

    /** Gehört der Speicher zum jetzt eingetragenen Proscenic-Konto? Nach einem Kontowechsel gilt er nicht mehr. */
    private fun speicherGilt(context: Context) =
        kartenPrefs(context).getString("karte_mail", null) == SecureSettings.get(context).proscenicMail.trim()

    /** Wann zuletzt eine Karte bei stehendem Roboter (also nach der Reinigung) gespeichert wurde, ms seit 1970; 0 = noch nie. */
    fun kartenStand(context: Context): Long = if (speicherGilt(context)) kartenPrefs(context).getLong("karte_fertig_ms", 0L) else 0L

    @Synchronized
    private fun kartenSpeichern(context: Context, daten: Map<Int, JSONObject>, mail: String) {
        runCatching {
            val j = JSONObject(); daten.forEach { (k, v) -> j.put(k.toString(), v) }
            val ziel = kartenDatei(context); val tmp = java.io.File(ziel.path + ".tmp")
            tmp.writeText(j.toString())
            if (!tmp.renameTo(ziel)) { tmp.delete(); return@runCatching }
            // Eine Karte mitten in der Reinigung zählt nicht als fertig: Sonst holte der Wächter die Endkarte nie.
            val e = kartenPrefs(context).edit().putString("karte_mail", mail)
            if (!aktiv(daten[20001]?.optString("mode"))) e.putLong("karte_fertig_ms", System.currentTimeMillis())
            e.apply()
        }.onFailure { Logbuch.fehler("Proscenic: Karte speichern", it) }
    }

    /** Die zuletzt gespeicherte Karte samt Stand, ohne Netz. */
    suspend fun ausSpeicher(context: Context): Live? = withContext(Dispatchers.IO) {
        runCatching {
            if (!speicherGilt(context)) return@runCatching null
            val d = kartenDatei(context).takeIf { it.exists() } ?: return@runCatching null
            val j = JSONObject(d.readText())
            val daten = HashMap<Int, JSONObject>()
            j.keys().forEach { k -> k.toIntOrNull()?.let { n -> j.optJSONObject(k)?.let { daten[n] = it } } }
            // Nur Karte, Fahrweg, Zonen und Zeitpläne aus dem Speicher; Zustand, Akku und Einstellungen kommen frisch.
            val roboter = daten[20001]?.optJSONArray("pos")
            daten.remove(20001)
            // Gespeicherte Zeitpläne und Zonen sind nicht frisch vom Roboter: nur anzeigen, nie als Grundlage zum Zurückschreiben.
            bauen(daten).copy(roboter = roboter?.let { it.optInt(0) to it.optInt(1) }, zeitplaeneGelesen = false, zonenGelesen = false)
        }.getOrNull()
    }

    /** Ende (ms) der letzten abgeschlossenen Reinigung laut Verlauf der Cloud; null = nicht lesbar. */
    suspend fun letzteReinigungEnde(context: Context): Long? = withContext(Dispatchers.IO) {
        runCatching {
            mitSitzung(context) { s ->
                val r = ProscenicCloud.post("/app/cleanRobot/records", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).add("page", "0").add("size", "1").build())
                if (r.optInt("code") == 102) throw ProscenicCloud.TokenUngueltig()
                r.optJSONObject("data")?.optJSONArray("content")?.optJSONObject(0)?.let { (it.optLong("startTime") + it.optInt("cleanTime")) * 1000 } ?: 0L
            }
        }.getOrNull()
    }

    /** Läuft der Roboter gerade (reinigt, pausiert oder fährt zurück)? Unbekannte Modi zählen als Arbeit, nur bekannte Ruhezustände nicht. */
    fun aktiv(modus: String?) = !modus.isNullOrBlank() && modus !in setOf("charge", "fullcharge", "idle", "fault", "sleep", "dormant", "DustCenterWorking")
    fun pausiert(modus: String?) = modus == "pause" || modus == "findchargerpause"

    private fun lesen(s: ProscenicCloud.Sitzung, mitKarte: Boolean): HashMap<Int, JSONObject>? {
        val adr = ProscenicCloud.post("/appInit/getSockAddr", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build())
        if (adr.optInt("code") == 102) throw ProscenicCloud.TokenUngueltig()
        val e = adr.optJSONObject("data")?.optJSONArray("addr_list")?.optJSONObject(0) ?: return null
        val daten = HashMap<Int, JSONObject>()
        val gesucht = if (mitKarte) setOf(20001, 20002, 21004, 21011, 21002) else setOf(20001)
        Socket().use { sock ->
            sock.connect(InetSocketAddress(e.optString("ip"), e.optInt("port")), 10_000)
            sock.soTimeout = 800
            val an = JSONObject().put("data", JSONObject().put("token", s.token).put("sn", s.sn)).put("infoType", 70001).toString() + ProscenicCloud.ENDE
            sock.getOutputStream().apply { write(an.toByteArray()); flush() }
            val ein = sock.getInputStream(); val puffer = ByteArrayOutputStream(); val buf = ByteArray(65536)
            val start = System.currentTimeMillis(); var angefragt = false
            while (System.currentTimeMillis() - start < 14_000 && !daten.keys.containsAll(gesucht)) {
                if (!angefragt && System.currentTimeMillis() - start > 800) {
                    angefragt = true
                    val q = u(s)
                    runCatching { ProscenicCloud.post("/instructions/cmd20001/${s.sn}$q", s.token, leerJson) }
                    if (mitKarte) {
                        runCatching { ProscenicCloud.post("/instructions/cmd21004/${s.sn}$q", s.token, leerJson) }
                        runCatching { ProscenicCloud.post("/instructions/cmd21002/${s.sn}$q", s.token, leerJson) }
                        runCatching { ProscenicCloud.post("/app/cleanRobot/21011/${s.sn}/0", s.token, FormBody.Builder().add("username", s.mail).add("pathId", "0").build()) }
                    }
                }
                val n = try { ein.read(buf) } catch (_: java.net.SocketTimeoutException) { continue }
                if (n < 0) break
                puffer.write(buf, 0, n)
                var text = puffer.toString("UTF-8")
                while (true) {
                    val i = text.indexOf(ProscenicCloud.ENDE); if (i < 0) break
                    val rahmen = text.substring(0, i); text = text.substring(i + ProscenicCloud.ENDE.length)
                    val j = ProscenicCloud.nachricht(rahmen, s.token) ?: continue
                    val typ = j.optInt("infoType")
                    val d = j.optJSONObject("data") ?: continue
                    // Der Fahrweg kommt auch leer in einer Sammelnachricht (30000); nur einen mit Punkten übernehmen.
                    if (typ == 21011 && (d.optJSONArray("posArray")?.length() ?: 0) == 0 && daten.containsKey(21011)) continue
                    daten[typ] = d
                }
                puffer.reset(); puffer.write(text.toByteArray())
            }
        }
        // Ohne Status (20001) hat der Roboter nicht geantwortet (offline oder WLAN-Aussetzer); was dann noch kam, ist nur
        // der Stand, den die Cloud zwischengespeichert hat, etwa die Karte. Das gilt nicht als frischer Stand.
        if (!daten.containsKey(20001)) { Logbuch.netz("Proscenic: kein Status vom Roboter", "Rahmen: ${daten.keys}"); return null }
        return daten
    }

    private fun bauen(daten: Map<Int, JSONObject>): Live {
        val st = daten[20001]
        val k = daten[20002]
        val zonenQuelle = daten[21004]?.optJSONArray("value") ?: k?.optJSONArray("area")
        val zp = daten[21002]
        return Live(
            modus = st?.optString("mode"), akku = st?.optInt("elec", -1)?.takeIf { it in 0..100 },
            saugstufe = st?.optString("workNoisy")?.ifBlank { null },
            lautstaerke = st?.optInt("vol", -1)?.takeIf { it >= 0 }, led = st?.takeIf { it.has("led") }?.optInt("led")?.let { it == 1 },
            staubFrequenz = st?.takeIf { it.has("dustCenterFreq") }?.optInt("dustCenterFreq"),
            karte = k?.let { runCatching { karte(it) }.onFailure { Logbuch.netz("Proscenic: Karte unlesbar", it.toString()) }.getOrNull() },
            pfad = daten[21011]?.optJSONArray("posArray")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONArray(it)?.let { p -> p.optInt(0) to p.optInt(1) } } }.orEmpty(),
            roboter = st?.optJSONArray("pos")?.let { it.optInt(0) to it.optInt(1) },
            station = k?.optJSONArray("chargeHandlePos")?.let { it.optInt(0) to it.optInt(1) },
            mapId = (daten[21004] ?: k)?.optLong("mapId"),
            zonen = zonenQuelle?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::Zone) } }.orEmpty(),
            zeitZone = zp?.optInt("timeZone", 1) ?: 1, zeitZoneSek = zp?.optInt("timeZoneSec", 3600) ?: 3600,
            zeitplaene = zp?.optJSONArray("value")?.let { a ->
                (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { z ->
                    Zeitplan(z.optBoolean("active"), z.optInt("startTime"),
                        z.optJSONArray("period")?.let { p -> (0 until p.length()).map { p.optInt(it) } }.orEmpty(), z.optString("workNoisy", "auto"), z)
                }
            }.orEmpty(),
            zeitplaeneGelesen = zp != null, zonenGelesen = daten[21004] != null,
        )
    }

    internal fun karte(d: JSONObject): Karte {
        val b = d.optInt("width"); val h = d.optInt("height")
        val roh = Base64.decode(d.optString("map").replace(' ', '+'), Base64.DEFAULT)
        val px = lz4(roh, b * h)
        val farben = IntArray(b * h)
        for (y in 0 until h) for (x in 0 until b) {
            val v = px.getOrElse(y * b + x) { 127 }.toInt() and 0xFF
            // Gespiegelt, damit y nach oben zeigt.
            farben[(h - 1 - y) * b + x] = when (v) { 255 -> 0xFFD9E4EE.toInt(); 0 -> 0xFF5B6B7A.toInt(); else -> 0x00000000 }
        }
        val bild = Bitmap.createBitmap(farben, b, h, Bitmap.Config.ARGB_8888)
        return Karte(b, h, d.optDouble("resolution", 0.05), d.optDouble("x_min"), d.optDouble("y_min"), bild)
    }

    /** Einfacher LZ4-Blockdecoder (ohne Rahmen), wie ihn die Karte des M7 Pro nutzt. */
    private fun lz4(src: ByteArray, laenge: Int): ByteArray {
        val dst = ByteArray(laenge); var i = 0; var o = 0
        while (i < src.size) {
            val tok = src[i++].toInt() and 0xFF
            var l = tok ushr 4
            if (l == 15) { while (true) { val z = src[i++].toInt() and 0xFF; l += z; if (z != 255) break } }
            System.arraycopy(src, i, dst, o, minOf(l, dst.size - o)); i += l; o += l
            if (i >= src.size || o >= dst.size) break
            val off = (src[i].toInt() and 0xFF) or ((src[i + 1].toInt() and 0xFF) shl 8); i += 2
            var m = tok and 15
            if (m == 15) { while (true) { val z = src[i++].toInt() and 0xFF; m += z; if (z != 255) break } }
            m += 4
            val st = o - off
            for (k in 0 until m) { if (o >= dst.size) break; dst[o] = dst[st + k]; o++ }
        }
        return dst
    }

    // ---- Einstellungen ----

    /** Führt einen Befehl aus: wartet kurz auf einen gerade weggefallenen Roboter, meldet bei abgelaufenem Token neu an. */
    private suspend fun mitErgebnis(context: Context, erfolg: String, block: (ProscenicCloud.Sitzung) -> Unit): SaugerAusgang = try {
        var s = ProscenicCloud.sitzung(context, neu = false)
        try { ProscenicCloud.roboterBereit(s) } catch (e: ProscenicCloud.TokenUngueltig) { s = ProscenicCloud.sitzung(context, neu = true); ProscenicCloud.roboterBereit(s) }
        try { block(s) } catch (e: ProscenicCloud.TokenUngueltig) { block(ProscenicCloud.sitzung(context, neu = true)) }
        SaugerAusgang(erfolg)
    } catch (e: ProscenicCloud.Abgewiesen) { SaugerAusgang(e.message.orEmpty(), true) }
    catch (e: ProscenicCloud.TokenUngueltig) { SaugerAusgang("Die Proscenic-Cloud lehnt die Anmeldung von Jarvis ab; trag E-Mail und Passwort unter Konten, Proscenic neu ein.", true) }
    catch (e: kotlinx.coroutines.CancellationException) { throw e }
    catch (e: Exception) { SaugerAusgang("Die Proscenic-Cloud war vom Handy aus nicht erreichbar (${e.message ?: e.javaClass.simpleName}); prüf die Internetverbindung des Handys.", true) }

    /** Saugstufe: quiet, auto oder strong. */
    suspend fun saugstufe(context: Context, stufe: String) = withContext(Dispatchers.IO) {
        mitErgebnis(context, "Saugstufe umgestellt.") { s -> ProscenicCloud.senden(s, "/instructions/${s.sn}/21022${u(s)}", FormBody.Builder().add("username", s.mail).add("setMode", stufe).build()) }
    }

    suspend fun pauseOderWeiter(context: Context, weiter: Boolean) = withContext(Dispatchers.IO) {
        mitErgebnis(context, if (weiter) "Reinigung fortgesetzt." else "Pausiert.") { s ->
            val f = FormBody.Builder().add("username", s.mail).apply { if (weiter) add("pauseOrContinue", "continue") else add("mode", "pause") }.build()
            ProscenicCloud.senden(s, "/instructions/${s.sn}/21017${u(s)}", f)
        }
    }

    /** Befehle mit `cmd`/`value` (wie die Proscenic-App): setVolume 0–10, setledswitch 0/1. */
    private fun robot21024(s: ProscenicCloud.Sitzung, cmd: String, wert: Int) =
        ProscenicCloud.senden(s, "/instructions/cmd21024/${s.sn}${u(s)}&cmd=$cmd&value=$wert", leerJson)

    suspend fun lautstaerke(context: Context, wert: Int) = withContext(Dispatchers.IO) {
        mitErgebnis(context, "Lautstärke auf ${wert * 10} Prozent gestellt.") { s -> robot21024(s, "setVolume", wert.coerceIn(0, 10)) }
    }

    suspend fun led(context: Context, an: Boolean) = withContext(Dispatchers.IO) {
        mitErgebnis(context, if (an) "Leuchte eingeschaltet." else "Leuchte ausgeschaltet.") { s -> robot21024(s, "setledswitch", if (an) 1 else 0) }
    }

    /** Automatische Staubsammlung nach dem Andocken: 0 = aus, 1–3 = nach jeder, jeder zweiten, jeder dritten Reinigung. */
    suspend fun staubFrequenz(context: Context, wert: Int) = withContext(Dispatchers.IO) {
        mitErgebnis(context, "Automatische Staubsammlung umgestellt.") { s ->
            val body = JSONObject().put("dInfo", JSONObject().put("userId", s.mail).put("ts", System.currentTimeMillis().toString()))
                .put("data", JSONObject().put("cmd", "dustCenterFreq").put("values", wert)).put("infoType", 21024)
            ProscenicCloud.senden(s, "/instructions/cmd/${s.sn}${u(s)}", json(body))
        }
    }

    /** Speichert Räume und Sperrzonen (die ganze Liste, wie die App über cmd21003). */
    /** Grundlage fehlt: Zeitpläne bzw. Zonen wurden nicht frisch vom Roboter gelesen. Zurückschreiben würde Vorhandenes löschen. */
    private fun ohneGrundlage(was: String) = SaugerAusgang("Die $was des M7 Pro konnte ich nicht frisch lesen; damit nichts verloren geht, habe ich nichts gespeichert. Versuch es gleich noch einmal.", true)

    /** Speichert alle Zonen; [basis] muss die Zonen frisch vom Roboter gelesen haben, sonst würde eine alte oder leere Liste Vorhandenes löschen. */
    suspend fun zonenSpeichern(context: Context, basis: Live, zonen: List<Zone>) = withContext(Dispatchers.IO) {
        val mapId = basis.mapId
        if (!basis.zonenGelesen || mapId == null) return@withContext ohneGrundlage("Sperrzonen")
        mitErgebnis(context, "Sperrzonen gespeichert.") { s ->
            val body = JSONObject().put("mapId", mapId).put("value", JSONArray().apply { zonen.forEach { put(it.json) } })
            ProscenicCloud.senden(s, "/instructions/cmd21003/${s.sn}${u(s)}", json(body))
        }
    }

    /** Speichert alle Zeitpläne (wie die App über cmd21001); [basis] muss sie frisch vom Roboter gelesen haben. */
    suspend fun zeitplaeneSpeichern(context: Context, basis: Live, liste: List<Zeitplan>) = withContext(Dispatchers.IO) {
        if (!basis.zeitplaeneGelesen) return@withContext ohneGrundlage("Zeitpläne")
        val zeitZone = basis.zeitZone; val zeitZoneSek = basis.zeitZoneSek
        mitErgebnis(context, "Zeitpläne gespeichert.") { s ->
            val werte = JSONArray()
            liste.forEach { z ->
                werte.put(JSONObject(z.roh.toString()).put("active", z.aktiv).put("startTime", z.startSek)
                    .put("period", JSONArray(z.tage)).put("workNoisy", z.stufe)
                    .apply { if (!has("endTime")) put("endTime", 0); if (!has("tagIds")) put("tagIds", JSONArray()); if (!has("unlock")) put("unlock", false); if (!has("cleanAreas")) put("cleanAreas", JSONObject.NULL) })
            }
            ProscenicCloud.senden(s, "/instructions/cmd21001/${s.sn}${u(s)}", json(JSONObject().put("timeZone", zeitZone).put("timeZoneSec", zeitZoneSek).put("value", werte)))
        }
    }

    suspend fun verlauf(context: Context): Verlauf? = withContext(Dispatchers.IO) {
        runCatching {
            mitSitzung(context) { s ->
                val r = ProscenicCloud.post("/app/cleanRobot/records", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).add("page", "0").add("size", "20").build())
                if (r.optInt("code") == 102) throw ProscenicCloud.TokenUngueltig()
                val st = runCatching { ProscenicCloud.post("/app/cleanRobot/stats", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build()).optJSONObject("data") }.getOrNull()
                val a = r.optJSONObject("data")?.optJSONArray("content")
                Verlauf(
                    (0 until (a?.length() ?: 0)).mapNotNull { a?.optJSONObject(it) }.map { Lauf(it.optLong("startTime"), it.optInt("cleanTime"), it.optInt("area")) },
                    st?.optInt("time")?.let { it / 60 }, st?.optInt("sweepArea"), st?.optInt("counts"),
                )
            }
        }.getOrNull()
    }
}
