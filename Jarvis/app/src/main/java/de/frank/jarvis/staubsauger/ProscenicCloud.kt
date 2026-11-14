package de.frank.jarvis.staubsauger

import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import de.frank.jarvis.observability.Logbuch
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Steuert den Proscenic M7 Pro direkt über die Proscenic-Cloud, ganz ohne die Proscenic-App: unsichtbar und im Hintergrund.
 * Protokoll (ProscenicHome-Cloud der 3irobotix-Plattform, EU-Server), belegt durch ioBroker.proscenic 0.4.0 (auf einem M7 Pro
 * geprüft), JuliusBlueTek/Proscenic-Home-Assistant, Fluepke/proscenic und andker87/Proscenic-M7-PRO (recherchiert 14.11.2026):
 *  - Anmeldung `POST /user/login` (E-Mail, MD5-Passwort) → Token; `POST /user/getEquips/{mail}` → Seriennummer.
 *  - Befehle als Formular-POST an `/instructions/...` mit Header `token`.
 *  - Stand über ein TCP-Gateway (`/appInit/getSockAddr`): Anmeldung `infoType 70001`, Antworten AES-128-ECB mit dem Token als Schlüssel,
 *    Rahmenende `#\t#`; `infoType 20001` trägt `mode` und `elec` (Akku).
 * Der Token wird gespeichert und nur bei Fehlern neu geholt, damit Jarvis die Proscenic-App möglichst selten verdrängt.
 */
object ProscenicCloud {
    private const val HOST = "https://mobile.proscenic.com.de"
    private const val ENDE = "#\t#"
    private val CLIENT = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).build()

    fun konfiguriert(context: Context): Boolean {
        val s = SecureSettings.get(context)
        return s.proscenicMail.isNotBlank() && s.proscenicPasswort.isNotBlank()
    }

    /** Nicht weiter versuchen: falsche Anmeldedaten oder kein Roboter im Konto. */
    class Abgewiesen(text: String) : Exception(text)

    private data class Sitzung(val token: String, val sn: String, val mail: String)

    private fun prefs(c: Context) = c.getSharedPreferences("staubsauger_cloud", Context.MODE_PRIVATE)

    suspend fun ausfuehren(context: Context, aktion: ProscenicSteuerung.Aktion): SaugerAusgang = withContext(Dispatchers.IO) {
        try {
            var s = sitzung(context, neu = false)
            var neuAngemeldet = false
            val vorher = try { stand(s) } catch (e: TokenUngueltig) {
                s = sitzung(context, neu = true); neuAngemeldet = true
                runCatching { stand(s) }.getOrNull()
            } catch (e: Exception) { null }
            val antwort = try {
                befehl(s, aktion, vorher)
            } catch (e: TokenUngueltig) {
                if (neuAngemeldet) throw Abgewiesen("Die Proscenic-Cloud lehnt die Anmeldung von Jarvis ab; trag E-Mail und Passwort unter Konten, Proscenic neu ein.")
                s = sitzung(context, neu = true)
                befehl(s, aktion, vorher)
            }
            // Kurz warten und den neuen Stand lesen, damit Frank hört, ob der Roboter reagiert hat.
            val nachher = if (aktion == ProscenicSteuerung.Aktion.STATUS) vorher else {
                var st: Stand? = null
                for (i in 0 until 3) { delay(if (i == 0) 3000 else 2500); st = runCatching { stand(s) }.getOrNull(); if (st != null && st.mode != vorher?.mode) break }
                st
            }
            nachher?.let { SaugerStand.merken(context, it.akku, it.zustand, it.saugstufe) }
            val standText = nachher?.let { listOfNotNull("Zustand: ${it.zustand}.", it.akku?.let { a -> "Akku $a Prozent." }, it.saugstufe?.let { m -> "Saugstufe: $m." }).joinToString(" ") }
                ?: if (aktion == ProscenicSteuerung.Aktion.STATUS) "" else "Den neuen Stand konnte ich nicht abrufen."
            if (aktion == ProscenicSteuerung.Aktion.STATUS && nachher == null)
                return@withContext SaugerAusgang("Den Stand des M7 Pro konnte ich über die Proscenic-Cloud gerade nicht abrufen; vermutlich ist das Internet zu langsam.", fehler = true)
            SaugerAusgang(listOf(antwort, standText).filter { it.isNotBlank() }.joinToString(" "))
        } catch (e: Abgewiesen) {
            SaugerAusgang(e.message.orEmpty(), fehler = true)
        } catch (e: TokenUngueltig) {
            SaugerAusgang("Die Proscenic-Cloud lehnt die Anmeldung von Jarvis ab; trag E-Mail und Passwort unter Konten, Proscenic neu ein.", fehler = true)
        } catch (e: java.io.IOException) {
            SaugerAusgang("Die Proscenic-Cloud war nicht erreichbar (${e.message ?: e.javaClass.simpleName}); vermutlich ist das Internet gerade zu langsam. Der Befehl kam nicht an.", fehler = true)
        }
        catch (e: org.json.JSONException) {
            SaugerAusgang("Die Proscenic-Cloud hat unerwartet geantwortet (${e.message}).", fehler = true)
        }
    }

    // ---- Anmeldung ----

    private class TokenUngueltig : Exception()

    private fun sitzung(context: Context, neu: Boolean): Sitzung {
        val s = SecureSettings.get(context)
        val mail = s.proscenicMail.trim()
        val p = prefs(context)
        if (!neu && p.getString("mail", null) == mail) {
            val token = p.getString("token", null); val sn = p.getString("sn", null)
            if (!token.isNullOrBlank() && !sn.isNullOrBlank()) return Sitzung(token, sn, mail)
        }
        val pw = s.proscenicPasswort
        val body = JSONObject().put("state", "欧洲").put("countryCode", "49").put("appVer", "1.7.8").put("type", "2").put("os", "IOS")
            .put("password", md5(pw)).put("registrationId", "13165ffa4eb156ac484").put("language", "EN").put("username", mail).put("pwd", pw)
        val login = post("/user/login", null, body.toString().toRequestBody("application/json".toMediaType()), ios = true)
        val daten = login.optJSONObject("data")
        val token = daten?.optString("token").orEmpty()
        if (token.isBlank() || daten?.optInt("equipcount", 0) == 0)
            throw Abgewiesen("Die Anmeldung bei Proscenic hat nicht geklappt: Prüfe E-Mail und Passwort in Jarvis unter Konten, Proscenic. Ist der M7 Pro in einem anderen Konto, trag dieses ein.")
        val geraete = post("/user/getEquips/$mail", token, FormBody.Builder().add("username", mail).build())
        val liste = geraete.optJSONObject("data")?.optJSONArray("content")
        val roboter = (0 until (liste?.length() ?: 0)).mapNotNull { liste?.optJSONObject(it) }
            .firstOrNull { it.optString("typeName").equals("CleanRobot", true) || it.optString("code").contains("M7", true) }
        val sn = roboter?.optString("sn").orEmpty()
        if (sn.isBlank()) throw Abgewiesen("In deinem Proscenic-Konto finde ich keinen Saugroboter.")
        p.edit().putString("mail", mail).putString("token", token).putString("sn", sn).apply()
        Logbuch.netz("Proscenic: Anmeldung", "neu angemeldet, Geräte ${daten?.optInt("equipcount")}, Modell ${roboter?.optString("model")}")
        return Sitzung(token, sn, mail)
    }

    // ---- Befehle ----

    private fun befehl(s: Sitzung, aktion: ProscenicSteuerung.Aktion, stand: Stand?): String {
        val m = stand?.mode.orEmpty()
        val user = "?username=" + java.net.URLEncoder.encode(s.mail, "UTF-8")
        fun form(vararg felder: Pair<String, String>) = FormBody.Builder().apply { add("username", s.mail); felder.forEach { add(it.first, it.second) } }.build()
        return when (aktion) {
            ProscenicSteuerung.Aktion.STATUS -> ""
            ProscenicSteuerung.Aktion.REINIGEN -> when (m) {
                "sweep" -> "Der M7 Pro reinigt schon."
                "pause" -> { senden(s, "/instructions/${s.sn}/21017$user", form("pauseOrContinue" to "continue")); "Reinigung fortgesetzt." }
                else -> { senden(s, "/instructions/cmd21005/${s.sn}$user", form("cleanMode" to "sweepOnly")); "Reinigung gestartet." }
            }
            ProscenicSteuerung.Aktion.TIEFENREINIGUNG ->
                if (m == "sweep") "Der M7 Pro reinigt gerade schon; halte ihn erst an, dann starte ich die Tiefenreinigung."
                else { senden(s, "/instructions/cmd21005_2/${s.sn}$user", form("mode" to "depthTotalClean")); "Tiefenreinigung gestartet." }
            ProscenicSteuerung.Aktion.STOPPEN ->
                if (stand != null && m !in setOf("sweep", "backcharge", "DustCenterWorking")) "Der M7 Pro reinigt gerade nicht."
                else { senden(s, "/instructions/${s.sn}/21017$user", form("mode" to "pause")); "Reinigung angehalten." }
            ProscenicSteuerung.Aktion.STATION -> when (m) {
                "backcharge" -> "Der M7 Pro fährt schon zur Ladestation."
                "charge", "fullcharge" -> "Der M7 Pro steht schon in der Ladestation."
                else -> { senden(s, "/instructions/${s.sn}/21012$user", form("charge" to "start")); "Der M7 Pro fährt zurück zur Ladestation." }
            }
            ProscenicSteuerung.Aktion.POSITIONIEREN -> {
                senden(s, "/instructions/${s.sn}/21020$user", form("ctrlCode" to "3010"))
                "Gerät positionieren an den M7 Pro gesendet; er sollte sich jetzt mit einer Ansage melden."
            }
            ProscenicSteuerung.Aktion.STAUBSAMMLUNG ->
                if (m == "sweep") "Der M7 Pro reinigt gerade; die Staubsammlung geht erst in der Station."
                else {
                    val json = JSONObject().put("dInfo", JSONObject().put("userId", s.mail).put("ts", System.currentTimeMillis().toString()))
                        .put("data", JSONObject().put("cmd", "startDustCenter").put("value", 0)).put("infoType", 21024)
                    senden(s, "/instructions/cmd/${s.sn}$user", json.toString().toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    "Staubsammlung gestartet."
                }
            ProscenicSteuerung.Aktion.WISCHEN -> throw IllegalStateException("Y-Wischen ist über die Cloud nicht bekannt")
        }
    }

    private fun senden(s: Sitzung, pfad: String, body: RequestBody) {
        val a = post(pfad, s.token, body)
        val code = a.optInt("code", 0)
        Logbuch.netz("Proscenic: Befehl", "${pfad.substringBefore('?')} → Code $code ${a.optString("msg")}".take(300))
        if (code == 102 || code == 401 || code == 403) throw TokenUngueltig()
        if (code != 0) throw Abgewiesen("Die Proscenic-Cloud hat den Befehl abgelehnt (Code $code: ${a.optString("msg").ifBlank { "ohne Grund" }}).")
    }

    private fun post(pfad: String, token: String?, body: RequestBody, ios: Boolean = false): JSONObject {
        val r = Request.Builder().url(HOST + pfad).post(body)
            .header("User-Agent", "ProscenicHome/1.7.8 (iPhone; iOS 14.2.1; Scale/3.00)")
            .apply { if (ios) { header("os", "i"); header("c", "338"); header("lan", "en"); header("v", "1.7.8") } }
            .apply { if (token != null) header("token", token) }
            .build()
        var letzter: java.io.IOException? = null
        repeat(2) { versuch ->
            try {
                CLIENT.newCall(r).execute().use { a ->
                    if (a.code == 401 || a.code == 403) throw TokenUngueltig()
                    val text = a.body?.string().orEmpty()
                    if (!a.isSuccessful) throw java.io.IOException("HTTP ${a.code}")
                    return runCatching { JSONObject(text) }.getOrElse { JSONObject().put("code", 0).put("roh", text) }
                }
            } catch (e: java.io.IOException) {
                letzter = e
                if (versuch == 0) Thread.sleep(1500)
            }
        }
        throw letzter ?: java.io.IOException("keine Antwort")
    }

    // ---- Stand über das TCP-Gateway ----

    private data class Stand(val mode: String, val akku: Int?, val saugstufe: String?) {
        val zustand: String get() = when (mode) {
            "charge" -> "lädt in der Station"
            "fullcharge" -> "voll geladen in der Station"
            "sweep" -> "reinigt"
            "pause" -> "pausiert"
            "findchargerpause" -> "Rückfahrt pausiert"
            "backcharge" -> "fährt zur Ladestation"
            "DustCenterWorking" -> "Staubsammlung läuft"
            "idle" -> "bereit"
            "shutdown" -> "ausgeschaltet"
            "fault" -> "Störung"
            "rfctrl" -> "Fernsteuerung"
            else -> mode
        }
    }

    private fun stand(s: Sitzung): Stand? {
        // Zur Erkundung: Antwort des HTTP-Infopunkts mitschreiben (Aufbau ist nirgends dokumentiert).
        runCatching { post("/app/cleanRobot/info", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build()) }
            .onSuccess { Logbuch.netz("Proscenic: Info", it.toString().take(900)) }
        val adr = post("/appInit/getSockAddr", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build())
        Logbuch.netz("Proscenic: Gateway", adr.toString().take(300))
        if (adr.optInt("code", 0) == 102) throw TokenUngueltig()
        val eintrag = adr.optJSONObject("data")?.optJSONArray("addr_list")?.optJSONObject(0) ?: return null
        val ip = eintrag.optString("ip"); val port = eintrag.optInt("port")
        if (ip.isBlank() || port <= 0) return null
        Socket().use { sock ->
            sock.connect(InetSocketAddress(ip, port), 10_000)
            sock.soTimeout = 3000
            val anmeldung = JSONObject().put("data", JSONObject().put("token", s.token).put("sn", s.sn)).put("infoType", 70001).toString() + ENDE
            sock.getOutputStream().apply { write(anmeldung.toByteArray()); flush() }
            val ein = sock.getInputStream()
            val puffer = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            val bis = System.currentTimeMillis() + 15_000
            val gesehen = mutableListOf<String>()
            while (System.currentTimeMillis() < bis) {
                val n = try { ein.read(buf) } catch (_: java.net.SocketTimeoutException) { continue }
                if (n < 0) break
                puffer.write(buf, 0, n)
                var text = puffer.toString("UTF-8")
                while (true) {
                    val i = text.indexOf(ENDE); if (i < 0) break
                    val rahmen = text.substring(0, i); text = text.substring(i + ENDE.length)
                    nachricht(rahmen, s.token).also { gesehen += (it?.optInt("infoType")?.toString() ?: "?:" + rahmen.take(60)) }?.let { j ->
                        if (j.optInt("infoType") == 20001) {
                            val d = j.optJSONObject("data") ?: return null
                            return Stand(d.optString("mode"), d.optInt("elec", -1).takeIf { it in 0..100 },
                                when (d.optString("workNoisy")) { "quiet" -> "Leise"; "auto" -> "Normal"; "strong" -> "Leistungsstark"; "" -> null; else -> d.optString("workNoisy") })
                        }
                    }
                }
                puffer.reset(); puffer.write(text.toByteArray())
            }
            Logbuch.netz("Proscenic: Stand fehlt", "Gateway $ip:$port, Rahmen: ${gesehen.joinToString().ifBlank { "keine" }}".take(400))
        }
        return null
    }

    /** Ein Rahmen vom Gateway: Klartext-JSON oder `{"encrypt":…,"data":"<Base64>"}` (AES-ECB, Schlüssel = Token). */
    private fun nachricht(rahmen: String, token: String): JSONObject? {
        val j = runCatching { JSONObject(rahmen.trim()) }.getOrNull() ?: return null
        if (j.has("infoType") && j.opt("data") is JSONObject) return j
        val daten = j.optString("data").ifBlank { return null }
        return runCatching {
            val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(token.toByteArray(Charsets.UTF_8), "AES"))
            JSONObject(String(c.doFinal(Base64.decode(daten, Base64.DEFAULT)), Charsets.UTF_8).trimEnd('\u0000'))
        }.getOrNull()
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
