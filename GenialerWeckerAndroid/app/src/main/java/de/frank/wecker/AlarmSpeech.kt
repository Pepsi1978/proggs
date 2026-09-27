package de.frank.wecker

/** Individuelle Auswahl auf einen Snapshot anwenden, ohne globale Einstellungen zu verändern. */
internal fun Alarm.resolveVoice(defaults: SyntheseStimme): SyntheseStimme = SyntheseStimme(
    stimme = voiceId.takeIf { voiceProvider == LokaleStimmen.PROVIDER && it.isNotBlank() } ?: defaults.stimme,
    ttsSpeechRate = speechRate ?: defaults.ttsSpeechRate,
)
