package de.frank.stacklabor.werftstudio.service.tts

/**
 * Every voice the app can speak with, taken over from PerfectMoment so both apps offer the
 * same choice.
 *
 * Microsoft Edge needs no key at all, Google Chirp 3 HD needs the Google Cloud key, and the
 * cloned voices live in the Alibaba account and are fetched at runtime by [QwenVoiceDirectory].
 */

enum class VoiceGender { WEIBLICH, MAENNLICH }

data class TtsVoice(
    val id: String,
    val name: String,
    val gender: VoiceGender,
) {
    /** What the settings screen shows, e.g. `Seraphina (weiblich)`. */
    val label: String
        get() = "$name (${if (gender == VoiceGender.WEIBLICH) "weiblich" else "männlich"})"
}

object TtsVoices {
    val edge = listOf(
        TtsVoice("de-DE-SeraphinaMultilingualNeural", "Seraphina", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-KatjaNeural", "Katja", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-AmalaNeural", "Amala", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-FlorianMultilingualNeural", "Florian", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-ConradNeural", "Conrad", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-KillianNeural", "Killian", VoiceGender.MAENNLICH),
    )

    val google = listOf(
        TtsVoice("de-DE-Chirp3-HD-Achernar", "Achernar", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Aoede", "Aoede", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Autonoe", "Autonoe", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Despina", "Despina", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Erinome", "Erinome", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Gacrux", "Gacrux", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Kore", "Kore", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Laomedeia", "Laomedeia", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Leda", "Leda", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Sulafat", "Sulafat", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Zephyr", "Zephyr", VoiceGender.WEIBLICH),
        TtsVoice("de-DE-Chirp3-HD-Achird", "Achird", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Algenib", "Algenib", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Algieba", "Algieba", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Alnilam", "Alnilam", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Charon", "Charon", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Enceladus", "Enceladus", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Fenrir", "Fenrir", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Iapetus", "Iapetus", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Orus", "Orus", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Puck", "Puck", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Sadachbia", "Sadachbia", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Sadaltager", "Sadaltager", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Schedar", "Schedar", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Umbriel", "Umbriel", VoiceGender.MAENNLICH),
        TtsVoice("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi", VoiceGender.MAENNLICH),
    )

    /** The catalogue of a provider — empty for the cloned voices, which come from the account. */
    fun of(provider: TtsProviderId): List<TtsVoice> = when (provider) {
        TtsProviderId.EDGE -> edge
        TtsProviderId.GOOGLE_CLOUD -> google
        TtsProviderId.QWEN -> emptyList()
    }

    /** Finds a voice by its id in any catalogue. */
    fun byId(voiceId: String): TtsVoice? = (edge + google).firstOrNull { it.id == voiceId }
}
