package com.entropyjournal.ui.screens.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.entropyjournal.data.remote.groq.GroqWhisperSettings

/** Settings card content: Whisper model choice + personal vocabulary for Groq transcription. */
@Composable
fun WhisperSettingsSection(prefs: SharedPreferences) {
    var model by remember { mutableStateOf(GroqWhisperSettings.storedModel(prefs)) }
    var vocabEnabled by remember { mutableStateOf(GroqWhisperSettings.isVocabularyEnabled(prefs)) }
    var vocabText by remember { mutableStateOf(GroqWhisperSettings.vocabularyText(prefs)) }
    var savedVocabText by remember { mutableStateOf(vocabText) }
    val latestVocabText by rememberUpdatedState(vocabText)

    // Save the vocabulary 500 ms after the last keystroke (encrypted prefs, not per keystroke)
    // and once more when the card leaves the screen, so no edit is lost.
    LaunchedEffect(vocabText) {
        if (vocabText != savedVocabText) {
            delay(500)
            GroqWhisperSettings.saveVocabularyText(prefs, vocabText)
            savedVocabText = vocabText
        }
    }
    DisposableEffect(prefs) {
        onDispose {
            if (latestVocabText != savedVocabText) {
                GroqWhisperSettings.saveVocabularyText(prefs, latestVocabText)
            }
        }
    }

    Column {
        Text(
            "Spracherkennung (Groq Whisper)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Whisper-Modell",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        listOf(
            GroqWhisperSettings.MODEL_TURBO to "Large V3 Turbo (schnell)",
            GroqWhisperSettings.MODEL_LARGE_V3 to "Large V3 (genauer)",
        ).forEach { (id, label) ->
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
                        RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            "Large V3 muss in der Groq-Konsole freigeschaltet sein, sonst wird automatisch Turbo verwendet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Persönliches Wörterbuch",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Hilft Whisper bei der Schreibweise von Namen und Fachbegriffen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = vocabEnabled,
                onCheckedChange = {
                    vocabEnabled = it
                    GroqWhisperSettings.setVocabularyEnabled(prefs, it)
                },
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextField(
            value = vocabText,
            onValueChange = { vocabText = it },
            label = { Text("Begriffe (durch Komma oder Zeilenumbruch getrennt)") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
