package de.frank.voicekey.wake

import android.content.Context
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.obs.Obs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Laedt die Vosk-Modelle (DE + EN, je ~40-45 MB) beim ersten Start von alphacephei.com
 * herunter und entpackt sie in den internen App-Speicher. NICHT in der APK (Groesse).
 * Eine ".complete"-Markerdatei schuetzt vor halb entpackten Zustaenden.
 */
object ModelManager {

    private data class ModelInfo(val folderName: String, val url: String)

    private val MODELS = mapOf(
        WakeLang.DE to ModelInfo(
            "vosk-model-small-de-0.15",
            "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
        ),
        WakeLang.EN to ModelInfo(
            "vosk-model-small-en-us-0.15",
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        ),
    )

    private fun modelsRoot(context: Context): File = File(context.filesDir, "models").apply { mkdirs() }

    fun modelDir(context: Context, lang: WakeLang): File =
        File(modelsRoot(context), MODELS.getValue(lang).folderName)

    fun isReady(context: Context, lang: WakeLang): Boolean {
        val dir = modelDir(context, lang)
        return File(dir, ".complete").exists() && File(dir, "conf").exists()
    }

    /**
     * Stellt sicher, dass das Modell der Sprache vorliegt. Fortschritt 0-100 (Download)
     * dann 100 nach dem Entpacken. Gibt true bei Erfolg zurueck — Fehler werden geloggt,
     * nie still verschluckt.
     */
    suspend fun ensureModel(context: Context, lang: WakeLang, onProgress: (Int) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            if (isReady(context, lang)) {
                onProgress(100)
                return@withContext true
            }
            val info = MODELS.getValue(lang)
            val root = modelsRoot(context)
            val zipFile = File(root, "${info.folderName}.zip")
            try {
                Obs.i("ModelManager", "ensureModel", "Modell-Download startet", mapOf("lang" to lang, "url" to info.url))
                download(info.url, zipFile, onProgress)
                unzip(zipFile, root)
                val dir = File(root, info.folderName)
                if (!Obs.probe(File(dir, "conf").exists(), "Entpacktes Modell unvollstaendig (conf fehlt)", "ModelManager", "ensureModel", mapOf("dir" to dir))) {
                    return@withContext false
                }
                File(dir, ".complete").writeText("ok")
                Obs.checkpoint(
                    step = "Modell bereit",
                    intent = "Sprachmodell beim ersten Start herunterladen und entpacken",
                    expected = "bereit",
                    actual = "bereit",
                    ctx = mapOf("lang" to lang, "dir" to dir.name),
                )
                onProgress(100)
                true
            } catch (e: Exception) {
                Obs.e("ModelManager", "ensureModel", "Modell-Download/Entpacken fehlgeschlagen", mapOf("lang" to lang), e)
                false
            } finally {
                zipFile.delete()
            }
        }

    private fun download(url: String, dest: File, onProgress: (Int) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            // Download = 0-95 %, Entpacken = Rest.
                            val percent = ((copied * 95) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        val targetCanonical = targetDir.canonicalPath
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val outFile = File(targetDir, entry.name)
                // Zip-Slip-Schutz: kein Eintrag darf aus dem Zielordner ausbrechen.
                if (!outFile.canonicalPath.startsWith(targetCanonical)) {
                    throw SecurityException("Zip-Eintrag ausserhalb des Ziels: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
            }
        }
    }
}
