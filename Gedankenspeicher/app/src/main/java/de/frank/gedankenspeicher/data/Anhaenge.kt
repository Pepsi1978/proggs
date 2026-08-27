package de.frank.gedankenspeicher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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

/**
 * **Die Anhänge einer Notiz.**
 *
 * Eine Art je Eintrag im Plus-Menü. Dateibasierte Anhänge liegen unter
 * `files/anhaenge` und gehören damit der App allein; textbasierte (Haftnotiz,
 * Tabelle) stehen als Text in der Notiz selbst und brauchen keine Datei.
 */
enum class Anhangsart { PDF, SPRACHAUFNAHME, BILD, SCAN, AUDIO, ZEICHNUNG, HAFTNOTIZ, TABELLE }

data class Anhang(
    val id: String = UUID.randomUUID().toString(),
    val art: Anhangsart,
    val name: String,
    val pfad: String = "",
    /** Vorschaubild der ersten PDF-Seite. */
    val vorschauPfad: String = "",
    /** Haftnotiz-Text beziehungsweise die Tabelle als Zeilen mit Tabulatoren. */
    val text: String = "",
    val farbe: Int = 0,
    val dauerMs: Long = 0L,
    val seiten: Int = 0,
    /** Spaltenbreiten einer Tabelle in dp — leer heisst: überall die Standardbreite. */
    val spaltenbreiten: List<Int> = emptyList(),
    val erstelltAm: Long = System.currentTimeMillis(),
) {
    val datei: File? get() = pfad.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::exists)
    val vorschau: File? get() = vorschauPfad.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::exists)

    val beschriftung: String get() = when (art) {
        Anhangsart.PDF -> "PDF"
        Anhangsart.SPRACHAUFNAHME -> "Sprachaufnahme"
        Anhangsart.BILD -> "Bild"
        Anhangsart.SCAN -> "Dokumentenscan"
        Anhangsart.AUDIO -> "Audiodatei"
        Anhangsart.ZEICHNUNG -> "Zeichnung"
        Anhangsart.HAFTNOTIZ -> "Haftnotiz"
        Anhangsart.TABELLE -> "Tabelle"
    }

    fun json(): JSONObject = JSONObject()
        .put("id", id).put("art", art.name).put("name", name).put("pfad", pfad)
        .put("vorschauPfad", vorschauPfad).put("text", text).put("farbe", farbe)
        .put("dauerMs", dauerMs).put("seiten", seiten).put("erstelltAm", erstelltAm)
        .put("spaltenbreiten", JSONArray(spaltenbreiten))
}

fun List<Anhang>.alsJson(): String = JSONArray(map(Anhang::json)).toString()

fun anhaengeAusJson(roh: String): List<Anhang> = runCatching {
    val feld = JSONArray(roh)
    (0 until feld.length()).mapNotNull(feld::optJSONObject).mapNotNull { eintrag ->
        val art = runCatching { Anhangsart.valueOf(eintrag.optString("art")) }.getOrNull()
            ?: return@mapNotNull null
        Anhang(
            id = eintrag.optString("id").ifBlank { UUID.randomUUID().toString() },
            art = art,
            name = eintrag.optString("name"),
            pfad = eintrag.optString("pfad"),
            vorschauPfad = eintrag.optString("vorschauPfad"),
            text = eintrag.optString("text"),
            farbe = eintrag.optInt("farbe"),
            dauerMs = eintrag.optLong("dauerMs"),
            seiten = eintrag.optInt("seiten"),
            erstelltAm = eintrag.optLong("erstelltAm", System.currentTimeMillis()),
            spaltenbreiten = eintrag.optJSONArray("spaltenbreiten")?.let { feld ->
                (0 until feld.length()).map(feld::optInt)
            }.orEmpty(),
        )
    }
}.getOrDefault(emptyList())

/**
 * Legt Anhangsdateien im App-eigenen Speicher ab und erzeugt ihre Vorschauen.
 *
 * Kopiert wird bewusst: die gewählte Datei kann verschwinden oder ihre Freigabe
 * verlieren, die Notiz soll aber dauerhaft vollständig bleiben.
 */
class Anhangsspeicher(private val ctx: Context) {

    val ordner: File get() = File(ctx.filesDir, ORDNER).apply { mkdirs() }

    fun neueDatei(endung: String): File = File(ordner, "${UUID.randomUUID()}$endung")

    suspend fun uebernimm(uri: Uri, art: Anhangsart): Anhang = withContext(Dispatchers.IO) {
        val anzeigename = anzeigename(uri)
        val endung = "." + (anzeigename.substringAfterLast('.', "").ifBlank { standardEndung(art) })
        val ziel = neueDatei(endung)
        ctx.contentResolver.openInputStream(uri)?.use { quelle ->
            FileOutputStream(ziel).use(quelle::copyTo)
        } ?: error("Die Datei ist nicht erreichbar.")
        beschreibe(ziel, art, anzeigename.ifBlank { standardName(art) })
    }

    /** Ergänzt eine fertige Datei um Vorschau, Seitenzahl beziehungsweise Laufzeit. */
    fun beschreibe(datei: File, art: Anhangsart, name: String): Anhang = when (art) {
        Anhangsart.PDF -> {
            val (vorschau, seiten) = pdfVorschau(datei)
            Anhang(art = art, name = name, pfad = datei.absolutePath, vorschauPfad = vorschau.orEmpty(), seiten = seiten)
        }
        Anhangsart.SPRACHAUFNAHME, Anhangsart.AUDIO ->
            Anhang(art = art, name = name, pfad = datei.absolutePath, dauerMs = laufzeit(datei))
        else -> Anhang(art = art, name = name, pfad = datei.absolutePath)
    }

    fun loesche(anhaenge: List<Anhang>) = anhaenge.forEach { anhang ->
        runCatching { anhang.datei?.delete(); anhang.vorschau?.delete() }
    }

    private fun anzeigename(uri: Uri): String = runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { zeiger ->
            if (zeiger.moveToFirst()) zeiger.getString(0) else null
        }
    }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty().substringAfterLast('/') }

    private fun standardEndung(art: Anhangsart): String = when (art) {
        Anhangsart.PDF -> "pdf"
        Anhangsart.SPRACHAUFNAHME, Anhangsart.AUDIO -> "m4a"
        else -> "jpg"
    }

    private fun standardName(art: Anhangsart): String = when (art) {
        Anhangsart.PDF -> "Dokument.pdf"
        Anhangsart.SPRACHAUFNAHME -> "Sprachaufnahme"
        Anhangsart.AUDIO -> "Audiodatei"
        Anhangsart.SCAN -> "Dokumentenscan"
        Anhangsart.ZEICHNUNG -> "Zeichnung"
        else -> "Bild"
    }

    private fun laufzeit(datei: File): Long = runCatching {
        val leser = MediaMetadataRetriever()
        try {
            leser.setDataSource(datei.absolutePath)
            leser.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            leser.release()
        }
    }.getOrDefault(0L)

    /** Rendert die erste PDF-Seite als Vorschaubild und liefert zusätzlich die Seitenzahl. */
    private fun pdfVorschau(datei: File): Pair<String?, Int> = runCatching {
        ParcelFileDescriptor.open(datei, ParcelFileDescriptor.MODE_READ_ONLY).use { griff ->
            PdfRenderer(griff).use { zeichner ->
                val seiten = zeichner.pageCount
                if (seiten == 0) return@use null to 0
                zeichner.openPage(0).use { seite ->
                    val hoehe = (VORSCHAU_BREITE.toFloat() * seite.height / seite.width).toInt().coerceAtLeast(1)
                    val bild = Bitmap.createBitmap(VORSCHAU_BREITE, hoehe, Bitmap.Config.ARGB_8888)
                    bild.eraseColor(Color.WHITE)
                    seite.render(bild, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val ziel = neueDatei("-vorschau.png")
                    FileOutputStream(ziel).use { aus -> bild.compress(Bitmap.CompressFormat.PNG, 92, aus) }
                    bild.recycle()
                    ziel.absolutePath to seiten
                }
            }
        }
    }.getOrNull() ?: (null to 0)

    companion object {
        const val ORDNER = "anhaenge"
        const val VORSCHAU_BREITE = 900
    }
}
