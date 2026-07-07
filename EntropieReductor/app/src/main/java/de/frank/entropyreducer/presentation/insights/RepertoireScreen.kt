package de.frank.entropyreducer.presentation.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/** "Mein Repertoire" (Spec §14.1): nach Wirkung sortierte Liste der bestaetigten Insights. */
@Composable
fun RepertoireScreen(
    onBack: () -> Unit,
    onOpenInsight: (String) -> Unit,
    vm: RepertoireViewModel = hiltViewModel(),
) {
    val items by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = "Mein Repertoire",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (items.isEmpty()) {
                GlassCard {
                    Text(
                        text =
                            "Noch keine bestätigten Methoden — schließe Hypothesen erfolgreich ab und Insights mit Confidence über 50 % landen hier.",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { insight ->
                        InsightCard(insight, onClick = { onOpenInsight(insight.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
