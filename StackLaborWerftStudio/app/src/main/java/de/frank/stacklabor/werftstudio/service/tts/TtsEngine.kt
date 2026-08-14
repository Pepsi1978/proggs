package de.frank.stacklabor.werftstudio.service.tts

class TtsEngine(providers: List<TtsProvider>) {
    private val providersById = providers.associateBy(TtsProvider::id)

    init {
        require(providersById.keys.containsAll(TtsProviderId.entries)) {
            "Für Edge, Google Cloud und Qwen muss jeweils ein TTS-Anbieter registriert sein."
        }
    }

    suspend fun synthesizeParagraph(text: String, configuration: TtsConfiguration): SynthesizedAudio {
        val provider = providersById.getValue(configuration.provider)
        val configuredVoice = configuration.voiceId.trim()
        val voice = when (configuration.provider) {
            TtsProviderId.EDGE -> configuredVoice.takeIf { it.startsWith("de-DE-") && it.endsWith("Neural") }
                ?: TtsConfiguration.DEFAULT_EDGE_VOICE
            TtsProviderId.GOOGLE_CLOUD -> configuredVoice.takeIf { it.contains("-Chirp3-HD-") }
                ?: TtsConfiguration.DEFAULT_GOOGLE_VOICE
            TtsProviderId.QWEN -> configuredVoice.takeUnless {
                it == TtsConfiguration.DEFAULT_EDGE_VOICE || it == TtsConfiguration.DEFAULT_GOOGLE_VOICE
            }.orEmpty()
        }
        return provider.synthesize(
            TtsSynthesisRequest(text = text, voiceId = voice, speechRate = configuration.speechRate),
        )
    }
}
