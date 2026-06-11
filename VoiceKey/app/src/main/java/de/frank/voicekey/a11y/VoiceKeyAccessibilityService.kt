package de.frank.voicekey.a11y

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.frank.voicekey.data.AppTarget
import de.frank.voicekey.obs.Obs
import de.frank.voicekey.trigger.AssistantLauncher

/**
 * Not-Aus-Rückfallebene: beendet die ChatGPT-Voice-Session WIRKLICH.
 *
 * Hintergrund (am Gerät verifiziert): Rück-Geste und Recents-Wegwischen schließen nur die
 * Kugel-UI — ChatGPTs Voice-Dienst hört im Hintergrund weiter zu. Eine normale App darf
 * fremde Apps nicht stoppen. Dieser Bedienungshilfen-Dienst drückt deshalb den ECHTEN
 * "Beenden"-Knopf der Voice-Oberfläche (content-desc "Beenden", am Gerät ausgelesen).
 *
 * Ablauf von endAssistantSession():
 * 1. "Beenden"-Knoten in den aktuellen Fenstern suchen -> klicken -> fertig.
 * 2. Kugel-Overlay offen (unbeschriftete Knoten)? -> kleinen Knopf unten links klicken
 *    (öffnet die In-App-Voice-UI mit beschriftetem "Beenden") -> weiter bei 1.
 * 3. Gar keine ChatGPT-UI sichtbar (Session läuft unsichtbar)? -> Kugel per Trigger hochholen
 *    -> weiter bei 2.
 * Danach: HOME, damit nichts von ChatGPT offen stehen bleibt.
 */
class VoiceKeyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var killRunning = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Obs.i("VoiceKeyA11y", "onServiceConnected", "Not-Aus-Dienst verbunden")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private var chatGptWindowVisible = false
    private var lastAutoKillAt = 0L

    /**
     * AUTOMATIK (Frank-Wunsch): Kugel wegwischen = ChatGPT-Voice wirklich aus.
     * Verschwindet das ChatGPT-Fenster, aber unsere Aufnahme bleibt stummgeschaltet
     * (= ChatGPT hoert weiter zu), wird der Beenden-Flow automatisch ausgefuehrt.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        val visibleNow = isChatGptWindowVisible()
        if (chatGptWindowVisible && !visibleNow && !killRunning) {
            handler.postDelayed({ requestAutoKillCheck("Kugel-Fenster verschwunden") }, 1_500)
        }
        chatGptWindowVisible = visibleNow
    }

    override fun onInterrupt() = Unit

    private fun isChatGptWindowVisible(): Boolean =
        allRoots().any { it.packageName == AppTarget.CHATGPT.packageName }

    /**
     * Prueft mit Schutzvorkehrungen, ob die ChatGPT-Session unsichtbar weiterlaeuft,
     * und beendet sie dann automatisch. Schutz: kein Eingriff waehrend Telefonaten,
     * nicht waehrend eines laufenden Beenden-Flows, Abkuehlzeit gegen Doppel-Laeufe.
     */
    fun requestAutoKillCheck(reason: String) {
        if (killRunning) return
        if (System.currentTimeMillis() - lastAutoKillAt < AUTO_KILL_COOLDOWN_MS) return
        val audio = getSystemService(android.media.AudioManager::class.java)
        if (audio.mode == android.media.AudioManager.MODE_IN_CALL ||
            audio.mode == android.media.AudioManager.MODE_IN_COMMUNICATION
        ) {
            Obs.d("VoiceKeyA11y", "requestAutoKillCheck", "Telefonat aktiv — kein Auto-Not-Aus")
            return
        }
        if (isChatGptWindowVisible()) return // Kugel/App sichtbar -> Frank benutzt sie gerade
        if (!de.frank.voicekey.service.WakeWordService.micSilenced) {
            Obs.d("VoiceKeyA11y", "requestAutoKillCheck", "Mic frei — Session ist schon aus", mapOf("ausloeser" to reason))
            return
        }
        lastAutoKillAt = System.currentTimeMillis()
        Obs.checkpoint(
            step = "Auto-Not-Aus",
            intent = "Kugel weggewischt = ChatGPT-Voice geht wirklich aus",
            expected = "wird beendet",
            actual = "wird beendet",
            ctx = mapOf("ausloeser" to reason),
        )
        endAssistantSession()
    }

    /** Startet die Beenden-Sequenz. Mehrfach-Aufrufe während eines Laufs werden ignoriert. */
    fun endAssistantSession() {
        if (killRunning) {
            Obs.d("VoiceKeyA11y", "endAssistantSession", "Lauf ignoriert — Sequenz aktiv")
            return
        }
        killRunning = true
        Obs.i("VoiceKeyA11y", "endAssistantSession", "Not-Aus gestartet")
        attempt(step = 0)
    }

    private fun attempt(step: Int) {
        if (step >= MAX_STEPS) {
            Obs.checkpoint(
                step = "Assistent beendet",
                intent = "Not-Aus soll die ChatGPT-Voice-Session beenden",
                expected = "beendet",
                actual = "TIMEOUT — Beenden-Knopf nicht gefunden",
            )
            killRunning = false
            return
        }

        // 1) Beschrifteten Beenden-Knopf suchen (In-App-Voice-UI).
        val endNode = findEndButton()
        if (endNode != null) {
            val clicked = endNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Obs.checkpoint(
                step = "Assistent beendet",
                intent = "Not-Aus soll die ChatGPT-Voice-Session beenden",
                expected = "beendet",
                actual = if (clicked) "beendet" else "Klick fehlgeschlagen",
                ctx = mapOf("knopf" to (endNode.contentDescription ?: endNode.text ?: "?")),
            )
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
                killRunning = false
            }, 800)
            return
        }

        // 2) Kugel-Overlay: unbeschrifteter kleiner Knopf unten links öffnet die In-App-UI.
        val orbButton = findOrbSmallButton()
        if (orbButton != null) {
            orbButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Obs.i("VoiceKeyA11y", "attempt", "Kugel-Knopf geklickt — warte auf In-App-Voice-UI", mapOf("step" to step))
            handler.postDelayed({ attempt(step + 1) }, STEP_DELAY_MS)
            return
        }

        // 3) Keine ChatGPT-UI sichtbar: Kugel hochholen, dann erneut versuchen.
        if (step == 0) {
            Obs.i("VoiceKeyA11y", "attempt", "Keine Voice-UI sichtbar — hole Kugel hoch")
            try {
                startActivity(
                    AssistantLauncher.buildIntent(AppTarget.CHATGPT)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Obs.e("VoiceKeyA11y", "attempt", "Kugel-Start fehlgeschlagen", emptyMap(), e)
            }
        }
        handler.postDelayed({ attempt(step + 1) }, STEP_DELAY_MS)
    }

    /** Alle erreichbaren Fenster-Wurzeln (windows-API + aktives Fenster als Fallback). */
    private fun allRoots(): List<AccessibilityNodeInfo> {
        val roots = windows.mapNotNull { it.root }.toMutableList()
        rootInActiveWindow?.let { active ->
            if (roots.none { it.packageName == active.packageName && it.windowId == active.windowId }) {
                roots.add(active)
            }
        }
        return roots
    }

    /** Sucht in allen Fenstern einen Knoten, dessen Beschriftung "Beenden"/"End"/"Stop"/"Close" ist. */
    private fun findEndButton(): AccessibilityNodeInfo? {
        allRoots().forEach { root ->
            val match = findNode(root) { node ->
                if (!node.isClickable && !node.isFocusable) return@findNode false
                val label = (node.contentDescription ?: node.text ?: "").toString().trim().lowercase()
                label.isNotEmpty() && (
                    label == "beenden" || label.contains("beenden") ||
                        label == "end" || label.contains("end voice") ||
                        label.contains("sprachmodus beenden") ||
                        label == "stop" || label == "close"
                    )
            }
            if (match != null) return match
        }
        return null
    }

    /**
     * Kugel-Heuristik: Im Overlay gibt es nur unbeschriftete Klick-Flächen — eine riesige
     * (die Kugel) und einen kleinen Knopf unten links (öffnet die In-App-Voice-UI).
     * Wir klicken den kleinsten klickbaren ChatGPT-Knoten im unteren Bildschirm-Drittel.
     */
    private fun findOrbSmallButton(): AccessibilityNodeInfo? {
        val metrics = resources.displayMetrics
        val screenArea = metrics.widthPixels.toLong() * metrics.heightPixels.toLong()
        var best: AccessibilityNodeInfo? = null
        var bestArea = Long.MAX_VALUE
        allRoots().forEach { root ->
            if (root.packageName != AppTarget.CHATGPT.packageName) return@forEach
            collectClickable(root) { node ->
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)
                val area = rect.width().toLong() * rect.height().toLong()
                val inLowerThird = rect.centerY() > metrics.heightPixels * 2 / 3
                val small = area in 1 until screenArea / 10
                if (small && inLowerThird && area < bestArea) {
                    best = node
                    bestArea = area
                }
            }
        }
        return best
    }

    private fun findNode(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findNode(child, predicate)?.let { return it }
        }
        return null
    }

    private fun collectClickable(root: AccessibilityNodeInfo, visit: (AccessibilityNodeInfo) -> Unit) {
        if (root.isClickable) visit(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            collectClickable(child, visit)
        }
    }

    companion object {
        private const val MAX_STEPS = 8
        private const val STEP_DELAY_MS = 900L
        private const val AUTO_KILL_COOLDOWN_MS = 10_000L

        @Volatile
        var instance: VoiceKeyAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
