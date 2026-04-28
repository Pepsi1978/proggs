package com.bestjournal.app.ui.screens.entrydetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.bestjournal.app.R
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
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
    followUps: List<EntryFollowUpEntity> = emptyList(),
) {
    // Frank-style auto-logic: if an improved version exists, always use it;
    // otherwise fall back to the original. No more RadioButtons — the user
    // asked us to drop the "Original vs. Verbessert" choice here.
    val hasImproved = entry.isImproved && !entry.improvedText.isNullOrBlank()
    var includeEntry by remember { mutableStateOf(true) }
    val selectedFollowUps = remember(followUps) { List(followUps.size) { true }.toMutableStateList() }
    val selectedPhotos = remember(photos) { List(photos.size) { true }.toMutableStateList() }

    val anySelected =
        includeEntry ||
            selectedFollowUps.any { it } ||
            selectedPhotos.any { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.share_title), color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Section header lets the user drop the main entry entirely
                // (e.g. share only a follow-up), and individually toggle each
                // follow-up.
                Text(
                    stringResource(R.string.share_what_to_share),
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
                    Text(
                        stringResource(R.string.share_journal_entry),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                            stringResource(
                                R.string.share_followup_numbered,
                                (index + 1).toString(),
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.fillMaxWidth())
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
                                if (photo.isVideo)
                                    "${stringResource(R.string.label_video)} ${index + 1}"
                                else "${stringResource(R.string.label_photo)} ${index + 1}",
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
                            context = context,
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

fun buildShareText(
    entry: JournalEntry,
    useImproved: Boolean,
    context: Context,
    followUps: List<EntryFollowUpEntity> = emptyList(),
    includeEntryBody: Boolean = true,
): String =
    buildString {
        append(context.getString(R.string.share_footer))
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
                append(
                    if (followUps.size == 1) {
                        context.getString(R.string.share_followup_single)
                    } else {
                        context.getString(
                            R.string.share_followup_numbered,
                            (index + 1).toString(),
                        )
                    }
                )
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
}
