package de.frank.novadrehen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

// Sperrt die Drehung, solange Nova vorne ist und das Fold zugeklappt ist, und solange der
// ChatGPT-Sprachmodus (Kugel nach langem Druck auf die Seitentaste) offen ist, sonst ist sie an.
// Setzt bei jedem Anlass den Soll-Zustand neu durch, statt sich einen eigenen Zustand zu merken.
class DrehService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var vorne: String? = null
    private var chatGptSprache = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) = pruefen()
    }

    private val drehObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = pruefen()
    }

    override fun onServiceConnected() {
        Log.i(TAG, "gestartet")
        getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, handler)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, drehObserver
        )
        pruefen()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            // Kugel verschwunden, ohne dass eine andere App nach vorne kam: Sperre trotzdem lösen.
            if (chatGptSprache && !chatGptFensterOffen()) {
                chatGptSprache = false
                pruefen()
            }
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in IGNORIEREN || pkg == tastaturPaket()) return
        val klasse = event.className?.toString().orEmpty()
        if (pkg == CHATGPT) {
            when {
                istSprachmodus(klasse) -> chatGptSprache = true
                istActivity(pkg, klasse) -> chatGptSprache = false
                // Dialoge und Teilfenster innerhalb von ChatGPT ändern den Modus nicht.
                vorne != CHATGPT -> chatGptSprache = false
            }
        } else {
            chatGptSprache = false
        }
        vorne = pkg
        pruefen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pruefen()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        contentResolver.unregisterContentObserver(drehObserver)
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
        super.onDestroy()
    }

    private fun pruefen() {
        if (!Settings.System.canWrite(this)) return
        val cr = contentResolver
        val zu = zugeklappt()
        val novaSperre = vorne in NOVA && zu
        val sperren = novaSperre || chatGptSprache
        val ist = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
        Log.i(TAG, "vorne=$vorne zu=$zu sprache=$chatGptSprache ist=$ist soll=${if (sperren) 0 else 1}")
        when {
            sperren && ist != 0 -> {
                // Nova: immer Hochformat. ChatGPT-Sprachmodus: die gerade gezeigte Lage einfrieren,
                // damit beim Sperren selbst kein Drehen passiert, das die Sitzung abbrechen würde.
                val lage = if (novaSperre) 0 else aktuelleLage()
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, lage)
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
            }
            // Grund wechselt bei schon gesperrter Drehung zu Nova (z. B. Sprachmodus aufgeklappt quer
            // eingefroren, dann zugeklappt): Nova muss trotzdem ins Hochformat.
            novaSperre && Settings.System.getInt(cr, Settings.System.USER_ROTATION, 0) != 0 ->
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, 0)
            !sperren && ist != 1 -> Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
    }

    private fun aktuelleLage(): Int =
        getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

    // Liest nur den Paketnamen der offenen App-Fenster, keine Inhalte. Ist ein Fenster gerade
    // nicht lesbar, gilt es vorsichtshalber als ChatGPT, damit die Sperre mitten im Gespräch hält.
    private fun chatGptFensterOffen(): Boolean = windows.any {
        if (it.type != AccessibilityWindowInfo.TYPE_APPLICATION) return@any false
        val paket = it.root?.packageName ?: return@any true
        paket == CHATGPT
    }

    private fun istSprachmodus(klasse: String): Boolean =
        klasse.startsWith("com.openai.voice.") || klasse.startsWith("com.openai.feature.assistant.")

    private fun istActivity(pkg: String, klasse: String): Boolean = try {
        packageManager.getActivityInfo(ComponentName(pkg, klasse), 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun zugeklappt(): Boolean {
        val b = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val kurz = minOf(b.width(), b.height()) / resources.displayMetrics.density
        return kurz < 500f
    }

    private fun tastaturPaket(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    companion object {
        private const val TAG = "NovaDrehen"
        private val NOVA = setOf("com.teslacoilsw.launcher")
        private const val CHATGPT = "com.openai.chatgpt"
        private val IGNORIEREN = setOf(
            "com.android.systemui", "de.frank.novadrehen", "com.sec.android.app.launcher"
        )
    }
}
