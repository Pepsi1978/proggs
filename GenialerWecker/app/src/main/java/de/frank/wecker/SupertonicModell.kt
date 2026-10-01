package de.frank.wecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Supertonic 3 (fp32-Originale von Supertone, OpenRAIL-M) wird nicht mehr in die APK gepackt, sondern einmal in der App
 * heruntergeladen – die APK schrumpft so um rund 400 MB, Updates über UpdateStation laufen wieder zuverlässig.
 * Die vier großen ONNX-Dateien kommen von Hugging Face (fester Stand, Größen geprüft); die kleinen Steuerdateien
 * (tts.json, unicode_indexer.bin, voice.bin aus dem sherpa-onnx-Export) liegen weiter als Assets in der APK und werden
 * beim ersten Laden daneben kopiert. Alles liegt in noBackupFilesDir.
 */
object SupertonicModell {
    private const val REVISION = "3cadd1ee6394adea1bd021217a0e650ede09a323"
    private const val QUELLE = "https://huggingface.co/Supertone/supertonic-3/resolve/$REVISION/onnx/"
    private const val ARBEIT = "supertonic-download"
    val DATEIEN: Map<String, Long> = linkedMapOf(
        "duration_predictor.onnx" to 3_700_147L,
        "text_encoder.onnx" to 36_416_150L,
        "vector_estimator.onnx" to 256_534_781L,
        "vocoder.onnx" to 101_424_195L,
    )
    val KLEIN = listOf("tts.json", "unicode_indexer.bin", "voice.bin")
    val gesamt: Long get() = DATEIEN.values.sum()
    /** z. B. „398 MB“ */
    val mb: String get() = "%,d MB".format(java.util.Locale.GERMANY, (gesamt + 500_000) / 1_000_000)

    fun ordner(context: Context) = File(context.noBackupFilesDir, "supertonic-3")
    fun datei(context: Context, name: String) = File(ordner(context), name)
    private fun teil(context: Context, name: String) = File(ordner(context), "$name.part")

    fun geladen(context: Context): Boolean = DATEIEN.all { (name, groesse) -> datei(context, name).length() == groesse }
    fun bytesDa(context: Context): Long = DATEIEN.keys.sumOf { name ->
        datei(context, name).takeIf { it.isFile }?.length() ?: teil(context, name).length()
    }
    fun platzReicht(context: Context): Boolean = context.noBackupFilesDir.usableSpace > (gesamt - bytesDa(context)) + 200_000_000L

    /** Kopiert die kleinen Steuerdateien aus den Assets neben die Modelle (einmalig). */
    fun steuerdateienBereitstellen(context: Context) {
        ordner(context).mkdirs()
        for (name in KLEIN) {
            val ziel = datei(context, name)
            if (ziel.length() > 0) continue
            val tmp = File(ziel.path + ".tmp")
            context.assets.open("tts/supertonic/$name").use { ein -> tmp.outputStream().use { ein.copyTo(it) } }
            if (!tmp.renameTo(ziel)) throw IOException("$name ließ sich nicht speichern")
        }
    }

    fun starteDownload(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(ARBEIT, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SupertonicDownload>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build())
    }

    fun pausieren(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(ARBEIT) }

    fun loeschen(context: Context) {
        pausieren(context)
        ordner(context).deleteRecursively()
    }

    fun laeuftFlow(context: Context) = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(ARBEIT)

    internal fun ladeAlles(context: Context, fortschritt: (Long) -> Unit, abgebrochen: () -> Boolean) {
        ordner(context).mkdirs()
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
        for ((name, groesse) in DATEIEN) {
            val ziel = datei(context, name)
            if (ziel.length() == groesse) continue
            ziel.delete()
            val teil = teil(context, name)
            if (teil.length() > groesse) teil.delete()
            var start = teil.length()
            if (start < groesse) {
                val anfrage = Request.Builder().url(QUELLE + name).apply { if (start > 0) header("Range", "bytes=$start-") }.build()
                client.newCall(anfrage).execute().use { antwort ->
                    if (!antwort.isSuccessful) throw IOException("HTTP ${antwort.code}")
                    // Ignoriert der Server den Range-Wunsch (200 statt 206), von vorn beginnen.
                    if (start > 0 && antwort.code != 206) start = 0
                    FileOutputStream(teil, start > 0).use { aus ->
                        antwort.body!!.byteStream().use { ein ->
                            val puffer = ByteArray(1 shl 16)
                            var zuletzt = 0L
                            while (true) {
                                if (abgebrochen()) return
                                val n = ein.read(puffer)
                                if (n < 0) break
                                aus.write(puffer, 0, n)
                                start += n
                                if (start - zuletzt > 2_000_000) { zuletzt = start; fortschritt(bytesDa(context)) }
                            }
                        }
                    }
                }
            }
            if (teil.length() != groesse) throw IOException("$name unvollständig (${teil.length()} von $groesse)")
            if (!teil.renameTo(ziel)) throw IOException("$name ließ sich nicht speichern")
            fortschritt(bytesDa(context))
        }
        steuerdateienBereitstellen(context)
    }
}

/** Hintergrund-Download der Supertonic-Stimmen mit Fortschritts-Benachrichtigung; Abbrüche setzen beim nächsten Versuch fort. */
class SupertonicDownload(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val kanal = "downloads"
    private val id = 4712

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching { setForeground(info(SupertonicModell.bytesDa(applicationContext))) }
        try {
            SupertonicModell.ladeAlles(applicationContext, fortschritt = { bytes ->
                runCatching { applicationContext.getSystemService(NotificationManager::class.java)?.notify(id, info(bytes).notification) }
            }, abgebrochen = { isStopped })
            ensureActive()
            if (SupertonicModell.geladen(applicationContext)) {
                applicationContext.getSystemService(NotificationManager::class.java)?.cancel(id)
                Result.success()
            } else Result.retry()
        } catch (e: IOException) {
            android.util.Log.w("WeckerStimmen", "Download Supertonic unterbrochen, wird fortgesetzt", e)
            if (runAttemptCount < 50) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = info(SupertonicModell.bytesDa(applicationContext))

    private fun info(bytes: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(kanal) == null)
            manager?.createNotificationChannel(NotificationChannel(kanal, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val gesamt = SupertonicModell.gesamt
        val prozent = (bytes * 100 / gesamt).toInt().coerceIn(0, 100)
        val n = NotificationCompat.Builder(applicationContext, kanal)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Supertonic-Stimmen werden geladen")
            .setContentText("${bytes / 1_000_000} von ${gesamt / 1_000_000} MB")
            .setProgress(100, prozent, false)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(id, n)
    }
}
