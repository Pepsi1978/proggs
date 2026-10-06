package de.frank.newskompass.news

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

/** Prüft das Ereignisdatum, unabhängig vom Abrufdatum oder einem neu aufgelegten Artikel. */
object NachrichtenAlter {
    private val deutscheZeit = ZoneId.of("Europe/Berlin")

    fun fruehestens(jetzt: Long, tage: Int): Long = jetzt - TimeUnit.DAYS.toMillis(tage.toLong())

    /**
     * Nur belegte ISO-Zeitangaben zulassen. Ist nur der Tag bekannt, zählt vorsichtig dessen
     * Beginn in deutscher Zeit; unklare, fehlende oder zukünftige Angaben fallen heraus.
     */
    fun ereignisZeit(angabe: String): Long? {
        val text = angabe.trim()
        return try {
            Instant.parse(text).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(text).atStartOfDay(deutscheZeit).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    fun istAktuell(ereignisUm: Long?, jetzt: Long, tage: Int): Boolean =
        ereignisUm != null && ereignisUm in fruehestens(jetzt, tage)..jetzt
}
