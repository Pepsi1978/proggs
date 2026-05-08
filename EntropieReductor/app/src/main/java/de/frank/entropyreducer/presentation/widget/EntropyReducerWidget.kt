package de.frank.entropyreducer.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.R
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Home-Screen-Widget (Spec §18, B.7).
 * Layout (4×2 default, resizable 2×2 bis 4×3):
 *   - Oben: Status-Pille (Prozent offene vs erledigte Eintraege)
 *   - Mitte: Top-1-Aufgabe (hoechste Prio)
 *   - Unten: großer Mic-Button -> oeffnet App
 *
 * Update: alle 15 Min via PeriodicWorkRequest des Systems oder manuell über
 * `EntropyReducerWidget().updateAll(context)` nach Datenaenderungen.
 */
class EntropyReducerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors
            .fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            ).entryDao()
        val active = dao.getActive().first()
        val total = active.size + dao.countByStatus(EntryStatus.REDUZIERT).first()
        val open = active.size
        val percentDone = if (total > 0) ((total - open) * 100) / total else 0
        val top = active.maxByOrNull { it.priorityScore }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    percentDone = percentDone,
                    open = open,
                    topEntry = top,
                )
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun entryDao(): EntropyEntryDao
    }
}

@Composable
private fun WidgetContent(
    percentDone: Int,
    open: Int,
    topEntry: EntropyEntryEntity?,
) {
    val accent = Color(0xFFB388FF)
    val bg = Color(0xFF0E0B24)
    val border = Color(0xFF2A1F44)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(ColorProvider(bg))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Entropie",
                style = TextStyle(color = ColorProvider(accent), fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "$percentDone% reduziert",
                style = TextStyle(color = ColorProvider(Color.White)),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .background(ColorProvider(border))
                .cornerRadius(4.dp),
        ) {
            val ratio = (percentDone / 100f).coerceIn(0f, 1f)
            Box(
                modifier = GlanceModifier
                    .height(8.dp)
                    .width((ratio * 200).dp)
                    .background(ColorProvider(accent))
                    .cornerRadius(4.dp),
            ) { }
        }
        Spacer(GlanceModifier.height(12.dp))
        Text(
            text = topEntry?.title ?: if (open == 0) "Alles erledigt." else "Keine Top-Aufgabe.",
            style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Medium),
            maxLines = 2,
        )
        topEntry?.let {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = "${it.category} • Prio ${it.priorityScore.toInt()}",
                style = TextStyle(color = ColorProvider(Color.LightGray)),
            )
        }
        Spacer(GlanceModifier.height(12.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(48.dp)
                .background(ColorProvider(accent))
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_btn_speak_now),
                    contentDescription = "Aufnahme starten",
                    modifier = GlanceModifier.size(20.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    "Aufnahme",
                    style = TextStyle(color = ColorProvider(Color.Black), fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

class EntropyReducerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntropyReducerWidget()
}
