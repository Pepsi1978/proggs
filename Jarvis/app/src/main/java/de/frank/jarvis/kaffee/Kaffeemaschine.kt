package de.frank.jarvis.kaffee

import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * De'Longhi PrimaDonna Soul (ECAM 610.74, WLAN-Modul Ayla „DL-millcore“) über die Coffee-Link-Cloud, wie die offizielle App:
 * Anmeldung Gigya → Ayla-Token, Befehle als ECAM-Rahmen in `data_request`, Antworten in `data_response`, Live-Zustand in `d302_monitor`.
 * Wissen und Fallen: best-practices/peripherie/delonghi-coffeelink.md und bugs/peripherie/delonghi-coffeelink.md.
 * Rezepte holt Jarvis vor jedem Getränk frisch aus der Maschine (Befehl 0xA6) und brüht sie genau so, wie die Maschine sie gespeichert hat.
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
        Getraenk(0x04, "2x_espresso", "2× Espresso", Bild.DOPPEL, 3, listOf("2x espresso", "zwei espresso", "doppelter espresso", "2 espresso")),
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

    fun finden(text: String): Getraenk? {
        val t = text.lowercase().trim()
        if (t.isEmpty()) return null
        return GETRAENKE.firstOrNull { g -> g.woerter.any { t == it } } ?: GETRAENKE.sortedByDescending { g -> g.woerter.maxOf { it.length } }
            .firstOrNull { g -> g.woerter.any { it in t } || t in g.name.lowercase() }
    }

    /** Einstellungen der Maschine: Kennung im ECAM-Protokoll, Eigenschaft mit dem aktuellen Wert, erlaubte Werte. */
    data class Einstellung(val schluessel: String, val name: String, val id: Int, val eigenschaft: String, val werte: Map<Int, String>)

    val EINSTELLUNGEN = listOf(
        Einstellung("abschaltzeit", "Automatisch aus nach", 0x3e, "d282_mchn_sett_aoff", mapOf(0 to "15 Minuten", 1 to "30 Minuten", 2 to "1 Stunde", 3 to "3 Stunden")),
        Einstellung("wasserhaerte", "Wasserhärte", 0x32, "d283_mchn_sett_water", mapOf(0 to "Stufe 1 (weich)", 1 to "Stufe 2", 2 to "Stufe 3", 3 to "Stufe 4 (sehr hart)")),
    )

    data class Zustand(
        val online: Boolean, val status: Int, val aktion: Int, val fortschritt: Int, val alarme: List<String>,
        val einstellungen: Map<String, Int>, val profil: Int, val profile: Map<Int, String>, val stand: Long,
    ) {
        val bereit get() = online && status == 7 && aktion == 0
        val brueht get() = online && status == 7 && aktion != 0
        fun text(): String = when {
            !online -> "Offline (nicht mit dem WLAN verbunden)"
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

    // ---- Zugang (aus der Coffee-Link-APK, öffentlich dokumentiert in actabi/delonghi_coffeelink) ----
    private const val GIGYA_KEY = "3_e5qn7USZK-QtsIso1wCelqUKAK_IVEsYshRIssQ-X-k55haiZXmKWDHDRul2e5Y2" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)
    private const val GIGYA = "https://accounts.eu1.gigya.com"
    private const val AYLA_USER = "https://user-field-eu.aylanetworks.com"
    private const val ADS = "https://ads-eu.aylanetworks.com/apiv1"
    private const val APP_ID = "DLonghiCoffeeIdKit-sQ-id" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)
    private const val APP_SECRET = "DLonghiCoffeeIdKit-HT6b0VNd4y6CSha9ivM5k8navLw" // gitleaks:allow (öffentliche Kennung der Coffee-Link-App, kein Geheimnis von Frank)

    private val sperre = Mutex()
    @Volatile private var zugang: String = ""
    @Volatile private var zugangBis = 0L
    @Volatile private var dsnCache = ""

    fun konfiguriert(context: Context): Boolean {
        val s = SecureSettings.get(context)
        return s.delonghiMail.isNotBlank() && s.delonghiPasswort.isNotBlank()
    }

    // ---- HTTP ----
    private fun http(methode: String, url: String, body: String? = null, form: Boolean = false, token: String? = null): Pair<Int, String> {
        val v = URL(url).openConnection() as HttpURLConnection
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
        v.disconnect()
        return code to text
    }

    private fun formular(vararg p: Pair<String, String>) = p.joinToString("&") { (k, w) -> "$k=" + URLEncoder.encode(w, "UTF-8") }
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

    /** Gigya-Anmeldung mit E-Mail und Passwort, dann ein signiertes JWT, das Ayla gegen ein eigenes Token tauscht. */
    private fun anmelden(context: Context) {
        val s = SecureSettings.get(context)
        val (c1, t1) = http("POST", "$GIGYA/accounts.login", formular("apiKey" to GIGYA_KEY, "loginID" to s.delonghiMail, "password" to s.delonghiPasswort, "format" to "json", "targetEnv" to "mobile"), form = true)
        val login = runCatching { JSONObject(t1) }.getOrNull()
        if (c1 !in 200..299 || login?.optInt("errorCode", -1) != 0) throw Fehler("Die De'Longhi-Anmeldung ist fehlgeschlagen" + (login?.optString("errorMessage")?.let { ": $it" } ?: "") + ". Prüf E-Mail und Passwort unter Konten.")
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

    private fun uebernehmen(context: Context, code: Int, text: String): String? {
        if (code !in 200..299) return null
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val a = j.optString("access_token").ifBlank { return null }
        zugang = a
        zugangBis = System.currentTimeMillis() + (j.optLong("expires_in", 3600) - 120) * 1000
        j.optString("refresh_token").takeIf { it.isNotBlank() }?.let { SecureSettings.get(context).delonghiAuffrischung = it }
        return a
    }

    private fun token(context: Context): String {
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
        if (c !in 200..299) throw Fehler("Die Coffee-Link-Cloud antwortet nicht wie erwartet (HTTP $c).")
        return t
    }

    private data class Geraet(val dsn: String, val online: Boolean)

    private fun geraet(context: Context): Geraet {
        val a = JSONArray(ads(context, "GET", "/devices.json"))
        for (i in 0 until a.length()) {
            val d = a.getJSONObject(i).optJSONObject("device") ?: a.getJSONObject(i)
            if (d.optString("oem_model").startsWith("DL-millcore") || a.length() == 1) {
                dsnCache = d.optString("dsn")
                return Geraet(dsnCache, d.optString("connection_status").equals("Online", true))
            }
        }
        throw Fehler("Im De'Longhi-Konto ist keine PrimaDonna Soul eingetragen.")
    }

    private fun dsn(context: Context) = dsnCache.ifBlank { geraet(context).dsn }

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

    /** Antwort der Maschine (0xD0 …) prüfen und die Nutzdaten nach Befehl und Flags liefern. */
    private fun antwort(b64: String, op: Int): ByteArray? {
        val r = runCatching { Base64.decode(b64.trim(), Base64.DEFAULT) }.getOrNull() ?: return null
        if (r.size < 6 || (r[0].toInt() and 0xFF) != 0xD0 || (r[2].toInt() and 0xFF) != op) return null
        val ende = (r[1].toInt() and 0xFF) + 1
        if (ende > r.size || ende < 6) return null
        val c = ((r[ende - 2].toInt() and 0xFF) shl 8) or (r[ende - 1].toInt() and 0xFF)
        if (c != crc(r, ende - 2)) return null
        return r.copyOfRange(4, ende - 2)
    }

    private fun befehl(context: Context, op: Int, flags: Int, daten: ByteArray) = schreiben(context, "data_request", rahmen(op, flags, daten))

    // ---- Zustand ----
    /** Die Soul meldet ihren Live-Zustand nur, wenn eine App-Sitzung in `device_connected` steht; danach kommt er nach 4–7 s. */
    suspend fun zustand(context: Context, frisch: Boolean = true): Zustand = withContext(Dispatchers.IO) {
        sperre.withLock { zustandOhneSperre(context, frisch) }
    }

    private suspend fun zustandOhneSperre(context: Context, frisch: Boolean): Zustand {
        if (!konfiguriert(context)) throw Fehler("Für die Kaffeemaschine fehlt die De'Longhi-Anmeldung unter Einstellungen, Konten und APIs.")
        val g = geraet(context)
        if (!g.online) return Zustand(false, 0, 0, 0, emptyList(), emptyMap(), 1, emptyMap(), System.currentTimeMillis())
        var mon = lesen(context, "d302_monitor")
        if (frisch) {
            val vorher = mon.zeit
            schreiben(context, "device_connected", System.currentTimeMillis() / 1000)
            for (versuch in 1..10) {
                delay(1000)
                val n = lesen(context, "d302_monitor")
                if (n.zeit != vorher) { mon = n; break }
            }
        }
        val d = antwort(mon.text, 0x75) ?: throw Fehler("Die Kaffeemaschine hat keinen lesbaren Zustand geschickt.")
        val inhalt = d.copyOfRange(2, d.size)
        fun u(i: Int) = if (i < inhalt.size) inhalt[i].toInt() and 0xFF else 0
        val alarmBits = u(3) or (u(4) shl 8) or (u(8) shl 16) or (u(9) shl 24)
        val alarme = ALARME.filterKeys { alarmBits and (1 shl it) != 0 }.values.toList()
        val einst = EINSTELLUNGEN.associate { e -> e.schluessel to (einstellungLesen(context, e) ?: -1) }
        return Zustand(true, u(5), u(6), u(7), alarme, einst, 1, profile(context), System.currentTimeMillis())
    }

    private fun einstellungLesen(context: Context, e: Einstellung): Int? {
        val d = antwort(runCatching { lesen(context, e.eigenschaft).text }.getOrDefault(""), 0x95) ?: return null
        if (d.size < 6) return null
        return ((d[2].toInt() and 0xFF) shl 24) or ((d[3].toInt() and 0xFF) shl 16) or ((d[4].toInt() and 0xFF) shl 8) or (d[5].toInt() and 0xFF)
    }

    /** Profilnamen aus d034/d035 (UTF-16, je 20 Bytes Name plus 1 Byte Symbol). */
    private fun profile(context: Context): Map<Int, String> {
        val namen = mutableMapOf<Int, String>()
        for ((eig, erstes) in listOf("d034_profiles_1_3" to 1, "d035_profiles_4_5" to 4)) {
            val d = antwort(runCatching { lesen(context, eig).text }.getOrDefault(""), 0xa4) ?: continue
            var i = 2
            var nr = erstes
            while (i + 20 <= d.size) {
                val name = String(d.copyOfRange(i, i + 20), Charsets.UTF_16BE).trimEnd('\u0000').trim()
                if (name.isNotBlank()) namen[nr] = name
                i += 21; nr++
            }
        }
        return namen
    }

    // ---- Befehle ----
    private suspend fun aufBereitWarten(context: Context, sekunden: Int): Zustand {
        var z = zustandOhneSperre(context, true)
        val ende = System.currentTimeMillis() + sekunden * 1000L
        while (!z.bereit && System.currentTimeMillis() < ende) {
            if (z.alarme.isNotEmpty() && z.status == 7) break
            delay(4000)
            z = zustandOhneSperre(context, true)
        }
        return z
    }

    suspend fun aufwecken(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            val z = zustandOhneSperre(context, true)
            if (!z.online) throw Fehler("Die Kaffeemaschine ist offline.")
            if (z.status != 0) return@withLock "Die Kaffeemaschine ist schon an (${z.text()})."
            befehl(context, 0x84, 0x0f, byteArrayOf(2, 1))
            "Die Kaffeemaschine wacht auf und spült kurz, dann ist sie bereit."
        }
    }

    suspend fun ausschalten(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (!zustandOhneSperre(context, false).online) throw Fehler("Die Kaffeemaschine ist offline.")
            befehl(context, 0x84, 0x0f, byteArrayOf(1, 1))
            "Die Kaffeemaschine spült und geht in Standby."
        }
    }

    suspend fun stoppen(context: Context): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            befehl(context, 0x83, 0xf0, byteArrayOf(0x10, 0x02, 0x0f, 0x00, 0xfa.toByte(), 0x1b, 0x01, 0x06))
            "Ich habe die Zubereitung gestoppt."
        }
    }

    /**
     * Holt das Rezept des Profils frisch aus der Maschine (0xA6, Antwort landet in d0xx_<profil>_rec_<name>) und baut daraus den
     * Brühbefehl wie die Coffee-Link-App: Getränk, Start, die Zutaten in der Reihenfolge der Maschine ohne Metadaten, am Ende das Profil.
     */
    private suspend fun rezept(context: Context, g: Getraenk, profil: Int): ByteArray {
        val eig = "d%03d_%d_rec_%s".format(39 + (profil - 1) * 21 + g.nr, profil, g.schluessel)
        val vorher = runCatching { lesen(context, eig) }.getOrNull()
        befehl(context, 0xa6, 0xf0, byteArrayOf(profil.toByte(), g.id.toByte()))
        var wert = vorher
        for (versuch in 1..8) {
            delay(1000)
            val n = runCatching { lesen(context, eig) }.getOrNull()
            if (n != null && n.zeit != vorher?.zeit) { wert = n; break }
        }
        val d = wert?.let { antwort(it.text, 0xa6) } ?: throw Fehler("Die Maschine hat das Rezept für ${g.name} nicht geschickt.")
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
            if ((tag < 23 && tag != 0x08) || tag in setOf(0x1b, 0x1c, 0x1f, 0x21, 0x26, 0x27)) for (k in 0 until lang) felder += d[i + k]
            i += lang
        }
        if (!sichtbar || !menge) throw Fehler("${g.name} ist an deiner Maschine im Profil $profil nicht freigeschaltet.")
        return byteArrayOf(g.id.toByte(), 0x01) + felder.toByteArray() + byteArrayOf(((profil shl 2) or 2).toByte())
    }

    /** Weckt die Maschine bei Bedarf, wartet die Aufheiz-Spülung ab und brüht dann das Getränk im aktiven Profil. */
    suspend fun bruehen(context: Context, g: Getraenk, profil: Int = 1, fortschritt: (String) -> Unit = {}): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            var z = zustandOhneSperre(context, true)
            if (!z.online) throw Fehler("Die Kaffeemaschine ist offline, also nicht mit dem WLAN verbunden.")
            if (z.status == 0) {
                fortschritt("Weckt die Maschine auf …")
                befehl(context, 0x84, 0x0f, byteArrayOf(2, 1))
                delay(5000)
            }
            if (!z.bereit) {
                fortschritt("Wartet, bis die Maschine bereit ist …")
                z = aufBereitWarten(context, 150)
            }
            if (z.alarme.isNotEmpty()) throw Fehler("Die Maschine meldet: ${z.alarme.joinToString(", ")}.")
            if (z.brueht) throw Fehler("Die Maschine bereitet gerade schon etwas zu.")
            if (!z.bereit) throw Fehler("Die Maschine ist nicht bereit (${z.text()}).")
            fortschritt("Holt das Rezept …")
            val daten = rezept(context, g, profil)
            befehl(context, 0x83, 0xf0, daten)
            fortschritt("${g.name} läuft …")
            "${g.name} läuft."
        }
    }

    suspend fun einstellen(context: Context, e: Einstellung, wert: Int): String = withContext(Dispatchers.IO) {
        sperre.withLock {
            if (wert !in e.werte) throw Fehler("Für ${e.name} gibt es nur: ${e.werte.values.joinToString(", ")}.")
            if (!zustandOhneSperre(context, false).online) throw Fehler("Die Kaffeemaschine ist offline.")
            schreiben(context, "device_connected", System.currentTimeMillis() / 1000)
            delay(800)
            val d = byteArrayOf((e.id shr 8).toByte(), e.id.toByte(), (wert ushr 24).toByte(), (wert ushr 16).toByte(), (wert ushr 8).toByte(), wert.toByte())
            befehl(context, 0x90, 0x0f, d)
            var ergebnis = "${e.name}: ${e.werte[wert]} gesendet; die Maschine hat noch nicht bestätigt."
            for (versuch in 1..8) {
                delay(1000)
                val a = antwort(runCatching { lesen(context, "data_response").text }.getOrDefault(""), 0x90)
                if (a != null && a.size >= 3 && (((a[0].toInt() and 0xFF) shl 8) or (a[1].toInt() and 0xFF)) == e.id) {
                    if (a[2].toInt() != 0) throw Fehler("Die Maschine hat ${e.name} abgelehnt.")
                    ergebnis = "${e.name}: ${e.werte[wert]}."
                    break
                }
            }
            ergebnis
        }
    }
}
