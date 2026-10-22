package de.frank.entropyreducer.data.remote.tts

/**
 * Liste der deutschen Chirp 3 HD-Stimmen von Google Cloud TTS.
 *
 * Quelle: cloud.google.com/text-to-speech/docs/chirp3-hd
 * Kosten: 1 Mio Zeichen/Monat gratis, danach $30/Mio.
 *
 * Default `de-DE-Chirp3-HD-Kore` (weiblich, warm) — uebernommen aus
 * BestJournalFrank wo dieselben Stimmen produktiv im Einsatz sind.
 */
data class GoogleTtsVoice(
    val name: String,
    val displayName: String,
    val gender: VoiceGender,
    /** Anbieter-Gruppe für die Auswahl: Chirp 3 HD, Gemini 3.8 Flash/Flash-Lite TTS oder Azure Neural. */
    val provider: String = GoogleTtsVoices.PROVIDER_CHIRP,
)

enum class VoiceGender { FEMALE, MALE }

object GoogleTtsVoices {

    const val DEFAULT_VOICE_NAME = "de-DE-Chirp3-HD-Kore"
    const val DEFAULT_LANGUAGE_CODE = "de-DE"

    /** Alle 30 deutschen Chirp 3 HD-Stimmen (14 weiblich, 16 maennlich). */
    val ALL: List<GoogleTtsVoice> = listOf(
        // Weibliche Stimmen
        GoogleTtsVoice("de-DE-Chirp3-HD-Achernar", "Achernar — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Aoede", "Aoede — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Autonoe", "Autonoe — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Despina", "Despina — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Erinome", "Erinome — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Gacrux", "Gacrux — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Kore", "Kore — weiblich (Standard)", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Laomedeia", "Laomedeia — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Leda", "Leda — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Sulafat", "Sulafat — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix — weiblich", VoiceGender.FEMALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Zephyr", "Zephyr — weiblich", VoiceGender.FEMALE),
        // Maennliche Stimmen
        GoogleTtsVoice("de-DE-Chirp3-HD-Achird", "Achird — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Algenib", "Algenib — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Algieba", "Algieba — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Alnilam", "Alnilam — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Charon", "Charon — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Enceladus", "Enceladus — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Fenrir", "Fenrir — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Iapetus", "Iapetus — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Orus", "Orus — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Puck", "Puck — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Sadachbia", "Sadachbia — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Sadaltager", "Sadaltager — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Schedar", "Schedar — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Umbriel", "Umbriel — männlich", VoiceGender.MALE),
        GoogleTtsVoice("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi — männlich", VoiceGender.MALE),
    )

    const val PROVIDER_CHIRP = "Google Chirp 3 HD"
    const val PROVIDER_GEMINI_FLASH = "Gemini 3.8 Flash TTS"
    const val PROVIDER_GEMINI_LITE = "Gemini 3.8 Flash-Lite TTS"
    const val PROVIDER_AZURE = "Azure Neural"

    const val GEMINI_FLASH_PREFIX = "gemini_flash:"
    const val GEMINI_LITE_PREFIX = "gemini_lite:"
    const val AZURE_PREFIX = "azure:"

    /**
     * Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD. Die Kennung trägt den Anbieter vorn
     * („gemini_flash:Leda“), damit Flash, Flash-Lite und Chirp auseinanderbleiben.
     */
    val GEMINI_FLASH: List<GoogleTtsVoice> = geminiVoices(GEMINI_FLASH_PREFIX, PROVIDER_GEMINI_FLASH)
    val GEMINI_LITE: List<GoogleTtsVoice> = geminiVoices(GEMINI_LITE_PREFIX, PROVIDER_GEMINI_LITE)

    private fun geminiVoices(prefix: String, provider: String) = ALL.map {
        val short = it.name.removePrefix("de-DE-Chirp3-HD-")
        GoogleTtsVoice(prefix + short, it.displayName.replace(" (Standard)", ""), it.gender, provider)
    }

    /** Azure AI Speech (eigenes Konto, Schlüssel + Region) inkl. HD-Stimmen (DragonHD, nur Stufe S0). */
    val AZURE: List<GoogleTtsVoice> = listOf(
        GoogleTtsVoice("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-KatjaNeural", "Katja — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-AmalaNeural", "Amala — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-ElkeNeural", "Elke — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-GiselaNeural", "Gisela (Kind) — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-KlarissaNeural", "Klarissa — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-LouisaNeural", "Louisa — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-MajaNeural", "Maja — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-TanjaNeural", "Tanja — weiblich", VoiceGender.FEMALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-FlorianMultilingualNeural", "Florian — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-ConradNeural", "Conrad — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-KillianNeural", "Killian — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-BerndNeural", "Bernd — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-ChristophNeural", "Christoph — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-KasperNeural", "Kasper — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-KlausNeural", "Klaus — männlich", VoiceGender.MALE, PROVIDER_AZURE),
        GoogleTtsVoice("azure:de-DE-RalfNeural", "Ralf — männlich", VoiceGender.MALE, PROVIDER_AZURE),
    )

    /** Alle wählbaren Stimmen aller Anbieter (Chirp zuerst). */
    val ALL_PROVIDERS: List<GoogleTtsVoice> = ALL + GEMINI_FLASH + GEMINI_LITE + AZURE

    fun isGemini(voiceId: String): Boolean = voiceId.startsWith(GEMINI_FLASH_PREFIX) || voiceId.startsWith(GEMINI_LITE_PREFIX)
    fun isAzure(voiceId: String): Boolean = voiceId.startsWith(AZURE_PREFIX)

    /** Der Stimmname, den Gemini erwartet („Leda“). */
    fun geminiName(voiceId: String): String = voiceId.substringAfter(':')

    /** Dieselbe Stimme bei Chirp 3 HD — für den Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert. */
    fun chirpFor(voiceId: String): String = "de-DE-Chirp3-HD-${geminiName(voiceId)}"

    /** Der Stimmname, den Azure erwartet („de-DE-KatjaNeural“). */
    fun azureName(voiceId: String): String = voiceId.removePrefix(AZURE_PREFIX)

    fun findByName(name: String): GoogleTtsVoice? = ALL_PROVIDERS.firstOrNull { it.name == name }

    fun displayNameFor(name: String): String =
        findByName(name)?.displayName ?: name
}
