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

    @Test
    fun longTextIsSplitOnUtf8SafeBoundaries() {
        val text = List(200) { "Wissen😀" }.joinToString(" ")
        val chunks = EdgeTtsPlayer.splitForTts(text, maxUtf8Bytes = 64)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.toByteArray(Charsets.UTF_8).size <= 64 })
        assertEquals(text, chunks.joinToString(" "))
    }

    @Test
    fun headingAndParagraphBecomeOneSpeechUnit() {
        val units = EdgeTtsPlayer.buildSpeechUnits("## Die Zelle\n\nEine Zelle ist der kleinste lebende Baustein.")

        assertEquals(listOf(SpeechUnit("Die Zelle. Eine Zelle ist der kleinste lebende Baustein.", 0L)), units)
    }

    @Test
    fun articleSectionsHaveOneSecondPauseAndSourcesAreSkipped() {
        val article = """
            ## Teil eins

            Erster Absatz.

            ## Teil zwei

            Zweiter Absatz.

            ## Quellen

            - Beispiel: https://example.org
        """.trimIndent()

        assertEquals(
            listOf(
                SpeechUnit("Teil eins. Erster Absatz.", 1_000L),
                SpeechUnit("Teil zwei. Zweiter Absatz.", 0L),
            ),
            EdgeTtsPlayer.buildSpeechUnits(article),
        )
    }

    @Test
    fun everyParagraphRepeatsItsSectionHeadingInANewSpeechUnit() {
        val article = """
            ## Photosynthese

            Pflanzen nehmen Licht auf.

            Dabei entsteht chemische Energie.
        """.trimIndent()

        assertEquals(
            listOf(
                SpeechUnit("Photosynthese. Pflanzen nehmen Licht auf.", 1_000L),
                SpeechUnit("Photosynthese. Dabei entsteht chemische Energie.", 0L),
            ),
            EdgeTtsPlayer.buildSpeechUnits(article),
        )
    }

    @Test
    fun longSectionPausesOnlyAfterItsLastTransportChunk() {
        val article = "## Eins\n\n${List(12) { "Wissen" }.joinToString(" ")}\n\n## Zwei\n\nKurz."
        val units = EdgeTtsPlayer.buildSpeechUnits(article, maxUtf8Bytes = 24)

        assertTrue(units.size > 2)
        assertTrue(units.dropLast(2).all { it.pauseAfterMs == 0L })
        assertEquals(1_000L, units[units.lastIndex - 1].pauseAfterMs)
        assertEquals(0L, units.last().pauseAfterMs)
        assertTrue(units.all { it.text.toByteArray(Charsets.UTF_8).size <= 24 })
    }

    @Test
    fun manualContinuationPlanPrefetchesAnswerAfterQuestionChunks() {
        val plan = EdgeTtsPlayer.buildManualSpeechPlan(
            text = "Kurze Frage?",
            continuationText = "Ausführliche Antwort.",
            maxUtf8Bytes = 18,
        )

        assertEquals(1, plan.continuationIndex)
        assertEquals("Kurze Frage?", plan.units.first().text)
        assertEquals(0L, plan.units.first().pauseAfterMs)
        assertEquals("Ausführliche", plan.units[plan.continuationIndex].text)
        assertTrue(plan.units.size > plan.continuationIndex)
    }
}
