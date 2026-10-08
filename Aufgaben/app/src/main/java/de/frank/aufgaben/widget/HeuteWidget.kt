package de.frank.aufgaben.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import android.widget.RemoteViews
import androidx.glance.appwidget.AndroidRemoteViews

/**
 * Heute-Widget: ganztägige Aufgaben oben, darunter der Zeitstrahl von 5 bis 22 Uhr mit den Terminen
 * an ihrer Stunde. Scrollbar. Datumsbasiert — um 0 Uhr rücken die Morgen-Aufgaben automatisch nach.
 * Eine rote Linie zeigt die aktuelle Zeit (nie mitten durch einen Termin, siehe [jetztLinie]); alles davor
 * ist ausgegraut, alles danach frisch.
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
        runCatching { planeSprung(context, naechsterSprung(termine, jetzt)) }
        provideContent { Inhalt(context, heute, ganztags, termine, jetzt) }
    }

    @Composable
    private fun Inhalt(context: Context, heute: Long, ganztags: List<Aufgabe>, termine: List<Aufgabe>, jetzt: Int) {
        val text = ColorProvider(Color(0xFF2A1A0E), Color(0xFFF7F1EC))
        val leise = ColorProvider(Color(0xFF7A6252), Color(0xFFB9A89A))
        val akzent = ColorProvider(Color(0xFFEE6A0C), Color(0xFFFF8A1F))
        val f = Farben(text, leise, akzent, vorbei = ColorProvider(Color(0xFFADA097), Color(0xFF6E635C)), rot = ColorProvider(Color(0xFFE53935), Color(0xFFFF5252)))
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
                        items(ganztags, itemId = { it.id }) { a -> Zeile(context, a, f, heute, zeigeZeit = false, vorbei = false) }
                        item(itemId = -2L) { Spacer(GlanceModifier.height(6.dp)) }
                    }
                    val e = Einstellungen.get(context)
                    val vonH = e.zeitleisteVon / 60
                    val bisH = (e.zeitleisteBis / 60).coerceIn(vonH, 23)
                    val linie = jetztLinie(termine, jetzt, vonH, bisH)
                    items((vonH..bisH).toList(), itemId = { -1000L - it }) { stunde ->
                        val hier = termine.filter { t -> stundeVon(t, vonH, bisH) == stunde }
                        Stunde(context, stunde, hier, jetzt, linie, f, heute)
                    }
                }
            }
        }
    }

    /** Eine Stunde im Zeitstrahl. In der Stunde der Jetzt-Linie wird sie geteilt: Vergangenes, Linie, Kommendes. */
    @Composable
    private fun Stunde(context: Context, stunde: Int, termine: List<Aufgabe>, jetzt: Int, linie: JetztLinie, f: Farben, heute: Long) {
        val beschriftung = "%02d:00".format(stunde)
        val istJetzt = jetzt / 60 == stunde
        if (linie.stunde != stunde) {
            Abschnitt(context, beschriftung, termine, istJetzt, vorbei = stunde < linie.stunde, leer = 20.dp, f, heute)
            return
        }
        val davor = termine.take(linie.davor)
        val danach = termine.drop(linie.davor)
        // Steht die Linie vor dem ersten Termin, der schon begonnen hat (oder vor Beginn des Zeitstrahls),
        // kommt sie über die Stundenzeile; sonst darunter, an der Stelle der aktuellen Uhrzeit.
        val oben = davor.isEmpty() && (jetzt < stunde * 60 || (danach.firstOrNull()?.minuten ?: Int.MAX_VALUE) <= jetzt)
        Column(GlanceModifier.fillMaxWidth()) {
            if (oben) {
                JetztStrich(context, f)
                Abschnitt(context, beschriftung, danach, istJetzt, vorbei = false, leer = 20.dp, f, heute)
            } else {
                Abschnitt(context, beschriftung, davor, istJetzt, vorbei = true, leer = 12.dp, f, heute)
                JetztStrich(context, f)
                Abschnitt(context, null, danach, istJetzt, vorbei = false, leer = 8.dp, f, heute)
            }
        }
    }

    @Composable
    private fun Abschnitt(context: Context, beschriftung: String?, termine: List<Aufgabe>, istJetzt: Boolean, vorbei: Boolean, leer: Dp, f: Farben, heute: Long) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                beschriftung.orEmpty(),
                style = TextStyle(
                    color = if (istJetzt) f.akzent else if (vorbei) f.vorbei else f.leise,
                    fontSize = 11.sp, fontWeight = if (istJetzt) FontWeight.Bold else FontWeight.Normal,
                ),
                modifier = GlanceModifier.width(40.dp).padding(top = 1.dp),
            )
            Box(GlanceModifier.width(12.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Image(ImageProvider(if (vorbei) R.drawable.widget_linie_vorbei else R.drawable.widget_linie), null, GlanceModifier.width(3.dp).fillMaxHeight(), contentScale = ContentScale.FillBounds)
                if (beschriftung != null) Image(ImageProvider(if (vorbei) R.drawable.widget_strich_vorbei else R.drawable.widget_strich), null, GlanceModifier.size(7.dp))
            }
            Column(GlanceModifier.defaultWeight().padding(start = 4.dp)) {
                if (termine.isEmpty()) Spacer(GlanceModifier.height(leer))
                termine.forEach { a -> Zeile(context, a, f, heute, zeigeZeit = true, vorbei = vorbei) }
            }
        }
    }

    /** Die rote Jetzt-Linie quer über das ganze Widget. */
    @Composable
    private fun JetztStrich(context: Context, f: Farben) {
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("jetzt", style = TextStyle(color = f.rot, fontSize = 10.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(40.dp))
            Box(GlanceModifier.width(12.dp).height(12.dp), contentAlignment = Alignment.Center) {
                Image(ImageProvider(R.drawable.widget_linie), null, GlanceModifier.width(3.dp).fillMaxHeight(), contentScale = ContentScale.FillBounds)
                Image(ImageProvider(R.drawable.widget_jetzt), null, GlanceModifier.size(10.dp))
            }
            Image(ImageProvider(R.drawable.widget_jetzt_linie), null, GlanceModifier.defaultWeight().height(2.dp), contentScale = ContentScale.FillBounds)
            // Mittig auf der Linie die aktuelle Uhrzeit, minutengenau: TextClock läuft von selbst weiter.
            AndroidRemoteViews(RemoteViews(context.packageName, R.layout.widget_uhrzeit))
            Image(ImageProvider(R.drawable.widget_jetzt_linie), null, GlanceModifier.defaultWeight().height(2.dp), contentScale = ContentScale.FillBounds)
        }
    }

    @Composable
    private fun Zeile(context: Context, a: Aufgabe, f: Farben, heute: Long, zeigeZeit: Boolean, vorbei: Boolean) {
        val punkt = when {
            vorbei -> R.drawable.punkt_vorbei
            a.prio == Prioritaet.HOCH -> R.drawable.punkt_hoch
            a.prio == Prioritaet.MITTEL -> R.drawable.punkt_mittel
            else -> R.drawable.punkt_gering
        }
        val haken = if (vorbei) R.drawable.ic_widget_haken_vorbei else R.drawable.ic_widget_haken
        Column(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Row(
                GlanceModifier.fillMaxWidth().background(ImageProvider(if (vorbei) R.drawable.widget_zeile_vorbei else R.drawable.widget_zeile)).cornerRadius(12.dp)
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
                Image(ImageProvider(if (a.erledigt) haken else punkt), null, GlanceModifier.size(11.dp))
                Spacer(GlanceModifier.width(8.dp))
                if (zeigeZeit && a.minuten != null) {
                    Text(Tage.zeit(a.minuten), style = TextStyle(color = if (vorbei) f.vorbei else f.leise, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    Spacer(GlanceModifier.width(6.dp))
                }
                Text(
                    a.titel + if ((a.tag ?: heute) < heute) "  · überfällig" else "",
                    maxLines = 1,
                    style = TextStyle(
                        color = when { vorbei -> f.vorbei; a.erledigt -> f.leise; else -> f.text }, fontSize = 13.sp,
                        textDecoration = if (a.erledigt) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
            }
        }
    }

    companion object {
        private const val SPRUNG_CODE = 4711

        /** Weckt das Widget, wenn die Jetzt-Linie das nächste Mal weiterspringt (ungenau, ohne Aufwecken). Die Minute
         *  wird als Uhrzeit gerechnet, nicht als vergangene Zeit ab Mitternacht (Sommerzeit-Umstellung). */
        private fun planeSprung(context: Context, minute: Int) {
            val app = context.applicationContext
            val zeit = LocalDate.now().atStartOfDay().plusMinutes(minute.toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1_000
            app.getSystemService(AlarmManager::class.java).setWindow(AlarmManager.RTC, zeit, 60_000, sprung(app))
        }

        private fun sprung(context: Context): PendingIntent {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, HeuteWidgetEmpfaenger::class.java))
            val intent = Intent(context, HeuteWidgetEmpfaenger::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            return PendingIntent.getBroadcast(context, SPRUNG_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun stoppeSprung(context: Context) {
            val app = context.applicationContext
            runCatching { app.getSystemService(AlarmManager::class.java).cancel(sprung(app)) }
        }

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

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        HeuteWidget.stoppeSprung(context)
    }
}

internal data class Farben(val text: Farbe, val leise: Farbe, val akzent: Farbe, val vorbei: Farbe, val rot: Farbe)
