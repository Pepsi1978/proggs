package de.frank.perfectmoment.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import de.frank.perfectmoment.MainActivity
import de.frank.perfectmoment.R

object SessionNotification {
    const val CHANNEL_ID = "perfect_moment_session"
    const val NOTIFICATION_ID = 1978

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_session),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Steuerung der laufenden Perfect-Moment-Sitzung"
                setShowBadge(false)
            },
        )
    }

    fun build(
        context: Context,
        mediaSession: MediaSession,
        runtime: SessionRuntime?,
        state: SessionState?,
    ): Notification {
        val isPlaying = state?.speakerOn == true && state.paused != true
        val toggleIntent = serviceIntent(context, SessionForegroundService.ACTION_PAUSE_RESUME, 1)
        val stopIntent = serviceIntent(context, SessionForegroundService.ACTION_STOP, 2)
        val openIntent = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val publicVersion = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Perfect Moment")
            .setContentText("Sitzung läuft")
            .build()

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Perfect Moment")
            .setContentText(runtime?.topic ?: "Sitzung wird vorbereitet")
            .setContentIntent(openIntent)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOngoing(state?.phase != Phase.ENDED)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Weiter",
                toggleIntent,
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stopp", stopIntent)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int) =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, SessionForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
