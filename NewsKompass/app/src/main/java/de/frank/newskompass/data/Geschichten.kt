package de.frank.newskompass.data

import java.util.Locale

/**
 * Erkennt, ob zwei Meldungen dieselbe Geschichte erzählen — allein aus Titel, erster Quelle und
 * Folgemeldungs-Kennzeichen. Genutzt vom Monatsrückblick und vom Abgleich doppelter Fotos.
 */
object Geschichten {

    fun gleich(titelA: String, quelleA: String?, updateA: Boolean, titelB: String, quelleB: String?, updateB: Boolean): Boolean {
        val adresseA = quelleA?.let(::normiereAdresse)
        if (adresseA != null && adresseA == quelleB?.let(::normiereAdresse)) return true
        val aehnlich = jaccard(woerter(titelA), woerter(titelB))
        return aehnlich >= 0.6 || ((updateA || updateB) && aehnlich >= 0.35)
    }

    private val STOPPWOERTER = setOf(
        "der", "die", "das", "und", "mit", "für", "von", "auf", "ist", "den", "dem", "des", "ein", "eine", "einen",
        "zum", "zur", "bei", "nach", "über", "als", "auch", "sich", "wird", "werden", "hat", "haben", "nicht",
        "mehr", "neue", "neuer", "neues", "neuen", "gegen", "aus", "vor", "wie", "noch", "jetzt", "soll",
    )

    fun woerter(titel: String): Set<String> =
        titel.lowercase(Locale.GERMANY).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 && it !in STOPPWOERTER }
            .toSet()

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }

    /** Nur Artikeladressen zählen; eine bloße Startseite würde fremde Geschichten zusammenwerfen. */
    fun normiereAdresse(adresse: String): String? {
        val ohne = adresse.substringBefore('#').substringBefore('?').lowercase(Locale.ROOT)
        val rest = ohne.substringAfter("://", ohne).removePrefix("www.")
        val host = rest.substringBefore('/')
        val pfad = rest.substringAfter('/', "").trimEnd('/')
        return if (pfad.length < 2) null else "$host/$pfad"
    }
}
