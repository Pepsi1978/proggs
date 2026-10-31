package de.frank.jarvis.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/** Ein Anhang einer Frage an Jarvis (Foto, Bild oder Datei), als Kopie unter `files/gespraeche/anhaenge/`. */
data class Anhang(val pfad: String) {
    val datei get() = File(pfad)
    /** Der Name ohne die Zeitmarke, die beim Übernehmen davorgesetzt wird. */
    val name: String get() = datei.name.substringAfter('_')
    val endung: String get() = name.substringAfterLast('.', "").lowercase()
    val istBild: Boolean get() = endung in BILDER
    /** Der Bildtyp fürs Modell; null für Formate, die das Modell nicht liest (HEIC). */
    val mime: String? get() = when (endung) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> null
    }
    val istPdf: Boolean get() = endung == "pdf"
    val istText: Boolean get() = endung in TEXTE

    companion object {
        val BILDER = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif")
        val TEXTE = setOf("txt", "md", "csv", "json", "xml", "html", "htm", "log", "kt", "kts", "java", "py", "js", "ts", "yaml", "yml", "ini", "conf", "cfg", "sql", "sh", "ps1", "cs", "swift", "gradle", "properties", "srt", "vtt")
    }
}

/** Eine Datei auf dem Weg zur Programmier-Sitzung: Prüfsumme, Name und wo sie auf dem Handy liegt. */
class UebertragDatei(val sha: String, val name: String, val datei: File)

/** Was vom Anhang ans Modell geht: Bilder (als data-URL) und lesbarer Text. */
class ModellAnhaenge(val bilder: List<String>, val text: String)

object Anhaenge {
    private const val KANTE = 1600
    private const val MAX_TEXT = 60_000
    private const val MAX_PDF_SEITEN = 6
    private const val MAX_SITZUNG_SEITEN = 20
    private const val SEITE_BREITE = 1400

    fun ordner(context: Context) = File(context.filesDir, "gespraeche/anhaenge").apply { mkdirs() }

    /** Eine Kamera-Datei im Cache, freigegeben über den FileProvider. Ältere Aufnahmen sind längst übernommen und fallen weg. */
    fun kameraDatei(context: Context): File {
        val ordner = File(context.cacheDir, "kamera").apply { mkdirs() }
        val grenze = System.currentTimeMillis() - 3_600_000
        ordner.listFiles()?.filter { it.lastModified() < grenze }?.forEach { runCatching { it.delete() } }
        return File(ordner, "foto_${System.currentTimeMillis()}.jpg")
    }

    /** Kopiert eine gewählte Datei in Jarvis' Speicher; Bilder werden dabei aufrecht gedreht und verkleinert. */
    fun uebernehmen(context: Context, uri: Uri): Anhang? = runCatching {
        val cr = context.contentResolver
        var name = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: uri.lastPathSegment?.substringAfterLast('/') ?: "datei"
        val mime = cr.getType(uri).orEmpty()
        if (!name.contains('.')) name += when {
            mime == "application/pdf" -> ".pdf"
            mime.startsWith("image/") -> ".jpg"
            mime.startsWith("text/") -> ".txt"
            else -> ""
        }
        name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        // Gleichnamige Dateien in derselben Millisekunde (etwa zwei „scan.pdf“) dürfen sich nicht überschreiben.
        val basis = System.currentTimeMillis()
        val ziel = generateSequence(0) { it + 1 }.map { File(ordner(context), "${basis + it}_$name") }
            .first { !it.exists() && !File(it.parentFile, it.nameWithoutExtension + ".jpg").exists() }
        val alsBild = mime.startsWith("image/") || name.substringAfterLast('.').lowercase() in Anhang.BILDER
        if (alsBild) {
            val roh = cr.openInputStream(uri)!!.use { it.readBytes() }
            val bild = verkleinert(roh)
            if (bild != null) {
                val jpg = File(ziel.parentFile, ziel.nameWithoutExtension + ".jpg")
                jpg.outputStream().use { bild.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                return@runCatching Anhang(jpg.path)
            }
        }
        cr.openInputStream(uri)!!.use { ein -> ziel.outputStream().use { ein.copyTo(it) } }
        Anhang(ziel.path)
    }.getOrNull()

    private fun verkleinert(roh: ByteArray): Bitmap? {
        val grenzen = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(roh, 0, roh.size, grenzen)
        if (grenzen.outWidth <= 0) return null
        var stufe = 1
        while (maxOf(grenzen.outWidth, grenzen.outHeight) / (stufe * 2) >= KANTE) stufe *= 2
        val bild = BitmapFactory.decodeByteArray(roh, 0, roh.size, BitmapFactory.Options().apply { inSampleSize = stufe }) ?: return null
        val drehung = runCatching {
            when (android.media.ExifInterface(roh.inputStream()).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
            }
        }.getOrDefault(0f)
        val faktor = minOf(1f, KANTE.toFloat() / maxOf(bild.width, bild.height))
        if (drehung == 0f && faktor >= 1f) return bild
        val m = Matrix().apply { postScale(faktor, faktor); postRotate(drehung) }
        return Bitmap.createBitmap(bild, 0, 0, bild.width, bild.height, m, true)
    }

    /** Bereitet die Anhänge fürs Modell auf: Bilder direkt, PDF-Seiten als Bilder, Textdateien als Text, sonst Name und Größe. */
    fun fuersModell(anhaenge: List<Anhang>): ModellAnhaenge {
        val bilder = mutableListOf<String>()
        val text = StringBuilder()
        anhaenge.forEach { a ->
            val f = a.datei
            if (!f.exists()) return@forEach
            when {
                // Übernommene Bilder sind JPEG; ließ sich eines nicht umwandeln, bleibt seine Endung und damit sein Typ.
                a.istBild && a.mime != null -> bilder += "data:${a.mime};base64," + Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
                a.istPdf -> {
                    val seiten = runCatching { pdfSeiten(f) }.getOrDefault(emptyList())
                    bilder += seiten
                    text.append("\n\n[Angehängtes PDF „${a.name}“: ${seiten.size} Seite(n) als Bilder beigefügt]")
                }
                a.istText -> {
                    val inhalt = runCatching { f.readText() }.getOrDefault("")
                    text.append("\n\n[Angehängte Datei „${a.name}“]\n").append(inhalt.take(MAX_TEXT))
                    if (inhalt.length > MAX_TEXT) text.append("\n(… gekürzt)")
                }
                else -> text.append("\n\n[Angehängte Datei „${a.name}“, ${f.length() / 1024} KB, Inhalt nicht lesbar. Sie liegt in Jarvis' Speicher.]")
            }
        }
        return ModellAnhaenge(bilder, text.toString())
    }

    /**
     * Die Dateien für die Programmier-Sitzung: jeder Anhang im Original, zu PDFs zusätzlich die Seiten als Bilder. Die kommen in der
     * CLI als echte Bilder an und bleiben lesbar, auch wenn dort pdftoppm fehlt (am 31.10.2026 scheiterte daran das Lesen eines PDFs).
     */
    fun fuerSitzung(context: Context, anhaenge: List<Anhang>): List<UebertragDatei> {
        val seitenOrdner = File(context.cacheDir, "anhang-seiten").apply { mkdirs() }
        val grenze = System.currentTimeMillis() - 86_400_000
        seitenOrdner.listFiles()?.filter { it.lastModified() < grenze }?.forEach { runCatching { it.delete() } }
        return anhaenge.filter { it.datei.isFile }.flatMap { a ->
            val seiten = if (!a.istPdf) emptyList() else runCatching { pdfSeitenDateien(a.datei, seitenOrdner) }.getOrDefault(emptyList())
            listOf(UebertragDatei(sha256(a.datei), a.name, a.datei)) + seiten.mapIndexed { i, f ->
                UebertragDatei(sha256(f), a.name.substringBeforeLast('.') + "_Seite_${i + 1}.jpg", f)
            }
        }
    }

    private fun sha256(f: File): String {
        val pruefung = java.security.MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ein ->
            val puffer = ByteArray(256 * 1024)
            while (true) { val n = ein.read(puffer); if (n < 0) break; pruefung.update(puffer, 0, n) }
        }
        return pruefung.digest().joinToString("") { "%02x".format(it) }
    }

    private fun pdfSeitenDateien(f: File, ordner: File): List<File> {
        val ergebnis = mutableListOf<File>()
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { pdf ->
                for (i in 0 until minOf(pdf.pageCount, MAX_SITZUNG_SEITEN)) {
                    pdf.openPage(i).use { seite ->
                        val hoehe = (SEITE_BREITE.toFloat() * seite.height / seite.width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(SEITE_BREITE, hoehe, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        seite.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val ziel = File(ordner, "${System.nanoTime()}.jpg")
                        ziel.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                        bmp.recycle()
                        ergebnis += ziel
                    }
                }
            }
        }
        return ergebnis
    }

    private fun pdfSeiten(f: File): List<String> {
        val ergebnis = mutableListOf<String>()
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { pdf ->
                for (i in 0 until minOf(pdf.pageCount, MAX_PDF_SEITEN)) {
                    pdf.openPage(i).use { seite ->
                        val breite = 1400
                        val hoehe = (breite.toFloat() * seite.height / seite.width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(breite, hoehe, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        seite.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val aus = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, aus)
                        ergebnis += "data:image/jpeg;base64," + Base64.encodeToString(aus.toByteArray(), Base64.NO_WRAP)
                    }
                }
            }
        }
        return ergebnis
    }
}
