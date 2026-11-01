package de.frank.jarvis.ablage

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import de.frank.jarvis.R
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.Job

/**
 * Eine Erinnerung in der Form Wecker: Weckton in der Schleife, dazwischen liest Jarvis die Kurzfassung vor, bis Frank ausdrücklich ausschaltet.
 * Bewusst ohne Höchstdauer und ohne Wegwischen: Weggewischt kommt die Meldung sofort wieder. Aus ist nur „Aus“, in der App oder per Sprache,
 * oder eine Bestätigung von einem anderen Gerät.
 */
class ErinnerungKlingelDienst : Service() {
    private var ablauf: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); instanz = this }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wer per startForegroundService gerufen wird, muss startForeground liefern, auch wenn schon alles aus ist.
        val vorn = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NUMMER, baueMeldung(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NUMMER, baueMeldung())
        }.onFailure { Logbuch.fehler("Erinnerung Vordergrund", it) }.isSuccess
        if (!vorn) { ErinnerungPlaner.dienstGescheitert(this); stopSelf(); return START_NOT_STICKY }
        if (ErinnerungPlaner.klingelt.value.isEmpty()) { beenden(); return START_NOT_STICKY }
        if (ablauf?.isActive != true) ablauf = ErinnerungsWeckablauf.starten(this, NUMMER) { ErinnerungPlaner.klingelt.value }
        else aktualisieren()
        return START_NOT_STICKY
    }

    private fun aktualisieren() {
        if (ErinnerungPlaner.klingelt.value.isEmpty()) beenden()
        else runCatching { getSystemService(NotificationManager::class.java).notify(NUMMER, baueMeldung()) }
    }

    private fun beenden() {
        ablauf?.cancel(); ablauf = null
        de.frank.jarvis.meldung.WeckerTon.stopp(NUMMER)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        instanz = null
        ablauf?.cancel()
        de.frank.jarvis.meldung.WeckerTon.stopp(NUMMER)
        super.onDestroy()
    }

    private fun baueMeldung(): Notification {
        val liste = ErinnerungPlaner.klingelt.value
        val flaggen = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        fun rundfunk(code: Int, aktion: String) = PendingIntent.getBroadcast(this, code, Intent(this, ErinnerungEmpfaenger::class.java).setAction(aktion), flaggen)
        val text = liste.firstOrNull()?.erinnerung?.kurz ?: "Erinnerung"
        val titel = if (liste.size > 1) "${liste.size} Erinnerungen" else liste.firstOrNull()?.erinnerung?.zeile ?: "Erinnerung"
        return NotificationCompat.Builder(this, de.frank.jarvis.timer.Timer.KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            // Auch bei „Inhalt verbergen“ muss sich der Wecker gesperrt abstellen lassen, nur der Text bleibt dann verborgen.
            .setVisibility(if (de.frank.jarvis.data.Einstellungen.get(this).meldungInhaltVerbergen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
            .apply { liste.firstOrNull()?.let { setContentIntent(ErinnerungPlaner.oeffnenAbsicht(this@ErinnerungKlingelDienst, it.erinnerung.id, 19_997)) } }
            .addAction(R.drawable.ic_stat_jarvis, "Aus", rundfunk(19_996, ErinnerungPlaner.AKTION_BESTAETIGEN))
            .setDeleteIntent(rundfunk(19_995, ErinnerungPlaner.AKTION_ZEIGEN))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val NUMMER = 19_994
        @Volatile private var instanz: ErinnerungKlingelDienst? = null
        fun abgleichen() { Handler(Looper.getMainLooper()).post { instanz?.aktualisieren() } }
    }
}
