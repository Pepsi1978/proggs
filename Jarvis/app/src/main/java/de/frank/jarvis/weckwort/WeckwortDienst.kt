package de.frank.jarvis.weckwort

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.speech.VorleseZustand
import de.frank.jarvis.speech.Vorleser
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Lauscht dauerhaft auf „Hey Jarvis“, auch bei gesperrtem Handy (Vordergrunddienst vom Typ Mikrofon).
 *
 * Bei einem Treffer gibt der Dienst das Mikrofon frei und holt Jarvis nach vorn ([MainActivity] mit
 * [EXTRA_WECKWORT]); die App nimmt dann auf, piept und antwortet. Aus dem Hintergrund darf eine App das nur mit
 * der Erlaubnis „Über anderen Apps einblenden“; fehlt sie, kommt eine Benachrichtigung zum Antippen.
 *
 * Gelauscht wird nur, solange Jarvis nicht selbst aufnimmt ([Weckwort.belegt]) und nichts vorliest. So weckt sich
 * Jarvis nicht selbst, wenn in einer Antwort „Jarvis“ vorkommt.
 *
 * Android erlaubt den Start eines Mikrofon-Dienstes nur, während Jarvis sichtbar ist. Deshalb startet ihn
 * [abgleichen] aus der App; nach Neustart und Update übernimmt [nachNeustart] (kurz unsichtbar Jarvis öffnen oder
 * eine Benachrichtigung).
 */
class WeckwortDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var erkenner: WeckwortErkenner? = null
    @Volatile private var lauf: AtomicBoolean? = null
    private var faden: Thread? = null
    private var wach: PowerManager.WakeLock? = null
    private var gestartet = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanaeleAnlegen(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (gestartet) return START_STICKY
        val e = Einstellungen.get(this)
        if (!e.weckwortAn || !mikrofonErlaubt(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val typ = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, HINWEIS_ID, hinweis(), typ)
        } catch (fehler: Exception) {
            // Aus dem Hintergrund (etwa nach einem Neustart durch Android) verweigert: über die App neu starten.
            Log.w(TAG, "Mikrofon-Dienst aus dem Hintergrund nicht erlaubt", fehler)
            nachNeustart(this)
            stopSelf()
            return START_NOT_STICKY
        }
        gestartet = true
        Weckwort.schwelle = e.weckwortSchwelle
        wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:weckwort")
            .apply { setReferenceCounted(false); runCatching { acquire() } }
        val vorleser = Vorleser.hole(this, SecureSettings(this))
        bereich.launch {
            combine(Weckwort.belegt, vorleser.stand) { belegt, stand -> !belegt && stand.zustand == VorleseZustand.AUS }
                .distinctUntilChanged()
                .collect { frei -> if (frei) lauschen() else anhalten() }
        }
        // Ruft der Dienst die App und sie kommt nicht (keine Erlaubnis, Android blockt), nicht ewig taub bleiben.
        bereich.launch {
            while (true) {
                delay(2_000)
                val gerufen = Weckwort.gerufenUm
                if (gerufen != 0L && System.currentTimeMillis() - gerufen > 8_000) {
                    Log.w(TAG, "Jarvis kam nach „Hey Jarvis“ nicht nach vorn")
                    Weckwort.mikrofonFreigeben()
                }
            }
        }
        return START_STICKY
    }

    private fun lauschen() {
        if (faden?.isAlive == true) return
        val erk = erkenner ?: runCatching { WeckwortErkenner(this) }
            .onFailure { Log.e(TAG, "Weckwort-Modelle nicht ladbar", it) }
            .getOrNull()?.also { erkenner = it } ?: return
        val an = AtomicBoolean(true)
        lauf = an
        faden = Thread({ schleife(erk, an) }, "Weckwort").apply { start() }
    }

    /** Hält das Lauschen an und wartet kurz, bis das Mikrofon wirklich frei ist. */
    private fun anhalten() {
        lauf?.set(false)
        lauf = null
        val alt = faden
        faden = null
        alt?.join(500)
    }

    @SuppressLint("MissingPermission") // geprüft in onStartCommand
    private fun schleife(erk: WeckwortErkenner, an: AtomicBoolean) {
        var aufnahme: AudioRecord? = null
        try {
            val minimum = AudioRecord.getMinBufferSize(WeckwortErkenner.ABTASTRATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            aufnahme = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, WeckwortErkenner.ABTASTRATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 4, WeckwortErkenner.ABTASTRATE * 2),
            )
            // Wie jeder andere Mikrofon-Fehler: nach einer Pause neu versuchen (catch unten).
            check(aufnahme.state == AudioRecord.STATE_INITIALIZED) { "Mikrofon nicht verfügbar" }
            aufnahme.startRecording()
            Weckwort.hoertZu(true)
            erk.zuruecksetzen()
            val block = ShortArray(WeckwortErkenner.BLOCK)
            while (an.get()) {
                var gelesen = 0
                while (gelesen < block.size && an.get()) {
                    val n = aufnahme.read(block, gelesen, block.size - gelesen)
                    if (n <= 0) throw IllegalStateException("Mikrofon liefert $n")
                    gelesen += n
                }
                if (!an.get()) break
                val wert = erk.bewerte(block)
                if (wert >= Weckwort.schwelle) {
                    Log.i(TAG, "Hey Jarvis erkannt (${"%.2f".format(wert)})")
                    Weckwort.erkannt(wert)
                    an.set(false)
                    bereich.launch { rufen() }
                    break
                }
            }
        } catch (fehler: Exception) {
            Log.w(TAG, "Lauschen unterbrochen", fehler)
            // Nach einem Fehler (Mikrofon belegt, Lesefehler) nach einer Pause neu versuchen.
            if (an.get()) bereich.launch {
                delay(3_000)
                if (lauf === an) { faden = null; lauschen() }
            }
        } finally {
            runCatching { aufnahme?.stop() }
            runCatching { aufnahme?.release() }
            // Läuft schon ein neuer Durchgang, gehört die Anzeige ihm.
            val aktuell = lauf
            if (aktuell == null || aktuell === an) Weckwort.hoertZu(false)
        }
    }

    /** Nach „Hey Jarvis“: Mikrofon ist frei, kurz warten (das Audiosystem gibt es verzögert frei), dann Jarvis holen. */
    private suspend fun rufen() {
        Weckwort.rufen()
        anhalten()
        delay(300)
        val absicht = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_WECKWORT, true)
        val direkt = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)
        if (direkt) {
            runCatching { startActivity(absicht) }.onFailure { Log.w(TAG, "Jarvis ließ sich nicht öffnen", it) }
        }
        // Ohne die Erlaubnis (oder falls Android den Start trotzdem blockt) bleibt der Ruf als Benachrichtigung.
        if (!direkt) zumAntippen(absicht)
    }

    private fun zumAntippen(absicht: Intent) {
        Weckwort.mikrofonFreigeben()
        val tippen = PendingIntent.getActivity(this, 31, absicht, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val meldung = NotificationCompat.Builder(this, KANAL_RUF)
            .setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("Hey Jarvis")
            .setContentText("Tippen, um mit Jarvis zu sprechen. Für den direkten Start in Jarvis „Über anderen Apps einblenden“ erlauben.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(tippen)
            .setFullScreenIntent(tippen, true)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build()
        runCatching { getSystemService(NotificationManager::class.java).notify(RUF_ID, meldung) }
    }

    private fun hinweis(): Notification {
        val oeffnen = PendingIntent.getActivity(
            this, 30, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("Jarvis hört auf „Hey Jarvis“")
            .setContentText("Abschalten unter Einstellungen → Hey Jarvis")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(oeffnen)
            .build()
    }

    override fun onDestroy() {
        anhalten()
        bereich.cancel()
        runCatching { erkenner?.close() }
        erkenner = null
        runCatching { if (wach?.isHeld == true) wach?.release() }
        Weckwort.hoertZu(false)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_WECKWORT = "weckwort"
        private const val TAG = "Weckwort"
        private const val KANAL = "jarvis_weckwort"
        private const val KANAL_RUF = "jarvis_weckwort_ruf"
        private const val HINWEIS_ID = 4300
        private const val RUF_ID = 4301

        /** Zuletzt unsichtbar gestartet (gegen eine Schleife, falls Android den Dienst immer wieder ablehnt). */
        @Volatile private var zuletztUnsichtbar = 0L

        fun mikrofonErlaubt(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        fun kanaeleAnlegen(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(KANAL, "Hey Jarvis", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt, dass Jarvis auf „Hey Jarvis“ hört."
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(KANAL_RUF, "Hey Jarvis: Ruf", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Meldet „Hey Jarvis“, wenn Jarvis nicht von selbst nach vorn kommen darf."
                },
            )
        }

        /** Nur aufrufen, während Jarvis sichtbar ist: Dann darf Android den Mikrofon-Dienst starten. */
        fun abgleichen(context: Context) {
            val app = context.applicationContext
            val absicht = Intent(app, WeckwortDienst::class.java)
            if (Einstellungen.get(app).weckwortAn && mikrofonErlaubt(app)) {
                runCatching { ContextCompat.startForegroundService(app, absicht) }.onFailure { Log.w(TAG, "Start nicht möglich", it) }
            } else {
                app.stopService(absicht)
            }
        }

        /**
         * Nach Neustart oder Update: Den Mikrofon-Dienst darf nur eine sichtbare App starten. Mit „Über anderen Apps
         * einblenden“ öffnet sich Jarvis dafür einen Augenblick unsichtbar ([Starter]); sonst eine Benachrichtigung.
         */
        fun nachNeustart(context: Context) {
            val app = context.applicationContext
            if (!Einstellungen.get(app).weckwortAn || !mikrofonErlaubt(app)) return
            val jetzt = SystemClock.elapsedRealtime()
            val unsichtbar = Build.VERSION.SDK_INT >= 29 && Settings.canDrawOverlays(app) && jetzt - zuletztUnsichtbar > 60_000
            if (unsichtbar) {
                zuletztUnsichtbar = jetzt
                val ok = runCatching { app.startActivity(Intent(app, Starter::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)) }.isSuccess
                if (ok) return
            }
            kanaeleAnlegen(app)
            val tippen = PendingIntent.getActivity(
                app, 32, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE,
            )
            val meldung = NotificationCompat.Builder(app, KANAL)
                .setSmallIcon(R.drawable.ic_stat_jarvis)
                .setContentTitle("„Hey Jarvis“ ist aus")
                .setContentText("Tippen, um es wieder einzuschalten.")
                .setContentIntent(tippen)
                .setAutoCancel(true)
                .build()
            runCatching { app.getSystemService(NotificationManager::class.java).notify(RUF_ID, meldung) }
        }
    }

    /** Unsichtbare Durchgangs-Seite: Solange sie sichtbar ist, darf der Mikrofon-Dienst starten. */
    class Starter : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            overridePendingTransition(0, 0)
        }

        override fun onResume() {
            super.onResume()
            abgleichen(this)
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
