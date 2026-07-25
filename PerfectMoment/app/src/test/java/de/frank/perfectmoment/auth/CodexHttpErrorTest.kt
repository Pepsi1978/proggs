package de.frank.perfectmoment.auth

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexHttpErrorTest {
    @Test
    fun `Serverfehler 503 ist wiederholbar`() {
        val error = assertThrows(CodexAuthException::class.java) {
            classifyHttpError(503, """{"error":{"message":"Service unavailable"}}""")
        }

        assertEquals(AuthErrorKind.NETWORK, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun `Zeitueberschreitung und alle 5xx sind wiederholbar`() {
        listOf(408, 500, 502, 503, 504).forEach { code ->
            val error = assertThrows(CodexAuthException::class.java) {
                classifyHttpError(code, "")
            }
            assertTrue("Fehler $code sollte wiederholbar sein", error.retryable)
        }
    }

    @Test
    fun `Kontingent- und Anmeldefehler werden nicht wiederholt`() {
        val quota = assertThrows(CodexAuthException::class.java) { classifyHttpError(429, "") }
        assertEquals(AuthErrorKind.QUOTA, quota.kind)
        assertFalse(quota.retryable)

        val reauth = assertThrows(CodexAuthException::class.java) { classifyHttpError(401, "") }
        assertEquals(AuthErrorKind.REAUTH, reauth.kind)
        assertFalse(reauth.retryable)
    }

    @Test
    fun `fehlerhafte Anfragen werden nicht wiederholt`() {
        val error = assertThrows(CodexAuthException::class.java) {
            classifyHttpError(400, """{"error":{"message":"bad request"}}""")
        }

        assertEquals(AuthErrorKind.NETWORK, error.kind)
        assertFalse(error.retryable)
    }

    @Test
    fun `Erfolgsantworten werfen nicht`() {
        classifyHttpError(200, "")
        classifyHttpError(299, "")
    }

    @Test
    fun `erster Versuch behaelt den Priority-Tarif`() {
        val payload = JSONObject().put("service_tier", "priority").put("model", "gpt-5")

        assertEquals("priority", payloadForAttempt(payload, 0).optString("service_tier"))
    }

    @Test
    fun `Folgeversuche fallen auf den Standard-Tarif zurueck ohne die Anfrage zu veraendern`() {
        val payload = JSONObject().put("service_tier", "priority").put("model", "gpt-5")

        val retry = payloadForAttempt(payload, 1)

        assertNull(retry.opt("service_tier"))
        assertEquals("gpt-5", retry.optString("model"))
        assertEquals("priority", payload.optString("service_tier"))
    }
}
