package de.frank.entropyreducer.domain.calendar

import de.frank.entropyreducer.domain.model.ShiftCode

/**
 * Parsed Frank's Schichtcode aus einem Google-Calendar-Ganztagestermin.
 *
 * Erlaubte Schreibweisen (case-insensitive, Whitespace toleriert):
 *   - "U" oder "Urlaub..." -> URLAUB
 *   - "Tag 1".."Tag 4" oder "Tag1".."Tag4" -> TAGDIENST
 *   - "Tag 1 X" oder "Tag 1 F" (Frei-Marker) -> FREI
 *   - "Nacht 1".."Nacht 4" -> NACHTDIENST
 *   - "Nacht 1 X" / "Nacht 1 F" -> FREI
 *   - alles andere -> UNBEKANNT
 *
 * Spec §15.5.
 */
object ShiftCodeParser {

    private val tagPattern = Regex("""^TAG\s*[1-4](?:\s*([XF]))?$""")
    private val nachtPattern = Regex("""^NACHT\s*[1-4](?:\s*([XF]))?$""")

    fun parse(summary: String): ShiftCode {
        val s = summary.trim().uppercase()
        if (s.isEmpty()) return ShiftCode.UNBEKANNT

        if (s == "U" || s.startsWith("URLAUB")) return ShiftCode.URLAUB

        tagPattern.find(s)?.let { match ->
            return if (match.groupValues[1].isNotEmpty()) ShiftCode.FREI else ShiftCode.TAGDIENST
        }
        nachtPattern.find(s)?.let { match ->
            return if (match.groupValues[1].isNotEmpty()) ShiftCode.FREI else ShiftCode.NACHTDIENST
        }
        return ShiftCode.UNBEKANNT
    }

    /**
     * Liefert das Schicht-Profil fuer die gegebene Schicht (Spec §15.5 Tabelle).
     * workWindow / sleepWindow als HH:mm; bei FREI/URLAUB sind die Fenster flexibel (null).
     */
    fun profileFor(code: ShiftCode): ShiftProfile = when (code) {
        ShiftCode.TAGDIENST -> ShiftProfile(
            workWindowStart = "04:00", workWindowEnd = "18:30",
            sleepWindowStart = "21:00", sleepWindowEnd = "04:00",
            availableMinutesEstimate = 45,
        )
        ShiftCode.NACHTDIENST -> ShiftProfile(
            workWindowStart = "16:00", workWindowEnd = "05:50",
            sleepWindowStart = "06:30", sleepWindowEnd = "15:00",
            availableMinutesEstimate = 75,
        )
        ShiftCode.FREI -> ShiftProfile(
            workWindowStart = null, workWindowEnd = null,
            sleepWindowStart = null, sleepWindowEnd = null,
            availableMinutesEstimate = 480,
        )
        ShiftCode.URLAUB -> ShiftProfile(
            workWindowStart = null, workWindowEnd = null,
            sleepWindowStart = null, sleepWindowEnd = null,
            availableMinutesEstimate = 600,
        )
        ShiftCode.UNBEKANNT -> ShiftProfile(
            workWindowStart = null, workWindowEnd = null,
            sleepWindowStart = "22:00", sleepWindowEnd = "06:00",
            availableMinutesEstimate = 240,
        )
    }
}

data class ShiftProfile(
    val workWindowStart: String?,
    val workWindowEnd: String?,
    val sleepWindowStart: String?,
    val sleepWindowEnd: String?,
    val availableMinutesEstimate: Int,
)
