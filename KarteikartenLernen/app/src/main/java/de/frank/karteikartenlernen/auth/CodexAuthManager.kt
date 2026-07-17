package de.frank.karteikartenlernen.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.StringReader
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

data class AuthResult(val email: String?)
data class DeviceAuthInfo(val userCode: String, val verificationUri: String)
data class ExistingSessionContext(val id: String, val title: String, val question: String, val answerExcerpt: String)
data class GeneratedCard(
    val question: String,
    val answer: String,
    val explanation: String,
    val targetSessionIds: List<String> = emptyList(),
)
data class GeneratedResearch(val title: String, val answer: String, val cards: List<GeneratedCard>)
private data class GeneratedSource(val title: String, val url: String)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }
class CodexAuthException(val kind: AuthErrorKind, message: String) : Exception(message)
internal enum class DevicePollAction { PROCESS, PENDING, FAIL }

internal fun devicePollAction(statusCode: Int): DevicePollAction = when {
    statusCode == 200 -> DevicePollAction.PROCESS
    statusCode == 403 || statusCode == 404 || statusCode == 429 || statusCode >= 500 -> DevicePollAction.PENDING
    else -> DevicePollAction.FAIL
}

internal fun devicePollInterval(value: Any?): Int =
    (value?.toString()?.toIntOrNull() ?: CodexAuthManager.DEFAULT_DEVICE_POLL_SECONDS)
        .coerceAtLeast(CodexAuthManager.MIN_DEVICE_POLL_SECONDS)

internal fun codexInput(question: String): JSONArray = JSONArray().put(
    JSONObject()
        .put("role", "user")
        .put("content", question),
)

internal class CodexSseAccumulator(private val onOutputDelta: (String) -> Unit = {}) {
    private val deltas = StringBuilder()
    private var completedText: String? = null
    private var completed = false
    val isCompleted: Boolean get() = completed

    fun accept(data: String) {
        if (data.isBlank() || data == "[DONE]") return
        val event = runCatching { JSONObject(data) }.getOrElse {
            throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat ein ungültiges Streaming-Ereignis geliefert.")
        }
        when (event.optString("type")) {
            "response.output_text.delta" -> event.optString("delta").takeIf(String::isNotEmpty)?.let { delta ->
                deltas.append(delta)
                onOutputDelta(delta)
            }
            "response.completed" -> {
                completed = true
                completedText = event.optJSONObject("response")?.let(::extractOutputText)
            }
            "response.incomplete" -> throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat die Antwort unvollständig beendet.")
            "response.failed" -> {
                val message = event.optJSONObject("response")?.optJSONObject("error")?.optString("message")
                    ?.takeIf(String::isNotBlank) ?: "OpenAI hat die Antwort abgebrochen."
                throw CodexAuthException(AuthErrorKind.NETWORK, message)
            }
            "error" -> {
                val message = event.optString("message").takeIf(String::isNotBlank)
                    ?: event.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
                    ?: "OpenAI hat einen unbekannten Streaming-Fehler gemeldet."
                throw CodexAuthException(AuthErrorKind.NETWORK, message)
            }
        }
    }

    fun result(): String {
        if (!completed) throw CodexAuthException(AuthErrorKind.NETWORK, "Die OpenAI-Verbindung endete vor dem Abschluss der Antwort.")
        return completedText?.takeIf(String::isNotBlank)
            ?: deltas.toString().takeIf(String::isNotBlank)
            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antworttext geliefert.")
    }
}

internal fun readSseData(
    reader: BufferedReader,
    onData: (String) -> Unit,
    shouldStop: () -> Boolean = { false },
) {
    val dataLines = mutableListOf<String>()
    fun dispatch() {
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

internal fun parseCodexSse(body: String): String {
    val accumulator = CodexSseAccumulator()
    readSseData(BufferedReader(StringReader(body)), accumulator::accept)
    return accumulator.result()
}

internal fun extractOutputText(json: JSONObject): String? {
    json.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val output = json.optJSONArray("output") ?: return null
    val text = StringBuilder()
    for (i in 0 until output.length()) {
        val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
        for (j in 0 until content.length()) {
            val item = content.optJSONObject(j) ?: continue
            (item.optString("text").takeIf(String::isNotBlank)
                ?: item.optString("output_text").takeIf(String::isNotBlank))?.let(text::append)
        }
    }
    return text.toString().takeIf(String::isNotBlank)
}

internal class StreamingAnswerDecoder {
    private var depth = 0
    private var inString = false
    private var escapedInString = false
    private var capturingKey = false
    private var expectingKey = false
    private var expectingColon = false
    private var expectingAnswerQuote = false
    private val key = StringBuilder()
    private var pendingKey: String? = null
    private var inAnswer = false
    private var answerEscape = false
    private var unicodeDigitsRemaining = 0
    private var unicodeValue = 0
    private var pendingHighSurrogate: Char? = null
    var isComplete: Boolean = false
        private set

    fun accept(chunk: String): String {
        if (isComplete || chunk.isEmpty()) return ""
        val decoded = StringBuilder()
        chunk.forEach { char ->
            if (isComplete) return@forEach
            if (inAnswer) {
                consumeAnswerCharacter(char, decoded)
                return@forEach
            }
            if (expectingAnswerQuote) {
                if (char.isWhitespace()) return@forEach
                if (char == '"') {
                    expectingAnswerQuote = false
                    inAnswer = true
                }
                return@forEach
            }
            if (inString) {
                when {
                    escapedInString -> {
                        if (capturingKey) key.append(char)
                        escapedInString = false
                    }
                    char == '\\' -> escapedInString = true
                    char == '"' -> {
                        inString = false
                        if (capturingKey) {
                            pendingKey = key.toString()
                            expectingColon = true
                            capturingKey = false
                        }
                    }
                    capturingKey -> key.append(char)
                }
                return@forEach
            }
            if (expectingColon) {
                if (char.isWhitespace()) return@forEach
                if (char == ':') {
                    expectingColon = false
                    if (pendingKey == "answer") expectingAnswerQuote = true
                    pendingKey = null
                }
                return@forEach
            }
            when (char) {
                '{', '[' -> {
                    depth++
                    if (depth == 1) expectingKey = true
                }
                '}', ']' -> depth--
                '"' -> {
                    inString = true
                    capturingKey = depth == 1 && expectingKey
                    if (capturingKey) {
                        key.setLength(0)
                        expectingKey = false
                    }
                }
                ',' -> if (depth == 1) expectingKey = true
            }
        }
        return decoded.toString()
    }

    private fun consumeAnswerCharacter(char: Char, decoded: StringBuilder) {
        if (unicodeDigitsRemaining > 0) {
            val digit = char.digitToIntOrNull(16)
            if (digit == null) {
                unicodeDigitsRemaining = 0
                appendDecoded(char, decoded)
            } else {
                unicodeValue = (unicodeValue shl 4) or digit
                unicodeDigitsRemaining--
                if (unicodeDigitsRemaining == 0) appendDecoded(unicodeValue.toChar(), decoded)
            }
            return
        }
        if (answerEscape) {
            answerEscape = false
            when (char) {
                'u' -> {
                    unicodeDigitsRemaining = 4
                    unicodeValue = 0
                }
                '"', '\\', '/' -> appendDecoded(char, decoded)
                'b' -> appendDecoded('\b', decoded)
                'f' -> appendDecoded('\u000C', decoded)
                'n' -> appendDecoded('\n', decoded)
                'r' -> appendDecoded('\r', decoded)
                't' -> appendDecoded('\t', decoded)
                else -> appendDecoded(char, decoded)
            }
            return
        }
        when (char) {
            '\\' -> answerEscape = true
            '"' -> {
                pendingHighSurrogate?.let(decoded::append)
                pendingHighSurrogate = null
                inAnswer = false
                isComplete = true
            }
            else -> appendDecoded(char, decoded)
        }
    }

    private fun appendDecoded(char: Char, decoded: StringBuilder) {
        val pending = pendingHighSurrogate
        when {
            pending != null && char.isLowSurrogate() -> {
                decoded.append(pending).append(char)
                pendingHighSurrogate = null
            }
            pending != null -> {
                decoded.append(pending)
                pendingHighSurrogate = null
                if (char.isHighSurrogate()) pendingHighSurrogate = char else decoded.append(char)
            }
            char.isHighSurrogate() -> pendingHighSurrogate = char
            else -> decoded.append(char)
        }
    }
}

internal class ThrottledAnswerEmitter(
    private val onDelta: (String) -> Unit,
    private val onComplete: () -> Unit,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val decoder = StreamingAnswerDecoder()
    private val pending = StringBuilder()
    private var published = false
    private var lastPublishMs = 0L
    private var completionSent = false

    fun accept(rawOutputDelta: String) {
        val decoded = decoder.accept(rawOutputDelta)
        if (decoded.isNotEmpty()) pending.append(decoded)
        val now = nowMs()
        if (pending.isNotEmpty() && (
                !published && pending.any(Char::isLetterOrDigit) ||
                    published && now - lastPublishMs >= STREAM_UI_INTERVAL_MS
                )) {
            flush(now)
        }
        if (decoder.isComplete && !completionSent) {
            flush(now)
            completionSent = true
            onComplete()
        }
    }

    fun finish() {
        flush(nowMs())
    }

    private fun flush(now: Long) {
        if (pending.isEmpty()) return
        onDelta(pending.toString())
        pending.setLength(0)
        published = true
        lastPublishMs = now
    }

    private companion object {
        const val STREAM_UI_INTERVAL_MS = 90L
    }
}

internal val RESEARCH_MODELS = mapOf(
    "GPT 5.6 Sol" to "gpt-5.6-sol",
    "GPT 5.6 Terra" to "gpt-5.6-terra",
    "GPT 5.6 Luna" to "gpt-5.6-luna",
)

internal fun codexModelId(label: String): String = RESEARCH_MODELS[label] ?: RESEARCH_MODELS.getValue("GPT 5.6 Terra")

internal fun codexReasoningEffort(label: String): String = when (label.trim().lowercase()) {
    "niedrig", "low" -> "low"
    "hoch", "high" -> "high"
    else -> "medium"
}

internal fun researchInstructions(): String = """
    Nutze die Websuche aktiv. Beantworte die Frage nur mit fachlich zuverlässigen und möglichst aktuellen Informationen.
    Schreibe die gesamte Ausgabe auf Deutsch für Menschen auf dem Niveau der 10. Klasse. Verwende kurze, klare Sätze,
    aktive Formulierungen und einfaches Deutsch. Das gilt auch für Erklärungen und Beschreibungen auf den Karteikarten.
    Vermeide unnötige Fremdwörter. Wenn ein Fachwort oder Fremdwort nötig ist, erkläre es beim ersten Auftreten sofort
    in einfachen Worten. Setze kein Vorwissen über das Thema voraus.

    Erzeuge einen kurzen, eindeutigen Sessiontitel. Die Lernantwort muss zwischen 1.500 und 5.000 Wörtern lang sein.
    Wähle innerhalb dieser Spanne die kürzeste Länge, die das Thema trotzdem vollständig und gründlich erklärt:
    einfache Fragen bleiben näher an 1.500 Wörtern, nur wirklich komplexe Themen benötigen deutlich mehr Text.
    Gliedere sie mit aussagekräftigen Markdown-Überschriften der Ebene 2 im Format "## Überschrift". Unter jeder
    Überschrift folgt ein zusammenhängender Absatz, der ungefähr 7 bis 13 Bildschirmzeilen beziehungsweise meist
    90 bis 150 Wörter umfasst. Trenne Absätze durch eine Leerzeile. Verwende keine Tabellen und keine Stichpunktwüsten.

    Erzeuge abhängig von Umfang und Verständnisdichte des Themas automatisch 30 bis 70 eigenständige Karteikarten.
    Decke die wichtigen Zusammenhänge ab, ohne Karten künstlich aufzufüllen oder Inhalte doppelt abzufragen. Jede
    Kartenfrage prüft echtes Verständnis. Jede Antwort ist klar und knapp. Jede Erklärung beschreibt in einfachem
    Deutsch, warum die Antwort stimmt, und erklärt darin vorkommende Fach- oder Fremdwörter.

    Prüfe für jede Karte alle im Benutzertext aufgeführten bestehenden Sessions. Trage in targetSessionIds nur IDs von
    Sessions ein, zu denen genau diese Karte fachlich wirklich passt und dort einen klaren Lernwert hat. Ein ähnliches
    Einzelwort reicht nicht. Eine Karte darf zu mehreren Sessions passen oder zu keiner; ein leeres Array ist besser
    als ein schwacher Vorschlag. Erfinde keine Session-ID und ordne nichts der neu erzeugten Session zu.

    Gib zusätzlich 3 bis 12 tatsächlich verwendete Webquellen mit präzisem Titel und vollständiger URL zurück.
""".trimIndent()

internal fun researchInput(question: String, sessions: List<ExistingSessionContext>): String = buildString {
    append("Frage für die neue Recherche:\n")
    append(question.trim())
    append("\n\nBestehende Sessions, die für jede neue Karte vollständig geprüft werden müssen:\n")
    if (sessions.isEmpty()) {
        append("Keine bestehenden Sessions. targetSessionIds muss bei jeder Karte leer sein.")
    } else {
        append(JSONArray(sessions.map { session ->
            JSONObject()
                .put("id", session.id)
                .put("title", session.title)
                .put("lastQuestion", session.question)
                .put("answerExcerpt", session.answerExcerpt)
        }).toString())
    }
}

internal fun codexResearchPayload(
    model: String,
    reasoning: String,
    question: String,
    sessions: List<ExistingSessionContext> = emptyList(),
): JSONObject = JSONObject().apply {
    put("model", model)
    put("service_tier", "priority")
    put("stream", true)
    put("store", false)
    put("instructions", researchInstructions())
    put("input", codexInput(researchInput(question, sessions)))
    put("reasoning", JSONObject().put("effort", codexReasoningEffort(reasoning)))
    put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
    put("tool_choice", "auto")
    put("text", structuredResearchOutputFormat())
}

internal fun structuredResearchOutputFormat(): JSONObject {
    val card = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("properties", JSONObject()
            .put("question", JSONObject().put("type", "string"))
            .put("answer", JSONObject().put("type", "string"))
            .put("explanation", JSONObject().put("type", "string"))
            .put("targetSessionIds", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string"))))
        .put("required", JSONArray(listOf("question", "answer", "explanation", "targetSessionIds")))
    val source = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("properties", JSONObject()
            .put("title", JSONObject().put("type", "string"))
            .put("url", JSONObject().put("type", "string")))
        .put("required", JSONArray(listOf("title", "url")))
    val schema = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("properties", JSONObject()
            .put("answer", JSONObject().put("type", "string"))
            .put("title", JSONObject().put("type", "string"))
            .put("cards", JSONObject().put("type", "array").put("minItems", 30).put("maxItems", 70).put("items", card))
            .put("sources", JSONObject().put("type", "array").put("minItems", 3).put("maxItems", 12).put("items", source)))
        .put("required", JSONArray(listOf("title", "answer", "cards", "sources")))
    return JSONObject().put("format", JSONObject()
        .put("type", "json_schema")
        .put("name", "karteikarten_research")
        .put("strict", true)
        .put("schema", schema))
}

internal fun researchWordCount(text: String): Int = text.trim()
    .split(Regex("\\s+"))
    .count(String::isNotBlank)

class CodexAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val store = EncryptedSharedPreferences.create(
        appContext,
        "codex_oauth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    val email: String? get() = store.getString(KEY_EMAIL, null)
    val isConnected: Boolean get() = store.contains(KEY_ACCESS_TOKEN)

    suspend fun login(activity: ComponentActivity, onDeviceCode: (DeviceAuthInfo) -> Unit): AuthResult = withContext(Dispatchers.IO) {
        val startConnection = postJsonWithDnsRetry(
            DEVICE_USER_CODE_URL,
            JSONObject().put("client_id", CLIENT_ID).toString(),
        )
        val startBody = startConnection.readBody()
        classifyHttpError(startConnection.responseCode, startBody)
        val start = JSONObject(startBody)
        val userCode = start.optString("user_code").trim()
        val deviceAuthId = start.optString("device_auth_id").trim()
        if (userCode.isEmpty() || deviceAuthId.isEmpty()) {
            throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen vollständigen Gerätecode geliefert.")
        }
        var intervalSeconds = devicePollInterval(start.opt("interval"))
        val info = DeviceAuthInfo(userCode, DEVICE_VERIFICATION_URL)
        withContext(Dispatchers.Main) {
            onDeviceCode(info)
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.verificationUri)))
        }
        val expiresAt = System.currentTimeMillis() + DEVICE_CODE_LIFETIME_MS
        while (System.currentTimeMillis() < expiresAt) {
            delay(intervalSeconds * 1_000L)
            val pollConnection = try {
                postJsonWithDnsRetry(
                    DEVICE_TOKEN_URL,
                    JSONObject().put("device_auth_id", deviceAuthId).put("user_code", userCode).toString(),
                )
            } catch (_: CodexAuthException) {
                intervalSeconds = (intervalSeconds + NETWORK_POLL_BACKOFF_SECONDS).coerceAtMost(MAX_DEVICE_POLL_SECONDS)
                continue
            }
            val pollBody = pollConnection.readBody()
            when (devicePollAction(pollConnection.responseCode)) {
                DevicePollAction.PENDING -> {
                    if (pollConnection.responseCode == 429 || pollBody.contains("slow_down", true)) {
                        intervalSeconds = (intervalSeconds + SLOW_DOWN_SECONDS).coerceAtMost(MAX_DEVICE_POLL_SECONDS)
                    }
                }
                DevicePollAction.FAIL -> classifyHttpError(pollConnection.responseCode, pollBody)
                DevicePollAction.PROCESS -> {
                    val authorization = JSONObject(pollBody)
                    val code = authorization.optString("authorization_code").trim()
                    val verifier = authorization.optString("code_verifier").trim()
                    if (code.isEmpty() || verifier.isEmpty()) {
                        throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat den Gerätecode bestätigt, aber keinen vollständigen Anmeldecode geliefert.")
                    }
                    awaitForegroundAndNetwork(activity)
                    return@withContext exchangeCode(code, verifier, DEVICE_REDIRECT_URI)
                }
            }
        }
        throw CodexAuthException(AuthErrorKind.REAUTH, "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.")
    }

    fun cancelLogin() = Unit

    suspend fun generateResearch(
        model: String,
        reasoning: String,
        question: String,
        sessions: List<ExistingSessionContext>,
        onAnswerDelta: (String) -> Unit = {},
        onAnswerComplete: () -> Unit = {},
    ): GeneratedResearch =
        withContext(Dispatchers.IO) {
            val startedAt = System.nanoTime()
            val token = validAccessToken()
            val accountId = jwtClaim(token, "chatgpt_account_id")
                ?: store.getString(KEY_ACCOUNT_ID, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
            val payload = codexResearchPayload(model, reasoning, question, sessions)
            val payloadText = payload.toString()
            val request = Request.Builder()
                .url(RESPONSES_URL)
                .post(payloadText.toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer $token")
                .header("originator", "codex_cli_rs")
                .header("User-Agent", "codex_cli_rs/0.0.0 (Karteikarten Lernen)")
                .header("ChatGPT-Account-ID", accountId)
                .build()
            var firstAnswerDelta = true
            val answerEmitter = ThrottledAnswerEmitter(
                onDelta = { delta ->
                    if (firstAnswerDelta) {
                        firstAnswerDelta = false
                        Log.i(PERF_TAG, "first_answer_delta model=$model elapsed_ms=${elapsedMs(startedAt)}")
                    }
                    onAnswerDelta(delta)
                },
                onComplete = {
                    Log.i(PERF_TAG, "answer_field_complete model=$model elapsed_ms=${elapsedMs(startedAt)}")
                    onAnswerComplete()
                },
            )
            val accumulator = CodexSseAccumulator(answerEmitter::accept)
            Log.i(PERF_TAG, "request_start model=$model effort=${codexReasoningEffort(reasoning)} sessions=${sessions.size} payload_bytes=${payloadText.toByteArray().size}")
            RESEARCH_HTTP_CLIENT.newCall(request).consumeSse(startedAt, model, accumulator)
            answerEmitter.finish()
            val output = accumulator.result().trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            Log.i(PERF_TAG, "stream_complete model=$model elapsed_ms=${elapsedMs(startedAt)} output_chars=${output.length}")
            val result = runCatching { JSONObject(output) }.getOrElse {
                throw CodexAuthException(AuthErrorKind.NETWORK, "Das gewählte GPT-Modell hat keine gültigen strukturierten Lerndaten geliefert.")
            }
            val cards = result.getJSONArray("cards")
            val validSessionIds = sessions.mapTo(mutableSetOf(), ExistingSessionContext::id)
            val answer = result.getString("answer").trim()
            val wordCount = researchWordCount(answer)
            if (wordCount !in MIN_RESEARCH_WORDS..MAX_RESEARCH_WORDS) {
                throw CodexAuthException(
                    AuthErrorKind.NETWORK,
                    "OpenAI hat $wordCount statt 1.500 bis 5.000 Wörtern geliefert. Bitte versuche die Recherche erneut.",
                )
            }
            if (cards.length() !in MIN_RESEARCH_CARDS..MAX_RESEARCH_CARDS) {
                throw CodexAuthException(
                    AuthErrorKind.NETWORK,
                    "OpenAI hat ${cards.length()} statt 30 bis 70 Verständnis-Karten geliefert. Bitte versuche die Recherche erneut.",
                )
            }
            val sourceRows = result.getJSONArray("sources")
            val sources = (0 until sourceRows.length()).map { index ->
                val source = sourceRows.getJSONObject(index)
                GeneratedSource(source.getString("title").trim(), source.getString("url").trim())
            }.filter { it.title.isNotBlank() && (it.url.startsWith("https://") || it.url.startsWith("http://")) }
            if (sources.size < MIN_RESEARCH_SOURCES) {
                throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keine ausreichende Liste der verwendeten Webquellen geliefert.")
            }
            val answerWithSources = buildString {
                append(answer)
                append("\n\n## Quellen\n\n")
                append(sources.joinToString("\n") { "- ${it.title}: ${it.url}" })
            }
            GeneratedResearch(
                title = result.getString("title").trim(),
                answer = answerWithSources,
                cards = (0 until cards.length()).map { index ->
                    val card = cards.getJSONObject(index)
                    val targetIds = card.getJSONArray("targetSessionIds")
                    GeneratedCard(
                        question = card.getString("question"),
                        answer = card.getString("answer"),
                        explanation = card.getString("explanation"),
                        targetSessionIds = (0 until targetIds.length())
                            .map(targetIds::getString)
                            .filter(validSessionIds::contains)
                            .distinct(),
                    )
                },
            )
        }

    private suspend fun Call.consumeSse(
        startedAt: Long,
        model: String,
        accumulator: CodexSseAccumulator,
    ): Unit = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, error: java.io.IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!continuation.isActive) return
                        Log.i(PERF_TAG, "headers_received model=$model status=${response.code} elapsed_ms=${elapsedMs(startedAt)}")
                        if (!response.isSuccessful) {
                            classifyHttpError(response.code, response.body?.string().orEmpty())
                        }
                        val reader = response.body?.charStream()?.buffered()
                            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antwortstream geliefert.")
                        reader.use { readSseData(it, accumulator::accept) { accumulator.isCompleted } }
                    }
                    if (continuation.isActive) continuation.resume(Unit)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000L

    fun logout() {
        store.edit().clear().apply()
    }

    private suspend fun exchangeCode(code: String, verifier: String, redirectUri: String): AuthResult {
        val form = formBody(
            "grant_type" to "authorization_code",
            "client_id" to CLIENT_ID,
            "code" to code,
            "redirect_uri" to redirectUri,
            "code_verifier" to verifier,
        )
        val connection = postFormWithDnsRetry(TOKEN_URL, form)
        val response = connection.readBody()
        classifyHttpError(connection.responseCode, response)
        val json = JSONObject(response)
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank)
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val foundEmail = idToken?.let { jwtValue(it, "email") } ?: jwtValue(accessToken, "email")
        val accountId = jwtClaim(accessToken, "chatgpt_account_id")
        store.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
                if (foundEmail != null) putString(KEY_EMAIL, foundEmail)
                if (accountId != null) putString(KEY_ACCOUNT_ID, accountId)
            }
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000)
            .apply()
        return AuthResult(foundEmail)
    }

    private suspend fun validAccessToken(): String {
        val token = store.getString(KEY_ACCESS_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
        if (System.currentTimeMillis() < store.getLong(KEY_EXPIRES_AT, 0) - REFRESH_SKEW_MS) return token
        val refreshToken = store.getString(KEY_REFRESH_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
        val connection = postFormWithDnsRetry(
            TOKEN_URL,
            formBody(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to CLIENT_ID,
            ),
        )
        val response = connection.readBody()
        classifyHttpError(connection.responseCode, response)
        val json = JSONObject(response)
        val newAccessToken = json.getString("access_token")
        val edit = store.edit()
            .putString(KEY_ACCESS_TOKEN, newAccessToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000)
        json.optString("refresh_token").takeIf(String::isNotBlank)?.let { edit.putString(KEY_REFRESH_TOKEN, it) }
        jwtClaim(newAccessToken, "chatgpt_account_id")?.let { edit.putString(KEY_ACCOUNT_ID, it) }
        edit.apply()
        return newAccessToken
    }

    private fun postForm(url: String, form: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        outputStream.use { it.write(form.toByteArray()) }
    }

    private fun postJson(url: String, json: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        outputStream.use { it.write(json.toByteArray()) }
    }

    private suspend fun postFormWithDnsRetry(url: String, form: String): HttpURLConnection {
        return withDnsRetry { postForm(url, form) }
    }

    private suspend fun postJsonWithDnsRetry(url: String, json: String): HttpURLConnection {
        return withDnsRetry { postJson(url, json) }
    }

    private suspend fun withDnsRetry(block: () -> HttpURLConnection): HttpURLConnection {
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
        throw CodexAuthException(
            AuthErrorKind.NETWORK,
            "OpenAI ist über die aktuelle Netzwerkverbindung noch nicht erreichbar. Bitte prüfe WLAN oder Mobilfunk und versuche die Anmeldung erneut. (${lastError?.message})",
        )
    }

    private suspend fun awaitForegroundAndNetwork(activity: ComponentActivity) {
        val ready = withTimeoutOrNull(FOREGROUND_NETWORK_TIMEOUT_MS) {
            while (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !hasValidatedNetwork()) {
                delay(NETWORK_POLL_MS)
            }
            true
        } ?: false
        if (!ready) {
            throw CodexAuthException(
                AuthErrorKind.NETWORK,
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
            // The following HTTP request remains the authoritative connectivity check.
            Log.e("CodexAuth", "Network-state permission unavailable; falling back to HTTP", error)
            true
        }
    }

    private fun HttpURLConnection.readBody(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun classifyHttpError(code: Int, body: String) {
        if (code in 200..299) return
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
            ?: body.take(240)
        when {
            code == 429 -> throw CodexAuthException(AuthErrorKind.QUOTA, "Dein ChatGPT-/Codex-Kontingent ist aktuell ausgeschöpft. Bitte später erneut versuchen.")
            body.contains("refresh_token_reused", true) -> throw CodexAuthException(AuthErrorKind.REAUTH, "Der Refresh-Token wurde bereits verwendet. Bitte erneut im Browser anmelden.")
            code == 401 || code == 403 || body.contains("invalid_grant", true) -> throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI-Anmeldung ungültig oder nicht mehr zugelassen. Bitte erneut anmelden. $message")
            else -> throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI-Fehler $code: $message")
        }
    }

    private fun jwtClaim(token: String, name: String): String? {
        val payload = jwtPayload(token) ?: return null
        return payload.optJSONObject("https://api.openai.com/auth")?.optString(name)?.takeIf(String::isNotBlank)
    }

    private fun jwtValue(token: String, name: String): String? =
        jwtPayload(token)?.optString(name)?.takeIf(String::isNotBlank)

    private fun jwtPayload(token: String): JSONObject? = runCatching {
        val part = token.split('.')[1]
        JSONObject(String(Base64.getUrlDecoder().decode(part.padEnd((part.length + 3) / 4 * 4, '='))))
    }.getOrNull()

    private fun formBody(vararg values: Pair<String, String>): String = values.joinToString("&") {
        "${URLEncoder.encode(it.first, "UTF-8")}=${URLEncoder.encode(it.second, "UTF-8")}"
    }

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val DEVICE_USER_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val DEVICE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val DEVICE_VERIFICATION_URL = "https://auth.openai.com/codex/device"
        private const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val PERF_TAG = "CodexPerf"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val RESEARCH_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()
        private const val MIN_RESEARCH_WORDS = 1_500
        private const val MAX_RESEARCH_WORDS = 5_000
        private const val MIN_RESEARCH_CARDS = 30
        private const val MAX_RESEARCH_CARDS = 70
        private const val MIN_RESEARCH_SOURCES = 3
        private const val REFRESH_SKEW_MS = 120_000L
        private const val FOREGROUND_NETWORK_TIMEOUT_MS = 5 * 60_000L
        private const val DEVICE_CODE_LIFETIME_MS = 15 * 60_000L
        private const val DNS_NETWORK_WAIT_MS = 10_000L
        private const val NETWORK_POLL_MS = 200L
        internal const val DEFAULT_DEVICE_POLL_SECONDS = 5
        internal const val MIN_DEVICE_POLL_SECONDS = 3
        private const val MAX_DEVICE_POLL_SECONDS = 30
        private const val NETWORK_POLL_BACKOFF_SECONDS = 2
        private const val SLOW_DOWN_SECONDS = 5
        internal val DNS_RETRY_DELAYS_MS = longArrayOf(500L, 1_500L, 3_000L)
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_EMAIL = "email"
    }
}
