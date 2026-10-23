package de.frank.jarvis.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.BufferedReader
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.AEADBadTagException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
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
import org.json.JSONArray
import org.json.JSONObject

internal enum class DevicePollAction { PROCESS, PENDING, FAIL }

internal fun devicePollAction(statusCode: Int): DevicePollAction = when {
    statusCode == 200 -> DevicePollAction.PROCESS
    statusCode == 403 || statusCode == 404 || statusCode == 429 || statusCode >= 500 -> DevicePollAction.PENDING
    else -> DevicePollAction.FAIL
}

internal fun devicePollInterval(value: Any?): Int =
    (value?.toString()?.toIntOrNull() ?: CodexAuthManager.DEFAULT_DEVICE_POLL_SECONDS)
        .coerceIn(CodexAuthManager.MIN_DEVICE_POLL_SECONDS, CodexAuthManager.MAX_DEVICE_POLL_SECONDS)

/** Der Satz aus Baustein M.3 — er steht in jedem Systemprompt an das Modell. */
internal const val UMLAUT_HINWEIS =
    "Antworte auf Deutsch mit echten Umlauten (ä ö ü Ä Ö Ü ß). Verwende niemals die " +
        "Ersatzschreibung ae, oe, ue oder ss."

/** Eine Sprechzeile im Gespräch: `user` für mich, `assistant` für das Modell. */
data class ChatTurn(val role: String, val text: String, val bilder: List<String> = emptyList())

internal fun codexChatPayload(
    instructions: String,
    turns: List<ChatTurn>,
    model: CodexModel,
    reasoningEffort: ReasoningEffort,
): JSONObject {
    val input = JSONArray()
    turns.forEach { turn ->
        if (turn.bilder.isEmpty()) input.put(JSONObject().put("role", turn.role).put("content", turn.text))
        else input.put(JSONObject().put("role", turn.role).put("content", JSONArray().apply {
            put(JSONObject().put("type", "input_text").put("text", turn.text))
            turn.bilder.forEach { put(JSONObject().put("type", "input_image").put("image_url", it)) }
        }))
    }
    return JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put("instructions", instructions.trim() + "\n\n" + UMLAUT_HINWEIS)
        .put("input", input)
        .put("reasoning", JSONObject().put("effort", model.normalizeEffort(reasoningEffort).apiValue))
}

/**
 * Ordnet eine HTTP-Antwort einer Fehlerart zu. Vorübergehende Störungen (Zeitüberschreitung,
 * Serverfehler 5xx) werden als wiederholbar gekennzeichnet, damit sie die Sitzung nicht beenden.
 */
internal fun classifyHttpError(code: Int, body: String) {
    if (code in 200..299) return
    val lowerBody = body.lowercase()
    val message = runCatching {
        val json = JSONObject(body)
        val error = json.optJSONObject("error")
        error?.optString("message")?.takeIf(String::isNotBlank)
            ?: json.optString("message").takeIf(String::isNotBlank)
    }.getOrNull() ?: body.take(240)
    when {
        code == 429 || lowerBody.contains("insufficient_quota") || lowerBody.contains("rate_limit") ->
            throw CodexAuthException(
                AuthErrorKind.QUOTA,
                "Dein ChatGPT-/Codex-Kontingent ist aktuell ausgeschöpft. Bitte später erneut versuchen.",
            )
        code == 401 || code == 403 || lowerBody.contains("invalid_grant") || lowerBody.contains("refresh_token_reused") ->
            throw CodexAuthException(
                AuthErrorKind.REAUTH,
                "OpenAI-Anmeldung ungültig oder nicht mehr zugelassen. Bitte erneut anmelden. $message",
            )
        code == 408 || code in 500..599 -> throw CodexAuthException(
            AuthErrorKind.NETWORK,
            "OpenAI ist gerade überlastet oder nicht erreichbar (Fehler $code). Bitte kurz erneut versuchen.",
            retryable = true,
        )
        else -> throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI-Fehler $code: $message")
    }
}

/**
 * Der erste Versuch läuft über den Priority-Pool. Ist der gestört — OpenAI antwortet dann mit
 * 5xx, obwohl der Standardweg bedient wird — fällt jeder Folgeversuch auf den Standard-Tarif
 * zurück, statt die Sitzung scheitern zu lassen. Die übergebene Anfrage bleibt unverändert.
 */
internal fun payloadForAttempt(payload: JSONObject, attempt: Int): JSONObject {
    if (attempt == 0) return payload
    return runCatching {
        JSONObject(payload.toString()).apply { remove("service_tier") }
    }.getOrDefault(payload)
}

internal class CodexSseAccumulator {
    private val deltas = StringBuilder()
    private var completedText: String? = null
    private var completed = false
    private val seenItems = mutableSetOf<String>()

    /** Base64-Bilder aus dem Bildwerkzeug (`image_generation_call`), in der Reihenfolge ihres Eintreffens. */
    val images = mutableListOf<String>()

    /** Adressen, die die Websuche benutzt oder die Antwort zitiert hat (wie im Sammler von News Kompass). */
    val sources = linkedSetOf<String>()

    val isCompleted: Boolean get() = completed

    /** Wie im Sammler von News Kompass: jedes fertige Ausgabe-Element nur einmal auswerten. */
    private fun collect(item: JSONObject) {
        val key = item.optString("id").ifBlank { item.toString().hashCode().toString() }
        if (!seenItems.add(key)) return
        when (item.optString("type")) {
            "image_generation_call" -> item.optString("result").takeIf(String::isNotBlank)?.let(images::add)
            "web_search_call" -> item.optJSONObject("action")?.optJSONArray("sources")?.let { found ->
                for (i in 0 until found.length()) found.optJSONObject(i)?.optString("url")?.takeIf(String::isNotBlank)?.let(sources::add)
            }
            "message" -> item.optJSONArray("content")?.let { content ->
                for (i in 0 until content.length()) {
                    val notes = content.optJSONObject(i)?.optJSONArray("annotations") ?: continue
                    for (j in 0 until notes.length()) notes.optJSONObject(j)?.optString("url")?.takeIf(String::isNotBlank)?.let(sources::add)
                }
            }
        }
    }

    /** Gibt das neue Textstück zurück, oder null, wenn das Ereignis keinen Text trug. */
    fun accept(data: String): String? {
        if (data.isBlank() || data == "[DONE]") return null
        val event = runCatching { JSONObject(data) }.getOrElse {
            throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat ein ungültiges Streaming-Ereignis geliefert.", it)
        }
        return when (event.optString("type")) {
            "response.output_text.delta" -> event.optString("delta").takeIf(String::isNotEmpty)?.also(deltas::append)
            "response.output_item.done" -> {
                event.optJSONObject("item")?.let(::collect)
                null
            }
            "response.completed" -> {
                completed = true
                val response = event.optJSONObject("response")
                response?.optJSONArray("output")?.let { output ->
                    for (index in 0 until output.length()) output.optJSONObject(index)?.let(::collect)
                }
                completedText = response?.let(::extractOutputText)
                null
            }
            "response.incomplete" -> throw CodexAuthException(
                AuthErrorKind.NETWORK,
                "OpenAI hat die Antwort unvollständig beendet.",
            )
            "response.failed" -> throw streamException(event.optJSONObject("response")?.optJSONObject("error"))
            "error" -> throw streamException(event.optJSONObject("error") ?: event)
            else -> null
        }
    }

    /** Das bisher Empfangene — auch dann, wenn die Verbindung vorzeitig abriss. */
    fun partial(): String = completedText ?: deltas.toString()

    fun result(): String {
        if (!completed) {
            throw CodexAuthException(AuthErrorKind.NETWORK, "Die OpenAI-Verbindung endete vor dem Abschluss der Antwort.")
        }
        return completedText?.takeIf(String::isNotBlank)
            ?: deltas.toString().takeIf(String::isNotBlank)
            ?: (if (images.isNotEmpty()) "" else null)
            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antworttext geliefert.")
    }
}

internal fun extractOutputText(response: JSONObject): String? {
    response.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val output = response.optJSONArray("output") ?: return null
    return buildString {
        for (outputIndex in 0 until output.length()) {
            val content = output.optJSONObject(outputIndex)?.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val item = content.optJSONObject(contentIndex) ?: continue
                val text = item.optString("text").takeIf(String::isNotBlank)
                    ?: item.optString("output_text").takeIf(String::isNotBlank)
                text?.let(::append)
            }
        }
    }.takeIf(String::isNotBlank)
}

internal suspend fun readSseData(
    reader: BufferedReader,
    onData: suspend (String) -> Unit,
    shouldStop: () -> Boolean = { false },
) {
    val dataLines = mutableListOf<String>()
    suspend fun dispatch() {
        // Ein abgelaufenes Zeitlimit beendet das Lesen beim nächsten Ereignis.
        currentCoroutineContext().ensureActive()
        if (dataLines.isNotEmpty()) {
            onData(dataLines.joinToString("\n"))
            dataLines.clear()
        }
    }
    while (true) {
        val line = reader.readLine() ?: break
        when {
            line.isEmpty() -> {
                dispatch()
                if (shouldStop()) break
            }
            line.startsWith(":") -> Unit
            line.startsWith("data:") -> dataLines += line.removePrefix("data:").trimStart()
        }
    }
    dispatch()
}

class CodexAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val loginMutex = Mutex()
    private val activeLoginJob = AtomicReference<Job?>()
    private val activeQuestionCall = ActiveCallTracker()
    /** Ein Speicher für alle Instanzen im Prozess: Keystore und Schlüssel werden nur einmal aufgebaut. */
    private val store: SharedPreferences get() = gemeinsamerStore(appContext)

    val email: String? get() = store.getString(KEY_EMAIL, null)
    val accountId: String? get() = store.getString(KEY_ACCOUNT_ID, null)
    val isConnected: Boolean get() = store.contains(KEY_ACCESS_TOKEN)

    private fun anthropicSchluessel(): String = de.frank.jarvis.data.settings.SecureSettings.get(appContext).anthropicApiKey.trim()
    val hatAnthropicSchluessel: Boolean get() = anthropicSchluessel().isNotBlank()

    /** Kann Jarvis mit diesem Modell denken? OpenAI braucht das ChatGPT-Konto, Claude den Anthropic-Schlüssel. */
    fun kannDenken(model: CodexModel): Boolean = if (model.istAnthropic) hatAnthropicSchluessel else isConnected

    /** Bildwerkzeug und Websuche gibt es nur über das ChatGPT-Konto: Bei einem Claude-Modell nimmt Jarvis dafür das zuletzt gewählte OpenAI-Modell. */
    private fun fuerOpenAi(model: CodexModel): CodexModel =
        if (model.istAnthropic) de.frank.jarvis.data.Einstellungen.get(appContext).openAiModell else model

    /**
     * [context] am besten der Application-Context: Die Anmeldung wartet bis zu einer Viertelstunde und überlebt so ein
     * Auf- oder Zuklappen, ohne eine alte Activity festzuhalten. [imVordergrund] meldet, ob die App gerade sichtbar ist.
     * [vorDemOeffnen] läuft auf dem Hauptfaden unmittelbar bevor die Anmeldeseite im Browser aufgeht.
     */
    suspend fun login(
        context: Context,
        imVordergrund: () -> Boolean,
        vorDemOeffnen: () -> Unit = {},
        onDeviceCode: (DeviceAuthInfo) -> Unit,
    ): AuthResult = loginMutex.withLock {
        val loginJob = currentCoroutineContext().job
        activeLoginJob.set(loginJob)
        try {
            withContext(Dispatchers.IO) { loginInternal(context, imVordergrund, vorDemOeffnen, onDeviceCode) }
        } catch (error: IOException) {
            throw networkException("Die OpenAI-Anmeldung ist wegen eines Netzwerkfehlers fehlgeschlagen.", error)
        } finally {
            activeLoginJob.compareAndSet(loginJob, null)
        }
    }

    fun cancelLogin() {
        activeLoginJob.getAndSet(null)?.cancel()
    }

    /** Bricht die laufende KI-Anfrage wirklich ab — nicht nur die Anzeige (Baustein O.2). */
    fun cancelChat() {
        activeQuestionCall.cancel()
    }

    /**
     * Führt ein Gespräch mit dem Modell. [onDelta] bekommt jedes Textstück, sobald es eintrifft,
     * damit die Antwort Wort für Wort erscheint.
     */
    suspend fun streamChat(
        instructions: String,
        turns: List<ChatTurn>,
        model: CodexModel,
        reasoningEffort: ReasoningEffort,
        onDelta: suspend (String) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        if (turns.isEmpty()) {
            throw CodexAuthException(AuthErrorKind.NETWORK, "Für die Anfrage fehlt der Text.")
        }
        try {
            if (model.istAnthropic) requestAnthropic(instructions, turns, model, reasoningEffort, onDelta)
            else requestCodexResponse(codexChatPayload(instructions, turns, model, reasoningEffort), onDelta)
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw networkException("Die Antwort konnte nicht vollständig empfangen werden.", error)
        }
    }

    /**
     * Erzeugt Bilder über das Bildwerkzeug von Codex (`image_generation`), derselbe Weg wie in News Kompass.
     * [size] ist optional ("1024x1536" = Hochformat für DIN A4). Lehnt der Dienst die Größenangabe ab,
     * folgt genau ein zweiter Versuch ohne sie. Rückgabe: die PNG-Daten; leer, wenn kein Bild kam.
     */
    suspend fun generateImages(
        prompt: String,
        model: CodexModel,
        size: String? = null,
        quality: String? = null,
    ): List<ByteArray> = withContext(Dispatchers.IO) {
        if (prompt.isBlank()) throw CodexAuthException(AuthErrorKind.NETWORK, "Für das Bild fehlt die Beschreibung.")
        val model = fuerOpenAi(model)
        suspend fun attempt(withOptions: Boolean): List<ByteArray> {
            val tool = JSONObject().put("type", "image_generation").put("output_format", "png")
            if (withOptions) {
                size?.let { tool.put("size", it) }
                quality?.let { tool.put("quality", it) }
            }
            val payload = codexChatPayload(
                "Du erzeugst Bilder. Rufe das Bildwerkzeug genau einmal auf und antworte danach nur mit OK.",
                listOf(ChatTurn("user", prompt)),
                model,
                ReasoningEffort.LOW,
            ).put("tools", JSONArray().put(tool)).put("tool_choice", "auto")
            return try {
                requestCodexAccumulated(payload).images.map { Base64.getMimeDecoder().decode(it) }
            } catch (error: IOException) {
                currentCoroutineContext().ensureActive()
                throw networkException("Das Bild konnte nicht vollständig empfangen werden.", error)
            }
        }
        try {
            attempt(withOptions = true)
        } catch (error: CodexAuthException) {
            val rejectedOption = (size != null || quality != null) && error.kind == AuthErrorKind.NETWORK && !error.retryable &&
                (error.message.orEmpty().contains("size", ignoreCase = true) || error.message.orEmpty().contains("quality", ignoreCase = true) ||
                    error.message.orEmpty().contains("400"))
            if (!rejectedOption) throw error
            attempt(withOptions = false)
        }
    }

    /**
     * Sucht über das eingebaute Websuche-Werkzeug von Codex (`web_search`), derselbe Weg wie in News Kompass.
     * Rückgabe: die Antwort des Modells und die benutzten Quellen. [knapp] = Frage im laufenden Gespräch: eine gezielte Suche,
     * kurze Antwort, damit sie in die Zeit einer Gesprächsantwort passt.
     */
    suspend fun searchWeb(query: String, model: CodexModel, knapp: Boolean = false): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw CodexAuthException(AuthErrorKind.NETWORK, "Die Suchanfrage ist leer.")
        val model = fuerOpenAi(model)
        val payload = codexChatPayload(
            (if (knapp) "Du beantwortest für einen anderen Assistenten eine Frage im laufenden Gespräch. Suche mit dem Websuche-Werkzeug, meist genügt eine gezielte Suche. " +
                "Antworte in höchstens drei Sätzen mit dem Ergebnis und der wichtigsten Quelle (Titel und Adresse). "
            else "Du recherchierst im Internet für einen anderen Assistenten. Suche mit dem Websuche-Werkzeug, gern mehrfach. " +
                "Antworte sachlich und knapp mit den gefundenen Fakten, je Aussage mit der Quelle (Titel und vollständige Adresse). ") +
                "Trenne Belegtes von Unsicherem. Erfinde nichts.",
            listOf(ChatTurn("user", query)),
            model,
            ReasoningEffort.LOW,
        ).put("tools", JSONArray().put(JSONObject().put("type", "web_search"))).put("tool_choice", "auto")
            .put("include", JSONArray().put("web_search_call.action.sources"))
        try {
            val result = requestCodexAccumulated(payload)
            result.result() to result.sources.toList()
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw networkException("Die Suchantwort konnte nicht vollständig empfangen werden.", error)
        }
    }

    fun logout() {
        store.edit().clear().apply()
    }

    private suspend fun loginInternal(
        context: Context,
        imVordergrund: () -> Boolean,
        vorDemOeffnen: () -> Unit,
        onDeviceCode: (DeviceAuthInfo) -> Unit,
    ): AuthResult {
        val start = postJson(
            DEVICE_USER_CODE_URL,
            JSONObject().put("client_id", CLIENT_ID).toString(),
        )
        classifyHttpError(start.code, start.body)
        val startJson = parseJson(start.body, "OpenAI hat keinen gültigen Gerätecode geliefert.")
        val userCode = startJson.optString("user_code").trim()
        val deviceAuthId = startJson.optString("device_auth_id").trim()
        if (userCode.isEmpty() || deviceAuthId.isEmpty()) {
            throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen vollständigen Gerätecode geliefert.")
        }
        var intervalSeconds = devicePollInterval(startJson.opt("interval"))
        val deviceInfo = DeviceAuthInfo(userCode, DEVICE_VERIFICATION_URL)
        withContext(Dispatchers.Main) {
            onDeviceCode(deviceInfo)
            // Erst kopieren, dann die Seite öffnen: Dort genügt dann Einfügen.
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("OpenAI-Code", userCode))
        }
        // Kurz stehen lassen, damit der Code sichtbar ist, bevor der Browser übernimmt.
        delay(1_400)
        withContext(Dispatchers.Main) {
            vorDemOeffnen()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deviceInfo.verificationUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        val expiresAt = System.currentTimeMillis() + DEVICE_CODE_LIFETIME_MS
        while (System.currentTimeMillis() < expiresAt) {
            delay(intervalSeconds * 1_000L)
            val poll = try {
                postJson(
                    DEVICE_TOKEN_URL,
                    JSONObject()
                        .put("device_auth_id", deviceAuthId)
                        .put("user_code", userCode)
                        .toString(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error is CodexAuthException && error.kind != AuthErrorKind.NETWORK) throw error
                intervalSeconds = (intervalSeconds + NETWORK_POLL_BACKOFF_SECONDS)
                    .coerceIn(MIN_DEVICE_POLL_SECONDS, MAX_DEVICE_POLL_SECONDS)
                continue
            }

            when (devicePollAction(poll.code)) {
                DevicePollAction.PENDING -> {
                    if (poll.code == 429 || poll.body.contains("slow_down", ignoreCase = true)) {
                        intervalSeconds = (intervalSeconds + SLOW_DOWN_SECONDS)
                            .coerceIn(MIN_DEVICE_POLL_SECONDS, MAX_DEVICE_POLL_SECONDS)
                    }
                }
                DevicePollAction.FAIL -> classifyHttpError(poll.code, poll.body)
                DevicePollAction.PROCESS -> {
                    val authorization = parseJson(poll.body, "OpenAI hat die Geräteanmeldung ungültig bestätigt.")
                    val code = authorization.optString("authorization_code").trim()
                    val verifier = authorization.optString("code_verifier").trim()
                    if (code.isEmpty() || verifier.isEmpty()) {
                        throw CodexAuthException(
                            AuthErrorKind.REAUTH,
                            "OpenAI hat den Gerätecode bestätigt, aber keinen vollständigen Anmeldecode geliefert.",
                        )
                    }
                    awaitForegroundAndNetwork(imVordergrund)
                    return exchangeCode(code, verifier)
                }
            }
        }
        throw CodexAuthException(AuthErrorKind.REAUTH, "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.")
    }

    private suspend fun exchangeCode(code: String, verifier: String): AuthResult {
        val result = postForm(
            TOKEN_URL,
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", DEVICE_REDIRECT_URI)
                .add("code_verifier", verifier)
                .build(),
        )
        classifyHttpError(result.code, result.body)
        val json = parseJson(result.body, "OpenAI hat keine gültigen Anmeldetokens geliefert.")
        val accessToken = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen Zugriffstoken geliefert.")
        val refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank)
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val foundEmail = idToken?.let(::jwtEmail) ?: jwtEmail(accessToken)
        val foundAccountId = jwtAccountId(accessToken) ?: idToken?.let(::jwtAccountId)
        store.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, expiryTime(json))
            .apply {
                if (refreshToken == null) remove(KEY_REFRESH_TOKEN) else putString(KEY_REFRESH_TOKEN, refreshToken)
                if (foundEmail == null) remove(KEY_EMAIL) else putString(KEY_EMAIL, foundEmail)
                if (foundAccountId == null) remove(KEY_ACCOUNT_ID) else putString(KEY_ACCOUNT_ID, foundAccountId)
            }
            .apply()
        return AuthResult(foundEmail)
    }

    private suspend fun validAccessToken(): String {
        val currentToken = store.getString(KEY_ACCESS_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
        if (!tokenNeedsRefresh()) return currentToken

        // Prozessweit gesperrt: Chat, Agenten, Websuche und Tagesauswertung haben eigene Instanzen, teilen aber einen Refresh-Token.
        // Zwei gleichzeitige Erneuerungen mit demselben Token beendet OpenAI mit refresh_token_reused und die Anmeldung wäre weg.
        return refreshMutex.withLock {
            val tokenAfterLock = store.getString(KEY_ACCESS_TOKEN, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
            if (!tokenNeedsRefresh()) return@withLock tokenAfterLock
            val refreshToken = store.getString(KEY_REFRESH_TOKEN, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
            // Nicht abbrechbar: OpenAI rotiert den Refresh-Token beim Erneuern. Bricht der Aufrufer zwischen Antwort und
            // Speichern ab, wäre der neue Token verloren und der alte verbraucht. Deshalb auch commit() statt apply().
            withContext(NonCancellable) {
                val result = postForm(
                    TOKEN_URL,
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .add("client_id", CLIENT_ID)
                        .build(),
                )
                classifyHttpError(result.code, result.body)
                val json = parseJson(result.body, "OpenAI hat beim Aktualisieren keine gültigen Tokens geliefert.")
                val newAccessToken = json.optString("access_token").takeIf(String::isNotBlank)
                    ?: throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen neuen Zugriffstoken geliefert.")
                val newRefreshToken = json.optString("refresh_token").takeIf(String::isNotBlank)
                val idToken = json.optString("id_token").takeIf(String::isNotBlank)
                val foundEmail = idToken?.let(::jwtEmail) ?: jwtEmail(newAccessToken)
                val foundAccountId = jwtAccountId(newAccessToken) ?: idToken?.let(::jwtAccountId)
                store.edit()
                    .putString(KEY_ACCESS_TOKEN, newAccessToken)
                    .putLong(KEY_EXPIRES_AT, expiryTime(json))
                    .apply {
                        if (newRefreshToken != null) putString(KEY_REFRESH_TOKEN, newRefreshToken)
                        if (foundEmail != null) putString(KEY_EMAIL, foundEmail)
                        if (foundAccountId != null) putString(KEY_ACCOUNT_ID, foundAccountId)
                    }
                    .commit()
                newAccessToken
            }
        }
    }

    private fun tokenNeedsRefresh(): Boolean =
        System.currentTimeMillis() >= store.getLong(KEY_EXPIRES_AT, 0L) - REFRESH_SKEW_MS

    private suspend fun postJson(url: String, json: String): HttpResult {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()
        return executeForText(AUTH_HTTP_CLIENT, request)
    }

    private suspend fun postForm(url: String, body: RequestBody): HttpResult {
        val request = Request.Builder().url(url).post(body).header("Accept", "application/json").build()
        return executeForText(AUTH_HTTP_CLIENT, request)
    }

    private suspend fun executeForText(client: OkHttpClient, request: Request): HttpResult =
        executeWithDnsRetry(client, request).use { response ->
            HttpResult(response.code, response.body?.string().orEmpty())
        }

    private suspend fun executeWithDnsRetry(client: OkHttpClient, request: Request): Response =
        withDnsRetry { client.newCall(request).awaitResponse() }

    private suspend fun executeQuestionRequest(request: Request): TrackedResponse = withDnsRetry {
        val call = RESPONSES_HTTP_CLIENT.newCall(request)
        activeQuestionCall.track(call)
        try {
            TrackedResponse(call, call.awaitResponse())
        } catch (error: Throwable) {
            activeQuestionCall.clear(call)
            throw error
        }
    }

    /**
     * Führt die Anfrage aus und wiederholt sie bei vorübergehenden Störungen (Serverfehler 5xx,
     * Zeitüberschreitungen, Verbindungsabbrüche) mit wachsendem Abstand.
     *
     * Wiederholt wird ausschliesslich, solange noch kein Text ausgeliefert wurde — sonst
     * stünde die halbe Antwort zweimal da. Ein vom Nutzer abgebrochener Aufruf wird nie
     * wiederholt.
     */
    private suspend fun requestCodexResponse(
        payload: JSONObject,
        onDelta: suspend (String) -> Unit = {},
    ): String = requestCodexAccumulated(payload, onDelta).result()

    private suspend fun requestCodexAccumulated(
        payload: JSONObject,
        onDelta: suspend (String) -> Unit = {},
    ): CodexSseAccumulator {
        var attempt = 0
        while (true) {
            var deliveredText = false
            try {
                return requestCodexResponseOnce(payloadForAttempt(payload, attempt)) { delta ->
                    deliveredText = true
                    onDelta(delta)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (deliveredText || attempt >= TRANSIENT_RETRY_DELAYS_MS.size || !error.isTransient()) {
                    throw error
                }
                delay(TRANSIENT_RETRY_DELAYS_MS[attempt])
                attempt++
            }
        }
    }

    private fun Exception.isTransient(): Boolean = when (this) {
        is CodexAuthException -> retryable
        is IOException -> true
        else -> false
    }

    /**
     * Codex Cloud wie in der Codex-App (neue Umgebungen, die `codex cloud exec` und /wham nicht sehen): JSON-RPC des
     * App-Servers über wss://codex-cloud-backend.chatgpt.com mit dem ChatGPT-Konto. [block] bekommt `rpc(methode, params)`,
     * das auf die Antwort wartet (Fehler als Ausnahme); `initialize` ist schon erledigt. Danach wird die Verbindung geschlossen,
     * ein gestarteter Zug läuft in der Cloud weiter.
     */
    suspend fun <T> codexCloudRpc(block: suspend (rpc: suspend (String, JSONObject) -> JSONObject) -> T): T = withContext(Dispatchers.IO) {
        val token = validAccessToken()
        val konto = jwtAccountId(token) ?: accountId
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
        val offen = kotlinx.coroutines.CompletableDeferred<Unit>()
        val warten = java.util.concurrent.ConcurrentHashMap<Int, kotlinx.coroutines.CompletableDeferred<JSONObject>>()
        fun alleScheitern(fehler: Throwable) {
            offen.completeExceptionally(fehler)
            warten.values.forEach { it.completeExceptionally(fehler) }
        }
        val anfrage = Request.Builder().url(CODEX_CLOUD_URL)
            .header("Authorization", "Bearer $token")
            .header("ChatGPT-Account-Id", konto)
            .header("Origin", "https://chatgpt.com")
            // Ohne Client-Kennung lehnt der Server die Verbindung mit 403 ab.
            .header("User-Agent", "codex_cli_rs/$CODEX_CLIENT_VERSION (Aufgaben)")
            .build()
        val socket = CODEX_CLOUD_HTTP_CLIENT.newWebSocket(anfrage, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) { offen.complete(Unit) }
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                val j = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (!j.has("id") || j.has("method")) return
                val ziel = warten.remove(j.optInt("id")) ?: return
                val fehler = j.optJSONObject("error")
                if (fehler != null) ziel.completeExceptionally(IOException("Codex Cloud: ${fehler.optString("message").take(300)}"))
                else ziel.complete(j.optJSONObject("result") ?: JSONObject())
            }
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                alleScheitern(when (response?.code) {
                    401, 403 -> CodexAuthException(AuthErrorKind.REAUTH, "Codex Cloud lehnt die Anmeldung ab (HTTP ${response.code}). Bitte in Jarvis neu bei ChatGPT anmelden.")
                    else -> IOException("Codex Cloud nicht erreichbar: ${t.message ?: t.javaClass.simpleName}", t)
                })
            }
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) { alleScheitern(IOException("Codex Cloud hat die Verbindung beendet ($code).")) }
        })
        var naechste = 1
        val rpc: suspend (String, JSONObject) -> JSONObject = { methode, params ->
            val id = naechste++
            val antwort = kotlinx.coroutines.CompletableDeferred<JSONObject>()
            warten[id] = antwort
            socket.send(JSONObject().put("id", id).put("method", methode).put("params", params).toString())
            withTimeoutOrNull(CODEX_CLOUD_TIMEOUT_MS) { antwort.await() } ?: throw IOException("Codex Cloud antwortet nicht auf $methode.")
        }
        try {
            withTimeoutOrNull(CODEX_CLOUD_TIMEOUT_MS) { offen.await() } ?: throw IOException("Codex Cloud: Verbindungsaufbau dauerte zu lange.")
            rpc("initialize", JSONObject().put("clientInfo", JSONObject().put("name", "jarvis").put("title", "Jarvis").put("version", CODEX_CLIENT_VERSION)))
            block(rpc)
        } finally {
            socket.close(1000, null)
        }
    }

    private suspend fun requestCodexResponseOnce(
        payload: JSONObject,
        onDelta: suspend (String) -> Unit,
    ): CodexSseAccumulator {
        val token = validAccessToken()
        val tokenAccountId = jwtAccountId(token)
        if (tokenAccountId != null && tokenAccountId != accountId) {
            store.edit().putString(KEY_ACCOUNT_ID, tokenAccountId).apply()
        }
        val requestAccountId = tokenAccountId ?: accountId
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
        val httpRequest = Request.Builder()
            .url(RESPONSES_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $token")
            .header("originator", "codex_cli_rs")
            // Neue Modelle (z. B. GPT-6.1 Sol) schaltet das Codex-Backend erst ab Client-Kennung 0.159 frei, sonst HTTP 400.
            .header("version", CODEX_CLIENT_VERSION)
            .header("User-Agent", "codex_cli_rs/$CODEX_CLIENT_VERSION (Aufgaben)")
            .header("ChatGPT-Account-ID", requestAccountId)
            .build()
        val accumulator = CodexSseAccumulator()
        val trackedResponse = executeQuestionRequest(httpRequest)
        coroutineScope {
            // Wächter: Läuft ein Zeitlimit ab, schließt er die Verbindung. Das blockierende readLine merkt einen Abbruch
            // der Koroutine sonst erst, wenn der ganze Modellzug fertig gestreamt ist oder nach Minuten das readTimeout greift.
            val fertig = AtomicBoolean(false)
            val waechter = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!fertig.get()) trackedResponse.call.cancel()
                }
            }
            try {
                trackedResponse.response.use { response ->
                    val responseBody = response.body
                    if (!response.isSuccessful) classifyHttpError(response.code, responseBody?.string().orEmpty())
                    val reader = responseBody?.charStream()?.buffered()
                        ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antwortstream geliefert.")
                    reader.use {
                        readSseData(
                            reader = it,
                            onData = { data -> accumulator.accept(data)?.let { delta -> onDelta(delta) } },
                            shouldStop = { accumulator.isCompleted },
                        )
                    }
                }
                fertig.set(true)
            } catch (error: IOException) {
                if (trackedResponse.call.isCanceled()) {
                    throw CancellationException("Die OpenAI-Anfrage wurde abgebrochen.")
                }
                throw error
            } finally {
                fertig.set(true)
                waechter.cancel()
                activeQuestionCall.clear(trackedResponse.call)
            }
        }
        // Prüft, ob die Antwort vollständig ist; das Ergebnis selbst holt der Aufrufer.
        accumulator.result()
        return accumulator
    }

    /** Wie [requestCodexAccumulated], aber für ein Claude-Modell über die Anthropic Messages API. */
    private suspend fun requestAnthropic(
        instructions: String,
        turns: List<ChatTurn>,
        model: CodexModel,
        reasoningEffort: ReasoningEffort,
        onDelta: suspend (String) -> Unit,
    ): String {
        val key = anthropicSchluessel().ifBlank {
            throw CodexAuthException(AuthErrorKind.NETWORK, "Für ${model.label} fehlt der Anthropic-API-Schlüssel. Bitte unter Einstellungen → Jarvis' Kopf eintragen.")
        }
        var attempt = 0
        var mitFallback = true
        while (true) {
            var deliveredText = false
            try {
                return requestAnthropicOnce(anthropicChatPayload(instructions, turns, model, reasoningEffort, mitFallback), key, mitFallback && model.mitFallback) { delta ->
                    deliveredText = true
                    onDelta(delta)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                // Lehnt Anthropic die Anfrage mit 400 ab (etwa weil das Konto die Ersatzmodell-Beta nicht kennt), einmal ohne versuchen statt zu scheitern.
                if (!deliveredText && mitFallback && model.mitFallback && error is CodexAuthException &&
                    error.message.orEmpty().startsWith(ANTHROPIC_FEHLER_PREFIX + "400")) { mitFallback = false; continue }
                if (deliveredText || attempt >= TRANSIENT_RETRY_DELAYS_MS.size || !error.isTransient()) throw error
                delay(TRANSIENT_RETRY_DELAYS_MS[attempt])
                attempt++
            }
        }
    }

    private suspend fun requestAnthropicOnce(
        payload: JSONObject,
        key: String,
        mitFallback: Boolean,
        onDelta: suspend (String) -> Unit,
    ): String {
        val builder = Request.Builder()
            .url(ANTHROPIC_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
        if (mitFallback) builder.header("anthropic-beta", ANTHROPIC_FALLBACK_BETA)
        val accumulator = AnthropicSseAccumulator()
        val trackedResponse = executeQuestionRequest(builder.build())
        coroutineScope {
            val fertig = AtomicBoolean(false)
            val waechter = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!fertig.get()) trackedResponse.call.cancel()
                }
            }
            try {
                trackedResponse.response.use { response ->
                    val responseBody = response.body
                    if (!response.isSuccessful) classifyAnthropicError(response.code, responseBody?.string().orEmpty())
                    val reader = responseBody?.charStream()?.buffered()
                        ?: throw CodexAuthException(AuthErrorKind.NETWORK, "Anthropic hat keinen Antwortstream geliefert.")
                    reader.use {
                        readSseData(
                            reader = it,
                            onData = { data -> accumulator.accept(data)?.let { delta -> onDelta(delta) } },
                            shouldStop = { accumulator.isCompleted },
                        )
                    }
                }
                fertig.set(true)
            } catch (error: IOException) {
                if (trackedResponse.call.isCanceled()) throw CancellationException("Die Anthropic-Anfrage wurde abgebrochen.")
                throw error
            } finally {
                fertig.set(true)
                waechter.cancel()
                activeQuestionCall.clear(trackedResponse.call)
            }
        }
        return accumulator.result()
    }

    private suspend fun <T> withDnsRetry(block: suspend () -> T): T {
        var lastError: UnknownHostException? = null
        repeat(DNS_RETRY_DELAYS_MS.size + 1) { attempt ->
            try {
                return block()
            } catch (error: UnknownHostException) {
                lastError = error
                // Gar kein Netz (Flugmodus, Funkloch): nach kurzer Wartezeit gleich melden statt über eine halbe Minute „denkt“.
                // Die langen Wartezeiten bleiben für den Fall, dass ein Netz da ist, aber noch nicht geprüft oder DNS hakt.
                if (!netzVorhanden()) {
                    val da = withTimeoutOrNull(KEIN_NETZ_WARTEN_MS) {
                        while (!netzVorhanden()) delay(NETWORK_POLL_MS)
                        true
                    }
                    if (da == null) {
                        de.frank.jarvis.observability.Logbuch.netz("OpenAI: kein Netz", "Abbruch nach ${KEIN_NETZ_WARTEN_MS / 1000} s ohne Verbindung")
                        throw networkException("Keine Internetverbindung. Bitte WLAN oder Mobilfunk prüfen.", error)
                    }
                }
                if (attempt < DNS_RETRY_DELAYS_MS.size) {
                    awaitValidatedNetwork()
                    delay(DNS_RETRY_DELAYS_MS[attempt])
                }
            }
        }
        throw networkException(
            "OpenAI ist über die aktuelle Netzwerkverbindung noch nicht erreichbar. Bitte prüfe WLAN oder Mobilfunk.",
            lastError,
        )
    }

    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isActive) return
                // Ein abgebrochener Aufruf ist kein Netzwerkfehler: als Abbruch melden, damit
                // er weder wiederholt noch als Fehler angezeigt wird.
                if (call.isCanceled()) continuation.cancel(e) else continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, cancelledResponse, _ -> cancelledResponse.close() }
            }
        })
    }

    private suspend fun awaitForegroundAndNetwork(imVordergrund: () -> Boolean) {
        val ready = withTimeoutOrNull(FOREGROUND_NETWORK_TIMEOUT_MS) {
            while (!imVordergrund() || !hasValidatedNetwork()) {
                delay(NETWORK_POLL_MS)
            }
            true
        } ?: false
        if (!ready) {
            throw networkException(
                "Die Anmeldung wurde autorisiert, aber die App hat noch keine aktive Internetverbindung. Kehre zur App zurück und versuche es erneut.",
            )
        }
    }

    private suspend fun awaitValidatedNetwork() {
        withTimeoutOrNull(DNS_NETWORK_WAIT_MS) {
            while (!hasValidatedNetwork()) delay(NETWORK_POLL_MS)
        }
    }

    /** Irgendein Netz ist angemeldet (ob es schon geprüft ist, egal). */
    private fun netzVorhanden(): Boolean = try {
        connectivityManager.activeNetwork != null
    } catch (error: SecurityException) {
        true
    }

    private fun hasValidatedNetwork(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (error: SecurityException) {
            Log.w(TAG, "Netzwerkstatus nicht verfügbar; HTTP-Aufruf entscheidet über die Verbindung.", error)
            true
        }
    }

    private fun parseJson(value: String, message: String): JSONObject = runCatching { JSONObject(value) }
        .getOrElse { throw CodexAuthException(AuthErrorKind.NETWORK, message, it) }

    private fun jwtAccountId(token: String): String? =
        jwtAuthValue(token, "chatgpt_account_id") ?: jwtAuthValue(token, "account_id")

    private fun jwtEmail(token: String): String? =
        jwtPayload(token)?.optString("email")?.takeIf(String::isNotBlank) ?: jwtAuthValue(token, "email")

    private fun jwtAuthValue(token: String, name: String): String? =
        jwtPayload(token)
            ?.optJSONObject("https://api.openai.com/auth")
            ?.optString(name)
            ?.takeIf(String::isNotBlank)

    private fun jwtPayload(token: String): JSONObject? = runCatching {
        val part = token.split('.')[1]
        val padded = part.padEnd((part.length + 3) / 4 * 4, '=')
        JSONObject(String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8))
    }.getOrNull()

    private fun expiryTime(json: JSONObject): Long =
        System.currentTimeMillis() + json.optLong("expires_in", DEFAULT_TOKEN_LIFETIME_SECONDS) * 1_000L

    private data class HttpResult(val code: Int, val body: String)
    private data class TrackedResponse(val call: Call, val response: Response)

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val DEVICE_USER_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val DEVICE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val DEVICE_VERIFICATION_URL = "https://auth.openai.com/codex/device"
        private const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"
        private const val STORE_NAME = "codex_oauth"
        private const val TAG = "CodexAuth"
        private const val CODEX_CLIENT_VERSION = "0.159.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val AUTH_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
        private const val CODEX_CLOUD_URL = "wss://codex-cloud-backend.chatgpt.com/"
        private const val CODEX_CLOUD_TIMEOUT_MS = 30_000L
        private val CODEX_CLOUD_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
        private val RESPONSES_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            // Ein Modellzug mit Effort „Maximal“ und 128 000 Token kann länger als 8 Minuten streamen.
            .callTimeout(15, TimeUnit.MINUTES)
            .build()
        private const val REFRESH_SKEW_MS = 120_000L
        private const val FOREGROUND_NETWORK_TIMEOUT_MS = 5 * 60_000L
        private const val DEVICE_CODE_LIFETIME_MS = 15 * 60_000L
        private const val DNS_NETWORK_WAIT_MS = 10_000L
        /** So lange wartet eine Anfrage ohne jedes Netz, bevor sie es meldet. */
        private const val KEIN_NETZ_WARTEN_MS = 3_000L
        private const val NETWORK_POLL_MS = 200L
        private const val NETWORK_POLL_BACKOFF_SECONDS = 2
        private const val SLOW_DOWN_SECONDS = 5
        private const val DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600L
        internal const val DEFAULT_DEVICE_POLL_SECONDS = 5
        internal const val MIN_DEVICE_POLL_SECONDS = 3
        internal const val MAX_DEVICE_POLL_SECONDS = 30
        internal val DNS_RETRY_DELAYS_MS = longArrayOf(500L, 1_500L, 3_000L)
        internal val TRANSIENT_RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L, 7_000L)
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_EMAIL = "email"

        /** Eine Sperre für die Token-Erneuerung im ganzen Prozess, über alle Instanzen. */
        private val refreshMutex = Mutex()
        @Volatile private var speicher: SharedPreferences? = null

        private fun gemeinsamerStore(context: Context): SharedPreferences = speicher ?: synchronized(this) {
            speicher ?: oeffneStore(context.applicationContext).also { speicher = it }
        }

        private fun verschluesselterStore(app: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                app,
                STORE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        /**
         * Öffnet den Token-Speicher wie Einstellungen.oeffne: Keystore-Fehler sind oft vorübergehend, also einmal kurz
         * warten und erneut versuchen. Passt der Schlüssel nicht mehr zu den Daten (AEADBadTag, etwa nach einer
         * Wiederherstellung auf einem neuen Gerät), sind die Tokens ohnehin unlesbar: Datei löschen und neu anlegen,
         * dann ist nur eine neue Anmeldung nötig statt eines Absturzes bei jedem Start. Einen Klartext-Ersatz gibt es
         * für Tokens bewusst nicht; scheitert auch das, geht der Fehler weiter (beim nächsten Zugriff neuer Versuch).
         */
        private fun oeffneStore(app: Context): SharedPreferences {
            try { return verschluesselterStore(app) } catch (e: Exception) { Log.w(TAG, "Token-Speicher: erster Versuch fehlgeschlagen", e) }
            SystemClock.sleep(200)
            val fehler = try { return verschluesselterStore(app) } catch (e: Exception) { e }
            if (generateSequence<Throwable>(fehler) { it.cause }.any { it is AEADBadTagException }) {
                runCatching { app.deleteSharedPreferences(STORE_NAME) }
                val neu = verschluesselterStore(app)
                Log.e(TAG, "Token-Speicher war unlesbar und wurde neu angelegt. Bitte erneut bei OpenAI anmelden.", fehler)
                return neu
            }
            throw fehler
        }
    }
}

private fun streamException(error: JSONObject?): CodexAuthException {
    val message = error?.optString("message")?.takeIf(String::isNotBlank)
        ?: "OpenAI hat die Antwort abgebrochen."
    val marker = listOf(error?.optString("code"), error?.optString("type"), message)
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
    val kind = when {
        marker.contains("quota") || marker.contains("rate_limit") -> AuthErrorKind.QUOTA
        marker.contains("invalid_grant") || marker.contains("refresh_token_reused") ||
            marker.contains("unauthorized") || marker.contains("authentication") -> AuthErrorKind.REAUTH
        else -> AuthErrorKind.NETWORK
    }
    return CodexAuthException(kind, message)
}

private fun networkException(message: String, cause: Throwable? = null): CodexAuthException =
    CodexAuthException(AuthErrorKind.NETWORK, message, cause)
