package de.frank.wecker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

data class PhotoResult(val accepted: Boolean, val message: String)

/** Lokaler Fotoabgleich: Struktur, Farbverteilung, Helligkeit. Kein Upload, auch im Flugmodus. */
object PhotoCheck {
    val colors = linkedMapOf("none" to "Keine Farbvorgabe", "red" to "Rot", "green" to "Grün", "blue" to "Blau", "yellow" to "Gelb", "white" to "Weiß")
    fun bitmap(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Das Foto ist nicht lesbar." }
        var scale = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / scale > 1024) scale *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = scale })
            ?: error("Das Foto konnte nicht geöffnet werden.")
    }
    fun check(file: File, alarm: Alarm): PhotoResult {
        val image = bitmap(file)
        val small = Bitmap.createScaledBitmap(image, 32, 32, true)
        if (small !== image) image.recycle()
        try {
            val pixels = IntArray(1024).also { small.getPixels(it, 0, 32, 0, 0, 32, 32) }
            val brightness = pixels.map { luminance(it) }.average() / 255 * 100
            if (brightness < maxOf(3, alarm.minBrightness)) return PhotoResult(false, "Das Foto ist zu dunkel (${brightness.toInt()} %). Mehr Licht einschalten.")
            if (alarm.color != "none") {
                val hsv = FloatArray(3)
                val share = pixels.count { pixel ->
                    Color.colorToHSV(pixel, hsv)
                    when (alarm.color) {
                        "white" -> hsv[1] < .2 && hsv[2] > .75
                        "red" -> hsv[1] > .35 && hsv[2] > .2 && (hsv[0] < 25 || hsv[0] > 335)
                        "green" -> hsv[1] > .3 && hsv[2] > .15 && hsv[0] in 65f..170f
                        "blue" -> hsv[1] > .3 && hsv[2] > .15 && hsv[0] in 185f..265f
                        "yellow" -> hsv[1] > .3 && hsv[2] > .3 && hsv[0] in 30f..65f
                        else -> false
                    }
                } * 100 / pixels.size
                if (share < alarm.colorPercent) return PhotoResult(false, "${colors[alarm.color]} bedeckt $share %. Benötigt: ${alarm.colorPercent} %.")
            }
            if (alarm.reference.isNotBlank()) {
                val original = bitmap(File(alarm.reference))
                val reference = Bitmap.createScaledBitmap(original, 32, 32, true)
                if (reference !== original) original.recycle()
                val referencePixels = IntArray(1024).also { reference.getPixels(it, 0, 32, 0, 0, 32, 32) }
                reference.recycle()
                val score = similarity(pixels, referencePixels)
                if (score < alarm.photoTolerance) return PhotoResult(false,
                    "Fotoabgleich: $score %. Fotografiere dasselbe Motiv möglichst aus der gespeicherten Perspektive (${alarm.photoTolerance} % benötigt).")
            }
            return PhotoResult(true, "Foto-Aufgabe erfüllt. Guten Morgen!")
        } finally { small.recycle() }
    }
    private fun luminance(pixel: Int): Double = .2126 * Color.red(pixel) + .7152 * Color.green(pixel) + .0722 * Color.blue(pixel)
    internal fun similarity(first: IntArray, second: IntArray): Int {
        fun normalized(pixels: IntArray): Pair<DoubleArray, Double> {
            val values = pixels.map(::luminance)
            val mean = values.average()
            val deviation = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
            return values.map { (it - mean) / maxOf(1.0, deviation) }.toDoubleArray() to deviation
        }
        val (a, da) = normalized(first)
        val (b, db) = normalized(second)
        if (da < 8 || db < 8) return 0 // Eine abgedeckte Kamera / leere Wand erfüllt keine Motiv-Aufgabe.
        val structure = (1 - a.indices.sumOf { abs(a[it] - b[it]) } / a.size / 2).coerceIn(0.0, 1.0)
        fun histogram(pixels: IntArray): DoubleArray {
            val values = DoubleArray(64)
            pixels.forEach { values[(Color.red(it) / 64) * 16 + (Color.green(it) / 64) * 4 + Color.blue(it) / 64]++ }
            return values.map { it / pixels.size }.toDoubleArray()
        }
        val ha = histogram(first); val hb = histogram(second)
        val color = ha.indices.sumOf { minOf(ha[it], hb[it]) }
        return ((structure * .8 + color * .2) * 100).toInt()
    }
}
