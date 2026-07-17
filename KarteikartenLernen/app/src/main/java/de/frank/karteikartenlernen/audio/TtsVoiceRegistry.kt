package de.frank.karteikartenlernen.audio

object TtsVoiceRegistry {
    enum class Gender { FEMALE, MALE }
    enum class Tier { MULTILINGUAL, STANDARD }

    data class Voice(
        val id: String,
        val name: String,
        val gender: Gender,
        val tier: Tier = Tier.STANDARD,
    )

    const val DEFAULT_VOICE_ID = "de-DE-SeraphinaMultilingualNeural"

    val voices = listOf(
        Voice(DEFAULT_VOICE_ID, "Seraphina", Gender.FEMALE, Tier.MULTILINGUAL),
        Voice("de-DE-FlorianMultilingualNeural", "Florian", Gender.MALE, Tier.MULTILINGUAL),
        Voice("de-DE-KatjaNeural", "Katja", Gender.FEMALE),
        Voice("de-DE-KillianNeural", "Killian", Gender.MALE),
        Voice("de-DE-ConradNeural", "Conrad", Gender.MALE),
        Voice("de-DE-AmalaNeural", "Amala", Gender.FEMALE),
    )

    fun displayName(voice: Voice): String {
        val gender = if (voice.gender == Gender.FEMALE) "weiblich" else "männlich"
        return if (voice.tier == Tier.MULTILINGUAL) "★ ${voice.name} — $gender" else "${voice.name} — $gender"
    }

    fun resolveVoiceId(value: String): String = when (value) {
        "Seraphina (Multilingual)" -> DEFAULT_VOICE_ID
        "Florian (Multilingual)" -> "de-DE-FlorianMultilingualNeural"
        "Katja" -> "de-DE-KatjaNeural"
        "Killian" -> "de-DE-KillianNeural"
        "Conrad" -> "de-DE-ConradNeural"
        "Amala" -> "de-DE-AmalaNeural"
        else -> voices.firstOrNull { it.id == value }?.id ?: DEFAULT_VOICE_ID
    }

    fun voice(value: String): Voice {
        val id = resolveVoiceId(value)
        return voices.first { it.id == id }
    }

    fun extractLocale(voiceId: String): String {
        val parts = voiceId.split("-")
        return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else "de-DE"
    }
}
