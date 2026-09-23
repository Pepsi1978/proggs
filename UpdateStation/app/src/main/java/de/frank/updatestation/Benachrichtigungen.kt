package de.frank.updatestation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Benachrichtigungen {
    private const val KANAL_UPDATES = "updates"
    private const val KANAL_STATUS = "status"
    private const val GRUPPE = "de.frank.updatestation.UPDATES"
    const val EXTRA_INSTALLIERE = "installiere"

    fun kanaeleAnlegen(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL_UPDATES, "Neue Updates", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Meldet, wenn für eine App eine neuere Version bereitliegt."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(KANAL_STATUS, "Installation & Anmeldung", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun darf(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun oeffneApp(context: Context, code: Int, installiere: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { installiere?.let { putExtra(EXTRA_INSTALLIERE, it) } }
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Meldet jedes Update genau einmal pro versionCode: "Update für App X gefunden". */
    fun meldeNeue(context: Context, eintraege: List<AppEintrag>) {
        if (!darf(context)) return
        val einst = Einstellungen(context)
        val nm = NotificationManagerCompat.from(context)
        eintraege.filter { it.status == Status.UPDATE && einst.gemeldet(it.paket) < it.fund.manifest.versionCode }.forEach { e ->
            val m = e.fund.manifest
            val id = e.paket.hashCode()
            val n = NotificationCompat.Builder(context, KANAL_UPDATES)
                .setSmallIcon(R.drawable.ic_launcher_vordergrund)
                .setColor(0xFF5B4BFF.toInt())
                .setContentTitle("Update für ${e.label} gefunden")
                .setContentText("${e.installiertName} → ${m.versionName} · Tippen zum Ansehen")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Installiert: ${e.installiertName} (Build ${e.installiertCode})\n" +
                            "Neu: ${m.versionName} (Build ${m.versionCode})",
                    ),
                )
                .setContentIntent(oeffneApp(context, id))
                .addAction(0, "Jetzt installieren", oeffneApp(context, id + 1, e.paket))
                .setGroup(GRUPPE)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission") nm.notify(id, n)
            einst.setzeGemeldet(e.paket, m.versionCode)
        }
    }

    fun entferneUpdate(context: Context, paket: String) =
        NotificationManagerCompat.from(context).cancel(paket.hashCode())

    fun installiert(context: Context, paket: String, label: String, version: String) {
        if (!darf(context)) return
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle("$label aktualisiert")
            .setContentText("Jetzt auf Version $version.")
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") NotificationManagerCompat.from(context).notify(paket.hashCode() + 7, n)
    }

    fun bestaetigen(context: Context, paket: String, label: String, intent: Intent) {
        if (!darf(context)) return
        val pi = PendingIntent.getActivity(context, paket.hashCode() + 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle("$label: Installation bestätigen")
            .setContentText("Tippen, um das Update zu installieren.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") NotificationManagerCompat.from(context).notify(paket.hashCode() + 5, n)
    }

    fun anmeldung(context: Context) {
        if (!darf(context)) return
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle("Google Drive neu verbinden")
            .setContentText("UpdateStation kann nicht mehr nach Updates suchen. Tippen zum Verbinden.")
            .setContentIntent(oeffneApp(context, 42))
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") NotificationManagerCompat.from(context).notify(42, n)
    }
}
