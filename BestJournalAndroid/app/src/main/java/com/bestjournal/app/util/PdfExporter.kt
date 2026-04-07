package com.bestjournal.app.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bestjournal.app.data.local.entity.JournalEntryEntity
import java.io.OutputStream

/**
 * Generates a PDF document from journal entries using android.graphics.pdf.PdfDocument.
 * Each entry gets its own page(s) with date, title, summary, and full text.
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

    // Colors matching app palette
    private val COLOR_COPPER = Color.parseColor("#D36B00")
    private val COLOR_TEXT = Color.parseColor("#1A1A2E")
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#5A5A70")
    private val COLOR_DIVIDER = Color.parseColor("#E0DCD4")
    private val COLOR_SUMMARY_BG = Color.parseColor("#FFF5EB")
    private val COLOR_HEADER_BG = Color.parseColor("#F8F8FC")

    // Paints
    private fun brandPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER
        textSize = 22f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private fun titlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT
        textSize = 18f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private fun datePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER
        textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun summaryLabelPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER
        textSize = 11f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        letterSpacing = 0.08f
    }

    private fun summaryPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.ITALIC)
    }

    private fun bodyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT
        textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun footerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 9f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun dividerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_DIVIDER
        strokeWidth = 1f
    }

    /**
     * Generate a PDF from the given entries and write it to the output stream.
     * Returns the number of entries successfully written.
     */
    fun export(entries: List<JournalEntryEntity>, outputStream: OutputStream): Int {
        val document = PdfDocument()
        val totalPages = entries.size
        var pageNumber = 0

        for ((index, entry) in entries.withIndex()) {
            pageNumber++
            val pages = renderEntry(document, entry, pageNumber, totalPages, index + 1)
            pageNumber = pages
        }

        document.writeTo(outputStream)
        document.close()
        return entries.size
    }

    /**
     * Renders a single entry, potentially across multiple pages if text is long.
     * Returns the last page number used.
     */
    private fun renderEntry(
        document: PdfDocument,
        entry: JournalEntryEntity,
        startPageNum: Int,
        totalEntries: Int,
        entryIndex: Int,
    ): Int {
        val dateText = DateTimeFormatter.formatFull(entry.timestamp)
        val titleText = entry.title ?: "Eintrag #$entryIndex"
        val summaryText = entry.summary
        val bodyText = entry.displayText

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
        canvas.drawText("Best Journal", MARGIN_LEFT, currentY + 20f, brand)

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
        canvas.drawText("Eintrag $entryIndex von $totalEntries", MARGIN_LEFT, currentY, entryBadge)
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
            canvas.drawRect(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY + boxHeight, boxPaint)

            // Left accent bar
            val accentPaint = Paint().apply { color = COLOR_COPPER; strokeWidth = 3f }
            canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_LEFT, currentY + boxHeight, accentPaint)

            // "ZUSAMMENFASSUNG" label
            currentY += 16f
            canvas.drawText("ZUSAMMENFASSUNG", MARGIN_LEFT + 12f, currentY, summaryLabelPaint())
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
                drawFooter(canvas, currentPage)
                document.finishPage(page)

                // Start new page
                currentPage++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN_TOP

                // Continuation header
                val contP = datePaint().apply { color = COLOR_TEXT_SECONDARY }
                canvas.drawText("$titleText (Fortsetzung)", MARGIN_LEFT, currentY, contP)
                currentY += 20f
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
                currentY += 16f
            }

            if (line.isBlank()) {
                currentY += lineHeight * 0.5f
            } else {
                canvas.drawText(line, MARGIN_LEFT, currentY, bodyP)
                currentY += lineHeight
            }
        }

        // Draw footer and finish page
        drawFooter(canvas, currentPage)
        document.finishPage(page)

        return currentPage
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerY = PAGE_HEIGHT - 30f
        val footerP = footerPaint()

        // Divider
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, PAGE_WIDTH - MARGIN_RIGHT, footerY - 10f, dividerPaint())

        // Left: app name
        canvas.drawText("Best Journal", MARGIN_LEFT, footerY, footerP)

        // Right: page number
        val pageText = "Seite $pageNumber"
        val pageWidth = footerP.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_RIGHT - pageWidth, footerY, footerP)
    }

    /**
     * Wraps text into lines that fit within the given width.
     * Handles newlines in the original text and word-wrapping for long lines.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split('\n')

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }

            val words = paragraph.split(' ')
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = paint.measureText(testLine)

                if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }

        return result
    }
}
