package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
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
 * Startbereich „Raumklima“: Temperatur und Luftfeuchtigkeit der SwitchBot-Sensoren, frisch über das Internet aus der SwitchBot-Cloud
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
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                Text(z.optString("name"), color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (problem.isNotEmpty()) Text(problem.replaceFirstChar { it.uppercase() }, color = f.gefahr, fontSize = 14.sp, modifier = Modifier.weight(2f))
                else Text(
                    String.format(Locale.GERMANY, "%.1f °C · %.0f %%", z.optDouble("temperatur_c"), z.optDouble("luftfeuchtigkeit_prozent")),
                    color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
