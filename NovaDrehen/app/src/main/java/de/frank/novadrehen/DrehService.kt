package de.frank.novadrehen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

// Sperrt die Drehung, solange Nova vorne ist und das Fold zugeklappt ist.
// Stellt die automatische Drehung wieder her, sobald eine App vorne ist oder aufgeklappt wird.
class DrehService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var vorne: String? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) = pruefen()
    }

    override fun onServiceConnected() {
        getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, handler)
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
        freigeben()
        super.onDestroy()
    }

    private fun pruefen() {
        if (!Settings.System.canWrite(this)) return
        if (vorne in NOVA && zugeklappt()) sperren() else freigeben()
    }

    private fun zugeklappt(): Boolean {
        val b = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val kurz = minOf(b.width(), b.height()) / resources.displayMetrics.density
        return kurz < 500f
    }

    private fun sperren() {
        val cr = contentResolver
        if (Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) != 1) return
        prefs().edit().putBoolean(GESPERRT, true).apply()
        Settings.System.putInt(cr, Settings.System.USER_ROTATION, 0)
        Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
    }

    private fun freigeben() {
        if (!prefs().getBoolean(GESPERRT, false)) return
        prefs().edit().putBoolean(GESPERRT, false).apply()
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
    }

    private fun tastaturPaket(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    private fun prefs() = getSharedPreferences("drehen", Context.MODE_PRIVATE)

    companion object {
        private const val GESPERRT = "von_uns_gesperrt"
        private val NOVA = setOf("com.teslacoilsw.launcher")
        private val IGNORIEREN = setOf("com.android.systemui", "de.frank.novadrehen")
    }
}
