package de.frank.updatestation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Schaltet "Debugging über WLAN" wieder ein, damit der PC ohne Kabel verbinden kann
 * (Werkzeuge/adb-wlan). Braucht WRITE_SECURE_SETTINGS, das adb-wlan.ps1 bei jeder
 * Kabelverbindung per `pm grant` vergibt.
 *
 * Android lässt WLAN-Debugging nur bei verbundenem WLAN an – beim Boot ist das WLAN meist
 * noch nicht da. Deshalb läuft das Einschalten als einmalige WorkManager-Aufgabe mit der
 * Bedingung "WLAN verbunden". (Ein PendingIntent-NetworkCallback taugt nicht: Android 17
 * räumt ihn ab, sobald die App in den Hintergrund-Ruhezustand geht.)
 * Pro WLAN-Verbindung höchstens ein Versuch: so erscheint die Android-Nachfrage in fremden
 * Netzen nicht ständig, und ein Ausschalten von Hand bleibt bis zur nächsten Verbindung bestehen.
 */
class WlanDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // BOOT_COMPLETED / MY_PACKAGE_REPLACED: auf WLAN warten und dann einschalten
        planen(context)
    }

    /** Läuft, sobald ein WLAN (nicht getaktetes Netz) verbunden ist. */
    class Aufgabe(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            if (einschalten(applicationContext)) return Result.success()
            // Android hat den Wert wieder zurückgesetzt (WLAN noch ohne IP): später erneut, begrenzt
            return if (runAttemptCount < MAX_VERSUCHE) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val TAG = "WlanDebug"
        private const val ARBEIT = "wlan-debug-einschalten"
        private const val PREFS = "wlan_debug"
        private const val LETZTES_NETZ = "letztes_netz"
        private const val MAX_VERSUCHE = 5

        /** Idempotent: eine bereits wartende Aufgabe bleibt bestehen (KEEP), nichts stapelt sich. */
        fun planen(context: Context) {
            try {
                val arbeit = OneTimeWorkRequestBuilder<Aufgabe>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                    .build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(ARBEIT, ExistingWorkPolicy.KEEP, arbeit)
            } catch (e: Exception) {
                Log.e(TAG, "Planen fehlgeschlagen", e)
            }
        }

        /**
         * Läuft im Worker-Thread (blockiert kurz). Liefert false, wenn ein späterer Versuch sinnvoll ist.
         * Android setzt adb_wifi_enabled binnen ~1 s zurück, wenn das WLAN noch keine IP hat – deshalb
         * nach dem Setzen nachprüfen und die Verbindung erst dann als erledigt merken.
         * Eine offene Android-Nachfrage (fremdes Netz) lässt den Wert auf 1 – dann kein Wiederholen.
         */
        fun einschalten(context: Context): Boolean {
            val app = context.applicationContext
            val erlaubt = app.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
            if (!erlaubt) {
                Log.w(TAG, "WRITE_SECURE_SETTINGS fehlt – einmal adb-wlan.ps1 ausführen")
                return true
            }
            val cm = app.getSystemService(ConnectivityManager::class.java) ?: return true
            val wlan = wlanNetz(cm)
            if (wlan == null) {
                // Nicht selbst neu planen (Schleifengefahr): WorkManager wiederholt begrenzt.
                Log.i(TAG, "Kein WLAN mit IP verbunden – später erneut")
                return false
            }
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val kennung = wlan.networkHandle
            if (prefs.getLong(LETZTES_NETZ, 0L) == kennung) return true
            return try {
                val cr = app.contentResolver
                if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) != 1) return true
                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) != 1) {
                    Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                    Thread.sleep(3000)
                    if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) != 1) {
                        Log.w(TAG, "Android hat Debugging über WLAN wieder abgeschaltet – später erneut")
                        return false
                    }
                    Log.i(TAG, "Debugging über WLAN wieder eingeschaltet")
                }
                prefs.edit().putLong(LETZTES_NETZ, kennung).apply()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Einschalten fehlgeschlagen", e)
                true
            }
        }

        /** WLAN mit zugewiesener IPv4-Adresse – vorher hält Android WLAN-Debugging nicht an. */
        private fun istWlan(cm: ConnectivityManager, n: Network): Boolean =
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                cm.getLinkProperties(n)?.linkAddresses?.any { it.address is java.net.Inet4Address } == true

        @Suppress("DEPRECATION")
        private fun wlanNetz(cm: ConnectivityManager): Network? =
            cm.allNetworks.firstOrNull { istWlan(cm, it) }
    }
}
