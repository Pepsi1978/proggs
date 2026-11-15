package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.SwitchbotFaehigkeit
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stand der Karte außerhalb der Komposition (wie bei der Solarbatterie), nur für diesen App-Lauf; nichts wird gespeichert. */
private object RaumklimaStartStand {
    var ergebnis by mutableStateOf<Ergebnis?>(null)
    var laeuft by mutableStateOf(false)
    /** Einmal je App-Start von selbst frisch lesen; danach nur über den Knopf. */
    var automatischGelesen = false
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun lesen(faehigkeit: SwitchbotFaehigkeit) {
        if (laeuft) return
        laeuft = true
        bereich.launch {
            try { ergebnis = runCatching { faehigkeit.abrufen() }.getOrElse { Ergebnis("Die Sensoren ließen sich nicht lesen.", fehler = true) } } finally { laeuft = false }
        }
    }
}

/**
 * Startbereich „Raumklima“: Temperatur, Luftfeuchtigkeit und beim CO2-Monitor CO2 der SwitchBot-Sensoren, frisch über das Internet aus der SwitchBot-Cloud
 * (also auch unterwegs), mit Aktualisieren-Knopf. Nicht erreichbare Sensoren stehen mit Grund statt mit Werten da.
 */
@Composable
fun RaumklimaStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val faehigkeit = remember(context) { Register.alle(context).filterIsInstance<SwitchbotFaehigkeit>().first() }
    val s = RaumklimaStartStand
    LaunchedEffect(Unit) {
        if (!s.automatischGelesen) { s.automatischGelesen = true; s.lesen(faehigkeit) }
    }
    val e = s.ergebnis
    val struktur = e?.struktur
    val sensoren = struktur?.optJSONArray("sensoren")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Raumklima", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        s.laeuft -> "Liest die Sensoren …"
                        struktur != null -> "Stand ${struktur.optString("abgerufen")} Uhr · SwitchBot-Cloud"
                        else -> "Noch kein Stand"
                    },
                    color = if (s.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (s.laeuft) f.primaer else null)
                    .antippen { s.lesen(faehigkeit) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, if (s.laeuft) "Raumklima wird gelesen" else "Raumklima aktualisieren", tint = if (s.laeuft) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            }
        }
        if (e != null && struktur == null && !s.laeuft) Text(e.text, color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        sensoren.forEach { z ->
            val problem = z.optString("problem")
            // CO2-Monitor: nur der CO2-Wert mit Skala; Temperatur und Feuchte liefern die anderen Sensoren (Frank, 15.11.2026).
            val co2 = if (problem.isEmpty() && z.has("co2_ppm")) z.optInt("co2_ppm") else null
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                Text(z.optString("name"), color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
                // Offline (z. B. das Auto außer Reichweite des Hubs) ist normal: leise statt rot.
                if (problem.isNotEmpty()) Text(problem.replaceFirstChar { it.uppercase() }, color = if (z.optBoolean("offline")) f.textLeise else f.gefahr, fontSize = 14.sp, modifier = Modifier.weight(2f))
                else Text(
                    if (co2 != null) "$co2 ppm · ${co2Stufe(co2)}"
                    else String.format(Locale.GERMANY, "%.1f °C · %.0f %%", z.optDouble("temperatur_c"), z.optDouble("luftfeuchtigkeit_prozent")),
                    color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f), textAlign = TextAlign.End,
                )
            }
            if (co2 != null) Co2Skala(co2)
        }
    }
}

/** Einordnung wie im Sprachhinweis von SwitchbotFaehigkeit: unter 800 gut, bis 1000 in Ordnung, bis 1400 lüften, darüber dringend lüften. */
private fun co2Stufe(ppm: Int): String = when {
    ppm < 800 -> "gut"
    ppm <= 1000 -> "in Ordnung"
    ppm <= 1400 -> "lüften"
    else -> "dringend lüften"
}

/** CO2-Skala 400–2000 ppm: links grün (gute Luft), über gelb nach rot (schlechte Luft), darauf der aktuelle Wert als Strich; Werte außerhalb am Rand. */
@Composable
private fun Co2Skala(ppm: Int) {
    val f = LocalFarben.current
    val messer = androidx.compose.ui.text.rememberTextMeasurer()
    val stil = androidx.compose.ui.text.TextStyle(color = f.textLeise, fontSize = 11.sp)
    val rot = androidx.compose.ui.graphics.Color(0xFFE5484D)
    val gelb = androidx.compose.ui.graphics.Color(0xFFF5C451)
    val gruen = androidx.compose.ui.graphics.Color(0xFF3DBE7A)
    val von = 400f
    val bis = 2000f
    fun lage(w: Float) = ((w - von) / (bis - von)).coerceIn(0f, 1f)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(top = 4.dp).height(34.dp)) {
        val dicke = 6.dp.toPx()
        val y = 10.dp.toPx()
        val rand = 7.dp.toPx()
        val breite = size.width - 2 * rand
        fun x(w: Float) = rand + breite * lage(w)
        drawLine(
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                0f to gruen, lage(800f) to gruen, lage(1000f) to gelb, lage(1200f) to gelb, lage(1400f) to rot, 1f to rot,
                startX = rand, endX = rand + breite,
            ),
            androidx.compose.ui.geometry.Offset(rand, y), androidx.compose.ui.geometry.Offset(rand + breite, y),
            strokeWidth = dicke, cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val ax = x(ppm.toFloat())
        drawLine(f.hgOben, androidx.compose.ui.geometry.Offset(ax, y - 8.dp.toPx()), androidx.compose.ui.geometry.Offset(ax, y + 8.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(f.text, androidx.compose.ui.geometry.Offset(ax, y - 7.dp.toPx()), androidx.compose.ui.geometry.Offset(ax, y + 7.dp.toPx()), strokeWidth = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Marken unter der Linie: Anfang, Grenze „lüften“ und Ende.
        for ((w, text) in listOf(400f to "400", 1000f to "1000", 2000f to "2000+")) {
            val t = messer.measure(text, stil)
            val tx = (x(w) - t.size.width / 2f).coerceIn(0f, size.width - t.size.width)
            drawText(t, topLeft = androidx.compose.ui.geometry.Offset(tx, y + dicke / 2 + 6.dp.toPx()))
        }
    }
}
