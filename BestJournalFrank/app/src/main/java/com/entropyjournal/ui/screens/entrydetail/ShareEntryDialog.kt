package com.entropyjournal.ui.screens.entrydetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.entropyjournal.data.local.entity.EntryFollowUpEntity
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.util.DateTimeFormatter
import java.io.File

@Composable
fun ShareEntryDialog(
    entry: JournalEntry,
    followUps: List<EntryFollowUpEntity>,
    photos: List<EntryPhotoEntity>,
    context: Context,
    onDismiss: () -> Unit,
) {
    val designTokens = LocalJournalDesignTokens.current
    val hasImproved = entry.isImproved && !entry.improvedText.isNullOrBlank()
    var includeEntry by remember { mutableStateOf(true) }
    val selectedFollowUps = remember { List(followUps.size) { true }.toMutableStateList() }
    val selectedPhotos = remember { List(photos.size) { true }.toMutableStateList() }

    val anySelected =
        includeEntry ||
            selectedFollowUps.any { it } ||
            selectedPhotos.any { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier =
            Modifier.border(
                1.dp,
                designTokens.chipBorder,
                RoundedCornerShape(22.dp),
            ),
        shape = RoundedCornerShape(22.dp),
        containerColor = designTokens.card,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Eintrag teilen",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Was möchtest du teilen?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeEntry = !includeEntry },
                ) {
                    Checkbox(
                        checked = includeEntry,
                        onCheckedChange = { includeEntry = it },
                    )
                    Text("Tagebucheintrag", color = MaterialTheme.colorScheme.onSurface)
                }

                followUps.forEachIndexed { index, _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                selectedFollowUps[index] = !selectedFollowUps[index]
                            },
                    ) {
                        Checkbox(
                            checked = selectedFollowUps[index],
                            onCheckedChange = { selectedFollowUps[index] = it },
                        )
                        Text(
                            "Nachtrag ${index + 1}",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Fotos und Videos",
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
                            Box {
                                AsyncImage(
                                    model = photo.filePath,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                                )
                                if (photo.isVideo) {
                                    Icon(
                                        Icons.Rounded.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier =
                                            Modifier.size(20.dp).align(Alignment.Center),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (photo.isVideo) "Video ${index + 1}" else "Foto ${index + 1}",
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
                    val includedFollowUps =
                        followUps.filterIndexed { i, _ -> selectedFollowUps[i] }
                    val text =
                        buildShareText(
                            entry = entry,
                            useImproved = hasImproved,
                            followUps = includedFollowUps,
                            includeEntryBody = includeEntry,
                        )
                    val photoUris =
                        photos.filterIndexed { i, _ -> selectedPhotos[i] }
                            .map { getPhotoUri(context, it) }
                    executeShare(context, text, photoUris)
                    onDismiss()
                },
                enabled = anySelected,
                shape = RoundedCornerShape(999.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text("Teilen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(999.dp)) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

fun buildShareText(
    entry: JournalEntry,
    useImproved: Boolean,
    followUps: List<EntryFollowUpEntity> = emptyList(),
    includeEntryBody: Boolean = true,
): String = buildString {
    append("Tagebucheintrag von der BestJournal App")
    append("\n")
    append(DateTimeFormatter.formatFull(entry.timestamp))
    if (!entry.moodTag.isNullOrBlank()) append(" · ${entry.moodTag}")
    append(
        "\n────────────────────"
    )
    if (includeEntryBody) {
        if (!entry.title.isNullOrBlank()) append("\n\n✨ ${entry.title}")
        val bodyText =
            if (useImproved && !entry.improvedText.isNullOrBlank()) entry.improvedText!!
            else entry.displayText
        append("\n\n$bodyText")
    }
    if (followUps.isNotEmpty()) {
        followUps.forEachIndexed { index, followUp ->
            append("\n\n")
            append(if (followUps.size == 1) "Nachtrag" else "Nachtrag ${index + 1}")
            append("\n${followUp.text}")
        }
    }
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
    context.startActivity(Intent.createChooser(intent, "Eintrag teilen"))
}
