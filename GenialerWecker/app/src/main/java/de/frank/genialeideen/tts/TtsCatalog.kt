package de.frank.genialeideen.tts

enum class TtsProvider(val id: String, val label: String, val kurz: String) {
    QWEN_CLONE("qwen_clone", "Meine Stimme", "Meine"),
    QWEN("qwen", "Alibaba", "Alibaba"),
    GOOGLE_CLOUD("google_cloud", "Google Chirp 3 HD", "Google"),
    EDGE("edge_tts", "Microsoft Edge", "Edge"),
}

enum class VoiceGender {
    FEMALE,
    MALE,
}

data class TtsVoice(
    val id: String,
    val name: String,
    val gender: VoiceGender,
)

object TtsCatalog {
    const val DEFAULT_EDGE_VOICE = "de-DE-SeraphinaMultilingualNeural"
    const val DEFAULT_GOOGLE_VOICE = "de-DE-Chirp3-HD-Kore"
    const val DEFAULT_QWEN_VOICE = "Cherry"

    /** Edge braucht keinen Schlüssel und ist deshalb der garantierte Rückfall (Kapitel 4.6). */
    val DEFAULT_PROVIDER = TtsProvider.EDGE

    val providers = TtsProvider.entries

    val edgeVoices = listOf(
        TtsVoice("de-DE-SeraphinaMultilingualNeural", "Seraphina", VoiceGender.FEMALE),
        TtsVoice("de-DE-KatjaNeural", "Katja", VoiceGender.FEMALE),
        TtsVoice("de-DE-AmalaNeural", "Amala", VoiceGender.FEMALE),
        TtsVoice("de-DE-FlorianMultilingualNeural", "Florian", VoiceGender.MALE),
        TtsVoice("de-DE-KillianNeural", "Killian", VoiceGender.MALE),
        TtsVoice("de-DE-ConradNeural", "Conrad", VoiceGender.MALE),
    )

    /**
     * Die fertigen Standardstimmen von Alibaba (Qwen TTS). Sie kommen über dasselbe Konto wie
     * die geklonten Stimmen, brauchen aber kein Klonen — nur den DashScope-Schlüssel.
     */
    val qwenVoices = listOf(
        TtsVoice("Cherry", "Cherry", VoiceGender.FEMALE),
        TtsVoice("Chelsie", "Chelsie", VoiceGender.FEMALE),
        TtsVoice("Serena", "Serena", VoiceGender.FEMALE),
        TtsVoice("Jada", "Jada", VoiceGender.FEMALE),
        TtsVoice("Sunny", "Sunny", VoiceGender.FEMALE),
        TtsVoice("Ethan", "Ethan", VoiceGender.MALE),
        TtsVoice("Dylan", "Dylan", VoiceGender.MALE),
    )

    val googleVoices = listOf(
        TtsVoice("de-DE-Chirp3-HD-Achernar", "Achernar", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Aoede", "Aoede", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Autonoe", "Autonoe", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Despina", "Despina", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Erinome", "Erinome", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Gacrux", "Gacrux", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Kore", "Kore", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Laomedeia", "Laomedeia", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Leda", "Leda", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Sulafat", "Sulafat", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Zephyr", "Zephyr", VoiceGender.FEMALE),
        TtsVoice("de-DE-Chirp3-HD-Achird", "Achird", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Algenib", "Algenib", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Algieba", "Algieba", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Alnilam", "Alnilam", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Charon", "Charon", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Enceladus", "Enceladus", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Fenrir", "Fenrir", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Iapetus", "Iapetus", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Orus", "Orus", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Puck", "Puck", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Sadachbia", "Sadachbia", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Sadaltager", "Sadaltager", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Schedar", "Schedar", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Umbriel", "Umbriel", VoiceGender.MALE),
        TtsVoice("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi", VoiceGender.MALE),
    )

    /** Findet den Anbieter zu einer Stimm-Kennung aus dem festen Katalog. */
    fun anbieterZu(stimmeId: String): TtsProvider? = when {
        edgeVoices.any { it.id == stimmeId } -> TtsProvider.EDGE
        googleVoices.any { it.id == stimmeId } -> TtsProvider.GOOGLE_CLOUD
        qwenVoices.any { it.id == stimmeId } -> TtsProvider.QWEN
        else -> null
    }
}
