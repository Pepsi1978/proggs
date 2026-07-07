package de.frank.entropyreducer.presentation.settings.codex

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.repository.GenieCodexRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MarkdownView
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CodexUiState(
    val latest: GenieCodexVersionEntity? = null,
    val history: List<GenieCodexVersionEntity> = emptyList(),
    val lastSyntheseAt: Long = 0L,
    val isRunning: Boolean = false,
)

@HiltViewModel
class CodexViewModel @Inject constructor(
    private val repo: GenieCodexRepository,
    private val settings: AppSettings,
    private val scheduler: BackgroundScheduler,
) : ViewModel() {

    private val runningFlow = MutableStateFlow(false)

    val state: StateFlow<CodexUiState> = combine(
        repo.observeLatest(),
        repo.observeRecent(),
        settings.lastCodexSyntheseMsFlow,
        runningFlow,
    ) { latest, history, lastAt, running ->
        CodexUiState(latest, history, lastAt, running)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CodexUiState())

    fun refreshNow() {
        if (runningFlow.value) return
        viewModelScope.launch {
            runningFlow.value = true
            scheduler.runCodexNow()
            // Worker laeuft asynchron — UI-State faellt zurück, sobald die neue Version
            // im Repository erscheint. Wir resetten den Spinner nach 2 s, damit der Button
            // nicht ewig deaktiviert bleibt.
            kotlinx.coroutines.delay(2_000)
            runningFlow.value = false
        }
    }
}

@Composable
fun CodexScreen(
    onBack: () -> Unit,
    vm: CodexViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = "Genie-Codex",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GlassCard {
                    Column {
                        Text(
                            text = "Was die KI über dich verstanden hat",
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Wird automatisch sonntags 19:00 aktualisiert. Du kannst eine neue Synthese auch jederzeit manuell anstoßen.",
                            color = cosmos.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (state.lastSyntheseAt > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Letzte Synthese: ${formatTimestamp(state.lastSyntheseAt)}",
                                color = cosmos.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = vm::refreshNow,
                    enabled = !state.isRunning,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCosmos.current.accentForscher,
                        contentColor = CosmosColors.BgDark,
                        disabledContainerColor = LocalCosmos.current.accentForscher.copy(alpha = 0.4f),
                    ),
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CosmosColors.BgDark,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Synthese laeuft …")
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Jetzt aktualisieren", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                GlassCard {
                    if (state.latest == null) {
                        Text(
                            text = "Noch keine Synthese vorhanden. Tippe auf \"Jetzt aktualisieren\", um die erste Genie-Codex-Version zu erstellen.",
                            color = cosmos.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        MarkdownView(text = state.latest!!.content)
                    }
                }
            }

            if (state.history.size > 1) {
                item {
                    Text(
                        text = "Versionshistorie",
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.history.drop(1)) { v ->
                    GlassCard {
                        Column {
                            Text(
                                text = formatTimestamp(v.createdAt),
                                color = cosmos.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = v.content.take(280).let { if (v.content.length > 280) "$it…" else it },
                                color = cosmos.textPrimary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    list: List<GenieCodexVersionEntity>,
    content: @Composable (GenieCodexVersionEntity) -> Unit,
) {
    list.forEach { v -> item(v.id) { content(v) } }
}

private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private fun formatTimestamp(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime().format(FORMAT)
