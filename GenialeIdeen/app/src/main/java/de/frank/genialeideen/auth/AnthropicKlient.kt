package de.frank.genialeideen.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Wer das Modell rechnet: OpenAI über das ChatGPT-Konto oder Anthropic über einen eigenen API-Schlüssel (Vorbild: Jarvis). */
enum class KiAnbieter(val label: String) { OPENAI("OpenAI"), ANTHROPIC("Anthropic") }

/** Die Claude-Modelle, die über die Anthropic Messages API mit eigenem API-Schlüssel laufen. */
enum class ClaudeModell(val label: String, val apiId: String) {
    OPUS("Claude Opus 5.5", "claude-opus-5-5"),
    FABLE("Claude Fable 5.1", "claude-fable-5-1"),
    SONNET("Claude Sonnet 5.5", "claude-sonnet-5-5"),
    HAIKU("Claude Haiku 5.5", "claude-haiku-5-5"),
    ;

    companion object {
        fun von(wert: String?): ClaudeModell? = wert?.trim()?.let { w ->
            entries.firstOrNull { it.apiId.equals(w, true) || it.label.equals(w, true) || it.name.equals(w, true) }
        }
        fun istClaude(wert: String?): Boolean = von(wert) != null

        /** Anthropic kennt die Denkstufen low bis max, aber kein Ultra. */
        val DENKSTUFEN = listOf("low", "medium", "high", "xhigh", "max")
        fun denkstufe(wert: String?): String = wert?.trim()?.lowercase()?.takeIf { it in DENKSTUFEN } ?: "medium"
    }
}

class AnthropicFehler(message: String, val wiederholbar: Boolean = false) : Exception(message)

/**
 * Anfrage an die Anthropic Messages API (`POST /v1/messages`, Streaming), wie in Jarvis.
 * Denken ist bei den Claude-Modellen von selbst an (adaptiv); die Tiefe steuert `output_config.effort`.
 */
object AnthropicKlient {
    private const val URL = "https://api.anthropic.com/v1/messages"
    private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
    private const val FEHLER_PREFIX = "Anthropic-Fehler "
    private val WARTEZEITEN_MS = longArrayOf(1_500, 4_000)
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var speicher: SharedPreferences? = null

    private fun speicher(context: Context): SharedPreferences = speicher ?: synchronized(this) {
        speicher ?: runCatching {
            val key = MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context.applicationContext, "anthropic_zugang", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse { context.applicationContext.getSharedPreferences("anthropic_zugang_einfach", Context.MODE_PRIVATE) }
            .also { speicher = it }
    }

    /** Anthropic-API-Schlüssel (sk-ant-…) von platform.claude.com. */
    fun schluessel(context: Context): String = speicher(context).getString("api_key", "").orEmpty().trim()
    fun hatSchluessel(context: Context): Boolean = schluessel(context).isNotBlank()
    fun schluesselSpeichern(context: Context, wert: String) {
        speicher(context).edit().putString("api_key", wert.trim()).apply()
    }

    /** Zuletzt gewählter Anbieter und zuletzt gewähltes Claude-Modell, für Apps ohne eigenen Speicherplatz dafür. */
    fun letztesClaudeModell(context: Context): ClaudeModell =
        ClaudeModell.von(speicher(context).getString("claude_modell", null)) ?: ClaudeModell.OPUS
    fun claudeModellMerken(context: Context, modell: ClaudeModell) {
        speicher(context).edit().putString("claude_modell", modell.apiId).apply()
    }
    fun letztesOpenAiModell(context: Context, standard: String): String =
        speicher(context).getString("openai_modell", null) ?: standard
    fun openAiModellMerken(context: Context, modell: String) {
        speicher(context).edit().putString("openai_modell", modell).apply()
    }

    /**
     * Fragt Claude. [zuege] sind (Rolle, Text) mit Rolle "user" oder "assistant".
     * [onDelta] bekommt jedes Textstück, sobald es eintrifft.
     */
    suspend fun antwort(
        context: Context,
        modell: String,
        denkstufe: String,
        anweisung: String,
        zuege: List<Pair<String, String>>,
        maxTokens: Int = 32_000,
        onDelta: suspend (String) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        val claude = ClaudeModell.von(modell) ?: ClaudeModell.OPUS
        val key = schluessel(context).ifBlank {
            throw AnthropicFehler("Für ${claude.label} fehlt der Anthropic-API-Schlüssel. Bitte in den Einstellungen eintragen.")
        }
        var versuch = 0
        var mitFallback = claude != ClaudeModell.HAIKU
        while (true) {
            var geliefert = false
            try {
                return@withContext einmal(key, payload(claude, denkstufe, anweisung, zuege, maxTokens, mitFallback), mitFallback) {
                    geliefert = true
                    onDelta(it)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                currentCoroutineContext().ensureActive()
                // Kennt das Konto die Ersatzmodell-Beta nicht (400), einmal ohne versuchen statt zu scheitern.
                if (!geliefert && mitFallback && fehler.message.orEmpty().startsWith(FEHLER_PREFIX + "400")) { mitFallback = false; continue }
                val wiederholbar = (fehler as? AnthropicFehler)?.wiederholbar ?: (fehler is IOException)
                if (geliefert || !wiederholbar || versuch >= WARTEZEITEN_MS.size) {
                    throw if (fehler is AnthropicFehler) fehler else AnthropicFehler("Claude ist nicht erreichbar: ${fehler.message}", true)
                }
                delay(WARTEZEITEN_MS[versuch++])
            }
        }
        ""
    }

    private fun payload(
        modell: ClaudeModell,
        denkstufe: String,
        anweisung: String,
        zuege: List<Pair<String, String>>,
        maxTokens: Int,
        mitFallback: Boolean,
    ): JSONObject {
        val messages = JSONArray()
        var rolle: String? = null
        val inhalt = StringBuilder()
        fun abschliessen() {
            val r = rolle ?: return
            messages.put(JSONObject().put("role", r).put("content", inhalt.toString().ifBlank { "…" }))
            inhalt.clear()
        }
        // Anthropic erwartet abwechselnd user und assistant, beginnend mit user; gleiche Rollen werden zusammengelegt.
        zuege.forEach { (rohRolle, text) ->
            val r = if (rohRolle == "assistant") "assistant" else "user"
            if (rolle == null && r == "assistant") { rolle = "user"; inhalt.append("(Gesprächsbeginn)") }
            if (r != rolle) { abschliessen(); rolle = r }
            if (text.isNotBlank()) { if (inhalt.isNotEmpty()) inhalt.append("\n\n"); inhalt.append(text) }
        }
        // Ein letzter Modell-Zug wäre ein Vorbefüllen, das die neuen Claude-Modelle ablehnen.
        if (rolle == "assistant") { abschliessen(); rolle = "user"; inhalt.append("Fahre fort.") }
        if (rolle == null) { rolle = "user"; inhalt.append("…") }
        abschliessen()
        val payload = JSONObject()
            .put("model", modell.apiId)
            .put("max_tokens", maxTokens)
            .put("stream", true)
            .put("messages", messages)
            .put("output_config", JSONObject().put("effort", ClaudeModell.denkstufe(denkstufe)))
        if (anweisung.isNotBlank()) {
            payload.put("system", JSONArray().put(JSONObject().put("type", "text").put("text", anweisung.trim())
                .put("cache_control", JSONObject().put("type", "ephemeral"))))
        }
        // Lehnt die Sicherheitsprüfung eine Anfrage ab, rechnet Anthropic sie selbst mit einem Ersatzmodell weiter.
        if (mitFallback) payload.put("fallbacks", "default")
        return payload
    }

    private suspend fun einmal(key: String, payload: JSONObject, mitFallback: Boolean, onDelta: suspend (String) -> Unit): String {
        val builder = Request.Builder().url(URL)
            .post(payload.toString().toRequestBody(JSON))
            .header("Accept", "text/event-stream")
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
        if (mitFallback) builder.header("anthropic-beta", FALLBACK_BETA)
        val call = client.newCall(builder.build())
        // Bricht der Aufrufer ab, auch den laufenden Stream wirklich schließen.
        val abbruch = currentCoroutineContext()[Job]?.invokeOnCompletion { if (it != null) call.cancel() }
        val response = try { call.warte() } catch (e: Throwable) { abbruch?.dispose(); throw e }
        val text = StringBuilder()
        var stopReason: String? = null
        var fertig = false
        try {
            response.use { r ->
                val body = r.body
                if (!r.isSuccessful) fehler(r.code, body?.string().orEmpty())
                val reader = body?.charStream()?.buffered() ?: throw AnthropicFehler("Anthropic hat keinen Antwortstream geliefert.")
                reader.use {
                    while (!fertig) {
                        currentCoroutineContext().ensureActive()
                        val zeile = it.readLine() ?: break
                        if (!zeile.startsWith("data:")) continue
                        val daten = zeile.removePrefix("data:").trim()
                        if (daten.isEmpty()) continue
                        val ereignis = runCatching { JSONObject(daten) }.getOrNull() ?: continue
                        when (ereignis.optString("type")) {
                            "content_block_delta" -> ereignis.optJSONObject("delta")?.takeIf { d -> d.optString("type") == "text_delta" }
                                ?.optString("text")?.takeIf(String::isNotEmpty)?.let { stueck -> text.append(stueck); onDelta(stueck) }
                            "message_delta" -> ereignis.optJSONObject("delta")?.optString("stop_reason")?.takeIf(String::isNotBlank)?.let { s -> stopReason = s }
                            "message_stop" -> fertig = true
                            "error" -> {
                                val f = ereignis.optJSONObject("error")
                                val art = f?.optString("type").orEmpty()
                                throw AnthropicFehler("Anthropic hat die Antwort abgebrochen: " + (f?.optString("message")?.takeIf(String::isNotBlank) ?: art),
                                    wiederholbar = art == "overloaded_error" || art == "api_error" || art == "rate_limit_error")
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            if (call.isCanceled()) throw CancellationException("Die Anthropic-Anfrage wurde abgebrochen.")
            throw e
        } finally {
            abbruch?.dispose()
        }
        if (!fertig) throw AnthropicFehler("Die Anthropic-Verbindung endete vor dem Abschluss der Antwort.", true)
        if (stopReason == "refusal") throw AnthropicFehler("Claude hat die Anfrage aus Sicherheitsgründen abgelehnt.")
        if (stopReason == "max_tokens" && text.isBlank()) throw AnthropicFehler("Die Claude-Antwort war zu lang und wurde abgeschnitten.")
        return text.toString().takeIf(String::isNotBlank) ?: throw AnthropicFehler("Anthropic hat keinen Antworttext geliefert.")
    }

    private fun fehler(code: Int, body: String): Nothing {
        val nachricht = runCatching { JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank) }.getOrNull() ?: body.take(240)
        throw when {
            code == 401 || code == 403 -> AnthropicFehler("Der Anthropic-API-Schlüssel ist ungültig oder nicht freigeschaltet. Bitte in den Einstellungen prüfen. $nachricht")
            code == 429 -> AnthropicFehler("Anthropic bremst gerade (Ratenlimit oder Guthaben). $nachricht", wiederholbar = true)
            code == 408 || code == 529 || code in 500..599 -> AnthropicFehler("Anthropic ist gerade überlastet oder nicht erreichbar (Fehler $code). Bitte kurz erneut versuchen.", wiederholbar = true)
            else -> AnthropicFehler("$FEHLER_PREFIX$code: $nachricht")
        }
    }

    private suspend fun Call.warte(): Response = suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (fortsetzung.isActive) fortsetzung.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                if (fortsetzung.isActive) fortsetzung.resume(response) else response.close()
            }
        })
    }
}
