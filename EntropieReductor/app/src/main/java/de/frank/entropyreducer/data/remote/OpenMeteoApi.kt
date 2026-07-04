package de.frank.entropyreducer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Archive API — kostenlose historische Wetterdaten ohne API-Key.
 * Quelle: https://open-meteo.com/en/docs/historical-weather-api
 *
 * Frank's Wunsch (2026-06-25): Wetter pro Training — exakte Stundentemperatur
 * zum Zeitpunkt des Trainings, kein Tages-Durchschnitt. Fuer heutige/aktuelle
 * Trainings braucht es den Forecast-Endpunkt; der Archive-Endpunkt ist fuer
 * aeltere historische Daten.
 */
interface OpenMeteoApi {

    @GET("v1/archive")
    suspend fun getHistoricalWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "Europe/Berlin",
    ): OpenMeteoResponse

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getRecentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("past_days") pastDays: Int = 7,
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "Europe/Berlin",
    ): OpenMeteoResponse
}

@Serializable
data class OpenMeteoResponse(
    val hourly: OpenMeteoHourly? = null,
    val error: Boolean? = null,
    val reason: String? = null,
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
)
