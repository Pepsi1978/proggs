package de.frank.entropyreducer.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.ColorUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.R
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.presentation.priorityRampArgb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService fuer die scrollable Aufgaben-ListView (Frank-Wunsch 2026-05-11).
 *
 * Wird vom AppWidgetProvider via setRemoteAdapter angebunden. Liefert pro
 * Widget-Update eine Factory die die Items aufbaut (Bucket-Header + Tasks).
 * Die Factory cached die Daten — Items werden nur bei onDataSetChanged neu
 * geladen, nicht bei jedem Scroll. Das ist deutlich performanter als
 * Glance's ständiges Recompose.
 */
class EntropyReducerRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EntropyReducerRemoteViewsFactory(applicationContext)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetDataEntryPoint {
        fun entryDao(): EntropyEntryDao
        fun appSettings(): AppSettings
    }
}

/**
 * Factory die die Items baut. Pro Bucket: 1 Header + N Aufgaben.
 *
 * Performance:
 *  - onDataSetChanged laeuft auf einem Background-Thread (von Android verwaltet)
 *  - Daten werden EINMAL geladen, danach via getViewAt aus dem Cache geliefert
 *  - getViewAt ist sehr schnell (nur RemoteViews aufbauen, keine Queries)
 */
class EntropyReducerRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetListItem> = emptyList()
    private var palette: SimpleWidgetPalette = SimpleWidgetDarkPalette

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            EntropyReducerRemoteViewsService.WidgetDataEntryPoint::class.java,
        )
        val dao = entry.entryDao()
        val settings = entry.appSettings()

        runBlocking {
            val themeMode = settings.readWidgetThemeModeOnce()
            palette = resolveWidgetPaletteSimple(context, themeMode)
            val onlyToday = settings.readWidgetOnlyTodayOnce()

            val all = dao.getActive().first()
                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }

            val grouped: Map<TimeBucket, List<EntropyEntryEntity>> = if (onlyToday) {
                mapOf(
                    TimeBucket.HEUTE to all
                        .filter { it.timeBucket == TimeBucket.HEUTE }
                        .sortedByDescending { it.manualPriorityScore ?: it.priorityScore },
                )
            } else {
                ALL_BUCKETS.associateWith { bucket ->
                    all.filter { it.timeBucket == bucket }
                        .sortedByDescending { it.manualPriorityScore ?: it.priorityScore }
                }
            }

            val list = mutableListOf<WidgetListItem>()
            ALL_BUCKETS.forEach { bucket ->
                val tasks = grouped[bucket].orEmpty()
                if (tasks.isNotEmpty()) {
                    list.add(WidgetListItem.BucketHeader(bucket, tasks.size))
                    tasks.forEach { list.add(WidgetListItem.Task(it)) }
                }
            }
            items = list
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return RemoteViews(context.packageName, R.layout.widget_item_bucket_header)
        }
        return when (val item = items[position]) {
            is WidgetListItem.BucketHeader -> buildBucketHeader(item)
            is WidgetListItem.Task -> buildTaskCard(item.entry)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) return position.toLong()
        return when (val it = items[position]) {
            is WidgetListItem.BucketHeader -> it.bucket.ordinal.toLong()
            is WidgetListItem.Task -> it.entry.id.hashCode().toLong()
        }
    }

    override fun hasStableIds(): Boolean = true

    // === Bucket-Header ===

    private fun buildBucketHeader(item: WidgetListItem.BucketHeader): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item_bucket_header)
        val accent = bucketColor(palette, item.bucket)
        // Hintergrund der Pille mit 22% Alpha vom Akzent
        views.setColorStateList(R.id.bucket_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(accent, 0.22f)))
        views.setImageViewResource(R.id.bucket_icon, bucketIconRes(item.bucket))
        views.setInt(R.id.bucket_icon, "setColorFilter", accent)
        views.setTextViewText(R.id.bucket_label, bucketLabel(item.bucket))
        views.setTextColor(R.id.bucket_label, accent)
        views.setTextViewText(R.id.bucket_count, item.count.toString())
        views.setTextColor(R.id.bucket_count, palette.textSecondary)
        return views
    }

    // === Aufgaben-Karte ===

    private fun buildTaskCard(entry: EntropyEntryEntity): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item_task)

        // Effektive Prioritaet wie in der App: manueller Wert hat Vorrang vor KI.
        val effectivePriority = entry.manualPriorityScore ?: entry.priorityScore

        // Karten-Hintergrund: horizontaler Verlauf in der Prioritaetsfarbe. Das
        // Basis-Drawable (weiss 20%→100%) wird mit der EXAKT gleichen Rampe wie in
        // der App getoent (priorityRampArgb ruft dieselbe Funktion auf). SRC_IN-Tint
        // erhaelt den Alpha-Verlauf → links dezent, rechts volle Farbe.
        val rampColor = priorityRampArgb(effectivePriority)
        views.setColorStateList(
            R.id.task_card_root,
            "setBackgroundTintList",
            android.content.res.ColorStateList.valueOf(rampColor),
        )

        // Haekchen: leeres Quadrat mit Border (Widget zeigt nur offene Aufgaben).
        views.setColorStateList(
            R.id.task_check_box,
            "setBackgroundTintList",
            android.content.res.ColorStateList.valueOf(palette.border),
        )

        // Titel (eine Zeile)
        views.setTextViewText(R.id.task_title, entry.title)
        views.setTextColor(R.id.task_title, palette.textPrimary)

        // Prioritaet-Perle: "Priorität KI" wenn die KI bestimmt, sonst "Priorität <Wert>".
        val prioLabel = entry.manualPriorityScore
            ?.let { "Priorität ${it.toInt()}" }
            ?: "Priorität KI"
        views.setTextViewText(R.id.task_prio_pill, prioLabel)
        views.setColorStateList(R.id.task_prio_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.surfaceMuted))
        views.setTextColor(R.id.task_prio_pill, palette.textSecondary)

        // KI/Manuell-Perle (Bucket): manuell = Bucket-Akzentfarbe, KI = gedaempft.
        val isManual = entry.manualBucket != null
        val bucketAccent = bucketColor(palette, entry.timeBucket)
        if (isManual) {
            views.setColorStateList(R.id.bucket_status_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(bucketAccent, 0.22f)))
            views.setInt(R.id.bucket_status_icon, "setColorFilter", bucketAccent)
            views.setTextViewText(R.id.bucket_status_label, "manuell")
            views.setTextColor(R.id.bucket_status_label, bucketAccent)
        } else {
            views.setColorStateList(R.id.bucket_status_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.surfaceMuted))
            views.setInt(R.id.bucket_status_icon, "setColorFilter", palette.textSecondary)
            views.setTextViewText(R.id.bucket_status_label, "KI")
            views.setTextColor(R.id.bucket_status_label, palette.textSecondary)
        }
        views.setImageViewResource(R.id.bucket_status_icon, bucketIconRes(entry.timeBucket))

        // FillInIntents (kombiniert mit dem Broadcast-Template aus dem Provider):
        //  - Haekchen   → ACTION_COMPLETE: hakt die Aufgabe DIREKT ab, ohne App
        //  - Karte      → ACTION_FOCUS: oeffnet die App (Detail)
        //  - Prio-Perle → ACTION_FOCUS: oeffnet die App (dort der Prio-Schieber)
        //  - Bucket     → ACTION_RESCHEDULE: oeffnet die App (dort der Bucket-Picker)
        views.setOnClickFillInIntent(R.id.task_check_box, fillIn(entry.id, WidgetIntents.ACTION_COMPLETE))
        views.setOnClickFillInIntent(R.id.task_card_root, fillIn(entry.id, WidgetIntents.ACTION_FOCUS))
        views.setOnClickFillInIntent(R.id.task_prio_pill, fillIn(entry.id, WidgetIntents.ACTION_FOCUS))
        views.setOnClickFillInIntent(R.id.bucket_status_pill, fillIn(entry.id, WidgetIntents.ACTION_RESCHEDULE))

        return views
    }

    /** Baut ein FillInIntent fuer ein ListView-Item (Action + Task-ID). */
    private fun fillIn(taskId: String, action: String): Intent = Intent().apply {
        putExtra(WidgetIntents.EXTRA_TASK_ID, taskId)
        putExtra(WidgetIntents.EXTRA_ACTION, action)
        // Eindeutige Daten-URI, damit Android die FillInIntents nicht als gleich cached.
        data = android.net.Uri.parse("widget://task/$taskId/$action")
    }
}

// === Sealed class fuer ListView-Items ===

sealed class WidgetListItem {
    data class BucketHeader(val bucket: TimeBucket, val count: Int) : WidgetListItem()
    data class Task(val entry: EntropyEntryEntity) : WidgetListItem()
}

// === Helpers ===

internal val ALL_BUCKETS = listOf(
    TimeBucket.HEUTE,
    TimeBucket.MORGEN,
    TimeBucket.FREIBLOCK,
    TimeBucket.SPAETER,
)

internal fun applyAlpha(color: Int, alpha: Float): Int =
    ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * 255).toInt())

internal fun bucketColor(p: SimpleWidgetPalette, bucket: TimeBucket): Int = when (bucket) {
    TimeBucket.HEUTE -> p.bucketHeute
    TimeBucket.MORGEN -> p.bucketMorgen
    TimeBucket.FREIBLOCK -> p.bucketFreiblock
    TimeBucket.SPAETER -> p.bucketSpaeter
}

internal fun bucketLabel(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "Heute"
    TimeBucket.MORGEN -> "Morgen"
    TimeBucket.FREIBLOCK -> "Freiblock"
    TimeBucket.SPAETER -> "Später"
}

internal fun bucketIconRes(bucket: TimeBucket): Int = when (bucket) {
    TimeBucket.HEUTE -> R.drawable.ic_widget_bucket_today
    TimeBucket.MORGEN -> R.drawable.ic_widget_bucket_morgen
    TimeBucket.FREIBLOCK -> R.drawable.ic_widget_bucket_freiblock
    TimeBucket.SPAETER -> R.drawable.ic_widget_bucket_spaeter
}

/**
 * Frank-Wunsch 2026-05-22 (fuenfte Iteration): Widget-Karten-Hintergrund folgt
 * jetzt der priorityScore-Farbe (gleiche Skala wie in der App). So sieht Frank
 * die Wichtigkeit einer Aufgabe direkt am Widget, ohne sie zu oeffnen.
 *
 * Skala 1:1 zur App:
 *  - 80-100  Rot
 *  - 60-80   Orange
 *  - 40-60   Gelb
 *  - 20-40   Gruen
 *  -  0-20   Blau
 */
internal fun cardBackgroundRes(p: SimpleWidgetPalette, score: Double): Int =
    if (p.isDark) when {
        score >= 80.0 -> R.drawable.bg_card_prio_red_dark
        score >= 60.0 -> R.drawable.bg_card_prio_orange_dark
        score >= 40.0 -> R.drawable.bg_card_prio_yellow_dark
        score >= 20.0 -> R.drawable.bg_card_prio_green_dark
        else -> R.drawable.bg_card_prio_blue_dark
    } else when {
        score >= 80.0 -> R.drawable.bg_card_prio_red_light
        score >= 60.0 -> R.drawable.bg_card_prio_orange_light
        score >= 40.0 -> R.drawable.bg_card_prio_yellow_light
        score >= 20.0 -> R.drawable.bg_card_prio_green_light
        else -> R.drawable.bg_card_prio_blue_light
    }

internal fun categoryColor(p: SimpleWidgetPalette, cat: EntropyCategory): Int = when (cat) {
    EntropyCategory.KOERPERLICH -> p.catPhysical
    EntropyCategory.MENTAL -> p.catMental
    EntropyCategory.ZEITLICH -> p.catTemporal
    EntropyCategory.EMOTIONAL -> p.catEmotional
    EntropyCategory.GESUNDHEITLICH -> p.catHealth
    EntropyCategory.UMGEBUNG -> p.catEnvironment
    EntropyCategory.SONSTIGES -> p.catOther
}

internal fun categoryLabel(cat: EntropyCategory): String = when (cat) {
    EntropyCategory.KOERPERLICH -> "Körperlich"
    EntropyCategory.MENTAL -> "Mental"
    EntropyCategory.ZEITLICH -> "Zeitlich"
    EntropyCategory.EMOTIONAL -> "Emotional"
    EntropyCategory.GESUNDHEITLICH -> "Gesundheitlich"
    EntropyCategory.UMGEBUNG -> "Umgebung"
    EntropyCategory.SONSTIGES -> "Sonstiges"
}

internal fun categoryIconRes(cat: EntropyCategory): Int = when (cat) {
    EntropyCategory.KOERPERLICH -> R.drawable.ic_widget_cat_physical
    EntropyCategory.MENTAL -> R.drawable.ic_widget_cat_mental
    EntropyCategory.ZEITLICH -> R.drawable.ic_widget_cat_temporal
    EntropyCategory.EMOTIONAL -> R.drawable.ic_widget_cat_emotional
    EntropyCategory.GESUNDHEITLICH -> R.drawable.ic_widget_cat_health
    EntropyCategory.UMGEBUNG -> R.drawable.ic_widget_cat_environment
    EntropyCategory.SONSTIGES -> R.drawable.ic_widget_cat_other
}

internal fun priorityColor(p: SimpleWidgetPalette, score: Double): Int = when {
    score >= 80.0 -> p.prioRed
    score >= 60.0 -> p.prioOrange
    score >= 40.0 -> p.prioYellow
    score >= 20.0 -> p.prioGreen
    else -> p.prioBlue
}
