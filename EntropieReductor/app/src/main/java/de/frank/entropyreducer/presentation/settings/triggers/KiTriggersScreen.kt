package de.frank.entropyreducer.presentation.settings.triggers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.DateFormat
import java.util.Date

/**
 * Approval-Screen für die KI-vorgeschlagenen Trigger (Spec §16.2).
 * Zwei Sektionen:
 *  - Vorgeschlagen: pro Eintrag „Annehmen" / „Ablehnen"
 *  - Aktiv: pro Eintrag „Pausieren" / „Löschen"
 */
@Composable
fun KiTriggersScreen(
    onBack: () -> Unit,
    vm: KiTriggersViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "KI-Trigger",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Die KI beobachtet deine Daten und schlaegt zweimal pro Woche neue Trigger vor. " +
                        "Du entscheidest, was die App damit machen darf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }
            if (state.pending.isNotEmpty()) {
                item { SectionHeader("Vorgeschlagen", state.pending.size, LocalCosmos.current.accentForscher) }
                items(state.pending, key = { "p-${it.id}" }) { t ->
                    PendingTriggerCard(
                        trigger = t,
                        onApprove = { vm.approve(t) },
                        onReject = { vm.reject(t) },
                    )
                }
            }
            if (state.active.isNotEmpty()) {
                item { SectionHeader("Aktiv", state.active.size, LocalCosmos.current.accent) }
                items(state.active, key = { "a-${it.id}" }) { t ->
                    ActiveTriggerCard(
                        trigger = t,
                        onDeactivate = { vm.deactivate(t) },
                        onDelete = { vm.delete(t) },
                    )
                }
            }
            if (state.pending.isEmpty() && state.active.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Noch keine Trigger.",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Die KI schlaegt zweimal pro Woche neue Trigger vor — Mittwoch und Sonntag um 11:00. " +
                                    "Du kannst sie jederzeit auch manuell anstoßen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, accent: Color) {
    val cosmos = LocalCosmos.current
    Row {
        Text(
            "$label ($count)",
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PendingTriggerCard(
    trigger: KiTriggerEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                trigger.name,
                style = MaterialTheme.typography.titleSmall,
                color = LocalCosmos.current.accentForscher,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Bedingung: ${trigger.condition}",
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Aktion: ${trigger.proposedAction}",
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Vorgeschlagen: ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(trigger.proposedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Ablehnen")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCosmos.current.accent,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Annehmen")
                }
            }
        }
    }
}

@Composable
private fun ActiveTriggerCard(
    trigger: KiTriggerEntity,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row {
                Icon(Icons.Outlined.Bolt, null, tint = LocalCosmos.current.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    trigger.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalCosmos.current.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                trigger.condition,
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Wirkung: ${trigger.proposedAction}",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            val firedText = if (trigger.lastTriggeredAt != null) {
                "Zuletzt gefeuert: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(trigger.lastTriggeredAt))}, insgesamt ${trigger.triggerCount}x"
            } else {
                "Noch nicht gefeuert."
            }
            Text(
                firedText,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDeactivate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Pause, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Pausieren")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Löschen")
                }
            }
        }
    }
}
