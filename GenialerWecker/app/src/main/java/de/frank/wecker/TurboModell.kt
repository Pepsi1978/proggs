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
 * Die beiden Whisper-Modelle fürs Einsprechen (int8, sherpa-onnx-Export, MIT), beide nur per Download, damit die APK
 * klein bleibt: Standard = small (schnell), Premium = Large V3 Turbo (etwa halb so viele Fehler, auf dem PC gemessen
 * nur 1,2- bis 2-mal langsamer, 28.09.2026). Alle Sprachen der App. Die Dateien liegen in noBackupFilesDir.
 */
enum class ErkennungsModell(val kurz: String, val titel: String, val modellName: String, val nutzen: String, private val repo: String,
    val encoder: String, val decoder: String, val tokens: String, val dateien: Map<String, Long>) {
    STANDARD("small", "Standard", "Whisper Small", "Gute Erkennung, besonders schnell", "sherpa-onnx-whisper-small",
        "small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt",
        linkedMapOf("small-encoder.int8.onnx" to 112_442_483L, "small-decoder.int8.onnx" to 262_226_114L, "small-tokens.txt" to 816_730L)),
    PREMIUM("turbo", "Premium", "Whisper Large V3 Turbo", "Etwa halb so viele Fehler, versteht Namen und Fachwörter besser", "sherpa-onnx-whisper-turbo",
        "turbo-encoder.int8.onnx", "turbo-decoder.int8.onnx", "turbo-tokens.txt",
        linkedMapOf("turbo-encoder.int8.onnx" to 674_716_297L, "turbo-decoder.int8.onnx" to 361_080_764L, "turbo-tokens.txt" to 816_730L));

    val gesamt: Long get() = dateien.values.sum()
    /** Genaue Größe in MB, z. B. „375 MB“ bzw. „1.037 MB“. */
    val mb: String get() = "%,d MB".format(java.util.Locale.GERMANY, (gesamt + 500_000) / 1_000_000)
    val quelle: String get() = "https://huggingface.co/csukuangfj/$repo/resolve/main/"
    val arbeit: String get() = "whisper-$kurz-download"
}

object Erkennung {
    fun ordner(context: Context, m: ErkennungsModell) = File(context.noBackupFilesDir, "whisper-${m.kurz}")
    fun datei(context: Context, m: ErkennungsModell, name: String) = File(ordner(context, m), name)
    private fun teil(context: Context, m: ErkennungsModell, name: String) = File(ordner(context, m), "$name.part")

    /** Alle Dateien vollständig da (Länge exakt wie auf dem Server). */
    fun geladen(context: Context, m: ErkennungsModell): Boolean = m.dateien.all { (name, groesse) -> datei(context, m, name).length() == groesse }
    /** Bereits geladene Bytes (fertige Dateien + angefangene Teile). */
    fun bytesDa(context: Context, m: ErkennungsModell): Long = m.dateien.keys.sumOf { name ->
        datei(context, m, name).takeIf { it.isFile }?.length() ?: teil(context, m, name).length()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("diktat", Context.MODE_PRIVATE)
    /** Hat der Nutzer schon einmal gewählt? */
    fun auswahlGetroffen(context: Context) = prefs(context).getBoolean("auswahl", false)
    /** Gewünschtes Modell; frühere Versionen speicherten nur „turbo“ als Wahrheitswert. */
    fun gewuenscht(context: Context): ErkennungsModell = prefs(context).getString("modell", null)
        ?.let { k -> ErkennungsModell.entries.firstOrNull { it.kurz == k } }
        ?: if (prefs(context).getBoolean("turbo", false)) ErkennungsModell.PREMIUM else ErkennungsModell.STANDARD
    fun waehlen(context: Context, m: ErkennungsModell) { prefs(context).edit().putString("modell", m.kurz).putBoolean("auswahl", true).apply() }
    /** Das Modell, das wirklich erkennt: das gewünschte, sonst ein anderes geladenes; null = noch keins geladen. */
    fun aktiv(context: Context): ErkennungsModell? = gewuenscht(context).takeIf { geladen(context, it) }
        ?: ErkennungsModell.entries.firstOrNull { geladen(context, it) }

    fun starteDownload(context: Context, m: ErkennungsModell) {
        WorkManager.getInstance(context).enqueueUniqueWork(m.arbeit, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ModellDownload>()
                .setInputData(workDataOf("modell" to m.kurz))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build())
    }

    /** Hält den Download an; das Geladene bleibt, „Fortsetzen“ macht an derselben Stelle weiter. */
    fun pausieren(context: Context, m: ErkennungsModell) { WorkManager.getInstance(context).cancelUniqueWork(m.arbeit) }

    /** Genug Platz für den Rest plus Reserve? */
    fun platzReicht(context: Context, m: ErkennungsModell): Boolean =
        context.noBackupFilesDir.usableSpace > (m.gesamt - bytesDa(context, m)) + 200_000_000L

    fun loeschen(context: Context, m: ErkennungsModell) {
        pausieren(context, m)
        ordner(context, m).deleteRecursively()
        WhisperErkenner.freigeben()
    }

    fun laeuftFlow(context: Context, m: ErkennungsModell) = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(m.arbeit)

    internal fun ladeAlles(context: Context, m: ErkennungsModell, fortschritt: (Long) -> Unit, abgebrochen: () -> Boolean) {
        ordner(context, m).mkdirs()
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
        for ((name, groesse) in m.dateien) {
            val ziel = datei(context, m, name)
            if (ziel.length() == groesse) continue
            ziel.delete()
            val teil = teil(context, m, name)
            if (teil.length() > groesse) teil.delete()
            var start = teil.length()
            if (start < groesse) {
                val anfrage = Request.Builder().url(m.quelle + name).apply { if (start > 0) header("Range", "bytes=$start-") }.build()
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
                                if (start - zuletzt > 2_000_000) { zuletzt = start; fortschritt(bytesDa(context, m)) }
                            }
                        }
                    }
                }
            }
            if (teil.length() != groesse) throw IOException("$name unvollständig (${teil.length()} von $groesse)")
            if (!teil.renameTo(ziel)) throw IOException("$name ließ sich nicht speichern")
            fortschritt(bytesDa(context, m))
        }
    }
}

/** Hintergrund-Download mit Fortschritts-Benachrichtigung; Fehler und Abbrüche setzen beim nächsten Versuch fort. */
class ModellDownload(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val kanal = "downloads"
    private val modell = ErkennungsModell.entries.firstOrNull { it.kurz == inputData.getString("modell") } ?: ErkennungsModell.STANDARD
    private val id = if (modell == ErkennungsModell.PREMIUM) 4711 else 4710

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Vordergrund hält den Download auch bei geschlossener App am Leben; scheitert das (Hintergrundstart-Sperre),
        // läuft er trotzdem und setzt nach einer Unterbrechung fort.
        runCatching { setForeground(info(Erkennung.bytesDa(applicationContext, modell))) }
        try {
            Erkennung.ladeAlles(applicationContext, modell, fortschritt = { bytes ->
                runCatching { applicationContext.getSystemService(NotificationManager::class.java)?.notify(id, info(bytes).notification) }
            }, abgebrochen = { isStopped })
            ensureActive()
            if (Erkennung.geladen(applicationContext, modell)) {
                applicationContext.getSystemService(NotificationManager::class.java)?.cancel(id)
                Result.success()
            } else Result.retry()
        } catch (e: IOException) {
            android.util.Log.w("WeckerDiktat", "Download ${modell.modellName} unterbrochen, wird fortgesetzt", e)
            if (runAttemptCount < 50) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = info(Erkennung.bytesDa(applicationContext, modell))

    private fun info(bytes: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(kanal) == null)
            manager?.createNotificationChannel(NotificationChannel(kanal, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val prozent = (bytes * 100 / modell.gesamt).toInt().coerceIn(0, 100)
        val n = NotificationCompat.Builder(applicationContext, kanal)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Spracherkennung ${modell.titel} wird geladen")
            .setContentText("${bytes / 1_000_000} von ${modell.gesamt / 1_000_000} MB")
            .setProgress(100, prozent, false)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(id, n)
    }
}
