package de.frank.entropyreducer.presentation.settings.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.notifications.AppNotification
import de.frank.entropyreducer.domain.notifications.NotificationGroup
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Frank-Wunsch 2026-08-12: Einstellungen -> "Benachrichtigungen".
 *
 * Listet JEDE Benachrichtigung auf, die die App verschickt, und macht sie einzeln
 * an- und abschaltbar. Die Liste kommt direkt aus dem Katalog [AppNotification] —
 * eine neue Benachrichtigung erscheint hier automatisch, sobald sie dort eingetragen ist.
 *
 * Drei Benachrichtigungen sind von Android vorgeschrieben (Vordergrund-Dienste:
 * Aufnahme, Vorlesen, laufender Polar-Import). Sie werden mit Erklaerung angezeigt
 * und lassen sich nur im System stummschalten — abschalten wuerde die Funktion
 * selbst zerstoeren.
 */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    vm: NotificationSettingsViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current

    // Nach der Rueckkehr aus den Android-Einstellungen neu pruefen, ob
    // Benachrichtigungen systemseitig erlaubt sind.
    var systemCheckToken by remember { mutableIntStateOf(0) }
    val systemAllowed = remember(systemCheckToken) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { systemCheckToken++ }

    CosmosScaffold(
        title = "Benachrichtigungen",
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
                    text = "Hier steht jede Benachrichtigung, die diese App verschickt. " +
                        "Schalte einzeln ab, was dich nicht erreichen soll — die Funktion " +
                        "dahinter läuft weiter, nur die Meldung entfällt. " +
                        "Zusätzlich gilt immer dein Schlaffenster: im Schlaf wird nichts zugestellt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }

            if (!systemAllowed) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.NotificationsOff,
                                    null,
                                    tint = cosmos.warn,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "Android blockiert alle Benachrichtigungen",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cosmos.warn,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Für diese App sind Benachrichtigungen im System abgeschaltet. " +
                                    "Solange das so ist, wirken die Schalter unten nicht — es kommt " +
                                    "gar nichts an.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textSecondary,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { settingsLauncher.launch(appNotificationSettings(context)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Android-Einstellungen öffnen")
                            }
                        }
                    }
                }
            }

            AppNotification.byGroup().forEach { (group, notifications) ->
                item(key = "header_${group.name}") {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = cosmos.accent,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                notifications.forEach { kind ->
                    item(key = kind.key) {
                        val toggle = vm.toggleFor(kind)
                        val checked by toggle.collectAsStateWithLifecycle()
                        NotificationToggleCard(
                            kind = kind,
                            checked = checked,
                            systemAllowed = systemAllowed,
                            onCheckedChange = { vm.setEnabled(kind, it) },
                            onOpenChannelSettings = {
                                settingsLauncher.launch(channelSettings(context, kind.channelId))
                            },
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "Alle auf einmal",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Setzt alle abschaltbaren Benachrichtigungen gemeinsam. Die von " +
                                "Android vorgeschriebenen bleiben unberührt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { vm.setAllSwitchable(true) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Alle an")
                            }
                            OutlinedButton(
                                onClick = { vm.setAllSwitchable(false) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Alle aus")
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
private fun NotificationToggleCard(
    kind: AppNotification,
    checked: Boolean,
    systemAllowed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenChannelSettings: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (kind.systemRequired) Icons.Outlined.Lock else Icons.Outlined.NotificationsActive,
                    null,
                    tint = if (kind.systemRequired) cosmos.textSecondary else cosmos.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    kind.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = if (kind.systemRequired) true else checked,
                    onCheckedChange = onCheckedChange,
                    enabled = !kind.systemRequired && systemAllowed,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                kind.description,
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            if (kind.systemRequired) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Von Android vorgeschrieben — lässt sich in der App nicht abschalten, " +
                        "im System aber stummschalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.warn,
                )
                TextButton(onClick = onOpenChannelSettings) {
                    Text("Im System stummschalten")
                }
            }
        }
    }
}

/** Systemseite "Benachrichtigungen" dieser App. */
private fun appNotificationSettings(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/** Systemseite eines einzelnen Benachrichtigungs-Kanals (Ton, Wichtigkeit, stumm). */
private fun channelSettings(context: Context, channelId: String): Intent =
    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

@HiltViewModel
class NotificationSettingsViewModel
@Inject
constructor(
    private val settings: AppSettings,
) : ViewModel() {

    private val toggles: Map<AppNotification, StateFlow<Boolean>> =
        AppNotification.entries.associateWith { kind ->
            settings.notificationEnabledFlow(kind.key)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        }

    fun toggleFor(kind: AppNotification): StateFlow<Boolean> = toggles.getValue(kind)

    fun setEnabled(kind: AppNotification, value: Boolean) {
        if (kind.systemRequired) return
        viewModelScope.launch { settings.setNotificationEnabled(kind.key, value) }
    }

    /** Setzt alle nicht-systembedingten Benachrichtigungen gemeinsam. */
    fun setAllSwitchable(value: Boolean) {
        viewModelScope.launch {
            AppNotification.entries
                .filterNot { it.systemRequired }
                .forEach { settings.setNotificationEnabled(it.key, value) }
        }
    }
}
