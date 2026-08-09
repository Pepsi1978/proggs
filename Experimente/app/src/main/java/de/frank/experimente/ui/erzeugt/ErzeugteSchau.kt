package de.frank.experimente.ui.erzeugt

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Zeigt die **erzeugten** Bildschirme — nicht die von Hand gebauten.
 *
 * Sie dient einem Zweck: nachzusehen, was der Erzeuger aus der Messung macht, ohne dass
 * jemand unterwegs etwas zurechtrückt. Waagerecht wischen blättert durch alle neun.
 */
@Composable
fun ErzeugteSchau(modifier: Modifier = Modifier) {
    val bildschirme: List<Pair<String, @Composable () -> Unit>> = listOf(
        "B-01 Heute" to { Heute1Erzeugt() },
        "B-02 Gespräch" to { Gesprch2Erzeugt() },
        "B-03 Auswertung" to { Auswertung3Erzeugt() },
        "B-08 Einstellungen" to { Einstellungen4Erzeugt() },
        "B-09 Selbstbild" to { Selbstbild5Erzeugt() },
        "B-06 Erkenntnisse" to { Erkenntnisse6Erzeugt() },
        "B-07 Logbuch" to { Logbuch7Erzeugt() },
        "B-05 Merkliste" to { Merkliste8Erzeugt() },
        "B-04 Wünsche & Ziele" to { Wnscheampziele9Erzeugt() },
    )
    var stelle by remember { mutableStateOf(0) }
    var weite by remember { mutableStateOf(0f) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (weite < -120f && stelle < bildschirme.lastIndex) stelle++
                        if (weite > 120f && stelle > 0) stelle--
                        weite = 0f
                    },
                ) { _, delta -> weite += delta }
            },
    ) {
        bildschirme[stelle].second()
    }
}
