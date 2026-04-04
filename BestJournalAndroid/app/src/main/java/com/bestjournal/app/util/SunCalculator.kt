package com.bestjournal.app.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Calculates sunrise and sunset times using the NOAA solar position algorithm.
 * No internet or API needed — pure math based on latitude, longitude and date.
 */
object SunCalculator {

    data class SunTimes(val sunriseHour: Double, val sunsetHour: Double)

    fun isDarkNow(latitude: Double, longitude: Double): Boolean {
        val times = calculate(latitude, longitude) ?: return false
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
        return currentHour < times.sunriseHour || currentHour > times.sunsetHour
    }

    /**
     * Asymmetric twilight calculation:
     * - Morning (dark→light): Civil twilight at 96° — switch to light when dawn begins
     * - Evening (light→dark): Nautical twilight at 102° — switch to dark when truly dark
     */
    fun calculate(latitude: Double, longitude: Double): SunTimes? {
        return try {
            val cal = Calendar.getInstance()
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0

            val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

            val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

            val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

            val latRad = Math.toRadians(latitude)

            // Morning: civil twilight (96°) — light mode when dawn begins
            val zenithMorning = Math.toRadians(96.0)
            val cosHaMorning = (cos(zenithMorning) / (cos(latRad) * cos(decl)) - tan(latRad) * tan(decl))
            if (cosHaMorning > 1.0 || cosHaMorning < -1.0) return null

            // Evening: nautical twilight (102°) — dark mode when truly dark
            val zenithEvening = Math.toRadians(102.0)
            val cosHaEvening = (cos(zenithEvening) / (cos(latRad) * cos(decl)) - tan(latRad) * tan(decl))
            if (cosHaEvening > 1.0 || cosHaEvening < -1.0) return null

            val haMorning = Math.toDegrees(acos(cosHaMorning))
            val haEvening = Math.toDegrees(acos(cosHaEvening))

            val sunriseMin = 720.0 - 4.0 * (longitude + haMorning) - eqTime
            val sunsetMin = 720.0 - 4.0 * (longitude - haEvening) - eqTime

            val sunriseHour = sunriseMin / 60.0 + tzOffset
            val sunsetHour = sunsetMin / 60.0 + tzOffset

            SunTimes(sunriseHour, sunsetHour)
        } catch (e: Exception) {
            null
        }
    }
}
