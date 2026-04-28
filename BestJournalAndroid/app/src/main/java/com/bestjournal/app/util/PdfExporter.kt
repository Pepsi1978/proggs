package com.bestjournal.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
import android.media.ExifInterface
import com.bestjournal.app.R
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.data.local.entity.JournalEntryEntity
import java.io.File
import java.io.OutputStream

/**
 * Generates a PDF document from journal entries using android.graphics.pdf.PdfDocument. Each entry
 * gets its own page(s) with date, title, summary, full text, and optional photos.
 */
object PdfExporter {

    // A4 dimensions in PostScript points (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Margins
    private const val MARGIN_LEFT = 50f
    private const val MARGIN_RIGHT = 50f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 60f

    // Content area
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    // Photo spacing
    private const val PHOTO_SPACING = 8f

    // Colors matching app palette
    private val COLOR_COPPER = Color.parseColor("#D36B00")
    private val COLOR_TEXT = Color.parseColor("#1A1A2E")
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#5A5A70")
    private val COLOR_DIVIDER = Color.parseColor("#E0DCD4")
    private val COLOR_SUMMARY_BG = Color.parseColor("#FFF5EB")
    private val COLOR_HEADER_BG = Color.parseColor("#F8F8FC")

    // Paints
    private fun brandPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_COPPER
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

    private fun titlePaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

    private fun datePaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_COPPER
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

    private fun summaryLabelPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_COPPER
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
        }

    private fun summaryPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
        }

    private fun bodyPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

    private fun footerPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 9f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

    private fun dividerPaint() =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }

    /**
     * Generate a PDF from the given entries and write it to the output stream.
     *
     * @param photosPerEntry map of entryId to list of photos (only non-video). Pass empty map to
     *   skip photos. Returns the number of entries successfully written.
     */
    fun export(
        entries: List<JournalEntryEntity>,
        outputStream: OutputStream,
        photosPerEntry: Map<Long, List<EntryPhotoEntity>> = emptyMap(),
        context: Context,
        followUpsPerEntry: Map<Long, List<EntryFollowUpEntity>> = emptyMap(),
    ): Int {
        val document = PdfDocument()
        try {
            var pageNumber = 0

            for ((index, entry) in entries.withIndex()) {
                pageNumber++
                val photos = photosPerEntry[entry.id] ?: emptyList()
                val followUps = followUpsPerEntry[entry.id] ?: emptyList()
                val pages =
                    renderEntry(
                        context,
                        document,
                        entry,
                        pageNumber,
                        entries.size,
                        index + 1,
                        photos,
                        followUps,
                    )
                pageNumber = pages
            }

            document.writeTo(outputStream)
            return entries.size
        } finally {
            // Close PdfDocument in a finally block so the native resources are
            // released even if writeTo or renderEntry throws (OOM, IO exception).
            document.close()
        }
    }

    /**
     * Renders a single entry, potentially across multiple pages if text is long. Photos are
     * rendered below the text, each scaled to page width. Returns the last page number used.
     */
    private fun renderEntry(
        context: Context,
        document: PdfDocument,
        entry: JournalEntryEntity,
        startPageNum: Int,
        totalEntries: Int,
        entryIndex: Int,
        photos: List<EntryPhotoEntity>,
        followUps: List<EntryFollowUpEntity> = emptyList(),
    ): Int {
        val dateText = DateTimeFormatter.formatFull(entry.timestamp)
        val titleText =
            entry.title ?: context.getString(R.string.pdf_entry_fallback_title, entryIndex)
        val summaryText = entry.summary
        // Body = main entry text plus each follow-up labeled "Nachtrag <n>".
        // Falls back to the denormalised followUpText cache column when the
        // caller didn't pass the structured follow-up list (e.g. an old
        // Drive backup restore path that only has the cache).
        val bodyText =
            buildString {
                append(entry.displayText)
                val pdfFollowUps =
                    if (followUps.isNotEmpty()) {
                        followUps
                    } else if (!entry.followUpText.isNullOrBlank()) {
                        listOf(
                            EntryFollowUpEntity(
                                entryId = entry.id,
                                text = entry.followUpText,
                                createdAt = entry.timestamp,
                                updatedAt = entry.timestamp,
                            )
                        )
                    } else {
                        emptyList()
                    }
                pdfFollowUps.forEachIndexed { index, followUp ->
                    append("\n\n")
                    append(
                        if (pdfFollowUps.size == 1) {
                            context.getString(R.string.share_followup_single)
                        } else {
                            context.getString(
                                R.string.share_followup_numbered,
                                (index + 1).toString(),
                            )
                        }
                    )
                    append("\n")
                    append(followUp.text)
                }
            }

        // Prepare text lines for body
        val bodyLines = wrapText(bodyText, bodyPaint(), CONTENT_WIDTH)

        // Calculate how much space we need for header section
        val titleLines = wrapText(titleText, titlePaint(), CONTENT_WIDTH)

        var currentPage = startPageNum
        var currentY = MARGIN_TOP

        // Create first page
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Draw header background
        val headerPaint = Paint().apply { color = COLOR_HEADER_BG }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), MARGIN_TOP + 70f, headerPaint)

        // Brand name
        val brand = brandPaint()
        canvas.drawText(context.getString(R.string.app_name), MARGIN_LEFT, currentY + 20f, brand)

        // Date on the right side
        val dateP = datePaint()
        val dateWidth = dateP.measureText(dateText)
        canvas.drawText(dateText, PAGE_WIDTH - MARGIN_RIGHT - dateWidth, currentY + 20f, dateP)

        // Divider below header
        currentY += 36f
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
        currentY += 20f

        // Entry number badge
        val entryBadge = datePaint().apply { color = COLOR_TEXT_SECONDARY }
        canvas.drawText(
            context.getString(R.string.pdf_entry_counter, entryIndex, totalEntries),
            MARGIN_LEFT,
            currentY,
            entryBadge,
        )
        currentY += 20f

        // Title
        val titleP = titlePaint()
        for (line in titleLines) {
            canvas.drawText(line, MARGIN_LEFT, currentY, titleP)
            currentY += 24f
        }
        currentY += 4f

        // Summary (if available)
        if (!summaryText.isNullOrBlank()) {
            currentY += 4f

            // Summary box background
            val summaryLines = wrapText(summaryText, summaryPaint(), CONTENT_WIDTH - 24f)
            val boxHeight = 12f + summaryLines.size * 16f + 12f + 16f
            val boxPaint = Paint().apply { color = COLOR_SUMMARY_BG }
            canvas.drawRect(
                MARGIN_LEFT,
                currentY,
                PAGE_WIDTH - MARGIN_RIGHT,
                currentY + boxHeight,
                boxPaint,
            )

            // Left accent bar
            val accentPaint =
                Paint().apply {
                    color = COLOR_COPPER
                    strokeWidth = 3f
                }
            canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_LEFT, currentY + boxHeight, accentPaint)

            // Summary label
            currentY += 16f
            canvas.drawText(
                context.getString(R.string.pdf_summary_label),
                MARGIN_LEFT + 12f,
                currentY,
                summaryLabelPaint(),
            )
            currentY += 14f

            // Summary text
            val sumP = summaryPaint()
            for (line in summaryLines) {
                canvas.drawText(line, MARGIN_LEFT + 12f, currentY, sumP)
                currentY += 16f
            }
            currentY += 12f
        }

        // Divider before body
        currentY += 8f
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
        currentY += 16f

        // Body text
        val bodyP = bodyPaint()
        val lineHeight = 17f
        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM

        for (line in bodyLines) {
            if (currentY + lineHeight > maxY) {
                // Draw footer on current page
                drawFooter(context, canvas, currentPage)
                document.finishPage(page)

                // Start new page
                currentPage++
                pageInfo =
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN_TOP

                // Continuation header
                val contP = datePaint().apply { color = COLOR_TEXT_SECONDARY }
                canvas.drawText(
                    context.getString(R.string.pdf_continuation, titleText),
                    MARGIN_LEFT,
                    currentY,
                    contP,
                )
                currentY += 20f
                canvas.drawLine(
                    MARGIN_LEFT,
                    currentY,
                    PAGE_WIDTH - MARGIN_RIGHT,
                    currentY,
                    dividerPaint(),
                )
                currentY += 16f
            }

            if (line.isBlank()) {
                currentY += lineHeight * 0.5f
            } else {
                canvas.drawText(line, MARGIN_LEFT, currentY, bodyP)
                currentY += lineHeight
            }
        }

        // Draw photos below text
        if (photos.isNotEmpty()) {
            currentY += PHOTO_SPACING * 2

            for (photo in photos) {
                val bitmap = loadAndScaleBitmap(photo.filePath) ?: continue
                val scaledHeight =
                    (bitmap.height.toFloat() / bitmap.width.toFloat()) * CONTENT_WIDTH

                // Check if photo fits on current page
                if (currentY + scaledHeight > maxY) {
                    drawFooter(context, canvas, currentPage)
                    document.finishPage(page)

                    currentPage++
                    pageInfo =
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN_TOP
                }

                // Draw the photo scaled to content width
                val destRect =
                    Rect(
                        MARGIN_LEFT.toInt(),
                        currentY.toInt(),
                        (MARGIN_LEFT + CONTENT_WIDTH).toInt(),
                        (currentY + scaledHeight).toInt(),
                    )
                canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                bitmap.recycle()

                currentY += scaledHeight + PHOTO_SPACING
            }
        }

        // Draw footer and finish page
        drawFooter(context, canvas, currentPage)
        document.finishPage(page)

        return currentPage
    }

    /**
     * Load a bitmap from file, apply EXIF rotation, and scale it down to fit page width. Returns
     * null if the file doesn't exist or can't be decoded.
     */
    private fun loadAndScaleBitmap(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        // First, decode bounds only to calculate scaling factor
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        // Calculate sample size to avoid loading huge images into memory
        val targetWidth = CONTENT_WIDTH.toInt() * 2 // 2x for quality
        options.inSampleSize =
            calculateInSampleSize(options.outWidth, options.outHeight, targetWidth)
        options.inJustDecodeBounds = false

        val rawBitmap =
            try {
                BitmapFactory.decodeFile(filePath, options)
            } catch (_: OutOfMemoryError) {
                options.inSampleSize *= 2
                try {
                    BitmapFactory.decodeFile(filePath, options)
                } catch (_: OutOfMemoryError) {
                    null
                }
            } ?: return null

        return applyExifRotation(rawBitmap, filePath)
    }

    /**
     * Read EXIF orientation from photo and rotate bitmap accordingly. Phone cameras store portrait
     * photos as landscape with an EXIF rotation tag.
     */
    private fun applyExifRotation(bitmap: Bitmap, filePath: String): Bitmap {
        val rotation =
            try {
                val exif = ExifInterface(filePath)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } catch (_: Exception) {
                0f
            }

        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int): Int {
        var inSampleSize = 1
        if (width > reqWidth) {
            val halfWidth = width / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun drawFooter(context: Context, canvas: Canvas, pageNumber: Int) {
        val footerY = PAGE_HEIGHT - 30f
        val footerP = footerPaint()

        // Divider
        canvas.drawLine(
            MARGIN_LEFT,
            footerY - 10f,
            PAGE_WIDTH - MARGIN_RIGHT,
            footerY - 10f,
            dividerPaint(),
        )

        // Left: app name
        canvas.drawText(context.getString(R.string.app_name), MARGIN_LEFT, footerY, footerP)

        // Right: page number
        val pageText = context.getString(R.string.pdf_page_number, pageNumber)
        val pageWidth = footerP.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_RIGHT - pageWidth, footerY, footerP)
    }

    /**
     * Wraps text into lines that fit within the given width. Handles newlines in the original text
     * and word-wrapping for long lines. Uses Paint.breakText() for correct wrapping of ALL scripts
     * including CJK (Chinese, Japanese, Korean) where words are not separated by spaces.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split('\n')

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }

            var offset = 0
            while (offset < paragraph.length) {
                val measured =
                    paint.breakText(paragraph, offset, paragraph.length, true, maxWidth, null)
                if (measured == 0) {
                    offset++
                    continue
                }
                var end = offset + measured
                // If we didn't consume the whole paragraph, try to break at a word boundary
                if (end < paragraph.length && paragraph[end] != ' ') {
                    val lastSpace = paragraph.lastIndexOf(' ', end - 1)
                    if (lastSpace > offset) {
                        end = lastSpace
                    }
                }
                result.add(paragraph.substring(offset, end).trim())
                offset = end
                // Skip the space at the break point
                if (offset < paragraph.length && paragraph[offset] == ' ') offset++
            }
        }

        return result
    }
}
