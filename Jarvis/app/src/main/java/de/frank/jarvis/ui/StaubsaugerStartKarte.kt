package de.frank.jarvis.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.R
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
import kotlinx.coroutines.withContext

private val saugerBereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * Startbereich „Staubsauger“ (Proscenic M7 Pro) im Stil der Proscenic-App, mit deren Symbolen: oben Akku, Zustand und
 * Saugstufe, darunter Reinigen, Tiefenreinigung, Y-förmiges Wischen, Gerät positionieren, Staubsammlung und Ladestation.
 * Jeder Knopf öffnet kurz die Proscenic-App und tippt dort (Bedienungshilfe „Jarvis Staubsauger“); Akku, Zustand und
 * Saugstufe stammen vom letzten Ablesen, Aktualisieren liest frisch. Die Saugstufe lässt die Proscenic-App nur während
 * einer Reinigung umstellen, sie steht hier deshalb nur als Anzeige.
 */
@Composable
fun StaubsaugerStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    // Ohne Proscenic-App auf diesem Gerät bleibt der Bereich leer, statt nur Fehler zu zeigen.
    val installiert = remember(context) { context.packageManager.getLaunchIntentForPackage(ProscenicSteuerung.PAKET) != null }
    if (!installiert) return
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { SaugerStand.laden(context) } }
    val s = SaugerStand
    fun los(aktion: Aktion) {
        if (s.laeuft) return
        // Sofort sperren (Hauptthread), damit ein Doppeltipp keinen zweiten Lauf anstößt.
        s.laeuft = true
        saugerBereich.launch { ProscenicSteuerung.ausfuehren(context, aktion) }
    }
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Staubsauger", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        s.laeuft -> "Bedient die Proscenic-App …"
                        s.gelesenUm != null -> "Stand ${alterKurz(s.gelesenUm!!)} · M7 Pro"
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
        // Kopfzeile wie in der Proscenic-App: Akkustand, Zustand, Saugstufe.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.sauger_laden), null, Modifier.size(26.dp))
                Column(Modifier.padding(start = 8.dp)) {
                    Text(s.akku?.let { "$it %" } ?: "–", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Akkustand", color = f.textLeise, fontSize = 12.sp)
                }
            }
            Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.sauger_saugstufe), null, Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(f.textLeise),
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(s.modus ?: "–", color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                    Text("Saugstufe", color = f.textLeise, fontSize = 12.sp)
                }
            }
        }
        s.status?.let { z -> Text(z, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        s.meldung?.takeIf { !s.laeuft }?.let { Text(it.text, color = if (it.fehler) f.gefahr else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaugerKachel("Reinigen", R.drawable.sauger_reinigen, s.laeuft, Modifier.weight(1f)) { los(Aktion.REINIGEN) }
            SaugerKachel("Tiefenreinigung", R.drawable.sauger_tiefenreinigung, s.laeuft, Modifier.weight(1f)) { los(Aktion.TIEFENREINIGUNG) }
            SaugerKachel("Y-förmiges Wischen", R.drawable.sauger_wischen, s.laeuft, Modifier.weight(1f)) { los(Aktion.WISCHEN) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaugerKachel("Gerät positionieren", R.drawable.sauger_positionieren, s.laeuft, Modifier.weight(1f)) { los(Aktion.POSITIONIEREN) }
            SaugerKachel("Staubsammlung", R.drawable.sauger_staubsammlung, s.laeuft, Modifier.weight(1f)) { los(Aktion.STAUBSAMMLUNG) }
            SaugerKachel("Zur Station", R.drawable.sauger_laden, s.laeuft, Modifier.weight(1f)) { los(Aktion.STATION) }
        }
    }
}

/** Knopf wie im Mehr-Menü der Proscenic-App: Symbol oben, Beschriftung darunter. */
@Composable
private fun SaugerKachel(titel: String, @DrawableRes symbol: Int, gesperrt: Boolean, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Column(
        modifier.heightIn(min = 92.dp).glas(f, 14.dp, erhoeht = 0.5f).antippen { if (!gesperrt) aktion() }
            .padding(horizontal = 4.dp, vertical = 10.dp).alpha(if (gesperrt) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(symbol), null, Modifier.size(40.dp))
        Text(
            titel, color = f.text, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
