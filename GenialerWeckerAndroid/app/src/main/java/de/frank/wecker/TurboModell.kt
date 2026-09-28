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
 * Premium-Erkennung „Whisper Large V3 Turbo“ (int8, sherpa-onnx-Export, MIT) als Download: etwa halb so viele Fehler wie
 * das eingebaute small-Modell, auf dem PC gemessen nur 1,2- bis 2-mal langsamer (28.09.2026). Alle Sprachen der App.
 * Liegt in noBackupFilesDir (1 GB gehört nicht in die Datensicherung); der Download läuft über WorkManager im
 * Hintergrund weiter und setzt nach Abbruch, App-Ende oder Neustart per HTTP-Range an derselben Stelle fort.
 */
object TurboModell {
    const val NAME = "Whisper Large V3 Turbo"
    private const val ARBEIT = "whisper-turbo-download"
    private const val QUELLE = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-turbo/resolve/main/"
    val DATEIEN = linkedMapOf(
        "turbo-encoder.int8.onnx" to 674_716_297L,
        "turbo-decoder.int8.onnx" to 361_080_764L,
        "turbo-tokens.txt" to 816_730L,
    )
    val GESAMT: Long = DATEIEN.values.sum()
    val groesseText: String get() = "%.1f GB".format(GESAMT / 1e9).replace('.', ',')

    fun ordner(context: Context) = File(context.noBackupFilesDir, "whisper-turbo")
    fun datei(context: Context, name: String) = File(ordner(context), name)
    private fun teil(context: Context, name: String) = File(ordner(context), "$name.part")

    /** Alle Dateien vollständig da (Länge exakt wie auf dem Server). */
    fun geladen(context: Context): Boolean = DATEIEN.all { (name, groesse) -> datei(context, name).length() == groesse }
    /** Bereits geladene Bytes (fertige Dateien + angefangene Teile). */
    fun bytesDa(context: Context): Long = DATEIEN.keys.sumOf { name ->
        datei(context, name).takeIf { it.isFile }?.length() ?: teil(context, name).length()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("diktat", Context.MODE_PRIVATE)
    /** Hat der Nutzer schon einmal zwischen Standard und Premium gewählt? */
    fun auswahlGetroffen(context: Context) = prefs(context).getBoolean("auswahl", false)
    /** Premium gewünscht (wirksam erst, wenn geladen). */
    fun gewuenscht(context: Context) = prefs(context).getBoolean("turbo", false)
    /** Premium wird wirklich benutzt. */
    fun aktiv(context: Context) = gewuenscht(context) && geladen(context)
    fun waehlen(context: Context, turbo: Boolean) { prefs(context).edit().putBoolean("turbo", turbo).putBoolean("auswahl", true).apply() }

    fun starteDownload(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(ARBEIT, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TurboDownload>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build())
    }

    fun abbrechen(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ARBEIT)
        DATEIEN.keys.forEach { teil(context, it).delete() }
    }

    fun loeschen(context: Context) {
        abbrechen(context)
        ordner(context).deleteRecursively()
        prefs(context).edit().putBoolean("turbo", false).apply()
        WhisperErkenner.freigeben()
    }

    /** Läuft oder wartet der Download (z. B. auf Netz)? */
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
    }
}

/** Hintergrund-Download mit Fortschritts-Benachrichtigung; Fehler und Abbrüche setzen beim nächsten Versuch fort. */
class TurboDownload(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val kanal = "downloads"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Vordergrund hält den Download auch bei geschlossener App am Leben; scheitert das (Hintergrundstart-Sperre),
        // läuft er trotzdem und setzt nach einer Unterbrechung fort.
        runCatching { setForeground(info(TurboModell.bytesDa(applicationContext))) }
        try {
            TurboModell.ladeAlles(applicationContext, fortschritt = { bytes ->
                runCatching { setProgressAsync(workDataOf("bytes" to bytes)) }
                runCatching { applicationContext.getSystemService(NotificationManager::class.java)?.notify(ID, info(bytes).notification) }
            }, abgebrochen = { isStopped })
            ensureActive()
            if (TurboModell.geladen(applicationContext)) {
                // Wer Premium gewünscht hat, bekommt es jetzt automatisch.
                applicationContext.getSystemService(NotificationManager::class.java)?.cancel(ID)
                Result.success()
            } else Result.retry()
        } catch (e: IOException) {
            android.util.Log.w("WeckerDiktat", "Turbo-Download unterbrochen, wird fortgesetzt", e)
            if (runAttemptCount < 50) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = info(TurboModell.bytesDa(applicationContext))

    private fun info(bytes: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(kanal) == null)
            manager?.createNotificationChannel(NotificationChannel(kanal, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val prozent = (bytes * 100 / TurboModell.GESAMT).toInt().coerceIn(0, 100)
        val n = NotificationCompat.Builder(applicationContext, kanal)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Premium-Erkennung wird geladen")
            .setContentText("${bytes / 1_000_000} von ${TurboModell.GESAMT / 1_000_000} MB")
            .setProgress(100, prozent, false)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(ID, n)
    }

    companion object { private const val ID = 4711 }
}
