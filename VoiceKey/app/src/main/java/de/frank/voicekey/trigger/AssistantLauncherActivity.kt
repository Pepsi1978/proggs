package de.frank.voicekey.trigger

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import de.frank.voicekey.data.AppTarget
import de.frank.voicekey.obs.Obs

/**
 * Trampolin-Activity (transparent, showWhenLocked + turnScreenOn im Manifest):
 * - Bildschirm an & entsperrt -> feuert den Trigger sofort.
 * - Bildschirm aus/gesperrt -> weckt den Bildschirm; bei sicherem Lockscreen muss der Nutzer 1x per
 *   Fingerprint entsperren (Android-Plattformgrenze, NEXT-SESSION §3), danach feuert der Trigger
 *   automatisch. Loest gleichzeitig das Background-Activity-Launch-Problem des Dienstes.
 */
class AssistantLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyguard = getSystemService(KeyguardManager::class.java)
        val locked = keyguard.isKeyguardLocked
        Obs.checkpoint(
            step = "Trampolin gestartet",
            intent = "Bildschirm wecken und Voice-Trigger vorbereiten",
            expected = "erreicht",
            actual = "erreicht",
            ctx = mapOf("keyguardLocked" to locked),
        )

        if (locked) {
            // Weckt + zeigt die Entsperr-Aufforderung; nach Fingerprint geht es weiter.
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = fireAndFinish()

                    override fun onDismissCancelled() {
                        Obs.w(
                            "AssistantLauncherActivity",
                            "onDismissCancelled",
                            "Entsperren abgebrochen — Trigger nicht gefeuert",
                        )
                        finish()
                    }

                    override fun onDismissError() {
                        Obs.e(
                            "AssistantLauncherActivity",
                            "onDismissError",
                            "Keyguard-Dismiss-Fehler — Trigger nicht gefeuert",
                        )
                        finish()
                    }
                },
            )
        } else {
            fireAndFinish()
        }
    }

    private fun fireAndFinish() {
        AssistantLauncher.launch(this, AppTarget.CHATGPT)
        finish()
    }
}
