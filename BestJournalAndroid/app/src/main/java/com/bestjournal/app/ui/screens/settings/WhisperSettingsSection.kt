package com.bestjournal.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R
import com.bestjournal.app.data.remote.groq.GroqWhisperSettings
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.util.EncryptedPrefsProvider

/** Settings card: Whisper model choice + personal vocabulary for the Groq transcription. */
@Composable
fun WhisperSettingsCard() {
    val context = LocalContext.current
    val prefs = remember { EncryptedPrefsProvider.get(context) }
    var model by remember { mutableStateOf(GroqWhisperSettings.storedModel(prefs)) }
    var vocabEnabled by remember { mutableStateOf(GroqWhisperSettings.isVocabularyEnabled(prefs)) }
    var vocabText by remember { mutableStateOf(GroqWhisperSettings.vocabularyText(prefs)) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_transcription_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Invisible counterbalance for icon+spacer so text is visually centered
                Spacer(modifier = Modifier.width(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.settings_whisper_model),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            listOf(
                GroqWhisperSettings.MODEL_TURBO to R.string.settings_whisper_model_turbo,
                GroqWhisperSettings.MODEL_LARGE_V3 to R.string.settings_whisper_model_large,
            ).forEach { (id, labelRes) ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth().clickable {
                            model = id
                            GroqWhisperSettings.saveModel(prefs, id)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = model == id,
                        onClick = {
                            model = id
                            GroqWhisperSettings.saveModel(prefs, id)
                        },
                        colors =
                            RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                stringResource(R.string.settings_whisper_model_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_vocabulary),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.settings_vocabulary_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = vocabEnabled,
                    onCheckedChange = {
                        vocabEnabled = it
                        GroqWhisperSettings.setVocabularyEnabled(prefs, it)
                    },
                    colors =
                        SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = vocabText,
                onValueChange = {
                    vocabText = it
                    GroqWhisperSettings.saveVocabularyText(prefs, it)
                },
                label = { Text(stringResource(R.string.settings_vocabulary_terms)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}
