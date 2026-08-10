package de.frank.experimente.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import de.frank.experimente.MainActivity
import de.frank.experimente.R
import de.frank.experimente.data.settings.Einstellungen

/**
 * Zeigt die fällige Erinnerung und setzt die Weckzeit für den nächsten Tag.
 *
 * Antippen öffnet B-01 im passenden Zustand (F-25 Schritt 3).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, absicht: Intent) {
        val art = absicht.getIntExtra(Erinnerungen.EXTRA_ART, Erinnerungen.MORGENS)
        val abends = art == Erinnerungen.ABENDS

        val erlaubt = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (erlaubt) {
            Erinnerungen.legeKanalAn(ctx)
            val oeffnen = PendingIntent.getActivity(
                ctx,
                art,
                Intent(ctx, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_ABEND, abends)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val meldung = Notification.Builder(ctx, Erinnerungen.KANAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (abends) "Wie ist es gelaufen?" else "Wie ist deine Lage heute?")
                .setContentIntent(oeffnen)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java).notify(art, meldung)
        }

        // Die nächste Weckzeit setzen — auch wenn die Meldung diesmal nicht gezeigt werden
        // durfte, damit die Reihe nicht abreisst.
        Erinnerungen.setzeAlle(ctx, Einstellungen(ctx))
    }
}
