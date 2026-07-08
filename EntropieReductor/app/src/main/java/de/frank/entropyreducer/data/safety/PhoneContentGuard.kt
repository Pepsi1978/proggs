package de.frank.entropyreducer.data.safety

import java.util.Locale

/**
 * Verhindert, dass interne Second-Brain-/Bibliothekar-Arbeitsartefakte als persönliche
 * Handy-Inhalte auftauchen. Gilt für alle Nutzerlisten: Entropie, Thesen, Tagebuch,
 * Mental, Gewohnheiten und Aufgaben.
 */
object PhoneContentGuard {
    private val blockedMarkers = listOf(
        "kurzgerüst",
        "kurzgeruest",
        "kurz gerüst",
        "kurz geruest",
        "nachtschichtbibliothek",
        "nachtschichtbibliothekar",
        "nachtschichtbibliotheker",
        "nacht schicht bibliothek",
        "nacht schicht bibliothekar",
        "nacht schicht bibliotheker",
        "nachtschech bibliothek",
        "nachtschech bibliothekar",
        "nachtschech bibliotheker",
        "nachtschechbibliothek",
        "nachtschechbibliothekar",
        "nachtschechbibliotheker",
        "nachschichtbibliothek",
        "nachschichtbibliothekar",
        "nachschichtbibliotheker",
    )

    fun isSecondBrainWorkArtifact(title: String?, text: String?): Boolean {
        val haystacks = normalizedVariants(listOfNotNull(title, text).joinToString(separator = "\n"))
        val markers = blockedMarkers.flatMap { normalizedVariants(it) }.toSet()
        return markers.any { marker -> haystacks.any { haystack -> marker in haystack } }
    }

    private fun normalizedVariants(raw: String): Set<String> {
        val lower = raw.lowercase(Locale.GERMANY)
        val wordSpaced = lower.replace(Regex("[^\\p{L}\\p{Nd}]+"), " ").trim()
        return setOf(lower, wordSpaced, wordSpaced.replace(" ", ""))
    }
}
