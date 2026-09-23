package de.frank.updatestation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
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

    /**
     * Kann eine Benachrichtigung in [kanal] wirklich erscheinen? Recht erteilt, App-Benachrichtigungen
     * an und der Kanal nicht auf "keine" gestellt.
     */
    fun zustellbar(context: Context, kanal: String = KANAL_UPDATES): Boolean {
        if (!darf(context)) return false
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return false
        val k = nm.getNotificationChannel(kanal) ?: return false
        return k.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Zeigt [n] nur, wenn zustellbar; true nur nach erfolgreichem notify (Grundlage für jedes Gemeldet-Flag). */
    private fun zeige(context: Context, id: Int, n: Notification): Boolean {
        if (!zustellbar(context, n.channelId)) return false
        return runCatching {
            @Suppress("MissingPermission") NotificationManagerCompat.from(context).notify(id, n)
        }.onFailure { Log.w(TAG, "Benachrichtigung nicht zustellbar: ${it.javaClass.simpleName}") }.isSuccess
    }

    private fun oeffneApp(context: Context, code: Int, installiere: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { installiere?.let { putExtra(EXTRA_INSTALLIERE, it) } }
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Meldet jedes Update und jede neue App genau einmal pro versionCode. Installiert wird nie
     * automatisch – erst nach Tippen des Nutzers und Android-Bestätigung.
     */
    fun meldeNeue(context: Context, eintraege: List<AppEintrag>) {
        if (!darf(context)) return
        val einst = Einstellungen(context)
        val nm = NotificationManagerCompat.from(context)
        eintraege.filter { einst.gemeldet(it.paket) < it.fund.manifest.versionCode }.forEach { e ->
            val gezeigt = when {
                e.status == Status.UPDATE -> meldeUpdate(context, nm, e)
                // Neue App: erst melden, wenn ihre APK da ist; ohne Installieren-Aktion, Tippen öffnet die App.
                e.status == Status.NICHT_INSTALLIERT && e.fund.apkRef != null -> meldeNeueApp(context, nm, e)
                else -> return@forEach
            }
            // Nur als gemeldet merken, was wirklich erschienen ist – sonst später erneut versuchen.
            if (!gezeigt) return@forEach
            einst.setzeGemeldet(e.paket, e.fund.manifest.versionCode)
            Diagnose.ereignis(context, Phase.BENACHRICHTIGUNG, if (e.status == Status.UPDATE) "UPDATE" else "NEUE_APP",
                "paket" to e.paket, "vc" to e.fund.manifest.versionCode)
        }
    }

    /** Einmalige Warnung (Status-Kanal); false, wenn sie nicht gezeigt werden durfte. */
    fun warnung(context: Context, id: Int, titel: String, text: String): Boolean {
        if (!darf(context)) return false
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle(titel)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(oeffneApp(context, id))
            .setAutoCancel(true)
            .build()
        return zeige(context, id, n)
    }

    private fun meldeNeueApp(context: Context, nm: NotificationManagerCompat, e: AppEintrag): Boolean {
        val m = e.fund.manifest
        val id = e.paket.hashCode()
        val n = NotificationCompat.Builder(context, KANAL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setColor(0xFF5B4BFF.toInt())
            .setContentTitle("Neue App verfügbar: ${e.label}")
            .setContentText("Version ${m.versionName} · Tippen zum Ansehen und Installieren")
            .setContentIntent(oeffneApp(context, id))
            .setGroup(GRUPPE)
            .setAutoCancel(true)
            .build()
        return zeige(context, id, n)
    }

    private fun meldeUpdate(context: Context, nm: NotificationManagerCompat, e: AppEintrag): Boolean {
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
        return zeige(context, id, n)
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
        zeige(context, paket.hashCode() + 7, n)
    }

    /** Liefert false, wenn die Benachrichtigung nicht gezeigt werden darf. */
    fun bestaetigen(context: Context, paket: String, label: String, intent: Intent): Boolean {
        if (!darf(context)) return false
        val pi = PendingIntent.getActivity(context, paket.hashCode() + 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle("$label: Installation bestätigen")
            .setContentText("Tippen, um das Update zu installieren.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        return zeige(context, paket.hashCode() + 5, n)
    }

    fun entferneBestaetigung(context: Context, paket: String) =
        NotificationManagerCompat.from(context).cancel(paket.hashCode() + 5)

    fun fehler(context: Context, paket: String, label: String, text: String) {
        if (!darf(context)) return
        val id = paket.hashCode() + 9
        val n = NotificationCompat.Builder(context, KANAL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_vordergrund)
            .setContentTitle("$label: Update nicht installiert")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(oeffneApp(context, id))
            .setAutoCancel(true)
            .build()
        zeige(context, id, n)
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
        zeige(context, 42, n)
    }
}
