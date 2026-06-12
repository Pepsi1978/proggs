package de.frank.voicekey.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.frank.voicekey.VoiceKeyApp
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.data.WakeWordRepository
import de.frank.voicekey.obs.Obs
import de.frank.voicekey.trigger.AssistantLauncher
import de.frank.voicekey.trigger.AssistantLauncherActivity
import de.frank.voicekey.trigger.AssistantStopper
import de.frank.voicekey.wake.ModelManager
import de.frank.voicekey.wake.VoskWakeEngine
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model

/**
 * Dauerhafter Wake-Word-Dienst (Foreground-Service, Typ "microphone"). Ablauf bei Treffer (Almanach
 * voice-assistant-trigger §8): Engine stoppen -> Mikrofon vollstaendig frei -> 450 ms warten
 * (HAL-Flush) -> Trampolin-Activity -> ChatGPT-Voice. Danach Wieder-Bewaffnung nach Debounce.
 *
 * Resilienz-Schichten:
 * - Watchdog: Stirbt die Engine unerwartet (Mic-Konflikt, Lesefehler), startet [onEngineDied] sie
 *   mit Backoff neu — vorher lauschte die App still einfach nicht mehr.
 * - Wake-Wort bei LAUFENDER ChatGPT-Session wird nicht mehr still ignoriert (das fuehlte sich wie
 *   "kaputt" an): Nach einer kurzen Gnadenfrist fuer "ok chatty beenden" wird die im Hintergrund
 *   weiterlaufende Kugel (Almanach §11) wieder nach vorn geholt.
 * - onDestroy entfernt die Notification EXPLIZIT — per notify() aktualisierte Eintraege konnten den
 *   Dienst-Tod ueberleben (Geister-Notification mit toten Knoepfen).
 */
class WakeWordService : LifecycleService() {

    private lateinit var repository: WakeWordRepository
    private lateinit var engine: VoskWakeEngine

    private val engineMutex = Mutex()
    private val triggering = AtomicBoolean(false)
    private val observerStarted = AtomicBoolean(false)
    private val models = mutableMapOf<WakeLang, Model>()

    /** Zaehlt Watchdog-Neustarts in Folge — bestimmt das Backoff (2 s … 60 s). */
    private val watchdogAttempts = AtomicInteger(0)

    /** Laeuft, wenn ein Wake-Wort bei AKTIVER Session auf ein evtl. Beenden-Wort wartet. */
    @Volatile private var pendingForegroundJob: Job? = null

    /**
     * Session-läuft-noch-Signal: Android schaltet unsere Aufnahme stumm, solange eine andere App
     * (ChatGPT-Voice) das Mikrofon hat. Der A11y-Not-Aus nutzt das, um zu erkennen, dass die Kugel
     * zwar weggewischt wurde, ChatGPT aber weiter zuhoert.
     */
    private val recordingCallback =
        object : android.media.AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(
                configs: List<android.media.AudioRecordingConfiguration>
            ) {
                val silenced = configs.any { it.isClientSilenced }
                if (silenced != micSilenced) {
                    micSilenced = silenced
                    Obs.i(
                        "WakeWordService",
                        "onRecordingConfigChanged",
                        "Mic-Status geaendert",
                        mapOf("silenced" to silenced),
                    )
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        serviceRunning = true
        repository = WakeWordRepository(applicationContext)
        engine =
            VoskWakeEngine(
                onWakeWord = ::onWakeWordHit,
                onStopWord = ::onStopWordHit,
                onEngineDied = ::onEngineDied,
                // Laeuft ChatGPT-Voice (haelt unser Mic gedaempft = micSilenced), VAD-Gate
                // aussetzen,
                // damit "Wake-Wort + beenden" waehrend der Session gehoert wird (wie vor dem Gate).
                bypassGate = { micSilenced || AssistantStopper.isVoiceSessionActive(this) },
            )
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        getSystemService(android.media.AudioManager::class.java)
            .registerAudioRecordingCallback(recordingCallback, android.os.Handler(mainLooper))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            Obs.i("WakeWordService", "onStartCommand", "Stopp angefordert")
            lifecycleScope.launch { repository.setServiceEnabled(false) }
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_END_ASSISTANT) {
            // Not-Aus aus der Notification: ChatGPT-Voice per Headsethook beenden — zuverlaessig
            // und OHNE Bedienungshilfe (AssistantStopper nutzt nur Standard-Audio-APIs).
            Obs.i("WakeWordService", "onStartCommand", "Not-Aus angefordert (Notification)")
            AssistantStopper.endVoiceSession(this, "Notification-Knopf")
            return START_STICKY
        }

        // startForeground SOFORT (binnen ~5 s Pflicht) und mit explizitem Mic-Typ (Almanach §4).
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Wird gestartet…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        Obs.checkpoint(
            step = "Dienst gestartet",
            intent = "Wake-Word-Dienst laeuft dauerhaft im Hintergrund",
            expected = "foreground",
            actual = "foreground",
        )

        if (
            !Obs.probe(
                hasMicPermission(),
                "RECORD_AUDIO fehlt — Dienst kann nicht lauschen",
                "WakeWordService",
                "onStartCommand",
            )
        ) {
            updateNotification("Mikrofon-Berechtigung fehlt. App öffnen.")
            return START_STICKY
        }

        // Ein einziger Beobachter (onStartCommand kann mehrfach kommen!): erste Emission
        // startet die Engine, jede Wort-Änderung startet sie mit neuer Grammatik neu.
        if (observerStarted.compareAndSet(false, true)) {
            lifecycleScope.launch(Dispatchers.Default) {
                repository.words.collectLatest { words ->
                    if (triggering.get()) return@collectLatest // Trigger-Phase macht selbst weiter
                    restartEngine()
                }
            }
        }
        return START_STICKY
    }

    /**
     * Engine mit dem aktuellen Favoriten-Stand (neu) starten. Modelle werden lazy geladen und
     * gecacht.
     */
    private suspend fun restartEngine() {
        engineMutex.withLock {
            engine.stop()
            val words = repository.words.first()
            val favoritesByLang =
                WakeLang.entries
                    .associateWith { lang ->
                        words.filter { it.lang == lang && it.favorit }.map { it.text }
                    }
                    .filterValues { it.isNotEmpty() }

            if (favoritesByLang.isEmpty()) {
                updateNotification("Keine Favoriten-Wörter aktiv. App öffnen und Stern setzen.")
                Obs.w(
                    "WakeWordService",
                    "restartEngine",
                    "Keine aktiven Favoriten — Engine pausiert",
                )
                return
            }

            val setups = mutableMapOf<WakeLang, Pair<Model, List<String>>>()
            for ((lang, phrases) in favoritesByLang) {
                if (!ModelManager.isReady(this@WakeWordService, lang)) {
                    Obs.w(
                        "WakeWordService",
                        "restartEngine",
                        "Modell fehlt — Sprache uebersprungen",
                        mapOf("lang" to lang),
                    )
                    continue
                }
                val model =
                    models.getOrPut(lang) {
                        withContext(Dispatchers.IO) {
                            Model(ModelManager.modelDir(this@WakeWordService, lang).absolutePath)
                        }
                    }
                setups[lang] = model to phrases
            }

            if (setups.isEmpty()) {
                updateNotification("Sprachmodelle fehlen noch. App öffnen zum Herunterladen.")
                return
            }

            engine.start(setups, STOP_WORDS_BY_LANG)
            val active = favoritesByLang.values.flatten().joinToString(", ")
            updateNotification("Lauscht auf: $active")
        }
    }

    /**
     * Watchdog: Die Engine ist unerwartet gestorben (Mic-Konflikt, Init-Fehler, Lesefehler).
     * Neustart mit exponentiellem Backoff — frueher blieb die App in diesem Zustand stumm, waehrend
     * die Notification "Lauscht…" zeigte. Lief die Engine vorher lange stabil, wird der
     * Versuchszaehler zurueckgesetzt, damit das Backoff nicht ueber Tage anwaechst.
     */
    private fun onEngineDied(reason: String, uptimeMs: Long) {
        if (uptimeMs >= WATCHDOG_HEALTHY_UPTIME_MS) watchdogAttempts.set(0)
        if (triggering.get()) return // Trigger-Pfad re-armt die Engine selbst
        val attempt = watchdogAttempts.incrementAndGet()
        val backoffMs = min(60_000L, 1_000L shl min(attempt, 6))
        Obs.w(
            "WakeWordService",
            "onEngineDied",
            "Engine gestorben — Watchdog startet neu",
            mapOf("grund" to reason, "versuch" to attempt, "backoffMs" to backoffMs),
        )
        updateNotification("Lauschen unterbrochen — Neustart in ${backoffMs / 1000} s…")
        lifecycleScope.launch(Dispatchers.Default) {
            delay(backoffMs)
            if (repository.serviceEnabled.first() && !triggering.get()) restartEngine()
        }
    }

    /** Wird im Audio-Thread aufgerufen — postet nur, blockiert nie (Deadlock-Schutz). */
    private fun onWakeWordHit(phrase: String, lang: WakeLang) {
        if (AssistantStopper.isVoiceSessionActive(this)) {
            // ChatGPT-Session laeuft noch — oft UNSICHTBAR im Hintergrund weiter, weil das
            // Wegwischen der Kugel sie nicht beendet (Almanach §11). Frueher wurde das Wake-Wort
            // hier still ignoriert (fuehlte sich wie "kaputt" an). Jetzt: kurze Gnadenfrist, damit
            // "ok chatty beenden" weiter funktioniert — kommt kein Beenden-Wort, holen wir die
            // laufende Kugel wieder nach vorn.
            if (triggering.get() || pendingForegroundJob?.isActive == true) {
                Obs.d("WakeWordService", "onWakeWordHit", "Treffer ignoriert — Aktion laeuft schon")
                return
            }
            Obs.i(
                "WakeWordService",
                "onWakeWordHit",
                "Wake-Wort bei laufender Session — Gnadenfrist fuer Beenden-Wort",
                mapOf("phrase" to phrase),
            )
            pendingForegroundJob =
                lifecycleScope.launch(Dispatchers.Default) {
                    delay(WAKE_STOP_GRACE_MS)
                    if (!AssistantStopper.isVoiceSessionActive(this@WakeWordService)) {
                        return@launch // Session wurde inzwischen beendet — nichts hochholen
                    }
                    Obs.checkpoint(
                        step = "Session nach vorn",
                        intent = "Wake-Wort bei laufender Session holt die Kugel wieder hoch",
                        expected = "Trampolin",
                        actual = "Trampolin",
                        ctx = mapOf("phrase" to phrase),
                    )
                    doTrigger("Session läuft — hole ChatGPT-Voice nach vorn…")
                }
            return
        }
        doTrigger("Erkannt: \"$phrase\" — starte ChatGPT Voice…")
    }

    /** Gemeinsamer Trigger-Pfad: Engine stoppen, Mic freigeben, Trampolin, Re-Arm. */
    private fun doTrigger(notificationText: String) {
        if (!triggering.compareAndSet(false, true)) {
            Obs.d("WakeWordService", "doTrigger", "Treffer ignoriert — Trigger laeuft bereits")
            return
        }
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                engineMutex.withLock {
                    engine.stop() // joint den Audio-Thread, Mic wird vollstaendig frei
                }
                updateNotification(notificationText)
                delay(MIC_HANDOFF_DELAY_MS) // Almanach §8: HAL braucht Zeit zum Mic-Flush
                launchTrampoline()
                delay(REARM_DELAY_MS) // Debounce: nicht sofort wieder triggern
            } finally {
                triggering.set(false)
            }
            if (repository.serviceEnabled.first()) restartEngine()
        }
    }

    /**
     * Beenden-Sprachbefehl ("beenden"/"stopp") erkannt -> ChatGPT-Voice per Headsethook beenden.
     */
    private fun onStopWordHit(phrase: String, lang: WakeLang) {
        // "ok chatty beenden": eine evtl. wartende Hochhol-Aktion abbrechen — der Nutzer will
        // die Session beenden, nicht die Kugel zurueckholen.
        pendingForegroundJob?.cancel()
        pendingForegroundJob = null
        Obs.i(
            "WakeWordService",
            "onStopWordHit",
            "Beenden-Wort erkannt",
            mapOf("phrase" to phrase, "lang" to lang),
        )
        AssistantStopper.endVoiceSession(this, "Sprachbefehl: $phrase")
    }

    private fun launchTrampoline() {
        if (!AssistantLauncher.isTargetAvailable(this)) {
            updateNotification(
                "ChatGPT-Voice-Activity nicht gefunden — App öffnen (Update-Hinweis)."
            )
            return
        }
        val intent =
            Intent(this, AssistantLauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Obs.e(
                "WakeWordService",
                "launchTrampoline",
                "Trampolin-Start fehlgeschlagen",
                emptyMap(),
                e,
            )
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun buildNotification(text: String): Notification {
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stopIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, WakeWordService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val endAssistantIntent =
            PendingIntent.getService(
                this,
                2,
                Intent(this, WakeWordService::class.java).setAction(ACTION_END_ASSISTANT),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Builder(this, VoiceKeyApp.CHANNEL_SERVICE)
            .setSmallIcon(de.frank.voicekey.R.drawable.ic_stat_mic)
            .setContentTitle("VoiceKey")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Assistent beenden", endAssistantIntent)
            .addAction(0, "Stopp", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(android.app.NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        pendingForegroundJob?.cancel()
        pendingForegroundJob = null
        engine.stop()
        models.values.forEach { runCatching { it.close() } }
        models.clear()
        getSystemService(android.media.AudioManager::class.java)
            .unregisterAudioRecordingCallback(recordingCallback)
        // Notification EXPLIZIT entfernen: per notify() aktualisierte Eintraege koennen den
        // Dienst-Tod sonst als Geister-Notification mit toten Knoepfen ueberleben.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION_ID)
        micSilenced = false
        serviceRunning = false
        Obs.i("WakeWordService", "onDestroy", "Dienst beendet, Mikrofon freigegeben")
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "de.frank.voicekey.action.STOP"
        const val ACTION_END_ASSISTANT = "de.frank.voicekey.action.END_ASSISTANT"

        /**
         * Beenden-Sprachbefehle pro Sprache (zusaetzlich zu den Wake-Woertern in der Grammatik).
         * Bewusst knappe, eindeutige Woerter, damit sie nicht mitten im ChatGPT-Gespraech
         * versehentlich ausloesen. Wirkt nur, wenn gerade eine Session laeuft.
         */
        private val STOP_WORDS_BY_LANG =
            mapOf(WakeLang.DE to listOf("beenden", "stopp"), WakeLang.EN to listOf("stop"))

        /** true = unsere Aufnahme ist stummgeschaltet, eine andere App (ChatGPT) hat das Mic. */
        @Volatile var micSilenced: Boolean = false

        /** true = der Wake-Word-Dienst lebt (fuer das Session-laeuft-Gate des Not-Aus). */
        @Volatile var serviceRunning: Boolean = false
        private const val NOTIFICATION_ID = 1001
        private const val MIC_HANDOFF_DELAY_MS = 450L
        private const val REARM_DELAY_MS = 5_000L

        /** Wartezeit auf ein Beenden-Wort, bevor die laufende Kugel nach vorn geholt wird. */
        private const val WAKE_STOP_GRACE_MS = 1_600L

        /** Lief die Engine so lange stabil, gilt sie als gesund — Backoff-Zaehler zuruecksetzen. */
        private const val WATCHDOG_HEALTHY_UPTIME_MS = 60_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WakeWordService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, WakeWordService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
