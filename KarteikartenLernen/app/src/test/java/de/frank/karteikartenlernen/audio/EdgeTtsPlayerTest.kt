package de.frank.karteikartenlernen.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsPlayerTest {
    @Test
    fun legacyVoiceNamesResolveToMicrosoftVoiceIds() {
        assertEquals(TtsVoiceRegistry.DEFAULT_VOICE_ID, TtsVoiceRegistry.resolveVoiceId("Seraphina (Multilingual)"))
        assertEquals("de-DE-KillianNeural", TtsVoiceRegistry.resolveVoiceId("Killian"))
        assertEquals(TtsVoiceRegistry.DEFAULT_VOICE_ID, TtsVoiceRegistry.resolveVoiceId("unknown"))
    }

    @Test
    fun ssmlEscapesTextAndPreservesSpeechRate() {
        val frame = EdgeTtsPlayer.buildSsmlFrame(
            requestId = "request-id",
            text = "A < B & C > D",
            voice = "de-DE-KatjaNeural",
            speechRate = 1.25f,
        )

        assertTrue(frame.contains("xml:lang='de-DE'"))
        assertTrue(frame.contains("name='de-DE-KatjaNeural'"))
        assertTrue(frame.contains("rate='+25%'"))
        assertTrue(frame.contains("A &lt; B &amp; C &gt; D"))
        assertFalse(frame.contains("A < B"))
    }
}
