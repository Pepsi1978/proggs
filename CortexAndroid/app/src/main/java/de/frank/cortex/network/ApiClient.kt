package de.frank.cortex.network

import com.squareup.moshi.Moshi
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.model.*
import de.frank.cortex.observability.CortexLog
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- Moshi Singleton (Best Practice §1.7 / §2.1) ---
    private val moshi: Moshi = Moshi.Builder().build()

    private val moshiFactory = MoshiConverterFactory.create(moshi)

    // --- Logging Interceptor (Best Practice §4.4: als LETZTER, Secrets redacted) ---
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        CortexLog.info("HTTP", "log", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
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

        val response = externalClient.newCall(request).execute()
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

        val response = externalClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Leere Gemini-TTS-Antwort")

        if (!response.isSuccessful) {
            throw Exception("Gemini-TTS-Fehler ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)

        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val finishReason = candidate?.optString("finishReason")
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason")
        if (blockReason != null) throw Exception("Gemini blockiert: $blockReason")
        if (finishReason != "STOP" && finishReason != null) {
            CortexLog.warn("Gemini", "tts", "Unerwarteter finishReason: $finishReason")
        }

        val audioData = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getJSONObject("inlineData")
            ?.getString("data")
            ?: throw Exception("Keine Audio-Daten in Gemini-TTS-Antwort")

        CortexLog.info("Gemini", "tts", "TTS-Audio erhalten", mapOf("bytes_b64" to audioData.length))
        return android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
    }

    // --- Gemini Text verbessern ---

    suspend fun geminiImprove(text: String): String {
        val key = SettingsStore.geminiApiKey
        val prompt = "Verbessere Grammatik und Zeichensetzung des folgenden deutschen Textes. Inhalt und Bedeutung 1:1 lassen, nichts hinzufuegen/weglassen. Gib NUR den verbesserten Text zurueck."
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text)}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 128},
                "maxOutputTokens": 4096
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = externalClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Leere Gemini-Antwort")

        if (!response.isSuccessful) {
            throw Exception("Gemini-Fehler ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val finishReason = candidate?.optString("finishReason")
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason")
        if (blockReason != null) throw Exception("Gemini blockiert: $blockReason")
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
}
