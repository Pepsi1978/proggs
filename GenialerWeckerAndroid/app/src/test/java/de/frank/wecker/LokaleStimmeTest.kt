package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test

/** Die Verkaufs-App kennt nur lokale Gerätestimmen; Altwerte fremder Anbieter werden nie als Stimme benutzt. */
class LokaleStimmeTest {
    private val standard = SyntheseStimme("de-de-x-standard", 1f)

    @Test fun ohneEigeneWahlGiltDerStandard() {
        val stimme = Alarm(steps = listOf(Step.TEXT), text = "Hallo").resolveVoice(standard)
        assertEquals("de-de-x-standard", stimme.stimme)
        assertEquals(LokaleStimmen.PROVIDER, stimme.ttsProvider)
        assertEquals(1f, stimme.playbackSpeed)
    }

    @Test fun eigeneLokaleStimmeUndTempoGeltenNurFuerDiesenWecker() {
        val stimme = Alarm(voiceProvider = LokaleStimmen.PROVIDER, voiceId = "de-de-x-eigene", speechRate = 1.4f).resolveVoice(standard)
        assertEquals("de-de-x-eigene", stimme.stimme)
        assertEquals(1.4f, stimme.ttsSpeechRate)
    }

    @Test fun fremderAnbieterFaelltAufDieLokaleStandardstimmeZurueck() {
        val stimme = Alarm(voiceProvider = "edge_tts", voiceId = "de-DE-KatjaNeural").resolveVoice(standard)
        assertEquals("de-de-x-standard", stimme.stimme)
    }

    @Test fun nurDerTextSchrittBrauchtSprache() {
        assertTrue(Alarm(steps = listOf(Step.TEXT), text = "x").needsSpeech)
        assertFalse(Alarm(steps = listOf(Step.TONE, Step.MUSIC)).needsSpeech)
        assertEquals(listOf("TONE", "TEXT", "MUSIC"), Step.entries.map { it.name })
    }
}
