package de.frank.cortex.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenVoiceTest {

    @Test
    fun `voice id yields the chosen name with word breaks`() {
        assertEquals(
            "Frank HD",
            QwenVoiceDirectory.displayName("qwen-tts-vc-FrankHD-voice-2026080323-4d0a")
        )
    }

    @Test
    fun `unknown id shape stays untouched`() {
        assertEquals("irgendwas", QwenVoiceDirectory.displayName("irgendwas"))
    }

    @Test
    fun `creation date is shown german`() {
        assertEquals("03.08.2026, 23:21", QwenVoiceDirectory.germanDate("2026-08-03 23:21:42"))
        assertEquals("kaputt", QwenVoiceDirectory.germanDate("kaputt"))
    }

    @Test
    fun `preferred name keeps only letters and digits`() {
        assertEquals("FrankAbend2", QwenVoiceEnrollment.preferredName("Frank Abend 2!"))
        assertEquals("Stimme", QwenVoiceEnrollment.preferredName("   "))
        assertEquals(16, QwenVoiceEnrollment.preferredName("a".repeat(40)).length)
    }

    // Der Zweig "Wortlaut aus der Alibaba-Antwort" ist hier NICHT geprueft: org.json ist im
    // JVM-Unit-Test nur ein Stub, jedes JSONObject wirft. Geprueft sind die Rueckfaelle, die
    // ohne echtes JSON auskommen.
    @Test
    fun `error text falls back to the http code`() {
        assertEquals(
            "Der Alibaba-Schlüssel wird abgewiesen.",
            QwenVoiceEnrollment.readableError("kein json", 401)
        )
        assertEquals(
            "Der Alibaba-Schlüssel wird abgewiesen.",
            QwenVoiceEnrollment.readableError("kein json", 403)
        )
        assertEquals(
            "Alibaba hat mit Fehler 500 geantwortet.",
            QwenVoiceEnrollment.readableError("kein json", 500)
        )
    }

    @Test
    fun `reading script stays inside the one minute budget`() {
        // Alibaba nimmt hoechstens 60 s an; ruhig gelesen sind ~105 Woerter etwa 50 s.
        val words = VoiceSampleScript.wordCount()
        assertTrue("Vorlesetext hat $words Wörter", words in 80..VoiceSampleScript.TARGET_WORDS)
    }
}
