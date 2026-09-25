package de.frank.newskompass.news

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.frank.newskompass.MainActivity
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.ai.CodexFehler
import de.frank.newskompass.ai.CodexFehlerArt
import de.frank.newskompass.observability.KompassLog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Zweimal am Tag: 5 Uhr und 17 Uhr.
 *
 * Die Uhrzeit hält ein einziger AlarmManager-Wecker, der sich nach jedem Klingeln neu stellt
 * (Almanach A1/A10: One-shot plus Neuplanung, immer derselbe PendingIntent). Die eigentliche
 * Arbeit macht WorkManager — mit Netzbedingung, Wiederholung und Vordergrund-Hinweis.
 */
object Zeitplan {

    val UHRZEITEN: List<LocalTime> = listOf(LocalTime.of(5, 0), LocalTime.of(17, 0))
    const val LAUF = "news-lauf"
    private const val KANAL_LAUF = "lauf"
    private const val KANAL_FERTIG = "fertig"
    const val HINWEIS_LAUF = 17
    private const val HINWEIS_FERTIG = 18

    /** Nächster Termin nach [jetzt], frisch aus der Zeitzone gerechnet (Almanach A6). */
    fun naechsterTermin(jetzt: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        val heute = LocalDate.now(zone)
        return (0..1).asSequence()
            .flatMap { tag -> UHRZEITEN.asSequence().map { ZonedDateTime.of(heute.plusDays(tag.toLong()), it, zone) } }
            .first { it.isAfter(jetzt.plusSeconds(30)) }
    }

    /** Letzter Termin, der schon vorbei ist — für das Nachholen beim App-Start. */
    fun letzterTermin(jetzt: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        val heute = LocalDate.now(zone)
        return (0..1).asSequence()
            .flatMap { tag -> UHRZEITEN.reversed().asSequence().map { ZonedDateTime.of(heute.minusDays(tag.toLong()), it, zone) } }
            .first { !it.isAfter(jetzt) }
    }

    fun plane(context: Context) {
        val app = context.applicationContext as NewsApplication
        val wecker = context.getSystemService(AlarmManager::class.java) ?: return
        val absicht = weckerAbsicht(context)
        if (!app.einstellungen.stand.value.zeitplanAktiv) {
            wecker.cancel(absicht)
            return
        }
        val zeit = naechsterTermin().toInstant().toEpochMilli()
        val genau = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || wecker.canScheduleExactAlarms()
        try {
            if (genau) {
                wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
            } else {
                wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
            }
        } catch (fehler: SecurityException) {
            wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
        }
        KompassLog.info("Zeitplan", "plane", "Nächster Lauf geplant", mapOf("um" to naechsterTermin().toString(), "genau" to genau))
    }

    private fun weckerAbsicht(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        4711,
        Intent(context, ZeitplanEmpfaenger::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Startet einen Lauf. Läuft schon einer, bleibt es bei dem. */
    fun starteLauf(context: Context, manuell: Boolean) {
        val auftrag = OneTimeWorkRequestBuilder<NewsWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .setInputData(workDataOf("manuell" to manuell))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(LAUF, ExistingWorkPolicy.KEEP, auftrag)
    }

    fun legeKanaeleAn(context: Context) {
        val verwaltung = context.getSystemService(NotificationManager::class.java) ?: return
        verwaltung.createNotificationChannel(
            NotificationChannel(KANAL_LAUF, "Nachrichten werden geladen", NotificationManager.IMPORTANCE_LOW),
        )
        verwaltung.createNotificationChannel(
            NotificationChannel(KANAL_FERTIG, "Neue Ausgabe", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun oeffneApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun laufHinweis(context: Context, text: String) = NotificationCompat.Builder(context, KANAL_LAUF)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle("News Kompass recherchiert")
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(oeffneApp(context))
        .build()

    fun meldeFertig(context: Context, titel: String, zeilen: List<String>) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val hinweis = NotificationCompat.Builder(context, KANAL_FERTIG)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(titel)
            .setContentText(zeilen.firstOrNull().orEmpty())
            .setStyle(NotificationCompat.InboxStyle().also { stil -> zeilen.take(6).forEach(stil::addLine) })
            .setAutoCancel(true)
            .setContentIntent(oeffneApp(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(HINWEIS_FERTIG, hinweis) }
    }
}

/** Der Wecker klingelt: Lauf anstoßen, nächsten Termin stellen. */
class ZeitplanEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KompassLog.info("Zeitplan", "onReceive", "Wecker ausgelöst")
        Zeitplan.starteLauf(context, manuell = false)
        Zeitplan.plane(context)
    }
}

/** Nach Neustart, App-Update oder geänderter Wecker-Erlaubnis den Termin neu stellen (Almanach B1). */
class NeustartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Zeitplan.plane(context)
    }
}

class NewsWorker(context: Context, parameter: WorkerParameters) : CoroutineWorker(context, parameter) {

    override suspend fun getForegroundInfo(): ForegroundInfo = vordergrund("Die Nachrichten werden zusammengestellt …")

    private fun vordergrund(text: String): ForegroundInfo {
        val hinweis = Zeitplan.laufHinweis(applicationContext, text)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(Zeitplan.HINWEIS_LAUF, hinweis, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(Zeitplan.HINWEIS_LAUF, hinweis)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as NewsApplication
        // Eine Recherche dauert Minuten — als Vordergrundarbeit überlebt sie die 10-Minuten-Grenze.
        runCatching { setForeground(vordergrund("Die Nachrichten werden zusammengestellt …")) }
            .onFailure { KompassLog.warn("NewsWorker", "doWork", "Kein Vordergrund möglich", mapOf("grund" to it.message)) }
        return try {
            val ausgabe = app.recherche.laufe { stand ->
                setProgress(workDataOf("text" to stand.text, "anteil" to stand.anteil))
                runCatching { setForeground(vordergrund(stand.text)) }
            }
            val zeilen = ausgabe.bloecke.flatMap { b -> b.meldungen.take(2).map { "${b.titel}: ${it.titel}" } }
            Zeitplan.meldeFertig(applicationContext, "Deine ${ausgabe.slot} ist da", zeilen)
            Result.success()
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("NewsWorker", "doWork", "Lauf gescheitert", mapOf("grund" to fehler.message, "versuch" to runAttemptCount))
            if ((fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) || runAttemptCount >= 2) {
                Result.failure(workDataOf("fehler" to (fehler.message ?: "Unbekannter Fehler")))
            } else {
                Result.retry()
            }
        }
    }
}
