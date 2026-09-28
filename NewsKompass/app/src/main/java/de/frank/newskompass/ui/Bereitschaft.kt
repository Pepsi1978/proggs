package de.frank.newskompass.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Bereitschaft wie beim Genialen Wecker: Kommt die Ausgabe um 17 Uhr auch im Hintergrund?
 * Jede Freigabe hat genau einen Weg dorthin — zuerst der direkte Systemdialog bzw. die genaue
 * Einstellungsseite dieser App, danach Ersatzseiten, zuletzt die App-Info.
 */
object Bereitschaft {

    const val BENACHRICHTIGUNGEN = "Benachrichtigungen"
    const val WECKER = "Pünktliche Ausgaben"
    const val AKKU = "Akku uneingeschränkt"

    private const val PREFS = "bereitschaft"
    private const val VERSUCHT = "automatisch_versucht"

    /** Jede Freigabe mit ihrem Zustand, in der Reihenfolge, in der sie angesteuert wird. */
    fun pruefe(context: Context): List<Pair<String, Boolean>> = listOf(
        BENACHRICHTIGUNGEN to NotificationManagerCompat.from(context).areNotificationsEnabled(),
        WECKER to (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true),
        AKKU to (context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true),
    )

    /** Öffnet die passende Stelle für [name]. [benachrichtigungenAnfragen] zeigt den Laufzeit-Dialog. */
    fun behebe(activity: Activity, name: String, benachrichtigungenAnfragen: () -> Unit): Boolean {
        val pkg = "package:${activity.packageName}"
        fun i(action: String, packageUri: Boolean = false, extraPackage: Boolean = false) = Intent(action).apply {
            if (packageUri) data = Uri.parse(pkg)
            if (extraPackage) putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }
        val appInfo = i(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri = true)
        val kette: List<Intent> = when (name) {
            BENACHRICHTIGUNGEN -> {
                if (Build.VERSION.SDK_INT >= 33 && !laufzeitErteilt(activity) && !dialogGesperrt(activity)) {
                    benachrichtigungenAnfragen()
                    return true
                }
                listOf(i(Settings.ACTION_APP_NOTIFICATION_SETTINGS, extraPackage = true), appInfo)
            }
            WECKER -> buildList {
                if (Build.VERSION.SDK_INT >= 31) add(i(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri = true))
                add(appInfo)
            }
            AKKU -> listOf(
                i(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri = true),
                i(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                appInfo,
            )
            else -> listOf(appInfo)
        }
        kette.forEach { absicht -> if (runCatching { activity.startActivity(absicht) }.isSuccess) return true }
        return runCatching { activity.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isSuccess
    }

    /**
     * Ein Satz, was nach dem Tipp auf „Erlauben“ zu tun ist. Wo Android ein Fenster zeigt, reicht
     * „Zulassen“; wo es nur eine Einstellungsseite gibt, steht hier genau der Schalter, den man sucht.
     */
    fun anleitung(activity: Activity, name: String): String = when (name) {
        BENACHRICHTIGUNGEN ->
            if (Build.VERSION.SDK_INT >= 33 && !laufzeitErteilt(activity) && !dialogGesperrt(activity)) "Im Fenster auf „Zulassen“ tippen."
            else "Auf der nächsten Seite „Benachrichtigungen zulassen“ einschalten, dann zurück."
        WECKER -> "Auf der nächsten Seite den Schalter bei „Wecker und Erinnerungen“ einschalten, dann zurück."
        AKKU -> "Im Fenster auf „Zulassen“ tippen."
        else -> "Auf der nächsten Seite die Freigabe einschalten, dann zurück."
    }

    /**
     * Ergebnis des Benachrichtigungs-Fensters. Kam „abgelehnt“, ohne dass Android noch fragen darf, geht es
     * sofort zur Einstellungsseite — sonst passierte beim Tipp auf „Erlauben“ sichtbar nichts.
     */
    fun benachrichtigungsAntwort(activity: Activity, erteilt: Boolean) {
        if (erteilt || Build.VERSION.SDK_INT < 33) return
        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(BENACHRICHTIGUNG_GESPERRT, true).apply()
        android.widget.Toast.makeText(activity, anleitung(activity, BENACHRICHTIGUNGEN), android.widget.Toast.LENGTH_LONG).show()
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
        }
    }

    /** Zweimal abgelehnt: Android zeigt das Fenster nicht mehr, nur noch die Einstellungsseite hilft. */
    private fun dialogGesperrt(activity: Activity): Boolean =
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(BENACHRICHTIGUNG_GESPERRT, false) &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    private fun laufzeitErteilt(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private const val BENACHRICHTIGUNG_GESPERRT = "benachrichtigung_gesperrt"

    fun oeffneAppInfo(activity: Activity) {
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))
        }
    }

    /** Nächste fehlende Freigabe, die nach der Installation noch nicht automatisch angesteuert wurde. */
    fun naechsteAutomatisch(context: Context): String? {
        val versucht = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(VERSUCHT, emptySet()).orEmpty()
        return pruefe(context).firstOrNull { !it.second && it.first !in versucht }?.first
    }

    fun alsVersuchtMerken(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versucht = prefs.getStringSet(VERSUCHT, emptySet()).orEmpty()
        prefs.edit().putStringSet(VERSUCHT, versucht + name).apply()
    }
}

/** Liest die Bereitschaft bei jeder Rückkehr in die App neu — nach dem Systemdialog stimmt sie sofort. */
@Composable
fun rememberBereitschaft(context: Context): List<Pair<String, Boolean>> {
    var stand by remember { mutableIntStateOf(0) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis -> if (ereignis == Lifecycle.Event.ON_RESUME) stand++ }
        lebenszyklus.addObserver(beobachter)
        onDispose { lebenszyklus.removeObserver(beobachter) }
    }
    return remember(stand) { Bereitschaft.pruefe(context) }
}

/**
 * Nach der Installation einmal alle fehlenden Freigaben der Reihe nach ansteuern: bei jeder
 * Rückkehr in die App die nächste. Jede höchstens einmal — wer ablehnt, wird nicht bedrängt;
 * der Weg bleibt in den Einstellungen unter „Bereitschaft“.
 */
@Composable
fun AutomatischeBereitschaft(activity: ComponentActivity) {
    val benachrichtigungen = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val anfragen by rememberUpdatedState { benachrichtigungen.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val name = Bereitschaft.naechsteAutomatisch(activity) ?: return@LifecycleEventObserver
            Bereitschaft.alsVersuchtMerken(activity, name)
            // Vor dem Sprung ein Satz, was dort anzutippen ist — er bleibt über der nächsten Seite stehen.
            android.widget.Toast.makeText(activity, Bereitschaft.anleitung(activity, name), android.widget.Toast.LENGTH_LONG).show()
            Bereitschaft.behebe(activity, name) { anfragen() }
        }
        lebenszyklus.addObserver(beobachter)
        onDispose { lebenszyklus.removeObserver(beobachter) }
    }
}
