package de.frank.perfectmoment.auth

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.BufferedReader
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
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

internal fun codexQuestionInput(request: CodexQuestionRequest): String = buildString {
    append("Thema dieser Sitzung:\n")
    append(request.topic.trim())
    append("\n\nZusatzkontext aus dem Intro (falls vorhanden):\n")
    append(request.introContext.trim())
    append("\n\nBereits gestellte Fragen dieser Sitzung (nicht wiederholen, auch nicht sinngemäß):\n")
    append(JSONArray(request.previousQuestions.map { it.trim() }).toString())
}

internal fun codexQuestionsPayload(request: CodexQuestionRequest): JSONObject {
    val questionSchema = JSONObject()
        .put("type", "array")
        .put("minItems", QuestionResponseValidator.REQUIRED_INPUT_COUNT)
        .put("maxItems", QuestionResponseValidator.REQUIRED_INPUT_COUNT)
        .put("items", JSONObject().put("type", "string"))
    val schema = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("required", JSONArray().put("fragen"))
        .put("properties", JSONObject().put("fragen", questionSchema))
    val excludedEntranceQuestions = listOf(IntroQuestionPolicy.QUESTION, request.entranceQuestion)
        .filter(String::isNotBlank)
        .distinct()
    val perspectiveInstruction = when (request.perspective) {
        QuestionPerspective.FIRST_PERSON ->
            "Formuliere ausnahmslos jede Frage in der Ich-Person, als würde der Hörer sie sich selbst stellen. " +
                "Beispiel: 'Warum genieße ich jeden Tag jedes Hier und Jetzt?'"
        QuestionPerspective.SECOND_PERSON ->
            "Formuliere ausnahmslos jede Frage in der Du-Person und sprich den Hörer direkt an. " +
                "Beispiel: 'Warum genießt du jeden Tag jedes Hier und Jetzt?'"
    }
    val instructions = request.skillText.trimEnd() + "\n\n" + request.operatingModeText.trim() +
        "\n\nDiese Perspektivvorgabe hat Vorrang vor allen widersprüchlichen Angaben: $perspectiveInstruction" +
        "\n\nDiese Eingangsfragen sind nur Eingabehilfen und dürfen niemals als erzeugte Frage erscheinen: " +
        JSONArray(excludedEntranceQuestions).toString()

    return JSONObject()
        .put("model", request.model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put("instructions", instructions)
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", codexQuestionInput(request)),
            ),
        )
        .put("reasoning", JSONObject().put("effort", request.reasoningEffort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "perfect_moment_fragen")
                    .put("strict", true)
                    .put("schema", schema),
            ),
        )
}

internal fun codexPerspectiveRewritePayload(
    questions: List<String>,
    perspective: QuestionPerspective,
    model: CodexModel,
    reasoningEffort: ReasoningEffort,
): JSONObject {
    val questionSchema = JSONObject()
        .put("type", "array")
        .put("minItems", questions.size)
        .put("maxItems", questions.size)
        .put("items", JSONObject().put("type", "string"))
    val schema = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("required", JSONArray().put("fragen"))
        .put("properties", JSONObject().put("fragen", questionSchema))
    val target = when (perspective) {
        QuestionPerspective.FIRST_PERSON ->
            "die Ich-Person, als würde der Hörer jede Frage sich selbst stellen"
        QuestionPerspective.SECOND_PERSON ->
            "die Du-Person, die den Hörer direkt anspricht"
    }
    return JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put(
            "instructions",
            "Formuliere jede deutsche Frage grammatikalisch natürlich in $target um. Erhalte Bedeutung, " +
                "Reihenfolge und Anzahl exakt. Ergänze keine Antworten, Erklärungen, Nummern oder Emojis.",
        )
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", "Umzuformulierende Fragen:\n${JSONArray(questions).toString()}"),
            ),
        )
        .put("reasoning", JSONObject().put("effort", reasoningEffort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "perfect_moment_perspektive")
                    .put("strict", true)
                    .put("schema", schema),
            ),
        )
}

internal fun parsePerspectiveQuestions(rawOutput: String, expectedCount: Int): List<String> {
    val output = rawOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    val json = runCatching { JSONObject(output) }.getOrElse {
        throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keine gültige Perspektivantwort geliefert.", it)
    }
    val questions = json.optJSONArray("fragen")
        ?: throw CodexAuthException(AuthErrorKind.NETWORK, "In der OpenAI-Perspektivantwort fehlt das Feld „fragen“.")
    if (questions.length() != expectedCount) {
        throw CodexAuthException(
            AuthErrorKind.NETWORK,
            "OpenAI hat ${questions.length()} statt $expectedCount umformulierte Fragen geliefert.",
        )
    }
    return (0 until questions.length()).map { index ->
        (questions.opt(index) as? String)?.trim()?.takeIf(String::isNotEmpty)
            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat eine leere Perspektivfrage geliefert.")
    }
}

internal fun codexSessionPromptPayload(
    prompt: String,
    model: CodexModel,
    reasoningEffort: ReasoningEffort,
): JSONObject {
    val schema = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("required", JSONArray().put("requires_answer").put("question"))
        .put(
            "properties",
            JSONObject()
                .put("requires_answer", JSONObject().put("type", "boolean"))
                .put("question", JSONObject().put("type", "string")),
        )
    return JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put(
            "instructions",
            "Prüfe, ob der Text zuerst eine Rückfrage an den Nutzer verlangt und die eigentliche Routine " +
                "ausdrücklich erst nach dessen Antwort beginnen soll. Erkenne jede Formulierung semantisch, " +
                "nicht nur Schlüsselwörter wie 'frage mich zuerst'. Beispiele sind Fragen nach einem Gefühl, " +
                "Thema, Wunsch oder Ziel mit anschließendem Start der Routine. Dann requires_answer=true und " +
                "question enthält genau die eine natürliche Rückfrage ohne Anführungszeichen. Bei einer normalen " +
                "Frage, einem Thema oder Arbeitsauftrag gilt requires_answer=false und question ist leer.",
        )
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", "Zu klassifizierender Text:\n${prompt.trim()}"),
            ),
        )
        .put("reasoning", JSONObject().put("effort", reasoningEffort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "perfect_moment_startentscheidung")
                    .put("strict", true)
                    .put("schema", schema),
            ),
        )
}

internal fun parseSessionPromptDecision(rawOutput: String): SessionPromptDecision {
    val output = rawOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    val json = runCatching { JSONObject(output) }.getOrElse {
        throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keine gültige Startentscheidung geliefert.", it)
    }
    val requiresAnswer = json.opt("requires_answer") as? Boolean
        ?: throw CodexAuthException(AuthErrorKind.NETWORK, "In der OpenAI-Startentscheidung fehlt die Rückfrage-Kennzeichnung.")
    val question = json.optString("question").trim().trim('„', '“', '"')
    if (requiresAnswer && question.isBlank()) {
        throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat die erforderliche Rückfrage nicht geliefert.")
    }
    return SessionPromptDecision(requiresAnswer, question.takeIf { requiresAnswer }.orEmpty())
}

internal class CodexSseAccumulator(private val decodeQuestions: Boolean = true) {
    private val deltas = StringBuilder()
    private val questionDecoder = StreamingQuestionDecoder().takeIf { decodeQuestions }
    private var completedText: String? = null
    private var completed = false

    val isCompleted: Boolean get() = completed

    fun accept(data: String): List<String> {
        if (data.isBlank() || data == "[DONE]") return emptyList()
        val event = runCatching { JSONObject(data) }.getOrElse {
            throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat ein ungültiges Streaming-Ereignis geliefert.", it)
        }
        return when (event.optString("type")) {
            "response.output_text.delta" -> event.optString("delta").takeIf(String::isNotEmpty)?.let { delta ->
                deltas.append(delta)
                questionDecoder?.append(delta).orEmpty()
            }.orEmpty()
            "response.completed" -> {
                completed = true
                completedText = event.optJSONObject("response")?.let(::extractOutputText)
                emptyList()
            }
            "response.incomplete" -> throw CodexAuthException(
                AuthErrorKind.NETWORK,
                "OpenAI hat die Antwort unvollständig beendet.",
            )
            "response.failed" -> throw streamException(event.optJSONObject("response")?.optJSONObject("error"))
            "error" -> throw streamException(event.optJSONObject("error") ?: event)
            else -> emptyList()
        }
    }

    fun result(): String {
        if (!completed) {
            throw CodexAuthException(AuthErrorKind.NETWORK, "Die OpenAI-Verbindung endete vor dem Abschluss der Antwort.")
        }
        return completedText?.takeIf(String::isNotBlank)
            ?: deltas.toString().takeIf(String::isNotBlank)
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
    private val refreshMutex = Mutex()
    private val loginMutex = Mutex()
    private val activeLoginJob = AtomicReference<Job?>()
    private val activeQuestionCall = ActiveCallTracker()
    private val store by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            STORE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val email: String? get() = store.getString(KEY_EMAIL, null)
    val accountId: String? get() = store.getString(KEY_ACCOUNT_ID, null)
    val isConnected: Boolean get() = store.contains(KEY_ACCESS_TOKEN)

    suspend fun login(
        activity: ComponentActivity,
        onDeviceCode: (DeviceAuthInfo) -> Unit,
    ): AuthResult = loginMutex.withLock {
        val loginJob = currentCoroutineContext().job
        activeLoginJob.set(loginJob)
        try {
            withContext(Dispatchers.IO) { loginInternal(activity, onDeviceCode) }
        } catch (error: IOException) {
            throw networkException("Die OpenAI-Anmeldung ist wegen eines Netzwerkfehlers fehlgeschlagen.", error)
        } finally {
            activeLoginJob.compareAndSet(loginJob, null)
        }
    }

    fun cancelLogin() {
        activeLoginJob.getAndSet(null)?.cancel()
    }

    fun cancelQuestionGeneration() {
        activeQuestionCall.cancel()
    }

    suspend fun generateQuestions(
        request: CodexQuestionRequest,
        onQuestion: suspend (String) -> Unit = {},
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            if (request.topic.isBlank()) {
                throw CodexAuthException(AuthErrorKind.NETWORK, "Für die Fragenerzeugung fehlt das Sitzungsthema.")
            }
            parseQuestionResponse(requestCodexResponse(codexQuestionsPayload(request), true, onQuestion))
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw networkException("Die OpenAI-Antwort konnte nicht vollständig empfangen werden.", error)
        }
    }

    suspend fun classifySessionPrompt(
        prompt: String,
        model: CodexModel,
        reasoningEffort: ReasoningEffort,
    ): SessionPromptDecision = withContext(Dispatchers.IO) {
        try {
            if (prompt.isBlank()) {
                throw CodexAuthException(AuthErrorKind.NETWORK, "Für die Startentscheidung fehlt die Eingabe.")
            }
            parseSessionPromptDecision(
                requestCodexResponse(codexSessionPromptPayload(prompt, model, reasoningEffort), false),
            )
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw networkException("Die OpenAI-Startentscheidung konnte nicht vollständig empfangen werden.", error)
        }
    }

    suspend fun rewriteQuestionsForPerspective(
        questions: List<String>,
        perspective: QuestionPerspective,
        model: CodexModel,
        reasoningEffort: ReasoningEffort,
    ): List<String> = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext emptyList()
        try {
            parsePerspectiveQuestions(
                requestCodexResponse(
                    codexPerspectiveRewritePayload(questions, perspective, model, reasoningEffort),
                    false,
                ),
                questions.size,
            )
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw networkException("Die Fragen konnten nicht in die gewählte Perspektive übertragen werden.", error)
        }
    }

    fun logout() {
        store.edit().clear().apply()
    }

    private suspend fun loginInternal(
        activity: ComponentActivity,
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
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deviceInfo.verificationUri)))
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
                    awaitForegroundAndNetwork(activity)
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

        return refreshMutex.withLock {
            val tokenAfterLock = store.getString(KEY_ACCESS_TOKEN, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
            if (!tokenNeedsRefresh()) return@withLock tokenAfterLock
            val refreshToken = store.getString(KEY_REFRESH_TOKEN, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
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
                .apply()
            newAccessToken
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

    private suspend fun requestCodexResponse(
        payload: JSONObject,
        decodeQuestions: Boolean,
        onQuestion: suspend (String) -> Unit = {},
    ): String {
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
            .header("User-Agent", "codex_cli_rs/0.0.0 (Perfect Moment)")
            .header("ChatGPT-Account-ID", requestAccountId)
            .build()
        val accumulator = CodexSseAccumulator(decodeQuestions)
        val trackedResponse = executeQuestionRequest(httpRequest)
        try {
            trackedResponse.response.use { response ->
                val responseBody = response.body
                if (!response.isSuccessful) classifyHttpError(response.code, responseBody?.string().orEmpty())
                val reader = responseBody?.charStream()?.buffered()
                    ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antwortstream geliefert.")
                reader.use {
                    readSseData(
                        reader = it,
                        onData = { data -> accumulator.accept(data).forEach { question -> onQuestion(question) } },
                        shouldStop = { accumulator.isCompleted },
                    )
                }
            }
        } finally {
            activeQuestionCall.clear(trackedResponse.call)
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
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, cancelledResponse, _ -> cancelledResponse.close() }
            }
        })
    }

    private suspend fun awaitForegroundAndNetwork(activity: ComponentActivity) {
        val ready = withTimeoutOrNull(FOREGROUND_NETWORK_TIMEOUT_MS) {
            while (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !hasValidatedNetwork()) {
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

    private fun parseQuestionResponse(rawOutput: String): List<String> {
        val output = rawOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = parseJson(output, "OpenAI hat kein gültiges strukturiertes Fragen-JSON geliefert.")
        val questions = json.optJSONArray("fragen")
            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "In der OpenAI-Antwort fehlt das Feld „fragen“.")
        if (questions.length() != QuestionResponseValidator.REQUIRED_INPUT_COUNT) {
            throw CodexAuthException(
                AuthErrorKind.NETWORK,
                "OpenAI hat ${questions.length()} statt genau ${QuestionResponseValidator.REQUIRED_INPUT_COUNT} Fragen geliefert.",
            )
        }
        return (0 until questions.length()).map { index ->
            val value = questions.opt(index)
            if (value !is String) {
                throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat im Feld „fragen“ einen Nicht-Text-Eintrag geliefert.")
            }
            value
        }
    }

    private fun parseJson(value: String, message: String): JSONObject = runCatching { JSONObject(value) }
        .getOrElse { throw CodexAuthException(AuthErrorKind.NETWORK, message, it) }

    private fun classifyHttpError(code: Int, body: String) {
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
            else -> throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI-Fehler $code: $message")
        }
    }

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
        private const val STORE_NAME = "codex_oauth"
        private const val TAG = "CodexAuth"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val AUTH_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
        private val RESPONSES_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()
        private const val REFRESH_SKEW_MS = 120_000L
        private const val FOREGROUND_NETWORK_TIMEOUT_MS = 5 * 60_000L
        private const val DEVICE_CODE_LIFETIME_MS = 15 * 60_000L
        private const val DNS_NETWORK_WAIT_MS = 10_000L
        private const val NETWORK_POLL_MS = 200L
        private const val NETWORK_POLL_BACKOFF_SECONDS = 2
        private const val SLOW_DOWN_SECONDS = 5
        private const val DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600L
        internal const val DEFAULT_DEVICE_POLL_SECONDS = 5
        internal const val MIN_DEVICE_POLL_SECONDS = 3
        internal const val MAX_DEVICE_POLL_SECONDS = 30
        internal val DNS_RETRY_DELAYS_MS = longArrayOf(500L, 1_500L, 3_000L)
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_EMAIL = "email"
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
