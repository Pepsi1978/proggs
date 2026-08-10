package de.frank.experimente.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Der Zugang zu Codex über Franks ChatGPT-Abo (F-24).
 *
 * Verfahren übernommen aus `PerfectMoment/.../auth/CodexAuthManager.kt`, wie der Bau-Auftrag
 * §2 es vorgibt: Geräteanmeldung mit Benutzercode, Token-Auffrischung mit Vorlauf, Anfragen
 * über die Codex-Responses-Schnittstelle als Ereignisstrom. Übernommen ist das Verfahren,
 * nicht die PerfectMoment-Fachlogik — hier steht nur, was diese App braucht.
 *
 * Kein Schlüssel im Quellcode: die Tokens liegen verschlüsselt auf dem Gerät.
 */
class CodexZugang(context: Context) {

    private val ctx = context.applicationContext
    private val auffrischen = Mutex()

    private val ablage by lazy {
        val schluessel = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx,
            "codex_oauth",
            schluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val email: String? get() = ablage.getString(KEY_EMAIL, null)
    private val kontoId: String? get() = ablage.getString(KEY_KONTO_ID, null)
    val istAngemeldet: Boolean get() = ablage.contains(KEY_ZUGRIFF)

    fun abmelden() = ablage.edit().clear().apply()

    // --- Geräteanmeldung ----------------------------------------------------------------

    /**
     * Startet die Geräteanmeldung, zeigt Benutzercode und Adresse über [beiCode] an und
     * öffnet die Bestätigungsseite. Kehrt zurück, sobald die Anmeldung steht.
     */
    suspend fun anmelden(beiCode: (Geraetecode) -> Unit): String? {
        val start = postJson(GERAET_CODE_URL, JSONObject().put("client_id", CLIENT_ID).toString())
        pruefe(start.code, start.rumpf)
        val json = alsJson(start.rumpf, "OpenAI hat keinen gültigen Gerätecode geliefert.")
        val benutzercode = json.optString("user_code").trim()
        val anmeldeId = json.optString("device_auth_id").trim()
        if (benutzercode.isEmpty() || anmeldeId.isEmpty()) {
            throw CodexFehler(FehlerArt.ANMELDUNG, "OpenAI hat keinen vollständigen Gerätecode geliefert.")
        }
        var abstand = (json.opt("interval")?.toString()?.toIntOrNull() ?: 5).coerceIn(3, 30)

        withContext(Dispatchers.Main) {
            beiCode(Geraetecode(benutzercode, GERAET_SEITE))
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(GERAET_SEITE))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        val laeuftAb = System.currentTimeMillis() + 15 * 60_000L
        while (System.currentTimeMillis() < laeuftAb) {
            delay(abstand * 1_000L)
            val runde = try {
                postJson(
                    GERAET_TOKEN_URL,
                    JSONObject()
                        .put("device_auth_id", anmeldeId)
                        .put("user_code", benutzercode)
                        .toString(),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                if (fehler is CodexFehler && fehler.art != FehlerArt.NETZ) throw fehler
                abstand = (abstand + 2).coerceIn(3, 30)
                continue
            }
            when {
                runde.code == 200 -> {
                    val bestaetigt = alsJson(runde.rumpf, "OpenAI hat die Geräteanmeldung ungültig bestätigt.")
                    val code = bestaetigt.optString("authorization_code").trim()
                    val pruefer = bestaetigt.optString("code_verifier").trim()
                    if (code.isEmpty() || pruefer.isEmpty()) {
                        throw CodexFehler(
                            FehlerArt.ANMELDUNG,
                            "OpenAI hat den Gerätecode bestätigt, aber keinen vollständigen Anmeldecode geliefert.",
                        )
                    }
                    return tauscheCode(code, pruefer)
                }
                // 403/404/429/5xx heissen: noch nicht bestätigt, weiter warten.
                runde.code == 403 || runde.code == 404 || runde.code == 429 || runde.code >= 500 -> {
                    if (runde.code == 429 || runde.rumpf.contains("slow_down", true)) {
                        abstand = (abstand + 5).coerceIn(3, 30)
                    }
                }
                else -> pruefe(runde.code, runde.rumpf)
            }
        }
        throw CodexFehler(
            FehlerArt.ANMELDUNG,
            "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.",
        )
    }

    private suspend fun tauscheCode(code: String, pruefer: String): String? {
        val ergebnis = postForm(
            TOKEN_URL,
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", GERAET_RUECKWEG)
                .add("code_verifier", pruefer)
                .build(),
        )
        pruefe(ergebnis.code, ergebnis.rumpf)
        val json = alsJson(ergebnis.rumpf, "OpenAI hat keine gültigen Anmeldetokens geliefert.")
        return sichereTokens(json)
    }

    private fun sichereTokens(json: JSONObject): String? {
        val zugriff = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexFehler(FehlerArt.ANMELDUNG, "OpenAI hat keinen Zugriffstoken geliefert.")
        val erneuern = json.optString("refresh_token").takeIf(String::isNotBlank)
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val gefundeneMail = idToken?.let(::jwtEmail) ?: jwtEmail(zugriff)
        val gefundenesKonto = jwtKontoId(zugriff) ?: idToken?.let(::jwtKontoId)
        ablage.edit()
            .putString(KEY_ZUGRIFF, zugriff)
            .putLong(KEY_LAEUFT_AB, System.currentTimeMillis() + json.optLong("expires_in", 3_600L) * 1_000L)
            .apply {
                if (erneuern != null) putString(KEY_ERNEUERN, erneuern)
                if (gefundeneMail != null) putString(KEY_EMAIL, gefundeneMail)
                if (gefundenesKonto != null) putString(KEY_KONTO_ID, gefundenesKonto)
            }
            .apply()
        return gefundeneMail
    }

    private suspend fun gueltigerToken(): String {
        val jetziger = ablage.getString(KEY_ZUGRIFF, null)
            ?: throw CodexFehler(FehlerArt.ANMELDUNG, "Bitte zuerst bei OpenAI anmelden.")
        if (!brauchtAuffrischung()) return jetziger
        return auffrischen.withLock {
            if (!brauchtAuffrischung()) return@withLock ablage.getString(KEY_ZUGRIFF, null) ?: jetziger
            val erneuern = ablage.getString(KEY_ERNEUERN, null)
                ?: throw CodexFehler(FehlerArt.ANMELDUNG, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
            val ergebnis = postForm(
                TOKEN_URL,
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", erneuern)
                    .add("client_id", CLIENT_ID)
                    .build(),
            )
            pruefe(ergebnis.code, ergebnis.rumpf)
            sichereTokens(alsJson(ergebnis.rumpf, "OpenAI hat beim Aktualisieren keine gültigen Tokens geliefert."))
            ablage.getString(KEY_ZUGRIFF, null)
                ?: throw CodexFehler(FehlerArt.ANMELDUNG, "OpenAI hat keinen neuen Zugriffstoken geliefert.")
        }
    }

    private fun brauchtAuffrischung(): Boolean =
        System.currentTimeMillis() >= ablage.getLong(KEY_LAEUFT_AB, 0L) - 120_000L

    // --- Anfragen -----------------------------------------------------------------------

    /**
     * Stellt eine Anfrage und gibt den Antworttext zurück.
     *
     * @param anweisung was die KI tun soll (System-Anweisung)
     * @param eingabe der Stoff, auf den sie sich bezieht
     * @param schema optionales JSON-Schema; ist es gesetzt, antwortet die KI strikt danach
     */
    suspend fun frage(
        anweisung: String,
        eingabe: String,
        modell: CodexModell,
        effort: Effort,
        schema: JSONObject? = null,
        schemaName: String = "antwort",
    ): String {
        val anfrage = JSONObject()
            .put("model", modell.apiId)
            .put("service_tier", "priority")
            .put("stream", true)
            .put("store", false)
            .put("instructions", anweisung)
            .put(
                "input",
                JSONArray().put(JSONObject().put("role", "user").put("content", eingabe)),
            )
            .put("reasoning", JSONObject().put("effort", effort.apiWert))
        if (schema != null) {
            anfrage.put(
                "text",
                JSONObject().put(
                    "format",
                    JSONObject()
                        .put("type", "json_schema")
                        .put("name", schemaName)
                        .put("strict", true)
                        .put("schema", schema),
                ),
            )
        }
        return mitWiederholung(anfrage)
    }

    /**
     * Wiederholt bei vorübergehenden Störungen mit wachsendem Abstand. Der erste Versuch
     * läuft über den Priority-Pool; ist der gestört, fällt jeder Folgeversuch auf den
     * Standard-Tarif zurück, statt die Anfrage scheitern zu lassen.
     */
    private suspend fun mitWiederholung(anfrage: JSONObject): String {
        var versuch = 0
        while (true) {
            try {
                return einmalFragen(fuerVersuch(anfrage, versuch))
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                currentCoroutineContext().ensureActive()
                val voruebergehend = fehler is CodexFehler && fehler.wiederholbar || fehler is IOException
                if (versuch >= ABSTAENDE_MS.size || !voruebergehend) throw fehler
                delay(ABSTAENDE_MS[versuch])
                versuch++
            }
        }
    }

    private fun fuerVersuch(anfrage: JSONObject, versuch: Int): JSONObject {
        if (versuch == 0) return anfrage
        return runCatching { JSONObject(anfrage.toString()).apply { remove("service_tier") } }
            .getOrDefault(anfrage)
    }

    private suspend fun einmalFragen(anfrage: JSONObject): String {
        val token = gueltigerToken()
        val kontoAusToken = jwtKontoId(token)
        if (kontoAusToken != null && kontoAusToken != kontoId) {
            ablage.edit().putString(KEY_KONTO_ID, kontoAusToken).apply()
        }
        val konto = kontoAusToken ?: kontoId
            ?: throw CodexFehler(FehlerArt.ANMELDUNG, "Im Codex-Token fehlt die ChatGPT-Konto-Kennung.")

        val http = Request.Builder()
            .url(ANTWORTEN_URL)
            .post(anfrage.toString().toRequestBody(JSON_TYP))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $token")
            .header("originator", "codex_cli_rs")
            .header("User-Agent", "codex_cli_rs/0.0.0 (Experimente)")
            .header("ChatGPT-Account-ID", konto)
            .build()

        val sammler = Stromsammler()
        mitDnsGeduld { ANTWORTEN_CLIENT.newCall(http).warteAufAntwort() }.use { antwort ->
            val rumpf = antwort.body
            if (!antwort.isSuccessful) pruefe(antwort.code, rumpf?.string().orEmpty())
            val leser = rumpf?.charStream()?.buffered()
                ?: throw CodexFehler(FehlerArt.NETZ, "OpenAI hat keinen Antwortstrom geliefert.")
            leser.use { liesEreignisse(it, { daten -> sammler.nimm(daten) }, { sammler.fertig }) }
        }
        return sammler.ergebnis()
    }

    // --- Ereignisstrom ------------------------------------------------------------------

    private class Stromsammler {
        private val teile = StringBuilder()
        private var ganzerText: String? = null
        var fertig = false
            private set

        fun nimm(daten: String) {
            if (daten.isBlank() || daten == "[DONE]") return
            val ereignis = runCatching { JSONObject(daten) }.getOrElse {
                throw CodexFehler(FehlerArt.NETZ, "OpenAI hat ein ungültiges Streaming-Ereignis geliefert.", it)
            }
            when (ereignis.optString("type")) {
                "response.output_text.delta" ->
                    ereignis.optString("delta").takeIf(String::isNotEmpty)?.let(teile::append)
                "response.completed" -> {
                    fertig = true
                    ganzerText = ereignis.optJSONObject("response")?.let(::textAusAntwort)
                }
                "response.incomplete" ->
                    throw CodexFehler(FehlerArt.NETZ, "OpenAI hat die Antwort unvollständig beendet.")
                "response.failed" -> throw stromFehler(ereignis.optJSONObject("response")?.optJSONObject("error"))
                "error" -> throw stromFehler(ereignis.optJSONObject("error") ?: ereignis)
            }
        }

        fun ergebnis(): String {
            if (!fertig) {
                throw CodexFehler(FehlerArt.NETZ, "Die OpenAI-Verbindung endete vor dem Abschluss der Antwort.")
            }
            return ganzerText?.takeIf(String::isNotBlank)
                ?: teile.toString().takeIf(String::isNotBlank)
                ?: throw CodexFehler(FehlerArt.NETZ, "OpenAI hat keinen Antworttext geliefert.")
        }
    }

    private suspend fun liesEreignisse(
        leser: BufferedReader,
        beiDaten: suspend (String) -> Unit,
        anhalten: () -> Boolean,
    ) {
        val zeilen = mutableListOf<String>()
        suspend fun weitergeben() {
            if (zeilen.isNotEmpty()) {
                beiDaten(zeilen.joinToString("\n"))
                zeilen.clear()
            }
        }
        while (true) {
            val zeile = leser.readLine() ?: break
            when {
                zeile.isEmpty() -> {
                    weitergeben()
                    if (anhalten()) break
                }
                zeile.startsWith(":") -> Unit
                zeile.startsWith("data:") -> zeilen += zeile.removePrefix("data:").trimStart()
            }
        }
        weitergeben()
    }

    // --- Werkzeug -----------------------------------------------------------------------

    private data class HttpErgebnis(val code: Int, val rumpf: String)

    private suspend fun postJson(url: String, json: String): HttpErgebnis {
        val anfrage = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_TYP))
            .header("Accept", "application/json")
            .build()
        return alsText(anfrage)
    }

    private suspend fun postForm(url: String, rumpf: RequestBody): HttpErgebnis {
        val anfrage = Request.Builder().url(url).post(rumpf).header("Accept", "application/json").build()
        return alsText(anfrage)
    }

    private suspend fun alsText(anfrage: Request): HttpErgebnis =
        mitDnsGeduld { ANMELDE_CLIENT.newCall(anfrage).warteAufAntwort() }.use {
            HttpErgebnis(it.code, it.body?.string().orEmpty())
        }

    /** Frisch verbundenes WLAN löst den Namen manchmal erst nach einem Moment auf. */
    private suspend fun <T> mitDnsGeduld(block: suspend () -> T): T {
        var letzter: UnknownHostException? = null
        repeat(DNS_ABSTAENDE_MS.size + 1) { versuch ->
            try {
                return block()
            } catch (fehler: UnknownHostException) {
                letzter = fehler
                if (versuch < DNS_ABSTAENDE_MS.size) delay(DNS_ABSTAENDE_MS[versuch])
            }
        }
        throw CodexFehler(
            FehlerArt.NETZ,
            "OpenAI ist über die aktuelle Verbindung nicht erreichbar. Bitte WLAN oder Mobilfunk prüfen.",
            letzter,
        )
    }

    private suspend fun Call.warteAufAntwort(): Response = suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = fortsetzung.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) = fortsetzung.resume(response)
        })
    }

    private fun alsJson(wert: String, meldung: String): JSONObject =
        runCatching { JSONObject(wert) }.getOrElse { throw CodexFehler(FehlerArt.NETZ, meldung, it) }

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

    private companion object {
        const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        const val GERAET_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        const val GERAET_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        const val GERAET_SEITE = "https://auth.openai.com/codex/device"
        const val GERAET_RUECKWEG = "https://auth.openai.com/deviceauth/callback"
        const val ANTWORTEN_URL = "https://chatgpt.com/backend-api/codex/responses"

        const val KEY_ZUGRIFF = "access_token"
        const val KEY_ERNEUERN = "refresh_token"
        const val KEY_LAEUFT_AB = "expires_at"
        const val KEY_KONTO_ID = "account_id"
        const val KEY_EMAIL = "email"

        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val ABSTAENDE_MS = longArrayOf(1_000L, 3_000L, 7_000L)
        val DNS_ABSTAENDE_MS = longArrayOf(500L, 1_500L, 3_000L)

        val ANMELDE_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()

        // Eine Vorschlagsanfrage mit vollem Kontext braucht Zeit — grosszügig bemessen.
        val ANTWORTEN_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()
    }
}

private fun textAusAntwort(antwort: JSONObject): String? {
    antwort.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val ausgabe = antwort.optJSONArray("output") ?: return null
    return buildString {
        for (i in 0 until ausgabe.length()) {
            val inhalt = ausgabe.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until inhalt.length()) {
                val stueck = inhalt.optJSONObject(j) ?: continue
                (stueck.optString("text").takeIf(String::isNotBlank)
                    ?: stueck.optString("output_text").takeIf(String::isNotBlank))
                    ?.let(::append)
            }
        }
    }.takeIf(String::isNotBlank)
}

private fun stromFehler(fehler: JSONObject?): CodexFehler {
    val meldung = fehler?.optString("message")?.takeIf(String::isNotBlank)
        ?: "OpenAI hat die Antwort abgebrochen."
    val merkmal = listOfNotNull(fehler?.optString("code"), fehler?.optString("type"), meldung)
        .joinToString(" ").lowercase()
    val art = when {
        merkmal.contains("quota") || merkmal.contains("rate_limit") -> FehlerArt.KONTINGENT
        merkmal.contains("auth") || merkmal.contains("token") -> FehlerArt.ANMELDUNG
        else -> FehlerArt.NETZ
    }
    return CodexFehler(art, meldung)
}

/**
 * Ordnet eine HTTP-Antwort einer Fehlerart zu. Vorübergehende Störungen werden als
 * wiederholbar gekennzeichnet, damit sie die Arbeit nicht abbrechen.
 */
private fun pruefe(code: Int, rumpf: String) {
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
                FehlerArt.KONTINGENT,
                "Dein Kontingent ist erschöpft. Bitte später erneut versuchen.",
            )
        code == 401 || code == 403 || klein.contains("invalid_grant") || klein.contains("refresh_token_reused") ->
            throw CodexFehler(
                FehlerArt.ANMELDUNG,
                "Deine Anmeldung ist abgelaufen. Bitte in den Einstellungen erneut anmelden. $meldung",
            )
        code == 408 || code in 500..599 -> throw CodexFehler(
            FehlerArt.NETZ,
            "OpenAI ist gerade nicht erreichbar (Fehler $code). Bitte kurz erneut versuchen.",
            wiederholbar = true,
        )
        else -> throw CodexFehler(FehlerArt.NETZ, "OpenAI-Fehler $code: $meldung")
    }
}
