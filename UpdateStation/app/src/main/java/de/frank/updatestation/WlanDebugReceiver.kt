package de.frank.updatestation

import android.app.PendingIntent
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
import java.util.concurrent.TimeUnit

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
        if (intent.action == AKTION_BESTAETIGEN) {
            // Tipp auf die Benachrichtigung: Frank schaut gerade aufs Gerät, jetzt Androids Nachfrage auslösen.
            Benachrichtigungen.entferneWlanDebug(context)
            runCatching { Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1) }
                .onFailure { Log.e(TAG, "Einschalten nach Tipp fehlgeschlagen", it) }
            pruefenPlanen(context, 60)
            return
        }
        // BOOT_COMPLETED / MY_PACKAGE_REPLACED: auf WLAN warten und dann einschalten
        planen(context)
    }

    /**
     * Prüft kurz nach dem Einschalten, ob Debugging über WLAN wirklich läuft. In einem neuen WLAN zeigt Android nur ein
     * Fenster („In diesem Netzwerk zulassen?“), das ungesehen verloren geht; dann bleibt der Wert 0. Solange das so ist
     * und das WLAN besteht: stehende Benachrichtigung, alle 2 Minuten neu prüfen. Läuft es oder ist das WLAN weg: entfernen.
     */
    class Pruefung(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            val app = applicationContext
            val cr = app.contentResolver
            val cm = app.getSystemService(ConnectivityManager::class.java)
            val erlaubt = app.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
            val offen = erlaubt && cm != null && wlanNetz(cm) != null &&
                Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1 &&
                Settings.Global.getInt(cr, "adb_wifi_enabled", 0) != 1
            if (!offen) {
                Benachrichtigungen.entferneWlanDebug(app)
                return Result.success()
            }
            val tippen = PendingIntent.getBroadcast(
                app, 43, Intent(app, WlanDebugReceiver::class.java).setAction(AKTION_BESTAETIGEN),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            Benachrichtigungen.wlanDebugBestaetigen(app, tippen)
            pruefenPlanen(app, 120)
            return Result.success()
        }
    }

    /**
     * Läuft, sobald irgendein Netz verbunden ist. Jedes WLAN zählt, auch ein getaktetes (Handy-Hotspot unterwegs):
     * Mit NetworkType.UNMETERED lief die Aufgabe dort nie, und der Server konnte Port 5555 nach einem Neustart nicht zurückholen.
     * Nur Mobilfunk: alle 2 Minuten erneut nachsehen, bis ein WLAN da ist (WorkManager kennt keine Bedingung „WLAN“).
     */
    class Aufgabe(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            val cm = applicationContext.getSystemService(ConnectivityManager::class.java)
            if (cm != null && wlanNetz(cm) == null) {
                spaeterNachsehen(applicationContext)
                return Result.success()
            }
            if (einschalten(applicationContext)) {
                pruefenPlanen(applicationContext, 60)
                return Result.success()
            }
            // Android hat den Wert wieder zurückgesetzt (WLAN noch ohne IP oder neues Netz mit offener Nachfrage):
            // später erneut, begrenzt; danach meldet die Prüfung es per Benachrichtigung.
            if (runAttemptCount < MAX_VERSUCHE) return Result.retry()
            pruefenPlanen(applicationContext, 60)
            return Result.success()
        }
    }

    companion object {
        private const val TAG = "WlanDebug"
        private const val ARBEIT = "wlan-debug-einschalten"
        private const val WARTEN = "wlan-debug-warten-auf-wlan"
        private const val PRUEFEN = "wlan-debug-pruefen"
        const val AKTION_BESTAETIGEN = "de.frank.updatestation.WLAN_DEBUG_BESTAETIGEN"

        /** Eine einzige wartende Prüfung (REPLACE), nach [sekunden]. */
        fun pruefenPlanen(context: Context, sekunden: Long) {
            try {
                val arbeit = OneTimeWorkRequestBuilder<Pruefung>().setInitialDelay(sekunden, TimeUnit.SECONDS).build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(PRUEFEN, ExistingWorkPolicy.REPLACE, arbeit)
            } catch (e: Exception) {
                Log.e(TAG, "Prüfung planen fehlgeschlagen", e)
            }
        }
        private const val PREFS = "wlan_debug"
        private const val LETZTES_NETZ = "letztes_netz"
        private const val MAX_VERSUCHE = 5

        /** Idempotent: eine bereits wartende Aufgabe bleibt bestehen (KEEP), nichts stapelt sich. */
        fun planen(context: Context) {
            try {
                val arbeit = OneTimeWorkRequestBuilder<Aufgabe>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(ARBEIT, ExistingWorkPolicy.KEEP, arbeit)
            } catch (e: Exception) {
                Log.e(TAG, "Planen fehlgeschlagen", e)
            }
        }

        /** Nur Mobilfunk: in 2 Minuten erneut prüfen (eigener Name, REPLACE: nie mehr als eine wartende Aufgabe). */
        private fun spaeterNachsehen(context: Context) {
            try {
                val arbeit = OneTimeWorkRequestBuilder<Aufgabe>()
                    .setInitialDelay(2, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(WARTEN, ExistingWorkPolicy.REPLACE, arbeit)
            } catch (e: Exception) {
                Log.e(TAG, "Nachsehen planen fehlgeschlagen", e)
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
        internal fun wlanNetz(cm: ConnectivityManager): Network? =
            cm.allNetworks.firstOrNull { istWlan(cm, it) }
    }
}
