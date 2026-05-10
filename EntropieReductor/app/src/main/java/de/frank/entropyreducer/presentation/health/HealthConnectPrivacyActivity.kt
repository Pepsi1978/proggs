package de.frank.entropyreducer.presentation.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import de.frank.entropyreducer.presentation.theme.EntropieReductorTheme

/**
 * Pflicht-Activity fuer die Health-Connect-Integration (Frank-Wunsch 2026-05-10).
 *
 * Google verlangt seit Android 14, dass jede App die Health Connect benutzt, eine
 * Activity bereitstellt, die den Benutzer ueber die Datenverwendung aufklaert.
 * Diese Activity wird vom System aufgerufen wenn der Benutzer im Health-Connect-
 * Permission-Dialog auf "Datenschutzerklaerung anzeigen" tippt — ODER ueber den
 * intent VIEW_PERMISSION_USAGE in den Android-Einstellungen.
 *
 * Wir verwenden Health Connect ausschliesslich nur-lesend fuer Gewicht. Die
 * Daten werden lokal in der App angezeigt und nicht an Dritte weitergegeben.
 */
@AndroidEntryPoint
class HealthConnectPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EntropieReductorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrivacyContent()
                }
            }
        }
    }
}

@Composable
private fun PrivacyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Health-Connect-Datenschutz",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Was die App liest",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Die Entropie Reductor liest ausschliesslich deinen Gewichts-Wert " +
                "aus Health Connect. Diese Daten werden in der App selbst angezeigt " +
                "und in der lokalen Datenbank gespeichert, damit du Verlauf und " +
                "30-Tage-Mittel sehen kannst.",
        )
        Text(
            text = "Was die App NICHT macht",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "• Es werden keine Daten an Dritte uebertragen.\n" +
                "• Es werden keine Werte nach Health Connect zurueck geschrieben.\n" +
                "• Es werden keine anderen Datentypen aus Health Connect gelesen.\n" +
                "• Du kannst die Erlaubnis jederzeit in den Health-Connect-Einstellungen widerrufen.",
        )
        Text(
            text = "Datenquelle",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Die Gewichtsdaten kommen typischerweise aus der Zepp-App (verbunden " +
                "mit deiner Amazfit Smart Scale). Wenn du die Verbindung zwischen " +
                "Zepp und Health Connect deaktivierst, sieht die App keine neuen Werte mehr.",
        )
    }
}
