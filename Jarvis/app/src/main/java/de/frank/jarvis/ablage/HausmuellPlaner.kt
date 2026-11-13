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
 * Hausmüll-Wecker: Einmal pro Woche (montags, sonst beim ersten Lauf danach) liest Jarvis die Hausmüll-Termine aus dem Kalender und legt
 * für jeden am Vortag eine Erinnerung in Weckerform an, die vorgelesen wird und ausgeschaltet werden muss. Uhrzeit nach dem Dienst am Vortag:
 * Nachtschicht 16:00 (Tonne auf dem Weg zur Arbeit mit raus), Tagschicht 17:40 (beim Heimkommen), sonst 16:50. X, F, U und K zählen als frei.
 * Schon angelegte Hausmüll-Erinnerungen werden angepasst statt verdoppelt; Bestätigte und Erledigte bleiben unberührt.
 * Nur das Handy plant: Tablet und PC bekommen die Erinnerungen über den Abgleich, so klingelt nichts doppelt.
 */
object HausmuellPlaner {
    private const val PREFS = "hausmuell_planer"
    /** Neue Regeln (Uhrzeiten, Texte) gelten sofort: Ein anderer Stand löst einen Lauf aus, auch mitten in der Woche. */
    private const val REGEL = "2"
    private val NACHT = LocalTime.of(16, 0)
    private val TAG = LocalTime.of(17, 40)
    private val FREI = LocalTime.of(16, 50)
    private val UHR = DateTimeFormatter.ofPattern("HH:mm")

    /** Aus der Minutenrunde des Wächters; kostet nichts, solange die Woche schon geprüft ist. */
    @Synchronized
    fun pruefen(context: Context) {
        val app = context.applicationContext
        if (de.frank.jarvis.data.Geraet.istTablet) return
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val heute = LocalDate.now()
        val montag = heute.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val zuletzt = p.getString("zuletzt", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (p.getString("regel", null) == REGEL && zuletzt != null && !zuletzt.isBefore(montag)) return
        if (Erinnerungen.defekt(app)) return
        val kalender = KalenderFaehigkeit(app)
        val tage = kalender.hausmuellTage(heute, heute.plusDays(400)) ?: return
        val schichten = kalender.schichten(heute, 402).orEmpty()
        val jetzt = LocalDateTime.now()
        val quittungen = Erinnerungen.quittungen(app)
        var neu = 0
        var angepasst = 0
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
            val alt = Erinnerungen.alle(app).firstOrNull { e ->
                !e.allgemein && e.datum == vortag.toString() && listOf(e.text, e.kurz, e.zeile).any { it.contains("Mülltonne", true) || it.contains("Hausmüll", true) }
            }
            if (alt == null) {
                if (Erinnerungen.anlegen(app, kurz, ZEILE, kurz, vortag.toString(), uhrzeit, Erinnerung.FORM_WECKER, emptyList(), uhrzeit) != null) neu++
                continue
            }
            // Erledigt oder schon ausgeschaltet: Frank hat sie gesehen, nichts mehr daran drehen.
            if (alt.erledigt != 0L || alt.termine().any { it.quittung in quittungen }) continue
            if (alt.uhrzeit == uhrzeit && alt.kurz == kurz && alt.wecker && alt.ausloesungen.size == 1 && alt.ausloesungen[0].minuten == 0 && alt.ausloesungen[0].uhrzeit == null) continue
            if (Erinnerungen.aendere(app, alt.id) { it.copy(text = kurz, kurz = kurz, zeile = ZEILE, uhrzeit = uhrzeit, form = Erinnerung.FORM_WECKER, ausloesungen = listOf(Erinnerungen.neueAusloesung(0))) } != null) angepasst++
        }
        p.edit().putString("zuletzt", heute.toString()).putString("regel", REGEL).apply()
        Logbuch.system("Hausmüll-Erinnerungen geprüft", "${tage.size} Abholtermine, $neu neu, $angepasst angepasst")
    }

    private const val ZEILE = "Mülltonne rausstellen"
}
