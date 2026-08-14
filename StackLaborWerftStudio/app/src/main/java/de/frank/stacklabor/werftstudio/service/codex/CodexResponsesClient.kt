package de.frank.stacklabor.werftstudio.service.codex

import de.frank.stacklabor.werftstudio.service.auth.CodexOAuthClient
import de.frank.stacklabor.werftstudio.service.network.awaitResponse
import java.io.BufferedReader
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class CodexResponsesClient(
    private val oauth: CodexOAuthClient,
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    suspend fun request(
        payload: JSONObject,
        operationId: String = UUID.randomUUID().toString(),
        allowNetworkRetry: Boolean = true,
        onEvent: suspend (CodexStreamEvent) -> Unit = {},
    ): String {
        var attempt = 0
        while (true) {
            try {
                return requestOnce(payload, operationId, onEvent)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val retryable = error is IOException ||
                    (error is CodexException && error.kind == CodexErrorKind.NETWORK && error.retryable)
                if (!allowNetworkRetry || attempt > 0 || !retryable) throw error.toCodexNetworkException()
                onEvent(CodexStreamEvent.NetworkRetry)
                delay(NETWORK_RETRY_MS)
                attempt++
            }
        }
    }

    private suspend fun requestOnce(
        payload: JSONObject,
        operationId: String,
        onEvent: suspend (CodexStreamEvent) -> Unit,
    ): String {
        val token = oauth.validAccessToken()
        val accountId = oauth.currentAccount()?.accountId
            ?: throw CodexException(CodexErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
        val request = Request.Builder()
            .url(RESPONSES_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $token")
            .header("ChatGPT-Account-ID", accountId)
            .header("Idempotency-Key", operationId)
            .header("originator", "codex_cli_rs")
            .header("User-Agent", "codex_cli_rs/0.0.0 (StackLabor Werft Studio)")
            .build()
        return withContext(Dispatchers.IO) {
            val call = httpClient.newCall(request)
            val cancellationHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            try {
                call.awaitResponse().use {
                    val body = it.body
                    if (!it.isSuccessful) {
                        throw CodexErrorClassifier.fromHttp(
                            it.code,
                            body?.string().orEmpty(),
                            it.header("Retry-After"),
                        )
                    }
                    val reader = body?.charStream()?.buffered()
                        ?: throw CodexException(CodexErrorKind.NETWORK, "Codex hat keinen Antwortstream geliefert.")
                    reader.use { stream -> readResponse(stream, onEvent) }
                }
            } finally {
                cancellationHandle.dispose()
            }
        }
    }

    private suspend fun readResponse(
        reader: BufferedReader,
        onEvent: suspend (CodexStreamEvent) -> Unit,
    ): String {
        val accumulator = ResponseAccumulator()
        readSse(reader) { data ->
            accumulator.accept(data).forEach { event -> onEvent(event) }
        }
        return accumulator.result()
    }

    private class ResponseAccumulator {
        private val deltas = StringBuilder()
        private var completedText: String? = null
        private var completed = false
        private var emittedNarrativeLength = 0

        fun accept(data: String): List<CodexStreamEvent> {
            if (data.isBlank() || data == "[DONE]") return emptyList()
            val event = runCatching { JSONObject(data) }.getOrElse {
                throw CodexException(CodexErrorKind.NETWORK, "Codex hat ein ungültiges Streaming-Ereignis geliefert.", it)
            }
            return when (event.optString("type")) {
                "response.output_text.delta" -> {
                    val delta = event.optString("delta")
                    if (delta.isEmpty()) return emptyList()
                    deltas.append(delta)
                    buildList {
                        add(CodexStreamEvent.RawDelta(delta))
                        val narrative = CodexJson.bestEffortNarrative(deltas.toString())
                        if (narrative.length > emittedNarrativeLength) {
                            add(CodexStreamEvent.NarrativeDelta(narrative.substring(emittedNarrativeLength)))
                            emittedNarrativeLength = narrative.length
                        }
                    }
                }
                "response.completed" -> {
                    completed = true
                    completedText = event.optJSONObject("response")?.let(::extractOutputText)
                    emptyList()
                }
                "response.incomplete" -> throw CodexException(
                    CodexErrorKind.NETWORK,
                    "Codex hat die Antwort unvollständig beendet.",
                    retryable = true,
                )
                "response.failed" -> throw CodexErrorClassifier.fromStream(
                    event.optJSONObject("response")?.optJSONObject("error"),
                )
                "error" -> throw CodexErrorClassifier.fromStream(event.optJSONObject("error") ?: event)
                else -> emptyList()
            }
        }

        fun result(): String {
            if (!completed) {
                throw CodexException(
                    CodexErrorKind.NETWORK,
                    "Die Codex-Verbindung endete vor dem Abschluss der Antwort.",
                    retryable = true,
                )
            }
            return completedText?.takeIf(String::isNotBlank)
                ?: deltas.toString().takeIf(String::isNotBlank)
                ?: throw CodexException(CodexErrorKind.NETWORK, "Codex hat keinen Antworttext geliefert.")
        }
    }

    companion object {
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val NETWORK_RETRY_MS = 3_000L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(8, TimeUnit.MINUTES)
            .build()
    }
}

internal suspend fun readSse(reader: BufferedReader, onData: suspend (String) -> Unit) {
    val dataLines = mutableListOf<String>()
    suspend fun dispatch() {
        if (dataLines.isEmpty()) return
        onData(dataLines.joinToString("\n"))
        dataLines.clear()
    }
    while (true) {
        val line = reader.readLine() ?: break
        when {
            line.isEmpty() -> dispatch()
            line.startsWith(':') -> Unit
            line.startsWith("data:") -> dataLines += line.removePrefix("data:").trimStart()
        }
    }
    dispatch()
}

internal fun extractOutputText(response: JSONObject): String? {
    response.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
    val output = response.optJSONArray("output") ?: return null
    return buildString {
        for (outputIndex in 0 until output.length()) {
            val content = output.optJSONObject(outputIndex)?.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val item = content.optJSONObject(contentIndex) ?: continue
                item.optString("text").takeIf(String::isNotBlank)?.let(::append)
            }
        }
    }.takeIf(String::isNotBlank)
}

private fun Exception.toCodexNetworkException(): Exception = when (this) {
    is CodexException -> this
    is IOException -> CodexException(
        CodexErrorKind.NETWORK,
        "Codex ist über die aktuelle Netzwerkverbindung nicht erreichbar.",
        this,
        retryable = true,
    )
    else -> this
}

internal fun codexPayload(
    request: CodexEvaluationRequest,
    goals: List<CodexGoal>,
    repairRawOutput: String? = null,
): JSONObject {
    val instructions = buildString {
        append("Bewerte Nahrungsergänzungsmittel evidenzorientiert und vorsichtig. ")
        append("Nur dieses JSON, kein Text davor oder danach. ")
        append("Nicht genannte Mittel-Ziel-Paare gelten als neutral. ")
        append("Als 'alterniert mit' gekennzeichnete Paare nie als Konkurrenz melden. ")
        append("Nutze in den ID-Feldern ('nem', 'nem_a', 'nem_b', 'ziel', 'frage') ausschließlich die übergebenen IDs. ")
        // Ohne diese Ansage schrieb das Modell die internen IDs in den Fliesstext — also
        // "hyaluronsaeure" und "loewenmaehne" statt "Hyaluronsäure" und "Löwenmähne".
        append("IM FLIESSTEXT ('gesamt', 'grund', 'hinweise', 'text') gilt dagegen: ")
        append("Schreibe die Mittel IMMER mit ihrem Anzeigenamen aus dem Feld 'name' der Kontextdaten, NIEMALS mit der ID. ")
        append("Schreibe durchgehend korrektes Deutsch nach aktueller Rechtschreibung: ")
        append("echte Umlaute ä, ö, ü und ß — niemals die Ersatzschreibung ae, oe, ue oder ss. ")
        append("Beachte die deutsche Groß- und Kleinschreibung: Substantive und Satzanfänge groß, ")
        append("Eigennamen und Präparatenamen so geschrieben wie im Feld 'name'. ")
        append("Gründe sind knapp. ")
        // Bei diesen Mitteln kennt Frank die Einnahme selbst — allgemeine Hinweise dazu sind
        // nur Rauschen. Genannt werden sie deshalb ausschliesslich bei einem echten Konflikt.
        append(
            BEKANNTE_MITTEL.joinToString(", ") + ": " +
                "Erwähne diese Mittel im Fließtext NUR dann, wenn sie mit einem anderen Mittel dieses Stacks " +
                "nicht zusammen eingenommen werden sollten — also bei einer echten Wechselwirkung, " +
                "Aufnahmekonkurrenz oder einem nötigen zeitlichen Abstand. Gibt es keinen solchen Konflikt, " +
                "erwähne sie überhaupt nicht: keine allgemeinen Einnahme-, Dosis-, Verordnungs- oder " +
                "Sicherheitshinweise, keine Einordnung, keine Nennung in der Einnahme-Reihenfolge. ",
        )
        if (goals.isEmpty()) {
            // Ohne Ziele gibt es nichts zu gewichten — dann zaehlt nur die Zusammenstellung selbst.
            append("Für diesen Stack sind KEINE Ziele hinterlegt. Gib 'zellen' deshalb als leere Liste aus. ")
            append("Beurteile stattdessen die Zusammenstellung selbst: Konkurrenzen um Aufnahmewege, ")
            append("Wechselwirkungen, sinnvolle Einnahme-Reihenfolge, Abstände, Dosishöhen und Verträglichkeit. ")
            append("Schreibe das ausführlich in 'gesamt'. ")
        }
        if (repairRawOutput != null) {
            append("Der erste Ausgabeversuch war ungültig. Repariere ihn strikt nach dem Schema; ")
            append("gib erneut nur vollständiges JSON aus.")
        }
    }
    val input = buildString {
        append("Kontextdaten:\n")
        append(request.contextJson)
        if (goals.isEmpty()) {
            append("\n\nZu diesem Stack sind keine Ziele festgelegt — 'zellen' bleibt leer.")
        } else {
            append("\n\nIn dieser Teilanfrage zu bewertende Ziele:\n")
            append(JSONArray(goals.map { JSONObject().put("id", it.id).put("rang", it.rank).put("text", it.text) }))
        }
        if (repairRawOutput != null) {
            append("\n\nZu reparierender Ausgabeversuch:\n")
            append(repairRawOutput.take(MAX_REPAIR_CHARS))
        }
    }
    return JSONObject()
        .put("model", request.model.apiId)
        .put("stream", true)
        .put("store", false)
        .put("instructions", instructions)
        .put(
            "input",
            JSONArray().put(JSONObject().put("role", "user").put("content", input)),
        )
        .put("reasoning", JSONObject().put("effort", request.reasoningEffort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "stacklabor_bewertung")
                    .put("strict", true)
                    .put("schema", CodexJson.schema()),
            ),
        )
}

private const val MAX_REPAIR_CHARS = 120_000

/**
 * Mittel, deren Einnahme Frank selbst sicher beherrscht.
 *
 * Sie sollen in der Auswertung nur auftauchen, wenn sie mit dem Stack kollidieren — allgemeine
 * Hinweise dazu sind für ihn Rauschen. Weitere Mittel lassen sich hier einfach ergänzen.
 */
private val BEKANNTE_MITTEL = listOf("Venlafaxin")
