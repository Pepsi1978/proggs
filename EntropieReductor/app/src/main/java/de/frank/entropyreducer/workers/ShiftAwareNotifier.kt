package de.frank.entropyreducer.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.R
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.notifications.AppNotification
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper um den NotificationManager, der vor jeder Notification prueft ob die
 * aktuelle Zeit im Schlaffenster für den heutigen CalendarDay liegt. Wenn ja,
 * wird die Notification verzoegert bis zum Wachzeitpunkt + 15 Min.
 *
 * Spec §16.4. Default-Schlaffenster bei UNBEKANNT: 22:00-06:00.
 */
@Singleton
class ShiftAwareNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarDao: CalendarDayDao,
    private val settings: AppSettings,
) {

    init { ensureChannel() }

    /**
     * Sendet eine Notification — falls aktuell Schlaffenster, wird sie als WorkManager-Job
     * mit setInitialDelay bis zum Wachzeitpunkt+15min geplant.
     *
     * Frank-Wunsch 2026-08-12: Vorher wird der Benutzer-Schalter aus
     * "Einstellungen -> Benachrichtigungen" geprueft. Ist [kind] dort abgeschaltet,
     * wird NICHTS gesendet — die dahinterliegende Funktion (Briefing erzeugen,
     * Trigger feuern, Frage speichern) laeuft unveraendert weiter, nur die
     * Benachrichtigung entfaellt.
     *
     * Liefert true wenn sofort gesendet wurde, false wenn abgeschaltet oder verzoegert.
     */
    suspend fun postOrDelay(
        kind: AppNotification,
        notificationId: Int,
        title: String,
        body: String,
        deepLink: String? = null,
    ): Boolean {
        if (!settings.isNotificationEnabled(kind.key)) {
            Diag.i(
                DiagnosticArea.AGENTIC,
                TAG,
                "Benachrichtigung '${kind.title}' ist in den Einstellungen abgeschaltet — nicht gesendet.",
            )
            return false
        }

        val now = LocalDateTime.now(ZoneId.systemDefault())
        val today = now.toLocalDate()
        val day = calendarDao.getDay(today.toString()).first()

        val sleepStart = parseTime(day?.sleepWindowStart) ?: LocalTime.of(22, 0)
        val sleepEnd = parseTime(day?.sleepWindowEnd) ?: LocalTime.of(6, 0)

        val withinSleep = isWithinSleepWindow(now.toLocalTime(), sleepStart, sleepEnd)
        return if (withinSleep) {
            // Notification wird in Stufe 2 (MVP) verworfen statt verzoegert — Stufe 4
            // baut die WorkManager-Verzoegerung mit Deep-Link auf einen Worker auf.
            // Aktueller Verhalten: einfach nicht senden, damit Frank im Schlaf nicht gestoert wird.
            false
        } else {
            postImmediate(notificationId, title, body, deepLink)
            true
        }
    }

    private fun postImmediate(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: String?,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            deepLink?.let { putExtra(EXTRA_DEEP_LINK, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Frage des Moments",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Kontextrelevante Fragen der KI — bewusst zurückhaltend dosiert."
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun parseTime(value: String?): LocalTime? = value?.let {
        runCatching { LocalTime.parse(it) }.getOrNull()
    }

    /**
     * Pruft ob [now] im Fenster zwischen [start] und [end] liegt — auch wenn das Fenster
     * über Mitternacht geht (z.B. Tagdienst Schlaf 21:00 - 04:00).
     */
    private fun isWithinSleepWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            now in start..end
        } else {
            // über Mitternacht
            !now.isBefore(start) || !now.isAfter(end)
        }
    }

    companion object {
        private const val TAG = "ShiftAwareNotifier"
        private const val CHANNEL_ID = AppNotification.CHANNEL_KI
        const val EXTRA_DEEP_LINK = "deep_link"
    }
}
