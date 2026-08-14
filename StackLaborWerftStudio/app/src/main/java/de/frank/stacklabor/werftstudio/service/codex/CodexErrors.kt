package de.frank.stacklabor.werftstudio.service.codex

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

object CodexErrorClassifier {
    fun fromHttp(code: Int, body: String, retryAfterHeader: String? = null): CodexException {
        val marker = errorMarker(body)
        val message = errorMessage(body)
        val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
        return when {
            code == 429 || marker.contains("quota") || marker.contains("rate_limit") -> CodexException(
                kind = CodexErrorKind.QUOTA,
                message = message.ifBlank { "Das Codex-Kontingent ist aktuell erschöpft." },
                retryAfterSeconds = retryAfter,
            )
            code == 401 || code == 403 || marker.contains("invalid_grant") ||
                marker.contains("refresh_token_reused") || marker.contains("unauthorized") ||
                marker.contains("authentication") -> CodexException(
                CodexErrorKind.REAUTH,
                message.ifBlank { "Die Codex-Anmeldung ist abgelaufen. Bitte erneut anmelden." },
            )
            code == 408 || code in 500..599 -> CodexException(
                CodexErrorKind.NETWORK,
                message.ifBlank { "Codex ist vorübergehend nicht erreichbar (HTTP $code)." },
                retryable = true,
                retryAfterSeconds = retryAfter,
            )
            else -> CodexException(
                CodexErrorKind.NETWORK,
                message.ifBlank { "Codex-Anfrage fehlgeschlagen (HTTP $code)." },
            )
        }
    }

    fun fromStream(error: JSONObject?): CodexException {
        val body = error?.toString().orEmpty()
        val marker = errorMarker(body)
        val message = errorMessage(body).ifBlank { "Codex hat die Antwort abgebrochen." }
        return when {
            marker.contains("quota") || marker.contains("rate_limit") ->
                CodexException(CodexErrorKind.QUOTA, message)
            marker.contains("invalid_grant") || marker.contains("refresh_token_reused") ||
                marker.contains("unauthorized") || marker.contains("authentication") ->
                CodexException(CodexErrorKind.REAUTH, message)
            else -> CodexException(CodexErrorKind.NETWORK, message, retryable = true)
        }
    }

    private fun errorMarker(body: String): String = runCatching {
        val json = JSONObject(body)
        val error = json.optJSONObject("error") ?: json
        listOf(error.optString("type"), error.optString("code"), error.optString("message"))
            .joinToString(" ")
            .lowercase()
    }.getOrDefault(body.lowercase())

    private fun errorMessage(body: String): String = runCatching {
        val json = JSONObject(body)
        val error = json.optJSONObject("error") ?: json
        error.optString("message").trim()
    }.getOrDefault(body.take(300).trim())

    internal fun parseRetryAfterSeconds(value: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
        val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        raw.toLongOrNull()?.let { return it.coerceAtLeast(0L) }
        return runCatching {
            val target = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            ((target - nowMillis).coerceAtLeast(0L) + 999L) / 1_000L
        }.getOrNull()
    }
}
