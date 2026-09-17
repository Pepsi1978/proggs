package de.frank.wecker

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

enum class Step(val title: String) {
    TONE("Klingelzeichen"), IDEAS("Offene Ideen"), TEXT("Eigener Text"), MUSIC("MP3 / Weckton")
}

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Guten Morgen",
    val hour: Int = 7,
    val minute: Int = 0,
    val days: Set<Int> = emptySet(),
    /** Bei Intervallen das feste Ankerdatum, sonst ein einmaliger Kalendertag. */
    val startDate: String = "",
    val intervalDays: Int = 0,
    val enabled: Boolean = true,
    val nextAt: Long = 0,
    val snoozeUntil: Long = 0,
    val snoozeMinutes: Int = 5,
    val snoozeLimit: Int = 3,
    val snoozes: Int = 0,
    val volume: Int = 70,
    val fadeSeconds: Int = 0,
    val vibrate: Boolean = true,
    val steps: List<Step> = listOf(Step.MUSIC),
    val text: String = "",
    val originalText: String = "",
    /** Leere Stimme bzw. null beim Tempo übernimmt den jeweiligen globalen Standard. */
    val voiceProvider: String = "",
    val voiceId: String = "",
    val speechRate: Float? = null,
    val music: String = "",
    val musicName: String = "Klassischer Wecker",
    val tone: String = "classic",
    val cue: String = "chime",
    val reference: String = "",
    val photoRequired: Boolean = false,
    val photoTolerance: Int = 75,
    val minBrightness: Int = 0,
    val color: String = "none",
    val colorPercent: Int = 20,
    val prepared: Map<String, List<String>> = emptyMap(),
    val voiceVariants: List<VoiceVariant> = emptyList(),
    val preparedAt: Long = 0,
    val preparedSpeed: Float = 1f,
    val preparedSignature: String = "",
    val preparationError: String = "",
    /** Gewünschte Schlafdauer in Minuten (30-Minuten-Schritte, 30 bis 1440); 0 = keine Angabe. */
    val sleepMinutes: Int = 0,
    /**
     * Auslassungsmarke als lokales ISO-Datum ("2026-09-19"), nur für wiederholende Wecker: ALLE Termine bis einschließlich
     * dieses Kalendertags gelten als ausgelassen. Ortszeit statt Zeitpunkt, damit ein Uhr- oder Zeitzonenwechsel die
     * Auslassung nicht verliert. Leer = keine Marke (auch bei älteren Einträgen).
     */
    val skippedThrough: String = "",
    /** "" = wie bisher (einmalig, täglich, Wochentage, alle X Tage), "month" = monatlich, "year" = jährlich. */
    val repeatUnit: String = "",
    /** Nur bei "month" (1–12) und "year" (1–5): jeden N-ten Monat bzw. jedes N-te Jahr ab dem Startdatum. */
    val repeatEvery: Int = 0,
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
    val needsSpeech: Boolean get() = steps.any { it == Step.IDEAS || it == Step.TEXT }
    val repeats: Boolean get() = days.isNotEmpty() || intervalDays > 0 || repeatUnit.isNotBlank()
    /** Monate zwischen zwei Terminen bei monatlicher oder jährlicher Wiederholung, sonst 0. */
    val repeatMonths: Int get() = when (repeatUnit) {
        MONTHLY -> repeatEvery.coerceAtLeast(1)
        YEARLY -> repeatEvery.coerceAtLeast(1) * 12
        else -> 0
    }
    /** The skip mark as date, only for repeating alarms; an unreadable value counts as no mark. */
    val skipDate: LocalDate? get() = if (!repeats || skippedThrough.isBlank()) null else runCatching { LocalDate.parse(skippedThrough) }.getOrNull()
    /**
     * A one-off alarm on a fixed date whose time has already passed; it cannot be switched on unchanged.
     * Only a genuinely one-off alarm can expire; monthly and yearly alarms repeat from their anchor.
     */
    fun isExpiredOnce(now: Instant = Instant.now()): Boolean = startDate.isNotBlank() && intervalDays == 0 && repeatUnit.isBlank() &&
        runCatching { AlarmTime.next(this, now) }.isFailure
    fun sameSpeechAs(other: Alarm): Boolean = text == other.text && steps == other.steps &&
        voiceProvider == other.voiceProvider && voiceId == other.voiceId && speechRate == other.speechRate
    fun validate() {
        require(hour in 0..23 && minute in 0..59) { "Ungültige Uhrzeit" }
        require(days.all { it in 1..7 }) { "Ungültiger Wochentag" }
        require(intervalDays in 0..365) { "Der Abstand muss zwischen 1 und 365 Tagen liegen." }
        require(repeatUnit.isBlank() || repeatUnit == MONTHLY || repeatUnit == YEARLY) { "Unbekannte Wiederholung." }
        if (repeatUnit.isBlank()) require(repeatEvery == 0) { "Ohne Monats- oder Jahresrhythmus gibt es keinen Abstand." } else {
            require(startDate.isNotBlank()) { "Wähle zuerst das Startdatum." }
            require(days.isEmpty() && intervalDays == 0) { "Wähle entweder Wochentage, Tagesabstand, Monate oder Jahre." }
            require(repeatEvery in 1..(if (repeatUnit == MONTHLY) 12 else 5)) {
                if (repeatUnit == MONTHLY) "Der Abstand muss zwischen 1 und 12 Monaten liegen." else "Der Abstand muss zwischen 1 und 5 Jahren liegen."
            }
        }
        require(intervalDays == 0 || startDate.isNotBlank()) { "Wähle den ersten Schichttag." }
        require(startDate.isBlank() || days.isEmpty()) { "Wähle entweder Wochentage oder einen Datumsplan." }
        if (startDate.isNotBlank()) LocalDate.parse(startDate)
        require(Schlaf.valid(sleepMinutes)) { "Die Schlafdauer muss zwischen 30 Minuten und 24 Stunden liegen." }
        require(volume in 1..100) { "Die Wecklautstärke muss größer als null sein." }
        require(speechRate == null || speechRate in .5f..2f) { "Das Sprechtempo muss zwischen 0,5× und 2× liegen." }
        require(voiceProvider.isBlank() == voiceId.isBlank()) { "Wähle eine Stimme oder den globalen Standard." }
        require(snoozeMinutes in 1..60 && snoozeLimit in 0..20)
        require(steps.isNotEmpty() && steps.distinct().size == steps.size) { "Wähle mindestens einen Weckschritt." }
        require(Step.TEXT !in steps || text.isNotBlank()) { "Der Erinnerungstext fehlt." }
        require(!photoRequired || reference.isNotBlank() || color != "none" || minBrightness > 0) {
            "Lege zuerst ein Referenzfoto, eine Farbe oder eine Mindesthelligkeit fest."
        }
    }
    fun json(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("hour", hour); put("minute", minute)
        put("days", JSONArray(days.sorted())); put("startDate", startDate); put("intervalDays", intervalDays); put("enabled", enabled); put("nextAt", nextAt)
        put("snoozeUntil", snoozeUntil); put("snoozeMinutes", snoozeMinutes); put("snoozeLimit", snoozeLimit)
        put("snoozes", snoozes); put("volume", volume); put("fadeSeconds", fadeSeconds); put("vibrate", vibrate)
        put("steps", JSONArray(steps.map { it.name })); put("text", text); put("originalText", originalText)
        put("voiceProvider", voiceProvider); put("voiceId", voiceId); put("speechRate", speechRate ?: JSONObject.NULL)
        put("music", music); put("musicName", musicName); put("tone", tone); put("cue", cue); put("reference", reference)
        put("photoRequired", photoRequired); put("photoTolerance", photoTolerance)
        put("minBrightness", minBrightness); put("color", color); put("colorPercent", colorPercent)
        put("prepared", JSONObject().apply { prepared.forEach { (key, files) -> put(key, JSONArray(files)) } })
        put("voiceVariants", JSONArray(voiceVariants.map { it.json() }))
        put("preparedAt", preparedAt); put("preparedSpeed", preparedSpeed); put("preparedSignature", preparedSignature); put("preparationError", preparationError)
        put("sleepMinutes", sleepMinutes)
        put("skippedThrough", skippedThrough)
        put("repeatUnit", repeatUnit); put("repeatEvery", repeatEvery)
    }
    companion object {
        const val MONTHLY = "month"
        const val YEARLY = "year"
        /**
         * Reads an entry. The new monthly/yearly fields are checked strictly: an unknown unit, a missing or unreadable
         * start date, a conflicting combination or an out-of-range interval make this entry invalid. It is then rejected,
         * so the existing AlarmStore path (raw backup, skip the entry) applies. Older entries keep their former meaning.
         */
        fun from(j: JSONObject): Alarm {
            val alarm = read(j)
            val unit = alarm.repeatUnit
            require(unit.isBlank() || unit == MONTHLY || unit == YEARLY) { "Unbekannte Wiederholung: $unit" }
            if (unit.isBlank()) require(alarm.repeatEvery == 0) { "Wiederholungsabstand ohne Einheit" }
            else {
                require(alarm.days.isEmpty() && alarm.intervalDays == 0) { "Widersprüchliche Wiederholung" }
                require(alarm.startDate.isNotBlank() && runCatching { LocalDate.parse(alarm.startDate) }.isSuccess) { "Startdatum fehlt" }
                require(alarm.repeatEvery in 1..(if (unit == MONTHLY) 12 else 5)) { "Abstand außerhalb des Bereichs" }
            }
            return alarm
        }

        private fun read(j: JSONObject) = Alarm(
            id = j.getString("id"), name = j.optString("name", "Wecker"), hour = j.getInt("hour"), minute = j.getInt("minute"),
            days = j.getJSONArray("days").let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() },
            startDate = j.optString("startDate"), intervalDays = j.optInt("intervalDays"),
            enabled = j.optBoolean("enabled"), nextAt = j.optLong("nextAt"), snoozeUntil = j.optLong("snoozeUntil"),
            snoozeMinutes = j.optInt("snoozeMinutes", 5), snoozeLimit = j.optInt("snoozeLimit", 3), snoozes = j.optInt("snoozes"),
            volume = j.optInt("volume", 70), fadeSeconds = j.optInt("fadeSeconds"), vibrate = j.optBoolean("vibrate", true),
            steps = j.getJSONArray("steps").let { a -> (0 until a.length()).map { Step.valueOf(a.getString(it)) } },
            text = j.optString("text"), originalText = j.optString("originalText"), music = j.optString("music"),
            voiceProvider = j.optString("voiceProvider"), voiceId = j.optString("voiceId"),
            speechRate = if (j.isNull("speechRate")) null else j.optDouble("speechRate").toFloat().takeIf { it in .5f..2f },
            musicName = j.optString("musicName", "Klassischer Wecker"), tone = j.optString("tone", "classic"), cue = j.optString("cue", "chime"),
            reference = j.optString("reference"), photoRequired = j.optBoolean("photoRequired"),
            photoTolerance = j.optInt("photoTolerance", 75), minBrightness = j.optInt("minBrightness"),
            color = j.optString("color", "none"), colorPercent = j.optInt("colorPercent", 20),
            prepared = j.optJSONObject("prepared")?.let { p -> p.keys().asSequence().associateWith { key ->
                p.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
            } }.orEmpty(), preparedAt = j.optLong("preparedAt"), preparedSpeed = j.optDouble("preparedSpeed", 1.0).toFloat(), preparedSignature = j.optString("preparedSignature"),
            voiceVariants = j.optJSONArray("voiceVariants")?.let { a -> (0 until a.length()).map { VoiceVariant.from(a.getJSONObject(it)) } }.orEmpty(),
            preparationError = j.optString("preparationError"),
            // Older entries have no field; an invalid stored value falls back to "no sleep duration".
            sleepMinutes = j.optInt("sleepMinutes", 0).takeIf(Schlaf::valid) ?: 0,
            skippedThrough = j.optString("skippedThrough", "").takeIf { raw -> raw.isBlank() || runCatching { LocalDate.parse(raw) }.isSuccess } ?: "",
            // Taken as stored; from() checks them and rejects an invalid entry instead of reinterpreting it.
            repeatUnit = j.optString("repeatUnit", ""),
            repeatEvery = j.optInt("repeatEvery", 0),
        )
    }
}

/**
 * Kalenderbasierte Weckzeiten: Schichtabstände bleiben auch bei Zeitumstellungen erhalten.
 *
 * Regel für Sommer-/Winterzeit (für alle Arten gleich: täglich, Wochentage, Datum, alle X Tage):
 * - Jeder Termin wird aus Datum + Wanduhrzeit berechnet; 07:00 bleibt 07:00 Ortszeit.
 * - Frühjahrslücke: eine Uhrzeit, die es nicht gibt (z. B. 02:30), klingelt um die Lückenlänge später, also 03:30 Sommerzeit.
 * - Doppelte Herbststunde: es gilt nur das erste Vorkommen (02:30 Sommerzeit). Pro Kalendertag gibt es genau einen Kandidaten;
 *   die Folgeplanung rechnet ab dem geklingelten Zeitpunkt weiter, daher klingelt das zweite 02:30 (Winterzeit) nicht erneut.
 * - Schlafdauer und Erinnerung rechnen dagegen in echter verstrichener Zeit (siehe Schlaf, SchlafPlan).
 * Abgesichert durch DstBerlinTest.
 */
object AlarmTime {
    fun next(alarm: Alarm, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = now.atZone(zone).toLocalDate()
        if (alarm.startDate.isNotBlank()) {
            val first = LocalDate.parse(alarm.startDate)
            val months = alarm.repeatMonths
            if (months > 0) {
                // Always counted from the anchor, never from the previous occurrence: no drift, and a missing 29th/30th/31st
                // falls back to the last day of that month while the anchor day returns in the next month that has it.
                val elapsed = java.time.temporal.ChronoUnit.MONTHS.between(java.time.YearMonth.from(first), java.time.YearMonth.from(today)).coerceAtLeast(0)
                val start = elapsed / months
                return (start..start + 2).asSequence()
                    .map { step -> first.plusMonths(step * months) }
                    .filter { !it.isBefore(first) }
                    .map { it.atTime(alarm.hour, alarm.minute).atZone(zone).toInstant() }
                    .first { it > now }.toEpochMilli()
            }
            if (alarm.intervalDays == 0) {
                val once = first.atTime(alarm.hour, alarm.minute).atZone(zone).toInstant()
                require(once > now) { "Diese Weckzeit liegt in der Vergangenheit. Wähle ein späteres Datum oder eine spätere Uhrzeit." }
                return once.toEpochMilli()
            }
            val elapsed = java.time.temporal.ChronoUnit.DAYS.between(first, today).coerceAtLeast(0)
            val cycles = elapsed / alarm.intervalDays
            return (cycles..cycles + 2).asSequence().map { cycle ->
                first.plusDays(cycle * alarm.intervalDays).atTime(alarm.hour, alarm.minute).atZone(zone).toInstant()
            }.first { it > now }.toEpochMilli()
        }
        return (0L..8L).asSequence().flatMap { offset ->
            val day: LocalDate = today.plusDays(offset)
            if (alarm.days.isNotEmpty() && day.dayOfWeek.value !in alarm.days) emptySequence()
            else {
                val local = day.atTime(LocalTime.of(alarm.hour, alarm.minute))
                // Pro Kalendertag genau einmal; eine zurückgestellte Stunde weckt nicht doppelt.
                sequenceOf(local.atZone(zone).toInstant())
            }
        }.filter { it > now }.minOrNull()!!.toEpochMilli()
    }

    /**
     * Like [next], but no candidate on or before the skip date. The search starts one nanosecond before the start of the
     * following day, so a 00:00 alarm on that day is kept; one candidate per calendar day makes a date mark sufficient.
     */
    fun nextRespectingSkip(alarm: Alarm, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val skip = alarm.skipDate ?: return next(alarm, now, zone)
        val afterSkip = skip.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1)
        return next(alarm, if (afterSkip > now) afterSkip else now, zone)
    }

    /**
     * Whether the card shows "skipped": with a mark, the regular next occurrence lies on or before the mark date; without
     * a mark (entries from before the mark existed) the former nextAt comparison stays as fallback.
     */
    fun isSkipping(alarm: Alarm, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!alarm.enabled || !alarm.repeats || alarm.nextAt <= 0) return false
        val regular = runCatching { next(alarm, now, zone) }.getOrNull() ?: return false
        val skip = alarm.skipDate ?: return alarm.nextAt > regular
        return !Instant.ofEpochMilli(regular).atZone(zone).toLocalDate().isAfter(skip)
    }

    /** Local calendar date of an instant, used to set the skip mark. */
    fun localDate(time: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
}
