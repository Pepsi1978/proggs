package com.entropyjournal.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.entropyjournal.data.local.entity.JournalEntryEntity
import java.io.OutputStream

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 50f
    private const val MARGIN_RIGHT = 50f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 60f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private val COLOR_COPPER = Color.parseColor("#D36B00")
    private val COLOR_TEXT = Color.parseColor("#1A1A2E")
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#5A5A70")
    private val COLOR_DIVIDER = Color.parseColor("#E0DCD4")
    private val COLOR_SUMMARY_BG = Color.parseColor("#FFF5EB")
    private val COLOR_HEADER_BG = Color.parseColor("#F8F8FC")

    private fun brandPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER; textSize = 22f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private fun titlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT; textSize = 18f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private fun datePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER; textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private fun summaryLabelPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_COPPER; textSize = 11f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); letterSpacing = 0.08f
    }
    private fun summaryPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY; textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.ITALIC)
    }
    private fun bodyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT; textSize = 12f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private fun footerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY; textSize = 9f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private fun dividerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_DIVIDER; strokeWidth = 1f
    }

    fun export(entries: List<JournalEntryEntity>, outputStream: OutputStream): Int {
        val document = PdfDocument()
        var pageNumber = 0
        for ((index, entry) in entries.withIndex()) {
            pageNumber++
            pageNumber = renderEntry(document, entry, pageNumber, entries.size, index + 1)
        }
        document.writeTo(outputStream)
        document.close()
        return entries.size
    }

    private fun renderEntry(
        document: PdfDocument, entry: JournalEntryEntity,
        startPageNum: Int, totalEntries: Int, entryIndex: Int,
    ): Int {
        val dateText = DateTimeFormatter.formatFull(entry.timestamp)
        val titleText = entry.title ?: "Eintrag #$entryIndex"
        val summaryText = entry.summary
        val bodyText = entry.displayText
        val bodyLines = wrapText(bodyText, bodyPaint(), CONTENT_WIDTH)
        val titleLines = wrapText(titleText, titlePaint(), CONTENT_WIDTH)

        var currentPage = startPageNum
        var currentY = MARGIN_TOP
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val headerPaint = Paint().apply { color = COLOR_HEADER_BG }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), MARGIN_TOP + 70f, headerPaint)
        canvas.drawText("Entropy Journal", MARGIN_LEFT, currentY + 20f, brandPaint())
        val dateP = datePaint()
        canvas.drawText(dateText, PAGE_WIDTH - MARGIN_RIGHT - dateP.measureText(dateText), currentY + 20f, dateP)

        currentY += 36f
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
        currentY += 20f

        canvas.drawText("Eintrag $entryIndex von $totalEntries", MARGIN_LEFT, currentY, datePaint().apply { color = COLOR_TEXT_SECONDARY })
        currentY += 20f

        for (line in titleLines) { canvas.drawText(line, MARGIN_LEFT, currentY, titlePaint()); currentY += 24f }
        currentY += 4f

        if (!summaryText.isNullOrBlank()) {
            currentY += 4f
            val summaryLines = wrapText(summaryText, summaryPaint(), CONTENT_WIDTH - 24f)
            val boxHeight = 12f + summaryLines.size * 16f + 12f + 16f
            canvas.drawRect(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY + boxHeight, Paint().apply { color = COLOR_SUMMARY_BG })
            canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_LEFT, currentY + boxHeight, Paint().apply { color = COLOR_COPPER; strokeWidth = 3f })
            currentY += 16f
            canvas.drawText("ZUSAMMENFASSUNG", MARGIN_LEFT + 12f, currentY, summaryLabelPaint())
            currentY += 14f
            for (line in summaryLines) { canvas.drawText(line, MARGIN_LEFT + 12f, currentY, summaryPaint()); currentY += 16f }
            currentY += 12f
        }

        currentY += 8f
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
        currentY += 16f

        val bodyP = bodyPaint()
        val lineHeight = 17f
        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM

        for (line in bodyLines) {
            if (currentY + lineHeight > maxY) {
                drawFooter(canvas, currentPage)
                document.finishPage(page)
                currentPage++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN_TOP
                canvas.drawText("$titleText (Fortsetzung)", MARGIN_LEFT, currentY, datePaint().apply { color = COLOR_TEXT_SECONDARY })
                currentY += 20f
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint())
                currentY += 16f
            }
            if (line.isBlank()) { currentY += lineHeight * 0.5f } else { canvas.drawText(line, MARGIN_LEFT, currentY, bodyP); currentY += lineHeight }
        }

        drawFooter(canvas, currentPage)
        document.finishPage(page)
        return currentPage
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerY = PAGE_HEIGHT - 30f
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, PAGE_WIDTH - MARGIN_RIGHT, footerY - 10f, dividerPaint())
        canvas.drawText("Entropy Journal", MARGIN_LEFT, footerY, footerPaint())
        val pageText = "Seite $pageNumber"
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_RIGHT - footerPaint().measureText(pageText), footerY, footerPaint())
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            if (paragraph.isBlank()) { result.add(""); continue }
            val words = paragraph.split(' ')
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }
            if (currentLine.isNotEmpty()) result.add(currentLine.toString())
        }
        return result
    }
}
