package de.frank.entropyreducer.data.remote.calendar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Schmale Retrofit-Schnittstelle zur Google Calendar API v3.
 * Wir lesen ausschliesslich Ganztagestermine ("date" statt "dateTime") und parsen
 * den `summary` mit dem ShiftCodeParser.
 *
 * Spec §15.5.
 */
interface GoogleCalendarApi {

    /**
     * Listet Events des primaeren Kalenders im angegebenen Zeitfenster.
     * `singleEvents=true` damit recurring events expandiert sind.
     *
     * @param authorization "Bearer <access_token>"
     * @param timeMin RFC-3339 (z.B. "2026-04-01T00:00:00Z")
     * @param timeMax RFC-3339
     */
    @GET("calendar/v3/calendars/primary/events")
    suspend fun listEvents(
        @Header("Authorization") authorization: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("maxResults") maxResults: Int = 250,
        @Query("pageToken") pageToken: String? = null,
    ): CalendarEventsResponse
}

@Serializable
data class CalendarEventsResponse(
    @SerialName("items") val items: List<CalendarEvent> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null,
)

@Serializable
data class CalendarEvent(
    @SerialName("id") val id: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("start") val start: CalendarDate? = null,
    @SerialName("end") val end: CalendarDate? = null,
    @SerialName("status") val status: String? = null,
)

@Serializable
data class CalendarDate(
    /** Bei Ganztagesterminen: "YYYY-MM-DD". Sonst null. */
    @SerialName("date") val date: String? = null,
    /** Bei Terminen mit Uhrzeit: ISO-8601 mit Zeitzone. */
    @SerialName("dateTime") val dateTime: String? = null,
)
