package de.frank.entropyreducer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.BuildConfig
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/** Top-Liste der 7 Settings-Sektionen (Spec §6, Bild 16/26). */
@Composable
fun SettingsHomeScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onSwitchTab: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    val sections = sectionsFor()
    CosmosScaffold(
        title = "Einstellungen",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.SETTINGS_HOME,
                micState = MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = { },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_UPDATED_AT})",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            sections.forEach { sec ->
                item {
                    SectionCard(
                        icon = sec.icon,
                        accent = sec.accent,
                        title = sec.title,
                        subtitle = sec.subtitle,
                        onClick = { onOpen(sec.route) },
                    )
                }
            }
        }
    }
}

private data class SectionDef(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val subtitle: String,
    val route: String,
)

@Composable
private fun sectionsFor(): List<SectionDef> =
    listOf(
        SectionDef(
            icon = Icons.Outlined.RecordVoiceOver,
            accent = LocalCosmos.current.accent,
            title = "Vorlesen",
            subtitle = "Lege fest, wie lange Mental- und Gewohnheits-Sätze beim Vorlesen pausieren.",
            route = Routes.SETTINGS_TTS,
        ),
        SectionDef(
            icon = Icons.Outlined.CalendarMonth,
            accent = LocalCosmos.current.accent,
            title = "Experiment-Kalender",
            subtitle = "Tag/Wochen/Monatsansicht aller laufenden und vorgeschlagenen Hypothesen.",
            route = Routes.EXPERIMENT_CALENDAR,
        ),
        SectionDef(
            icon = Icons.Outlined.Lightbulb,
            accent = LocalCosmos.current.warn,
            title = "Insight Board",
            subtitle = "Bestätigte Methoden, Beobachtungen und verworfene Ansätze.",
            route = Routes.INSIGHT_BOARD,
        ),
        SectionDef(
            icon = Icons.Outlined.EmojiEvents,
            accent = LocalCosmos.current.ok,
            title = "Mein Repertoire",
            subtitle = "Deine bewaehrtesten Hebel — sortiert nach Wirkung × Wiederholung.",
            route = Routes.REPERTOIRE,
        ),
        SectionDef(
            icon = Icons.Outlined.Key,
            accent = LocalCosmos.current.accent,
            title = "API-Schlüssel",
            subtitle = "Verwalte deine API-Schlüssel und verknuepfe deine bevorzugten Anbieter.",
            route = Routes.SETTINGS_API,
        ),
        SectionDef(
            icon = Icons.Outlined.CloudSync,
            accent = LocalCosmos.current.accentTasksSub,
            title = "Second Brain",
            subtitle = "Wähle, welche Bereiche (z. B. Ideen) automatisch ins Second Brain synchronisiert werden.",
            route = Routes.SETTINGS_SECOND_BRAIN,
        ),
        SectionDef(
            icon = Icons.Outlined.Psychology,
            accent = LocalCosmos.current.accentForscher,
            title = "KI-Modell-Auswahl",
            subtitle = "Wähle das passende KI-Modell für deine Fragen und Aufgaben aus.",
            route = Routes.SETTINGS_MODELS,
        ),
        SectionDef(
            icon = Icons.Outlined.Person,
            accent = LocalCosmos.current.ok,
            title = "Persönliches Profil",
            subtitle = "Passe deine Vorlieben, Ziele und Einstellungen an.",
            route = Routes.SETTINGS_PROFILE,
        ),
        SectionDef(
            icon = Icons.Outlined.ChatBubbleOutline,
            accent = LocalCosmos.current.warn,
            title = "Eigene Prompts",
            subtitle = "Erstelle, verwalte und organisiere deine eigenen Prompt-Vorlagen.",
            route = Routes.SETTINGS_PROMPTS,
        ),
        SectionDef(
            icon = Icons.Outlined.Memory,
            accent = LocalCosmos.current.accent,
            title = "Gedächtnis",
            subtitle = "Entscheide, was sich das Genie merken darf und wie es dich unterstuetzt.",
            route = Routes.SETTINGS_MEMORY,
        ),
        SectionDef(
            icon = Icons.Outlined.Bolt,
            accent = LocalCosmos.current.accentTasksSub,
            title = "Prioritäts-Gedächtnis",
            subtitle = "Gemerkte Aufgaben-Prioritäten, die die KI für neue, sehr ähnliche Aufgaben nutzt.",
            route = Routes.SETTINGS_PRIORITY_MEMORY,
        ),
        SectionDef(
            icon = Icons.Outlined.AutoAwesome,
            accent = LocalCosmos.current.accentForscher,
            title = "Genie-Codex",
            subtitle = "Lege Verhaltensregeln, Werte und Antwortstil für das Genie fest.",
            route = Routes.SETTINGS_CODEX,
        ),
        SectionDef(
            icon = Icons.Outlined.Bolt,
            accent = LocalCosmos.current.accent,
            title = "KI-Trigger",
            subtitle =
                "Beobachte und genehmige automatische Trigger, die das Genie aus deinen Daten ableitet.",
            route = Routes.SETTINGS_TRIGGERS,
        ),
        SectionDef(
            icon = Icons.Outlined.Inventory2,
            accent = LocalCosmos.current.accentForscher,
            title = "Archiv",
            subtitle = "Erledigte Aufgaben mit deiner Lösungsmethode — Quelle für den Forscher.",
            route = Routes.SETTINGS_ARCHIVE,
        ),
        SectionDef(
            icon = Icons.Outlined.IosShare,
            accent = LocalCosmos.current.crit,
            title = "Datenexport / Datenschutz",
            subtitle = "Exportiere deine Daten oder überwache den Datenschutz.",
            route = Routes.SETTINGS_EXPORT,
        ),
        SectionDef(
            icon = Icons.Outlined.MonitorHeart,
            accent = LocalCosmos.current.accentForscher,
            title = "Diagnose-Protokoll",
            subtitle =
                "Fehler und Erfolge aller Verknuepfungen (Whoop, Oura, Drive, Kalender, KI-Schluessel) — siehst du warum etwas nicht aktualisiert.",
            route = Routes.SETTINGS_DIAGNOSTICS,
        ),
    )

@Composable
private fun SectionCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = cosmos.textSecondary)
        }
    }
}
