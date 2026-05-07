package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.remote.calendar.CalendarSession
import de.frank.entropyreducer.data.remote.calendar.GoogleCalendarApi
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.calendar.ShiftCodeParser
import de.frank.entropyreducer.domain.model.ShiftCode
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronisiert Ganztagestermine aus Google Calendar in den lokalen Cache.
 * Pro Tag im Sync-Fenster wird genau ein CalendarDayEntity erzeugt.
 *
 * Sync-Fenster: 30 Tage zurueck + 30 Tage vorwaerts (Spec §15.5).
 * Bei mehreren passenden Events pro Tag gewinnt der erste erkannte Schichtcode;
 * Termine ohne Schichtcode werden als UNBEKANNT eingetragen, ohne andere zu ueberschreiben.
 */
@Singleton
class CalendarRepository @Inject constructor(
    private val dao: CalendarDayDao,
    private val eventDao: CalendarEventDao,
    private val api: GoogleCalendarApi,
    private val session: CalendarSession,
    private val settings: AppSettings,
) {

    fun observeDay(date: String): Flow<CalendarDayEntity?> = dao.getDay(date)
    fun observeRange(from: String, to: String): Flow<List<CalendarDayEntity>> =
        dao.getRange(from, to)
    fun observeEventsForDate(date: String): Flow<List<CalendarEventEntity>> =
        eventDao.getByDate(date)
    fun observeEventsRange(from: String, to: String): Flow<List<CalendarEventEntity>> =
        eventDao.getRange(from, to)

    /**
     * Synchronisiert das Standard-Fenster (-30 / +30 Tage) und schreibt CalendarDay-Eintraege.
     * Liefert die Anzahl gesyncter Tage zurueck.
     */
    suspend fun syncDefaultWindow(): Result<Int> = runCatching {
        val token = session.freshToken()

        val today = LocalDate.now()
        val from = today.minusDays(30)
        val to = today.plusDays(31) // exklusive Obergrenze

        val syncedAt = System.currentTimeMillis()
        val daysByDate = mutableMapOf<String, CalendarDayEntity>()

        // 1. Initialisiere alle Tage als FREI — Frank's Kalender-Konvention:
        //    es werden nur Tag-/Nachtschichten und Urlaub eingetragen, alles ohne
        //    Eintrag bedeutet Frei-Tag. UNBEKANNT bleibt nur als Notfall-Fallback
        //    fuer den Fall, dass der Sync gar nicht durchgelaufen ist (dann existiert
        //    der DB-Eintrag erst gar nicht und der StatusObserver liest null).
        var d = from
        while (d.isBefore(to)) {
            val dateStr = d.toString()
            val profile = ShiftCodeParser.profileFor(ShiftCode.FREI)
            daysByDate[dateStr] = CalendarDayEntity(
                date = dateStr,
                shiftCode = ShiftCode.FREI,
                rawCalendarText = "",
                workWindowStart = profile.workWindowStart,
                workWindowEnd = profile.workWindowEnd,
                sleepWindowStart = profile.sleepWindowStart,
                sleepWindowEnd = profile.sleepWindowEnd,
                availableMinutesEstimate = profile.availableMinutesEstimate,
                syncedAt = syncedAt,
            )
            d = d.plusDays(1)
        }

        // 2. Hole Events seitenweise.
        val timeMin = OffsetDateTime.of(from.atStartOfDay(), ZoneOffset.UTC).format(ISO)
        val timeMax = OffsetDateTime.of(to.atStartOfDay(), ZoneOffset.UTC).format(ISO)
        var pageToken: String? = null
        // Alle Events des Sync-Fensters sammeln — werden am Ende komplett ersetzt
        // (kein Patch-Update, sonst bleiben geloeschte Google-Events haengen).
        val collectedEvents = mutableListOf<CalendarEventEntity>()
        do {
            val resp = api.listEvents(
                authorization = "Bearer $token",
                timeMin = timeMin,
                timeMax = timeMax,
                pageToken = pageToken,
            )
            resp.items.forEach { event ->
                if (event.status == "cancelled") return@forEach
                val summary = event.summary?.trim().orEmpty()
                if (summary.isBlank()) return@forEach

                // 2a. Ganztagestermine: gegen Schichtcode-Parser laufen lassen.
                val allDayDate = event.start?.date
                val timedStart = event.start?.dateTime
                val isAllDay = allDayDate != null && timedStart == null

                val eventDate = when {
                    isAllDay -> allDayDate!!
                    timedStart != null -> {
                        // ISO mit Zeitzone -> in lokales Datum umrechnen
                        runCatching { OffsetDateTime.parse(timedStart) }
                            .getOrNull()
                            ?.atZoneSameInstant(java.time.ZoneId.systemDefault())
                            ?.toLocalDate()
                            ?.toString()
                    }
                    else -> null
                } ?: return@forEach

                if (isAllDay) {
                    val parsed = ShiftCodeParser.parse(summary)
                    val existing = daysByDate[eventDate]
                    // Sanftes Override: erkannter Schichtcode darf einen UNBEKANNT-Eintrag
                    // ersetzen, aber nicht einen schon erkannten Code.
                    if (parsed != ShiftCode.UNBEKANNT ||
                        existing == null || existing.shiftCode == ShiftCode.UNBEKANNT
                    ) {
                        val profile = ShiftCodeParser.profileFor(parsed)
                        daysByDate[eventDate] = CalendarDayEntity(
                            date = eventDate,
                            shiftCode = parsed,
                            rawCalendarText = summary,
                            workWindowStart = profile.workWindowStart,
                            workWindowEnd = profile.workWindowEnd,
                            sleepWindowStart = profile.sleepWindowStart,
                            sleepWindowEnd = profile.sleepWindowEnd,
                            availableMinutesEstimate = profile.availableMinutesEstimate,
                            syncedAt = syncedAt,
                        )
                    }
                }

                // 2b. Generic-Event-Persistierung: ALLE Events (Ganztags + Time-Bound)
                //     werden in die calendar_events-Tabelle geschrieben — egal ob
                //     Schichtcode oder Arzttermin. UI + KI koennen sich dann darauf beziehen.
                val startMs = if (isAllDay) 0L else parseIsoToMs(timedStart)
                val endMs = if (isAllDay) 0L else parseIsoToMs(event.end?.dateTime)
                event.id?.let { id ->
                    collectedEvents += CalendarEventEntity(
                        id = id,
                        date = eventDate,
                        summary = summary,
                        description = event.description?.trim()?.takeIf { it.isNotBlank() },
                        location = event.location?.trim()?.takeIf { it.isNotBlank() },
                        startMs = startMs,
                        endMs = endMs,
                        allDay = isAllDay,
                        syncedAt = syncedAt,
                    )
                }
            }
            pageToken = resp.nextPageToken
        } while (pageToken != null)

        // 3. Persistieren.
        dao.upsertAll(daysByDate.values.toList())
        // Events: alte im Sync-Fenster loeschen, neue schreiben — so verschwinden
        // geloeschte Google-Events automatisch auch aus der lokalen DB.
        eventDao.deleteRange(from.toString(), to.toString())
        if (collectedEvents.isNotEmpty()) {
            eventDao.upsertAll(collectedEvents)
        }
        settings.setLastCalendarSync(syncedAt)
        Log.i(TAG, "Calendar-Sync: ${daysByDate.size} Tage + ${collectedEvents.size} Events geschrieben")
        daysByDate.size
    }.onFailure { Log.e(TAG, "Calendar-Sync fehlgeschlagen", it) }
        .also { session.end() } // Token-Cache leeren — naechster Sync holt frisch

    private fun parseIsoToMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return runCatching {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        }.getOrElse { 0L }
    }

    companion object {
        private const val TAG = "CalendarRepository"
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
