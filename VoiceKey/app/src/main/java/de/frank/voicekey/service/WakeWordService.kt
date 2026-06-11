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
import de.frank.voicekey.wake.ModelManager
import de.frank.voicekey.wake.VoskWakeEngine
import kotlinx.coroutines.Dispatchers
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dauerhafter Wake-Word-Dienst (Foreground-Service, Typ "microphone").
 * Ablauf bei Treffer (Almanach voice-assistant-trigger §8):
 * Engine stoppen -> Mikrofon vollstaendig frei -> 450 ms warten (HAL-Flush) ->
 * Trampolin-Activity -> ChatGPT-Voice. Danach Wieder-Bewaffnung nach Debounce.
 */
class WakeWordService : LifecycleService() {

    private lateinit var repository: WakeWordRepository
    private lateinit var engine: VoskWakeEngine

    private val engineMutex = Mutex()
    private val triggering = AtomicBoolean(false)
    private val models = mutableMapOf<WakeLang, Model>()

    /**
     * Session-läuft-noch-Signal: Android schaltet unsere Aufnahme stumm, solange eine
     * andere App (ChatGPT-Voice) das Mikrofon hat. Der A11y-Not-Aus nutzt das, um zu
     * erkennen, dass die Kugel zwar weggewischt wurde, ChatGPT aber weiter zuhoert.
     */
    private val recordingCallback = object : android.media.AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<android.media.AudioRecordingConfiguration>) {
            val silenced = configs.any { it.isClientSilenced }
            if (silenced != micSilenced) {
                micSilenced = silenced
                Obs.i("WakeWordService", "onRecordingConfigChanged", "Mic-Status geaendert", mapOf("silenced" to silenced))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = WakeWordRepository(applicationContext)
        engine = VoskWakeEngine(onWakeWord = ::onWakeWordHit)
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
            // Not-Aus aus der Notification: ChatGPT-Voice-Session wirklich beenden.
            val a11y = de.frank.voicekey.a11y.VoiceKeyAccessibilityService.instance
            if (a11y != null) {
                a11y.endAssistantSession()
            } else {
                Obs.w("WakeWordService", "onStartCommand", "Not-Aus angefordert, aber Bedienungshilfe nicht aktiv")
                updateNotification("Not-Aus braucht die Bedienungshilfe. App öffnen → Einrichtung.")
            }
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

        if (!Obs.probe(hasMicPermission(), "RECORD_AUDIO fehlt — Dienst kann nicht lauschen", "WakeWordService", "onStartCommand")) {
            updateNotification("Mikrofon-Berechtigung fehlt. App öffnen.")
            return START_STICKY
        }

        // Ein einziger Beobachter: erste Emission startet die Engine, jede Wort-Änderung
        // startet sie mit der neuen Favoriten-Grammatik neu.
        lifecycleScope.launch(Dispatchers.Default) {
            repository.words.collectLatest { words ->
                if (triggering.get()) return@collectLatest // Trigger-Phase macht selbst weiter
                restartEngine()
            }
        }
        return START_STICKY
    }

    /** Engine mit dem aktuellen Favoriten-Stand (neu) starten. Modelle werden lazy geladen und gecacht. */
    private suspend fun restartEngine() {
        engineMutex.withLock {
            engine.stop()
            val words = repository.words.first()
            val favoritesByLang = WakeLang.entries.associateWith { lang ->
                words.filter { it.lang == lang && it.favorit }.map { it.text }
            }.filterValues { it.isNotEmpty() }

            if (favoritesByLang.isEmpty()) {
                updateNotification("Keine Favoriten-Wörter aktiv. App öffnen und Stern setzen.")
                Obs.w("WakeWordService", "restartEngine", "Keine aktiven Favoriten — Engine pausiert")
                return
            }

            val setups = mutableMapOf<WakeLang, Pair<Model, List<String>>>()
            for ((lang, phrases) in favoritesByLang) {
                if (!ModelManager.isReady(this@WakeWordService, lang)) {
                    Obs.w("WakeWordService", "restartEngine", "Modell fehlt — Sprache uebersprungen", mapOf("lang" to lang))
                    continue
                }
                val model = models.getOrPut(lang) {
                    withContext(Dispatchers.IO) { Model(ModelManager.modelDir(this@WakeWordService, lang).absolutePath) }
                }
                setups[lang] = model to phrases
            }

            if (setups.isEmpty()) {
                updateNotification("Sprachmodelle fehlen noch. App öffnen zum Herunterladen.")
                return
            }

            engine.start(setups)
            val active = favoritesByLang.values.flatten().joinToString(", ")
            updateNotification("Lauscht auf: $active")
        }
    }

    /** Wird im Audio-Thread aufgerufen — postet nur, blockiert nie (Deadlock-Schutz). */
    private fun onWakeWordHit(phrase: String, lang: WakeLang) {
        if (!triggering.compareAndSet(false, true)) {
            Obs.d("WakeWordService", "onWakeWordHit", "Treffer ignoriert — Trigger laeuft bereits")
            return
        }
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                engineMutex.withLock {
                    engine.stop() // joint den Audio-Thread, Mic wird vollstaendig frei
                }
                updateNotification("Erkannt: \"$phrase\" — starte ChatGPT Voice…")
                delay(MIC_HANDOFF_DELAY_MS) // Almanach §8: HAL braucht Zeit zum Mic-Flush
                launchTrampoline()
                delay(REARM_DELAY_MS) // Debounce: nicht sofort wieder triggern
            } finally {
                triggering.set(false)
            }
            if (repository.serviceEnabled.first()) restartEngine()

            // Schnell-Szenario: Kugel wurde sofort weggewischt, ChatGPT haelt das Mic
            // trotzdem weiter -> Auto-Not-Aus pruefen (Kugel-sichtbar/Anruf prueft der A11y).
            delay(2_000)
            if (micSilenced) {
                de.frank.voicekey.a11y.VoiceKeyAccessibilityService.instance?.requestAutoKillCheck("Re-Arm: Mic weiter stumm")
            }
        }
    }

    private fun launchTrampoline() {
        if (!AssistantLauncher.isTargetAvailable(this)) {
            updateNotification("ChatGPT-Voice-Activity nicht gefunden — App öffnen (Update-Hinweis).")
            return
        }
        val intent = Intent(this, AssistantLauncherActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Obs.e("WakeWordService", "launchTrampoline", "Trampolin-Start fehlgeschlagen", emptyMap(), e)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, WakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val endAssistantIntent = PendingIntent.getService(
            this, 2,
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
        engine.stop()
        models.values.forEach { runCatching { it.close() } }
        models.clear()
        getSystemService(android.media.AudioManager::class.java)
            .unregisterAudioRecordingCallback(recordingCallback)
        micSilenced = false
        Obs.i("WakeWordService", "onDestroy", "Dienst beendet, Mikrofon freigegeben")
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "de.frank.voicekey.action.STOP"
        const val ACTION_END_ASSISTANT = "de.frank.voicekey.action.END_ASSISTANT"

        /** true = unsere Aufnahme ist stummgeschaltet, eine andere App (ChatGPT) hat das Mic. */
        @Volatile
        var micSilenced: Boolean = false
        private const val NOTIFICATION_ID = 1001
        private const val MIC_HANDOFF_DELAY_MS = 450L
        private const val REARM_DELAY_MS = 5_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WakeWordService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, WakeWordService::class.java).setAction(ACTION_STOP))
        }
    }
}
