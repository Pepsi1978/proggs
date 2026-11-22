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
 * Bewusst ohne Höchstdauer und ohne Wegwischen: Weggewischt kommt die Meldung sofort wieder. Aus ist nur „Wecker ausschalten“ im Vollbild
 * (Overlay über jeder App, bei gesperrtem Handy über dem Sperrbildschirm), „Wecker ausschalten“ in der Meldung, in der App oder per Sprache, oder eine Bestätigung von einem anderen Gerät.
 */
class ErinnerungKlingelDienst : Service(), SavedStateRegistryOwner {
    private var ablauf: Job? = null
    private val lebenszyklus = LifecycleRegistry(this)
    private val speicher = SavedStateRegistryController.create(this)
    private var karte: ComposeView? = null
    /** Das Vollbild-Overlay ließ sich nicht einblenden: die Meldung bleibt dann laut. */
    private var karteFehlt = false

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

    /** Vollbild-Overlay über jeder App, nicht wegzuwischen; aus nur mit „Wecker ausschalten“. Ohne Overlay-Recht bleibt es bei Meldung und Sperrbildschirm-Ansicht. */
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
                    if (liste.isNotEmpty() && !sperrAnsicht) ErinnerungWeckerAnsicht.Vollbild(liste) { ErinnerungPlaner.ausschalten(this@ErinnerungKlingelDienst) }
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            // Ganzer Bildschirm: Die App darunter ist erst nach dem Ausschalten wieder bedienbar.
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Fokussierbar: Zurück und andere Tasten landen beim Vollbild, nicht unsichtbar in der App darunter.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; title = "Jarvis-Wecker" }
        runCatching { getSystemService(WindowManager::class.java).addView(neu, lp) }
            .onSuccess { karte = neu }
            .onFailure {
                Logbuch.fehler("Wecker-Vollbild ließ sich nicht einblenden", it)
                // Rückfall: Meldung wieder laut mit Full-Screen-Intent statt still.
                karteFehlt = true
                aktualisieren()
            }
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
        // Entsperrt mit Overlay-Recht steht das Vollbild schon: Meldung dann still, ohne schwebende Meldung oben.
        val direkt = !ErinnerungWeckerActivity.gesperrt(this) && de.frank.jarvis.ui.SprachBlase.darf(this) && !karteFehlt
        if (direkt) stillerKanal()
        return NotificationCompat.Builder(this, if (direkt) KANAL_VOLLBILD else de.frank.jarvis.timer.Timer.KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (direkt) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            // Auch bei „Inhalt verbergen“ muss sich der Wecker gesperrt abstellen lassen, nur der Text bleibt dann verborgen.
            .setVisibility(if (de.frank.jarvis.data.Einstellungen.get(this).meldungInhaltVerbergen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
            // Tippen öffnet die Wecker-Karte, nicht die App: Ausschalten geht dort mit einem Tipp.
            .setContentIntent(vollbild)
            .apply { if (!direkt) setFullScreenIntent(vollbild, true) }
            .addAction(R.drawable.ic_stat_jarvis, "Wecker ausschalten", rundfunk(19_996, ErinnerungPlaner.AKTION_BESTAETIGEN))
            .apply { liste.firstOrNull { it.erinnerung.ortsgebunden }?.let { addAction(R.drawable.ic_stat_jarvis, "Erledigt", ErinnerungPlaner.erledigtAbsicht(this@ErinnerungKlingelDienst, null, 19_994)) } }
            .apply { liste.firstOrNull()?.let { addAction(R.drawable.ic_stat_jarvis, "In Jarvis öffnen", ErinnerungPlaner.oeffnenAbsicht(this@ErinnerungKlingelDienst, it.erinnerung.id, 19_997)) } }
            .setDeleteIntent(rundfunk(19_995, ErinnerungPlaner.AKTION_ZEIGEN))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun stillerKanal() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL_VOLLBILD) != null) return
        nm.createNotificationChannel(android.app.NotificationChannel(KANAL_VOLLBILD, "Erinnerungs-Wecker im Vollbild", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Stille Meldung, solange eine Erinnerung als Wecker im Vollbild klingelt."
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
    }

    companion object {
        private const val NUMMER = 19_994
        private const val KANAL_VOLLBILD = "jarvis-erinnerung-vollbild"
        @Volatile private var instanz: ErinnerungKlingelDienst? = null
        fun abgleichen() { Handler(Looper.getMainLooper()).post { instanz?.aktualisieren() } }
    }
}
