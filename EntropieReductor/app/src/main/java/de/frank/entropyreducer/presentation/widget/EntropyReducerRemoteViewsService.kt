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
import de.frank.entropyreducer.data.local.dao.RecurringTemplateDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
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
        fun recurringDao(): RecurringTemplateDao
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
        val recurringDao = entry.recurringDao()

        runBlocking {
            val themeMode = settings.readWidgetThemeModeOnce()
            palette = resolveWidgetPaletteSimple(context, themeMode)
            val onlyToday = settings.readWidgetOnlyTodayOnce()

            val all = dao.getActive().first()
                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }

            // Bei "Sehr hoch" wird nur der höchste Prioritätsbereich gezeigt; sonst alle fünf.
            // Loop und Erledigt kommen IMMER dazu (Frank-Wunsch 2026-05-31).
            val bucketsToShow = if (onlyToday) listOf(TimeBucket.HEUTE) else ALL_BUCKETS
            val grouped: Map<TimeBucket, List<EntropyEntryEntity>> =
                bucketsToShow.associateWith { bucket ->
                    all.filter { priorityBucketForScore(it.manualPriorityScore ?: it.priorityScore) == bucket }
                        .sortedByDescending { it.manualPriorityScore ?: it.priorityScore }
                }

            // Erledigte (REDUZIERT) — fuer die Erledigt-Sektion, neueste zuerst.
            val resolved = runCatching {
                dao.getRecentlyResolved(sinceMillis = 0L).first()
                    .sortedByDescending { it.resolvedAt ?: it.updatedAt }
            }.getOrDefault(emptyList())

            // Loop-Vorlagen (wiederkehrende Aufgaben) — fuer die Loop-Sektion.
            val templates = runCatching {
                recurringDao.observeAll().first()
            }.getOrDefault(emptyList())

            // Akkordeon (Frank-Wunsch 2026-05-31): ALLE Sektions-Header sind IMMER sichtbar
            // (auch leer), damit Frank durchschalten kann. Eintraege erscheinen nur in der
            // aktuell offenen Sektion (WidgetExpandState, Default HEUTE).
            val expandedName = WidgetExpandState.get()
            val list = mutableListOf<WidgetListItem>()

            // 1.-5. Prioritätsbereiche
            bucketsToShow.forEach { bucket ->
                val tasks = grouped[bucket].orEmpty()
                val isExpanded = expandedName == bucket.name
                list.add(
                    WidgetListItem.SectionHeader(
                        key = bucket.name,
                        label = bucketLabel(bucket).uppercase(),
                        iconRes = bucketIconRes(bucket),
                        accent = bucketColor(palette, bucket),
                        count = tasks.size,
                        expanded = isExpanded,
                    ),
                )
                if (isExpanded) tasks.forEach { list.add(WidgetListItem.Task(it, resolved = false)) }
            }

            // 5. Loop
            val loopExpanded = expandedName == SECTION_LOOP
            list.add(
                WidgetListItem.SectionHeader(
                    key = SECTION_LOOP,
                    label = "LOOP",
                    iconRes = R.drawable.ic_widget_loop,
                    accent = palette.prioOrange,
                    count = templates.size,
                    expanded = loopExpanded,
                ),
            )
            if (loopExpanded) templates.forEach { list.add(WidgetListItem.Loop(it)) }

            // 6. Erledigt
            val resolvedExpanded = expandedName == SECTION_ERLEDIGT
            list.add(
                WidgetListItem.SectionHeader(
                    key = SECTION_ERLEDIGT,
                    label = "ERLEDIGT",
                    iconRes = R.drawable.ic_widget_done,
                    accent = WIDGET_SUCCESS,
                    count = resolved.size,
                    expanded = resolvedExpanded,
                ),
            )
            if (resolvedExpanded) resolved.forEach { list.add(WidgetListItem.Task(it, resolved = true)) }

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
            is WidgetListItem.SectionHeader -> buildSectionHeader(item)
            is WidgetListItem.Task -> buildTaskCard(item.entry, item.resolved)
            is WidgetListItem.Loop -> buildLoopCard(item.template)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) return position.toLong()
        return when (val it = items[position]) {
            is WidgetListItem.SectionHeader -> it.key.hashCode().toLong()
            is WidgetListItem.Task -> it.entry.id.hashCode().toLong()
            is WidgetListItem.Loop -> ("loop-" + it.template.id).hashCode().toLong()
        }
    }

    override fun hasStableIds(): Boolean = true

    // === Bucket-Header ===

    private fun buildSectionHeader(item: WidgetListItem.SectionHeader): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item_bucket_header)
        val accent = item.accent
        // Icon-Kaestchen: Akzent mit 18% Alpha + Icon in Akzentfarbe.
        views.setColorStateList(R.id.bucket_icon_box, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(accent, 0.18f)))
        views.setImageViewResource(R.id.bucket_icon, item.iconRes)
        views.setInt(R.id.bucket_icon, "setColorFilter", accent)
        // Grosses Label (Grossbuchstaben) in textPrimary.
        views.setTextViewText(R.id.bucket_label, item.label)
        views.setTextColor(R.id.bucket_label, palette.textPrimary)
        // Count-Pille: Akzent 18% Hintergrund + Akzent-Text.
        views.setColorStateList(R.id.bucket_count, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(accent, 0.18f)))
        views.setTextViewText(R.id.bucket_count, item.count.toString())
        views.setTextColor(R.id.bucket_count, accent)
        // Chevron: hoch = offen, runter = zu.
        views.setImageViewResource(
            R.id.bucket_chevron,
            if (item.expanded) R.drawable.ic_widget_chevron_up else R.drawable.ic_widget_chevron_down,
        )
        views.setInt(R.id.bucket_chevron, "setColorFilter", accent)
        // Offene Sektion bekommt eine zarte Akzent-Toenung (wie die App-AccordionHeaderRow).
        if (item.expanded) {
            views.setInt(R.id.bucket_header_root, "setBackgroundResource", R.drawable.widget_section_header_bg)
            views.setColorStateList(R.id.bucket_header_root, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(applyAlpha(accent, 0.12f)))
        } else {
            views.setInt(R.id.bucket_header_root, "setBackgroundResource", 0)
        }
        // Tap auf den ganzen Header → diese Sektion auf-/zuklappen (ohne App zu oeffnen).
        val toggleFill = Intent().apply {
            putExtra(WidgetIntents.EXTRA_ACTION, WidgetIntents.ACTION_TOGGLE_BUCKET)
            putExtra(WidgetIntents.EXTRA_BUCKET, item.key)
            data = android.net.Uri.parse("widget://section/${item.key}/toggle")
        }
        views.setOnClickFillInIntent(R.id.bucket_header_root, toggleFill)
        return views
    }

    private fun buildLoopCard(template: RecurringTemplateEntity): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item_loop)
        // Verlaufs-Hintergrund nach Prioritaet (gleiche Rampe wie Aufgaben/App).
        views.setColorStateList(
            R.id.loop_card_root,
            "setBackgroundTintList",
            android.content.res.ColorStateList.valueOf(priorityRampArgb(template.priorityScore.toDouble())),
        )
        views.setInt(R.id.loop_icon, "setColorFilter", palette.textPrimary)
        views.setTextViewText(R.id.loop_title, template.title)
        views.setTextColor(R.id.loop_title, palette.textPrimary)

        // Prio-Perle: "Priorität <Wert>" — gleiche Optik wie die Aufgaben-Karte.
        views.setTextViewText(R.id.loop_prio_pill, "Priorität ${template.priorityScore}")
        views.setColorStateList(R.id.loop_prio_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.pearlBg))
        views.setTextColor(R.id.loop_prio_pill, palette.pearlText)

        // Tag-Perle: "KI" (automatisch) oder der vorgegebene Tag.
        val bucketText = template.targetBucket?.let { bucketLabel(it) } ?: "KI"
        views.setTextViewText(R.id.loop_bucket_pill, bucketText)
        views.setColorStateList(R.id.loop_bucket_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.pearlBg))
        views.setTextColor(R.id.loop_bucket_pill, palette.pearlText)

        // FillIns (EXTRA_TASK_ID traegt die Template-ID):
        //  - Karte      → ACTION_OPEN: oeffnet die App (Aktiv/Inaktiv bleibt dort)
        //  - Prio-Perle → ACTION_SET_LOOP_PRIORITY: App + Schieberegler dieser Vorlage
        //  - Tag-Perle  → ACTION_SET_LOOP_BUCKET: App + Tag-Auswahl dieser Vorlage
        views.setOnClickFillInIntent(R.id.loop_card_root, fillIn(template.id, WidgetIntents.ACTION_OPEN))
        views.setOnClickFillInIntent(R.id.loop_prio_pill, fillIn(template.id, WidgetIntents.ACTION_SET_LOOP_PRIORITY))
        views.setOnClickFillInIntent(R.id.loop_bucket_pill, fillIn(template.id, WidgetIntents.ACTION_SET_LOOP_BUCKET))
        return views
    }

    // === Aufgaben-Karte ===

    private fun buildTaskCard(entry: EntropyEntryEntity, resolved: Boolean): RemoteViews {
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

        // Haekchen: normalerweise leeres Quadrat mit deutlicher dunkler Umrandung
        // (klar als Klickfeld erkennbar). Direkt nach dem Tap (ID im WidgetCheckState)
        // kurz ein gefuelltes gruenes Haekchen — dann verschwindet die Aufgabe.
        if (resolved || WidgetCheckState.isChecking(entry.id)) {
            // Erledigt (Erledigt-Sektion) ODER gerade abgehakt: gefuelltes gruenes Haekchen.
            views.setInt(R.id.task_check_box, "setBackgroundResource", R.drawable.widget_check_box_done)
            views.setColorStateList(R.id.task_check_box, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(WIDGET_SUCCESS))
            views.setImageViewResource(R.id.task_check_box, R.drawable.ic_widget_check)
            views.setInt(R.id.task_check_box, "setColorFilter", 0xFFFFFFFF.toInt())
        } else {
            // Offenes Feld: weisse Fuellung + graue Umrandung kommen FEST aus dem Drawable.
            // Tint MUSS explizit auf null zurueckgesetzt werden — sonst erbt eine aus einer
            // erledigten/abgehakten Karte recycelte ListView-Zeile deren gruenen
            // SRC_IN-Tint (RemoteViews-Recycling toent kumulativ). Genau das liess mit der
            // Zeit ALLE Haekchenfelder gruen werden. null = Drawable zeigt seine echten
            // Farben (weiss + grau), ohne dass ein eigener Tint die zwei Farben ueberschreibt.
            views.setInt(R.id.task_check_box, "setBackgroundResource", R.drawable.widget_check_box_bg)
            views.setColorStateList(R.id.task_check_box, "setBackgroundTintList", null)
            views.setImageViewResource(R.id.task_check_box, 0)
        }

        // Titel (eine Zeile)
        views.setTextViewText(R.id.task_title, entry.title)
        views.setTextColor(R.id.task_title, palette.textPrimary)

        // Prioritaet-Perle: "Priorität KI" wenn die KI bestimmt, sonst "Priorität <Wert>".
        // Farben EXAKT wie die App-Karte: glassBg-Hintergrund + textSecondary-Text (neutral).
        val prioLabel = entry.manualPriorityScore
            ?.let { "Priorität ${it.toInt()}" }
            ?: "Priorität KI"
        views.setTextViewText(R.id.task_prio_pill, prioLabel)
        views.setColorStateList(R.id.task_prio_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.pearlBg))
        views.setTextColor(R.id.task_prio_pill, palette.pearlText)

        // KI/Manuell-Perle (Priorität): wie in der App KOMPLETT identisch zur Prio-Perle —
        // gleicher glassBg-Hintergrund + gleiche textSecondary-Farbe fuer Icon UND Text.
        // Nur das Wort unterscheidet ("KI" vs. "manuell"); kein Akzent/Hellblau mehr.
        val isManual = entry.manualPriorityScore != null
        val priorityBucket = priorityBucketForScore(effectivePriority)
        views.setColorStateList(R.id.bucket_status_pill, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(palette.pearlBg))
        views.setInt(R.id.bucket_status_icon, "setColorFilter", palette.pearlText)
        views.setTextViewText(R.id.bucket_status_label, if (isManual) "manuell" else "KI")
        views.setTextColor(R.id.bucket_status_label, palette.pearlText)
        views.setImageViewResource(R.id.bucket_status_icon, bucketIconRes(priorityBucket))

        // FillInIntents (kombiniert mit dem Broadcast-Template aus dem Provider):
        //  - Haekchen   → ACTION_COMPLETE: hakt die Aufgabe DIREKT ab, ohne App
        //  - Karte      → ACTION_FOCUS: oeffnet die App (Detail)
        //  - Prio-Perle → ACTION_SET_PRIORITY: oeffnet die App und klappt direkt den
        //                 Prio-Schieber DIESER Aufgabe auf (manuelle Prioritaet setzen)
        //  - Bereich    → ACTION_RESCHEDULE: öffnet die App (dort der Prioritäts-Picker)
        // Bei erledigten Eintraegen hakt das Haekchen NICHT erneut ab — Tap oeffnet die App.
        views.setOnClickFillInIntent(
            R.id.task_check_box,
            fillIn(entry.id, if (resolved) WidgetIntents.ACTION_FOCUS else WidgetIntents.ACTION_COMPLETE),
        )
        views.setOnClickFillInIntent(R.id.task_card_root, fillIn(entry.id, WidgetIntents.ACTION_FOCUS))
        views.setOnClickFillInIntent(R.id.task_prio_pill, fillIn(entry.id, WidgetIntents.ACTION_SET_PRIORITY))
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
    /** Generischer Sektions-Header (Prioritätsbereich, Loop oder Erledigt). */
    data class SectionHeader(
        val key: String,
        val label: String,
        val iconRes: Int,
        val accent: Int,
        val count: Int,
        val expanded: Boolean,
    ) : WidgetListItem()
    data class Task(val entry: EntropyEntryEntity, val resolved: Boolean) : WidgetListItem()
    data class Loop(val template: RecurringTemplateEntity) : WidgetListItem()
}

/** Sektions-Schlüssel für Loop und Erledigt (Prioritätsbereiche nutzen TimeBucket.name). */
internal const val SECTION_LOOP = "LOOP"
internal const val SECTION_ERLEDIGT = "ERLEDIGT"

// === Helpers ===

/** Mintgruen (CosmosColors.Success) fuer das kurz sichtbare Erledigt-Haekchen. */
internal val WIDGET_SUCCESS: Int = 0xFF34D399.toInt()

/**
 * Haelt die IDs der Aufgaben, die gerade abgehakt wurden und kurz ein gefuelltes
 * Haekchen zeigen sollen, BEVOR sie aus der Liste verschwinden (Frank-Wunsch
 * 2026-05-31, Mini-Animation). Der WidgetActionReceiver markiert die ID, refresht,
 * wartet kurz, markiert die Aufgabe als erledigt und entfernt die ID wieder.
 */
object WidgetCheckState {
    private val checking = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    fun mark(id: String) { checking.add(id) }
    fun clear(id: String) { checking.remove(id) }
    fun isChecking(id: String): Boolean = checking.contains(id)
}

/**
 * Akkordeon-State des Widgets (Frank-Wunsch 2026-05-31): welcher Bucket-Header
 * gerade aufgeklappt ist (TimeBucket.name) — analog zu expandedSection in der App.
 * Immer nur EINE Sektion offen; null = alle zu. Default: sehr hoch offen. In-memory
 * (wie rememberSaveable in der App, ueberlebt keinen Prozess-Tod — Default greift dann).
 */
object WidgetExpandState {
    @Volatile
    private var expanded: String? = TimeBucket.HEUTE.name
    fun get(): String? = expanded
    fun toggle(bucketName: String) {
        expanded = if (expanded == bucketName) null else bucketName
    }
}

internal val ALL_BUCKETS = listOf(
    TimeBucket.HEUTE,
    TimeBucket.MORGEN,
    TimeBucket.FREIBLOCK,
    TimeBucket.GERING,
    TimeBucket.SPAETER,
)

internal fun applyAlpha(color: Int, alpha: Float): Int =
    ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * 255).toInt())

internal fun bucketColor(p: SimpleWidgetPalette, bucket: TimeBucket): Int = when (bucket) {
    TimeBucket.HEUTE -> p.prioRed
    TimeBucket.MORGEN -> p.prioOrange
    TimeBucket.FREIBLOCK -> p.prioYellow
    TimeBucket.GERING -> p.prioGreen
    TimeBucket.SPAETER -> p.prioBlue
}

internal fun bucketLabel(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "Sehr hoch"
    TimeBucket.MORGEN -> "Hoch"
    TimeBucket.FREIBLOCK -> "Mittel"
    TimeBucket.GERING -> "Gering"
    TimeBucket.SPAETER -> "Später"
}

internal fun bucketIconRes(bucket: TimeBucket): Int = when (bucket) {
    TimeBucket.HEUTE -> R.drawable.ic_widget_bucket_today
    TimeBucket.MORGEN -> R.drawable.ic_widget_bucket_morgen
    TimeBucket.FREIBLOCK -> R.drawable.ic_widget_bucket_freiblock
    TimeBucket.GERING -> R.drawable.ic_widget_bucket_freiblock
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
