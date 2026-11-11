package de.frank.jarvis.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.VictronFaehigkeit
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Stand der Karte außerhalb der Komposition (wie bei den Biomarkern): Sie liegt in der LazyColumn des Startbildschirms und soll
 * nach dem Wegscrollen nicht leer und klein zurückkommen. Das Lesen läuft in einem eigenen Bereich weiter, auch wenn die Karte verschwindet.
 */
private object VictronStartStand {
    var stand by mutableStateOf<VictronFaehigkeit.Stand?>(null)
    var laeuft by mutableStateOf(false)
    var geladen = false
    /** Einmal je App-Start von selbst frisch lesen; danach nur über den Knopf. */
    var automatischGelesen = false
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun lesen(faehigkeit: VictronFaehigkeit) {
        if (laeuft) return
        laeuft = true
        bereich.launch {
            try { stand = faehigkeit.abrufen() } finally { laeuft = false }
        }
    }
}

private fun alterKurz(zeit: Long): String {
    if (zeit <= 0) return ""
    val s = System.currentTimeMillis() / 1000 - zeit
    val uhr = java.time.Instant.ofEpochSecond(zeit).atZone(java.time.ZoneId.systemDefault())
    val heute = uhr.toLocalDate() == java.time.LocalDate.now()
    val zeitText = if (heute) "%02d:%02d Uhr".format(uhr.hour, uhr.minute) else "%02d.%02d. %02d:%02d Uhr".format(uhr.dayOfMonth, uhr.monthValue, uhr.hour, uhr.minute)
    return if (s < 120) "gerade eben" else zeitText
}

/**
 * Startbereich „Solarbatterie“: zuletzt gemessene Werte des BMV-712 (Batterie) und des SmartSolar-Ladereglers (MPPT),
 * mit Aktualisieren-Knopf. Langer Druck auf die Karte verschiebt sie wie die anderen Bereiche.
 */
@Composable
fun VictronStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val faehigkeit = victronFaehigkeit(context)
    val s = VictronStartStand
    var erlaubt by androidx.compose.runtime.remember { mutableStateOf(VictronFaehigkeit.erlaubt(context)) }
    val anfrage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        erlaubt = ok
        if (ok) s.lesen(faehigkeit)
    }
    LaunchedEffect(Unit) {
        if (!s.geladen) { s.geladen = true; if (s.stand == null) s.stand = faehigkeit.letzter() }
        if (!s.automatischGelesen) { s.automatischGelesen = true; s.lesen(faehigkeit) }
    }
    val stand = s.stand
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Solarbatterie", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        s.laeuft -> "Liest die Anlage …"
                        stand == null || stand.werte.isEmpty() -> stand?.fehler ?: "Noch kein Stand"
                        else -> "Stand " + alterKurz(stand.zeit) + " · " + stand.quelle
                    },
                    color = if (s.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (s.laeuft) f.primaer else null)
                    .antippen { s.lesen(faehigkeit) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, if (s.laeuft) "Solarbatterie wird gelesen" else "Solarbatterie aktualisieren", tint = if (s.laeuft) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            }
        }
        if (!erlaubt) {
            Text("Für das direkte Lesen per Bluetooth fehlt die Erlaubnis „Geräte in der Nähe“. Ohne sie kommen die Werte nur über den Rechner.",
                color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Knopf("Bluetooth erlauben", Modifier.padding(top = 6.dp)) { anfrage.launch(Manifest.permission.BLUETOOTH_SCAN) }
        }
        if (stand != null && stand.werte.isNotEmpty()) {
            if (stand.fehler != null && !s.laeuft) Text(stand.fehler, color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            stand.werte["batteriemonitor"]?.let { b -> Abschnitt("Batterie (BMV-712)", VictronFaehigkeit.batterieZeilen(b)) }
            stand.werte["laderegler"]?.let { l -> Abschnitt("Laderegler (MPPT)", VictronFaehigkeit.reglerZeilen(l)) }
        }
    }
}

@Composable
private fun Abschnitt(titel: String, zeilen: List<Pair<String, String>>) {
    val f = LocalFarben.current
    Text(titel, color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    zeilen.forEach { (name, wert) ->
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(name, color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(wert, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Die Fähigkeit aus dem Register, damit Karte und Werkzeug denselben Lauf (Sperre) teilen. */
@Composable
private fun victronFaehigkeit(context: android.content.Context): VictronFaehigkeit = androidx.compose.runtime.remember(context) {
    de.frank.jarvis.faehigkeit.Register.alle(context).filterIsInstance<VictronFaehigkeit>().first()
}
