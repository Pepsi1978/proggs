package de.frank.aufgaben.erinnerung

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Eigene, lizenzfreie Erinnerungstöne (wie im Genialen Wecker), dazu jeder Systemton.
 * Gespielt wird mit eigener Lautstärke — unabhängig davon, was der Kanal sonst täte.
 */
object Toene {
    val eigene = linkedMapOf(
        "chime" to "Erinnerungszeichen",
        "bells" to "Sanfte Glocken",
        "pulse" to "Digitaler Puls",
        "classic" to "Klassisch",
        "kristall" to "Kristall",
        "tropfen" to "Wassertropfen",
    )

    /** Ordner für eigene Töne (MP3s), die der Nutzer auswählt; sie werden hierher kopiert. */
    fun eigenerOrdner(context: Context): File = File(context.filesDir, "toene").apply { mkdirs() }

    /**
     * Kopiert eine gewählte Audiodatei in die App, damit sie auch nach Verschieben/Löschen des
     * Originals, ohne Netz und im Ruhezustand verfügbar bleibt. Liefert den Einstellungswert "datei:<name>".
     */
    fun eigenenUebernehmen(context: Context, uri: Uri): Pair<String, String> {
        val cr = context.contentResolver
        val anzeige = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Eigener Ton"
        val endung = anzeige.substringAfterLast('.', "mp3").lowercase().filter { it.isLetterOrDigit() }.take(5).ifEmpty { "mp3" }
        val ordner = eigenerOrdner(context)
        val tmp = File(ordner, "neu_${System.nanoTime()}.tmp")
        try {
            val geschrieben = (cr.openInputStream(uri) ?: error("Die Datei lässt sich nicht öffnen.")).use { ein ->
                tmp.outputStream().use { aus ->
                    val puffer = ByteArray(64 * 1024)
                    var summe = 0L
                    while (true) {
                        val n = ein.read(puffer)
                        if (n < 0) break
                        summe += n
                        check(summe <= MAX_BYTES) { "Die Datei ist größer als 20 MB." }
                        aus.write(puffer, 0, n)
                    }
                    summe
                }
            }
            check(geschrieben > 0) { "Die Datei ist leer." }
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(tmp.absolutePath)
                check(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Das ist keine abspielbare Audiodatei." }
            } finally {
                runCatching { mmr.release() }
            }
            val ziel = File(ordner, "eigen_${tmp.length()}_${anzeige.hashCode().toUInt()}.$endung")
            if (ziel.exists()) ziel.delete()
            check(tmp.renameTo(ziel)) { "Der Ton konnte nicht gespeichert werden." }
            // Ältere eigene Töne aufräumen, damit nicht jede Auswahl liegen bleibt.
            ordner.listFiles()?.filter { it.name.startsWith("eigen_") && it != ziel }?.forEach { it.delete() }
            return "datei:${ziel.name}" to anzeige.substringBeforeLast('.')
        } finally {
            tmp.delete()
        }
    }

    private const val MAX_BYTES = 20L * 1024 * 1024

    private fun datei(context: Context, name: String): File {
        val art = name.takeIf { it in eigene } ?: "chime"
        val file = File(context.filesDir, "ton_$art.wav")
        if (file.exists() && file.length() > 44) return file
        synchronized(this) {
            val rate = 22050
            val dauer = when (art) { "chime", "tropfen" -> 2.0; "kristall" -> 2.5; else -> 4.0 }
            val samples = (rate * dauer).toInt()
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            repeat(samples) { i ->
                val t = i.toDouble() / rate
                val wert = when (art) {
                    "bells" -> { val p = t % 1.5; sin(2 * PI * 523.25 * t) * exp(-3.0 * p) + .3 * sin(2 * PI * 1046.5 * t) * exp(-4.0 * p) }
                    "pulse" -> { val p = t % 1.0; (if (p % .25 < .16) .8 else 0.0) * sin(2 * PI * (if (p < .5) 880.0 else 1108.73) * t) }
                    "classic" -> { val p = t % 1.0; (if (p % .25 < .16) .8 else 0.0) * sin(2 * PI * 880.0 * t) }
                    "kristall" -> listOf(0.0 to 1318.5, 0.18 to 1568.0, 0.36 to 2093.0).sumOf { (s, f) ->
                        if (t < s) 0.0 else sin(2 * PI * f * t) * exp(-4.0 * (t - s)) * .5
                    }
                    "tropfen" -> { val p = t % 0.5; sin(2 * PI * (600.0 + 900.0 * exp(-18.0 * p)) * t) * exp(-9.0 * p) }
                    else -> { val s = if (t < .45) t else t - .45; sin(2 * PI * (if (t < .45) 659.25 else 987.77) * t) * exp(-3.0 * s) }
                }
                val weich = minOf(1.0, t * 200, (dauer - t) * 60)
                buffer.putShort((wert * weich * 17000).toInt().coerceIn(-32767, 32767).toShort())
            }
            val tmp = File(context.filesDir, "ton_$art.tmp")
            tmp.writeBytes(buffer.array())
            tmp.renameTo(file)
        }
        return file
    }

    /** Eingebauter Ton, Systemton ("uri:") oder eigene Datei ("datei:") als Quelle des Players. */
    fun setzeQuelle(context: Context, player: MediaPlayer, ton: String) {
        when {
            ton.startsWith("uri:") -> player.setDataSource(context, Uri.parse(ton.removePrefix("uri:")))
            ton.startsWith("datei:") -> {
                val eigen = File(eigenerOrdner(context), ton.removePrefix("datei:"))
                // Fehlt die eigene Datei (z. B. nach einer Wiederherstellung), klingt der Standardton.
                player.setDataSource(if (eigen.isFile) eigen.absolutePath else datei(context, "chime").absolutePath)
            }
            else -> player.setDataSource(datei(context, ton).absolutePath)
        }
    }

    /** Spielt den Ton einmal. [fertig] kommt nach dem Ende oder spätestens nach [maxMs]. */
    fun spiele(context: Context, ton: String, lautstaerke: Float, maxMs: Long = 9_000, fertig: () -> Unit = {}): MediaPlayer? {
        return try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setzeQuelle(context, player, ton)
            player.setVolume(lautstaerke, lautstaerke)
            var beendet = false
            val ende = {
                if (!beendet) {
                    beendet = true
                    runCatching { player.stop() }
                    runCatching { player.release() }
                    fertig()
                }
            }
            player.setOnCompletionListener { ende() }
            player.setOnErrorListener { _, _, _ -> ende(); true }
            player.prepare()
            player.start()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ ende() }, maxMs)
            player
        } catch (_: Exception) {
            fertig()
            null
        }
    }
}
