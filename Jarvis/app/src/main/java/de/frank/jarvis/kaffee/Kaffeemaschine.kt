package de.frank.jarvis.kaffee

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import de.frank.jarvis.data.settings.SecureSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * De'Longhi PrimaDonna Soul (ECAM 610.74, WLAN-Modul Ayla „DL-millcore“) über die Coffee-Link-Cloud, wie die offizielle App:
 * Anmeldung Gigya → Ayla-Token, Befehle als ECAM-Rahmen in `data_request`, Antworten in `data_response`, Live-Zustand in `d302_monitor`.
 * Wissen und Fallen: best-practices/peripherie/delonghi-coffeelink.md und bugs/peripherie/delonghi-coffeelink.md.
 * Rezepte holt Jarvis vor jedem Getränk frisch aus der Maschine (Befehl 0xA6, Profil 1) und brüht sie genau so, wie die Maschine sie gespeichert hat.
 */
object Kaffeemaschine {
    enum class Bild { ESPRESSO, KAFFEE, LANG, DOPPEL, AMERICANO, LONG_BLACK, CAPPUCCINO, LATTE, MILCH, WASSER, CORTADO }

    /** [nr] = Platz in der Rezeptliste je Profil (Eigenschaft d0xx_<profil>_rec_<schluessel>, je Profil 21 Plätze ab d039). */
    data class Getraenk(val id: Int, val schluessel: String, val name: String, val bild: Bild, val nr: Int, val woerter: List<String>)

    val GETRAENKE = listOf(
        Getraenk(0x19, "long_black", "Long Black", Bild.LONG_BLACK, 18, listOf("long black", "longblack", "mein kaffee", "standard")),
        Getraenk(0x01, "espresso", "Espresso", Bild.ESPRESSO, 0, listOf("espresso")),
        Getraenk(0x02, "regular", "Kaffee", Bild.KAFFEE, 1, listOf("kaffee", "coffee", "normaler kaffee")),
        Getraenk(0x03, "long_coffee", "Long Coffee", Bild.LANG, 2, listOf("long coffee", "langer kaffee", "großer kaffee")),
        Getraenk(0x04, "2x_espresso", "2× Espresso", Bild.DOPPEL, 3, listOf("2x espresso", "2× espresso", "zwei espresso", "doppelter espresso", "2 espresso")),
        Getraenk(0x05, "doppio", "Doppio+", Bild.DOPPEL, 4, listOf("doppio")),
        Getraenk(0x06, "americano", "Americano", Bild.AMERICANO, 5, listOf("americano", "amerikaner")),
        Getraenk(0x07, "cappuccino", "Cappuccino", Bild.CAPPUCCINO, 6, listOf("cappuccino")),
        Getraenk(0x08, "latte_macchiato", "Latte Macchiato", Bild.LATTE, 7, listOf("latte macchiato", "latte")),
        Getraenk(0x09, "caffelatte", "Caffè Latte", Bild.LATTE, 8, listOf("caffè latte", "caffe latte", "milchkaffee")),
        Getraenk(0x0a, "flat_white", "Flat White", Bild.CAPPUCCINO, 9, listOf("flat white")),
        Getraenk(0x0b, "espr_macchiato", "Espresso Macchiato", Bild.CORTADO, 10, listOf("espresso macchiato", "macchiato")),
        Getraenk(0x0d, "capp_doppio", "Cappuccino+", Bild.CAPPUCCINO, 12, listOf("cappuccino plus", "cappuccino+", "doppelter cappuccino")),
        Getraenk(0x0f, "capp_reverse", "Cappuccino Mix", Bild.CAPPUCCINO, 13, listOf("cappuccino mix", "cappuccino reverse")),
        Getraenk(0x18, "cortado", "Cortado", Bild.CORTADO, 17, listOf("cortado")),
        Getraenk(0x0c, "hot_milk", "Heiße Milch", Bild.MILCH, 11, listOf("heiße milch", "warme milch", "milch")),
        Getraenk(0x10, "hot_water", "Heißwasser", Bild.WASSER, 14, listOf("heißwasser", "heißes wasser", "wasser")),
    )
    val STANDARD = GETRAENKE.first()

    /** Genaue Treffer zuerst, sonst das längste Stichwort, das im Text vorkommt („Milchkaffee“ schlägt „Kaffee“, „Latte Macchiato“ schlägt „Macchiato“). */
    fun finden(text: String): Getraenk? {
        val t = text.lowercase(Locale.GERMANY).trim()
        if (t.isEmpty()) return null
        GETRAENKE.firstOrNull { g -> t == g.name.lowercase(Locale.GERMANY) || g.woerter.any { t == it } }?.let { return it }
        return GETRAENKE.flatMap { g -> (g.woerter + g.name.lowercase(Locale.GERMANY)).map { it to g } }
            .filter { (wort, _) -> wort in t }.maxByOrNull { (wort, _) -> wort.length }?.second
    }

    /** Einstellungen der Maschine: Kennung im ECAM-Protokoll, Eigenschaft mit dem aktuellen Wert, erlaubte Werte. */
    data class Einstellung(val schluessel: String, val name: String, val id: Int, val eigenschaft: String, val werte: Map<Int, String>)

    val EINSTELLUNGEN = listOf(
        Einstellung("abschaltzeit", "Automatisch aus nach", 0x3e, "d282_mchn_sett_aoff", mapOf(0 to "15 Minuten", 1 to "30 Minuten", 2 to "1 Stunde", 3 to "3 Stunden")),
        Einstellung("wasserhaerte", "Wasserhärte", 0x32, "d283_mchn_sett_water", mapOf(0 to "Stufe 1 (weich)", 1 to "Stufe 2", 2 to "Stufe 3", 3 to "Stufe 4 (sehr hart)")),
    )

    data class Zustand(
        val online: Boolean, val status: Int, val aktion: Int, val alarme: List<String>, val sperrend: List<String>,
        val einstellungen: Map<String, Int>, val profile: Map<Int, String>,
        /** Hat die Maschine gerade eben frisch geantwortet? Sonst ist der Wert ein alter Stand aus der Cloud. */
        val frisch: Boolean,
    ) {
        val bereit get() = online && frisch && status == 7 && aktion == 0
        val brueht get() = online && frisch && status == 7 && aktion != 0
        fun text(): String = when {
            !online -> "Offline (nicht mit dem WLAN verbunden)"
            !frisch -> "Antwortet gerade nicht"
            status == 7 && aktion != 0 -> "Bereitet zu …"
            else -> STATUS[status] ?: "Zustand $status"
        }
    }

    class Fehler(text: String) : Exception(text)

    private val STATUS = mapOf(
        0 to "Standby", 1 to "Wacht auf", 2 to "Geht in Standby", 4 to "Entkalkt", 5 to "Dampf", 6 to "Erholt sich", 7 to "Bereit",
        8 to "Spült", 10 to "Bereitet Milch zu", 11 to "Heißwasser", 12 to "Reinigt den Milchaufschäumer",
    )
    private val ALARME = mapOf(
        0 to "Wassertank leer", 1 to "Satzbehälter voll", 2 to "Entkalken nötig", 3 to "Wasserfilter tauschen", 5 to "Bohnen leer",
        6 to "Service nötig", 11 to "Abtropfschale leeren", 12 to "Problem im Wasserkreislauf", 17 to "Bohnenbehälter fehlt",
    )
    /** Nur diese Meldungen verhindern einen Kaffee; Entkalken, Filter und Service sind Erinnerungen, die Maschine brüht trotzdem. */
    private val SPERREND = setOf(0, 1, 5, 11, 12, 17)
    private val ZUSATZ_FELDER = setOf(0x1b, 0x1c, 0x1f, 0x21, 0x26, 0x27)

    // ---- Zugang (aus der Coffee-Link-APK, öffentlich dokumentiert in actabi/delonghi_coffeelink) ----
    private const val GIGYA_KEY = "3_e5qn7USZK-QtsIso1wCelqUKAK_IVEsYshRIssQ-X-k55haiZXmKWDHDRul2e5Y2" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)
    private const val GIGYA = "https://accounts.eu1.gigya.com"
    private const val AYLA_USER = "https://user-field-eu.aylanetworks.com"
    private const val ADS = "https://ads-eu.aylanetworks.com/apiv1"
    private const val APP_ID = "DLonghiCoffeeIdKit-sQ-id" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)
    private const val APP_SECRET = "DLonghiCoffeeIdKit-HT6b0VNd4y6CSha9ivM5k8navLw" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)

    private val sperre = Mutex()
    private val hintergrund = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var zugang: String = ""
    @Volatile private var zugangBis = 0L
    @Volatile private var zugangFuer = ""
    @Volatile private var dsnCache = ""

    fun konfiguriert(context: Context): Boolean {
        val s = SecureSettings.get(context)
        return s.delonghiMail.isNotBlank() && s.delonghiPasswort.isNotBlank()
    }

    // ---- HTTP ----
    private fun http(methode: String, url: String, body: String? = null, form: Boolean = false, token: String? = null): Pair<Int, String> {
        val v = URL(url).openConnection() as HttpURLConnection
        try {
            v.requestMethod = methode
            v.connectTimeout = 15_000; v.readTimeout = 20_000
            v.setRequestProperty("Accept", "application/json")
            v.setRequestProperty("User-Agent", "okhttp/4.12.0")
            if (token != null) v.setRequestProperty("Authorization", "auth_token $token")
            if (body != null) {
                v.doOutput = true
                v.setRequestProperty("Content-Type", if (form) "application/x-www-form-urlencoded" else "application/json")
                v.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = v.responseCode
            val text = (if (code in 200..299) v.inputStream else v.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally { v.disconnect() }
    }

    private fun formular(vararg p: Pair<String, String>) = p.joinToString("&") { (k, w) -> "$k=" + URLEncoder.encode(w, "UTF-8") }
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

    /** Gigya-Anmeldung mit E-Mail und Passwort, dann ein signiertes JWT, das Ayla gegen ein eigenes Token tauscht. */
    private fun anmelden(context: Context) {
        val s = SecureSettings.get(context)
        val (c1, t1) = http("POST", "$GIGYA/accounts.login", formular("apiKey" to GIGYA_KEY, "loginID" to s.delonghiMail, "password" to s.delonghiPasswort, "format" to "json", "targetEnv" to "mobile"), form = true)
        val login = runCatching { JSONObject(t1) }.getOrNull()
        if (c1 !in 200..299 || login?.optInt("errorCode", -1) != 0) throw Fehler("Die De'Longhi-Anmeldung ist fehlgeschlagen" + (login?.optString("errorMessage")?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "") + ". Prüf E-Mail und Passwort unter Konten.")
        val info = login.getJSONObject("sessionInfo")
        val sitzung = info.getString("sessionToken")
        val geheim = info.getString("sessionSecret")
        val zeit = (System.currentTimeMillis() / 1000).toString()
        val url = "$GIGYA/accounts.getJWT"
        val p = sortedMapOf("apiKey" to GIGYA_KEY, "oauth_token" to sitzung, "format" to "json", "timestamp" to zeit, "nonce" to "${zeit}_1")
        val basis = "POST&" + enc(url) + "&" + enc(p.entries.joinToString("&") { "${it.key}=${enc(it.value)}" })
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(Base64.decode(geheim, Base64.DEFAULT), "HmacSHA1")) }
        p["sig"] = Base64.encodeToString(mac.doFinal(basis.toByteArray()), Base64.NO_WRAP)
        val (c2, t2) = http("POST", url, formular(*p.map { it.key to it.value }.toTypedArray()), form = true)
        val jwt = runCatching { JSONObject(t2).optString("id_token") }.getOrDefault("")
        if (c2 !in 200..299 || jwt.isBlank()) throw Fehler("De'Longhi hat kein Anmelde-Token ausgestellt.")
        val (c3, t3) = http("POST", "$AYLA_USER/api/v1/token_sign_in", formular("token" to jwt, "app_id" to APP_ID, "app_secret" to APP_SECRET), form = true)
        uebernehmen(context, c3, t3) ?: throw Fehler("Die Coffee-Link-Cloud hat die Anmeldung abgelehnt (HTTP $c3).")
    }

    private fun kontoKennung(context: Context) = SecureSettings.get(context).let { it.delonghiMail + "\u0000" + it.delonghiPasswort.hashCode() }

    private fun uebernehmen(context: Context, code: Int, text: String): String? {
        if (code !in 200..299) return null
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val a = j.optString("access_token").ifBlank { return null }
        zugang = a
        zugangFuer = kontoKennung(context)
        zugangBis = System.currentTimeMillis() + maxOf(60L, j.optLong("expires_in", 3600) - 120) * 1000
        j.optString("refresh_token").takeIf { it.isNotBlank() }?.let { SecureSettings.get(context).delonghiAuffrischung = it }
        return a
    }

    private fun token(context: Context): String {
        // Andere E-Mail oder anderes Passwort unter Konten: alten Zugang samt Gerät verwerfen.
        if (zugangFuer.isNotEmpty() && zugangFuer != kontoKennung(context)) {
            zugang = ""; zugangBis = 0; dsnCache = ""; zugangFuer = ""
            SecureSettings.get(context).delonghiAuffrischung = ""
        }
        if (zugang.isNotBlank() && System.currentTimeMillis() < zugangBis) return zugang
        val auf = SecureSettings.get(context).delonghiAuffrischung
        if (auf.isNotBlank()) {
            val (c, t) = http("POST", "$AYLA_USER/users/refresh_token.json", JSONObject().put("user", JSONObject().put("refresh_token", auf)).toString())
            uebernehmen(context, c, t)?.let { return it }
        }
        anmelden(context)
        return zugang
    }

    /** Einmal neu anmelden, wenn Ayla das Token ablehnt. */
    private fun ads(context: Context, methode: String, pfad: String, body: String? = null): String {
        var (c, t) = http(methode, ADS + pfad, body, token = token(context))
        if (c == 401) { zugang = ""; zugangBis = 0; val n = http(methode, ADS + pfad, body, token = token(context)); c = n.first; t = n.second }
        if (c == 404) dsnCache = ""  // Maschine neu angemeldet oder Modul getauscht: Gerät beim nächsten Aufruf neu suchen.
        if (c !in 200..299) throw Fehler("Die Coffee-Link-Cloud antwortet nicht wie erwartet (HTTP $c).")
        return t
    }

    /** Liest die Geräteliste (Online-Status, DSN) und merkt sich die DSN für alle weiteren Aufrufe. */
    private fun online(context: Context): Boolean {
        val a = JSONArray(ads(context, "GET", "/devices.json"))
        for (i in 0 until a.length()) {
            val d = a.getJSONObject(i).optJSONObject("device") ?: a.getJSONObject(i)
            if (d.optString("oem_model").startsWith("DL-millcore") || a.length() == 1) {
                dsnCache = d.optString("dsn")
                return d.optString("connection_status").equals("Online", true)
            }
        }
        throw Fehler("Im De'Longhi-Konto ist keine PrimaDonna Soul eingetragen.")
    }

    private fun dsn(context: Context): String { if (dsnCache.isBlank()) online(context); return dsnCache }

    private data class Wert(val text: String, val zeit: String)

    private fun lesen(context: Context, name: String): Wert {
        val p = JSONObject(ads(context, "GET", "/dsns/${dsn(context)}/properties/$name.json")).let { it.optJSONObject("property") ?: it }
        return Wert(if (p.isNull("value")) "" else p.optString("value"), p.optString("data_updated_at"))
    }

    private fun schreiben(context: Context, name: String, wert: Any) {
        ads(context, "POST", "/dsns/${dsn(context)}/properties/$name/datapoints.json", JSONObject().put("datapoint", JSONObject().put("value", wert)).toString())
    }

    // ---- ECAM-Rahmen ----
    private fun crc(b: ByteArray, bis: Int = b.size): Int {
        var c = 0x1D0F
        for (i in 0 until bis) {
            c = c xor ((b[i].toInt() and 0xFF) shl 8)
            repeat(8) { c = if (c and 0x8000 != 0) (c shl 1) xor 0x1021 else c shl 1; c = c and 0xFFFF }
        }
        return c
    }

    private fun rahmen(op: Int, flags: Int, daten: ByteArray): String {
        val kopf = byteArrayOf(0x0d, (daten.size + 5).toByte(), op.toByte(), flags.toByte()) + daten
        val c = crc(kopf)
        val t = (System.currentTimeMillis() / 1000).toInt()
        val alles = kopf + byteArrayOf((c shr 8).toByte(), c.toByte(), (t ushr 24).toByte(), (t ushr 16).toByte(), (t ushr 8).toByte(), t.toByte())
        return Base64.encodeToString(alles, Base64.NO_WRAP)
    }

    private class Antwort(val daten: ByteArray, val zeit: Long)

    /** Antwort der Maschine (0xD0 …) prüfen; liefert die Nutzdaten und den Zeitstempel der Maschine (Unix-Sekunden, 0 = keiner). */
    private fun antwort(b64: String, op: Int): Antwort? {
        val r = runCatching { Base64.decode(b64.trim(), Base64.DEFAULT) }.getOrNull() ?: return null
        if (r.size < 6 || (r[0].toInt() and 0xFF) != 0xD0 || (r[2].toInt() and 0xFF) != op) return null
        val ende = (r[1].toInt() and 0xFF) + 1
        if (ende > r.size || ende < 6) return null
        val c = ((r[ende - 2].toInt() and 0xFF) shl 8) or (r[ende - 1].toInt() and 0xFF)
        if (c != crc(r, ende - 2)) return null
        val zeit = if (r.size >= ende + 4) ((r[ende].toLong() and 0xFF) shl 24) or ((r[ende + 1].toLong() and 0xFF) shl 16) or ((r[ende + 2].toLong() and 0xFF) shl 8) or (r[ende + 3].toLong() and 0xFF) else 0L
        return Antwort(r.copyOfRange(4, ende - 2), zeit)
    }

    private fun befehl(context: Context, op: Int, flags: Int, daten: ByteArray) = schreiben(context, "data_request", rahmen(op, flags, daten))

    // ---- Zustand ----
    private class Monitor(val status: Int, val aktion: Int, val alarmBits: Int, val frisch: Boolean)

    /** Die Soul meldet ihren Live-Zustand nur, wenn eine App-Sitzung in `device_connected` steht; danach kommt er nach 4–7 s. */
    private suspend fun monitor(context: Context): Monitor {
        val vorher = lesen(context, "d302_monitor")
        val beginn = System.currentTimeMillis() / 1000
        schreiben(context, "device_connected", beginn)
        var mon = vorher
        for (versuch in 1..10) {
            delay(1000)
            val n = lesen(context, "d302_monitor")
            if (n.zeit != vorher.zeit) { mon = n; break }
        }
        val a = antwort(mon.text, 0x75) ?: throw Fehler("Die Kaffeemaschine hat keinen lesbaren Zustand geschickt.")
        // antwort() liefert schon den Inhalt hinter „D0 len 75 0F“: Feld 5 = Zustand, 6 = Aktion, 3/4/8/9 = Meldungsbits (wie actabi monitor.py).
        val inhalt = a.daten
        fun u(i: Int) = if (i < inhalt.size) inhalt[i].toInt() and 0xFF else 0
        // Frisch nur, wenn der Rahmen neu ist und der Zeitstempel der Maschine zu jetzt passt.
        val frisch = mon.zeit != vorher.zeit || (a.zeit > 0 && kotlin.math.abs(System.currentTimeMillis() / 1000 - a.zeit) < 20)
        // Im Standby (0) stehen in den Meldungsbytes Altwerte (gesehen: „Wassertank leer“ bei vollem Tank); dann keine Meldungen auswerten.
        val bits = if (u(5) == 0) 0 else u(3) or (u(4) shl 8) or (u(8) shl 16) or (u(9) shl 24)
        return Monitor(u(5), u(6), bits, frisch)
    }

    suspend fun zustand(context: Context): Zustand = withContext(Dispatchers.IO) {
        sperre.withLock { zustandOhneSperre(context) }
    }

    private suspend fun zustandOhneSperre(context: Context): Zustand {
        if (!konfiguriert(context)) throw Fehler("Für die Kaffeemaschine fehlt die De'Longhi-Anmeldung unter Einstellungen, Konten und APIs.")
        if (!online(context)) return Zustand(false, 0, 0, emptyList(), emptyList(), emptyMap(), emptyMap(), false)
        val m = monitor(context)
        val einst = EINSTELLUNGEN.associate { e -> e.schluessel to (einstellungLesen(context, e) ?: -1) }
        return Zustand(
            true, m.status, m.aktion, ALARME.filterKeys { m.alarmBits and (1 shl it) != 0 }.values.toList(),
            ALARME.filterKeys { it in SPERREND && m.alarmBits and (1 shl it) != 0 }.values.toList(), einst, profile(context), m.frisch,
        )
    }

    private fun einstellungLesen(context: Context, e: Einstellung): Int? {
        val d = antwort(runCatching { lesen(context, e.eigenschaft).text }.getOrDefault(""), 0x95)?.daten ?: return null
        if (d.size < 6) return null
        return ((d[2].toInt() and 0xFF) shl 24) or ((d[3].toInt() and 0xFF) shl 16) or ((d[4].toInt() and 0xFF) shl 8) or (d[5].toInt() and 0xFF)
    }

    @Volatile private var profilCache: Pair<Long, Map<Int, String>>? = null

    /** Profilnamen aus d034/d035 (UTF-16, je 20 Bytes Name plus 1 Byte Symbol); ändern sich selten, daher eine Stunde zwischengespeichert. */
    private fun profile(context: Context): Map<Int, String> {
        profilCache?.let { (wann, namen) -> if (System.currentTimeMillis() - wann < 3_600_000L && namen.isNotEmpty()) return namen }
        val namen = mutableMapOf<Int, String>()
        for ((eig, erstes) in listOf("d034_profiles_1_3" to 1, "d035_profiles_4_5" to 4)) {
            val d = antwort(runCatching { lesen(context, eig).text }.getOrDefault(""), 0xa4)?.daten ?: continue
            var i = 2
            var nr = erstes
            while (i + 20 <= d.size) {
                val name = String(d.copyOfRange(i, i + 20), Charsets.UTF_16BE).trimEnd('\u0000').trim()
                if (name.isNotBlank()) namen[nr] = name
                i += 21; nr++
            }
        }
        profilCache = System.currentTimeMillis() to namen
        return namen
    }

    // ---- Befehle ----
    suspend fun aufwecken(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (!online(context)) throw Fehler("Die Kaffeemaschine ist offline.")
            val m = monitor(context)
            if (m.frisch && !m.schlaeft()) return@withLock "Die Kaffeemaschine ist schon an (${zustandName(m.status)})."
            wecken(context)
            "Die Kaffeemaschine wacht auf und spült kurz, dann ist sie bereit."
        }
    }

    suspend fun ausschalten(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (!online(context)) throw Fehler("Die Kaffeemaschine ist offline.")
            ausBefehl(context)
            manuellAus(context)
            "Die Kaffeemaschine spült und geht in Standby" + (if (de.frank.jarvis.data.Einstellungen.get(context.applicationContext).kaffeeWachHalten) "; Jarvis weckt sie heute nicht mehr von selbst." else ".")
        }
    }

    /** Stopp-Rahmen wie im ioBroker-Adapter, der ihn an der Soul erprobt hat (ecam-soul.js, STOP_DRINK). */
    suspend fun stoppen(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (!konfiguriert(context)) throw Fehler("Für die Kaffeemaschine fehlt die De'Longhi-Anmeldung unter Einstellungen, Konten und APIs.")
            if (!online(context)) throw Fehler("Die Kaffeemaschine ist offline.")
            befehl(context, 0x83, 0xf0, byteArrayOf(0x10, 0x02, 0x0f, 0x00, 0xfa.toByte(), 0x1b, 0x01, 0x06))
            "Stopp an die Kaffeemaschine gesendet."
        }
    }

    /**
     * Holt das Rezept des Profils frisch aus der Maschine (0xA6, Antwort landet in d0xx_<profil>_rec_<name>) und baut daraus den
     * Brühbefehl wie die Coffee-Link-App: Getränk, Start, die Zutaten in der Reihenfolge der Maschine ohne Metadaten, am Ende das Profil.
     * Antwortet die Maschine nicht, brüht Jarvis nicht: Ein alter Stand hieße sonst „läuft“, obwohl nichts passiert.
     */
    private suspend fun rezept(context: Context, g: Getraenk, profil: Int): ByteArray {
        val eig = String.format(Locale.ROOT, "d%03d_%d_rec_%s", 39 + (profil - 1) * 21 + g.nr, profil, g.schluessel)
        val vorher = runCatching { lesen(context, eig) }.getOrNull()
        befehl(context, 0xa6, 0xf0, byteArrayOf(profil.toByte(), g.id.toByte()))
        var wert: Wert? = null
        for (versuch in 1..8) {
            delay(1000)
            val n = runCatching { lesen(context, eig) }.getOrNull()
            if (n != null && n.zeit != vorher?.zeit) { wert = n; break }
        }
        val d = wert?.let { antwort(it.text, 0xa6)?.daten }
            ?: throw Fehler("Die Maschine hat das Rezept für ${g.name} nicht geschickt. Ist die Coffee-Link-App gerade offen? Dann bitte schließen.")
        if (d.size < 2 || (d[0].toInt() and 0xFF) != profil || (d[1].toInt() and 0xFF) != g.id) throw Fehler("Das Rezept für ${g.name} passt nicht zum Profil.")
        val felder = mutableListOf<Byte>()
        var sichtbar = false
        var menge = false
        var i = 2
        while (i < d.size) {
            val tag = d[i].toInt() and 0xFF
            val lang = if (tag == 0x01 || tag == 0x09 || tag == 0x0f) 3 else 2
            if (i + lang > d.size) break
            if (tag == 0x19) sichtbar = d[i + 1].toInt() == 1
            if (lang == 3 && (((d[i + 1].toInt() and 0xFF) shl 8) or (d[i + 2].toInt() and 0xFF)) > 0) menge = true
            if ((tag < 23 && tag != 0x08) || tag in ZUSATZ_FELDER) for (k in 0 until lang) felder += d[i + k]
            i += lang
        }
        if (!sichtbar || !menge) throw Fehler("${g.name} ist an deiner Maschine im Profil $profil nicht freigeschaltet.")
        return byteArrayOf(g.id.toByte(), 0x01) + felder.toByteArray() + byteArrayOf(((profil shl 2) or 2).toByte())
    }

    private fun Monitor.bereit() = frisch && status == 7 && aktion == 0
    private fun Monitor.schlaeft() = frisch && (status == 0 || status == 2)
    private fun wecken(context: Context) = befehl(context, 0x84, 0x0f, byteArrayOf(2, 1))
    private fun ausBefehl(context: Context) = befehl(context, 0x84, 0x0f, byteArrayOf(1, 1))
    private fun zustandName(status: Int) = STATUS[status] ?: "Zustand $status"

    /**
     * Brüht das Getränk aus Profil 1, aber nur, wenn die Maschine schon bereit ist. Schläft sie, weckt Jarvis sie nur auf: Beim Aufwachen spült die
     * Soul immer heißes Wasser durch die Auslässe (laut Anleitung nicht abschaltbar), und das liefe sonst in die Tasse und verdünnte den Kaffee
     * (Frank, 18.11.2026). Sobald sie bereit ist, meldet sich Jarvis; dann Tasse leeren, unterstellen und noch einmal bestellen.
     * Läuft schon eine Zubereitung oder ist sie anderweitig beschäftigt (Entkalken, Reinigen …), lehnt Jarvis mit dem Grund ab.
     */
    suspend fun bruehen(context: Context, g: Getraenk, profil: Int = 1, fortschritt: (String) -> Unit = {}): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (!konfiguriert(context)) throw Fehler("Für die Kaffeemaschine fehlt die De'Longhi-Anmeldung unter Einstellungen, Konten und APIs.")
            if (!online(context)) throw Fehler("Die Kaffeemaschine ist offline, also nicht mit dem WLAN verbunden.")
            val m = monitor(context)
            sperrendeMeldung(m)?.let { throw Fehler("Die Maschine meldet: $it.") }
            if (m.frisch && m.status == 7 && m.aktion != 0) throw Fehler("Die Maschine bereitet gerade schon etwas zu.")
            if (!m.bereit()) {
                // Nur Schlafen, Aufwachen und Spülen enden von selbst in „bereit“; alles andere (Entkalken, Dampf, Reinigen) nennt Jarvis beim Namen.
                if (m.frisch && m.status !in setOf(0, 1, 2, 8)) throw Fehler("Die Maschine ist gerade beschäftigt: ${zustandName(m.status)}.")
                if (!m.frisch || m.schlaeft()) { fortschritt("Weckt die Maschine auf …"); wecken(context) }
                aufBereitMelden(context.applicationContext, g)
                return@withLock "Die Maschine wacht auf und spült zuerst heißes Wasser durch. Bitte noch keine Tasse drunter, sonst verdünnt das Spülwasser den Kaffee. " +
                    "In etwa einer Minute ist sie bereit, dann sage ich Bescheid: Tasse unterstellen und ${g.name} noch einmal bestellen."
            }
            bereitMeldung = null
            fortschritt("Holt das Rezept …")
            befehl(context, 0x83, 0xf0, rezept(context, g, profil))
            fortschritt("${g.name} läuft …")
            "${g.name} läuft."
        }
    }

    private val bereitWaechterLaeuft = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Wartet im Hintergrund (höchstens drei Minuten), bis die Maschine nach dem Aufwachen bereit ist, und meldet das. Nie zwei Wächter gleichzeitig. */
    private fun aufBereitMelden(app: Context, g: Getraenk) {
        if (!bereitWaechterLaeuft.compareAndSet(false, true)) return
        hintergrund.launch {
            try {
                val ende = System.currentTimeMillis() + 180_000
                var text = "Die Kaffeemaschine ist nicht rechtzeitig bereit geworden."
                while (System.currentTimeMillis() < ende) {
                    delay(10_000)
                    val m = runCatching { sperre.withLock { monitor(app) } }.getOrNull() ?: continue
                    sperrendeMeldung(m)?.let { text = "Die Kaffeemaschine meldet: $it." }
                    if (m.bereit() || text.startsWith("Die Kaffeemaschine meldet")) {
                        if (m.bereit() && !text.startsWith("Die Kaffeemaschine meldet")) text = "Die Kaffeemaschine ist bereit. Tasse unterstellen und ${g.name} noch einmal bestellen."
                        break
                    }
                }
                zeigen(app, text)
            } finally { bereitWaechterLaeuft.set(false) }
        }
    }

    private fun zeigen(app: Context, text: String) {
        bereitMeldung = text
        bereitMeldungZeit = System.currentTimeMillis()
        Handler(Looper.getMainLooper()).post { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
    }

    /** Letzte Hintergrundmeldung für die Karte (Compose-Zustand, nach 10 Minuten nicht mehr angezeigt). */
    var bereitMeldung by androidx.compose.runtime.mutableStateOf<String?>(null)
    @Volatile var bereitMeldungZeit = 0L

    private fun sperrendeMeldung(m: Monitor): String? =
        ALARME.filterKeys { it in SPERREND && m.alarmBits and (1 shl it) != 0 }.values.joinToString(", ").ifBlank { null }

    // ---- Wach halten ----
    private const val WACH_PREFS = "kaffee_wach"
    /** Höchstens so oft weckt Jarvis die Maschine je Fenster wieder auf, wenn sie doch ausgegangen ist (jedes Wecken spült). */
    private const val WACH_MAX_WECKEN = 3

    /**
     * Signalabstand: höchstens 10 Minuten, bei kurzer Abschaltzeit ein Drittel davon. Ein Test am 18.11.2026 zeigte, dass das Signal die Abschaltzeit
     * zurücksetzt, aber nicht jedes zählt (bei 15 Minuten ging sie 15 Minuten nach einem Signal trotz späterer aus).
     */
    private fun signalAbstandMs(abschaltMin: Int) = minOf(10L, abschaltMin / 3L).coerceAtLeast(3L) * 60_000L

    /** Start des Wachfensters heute: laut Wecker (nach Dienst gewählt) oder die eingestellte volle Stunde. */
    data class WachStart(val zeit: java.time.LocalDateTime, val grund: String)

    @Volatile private var weckerCache: Triple<Long, java.time.LocalDate, WachStart?>? = null

    private fun dienstArt(app: Context, tag: java.time.LocalDate): String? = runCatching {
        de.frank.jarvis.faehigkeit.Register.alle(app).filterIsInstance<de.frank.jarvis.faehigkeit.KalenderFaehigkeit>().firstOrNull()?.schichten(tag, 1)?.get(tag)?.first
    }.getOrNull()

    /**
     * Der Wecker, der heute klingelt und eingeschaltet ist. Tagschicht: der Tagschicht-Wecker, Nachtschicht: der Nachtschicht-Wecker, sonst der früheste
     * andere (etwa der Sonnenaufgangswecker). Der einmal gewählte Start gilt für den ganzen Tag (nach dem Klingeln zeigt der Wecker schon auf morgen).
     */
    suspend fun weckerStart(context: Context): WachStart? = withContext(Dispatchers.IO) {
        val heute = java.time.LocalDate.now()
        weckerCache?.let { (wann, tag, wert) -> if (tag == heute && System.currentTimeMillis() - wann < 10 * 60_000L) return@withContext wert }
        val app = context.applicationContext
        val p = app.getSharedPreferences(WACH_PREFS, Context.MODE_PRIVATE)
        val gemerkt = runCatching { java.time.LocalDateTime.parse(p.getString("weckerZeit", "")) }.getOrNull()?.takeIf { it.toLocalDate() == heute }
            ?.let { WachStart(it, p.getString("weckerGrund", "Wecker").orEmpty()) }
        if (gemerkt != null && !java.time.LocalDateTime.now().isBefore(gemerkt.zeit)) { weckerCache = Triple(System.currentTimeMillis(), heute, gemerkt); return@withContext gemerkt }
        val liste = de.frank.jarvis.faehigkeit.Register.alle(app).filterIsInstance<de.frank.jarvis.faehigkeit.WeckerFaehigkeit>().firstOrNull()?.weckerRoh()
            ?: return@withContext gemerkt
        data class W(val name: String, val zeit: java.time.LocalDateTime)
        val heutige = (0 until liste.length()).mapNotNull { i ->
            val w = liste.optJSONObject(i) ?: return@mapNotNull null
            if (!w.optBoolean("aktiv") || w.isNull("naechstes_klingeln")) return@mapNotNull null
            val z = runCatching { java.time.LocalDateTime.parse(w.optString("naechstes_klingeln").take(16)) }.getOrNull() ?: return@mapNotNull null
            if (z.toLocalDate() != heute) null else W(w.optString("name"), z)
        }.sortedBy { it.zeit }
        fun hat(w: W, wort: String) = w.name.lowercase(Locale.GERMANY).contains(wort)
        val gewaehlt = when (dienstArt(app, heute)) {
            "tag" -> heutige.firstOrNull { hat(it, "tagschicht") || hat(it, "frühschicht") }
            "nacht" -> heutige.firstOrNull { hat(it, "nachtschicht") }
            else -> null
        } ?: heutige.firstOrNull { !hat(it, "tagschicht") && !hat(it, "nachtschicht") } ?: heutige.firstOrNull()
        val ergebnis = gewaehlt?.let { WachStart(it.zeit, "Wecker „${it.name}“") }?.also { p.edit().putString("weckerZeit", it.zeit.toString()).putString("weckerGrund", it.grund).apply() }
            ?: gemerkt
        weckerCache = Triple(System.currentTimeMillis(), heute, ergebnis)
        ergebnis
    }

    /** Start heute (Wecker oder feste Stunde) und das Ende: die „bis“-Stunde danach, notfalls am nächsten Tag. */
    suspend fun wachFenster(context: Context): Pair<WachStart, java.time.LocalDateTime>? = withContext(Dispatchers.IO) {
        val e = de.frank.jarvis.data.Einstellungen.get(context.applicationContext)
        val heute = java.time.LocalDate.now()
        val start = if (e.kaffeeWachNachWecker) weckerStart(context) ?: return@withContext null
            else WachStart(heute.atTime(e.kaffeeWachVon, 0), "fest ab ${e.kaffeeWachVon} Uhr")
        var ende = start.zeit.toLocalDate().atTime(e.kaffeeWachBis, 0)
        if (!ende.isAfter(start.zeit)) ende = ende.plusDays(1)
        start to ende
    }

    /** Aus für das laufende Fenster (über Jarvis, am Gerät oder nach dem Dienst): Jarvis weckt sie darin nicht wieder. Ohne Fenster gilt der Tag. */
    private fun manuellAus(context: Context) {
        val p = context.applicationContext.getSharedPreferences(WACH_PREFS, Context.MODE_PRIVATE)
        p.edit().putString("aus", aktuellesFenster(p)).apply()
    }

    private fun aktuellesFenster(p: android.content.SharedPreferences): String {
        val ende = runCatching { java.time.LocalDateTime.parse(p.getString("ende", "")) }.getOrNull()
        return if (ende != null && java.time.LocalDateTime.now().isBefore(ende)) p.getString("fenster", "").orEmpty() else java.time.LocalDate.now().toString()
    }

    /**
     * Aus der Minutenrunde des Wächters, nur am Handy: Ist „wach halten“ an, weckt Jarvis die Maschine zum Start des Fensters (laut Wecker oder feste
     * Stunde; sie spült dabei ins Abtropfgitter) und hält sie bis zum Ende mit einem Signal (höchstens alle 10 Minuten) an, wie es die Coffee-Link-App
     * beim Verbinden schickt (Profil 1 wählen). Läuft nur am Handy und über die Cloud, also auch unterwegs mit mobilen Daten. Geht sie aus, obwohl das
     * letzte bestätigte Signal frisch war, hat Frank sie am Gerät ausgeschaltet: dann im Fenster nicht wieder wecken (ebenso nach „Ausschalten“ über Jarvis).
     */
    suspend fun wachHalten(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val e = de.frank.jarvis.data.Einstellungen.get(app)
        if (!e.kaffeeWachHalten || de.frank.jarvis.data.Geraet.istTablet || !konfiguriert(app)) return@withContext
        val p = app.getSharedPreferences(WACH_PREFS, Context.MODE_PRIVATE)
        val jetzt = java.time.LocalDateTime.now()
        // Ein neues Fenster öffnet sich, sobald der heutige Start erreicht ist; ein über Mitternacht laufendes gilt bis zu seinem gespeicherten Ende.
        val fenster = runCatching { wachFenster(app) }.getOrNull()
        if (fenster != null && !jetzt.isBefore(fenster.first.zeit) && jetzt.isBefore(fenster.second) && p.getString("fenster", "") != fenster.first.zeit.toString()) {
            // Neues Fenster: alte Merker (weg, Weckzähler, letztes Signal) gelten nicht mehr; ob Frank weg ist, misst zuhausePruefen neu.
            p.edit().putString("fenster", fenster.first.zeit.toString()).putString("ende", fenster.second.toString()).putLong("zuletzt", 0L)
                .putBoolean("weg", false).putInt("draussen", 0).putLong("ortZuletzt", 0L).putInt("weckAnzahl", 0).putLong("signalOk", 0L).apply()
        }
        val ende = runCatching { java.time.LocalDateTime.parse(p.getString("ende", "")) }.getOrNull() ?: return@withContext
        if (!jetzt.isBefore(ende)) return@withContext
        val fensterTag = runCatching { java.time.LocalDateTime.parse(p.getString("fenster", "")).toLocalDate() }.getOrNull() ?: jetzt.toLocalDate()
        // Nur zu Hause: Verlässt Frank den Umkreis, geht die Maschine aus und bleibt aus, solange er weg ist.
        if (e.kaffeeNurZuhause && !zuhausePruefen(app, p, fensterTag)) return@withContext
        val abschaltMin = p.getInt("abschaltMin", 180)
        if (System.currentTimeMillis() - p.getLong("zuletzt", 0L) in 0 until signalAbstandMs(abschaltMin)) return@withContext
        if (!sperre.tryLock()) return@withContext
        try {
            p.edit().putLong("zuletzt", System.currentTimeMillis()).apply()
            if (!online(app)) return@withContext
            val m = monitor(app)
            val fensterKennung = p.getString("fenster", "").orEmpty()
            if (m.schlaeft()) {
                // Von selbst geht sie bei regelmäßigem Signal nicht aus: Lag das letzte bestätigte Signal deutlich unter der Abschaltzeit zurück, hat Frank
                // sie am Gerät ausgeschaltet. Nur nach langer Signalpause (Handy aus, kein Netz) oder zum Fensterstart weckt Jarvis.
                val aus = when (einstellungLesen(app, EINSTELLUNGEN[0])) { 0 -> 15; 1 -> 30; 2 -> 60; else -> 180 }
                p.edit().putInt("abschaltMin", aus).apply()
                val seitSignal = System.currentTimeMillis() - p.getLong("signalOk", 0L)
                val amGeraetAus = seitSignal in 0 until aus * 60_000L - signalAbstandMs(aus)
                if (amGeraetAus && p.getString("aus", "") != fensterKennung) {
                    manuellAus(app)
                    de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "am Gerät ausgeschaltet, in diesem Fenster nicht wieder wecken")
                }
                val geweckt = p.getInt("weckAnzahl", 0)
                if (p.getString("aus", "") != fensterKennung && geweckt < WACH_MAX_WECKEN) {
                    // signalOk erst setzen, wenn sie wirklich bereit gemeldet hat (nächstes Signal), sonst hielte ein verlorener Weckbefehl sie für „am Gerät aus“.
                    wecken(app)
                    p.edit().putInt("weckAnzahl", geweckt + 1).putLong("signalOk", 0L).putLong("zuletzt", System.currentTimeMillis() - signalAbstandMs(aus) + 2 * 60_000L).apply()
                    de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "geweckt (${geweckt + 1}. Mal in diesem Fenster)")
                }
            } else if (m.frisch) {
                befehl(app, 0xa9, 0xf0, byteArrayOf(1))
                p.edit().putLong("signalOk", System.currentTimeMillis()).apply()
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (x: Exception) {
            de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "Fehler: ${x.message ?: x.javaClass.simpleName}")
        } finally { sperre.unlock() }
    }

    // ---- Nur zu Hause ----
    /** Zuhause-Koordinaten: einmal aus der Adresse in den Einstellungen ermittelt (Netz, daher nur im Hintergrund) oder per Knopf vom aktuellen Standort. */
    suspend fun zuhause(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val p = context.applicationContext.getSharedPreferences(WACH_PREFS, Context.MODE_PRIVATE)
        val adresse = de.frank.jarvis.data.Einstellungen.get(context.applicationContext).adresseZuhause
        if (p.contains("heimLat") && p.getString("heimAdresse", "") == adresse)
            return@withContext p.getFloat("heimLat", 0f).toDouble() to p.getFloat("heimLon", 0f).toDouble()
        @Suppress("DEPRECATION")
        val treffer = runCatching { android.location.Geocoder(context, Locale.GERMANY).getFromLocationName(adresse, 1)?.firstOrNull() }.getOrNull() ?: return@withContext null
        p.edit().putFloat("heimLat", treffer.latitude.toFloat()).putFloat("heimLon", treffer.longitude.toFloat()).putString("heimAdresse", adresse).apply()
        treffer.latitude to treffer.longitude
    }

    /** Knopf auf der Karte: „Ich bin gerade zu Hause“, genauer als die Adresse. Nur mit einer frischen Ortung (höchstens 2 Minuten alt). */
    suspend fun zuhauseHierSetzen(context: Context): String {
        val ort = de.frank.jarvis.fahrt.Standort.aktuell(context.applicationContext, 15_000)?.takeIf { System.currentTimeMillis() - it.time < 2 * 60_000L }
            ?: throw Fehler("Jarvis bekommt gerade keine frische Ortung. Bitte gleich noch einmal versuchen (Ortung an und für Jarvis erlaubt?).")
        context.applicationContext.getSharedPreferences(WACH_PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("heimLat", ort.latitude.toFloat()).putFloat("heimLon", ort.longitude.toFloat())
            .putString("heimAdresse", de.frank.jarvis.data.Einstellungen.get(context.applicationContext).adresseZuhause).apply()
        return "Zuhause ist jetzt dieser Standort (Genauigkeit etwa ${ort.accuracy.toInt()} m)."
    }

    /** Abstand zu Hause in Metern samt Ungenauigkeit, null ohne Standort oder Zuhause. */
    suspend fun abstandZuHause(context: Context): Pair<Float, Float>? = withContext(Dispatchers.IO) {
        val heim = zuhause(context) ?: return@withContext null
        val ort = de.frank.jarvis.fahrt.Standort.aktuell(context.applicationContext, 8000) ?: return@withContext null
        val d = FloatArray(1)
        android.location.Location.distanceBetween(heim.first, heim.second, ort.latitude, ort.longitude, d)
        d[0] to ort.accuracy
    }

    /**
     * Prüft höchstens alle 3 Minuten den Standort. Weg heißt: zweimal hintereinander sicher außerhalb des Umkreises (Abstand minus Ungenauigkeit);
     * schon nach der ersten Messung draußen weckt und signalisiert Jarvis nicht. Beim Verlassen schaltet Jarvis die Maschine aus (wenn sie gerade
     * brüht, beim nächsten Mal). Kommt Frank zurück, weckt Jarvis sie nur an freien Tagen wieder: Begann das Fenster an einem Diensttag, bleibt sie aus.
     * true = zu Hause (Wachhalten geht weiter), false = weg oder unsicher.
     */
    private suspend fun zuhausePruefen(app: Context, p: android.content.SharedPreferences, fensterTag: java.time.LocalDate): Boolean {
        val warWeg = p.getBoolean("weg", false)
        val unsicher = p.getInt("draussen", 0) > 0
        if (System.currentTimeMillis() - p.getLong("ortZuletzt", 0L) in 0 until 3 * 60_000L) return !warWeg && !unsicher
        p.edit().putLong("ortZuletzt", System.currentTimeMillis()).apply()
        val radius = de.frank.jarvis.data.Einstellungen.get(app).kaffeeRadius
        val (abstand, ungenau) = abstandZuHause(app) ?: return !warWeg && !unsicher
        val sicherDraussen = abstand - minOf(ungenau, 150f) > radius
        val sicherDrinnen = abstand <= radius + minOf(ungenau, 50f)
        if (!warWeg && sicherDraussen) {
            val zweimal = p.getInt("draussen", 0) + 1
            p.edit().putInt("draussen", zweimal).apply()
            if (zweimal < 2) return false
            var ausGeschickt = false
            runCatching {
                sperre.withLock {
                    if (online(app)) {
                        val m = monitor(app)
                        if (m.frisch && m.status == 7 && m.aktion != 0) return@withLock
                        if (m.frisch && !m.schlaeft()) ausBefehl(app)
                        ausGeschickt = true
                    }
                }
            }
            if (!ausGeschickt) return false
            // signalOk löschen: Das Aus beim Verlassen kommt von Jarvis und darf beim Heimkommen nicht als „am Gerät ausgeschaltet“ gelten.
            p.edit().putBoolean("weg", true).putInt("draussen", 0).putLong("signalOk", 0L).apply()
            de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "Frank ist weg (${abstand.toInt()} m), Maschine aus")
            return false
        }
        if (!sicherDraussen) p.edit().putInt("draussen", 0).apply()
        if (warWeg && sicherDrinnen) {
            p.edit().putBoolean("weg", false).putLong("zuletzt", 0L).apply()
            if (dienstArt(app, fensterTag) != null) {
                manuellAus(app)
                de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "zurück nach dem Dienst, Maschine bleibt aus")
            } else de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine wach halten", "Frank ist zurück, Maschine wird geweckt")
            return true
        }
        return !warWeg && !sicherDraussen
    }

    suspend fun einstellen(context: Context, e: Einstellung, wert: Int): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (wert !in e.werte) throw Fehler("Für ${e.name} gibt es nur: ${e.werte.values.joinToString(", ")}.")
            if (!online(context)) throw Fehler("Die Kaffeemaschine ist offline.")
            schreiben(context, "device_connected", System.currentTimeMillis() / 1000)
            delay(800)
            val vorher = runCatching { lesen(context, "data_response").zeit }.getOrDefault("")
            val d = byteArrayOf((e.id shr 8).toByte(), e.id.toByte(), (wert ushr 24).toByte(), (wert ushr 16).toByte(), (wert ushr 8).toByte(), wert.toByte())
            befehl(context, 0x90, 0x0f, d)
            var ergebnis = "${e.name}: ${e.werte[wert]} gesendet; die Maschine hat noch nicht bestätigt."
            for (versuch in 1..8) {
                delay(1000)
                val w = runCatching { lesen(context, "data_response") }.getOrNull() ?: continue
                if (w.zeit == vorher) continue
                val a = antwort(w.text, 0x90)?.daten ?: continue
                if (a.size >= 3 && (((a[0].toInt() and 0xFF) shl 8) or (a[1].toInt() and 0xFF)) == e.id) {
                    if (a[2].toInt() != 0) throw Fehler("Die Maschine hat ${e.name} abgelehnt.")
                    ergebnis = "${e.name}: ${e.werte[wert]}."
                    break
                }
            }
            ergebnis
        }
    }
}
