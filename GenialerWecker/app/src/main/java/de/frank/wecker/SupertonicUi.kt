package de.frank.wecker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.LocalSemantisch

/** Stand der Supertonic-Stimmen in den Einstellungen: herunterladen, Fortschritt mit Pausieren, oder „geladen“ mit Löschen. */
@Composable
internal fun SupertonicDownloadBereich() {
    val context = LocalContext.current
    val gold = LocalGold.current
    val arbeiten by remember { SupertonicModell.laeuftFlow(context) }.collectAsStateWithLifecycle(emptyList())
    var gestartet by remember { mutableStateOf(false) }
    // Takt für Fortschritt und Dateistand; liest nur Dateigrößen.
    var takt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(500); takt++ } }
    LaunchedEffect(arbeiten) { if (arbeiten.isNotEmpty()) gestartet = false }
    val laeuft = gestartet || arbeiten.any { !it.state.isFinished }
    takt
    val geladen = SupertonicModell.geladen(context)
    val b = SupertonicModell.bytesDa(context)
    val gesamt = SupertonicModell.gesamt
    when {
        geladen -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓ Stimmen geladen (${SupertonicModell.mb}) · arbeiten ohne Internet.", Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = gold.primaer)
            StillerKnopf("Löschen", { SupertonicModell.loeschen(context); takt++ })
        }
        laeuft -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val anteil = (b.toFloat() / gesamt).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(gold.textGedaempft.copy(alpha = .2f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(anteil).clip(RoundedCornerShape(50))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = .6f), gold.primaer))))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${(anteil * 100).toInt()} % · ${b / 1_000_000} von ${gesamt / 1_000_000} MB · lädt im Hintergrund weiter, " +
                    "auch wenn du die App schließt.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                StillerKnopf("Pausieren", { SupertonicModell.pausieren(context); gestartet = false; takt++ })
            }
        }
        !SupertonicModell.platzReicht(context) -> Text("Die Supertonic-Stimmen brauchen einen einmaligen Download von ${SupertonicModell.mb}. " +
            "Dafür ist auf dem Handy nicht genug Speicher frei – bitte etwas Platz freimachen.",
            style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Die Stimmen gibt es per Download: einmalig ${SupertonicModell.mb}, am besten im WLAN. Danach arbeiten sie ganz ohne Internet.",
                style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
            GoldKnopf(if (b > 0) "Download fortsetzen · noch ${(gesamt - b + 500_000) / 1_000_000} MB" else "Stimmen laden · ${SupertonicModell.mb}",
                { SupertonicModell.starteDownload(context); gestartet = true }, Modifier.fillMaxWidth(), hauptKnopf = true)
        }
    }
}
