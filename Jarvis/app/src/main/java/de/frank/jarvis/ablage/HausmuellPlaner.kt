package de.frank.jarvis.ablage

import android.content.Context
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.observability.Logbuch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Hausmüll-Wecker: Jeden Montag um 6 Uhr (eigener Systemwecker, auch bei gesperrtem Handy) und zusätzlich einmal täglich aus der Minutenrunde liest Jarvis
 * die Hausmüll-Termine der nächsten [VORLAUF_TAGE] Tage aus dem Kalender und legt für jeden am Vortag eine Erinnerung in Weckerform an, die vorgelesen wird
 * und ausgeschaltet werden muss. Uhrzeit nach dem Dienst am Vortag: Nachtschicht 16:00 (Tonne auf dem Weg zur Arbeit mit raus), Tagschicht 17:40
 * (beim Heimkommen), sonst 16:50. X, F, U und K zählen als frei. Der tägliche Lauf zieht spätere Dienständerungen nach.
 * Bestehende Hausmüll-Erinnerungen werden angepasst statt verdoppelt; Bestätigte und Erledigte bleiben unberührt. Eigene Wecker, deren Abholtermin
 * verschwunden oder verschoben ist, entfernt Jarvis; einen von Frank gelöschten legt es nicht wieder an.
 * Nur das Handy plant: Tablet und PC sehen die Erinnerungen über den Abgleich nur in der Liste.
 */
object HausmuellPlaner {
    private const val PREFS = "hausmuell_planer"
    /** Neue Regeln (Uhrzeiten, Texte) gelten sofort: Ein anderer Stand löst einen Lauf aus. */
    private const val REGEL = "3"
    private const val VORLAUF_TAGE = 35L
    private const val ZEILE = "Mülltonne rausstellen"
    const val AKTION = "de.frank.jarvis.HAUSMUELL_PRUEFEN"
    private val NACHT = LocalTime.of(16, 0)
    private val TAG = LocalTime.of(17, 40)
    private val FREI = LocalTime.of(16, 50)
    private val UHR = DateTimeFormatter.ofPattern("HH:mm")

    /** Unsere Erinnerung oder eine, die Jarvis per Sprache für den Hausmüll angelegt hat; nie Bio, Papier oder Gelbe Tonne. */
    private fun istHausmuell(e: Erinnerung) = !e.allgemein && (e.zeile == ZEILE || listOf(e.text, e.kurz, e.zeile).any { it.contains("Hausmüll", true) })

    /** Aus der Minutenrunde und vom Montagswecker; kostet nichts, wenn heute schon geprüft ist. [erzwingen] = Montagswecker. */
    @Synchronized
    fun pruefen(context: Context, erzwingen: Boolean = false) {
        val app = context.applicationContext
        if (de.frank.jarvis.data.Geraet.istTablet) return
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val heute = LocalDate.now()
        if (!erzwingen && p.getString("regel", null) == REGEL && p.getString("zuletzt", null) == heute.toString()) return
        if (Erinnerungen.defekt(app)) return
        val kalender = KalenderFaehigkeit(app)
        val tage = kalender.hausmuellTage(heute, heute.plusDays(VORLAUF_TAGE)) ?: return
        // Leer heißt meist: Kalender nach dem Start noch nicht synchronisiert. Dann nichts löschen und später erneut versuchen.
        if (tage.isEmpty()) { Logbuch.system("Hausmüll-Erinnerungen", "keine Abholtermine in den nächsten $VORLAUF_TAGE Tagen, später erneut"); return }
        val schichten = kalender.schichten(heute, VORLAUF_TAGE.toInt() + 1).orEmpty()
        val jetzt = LocalDateTime.now()
        val quittungen = Erinnerungen.quittungen(app)
        val vorhanden = Erinnerungen.alle(app).filter(::istHausmuell).groupBy { it.datum }
        // Datum → id der von Jarvis angelegten: Fehlt eine davon, hat Frank sie gelöscht, und sie kommt nicht wieder.
        val angelegt = p.getStringSet("angelegt", emptySet()).orEmpty().mapNotNull { s -> s.split('|').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap().toMutableMap()
        val alleIds = Erinnerungen.alle(app).map { it.id }.toSet()
        var neu = 0; var angepasst = 0; var entfernt = 0; var fehler = false
        val vortage = tage.map { it.minusDays(1) }.toSet()
        for (abholung in tage) {
            val vortag = abholung.minusDays(1)
            if (vortag.isBefore(heute)) continue
            val art = schichten[vortag]?.first
            val zeit = when (art) { "nacht" -> NACHT; "tag" -> TAG; else -> FREI }
            if (!vortag.atTime(zeit).isAfter(jetzt)) continue
            val wochentag = abholung.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN))
            val kurz = when (art) {
                "nacht" -> "Morgen, am $wochentag, wird der Hausmüll abgeholt. Stell die Mülltonne gleich mit raus, wenn du zur Nachtschicht fährst."
                "tag" -> "Morgen, am $wochentag, wird der Hausmüll abgeholt. Stell die Mülltonne raus, sobald du von der Arbeit nach Hause kommst."
                else -> "Morgen, am $wochentag, wird der Hausmüll abgeholt. Bitte jetzt die Mülltonne rausstellen."
            }
            val uhrzeit = zeit.format(UHR)
            val alt = vorhanden[vortag.toString()]?.firstOrNull()
            if (alt == null) {
                if (angelegt[vortag.toString()]?.let { it !in alleIds } == true) continue
                val e = Erinnerungen.anlegen(app, kurz, ZEILE, kurz, vortag.toString(), uhrzeit, Erinnerung.FORM_WECKER, emptyList(), uhrzeit)
                if (e != null) { neu++; angelegt[vortag.toString()] = e.id } else fehler = true
                continue
            }
            angelegt[vortag.toString()] = alt.id
            // Erledigt oder schon ausgeschaltet: Frank hat sie gesehen, nichts mehr daran drehen.
            if (alt.erledigt != 0L || alt.termine().any { it.quittung in quittungen }) continue
            if (alt.uhrzeit == uhrzeit && alt.kurz == kurz && alt.zeile == ZEILE && alt.wecker && alt.ausloesungen.size == 1 && alt.ausloesungen[0].minuten == 0 && alt.ausloesungen[0].uhrzeit == null) continue
            if (Erinnerungen.aendere(app, alt.id) { it.copy(text = kurz, kurz = kurz, zeile = ZEILE, uhrzeit = uhrzeit, form = Erinnerung.FORM_WECKER, ausloesungen = listOf(Erinnerungen.neueAusloesung(0))) } != null) angepasst++
            else fehler = true
        }
        // Abholtermin verschoben oder gestrichen: eigene, noch offene Wecker im Planungsfenster fallen weg.
        val ende = heute.plusDays(VORLAUF_TAGE - 1)
        for (e in vorhanden.values.flatten()) {
            val d = e.tag() ?: continue
            if (e.zeile != ZEILE || d in vortage || d.isBefore(heute) || d.isAfter(ende) || e.erledigt != 0L || e.termine().any { it.quittung in quittungen }) continue
            if ((e.termin() ?: 0L) <= System.currentTimeMillis()) continue
            if (Erinnerungen.loesche(app, e.id)) { entfernt++; angelegt.remove(e.datum) }
        }
        val behalten = angelegt.filterKeys { k -> runCatching { !LocalDate.parse(k).isBefore(heute) }.getOrDefault(false) }
        p.edit().putStringSet("angelegt", behalten.map { "${it.key}|${it.value}" }.toSet()).apply()
        // Nur ein vollständiger Lauf gilt als erledigt; sonst versucht es die nächste Minutenrunde erneut.
        if (!fehler) p.edit().putString("zuletzt", heute.toString()).putString("regel", REGEL).apply()
        Logbuch.system("Hausmüll-Erinnerungen geprüft", "${tage.size} Abholtermine, $neu neu, $angepasst angepasst, $entfernt entfernt" + if (fehler) ", nicht alles gespeichert" else "")
    }

    /**
     * Eigener Systemwecker für den nächsten Montag 6 Uhr: Er löst auch bei gesperrtem Handy im Ruhemodus aus und weckt dafür die App,
     * unabhängig von der Minutenrunde. Wird nach jedem Lauf und bei jedem App-Start (auch nach dem Hochfahren) neu gestellt.
     */
    fun planen(context: Context) {
        val app = context.applicationContext
        if (de.frank.jarvis.data.Geraet.istTablet) return
        runCatching {
            val jetzt = LocalDateTime.now()
            var ziel = jetzt.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(6, 0)
            if (!ziel.isAfter(jetzt)) ziel = ziel.plusWeeks(1)
            val ms = ziel.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = android.app.PendingIntent.getBroadcast(app, 19_992, android.content.Intent(app, ErinnerungEmpfaenger::class.java).setAction(AKTION),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
            val wecker = app.getSystemService(android.app.AlarmManager::class.java)
            if (android.os.Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) wecker.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, ms, pi)
            else wecker.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, ms, pi)
        }.onFailure { Logbuch.fehler("Hausmüll-Wecker stellen", it) }
    }
}
