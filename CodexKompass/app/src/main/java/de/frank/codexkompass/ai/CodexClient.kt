package de.frank.codexkompass.ai

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.codexkompass.network.awaitAntwort
import de.frank.codexkompass.observability.KompassLog
import java.io.BufferedReader
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Verbindung zu Codex — Anmeldung, Erneuerung des Zugangs und die eigentlichen Anfragen.
 *
 * Die Anmeldung läuft über einen Gerätecode: Die App holt einen kurzen Code, zeigt ihn an und
 * öffnet die Bestätigungsseite. Dort wird der Code eingetippt, danach holt sich die App den
 * Zugang ab. Ein Passwort tippt man nie in die App — das ist der Sinn dieses Verfahrens.
 *
 * Die Zugangsdaten liegen ausschließlich verschlüsselt.
 */
class CodexClient(context: Context) {

    private val appContext = context.applicationContext
    private val netzManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val erneuerungsSperre = Mutex()
    private val anmeldeSperre = Mutex()
    private val laufendeAnmeldung = AtomicReference<Job?>(null)
    private val laufendeAufrufe: MutableSet<Call> = Collections.newSetFromMap(ConcurrentHashMap())

    private val ablage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val hauptschluessel = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            ABLAGE_NAME,
            hauptschluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val email: String? get() = ablage.getString(SCHL_EMAIL, null)
    val kontoId: String? get() = ablage.getString(SCHL_KONTO, null)
    val istVerbunden: Boolean get() = ablage.contains(SCHL_ZUGANG)

    // --- Anmeldung -------------------------------------------------------------------------

    suspend fun melde(
        activity: ComponentActivity,
        beiCode: (GeraeteAnmeldung) -> Unit,
    ): AnmeldeErgebnis = anmeldeSperre.withLock {
        val meinJob = currentCoroutineContext().job
        laufendeAnmeldung.set(meinJob)
        try {
            withContext(Dispatchers.IO) { meldeIntern(activity, beiCode) }
        } catch (fehler: IOException) {
            throw CodexFehler(CodexFehlerArt.NETZ, "Die Anmeldung ist an einem Netzwerkfehler gescheitert.", fehler)
        } finally {
            laufendeAnmeldung.compareAndSet(meinJob, null)
        }
    }

    fun brichAnmeldungAb() {
        laufendeAnmeldung.getAndSet(null)?.cancel()
    }

    fun meldeAb() {
        ablage.edit().clear().apply()
        KompassLog.info("CodexClient", "meldeAb", "Zugang gelöscht")
    }

    private suspend fun meldeIntern(
        activity: ComponentActivity,
        beiCode: (GeraeteAnmeldung) -> Unit,
    ): AnmeldeErgebnis {
        val start = sendeJson(GERAETECODE_URL, JSONObject().put("client_id", CLIENT_ID).toString())
        pruefeHttp(start.code, start.rumpf)
        val startJson = leseJson(start.rumpf, "Es kam kein gültiger Gerätecode zurück.")
        val benutzerCode = startJson.optString("user_code").trim()
        val geraeteId = startJson.optString("device_auth_id").trim()
        if (benutzerCode.isEmpty() || geraeteId.isEmpty()) {
            throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Der Gerätecode kam unvollständig zurück.")
        }
        var abstandSekunden = abfrageAbstand(startJson.opt("interval"))
        val anmeldung = GeraeteAnmeldung(benutzerCode, BESTAETIGUNGS_URL)
        withContext(Dispatchers.Main) {
            beiCode(anmeldung)
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(anmeldung.bestaetigungsAdresse)))
        }
        KompassLog.info("CodexClient", "meldeIntern", "Gerätecode angezeigt", mapOf("gruppen" to geraeteCodeGruppen(benutzerCode).size))

        val laeuftAbUm = System.currentTimeMillis() + CODE_LEBENSDAUER_MS
        while (System.currentTimeMillis() < laeuftAbUm) {
            delay(abstandSekunden * 1_000L)
            val abfrage = try {
                sendeJson(
                    GERAETETOKEN_URL,
                    JSONObject()
                        .put("device_auth_id", geraeteId)
                        .put("user_code", benutzerCode)
                        .toString(),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                if (fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) throw fehler
                // Eine einzelne gescheiterte Abfrage bedeutet nicht, dass die Anmeldung
                // gescheitert ist. Langsamer weiterfragen statt aufgeben.
                abstandSekunden = (abstandSekunden + ABSTAND_ZUWACHS)
                    .coerceIn(MIN_ABSTAND_SEKUNDEN, MAX_ABSTAND_SEKUNDEN)
                continue
            }

            when {
                abfrage.code == 200 -> {
                    val bestaetigung = leseJson(abfrage.rumpf, "Die Bestätigung war unvollständig.")
                    val code = bestaetigung.optString("authorization_code").trim()
                    val pruefer = bestaetigung.optString("code_verifier").trim()
                    if (code.isEmpty() || pruefer.isEmpty()) {
                        throw CodexFehler(
                            CodexFehlerArt.ANMELDUNG,
                            "Der Gerätecode wurde bestätigt, aber der Anmeldecode kam unvollständig zurück.",
                        )
                    }
                    warteAufVordergrundUndNetz(activity)
                    return tauscheCode(code, pruefer)
                }
                abfrage.code == 403 || abfrage.code == 404 || abfrage.code == 429 || abfrage.code >= 500 -> {
                    // Noch nicht bestätigt — das ist der Normalfall beim Warten.
                    if (abfrage.code == 429 || abfrage.rumpf.contains("slow_down", ignoreCase = true)) {
                        abstandSekunden = (abstandSekunden + LANGSAMER_SEKUNDEN)
                            .coerceIn(MIN_ABSTAND_SEKUNDEN, MAX_ABSTAND_SEKUNDEN)
                    }
                }
                else -> pruefeHttp(abfrage.code, abfrage.rumpf)
            }
        }
        throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Der Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.")
    }

    private suspend fun tauscheCode(code: String, pruefer: String): AnmeldeErgebnis {
        val ergebnis = sendeFormular(
            TOKEN_URL,
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", WEITERLEITUNG_URL)
                .add("code_verifier", pruefer)
                .build(),
        )
        pruefeHttp(ergebnis.code, ergebnis.rumpf)
        val json = leseJson(ergebnis.rumpf, "Es kamen keine gültigen Zugangsdaten zurück.")
        val zugang = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Es kam kein Zugangstoken zurück.")
        val erneuerung = json.optString("refresh_token").takeIf(String::isNotBlank)
        val kennung = json.optString("id_token").takeIf(String::isNotBlank)
        val gefundeneEmail = kennung?.let(::jwtEmail) ?: jwtEmail(zugang)
        val gefundenesKonto = jwtKontoId(zugang) ?: kennung?.let(::jwtKontoId)

        ablage.edit()
            .putString(SCHL_ZUGANG, zugang)
            .putLong(SCHL_LAEUFT_AB, ablaufZeit(json))
            .apply {
                if (erneuerung == null) remove(SCHL_ERNEUERUNG) else putString(SCHL_ERNEUERUNG, erneuerung)
                if (gefundeneEmail == null) remove(SCHL_EMAIL) else putString(SCHL_EMAIL, gefundeneEmail)
                if (gefundenesKonto == null) remove(SCHL_KONTO) else putString(SCHL_KONTO, gefundenesKonto)
            }
            .apply()
        KompassLog.info("CodexClient", "tauscheCode", "Anmeldung erfolgreich")
        return AnmeldeErgebnis(gefundeneEmail)
    }

    private suspend fun gueltigerZugang(): String {
        val jetziger = ablage.getString(SCHL_ZUGANG, null)
            ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Bitte zuerst bei Codex anmelden.")
        if (!brauchtErneuerung()) return jetziger

        return erneuerungsSperre.withLock {
            val nachSperre = ablage.getString(SCHL_ZUGANG, null)
                ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Bitte zuerst bei Codex anmelden.")
            if (!brauchtErneuerung()) return@withLock nachSperre
            val erneuerung = ablage.getString(SCHL_ERNEUERUNG, null)
                ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
            val ergebnis = sendeFormular(
                TOKEN_URL,
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", erneuerung)
                    .add("client_id", CLIENT_ID)
                    .build(),
            )
            pruefeHttp(ergebnis.code, ergebnis.rumpf)
            val json = leseJson(ergebnis.rumpf, "Die Erneuerung lieferte keine gültigen Daten.")
            val neuerZugang = json.optString("access_token").takeIf(String::isNotBlank)
                ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Es kam kein neuer Zugangstoken zurück.")
            ablage.edit()
                .putString(SCHL_ZUGANG, neuerZugang)
                .putLong(SCHL_LAEUFT_AB, ablaufZeit(json))
                .apply {
                    json.optString("refresh_token").takeIf(String::isNotBlank)?.let { putString(SCHL_ERNEUERUNG, it) }
                    jwtEmail(neuerZugang)?.let { putString(SCHL_EMAIL, it) }
                    jwtKontoId(neuerZugang)?.let { putString(SCHL_KONTO, it) }
                }
                .apply()
            KompassLog.info("CodexClient", "gueltigerZugang", "Zugang erneuert")
            neuerZugang
        }
    }

    // --- Anfragen --------------------------------------------------------------------------

    /**
     * Stellt eine Frage und liefert die vollständige Antwort.
     *
     * [beiTeilstueck] bekommt die Antwort in Stücken, während sie eintrifft. Damit kann der
     * Chat schon mitschreiben, statt bis zum Schluss ein leeres Feld zu zeigen.
     */
    suspend fun frage(
        anweisung: String,
        eingabe: String,
        modellId: String,
        denktiefe: String,
        beiTeilstueck: suspend (String) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        var versuch = 0
        while (true) {
            var etwasGeliefert = false
            try {
                return@withContext frageEinmal(
                    nutzlast = baueNutzlast(anweisung, eingabe, modellId, denktiefe, versuch),
                ) { stueck ->
                    etwasGeliefert = true
                    beiTeilstueck(stueck)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                currentCoroutineContext().ensureActive()
                // Nach dem ersten gelieferten Stück NICHT wiederholen: Der zweite Durchlauf
                // würde den bereits angezeigten Anfang doppeln.
                val wiederholbar = (fehler as? CodexFehler)?.wiederholbar ?: (fehler is IOException)
                if (etwasGeliefert || versuch >= WIEDERHOLUNGEN.size || !wiederholbar) throw fehler
                delay(WIEDERHOLUNGEN[versuch])
                versuch += 1
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("unerreichbar")
    }

    /**
     * Der erste Versuch läuft über den bevorzugten Tarif. Ist der gestört — der Dienst
     * antwortet dann mit 5xx, obwohl der normale Weg bedient wird —, fällt jeder Folgeversuch
     * auf den Standardtarif zurück, statt die Anfrage scheitern zu lassen.
     */
    private fun baueNutzlast(
        anweisung: String,
        eingabe: String,
        modellId: String,
        denktiefe: String,
        versuch: Int,
    ): JSONObject {
        val nutzlast = JSONObject()
            .put("model", modellId)
            .put("stream", true)
            .put("store", false)
            .put("instructions", anweisung)
            .put(
                "input",
                JSONArray().put(JSONObject().put("role", "user").put("content", eingabe)),
            )
            .put("reasoning", JSONObject().put("effort", denktiefe))
        return if (versuch == 0) nutzlast.put("service_tier", "priority") else nutzlast
    }

    private suspend fun frageEinmal(
        nutzlast: JSONObject,
        beiTeilstueck: suspend (String) -> Unit,
    ): String {
        val zugang = gueltigerZugang()
        val kontoAusToken = jwtKontoId(zugang)
        if (kontoAusToken != null && kontoAusToken != kontoId) {
            ablage.edit().putString(SCHL_KONTO, kontoAusToken).apply()
        }
        val konto = kontoAusToken ?: kontoId
            ?: throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Im Zugang fehlt die Konto-Kennung.")

        val anfrage = Request.Builder()
            .url(ANTWORT_URL)
            .post(nutzlast.toString().toRequestBody(JSON_TYP))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $zugang")
            .header("originator", "codex_cli_rs")
            .header("User-Agent", "codex_cli_rs/0.0.0 (Codex Kompass)")
            .header("ChatGPT-Account-ID", konto)
            .build()

        val sammler = StromSammler()
        val aufruf = client.newCall(anfrage)
        laufendeAufrufe.add(aufruf)
        try {
            aufruf.awaitAntwort().use { antwort ->
                val rumpf = antwort.body
                if (!antwort.isSuccessful) pruefeHttp(antwort.code, rumpf?.string().orEmpty())
                val leser = rumpf?.charStream()?.buffered()
                    ?: throw CodexFehler(CodexFehlerArt.NETZ, "Es kam kein Antwortstrom zurück.")
                leser.use { lieferStrom(it, sammler, beiTeilstueck) }
            }
        } catch (fehler: IOException) {
            if (aufruf.isCanceled()) throw CancellationException("Die Anfrage wurde abgebrochen.")
            throw fehler
        } finally {
            laufendeAufrufe.remove(aufruf)
        }
        return sammler.ergebnis()
    }

    /** Bricht alle laufenden Anfragen ab — etwa wenn der Bildschirm verlassen wird. */
    fun brichAnfragenAb() {
        val laufend = laufendeAufrufe.toList()
        laufendeAufrufe.clear()
        laufend.forEach(Call::cancel)
    }

    private suspend fun lieferStrom(
        leser: BufferedReader,
        sammler: StromSammler,
        beiTeilstueck: suspend (String) -> Unit,
    ) {
        val datenZeilen = mutableListOf<String>()
        suspend fun verarbeite() {
            if (datenZeilen.isEmpty()) return
            val stueck = sammler.nimm(datenZeilen.joinToString("\n"))
            datenZeilen.clear()
            if (stueck.isNotEmpty()) beiTeilstueck(stueck)
        }
        while (true) {
            val zeile = leser.readLine() ?: break
            when {
                zeile.isEmpty() -> {
                    verarbeite()
                    if (sammler.istFertig) break
                }
                zeile.startsWith(":") -> Unit
                zeile.startsWith("data:") -> datenZeilen += zeile.removePrefix("data:").trimStart()
            }
        }
        verarbeite()
    }

    // --- Hilfen ----------------------------------------------------------------------------

    private data class HttpErgebnis(val code: Int, val rumpf: String)

    private suspend fun sendeJson(adresse: String, json: String): HttpErgebnis {
        val anfrage = Request.Builder()
            .url(adresse)
            .post(json.toRequestBody(JSON_TYP))
            .header("Accept", "application/json")
            .build()
        return fuehreAus(anmeldeClient, anfrage)
    }

    private suspend fun sendeFormular(adresse: String, rumpf: RequestBody): HttpErgebnis {
        val anfrage = Request.Builder().url(adresse).post(rumpf).header("Accept", "application/json").build()
        return fuehreAus(anmeldeClient, anfrage)
    }

    private suspend fun fuehreAus(client: OkHttpClient, anfrage: Request): HttpErgebnis =
        mitDnsGeduld { client.newCall(anfrage).awaitAntwort() }
            .use { HttpErgebnis(it.code, it.body?.string().orEmpty()) }

    /**
     * Direkt nach dem Aufwachen aus dem Hintergrund ist die Namensauflösung oft noch nicht
     * bereit. Ein einzelner Fehlschlag darf deshalb nicht als „nicht erreichbar" gelten.
     */
    private suspend fun <T> mitDnsGeduld(block: suspend () -> T): T {
        var letzter: UnknownHostException? = null
        repeat(DNS_WARTEN_MS.size + 1) { versuch ->
            try {
                return block()
            } catch (fehler: UnknownHostException) {
                letzter = fehler
                if (versuch < DNS_WARTEN_MS.size) {
                    warteAufGeprueftesNetz()
                    delay(DNS_WARTEN_MS[versuch])
                }
            }
        }
        throw CodexFehler(
            CodexFehlerArt.NETZ,
            "Codex ist über die aktuelle Verbindung noch nicht erreichbar. Prüf WLAN oder Mobilfunk.",
            letzter,
        )
    }

    private suspend fun warteAufVordergrundUndNetz(activity: ComponentActivity) {
        val bereit = withTimeoutOrNull(VORDERGRUND_WARTEN_MS) {
            while (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !hatGeprueftesNetz()) {
                delay(NETZ_ABFRAGE_MS)
            }
            true
        } ?: false
        if (!bereit) {
            throw CodexFehler(
                CodexFehlerArt.NETZ,
                "Die Anmeldung wurde bestätigt, aber die App hat noch keine Internetverbindung. " +
                    "Komm zurück in die App und versuch es erneut.",
            )
        }
    }

    private suspend fun warteAufGeprueftesNetz() {
        withTimeoutOrNull(NETZ_WARTEN_MS) {
            while (!hatGeprueftesNetz()) delay(NETZ_ABFRAGE_MS)
        }
    }

    private fun hatGeprueftesNetz(): Boolean = try {
        val netz = netzManager?.activeNetwork
        val faehigkeiten = netz?.let { netzManager.getNetworkCapabilities(it) }
        faehigkeiten != null &&
            faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (fehler: SecurityException) {
        // Ohne die Berechtigung entscheidet der HTTP-Aufruf selbst über die Erreichbarkeit.
        KompassLog.warn("CodexClient", "hatGeprueftesNetz", "Netzstatus nicht abfragbar", mapOf("grund" to fehler.message))
        true
    }

    private fun brauchtErneuerung(): Boolean =
        System.currentTimeMillis() >= ablage.getLong(SCHL_LAEUFT_AB, 0L) - ERNEUERUNGS_VORLAUF_MS

    private fun leseJson(wert: String, meldung: String): JSONObject =
        runCatching { JSONObject(wert) }.getOrElse { throw CodexFehler(CodexFehlerArt.NETZ, meldung, it) }

    private fun jwtKontoId(token: String): String? =
        jwtWert(token, "chatgpt_account_id") ?: jwtWert(token, "account_id")

    private fun jwtEmail(token: String): String? =
        jwtInhalt(token)?.optString("email")?.takeIf(String::isNotBlank) ?: jwtWert(token, "email")

    private fun jwtWert(token: String, name: String): String? = jwtInhalt(token)
        ?.optJSONObject("https://api.openai.com/auth")
        ?.optString(name)
        ?.takeIf(String::isNotBlank)

    private fun jwtInhalt(token: String): JSONObject? = runCatching {
        val teil = token.split('.')[1]
        val gefuellt = teil.padEnd((teil.length + 3) / 4 * 4, '=')
        JSONObject(String(Base64.getUrlDecoder().decode(gefuellt), Charsets.UTF_8))
    }.getOrNull()

    private fun ablaufZeit(json: JSONObject): Long =
        System.currentTimeMillis() + json.optLong("expires_in", STANDARD_LEBENSDAUER_SEKUNDEN) * 1_000L

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val GERAETECODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val GERAETETOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val BESTAETIGUNGS_URL = "https://auth.openai.com/codex/device"
        private const val WEITERLEITUNG_URL = "https://auth.openai.com/deviceauth/callback"
        private const val ANTWORT_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val ABLAGE_NAME = "codex_oauth"

        private const val SCHL_ZUGANG = "access_token"
        private const val SCHL_ERNEUERUNG = "refresh_token"
        private const val SCHL_LAEUFT_AB = "expires_at"
        private const val SCHL_KONTO = "account_id"
        private const val SCHL_EMAIL = "email"

        private const val ERNEUERUNGS_VORLAUF_MS = 120_000L
        private const val VORDERGRUND_WARTEN_MS = 5 * 60_000L
        private const val CODE_LEBENSDAUER_MS = 15 * 60_000L
        private const val NETZ_WARTEN_MS = 10_000L
        private const val NETZ_ABFRAGE_MS = 200L
        private const val ABSTAND_ZUWACHS = 2
        private const val LANGSAMER_SEKUNDEN = 5
        private const val STANDARD_LEBENSDAUER_SEKUNDEN = 3_600L
        private const val STANDARD_ABSTAND_SEKUNDEN = 5
        private const val MIN_ABSTAND_SEKUNDEN = 3
        private const val MAX_ABSTAND_SEKUNDEN = 30

        private val DNS_WARTEN_MS = longArrayOf(500L, 1_500L, 3_000L)
        private val WIEDERHOLUNGEN = longArrayOf(1_000L, 3_000L, 7_000L)
        private val JSON_TYP = "application/json; charset=utf-8".toMediaType()

        private val anmeldeClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()

        private val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()

        internal fun abfrageAbstand(wert: Any?): Int =
            (wert?.toString()?.toIntOrNull() ?: STANDARD_ABSTAND_SEKUNDEN)
                .coerceIn(MIN_ABSTAND_SEKUNDEN, MAX_ABSTAND_SEKUNDEN)

        /** Ordnet eine abgelehnte Antwort einer Fehlerart zu und wirft sie als [CodexFehler]. */
        internal fun pruefeHttp(code: Int, rumpf: String) {
            if (code in 200..299) return
            val klein = rumpf.lowercase()
            val meldung = runCatching {
                val json = JSONObject(rumpf)
                json.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
                    ?: json.optString("message").takeIf(String::isNotBlank)
            }.getOrNull() ?: rumpf.take(240)
            when {
                code == 429 || klein.contains("insufficient_quota") || klein.contains("rate_limit") ->
                    throw CodexFehler(
                        CodexFehlerArt.KONTINGENT,
                        "Dein Codex-Kontingent ist gerade ausgeschöpft. Bitte später erneut versuchen.",
                    )
                code == 401 || code == 403 || klein.contains("invalid_grant") ||
                    klein.contains("refresh_token_reused") ->
                    throw CodexFehler(
                        CodexFehlerArt.ANMELDUNG,
                        "Die Codex-Anmeldung ist ungültig. Bitte in den Einstellungen neu anmelden. $meldung",
                    )
                code == 408 || code in 500..599 -> throw CodexFehler(
                    CodexFehlerArt.NETZ,
                    "Codex ist gerade überlastet oder nicht erreichbar (Fehler $code). Kurz erneut versuchen.",
                    wiederholbar = true,
                )
                else -> throw CodexFehler(CodexFehlerArt.NETZ, "Codex-Fehler $code: $meldung")
            }
        }
    }
}

/**
 * Setzt die Antwort aus den einzelnen Strom-Ereignissen zusammen.
 *
 * Wichtig ist [istFertig]: Endet die Verbindung, ohne dass ein Abschluss kam, ist die Antwort
 * unvollständig. Das muss auffallen — sonst wird ein abgeschnittener Text als vollständig
 * gespeichert.
 */
internal class StromSammler {

    private val teile = StringBuilder()
    private var abgeschlossenerText: String? = null
    private var fertig = false

    val istFertig: Boolean get() = fertig

    fun nimm(daten: String): String {
        if (daten.isBlank() || daten == "[DONE]") return ""
        val ereignis = runCatching { JSONObject(daten) }.getOrElse {
            throw CodexFehler(CodexFehlerArt.NETZ, "Es kam ein unlesbares Strom-Ereignis an.", it)
        }
        return when (ereignis.optString("type")) {
            "response.output_text.delta" -> {
                val stueck = ereignis.optString("delta")
                if (stueck.isNotEmpty()) teile.append(stueck)
                stueck
            }
            "response.completed" -> {
                fertig = true
                abgeschlossenerText = ereignis.optJSONObject("response")?.let(::holeText)
                ""
            }
            "response.incomplete" -> throw CodexFehler(
                CodexFehlerArt.NETZ,
                "Codex hat die Antwort unvollständig beendet.",
            )
            "response.failed" -> throw stromFehler(ereignis.optJSONObject("response")?.optJSONObject("error"))
            "error" -> throw stromFehler(ereignis.optJSONObject("error") ?: ereignis)
            else -> ""
        }
    }

    fun ergebnis(): String {
        if (!fertig) {
            throw CodexFehler(CodexFehlerArt.NETZ, "Die Verbindung endete, bevor die Antwort fertig war.")
        }
        return abgeschlossenerText?.takeIf(String::isNotBlank)
            ?: teile.toString().takeIf(String::isNotBlank)
            ?: throw CodexFehler(CodexFehlerArt.NETZ, "Codex hat keinen Antworttext geliefert.")
    }

    private fun holeText(antwort: JSONObject): String? {
        antwort.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
        val ausgabe = antwort.optJSONArray("output") ?: return null
        return buildString {
            for (aussen in 0 until ausgabe.length()) {
                val inhalt = ausgabe.optJSONObject(aussen)?.optJSONArray("content") ?: continue
                for (innen in 0 until inhalt.length()) {
                    val stueck = inhalt.optJSONObject(innen) ?: continue
                    val text = stueck.optString("text").takeIf(String::isNotBlank)
                        ?: stueck.optString("output_text").takeIf(String::isNotBlank)
                    text?.let(::append)
                }
            }
        }.takeIf(String::isNotBlank)
    }

    private fun stromFehler(fehler: JSONObject?): CodexFehler {
        val meldung = fehler?.optString("message")?.takeIf(String::isNotBlank)
            ?: "Codex hat die Antwort abgebrochen."
        val kennzeichen = listOfNotNull(
            fehler?.optString("code"),
            fehler?.optString("type"),
            meldung,
        ).joinToString(" ").lowercase()
        val art = when {
            kennzeichen.contains("quota") || kennzeichen.contains("rate_limit") -> CodexFehlerArt.KONTINGENT
            kennzeichen.contains("invalid_grant") || kennzeichen.contains("unauthorized") ||
                kennzeichen.contains("authentication") -> CodexFehlerArt.ANMELDUNG
            else -> CodexFehlerArt.NETZ
        }
        return CodexFehler(art, meldung)
    }
}
