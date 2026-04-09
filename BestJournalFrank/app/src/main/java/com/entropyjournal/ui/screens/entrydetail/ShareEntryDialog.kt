package com.entropyjournal.ui.screens.entrydetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.util.DateTimeFormatter
import java.io.File

@Composable
fun ShareEntryDialog(
    entry: JournalEntry,
    photos: List<EntryPhotoEntity>,
    context: Context,
    onDismiss: () -> Unit,
) {
    val hasImproved = entry.isImproved && !entry.improvedText.isNullOrBlank()
    var useImproved by remember { mutableStateOf(hasImproved) }
    val selectedPhotos = remember { List(photos.size) { true }.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag teilen", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasImproved) {
                    Text(
                        "Welche Version teilen?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useImproved = false },
                    ) {
                        RadioButton(selected = !useImproved, onClick = { useImproved = false })
                        Text("Original", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useImproved = true },
                    ) {
                        RadioButton(selected = useImproved, onClick = { useImproved = true })
                        Text("Verbesserte Version", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (photos.size > 1) {
                    Text(
                        "Fotos/Videos mitteilen:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    photos.forEachIndexed { index, photo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.fillMaxWidth().clickable {
                                    selectedPhotos[index] = !selectedPhotos[index]
                                },
                        ) {
                            Checkbox(
                                checked = selectedPhotos[index],
                                onCheckedChange = { selectedPhotos[index] = it },
                            )
                            AsyncImage(
                                model = photo.filePath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .padding(end = 8.dp),
                            )
                            Text(
                                if (photo.isVideo) "Video" else "Foto",
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val text = buildShareText(entry, useImproved && hasImproved)
                    val photoUris =
                        if (photos.size == 1) {
                            listOf(getPhotoUri(context, photos[0]))
                        } else {
                            photos
                                .filterIndexed { i, _ -> selectedPhotos[i] }
                                .map { getPhotoUri(context, it) }
                        }
                    executeShare(context, text, photoUris)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
            ) {
                Text("Teilen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

fun buildShareText(entry: JournalEntry, useImproved: Boolean): String = buildString {
    append(DateTimeFormatter.formatFull(entry.timestamp))
    if (!entry.title.isNullOrBlank()) append("\n\n${entry.title}")
    val bodyText = if (useImproved) entry.improvedText!! else entry.rawText
    append("\n\n$bodyText")
    if (!entry.moodTag.isNullOrBlank()) append("\n\nStimmung: ${entry.moodTag}")
}

fun getPhotoUri(context: Context, photo: EntryPhotoEntity): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(photo.filePath))

fun executeShare(context: Context, text: String, photoUris: List<Uri>) {
    val intent =
        if (photoUris.isEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
        } else if (photoUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, photoUris[0])
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photoUris))
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    context.startActivity(Intent.createChooser(intent, "Eintrag teilen"))
}
