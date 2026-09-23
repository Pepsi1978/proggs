package de.frank.updatestation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * Schaltet nach dem Handy-Neustart "Debugging über WLAN" wieder ein, damit der PC
 * ohne Kabel verbinden kann (Werkzeuge/adb-wlan). Braucht WRITE_SECURE_SETTINGS,
 * das adb-wlan.ps1 bei jeder Kabelverbindung per `pm grant` vergibt.
 */
class WlanDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        einschalten(context)
    }

    companion object {
        private const val TAG = "WlanDebug"

        fun einschalten(context: Context) {
            val erlaubt = context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
            if (!erlaubt) {
                Log.w(TAG, "WRITE_SECURE_SETTINGS fehlt – einmal per Kabel adb-wlan.ps1 ausführen")
                return
            }
            try {
                val cr = context.contentResolver
                if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) != 1) return
                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) != 1) {
                    Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                    Log.i(TAG, "Debugging über WLAN wieder eingeschaltet")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Einschalten fehlgeschlagen", e)
            }
        }
    }
}
