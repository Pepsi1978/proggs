package de.frank.jarvis.ablage

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import de.frank.jarvis.R
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.ui.theme.JarvisTheme
import kotlinx.coroutines.Job

/**
 * Eine Erinnerung in der Form Wecker: Weckton in der Schleife, dazwischen liest Jarvis die Kurzfassung vor, bis Frank ausdrücklich ausschaltet.
 * Bewusst ohne Höchstdauer und ohne Wegwischen: Weggewischt kommt die Meldung sofort wieder. Aus ist nur „Wecker ausschalten“ auf der Wecker-Karte
 * (Overlay über jeder App, bei gesperrtem Handy über dem Sperrbildschirm), „Wecker ausschalten“ in der Meldung, in der App oder per Sprache, oder eine Bestätigung von einem anderen Gerät.
 */
class ErinnerungKlingelDienst : Service(), SavedStateRegistryOwner {
    private var ablauf: Job? = null
    private val lebenszyklus = LifecycleRegistry(this)
    private val speicher = SavedStateRegistryController.create(this)
    private var karte: ComposeView? = null

    override val lifecycle: Lifecycle get() = lebenszyklus
    override val savedStateRegistry: SavedStateRegistry get() = speicher.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        speicher.performRestore(null)
        lebenszyklus.currentState = Lifecycle.State.RESUMED
        instanz = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wer per startForegroundService gerufen wird, muss startForeground liefern, auch wenn schon alles aus ist.
        val vorn = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NUMMER, baueMeldung(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NUMMER, baueMeldung())
        }.onFailure { Logbuch.fehler("Erinnerung Vordergrund", it) }.isSuccess
        if (!vorn) { ErinnerungPlaner.dienstGescheitert(this); stopSelf(); return START_NOT_STICKY }
        if (ErinnerungPlaner.klingelt.value.isEmpty()) { beenden(); return START_NOT_STICKY }
        zeigeKarte()
        if (!empfaengerAn) empfaengerAn = runCatching { registerReceiver(bildschirmAn, android.content.IntentFilter(Intent.ACTION_SCREEN_ON)) }.isSuccess
        if (ablauf?.isActive != true) {
            ablauf = ErinnerungsWeckablauf.starten(this, NUMMER) { ErinnerungPlaner.klingelt.value }
            sperrbildschirm()
        } else aktualisieren()
        return START_NOT_STICKY
    }

    /** Kleines Overlay oben über jeder App; die App darunter bleibt bedienbar. Ohne Overlay-Recht bleibt es bei Meldung und Sperrbildschirm-Ansicht. */
    private fun zeigeKarte() {
        if (karte != null || !de.frank.jarvis.ui.SprachBlase.darf(this)) return
        val einst = de.frank.jarvis.data.Einstellungen.get(this)
        val neu = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ErinnerungKlingelDienst)
            setViewTreeSavedStateRegistryOwner(this@ErinnerungKlingelDienst)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@ErinnerungKlingelDienst))
            setContent {
                JarvisTheme(einst.modus, einst.design) {
                    val liste by ErinnerungPlaner.klingelt.collectAsState()
                    val sperrAnsicht by ErinnerungWeckerAnsicht.vorn.collectAsState()
                    if (liste.isNotEmpty() && !sperrAnsicht) ErinnerungWeckerAnsicht.Karte(liste) { ErinnerungPlaner.ausschalten(this@ErinnerungKlingelDienst) }
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            // Nur so breit wie die Karte: Daneben bleibt die App darunter bedienbar.
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; title = "Jarvis-Wecker" }
        runCatching { getSystemService(WindowManager::class.java).addView(neu, lp) }
            .onSuccess { karte = neu }
            .onFailure { Logbuch.fehler("Wecker-Karte ließ sich nicht einblenden", it) }
    }

    private fun entferneKarte() {
        karte?.let { v -> runCatching { v.disposeComposition(); getSystemService(WindowManager::class.java).removeViewImmediate(v) } }
        karte = null
    }

    /** Wird das Handy während des Klingelns gesperrt und wieder eingeschaltet, steht die Karte gleich auf dem Sperrbildschirm. */
    private val bildschirmAn = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { if (ErinnerungPlaner.klingelt.value.isNotEmpty()) sperrbildschirm() }
    }
    private var empfaengerAn = false

    /** Gesperrt oder Bildschirm aus: die Wecker-Karte über dem Sperrbildschirm. Mit Overlay-Recht darf Jarvis sie auch aus dem Hintergrund öffnen. */
    private fun sperrbildschirm() {
        if (!ErinnerungWeckerActivity.gesperrt(this)) return
        runCatching { startActivity(Intent(this, ErinnerungWeckerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            .onFailure { Logbuch.fehler("Wecker-Ansicht Sperrbildschirm", it) }
    }

    private fun aktualisieren() {
        if (ErinnerungPlaner.klingelt.value.isEmpty()) beenden()
        else runCatching { getSystemService(NotificationManager::class.java).notify(NUMMER, baueMeldung()) }
    }

    private fun beenden() {
        ablauf?.cancel(); ablauf = null
        de.frank.jarvis.meldung.WeckerTon.stopp(NUMMER)
        entferneKarte()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        instanz = null
        if (empfaengerAn) { runCatching { unregisterReceiver(bildschirmAn) }; empfaengerAn = false }
        ablauf?.cancel()
        de.frank.jarvis.meldung.WeckerTon.stopp(NUMMER)
        entferneKarte()
        lebenszyklus.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun baueMeldung(): Notification {
        val liste = ErinnerungPlaner.klingelt.value
        val flaggen = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        fun rundfunk(code: Int, aktion: String) = PendingIntent.getBroadcast(this, code, Intent(this, ErinnerungEmpfaenger::class.java).setAction(aktion), flaggen)
        val text = liste.firstOrNull()?.erinnerung?.kurz ?: "Erinnerung"
        val titel = if (liste.size > 1) "${liste.size} Erinnerungen" else liste.firstOrNull()?.erinnerung?.zeile ?: "Erinnerung"
        val vollbild = PendingIntent.getActivity(this, 19_993, Intent(this, ErinnerungWeckerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), flaggen)
        return NotificationCompat.Builder(this, de.frank.jarvis.timer.Timer.KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            // Auch bei „Inhalt verbergen“ muss sich der Wecker gesperrt abstellen lassen, nur der Text bleibt dann verborgen.
            .setVisibility(if (de.frank.jarvis.data.Einstellungen.get(this).meldungInhaltVerbergen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
            // Tippen öffnet die Wecker-Karte, nicht die App: Ausschalten geht dort mit einem Tipp.
            .setContentIntent(vollbild)
            .setFullScreenIntent(vollbild, true)
            .addAction(R.drawable.ic_stat_jarvis, "Wecker ausschalten", rundfunk(19_996, ErinnerungPlaner.AKTION_BESTAETIGEN))
            .apply { liste.firstOrNull()?.let { addAction(R.drawable.ic_stat_jarvis, "In Jarvis öffnen", ErinnerungPlaner.oeffnenAbsicht(this@ErinnerungKlingelDienst, it.erinnerung.id, 19_997)) } }
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
