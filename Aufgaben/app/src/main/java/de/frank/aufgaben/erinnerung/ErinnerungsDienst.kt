package de.frank.aufgaben.erinnerung

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.tts.SpeechLoudness
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Spielt eine Erinnerung mit Ton und vorgelesenem Aufgabentext ab, auch wenn die App geschlossen ist.
 *
 * - Benachrichtigung: Ton, dann die sechs vorbereiteten Fassungen nacheinander, je 3 s Pause, Ende.
 * - Wecker: Ton und Fassungen laufen in Schleife weiter, bis man in der Benachrichtigung „Ausschalten“
 *   tippt (oder sie öffnet, „Erledigt“ / „In 10 Min.“ wählt). Kein eigener Bildschirm. Die Benachrichtigung
 *   ist bewusst „ongoing“; wo Android 14+ sie trotzdem wegwischen lässt, stoppt der deleteIntent den Wecker.
 * Fehlen die Dateien (z. B. offline erstellt), spricht die Android-Stimme des Geräts.
 * Kommt während einer Erinnerung die nächste, wartet sie, bis die laufende vorbei ist.
 * Alles läuft über den Wecker-Kanal und den eingebauten Lautsprecher, auch bei Lautlos und Bluetooth ([Lautsprecher]).
 */
class ErinnerungsDienst : Service() {
    /** [anzahl]: wie oft der Text vorgelesen wird (Aufgaben sechsmal, Ende des Fokus-Timers einmal). */
    private data class Auftrag(val aufgabe: Aufgabe, val wann: String, val anzahl: Int = Ansage.ANZAHL) {
        val id: Long get() = aufgabe.id
        val wecker: Boolean get() = aufgabe.alsWecker
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val warteschlange = ArrayDeque<Auftrag>()
    private var aktuell: Auftrag? = null
    private var job: Job? = null
    private var player: MediaPlayer? = null
    private var geraeteStimme: TextToSpeech? = null
    /** Die Android-Stimme als Datei (Text → Datei), damit auch sie über den Lautsprecher läuft. */
    private var geraeteDatei: Pair<String, File>? = null
    private var wake: PowerManager.WakeLock? = null
    private var fokus: AudioFocusRequest? = null
    private var lautstaerkeZurueck: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instanz = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val istFokus = intent?.action == Fokus.AKTION_ENDE
        val id = if (istFokus) Fokus.ID else intent?.getLongExtra(Planer.ID, -1) ?: -1
        val wann = intent?.getStringExtra(EXTRA_WANN).orEmpty()
        val laeuft = aktuell
        // Jeder startForegroundService() verlangt sofort startForeground(), sonst beendet Android die App.
        val n = if (laeuft != null) benachrichtigung(laeuft, imDienst = true) else Planer.platzhalter(this)
        if (!vordergrund(laeuft?.id?.toInt() ?: PLATZHALTER_ID, n)) {
            Log.w(TAG, "Vordergrund nicht erlaubt; Erinnerung ohne Vorlesen")
            if (istFokus) Fokus.einfach(applicationContext)
            else if (id >= 0) Planer.einfacheErinnerung(applicationContext, id)
            if (aktuell == null) stopSelf()
            return START_NOT_STICKY
        }
        if (id < 0) {
            if (aktuell == null) beenden(null, entfernen = true)
            return START_NOT_STICKY
        }
        if (istFokus) {
            annehmen(Auftrag(Fokus.aufgabe(), "", anzahl = 1))
            return START_NOT_STICKY
        }
        scope.launch {
            val a = withContext(Dispatchers.IO) { AufgabenRepository.get(this@ErinnerungsDienst).eine(id) }
            if (a == null || a.erledigt) {
                if (aktuell == null && warteschlange.isEmpty()) beenden(null, entfernen = true)
                return@launch
            }
            annehmen(Auftrag(a, wann))
        }
        return START_NOT_STICKY
    }

    /** Spielt [auftrag] sofort ab oder stellt ihn hinter die laufende Erinnerung. */
    private fun annehmen(auftrag: Auftrag) {
        val id = auftrag.id
        when {
            aktuell?.id == id || warteschlange.any { it.id == id } -> Unit
            aktuell == null -> {
                val bn = benachrichtigung(auftrag, imDienst = true)
                vordergrund(auftrag.id.toInt(), bn)
                // Der Platzhalter verschwindet, sobald die echte Benachrichtigung steht.
                NotificationManagerCompat.from(this).cancel(PLATZHALTER_ID)
                starte(auftrag)
            }
            else -> {
                zeige(auftrag)
                warteschlange.addLast(auftrag)
            }
        }
    }

    private fun benachrichtigung(a: Auftrag, imDienst: Boolean): Notification =
        if (a.id == Fokus.ID) Fokus.benachrichtigung(this, imDienst)
        else Planer.benachrichtigung(this, a.aufgabe, a.wann, imDienst)

    private fun zeige(a: Auftrag) {
        runCatching { NotificationManagerCompat.from(this).notify(a.id.toInt(), benachrichtigung(a, imDienst = false)) }
    }

    private fun vordergrund(nid: Int, n: Notification): Boolean = try {
        val typ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, nid, n, typ)
        true
    } catch (e: Exception) {
        Log.w(TAG, "startForeground fehlgeschlagen", e)
        false
    }

    private fun starte(a: Auftrag) {
        aktuell = a
        val e = Einstellungen.get(this)
        val grenze = if (a.wecker) WECKER_MAX_MS else HINWEIS_MAX_MS
        wake?.let { if (it.isHeld) it.release() }
        wake = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:erinnerung")
            ?.apply { acquire(grenze + 60_000L) }
        fokusAnfordern(a.wecker)
        if (lautstaerkeZurueck == null) lautstaerkeZurueck = Lautsprecher.hoerbarMachen(this)
        val laufend = scope.launch {
            try {
                withTimeoutOrNull(grenze) { ablauf(a, e) }
            } catch (c: CancellationException) {
                throw c
            } catch (x: Exception) {
                Log.w(TAG, "Wiedergabe fehlgeschlagen", x)
            }
        }
        job = laufend
        laufend.invokeOnCompletion { grund ->
            // Natürliches Ende (nicht über stoppe): Benachrichtigung bleibt stehen, nächste Erinnerung folgt.
            if (grund == null) Handler(Looper.getMainLooper()).post { if (aktuell === a && job === laufend) weiter(a, entfernen = false) }
        }
    }

    private suspend fun ablauf(a: Auftrag, e: Einstellungen) {
        val tonAttribute = Lautsprecher.attribute(sprache = false)
        val attribute = Lautsprecher.attribute(sprache = true)
        if (e.vibration) runCatching {
            getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 260), -1), tonAttribute)
        }
        val dateien = if (a.aufgabe.vorlesen) withContext(Dispatchers.IO) { Ansage.dateien(this@ErinnerungsDienst, a.aufgabe) } else emptyList()
        val text = Ansage.text(a.aufgabe)
        do {
            spiele(null, e.ton, e.lautstaerke, tonAttribute, TON_MAX_MS, verstaerken = false)
            if (a.aufgabe.vorlesen && text.isNotBlank()) {
                delay(1_000)
                for (i in 0 until a.anzahl) {
                    val datei = dateien.getOrNull(i % dateien.size.coerceAtLeast(1))
                    if (datei != null) spiele(datei, null, e.lautstaerke, attribute, SATZ_MAX_MS, verstaerken = true)
                    else geraetSpricht(text, e.lautstaerke, attribute)
                    if (i < a.anzahl - 1 || a.wecker) delay(Ansage.PAUSE_MS)
                }
            } else if (a.wecker) {
                delay(Ansage.PAUSE_MS)
            }
        } while (a.wecker)
    }

    /** Spielt eine Datei oder einen Ton bis zum Ende (höchstens [maxMs]); Fehler gelten als Ende. */
    private suspend fun spiele(datei: File?, ton: String?, lautstaerke: Float, attribute: AudioAttributes, maxMs: Long, verstaerken: Boolean) {
        withTimeoutOrNull(maxMs) {
            suspendCancellableCoroutine<Unit> { k ->
                val p = MediaPlayer()
                val zu = AtomicBoolean(false)
                fun ende() {
                    if (!zu.compareAndSet(false, true)) return
                    if (player === p) player = null
                    runCatching { p.stop() }
                    SpeechLoudness.release(p)
                    runCatching { p.release() }
                    if (k.isActive) k.resume(Unit)
                }
                k.invokeOnCancellation { Handler(Looper.getMainLooper()).post { ende() } }
                try {
                    player = p
                    // Erst die Quelle, dann die Attribute: Der Rückfall in setzeQuelle setzt den Player zurück.
                    if (datei != null) p.setDataSource(datei.absolutePath) else Toene.setzeQuelle(this@ErinnerungsDienst, p, ton.orEmpty())
                    p.setAudioAttributes(attribute)
                    p.setOnCompletionListener { ende() }
                    p.setOnErrorListener { _, _, _ -> ende(); true }
                    p.prepare()
                    Lautsprecher.aufGeraet(this@ErinnerungsDienst, p)
                    if (verstaerken) SpeechLoudness.boost(p)
                    p.setVolume(lautstaerke, lautstaerke)
                    p.start()
                } catch (x: Exception) {
                    Log.w(TAG, "Abspielen fehlgeschlagen: ${x.message}")
                    ende()
                }
            }
        }
    }

    /**
     * Rückfall ohne vorbereitete Dateien: die Android-Stimme des Geräts (offline, sofern installiert).
     * Sie wird einmal in eine Datei gesprochen und wie die anderen Fassungen über den Lautsprecher abgespielt;
     * nur wenn das scheitert, spricht sie direkt (dann wählt Android die Ausgabe).
     */
    private suspend fun geraetSpricht(text: String, lautstaerke: Float, attribute: AudioAttributes) {
        val tts = geraeteStimme ?: withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<TextToSpeech?> { k ->
                var neu: TextToSpeech? = null
                neu = TextToSpeech(this@ErinnerungsDienst) { status ->
                    if (status == TextToSpeech.SUCCESS) { if (k.isActive) k.resume(neu) }
                    else { neu?.shutdown(); if (k.isActive) k.resume(null) }
                }
            }
        }?.also { geraeteStimme = it }
        if (tts == null) { delay(2_000); return }
        tts.setLanguage(Locale.GERMANY)
        val datei = geraeteDatei?.takeIf { it.first == text && it.second.length() > 44 }?.second
            ?: synthetisiere(tts, text)?.also { geraeteDatei = text to it }
        if (datei != null) {
            spiele(datei, null, lautstaerke, attribute, SATZ_MAX_MS, verstaerken = true)
            return
        }
        tts.setAudioAttributes(attribute)
        withTimeoutOrNull(SATZ_MAX_MS) {
            suspendCancellableCoroutine<Unit> { k ->
                val kennung = "erinnerung_${System.nanoTime()}"
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { if (utteranceId == kennung && k.isActive) k.resume(Unit) }
                    @Deprecated("Ältere Android-Versionen") override fun onError(utteranceId: String?) { if (utteranceId == kennung && k.isActive) k.resume(Unit) }
                })
                k.invokeOnCancellation { runCatching { tts.stop() } }
                val parameter = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, lautstaerke) }
                if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, parameter, kennung) != TextToSpeech.SUCCESS && k.isActive) k.resume(Unit)
            }
        }
    }

    /** Spricht [text] mit der Android-Stimme in eine Datei; null, wenn das nicht klappt. */
    private suspend fun synthetisiere(tts: TextToSpeech, text: String): File? {
        val datei = File(cacheDir, "geraetestimme.wav")
        datei.delete()
        val ok = withTimeoutOrNull(30_000) {
            suspendCancellableCoroutine<Boolean> { k ->
                val kennung = "datei_${System.nanoTime()}"
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { if (utteranceId == kennung && k.isActive) k.resume(true) }
                    @Deprecated("Ältere Android-Versionen") override fun onError(utteranceId: String?) { if (utteranceId == kennung && k.isActive) k.resume(false) }
                })
                k.invokeOnCancellation { runCatching { tts.stop() } }
                if (tts.synthesizeToFile(text, Bundle(), datei, kennung) != TextToSpeech.SUCCESS && k.isActive) k.resume(false)
            }
        } == true
        return datei.takeIf { ok && it.length() > 44 }
    }

    private fun fokusAnfordern(wecker: Boolean) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        fokus?.let { audio.abandonAudioFocusRequest(it) }
        fokus = AudioFocusRequest.Builder(if (wecker) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(Lautsprecher.attribute(sprache = true))
            .build()
            .also { runCatching { audio.requestAudioFocus(it) } }
    }

    private fun stille() {
        job?.cancel()
        job = null
        player?.let { p -> player = null; runCatching { p.stop() }; SpeechLoudness.release(p); runCatching { p.release() } }
        runCatching { geraeteStimme?.stop() }
    }

    /** Beendet die Erinnerung [id] (Ausschalten, Erledigt, Schlummern, Wegwischen, Öffnen). */
    private fun stoppeIntern(id: Long) {
        warteschlange.removeAll { it.id == id }
        runCatching { NotificationManagerCompat.from(this).cancel(id.toInt()) }
        val a = aktuell
        if (a != null && a.id == id) {
            stille()
            weiter(a, entfernen = true)
        }
    }

    /** Nach dem Ende von [vorher]: nächste Erinnerung aus der Warteschlange oder Dienst beenden. */
    private fun weiter(vorher: Auftrag, entfernen: Boolean) {
        stille()
        aktuell = null
        val naechste = warteschlange.removeFirstOrNull()
        if (naechste != null) {
            // startForeground mit neuer Nummer entfernt die alte Benachrichtigung; nach natürlichem Ende neu zeigen.
            vordergrund(naechste.id.toInt(), benachrichtigung(naechste, imDienst = true))
            if (!entfernen) zeige(vorher)
            starte(naechste)
        } else {
            beenden(vorher, entfernen)
        }
    }

    private fun beenden(vorher: Auftrag?, entfernen: Boolean) {
        stille()
        aktuell = null
        ServiceCompat.stopForeground(this, if (entfernen) ServiceCompat.STOP_FOREGROUND_REMOVE else ServiceCompat.STOP_FOREGROUND_DETACH)
        if (vorher != null && !entfernen) zeige(vorher)
        runCatching { NotificationManagerCompat.from(this).cancel(PLATZHALTER_ID) }
        getSystemService(AudioManager::class.java)?.let { audio -> fokus?.let { audio.abandonAudioFocusRequest(it) } }
        fokus = null
        lautstaerkeZurueck?.invoke()
        lautstaerkeZurueck = null
        wake?.let { if (it.isHeld) it.release() }
        wake = null
        stopSelf()
    }

    override fun onDestroy() {
        if (instanz === this) instanz = null
        stille()
        runCatching { geraeteStimme?.shutdown() }
        geraeteStimme = null
        lautstaerkeZurueck?.invoke()
        lautstaerkeZurueck = null
        wake?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ErinnerungsDienst"
        private const val EXTRA_WANN = "wann"
        const val PLATZHALTER_ID = 1_000_000_007
        private const val WECKER_MAX_MS = 60 * 60_000L
        private const val HINWEIS_MAX_MS = 5 * 60_000L
        private const val TON_MAX_MS = 30_000L
        private const val SATZ_MAX_MS = 60_000L

        @Volatile private var instanz: ErinnerungsDienst? = null

        /** Startet die Erinnerung. Wirft, wenn Android den Vordergrund-Dienst nicht erlaubt. */
        fun starte(context: Context, id: Long, wann: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ErinnerungsDienst::class.java).putExtra(Planer.ID, id).putExtra(EXTRA_WANN, wann),
            )
        }

        /** Ende des Fokus-Timers: Ton und einmal [Fokus.TEXT]. Wirft, wenn Android den Vordergrund-Dienst nicht erlaubt. */
        fun starteFokus(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ErinnerungsDienst::class.java).setAction(Fokus.AKTION_ENDE))
        }

        /** Beendet Vorlesen/Wecker der Aufgabe [id], falls gerade aktiv oder wartend. */
        fun stoppe(context: Context, id: Long) {
            val d = instanz ?: return
            Handler(Looper.getMainLooper()).post { d.stoppeIntern(id) }
        }
    }
}
