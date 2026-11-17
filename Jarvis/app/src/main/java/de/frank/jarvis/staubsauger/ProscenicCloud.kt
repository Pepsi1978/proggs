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
    internal const val HOST = "https://mobile.proscenic.com.de"
    internal const val ENDE = "#\t#"
    internal val CLIENT = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).build()

    fun konfiguriert(context: Context): Boolean {
        val s = SecureSettings.get(context)
        return s.proscenicMail.isNotBlank() && s.proscenicPasswort.isNotBlank()
    }

    /** Nicht weiter versuchen: falsche Anmeldedaten oder kein Roboter im Konto. */
    open class Abgewiesen(text: String) : Exception(text)

    /** Der Roboter selbst hat gerade keine Verbindung zur Cloud (meist WLAN-Aussetzer); Anmeldung und Cloud sind in Ordnung. */
    class Offline : Abgewiesen(OFFLINE_TEXT)

    /** Rat bei einem Roboter ohne Cloud-Verbindung; gemeinsam für [OFFLINE_TEXT] und die Verbindungsprüfung. */
    private const val OFFLINE_RAT = "Meist ist es ein kurzer WLAN-Aussetzer, und er meldet sich nach ein, zwei Minuten von selbst wieder. " +
        "Bleibt er länger weg, ist er vermutlich ausgeschaltet, der Akku leer oder das WLAN an der Station zu schwach."

    const val OFFLINE_TEXT = "Der M7 Pro ist gerade nicht mit der Proscenic-Cloud verbunden; Anmeldung und Jarvis sind in Ordnung. " + OFFLINE_RAT

    private const val ANMELDUNG_ABGELEHNT = "Die Proscenic-Cloud lehnt die Anmeldung von Jarvis ab; trag E-Mail und Passwort unter Konten, Proscenic neu ein."

    /** Hinweis nach einer Neu-Anmeldung: Proscenic erlaubt je Konto nur eine aktive Anmeldung, die Proscenic-App ist damit abgemeldet. */
    private fun abmeldeHinweis(context: Context, neuAngemeldet: Boolean) = if (neuAngemeldet && !geteilt(context))
        "Hinweis: Weil du dich in der Proscenic-App angemeldet hattest, musste ich mich neu anmelden; die App ist damit abgemeldet. " +
            "Teile den Roboter in der App mit einem zweiten Konto (Gerät teilen) und trag dieses in Jarvis unter Konten ein, dann geht beides parallel." else ""

    /** So lange wartet ein Befehl auf einen Roboter, der gerade kurz aus dem WLAN gefallen ist (er springt oft nach Sekunden zurück). */
    private const val WARTEN_MS = 20_000L

    /** Kurzer Client nur für die Online-Abfrage, damit das Warten auf den Roboter berechenbar bleibt. */
    private val KURZ = CLIENT.newBuilder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS).build()

    /**
     * Ist der Roboter gerade mit der Cloud verbunden? null = Cloud nicht erreichbar oder unlesbare Antwort.
     * Meldet die Cloud ein abgelaufenes Token (HTTP 401/403 oder Code 102), wirft es [TokenUngueltig], damit neu angemeldet wird.
     */
    internal fun online(s: Sitzung): Boolean? = onlineMitGrund(s).online

    /** Antwort der Online-Abfrage: [online] wie bei [online], [grund] erklärt bei null, woran es lag. */
    internal class OnlineAntwort(val online: Boolean?, val grund: String = "")

    /** Wie [online], nennt aber bei null den Grund (Zeitüberschreitung, Server nicht gefunden, Serverfehler …). */
    internal fun onlineMitGrund(s: Sitzung): OnlineAntwort {
        val j = try {
            KURZ.newCall(Request.Builder().url("$HOST/appInit/robotOnLine/${s.sn}").header("token", s.token).get().build()).execute().use { a ->
                if (a.code == 401 || a.code == 403) throw TokenUngueltig()
                if (!a.isSuccessful) return OnlineAntwort(null, "sie hat mit einem Serverfehler geantwortet (HTTP ${a.code})")
                JSONObject(a.body?.string().orEmpty())
            }
        } catch (e: TokenUngueltig) { throw e } catch (e: Exception) { return OnlineAntwort(null, netzGrund(e)) }
        if (j.optInt("code", -1) == 102) throw TokenUngueltig()
        if (j.optInt("code", -1) != 0 || j.opt("data") !is Boolean)
            return OnlineAntwort(null, "sie hat unerwartet geantwortet (Code ${j.optInt("code", -1)})")
        return OnlineAntwort(j.optBoolean("data").also { SaugerStand.erreichbar = it })
    }

    /** Übersetzt einen Netzfehler in einen kurzen, vorlesbaren Grund. */
    internal fun netzGrund(e: Throwable): String = when {
        e is java.net.UnknownHostException -> "das Handy hat den Proscenic-Server nicht gefunden (Namensauflösung gescheitert)"
        e is java.io.InterruptedIOException -> "sie hat nicht rechtzeitig geantwortet (Zeitüberschreitung)"
        e is java.net.ConnectException -> "die Verbindung zum Proscenic-Server kam nicht zustande (${e.message?.take(80) ?: "ohne Angabe"})"
        e is javax.net.ssl.SSLException -> "die verschlüsselte Verbindung zum Proscenic-Server ist gescheitert"
        e is org.json.JSONException -> "sie hat eine unlesbare Antwort geschickt"
        e.message?.startsWith("HTTP ") == true -> "sie hat mit einem Serverfehler geantwortet (${e.message})"
        else -> "Netzfehler ${e.message?.take(80) ?: e.javaClass.simpleName}"
    }

    /** Hat die Cloud geantwortet, nur mit einem Fehler (HTTP-Fehler, unlesbare Antwort)? Dann ist sie gestört, nicht unerreichbar. */
    private fun gestoert(e: Throwable) = e is org.json.JSONException || e.message?.startsWith("HTTP ") == true || e is AntwortUnlesbar

    /** Die Cloud hat geantwortet, aber nichts Verwertbares (etwa eine Wartungsseite statt JSON). */
    internal class AntwortUnlesbar : java.io.IOException("unlesbare Antwort der Proscenic-Cloud")

    /**
     * Hat das Handy ein Netz mit funktionierendem Internet (von Android geprüft)? Ein VPN wie der WireGuard-Tunnel zählt nicht,
     * nur ein darunterliegendes Netz. null = ließ sich nicht prüfen.
     */
    internal fun handyHatInternet(context: Context): Boolean? = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        @Suppress("DEPRECATION")
        cm.allNetworks.any { n ->
            val c = cm.getNetworkCapabilities(n) ?: return@any false
            !c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) &&
                c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }.getOrNull()

    /**
     * Ergebnis einer Verbindungsprüfung: [online] true/false/null (unbekannt), [text] vorlesbar mit Ursache, [kurz] die Ursache in
     * Stichworten. [endgueltig] = ein Neuversuch ändert nichts (Anmeldung abgelehnt oder gescheitert).
     */
    data class Verbindung(val online: Boolean?, val text: String, val kurz: String, val endgueltig: Boolean = false)

    /** So lange wartet die Verbindungsprüfung vor dem einen Neuversuch, damit kurze Aussetzer nicht als Ausfall gemeldet werden. */
    private const val NEUVERSUCH_MS = 6_000L

    /**
     * Prüft, ob der Roboter mit der Cloud verbunden ist, und nennt sonst die Ursache: Handy ohne Internet, Cloud nicht erreichbar
     * (mit Grund), Anmeldung abgelehnt oder Roboter offline. Klappt der erste Versuch nicht, prüft sie nach kurzer Pause einmal neu.
     */
    suspend fun verbindungPruefen(context: Context): Verbindung = withContext(Dispatchers.IO) {
        val c = context.applicationContext
        var neuAngemeldet = false
        val erst = einmalPruefen(c) { neuAngemeldet = true }
        // Kein Neuversuch, wenn er nichts ändern kann: verbunden, oder die Anmeldung selbst scheitert (sonst zweiter Login-Versuch).
        val ergebnis = if (erst.online == true || erst.endgueltig) erst else {
            Logbuch.netz("Proscenic: Erreichbarkeit, Neuversuch", erst.kurz)
            delay(NEUVERSUCH_MS)
            val zweit = einmalPruefen(c) { neuAngemeldet = true }
            when {
                zweit.online == true -> zweit.copy(text = "Der M7 Pro ist erreichbar und mit der Proscenic-Cloud verbunden. " +
                    "Beim ersten Versuch gab es kurz einen Aussetzer (${erst.kurz}), der Neuversuch hat geklappt.")
                zweit.kurz == erst.kurz -> zweit.copy(text = zweit.text + " Ich habe nach ${NEUVERSUCH_MS / 1000} Sekunden noch einmal geprüft, mit demselben Ergebnis.")
                else -> zweit.copy(text = zweit.text + " Beim ersten Versuch ${NEUVERSUCH_MS / 1000} Sekunden vorher lag es noch an etwas anderem: ${erst.kurz}.")
            }
        }
        val hinweis = abmeldeHinweis(c, neuAngemeldet)
        if (hinweis.isEmpty()) ergebnis else ergebnis.copy(text = ergebnis.text + " " + hinweis)
    }

    /** Ein Prüfdurchgang; [neuAngemeldet] wird gerufen, wenn dafür eine Neu-Anmeldung nötig war. */
    private inline fun einmalPruefen(context: Context, neuAngemeldet: () -> Unit): Verbindung {
        val a = try {
            try { onlineMitGrund(sitzung(context, neu = false)) } catch (e: TokenUngueltig) {
                neuAngemeldet()
                onlineMitGrund(sitzung(context, neu = true))
            }
        } catch (e: TokenUngueltig) {
            return Verbindung(null, ANMELDUNG_ABGELEHNT, "Anmeldung abgelehnt", endgueltig = true)
        } catch (e: Abgewiesen) {
            return Verbindung(null, e.message.orEmpty(), "Anmeldung gescheitert", endgueltig = true)
        } catch (e: Exception) {
            OnlineAntwort(null, netzGrund(e))
        }
        return when (a.online) {
            true -> Verbindung(true, "Der M7 Pro ist erreichbar und mit der Proscenic-Cloud verbunden.", "verbunden")
            false -> Verbindung(false, "Ursache: Roboter offline. Die Proscenic-Cloud ist erreichbar, aber der M7 Pro selbst ist gerade nicht mit ihr verbunden. " +
                OFFLINE_RAT, "Roboter offline")
            null -> when (handyHatInternet(context)) {
                false -> Verbindung(null, "Ursache: Das Handy hat gerade kein funktionierendes Internet, deshalb erreicht Jarvis die Proscenic-Cloud nicht.", "Handy ohne Internet")
                true -> Verbindung(null, "Ursache: Proscenic-Cloud nicht erreichbar oder gestört. Das Internet des Handys funktioniert, aber ${a.grund}. " +
                    "Ob der Roboter selbst verbunden ist, lässt sich so nicht sagen.", "Proscenic-Cloud: ${a.grund}")
                null -> Verbindung(null, "Ursache: Die Proscenic-Cloud antwortet nicht verwertbar, ${a.grund}. Ob das Internet des Handys funktioniert, " +
                    "ließ sich nicht prüfen. Ob der Roboter selbst verbunden ist, lässt sich so nicht sagen.", "Proscenic-Cloud: ${a.grund}")
            }
        }
    }

    /**
     * Wartet bis zu [WARTEN_MS], bis der Roboter wieder online ist (abbrechbar). true = online, false = weiterhin offline,
     * null = Stand unbekannt (Cloud antwortet nicht verwertbar); dann versucht der Aufrufer es einfach.
     */
    internal suspend fun aufRoboterWarten(s: Sitzung): Boolean? {
        val bis = System.currentTimeMillis() + WARTEN_MS
        while (true) {
            val o = online(s)
            if (o != false) return o
            if (System.currentTimeMillis() >= bis) return false
            delay(3000)
        }
    }

    /** Wie [aufRoboterWarten], wirft aber [Offline], wenn der Roboter auch nach dem Warten nicht verbunden ist. */
    internal suspend fun roboterBereit(s: Sitzung) { if (aufRoboterWarten(s) == false) throw Offline() }

    internal data class Sitzung(val token: String, val sn: String, val mail: String)

    private fun prefs(c: Context) = c.getSharedPreferences("staubsauger_cloud", Context.MODE_PRIVATE)

    /** Nutzt Jarvis ein Zweitkonto, mit dem der Roboter geteilt ist? Dann verdrängt seine Anmeldung die Proscenic-App nicht. */
    private fun geteilt(c: Context) = prefs(c).getBoolean("geteilt", false)

    suspend fun ausfuehren(context: Context, aktion: ProscenicSteuerung.Aktion): SaugerAusgang = withContext(Dispatchers.IO) {
        try {
            var s = sitzung(context, neu = false)
            var neuAngemeldet = false
            // Erst schnell fragen, ob der Roboter überhaupt online ist: Sonst liefe die Gateway-Abfrage 15 Sekunden ins Leere
            // und der Befehl bekäme nur „failure“. Kurze WLAN-Aussetzer überbrückt Jarvis durch Warten.
            try { roboterBereit(s) } catch (e: TokenUngueltig) { s = sitzung(context, neu = true); neuAngemeldet = true; roboterBereit(s) }
            val vorher = try { stand(s) } catch (e: TokenUngueltig) {
                s = sitzung(context, neu = true); neuAngemeldet = true
                runCatching { stand(s) }.getOrNull()
            } catch (e: Exception) { null }
            val antwort = try {
                befehl(s, aktion, vorher)
            } catch (e: TokenUngueltig) {
                if (neuAngemeldet) throw Abgewiesen(ANMELDUNG_ABGELEHNT)
                s = sitzung(context, neu = true); neuAngemeldet = true
                befehl(s, aktion, vorher)
            }
            // Kurz warten und den neuen Stand lesen, damit Frank hört, ob der Roboter reagiert hat.
            val nachher = if (aktion == ProscenicSteuerung.Aktion.STATUS) vorher else {
                var st: Stand? = null
                // Höchstens zwei kurze Gateway-Abfragen: jede verdrängt kurz die Verbindung der Proscenic-App.
                for (i in 0 until 2) { delay(4000); st = runCatching { stand(s) }.getOrNull(); if (st != null && st.mode != vorher?.mode) break }
                st
            }
            nachher?.let { SaugerStand.merken(context, it.akku, it.zustand, it.saugstufe) }
            val standText = nachher?.let { listOfNotNull("Zustand: ${it.zustand}.", it.akku?.let { a -> "Akku $a Prozent." }, it.saugstufe?.let { m -> "Saugstufe: $m." }).joinToString(" ") }
                ?: if (aktion == ProscenicSteuerung.Aktion.STATUS) "" else "Den neuen Stand konnte ich nicht abrufen."
            if (aktion == ProscenicSteuerung.Aktion.STATUS && nachher == null)
                return@withContext if (runCatching { online(s) }.getOrNull() == false) SaugerAusgang(OFFLINE_TEXT, fehler = true)
                else SaugerAusgang("Der M7 Pro ist online, hat seinen Stand aber gerade nicht geschickt. Versuch es gleich noch einmal.", fehler = true)
            // Proscenic erlaubt je Konto nur eine aktive Anmeldung: Musste Jarvis sich neu anmelden, ist die Proscenic-App jetzt abgemeldet.
            val hinweis = abmeldeHinweis(context, neuAngemeldet)
            SaugerAusgang(listOf(antwort, standText, hinweis).filter { it.isNotBlank() }.joinToString(" "))
        } catch (e: Abgewiesen) {
            SaugerAusgang(e.message.orEmpty(), fehler = true)
        } catch (e: TokenUngueltig) {
            SaugerAusgang(ANMELDUNG_ABGELEHNT, fehler = true)
        } catch (e: java.io.IOException) {
            // Eine Zeitüberschreitung kann nach dem Absenden kommen: Dann ist offen, ob der Roboter den Befehl schon ausführt.
            val folge = if (e is java.io.InterruptedIOException)
                "Ob der Befehl angekommen ist, ist unklar; frag erst den Stand ab, bevor du ihn wiederholst."
            else "Der Befehl kam nicht an, versuch es gleich noch einmal."
            val ursache = when {
                gestoert(e) -> "Ursache: Proscenic-Cloud gestört, ${netzGrund(e)}."
                handyHatInternet(context) == false -> "Ursache: Das Handy hat gerade kein funktionierendes Internet."
                handyHatInternet(context) == true -> "Ursache: Proscenic-Cloud nicht erreichbar, ${netzGrund(e)}; das Internet des Handys funktioniert."
                else -> "Ursache: Proscenic-Cloud nicht erreichbar, ${netzGrund(e)}."
            }
            SaugerAusgang("$ursache $folge", fehler = true)
        }
        catch (e: org.json.JSONException) {
            SaugerAusgang("Die Proscenic-Cloud hat unerwartet geantwortet (${e.message}).", fehler = true)
        }
    }

    // ---- Anmeldung ----

    internal class TokenUngueltig : Exception()

    internal fun sitzung(context: Context, neu: Boolean): Sitzung {
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
        // Wartungs- oder Fehlerseite statt JSON: Die Cloud ist gestört, die Anmeldedaten sind nicht schuld.
        if (login.has("roh")) throw AntwortUnlesbar()
        val daten = login.optJSONObject("data")
        val token = daten?.optString("token").orEmpty()
        Logbuch.netz("Proscenic: Anmeldung", "Antwort ohne Token: " + JSONObject(login.toString()).also { it.optJSONObject("data")?.remove("token") }.toString().take(400))
        if (token.isBlank())
            throw Abgewiesen("Die Anmeldung bei Proscenic hat nicht geklappt: Prüfe E-Mail und Passwort in Jarvis unter Konten, Proscenic.")
        val geraete = post("/user/getEquips/$mail", token, FormBody.Builder().add("username", mail).build())
        Logbuch.netz("Proscenic: Geräteliste", geraete.toString().take(900))
        val liste = geraete.optJSONObject("data")?.optJSONArray("content")
        val roboter = (0 until (liste?.length() ?: 0)).mapNotNull { liste?.optJSONObject(it) }
            .firstOrNull { it.optString("typeName").equals("CleanRobot", true) || it.optString("code").contains("M7", true) }
        val sn = roboter?.optString("sn").orEmpty()
        if (sn.isBlank()) throw Abgewiesen(
            "Im Proscenic-Konto $mail ist noch kein Saugroboter sichtbar. Ist es ein Zweitkonto, muss es die Freigabe erst annehmen: " +
                "Melde dich in der Proscenic-App einmal mit diesem Konto an, nimm die Einladung unter Nachrichten an und prüfe, ob der M7 Pro in der Geräteliste steht. " +
                "Danach meldest du dich in der App wieder mit deinem Hauptkonto an.")
        p.edit().putString("mail", mail).putString("token", token).putString("sn", sn).putBoolean("geteilt", roboter?.optBoolean("shared") == true).apply()
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
            ProscenicSteuerung.Aktion.WISCHEN -> {
                senden(s, "/instructions/${s.sn}/21020$user", form("ctrlCode" to "3024"))
                "Y-förmiges Wischen gestartet."
            }
        }
    }

    /**
     * Schickt einen Befehl genau einmal (kein automatisches Wiederholen: Start, Pause/Weiter oder Ansage liefen sonst doppelt,
     * wenn die Cloud „failure“ meldet, der Roboter den Befehl aber doch ausgeführt hat). Auf einen kurz weggefallenen Roboter
     * wartet der Aufrufer vorher mit [roboterBereit]. Lehnt die Cloud ab, klärt eine Online-Abfrage die Ursache.
     */
    internal fun senden(s: Sitzung, pfad: String, body: RequestBody) {
        val a = post(pfad, s.token, body)
        val code = if (a.has("roh") || !a.has("code")) null else a.optInt("code")
        Logbuch.netz("Proscenic: Befehl", "${pfad.substringBefore('?')} → Code $code ${a.optString("msg")}".take(300))
        if (code == 102 || code == 401 || code == 403) throw TokenUngueltig()
        if (code == 0) { SaugerStand.erreichbar = true; return }
        if (code == null) throw Abgewiesen("Die Proscenic-Cloud hat unerwartet geantwortet; der Befehl kam vermutlich nicht an. Versuch es gleich noch einmal.")
        if (online(s) == false) throw Offline()
        throw Abgewiesen("Die Proscenic-Cloud hat den Befehl abgelehnt (Code $code: ${a.optString("msg").ifBlank { "ohne Grund" }}).")
    }

    internal fun post(pfad: String, token: String?, body: RequestBody, ios: Boolean = false): JSONObject {
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
    internal fun nachricht(rahmen: String, token: String): JSONObject? {
        val j = runCatching { JSONObject(rahmen.trim()) }.getOrNull() ?: return null
        if (j.has("infoType") && j.opt("data") is JSONObject) return j
        // Unverschlüsselt verpackt: {"encrypt":0,"data":{"infoType":…,"data":{…}}}
        (j.opt("data") as? JSONObject)?.takeIf { it.has("infoType") }?.let { return it }
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
