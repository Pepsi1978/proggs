package de.frank.entropyreducer.presentation.settings.secondbrain

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaConnector
import de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaSyncState
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Frank-Wunsch 2026-07-04: Eigener Einstellungsbereich „Second Brain" (getrennt vom API-Schlüssel-
 * Bereich, der nur die Verbindung verwaltet). Hier werden pro Bereich Schalter angeboten, welche
 * Inhalte ins Second Brain synchronisiert werden. Aktuell: „Ideen" (erweiterbar aufgebaut).
 */
@Composable
fun SecondBrainSettingsScreen(
    onBack: () -> Unit,
    vm: SecondBrainSettingsViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val ideasEnabled by vm.ideasEnabled.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    CosmosScaffold(
        title = "Second Brain",
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
                    text = "Wähle aus, welche Bereiche der App automatisch in dein Second Brain " +
                        "synchronisiert werden. Die Verbindung selbst (Schlüssel + WireGuard) " +
                        "richtest du im Bereich „API-Schlüssel“ ein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }
            if (!vm.hasConnection) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Noch keine Verbindung eingerichtet. Hinterlege zuerst unter " +
                                "„Einstellungen → API-Schlüssel“ den Second-Brain-Schlüssel und die " +
                                "WireGuard-Konfiguration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.warn,
                        )
                    }
                }
            }
            item {
                AreaToggleCard(
                    title = "Ideen",
                    description = "Jede Idee aus „Aufgaben → Ideen“ wird als Eintrag in der " +
                        "Second-Brain-Kategorie „Ideen“ gespeichert und synchron gehalten: " +
                        "Änderungen werden hochgeladen, in der App gelöschte Ideen im Brain entfernt.",
                    checked = ideasEnabled,
                    onCheckedChange = vm::setIdeasEnabled,
                    enabled = vm.hasConnection,
                )
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "Synchronisation",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.accentTasksSub,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            syncState.lastMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = vm::syncNow,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = vm.hasConnection && !syncState.syncing,
                        ) {
                            if (syncState.syncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Synchronisiere …")
                            } else {
                                Text("Alt-Ideen jetzt ins Second Brain speichern")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AreaToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    null,
                    tint = cosmos.accentTasksSub,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
        }
    }
}

@HiltViewModel
class SecondBrainSettingsViewModel
@Inject
constructor(
    private val settings: AppSettings,
    private val connector: SecondBrainIdeaConnector,
    secrets: EncryptedSecretsStore,
) : ViewModel() {
    /** Verbindung eingerichtet = Second-Brain-API-Key hinterlegt (Bereich „API-Schlüssel"). */
    val hasConnection: Boolean = !secrets.secondBrainApiKey.isNullOrBlank()

    val ideasEnabled: StateFlow<Boolean> =
        settings.secondBrainIdeasConnectorEnabledFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val syncState: StateFlow<SecondBrainIdeaSyncState> =
        connector.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SecondBrainIdeaSyncState(),
        )

    fun setIdeasEnabled(value: Boolean) {
        viewModelScope.launch { settings.setSecondBrainIdeasConnectorEnabled(value) }
    }

    fun syncNow() {
        connector.syncAllNow(viewModelScope)
    }
}
