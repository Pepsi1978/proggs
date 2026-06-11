package de.frank.voicekey.a11y

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import de.frank.voicekey.data.AppTarget
import de.frank.voicekey.obs.Obs
import de.frank.voicekey.trigger.AssistantStopper

/**
 * Erkennt das Wegwischen der ChatGPT-Voice-Kugel und beendet die dann unsichtbar
 * weiterlaufende Session zuverlaessig.
 *
 * Hintergrund (am Fold 6 verifiziert, 2026-06-11): Wird die Kugel weggewischt, verschwindet
 * nur die Oberflaeche — ChatGPTs Voice-Dienst hoert im Hintergrund weiter zu (Audio-Modus
 * bleibt MODE_IN_COMMUNICATION). ChatGPTs Kugel hat KEINEN Beenden-Knopf, und selbst der
 * In-App-Beenden-Knopf stoppt die Aufnahme nicht zuverlaessig. Was zuverlaessig wirkt: ein
 * KEYCODE_HEADSETHOOK an ChatGPTs Media-Session (siehe [AssistantStopper]).
 *
 * Aufgabe dieses Dienstes ist deshalb nur noch ERKENNEN, dass die ChatGPT-Kugel verschwunden
 * ist, waehrend die Voice-Session noch laeuft -> dann den Headsethook senden lassen.
 */
class VoiceKeyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var chatGptWindowVisible = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Obs.i("VoiceKeyA11y", "onServiceConnected", "Not-Aus-Dienst verbunden")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /**
     * Kugel weggewischt = ChatGPT-Voice wirklich aus. Verschwindet das ChatGPT-Fenster und die
     * Voice-Session laeuft trotzdem weiter, wird sie nach kurzer Wartezeit automatisch beendet.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        val visibleNow = isChatGptWindowVisible()
        if (chatGptWindowVisible && !visibleNow) {
            // Kurz warten — sonst greift es bei einem reinen Fensterwechsel innerhalb von ChatGPT.
            handler.postDelayed({ endIfStillRunning("Kugel weggewischt") }, DISMISS_DELAY_MS)
        }
        chatGptWindowVisible = visibleNow
    }

    override fun onInterrupt() = Unit

    /** Manueller Not-Aus (z. B. ueber den Notification-Knopf, wenn er ueber die Bedienungshilfe laeuft). */
    fun endAssistantSession(reason: String = "Bedienungshilfe") =
        AssistantStopper.endVoiceSession(this, reason)

    private fun endIfStillRunning(reason: String) {
        if (isChatGptWindowVisible()) return // Kugel/App wieder sichtbar -> Frank nutzt sie gerade
        if (!AssistantStopper.isVoiceSessionActive(this)) {
            Obs.d("VoiceKeyA11y", "endIfStillRunning", "Voice schon aus — nichts zu tun", mapOf("ausloeser" to reason))
            return
        }
        Obs.checkpoint(
            step = "Auto-Not-Aus",
            intent = "Kugel weggewischt = ChatGPT-Voice geht wirklich aus",
            expected = "wird beendet",
            actual = "wird beendet",
            ctx = mapOf("ausloeser" to reason),
        )
        AssistantStopper.endVoiceSession(this, reason)
    }

    /** Ist irgendwo (Fenster-Liste oder aktives Fenster) noch eine ChatGPT-Oberflaeche sichtbar? */
    private fun isChatGptWindowVisible(): Boolean {
        val roots = windows.mapNotNull { it.root }.toMutableList()
        rootInActiveWindow?.let { roots.add(it) }
        return roots.any { it.packageName == AppTarget.CHATGPT.packageName }
    }

    companion object {
        private const val DISMISS_DELAY_MS = 1_200L

        @Volatile
        var instance: VoiceKeyAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
