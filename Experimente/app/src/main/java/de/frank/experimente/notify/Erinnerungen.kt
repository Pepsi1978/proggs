package de.frank.experimente.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.frank.experimente.data.settings.Einstellungen
import java.util.Calendar

/**
 * F-25 — Erinnerungen morgens und abends.
 *
 * Zwei Wecker, beide einzeln abschaltbar, Zeiten aus den Einstellungen. Sonst meldet sich
 * die App **nie** von selbst. Nach einem Neustart des Geräts werden sie neu gesetzt
 * (siehe [BootReceiver]).
 */
object Erinnerungen {

    const val KANAL = "erinnerungen"
    const val EXTRA_ART = "art"
    const val ART_MORGENS = "morgens"
    const val ART_ABENDS = "abends"

    private const val CODE_MORGENS = 1001
    private const val CODE_ABENDS = 1002

    fun kanalAnlegen(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val kanal = NotificationChannel(
            KANAL,
            "Erinnerungen",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morgens nach deiner Lage, abends nach dem Verlauf."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }

    /** Setzt beide Wecker gemäß den Einstellungen neu — abgeschaltete werden gelöscht. */
    fun allesNeuSetzen(context: Context, einstellungen: Einstellungen) {
        kanalAnlegen(context)
        setzen(
            context, ART_MORGENS, CODE_MORGENS,
            einstellungen.erinnerungMorgensAn, einstellungen.erinnerungMorgensZeit,
        )
        setzen(
            context, ART_ABENDS, CODE_ABENDS,
            einstellungen.erinnerungAbendsAn, einstellungen.erinnerungAbendsZeit,
        )
    }

    private fun setzen(context: Context, art: String, code: Int, an: Boolean, zeit: String) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val absicht = PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ART, art),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (!an) {
            manager.cancel(absicht)
            return
        }

        val (stunde, minute) = zeit.split(":").let {
            (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        val naechster = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, stunde)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        // Exakte Weckzeit, wo erlaubt — sonst der ungefähre Weg, damit die Erinnerung
        // trotzdem kommt statt ganz auszufallen.
        val darfExakt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (darfExakt) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, naechster.timeInMillis, absicht,
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, naechster.timeInMillis, absicht,
            )
        }
    }
}
