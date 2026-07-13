package de.frank.entropyreducer.presentation.dashboard1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.launch

/**
 * Frank-Wunsch 2026-05-31: Review-Fenster, das nach dem Einsprechen einer Aufgabe erscheint (sobald
 * das Transkript erkannt wurde). Frank kann:
 * - den transkribierten Text pruefen und direkt bearbeiten,
 * - ihn per "Mit KI verbessern" (Gemini) sprachlich aufwerten lassen (Inhalt + ungefaehre Laenge
 *   bleiben),
 * - optional einen eigenen Titel eintippen (leer → KI erstellt den Titel),
 * - speichern oder abbrechen.
 *
 * Vorbild: das Aufnahme-Fenster in BestJournalFrank.
 */
@Composable
fun TaskCaptureReviewDialog(
    initialTranscript: String,
    initialTitle: String,
    accent: Color,
    onImprove: suspend (String) -> Result<String>,
    onSave: (text: String, manualTitle: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val scope = rememberCoroutineScope()
    // Bei einem neuen Transkript starten die Felder frisch.
    var text by remember(initialTranscript) { mutableStateOf(initialTranscript) }
    var title by remember(initialTranscript) { mutableStateOf(initialTitle) }
    var textBeforeImprovement by remember(initialTranscript) { mutableStateOf<String?>(null) }
    var improving by remember(initialTranscript) { mutableStateOf(false) }
    var error by remember(initialTranscript) { mutableStateOf<String?>(null) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = cosmos.textPrimary,
            unfocusedTextColor = cosmos.textPrimary,
            focusedBorderColor = accent,
            unfocusedBorderColor = cosmos.glassBorder,
            cursorColor = accent,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor =
            if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
        title = {
            Text(
                text = "Aufgabe prüfen",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Der KI-Titel ist direkt editierbar, bevor die Aufgabe gespeichert wird.
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel", color = cosmos.textSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                // Der transkribierte Text — direkt bearbeitbar.
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Eingesprochener Text", color = cosmos.textSecondary) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    colors = fieldColors,
                )
                // "Mit KI verbessern" — wertet den Text per Gemini auf.
                Column {
                    TextButton(
                        onClick = {
                            if (improving) return@TextButton
                            error = null
                            improving = true
                            scope.launch {
                                onImprove(text)
                                    .onSuccess {
                                        textBeforeImprovement = text
                                        text = it
                                    }
                                    .onFailure {
                                        error =
                                            it.message?.takeIf { m -> m.isNotBlank() }
                                                ?: "Verbesserung fehlgeschlagen"
                                    }
                                improving = false
                            }
                        },
                        enabled = !improving && text.isNotBlank(),
                    ) {
                        if (improving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = accent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Verbessere …", color = accent)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Mit KI verbessern", color = accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(
                        onClick = {
                            text = textBeforeImprovement.orEmpty()
                            textBeforeImprovement = null
                        },
                        enabled = !improving && textBeforeImprovement != null,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Rückgängig", color = accent)
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        color = LocalCosmos.current.crit,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text.trim(), title.trim().takeIf { it.isNotBlank() })
                    }
                },
                enabled = text.isNotBlank() && !improving,
            ) {
                Text("Speichern", color = accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !improving) {
                Text("Abbrechen", color = cosmos.textSecondary)
            }
        },
    )
}
