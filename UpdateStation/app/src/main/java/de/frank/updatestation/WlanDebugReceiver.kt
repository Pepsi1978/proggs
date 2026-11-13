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
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Schaltet "Debugging über WLAN" wieder ein, damit PC, Mac und der Hostinger-Server ohne Kabel verbinden können
 * (Werkzeuge/adb-wlan; der Server holt danach über den WireGuard-Tunnel Port 5555 zurück). Braucht WRITE_SECURE_SETTINGS,
 * das adb-wlan.ps1/.sh per `pm grant` vergibt.
 *
 * Android lässt WLAN-Debugging nur bei verbundenem WLAN an – beim Boot ist das WLAN meist noch nicht da. Deshalb läuft das
 * Einschalten als WorkManager-Aufgabe. Jedes WLAN zählt, auch ein getaktetes (Handy-Hotspot unterwegs; mit UNMETERED lief die
 * Aufgabe dort nie). (Ein PendingIntent-NetworkCallback taugt nicht: Android 17 räumt ihn ab, sobald die App ruht.)
 * Pro WLAN-Verbindung ein Versuch: Ein Ausschalten von Hand bleibt bis zur nächsten Verbindung bestehen.
 *
 * In einem neuen WLAN fragt Android per Fenster „In diesem Netzwerk zulassen?“ und setzt den Wert bis zur Antwort auf 0
 * (AOSP AdbDebuggingManager). Das Fenster geht in der Hosentasche ungesehen verloren. Dann übernimmt [Pruefung]:
 * stehende Benachrichtigung, Tipp löst die Nachfrage neu aus ([WlanDebugTipp], nicht exportiert).
 */
class WlanDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // BOOT_COMPLETED / MY_PACKAGE_REPLACED: auf WLAN warten und dann einschalten
        planen(context)
    }

    /** Ergebnis von [einschalten]. */
    enum class Stand { FERTIG, KEIN_WLAN, NACHFRAGE }

    /**
     * Läuft, sobald irgendein Netz verbunden ist. Nur Mobilfunk: später erneut nachsehen, mit wachsendem Abstand
     * (2, 4, 8, 16, höchstens 30 Minuten), bis ein WLAN da ist (WorkManager kennt keine Bedingung „WLAN“).
     */
    class Aufgabe(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): ListenableWorker.Result {
            when (einschalten(applicationContext)) {
                Stand.KEIN_WLAN -> nachsehenPlanen(applicationContext, inputData.getInt(STUFE, 0) + 1)
                Stand.NACHFRAGE -> pruefenPlanen(applicationContext, 60, 0)
                Stand.FERTIG -> Unit
            }
            return ListenableWorker.Result.success()
        }
    }

    /**
     * Läuft nur für das WLAN, in dem [einschalten] die Nachfrage ausgelöst hat ([PRUEF_NETZ]). Stufe 0: einmal still erneut
     * einschalten (falls Android nur kurz zurückgesetzt hat). Danach: stehende Benachrichtigung, Prüfung mit wachsendem
     * Abstand (2 bis 30 Minuten). Läuft es, ist das WLAN weg oder hat Frank „Nicht in diesem Netz“ gewählt: entfernen, Schluss.
     */
    class Pruefung(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): ListenableWorker.Result {
            val app = applicationContext
            val cr = app.contentResolver
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cm = app.getSystemService(ConnectivityManager::class.java)
            val netz = cm?.let { wlanNetz(it) }?.networkHandle
            val stufe = inputData.getInt(STUFE, 0)
            val zustaendig = netz != null && netz == prefs.getLong(PRUEF_NETZ, 0L) && netz != prefs.getLong(ABGELEHNT_NETZ, 0L) &&
                darfSchreiben(app) && Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
            if (!zustaendig || Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) {
                if (zustaendig) prefs.edit().remove(PRUEF_NETZ).apply()
                Benachrichtigungen.entferneWlanDebug(app)
                return ListenableWorker.Result.success()
            }
            if (stufe == 0) {
                runCatching { Settings.Global.putInt(cr, "adb_wifi_enabled", 1); Thread.sleep(3000) }
                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) {
                    prefs.edit().remove(PRUEF_NETZ).apply()
                    return ListenableWorker.Result.success()
                }
            }
            // Nicht zustellbar (Benachrichtigungen aus): nicht weiter alle paar Minuten aufwachen.
            if (!Benachrichtigungen.wlanDebugBestaetigen(app, tippen(app, AKTION_BESTAETIGEN, 43), tippen(app, AKTION_ABLEHNEN, 44))) {
                return ListenableWorker.Result.success()
            }
            val naechste = stufe + 1
            pruefenPlanen(app, minOf(120L shl minOf(stufe, 4), 1800L), naechste)
            return ListenableWorker.Result.success()
        }
    }

    companion object {
        private const val TAG = "WlanDebug"
        private const val ARBEIT = "wlan-debug-einschalten"
        private const val WARTEN = "wlan-debug-warten-auf-wlan"
        private const val PRUEFEN = "wlan-debug-pruefen"
        private const val STUFE = "stufe"
        private const val PREFS = "wlan_debug"
        private const val LETZTES_NETZ = "letztes_netz"
        private const val PRUEF_NETZ = "pruef_netz"
        private const val ABGELEHNT_NETZ = "abgelehnt_netz"
        const val AKTION_BESTAETIGEN = "de.frank.updatestation.WLAN_DEBUG_BESTAETIGEN"
        const val AKTION_ABLEHNEN = "de.frank.updatestation.WLAN_DEBUG_NICHT_HIER"

        private fun anfrage(verzoegerungSek: Long, stufe: Int): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<Aufgabe>()
                .setInitialDelay(verzoegerungSek, TimeUnit.SECONDS)
                .setInputData(workDataOf(STUFE to stufe))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        private fun einreihen(context: Context, name: String, policy: ExistingWorkPolicy, arbeit: OneTimeWorkRequest) {
            try {
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(name, policy, arbeit)
            } catch (e: Exception) {
                Log.e(TAG, "Planen ($name) fehlgeschlagen", e)
            }
        }

        /** Idempotent: eine bereits wartende Aufgabe bleibt bestehen (KEEP), nichts stapelt sich. */
        fun planen(context: Context) = einreihen(context, ARBEIT, ExistingWorkPolicy.KEEP, anfrage(0, 0))

        /** Nur Mobilfunk: später erneut (eigener Name, REPLACE: nie mehr als eine wartende Aufgabe). */
        private fun nachsehenPlanen(context: Context, stufe: Int) =
            einreihen(context, WARTEN, ExistingWorkPolicy.REPLACE, anfrage(minOf(120L shl minOf(stufe - 1, 4), 1800L), stufe))

        /** Eine einzige wartende Prüfung (REPLACE). */
        fun pruefenPlanen(context: Context, sekunden: Long, stufe: Int) =
            einreihen(context, PRUEFEN, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<Pruefung>().setInitialDelay(sekunden, TimeUnit.SECONDS)
                    .setInputData(workDataOf(STUFE to stufe)).build())

        private fun tippen(app: Context, aktion: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            app, code, Intent(app, WlanDebugTipp::class.java).setAction(aktion),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Von [WlanDebugTipp]: Frank hat auf die Benachrichtigung getippt oder „Nicht in diesem Netz“ gewählt. */
        internal fun tipp(context: Context, aktion: String?) {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            Benachrichtigungen.entferneWlanDebug(app)
            if (aktion == AKTION_ABLEHNEN) {
                prefs.edit().putLong(ABGELEHNT_NETZ, prefs.getLong(PRUEF_NETZ, 0L)).remove(PRUEF_NETZ).apply()
                return
            }
            if (aktion != AKTION_BESTAETIGEN || !darfSchreiben(app)) return
            // Frank schaut gerade aufs Gerät: jetzt Androids Nachfrage auslösen, danach nachprüfen (ohne stillen Zweitversuch).
            runCatching { Settings.Global.putInt(app.contentResolver, "adb_wifi_enabled", 1) }
                .onFailure { Log.e(TAG, "Einschalten nach Tipp fehlgeschlagen", it) }
            pruefenPlanen(app, 60, 1)
        }

        private fun darfSchreiben(app: Context) =
            app.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        /**
         * Läuft im Worker-Thread (blockiert kurz). Pro WLAN-Verbindung ein Versuch ([LETZTES_NETZ]). Setzt Android den Wert
         * nach dem Einschalten wieder auf 0, wartet eine Nachfrage (neues Netz) oder das WLAN hatte noch keine IP: NACHFRAGE,
         * dann übernimmt [Pruefung] für genau dieses Netz.
         */
        fun einschalten(context: Context): Stand {
            val app = context.applicationContext
            if (!darfSchreiben(app)) {
                Log.w(TAG, "WRITE_SECURE_SETTINGS fehlt – einmal adb-wlan.ps1 ausführen")
                return Stand.FERTIG
            }
            val cm = app.getSystemService(ConnectivityManager::class.java) ?: return Stand.FERTIG
            val wlan = wlanNetz(cm) ?: return Stand.KEIN_WLAN
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val kennung = wlan.networkHandle
            if (prefs.getLong(LETZTES_NETZ, 0L) == kennung) return Stand.FERTIG
            return try {
                val cr = app.contentResolver
                if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) != 1) return Stand.FERTIG
                prefs.edit().putLong(LETZTES_NETZ, kennung).apply()
                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) return Stand.FERTIG
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Thread.sleep(3000)
                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) {
                    Log.i(TAG, "Debugging über WLAN wieder eingeschaltet")
                    Stand.FERTIG
                } else {
                    Log.w(TAG, "Android hat Debugging über WLAN nicht eingeschaltet (Nachfrage offen?) – Prüfung folgt")
                    prefs.edit().putLong(PRUEF_NETZ, kennung).apply()
                    Stand.NACHFRAGE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Einschalten fehlgeschlagen", e)
                Stand.FERTIG
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

/** Nimmt nur die eigenen Benachrichtigungs-Aktionen an (nicht exportiert: keine andere App kann WLAN-Debugging auslösen). */
class WlanDebugTipp : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = WlanDebugReceiver.tipp(context, intent.action)
}
