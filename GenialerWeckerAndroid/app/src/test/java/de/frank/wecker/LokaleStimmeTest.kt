package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test

/** Je Wecker sind nur natürliche Stimmen wählbar; alte Gerätestimmen-Wahlen fallen auf den Standard zurück. */
class LokaleStimmeTest {
    private val standard = SyntheseStimme("de-de-x-standard", 1f)

    @Test fun ohneEigeneWahlGiltDerStandard() {
        val stimme = Alarm(steps = listOf(Step.TEXT), text = "Hallo").resolveVoice(standard)
        assertEquals("de-de-x-standard", stimme.stimme)
        assertEquals(LokaleStimmen.PROVIDER, stimme.ttsProvider)
        assertEquals(1f, stimme.playbackSpeed)
    }

    @Test fun eigeneNatuerlicheStimmeUndTempoGeltenNurFuerDiesenWecker() {
        val stimme = Alarm(voiceProvider = PremiumKatalog.PROVIDER, voiceId = "de-DE-KatjaNeural", speechRate = 1.4f).resolveVoice(standard)
        assertEquals("de-DE-KatjaNeural", stimme.stimme)
        assertEquals(PremiumKatalog.PROVIDER, stimme.ttsProvider)
        assertEquals(1.4f, stimme.ttsSpeechRate)
    }

    @Test fun alteGeraetestimmenWahlFaelltAufDenStandardZurueck() {
        val stimme = Alarm(voiceProvider = LokaleStimmen.PROVIDER, voiceId = "de-de-x-eigene").resolveVoice(standard)
        assertEquals("de-de-x-standard", stimme.stimme)
    }

    @Test fun nurDieZweiBevorzugtenGoogleStimmenWerdenAngeboten() {
        val geraet = listOf("de-DE-language", "de-de-x-dea-local", "de-de-x-deb-local", "de-de-x-deg-local", "de-de-x-nfh-local")
        assertEquals(listOf("de-de-x-deg-local", "de-de-x-nfh-local"), StimmAuswahl.angeboten(geraet))
        assertEquals("Deutsch · hohe Qualität · männlich", LokaleStimmeInfo("de-de-x-deg-local", java.util.Locale.GERMANY, 400).anzeige)
        assertEquals("Deutsch · hohe Qualität · weiblich", LokaleStimmeInfo("de-de-x-nfh-local", java.util.Locale.GERMANY, 400).anzeige)
        // Eine früher gewählte, jetzt ausgeblendete Roboterstimme wird nie mehr benutzt.
        assertEquals("de-de-x-deg-local", StimmAuswahl.wirksam("de-de-x-deb-local", StimmAuswahl.angeboten(geraet)))
        assertEquals("de-de-x-nfh-local", StimmAuswahl.wirksam("de-de-x-nfh-local", StimmAuswahl.angeboten(geraet)))
    }

    @Test fun andereGeraeteBehaltenAlleOfflineStimmen() {
        val samsung = listOf("de-DE-SMTf00", "de-DE-SMTm00")
        assertEquals(samsung, StimmAuswahl.angeboten(samsung))
        assertEquals("Deutsch · hohe Qualität · Stimme 2", LokaleStimmeInfo("de-DE-SMTm00", java.util.Locale.GERMANY, 400, nummer = 2).anzeige)
        assertNull(StimmAuswahl.wirksam("x", emptyList()))
    }

    @Test fun nurDerTextSchrittBrauchtSprache() {
        assertTrue(Alarm(steps = listOf(Step.TEXT), text = "x").needsSpeech)
        assertFalse(Alarm(steps = listOf(Step.TONE, Step.MUSIC)).needsSpeech)
        assertEquals(listOf("TONE", "TEXT", "MUSIC"), Step.entries.map { it.name })
    }
}
