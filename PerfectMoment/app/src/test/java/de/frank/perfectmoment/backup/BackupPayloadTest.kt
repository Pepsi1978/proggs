package de.frank.perfectmoment.backup

import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SessionWithQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPayloadTest {
    private fun payload(profiles: VoiceProfiles) = BackupPayload(
        dataVersion = 7,
        createdAt = 1_000L,
        device = "Testgerät",
        hooks = emptyList(),
        skills = emptyList(),
        sessions = listOf(
            SessionWithQuestions(
                session = SessionEntity(
                    id = 3,
                    topic = "Ein sehr langer Wunsch",
                    startedAt = 500L,
                    durationMin = 20,
                    voiceName = "de-DE-KatjaNeural",
                    providerId = "edge_tts",
                    pauseRep = 3,
                    pauseNext = 6,
                    reps = 2,
                    summary = "Kurztitel",
                    summaryManual = true,
                    voiceProviderOverride = "qwen_clone",
                    voiceOverride = "qwen-tts-vc-FrankHD-voice-1-a",
                ),
                questions = emptyList(),
            ),
        ),
        voiceProfiles = profiles,
    )

    @Test
    fun `carries the own voices and their names through a round trip`() {
        val profiles = VoiceProfiles(
            names = mapOf("qwen-tts-vc-FrankHD-voice-1-a" to "Frank Abend"),
            selectedVoiceId = "qwen-tts-vc-FrankHD-voice-1-a",
            providerId = "qwen_clone",
        )

        val restored = BackupPayload.fromJson(payload(profiles).toJson())

        assertNotNull(restored)
        assertEquals(profiles.names, restored!!.voiceProfiles.names)
        assertEquals(profiles.selectedVoiceId, restored.voiceProfiles.selectedVoiceId)
        assertEquals(profiles.providerId, restored.voiceProfiles.providerId)
    }

    @Test
    fun `keeps the title and the voice assigned to a history entry`() {
        val restored = BackupPayload.fromJson(payload(VoiceProfiles()).toJson())

        val session = restored!!.sessions.single().session
        assertEquals("Kurztitel", session.summary)
        // Without the flag the AI would rename the entry after a restore.
        assertEquals(true, session.summaryManual)
        assertEquals("qwen_clone", session.voiceProviderOverride)
        assertEquals("qwen-tts-vc-FrankHD-voice-1-a", session.voiceOverride)
    }

    @Test
    fun `never writes an account key into the backup`() {
        val json = payload(
            VoiceProfiles(names = mapOf("id" to "Name"), selectedVoiceId = "id", providerId = "qwen_clone"),
        ).toJson()

        assertTrue("Der Sicherung fehlen die Stimmen", json.contains("voiceProfiles"))
        assertTrue("Ein Schlüsselfeld ist in der Sicherung gelandet", !json.contains("apiKey", ignoreCase = true))
        assertTrue("Ein Schlüsselfeld ist in der Sicherung gelandet", !json.contains("Key\"", ignoreCase = true))
    }

    @Test
    fun `reads a backup written before the voices existed`() {
        val old = """{"formatVersion":1,"dataVersion":1,"createdAt":1,"device":"alt","hooks":[],"skills":[],"sessions":[]}"""

        val restored = BackupPayload.fromJson(old)

        assertNotNull(restored)
        assertEquals(emptyMap<String, String>(), restored!!.voiceProfiles.names)
        assertEquals("", restored.voiceProfiles.selectedVoiceId)
    }
}
