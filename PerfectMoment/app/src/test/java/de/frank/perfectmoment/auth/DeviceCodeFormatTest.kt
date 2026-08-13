package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hält fest, dass der Anmelde-Bildschirm den Gerätecode vollständig zeigt.
 *
 * Vorfall vom 13.08.2026: Die Anzeige schnitt den Code hart auf acht Zeichen zu
 * (`take(8)`, zwei Vierergruppen), während die Seite auth.openai.com/codex/device
 * neun Zeichen als 4 + 5 verlangt. Das neunte Zeichen war nicht abzulesen, die
 * Anmeldung damit unmöglich. Die Länge bestimmt ausschließlich der Server.
 */
class DeviceCodeFormatTest {
    @Test
    fun `nine characters are shown as four plus five`() {
        assertEquals(listOf("ABCD", "EFGHJ"), deviceCodeGroups("ABCDEFGHJ"))
    }

    @Test
    fun `no character is lost however long the code is`() {
        val long = "ABCDEFGHJKLM"
        assertEquals(long, deviceCodeGroups(long).joinToString(""))
        assertEquals("ABCDEFGHJ", deviceCodeGroups("ABCDEFGHJ").joinToString(""))
        assertEquals("ABCDEFGH", deviceCodeGroups("ABCDEFGH").joinToString(""))
    }

    @Test
    fun `eight characters stay two groups of four`() {
        assertEquals(listOf("ABCD", "EFGH"), deviceCodeGroups("ABCDEFGH"))
    }

    @Test
    fun `a separator sent by the server decides the grouping`() {
        assertEquals(listOf("ABCD", "EFGHJ"), deviceCodeGroups("ABCD-EFGHJ"))
        assertEquals(listOf("ABC", "DEFGHJ"), deviceCodeGroups("ABC-DEFGHJ"))
        assertEquals(listOf("ABCD", "EFGHJ"), deviceCodeGroups("ABCD EFGHJ"))
    }

    @Test
    fun `surrounding whitespace does not create empty groups`() {
        assertEquals(listOf("ABCD", "EFGHJ"), deviceCodeGroups("  ABCD-EFGHJ  "))
    }

    @Test
    fun `short codes stay in one piece`() {
        assertEquals(listOf("ABCD"), deviceCodeGroups("ABCD"))
        assertEquals(listOf("AB"), deviceCodeGroups("AB"))
    }

    @Test
    fun `an empty code shows placeholders in the real length`() {
        assertEquals(listOf("––––", "–––––"), deviceCodeGroups(""))
        assertEquals(listOf("––––", "–––––"), deviceCodeGroups("   "))
    }
}
