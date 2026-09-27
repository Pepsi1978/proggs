package de.frank.newskompass.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Wie oft ein Thema automatisch recherchiert wird. */
enum class RhythmusArt(val id: String, val label: String) {
    TAEGLICH("taeglich", "Täglich"),
    ALLE_X_TAGE("tage", "Alle x Tage"),
    WOECHENTLICH("woechentlich", "Wöchentlich"),
    MONATLICH("monatlich", "Monatlich"),
    JAEHRLICH("jaehrlich", "Jährlich"),
    ;

    companion object {
        fun fromId(value: String?): RhythmusArt = entries.firstOrNull { it.id == value } ?: TAEGLICH
    }
}

/**
 * An welchen Tagen ein Thema dran ist; die Uhrzeiten stehen am [Thema].
 *
 * [intervall] zählt Tage bei [RhythmusArt.ALLE_X_TAGE] und Wochen bei [RhythmusArt.WOECHENTLICH],
 * jeweils gerechnet ab dem Tag [ab]. [wochentage] sind ISO-Nummern (1 = Montag … 7 = Sonntag).
 * Gibt es [tag] in einem Monat nicht (etwa den 31. oder den 29. Februar), gilt dessen letzter Tag.
 */
data class Rhythmus(
    val art: RhythmusArt = RhythmusArt.TAEGLICH,
    val intervall: Int = 1,
    val wochentage: Set<Int> = setOf(1),
    val tag: Int = 1,
    val monat: Int = 1,
    val ab: LocalDate = LocalDate.of(2026, 1, 1),
) {
    fun normiert(): Rhythmus = copy(
        intervall = intervall.coerceIn(1, MAX_INTERVALL),
        wochentage = wochentage.filter { it in 1..7 }.toSortedSet(),
        tag = tag.coerceIn(1, 31),
        monat = monat.coerceIn(1, 12),
    )

    /** Ist dieses Thema an [datum] dran? */
    fun trifft(datum: LocalDate): Boolean = when (art) {
        RhythmusArt.TAEGLICH -> true
        RhythmusArt.ALLE_X_TAGE -> {
            val tage = ChronoUnit.DAYS.between(ab, datum)
            tage >= 0 && tage % intervall == 0L
        }
        RhythmusArt.WOECHENTLICH -> {
            val montag = { d: LocalDate -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            val wochen = ChronoUnit.WEEKS.between(montag(ab), montag(datum))
            datum.dayOfWeek.value in wochentage && (intervall == 1 || (wochen >= 0 && wochen % intervall == 0L))
        }
        RhythmusArt.MONATLICH -> datum.dayOfMonth == minOf(tag, datum.lengthOfMonth())
        RhythmusArt.JAEHRLICH -> datum.monthValue == monat &&
            datum.dayOfMonth == minOf(tag, YearMonth.of(datum.year, monat).lengthOfMonth())
    }

    /** Ein Satz wie „Jeden Montag und Donnerstag“ oder „Jährlich am 13. Februar“. */
    fun beschreibung(): String = when (art) {
        RhythmusArt.TAEGLICH -> "Täglich"
        RhythmusArt.ALLE_X_TAGE ->
            (if (intervall == 1) "Täglich" else "Alle $intervall Tage") + " ab ${datumText(ab)}"
        RhythmusArt.WOECHENTLICH -> {
            val tage = aufzaehlung(wochentage.sorted().map(::wochentagName))
            when {
                wochentage.isEmpty() -> "Wöchentlich, aber noch ohne Wochentag"
                intervall == 1 -> "Jeden $tage"
                else -> "Alle $intervall Wochen am $tage, ab der Woche vom ${datumText(ab)}"
            }
        }
        RhythmusArt.MONATLICH ->
            "Monatlich am $tag." + if (tag > 28) " (in kürzeren Monaten am letzten Tag)" else ""
        RhythmusArt.JAEHRLICH -> "Jährlich am ${jahrestagText(tag, monat)}" +
            if (monat == 2 && tag == 29) " (sonst am 28. Februar)" else ""
    }

    fun zuJson(): JSONObject = JSONObject()
        .put("art", art.id)
        .put("intervall", intervall)
        .put("wochentage", JSONArray(wochentage.sorted()))
        .put("tag", tag)
        .put("monat", monat)
        .put("ab", ab.toString())

    companion object {
        const val MAX_INTERVALL = 365

        fun ausJson(j: JSONObject?): Rhythmus {
            if (j == null) return Rhythmus()
            val tage = j.optJSONArray("wochentage")
            return Rhythmus(
                art = RhythmusArt.fromId(j.optString("art")),
                intervall = j.optInt("intervall", 1),
                wochentage = tage?.let { t -> (0 until t.length()).map(t::getInt).toSet() } ?: setOf(1),
                tag = j.optInt("tag", 1),
                monat = j.optInt("monat", 1),
                ab = runCatching { LocalDate.parse(j.optString("ab")) }.getOrDefault(LocalDate.of(2026, 1, 1)),
            ).normiert()
        }

        /** Sinnvolle Vorbelegung beim Wechsel der Art: alles hängt am heutigen Tag. */
        fun neu(art: RhythmusArt, heute: LocalDate): Rhythmus = Rhythmus(
            art = art,
            intervall = if (art == RhythmusArt.ALLE_X_TAGE) 2 else 1,
            wochentage = setOf(heute.dayOfWeek.value),
            tag = heute.dayOfMonth,
            monat = heute.monthValue,
            ab = heute,
        )

        private val deutsch = Locale.GERMANY

        fun wochentagName(nummer: Int): String = DayOfWeek.of(nummer).getDisplayName(TextStyle.FULL, deutsch)
        fun wochentagKurz(nummer: Int): String = DayOfWeek.of(nummer).getDisplayName(TextStyle.SHORT, deutsch).removeSuffix(".")
        fun monatName(nummer: Int): String = Month.of(nummer).getDisplayName(TextStyle.FULL, deutsch)
        fun jahrestagText(tag: Int, monat: Int): String = "$tag. ${monatName(monat)}"
        fun datumText(datum: LocalDate): String = "%02d.%02d.%d".format(datum.dayOfMonth, datum.monthValue, datum.year)

        /** „Montag“, „Montag und Freitag“, „Montag, Mittwoch und Freitag“. */
        fun aufzaehlung(teile: List<String>): String = when (teile.size) {
            0 -> ""
            1 -> teile[0]
            else -> teile.dropLast(1).joinToString(", ") + " und " + teile.last()
        }
    }
}
