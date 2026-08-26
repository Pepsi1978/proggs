package de.frank.gedankenspeicher.ui.verlauf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Der Bildspeicher des Verlaufs.**
 *
 * Ohne ihn kostete jedes Bild, jede Zeichnung und jede PDF-Vorschau beim Scrollen den vollen
 * Weg: Datei öffnen, Maße lesen, ein zweites Mal dekodieren, nach EXIF drehen. Das sind je
 * Bild etliche Millisekunden auf der Platte und mehrere Megabyte frischer Speicher — bei
 * jedem Mal, das die Karte aus dem Bild scrollt und zurückkommt. Der Speicherdruck rief
 * dann noch die Speicherbereinigung auf den Plan, und die hielt den Zeichenfaden an.
 *
 * Hier wird jedes Bild genau **einmal** geladen. Der Speicher misst in Bytes, nicht in
 * Stück, und nimmt ein Achtel des App-Speichers — genug für das Fenster um die sichtbaren
 * Karten, klein genug, um nie der Grund für einen Speichernotstand zu sein.
 *
 * Wichtig ist der **synchrone** Zugriff in [merkeBild]: liegt das Bild schon im Speicher,
 * steht es beim allerersten Zeichnen der Karte da. Ohne das käme die Karte erst leer, dann
 * mit Bild — und jede solche Nachmeldung ist ein zweites Layout mitten im Scrollen.
 */
internal object Bildspeicher {

    private val speicher = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 8L).coerceIn(4L * 1024 * 1024, 64L * 1024 * 1024).toInt(),
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            runCatching { value.asAndroidBitmap().allocationByteCount }.getOrDefault(1)
    }

    private fun schluessel(pfad: String, maxKante: Int) = "$pfad|$maxKante"

    /** Was schon im Speicher liegt — ohne einen Griff auf die Platte. */
    fun bereits(pfad: String, maxKante: Int): ImageBitmap? = speicher.get(schluessel(pfad, maxKante))

    /**
      * Bis zu dieser Kante wird behalten. Darüber liegt nur die Vollbildschau, und ein
      * Bild in voller Grösse wäre allein schon ein gutes Dutzend Megabyte — es verdrängte
      * genau die kleinen Vorschauen, um deretwillen es diesen Speicher gibt.
      */
    private const val HOECHSTE_GEMERKTE_KANTE = 1600

    /** Lädt das Bild, falls nötig, und legt es ab. Gehört auf einen Hintergrundfaden. */
    fun lade(pfad: String, maxKante: Int): ImageBitmap? {
        bereits(pfad, maxKante)?.let { return it }
        val bild = ladeBild(pfad, maxKante)?.asImageBitmap() ?: return null
        // Die Textur vorab an die Grafik übergeben: sonst geschieht genau das beim ersten
        // Zeichnen — mitten im Scrollen, auf dem Zeichenfaden.
        runCatching { bild.prepareToDraw() }
        if (maxKante <= HOECHSTE_GEMERKTE_KANTE) speicher.put(schluessel(pfad, maxKante), bild)
        return bild
    }

    fun leere() = speicher.evictAll()

    private fun ladeBild(pfad: String, maxKante: Int): Bitmap? {
        if (!File(pfad).exists()) return null
        val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(pfad, masse)
        if (masse.outWidth <= 0) return null
        var faktor = 1
        while (masse.outWidth / faktor > maxKante || masse.outHeight / faktor > maxKante) faktor *= 2
        val roh = BitmapFactory.decodeFile(pfad, BitmapFactory.Options().apply { inSampleSize = faktor })
            ?: return null
        return drehNachExif(roh, pfad)
    }

    /** Wendet die im Bild vermerkte Ausrichtung wirklich an. */
    private fun drehNachExif(bild: Bitmap, pfad: String): Bitmap {
        val ausrichtung = runCatching {
            ExifInterface(pfad).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        if (ausrichtung == ExifInterface.ORIENTATION_NORMAL || ausrichtung == ExifInterface.ORIENTATION_UNDEFINED) {
            return bild
        }
        val matrix = Matrix()
        when (ausrichtung) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bild
        }
        return runCatching {
            Bitmap.createBitmap(bild, 0, 0, bild.width, bild.height, matrix, true)
                .also { gedreht -> if (gedreht !== bild) bild.recycle() }
        }.getOrDefault(bild)
    }
}

/**
 * Lädt ein Bild verkleinert, gedreht und außerhalb des Hauptfadens — und merkt es sich.
 *
 * Ohne die Verkleinerung reißt eine 12-Megapixel-Aufnahme in einer Liste den Speicher auf;
 * ohne die EXIF-Drehung liegt jede Hochformat-Aufnahme quer, weil die Kamera den Sensor
 * nicht dreht, sondern nur vermerkt, wie das Bild gemeint war.
 */
@Composable
internal fun merkeBild(pfad: String?, maxKante: Int = 1400): ImageBitmap? {
    // Der erste Wert kommt aus dem Speicher: eine Karte, die schon einmal sichtbar war,
    // zeigt ihr Bild beim Zurückscrollen ohne Umweg und ohne zweites Layout.
    var bild by remember(pfad, maxKante) {
        mutableStateOf(if (pfad.isNullOrBlank()) null else Bildspeicher.bereits(pfad, maxKante))
    }
    LaunchedEffect(pfad, maxKante) {
        if (bild != null) return@LaunchedEffect
        bild = if (pfad.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { Bildspeicher.lade(pfad, maxKante) }.getOrNull()
        }
    }
    return bild
}
