package de.frank.entropyreducer.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Home-Screen-Widget v2 (Frank-Wunsch 2026-05-11).
 *
 * Scrollbare Aufgaben-Liste die den Tagesplan-Inhalt 1:1 wiederspiegelt:
 *  - 4 Sektionen (HEUTE / MORGEN / FREIBLOCK / SPAETER) mit jeweils eigener
 *    Header-Pille in der Bucket-Akzent-Farbe
 *  - Karten in derselben Reihenfolge wie im TasksScreen (priorityScore desc)
 *  - Karten 1:1 nachgebaut: Bucket-Tint-Hintergrund, Kategorie-Pille mit
 *    Akzent-Farbe, Title + Description, Prio-Zahl in Score-Farbe + "Prio"-
 *    Label, Severity-Bar als 5 Boxen, Tags-Pills, KI/Manuell-Status-Pille
 *  - Zwei Tap-Zonen pro Karte:
 *      * Tap irgendwo auf der Karte → MainActivity oeffnet mit Fokus auf Task
 *      * Tap auf KI/Manuell-Pille → MainActivity oeffnet + Bucket-Picker-Sheet
 *
 * Glance-Realitaet: GlassCard (Glas-Effekt), graphicsLayer, BorderStroke und
 * Canvas-basierte Severity-Bar gibt es in RemoteViews nicht. Stattdessen
 * Bucket-Tint-Background, cornerRadius, Boxen-Severity-Bar — die Optik bleibt
 * dem TasksScreen sehr nahe, ist aber nicht pixelgenau identisch (technisch
 * unmoeglich).
 *
 * Update: alle 15 Min via PeriodicWorkRequest (widget_info.xml) ODER auf
 * Abruf via `EntropyReducerWidget().updateAll(context)` nach jedem DB-
 * Schreibvorgang in TasksScreen.
 */
class EntropyReducerWidget : GlanceAppWidget() {

    // SizeMode.Exact: bei jedem Resize wird provideGlance neu aufgerufen,
    // damit die Liste sich an Hoehe/Breite anpasst (z.B. Fullscreen-Resize
    // zeigt mehr Karten als Mini-Variante).
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .entryDao()
        // Alle aktiven Eintraege laden (ohne ARCHIVIERT). REDUZIERT (erledigt)
        // kommt zwar mit, wird aber im Widget nicht angezeigt — Frank will im
        // Widget nur OFFENE/IN_ARBEIT-Karten sehen.
        val all = dao.getActive().first()
            .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
        // Pro Bucket nach priorityScore absteigend sortieren — identisch zur
        // TasksScreen-Sortierung.
        val grouped: Map<TimeBucket, List<EntropyEntryEntity>> = ALL_BUCKETS.associateWith { bucket ->
            all.filter { it.timeBucket == bucket }.sortedByDescending { it.priorityScore }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(grouped = grouped)
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun entryDao(): EntropyEntryDao
    }
}

private val ALL_BUCKETS = listOf(
    TimeBucket.HEUTE,
    TimeBucket.MORGEN,
    TimeBucket.FREIBLOCK,
    TimeBucket.SPAETER,
)

// --- Farb-Konstanten (Spiegel von theme/Color.kt) ---
// Glance laeuft im RemoteViews-Process — Compose-Theme-Tokens (LocalCosmos,
// MaterialTheme) sind hier NICHT verfuegbar. Werte werden 1:1 aus
// theme/Color.kt gespiegelt; Aenderungen dort muessen hier mitgezogen werden.
private val BgRoot = Color(0xFF0E0B24)
private val SurfaceCard = Color(0xFF1A1438)
private val SurfaceMuted = Color(0xFF2A1F44)
private val TextPrimary = Color(0xFFEAE6FF)
private val TextSecondary = Color(0xFFA499D9)

private val BucketHeuteTint = Color(0xFFF472B6)
private val BucketMorgenTint = Color(0xFFFBBF24)
private val BucketFreiblockTint = Color(0xFF22C55E)
private val BucketSpaeterTint = Color(0xFF60A5FA)

private val CatPhysical = Color(0xFFF87171)
private val CatMental = Color(0xFFA78BFA)
private val CatTemporal = Color(0xFFFBBF24)
private val CatEmotional = Color(0xFFF472B6)
private val CatHealth = Color(0xFF34D399)
private val CatEnvironment = Color(0xFF22D3EE)
private val CatOther = Color(0xFF94A3B8)

private val PrioRed = Color(0xFFEF4444)
private val PrioOrange = Color(0xFFF97316)
private val PrioYellow = Color(0xFFEAB308)
private val PrioGreen = Color(0xFF22C55E)
private val PrioBlue = Color(0xFF3B82F6)

private fun bucketTint(bucket: TimeBucket): Color = when (bucket) {
    TimeBucket.HEUTE -> BucketHeuteTint
    TimeBucket.MORGEN -> BucketMorgenTint
    TimeBucket.FREIBLOCK -> BucketFreiblockTint
    TimeBucket.SPAETER -> BucketSpaeterTint
}

private fun bucketLabel(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "Heute"
    TimeBucket.MORGEN -> "Morgen"
    TimeBucket.FREIBLOCK -> "Freiblock"
    TimeBucket.SPAETER -> "Später"
}

private fun categoryColor(cat: EntropyCategory): Color = when (cat) {
    EntropyCategory.KOERPERLICH -> CatPhysical
    EntropyCategory.MENTAL -> CatMental
    EntropyCategory.ZEITLICH -> CatTemporal
    EntropyCategory.EMOTIONAL -> CatEmotional
    EntropyCategory.GESUNDHEITLICH -> CatHealth
    EntropyCategory.UMGEBUNG -> CatEnvironment
    EntropyCategory.SONSTIGES -> CatOther
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

private fun priorityColor(score: Double): Color = when {
    score >= 80.0 -> PrioRed
    score >= 60.0 -> PrioOrange
    score >= 40.0 -> PrioYellow
    score >= 20.0 -> PrioGreen
    else -> PrioBlue
}

// --- Widget-Layout ---

@Composable
private fun WidgetContent(grouped: Map<TimeBucket, List<EntropyEntryEntity>>) {
    val totalTasks = grouped.values.sumOf { it.size }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BgRoot))
            .padding(10.dp),
    ) {
        // Header: Logo-Pille + Gesamt-Zaehler + Tap-to-open (oeffnet App ohne
        // Task-Fokus → landet auf dem zuletzt aktiven Tab)
        WidgetHeader(totalTasks = totalTasks)
        Spacer(GlanceModifier.height(8.dp))
        if (totalTasks == 0) {
            EmptyState()
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                ALL_BUCKETS.forEach { bucket ->
                    val tasks = grouped[bucket].orEmpty()
                    if (tasks.isNotEmpty()) {
                        item(itemId = bucket.ordinal.toLong()) {
                            BucketHeader(bucket = bucket, count = tasks.size)
                        }
                        items(items = tasks, itemId = { it.id.hashCode().toLong() }) { entry ->
                            Column {
                                Spacer(GlanceModifier.height(6.dp))
                                TaskCard(entry = entry)
                            }
                        }
                        item(itemId = (bucket.ordinal + 100).toLong()) {
                            Spacer(GlanceModifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(totalTasks: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Aufgaben",
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "$totalTasks offen",
            style = TextStyle(
                color = ColorProvider(TextSecondary),
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun BucketHeader(bucket: TimeBucket, count: Int) {
    val accent = bucketTint(bucket)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Akzent-Pille mit Bucket-Name
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(accent.copy(alpha = 0.22f)))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = bucketLabel(bucket),
                style = TextStyle(
                    color = ColorProvider(accent),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = "$count",
            style = TextStyle(
                color = ColorProvider(TextSecondary),
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun TaskCard(entry: EntropyEntryEntity) {
    val catColor = categoryColor(entry.category)
    val prioColor = priorityColor(entry.priorityScore)
    val tintBase = bucketTint(entry.timeBucket).copy(alpha = 0.12f)
    val isManual = entry.manualBucket != null
    val bucketAccent = bucketTint(entry.timeBucket)

    // AEUSSERE Box: clickable = Karte-Focus-Action (Tap irgendwo auf der Karte)
    // INNERE KI/Manuell-Pille hat ihre EIGENE clickable, die das outer-click
    // ueberschreibt (Glance Click-Propagation: innerstes clickable gewinnt).
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(SurfaceCard))
            .cornerRadius(16.dp)
            .clickable(WidgetIntents.openTaskAction(entry.id, WidgetIntents.ACTION_FOCUS)),
    ) {
        // Zweite Layer: leichter Bucket-Tint oben drueber, damit die Karte den
        // Bucket schon visuell verraet (ersetzt das GlassCard-Tint-Gradient).
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(tintBase))
                .cornerRadius(16.dp)
                .padding(12.dp),
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                // TOP-ROW: Kategorie-Pille links | weight=1 fuer Title | Prio-Zahl rechts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryPill(category = entry.category, color = catColor)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = "${entry.priorityScore.toInt()}",
                        style = TextStyle(
                            color = ColorProvider(prioColor),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = "Prio",
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontSize = 10.sp,
                        ),
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                // TITLE
                Text(
                    text = entry.title,
                    style = TextStyle(
                        color = ColorProvider(TextPrimary),
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
                            color = ColorProvider(TextSecondary),
                            fontSize = 12.sp,
                        ),
                        maxLines = 2,
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                // SEVERITY-BAR (5 Boxen, gefuellt nach severity/2)
                SeverityBar(severity = entry.severity)
                // TAGS (max 3)
                val tagsToShow = entry.tags.take(3)
                if (tagsToShow.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row {
                        tagsToShow.forEach { tag ->
                            TagPill(tag)
                            Spacer(GlanceModifier.width(6.dp))
                        }
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                // BOTTOM-ROW: Bucket-Pill (KI/Manuell) — eigene clickable
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(GlanceModifier.defaultWeight())
                    BucketStatusPill(
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
}

@Composable
private fun CategoryPill(category: EntropyCategory, color: Color) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(color.copy(alpha = 0.20f)))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = categoryLabel(category),
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun TagPill(tag: String) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(SurfaceMuted))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp),
        )
    }
}

/**
 * Severity-Bar als 5 Boxen — Ersatz fuer die Canvas-basierte
 * SeverityRainbowBar aus TasksScreen.kt. severity 1-10 wird auf 1-5 Boxen
 * gemappt (severity/2 aufgerundet). Farben gleich wie im Original:
 *  1 = blau, 2 = gruen, 3 = gelb, 4 = orange, 5 = rot.
 */
@Composable
private fun SeverityBar(severity: Int) {
    val filledCount = ((severity + 1) / 2).coerceIn(0, 5)
    val segmentColors = listOf(PrioBlue, PrioGreen, PrioYellow, PrioOrange, PrioRed)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (i in 0 until 5) {
            val isFilled = i < filledCount
            val segColor = if (isFilled) segmentColors[i] else SurfaceMuted
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(6.dp)
                    .background(ColorProvider(segColor))
                    .cornerRadius(3.dp),
            ) {}
            if (i < 4) Spacer(GlanceModifier.width(3.dp))
        }
    }
}

@Composable
private fun BucketStatusPill(
    bucket: TimeBucket,
    accent: Color,
    isManual: Boolean,
    onClickAction: androidx.glance.action.Action,
) {
    val bgColor = if (isManual) accent.copy(alpha = 0.22f) else SurfaceMuted
    val textColor = if (isManual) accent else TextSecondary
    val label = if (isManual) "manuell" else "KI"
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(bgColor))
            .cornerRadius(50.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(onClickAction),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 11.sp,
                fontWeight = if (isManual) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Keine offenen Aufgaben.\nAntippen, um die App zu oeffnen.",
            style = TextStyle(
                color = ColorProvider(TextSecondary),
                fontSize = 14.sp,
            ),
        )
    }
}

class EntropyReducerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntropyReducerWidget()
}
