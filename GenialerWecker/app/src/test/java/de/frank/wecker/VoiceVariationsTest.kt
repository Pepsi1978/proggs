package de.frank.wecker

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VoiceVariationsTest {
    @Test fun ideaBoundariesSurviveStorageAndOnlySeparateWholeIdeas() = runBlocking {
        val variants = VoiceVariations.buildGroups(listOf(
            SpeechGroup("IDEAS", listOf("erste Idee Absatz 1", "erste Idee Absatz 2")),
            SpeechGroup("IDEAS", listOf("zweite Idee")),
            SpeechGroup("TEXT", listOf("Erinnerung"))), { text, _ -> PreparedAudio(text, speed = 1.3f) })
        val alarm = Alarm.from(Alarm(steps = listOf(Step.IDEAS, Step.TEXT), voiceVariants = variants).json())
        val clips = AlarmPlaylist.build(alarm, mapOf("classic" to "tone")) { true }
        assertEquals(List(6) { listOf(0L, 1500L, 1500L, 2000L) }.flatten(), clips.map { it.pauseAfterMillis })
        assertEquals(listOf(false, true, true), alarm.voiceVariants.first().steps.getValue("IDEAS").map { it.endOfIdea })
        assertFalse(PreparedAudio.from(org.json.JSONObject().put("path", "legacy")).endOfIdea)
    }
    @Test fun reminderPausesOnlyAfterTheWholeTextInEveryVariantIncludingTheWrap() {
        val alarm = Alarm(steps = listOf(Step.TEXT, Step.MUSIC), music = "song",
            voiceVariants = List(6) { VoiceVariant(mapOf("TEXT" to listOf(PreparedAudio("first"), PreparedAudio("last")))) })
        val clips = AlarmPlaylist.build(alarm, mapOf("classic" to "tone")) { true }
        assertEquals(List(6) { listOf(0L, 2000L, 0L) }.flatten(), clips.map { it.pauseAfterMillis })
        val legacy = AlarmPlaylist.build(Alarm(steps = listOf(Step.TEXT), prepared = mapOf("TEXT" to listOf("old"))), mapOf("classic" to "tone")) { true }
        assertEquals(2000L, legacy.single().pauseAfterMillis)
    }
    @Test fun eachIdeaIsFinishedInSixVariantsBeforeTheNextIdea() = runBlocking {
        val calls = mutableListOf<Pair<String, Int>>()
        val result = VoiceVariations.buildGroups(listOf(SpeechGroup("IDEAS", listOf("Idee 1 Absatz 1", "Idee 1 Absatz 2")), SpeechGroup("IDEAS", listOf("Idee 2"))), { text, variant ->
            calls += text to variant; PreparedAudio("$text-$variant")
        })
        assertEquals(18, calls.size)
        assertTrue(calls.take(12).all { it.first.startsWith("Idee 1") })
        assertTrue(calls.drop(12).all { it.first == "Idee 2" })
        assertEquals(listOf("Idee 1 Absatz 1-0", "Idee 1 Absatz 2-0", "Idee 2-0"), result.first().steps.getValue("IDEAS").map { it.path })
    }
    @Test fun eachParagraphIsGeneratedSixTimesInOrder() = runBlocking {
        val calls = mutableListOf<Pair<String, Int>>()
        val variants = VoiceVariations.build(linkedMapOf("TEXT" to listOf("Erster Absatz.", "Zweiter Absatz.")), { text, variation ->
            calls += text to variation
            PreparedAudio("$variation/$text", VoiceVariations.rate(1f, variation), "my-voice")
        })
        assertEquals(6, variants.size)
        assertEquals(12, calls.size)
        repeat(6) { index -> assertEquals(listOf("Erster Absatz." to index, "Zweiter Absatz." to index), calls.subList(index * 2, index * 2 + 2)) }
        assertEquals(6, variants.map { it.steps.getValue("TEXT").first().path }.distinct().size)
    }
    @Test fun sixRatesRemainDistinctAtBothTempoLimits() {
        listOf(.5f, 1f, 2f).forEach { base ->
            val rates = (0..5).map { VoiceVariations.rate(base, it) }
            assertEquals(6, rates.distinct().size)
            assertTrue(rates.all { it in .5f..2f })
            assertEquals(base, rates.first())
        }
    }
    @Test fun completeParagraphsRemainIntact() {
        assertEquals(listOf("Absatz eins mit einer Zeile mehr.", "Absatz zwei."),
            SpeechPreparation.chunks("Absatz eins\nmit einer Zeile mehr.\n\nAbsatz zwei."))
    }
    @Test fun playlistCyclesVariationsWithoutShorteningMusic() {
        val variants = (1..6).map { VoiceVariant(mapOf("TEXT" to listOf(PreparedAudio("paragraph-$it")))) }
        val alarm = Alarm(steps = listOf(Step.TEXT, Step.MUSIC), text = "Test", music = "whole-song", voiceVariants = variants)
        val clips = AlarmPlaylist.build(alarm, mapOf("classic" to "tone", "chime" to "cue")) { true }
        assertEquals(12, clips.size)
        assertEquals((1..6).flatMap { listOf(it, it) }, clips.map { it.variation })
        assertEquals((1..6).flatMap { listOf("paragraph-$it", "whole-song") }, clips.map { it.audio.path })
    }
    @Test fun oldSingleVariantAlarmsRemainPlayable() {
        val clips = AlarmPlaylist.build(Alarm(steps = listOf(Step.TEXT), prepared = mapOf("TEXT" to listOf("old")), preparedSpeed = 1.2f), mapOf("classic" to "tone")) { true }
        assertEquals("old", clips.single().audio.path)
        assertEquals(1.2f, clips.single().audio.speed)
    }
    @Test fun fallbackDoesNotApplyAlibabaTempoTwice() {
        val alarm = Alarm(steps = listOf(Step.TEXT), voiceVariants = listOf(VoiceVariant(mapOf("TEXT" to listOf(
            PreparedAudio("own.wav", .8f, "qwen_clone"), PreparedAudio("edge.mp3", 1f, "edge", true))))))
        val clips = AlarmPlaylist.build(alarm, mapOf("classic" to "tone")) { true }
        assertEquals(listOf(.8f, 1f), clips.map { it.audio.speed })
        assertTrue(clips.last().audio.fallback)
    }
    @Test fun missingParagraphUsesLocalAlarmInsteadOfSilence() {
        val alarm = Alarm(steps = listOf(Step.TEXT), prepared = mapOf("TEXT" to listOf("missing")))
        val clip = AlarmPlaylist.build(alarm, mapOf("classic" to "tone")) { it == "tone" }.single()
        assertEquals("tone", clip.audio.path)
        assertTrue(clip.audio.fallback)
    }
    @Test fun variantsSurviveSerializationWithIndividualSpeeds() {
        val alarm = Alarm(voiceVariants = listOf(VoiceVariant(mapOf("TEXT" to listOf(PreparedAudio("one", .95f, "clone"), PreparedAudio("two", 1f, "edge", true))))))
        assertEquals(alarm, Alarm.from(alarm.json()))
    }
}
