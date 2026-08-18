package de.frank.denknotiz.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.denknotiz.data.CodexModel
import de.frank.denknotiz.data.ReasoningEffort
import java.io.BufferedReader
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

data class DeviceCode(val code: String, val verificationUrl: String)
data class WebSource(val title: String, val url: String)
data class AiResult(val text: String, val sources: List<WebSource>)

class CodexException(message: String, val reconnect: Boolean = false, cause: Throwable? = null) : Exception(message, cause)

class CodexClient(context: Context) {
    private val app = context.applicationContext
    private val refreshLock = Mutex()
    private val authStore by lazy {
        EncryptedSharedPreferences.create(
            app,
            "codex_oauth",
            MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val isConnected: Boolean get() = authStore.contains(KEY_ACCESS)
    val email: String? get() = authStore.getString(KEY_EMAIL, null)

    fun disconnect() = authStore.edit().clear().apply()

    suspend fun connect(onCode: (DeviceCode) -> Unit): String? {
        val start = postJson(DEVICE_CODE_URL, JSONObject().put("client_id", CLIENT_ID).toString())
        ensureSuccess(start.code, start.body, false)
        val json = JSONObject(start.body)
        val userCode = json.optString("user_code").trim()
        val authId = json.optString("device_auth_id").trim()
        if (userCode.isBlank() || authId.isBlank()) throw CodexException("OpenAI hat keinen vollständigen Gerätecode geliefert.")
        val deviceCode = DeviceCode(userCode, DEVICE_PAGE)
        withContext(Dispatchers.Main) {
            onCode(deviceCode)
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVICE_PAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        var interval = (json.opt("interval")?.toString()?.toIntOrNull() ?: 5).coerceIn(3, 30)
        val expires = System.currentTimeMillis() + 15 * 60_000L
        while (System.currentTimeMillis() < expires) {
            delay(interval * 1_000L)
            val poll = try {
                postJson(DEVICE_TOKEN_URL, JSONObject().put("device_auth_id", authId).put("user_code", userCode).toString())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                interval = (interval + 2).coerceAtMost(30)
                continue
            }
            when {
                poll.code == 200 -> {
                    val answer = JSONObject(poll.body)
                    return exchange(answer.getString("authorization_code"), answer.getString("code_verifier"))
                }
                poll.code == 403 || poll.code == 404 || poll.code == 429 || poll.code >= 500 -> {
                    if (poll.code == 429) interval = (interval + 5).coerceAtMost(30)
                }
                else -> ensureSuccess(poll.code, poll.body, false)
            }
        }
        throw CodexException("Der OpenAI-Gerätecode ist abgelaufen. Bitte erneut verbinden.")
    }

    suspend fun evaluate(
        chunks: List<String>,
        focus: String,
        profileInstruction: String,
        model: CodexModel,
        reasoning: ReasoningEffort,
        web: Boolean,
        manualToken: String,
    ): AiResult {
        require(chunks.isNotEmpty())
        val partials = mutableListOf<String>()
        val sources = linkedMapOf<String, WebSource>()
        chunks.forEachIndexed { index, chunk ->
            val evidenceRule = if (web) {
                "Nutze die bereitgestellten Denknotizen als Ausgangspunkt und ergänze oder prüfe sie gezielt mit Webquellen. " +
                    "Trenne klar zwischen Notizinhalt und extern recherchierten Fakten."
            } else {
                "Beziehe dich ausschließlich auf die bereitgestellten Denknotizen."
            }
            val result = request(
                instructions = "$profileInstruction\n$evidenceRule " +
                    "Bearbeite Teil ${index + 1} von ${chunks.size}. Erhalte Widersprüche und Unsicherheiten.",
                input = "Fokusfrage: ${focus.ifBlank { "Welche wichtigen Muster und nächsten Schritte ergeben sich?" }}\n\n$chunk",
                model = model, reasoning = reasoning, web = web, manualToken = manualToken,
            )
            partials += result.text
            result.sources.forEach { sources[it.url] = it }
        }
        if (partials.size == 1) return AiResult(partials.single(), sources.values.toList())
        val synthesisInput = partials.mapIndexed { index, value -> "[Teilauswertung ${index + 1}]\n$value" }.joinToString("\n\n")
        val final = request(
            instructions = "$profileInstruction\nFühre alle Teilauswertungen zu einer vollständigen finalen Antwort zusammen. " +
                "Kürze keine relevanten Punkte nur wegen der Länge. Wiederhole nichts unnötig.",
            input = "Fokusfrage: ${focus.ifBlank { "Welche wichtigen Muster und nächsten Schritte ergeben sich?" }}\n\n$synthesisInput",
            model = model, reasoning = reasoning, web = false, manualToken = manualToken,
        )
        final.sources.forEach { sources[it.url] = it }
        return AiResult(final.text, sources.values.toList())
    }

    suspend fun improve(text: String, model: CodexModel, reasoning: ReasoningEffort, manualToken: String): String = request(
        instructions = "Verbessere diesen diktierten Text in derselben Sprache. Korrigiere Grammatik, Satzbau, " +
            "Versprecher, Füllwörter und Wiederholungen. Füge nichts hinzu und lasse keine Aussage weg. Antworte nur mit der Fassung.",
        input = text,
        model = model, reasoning = reasoning, web = false, manualToken = manualToken,
    ).text.trim().trim('„', '“', '"')

    suspend fun title(text: String, model: CodexModel, manualToken: String): String = request(
        instructions = "Formuliere einen präzisen Verlaufstitel mit höchstens 72 Zeichen. Keine Anführungszeichen, keine Erklärung.",
        input = text.take(4_000), model = model, reasoning = ReasoningEffort.LOW, web = false, manualToken = manualToken,
    ).text.replace('\n', ' ').trim().trim('„', '“', '"').take(72)

    private suspend fun request(
        instructions: String,
        input: String,
        model: CodexModel,
        reasoning: ReasoningEffort,
        web: Boolean,
        manualToken: String,
    ): AiResult = withContext(Dispatchers.IO) {
        val token = validToken(manualToken)
        val account = jwtValue(token, "chatgpt_account_id") ?: jwtValue(token, "account_id")
            ?: authStore.getString(KEY_ACCOUNT, null)
            ?: throw CodexException("Im Codex-Token fehlt die ChatGPT-Account-ID.", reconnect = manualToken.isBlank())
        val payload = JSONObject()
            .put("model", model.apiId)
            .put("service_tier", "priority")
            .put("stream", true)
            .put("store", false)
            .put("instructions", instructions)
            .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", input)))
            .put("reasoning", JSONObject().put("effort", reasoning.apiValue))
        if (web) payload.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
        val http = Request.Builder()
            .url(RESPONSES_URL)
            .post(payload.toString().toRequestBody(JSON))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $token")
            .header("originator", "codex_cli_rs")
            .header("User-Agent", "codex_cli_rs/0.0.0 (Denknotiz)")
            .header("ChatGPT-Account-ID", account)
            .build()
        HTTP.newCall(http).await().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                if (web && response.code in 400..499 && (body.contains("tool", true) || body.contains("web_search", true))) {
                    throw CodexException("Der interne Codex-Endpunkt unterstützt die Websuche für dieses Konto oder Modell nicht. Webmodus wurde nicht vorgetäuscht.")
                }
                ensureSuccess(response.code, body, web)
            }
            val reader = response.body?.charStream()?.buffered() ?: throw CodexException("OpenAI hat keinen Antwortstream geliefert.")
            val accumulator = SseAccumulator()
            reader.use { readSse(it, accumulator::accept) { accumulator.completed } }
            accumulator.result()
        }
    }

    private suspend fun exchange(code: String, verifier: String): String? {
        val result = postForm(TOKEN_URL, FormBody.Builder().add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID).add("code", code).add("redirect_uri", REDIRECT).add("code_verifier", verifier).build())
        ensureSuccess(result.code, result.body, false)
        return saveTokens(JSONObject(result.body))
    }

    private fun saveTokens(json: JSONObject): String? {
        val access = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexException("OpenAI hat keinen Zugriffstoken geliefert.")
        val refresh = json.optString("refresh_token").takeIf(String::isNotBlank)
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val email = idToken?.let { jwtPayload(it)?.optString("email") }?.takeIf(String::isNotBlank)
            ?: jwtPayload(access)?.optString("email")?.takeIf(String::isNotBlank)
        val account = jwtValue(access, "chatgpt_account_id") ?: jwtValue(access, "account_id")
        authStore.edit().putString(KEY_ACCESS, access)
            .putLong(KEY_EXPIRES, System.currentTimeMillis() + json.optLong("expires_in", 3_600L) * 1_000L)
            .apply {
                if (refresh != null) putString(KEY_REFRESH, refresh)
                if (email != null) putString(KEY_EMAIL, email)
                if (account != null) putString(KEY_ACCOUNT, account)
            }.apply()
        return email
    }

    private suspend fun validToken(manual: String): String {
        if (manual.isNotBlank()) return manual.trim()
        val token = authStore.getString(KEY_ACCESS, null) ?: throw CodexException("Bitte zuerst Codex verbinden.", reconnect = true)
        if (System.currentTimeMillis() < authStore.getLong(KEY_EXPIRES, 0) - 120_000L) return token
        return refreshLock.withLock {
            val latest = authStore.getString(KEY_ACCESS, null) ?: token
            if (System.currentTimeMillis() < authStore.getLong(KEY_EXPIRES, 0) - 120_000L) return@withLock latest
            val refresh = authStore.getString(KEY_REFRESH, null)
                ?: throw CodexException("Die Codex-Anmeldung ist abgelaufen.", reconnect = true)
            val answer = postForm(TOKEN_URL, FormBody.Builder().add("grant_type", "refresh_token")
                .add("refresh_token", refresh).add("client_id", CLIENT_ID).build())
            ensureSuccess(answer.code, answer.body, false)
            saveTokens(JSONObject(answer.body))
            authStore.getString(KEY_ACCESS, null) ?: throw CodexException("Token-Aktualisierung fehlgeschlagen.", true)
        }
    }

    private data class HttpText(val code: Int, val body: String)
    private suspend fun postJson(url: String, body: String) = executeText(Request.Builder().url(url).post(body.toRequestBody(JSON)).build())
    private suspend fun postForm(url: String, body: RequestBody) = executeText(Request.Builder().url(url).post(body).build())
    private suspend fun executeText(request: Request): HttpText = withContext(Dispatchers.IO) {
        AUTH.newCall(request).await().use { HttpText(it.code, it.body?.string().orEmpty()) }
    }

    private class SseAccumulator {
        private val deltas = StringBuilder()
        private var finalResponse: JSONObject? = null
        var completed = false
            private set

        fun accept(data: String) {
            if (data.isBlank() || data == "[DONE]") return
            val event = JSONObject(data)
            when (event.optString("type")) {
                "response.output_text.delta" -> deltas.append(event.optString("delta"))
                "response.completed" -> { completed = true; finalResponse = event.optJSONObject("response") }
                "response.failed", "response.incomplete", "error" ->
                    throw CodexException(event.optJSONObject("error")?.optString("message").orEmpty().ifBlank { "OpenAI hat die Antwort abgebrochen." })
            }
        }

        fun result(): AiResult {
            if (!completed) throw CodexException("Die OpenAI-Verbindung endete vor dem Abschluss der Antwort.")
            val response = finalResponse
            val text = response?.let(::extractText)?.takeIf(String::isNotBlank) ?: deltas.toString().takeIf(String::isNotBlank)
                ?: throw CodexException("OpenAI hat keinen Antworttext geliefert.")
            return AiResult(text, response?.let(::extractSources).orEmpty())
        }
    }

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val DEVICE_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val DEVICE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val DEVICE_PAGE = "https://auth.openai.com/codex/device"
        private const val REDIRECT = "https://auth.openai.com/deviceauth/callback"
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_ACCOUNT = "account_id"
        private const val KEY_EMAIL = "email"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val AUTH = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
        private val HTTP = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES).build()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) { _, value, _ -> value.close() } else response.close()
        }
    })
}

private suspend fun readSse(reader: BufferedReader, onData: (String) -> Unit, stop: () -> Boolean) {
    val data = mutableListOf<String>()
    fun flush() { if (data.isNotEmpty()) { onData(data.joinToString("\n")); data.clear() } }
    while (true) {
        val line = reader.readLine() ?: break
        when {
            line.isEmpty() -> { flush(); if (stop()) break }
            line.startsWith("data:") -> data += line.removePrefix("data:").trimStart()
        }
    }
    flush()
}

private fun extractText(response: JSONObject): String? {
    response.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val output = response.optJSONArray("output") ?: return null
    return buildString {
        for (i in 0 until output.length()) {
            val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) content.optJSONObject(j)?.optString("text")?.takeIf(String::isNotBlank)?.let(::append)
        }
    }.takeIf(String::isNotBlank)
}

private fun extractSources(response: JSONObject): List<WebSource> {
    val found = linkedMapOf<String, WebSource>()
    fun walk(value: Any?) {
        when (value) {
            is JSONObject -> {
                val url = value.optString("url").takeIf { it.startsWith("http://") || it.startsWith("https://") }
                if (url != null) found[url] = WebSource(value.optString("title").ifBlank { Uri.parse(url).host ?: url }, url)
                value.keys().forEachRemaining { walk(value.opt(it)) }
            }
            is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i))
        }
    }
    walk(response)
    return found.values.toList()
}

private fun jwtPayload(token: String): JSONObject? = runCatching {
    val part = token.split('.')[1]
    JSONObject(String(Base64.getUrlDecoder().decode(part.padEnd((part.length + 3) / 4 * 4, '=')), Charsets.UTF_8))
}.getOrNull()
private fun jwtValue(token: String, key: String): String? = jwtPayload(token)
    ?.optJSONObject("https://api.openai.com/auth")?.optString(key)?.takeIf(String::isNotBlank)

private fun ensureSuccess(code: Int, body: String, web: Boolean) {
    if (code in 200..299) return
    val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
    when {
        code == 401 || code == 403 -> throw CodexException("Codex-Anmeldung ungültig. Bitte erneut verbinden. $message", reconnect = true)
        code == 429 -> throw CodexException("Das Codex-Kontingent ist aktuell ausgeschöpft.")
        web && code == 400 -> throw CodexException("Der Codex-Endpunkt hat die Websuche abgelehnt. Es wurde kein Webresultat erzeugt. $message")
        else -> throw CodexException("OpenAI-Fehler $code: ${message.ifBlank { body.take(240) }}")
    }
}
