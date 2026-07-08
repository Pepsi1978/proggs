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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import de.frank.entropyreducer.data.remote.brain.SecondBrainArea
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
                        "richtest du im Bereich „API-Schlüssel“ ein. Mentals werden absichtlich " +
                        "nie synchronisiert: Sie bleiben ausschließlich persönliche Handy-Inhalte.",
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
            vm.areaToggles.forEach { areaToggle ->
                item(key = areaToggle.area.key) {
                    val checked by areaToggle.enabled.collectAsStateWithLifecycle()
                    AreaToggleCard(
                        title = areaToggle.area.label,
                        description = areaDescription(areaToggle.area),
                        checked = checked,
                        onCheckedChange = { vm.setAreaEnabled(areaToggle.area.key, it) },
                        enabled = vm.hasConnection,
                    )
                }
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
                                Text("Aktive Bereiche jetzt ins Second Brain speichern")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Vollständiger Neu-Sync: löscht die aktiven Kategorien im Second Brain " +
                                "und schreibt die passenden Einträge deines Handys frisch im aktuellen " +
                                "Format neu. Deine App-Einträge bleiben unverändert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = vm::resyncAll,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = vm.hasConnection && !syncState.syncing,
                            colors = ButtonDefaults.buttonColors(containerColor = cosmos.accentTasksSub),
                        ) {
                            Text("Aktive Bereiche neu ins Second Brain schreiben")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun areaDescription(area: SecondBrainArea): String = when (area.key) {
    "ideas" -> "Jede Idee aus „Aufgaben → Ideen“ wird in der Kategorie „Ideen“ gespeichert und bidirektional synchron gehalten."
    "habits" -> "Alle festen Gewohnheiten über den Gewohnheitsvorschlägen werden in der Kategorie „Gewohnheiten“ synchron gehalten."
    "entropy" -> "Alle Entropie-Einträge werden in der Kategorie „Entropie“ synchron gehalten."
    "theses" -> "Alle Thesen werden in der Kategorie „Thesen“ synchron gehalten."
    "journal" -> "Alle Einträge aus dem Journal-Reiter werden in der Kategorie „Tagebucheinträge“ synchron gehalten."
    else -> "Dieser Bereich wird in der Kategorie „${area.category}“ synchron gehalten."
}

data class SecondBrainAreaToggle(
    val area: SecondBrainArea,
    val enabled: StateFlow<Boolean>,
)

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

    val areaToggles: List<SecondBrainAreaToggle> = connector.areas.map { area ->
        SecondBrainAreaToggle(
            area = area,
            enabled = settings.secondBrainConnectorEnabledFlow(area.key)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false),
        )
    }

    val syncState: StateFlow<SecondBrainIdeaSyncState> =
        connector.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SecondBrainIdeaSyncState(),
        )

    fun setAreaEnabled(areaKey: String, value: Boolean) {
        viewModelScope.launch { settings.setSecondBrainConnectorEnabled(areaKey, value) }
    }

    fun syncNow() {
        connector.syncAllNow(viewModelScope)
    }

    /** Vollstaendiger Neu-Sync: aktive Brain-Kategorien leeren + Handy-Inhalte frisch hochladen. */
    fun resyncAll() {
        connector.resyncAll(viewModelScope)
    }
}
