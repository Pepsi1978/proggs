package de.frank.entropyreducer.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Home-Screen-Widget v3 (Frank-Wunsch 2026-05-11, zweite Iteration).
 *
 * Verbesserungen gegenueber v2:
 *  - Karte 1:1 zur App-Karte: Icon-Kreis links + Title-Block + Prio-Zahl + Check-Box rechts
 *  - KI/Manuell-Pille deutlich prominenter mit Bucket-Icon + Text
 *  - Settings-Icon oben rechts im Header
 *  - Hell/Dunkel-Theme via WidgetPalette (gesteuert per WidgetSettingsScreen)
 *  - Severity-Bar mit 10 feinen Segmenten statt 5
 *  - 4 Bereichs-Sektionen (Heute/Morgen/Freiblock/Spaeter) mit prominenten Headern
 *
 * Glance-Realitaet:
 *  - Glas-Effekt (Blur, GraphicsLayer) ist in RemoteViews unmoeglich. Wir
 *    nutzen surfaceCard-Hintergrund + Bucket-Tint-Overlay (cardTintAlpha aus
 *    Palette) — wirkt aus Distanz wie Glas.
 *  - Severity-Bar als 10 nebeneinander-Boxen statt Canvas-Gradient.
 *  - Compose ImageVectors (Icons.Outlined.Psychology etc.) sind hier nicht
 *    nutzbar — stattdessen XML-Vector-Drawables aus res/drawable.
 *
 * Update:
 *  - System: alle 30 Min via PeriodicWorkRequest (widget_info.xml)
 *  - App: nach jedem Bucket-Wechsel ruft TasksScreen updateAll(context)
 *  - Settings-Aenderung: WidgetSettingsScreen ruft updateAll
 */
class EntropyReducerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val dao = entry.entryDao()
        val settings = entry.appSettings()

        val themeMode = settings.readWidgetThemeModeOnce()
        val palette = resolveWidgetPalette(context, themeMode)
        val onlyToday = settings.readWidgetOnlyTodayOnce()
        val bgAlpha = settings.readWidgetBgAlphaOnce()
        android.util.Log.i("WidgetWidget", "provideGlance: themeMode=$themeMode, onlyToday=$onlyToday, bgAlpha=$bgAlpha")

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

        provideContent {
            GlanceTheme {
                WidgetContent(grouped = grouped, palette = palette, onlyToday = onlyToday, bgAlpha = bgAlpha)
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun entryDao(): EntropyEntryDao
        fun appSettings(): AppSettings
    }
}

private val ALL_BUCKETS = listOf(
    TimeBucket.HEUTE,
    TimeBucket.MORGEN,
    TimeBucket.FREIBLOCK,
    TimeBucket.SPAETER,
)

// --- Bucket-Helpers ---

private fun bucketTint(palette: WidgetPalette, bucket: TimeBucket): Color = when (bucket) {
    TimeBucket.HEUTE -> palette.bucketHeute
    TimeBucket.MORGEN -> palette.bucketMorgen
    TimeBucket.FREIBLOCK -> palette.bucketFreiblock
    TimeBucket.SPAETER -> palette.bucketSpaeter
}

private fun bucketLabel(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "Heute"
    TimeBucket.MORGEN -> "Morgen"
    TimeBucket.FREIBLOCK -> "Freiblock"
    TimeBucket.SPAETER -> "Später"
}

private fun bucketIconRes(bucket: TimeBucket): Int = when (bucket) {
    TimeBucket.HEUTE -> R.drawable.ic_widget_bucket_today
    TimeBucket.MORGEN -> R.drawable.ic_widget_bucket_morgen
    TimeBucket.FREIBLOCK -> R.drawable.ic_widget_bucket_freiblock
    TimeBucket.SPAETER -> R.drawable.ic_widget_bucket_spaeter
}

/**
 * Karten-Hintergrund mit Gradient-Drawable: BOTTOM-LEFT (transparent ueber
 * surfaceCard) nach TOP-RIGHT (Bucket-Tint mit alpha). Spiegelt das
 * GlassCard-Tint-Verhalten der App (Frank-Wunsch 2026-05-11). Pro Bucket x
 * Theme ein eigenes Drawable (8 total in res/drawable/).
 */
private fun cardBackgroundRes(palette: WidgetPalette, bucket: TimeBucket): Int = if (palette.isDark) {
    when (bucket) {
        TimeBucket.HEUTE -> R.drawable.bg_card_heute_dark
        TimeBucket.MORGEN -> R.drawable.bg_card_morgen_dark
        TimeBucket.FREIBLOCK -> R.drawable.bg_card_freiblock_dark
        TimeBucket.SPAETER -> R.drawable.bg_card_spaeter_dark
    }
} else {
    when (bucket) {
        TimeBucket.HEUTE -> R.drawable.bg_card_heute_light
        TimeBucket.MORGEN -> R.drawable.bg_card_morgen_light
        TimeBucket.FREIBLOCK -> R.drawable.bg_card_freiblock_light
        TimeBucket.SPAETER -> R.drawable.bg_card_spaeter_light
    }
}

/**
 * Kompaktes Bucket-Label fuer EntryMetaRow (Spiegel von TasksScreen Z.1471).
 */
private fun bucketLabelShort(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "heute"
    TimeBucket.MORGEN -> "morgen"
    TimeBucket.FREIBLOCK -> "Freiblock"
    TimeBucket.SPAETER -> "später"
}

/**
 * Geschaetzte Dauer als kompakter Text — Spiegel von TasksScreen Z.1477.
 */
private fun durationHint(minutes: Int?): String? = minutes?.let {
    when {
        it < 60 -> "$it min"
        it < 24 * 60 -> "${it / 60} h"
        else -> "${it / (24 * 60)} d"
    }
}

private fun categoryColor(palette: WidgetPalette, cat: EntropyCategory): Color = when (cat) {
    EntropyCategory.KOERPERLICH -> palette.catPhysical
    EntropyCategory.MENTAL -> palette.catMental
    EntropyCategory.ZEITLICH -> palette.catTemporal
    EntropyCategory.EMOTIONAL -> palette.catEmotional
    EntropyCategory.GESUNDHEITLICH -> palette.catHealth
    EntropyCategory.UMGEBUNG -> palette.catEnvironment
    EntropyCategory.SONSTIGES -> palette.catOther
}

private fun categoryLabel(cat: EntropyCategory): String = when (cat) {
    EntropyCategory.KOERPERLICH -> "Körperlich"
    EntropyCategory.MENTAL -> "Mental"
    EntropyCategory.ZEITLICH -> "Zeitlich"
    EntropyCategory.EMOTIONAL -> "Emotional"
    EntropyCategory.GESUNDHEITLICH -> "Gesundheitlich"
    EntropyCategory.UMGEBUNG -> "Umgebung"
    EntropyCategory.SONSTIGES -> "Sonstiges"
}

private fun categoryIconRes(cat: EntropyCategory): Int = when (cat) {
    EntropyCategory.KOERPERLICH -> R.drawable.ic_widget_cat_physical
    EntropyCategory.MENTAL -> R.drawable.ic_widget_cat_mental
    EntropyCategory.ZEITLICH -> R.drawable.ic_widget_cat_temporal
    EntropyCategory.EMOTIONAL -> R.drawable.ic_widget_cat_emotional
    EntropyCategory.GESUNDHEITLICH -> R.drawable.ic_widget_cat_health
    EntropyCategory.UMGEBUNG -> R.drawable.ic_widget_cat_environment
    EntropyCategory.SONSTIGES -> R.drawable.ic_widget_cat_other
}

private fun priorityColor(palette: WidgetPalette, score: Double): Color = when {
    score >= 80.0 -> palette.prioRed
    score >= 60.0 -> palette.prioOrange
    score >= 40.0 -> palette.prioYellow
    score >= 20.0 -> palette.prioGreen
    else -> palette.prioBlue
}

// --- Layout ---

@Composable
private fun WidgetContent(
    grouped: Map<TimeBucket, List<EntropyEntryEntity>>,
    palette: WidgetPalette,
    onlyToday: Boolean,
    bgAlpha: Float = 1.0f,
) {
    val totalTasks = grouped.values.sumOf { it.size }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.bgRoot.copy(alpha = bgAlpha)))
            .padding(10.dp),
    ) {
        WidgetHeader(palette = palette, totalTasks = totalTasks, onlyToday = onlyToday)
        Spacer(GlanceModifier.height(8.dp))
        if (totalTasks == 0) {
            EmptyState(palette = palette)
        } else {
            // Frank-Wunsch 2026-05-11: LazyColumn durch normale Column ersetzt,
            // damit kein Scrollbar mehr im Widget erscheint. Glance erlaubt
            // keine offizielle Scrollbar-Deaktivierung in LazyColumn — Receiver-
            // Theme wird vom Launcher (Nova) ignoriert. Loesung: ohne
            // scrollable Container, dafuer Limit auf max 5 Tasks pro Bucket
            // damit alles in das Widget passt (insgesamt max 20 Tasks).
            // Wenn ein Bucket >5 Tasks hat, gibt es einen "+X weitere"-Hinweis.
            Column(modifier = GlanceModifier.fillMaxSize()) {
                ALL_BUCKETS.forEach { bucket ->
                    val tasks = grouped[bucket].orEmpty()
                    if (tasks.isNotEmpty()) {
                        BucketHeader(palette = palette, bucket = bucket, count = tasks.size)
                        tasks.take(MAX_TASKS_PER_BUCKET).forEach { entry ->
                            Spacer(GlanceModifier.height(8.dp))
                            TaskCard(entry = entry, palette = palette)
                        }
                        if (tasks.size > MAX_TASKS_PER_BUCKET) {
                            Spacer(GlanceModifier.height(6.dp))
                            MoreTasksHint(
                                palette = palette,
                                count = tasks.size - MAX_TASKS_PER_BUCKET,
                            )
                        }
                        Spacer(GlanceModifier.height(14.dp))
                    }
                }
            }
        }
    }
}

/** Maximum an Tasks pro Bucket im Widget (ohne Scroll). Bei 4 Buckets = 20 Tasks total. */
private const val MAX_TASKS_PER_BUCKET = 5

@Composable
private fun MoreTasksHint(palette: WidgetPalette, count: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(palette.surfaceMuted))
            .cornerRadius(50.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(WidgetIntents.openHeaderAction(WidgetIntents.ACTION_OPEN)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+ $count weitere — antippen zum Oeffnen",
            style = TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun WidgetHeader(palette: WidgetPalette, totalTasks: Int, onlyToday: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Linker Bereich: Title+Counter, oeffnet die App
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(WidgetIntents.openHeaderAction(WidgetIntents.ACTION_OPEN)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (onlyToday) "Heute" else "Aufgaben",
                style = TextStyle(
                    color = ColorProvider(palette.textPrimary),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "$totalTasks offen",
                style = TextStyle(
                    color = ColorProvider(palette.textSecondary),
                    fontSize = 12.sp,
                ),
            )
        }
        // Frank-Wunsch 2026-05-11: Häkchen-Toggle "Nur Heute". Glance-ActionCallback
        // toggelt das DataStore-Flag und rendert das Widget neu — die App
        // wird nicht geoeffnet.
        OnlyTodayToggle(palette = palette, isActive = onlyToday)
        Spacer(GlanceModifier.width(6.dp))
        // Settings-Icon rechts — oeffnet Widget-Settings-Screen
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .background(ColorProvider(palette.surfaceMuted))
                .cornerRadius(50.dp)
                .clickable(WidgetIntents.openHeaderAction(WidgetIntents.ACTION_SETTINGS)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_settings),
                contentDescription = "Widget-Einstellungen",
                modifier = GlanceModifier.size(18.dp),
                colorFilter = androidx.glance.ColorFilter.tint(
                    ColorProvider(palette.textSecondary),
                ),
            )
        }
    }
}

/**
 * Häkchen-Modus-Indikator im Widget-Header (Bugfix 2026-05-11, finale Loesung).
 * Aktiv = nur HEUTE wird angezeigt, inaktiv = alle Buckets.
 *
 * Nach 10 gescheiterten Iterationen mit einem klickbaren Toggle (Glance +
 * Nova Launcher konnten den Klick nicht zuverlaessig zum Render-Update
 * uebersetzen) ist dieser Indikator jetzt NICHT mehr klickbar — er zeigt
 * nur den aktuellen Modus an. Der Wechsel passiert in den Widget-Einstellungen
 * (Tap aufs Settings-Zahnrad rechts daneben oeffnet die Settings, dort
 * gibt es einen "Nur Heute"-Schalter).
 *
 * Aktiv-Farbe: GRUEN (palette.catHealth = #34D399 / #059669) — Frank's Wunsch.
 */
@Composable
private fun OnlyTodayToggle(palette: WidgetPalette, isActive: Boolean) {
    val activeColor = palette.catHealth
    val bg = if (isActive) activeColor.copy(alpha = 0.30f) else palette.surfaceMuted
    val tint = if (isActive) activeColor else palette.textSecondary
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(bg))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            // KEIN clickable mehr — Wechsel via Widget-Einstellungen (siehe oben).
            .clickable(WidgetIntents.openHeaderAction(WidgetIntents.ACTION_SETTINGS)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_check),
            contentDescription = if (isActive) "Filter aktiv: nur Heute" else "Filter aus: alle Buckets",
            modifier = GlanceModifier.size(14.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(tint)),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = if (isActive) "Nur Heute" else "Alle",
            style = TextStyle(
                color = ColorProvider(tint),
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun BucketHeader(palette: WidgetPalette, bucket: TimeBucket, count: Int) {
    val accent = bucketTint(palette, bucket)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(accent.copy(alpha = 0.22f)))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(bucketIconRes(bucket)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp),
                    colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(accent)),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = bucketLabel(bucket),
                    style = TextStyle(
                        color = ColorProvider(accent),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = "$count",
            style = TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun TaskCard(entry: EntropyEntryEntity, palette: WidgetPalette) {
    val catColor = categoryColor(palette, entry.category)
    val prioColor = priorityColor(palette, entry.priorityScore)
    val isManual = entry.manualBucket != null
    val bucketAccent = bucketTint(palette, entry.timeBucket)

    // EINE Box mit Gradient-Drawable als Hintergrund (Frank-Wunsch 2026-05-11):
    // Das Drawable enthaelt cornerRadius UND linearen Gradient bottom-left →
    // top-right (transparent surfaceCard → Bucket-Tint mit alpha). Glance
    // unterstuetzt background(ImageProvider, ContentScale) nativ.
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                imageProvider = ImageProvider(cardBackgroundRes(palette, entry.timeBucket)),
                contentScale = ContentScale.FillBounds,
            )
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(WidgetIntents.openTaskAction(entry.id, WidgetIntents.ACTION_FOCUS)),
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            // TOP-ROW: IconCircle | Title-Block | Prio+Check (4-Spalten wie Original)
            Row(verticalAlignment = Alignment.Top) {
                CategoryIconCircle(palette = palette, color = catColor, iconRes = categoryIconRes(entry.category))
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    CategoryPill(palette = palette, label = categoryLabel(entry.category), color = catColor)
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = entry.title,
                        style = TextStyle(
                            color = ColorProvider(palette.textPrimary),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 2,
                    )
                    if (entry.description.isNotBlank()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = entry.description,
                            style = TextStyle(
                                color = ColorProvider(palette.textSecondary),
                                fontSize = 12.sp,
                            ),
                            maxLines = 2,
                        )
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                // Rechte Spalte: Prio-Zahl + Label + Check-Box
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.priorityScore.toInt()}",
                        style = TextStyle(
                            color = ColorProvider(prioColor),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Prio",
                        style = TextStyle(
                            color = ColorProvider(palette.textSecondary),
                            fontSize = 10.sp,
                        ),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(26.dp)
                            .cornerRadius(8.dp)
                            .background(ColorProvider(palette.border)),
                        contentAlignment = Alignment.Center,
                    ) {}
                }
            }
            Spacer(GlanceModifier.height(12.dp))
            SeverityBar(severity = entry.severity, palette = palette)

            val tagsToShow = entry.tags.take(3)
            if (tagsToShow.isNotEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Row {
                    tagsToShow.forEach { tag ->
                        TagPill(palette = palette, tag = tag)
                        Spacer(GlanceModifier.width(6.dp))
                    }
                }
            }
            Spacer(GlanceModifier.height(10.dp))

            // BOTTOM-ROW: Meta-Info LINKS (Bucket+Dauer, Empfohlen, Wearable) +
            // KI/Manuell-Pille RECHTS — exakt wie im TasksScreen Z.1170-1173.
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntryMetaRow(
                    palette = palette,
                    entry = entry,
                    modifier = GlanceModifier.defaultWeight(),
                )
                BucketStatusPill(
                    palette = palette,
                    bucket = entry.timeBucket,
                    accent = bucketAccent,
                    isManual = isManual,
                    onClickAction = WidgetIntents.openTaskAction(
                        entry.id,
                        WidgetIntents.ACTION_RESCHEDULE,
                    ),
                )
            }
        }
    }
}

/**
 * Meta-Zeile unter den Tags — Spiegel von TasksScreen.EntryMetaRow (Z.1463).
 *  - Bucket-Label (klein) mit Uhr-Icon + optional ", X min" Dauer
 *  - "Empfohlen"-Badge falls priorityScore > 70
 *  - "Wearable"-Indikator falls biomarkerSnapshotId != null
 */
@Composable
private fun EntryMetaRow(
    palette: WidgetPalette,
    entry: EntropyEntryEntity,
    modifier: GlanceModifier = GlanceModifier,
) {
    val bucketText = bucketLabelShort(entry.timeBucket)
    val dur = durationHint(entry.estimatedDurationMinutes)
    val metaText = if (dur != null) "$bucketText, $dur" else bucketText
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_cat_temporal),
            contentDescription = null,
            modifier = GlanceModifier.size(12.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(palette.textSecondary)),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = metaText,
            style = TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 11.sp,
            ),
        )
        if (entry.priorityScore > 70) {
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(palette.prioGreen.copy(alpha = 0.18f)))
                    .cornerRadius(50.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Empfohlen",
                    style = TextStyle(
                        color = ColorProvider(palette.prioGreen),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
        if (entry.biomarkerSnapshotId != null) {
            Spacer(GlanceModifier.width(6.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_widget_cat_health),
                contentDescription = null,
                modifier = GlanceModifier.size(11.dp),
                colorFilter = androidx.glance.ColorFilter.tint(
                    ColorProvider(palette.bucketSpaeter),
                ),
            )
        }
    }
}

@Composable
private fun CategoryIconCircle(palette: WidgetPalette, color: Color, iconRes: Int) {
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .background(ColorProvider(color.copy(alpha = 0.18f)))
            .cornerRadius(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(22.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(color)),
        )
    }
}

@Composable
private fun CategoryPill(palette: WidgetPalette, label: String, color: Color) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(color.copy(alpha = 0.22f)))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun TagPill(palette: WidgetPalette, tag: String) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(palette.surfaceMuted))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 10.sp,
            ),
        )
    }
}

/**
 * Severity-Bar mit 10 feinen Segmenten — 1:1-naehe zur App-Bar (die Canvas
 * mit Gradient nutzt; das geht in RemoteViews nicht). Jedes Segment ist
 * eine kleine Box mit dem severity-Wert als Fuellung.
 *
 * Bugfix 2026-05-11: Glance/RemoteViews erlaubt MAX 10 KINDER pro Container.
 * 10 Boxes + 9 Spacer waren 19 Elemente → IllegalArgumentException, das
 * gesamte Widget konnte nicht rendern (Widget blieb im alten State sichtbar
 * obwohl DataStore-Toggle korrekt war). Loesung: Nested Rows (Outer Row mit
 * 2 Half-Rows). Outer-Row = 3 Elemente, Inner-Rows = je 9 Elemente.
 */
@Composable
private fun SeverityBar(severity: Int, palette: WidgetPalette) {
    val sev = severity.coerceIn(1, 10)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        SeverityBarHalf(
            startIndex = 0,
            endIndex = 5,
            sev = sev,
            palette = palette,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(2.dp))
        SeverityBarHalf(
            startIndex = 5,
            endIndex = 10,
            sev = sev,
            palette = palette,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

/**
 * 5 Severity-Segmente in einer Row — Helper fuer SeverityBar Split.
 * 5 Boxes + 4 Spacer = 9 Elemente (unter dem Glance-10-Limit).
 */
@Composable
private fun SeverityBarHalf(
    startIndex: Int,
    endIndex: Int,
    sev: Int,
    palette: WidgetPalette,
    modifier: GlanceModifier,
) {
    Row(modifier = modifier) {
        for (i in startIndex until endIndex) {
            val isFilled = i < sev
            val segColor = if (isFilled) {
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
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(7.dp)
                    .background(ColorProvider(segColor))
                    .cornerRadius(2.dp),
            ) {}
            if (i < endIndex - 1) Spacer(GlanceModifier.width(2.dp))
        }
    }
}

@Composable
private fun BucketStatusPill(
    palette: WidgetPalette,
    bucket: TimeBucket,
    accent: Color,
    isManual: Boolean,
    onClickAction: androidx.glance.action.Action,
) {
    val bgColor = if (isManual) accent.copy(alpha = 0.22f) else palette.surfaceMuted
    val textColor = if (isManual) accent else palette.textSecondary
    val label = if (isManual) "manuell" else "KI"
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(bgColor))
            .cornerRadius(50.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClickAction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(bucketIconRes(bucket)),
            contentDescription = null,
            modifier = GlanceModifier.size(14.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(textColor)),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 12.sp,
                fontWeight = if (isManual) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun EmptyState(palette: WidgetPalette) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Keine offenen Aufgaben.\nAntippen, um die App zu öffnen.",
            style = TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 14.sp,
            ),
        )
    }
}

class EntropyReducerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntropyReducerWidget()
}
