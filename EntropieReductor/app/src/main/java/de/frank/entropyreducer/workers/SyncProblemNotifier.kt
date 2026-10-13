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
import de.frank.entropyreducer.presentation.MainActivity
import de.frank.entropyreducer.presentation.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Meldet Frank, wenn eine Biomarker-Quelle nur durch SEIN Eingreifen wieder Daten liefert
 * (Whoop-Anmeldung abgelaufen). Vorgeschichte 2026-10-09: Whoop lehnte vier Tage lang jeden Abruf
 * ab, der Fehler stand nur im Diagnose-Protokoll — niemand hat es gemerkt.
 *
 * Schlägt die Benachrichtigung fehl (keine Erlaubnis), bleibt der Hinweis in der Fußzeile des
 * Biomarker-Reiters und in den Antworten an Jarvis: drei voneinander unabhängige Wege.
 */
@Singleton
class SyncProblemNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun whoopNeuAnmelden() = zeige(
        ID_WHOOP,
        "Whoop: bitte neu anmelden",
        "Whoop liefert keine neuen Daten mehr, weil die Anmeldung abgelaufen ist. Tippen, dann unter API-Schlüssel bei Whoop neu anmelden.",
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
                    NotificationChannel(CHANNEL_ID, "Abgleich-Probleme", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Wenn Whoop, Oura oder eine andere Quelle keine Daten mehr liefert und du eingreifen musst."
                    },
                )
            }
            val oeffnen = Intent(context, MainActivity::class.java).apply {
                putExtra(ShiftAwareNotifier.EXTRA_DEEP_LINK, Routes.SETTINGS_API)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(titel)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(context, id, oeffnen, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                )
                .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }.onFailure { Diag.w(DiagnosticArea.APP, TAG, "Benachrichtigung fehlgeschlagen: $titel", it) }
    }

    private companion object {
        const val TAG = "SyncProblemNotifier"
        const val CHANNEL_ID = "sync_problems"
        const val ID_WHOOP = 7301
    }
}
