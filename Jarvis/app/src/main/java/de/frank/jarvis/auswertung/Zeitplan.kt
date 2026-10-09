package de.frank.jarvis.auswertung

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Die Tagesauswertungs-Synchronisation: Jarvis schreibt die Tagesauswertung zu jeder vollen Stunde neu (oder im
 * eingestellten Abstand). Solange Frank laut Dienstplan schläft, ruht sie; die erste volle Stunde nach dem Schlaf
 * läuft immer. Es ist immer genau ein Wecker gestellt: der nächste. Nach jedem Lauf, beim Start der App, nach dem
 * Einschalten des Handys und nach einer Zeit- oder Zeitzonenänderung wird neu gestellt. Der Wecker ist exakt und
 * geht auch im Stromsparmodus des Geräts.
 */
object Zeitplan {
    private const val TAG = "JarvisZeitplan"
    const val AKTION = "de.frank.jarvis.TAGESAUSWERTUNG"

    /** Wählbare Abstände in Stunden. */
    val ABSTAENDE = listOf(1, 2, 3, 4, 6)

    /** Prüft für eine volle Stunde, ob dann ein Lauf ansteht. */
    private fun pruefer(context: Context): (LocalDateTime) -> Boolean {
        val e = Einstellungen.get(context)
        val abstand = e.auswertungAbstand
        val schlaeft: (LocalDateTime) -> Boolean = (if (e.auswertungSchlafpause) Register.alle(context).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.schlafzeiten() else null) ?: { false }
        // Nach dem Schlaf zählt die erste Stunde auch dann, wenn sie nicht in den Abstand fällt (15 Uhr nach dem Nachtdienst).
        return { t -> !schlaeft(t) && (t.hour % abstand == 0 || schlaeft(t.minusHours(1))) }
    }

    /** Nächster Lauf ab jetzt, oder null, wenn die Synchronisation aus ist. */
    fun naechster(context: Context, ab: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!Einstellungen.get(context).auswertungAn) return null
        val faellig = pruefer(context)
        val start = ab.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        return (0L until 48L).map { start.plusHours(it) }.firstOrNull(faellig) ?: start
    }

    /** Der letzte geplante Lauf, der schon hätte stattfinden sollen (höchstens einen Tag zurück). */
    fun letzterFaelliger(context: Context, jetzt: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!Einstellungen.get(context).auswertungAn) return null
        val faellig = pruefer(context)
        val start = jetzt.truncatedTo(ChronoUnit.HOURS)
        return (0L until 24L).map { start.minusHours(it) }.firstOrNull(faellig)
    }

    fun stelle(context: Context) {
        val app = context.applicationContext
        val wecker = app.getSystemService(AlarmManager::class.java)
        val absicht = PendingIntent.getBroadcast(app, 7, Intent(app, AuswertungsEmpfaenger::class.java).setAction(AKTION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        wecker.cancel(absicht)
        val wann = naechster(app) ?: return
        val millis = wann.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) {
                wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, absicht)
            } else {
                // Ohne Erlaubnis für exakte Wecker: ungefähr zur Zeit, das System darf um Minuten verschieben.
                wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, absicht)
            }
            Log.i(TAG, "Nächste Tagesauswertung: $wann")
        }.onFailure { Log.w(TAG, "Wecker ließ sich nicht stellen", it) }
    }
}

/** Der Wecker ist da: den Dienst mit der Auswertung beauftragen. Der Dienst stellt danach den nächsten Wecker. */
class AuswertungsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Zeitplan.AKTION) return
        JarvisDienst.auswerten(context, "automatische Synchronisation")
    }
}
