package de.frank.jarvis.bio.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.jarvis.bio.BioAbruf
import de.frank.jarvis.bio.BioModul
import de.frank.jarvis.bio.BioSpiegel
import de.frank.jarvis.bio.domain.usecase.ApiSyncBericht
import de.frank.jarvis.bio.domain.usecase.QuellenStand
import de.frank.jarvis.bio.presentation.components.GlassCard
import de.frank.jarvis.bio.presentation.theme.LocalCosmos
import de.frank.jarvis.data.Geraet

/**
 * Ehrlicher Stand je Quelle nach dem letzten Abruf: neue Werte, abgerufen ohne neue Messung, nicht gefragt (mit Grund),
 * pausiert oder gestört (mit letztem Erfolg), dazu das Datum der neuesten Messung. Liest nur, löst nie einen Abruf aus.
 */
@Composable
fun BioAbrufStandKarte(modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val stand by BioAbruf.stand.collectAsStateWithLifecycle()
    val bericht by produceState<ApiSyncBericht?>(stand.bericht, stand.laeuft, stand.bericht) {
        value = stand.bericht ?: if (Geraet.istTablet) null else runCatching { BioModul.sync.letzterStand() }.getOrNull()
    }
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stand.laeuft) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = cosmos.accent)
                val kopf = when {
                    stand.laeuft -> "Abruf läuft (${stand.anlass}) …"
                    Geraet.istTablet -> "Spiegel des Handys: " + BioSpiegel.standText(context)
                    bericht != null -> "Letzter Abruf: ${ApiSyncBericht.uhr(bericht!!.versuchMs)} Uhr · ${bericht!!.anlass}"
                    !BioAbruf.bereit(context) -> "Noch kein Abruf: Die Übernahme der Biodaten nach Jarvis ist nicht abgeschlossen."
                    else -> "Noch kein Abruf in Jarvis"
                }
                Text(kopf, color = cosmos.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            bericht?.quellen?.forEach { q -> QuellenZeile(q) }
        }
    }
}

@Composable
private fun QuellenZeile(q: QuellenStand) {
    val cosmos = LocalCosmos.current
    val (text, farbe) = when {
        q.pausiert -> "pausiert" to cosmos.textSecondary
        !q.verbunden -> "nicht verbunden" to cosmos.textSecondary
        !q.gefragt -> ("nicht gefragt" + (q.nichtGefragtGrund?.let { " ($it)" } ?: "")) to cosmos.textSecondary
        q.ok && q.neueWerte -> "neue Werte" to cosmos.ok
        q.ok -> "abgerufen, keine neue Messung" to cosmos.textSecondary
        else -> ("gestört: " + (q.fehler ?: "Fehler") +
            (if (q.zuletztOkMs > 0L) ", letzter Erfolg am ${ApiSyncBericht.uhr(q.zuletztOkMs)} Uhr" else "") +
            (if (q.eingriffNoetig) " – neu anmelden" else "")) to cosmos.crit
    }
    val messung = if (q.neuesteMessungMs > 0L) " · neueste Messung ${ApiSyncBericht.uhr(q.neuesteMessungMs)} Uhr" else ""
    Text(
        "${q.name}: $text$messung",
        color = farbe,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}
