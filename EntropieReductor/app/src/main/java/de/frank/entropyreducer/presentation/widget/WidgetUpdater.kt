package de.frank.entropyreducer.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.frank.entropyreducer.R

/**
 * Wrapper damit App-Seite (TasksScreen, WidgetSettingsScreen) einheitlich das
 * Widget refreshen kann — egal ob Glance oder klassisch.
 *
 * Bei klassischem AppWidgetProvider (Frank-Wunsch 2026-05-11) machen wir:
 *  1. Provider-Broadcast → onUpdate wird ausgeloest, Root-Layout neu gerendert
 *  2. notifyAppWidgetViewDataChanged → ListView laed sich frisch (Factory.onDataSetChanged)
 */
object WidgetUpdater {

    /**
     * Refresht alle installierten Widget-Instanzen. Suspend, damit Aufrufer
     * den Call mit dem AppSettings-Write sequenzieren koennen (sonst race).
     */
    suspend fun updateAll(context: Context) {
        val appCtx = context.applicationContext
        val mgr = AppWidgetManager.getInstance(appCtx)
        val component = ComponentName(appCtx, EntropyReducerWidgetReceiver::class.java)
        val ids = mgr.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        // 1. Force-Update via Broadcast — triggert AppWidgetProvider.onUpdate
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        appCtx.sendBroadcast(intent)

        // 2. ListView-Daten frisch ziehen (RemoteViewsFactory.onDataSetChanged)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }
}
