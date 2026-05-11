package de.frank.entropyreducer.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Unsichtbare Activity (Hybrid-Pattern, Bugfix 2026-05-11 neunte Iteration).
 *
 * Hintergrund: ActionCallback (Broadcast-basiert) wird vom Samsung-OS
 * mit Nova Launcher zusammen eingefroren — der Klick erreicht die Callback
 * nicht zuverlaessig. Eine Activity dagegen startet IMMER, weil das System
 * sie startet (unabhaengig vom App-Process-State).
 *
 * Diese Activity ist Theme.NoDisplay + noHistory=true — sichtbar fuer den
 * User passiert nichts, sie laeuft 50ms im Hintergrund und togglet Glance's
 * eigenen State (PreferencesGlanceStateDefinition).
 *
 * KEIN Hilt mehr (vorher fehleranfaellig). KEIN AppSettings mehr (extern
 * = nicht reaktiv). Nur Glance-eigener State, direkt geschrieben.
 */
class WidgetToggleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("WidgetToggle", "Activity.onCreate ENTRY")
        val appCtx = applicationContext
        // Application-Scope: ueberleben das Activity-finish() ohne Cancel
        CoroutineScope(Dispatchers.IO).launch {
            try {
                doToggleAndRefresh(appCtx)
            } catch (t: Throwable) {
                android.util.Log.e("WidgetToggle", "Activity-toggle FAILED", t)
            }
        }
        finish()
    }

    private suspend fun doToggleAndRefresh(context: Context) {
        val mgr = GlanceAppWidgetManager(context)
        val ids = mgr.getGlanceIds(EntropyReducerWidget::class.java)
        android.util.Log.i("WidgetToggle", "Activity found ${ids.size} widget instances")
        if (ids.isEmpty()) return

        // 1. Glance-State togglen (reaktiver Store)
        ids.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    val current = this[EntropyReducerWidget.KEY_ONLY_TODAY] ?: false
                    val newValue = !current
                    set(EntropyReducerWidget.KEY_ONLY_TODAY, newValue)
                    android.util.Log.i("WidgetToggle", "Activity-toggle: $current → $newValue")
                }
            }
        }

        // 2. KRITISCH (Bugfix 2026-05-11, zehnte Iteration):
        // Direkter AppWidget-Update-Broadcast statt Glance.updateAll().
        // Logcat-Beweis: updateAll() wird zwar aufgerufen aber Glance triggert
        // provideGlance NICHT neu (Worker-Queue-Dedup-Problem). Der DIREKTE
        // Broadcast triggert dagegen sofort den GlanceAppWidgetReceiver, der
        // synchron einen neuen Worker startet — exakt wie
        // `adb shell am broadcast -a APPWIDGET_UPDATE` (was nachweislich
        // funktioniert).
        val appWidgetMgr = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, EntropyReducerWidgetReceiver::class.java)
        val intIds = appWidgetMgr.getAppWidgetIds(component)
        if (intIds.isNotEmpty()) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                this.component = component
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intIds)
            }
            context.sendBroadcast(intent)
            android.util.Log.i("WidgetToggle", "Direct broadcast sent for ${intIds.size} widget(s)")
        } else {
            android.util.Log.w("WidgetToggle", "Keine AppWidget-IDs gefunden fuer Direct-Broadcast")
        }
        android.util.Log.i("WidgetToggle", "Activity-toggle completed")
    }
}
