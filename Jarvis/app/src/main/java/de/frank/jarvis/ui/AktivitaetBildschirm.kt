package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/** Alles, was Jarvis getan hat — egal ob ChatGPT oder der eigene Chat es ausgelöst hat. */
@Composable
fun AktivitaetBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val eintraege by Protokoll.eintraege.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aktivität", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Was Jarvis für dich erledigt hat", color = f.textLeise, fontSize = 14.sp)
            }
            if (eintraege.isNotEmpty()) {
                Box(Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen { vm.protokollLeeren() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DeleteSweep, "Verlauf leeren", tint = f.textLeise, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (eintraege.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Kern(96.dp, f.primaer, aktiv = false)
                Text("Noch nichts passiert", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Text("Sobald ChatGPT oder du Jarvis etwas auftragt, steht es hier.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            return
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(eintraege) { e ->
                val farbe = when {
                    !e.ok -> f.gefahr
                    e.quelle == Quelle.CHATGPT -> f.erfolg
                    else -> f.primaer
                }
                Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = farbe).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).glas(f, 4.dp, 0f, farbe, rand = false))
                        Text(e.werkzeug, Modifier.weight(1f).padding(start = 8.dp), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.quelle.anzeige + " · " + zeitKurz(e.zeit), color = f.textSchwach, fontSize = 12.sp)
                    }
                    Text(e.text, color = if (e.ok) f.textLeise else f.gefahr, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
