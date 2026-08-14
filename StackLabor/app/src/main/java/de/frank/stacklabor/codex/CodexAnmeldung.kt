package de.frank.stacklabor.codex

// PLAN (F-17): Geräte-Flow gegen das ChatGPT-Konto, Mechanik 1:1 aus PerfectMoment/CodexAuthManager.
// 1. starteAnmeldung() holt Benutzercode + device_auth_id und merkt sich den Lauf; die Oberfläche
//    (B-11) zeigt Code und Adresse und öffnet den Browser selbst — hier kein UI-Code.
// 2. warteAufBestaetigung() fragt im Takt nach, tauscht den Anmeldecode gegen die Schlüssel und
//    legt sie verschlüsselt ab (EncryptedSharedPreferences, Speichername `codex_oauth`).
// 3. gueltigerZugriffsschluessel() erneuert automatisch, sobald der Ablauf näher als 2 Minuten ist.
// 4. istAngemeldet/konto sind Flows, damit B-10 ohne Nachfragen aktuell bleibt.
// Geändert gegenüber der Vorlage: Anmeldung in zwei Schritte getrennt (statt einer login()-Funktion
// mit Activity-Abhängigkeit), Zustand als Flow statt als Getter, deutsche Bezeichner.

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/** Was B-11 anzeigt: der Benutzercode und die Adresse, die im Browser geöffnet wird. */
data class GeraeteCode(val userCode: String, val verifizierungsUrl: String)

/** Ergebnis einer bestätigten Anmeldung; [email] steht danach in B-10. */
data class AnmeldeErgebnis(val email: String?, val kontoKennung: String?)

internal data class HttpErgebnis(val code: Int, val koerper: String)

/** Wie auf eine Antwort der Geräteabfrage reagiert wird. */
internal enum class AbfrageSchritt { UEBERNEHMEN, WEITER_WARTEN, ABBRECHEN }

internal fun abfrageSchritt(statusCode: Int): AbfrageSchritt = when {
    statusCode == 200 -> AbfrageSchritt.UEBERNEHMEN
    statusCode == 403 || statusCode == 404 || statusCode == 429 || statusCode >= 500 -> AbfrageSchritt.WEITER_WARTEN
    else -> AbfrageSchritt.ABBRECHEN
}

internal fun abfrageTakt(wert: Any?): Int =
    (wert?.toString()?.toIntOrNull() ?: CodexAnmeldung.VORGABE_TAKT_SEKUNDEN)
        .coerceIn(CodexAnmeldung.MIN_TAKT_SEKUNDEN, CodexAnmeldung.MAX_TAKT_SEKUNDEN)

/**
 * Übersetzt einen HTTP-Fehler in die drei Fehlerklassen aus [CodexFehlerArt].
 * Vorübergehende Störungen (Zeitüberschreitung, Serverfehler 5xx) werden als wiederholbar
 * gekennzeichnet, damit ein zweiter Versuch die Auswertung noch retten kann.
 */
internal fun klassifiziereHttpFehler(code: Int, koerper: String) {
    if (code in 200..299) return
    val kleingeschrieben = koerper.lowercase()
    val meldung = runCatching {
        val json = JSONObject(koerper)
        json.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
            ?: json.optString("message").takeIf(String::isNotBlank)
    }.getOrNull() ?: koerper.take(240)
    when {
        code == 429 || kleingeschrieben.contains("insufficient_quota") || kleingeschrieben.contains("rate_limit") ->
            throw CodexFehler(
                CodexFehlerArt.KONTINGENT,
                "Dein ChatGPT-Kontingent ist gerade ausgeschöpft. Bitte später erneut versuchen.",
            )
        code == 401 || code == 403 || kleingeschrieben.contains("invalid_grant") ||
            kleingeschrieben.contains("refresh_token_reused") ->
            throw CodexFehler(
                CodexFehlerArt.NEU_ANMELDEN,
                "Die Anmeldung bei Codex ist abgelaufen oder nicht mehr zugelassen. Bitte erneut anmelden. $meldung",
            )
        code == 408 || code in 500..599 -> throw CodexFehler(
            CodexFehlerArt.NETZ,
            "Codex ist gerade überlastet oder nicht erreichbar (Fehler $code). Bitte kurz erneut versuchen.",
            wiederholbar = true,
        )
        else -> throw CodexFehler(CodexFehlerArt.NETZ, "Codex-Fehler $code: $meldung")
    }
}

internal fun netzFehler(meldung: String, ursache: Throwable? = null): CodexFehler =
    CodexFehler(CodexFehlerArt.NETZ, meldung, ursache)

/**
 * Hält die Anmeldung am ChatGPT-Konto (F-17) und liefert dem [CodexDienst] gültige Schlüssel.
 *
 * Alle Netzarbeit läuft auf [Dispatchers.IO]. Es steht kein Zugangsschlüssel im Quelltext:
 * gearbeitet wird ausschliesslich mit der öffentlichen Client-Kennung des Geräte-Flows.
 */
class CodexAnmeldung(context: Context) {

    private val appContext = context.applicationContext
    private val verbindungen = appContext.getSystemService(ConnectivityManager::class.java)
    private val erneuerungsSperre = Mutex()
    private val anmeldeSperre = Mutex()

    @Volatile
    private var laufenderVersuch: LaufenderVersuch? = null

    private val speicher by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val hauptschluessel = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            SPEICHER_NAME,
            hauptschluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val angemeldetZustand = MutableStateFlow(false)
    private val kontoZustand = MutableStateFlow<String?>(null)

    /** true, sobald ein Zugangsschlüssel abgelegt ist (B-10 schaltet die Auswerten-Knöpfe frei). */
    val istAngemeldet: Flow<Boolean> = angemeldetZustand.asStateFlow()

    /** Die E-Mail des angemeldeten Kontos, sonst null. */
    val konto: Flow<String?> = kontoZustand.asStateFlow()

    init {
        uebernehmeZustand()
    }

    /**
     * Schritt 1 des Geräte-Flows: holt Benutzercode und Verifizierungsadresse.
     * Der Lauf wird gemerkt, [warteAufBestaetigung] setzt darauf auf.
     */
    suspend fun starteAnmeldung(): GeraeteCode = anmeldeSperre.withLock {
        withContext(Dispatchers.IO) {
            val start = try {
                sendeJson(GERAETECODE_URL, JSONObject().put("client_id", CLIENT_ID).toString())
            } catch (fehler: IOException) {
                throw netzFehler("Der Gerätecode konnte nicht geholt werden.", fehler)
            }
            klassifiziereHttpFehler(start.code, start.koerper)
            val json = leseJson(start.koerper, "Codex hat keinen gültigen Gerätecode geliefert.")
            val userCode = json.optString("user_code").trim()
            val geraeteAuftrag = json.optString("device_auth_id").trim()
            if (userCode.isEmpty() || geraeteAuftrag.isEmpty()) {
                throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Codex hat keinen vollständigen Gerätecode geliefert.")
            }
            laufenderVersuch = LaufenderVersuch(
                userCode = userCode,
                geraeteAuftrag = geraeteAuftrag,
                taktSekunden = abfrageTakt(json.opt("interval")),
                laeuftAbUm = System.currentTimeMillis() + CODE_LEBENSDAUER_MS,
            )
            GeraeteCode(userCode, VERIFIZIERUNGS_URL)
        }
    }

    /**
     * Schritt 2 des Geräte-Flows: fragt im Takt nach, bis der Benutzer die Seite bestätigt hat,
     * und legt Zugangs- und Erneuerungsschlüssel verschlüsselt ab.
     */
    suspend fun warteAufBestaetigung(): AnmeldeErgebnis = anmeldeSperre.withLock {
        val versuch = laufenderVersuch
            ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Es läuft keine Anmeldung. Bitte einen neuen Code holen.")
        withContext<AnmeldeErgebnis>(Dispatchers.IO) {
            var takt = versuch.taktSekunden
            while (System.currentTimeMillis() < versuch.laeuftAbUm) {
                delay(takt * 1_000L)
                val antwort = try {
                    sendeJson(
                        GERAETE_TOKEN_URL,
                        JSONObject()
                            .put("device_auth_id", versuch.geraeteAuftrag)
                            .put("user_code", versuch.userCode)
                            .toString(),
                    )
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    // Netzschluckauf während des Wartens beendet die Anmeldung nicht — nur langsamer fragen.
                    if (fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) throw fehler
                    takt = (takt + TAKT_ZUSCHLAG_SEKUNDEN).coerceIn(MIN_TAKT_SEKUNDEN, MAX_TAKT_SEKUNDEN)
                    continue
                }

                when (abfrageSchritt(antwort.code)) {
                    AbfrageSchritt.WEITER_WARTEN -> {
                        if (antwort.code == 429 || antwort.koerper.contains("slow_down", ignoreCase = true)) {
                            takt = (takt + BREMSE_SEKUNDEN).coerceIn(MIN_TAKT_SEKUNDEN, MAX_TAKT_SEKUNDEN)
                        }
                    }
                    AbfrageSchritt.ABBRECHEN -> klassifiziereHttpFehler(antwort.code, antwort.koerper)
                    AbfrageSchritt.UEBERNEHMEN -> {
                        val bestaetigung = leseJson(antwort.koerper, "Codex hat die Geräteanmeldung ungültig bestätigt.")
                        val code = bestaetigung.optString("authorization_code").trim()
                        val pruefwert = bestaetigung.optString("code_verifier").trim()
                        if (code.isEmpty() || pruefwert.isEmpty()) {
                            throw CodexFehler(
                                CodexFehlerArt.NEU_ANMELDEN,
                                "Codex hat den Gerätecode bestätigt, aber keinen vollständigen Anmeldecode geliefert.",
                            )
                        }
                        warteAufNetz()
                        laufenderVersuch = null
                        return@withContext tauscheCode(code, pruefwert)
                    }
                }
            }
            laufenderVersuch = null
            throw CodexFehler(
                CodexFehlerArt.NEU_ANMELDEN,
                "Der Gerätecode ist abgelaufen. Bitte einen neuen Code holen.",
            )
        }
    }

    /** Löscht alle Schlüssel. Gespeicherte Bewertungen bleiben unberührt (F-17). */
    fun abmelden() {
        laufenderVersuch = null
        speicher.edit().clear().apply()
        uebernehmeZustand()
    }

    /**
     * Liefert einen gültigen Zugangsschlüssel und erneuert ihn selbstständig, sobald weniger als
     * zwei Minuten Restlaufzeit bleiben.
     */
    internal suspend fun gueltigerZugriffsschluessel(): String {
        val vorhanden = speicher.getString(SCHLUESSEL_ZUGANG, null)
            ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Bitte zuerst bei Codex anmelden.")
        if (!erneuerungNoetig()) return vorhanden

        return erneuerungsSperre.withLock {
            val nachSperre = speicher.getString(SCHLUESSEL_ZUGANG, null)
                ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Bitte zuerst bei Codex anmelden.")
            if (!erneuerungNoetig()) return@withLock nachSperre
            val erneuerung = speicher.getString(SCHLUESSEL_ERNEUERUNG, null)
                ?: throw CodexFehler(
                    CodexFehlerArt.NEU_ANMELDEN,
                    "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.",
                )
            val antwort = withContext(Dispatchers.IO) {
                sendeFormular(
                    TOKEN_URL,
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", erneuerung)
                        .add("client_id", CLIENT_ID)
                        .build(),
                )
            }
            klassifiziereHttpFehler(antwort.code, antwort.koerper)
            val json = leseJson(antwort.koerper, "Codex hat beim Erneuern keine gültigen Schlüssel geliefert.")
            val neuerZugang = json.optString("access_token").takeIf(String::isNotBlank)
                ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Codex hat keinen neuen Zugangsschlüssel geliefert.")
            legeAb(json, neuerZugang, nurVorhandeneErsetzen = true)
            neuerZugang
        }
    }

    /** Die ChatGPT-Konto-Kennung, die jede Anfrage im Kopf mitführen muss. */
    internal fun kontoKennungFuer(zugangsschluessel: String): String {
        val ausSchluessel = jwtKontoKennung(zugangsschluessel)
        if (ausSchluessel != null && ausSchluessel != speicher.getString(SCHLUESSEL_KONTO, null)) {
            speicher.edit().putString(SCHLUESSEL_KONTO, ausSchluessel).apply()
        }
        return ausSchluessel
            ?: speicher.getString(SCHLUESSEL_KONTO, null)
            ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Im Codex-Schlüssel fehlt die ChatGPT-Konto-Kennung.")
    }

    /**
     * Wiederholt einen Aufruf, wenn der Name noch nicht auflösbar war — typisch direkt nach dem
     * Wechsel zwischen WLAN und Mobilfunk.
     */
    internal suspend fun <T> mitNamensWiederholung(block: suspend () -> T): T {
        var letzterFehler: UnknownHostException? = null
        repeat(DNS_PAUSEN_MS.size + 1) { versuch ->
            try {
                return block()
            } catch (fehler: UnknownHostException) {
                letzterFehler = fehler
                if (versuch < DNS_PAUSEN_MS.size) {
                    warteAufNetz(DNS_WARTEZEIT_MS)
                    delay(DNS_PAUSEN_MS[versuch])
                }
            }
        }
        throw netzFehler(
            "Codex ist über die aktuelle Verbindung nicht erreichbar. Bitte WLAN oder Mobilfunk prüfen.",
            letzterFehler,
        )
    }

    internal fun antwortClient(): OkHttpClient = ANTWORT_CLIENT

    private suspend fun tauscheCode(code: String, pruefwert: String): AnmeldeErgebnis {
        val antwort = sendeFormular(
            TOKEN_URL,
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", RUECKLEIT_URL)
                .add("code_verifier", pruefwert)
                .build(),
        )
        klassifiziereHttpFehler(antwort.code, antwort.koerper)
        val json = leseJson(antwort.koerper, "Codex hat keine gültigen Anmeldeschlüssel geliefert.")
        val zugang = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexFehler(CodexFehlerArt.NEU_ANMELDEN, "Codex hat keinen Zugangsschlüssel geliefert.")
        legeAb(json, zugang, nurVorhandeneErsetzen = false)
        return AnmeldeErgebnis(kontoZustand.value, speicher.getString(SCHLUESSEL_KONTO, null))
    }

    private fun legeAb(json: JSONObject, zugang: String, nurVorhandeneErsetzen: Boolean) {
        val erneuerung = json.optString("refresh_token").takeIf(String::isNotBlank)
        val ausweis = json.optString("id_token").takeIf(String::isNotBlank)
        val email = ausweis?.let(::jwtEmail) ?: jwtEmail(zugang)
        val kontoKennung = jwtKontoKennung(zugang) ?: ausweis?.let(::jwtKontoKennung)
        val stift = speicher.edit()
        stift.putString(SCHLUESSEL_ZUGANG, zugang)
        stift.putLong(SCHLUESSEL_ABLAUF, ablaufZeitpunkt(json))
        if (erneuerung != null) stift.putString(SCHLUESSEL_ERNEUERUNG, erneuerung)
        else if (!nurVorhandeneErsetzen) stift.remove(SCHLUESSEL_ERNEUERUNG)
        if (email != null) stift.putString(SCHLUESSEL_EMAIL, email)
        else if (!nurVorhandeneErsetzen) stift.remove(SCHLUESSEL_EMAIL)
        if (kontoKennung != null) stift.putString(SCHLUESSEL_KONTO, kontoKennung)
        else if (!nurVorhandeneErsetzen) stift.remove(SCHLUESSEL_KONTO)
        stift.apply()
        uebernehmeZustand()
    }

    private fun uebernehmeZustand() {
        angemeldetZustand.value = speicher.contains(SCHLUESSEL_ZUGANG)
        kontoZustand.value = speicher.getString(SCHLUESSEL_EMAIL, null)
    }

    private fun erneuerungNoetig(): Boolean =
        System.currentTimeMillis() >= speicher.getLong(SCHLUESSEL_ABLAUF, 0L) - ERNEUERUNGS_VORLAUF_MS

    private suspend fun sendeJson(url: String, koerper: String): HttpErgebnis {
        val anfrage = Request.Builder()
            .url(url)
            .post(koerper.toRequestBody(JSON_TYP))
            .header("Accept", "application/json")
            .build()
        return alsText(ANMELDE_CLIENT, anfrage)
    }

    private suspend fun sendeFormular(url: String, koerper: RequestBody): HttpErgebnis {
        val anfrage = Request.Builder().url(url).post(koerper).header("Accept", "application/json").build()
        return alsText(ANMELDE_CLIENT, anfrage)
    }

    private suspend fun alsText(client: OkHttpClient, anfrage: Request): HttpErgebnis =
        mitNamensWiederholung { client.newCall(anfrage).warteAufAntwort() }.use { antwort ->
            HttpErgebnis(antwort.code, antwort.body?.string().orEmpty())
        }

    private suspend fun warteAufNetz(wartezeitMs: Long = NETZ_WARTEZEIT_MS) {
        withTimeoutOrNull(wartezeitMs) {
            while (!hatGeprueftesNetz()) delay(NETZ_TAKT_MS)
        }
    }

    private fun hatGeprueftesNetz(): Boolean = try {
        val netz = verbindungen?.activeNetwork
        val faehigkeiten = netz?.let { verbindungen.getNetworkCapabilities(it) }
        faehigkeiten != null &&
            faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (fehler: SecurityException) {
        Log.w(MARKE, "Netzstatus nicht abfragbar; der HTTP-Aufruf entscheidet über die Verbindung.", fehler)
        true
    }

    private fun leseJson(wert: String, meldung: String): JSONObject = runCatching { JSONObject(wert) }
        .getOrElse { throw CodexFehler(CodexFehlerArt.NETZ, meldung, it) }

    private fun jwtKontoKennung(schluessel: String): String? =
        jwtAuthWert(schluessel, "chatgpt_account_id") ?: jwtAuthWert(schluessel, "account_id")

    private fun jwtEmail(schluessel: String): String? =
        jwtInhalt(schluessel)?.optString("email")?.takeIf(String::isNotBlank) ?: jwtAuthWert(schluessel, "email")

    private fun jwtAuthWert(schluessel: String, name: String): String? =
        jwtInhalt(schluessel)
            ?.optJSONObject("https://api.openai.com/auth")
            ?.optString(name)
            ?.takeIf(String::isNotBlank)

    private fun jwtInhalt(schluessel: String): JSONObject? = runCatching {
        val teil = schluessel.split('.')[1]
        val gefuellt = teil.padEnd((teil.length + 3) / 4 * 4, '=')
        JSONObject(String(Base64.getUrlDecoder().decode(gefuellt), Charsets.UTF_8))
    }.getOrNull()

    private fun ablaufZeitpunkt(json: JSONObject): Long =
        System.currentTimeMillis() + json.optLong("expires_in", VORGABE_LAUFZEIT_SEKUNDEN) * 1_000L

    private data class LaufenderVersuch(
        val userCode: String,
        val geraeteAuftrag: String,
        val taktSekunden: Int,
        val laeuftAbUm: Long,
    )

    companion object {
        // Öffentliche Client-Kennung des Codex-Geräte-Flows — kein Geheimnis, kein Zugangsschlüssel.
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val GERAETECODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val GERAETE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val VERIFIZIERUNGS_URL = "https://auth.openai.com/codex/device"
        private const val RUECKLEIT_URL = "https://auth.openai.com/deviceauth/callback"
        internal const val ANTWORT_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val SPEICHER_NAME = "codex_oauth"
        private const val MARKE = "CodexAnmeldung"

        internal val JSON_TYP = "application/json; charset=utf-8".toMediaType()

        private val ANMELDE_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()

        private val ANTWORT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()

        private const val ERNEUERUNGS_VORLAUF_MS = 120_000L
        private const val CODE_LEBENSDAUER_MS = 15 * 60_000L
        private const val NETZ_WARTEZEIT_MS = 30_000L
        private const val DNS_WARTEZEIT_MS = 10_000L
        private const val NETZ_TAKT_MS = 200L
        private const val TAKT_ZUSCHLAG_SEKUNDEN = 2
        private const val BREMSE_SEKUNDEN = 5
        private const val VORGABE_LAUFZEIT_SEKUNDEN = 3_600L
        internal const val VORGABE_TAKT_SEKUNDEN = 5
        internal const val MIN_TAKT_SEKUNDEN = 3
        internal const val MAX_TAKT_SEKUNDEN = 30
        private val DNS_PAUSEN_MS = longArrayOf(500L, 1_500L, 3_000L)

        private const val SCHLUESSEL_ZUGANG = "access_token"
        private const val SCHLUESSEL_ERNEUERUNG = "refresh_token"
        private const val SCHLUESSEL_ABLAUF = "expires_at"
        private const val SCHLUESSEL_KONTO = "account_id"
        private const val SCHLUESSEL_EMAIL = "email"
    }
}

/**
 * Hängt den OkHttp-Aufruf an die Koroutine: ein abgebrochener Aufruf meldet Abbruch statt eines
 * Netzfehlers, damit er weder wiederholt noch als Störung angezeigt wird.
 */
internal suspend fun Call.warteAufAntwort(): Response = suspendCancellableCoroutine { fortsetzung ->
    fortsetzung.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!fortsetzung.isActive) return
            if (call.isCanceled()) fortsetzung.cancel(e) else fortsetzung.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            fortsetzung.resume(response) { _, abgebrochene, _ -> abgebrochene.close() }
        }
    })
}
