package com.entropyjournal.util

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

    fun calculate(latitude: Double, longitude: Double): SunTimes? {
        return try {
            val cal = Calendar.getInstance()
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0

            val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

            val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

            val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

            val latRad = Math.toRadians(latitude)
            val zenith = Math.toRadians(90.833)

            val cosHa = (cos(zenith) / (cos(latRad) * cos(decl)) - tan(latRad) * tan(decl))

            if (cosHa > 1.0 || cosHa < -1.0) return null // No sunrise/sunset (polar)

            val ha = Math.toDegrees(acos(cosHa))

            val sunriseMin = 720.0 - 4.0 * (longitude + ha) - eqTime
            val sunsetMin = 720.0 - 4.0 * (longitude - ha) - eqTime

            val sunriseHour = sunriseMin / 60.0 + tzOffset
            val sunsetHour = sunsetMin / 60.0 + tzOffset

            SunTimes(sunriseHour, sunsetHour)
        } catch (e: Exception) {
            null
        }
    }
}
