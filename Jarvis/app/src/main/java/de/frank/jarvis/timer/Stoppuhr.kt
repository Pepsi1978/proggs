package de.frank.jarvis.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stand der Stoppuhr. [laeuftSeit] = Wanduhrzeit des letzten Starts oder Weiter (null = angehalten),
 * [angesammelt] = bis dahin gestoppte Zeit, [gestartetUm] = erster Start (null = nie gestartet bzw. zurückgesetzt).
 */
data class StoppuhrStand(val laeuftSeit: Long? = null, val angesammelt: Long = 0, val gestartetUm: Long? = null) {
    val laeuft get() = laeuftSeit != null
    val aktiv get() = gestartetUm != null
    fun gesamt(jetzt: Long = System.currentTimeMillis()): Long = angesammelt + (laeuftSeit?.let { (jetzt - it).coerceAtLeast(0) } ?: 0)
}

/**
 * Stoppuhr von Jarvis: „Starte eine Stoppuhr“ startet sofort. Sie läuft über Wanduhrzeiten (kein Hintergrunddienst nötig),
 * überlebt das Beenden der App und bleibt auf der Startseite stehen, bis Frank sie zurücksetzt. Solange sie läuft,
 * zeigt eine stille Benachrichtigung die mitlaufende Zeit.
 */
object Stoppuhr {
    private const val TAG = "JarvisStoppuhr"
    private const val SPEICHER = "stoppuhr"
    private const val KANAL = "jarvis-stoppuhr"
    private const val MELDUNG_ID = 7_998

    private val _stand = MutableStateFlow(StoppuhrStand())
    val stand: StateFlow<StoppuhrStand> = _stand.asStateFlow()
    @Volatile private var geladen = false

    fun starte(context: Context) { lade(context.applicationContext); meldung(context.applicationContext) }

    fun aktuell(context: Context): StoppuhrStand { lade(context.applicationContext); return _stand.value }

    /** Startet neu von null (auch wenn schon eine lief). */
    fun starten(context: Context): StoppuhrStand {
        val jetzt = System.currentTimeMillis()
        return setze(context, StoppuhrStand(laeuftSeit = jetzt, angesammelt = 0, gestartetUm = jetzt))
    }

    fun anhalten(context: Context): StoppuhrStand {
        val s = aktuell(context)
        if (!s.laeuft) return s
        return setze(context, s.copy(laeuftSeit = null, angesammelt = s.gesamt()))
    }

    /** Weiter nach dem Anhalten; war noch nie eine gestartet, startet sie. */
    fun fortsetzen(context: Context): StoppuhrStand {
        val s = aktuell(context)
        if (!s.aktiv) return starten(context)
        if (s.laeuft) return s
        return setze(context, s.copy(laeuftSeit = System.currentTimeMillis()))
    }

    fun zuruecksetzen(context: Context): StoppuhrStand = setze(context, StoppuhrStand())

    /** „1:05:09,4“ bzw. „3:07,25“ mit Hundertsteln, für die große Anzeige. */
    fun anzeige(ms: Long, hundertstel: Boolean = true): String {
        val s = ms / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sek = s % 60
        val rest = if (hundertstel) ",%02d".format((ms % 1000) / 10) else ""
        return (if (h > 0) "%d:%02d:%02d".format(h, m, sek) else "%02d:%02d".format(m, sek)) + rest
    }

    private fun setze(context: Context, neu: StoppuhrStand): StoppuhrStand {
        val app = context.applicationContext
        lade(app)
        _stand.value = neu
        app.getSharedPreferences(SPEICHER, Context.MODE_PRIVATE).edit()
            .putLong("seit", neu.laeuftSeit ?: -1).putLong("angesammelt", neu.angesammelt).putLong("gestartet", neu.gestartetUm ?: -1).apply()
        meldung(app)
        return neu
    }

    @Synchronized private fun lade(app: Context) {
        if (geladen) return
        val p = app.getSharedPreferences(SPEICHER, Context.MODE_PRIVATE)
        _stand.value = StoppuhrStand(
            p.getLong("seit", -1).takeIf { it > 0 }, p.getLong("angesammelt", 0), p.getLong("gestartet", -1).takeIf { it > 0 },
        )
        geladen = true
    }

    /** Nach dem Schalter unter Einstellungen → Benachrichtigungen: Meldung zeigen oder entfernen. */
    fun meldungErneuern(context: Context) = meldung(context.applicationContext)

    /** Läuft sie, steht eine stille Meldung mit mitlaufender Zeit da; angehalten oder zurückgesetzt verschwindet sie. */
    private fun meldung(app: Context) {
        val nm = app.getSystemService(NotificationManager::class.java)
        val s = _stand.value
        // Angehalten, oder unter Einstellungen → Benachrichtigungen abgeschaltet: keine Meldung.
        if (!s.laeuft || !de.frank.jarvis.meldung.Meldungen.an(app, de.frank.jarvis.meldung.Meldungsarten.STOPPUHR)) { runCatching { nm.cancel(MELDUNG_ID) }; return }
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(
            NotificationChannel(KANAL, "Stoppuhr", NotificationManager.IMPORTANCE_LOW).apply { description = "Zeigt die laufende Stoppuhr."; setShowBadge(false) },
        )
        val oeffnen = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val m = NotificationCompat.Builder(app, KANAL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("Stoppuhr läuft")
            .setContentText("Gestartet um " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.GERMAN).format(java.util.Date(s.gestartetUm ?: System.currentTimeMillis())) + " Uhr")
            .setUsesChronometer(true).setWhen(System.currentTimeMillis() - s.gesamt()).setShowWhen(true)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(oeffnen)
            .build()
        runCatching { nm.notify(MELDUNG_ID, m) }.onFailure { Log.w(TAG, "Meldung nicht möglich", it) }
    }
}
