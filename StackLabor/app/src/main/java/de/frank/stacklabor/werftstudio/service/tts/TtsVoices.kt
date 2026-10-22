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

    /**
     * Gemini 3.8 TTS has the same 30 voice names as Chirp 3 HD. The id carries the provider in front
     * ("gemini_flash:Leda") so Flash, Flash-Lite and Chirp stay apart.
     */
    const val GEMINI_FLASH_ID = "gemini_flash"
    const val GEMINI_LITE_ID = "gemini_lite"
    const val DEFAULT_GEMINI_FLASH_VOICE = "$GEMINI_FLASH_ID:Leda"
    const val DEFAULT_GEMINI_LITE_VOICE = "$GEMINI_LITE_ID:Leda"
    val geminiFlash = geminiVoices(GEMINI_FLASH_ID)
    val geminiLite = geminiVoices(GEMINI_LITE_ID)

    private fun geminiVoices(providerId: String) =
        google.map { TtsVoice("$providerId:${it.name}", it.name, it.gender) }

    /** The voice name Gemini expects ("Leda"). */
    fun geminiName(voiceId: String): String = voiceId.substringAfter(':')

    /** The same voice at Chirp 3 HD — for the fallback when the free Gemini key fails. */
    fun chirpFor(voiceId: String): String = "de-DE-Chirp3-HD-${geminiName(voiceId)}"

    /**
     * Azure AI Speech (own account, key + region): the official Neural voices plus the HD voices (DragonHD).
     * The id carries "azure:" in front so it differs from the Edge voices of the same name.
     */
    val azure = listOf(
        TtsVoice("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-KatjaNeural", "Katja", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-AmalaNeural", "Amala", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-ElkeNeural", "Elke", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-GiselaNeural", "Gisela (Kind)", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-KlarissaNeural", "Klarissa", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-LouisaNeural", "Louisa", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-MajaNeural", "Maja", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-TanjaNeural", "Tanja", VoiceGender.WEIBLICH),
        TtsVoice("azure:de-DE-FlorianMultilingualNeural", "Florian", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-ConradNeural", "Conrad", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-KillianNeural", "Killian", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-BerndNeural", "Bernd", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-ChristophNeural", "Christoph", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-KasperNeural", "Kasper", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-KlausNeural", "Klaus", VoiceGender.MAENNLICH),
        TtsVoice("azure:de-DE-RalfNeural", "Ralf", VoiceGender.MAENNLICH),
    )
    const val DEFAULT_AZURE_VOICE = "azure:de-DE-SeraphinaMultilingualNeural"
    const val DEFAULT_AZURE_REGION = "westeurope"

    /** The voice name Azure expects ("de-DE-KatjaNeural"). */
    fun azureName(voiceId: String): String = voiceId.removePrefix("azure:")

    fun isAzureHd(voiceId: String): Boolean = voiceId.contains("DragonHD")

    /**
     * The region name for the address ("westeurope"). Also accepts the endpoint pasted from the portal
     * ("https://westeurope.api.cognitive.microsoft.com/"); null if nothing usable is in it.
     */
    fun azureRegion(input: String): String? {
        val raw = input.trim().lowercase().ifBlank { DEFAULT_AZURE_REGION }
        val fromEndpoint = Regex("^(?:https?://)?([a-z0-9]+)\\.(?:api\\.cognitive|tts\\.speech)\\.").find(raw)?.groupValues?.get(1)
        return (fromEndpoint ?: raw).takeIf { Regex("^[a-z0-9]{2,40}$").matches(it) }
    }

    /** The default voice of a provider; empty for the cloned voices. */
    fun defaultFor(provider: TtsProviderId): String = when (provider) {
        TtsProviderId.EDGE -> TtsConfiguration.DEFAULT_EDGE_VOICE
        TtsProviderId.GOOGLE_CLOUD -> TtsConfiguration.DEFAULT_GOOGLE_VOICE
        TtsProviderId.QWEN -> ""
        TtsProviderId.GEMINI_FLASH -> DEFAULT_GEMINI_FLASH_VOICE
        TtsProviderId.GEMINI_LITE -> DEFAULT_GEMINI_LITE_VOICE
        TtsProviderId.AZURE -> DEFAULT_AZURE_VOICE
    }

    /** The catalogue of a provider — empty for the cloned voices, which come from the account. */
    fun of(provider: TtsProviderId): List<TtsVoice> = when (provider) {
        TtsProviderId.EDGE -> edge
        TtsProviderId.GOOGLE_CLOUD -> google
        TtsProviderId.QWEN -> emptyList()
        TtsProviderId.GEMINI_FLASH -> geminiFlash
        TtsProviderId.GEMINI_LITE -> geminiLite
        TtsProviderId.AZURE -> azure
    }

    /** Finds a voice by its id in any catalogue. */
    fun byId(voiceId: String): TtsVoice? = (edge + google + geminiFlash + geminiLite + azure).firstOrNull { it.id == voiceId }
}
