package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.OpenMeteoApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Frank-Wunsch 2026-06-25 ("Entropie Reductor"): Wetter-Rückblick pro Training.
 *
 * Nutzt Open-Meteo mit Stunden-Temperatur — nicht Tages-Durchschnitt.
 * Frank will wissen: "Wie heiß war es genau in dieser Zeit, wo ich gelaufen bin?"
 *
 * Für jedes Training mit GPS-Track: Open-Meteo liefert hourly temperature_2m
 * + weathercode. Wir suchen die Stunde die zum Trainings-Start passt.
 *
 * Fehler-Verhalten (Frank: "Wenn du keine [GPS-Daten] findest, dann mach auch
 * keine Wetterdaten"):
 *   - GPS-Punkt nicht parsebar → null (kein Wetter)
 *   - Open-Meteo-Fehler → null, aber ins Diag-Log
 */
@Singleton
class WeatherRepository
@Inject
constructor(
    private val openMeteo: OpenMeteoApi,
) {

    /**
     * Recherche-Ergebnis für ein Training: Temperatur in °C + Wetterbedingung
     * als ein Wort. null = nicht recherchierbar (kein GPS-Track, API-Fehler).
     */
    data class WeatherInfo(val tempCelsius: Int, val condition: String)

    /**
     * Recherche das Wetter an der Stelle [lat]/[lon] zum Zeitpunkt [startMs].
     * Open-Meteo liefert 24 Stunden-Werte pro Tag. Wir suchen die Stunde,
     * die dem Trainings-Start am nächsten liegt.
     */
    suspend fun fetchWeatherFor(
        lat: Double,
        lon: Double,
        startMs: Long,
        cityHint: String? = null,
        durationMinutes: Long? = null,
    ): WeatherInfo? {
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(startMs).atZone(zone)
        val dateStr = zdt.format(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
        val targetHour = "%sT%02d:00".format(dateStr, zdt.hour)
        val today = LocalDate.now(zone)
        val useRecentEndpoint = !zdt.toLocalDate().isBefore(today.minusDays(7))

        return try {
            val response =
                if (useRecentEndpoint) {
                    openMeteo.getRecentWeather(latitude = lat, longitude = lon)
                } else {
                    openMeteo.getHistoricalWeather(
                        latitude = lat,
                        longitude = lon,
                        startDate = dateStr,
                        endDate = dateStr,
                    )
                }

            if (response.error == true) {
                Diag.w(
                    DiagnosticArea.BIOMARKER,
                    TAG,
                    "Open-Meteo Fehler für lat=$lat lon=$lon am $targetHour: ${response.reason}",
                )
                return null
            }

            val hourly = response.hourly ?: run {
                Diag.w(
                    DiagnosticArea.BIOMARKER,
                    TAG,
                    "Open-Meteo: leere hourly-Antwort für lat=$lat lon=$lon am $targetHour",
                )
                return null
            }

            if (hourly.time.isEmpty()) {
                Diag.d(
                    DiagnosticArea.BIOMARKER,
                    TAG,
                    "Open-Meteo: keine Daten für lat=$lat lon=$lon am $targetHour",
                )
                return null
            }

            val bestIndex = hourly.time.indexOf(targetHour)

            if (bestIndex < 0) {
                Diag.w(
                    DiagnosticArea.BIOMARKER,
                    TAG,
                    "Open-Meteo: keine passende Stunde fuer lat=$lat lon=$lon bei $targetHour " +
                        "(recent=$useRecentEndpoint, erste=${hourly.time.firstOrNull()}, letzte=${hourly.time.lastOrNull()})",
                )
                return null
            }

            val temp = hourly.temperature.getOrNull(bestIndex)
            val wmoCode = hourly.weatherCode.getOrNull(bestIndex)

            if (temp == null || wmoCode == null) {
                Diag.w(
                    DiagnosticArea.BIOMARKER,
                    TAG,
                    "Open-Meteo: unvollstaendige Daten fuer lat=$lat lon=$lon bei $targetHour " +
                        "(index=$bestIndex, temp=$temp, wmo=$wmoCode)",
                )
                return null
            }

            val condition = wmoToCondition(wmoCode)

            Diag.d(
                DiagnosticArea.BIOMARKER,
                TAG,
                "Open-Meteo OK: lat=$lat lon=$lon bei $targetHour -> " +
                    "${temp.toInt()}°C ($condition) [WMO=$wmoCode, recent=$useRecentEndpoint]",
            )

            WeatherInfo(tempCelsius = temp.toInt(), condition = condition)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Diag.w(
                DiagnosticArea.BIOMARKER,
                TAG,
                "Open-Meteo Aufruf fehlgeschlagen für lat=$lat lon=$lon bei $targetHour",
                t,
            )
            null
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"

        /**
         * WMO Weather Interpretation Codes → deutsches Einzelwort.
         * Quelle: https://open-meteo.com/en/docs (WMO Codes Tabelle)
         */
        internal fun wmoToCondition(code: Int): String = when (code) {
            0 -> "sonnig"                  // Clear sky
            1 -> "sonnig"                  // Mainly clear
            2 -> "heiter"                  // Partly cloudy
            3 -> "bewölkt"                 // Overcast
            45, 48 -> "neblig"             // Fog / depositing rime fog
            51, 53, 55 -> "regnerisch"     // Drizzle
            56, 57 -> "regnerisch"         // Freezing drizzle
            61, 63, 65 -> "regnerisch"     // Rain
            66, 67 -> "regnerisch"         // Freezing rain
            71, 73, 75 -> "schneeig"       // Snow fall
            77 -> "schneeig"               // Snow grains
            80, 81, 82 -> "regnerisch"     // Rain showers
            85, 86 -> "schneeig"           // Snow showers
            95 -> "gewittrig"              // Thunderstorm
            96, 99 -> "gewittrig"          // Thunderstorm with hail
            else -> "wechselhaft"           // Unknown WMO code
        }

        /**
         * Fixe Liste der erlaubten Wetterbedingungen (UI-Validierung).
         */
        internal val ALLOWED_CONDITIONS: Set<String> =
            setOf(
                "sonnig",
                "heiter",
                "bewölkt",
                "wechselhaft",
                "regnerisch",
                "schneeig",
                "neblig",
                "gewittrig",
                "windig",
                "schwül",
            )
    }
}
