package de.frank.experimente.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.frank.experimente.data.settings.Einstellungen
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * F-25 — die zwei Erinnerungen, morgens und abends.
 *
 * Über `AlarmManager` mit exakten Weckzeiten; nach einem Neustart des Geräts setzt
 * [BootReceiver] sie neu. Sonst meldet sich die App **nie** von selbst.
 */
object Erinnerungen {

    const val KANAL = "erinnerungen"
    const val MORGENS = 1001
    const val ABENDS = 1002
    const val EXTRA_ART = "art"

    fun legeKanalAn(ctx: Context) {
        val kanal = NotificationChannel(
            KANAL,
            "Erinnerungen",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morgens nach der Lage, abends nach dem Verlauf."
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }

    /** Setzt beide Weckzeiten neu — nach jeder Änderung in B-08 und nach einem Neustart. */
    fun setzeAlle(ctx: Context, einstellungen: Einstellungen) {
        setze(ctx, MORGENS, einstellungen.erinnerungMorgensAn, einstellungen.erinnerungMorgensZeit)
        setze(ctx, ABENDS, einstellungen.erinnerungAbendsAn, einstellungen.erinnerungAbendsZeit)
    }

    private fun setze(ctx: Context, kennung: Int, an: Boolean, zeit: String) {
        val wecker = ctx.getSystemService(AlarmManager::class.java)
        val absicht = PendingIntent.getBroadcast(
            ctx,
            kennung,
            Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_ART, kennung),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        wecker.cancel(absicht)
        if (!an) return

        val naechste = naechsterZeitpunkt(zeit)
        // Ab Android 12 darf ohne Zusage keine exakte Weckzeit gesetzt werden — dann lieber
        // ungenau wecken als die Erinnerung ganz ausfallen zu lassen.
        val darfExakt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || wecker.canScheduleExactAlarms()
        if (darfExakt) {
            wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, naechste, absicht)
        } else {
            wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, naechste, absicht)
        }
    }

    /** Heute, wenn die Uhrzeit noch kommt — sonst morgen. */
    internal fun naechsterZeitpunkt(zeit: String, jetzt: LocalDateTime = LocalDateTime.now()): Long {
        val uhr = runCatching { LocalTime.parse(zeit) }.getOrElse { LocalTime.of(8, 0) }
        var ziel = LocalDateTime.of(jetzt.toLocalDate(), uhr)
        if (!ziel.isAfter(jetzt)) ziel = LocalDateTime.of(jetzt.toLocalDate().plusDays(1), uhr)
        return ziel.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun heute(): LocalDate = LocalDate.now()
}
