package de.frank.updatestation

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Vor jeder Jarvis-Installation fragt Jarvis' eigenes Overlay nach dem Zeitpunkt (Sofort oder in 1 bis 5 Minuten, ohne
 * Antwort nach 30 Sekunden), dasselbe wie bei Werkzeuge/jarvis-install. Die Antwort kommt als Broadcast, ein Wecker löst
 * die Installation auch aus, wenn UpdateStation längst im Hintergrund oder beendet ist.
 */
object Aufschub {
    const val PAKET_JARVIS = "de.frank.jarvis"
    private const val FRAGE_DIENST = "de.frank.jarvis.ui.InstallFrageBruecke"
    private const val EXTRA_PAKET = "paket"
    private const val EXTRA_INSTALLIEREN = "installieren"
    private const val EXTRA_WERT = "wert"
    // Wie im Skript: Bis zur Meldung „offen“ gilt die Meldefrist, danach die Frist des Overlays plus Reserve.
    private const val MELDEFRIST_MS = 20_000L
    private const val WARTEZEIT_MS = 33_000L

    enum class Frage { GESTELLT, OHNE_OVERLAY, FEHLER }

    /** Öffnet das Overlay. OHNE_OVERLAY: Jarvis fehlt oder kennt die Frage noch nicht, dann geht es nur ohne sie. */
    fun frage(context: Context, paket: String): Frage {
        val antwort = PendingIntent.getBroadcast(
            context, paket.hashCode(),
            Intent(context, AntwortEmpfaenger::class.java).putExtra(EXTRA_PAKET, paket),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val dienst = Intent().setClassName(paket, FRAGE_DIENST)
            .putExtra("kennung", System.currentTimeMillis().toString())
            .putExtra("antwort", antwort)
        val gestartet = try {
            context.startForegroundService(dienst)
        } catch (e: RuntimeException) {
            Log.w(TAG, "$paket: Installationsfrage ließ sich nicht öffnen", e)
            Diagnose.ereignis(context, Phase.INSTALLATION, "FRAGE_FEHLER", "paket" to paket, "klasse" to Diagnose.klasse(e))
            scheitere(context, paket)
            return Frage.FEHLER
        } ?: return Frage.OHNE_OVERLAY
        Log.i(TAG, "$paket: Installationsfrage gestellt ($gestartet)")
        ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufAntwort)
        stelleWecker(context, paket, MELDEFRIST_MS, installieren = false)
        return Frage.GESTELLT
    }

    private fun stelleWecker(context: Context, paket: String, inMs: Long, installieren: Boolean): Long {
        val um = System.currentTimeMillis() + inMs
        val wecker = context.getSystemService(AlarmManager::class.java)
        val pi = weckerIntent(context, paket, installieren)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || wecker.canScheduleExactAlarms()) {
            wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, um, pi)
        } else {
            wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, um, pi)
        }
        return um
    }

    private fun weckerAus(context: Context, paket: String) {
        context.getSystemService(AlarmManager::class.java).cancel(weckerIntent(context, paket, false))
    }

    private fun weckerIntent(context: Context, paket: String, installieren: Boolean): PendingIntent {
        val intent = Intent(context, AufschubEmpfaenger::class.java)
            .putExtra(EXTRA_PAKET, paket)
            .putExtra(EXTRA_INSTALLIEREN, installieren)
        return PendingIntent.getBroadcast(context, paket.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Ohne sichtbare Frage wird nicht installiert. */
    private fun scheitere(context: Context, paket: String) {
        val text = "Die Installationsfrage von Jarvis ließ sich nicht anzeigen, daher wird nicht installiert. Bitte erneut versuchen."
        ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(text))
        Benachrichtigungen.fehler(context, paket, "Jarvis", text)
    }

    private fun installiereJetzt(context: Context, paket: String) {
        val anfrage = OneTimeWorkRequestBuilder<AufschubWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(EXTRA_PAKET to paket))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("aufschub-$paket", ExistingWorkPolicy.KEEP, anfrage)
    }

    /** Antwort des Overlays: offen, fehler, frist oder die Minuten (0 = sofort). */
    class AntwortEmpfaenger : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val paket = intent.getStringExtra(EXTRA_PAKET) ?: return
            val wert = intent.getStringExtra(EXTRA_WERT)
            Diagnose.ereignis(context, Phase.INSTALLATION, "FRAGE_ANTWORT", "paket" to paket, "wert" to wert.orEmpty())
            val minuten = wert?.toIntOrNull()
            when {
                wert == "offen" -> stelleWecker(context, paket, WARTEZEIT_MS, installieren = true)
                wert == "frist" || minuten == 0 -> {
                    weckerAus(context, paket)
                    installiereJetzt(context, paket)
                }
                minuten != null && minuten in 1..5 -> {
                    val um = stelleWecker(context, paket, minuten * 60_000L, installieren = true)
                    ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Geplant(um))
                }
                else -> {
                    weckerAus(context, paket)
                    scheitere(context, paket)
                }
            }
        }
    }

    class AufschubEmpfaenger : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val paket = intent.getStringExtra(EXTRA_PAKET) ?: return
            if (intent.getBooleanExtra(EXTRA_INSTALLIEREN, false)) installiereJetzt(context, paket) else scheitere(context, paket)
        }
    }

    /** Lädt und übergibt im Worker, damit Android den Prozess bis zur Übergabe am Leben lässt. */
    class AufschubWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext
            val paket = inputData.getString(EXTRA_PAKET) ?: return Result.success()
            Pruefer.bewerteGespeichert(app)
            ZustandsSpeicher.setzeInstallation(paket, null)
            val eintrag = ZustandsSpeicher.zustand.value.eintraege
                .firstOrNull { it.paket == paket && (it.status == Status.UPDATE || it.status == Status.NICHT_INSTALLIERT) }
            if (eintrag == null) {
                Log.i(TAG, "$paket: verschobene Installation nicht mehr nötig")
                return Result.success()
            }
            if (!app.packageManager.canRequestPackageInstalls()) {
                val text = "Bitte zuerst „Unbekannte Apps installieren“ erlauben."
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(text, eintrag.fund.manifest.versionCode))
                Benachrichtigungen.fehler(app, paket, eintrag.label, text)
                return Result.success()
            }
            Installierer.starte(app, eintrag, freigegeben = true).await()
            return Result.success()
        }

        /** Nur unter Android 12 nötig: Dort läuft eilige Arbeit als Vordergrunddienst. */
        override suspend fun getForegroundInfo(): ForegroundInfo =
            ForegroundInfo(FOREGROUND_ID, Benachrichtigungen.verschobeneInstallation(applicationContext))
    }

    private const val FOREGROUND_ID = 45
}
