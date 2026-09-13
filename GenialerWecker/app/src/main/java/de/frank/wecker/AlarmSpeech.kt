package de.frank.wecker

import de.frank.genialeideen.speech.SyntheseStimme
import de.frank.genialeideen.tts.TtsProvider

/** Individuelle Auswahl auf einen Snapshot anwenden, ohne globale Einstellungen zu verändern. */
internal fun Alarm.resolveVoice(defaults: SyntheseStimme): SyntheseStimme {
    val provider = voiceProvider.takeIf { voiceId.isNotBlank() }?.ifBlank { null }
    return SyntheseStimme(
        ttsProvider = provider ?: defaults.ttsProvider,
        googleTtsApiKey = defaults.googleTtsApiKey,
        googleTtsVoice = if (provider == TtsProvider.GOOGLE_CLOUD.id) voiceId else defaults.googleTtsVoice,
        qwenTtsApiKey = defaults.qwenTtsApiKey,
        qwenTtsVoiceId = if (provider == TtsProvider.QWEN_CLONE.id) voiceId else defaults.qwenTtsVoiceId,
        qwenStandardVoice = if (provider == TtsProvider.QWEN.id) voiceId else defaults.qwenStandardVoice,
        ttsSpeechRate = speechRate ?: defaults.ttsSpeechRate,
        immerDeutschVorlesen = defaults.immerDeutschVorlesen,
        edgeTtsVoice = if (provider == TtsProvider.EDGE.id) voiceId else defaults.edgeTtsVoice,
    )
}
