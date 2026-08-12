package de.frank.experimente.ui.theme

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Wie stark eine Karte geneigt ist — in Grad, je Achse. */
data class Neigung(val umX: Float = 0f, val umY: Float = 0f)

/**
 * **E-08 · M-78 — die Kipp-Parallaxe.** Karten neigen sich mit dem Gerät, höchstens ±6°.
 *
 * Gelesen wird der Rotationsvektor; der Wert wird gedämpft, damit die Karte nicht zittert
 * (`spring(0.75, 200)` im Motion-Spec — hier als gleitender Mittelwert, weil der Sensor
 * schneller liefert, als Compose neu zeichnet).
 *
 * Auf den Stufen *Gedämpft* und *Aus* bleibt sie aus, und der Zuhörer wird gar nicht erst
 * angemeldet — ein stiller Sensor kostet keinen Strom.
 */
@Composable
fun neigung(stufe: Effektstufe, hoechstens: Float = 6f): Neigung {
    if (!stufe.parallaxe) return Neigung()
    val ctx = LocalContext.current
    var wert by remember { mutableStateOf(Neigung()) }

    DisposableEffect(ctx, hoechstens) {
        val verwaltung = ctx.getSystemService(SensorManager::class.java)
        val geber = verwaltung?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: verwaltung?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (verwaltung == null || geber == null) return@DisposableEffect onDispose {}

        val matrix = FloatArray(9)
        val winkel = FloatArray(3)
        val zuhoerer = object : SensorEventListener {
            override fun onSensorChanged(ereignis: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, ereignis.values)
                SensorManager.getOrientation(matrix, winkel)
                val neigenVorn = Math.toDegrees(winkel[1].toDouble()).toFloat()
                val neigenSeit = Math.toDegrees(winkel[2].toDouble()).toFloat()
                // Das Gerät liegt beim Lesen meist um −45° gekippt; die Ruhelage wird
                // herausgerechnet, damit die Karte im Normalfall gerade steht.
                val roh = Neigung(
                    umX = ((neigenVorn + 45f) / 6f).coerceIn(-hoechstens, hoechstens),
                    umY = (neigenSeit / 6f).coerceIn(-hoechstens, hoechstens),
                )
                wert = Neigung(
                    umX = wert.umX + (roh.umX - wert.umX) * 0.12f,
                    umY = wert.umY + (roh.umY - wert.umY) * 0.12f,
                )
            }

            override fun onAccuracyChanged(geber: Sensor?, genauigkeit: Int) = Unit
        }
        verwaltung.registerListener(zuhoerer, geber, SensorManager.SENSOR_DELAY_UI)
        onDispose { verwaltung.unregisterListener(zuhoerer) }
    }
    return wert
}
