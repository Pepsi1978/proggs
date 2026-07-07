package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.remote.calendar.CalendarSession
import de.frank.entropyreducer.data.remote.calendar.GoogleCalendarApi
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.calendar.ShiftCodeParser
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.util.runCatchingCancellable
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Synchronisiert Ganztagestermine aus Google Calendar in den lokalen Cache.
 * Pro Tag im Sync-Fenster wird genau ein CalendarDayEntity erzeugt.
 *
 * Sync-Fenster: 30 Tage zurück + 5 Jahre vorwaerts (Frank-Wunsch 2026-05-08:
 * "Kalender geht 50 Jahre nach vorne, ich kenne meine Dienstpläne"). 5 Jahre =
 * 1825 Tage ist ein guter Kompromiss zwischen "alles drin" und Sync-Performance.
 * Mehrtaegige Termine (z.B. "X" 10.-11. Mai oder "Urlaub" 10 Tage am Stueck)
 * werden in JEDEN betroffenen Tag eingetragen — vorher hat nur der Start-Tag
 * den Schichtcode bekommen, der Folgetag blieb leer.
 *
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
    private val diagnostics: DiagnosticLogger,
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
     * Liefert die Anzahl gesyncter Tage zurück.
     */
    suspend fun syncDefaultWindow(): Result<Int> = runCatchingCancellable {
        val token = session.freshToken()

        val today = LocalDate.now()
        val from = today.minusDays(30)
        // 5 Jahre nach vorne (Frank-Wunsch 2026-05-08): so kennt die KI
        // alle bekannten Dienstplaene + Urlaube + Termine im Voraus.
        val to = today.plusDays(1825)

        val syncedAt = System.currentTimeMillis()
        val daysByDate = mutableMapOf<String, CalendarDayEntity>()

        // 1. Initialisiere alle Tage als FREI — Frank's Kalender-Konvention:
        //    es werden nur Tag-/Nachtschichten und Urlaub eingetragen, alles ohne
        //    Eintrag bedeutet Frei-Tag. UNBEKANNT bleibt nur als Notfall-Fallback
        //    für den Fall, dass der Sync gar nicht durchgelaufen ist (dann existiert
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
                    // Multi-Day-Expansion (Frank-Wunsch 2026-05-08):
                    // Google Calendar all-day-events haben start.date = erster Tag und
                    // end.date = exklusive Obergrenze. "X" 10.-11. Mai kommt also als
                    // start=2026-05-10, end=2026-05-12. Vorher haben wir nur den
                    // Start-Tag eingetragen und der Folgetag blieb leer. Jetzt
                    // iterieren wir über JEDEN Tag im Range.
                    val startDate = runCatching { LocalDate.parse(allDayDate) }.getOrNull()
                    val endDateString = event.end?.date
                    val endDateExclusive = endDateString
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: startDate?.plusDays(1)
                    if (startDate != null && endDateExclusive != null) {
                        val parsed = ShiftCodeParser.parse(summary)
                        val profile = ShiftCodeParser.profileFor(parsed)
                        var dayIter: LocalDate = startDate
                        while (dayIter.isBefore(endDateExclusive)) {
                            val dayStr = dayIter.toString()
                            val existing = daysByDate[dayStr]
                            // Prioritaets-Stufen (Frank-Regel 2026-05-11):
                            // URLAUB (100) > FREI(explizit) (80) > TAGDIENST/NACHTDIENST (50) >
                            // FREI(default, rawText leer) (10) > UNBEKANNT (0).
                            // Sonst wuerden zwei Events am gleichen Tag (z.B. "X" + "Tag 2")
                            // sich gegenseitig ueberschreiben — Reihenfolge ist API-abhaengig.
                            // Frank's Konvention: Wenn "X" steht, ist der Tag IMMER frei.
                            val newPrio = priorityOf(parsed, summary)
                            val oldPrio = existing
                                ?.let { priorityOf(it.shiftCode, it.rawCalendarText) }
                                ?: -1
                            if (newPrio >= oldPrio) {
                                daysByDate[dayStr] = CalendarDayEntity(
                                    date = dayStr,
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
                            // Auch das CalendarEventEntity pro Tag eintragen — sonst
                            // sieht man im Tag-Detail-Sheet am Folgetag nicht den
                            // dazugehoerigen Termin.
                            event.id?.let { id ->
                                collectedEvents += CalendarEventEntity(
                                    id = if (dayIter == startDate) id else "$id-$dayStr",
                                    date = dayStr,
                                    summary = summary,
                                    description = event.description?.trim()?.takeIf { it.isNotBlank() },
                                    location = event.location?.trim()?.takeIf { it.isNotBlank() },
                                    startMs = 0L,
                                    endMs = 0L,
                                    allDay = true,
                                    syncedAt = syncedAt,
                                )
                            }
                            dayIter = dayIter.plusDays(1)
                        }
                    }
                } else {
                    // Time-Bound-Event: nur den eventDate-Eintrag schreiben (Termine
                    // ueberspannen typischerweise keinen Tageswechsel, und falls doch
                    // würde Google Calendar zwei separate Eintraege liefern).
                    val startMs = parseIsoToMs(timedStart)
                    val endMs = parseIsoToMs(event.end?.dateTime)
                    event.id?.let { id ->
                        collectedEvents += CalendarEventEntity(
                            id = id,
                            date = eventDate,
                            summary = summary,
                            description = event.description?.trim()?.takeIf { it.isNotBlank() },
                            location = event.location?.trim()?.takeIf { it.isNotBlank() },
                            startMs = startMs,
                            endMs = endMs,
                            allDay = false,
                            syncedAt = syncedAt,
                        )
                    }
                }
            }
            pageToken = resp.nextPageToken
        } while (pageToken != null)

        // 3. Persistieren.
        dao.upsertAll(daysByDate.values.toList())
        // Events: alte im Sync-Fenster löschen, neue schreiben — so verschwinden
        // geloeschte Google-Events automatisch auch aus der lokalen DB.
        eventDao.deleteRange(from.toString(), to.toString())
        if (collectedEvents.isNotEmpty()) {
            eventDao.upsertAll(collectedEvents)
        }
        settings.setLastCalendarSync(syncedAt)
        Diag.i(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Calendar-Sync: ${daysByDate.size} Tage + ${collectedEvents.size} Events geschrieben")
        daysByDate.size
    }
        .onSuccess {
            diagnostics.success(DiagnosticArea.GOOGLE_CALENDAR, "Sync OK — $it Tage geladen")
        }
        .onFailure {
            Diag.e(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Calendar-Sync fehlgeschlagen", it)
            diagnostics.error(
                DiagnosticArea.GOOGLE_CALENDAR,
                "Sync fehlgeschlagen: ${it.message ?: it::class.java.simpleName}",
                it,
            )
        }
        .also { session.end() } // Token-Cache leeren — naechster Sync holt frisch

    /**
     * Prioritaet eines Schichtcodes fuer Konflikt-Aufloesung wenn mehrere
     * Events am gleichen Tag liegen.
     *   100 = URLAUB (gewinnt immer)
     *    80 = FREI explizit aus Event ("X", "Tag 2 X" etc., rawText nicht leer)
     *    50 = TAGDIENST / NACHTDIENST
     *    10 = FREI Default (kein Event eingetragen, rawText leer)
     *     0 = UNBEKANNT
     * Frank-Regel 2026-05-11: "Wenn ein X mit drin steht, habe ich immer frei."
     */
    private fun priorityOf(code: ShiftCode, rawText: String): Int = when (code) {
        ShiftCode.URLAUB -> 100
        ShiftCode.FREI -> if (rawText.isNotBlank()) 80 else 10
        ShiftCode.TAGDIENST, ShiftCode.NACHTDIENST -> 50
        ShiftCode.UNBEKANNT -> 0
    }

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
