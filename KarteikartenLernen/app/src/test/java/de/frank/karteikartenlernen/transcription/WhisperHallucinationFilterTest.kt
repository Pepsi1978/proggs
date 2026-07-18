package de.frank.karteikartenlernen.transcription

import de.frank.karteikartenlernen.audio.WavAudioRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperHallucinationFilterTest {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()

    @Test
    fun speechAnalyzerRejectsPureSilenceBeforeUpload() {
        val silence = requireNotNull(analyzer.analyze(wav(voicedMs = 0, silenceMs = 1_000)))
        val speech = requireNotNull(analyzer.analyze(wav(voicedMs = 200, silenceMs = 800)))

        assertTrue(silence.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS)
        assertTrue(speech.voicedMs >= SpeechAnalyzer.MIN_SPEECH_MS)
    }

    @Test
    fun confidenceGateUsesConservativeAndCondition() {
        val weakSilence = segment("Halluzination", 0.0, 1.0, noSpeech = 0.8, logProbability = -1.2)
        val quietSpeech = segment("Leise Sprache", 0.0, 1.0, noSpeech = 0.8, logProbability = -0.4)

        assertEquals("", filter.filter(response(weakSilence), null))
        assertEquals("Leise Sprache", filter.filter(response(quietSpeech), null))
    }

    @Test
    fun confidenceGateRemovesRepetitionAndMiniNoise() {
        val repetition = segment("danke danke danke", 0.0, 1.0, compression = 2.5)
        val miniNoise = segment("Ja", 0.0, 0.3, noSpeech = 0.8)

        assertEquals("", filter.filter(response(repetition), null))
        assertEquals("", filter.filter(response(miniNoise), null))
    }

    @Test
    fun segmentAudioAlignmentRemovesTrailingHallucination() {
        val analysis = requireNotNull(analyzer.analyze(wav(voicedMs = 200, silenceMs = 800)))
        val spoken = segment("Echter Satz.", 0.0, 0.2)
        val trailing = segment("Ja", 0.4, 0.8)

        assertEquals("Echter Satz.", filter.filter(response(spoken, trailing), analysis))
    }

    @Test
    fun timestampDriftNeverDeletesTheWholeTranscript() {
        val analysis = requireNotNull(analyzer.analyze(wav(voicedMs = 200, silenceMs = 800)))
        val drifted = segment("Echter Satz", 0.4, 0.8)

        assertEquals("Echter Satz", filter.filter(response(drifted), analysis))
    }

    @Test
    fun exactBlocklistNeedsMeasurableSilenceContext() {
        val silence = requireNotNull(analyzer.analyze(wav(voicedMs = 0, silenceMs = 1_000)))
        val spoken = requireNotNull(analyzer.analyze(wav(voicedMs = 800, silenceMs = 200)))
        val phrase = GroqTranscriptionResponse(text = "Vielen Dank!", segments = null)

        assertEquals("", filter.filter(phrase, silence))
        assertEquals("Vielen Dank!", filter.filter(phrase, spoken))
        assertEquals("Vielen Dank!", filter.filter(phrase, null))
    }

    @Test
    fun analyzerFailsOpenForMalformedWav() {
        assertEquals(null, analyzer.analyze(byteArrayOf(1, 2, 3)))
        val valid = requireNotNull(analyzer.analyze(wav(voicedMs = 200, silenceMs = 800)))
        assertNotNull(valid)
        assertFalse(valid.segmentHasSpeech(0.4, 0.8))
    }

    @Test
    fun verboseJsonParserKeepsMissingConfidenceValuesNull() {
        val transcriber = GroqTranscriber(apiKey = "", model = "whisper-large-v3-turbo")
        val parsed = transcriber.parseResponse(
            """{"text":"Hallo","segments":[{"start":0.0,"end":0.5,"text":"Hallo"}]}""",
        )

        assertEquals("Hallo", parsed.text)
        val segment = requireNotNull(parsed.segments).single()
        assertNull(segment.noSpeechProbability)
        assertNull(segment.averageLogProbability)
        assertNull(segment.compressionRatio)
        transcriber.shutdown()
    }

    private fun response(vararg segments: GroqSegment) = GroqTranscriptionResponse(
        text = segments.joinToString(" ") { it.text.orEmpty() },
        segments = segments.toList(),
    )

    private fun segment(
        text: String,
        start: Double,
        end: Double,
        noSpeech: Double = 0.1,
        logProbability: Double = -0.2,
        compression: Double = 1.0,
    ) = GroqSegment(start, end, text, noSpeech, logProbability, compression)

    private fun wav(voicedMs: Int, silenceMs: Int): ByteArray {
        val sampleRate = 16_000
        val voicedSamples = sampleRate * voicedMs / 1_000
        val totalSamples = sampleRate * (voicedMs + silenceMs) / 1_000
        val pcm = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(totalSamples) { index -> pcm.putShort(if (index < voicedSamples) 2_000 else 0) }
        return WavAudioRecorder.wavHeader(pcm.array().size.toLong()) + pcm.array()
    }
}
