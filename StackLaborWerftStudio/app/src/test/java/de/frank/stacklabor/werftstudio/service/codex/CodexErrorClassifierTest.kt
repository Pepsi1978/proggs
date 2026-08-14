package de.frank.stacklabor.werftstudio.service.codex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodexErrorClassifierTest {
    @Test
    fun `klassifiziert abgelaufene Anmeldung als REAUTH`() {
        val error = CodexErrorClassifier.fromHttp(
            401,
            "{\"error\":{\"code\":\"invalid_grant\",\"message\":\"expired\"}}",
        )

        assertEquals(CodexErrorKind.REAUTH, error.kind)
        assertFalse(error.retryable)
    }

    @Test
    fun `klassifiziert Kontingent und uebernimmt Retry After`() {
        val error = CodexErrorClassifier.fromHttp(
            429,
            "{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"wait\"}}",
            "120",
        )

        assertEquals(CodexErrorKind.QUOTA, error.kind)
        assertEquals(120L, error.retryAfterSeconds)
    }

    @Test
    fun `klassifiziert Serverfehler als wiederholbares NETWORK`() {
        val error = CodexErrorClassifier.fromHttp(503, "temporarily unavailable")

        assertEquals(CodexErrorKind.NETWORK, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun `klassifiziert sonstige Clientfehler als nicht wiederholbares NETWORK`() {
        val error = CodexErrorClassifier.fromHttp(400, "invalid request")

        assertEquals(CodexErrorKind.NETWORK, error.kind)
        assertFalse(error.retryable)
    }
}
