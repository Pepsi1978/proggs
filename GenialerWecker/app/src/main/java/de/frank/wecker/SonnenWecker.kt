package de.frank.wecker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Weckzeit nach der Sonne: Statt einer festen Uhrzeit weckt der Wecker jeden Tag zum gewählten Sonnenzeitpunkt
 * am Standort des Handys (siehe SonnenOrt) in der automatischen Zeitzone, offline berechnet (Sonnenaufgangsgleichung, dieselbe wie im Automatikmodus der Darstellung).
 * Ergebnis immer auf die volle Minute abgerundet, damit Planung, Schlafenszeit und Erinnerung deterministisch bleiben.
 */
object SonnenWecker {

    private const val TAG_MS = 86_400_000L
    const val VERSATZ_MIN = -60
    const val VERSATZ_MAX = 90

    enum class Ereignis(val id: String, val titel: String, val hoehe: Double, val erklaerung: String) {
        DAEMMERUNG("daemmerung", "Beginn der Morgendämmerung", -6.0,
            "Erstes Tageslicht: Die Sonne steht noch 6° unter dem Horizont. Draußen ist es noch dämmrig, drinnen meist dunkel. " +
                "Etwa 35 bis 45 Minuten vor Sonnenaufgang."),
        AUFGANG("aufgang", "Sonnenaufgang", -0.833,
            "Der obere Rand der Sonne erscheint am Horizont. Die Dämmerung ist vorbei, draußen ist es bereits hell."),
        TAGHELL("taghell", "Heller Morgen (Sonne 6° hoch)", 6.0,
            "Die Sonne steht 6° über dem Horizont, etwa 40 bis 60 Minuten nach Sonnenaufgang. Voller Tag, auch in Räumen deutlich hell."),
        ;
        companion object { fun von(id: String): Ereignis? = entries.find { it.id == id } }
    }

    val STANDARD = Ereignis.AUFGANG


    /**
     * Zeitpunkt, zu dem die Sonne am Ortsdatum [tag] (in [zone]) morgens die Höhe [hoehe] erreicht, am Ort [ort];
     * null bei Polartag oder Polarnacht. Fern von Greenwich kann das Ereignis auf dem UTC-Vor- oder Folgetag liegen,
     * deshalb wird auf das Ortsdatum korrigiert.
     */
    fun zeitpunkt(tag: LocalDate, hoehe: Double, ort: SonnenOrt.Ort = SonnenOrt.aktuell(), zone: ZoneId = ZoneId.systemDefault()): Long? {
        var utcTag = tag.toEpochDay()
        repeat(3) {
            val t = zeitpunktUtc(utcTag, hoehe, ort) ?: return null
            val lokal = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
            when {
                lokal == tag -> return t
                lokal.isBefore(tag) -> utcTag++
                else -> utcTag--
            }
        }
        return zeitpunktUtc(utcTag, hoehe, ort)
    }

    private fun zeitpunktUtc(utcTag: Long, hoehe: Double, ort: SonnenOrt.Ort): Long? {
        val BREITE = ort.breite
        val j = utcTag - 10957 - ort.laenge / 360
        val m = 357.5291 + 0.98560028 * j
        val c = 1.9148 * sinG(m) + 0.02 * sinG(2 * m) + 0.0003 * sinG(3 * m)
        val laenge = m + c + 282.9372
        val mittag = j + 0.0053 * sinG(m) - 0.0069 * sinG(2 * laenge)
        val sinDekl = sinG(laenge) * sinG(23.4397)
        val cosDekl = sqrt(1 - sinDekl * sinDekl)
        val x = (sinG(hoehe) - sinG(BREITE) * sinDekl) / (cos(Math.toRadians(BREITE)) * cosDekl)
        if (x < -1 || x > 1) return null
        val stundenwinkel = Math.toDegrees(acos(x))
        return ((mittag - stundenwinkel / 360 + 10957.5) * TAG_MS).toLong()
    }

    /** Weckzeitpunkt am Ortsdatum [tag]: Sonnenzeitpunkt plus Versatz, auf die Minute abgerundet. */
    fun weckzeit(alarm: Alarm, tag: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Instant {
        val ereignis = Ereignis.von(alarm.sunMode) ?: STANDARD
        val ort = SonnenOrt.aktuell(zone)
        // Hohe Breiten: erreicht die Sonne die gewählte Höhe nicht (Polartag, Polarnacht, Mittsommer), gilt der Sonnenaufgang;
        // gibt es auch den nicht, die zuletzt gezeigte feste Uhrzeit.
        val roh = zeitpunkt(tag, ereignis.hoehe, ort, zone) ?: zeitpunkt(tag, Ereignis.AUFGANG.hoehe, ort, zone)
            ?: return tag.atTime(alarm.hour, alarm.minute).atZone(zone).toInstant()
        val mitVersatz = roh + alarm.sunOffsetMinutes * 60_000L
        return Instant.ofEpochMilli(Math.floorDiv(mitVersatz, 60_000L) * 60_000L)
    }

    /** „Sonnenaufgang“, „Sonnenaufgang + 15 Min.“, „20 Min. vor Sonnenaufgang“. */
    fun bezeichnung(alarm: Alarm): String {
        val titel = (Ereignis.von(alarm.sunMode) ?: STANDARD).titel
        val v = alarm.sunOffsetMinutes
        return when {
            v == 0 -> titel
            v > 0 -> "$v Min. nach $titel"
            else -> "${-v} Min. vor $titel"
        }
    }

    /** Für die Ansage: „zum Sonnenaufgang“, „15 Minuten nach Sonnenaufgang“. */
    fun gesprochen(alarm: Alarm): String {
        val e = Ereignis.von(alarm.sunMode) ?: STANDARD
        val (zum, bezug) = when (e) {
            Ereignis.DAEMMERUNG -> "zu Beginn der Morgendämmerung" to "Beginn der Morgendämmerung"
            Ereignis.AUFGANG -> "zum Sonnenaufgang" to "Sonnenaufgang"
            Ereignis.TAGHELL -> "zum hellen Morgen" to "dem hellen Morgen"
        }
        val v = alarm.sunOffsetMinutes
        return when {
            v == 0 -> zum
            v > 0 -> "$v Minuten nach $bezug"
            else -> "${-v} Minuten vor $bezug"
        }
    }

    /** Die nächsten [anzahl] Ortstage ab [ab] mit ihrer Weckzeit, für die Vorschau im Editor. */
    fun vorschau(alarm: Alarm, ab: LocalDate, anzahl: Int = 4, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, String>> =
        (0 until anzahl).map { ab.plusDays(it.toLong()) }.map { tag ->
            tag to weckzeit(alarm, tag, zone).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
        }

    private fun sinG(grad: Double) = sin(Math.toRadians(grad))
}
