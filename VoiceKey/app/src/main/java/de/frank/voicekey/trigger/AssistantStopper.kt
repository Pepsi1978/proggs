package de.frank.voicekey.trigger

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import de.frank.voicekey.obs.Obs

/**
 * Beendet eine laufende ChatGPT-Voice-Session ZUVERLAESSIG — ohne Root, Shizuku oder
 * Bedienungshilfe-Klicks, nur mit Standard-Android-APIs.
 *
 * Am Galaxy Fold 6 (One UI 8 / Android 16) live verifiziert (2026-06-11):
 * - ChatGPT-Voice laeuft im Audio-Modus `MODE_IN_COMMUNICATION` (wie ein VoIP-Anruf).
 * - ChatGPT meldet eine MediaSession "VoiceModeService" als Media-Button-Empfaenger an.
 * - Ein `KEYCODE_HEADSETHOOK` an diese Session BEENDET die Voice-Session (Audio faellt danach auf
 *   `MODE_NORMAL`). `PAUSE`/`STOP`/`PLAY_PAUSE` ignoriert ChatGPT.
 * - ChatGPTs Kugel-Overlay hat KEINEN Beenden-Knopf; selbst der In-App-Beenden-Knopf stoppt die
 *   Aufnahme nicht zuverlaessig. Deshalb der Headsethook-Weg.
 *
 * Der Abbau dauert 1-3 s (HAL-/UI-Teardown), daher wird verzoegert verifiziert und bei Bedarf ein
 * zweiter Druck gesendet.
 */
object AssistantStopper {

    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var ending = false

    /**
     * true = eine Voice-/VoIP-Session haelt das Audio (ChatGPT-Voice = `MODE_IN_COMMUNICATION`).
     */
    fun isVoiceSessionActive(context: Context): Boolean =
        audio(context).mode == AudioManager.MODE_IN_COMMUNICATION

    /**
     * Beendet die laufende Voice-Session per Headsethook. Schickt bis zu [MAX_ATTEMPTS]
     * Tastendruecke (Abbau braucht Zeit) und verifiziert ueber den Audio-Modus, ob die Session
     * wirklich aus ist. Tut nichts, wenn keine Session aktiv ist (kein versehentlicher Start einer
     * neuen Session).
     */
    fun endVoiceSession(context: Context, reason: String) {
        val am = audio(context)
        // MODE_IN_CALL = echtes Telefonat -> niemals anfassen. Nur MODE_IN_COMMUNICATION
        // (ChatGPT/VoIP).
        if (am.mode != AudioManager.MODE_IN_COMMUNICATION) {
            Obs.checkpoint(
                step = "Assistent beenden",
                intent = "ChatGPT-Voice per Headsethook beenden",
                expected = "beendet",
                actual = "keine Voice-Session aktiv — nichts zu tun",
                ctx = mapOf("ausloeser" to reason, "modus" to am.mode),
            )
            return
        }
        if (ending) {
            Obs.d(
                "AssistantStopper",
                "endVoiceSession",
                "Lauf laeuft bereits",
                mapOf("ausloeser" to reason),
            )
            return
        }
        ending = true
        Obs.i(
            "AssistantStopper",
            "endVoiceSession",
            "Headsethook gesendet",
            mapOf("ausloeser" to reason),
        )
        sendHeadsetHook(am)
        handler.postDelayed({ verifyOrRetry(am, attempt = 1) }, VERIFY_DELAY_MS)
    }

    /** Prueft nach dem Teardown, ob die Session aus ist; sonst noch einmal Headsethook. */
    private fun verifyOrRetry(am: AudioManager, attempt: Int) {
        if (am.mode != AudioManager.MODE_IN_COMMUNICATION) {
            Obs.checkpoint(
                step = "Assistent beenden",
                intent = "ChatGPT-Voice per Headsethook beenden",
                expected = "beendet",
                actual = "beendet",
                ctx = mapOf("versuche" to attempt),
            )
            ending = false
            return
        }
        if (attempt >= MAX_ATTEMPTS) {
            Obs.checkpoint(
                step = "Assistent beenden",
                intent = "ChatGPT-Voice per Headsethook beenden",
                expected = "beendet",
                actual = "NOCH aktiv nach $attempt Versuchen",
                ctx = mapOf("modus" to am.mode),
            )
            ending = false
            return
        }
        Obs.w(
            "AssistantStopper",
            "verifyOrRetry",
            "Noch aktiv — erneuter Headsethook",
            mapOf("versuch" to attempt + 1),
        )
        sendHeadsetHook(am)
        handler.postDelayed({ verifyOrRetry(am, attempt + 1) }, VERIFY_DELAY_MS)
    }

    /** Schickt einen Headset-Hook-Tastendruck (DOWN+UP) an die aktive Media-Button-Session. */
    private fun sendHeadsetHook(am: AudioManager) {
        val t = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(
            KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0)
        )
        am.dispatchMediaKeyEvent(
            KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK, 0)
        )
    }

    private fun audio(context: Context): AudioManager =
        context.getSystemService(AudioManager::class.java)

    private const val VERIFY_DELAY_MS = 1_800L
    private const val MAX_ATTEMPTS = 3
}
