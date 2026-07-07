package de.frank.entropyreducer.presentation.settings.models

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import de.frank.entropyreducer.presentation.settings.ModelsViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun ModelsScreen(onBack: () -> Unit, vm: ModelsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "KI-Modell-Auswahl",
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
                ModelGroup(
                    title = "Whisper-Modell (Groq)",
                    options = listOf("whisper-large-v3-turbo", "whisper-large-v3"),
                    current = state.whisperModel,
                    onSelect = vm::setWhisper,
                )
            }
            item {
                ModelGroup(
                    title = "Gemini-Modell",
                    options =
                        listOf(
                            // Gemini 3.x — neueste Generation
                            "gemini-3-flash-preview",
                            "gemini-3.1-flash-lite",
                            // Gemini 2.x — bewaehrte Generation
                            "gemini-2.5-flash",
                            "gemini-2.5-flash-lite",
                            "gemini-2.0-flash",
                        ),
                    current = state.geminiModel,
                    onSelect = vm::setGemini,
                )
            }
            item {
                ModelGroup(
                    title = "Sprache der Transkription",
                    options = listOf("de", "auto"),
                    current = state.transcriptionLanguage,
                    onSelect = vm::setLanguage,
                )
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "TTS-Stimme",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Wird beim ersten Verbinden mit der Google-TTS-API aus den verfügbaren Chirp-3-HD-Stimmen geladen. Stufe 4.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelGroup(
    title: String,
    options: List<String>,
    current: String,
    onSelect: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            options.forEach { opt ->
                val active = opt == current
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(opt) }
                            .padding(vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = active,
                        onClick = { onSelect(opt) },
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    Text(
                        text = opt,
                        color = if (active) LocalCosmos.current.accent else cosmos.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
