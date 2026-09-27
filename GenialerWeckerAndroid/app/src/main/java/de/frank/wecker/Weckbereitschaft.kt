package de.frank.wecker

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Weckbereitschaft: jede Freigabe hat genau einen Weg dorthin. Zuerst der direkte Systemdialog bzw. die
 * genaue Einstellungsseite dieser App, danach Ersatzseiten, die es auf jedem Android-Handy gibt
 * (Hersteller blenden einzelne Seiten aus) – zuletzt die App-Info, von der aus alles erreichbar ist.
 */
object Weckbereitschaft {

    private const val PREFS = "wecker_bereitschaft"
    private const val VERSUCHT = "automatisch_versucht"

    /** Öffnet die passende Stelle für [name]. [benachrichtigungenAnfragen] zeigt den Laufzeit-Dialog. */
    fun beheben(activity: Activity, name: String, benachrichtigungenAnfragen: () -> Unit): Boolean {
        val pkg = "package:${activity.packageName}"
        fun i(action: String, packageUri: Boolean = false, extraPackage: Boolean = false) = Intent(action).apply {
            if (packageUri) data = Uri.parse(pkg)
            if (extraPackage) putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }
        val appInfo = i(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri = true)
        val kette: List<Intent> = when (name) {
            "Genaue Weckzeiten" -> buildList {
                if (Build.VERSION.SDK_INT >= 31) add(i(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri = true))
                add(appInfo)
            }
            "Benachrichtigungen" -> {
                if (Build.VERSION.SDK_INT >= 33 && !laufzeitErteilt(activity)) {
                    benachrichtigungenAnfragen(); return true
                }
                listOf(i(Settings.ACTION_APP_NOTIFICATION_SETTINGS, extraPackage = true), appInfo)
            }
            "Vollbild-Wecker" -> buildList {
                if (Build.VERSION.SDK_INT >= 34) add(i(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri = true))
                add(i(Settings.ACTION_APP_NOTIFICATION_SETTINGS, extraPackage = true))
                add(appInfo)
            }
            "Wecker bei Nicht stören" -> {
                val nm = activity.getSystemService(NotificationManager::class.java)
                if (!nm.isNotificationPolicyAccessGranted) listOf(i(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), i("android.settings.ZEN_MODE_SETTINGS"), appInfo)
                else {
                    if (weckerImNichtStoerenErlauben(activity) && erfuellt(activity, name)) return true
                    listOf(i("android.settings.ZEN_MODE_SETTINGS"), i(Settings.ACTION_SOUND_SETTINGS), appInfo)
                }
            }
            "Akku uneingeschränkt" -> {
                val pm = activity.getSystemService(PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(activity.packageName)) listOf(appInfo)
                else listOf(i(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri = true),
                    i(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appInfo)
            }
            else -> listOf(appInfo)
        }
        kette.forEach { intent -> if (runCatching { activity.startActivity(intent) }.isSuccess) return true }
        return runCatching { activity.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isSuccess
    }

    private fun laufzeitErteilt(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun erfuellt(context: Context, name: String) = readiness(context).firstOrNull { it.first == name }?.second ?: true

    /**
     * Mit Nicht-stören-Zugriff darf die App die Wecker-Ausnahme selbst setzen – dann muss niemand suchen.
     * Nur die Kategorie „Wecker“ kommt hinzu; alles andere am Modus bleibt, wie es ist.
     */
    fun weckerImNichtStoerenErlauben(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 28) return true
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return false
        val p = nm.notificationPolicy
        if ((p.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0) return true
        val kategorien = p.priorityCategories or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
        nm.notificationPolicy = if (Build.VERSION.SDK_INT >= 30)
            NotificationManager.Policy(kategorien, p.priorityCallSenders, p.priorityMessageSenders,
                p.suppressedVisualEffects, p.priorityConversationSenders)
        else NotificationManager.Policy(kategorien, p.priorityCallSenders, p.priorityMessageSenders, p.suppressedVisualEffects)
        true
    }.getOrDefault(false)

    /** Nächste fehlende Freigabe, die nach der Installation noch nicht automatisch angesteuert wurde. */
    fun naechsteAutomatisch(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versucht = prefs.getStringSet(VERSUCHT, emptySet()).orEmpty()
        return readiness(context).firstOrNull { !it.second && it.first !in versucht }?.first
    }

    fun alsVersuchtMerken(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versucht = prefs.getStringSet(VERSUCHT, emptySet()).orEmpty()
        prefs.edit().putStringSet(VERSUCHT, versucht + name).commit()
    }
}

/**
 * Nach der Installation einmal alle fehlenden Freigaben der Reihe nach ansteuern: bei jeder Rückkehr in
 * die App die nächste. Jede Freigabe wird höchstens einmal automatisch geöffnet – wer ablehnt, wird nicht
 * bedrängt; der Weg bleibt in den Einstellungen unter „Weckbereitschaft“.
 */
@Composable
fun AutomatischeWeckbereitschaft(activity: ComponentActivity) {
    val benachrichtigungen = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val anfragen = rememberUpdatedState { benachrichtigungen.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            // Still und ohne Rückfrage: fehlt nur die Wecker-Ausnahme, setzt die App sie selbst.
            Weckbereitschaft.weckerImNichtStoerenErlauben(activity)
            if (AlarmService.state.value.alarm != null) return@LifecycleEventObserver
            val name = Weckbereitschaft.naechsteAutomatisch(activity) ?: return@LifecycleEventObserver
            Weckbereitschaft.alsVersuchtMerken(activity, name)
            Weckbereitschaft.beheben(activity, name) { anfragen.value() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
}
