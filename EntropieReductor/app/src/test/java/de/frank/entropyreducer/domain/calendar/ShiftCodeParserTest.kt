package de.frank.entropyreducer.domain.calendar

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.domain.model.ShiftCode
import org.junit.Test

class ShiftCodeParserTest {

    @Test fun `Tag 1 ist Tagdienst`() {
        assertThat(ShiftCodeParser.parse("Tag 1")).isEqualTo(ShiftCode.TAGDIENST)
    }

    @Test fun `Tag 4 ohne Marker ist Tagdienst`() {
        assertThat(ShiftCodeParser.parse("Tag 4")).isEqualTo(ShiftCode.TAGDIENST)
    }

    @Test fun `Tag 1 mit X-Marker ist Frei`() {
        assertThat(ShiftCodeParser.parse("Tag 1 X")).isEqualTo(ShiftCode.FREI)
    }

    @Test fun `Tag 2 mit F-Marker ist Frei`() {
        assertThat(ShiftCodeParser.parse("Tag 2 F")).isEqualTo(ShiftCode.FREI)
    }

    @Test fun `Tag 1 ohne Leerzeichen ist Tagdienst`() {
        assertThat(ShiftCodeParser.parse("Tag1")).isEqualTo(ShiftCode.TAGDIENST)
    }

    @Test fun `Nacht 1 ist Nachtdienst`() {
        assertThat(ShiftCodeParser.parse("Nacht 1")).isEqualTo(ShiftCode.NACHTDIENST)
    }

    @Test fun `Nacht 4 mit X-Marker ist Frei`() {
        assertThat(ShiftCodeParser.parse("Nacht 4 X")).isEqualTo(ShiftCode.FREI)
    }

    @Test fun `U ist Urlaub`() {
        assertThat(ShiftCodeParser.parse("U")).isEqualTo(ShiftCode.URLAUB)
    }

    @Test fun `Urlaub Sommer ist Urlaub`() {
        assertThat(ShiftCodeParser.parse("Urlaub Sommer")).isEqualTo(ShiftCode.URLAUB)
    }

    @Test fun `Tag 5 ist unbekannt — nur 1-4 erlaubt`() {
        assertThat(ShiftCodeParser.parse("Tag 5")).isEqualTo(ShiftCode.UNBEKANNT)
    }

    @Test fun `Geburtstag ist unbekannt`() {
        assertThat(ShiftCodeParser.parse("Geburtstag Mama")).isEqualTo(ShiftCode.UNBEKANNT)
    }

    @Test fun `Leerstring ist unbekannt`() {
        assertThat(ShiftCodeParser.parse("")).isEqualTo(ShiftCode.UNBEKANNT)
    }

    @Test fun `Whitespace wird ignoriert`() {
        assertThat(ShiftCodeParser.parse("  Tag 1  ")).isEqualTo(ShiftCode.TAGDIENST)
    }

    @Test fun `Tagdienst-Profil hat Schlaffenster 21-04`() {
        val profile = ShiftCodeParser.profileFor(ShiftCode.TAGDIENST)
        assertThat(profile.sleepWindowStart).isEqualTo("21:00")
        assertThat(profile.sleepWindowEnd).isEqualTo("04:00")
        assertThat(profile.workWindowStart).isEqualTo("04:00")
        assertThat(profile.workWindowEnd).isEqualTo("18:30")
        assertThat(profile.availableMinutesEstimate).isAtLeast(30)
    }

    @Test fun `Nachtdienst-Profil hat Schlaffenster 06-15`() {
        val profile = ShiftCodeParser.profileFor(ShiftCode.NACHTDIENST)
        assertThat(profile.sleepWindowStart).isEqualTo("06:30")
        assertThat(profile.sleepWindowEnd).isEqualTo("15:00")
        assertThat(profile.availableMinutesEstimate).isAtLeast(60)
    }

    @Test fun `Frei-Profil hat 480 verfuegbare Minuten`() {
        val profile = ShiftCodeParser.profileFor(ShiftCode.FREI)
        assertThat(profile.availableMinutesEstimate).isEqualTo(480)
        assertThat(profile.workWindowStart).isNull()
    }

    @Test fun `Urlaub-Profil hat 600 verfuegbare Minuten`() {
        val profile = ShiftCodeParser.profileFor(ShiftCode.URLAUB)
        assertThat(profile.availableMinutesEstimate).isEqualTo(600)
    }

    @Test fun `Unbekannt-Profil hat Default-Schlaffenster 22-06`() {
        val profile = ShiftCodeParser.profileFor(ShiftCode.UNBEKANNT)
        assertThat(profile.sleepWindowStart).isEqualTo("22:00")
        assertThat(profile.sleepWindowEnd).isEqualTo("06:00")
    }
}
