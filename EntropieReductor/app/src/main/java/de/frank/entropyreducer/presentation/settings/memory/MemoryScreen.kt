package de.frank.entropyreducer.presentation.settings.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.MemoryViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun MemoryScreen(onBack: () -> Unit, vm: MemoryViewModel = hiltViewModel()) {
    val memories by vm.memories.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    var adding by remember { mutableStateOf(false) }

    // Frank-Wunsch 2026-05-20: "Aus Profil — Neu generieren" loescht alle bestehenden
    // AUS_PROFIL-Memories und ruft Gemini erneut. Wir holen uns den ProfileViewModel
    // hier per hiltViewModel — dieselbe Instanz wie im ProfileScreen wenn Activity gleich.
    val profileVm: de.frank.entropyreducer.presentation.settings.ProfileViewModel = hiltViewModel()
    val distillState by profileVm.distillState.collectAsStateWithLifecycle()
    val distillError by profileVm.distillError.collectAsStateWithLifecycle()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(distillError) {
        distillError?.let {
            snackbar.showSnackbar(it)
            profileVm.dismissDistillError()
        }
    }
    androidx.compose.runtime.LaunchedEffect(distillState) {
        if (distillState == de.frank.entropyreducer.presentation.settings.DistillState.DONE) {
            snackbar.showSnackbar("Gedächtnis aus Profil neu generiert.")
        }
    }
    val isRegenerating =
        distillState == de.frank.entropyreducer.presentation.settings.DistillState.RUNNING

    CosmosScaffold(
        title = "Gedächtnis",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 3 Buttons oben — Frank-Reklamation 2026-05-08: vorher waren die Texte
                // zu lang für weight(1f), der 3. Button "Aus Profil neu generieren"
                // wurde auf 4 Zeilen umgebrochen (sah aus wie "reine Kugel"). Jetzt
                // jede Card mit Icon oben + zwei kuerzeren Zeilen darunter (Titel + Hint),
                // gleiche Hoehe, gleiche Akzentfarbe pro Aktion.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MemoryActionButton(
                        icon = Icons.Outlined.Add,
                        title = "Hinzufügen",
                        subtitle = "Manuell",
                        accent = LocalCosmos.current.accent,
                        onClick = { adding = true },
                        modifier = Modifier.weight(1f),
                    )
                    MemoryActionButton(
                        icon = Icons.Outlined.RateReview,
                        title = "KI-Vorschläge",
                        subtitle = "Prüfen",
                        accent = LocalCosmos.current.accentForscher,
                        onClick = { /* Stufe 4 */ },
                        modifier = Modifier.weight(1f),
                    )
                    MemoryActionButton(
                        icon = Icons.Outlined.AutoFixHigh,
                        title = "Aus Profil",
                        subtitle = if (isRegenerating) "Läuft …" else "Neu generieren",
                        accent = LocalCosmos.current.ok,
                        onClick = { if (!isRegenerating) profileVm.regenerateFromProfile() },
                        modifier = Modifier.weight(1f),
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (memories.isEmpty()) {
                        item {
                            Text(
                                "Noch keine Gedächtnis-Einträge. Manuell hinzufügen oder aus dem Profil generieren.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                    items(memories, key = { it.id }) { m ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    m.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cosmos.textPrimary,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SourcePill(m.source)
                                    if (m.source == MemorySource.KI_VORSCHLAG) {
                                        Spacer(Modifier.width(8.dp))
                                        ConfidenceBar(m.confidence)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Switch(
                                        checked = m.isActive,
                                        onCheckedChange = { vm.toggle(m) },
                                        colors =
                                            SwitchDefaults.colors(
                                                checkedThumbColor = LocalCosmos.current.accent
                                            ),
                                    )
                                    IconButton(onClick = { vm.delete(m) }) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            "Löschen",
                                            tint = LocalCosmos.current.crit,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            androidx.compose.material3.SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(16.dp),
            ) {
                androidx.compose.material3.Snackbar(it)
            }
        }
    }

    if (adding) {
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            vm.add(content.trim())
                            adding = false
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = LocalCosmos.current.accent,
                            contentColor = Color.Black,
                        ),
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Abbrechen") } },
            title = { Text("Memory hinzufügen") },
            text = {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Was soll die KI über dich wissen?") },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            },
        )
    }
}

/**
 * Action-Button mit Icon-in-Kreis + Titel + Subtitel — für die 3 Aktionen oben im
 * Gedaechtnis-Screen. Vorher waren das schmale OutlinedButtons mit zu langen Texten die sich auf
 * 3-4 Zeilen umbrachen ("reine Kugel"-Effekt).
 */
@Composable
private fun MemoryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier.size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = cosmos.textPrimary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SourcePill(source: MemorySource) {
    val (label, color) =
        when (source) {
            MemorySource.MANUELL -> "MANUELL" to LocalCosmos.current.accent
            MemorySource.KI_VORSCHLAG -> "KI-VORSCHLAG" to LocalCosmos.current.accentForscher
            MemorySource.AUS_PROFIL -> "AUS PROFIL" to LocalCosmos.current.ok
        }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier =
            Modifier.clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ConfidenceBar(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Vertrauen",
            style = MaterialTheme.typography.labelSmall,
            color = LocalCosmos.current.textSecondary,
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier.size(width = 64.dp, height = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LocalCosmos.current.glassBg)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier.fillMaxWidth(percent / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LocalCosmos.current.accent)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "$percent %",
            style = MaterialTheme.typography.labelSmall,
            color = LocalCosmos.current.textSecondary,
        )
    }
}
