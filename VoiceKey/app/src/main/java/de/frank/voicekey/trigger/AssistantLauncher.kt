package de.frank.voicekey.trigger

import android.content.Context
import android.content.Intent
import de.frank.voicekey.data.AppTarget
import de.frank.voicekey.obs.Obs

/**
 * Baut und feuert den am Fold 6 VERIFIZIERTEN Trigger: direkter Component-Start der
 * ChatGPT-Voice-Activity (NEXT-SESSION §1). Funktioniert ohne Root/Shizuku/Sonderrechte.
 */
object AssistantLauncher {

    fun buildIntent(target: AppTarget): Intent =
        Intent()
            .setClassName(target.packageName, target.activityClass)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Trigger-Waechter: prueft, ob die Ziel-Activity noch existiert. Faellt sie nach einem
     * ChatGPT-Update weg, meldet das ein ok:false-Checkpoint, statt still ins Leere zu laufen.
     */
    fun isTargetAvailable(context: Context, target: AppTarget = AppTarget.CHATGPT): Boolean {
        val resolved = context.packageManager.resolveActivity(buildIntent(target), 0) != null
        Obs.probe(
            resolved,
            "Ziel-Activity nicht auffindbar — hat ein ChatGPT-Update sie umbenannt? " +
                "Neu ermitteln: adb shell input keyevent 219 + dumpsys activity activities",
            "AssistantLauncher", "isTargetAvailable",
            mapOf("package" to target.packageName, "activity" to target.activityClass),
        )
        return resolved
    }

    /** Feuert den Trigger. Rueckgabe: hat der Activity-Start geklappt? */
    fun launch(context: Context, target: AppTarget = AppTarget.CHATGPT): Boolean {
        val intent = buildIntent(target)
        val result = try {
            context.startActivity(intent)
            "gestartet"
        } catch (e: Exception) {
            Obs.e("AssistantLauncher", "launch", "Trigger fehlgeschlagen", mapOf("target" to target.name), e)
            "FEHLER: ${e.javaClass.simpleName}"
        }
        Obs.checkpoint(
            step = "ChatGPT-Voice ausloesen",
            intent = "Wake-Word soll den Voice-Mode oeffnen wie der Side-Key",
            expected = "gestartet",
            actual = result,
            ctx = mapOf("component" to "${target.packageName}/${target.activityClass}"),
        )
        return result == "gestartet"
    }
}
