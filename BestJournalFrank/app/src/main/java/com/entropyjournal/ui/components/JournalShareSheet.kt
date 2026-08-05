package com.entropyjournal.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.ui.theme.scaledBody
import java.io.File
import java.io.FileOutputStream

enum class JournalShareFormat(val label: String) {
    TEXT("Als Text"),
    IMAGE("Als Bild"),
    PDF("Als PDF"),
}

data class JournalShareAttachment(val uri: Uri, val isVideo: Boolean)

data class JournalSharePayload(
    val title: String,
    val subtitle: String,
    val text: String,
    val attachments: List<JournalShareAttachment> = emptyList(),
)

private enum class ShareTarget(val label: String) {
    WHATSAPP("WhatsApp"),
    EMAIL("E-Mail"),
    SAVE("Speichern"),
    COPY("Kopieren"),
    MORE("Mehr"),
}

private data class PreparedShare(val text: String, val file: File?, val mimeType: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalShareSheet(
    payload: JournalSharePayload,
    onDismiss: () -> Unit,
    selectionContent: @Composable ColumnScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val designTokens = LocalJournalDesignTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var format by remember { mutableStateOf(JournalShareFormat.TEXT) }
    var pendingSave by remember { mutableStateOf<File?>(null) }
    val canShare = payload.text.isNotBlank() || payload.attachments.isNotEmpty()

    fun saveResult(uri: Uri?) {
        val source = pendingSave
        if (uri != null && source != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Zieldatei konnte nicht geöffnet werden")
            }.onSuccess {
                Toast.makeText(context, "Gespeichert", Toast.LENGTH_SHORT).show()
                onDismiss()
            }.onFailure {
                Toast.makeText(context, "Speichern fehlgeschlagen", Toast.LENGTH_SHORT).show()
            }
        }
        pendingSave = null
    }

    val textSaver =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain"), ::saveResult)
    val imageSaver =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png"), ::saveResult)
    val pdfSaver =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), ::saveResult)

    fun handleTarget(target: ShareTarget) {
        if (!canShare) return
        val prepared = prepareShare(context, format, payload)
        when (target) {
            ShareTarget.SAVE -> {
                val file = prepared.file ?: createTextFile(context, payload)
                pendingSave = file
                when (format) {
                    JournalShareFormat.TEXT -> textSaver.launch(file.name)
                    JournalShareFormat.IMAGE -> imageSaver.launch(file.name)
                    JournalShareFormat.PDF -> pdfSaver.launch(file.name)
                }
            }
            ShareTarget.COPY -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip =
                    prepared.file?.let { file ->
                        ClipData.newUri(
                            context.contentResolver,
                            payload.title,
                            fileUri(context, file),
                        )
                    } ?: ClipData.newPlainText(payload.title, prepared.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "In die Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            else -> {
                val intent = createShareIntent(context, prepared, payload.attachments)
                try {
                    when (target) {
                        ShareTarget.WHATSAPP -> context.startActivity(intent.setPackage("com.whatsapp"))
                        ShareTarget.EMAIL ->
                            context.startActivity(Intent.createChooser(intent.setType("message/rfc822"), "Per E-Mail teilen"))
                        ShareTarget.MORE -> context.startActivity(Intent.createChooser(intent, "Teilen"))
                        else -> Unit
                    }
                    onDismiss()
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent.createChooser(createShareIntent(context, prepared, payload.attachments), "Teilen"))
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = designTokens.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp).size(width = 40.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = designTokens.cardBorder,
            ) {}
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Teilen",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Schließen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp).clickable(onClick = onDismiss),
                )
            }
            Text(
                payload.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp.scaledBody),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())
            ) {
                selectionContent()
            }
            NeonDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                horizontalPadding = 0.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JournalShareFormat.entries.forEach { item ->
                    val active = format == item
                    Surface(
                        onClick = { format = item },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        color =
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else designTokens.chipBackground,
                        border =
                            BorderStroke(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else designTokens.cardBorder,
                            ),
                        shadowElevation = if (active) 4.dp else 0.dp,
                    ) {
                        Text(
                            item.label,
                            modifier = Modifier.padding(vertical = 9.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
                            color =
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShareTarget.entries.forEach { target ->
                    ShareTargetButton(
                        target = target,
                        enabled = canShare,
                        onClick = { handleTarget(target) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareTargetButton(target: ShareTarget, enabled: Boolean, onClick: () -> Unit) {
    val designTokens = LocalJournalDesignTokens.current
    val icon =
        when (target) {
            ShareTarget.WHATSAPP -> Icons.AutoMirrored.Rounded.Chat
            ShareTarget.EMAIL -> Icons.Rounded.Mail
            ShareTarget.SAVE -> Icons.Rounded.PictureAsPdf
            ShareTarget.COPY -> Icons.Rounded.ContentCopy
            ShareTarget.MORE -> Icons.Rounded.MoreHoriz
        }
    Column(
        modifier = Modifier.width(60.dp).alpha(if (enabled) 1f else 0.4f).clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, designTokens.chipBorder),
        ) {
            Icon(
                icon,
                contentDescription = target.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(14.dp),
            )
        }
        Text(
            target.label,
            maxLines = 1,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun prepareShare(
    context: Context,
    format: JournalShareFormat,
    payload: JournalSharePayload,
): PreparedShare =
    when (format) {
        JournalShareFormat.TEXT -> PreparedShare(payload.text, null, "text/plain")
        JournalShareFormat.IMAGE -> PreparedShare(payload.text, createImageFile(context, payload), "image/png")
        JournalShareFormat.PDF -> PreparedShare(payload.text, createPdfFile(context, payload), "application/pdf")
    }

private fun createShareIntent(
    context: Context,
    prepared: PreparedShare,
    attachments: List<JournalShareAttachment>,
): Intent {
    val generated = prepared.file?.let { fileUri(context, it) }
    val uris = buildList {
        generated?.let(::add)
        attachments.forEach { add(it.uri) }
    }
    val mimeType =
        when {
            generated != null && attachments.isEmpty() -> prepared.mimeType
            generated == null && attachments.isNotEmpty() && attachments.all { it.isVideo } -> "video/*"
            generated == null && attachments.isNotEmpty() && attachments.none { it.isVideo } -> "image/*"
            uris.isNotEmpty() -> "*/*"
            else -> "text/plain"
        }
    return if (uris.size <= 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, prepared.text)
            uris.firstOrNull()?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("", uri)
            }
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, prepared.text)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData =
                ClipData.newRawUri("", uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
        }
    }
}

private fun createTextFile(context: Context, payload: JournalSharePayload): File =
    shareFile(context, payload, "txt").apply { writeText(payload.text, Charsets.UTF_8) }

private fun createImageFile(context: Context, payload: JournalSharePayload): File {
    val width = 1080
    val padding = 72
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(232, 181, 71)
        textSize = 52f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(239, 231, 219)
        textSize = 34f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val layout = textLayout(payload.text, bodyPaint, width - padding * 2)
    val height = (padding * 3 + 90 + layout.height).coerceIn(720, 4096)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(22, 17, 13))
    canvas.drawText(payload.title, padding.toFloat(), padding + 48f, titlePaint)
    canvas.drawRect(
        padding.toFloat(),
        padding + 76f,
        (width - padding).toFloat(),
        padding + 80f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(223, 116, 30) },
    )
    canvas.save()
    canvas.translate(padding.toFloat(), padding + 116f)
    layout.draw(canvas)
    canvas.restore()
    val file = shareFile(context, payload, "png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

private fun createPdfFile(context: Context, payload: JournalSharePayload): File {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 48
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(44, 38, 32)
        textSize = 13f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val fullLayout = textLayout(payload.text, bodyPaint, pageWidth - margin * 2)
    var startLine = 0
    var pageNumber = 1
    while (startLine < fullLayout.lineCount) {
        val startOffset = fullLayout.getLineStart(startLine)
        val startTop = fullLayout.getLineTop(startLine)
        var endLine = startLine
        while (
            endLine + 1 < fullLayout.lineCount &&
                fullLayout.getLineBottom(endLine + 1) - startTop <= pageHeight - 150
        ) {
            endLine++
        }
        val endOffset = fullLayout.getLineEnd(endLine)
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        val canvas = page.canvas
        canvas.drawColor(AndroidColor.rgb(251, 245, 234))
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(180, 83, 9)
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            canvas.drawText(payload.title, margin.toFloat(), 58f, this)
        }
        canvas.drawRect(
            margin.toFloat(),
            76f,
            (pageWidth - margin).toFloat(),
            78f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(232, 181, 71) },
        )
        canvas.save()
        canvas.translate(margin.toFloat(), 100f)
        textLayout(payload.text.substring(startOffset, endOffset), bodyPaint, pageWidth - margin * 2).draw(canvas)
        canvas.restore()
        document.finishPage(page)
        startLine = endLine + 1
        pageNumber++
    }
    val file = shareFile(context, payload, "pdf")
    FileOutputStream(file).use(document::writeTo)
    document.close()
    return file
}

private fun textLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setLineSpacing(4f, 1f)
        .build()

private fun shareFile(context: Context, payload: JournalSharePayload, extension: String): File {
    val directory = File(context.cacheDir, "share").apply { mkdirs() }
    val safeName = payload.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "entropy-journal" }
    return File(directory, "$safeName-${System.currentTimeMillis()}.$extension")
}

private fun fileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
