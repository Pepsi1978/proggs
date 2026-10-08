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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Weckt Jarvis zu den eingestellten Uhrzeiten für die Tagesauswertung. Es ist immer genau ein Wecker gestellt:
 * der nächste. Nach jedem Lauf, beim Start der App, nach dem Einschalten des Handys und nach einer Zeit- oder
 * Zeitzonenänderung wird neu gestellt. Der Wecker ist exakt und geht auch im Stromsparmodus des Geräts.
 */
object Zeitplan {
    private const val TAG = "JarvisZeitplan"
    const val AKTION = "de.frank.jarvis.TAGESAUSWERTUNG"

    /** Die eingestellten Uhrzeiten, sortiert. Unlesbare Einträge fallen weg. */
    fun zeiten(context: Context): List<LocalTime> = Einstellungen.get(context).auswertungZeiten
        .split(",").mapNotNull { leseZeit(it) }.distinct().sorted()

    fun leseZeit(text: String): LocalTime? {
        val treffer = Regex("^\\s*([01]?\\d|2[0-3])[:.]([0-5]\\d)\\s*$").find(text) ?: return null
        return LocalTime.of(treffer.groupValues[1].toInt(), treffer.groupValues[2].toInt())
    }

    /** Nächster Lauf ab jetzt, oder null, wenn die Auswertung aus ist oder keine Uhrzeit eingetragen ist. */
    fun naechster(context: Context, ab: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!Einstellungen.get(context).auswertungAn) return null
        val zeiten = zeiten(context).ifEmpty { return null }
        val heute = ab.toLocalDate()
        val regulaer = zeiten.map { heute.atTime(it) }.firstOrNull { it.isAfter(ab) } ?: heute.plusDays(1).atTime(zeiten.first())
        // Ein vorgemerkter Nachbesserungslauf kommt dazwischen, wenn er früher liegt.
        val nachbesserung = Einstellungen.get(context).nachbesserungUm.takeIf { it > 0 }
            ?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }?.takeIf { it.isAfter(ab) }
        return if (nachbesserung != null && nachbesserung.isBefore(regulaer)) nachbesserung else regulaer
    }

    /** Der letzte geplante Lauf, der heute schon hätte stattfinden sollen. */
    fun letzterFaelliger(context: Context, jetzt: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!Einstellungen.get(context).auswertungAn) return null
        return zeiten(context).map { LocalDate.now().atTime(it) }.lastOrNull { !it.isAfter(jetzt) }
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
        val e = Einstellungen.get(context)
        // Ist dieser Wecker der vorgemerkte Zusatzlauf, heißt der Anlass so; damit folgt darauf kein weiterer.
        val nachbesserung = e.nachbesserungUm > 0 && kotlin.math.abs(System.currentTimeMillis() - e.nachbesserungUm) < 10 * 60_000L
        JarvisDienst.auswerten(context, if (nachbesserung) "nachgebessert, weil der Schlafwert zuerst fehlte" else "automatisch zur eingestellten Uhrzeit")
    }
}
