package de.frank.entropyreducer.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.R
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.ThemeMode
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * AppWidgetProvider (klassisch, ohne Glance — Frank-Wunsch 2026-05-11).
 *
 * Nach 11 gescheiterten Glance-Iterationen den Widget-Scrollbar zu verstecken
 * wurde das Widget komplett ohne Glance neu gebaut. Vorteile:
 *   - ListView hat android:scrollbars="none" → Scrollbar GARANTIERT weg
 *   - RemoteViewsFactory cached Items effizient → bessere App-Performance
 *     waehrend des Widget-Updates
 *   - Direkte Kontrolle ueber Layout-Properties (Themes, Farben, Spacing)
 *
 * Architektur:
 *   - Provider (diese Klasse) baut das Root-Layout (Header + ListView-Adapter)
 *   - RemoteViewsService liefert die ListView-Items (Bucket-Header + Tasks)
 *   - WidgetUpdater (separates Object) wird von App-Seite aufgerufen wenn
 *     Settings/Tasks geaendert werden
 */
class EntropyReducerWidgetReceiver : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun appSettings(): AppSettings
        fun entryDao(): EntropyEntryDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            val pending = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = EntryPointAccessors
                        .fromApplication(appContext, WidgetEntryPoint::class.java)
                        .appSettings()
                    if (shouldRefreshWidgetForConfigurationChange(
                            settings.readWidgetThemeModeOnce(),
                        )
                    ) {
                        WidgetUpdater.updateAll(appContext)
                    }
                } finally {
                    pending.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
    ) {
        val entry = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val settings = entry.appSettings()
        val dao = entry.entryDao()

        // Settings synchron lesen — onUpdate laeuft auf Main-Thread aber kurz.
        val themeMode = runBlocking { settings.readWidgetThemeModeOnce() }
        val onlyToday = runBlocking { settings.readWidgetOnlyTodayOnce() }
        val bgAlpha = runBlocking { settings.readWidgetBgAlphaOnce() }
        val palette = resolveWidgetPaletteSimple(context, themeMode)

        // Counter laden — Anzahl offener Aufgaben (gefiltert nach onlyToday)
        val openCount = runBlocking {
            val all = dao.getActive().first()
                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
            if (onlyToday) {
                all.count { priorityBucketForScore(it.manualPriorityScore ?: it.priorityScore) == TimeBucket.HEUTE }
            } else {
                all.size
            }
        }

        val views = RemoteViews(context.packageName, R.layout.widget_root)

        // Hintergrund mit Theme-Farbe + Alpha
        val bgColor = applyAlpha(palette.bgRoot, bgAlpha)
        views.setInt(R.id.widget_bg, "setBackgroundColor", bgColor)

        // Header befuellen
        applyHeader(views, palette, onlyToday, openCount)

        // PendingIntents fuer Header-Taps
        val openAppIntent = createMainActivityIntent(context, "", WidgetIntents.ACTION_OPEN, widgetId)
        val openSettingsIntent = createMainActivityIntent(context, "", WidgetIntents.ACTION_SETTINGS, widgetId * 1000 + 1)
        views.setOnClickPendingIntent(R.id.header_title_block, openAppIntent)
        views.setOnClickPendingIntent(R.id.header_today_pill, openSettingsIntent)
        views.setOnClickPendingIntent(R.id.header_settings_btn, openSettingsIntent)

        // ListView mit RemoteViewsService verbinden
        val serviceIntent = Intent(context, EntropyReducerRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // PendingIntentTemplate fuer ListView-Items — die Items selber bekommen
        // setOnClickFillInIntent in der Factory, die Templates werden hier zusammengefuegt.
        // Frank-Wunsch 2026-05-31: Template zeigt jetzt auf den WidgetActionReceiver
        // (Broadcast statt Activity), damit das Haekchen die Aufgabe abhaken kann OHNE
        // die App zu oeffnen. Der Receiver entscheidet anhand der Action: ACTION_COMPLETE
        // erledigt direkt, alle anderen Actions oeffnen wie bisher die MainActivity.
        val templateIntent = Intent(context, WidgetActionReceiver::class.java)
        val templatePending = PendingIntent.getBroadcast(
            context,
            widgetId * 1000 + 2,
            templateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        views.setPendingIntentTemplate(R.id.widget_list, templatePending)

        appWidgetManager.updateAppWidget(widgetId, views)
        // ListView-Daten frisch laden
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
    }

    private fun applyHeader(
        views: RemoteViews,
        palette: SimpleWidgetPalette,
        onlyToday: Boolean,
        openCount: Int,
    ) {
        // Titel + Counter mit echter Aufgaben-Anzahl
        views.setTextViewText(R.id.header_title, if (onlyToday) "Sehr hoch" else "Aufgaben")
        views.setTextColor(R.id.header_title, palette.textPrimary)
        views.setTextViewText(R.id.header_counter, "$openCount offen")
        views.setTextColor(R.id.header_counter, palette.textSecondary)

        // Prioritätsfilter-Pille: active = sehr hohe Priorität, inactive = alle.
        val pillBg: Int
        val pillTint: Int
        val pillLabel: String
        if (onlyToday) {
            pillBg = applyAlpha(palette.catHealth, 0.30f)
            pillTint = palette.catHealth
            pillLabel = "Sehr hoch"
        } else {
            pillBg = palette.surfaceMuted
            pillTint = palette.textSecondary
            pillLabel = "Alle"
        }
        views.setColorStateList(R.id.header_today_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(pillBg))
        views.setInt(R.id.header_today_icon, "setColorFilter", pillTint)
        views.setTextViewText(R.id.header_today_label, pillLabel)
        views.setTextColor(R.id.header_today_label, pillTint)

        // Settings-Zahnrad: surfaceMuted Hintergrund + textSecondary Tint
        views.setColorStateList(R.id.header_settings_btn, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.surfaceMuted))
    }

    private fun createMainActivityIntent(
        context: Context,
        taskId: String,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(WidgetIntents.EXTRA_TASK_ID, taskId)
            putExtra(WidgetIntents.EXTRA_ACTION, action)
            // Unique data URI damit Android nicht alle PendingIntents als gleich
            // betrachtet und cached
            data = android.net.Uri.parse("widget://action/$action/$taskId/$requestCode")
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

internal fun shouldRefreshWidgetForConfigurationChange(mode: ThemeMode): Boolean =
    mode == ThemeMode.SYSTEM

// applyAlpha lebt jetzt nur in EntropyReducerRemoteViewsService.kt als
// internal — wird hier ueber Package-Import (selbe package) genutzt.
