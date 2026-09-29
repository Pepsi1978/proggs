package de.frank.aufgaben.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as Farbe
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import de.frank.aufgaben.MainActivity
import de.frank.aufgaben.R
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage

/**
 * Heute-Widget: zeigt die Aufgaben des aktuellen Tages. Die Liste ist datumsbasiert — um 0 Uhr
 * (Mitternachts-Alarm plus DATE_CHANGED) werden die bisherigen Morgen-Aufgaben automatisch zu Heute.
 */
class HeuteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp), DpSize(260.dp, 200.dp), DpSize(320.dp, 320.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val heute = Tage.heute()
        val liste = AufgabenRepository.get(context).fuerTag(heute, true)
            .filter { !it.erledigt || it.tag == heute }
        val offen = liste.filter { !it.erledigt }.sortedWith(
            compareBy<Aufgabe>({ it.minuten == null }, { it.minuten ?: 0 }, { it.prio.rang }),
        )
        val erledigt = liste.filter { it.erledigt }
        provideContent { Inhalt(context, heute, offen, erledigt) }
    }

    @Composable
    private fun Inhalt(context: Context, heute: Long, offen: List<Aufgabe>, erledigt: List<Aufgabe>) {
        val text = ColorProvider(Color(0xFF1E1B3A), Color(0xFFF1EEFF))
        val leise = ColorProvider(Color(0xFF5E5A7E), Color(0xFFB9B3E0))
        val akzent = ColorProvider(Color(0xFF6C4DF6), Color(0xFFB7A6FF))
        val klein = LocalSize.current.height < 150.dp
        val gesamt = offen.size + erledigt.size
        Box(
            GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_hintergrund)).cornerRadius(26.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        ) {
            Column(GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.ic_widget_sonne), null, GlanceModifier.size(22.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text("Heute", style = TextStyle(color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                        if (!klein) Text(Tage.langesDatum(heute), style = TextStyle(color = leise, fontSize = 11.sp))
                    }
                    Text(
                        if (gesamt == 0) "" else "${erledigt.size}/$gesamt",
                        style = TextStyle(color = akzent, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Image(
                        ImageProvider(R.drawable.ic_widget_plus), "Neue Aufgabe",
                        GlanceModifier.size(30.dp).clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_NEU, true)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            ),
                        ),
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                if (offen.isEmpty()) {
                    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (gesamt > 0) "Alles erledigt – stark!" else "Nichts geplant. Zeit zum Durchatmen.",
                            style = TextStyle(color = leise, fontSize = 13.sp),
                        )
                    }
                } else {
                    LazyColumn(GlanceModifier.fillMaxSize()) {
                        items(offen, itemId = { it.id }) { a -> Zeile(context, a, text, leise, heute) }
                        items(erledigt, itemId = { -it.id - 1 }) { a -> Zeile(context, a, text, leise, heute) }
                    }
                }
            }
        }
    }

    @Composable
    private fun Zeile(context: Context, a: Aufgabe, text: Farbe, leise: Farbe, heute: Long) {
        val punkt = when (a.prio) {
            Prioritaet.HOCH -> R.drawable.punkt_hoch
            Prioritaet.MITTEL -> R.drawable.punkt_mittel
            else -> R.drawable.punkt_gering
        }
        Column(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(
                GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_zeile)).cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_AUFGABE, a.id)
                                .setAction("aufgabe_${a.id}")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        ),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(if (a.erledigt) R.drawable.ic_widget_haken else punkt), null, GlanceModifier.size(12.dp))
                Spacer(GlanceModifier.width(8.dp))
                if (a.minuten != null) {
                    Text(Tage.zeit(a.minuten), style = TextStyle(color = leise, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    Spacer(GlanceModifier.width(8.dp))
                }
                Text(
                    a.titel + if ((a.tag ?: heute) < heute) "  · überfällig" else "",
                    maxLines = 1,
                    style = TextStyle(
                        color = if (a.erledigt) leise else text, fontSize = 14.sp,
                        textDecoration = if (a.erledigt) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
            }
        }
    }

    companion object {
        suspend fun aktualisiere(context: Context) {
            val app = context.applicationContext
            if (GlanceAppWidgetManager(app).getGlanceIds(HeuteWidget::class.java).isNotEmpty()) {
                HeuteWidget().updateAll(app)
            }
        }
    }
}

class HeuteWidgetEmpfaenger : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HeuteWidget()
}
