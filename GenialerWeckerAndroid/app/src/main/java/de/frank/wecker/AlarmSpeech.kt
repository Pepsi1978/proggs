package de.frank.wecker

/**
 * Die Stimme für diesen Wecker: seine Sprache, dazu die eigene Stimmwahl nur, wenn sie zu dieser Sprache gehört;
 * sonst die bevorzugte Stimme dieser Sprache aus den Einstellungen. Globale Einstellungen bleiben unverändert.
 */
internal fun Alarm.resolveVoice(defaults: SyntheseStimme): SyntheseStimme {
    val code = Sprachen.gueltig(sprache)
    val basis = defaults.fuerSprache(code)
    val eigene = voiceId.takeIf { voiceProvider == PremiumKatalog.PROVIDER && PremiumKatalog.istPremium(it) && it.substringBefore('-').equals(code, ignoreCase = true) }
    return basis.copy(stimme = eigene ?: basis.stimme, ttsSpeechRate = speechRate ?: defaults.ttsSpeechRate)
}
