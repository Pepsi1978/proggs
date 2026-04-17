package com.bestjournal.app.ui.screens.entrydetail

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.bestjournal.app.R
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.domain.model.JournalEntry
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.DateTimeFormatter
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
        title = {
            Text(stringResource(R.string.share_title), color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasImproved) {
                    Text(
                        stringResource(R.string.share_which_version),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useImproved = false },
                    ) {
                        RadioButton(selected = !useImproved, onClick = { useImproved = false })
                        Text(
                            stringResource(R.string.label_original),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useImproved = true },
                    ) {
                        RadioButton(selected = useImproved, onClick = { useImproved = true })
                        Text(
                            stringResource(R.string.share_improved_version),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (photos.size > 1) {
                    Text(
                        stringResource(R.string.share_photos_videos_label),
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
                                if (photo.isVideo) stringResource(R.string.label_video)
                                else stringResource(R.string.label_photo),
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
                    val text = buildShareText(entry, useImproved && hasImproved, context)
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
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

fun buildShareText(entry: JournalEntry, useImproved: Boolean, context: Context): String =
    buildString {
        append(context.getString(R.string.share_footer))
        append("\n")
        append(DateTimeFormatter.formatFull(entry.timestamp))
        if (!entry.moodTag.isNullOrBlank()) append(" \u00b7 ${entry.moodTag}")
        append(
            "\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
        )
        if (!entry.title.isNullOrBlank()) append("\n\n\u2728 ${entry.title}")
        val bodyText =
            if (useImproved) entry.improvedText ?: entry.displayText else entry.displayText
        append("\n\n$bodyText")
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
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("", photoUris[0])
                clipData?.addItem(android.content.ClipData.Item(photoUris[0]))
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photoUris))
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData =
                    android.content.ClipData.newRawUri("", photoUris[0]).apply {
                        photoUris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
                    }
            }
        }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
}
