package de.frank.entropyreducer.data.safety

import java.util.Locale

/**
 * Verhindert, dass interne Second-Brain-/Bibliothekar-Arbeitsartefakte als persönliche
 * Handy-Inhalte auftauchen. Mentals gehören ausschließlich Frank.
 */
object PhoneContentGuard {
    private val blockedMarkers = listOf(
        "kurzgerüst",
        "kurzgeruest",
        "nachtschichtbibliothek",
        "nachtschichtbibliothekar",
        "nachtschichtbibliotheker",
        "nachschichtbibliothek",
        "nachschichtbibliothekar",
        "nachschichtbibliotheker",
    )

    fun isSecondBrainWorkArtifact(title: String?, text: String?): Boolean {
        val haystack = listOfNotNull(title, text)
            .joinToString(separator = "\n")
            .lowercase(Locale.GERMANY)
        return blockedMarkers.any { marker -> marker in haystack }
    }
}
