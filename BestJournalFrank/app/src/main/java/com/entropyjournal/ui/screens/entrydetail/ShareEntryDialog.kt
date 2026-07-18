package com.entropyjournal.ui.screens.entrydetail

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.entropyjournal.ui.components.JournalShareAttachment
import com.entropyjournal.ui.components.JournalSharePayload
import com.entropyjournal.ui.components.JournalShareSheet
import com.entropyjournal.util.DateTimeFormatter
import java.io.File

@Composable
fun ShareEntryDialog(
    entry: JournalEntry,
    followUps: List<EntryFollowUpEntity>,
    photos: List<EntryPhotoEntity>,
    context: Context,
    useImproved: Boolean,
    onDismiss: () -> Unit,
) {
    val includeEntry = remember { androidx.compose.runtime.mutableStateOf(true) }
    val selectedFollowUps =
        remember(followUps.map { it.id }) { List(followUps.size) { true }.toMutableStateList() }
    val selectedPhotos =
        remember(photos.map { it.id }) { List(photos.size) { true }.toMutableStateList() }
    val includedFollowUps = followUps.filterIndexed { index, _ -> selectedFollowUps[index] }
    val selectedMedia = photos.filterIndexed { index, _ -> selectedPhotos[index] }
    val text =
        buildShareText(
            entry = entry,
            useImproved = useImproved,
            followUps = includedFollowUps,
            includeEntryBody = includeEntry.value,
        )
    val attachments =
        selectedMedia.map { media ->
            JournalShareAttachment(uri = getPhotoUri(context, media), isVideo = media.isVideo)
        }
    JournalShareSheet(
        payload =
            JournalSharePayload(
                title = entry.title?.takeIf { it.isNotBlank() } ?: "Tagebucheintrag",
                subtitle = "Tagebucheintrag · ${DateTimeFormatter.formatFull(entry.timestamp)}",
                text = text,
                attachments = attachments,
            ),
        onDismiss = onDismiss,
        selectionContent = {
            Text(
                "Inhalt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { includeEntry.value = !includeEntry.value },
            ) {
                Checkbox(checked = includeEntry.value, onCheckedChange = { includeEntry.value = it })
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
                    Text("Nachtrag ${index + 1}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
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
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        if (photo.isVideo) {
                            Icon(
                                Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp).align(Alignment.Center),
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
    append("\n────────────────────")
    if (includeEntryBody) {
        if (!entry.title.isNullOrBlank()) append("\n\n✨ ${entry.title}")
        val bodyText =
            if (useImproved && !entry.improvedText.isNullOrBlank()) entry.improvedText!!
            else entry.rawText.ifBlank { entry.displayText }
        append("\n\n$bodyText")
    }
    followUps.forEachIndexed { index, followUp ->
        append("\n\n")
        append(if (followUps.size == 1) "Nachtrag" else "Nachtrag ${index + 1}")
        append("\n${followUp.text}")
    }
}

fun getPhotoUri(context: Context, photo: EntryPhotoEntity): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(photo.filePath))
