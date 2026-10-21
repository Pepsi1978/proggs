package de.frank.jarvis.tts

enum class TtsProvider(val id: String, val label: String, val kurz: String) {
    QWEN_CLONE("qwen_clone", "Meine Stimme", "Meine"),
    QWEN("qwen", "Alibaba", "Alibaba"),
    GOOGLE_CLOUD("google_cloud", "Google Chirp 3 HD", "Google"),
    GEMINI_FLASH("gemini_flash", "Gemini 3.8 Flash TTS", "Gemini"),
    GEMINI_LITE("gemini_lite", "Gemini 3.8 Flash-Lite TTS", "Gemini Lite"),
    EDGE("edge_tts", "Microsoft Edge", "Edge"),
    AZURE("azure_neural", "Azure Neural", "Azure"),
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

    /**
     * Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD. Die Kennung trägt den Anbieter vorn
     * („gemini_flash:Leda“), damit [anbieterZu] Flash, Flash-Lite und Chirp auseinanderhält.
     */
    val geminiFlashVoices = geminiStimmen(TtsProvider.GEMINI_FLASH)
    val geminiLiteVoices = geminiStimmen(TtsProvider.GEMINI_LITE)
    val DEFAULT_GEMINI_FLASH_VOICE = "${TtsProvider.GEMINI_FLASH.id}:Leda"
    val DEFAULT_GEMINI_LITE_VOICE = "${TtsProvider.GEMINI_LITE.id}:Leda"

    private fun geminiStimmen(anbieter: TtsProvider) =
        googleVoices.map { TtsVoice("${anbieter.id}:${it.name}", it.name, it.gender) }

    /** Der Stimmname, den Gemini erwartet („Leda“). */
    fun geminiName(stimmeId: String): String = stimmeId.substringAfter(':')

    /** Dieselbe Stimme bei Chirp 3 HD — für den Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert. */
    fun chirpZu(stimmeId: String): String = "de-DE-Chirp3-HD-${geminiName(stimmeId)}"

    /**
     * Azure AI Speech (eigenes Konto, Schlüssel + Region): dieselben Neural-Stimmen wie Edge, offiziell und
     * mit allen deutschen Stimmen, dazu die HD-Stimmen (DragonHD). Die Kennung trägt „azure:“ vorn, damit
     * [anbieterZu] sie von den gleichnamigen Edge-Stimmen unterscheidet. HD zählt nicht zum kostenlosen F0-Kontingent.
     */
    val azureVoices = listOf(
        TtsVoice("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD", VoiceGender.MALE),
        TtsVoice("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-KatjaNeural", "Katja", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-AmalaNeural", "Amala", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-ElkeNeural", "Elke", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-GiselaNeural", "Gisela (Kind)", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-KlarissaNeural", "Klarissa", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-LouisaNeural", "Louisa", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-MajaNeural", "Maja", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-TanjaNeural", "Tanja", VoiceGender.FEMALE),
        TtsVoice("azure:de-DE-FlorianMultilingualNeural", "Florian", VoiceGender.MALE),
        TtsVoice("azure:de-DE-ConradNeural", "Conrad", VoiceGender.MALE),
        TtsVoice("azure:de-DE-KillianNeural", "Killian", VoiceGender.MALE),
        TtsVoice("azure:de-DE-BerndNeural", "Bernd", VoiceGender.MALE),
        TtsVoice("azure:de-DE-ChristophNeural", "Christoph", VoiceGender.MALE),
        TtsVoice("azure:de-DE-KasperNeural", "Kasper", VoiceGender.MALE),
        TtsVoice("azure:de-DE-KlausNeural", "Klaus", VoiceGender.MALE),
        TtsVoice("azure:de-DE-RalfNeural", "Ralf", VoiceGender.MALE),
    )
    const val DEFAULT_AZURE_VOICE = "azure:de-DE-SeraphinaMultilingualNeural"
    const val DEFAULT_AZURE_REGION = "westeurope"

    /** Der Stimmname, den Azure erwartet („de-DE-KatjaNeural“). */
    fun azureName(stimmeId: String): String = stimmeId.removePrefix("azure:")

    fun istAzureHd(stimmeId: String): Boolean = stimmeId.contains("DragonHD")

    /** Findet den Anbieter zu einer Stimm-Kennung aus dem festen Katalog. */
    fun anbieterZu(stimmeId: String): TtsProvider? = when {
        edgeVoices.any { it.id == stimmeId } -> TtsProvider.EDGE
        azureVoices.any { it.id == stimmeId } -> TtsProvider.AZURE
        googleVoices.any { it.id == stimmeId } -> TtsProvider.GOOGLE_CLOUD
        geminiFlashVoices.any { it.id == stimmeId } -> TtsProvider.GEMINI_FLASH
        geminiLiteVoices.any { it.id == stimmeId } -> TtsProvider.GEMINI_LITE
        qwenVoices.any { it.id == stimmeId } -> TtsProvider.QWEN
        else -> null
    }
}
