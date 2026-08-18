package de.frank.denknotiz.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.frank.denknotiz.MainActivity
import de.frank.denknotiz.R

class RecordingForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Sprachaufnahme", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentTitle("Denknotiz nimmt auf")
                .setContentText("Zum Beenden Denknotiz öffnen und auf Stopp tippen.")
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build(),
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "recording"
        private const val NOTIFICATION_ID = 4103

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, RecordingForegroundService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, RecordingForegroundService::class.java))
    }
}
