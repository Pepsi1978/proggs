package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.staubsauger.ProscenicSteuerung
import de.frank.jarvis.staubsauger.ProscenicSteuerung.Aktion
import de.frank.jarvis.staubsauger.SaugerStand
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val saugerBereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * Startbereich „Staubsauger“ (Proscenic M7 Pro): Reinigen, Tiefenreinigung, Gerät positionieren und Ein-Klick-Staubsammlung.
 * Jeder Knopf öffnet kurz die Proscenic-App und tippt dort (Bedienungshilfe „Jarvis Staubsauger“); Akku und Zustand stammen
 * vom letzten Ablesen, Aktualisieren liest frisch.
 */
@Composable
fun StaubsaugerStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    remember(context) { SaugerStand.laden(context); true }
    val s = SaugerStand
    fun los(aktion: Aktion) {
        if (s.laeuft) return
        saugerBereich.launch { ProscenicSteuerung.ausfuehren(context, aktion) }
    }
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Staubsauger", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        s.laeuft -> "Bedient die Proscenic-App …"
                        s.gelesenUm != null -> "Stand ${SimpleDateFormat("HH:mm", Locale.GERMAN).format(Date(s.gelesenUm!!))} Uhr · M7 Pro"
                        else -> "Noch kein Stand · M7 Pro"
                    },
                    color = if (s.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (s.laeuft) f.primaer else null)
                    .antippen { los(Aktion.STATUS) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, if (s.laeuft) "Staubsauger wird gelesen" else "Akku und Zustand aktualisieren", tint = if (s.laeuft) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Text("Akku", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(s.akku?.let { "$it %" } ?: "–", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        s.status?.let { z ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                Text("Zustand", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(z, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(2f))
            }
        }
        s.meldung?.takeIf { it.fehler && !s.laeuft }?.let { Text(it.text, color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaugerKnopf("Reinigen", s.laeuft, Modifier.weight(1f)) { los(Aktion.REINIGEN) }
            SaugerKnopf("Tiefenreinigung", s.laeuft, Modifier.weight(1f)) { los(Aktion.TIEFENREINIGUNG) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaugerKnopf("Gerät positionieren", s.laeuft, Modifier.weight(1f)) { los(Aktion.POSITIONIEREN) }
            SaugerKnopf("Staubsammlung", s.laeuft, Modifier.weight(1f)) { los(Aktion.STAUBSAMMLUNG) }
        }
    }
}

@Composable
private fun SaugerKnopf(titel: String, gesperrt: Boolean, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        modifier.heightIn(min = 44.dp).glas(f, 14.dp, erhoeht = 0.5f).antippen { if (!gesperrt) aktion() }.padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(titel, color = if (gesperrt) f.textLeise else f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
