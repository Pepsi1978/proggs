package de.frank.experimente.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.frank.experimente.ExperimenteApp
import de.frank.experimente.MainActivity
import de.frank.experimente.R

/**
 * Zeigt die Erinnerung an und setzt den Wecker für den nächsten Tag neu.
 * Der Text ist der aus 02-UI-SPEC.md §8.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val art = intent.getStringExtra(Erinnerungen.EXTRA_ART) ?: Erinnerungen.ART_MORGENS
        val text = if (art == Erinnerungen.ART_ABENDS) {
            "Wie ist es gelaufen?"
        } else {
            "Wie ist deine Lage heute?"
        }

        val darfMelden = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (darfMelden) {
            Erinnerungen.kanalAnlegen(context)
            val oeffnen = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val meldung = NotificationCompat.Builder(context, Erinnerungen.KANAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Experimente")
                .setContentText(text)
                .setContentIntent(oeffnen)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java)
                ?.notify(if (art == Erinnerungen.ART_ABENDS) 2 else 1, meldung)
        }

        // Für morgen neu setzen — ein Wecker feuert nur einmal.
        val app = context.applicationContext as? ExperimenteApp ?: return
        Erinnerungen.allesNeuSetzen(context, app.einstellungen)
    }
}
