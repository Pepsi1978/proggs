package de.frank.jarvis.listen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.observability.Logbuch

/**
 * Listen-Widget für den Startbildschirm: Kopfzeile mit Listenname (antippen = Liste wählen, direkt im Widget), Pfeilen zum Blättern
 * und „+“ (öffnet die Liste in Jarvis zum Eintippen). Darunter die Einträge wie in der App, Unterpunkte eingerückt; ein Tipp hakt ab
 * oder öffnet wieder. Jede Änderung in Jarvis, per Sprache oder vom Tablet ruft [ListenWidget.aktualisieren] auf.
 *
 * Bewusst ohne Glance und ohne RemoteViewsService: [RemoteViews.RemoteCollectionItems] (ab Android 12) liefert die Zeilen gleich mit,
 * so steht eine Änderung ohne Umweg über eine Factory sofort im Widget. Welche Liste ein Widget zeigt, merkt es sich je appWidgetId.
 */
class ListenWidgetEmpfaenger : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { ListenWidget.zeichne(context, manager, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val p = ListenWidget.prefs(context).edit()
        ids.forEach { p.remove("liste_$it").remove("wahl_$it") }
        p.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ListenWidget.AKTION) { super.onReceive(context, intent); return }
        val widget = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val app = context.applicationContext
        val p = ListenWidget.prefs(app)
        runCatching {
            when (intent.getStringExtra(ListenWidget.WAS)) {
                "abhaken" -> {
                    val id = intent.getLongExtra(ListenWidget.EINTRAG, 0L)
                    val e = Listen.sicher(app).eintraege.firstOrNull { it.id == id } ?: return@runCatching
                    Listen.abhaken(app, id, !e.erledigt)   // aktualisiert alle Widgets selbst
                    return
                }
                "wahl" -> p.edit().putBoolean("wahl_$widget", !p.getBoolean("wahl_$widget", false)).apply()
                "liste" -> p.edit().putLong("liste_$widget", intent.getLongExtra(ListenWidget.LISTE, 0L)).putBoolean("wahl_$widget", false).apply()
                "vor", "zurueck" -> {
                    val listen = Listen.sicher(app).listen
                    if (listen.isNotEmpty()) {
                        val jetzt = listen.indexOfFirst { it.id == ListenWidget.listeVon(app, widget, listen)?.id }.coerceAtLeast(0)
                        val neu = (jetzt + if (intent.getStringExtra(ListenWidget.WAS) == "vor") 1 else -1 + listen.size) % listen.size
                        p.edit().putLong("liste_$widget", listen[neu].id).putBoolean("wahl_$widget", false).apply()
                    }
                }
            }
        }.onFailure { Logbuch.fehler("Listen-Widget", it) }
        if (widget != AppWidgetManager.INVALID_APPWIDGET_ID) ListenWidget.zeichne(app, AppWidgetManager.getInstance(app), widget)
    }
}

object ListenWidget {
    const val AKTION = "de.frank.jarvis.LISTEN_WIDGET"
    const val WAS = "was"
    const val EINTRAG = "eintrag"
    const val LISTE = "liste"
    /** Für MainActivity: diese Liste öffnen (Long); mit [EXTRA_NEU] gleich zum Eintippen. */
    const val EXTRA_OEFFNEN = "de.frank.jarvis.LISTE_OEFFNEN"
    const val EXTRA_NEU = "de.frank.jarvis.LISTE_NEU"

    private val TEXT = Color.parseColor("#EAF4FC")
    private val LEISE = Color.parseColor("#A9BED6")
    private val SCHWACH = Color.parseColor("#6D819C")
    private val ERFOLG = Color.parseColor("#4FE0A0")
    /** Farben der Prioritäten wie in der App: keine, niedrig (blau), mittel (gelb), hoch (rot). */
    val PRIO = intArrayOf(Color.parseColor("#A9BED6"), Color.parseColor("#4FC8FF"), Color.parseColor("#FFB547"), Color.parseColor("#FF6B6B"))

    fun prefs(context: Context) = context.applicationContext.getSharedPreferences("listen_widget", Context.MODE_PRIVATE)

    fun listeVon(context: Context, widget: Int, listen: List<Liste>): Liste? {
        val id = prefs(context).getLong("liste_$widget", 0L)
        return listen.firstOrNull { it.id == id } ?: listen.firstOrNull { it.art == ListenArt.EINKAUF.kennung } ?: listen.firstOrNull()
    }

    /** Alle Listen-Widgets neu zeichnen; nach jeder Änderung an den Listen. */
    fun aktualisieren(context: Context) {
        runCatching {
            val app = context.applicationContext
            val m = AppWidgetManager.getInstance(app) ?: return
            m.getAppWidgetIds(ComponentName(app, ListenWidgetEmpfaenger::class.java)).forEach { zeichne(app, m, it) }
        }.onFailure { Logbuch.fehler("Listen-Widget", it) }
    }

    private fun aktion(context: Context, widget: Int, was: String, code: Int, extra: (Intent) -> Unit = {}): PendingIntent {
        val i = Intent(context, ListenWidgetEmpfaenger::class.java).setAction(AKTION).putExtra(WAS, was)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget).also(extra)
        return PendingIntent.getBroadcast(context, widget * 100 + code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun oeffnen(context: Context, widget: Int, liste: Long?, neu: Boolean): PendingIntent {
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OEFFNEN, liste ?: 0L).putExtra(EXTRA_NEU, neu)
        return PendingIntent.getActivity(context, widget * 100 + if (neu) 7 else 8, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun zeichne(context: Context, m: AppWidgetManager, widget: Int) {
        val app = context.applicationContext
        val stand = Listen.sicher(app)
        val wahl = prefs(app).getBoolean("wahl_$widget", false) || stand.listen.isEmpty()
        val liste = listeVon(app, widget, stand.listen)
        val v = RemoteViews(app.packageName, R.layout.widget_listen)
        if (wahl || liste == null) {
            v.setTextViewText(R.id.widget_titel, if (stand.listen.isEmpty()) "Listen" else "Liste wählen")
            v.setTextViewText(R.id.widget_unter, if (stand.listen.isEmpty()) "noch keine Liste" else "${stand.listen.size} Listen · antippen zum Zurück")
        } else {
            v.setTextViewText(R.id.widget_titel, "${ListenArt.von(liste.art).symbol} ${liste.name}  ▾")
            v.setTextViewText(R.id.widget_unter, "${stand.offen(liste.id)} offen")
        }
        v.setOnClickPendingIntent(R.id.widget_kopf, aktion(app, widget, "wahl", 1))
        v.setOnClickPendingIntent(R.id.widget_zurueck, aktion(app, widget, "zurueck", 2))
        v.setOnClickPendingIntent(R.id.widget_vor, aktion(app, widget, "vor", 3))
        v.setOnClickPendingIntent(R.id.widget_neu, oeffnen(app, widget, liste?.id, neu = true))
        v.setOnClickPendingIntent(R.id.widget_leer, oeffnen(app, widget, liste?.id, neu = false))

        if (Build.VERSION.SDK_INT >= 31) {
            // Vorlage für alle Zeilen: Broadcast an diesen Empfänger, die Zeile ergänzt, was sie meint. Ab Android 14 muss die Vorlage
            // ausdrücklich adressiert und veränderlich sein, sonst bleibt der Tipp stumm.
            val vorlage = Intent(app, ListenWidgetEmpfaenger::class.java).setAction(AKTION).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget)
            v.setPendingIntentTemplate(R.id.widget_liste, PendingIntent.getBroadcast(app, widget * 100 + 9, vorlage, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
            val b = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(3)
            if (wahl || liste == null) {
                stand.listen.forEach { l ->
                    val z = RemoteViews(app.packageName, R.layout.widget_listen_wahl)
                    z.setTextViewText(R.id.wahl_name, "${ListenArt.von(l.art).symbol}  ${l.name}")
                    z.setTextViewText(R.id.wahl_zahl, stand.offen(l.id).toString())
                    z.setTextColor(R.id.wahl_name, if (l.id == liste?.id) PRIO[1] else TEXT)
                    z.setOnClickFillInIntent(R.id.wahl_zeile, Intent().putExtra(WAS, "liste").putExtra(LISTE, l.id))
                    b.addItem(l.id, z)
                }
                v.setTextViewText(R.id.widget_leer, "Noch keine Liste. Tippen, um Jarvis zu öffnen.")
            } else {
                Listen.ansicht(stand, liste).forEachIndexed { i, z ->
                    val e = z.eintrag
                    if (e == null) {
                        val k = RemoteViews(app.packageName, R.layout.widget_listen_kopf)
                        k.setTextViewText(R.id.kopf_text, "${z.kopf}  ${z.kopfAnzahl}")
                        b.addItem(-1L - i, k)
                    } else {
                        val r = RemoteViews(app.packageName, R.layout.widget_listen_zeile)
                        val titel = SpannableString(e.titel).apply { if (e.erledigt) setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                        r.setTextViewText(R.id.zeile_titel, titel)
                        r.setTextColor(R.id.zeile_titel, if (e.erledigt) SCHWACH else TEXT)
                        r.setImageViewResource(R.id.zeile_haken, if (e.erledigt) R.drawable.widget_haken_an else R.drawable.widget_haken_aus)
                        r.setInt(R.id.zeile_haken, "setColorFilter", if (e.erledigt) ERFOLG else PRIO[e.prioritaet.coerceIn(0, 3)])
                        val info = buildList {
                            if (z.kinderGesamt > 0) add("${z.kinderErledigt}/${z.kinderGesamt}")
                            if (e.faellig.length >= 10) add(e.faellig.substring(8, 10) + "." + e.faellig.substring(5, 7) + ".")
                            if (e.angeheftet) add("📌")
                        }.joinToString("  ")
                        r.setTextViewText(R.id.zeile_info, info)
                        r.setTextColor(R.id.zeile_info, LEISE)
                        val dp = app.resources.displayMetrics.density
                        r.setViewPadding(R.id.zeile, ((10 + 22 * z.tiefe) * dp).toInt(), (7 * dp).toInt(), (10 * dp).toInt(), (7 * dp).toInt())
                        r.setOnClickFillInIntent(R.id.zeile, Intent().putExtra(WAS, "abhaken").putExtra(EINTRAG, e.id))
                        b.addItem(e.id, r)
                    }
                }
                v.setTextViewText(R.id.widget_leer, "Leer. Mit + etwas eintragen oder Jarvis sagen.")
            }
            v.setRemoteAdapter(R.id.widget_liste, b.build())
            v.setEmptyView(R.id.widget_liste, R.id.widget_leer)
        } else {
            v.setTextViewText(R.id.widget_leer, "Das Listen-Widget braucht Android 12. Tippen öffnet die Liste in Jarvis.")
        }
        m.updateAppWidget(widget, v)
    }
}
