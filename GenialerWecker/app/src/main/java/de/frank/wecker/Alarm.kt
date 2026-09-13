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
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
    val needsSpeech: Boolean get() = steps.any { it == Step.IDEAS || it == Step.TEXT }
    val repeats: Boolean get() = days.isNotEmpty() || intervalDays > 0
    fun sameSpeechAs(other: Alarm): Boolean = text == other.text && steps == other.steps &&
        voiceProvider == other.voiceProvider && voiceId == other.voiceId && speechRate == other.speechRate
    fun validate() {
        require(hour in 0..23 && minute in 0..59) { "Ungültige Uhrzeit" }
        require(days.all { it in 1..7 }) { "Ungültiger Wochentag" }
        require(intervalDays in 0..365) { "Der Abstand muss zwischen 1 und 365 Tagen liegen." }
        require(intervalDays == 0 || startDate.isNotBlank()) { "Wähle den ersten Schichttag." }
        require(startDate.isBlank() || days.isEmpty()) { "Wähle entweder Wochentage oder einen Datumsplan." }
        if (startDate.isNotBlank()) LocalDate.parse(startDate)
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
    }
    companion object {
        fun from(j: JSONObject) = Alarm(
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
        )
    }
}

/** Kalenderbasierte Weckzeiten: Schichtabstände bleiben auch bei Zeitumstellungen erhalten. */
object AlarmTime {
    fun next(alarm: Alarm, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = now.atZone(zone).toLocalDate()
        if (alarm.startDate.isNotBlank()) {
            val first = LocalDate.parse(alarm.startDate)
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
}
