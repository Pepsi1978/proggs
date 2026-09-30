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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import androidx.glance.unit.ColorProvider as Farbe
import de.frank.aufgaben.MainActivity
import de.frank.aufgaben.R
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage
import java.time.LocalTime

/**
 * Heute-Widget: ganztägige Aufgaben oben, darunter der Zeitstrahl von 5 bis 22 Uhr mit den Terminen
 * an ihrer Stunde. Scrollbar. Datumsbasiert — um 0 Uhr rücken die Morgen-Aufgaben automatisch nach.
 */
class HeuteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp), DpSize(260.dp, 200.dp), DpSize(320.dp, 320.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val heute = Tage.heute()
        val liste = AufgabenRepository.get(context).fuerTag(heute, true)
            .filter { !it.erledigt || it.tag == heute }
        val ganztags = liste.filter { it.minuten == null || (it.tag ?: heute) < heute }
            .sortedWith(compareBy<Aufgabe>({ it.erledigt }, { it.prio.rang }))
        val termine = liste.filter { it.minuten != null && it.tag == heute }.sortedBy { it.minuten }
        val jetzt = LocalTime.now().let { it.hour * 60 + it.minute }
        provideContent { Inhalt(context, heute, ganztags, termine, jetzt) }
    }

    @Composable
    private fun Inhalt(context: Context, heute: Long, ganztags: List<Aufgabe>, termine: List<Aufgabe>, jetzt: Int) {
        val text = ColorProvider(Color(0xFF2A1A0E), Color(0xFFF7F1EC))
        val leise = ColorProvider(Color(0xFF7A6252), Color(0xFFB9A89A))
        val akzent = ColorProvider(Color(0xFFEE6A0C), Color(0xFFFF8A1F))
        val klein = LocalSize.current.height < 150.dp
        val alle = ganztags + termine
        val fertig = alle.count { it.erledigt }
        val oeffnen = actionStartActivity(Intent(context, MainActivity::class.java))
        Box(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_hintergrund)).cornerRadius(26.dp)) {
            Column(GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(GlanceModifier.fillMaxWidth().clickable(oeffnen), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.ic_widget_sonne), null, GlanceModifier.size(20.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text("Heute", style = TextStyle(color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                        if (!klein) Text(Tage.langesDatum(heute), style = TextStyle(color = leise, fontSize = 11.sp))
                    }
                    if (alle.isNotEmpty()) Text("$fertig/${alle.size}", style = TextStyle(color = akzent, fontSize = 13.sp, fontWeight = FontWeight.Bold))
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
                Spacer(GlanceModifier.height(6.dp))
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    if (ganztags.isNotEmpty()) {
                        item(itemId = -1L) { Text("GANZTÄGIG", style = TextStyle(color = leise, fontSize = 10.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.padding(start = 4.dp, bottom = 2.dp)) }
                        items(ganztags, itemId = { it.id }) { a -> Zeile(context, a, text, leise, heute, zeigeZeit = false) }
                        item(itemId = -2L) { Spacer(GlanceModifier.height(6.dp)) }
                    }
                    val e = Einstellungen.get(context)
                    val vonH = e.zeitleisteVon / 60
                    val bisH = (e.zeitleisteBis / 60).coerceIn(vonH, 23)
                    items((vonH..bisH).toList(), itemId = { -1000L - it }) { stunde ->
                        val hier = termine.filter { t -> ((t.minuten ?: 0) / 60).coerceIn(vonH, bisH) == stunde }
                        Stunde(context, stunde, hier, jetzt / 60 == stunde, text, leise, akzent, heute)
                    }
                }
            }
        }
    }

    @Composable
    private fun Stunde(context: Context, stunde: Int, termine: List<Aufgabe>, istJetzt: Boolean, text: Farbe, leise: Farbe, akzent: Farbe, heute: Long) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                "%02d:00".format(stunde),
                style = TextStyle(color = if (istJetzt) akzent else leise, fontSize = 11.sp, fontWeight = if (istJetzt) FontWeight.Bold else FontWeight.Normal),
                modifier = GlanceModifier.width(40.dp).padding(top = 1.dp),
            )
            Box(GlanceModifier.width(12.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Image(ImageProvider(R.drawable.widget_linie), null, GlanceModifier.width(3.dp).fillMaxHeight(), contentScale = ContentScale.FillBounds)
                Image(ImageProvider(if (istJetzt) R.drawable.widget_jetzt else R.drawable.widget_strich), null, GlanceModifier.size(if (istJetzt) 10.dp else 7.dp))
            }
            Column(GlanceModifier.defaultWeight().padding(start = 4.dp)) {
                if (termine.isEmpty()) Spacer(GlanceModifier.height(20.dp))
                termine.forEach { a -> Zeile(context, a, text, leise, heute, zeigeZeit = true) }
            }
        }
    }

    @Composable
    private fun Zeile(context: Context, a: Aufgabe, text: Farbe, leise: Farbe, heute: Long, zeigeZeit: Boolean) {
        val punkt = when (a.prio) {
            Prioritaet.HOCH -> R.drawable.punkt_hoch
            Prioritaet.MITTEL -> R.drawable.punkt_mittel
            else -> R.drawable.punkt_gering
        }
        Column(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Row(
                GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_zeile)).cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_AUFGABE, a.id)
                                .setAction("aufgabe_${a.id}")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        ),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(if (a.erledigt) R.drawable.ic_widget_haken else punkt), null, GlanceModifier.size(11.dp))
                Spacer(GlanceModifier.width(8.dp))
                if (zeigeZeit && a.minuten != null) {
                    Text(Tage.zeit(a.minuten), style = TextStyle(color = leise, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    Spacer(GlanceModifier.width(6.dp))
                }
                Text(
                    a.titel + if ((a.tag ?: heute) < heute) "  · überfällig" else "",
                    maxLines = 1,
                    style = TextStyle(
                        color = if (a.erledigt) leise else text, fontSize = 13.sp,
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
