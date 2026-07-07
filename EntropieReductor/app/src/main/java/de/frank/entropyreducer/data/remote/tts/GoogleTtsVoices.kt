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

    fun findByName(name: String): GoogleTtsVoice? = ALL.firstOrNull { it.name == name }

    fun displayNameFor(name: String): String =
        findByName(name)?.displayName ?: name
}
