package de.frank.experimente.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion-Spec §8 — Pflichtabschnitt „Reduzierte Bewegung“.
 *
 * Meldet das System, dass Bewegung reduziert werden soll, gilt in der ganzen App:
 * Dauerbewegung aus, nur noch Überblenden, alle Dauern halbiert, keine Staffelung.
 * Die Vibration bleibt — sie ist keine Bewegung und trägt die Rückmeldung, wenn die
 * sichtbare fehlt.
 */
val LocalReduzierteBewegung = staticCompositionLocalOf { false }

fun istBewegungReduziert(context: Context): Boolean {
    val skala = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return skala == 0f
}

/** Halbiert eine Dauer, wenn reduzierte Bewegung gilt. */
fun dauer(normal: Int, reduziert: Boolean): Int = if (reduziert) normal / 2 else normal

/** Versatz der gestaffelten Vorschlagskarten — entfällt bei reduzierter Bewegung. */
fun staffel(index: Int, reduziert: Boolean): Int =
    if (reduziert) 0 else index * Bewegung.STAFFEL_MS
