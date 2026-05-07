package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.remote.calendar.GoogleCalendarApi
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.calendar.ShiftCodeParser
import de.frank.entropyreducer.domain.model.ShiftCode
import kotlinx.coroutines.flow.Flow
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
    private val api: GoogleCalendarApi,
    private val oauth: OAuthService,
    private val settings: AppSettings,
) {

    fun observeDay(date: String): Flow<CalendarDayEntity?> = dao.getDay(date)
    fun observeRange(from: String, to: String): Flow<List<CalendarDayEntity>> =
        dao.getRange(from, to)

    /**
     * Synchronisiert das Standard-Fenster (-30 / +30 Tage) und schreibt CalendarDay-Eintraege.
     * Liefert die Anzahl gesyncter Tage zurueck.
     */
    suspend fun syncDefaultWindow(): Result<Int> = runCatching {
        val token = oauth.freshGoogleAccessToken()
            ?: throw IllegalStateException("Kein Google-Calendar-Access-Token — bitte erneut anmelden.")

        val today = LocalDate.now()
        val from = today.minusDays(30)
        val to = today.plusDays(31) // exklusive Obergrenze

        val syncedAt = System.currentTimeMillis()
        val daysByDate = mutableMapOf<String, CalendarDayEntity>()

        // 1. Initialisiere alle Tage als UNBEKANNT — sie werden ueberschrieben falls
        //    ein Schicht-Event gefunden wird.
        var d = from
        while (d.isBefore(to)) {
            val dateStr = d.toString()
            val profile = ShiftCodeParser.profileFor(ShiftCode.UNBEKANNT)
            daysByDate[dateStr] = CalendarDayEntity(
                date = dateStr,
                shiftCode = ShiftCode.UNBEKANNT,
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
        do {
            val resp = api.listEvents(
                authorization = "Bearer $token",
                timeMin = timeMin,
                timeMax = timeMax,
                pageToken = pageToken,
            )
            resp.items.forEach { event ->
                val date = event.start?.date ?: return@forEach // nur Ganztagestermine
                if (event.status == "cancelled") return@forEach
                val summary = event.summary?.trim().orEmpty()
                val parsed = ShiftCodeParser.parse(summary)

                val existing = daysByDate[date]
                // Sanftes Override: ein erkannter Schichtcode darf einen UNBEKANNT-Eintrag
                // ersetzen, aber nicht einen schon erkannten Code.
                if (parsed != ShiftCode.UNBEKANNT ||
                    existing == null || existing.shiftCode == ShiftCode.UNBEKANNT
                ) {
                    val profile = ShiftCodeParser.profileFor(parsed)
                    daysByDate[date] = CalendarDayEntity(
                        date = date,
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
            pageToken = resp.nextPageToken
        } while (pageToken != null)

        // 3. Persistieren.
        dao.upsertAll(daysByDate.values.toList())
        settings.setLastCalendarSync(syncedAt)
        Log.i(TAG, "Calendar-Sync: ${daysByDate.size} Tage geschrieben")
        daysByDate.size
    }.onFailure { Log.e(TAG, "Calendar-Sync fehlgeschlagen", it) }

    companion object {
        private const val TAG = "CalendarRepository"
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
