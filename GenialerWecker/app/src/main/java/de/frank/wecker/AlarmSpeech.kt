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
        modellStimme = if (provider == TtsProvider.MODELL.id) voiceId else defaults.modellStimme,
    ).let { basis ->
        // Schlüssel, Region und Schlüsselwahl bleiben global; nur die Stimme kommt vom Wecker.
        defaults.copy(
            ttsProvider = basis.ttsProvider, googleTtsVoice = basis.googleTtsVoice, qwenTtsVoiceId = basis.qwenTtsVoiceId,
            qwenStandardVoice = basis.qwenStandardVoice, ttsSpeechRate = basis.ttsSpeechRate, edgeTtsVoice = basis.edgeTtsVoice,
            modellStimme = basis.modellStimme,
            geminiFlashTtsVoice = if (provider == TtsProvider.GEMINI_FLASH.id) voiceId else defaults.geminiFlashTtsVoice,
            geminiLiteTtsVoice = if (provider == TtsProvider.GEMINI_LITE.id) voiceId else defaults.geminiLiteTtsVoice,
            azureTtsVoice = if (provider == TtsProvider.AZURE.id) voiceId else defaults.azureTtsVoice,
        )
    }
}
