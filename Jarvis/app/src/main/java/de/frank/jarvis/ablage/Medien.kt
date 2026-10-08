package de.frank.jarvis.ablage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

/**
 * Bildarbeit auf dem Gerät: Vorschaubilder (Bild, Video-Standbild, erste PDF-Seite), PDF-Seiten für den Betrachter
 * und das Erzeugen einfacher PDFs (Text als DIN-A4-Dokument, Bild auf eine DIN-A4-Seite). Große Bilder werden nie
 * in voller Auflösung in den Speicher geladen, sondern beim Dekodieren verkleinert.
 */
object Medien {
    /** Liest nur die Maße, ohne das Bild zu laden. */
    fun bildMasse(datei: File): Pair<Int, Int>? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(datei.absolutePath, o)
        return if (o.outWidth > 0 && o.outHeight > 0) o.outWidth to o.outHeight else null
    }

    /** Dekodiert ein Bild so verkleinert, dass die längere Seite höchstens etwa [laengste] Pixel hat. */
    fun bildVerkleinert(datei: File, laengste: Int): Bitmap? {
        val (b, h) = bildMasse(datei) ?: return null
        var faktor = 1
        while (maxOf(b, h) / (faktor * 2) >= laengste) faktor *= 2
        return BitmapFactory.decodeFile(datei.absolutePath, BitmapFactory.Options().apply { inSampleSize = faktor })
    }

    /**
     * Erzeugt das Vorschaubild eines Anhangs als JPEG (längste Seite 360 px) und gibt es zurück; null, wenn der Typ
     * keines hat oder die Datei sich nicht lesen lässt. Das Original bleibt unberührt.
     */
    fun vorschaubild(quelle: File, art: Art, ziel: File): File? {
        if (ziel.isFile && ziel.length() > 0 && ziel.lastModified() >= quelle.lastModified()) return ziel
        val bild: Bitmap = runCatching {
            when (art) {
                Art.BILD, Art.ANIMATION -> bildVerkleinert(quelle, 360)
                Art.VIDEO -> MediaMetadataRetriever().let { r ->
                    try {
                        r.setDataSource(quelle.absolutePath)
                        if (Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 360, 360)
                        else r.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } finally { runCatching { r.release() } }
                }
                Art.PDF -> pdfSeite(quelle, 0, 360)?.second
                else -> null
            }
        }.getOrNull() ?: return null
        val klein = if (maxOf(bild.width, bild.height) > 400) {
            val f = 360f / maxOf(bild.width, bild.height)
            Bitmap.createScaledBitmap(bild, (bild.width * f).toInt().coerceAtLeast(1), (bild.height * f).toInt().coerceAtLeast(1), true).also { if (it !== bild) bild.recycle() }
        } else bild
        return runCatching {
            val tmp = File(ziel.parentFile, ziel.name + ".tmp")
            FileOutputStream(tmp).use { klein.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            tmp.renameTo(ziel)
            ziel
        }.getOrNull().also { klein.recycle() }
    }

    class PdfFehler(meldung: String) : Exception(meldung)

    /** Seitenzahl eines PDFs; wirft [PdfFehler] mit verständlichem Text bei Passwortschutz oder Beschädigung. */
    fun pdfSeiten(datei: File): Int = mitPdf(datei) { it.pageCount }

    /**
     * Rendert eine PDF-Seite, die längere Seite höchstens [laengste] Pixel. Rückgabe: (Seitenzahl gesamt, Bild).
     * Weißer Hintergrund, weil PDFs ohne Hintergrund sonst im dunklen Design schwarz auf schwarz erscheinen.
     */
    fun pdfSeite(datei: File, index: Int, laengste: Int): Pair<Int, Bitmap>? = mitPdf(datei) { pdf ->
        if (pdf.pageCount == 0) return@mitPdf null
        pdf.openPage(index.coerceIn(0, pdf.pageCount - 1)).use { seite ->
            val f = laengste.toFloat() / maxOf(seite.width, seite.height)
            val bmp = Bitmap.createBitmap((seite.width * f).toInt().coerceAtLeast(1), (seite.height * f).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            seite.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdf.pageCount to bmp
        }
    }

    /** Rendert eine Seite in eine PNG-Datei (für den zoombaren Betrachter, der große Bilder in Kacheln lädt). */
    fun pdfSeiteAlsDatei(datei: File, index: Int, laengste: Int, ziel: File): File? {
        if (ziel.isFile && ziel.length() > 0 && ziel.lastModified() >= datei.lastModified()) return ziel
        val (_, bmp) = pdfSeite(datei, index, laengste) ?: return null
        return try {
            ziel.parentFile?.mkdirs()
            val tmp = File(ziel.parentFile, ziel.name + ".tmp")
            FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            tmp.renameTo(ziel)
            ziel
        } finally { bmp.recycle() }
    }

    private fun <T> mitPdf(datei: File, block: (PdfRenderer) -> T): T {
        val fd = try { ParcelFileDescriptor.open(datei, ParcelFileDescriptor.MODE_READ_ONLY) } catch (e: Exception) { throw PdfFehler("Die PDF-Datei lässt sich nicht öffnen.") }
        val pdf = try {
            PdfRenderer(fd)
        } catch (e: SecurityException) {
            fd.close(); throw PdfFehler("Diese PDF-Datei ist mit einem Passwort geschützt. Die eingebaute Vorschau kann sie nicht öffnen; bitte mit einer anderen App öffnen.")
        } catch (e: Exception) {
            fd.close(); throw PdfFehler("Diese PDF-Datei ist beschädigt oder unvollständig und lässt sich nicht anzeigen.")
        }
        return try { pdf.use(block) } finally { runCatching { fd.close() } }
    }

    // ---- PDF erzeugen ----

    private const val A4_B = 595
    private const val A4_H = 842

    /**
     * Setzt einen Text als mehrseitiges DIN-A4-PDF (Rand 2 cm, Überschrift, Seitenzahlen). Einfache Markdown-Zeichen
     * (#, **, -) werden in lesbare Form gebracht. Für „Recherche als PDF“.
     */
    fun textAlsPdf(titel: String, text: String, ziel: File) {
        val doc = PdfDocument()
        try {
            val rand = 57f
            val breite = (A4_B - 2 * rand).toInt()
            val koerper = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; color = Color.BLACK }
            val kopf = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 17f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD }
            val fuss = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.GRAY }
            val absaetze = text.replace("\r", "").lines().map { z ->
                val t = z.trimEnd()
                when {
                    t.startsWith("#") -> "\u0001" + t.trimStart('#').trim()
                    t.trimStart().startsWith("- ") || t.trimStart().startsWith("* ") -> "  • " + t.trimStart().drop(2)
                    else -> t
                }.replace("**", "").replace("__", "")
            }
            var nr = 0
            var seite: PdfDocument.Page? = null
            var y = 0f
            fun neueSeite() {
                seite?.let { s -> s.canvas.drawText("Seite $nr", A4_B - rand - fuss.measureText("Seite $nr"), A4_H - 30f, fuss); doc.finishPage(s) }
                nr++
                seite = doc.startPage(PdfDocument.PageInfo.Builder(A4_B, A4_H, nr).create())
                y = rand
            }
            fun setze(t: String, farbe: TextPaint, abstandNach: Float) {
                val layout = statisch(t.ifEmpty { " " }, farbe, breite)
                var zeile = 0
                while (zeile < layout.lineCount) {
                    if (seite == null || y + (layout.getLineBottom(zeile) - layout.getLineTop(zeile)) > A4_H - rand) neueSeite()
                    // Zeile für Zeile, damit lange Absätze sauber über Seitengrenzen laufen.
                    val start = layout.getLineStart(zeile)
                    val ende = layout.getLineEnd(zeile)
                    seite!!.canvas.drawText(t, start, ende, rand, y - layout.getLineAscent(zeile), farbe)
                    y += layout.getLineBottom(zeile) - layout.getLineTop(zeile)
                    zeile++
                }
                y += abstandNach
            }
            neueSeite()
            setze(titel, kopf, 12f)
            absaetze.forEach { z -> if (z.startsWith("\u0001")) { y += 6f; setze(z.drop(1), kopf.apply { textSize = 13f }, 4f); kopf.textSize = 17f } else setze(z, koerper, 3f) }
            seite?.let { s -> s.canvas.drawText("Seite $nr", A4_B - rand - fuss.measureText("Seite $nr"), A4_H - 30f, fuss); doc.finishPage(s) }
            FileOutputStream(ziel).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    /** Legt ein Bild (zum Beispiel eine Infografik) eingepasst auf eine DIN-A4-Seite, Hoch- oder Querformat nach dem Bild. */
    fun bildAlsA4Pdf(bild: File, ziel: File) {
        val (b, h) = bildMasse(bild) ?: throw PdfFehler("Das Bild lässt sich nicht lesen.")
        val hoch = h >= b
        val sb = if (hoch) A4_B else A4_H
        val sh = if (hoch) A4_H else A4_B
        // In 300 dpi Druckqualität einbetten, aber nie größer als das Original.
        val bmp = bildVerkleinert(bild, (maxOf(sb, sh) * 300 / 72)) ?: throw PdfFehler("Das Bild lässt sich nicht lesen.")
        val doc = PdfDocument()
        try {
            val seite = doc.startPage(PdfDocument.PageInfo.Builder(sb, sh, 1).create())
            val f = minOf(sb.toFloat() / bmp.width, sh.toFloat() / bmp.height)
            val w = bmp.width * f
            val hh = bmp.height * f
            val ziel2 = RectF((sb - w) / 2, (sh - hh) / 2, (sb + w) / 2, (sh + hh) / 2)
            seite.canvas.drawColor(Color.WHITE)
            seite.canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), ziel2, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            doc.finishPage(seite)
            FileOutputStream(ziel).use { doc.writeTo(it) }
        } finally {
            doc.close()
            bmp.recycle()
        }
    }

    private fun statisch(text: String, farbe: TextPaint, breite: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, farbe, breite).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1.1f).build()
}
