package de.frank.karteikartenlernen

import de.frank.karteikartenlernen.audio.WavAudioRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionHelpersTest {
    @Test
    fun transcriptIsAppendedWithoutReplacingExistingInput() {
        assertEquals("Neue Frage", appendTranscription("", " Neue Frage "))
        assertEquals("Erster Satz Zweiter Satz", appendTranscription("Erster Satz", "Zweiter Satz"))
        assertEquals("Erster Satz Zweiter Satz", appendTranscription("Erster Satz ", " Zweiter Satz "))
        assertEquals("Erster Satz", appendTranscription("Erster Satz", "   "))
    }

    @Test
    fun wavHeaderDescribesGroqCompatiblePcm() {
        val header = WavAudioRecorder.wavHeader(32_000)
        val littleEndian = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(header, 8, 4, Charsets.US_ASCII))
        assertEquals(1, littleEndian.getShort(20).toInt())
        assertEquals(1, littleEndian.getShort(22).toInt())
        assertEquals(16_000, littleEndian.getInt(24))
        assertEquals(16, littleEndian.getShort(34).toInt())
        assertEquals(32_000, littleEndian.getInt(40))
    }

    @Test
    fun buildUsesWhisperLargeV3Turbo() {
        assertEquals("whisper-large-v3-turbo", BuildConfig.GROQ_TRANSCRIPTION_MODEL)
    }
}
