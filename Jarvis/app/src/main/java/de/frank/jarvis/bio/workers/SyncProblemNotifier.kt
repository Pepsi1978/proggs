package de.frank.jarvis.bio.workers

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
import de.frank.jarvis.bio.data.diagnostics.Diag
import de.frank.jarvis.bio.data.diagnostics.DiagnosticArea

/**
 * Meldet Frank, wenn eine Biomarker-Quelle nur durch SEIN Eingreifen wieder Daten liefert (Whoop-Anmeldung abgelaufen).
 * Ohne Erlaubnis für Benachrichtigungen bleibt der Hinweis im Biomarker-Bereich und in jeder Antwort von Jarvis.
 */
class SyncProblemNotifier(context: Context) {
    private val context = context.applicationContext

    fun whoopNeuAnmelden() = zeige(
        ID_WHOOP,
        "Whoop: bitte neu anmelden",
        "Whoop liefert keine neuen Daten mehr, weil die Anmeldung abgelaufen ist. Tippen, dann in Jarvis unter Biomarker → Einstellungen bei Whoop neu anmelden.",
    )

    fun whoopWiederOk() {
        runCatching { NotificationManagerCompat.from(context).cancel(ID_WHOOP) }
    }

    private fun zeige(id: Int, titel: String, text: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Diag.w(DiagnosticArea.APP, TAG, "Benachrichtigung nicht erlaubt — Hinweis nur in der App: $titel")
                return
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Biomarker-Abruf", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Wenn Whoop, Oura oder eine andere Quelle keine Daten mehr liefert und du eingreifen musst."
                    },
                )
            }
            val oeffnen = Intent(context, de.frank.jarvis.MainActivity::class.java).apply {
                putExtra(de.frank.jarvis.bio.BioModul.EXTRA_OEFFNEN, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(titel)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setSilent(de.frank.jarvis.speech.GespraechsFokus.stilleHinweise)
                .setContentIntent(PendingIntent.getActivity(context, id, oeffnen, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }.onFailure { Diag.w(DiagnosticArea.APP, TAG, "Benachrichtigung fehlgeschlagen: $titel", it) }
    }

    private companion object {
        const val TAG = "SyncProblemNotifier"
        const val CHANNEL_ID = "bio_abruf_probleme"
        const val ID_WHOOP = 7301
    }
}
