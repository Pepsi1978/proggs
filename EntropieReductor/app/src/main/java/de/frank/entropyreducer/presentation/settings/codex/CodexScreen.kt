package de.frank.entropyreducer.presentation.settings.codex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun CodexScreen(onBack: () -> Unit) {
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "Genie-Codex",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Genie-Codex",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hier erscheint die wochentliche Synthese der KI: was sie aktuell ueber dich versteht. " +
                                "Wird in Stufe 4 freigeschaltet — automatisch sonntags 19:00 Uhr und manuell anstossbar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            }
        }
    }
}
