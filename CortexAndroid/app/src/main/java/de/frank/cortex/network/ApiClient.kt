package de.frank.cortex.network

import com.squareup.moshi.Moshi
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.model.*
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 429 von Gemini-TTS. Eigener Typ statt generischer Exception, damit der Aufrufer
 * (ChatViewModel) gezielt drosseln statt sofort blind weiter-haemmern kann.
 * [retryAfterMs] kommt aus dem `Retry-After`-Header, falls Gemini ihn mitsendet.
 */
class GeminiRateLimitException(message: String, val retryAfterMs: Long?) : Exception(message)

object ApiClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private const val CODEX_AUTH_ISSUER = "https://auth.openai.com"
    private const val CODEX_TOKEN_URL = "https://auth.openai.com/oauth/token"
    private const val CODEX_BASE_URL = "https://chatgpt.com/backend-api/codex"
    private const val CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    private val codexFallbackModels = listOf("gpt-5.5", "gpt-5.4", "gpt-5.4-mini", "gpt-5.3-codex", "gpt-5.3-codex-spark")

    // --- Moshi Singleton (Best Practice §1.7 / §2.1) ---
    private val moshi: Moshi = Moshi.Builder().build()

    private val moshiFactory = MoshiConverterFactory.create(moshi)

    // --- Logging Interceptor (Best Practice §4.4: als LETZTER, Secrets redacted) ---
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        CortexLog.info("HTTP", "log", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("x-goog-api-key")
    }

    // --- EIN OkHttpClient pro Backend (Best Practice §1.1) ---
    // Agent + Brain teilen sich einen Client mit Bearer-Auth-Interceptor
    private val authClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val key = SettingsStore.sbApiKey
                val req = chain.request().newBuilder()
                    .header("Authorization", "Bearer $key")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(loggingInterceptor) // LETZTER Interceptor
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Dashboard: kein Auth
    private val dashboardClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Externer Client für Groq/Gemini (ohne Auth-Interceptor, eigene Keys pro Request)
    private val externalClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // TTS liefert große Base64-Audioantworten. BODY-Logging würde den ersten Ton stark verzögern
    // und private Antworttexte ins Log schreiben, deshalb läuft TTS über einen eigenen schlanken Client.
    private val ttsClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Token-Antworten dürfen nie im Log landen, deshalb kein BODY-Logging für OAuth/Codex.
    private val secretExternalClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // --- Retrofit-Instanzen: EINE pro Backend (Best Practice §1.3) ---
    private val agentRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SettingsStore.agentUrl() + "/")
            .client(authClient)
            .addConverterFactory(ScalarsConverterFactory.create()) // VOR Moshi (§1.7)
            .addConverterFactory(moshiFactory)
            .build()
    }

    private val brainRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SettingsStore.brainUrl() + "/")
            .client(authClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(moshiFactory)
            .build()
    }

    private val dashboardRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SettingsStore.dashboardUrl() + "/")
            .client(dashboardClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(moshiFactory)
            .build()
    }

    // --- Service-Interfaces (billig via Proxy) ---
    fun agentApi(): AgentApi = agentRetrofit.create(AgentApi::class.java)
    fun brainApi(): BrainApi = brainRetrofit.create(BrainApi::class.java)
    fun dashboardApi(): DashboardApi = dashboardRetrofit.create(DashboardApi::class.java)

    suspend fun codexStartDeviceAuth(): CodexAuthStartResponse {
        val body = """{"client_id":${JSONObject.quote(CODEX_CLIENT_ID)}}"""
        val request = Request.Builder()
            .url("$CODEX_AUTH_ISSUER/api/accounts/deviceauth/usercode")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        val response = withContext(Dispatchers.IO) { secretExternalClient.newCall(request).execute() }
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw Exception("Codex Device-Code-Start HTTP ${response.code}: $responseBody")
        val json = JSONObject(responseBody)
        val userCode = json.optString("user_code")
        val authId = json.optString("device_auth_id")
        if (userCode.isBlank() || authId.isBlank()) throw Exception("Codex Device-Code-Antwort unvollständig")
        return CodexAuthStartResponse(
            ok = true,
            auth_id = authId,
            user_code = userCode,
            verification_uri = "$CODEX_AUTH_ISSUER/codex/device",
            expires_in = 900,
            interval = maxOf(3, json.optInt("interval", 5))
        )
    }

    suspend fun codexPollDeviceAuth(authId: String, userCode: String): CodexAuthPollResponse {
        val body = """{"device_auth_id":${JSONObject.quote(authId)},"user_code":${JSONObject.quote(userCode)}}"""
        val request = Request.Builder()
            .url("$CODEX_AUTH_ISSUER/api/accounts/deviceauth/token")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        val response = withContext(Dispatchers.IO) { secretExternalClient.newCall(request).execute() }
        val responseBody = response.body?.string().orEmpty()
        if (response.code == 403 || response.code == 404) return CodexAuthPollResponse(ok = true, status = "pending")
        if (!response.isSuccessful) throw Exception("Codex Device-Code-Poll HTTP ${response.code}: $responseBody")

        val codeJson = JSONObject(responseBody)
        val authorizationCode = codeJson.optString("authorization_code")
        val codeVerifier = codeJson.optString("code_verifier")
        if (authorizationCode.isBlank() || codeVerifier.isBlank()) throw Exception("Codex Authorization-Code unvollständig")

        val form = listOf(
            "grant_type=authorization_code",
            "code=${java.net.URLEncoder.encode(authorizationCode, "UTF-8")}",
            "redirect_uri=${java.net.URLEncoder.encode("$CODEX_AUTH_ISSUER/deviceauth/callback", "UTF-8")}",
            "client_id=${java.net.URLEncoder.encode(CODEX_CLIENT_ID, "UTF-8")}",
            "code_verifier=${java.net.URLEncoder.encode(codeVerifier, "UTF-8")}"
        ).joinToString("&")
        val tokenRequest = Request.Builder()
            .url(CODEX_TOKEN_URL)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val tokenResponse = withContext(Dispatchers.IO) { secretExternalClient.newCall(tokenRequest).execute() }
        val tokenBody = tokenResponse.body?.string().orEmpty()
        if (!tokenResponse.isSuccessful) throw Exception("Codex Token-Austausch HTTP ${tokenResponse.code}: $tokenBody")
        val tokenJson = JSONObject(tokenBody)
        val accessToken = tokenJson.optString("access_token")
        if (accessToken.isBlank()) throw Exception("Codex Token-Antwort ohne access_token")
        SettingsStore.codexAccessToken = accessToken
        val refreshToken = tokenJson.optString("refresh_token")
        if (refreshToken.isNotBlank()) SettingsStore.codexRefreshToken = refreshToken
        return CodexAuthPollResponse(ok = true, status = "connected", connected = true, models = codexFallbackModels)
    }

    suspend fun codexModels(): List<String> {
        val access = codexAccessToken()
        val request = Request.Builder()
            .url("$CODEX_BASE_URL/models?client_version=1.0.0")
            .headers(codexHeaders(access))
            .get()
            .build()
        return try {
            val response = withContext(Dispatchers.IO) { secretExternalClient.newCall(request).execute() }
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) return codexFallbackModels
            val arr = JSONObject(body).optJSONArray("models") ?: return codexFallbackModels
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val slug = item.optString("slug")
                    val visibility = item.optString("visibility").lowercase()
                    if (slug.isNotBlank() && visibility !in setOf("hide", "hidden")) add(slug)
                }
                codexFallbackModels.forEach { if (!contains(it)) add(it) }
            }.ifEmpty { codexFallbackModels }
        } catch (_: Exception) {
            codexFallbackModels
        }
    }

    suspend fun codexGenerate(text: String, model: String = SettingsStore.codexModel, reasoning: String = SettingsStore.codexReasoning): String {
        val access = codexAccessToken()
        val effort = reasoning.lowercase().takeIf { it in setOf("minimal", "low", "medium", "high", "xhigh") }
        val body = JSONObject()
            .put("model", model)
            .put("instructions", "Du bist Cortex direkt auf Franks Handy. Antworte präzise, deutsch und ohne erfundene Fakten. Du hast in diesem Handy-Modus keinen direkten Zugriff auf das Second Brain; sage das ehrlich, wenn Speicher- oder Suchfunktionen nötig wären.")
            .put("input", text)
            .put("max_output_tokens", 4096)
        if (effort != null) body.put("reasoning", JSONObject().put("effort", effort))
        val request = Request.Builder()
            .url("$CODEX_BASE_URL/responses")
            .headers(codexHeaders(access))
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = withContext(Dispatchers.IO) { secretExternalClient.newCall(request).execute() }
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw Exception("Codex-Fehler ${response.code}: $responseBody")
        return extractCodexText(JSONObject(responseBody)).ifBlank { throw Exception("Codex lieferte leeren Text") }
    }

    private suspend fun codexAccessToken(): String {
        val access = SettingsStore.codexAccessToken
        if (access.isBlank()) throw IllegalStateException("Codex ist auf dem Handy nicht verbunden")
        val exp = jwtExp(access)
        return if (exp > 0 && exp - (System.currentTimeMillis() / 1000L) < 120) refreshCodexTokens() else access
    }

    private suspend fun refreshCodexTokens(): String {
        val refresh = SettingsStore.codexRefreshToken
        if (refresh.isBlank()) throw IllegalStateException("Codex refresh_token fehlt — bitte neu verbinden")
        val form = "grant_type=refresh_token&refresh_token=${java.net.URLEncoder.encode(refresh, "UTF-8")}&client_id=${java.net.URLEncoder.encode(CODEX_CLIENT_ID, "UTF-8")}"
        val request = Request.Builder()
            .url(CODEX_TOKEN_URL)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = withContext(Dispatchers.IO) { secretExternalClient.newCall(request).execute() }
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw Exception("Codex Refresh HTTP ${response.code}: $body")
        val json = JSONObject(body)
        val access = json.optString("access_token")
        if (access.isBlank()) throw Exception("Codex Refresh ohne access_token")
        SettingsStore.codexAccessToken = access
        val newRefresh = json.optString("refresh_token")
        if (newRefresh.isNotBlank()) SettingsStore.codexRefreshToken = newRefresh
        return access
    }

    private fun codexHeaders(access: String): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
            .add("Authorization", "Bearer $access")
            .add("Content-Type", "application/json")
            .add("Accept", "application/json")
            .add("User-Agent", "codex_cli_rs/0.0.0 (Cortex Android)")
            .add("originator", "codex_cli_rs")
        codexAccountId(access).takeIf { it.isNotBlank() }?.let { builder.add("ChatGPT-Account-ID", it) }
        return builder.build()
    }

    private fun jwtPayload(token: String): JSONObject? = try {
        val part = token.split(".").getOrNull(1).orEmpty()
        val decoded = Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }

    private fun jwtExp(token: String): Long = jwtPayload(token)?.optLong("exp", 0L) ?: 0L

    private fun codexAccountId(token: String): String = jwtPayload(token)
        ?.optJSONObject("https://api.openai.com/auth")
        ?.optString("chatgpt_account_id")
        .orEmpty()

    private fun extractCodexText(json: JSONObject): String {
        json.optString("output_text").takeIf { it.isNotBlank() }?.let { return it.trim() }
        val output = json.optJSONArray("output") ?: return ""
        val parts = mutableListOf<String>()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val c = content.optJSONObject(j) ?: continue
                val t = c.optString("text", c.optString("output_text"))
                if (t.isNotBlank()) parts.add(t)
            }
        }
        return parts.joinToString("").trim()
    }

    // --- Groq STT (direkt über OkHttp, multipart) ---

    suspend fun groqTranscribe(wavBytes: ByteArray): GroqTranscriptionResponse {
        val key = SettingsStore.groqApiKey
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "audio.wav",
                wavBytes.toRequestBody("audio/wav".toMediaType())
            )
            .addFormDataPart("model", "whisper-large-v3-turbo")
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .header("Authorization", "Bearer $key")
            .post(body)
            .build()

        val response = withContext(Dispatchers.IO) { externalClient.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw Exception("Leere Groq-Antwort")

        if (!response.isSuccessful) {
            throw Exception("Groq-Fehler ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val text = json.getString("text")
        val segments = json.optJSONArray("segments")?.let { arr ->
            (0 until arr.length()).map { i ->
                val seg = arr.getJSONObject(i)
                GroqSegment(
                    start = seg.optDouble("start"),
                    end = seg.optDouble("end"),
                    text = seg.optString("text"),
                    no_speech_prob = seg.optDouble("no_speech_prob"),
                    avg_logprob = seg.optDouble("avg_logprob")
                )
            }
        }

        CortexLog.info("Groq", "transcribe", "Transkription erhalten", mapOf("text_length" to text.length))
        return GroqTranscriptionResponse(text = text, segments = segments)
    }

    // --- Gemini TTS ---

    suspend fun geminiTts(text: String, voice: String = SettingsStore.ttsVoice): ByteArray {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val startedAt = System.currentTimeMillis()
        val body = """
        {
            "contents": [{"parts": [{"text": ${JSONObject.quote(text)}}]}],
            "generationConfig": {
                "responseModalities": ["AUDIO"],
                "speechConfig": {
                    "voiceConfig": {
                        "prebuiltVoiceConfig": {
                            "voiceName": ${JSONObject.quote(voice)}
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        executeCancellable(ttsClient, request).use { response ->
            if (response.code == 429) {
                // Retry-After ist laut RFC entweder Sekunden oder ein HTTP-Datum — hier reicht Sekunden-Fall.
                val retryAfterMs = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                val body = response.body?.string().orEmpty()
                CortexLog.warn("Gemini", "tts", "429 Rate-Limit", mapOf(
                    "text_len" to text.length, "retry_after_ms" to (retryAfterMs ?: -1L)
                ))
                throw GeminiRateLimitException("Gemini-TTS-Rate-Limit (429): $body", retryAfterMs)
            }

            val responseBody = response.body?.string() ?: throw Exception("Leere Gemini-TTS-Antwort")

            if (!response.isSuccessful) {
                throw Exception("Gemini-TTS-Fehler ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)

            val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            val finishReason = candidate?.optString("finishReason")
            val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")
            if (finishReason != "STOP" && finishReason != null) {
                CortexLog.warn("Gemini", "tts", "Unerwarteter finishReason: $finishReason")
            }

            val audioInline = candidate
                ?.getJSONObject("content")
                ?.getJSONArray("parts")
                ?.getJSONObject(0)
                ?.getJSONObject("inlineData")
                ?: throw Exception("Keine Audio-Daten in Gemini-TTS-Antwort")

            val mimeType = audioInline.optString("mimeType", "")
            val raw = android.util.Base64.decode(audioInline.getString("data"), android.util.Base64.DEFAULT)
            val pcm = if (mimeType == "audio/wav" && raw.size > 44) raw.copyOfRange(44, raw.size) else raw

            CortexLog.info("Gemini", "tts", "TTS-Audio erhalten", mapOf(
                "text_len" to text.length,
                "mime" to mimeType,
                "raw_bytes" to raw.size,
                "pcm_bytes" to pcm.size,
                "elapsed_ms" to (System.currentTimeMillis() - startedAt)
            ))
            return pcm
        }
    }

    private suspend fun executeCancellable(client: OkHttpClient, request: Request): Response =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                try {
                    val response = call.execute()
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                } catch (e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        }

    // --- Gemini Text verbessern ---

    suspend fun geminiImprove(text: String): String {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val prompt = """
            Du bist ein deutscher Korrektur- und Formulierungsassistent für diktierte oder getippte Notizen.
            Nimm den folgenden Text vollständig als Quelle.
            Erkenne aus dieser Quelle die gemeinte Intention und formuliere sie in sehr gutes, klares, natürliches Deutsch um.
            Korrigiere Grammatik, Rechtschreibung, Zeichensetzung, Satzbau, Diktierfehler, Wiederholungen und holprige Formulierungen.
            Du darfst Sätze neu ordnen und verständlicher formulieren, wenn die Bedeutung erhalten bleibt.
            Füge keine neuen Fakten, Aufgaben, Bewertungen, Beispiele oder Schlussfolgerungen hinzu.
            Lasse keine vorhandenen Informationen, Einschränkungen, Absichten, Namen, Zahlen oder Details weg.
            Wenn der Quelltext unklar ist, bewahre die Unklarheit, statt sie zu erfinden.
            Gib ausschließlich den verbesserten Text zurück, ohne Erklärung, Markdown, Überschrift oder Anführungszeichen.
        """.trimIndent()
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text)}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 128},
                "temperature": 0.2,
                "maxOutputTokens": 4096
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = withContext(Dispatchers.IO) { externalClient.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw Exception("Leere Gemini-Antwort")

        if (!response.isSuccessful) {
            throw Exception("Gemini-Fehler ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val finishReason = candidate?.optString("finishReason")
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")
        if (finishReason == "MAX_TOKENS") {
            CortexLog.warn("Gemini", "improve", "MAX_TOKENS erreicht — Antwort evtl. unvollstaendig")
        }

        val result = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getString("text")
            ?: throw Exception("Kein Text in Gemini-Antwort")

        CortexLog.info("Gemini", "improve", "Text verbessert", mapOf("input_len" to text.length, "output_len" to result.length))
        return result
    }

    suspend fun geminiTitle(text: String): String {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val prompt = """
            Erzeuge aus dem folgenden deutschen Eintrag einen kurzen, präzisen Titel.
            Der Titel soll den Inhalt oder die wichtigste Intention zusammenfassen.
            Maximal 6 bis 7 Wörter.
            Keine neuen Informationen hinzufügen.
            Keine Details erfinden.
            Keine Anführungszeichen, kein Punkt am Ende, keine Erklärung, kein Markdown.
            Gib ausschließlich den Titel zurück.
        """.trimIndent()
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text)}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 64},
                "temperature": 0.15,
                "maxOutputTokens": 64
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = withContext(Dispatchers.IO) { externalClient.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw Exception("Leere Gemini-Antwort")

        if (!response.isSuccessful) {
            throw Exception("Gemini-Fehler ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")

        val result = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getString("text")
            ?.trim()
            ?.trim('"', '„', '“', '.', ' ')
            ?: throw Exception("Kein Titel in Gemini-Antwort")

        CortexLog.info("Gemini", "title", "Titel erzeugt", mapOf("input_len" to text.length, "title" to result))
        return result
    }
}
