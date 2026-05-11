package de.frank.entropyreducer.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Widget Deep-Link Infrastruktur (Frank-Wunsch 2026-05-11).
 *
 * Das scrollbare Aufgaben-Widget hat zwei Tap-Zonen pro Karte:
 *  - Karte allgemein → oeffnet App auf dem Aufgaben-Tab mit Fokus auf der Task
 *  - KI/Manuell-Pille → oeffnet App und triggert direkt das Bucket-Picker-Sheet
 *
 * Glance verpackt die ActionParameters automatisch als Intent-Extras (der Key-Name
 * landet als String-Schluessel im Bundle), MainActivity liest sie in onCreate +
 * onNewIntent und schickt sie in den WidgetDeepLinkBus. TasksScreen beobachtet
 * den Bus und reagiert per LaunchedEffect.
 */
object WidgetIntents {

    /** ID der angetippten EntropyEntryEntity (String-UUID). */
    val KEY_TASK_ID: ActionParameters.Key<String> = ActionParameters.Key("widget_task_id")

    /** Aktion: "focus" (Karte angetippt) oder "reschedule" (KI/Manuell-Pille angetippt). */
    val KEY_ACTION: ActionParameters.Key<String> = ActionParameters.Key("widget_action")

    const val ACTION_FOCUS = "focus"
    const val ACTION_RESCHEDULE = "reschedule"
    const val ACTION_SETTINGS = "settings"
    const val ACTION_OPEN = "open"

    /** Bundle-Schluessel im Intent — identisch zum ActionParameters.Key-Namen. */
    const val EXTRA_TASK_ID = "widget_task_id"
    const val EXTRA_ACTION = "widget_action"

    /**
     * Header-Tap (App oeffnen) oder Settings-Icon-Tap. Hat keine Task-ID,
     * nur die Action. taskId wird hier auf einen leeren Marker gesetzt damit
     * das Schema gleich bleibt.
     */
    fun openHeaderAction(action: String) = actionStartActivity<MainActivity>(
        actionParametersOf(
            KEY_TASK_ID to "",
            KEY_ACTION to action,
        ),
    )

    /**
     * Baut eine Glance-Action die MainActivity startet und Task-ID + Action als
     * Extras mitschickt. Wird in jeder Aufgabenkarte zweimal verwendet (einmal
     * fuer die ganze Karte, einmal fuer die KI/Manuell-Pille).
     *
     * Singletop-Flag in MainActivity sorgt dafuer dass kein zweiter Activity-
     * Stack entsteht — onNewIntent uebernimmt.
     */
    fun openTaskAction(taskId: String, action: String) = actionStartActivity<MainActivity>(
        actionParametersOf(
            KEY_TASK_ID to taskId,
            KEY_ACTION to action,
        ),
    )

    /**
     * Fallback fuer Klassik-Intent-Erzeugung (z.B. fuer Tests oder manuelle
     * Trigger). Wird aktuell nicht direkt vom Widget verwendet — Glance baut
     * den Intent selbst aus den ActionParameters.
     */
    fun buildExplicitIntent(context: Context, taskId: String, action: String): Intent =
        Intent().apply {
            component = ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ACTION, action)
        }
}

/**
 * Datentraeger fuer einen Widget-Tap, der in der App verarbeitet werden soll.
 * Wird vom WidgetDeepLinkBus als StateFlow durchgeschoben.
 */
data class WidgetDeepLink(
    val taskId: String,
    val action: String,
)

/**
 * Process-scoped Bus fuer Widget-Deep-Links.
 *
 * Warum object statt Hilt-Singleton: Widget-Klicks landen IMMER im selben
 * Process wie die UI (singleTop-Activity), Lifecycle ist kurzlebig (App ist
 * gerade gestartet oder schon offen). Ein simples Kotlin-Singleton reicht
 * voellig und vermeidet zusaetzliche Hilt-Module.
 *
 * Nach Verarbeitung MUSS der Consumer das Event mit clear() konsumieren,
 * damit ein Configuration-Change (z.B. Theme-Wechsel) den Tap nicht zweimal
 * triggert.
 */
object WidgetDeepLinkBus {
    private val _events = MutableStateFlow<WidgetDeepLink?>(null)
    val events: kotlinx.coroutines.flow.StateFlow<WidgetDeepLink?> = _events

    fun emit(link: WidgetDeepLink) {
        _events.value = link
    }

    fun clear() {
        _events.value = null
    }
}

/**
 * Offizieller Glance-Toggle-Pattern (Bugfix 2026-05-11, fuenfte Iteration).
 *
 * Recherche-Ergebnis: Die richtige Loesung fuer Widget-Toggle-Buttons ist
 * `actionRunCallback<ActionCallback>` (laeuft in WorkManager Worker mit voller
 * Ausfuehrungszeit) plus `update(context, glanceId)` fuer gezielten Re-Render
 * der spezifischen Widget-Instanz. Quelle: Android Developers Docs
 * (developer.android.com/develop/ui/compose/glance/user-interaction).
 *
 * Frueher: actionStartActivity<WidgetToggleActivity> — Activity wurde zwar
 * gestartet (Logcat zeigte ActivityTaskManager:START), aber onCreate-Logs
 * kamen nicht durch, Hilt-Injection unzuverlaessig. ActionCallback umgeht
 * den Activity-Lifecycle komplett — laeuft direkt im Widget-Worker.
 *
 * Logging an mehreren Stellen damit wir bei zukuenftigen Bugs sofort sehen
 * wo es klemmt (Worker startet nicht / Hilt fehlt / DataStore-Write haengt /
 * update() schlaegt fehl).
 */
class WidgetToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        android.util.Log.i("WidgetToggle", "ActionCallback.onAction ENTRY")
        try {
            val settings = EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    EntropyReducerWidget.WidgetEntryPoint::class.java,
                )
                .appSettings()
            android.util.Log.i("WidgetToggle", "Hilt-Settings entryPoint geladen")
            val newValue = settings.toggleWidgetOnlyToday()
            android.util.Log.i("WidgetToggle", "Atomic toggle → $newValue")

            // KRITISCH (Bugfix 2026-05-11, siebte Iteration — Nova Launcher fix):
            // Frank nutzt Nova Launcher der Widgets aggressiv cached. Glance's
            // updateAll() queued einen Worker der manchmal erst 30 Sekunden
            // spaeter laeuft → User sieht keine Reaktion. Loesung: DIREKTER
            // AppWidget-Update-Broadcast bypassed Glance's Worker-Queue komplett
            // und triggert sofort den EntropyReducerWidgetReceiver, der dann
            // synchron provideGlance aufruft. Dieser Broadcast funktioniert exakt
            // wie `adb shell am broadcast -a APPWIDGET_UPDATE` (was nachweislich
            // funktioniert).
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    set(WIDGET_TICK_KEY, System.currentTimeMillis())
                }
            }
            android.util.Log.i("WidgetToggle", "Glance-State Tick geschrieben")

            // Direkter AppWidget-Update-Broadcast (umgeht Nova-Cache + Glance-Queue)
            val mgr = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EntropyReducerWidgetReceiver::class.java)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    this.component = component
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
                android.util.Log.i("WidgetToggle", "Direct broadcast sent for ${ids.size} widget(s)")
            }

            // Zusaetzlich Glance's updateAll als Fallback fuer normale Launcher
            EntropyReducerWidget().updateAll(context)
            android.util.Log.i("WidgetToggle", "updateAll(context) fired")
        } catch (t: Throwable) {
            android.util.Log.e("WidgetToggle", "ActionCallback FAILED", t)
        }
    }

    companion object {
        /** Tick-Key in Glance's eigenem State — forciert Recompose bei Toggle. */
        val WIDGET_TICK_KEY = longPreferencesKey("widget_tick")
    }
}
