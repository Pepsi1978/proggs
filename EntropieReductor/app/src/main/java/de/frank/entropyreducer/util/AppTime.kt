package de.frank.entropyreducer.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Zentrale Zeit-Konstanten der App.
 *
 * Frank-Wunsch 2026-05-13: Die App rechnet IMMER in Berliner Zeit (Europe/Berlin), unabhaengig von
 * der Geraete-Zeitzone — auch wenn das Handy auf Reisen ist oder der Benutzer absichtlich eine
 * andere Systemzone gesetzt hat. So passen Datums- grenzen in Charts und Historie immer zum
 * Berliner Kalendertag.
 *
 * Die JVM-Default-Zone wird zusaetzlich in [EntropyReducerApp.onCreate] auf Europe/Berlin gesetzt —
 * damit funktionieren ALLE bestehenden `ZoneId.systemDefault()`
 * - Aufrufe automatisch ohne dass wir jede einzelne Datei anfassen muessen. Dieses Objekt ist die
 *   Vorzugs-API fuer NEUEN Code, der semantisch klar machen will dass er Berliner Zeit braucht.
 *
 * Verwendung: val today: LocalDate = AppTime.today() val now: ZonedDateTime = AppTime.now() val
 * date = Instant.ofEpochMilli(ms).atZone(AppTime.zone).toLocalDate()
 */
object AppTime {

    /** Feste Anwendungs-Zeitzone — IMMER Europe/Berlin. */
    val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Anwendungs-Locale fuer Datumsformatierung (Wochentags-Namen etc.). */
    val locale: Locale = Locale.GERMANY

    /** Heutiger Kalendertag in Berliner Zeit. */
    fun today(): LocalDate = LocalDate.now(zone)

    /** Aktueller Zeitpunkt in Berliner Zeit. */
    fun now(): ZonedDateTime = ZonedDateTime.now(zone)

    /** Aktuelle lokale Uhrzeit in Berliner Zeit (ohne Zonen-Information). */
    fun localNow(): LocalDateTime = LocalDateTime.now(zone)
}
