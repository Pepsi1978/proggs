package de.frank.denknotiz.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Die Anhangsarten des Plus-Menüs — eine je Eintrag im Menü. */
enum class AttachmentKind { PDF, VOICE, IMAGE, SCAN, AUDIO, DRAWING, STICKY, TABLE }

/**
 * Ein Anhang einer Notiz. Dateibasierte Anhänge (PDF, Bild, Scan, Audio, Zeichnung)
 * liegen unter files/anhaenge; textbasierte (Haftnotiz, Tabelle) stehen direkt in [text].
 */
data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val kind: AttachmentKind,
    val name: String,
    val path: String = "",
    val thumbPath: String = "",
    val text: String = "",
    val color: Int = 0,
    val durationMs: Long = 0L,
    val pages: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val file: File? get() = path.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::exists)
    val thumb: File? get() = thumbPath.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::exists)

    fun json(): JSONObject = JSONObject()
        .put("id", id).put("kind", kind.name).put("name", name).put("path", path)
        .put("thumbPath", thumbPath).put("text", text).put("color", color)
        .put("durationMs", durationMs).put("pages", pages).put("createdAt", createdAt)
}

fun List<Attachment>.toAttachmentJson(): String = JSONArray(map(Attachment::json)).toString()

fun attachmentsFromJson(raw: String): List<Attachment> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull(array::optJSONObject).mapNotNull { item ->
        val kind = runCatching { AttachmentKind.valueOf(item.optString("kind")) }.getOrNull() ?: return@mapNotNull null
        Attachment(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            kind = kind,
            name = item.optString("name"),
            path = item.optString("path"),
            thumbPath = item.optString("thumbPath"),
            text = item.optString("text"),
            color = item.optInt("color"),
            durationMs = item.optLong("durationMs"),
            pages = item.optInt("pages"),
            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
        )
    }
}.getOrDefault(emptyList())

/** Legt Anhangsdateien im App-eigenen Speicher ab und erzeugt Vorschauen. */
class AttachmentStore(private val context: Context) {

    val directory: File get() = File(context.filesDir, ORDNER).apply { mkdirs() }

    fun newFile(suffix: String): File = File(directory, "${UUID.randomUUID()}$suffix")

    /** Kopiert das gewählte Dokument in den App-Speicher, damit es dauerhaft verfügbar bleibt. */
    suspend fun importDocument(uri: Uri, kind: AttachmentKind): Attachment = withContext(Dispatchers.IO) {
        val display = displayName(uri)
        val suffix = "." + (display.substringAfterLast('.', "").ifBlank { defaultSuffix(kind) })
        val target = newFile(suffix)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use(input::copyTo)
        } ?: error("Die Datei ist nicht erreichbar.")
        describe(target, kind, display.ifBlank { defaultName(kind) })
    }

    /** Ergänzt eine fertige Datei um Vorschau, Seitenzahl beziehungsweise Laufzeit. */
    fun describe(file: File, kind: AttachmentKind, name: String): Attachment = when (kind) {
        AttachmentKind.PDF -> {
            val preview = renderPdfPreview(file)
            Attachment(kind = kind, name = name, path = file.absolutePath,
                thumbPath = preview.first.orEmpty(), pages = preview.second)
        }
        AttachmentKind.VOICE, AttachmentKind.AUDIO ->
            Attachment(kind = kind, name = name, path = file.absolutePath, durationMs = durationOf(file))
        else -> Attachment(kind = kind, name = name, path = file.absolutePath)
    }

    fun deleteAll(attachments: List<Attachment>) = attachments.forEach { attachment ->
        runCatching { attachment.file?.delete(); attachment.thumb?.delete() }
    }

    private fun displayName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty().substringAfterLast('/') }

    private fun defaultSuffix(kind: AttachmentKind): String = when (kind) {
        AttachmentKind.PDF -> "pdf"
        AttachmentKind.VOICE, AttachmentKind.AUDIO -> "m4a"
        else -> "jpg"
    }

    private fun defaultName(kind: AttachmentKind): String = when (kind) {
        AttachmentKind.PDF -> "Dokument.pdf"
        AttachmentKind.VOICE -> "Sprachaufnahme"
        AttachmentKind.AUDIO -> "Audiodatei"
        AttachmentKind.SCAN -> "Dokumentenscan"
        AttachmentKind.DRAWING -> "Zeichnung"
        else -> "Bild"
    }

    private fun durationOf(file: File): Long = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }.getOrDefault(0L)

    /** Rendert die erste PDF-Seite als Vorschaubild und liefert zusätzlich die Seitenzahl. */
    private fun renderPdfPreview(file: File): Pair<String?, Int> = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val pages = renderer.pageCount
                if (pages == 0) return@use null to 0
                renderer.openPage(0).use { page ->
                    val breite = VORSCHAU_BREITE
                    val hoehe = (breite.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(breite, hoehe, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val target = newFile("-vorschau.png")
                    FileOutputStream(target).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 92, out) }
                    bitmap.recycle()
                    target.absolutePath to pages
                }
            }
        }
    }.getOrNull() ?: (null to 0)

    private companion object {
        const val ORDNER = "anhaenge"
        const val VORSCHAU_BREITE = 900
    }
}
