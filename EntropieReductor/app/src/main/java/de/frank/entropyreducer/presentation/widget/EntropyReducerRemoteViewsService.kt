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
                        .sortedByDescending { it.priorityScore },
                )
            } else {
                ALL_BUCKETS.associateWith { bucket ->
                    all.filter { it.timeBucket == bucket }
                        .sortedByDescending { it.priorityScore }
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
        val catColor = categoryColor(palette, entry.category)
        val prioColor = priorityColor(palette, entry.priorityScore)
        val isManual = entry.manualBucket != null
        val bucketAccent = bucketColor(palette, entry.timeBucket)

        // Card-Hintergrund: Gradient-Drawable basierend auf Bucket + Theme
        views.setInt(
            R.id.task_card_root,
            "setBackgroundResource",
            cardBackgroundRes(palette, entry.timeBucket),
        )

        // Icon-Kreis: 18% Alpha vom catColor
        views.setColorStateList(R.id.cat_icon_bg, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(catColor, 0.18f)))
        views.setImageViewResource(R.id.cat_icon, categoryIconRes(entry.category))
        views.setInt(R.id.cat_icon, "setColorFilter", catColor)

        // Kategorie-Pille
        views.setColorStateList(R.id.cat_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(catColor, 0.22f)))
        views.setTextViewText(R.id.cat_pill, categoryLabel(entry.category))
        views.setTextColor(R.id.cat_pill, catColor)

        // Titel + Beschreibung
        views.setTextViewText(R.id.task_title, entry.title)
        views.setTextColor(R.id.task_title, palette.textPrimary)
        if (entry.description.isNotBlank()) {
            views.setTextViewText(R.id.task_description, entry.description)
            views.setTextColor(R.id.task_description, palette.textSecondary)
            views.setViewVisibility(R.id.task_description, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.task_description, android.view.View.GONE)
        }

        // Prio-Zahl
        views.setTextViewText(R.id.task_prio_value, entry.priorityScore.toInt().toString())
        views.setTextColor(R.id.task_prio_value, prioColor)

        // Check-Box
        views.setColorStateList(R.id.task_check_box, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.border))

        // Severity-Bar (10 Segmente)
        applySeverityBar(views, entry.severity)

        // Tags (bis zu 3)
        applyTags(views, entry.tags)

        // Meta-Row: Bucket-Label + Dauer + ggf. Empfohlen + Wearable
        val metaText = buildMetaText(entry)
        views.setTextViewText(R.id.meta_text, metaText)
        views.setTextColor(R.id.meta_text, palette.textSecondary)
        if (entry.priorityScore > 70) {
            views.setViewVisibility(R.id.empfohlen_badge, android.view.View.VISIBLE)
            views.setColorStateList(R.id.empfohlen_badge, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(palette.prioGreen, 0.18f)))
            views.setTextColor(R.id.empfohlen_badge, palette.prioGreen)
        } else {
            views.setViewVisibility(R.id.empfohlen_badge, android.view.View.GONE)
        }
        if (entry.biomarkerSnapshotId != null) {
            views.setViewVisibility(R.id.wearable_icon, android.view.View.VISIBLE)
            views.setInt(R.id.wearable_icon, "setColorFilter", palette.bucketSpaeter)
        } else {
            views.setViewVisibility(R.id.wearable_icon, android.view.View.GONE)
        }

        // KI/Manuell-Pille
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

        // FillInIntent fuer Tap auf die Karte (ACTION_FOCUS) und KI/Manuell-Pille (ACTION_RESCHEDULE)
        val cardFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_TASK_ID, entry.id)
            putExtra(WidgetIntents.EXTRA_ACTION, WidgetIntents.ACTION_FOCUS)
            data = android.net.Uri.parse("widget://task/${entry.id}/focus")
        }
        views.setOnClickFillInIntent(R.id.task_card_root, cardFillIn)
        val pillFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_TASK_ID, entry.id)
            putExtra(WidgetIntents.EXTRA_ACTION, WidgetIntents.ACTION_RESCHEDULE)
            data = android.net.Uri.parse("widget://task/${entry.id}/reschedule")
        }
        views.setOnClickFillInIntent(R.id.bucket_status_pill, pillFillIn)

        return views
    }

    private fun applySeverityBar(views: RemoteViews, severity: Int) {
        val sev = severity.coerceIn(1, 10)
        val segIds = intArrayOf(
            R.id.seg_0, R.id.seg_1, R.id.seg_2, R.id.seg_3, R.id.seg_4,
            R.id.seg_5, R.id.seg_6, R.id.seg_7, R.id.seg_8, R.id.seg_9,
        )
        for (i in 0 until 10) {
            val color = if (i < sev) {
                when (i) {
                    in 0..1 -> palette.prioBlue
                    in 2..3 -> palette.prioGreen
                    in 4..5 -> palette.prioYellow
                    in 6..7 -> palette.prioOrange
                    else -> palette.prioRed
                }
            } else {
                palette.severityEmpty
            }
            views.setColorStateList(segIds[i], "setBackgroundTintList", android.content.res.ColorStateList.valueOf(color))
        }
    }

    private fun applyTags(views: RemoteViews, tags: List<String>) {
        val tagIds = intArrayOf(R.id.tag_1, R.id.tag_2, R.id.tag_3)
        val spacerIds = intArrayOf(R.id.tag_2_spacer, R.id.tag_3_spacer)
        if (tags.isEmpty()) {
            views.setViewVisibility(R.id.tags_row, android.view.View.GONE)
            return
        }
        views.setViewVisibility(R.id.tags_row, android.view.View.VISIBLE)
        val take = tags.take(3)
        for (i in 0 until 3) {
            if (i < take.size) {
                views.setViewVisibility(tagIds[i], android.view.View.VISIBLE)
                views.setTextViewText(tagIds[i], take[i])
                views.setColorStateList(tagIds[i], "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.surfaceMuted))
                views.setTextColor(tagIds[i], palette.textSecondary)
                if (i > 0) {
                    views.setViewVisibility(spacerIds[i - 1], android.view.View.VISIBLE)
                }
            } else {
                views.setViewVisibility(tagIds[i], android.view.View.GONE)
                if (i > 0) views.setViewVisibility(spacerIds[i - 1], android.view.View.GONE)
            }
        }
    }

    private fun buildMetaText(entry: EntropyEntryEntity): String {
        val bucketShort = when (entry.timeBucket) {
            TimeBucket.HEUTE -> "heute"
            TimeBucket.MORGEN -> "morgen"
            TimeBucket.FREIBLOCK -> "Freiblock"
            TimeBucket.SPAETER -> "später"
        }
        val dur = entry.estimatedDurationMinutes?.let { mins ->
            when {
                mins < 60 -> "$mins min"
                mins < 24 * 60 -> "${mins / 60} h"
                else -> "${mins / (24 * 60)} d"
            }
        }
        return if (dur != null) "$bucketShort, $dur" else bucketShort
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

internal fun cardBackgroundRes(p: SimpleWidgetPalette, bucket: TimeBucket): Int =
    if (p.isDark) when (bucket) {
        TimeBucket.HEUTE -> R.drawable.bg_card_heute_dark
        TimeBucket.MORGEN -> R.drawable.bg_card_morgen_dark
        TimeBucket.FREIBLOCK -> R.drawable.bg_card_freiblock_dark
        TimeBucket.SPAETER -> R.drawable.bg_card_spaeter_dark
    } else when (bucket) {
        TimeBucket.HEUTE -> R.drawable.bg_card_heute_light
        TimeBucket.MORGEN -> R.drawable.bg_card_morgen_light
        TimeBucket.FREIBLOCK -> R.drawable.bg_card_freiblock_light
        TimeBucket.SPAETER -> R.drawable.bg_card_spaeter_light
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
