package de.frank.novadrehen

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

// Sperrt die Drehung, solange Nova vorne ist und das Fold zugeklappt ist, sonst ist sie an.
// Setzt bei jedem Anlass den Soll-Zustand neu durch, statt sich einen eigenen Zustand zu merken.
class DrehService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var vorne: String? = null

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
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in IGNORIEREN || pkg == tastaturPaket()) return
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
        val sperren = vorne in NOVA && zu
        val ist = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
        Log.i(TAG, "vorne=$vorne zu=$zu ist=$ist soll=${if (sperren) 0 else 1}")
        when {
            sperren && ist != 0 -> {
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, 0)
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
            }
            !sperren && ist != 1 -> Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
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
        private val IGNORIEREN = setOf(
            "com.android.systemui", "de.frank.novadrehen", "com.sec.android.app.launcher"
        )
    }
}
