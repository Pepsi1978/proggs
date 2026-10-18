package de.frank.jarvis.ablage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Bildarbeit auf dem Gerät: Vorschaubilder (Bild, Video-Standbild, erste PDF-Seite), PDF-Seiten für den Betrachter
 * und das Erzeugen einfacher PDFs (Text als DIN-A4-Dokument, Bild auf eine DIN-A4-Seite). Große Bilder werden nie
 * in voller Auflösung in den Speicher geladen, sondern beim Dekodieren verkleinert.
 */
object Medien {
    /** Eine Sperre je Zieldatei: Liste und Speichern erzeugen dasselbe Vorschaubild sonst gleichzeitig und verschränkt. */
    private val sperren = ConcurrentHashMap<String, Any>()

    private fun <T> gesperrt(ziel: File, block: () -> T): T = synchronized(sperren.getOrPut(ziel.absolutePath) { Any() }) { block() }

    /** Schreibt ein Bild über eine eigene Zwischendatei und benennt dann um; bleibt sie liegen, wird sie entfernt. */
    private fun schreibeBild(bmp: Bitmap, format: Bitmap.CompressFormat, qualitaet: Int, ziel: File): File? {
        ziel.parentFile?.mkdirs()
        val tmp = File(ziel.parentFile, ziel.name + "." + UUID.randomUUID().toString().take(8) + ".tmp")
        return try {
            FileOutputStream(tmp).use { bmp.compress(format, qualitaet, it) }
            if (tmp.renameTo(ziel) || (ziel.delete() && tmp.renameTo(ziel))) ziel else null
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

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
    fun vorschaubild(quelle: File, art: Art, ziel: File): File? = gesperrt(ziel) {
        // Unter der Sperre erneut prüfen: Hat ein anderer Faden das Bild gerade erzeugt, wird es nur noch übernommen.
        if (ziel.isFile && ziel.length() > 0 && ziel.lastModified() >= quelle.lastModified()) return@gesperrt ziel
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
        }.getOrNull() ?: return@gesperrt null
        val klein = if (maxOf(bild.width, bild.height) > 400) {
            val f = 360f / maxOf(bild.width, bild.height)
            Bitmap.createScaledBitmap(bild, (bild.width * f).toInt().coerceAtLeast(1), (bild.height * f).toInt().coerceAtLeast(1), true).also { if (it !== bild) bild.recycle() }
        } else bild
        runCatching { schreibeBild(klein, Bitmap.CompressFormat.JPEG, 82, ziel) }.getOrNull().also { klein.recycle() }
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

    /**
     * Rendert eine Seite in eine Bilddatei (für den zoombaren Betrachter, der große Bilder in Kacheln lädt). Endet [ziel]
     * auf .png, wird verlustfrei gespeichert, sonst als JPEG mit Qualität 92: Der Hintergrund ist ohnehin weiß, Text
     * bleibt scharf, und das Speichern geht um ein Vielfaches schneller als ein PNG in voller Auflösung.
     */
    fun pdfSeiteAlsDatei(datei: File, index: Int, laengste: Int, ziel: File): File? = gesperrt(ziel) {
        if (ziel.isFile && ziel.length() > 0 && ziel.lastModified() >= datei.lastModified()) return@gesperrt ziel
        val (_, bmp) = pdfSeite(datei, index, laengste) ?: return@gesperrt null
        try {
            if (ziel.name.endsWith(".png")) schreibeBild(bmp, Bitmap.CompressFormat.PNG, 100, ziel)
            else schreibeBild(bmp, Bitmap.CompressFormat.JPEG, 92, ziel)
        } finally { bmp.recycle() }
    }

    /**
     * Räumt den Zwischenspeicher der PDF-Seiten auf: Seiten älter als [alterMs] und Seiten im alten PNG-Format.
     * Sonst wächst er je angesehener Seite um mehrere MB und wird nur vom System geleert.
     */
    fun pdfSeitenAufraeumen(ordner: File, alterMs: Long = 7 * 24 * 3_600_000L) {
        val grenze = System.currentTimeMillis() - alterMs
        ordner.listFiles()?.forEach { f -> if (f.isFile && (f.lastModified() < grenze || f.name.endsWith(".png") || f.name.endsWith(".tmp"))) f.delete() }
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

    private val BILD_ZEILE = Regex("^\\s*!\\[(.*?)]\\((.+?)\\)\\s*$")

    /**
     * Setzt einen Markdown-Text als gestaltetes DIN-A4-Dokument ([PdfGestaltung]: Titelseite, Inhaltsübersicht, Merkkästen,
     * Tabellen, Abbildungen). [bilder] (Dateiname klein geschrieben → Datei) sind die Bilder des Ablage-Eintrags: Eine Zeile
     * `![Unterschrift](name.png)` setzt das Bild an genau diese Stelle, `![Titelbild](name.png)` auf die Titelseite, Bilder ohne
     * solche Zeile kommen am Ende unter „Abbildungen“.
     */
    fun textAlsPdf(titel: String, text: String, ziel: File, bilder: Map<String, File> = emptyMap(), untertitel: String? = null): PdfPruefung =
        PdfGestaltung.setze(titel, text, ziel, bilder, untertitel)

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

}
