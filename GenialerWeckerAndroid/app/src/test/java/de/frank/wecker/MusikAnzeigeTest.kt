package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test

/** Der unter „MP3 / Weckton“ angezeigte Titel muss dem entsprechen, was AlarmPlaylist wirklich spielt. */
class MusikAnzeigeTest {
    private val toene = Tones.names.keys.associateWith { "tone_$it.wav" }

    private fun gespielt(alarm: Alarm, dateiDa: Boolean) =
        AlarmPlaylist.build(alarm, toene) { pfad -> pfad in toene.values || dateiDa }.single { it.step == Step.MUSIC.title }.audio.path

    @Test fun neuerWeckerOhneDateiSpieltDenAngezeigtenEingebautenTon() {
        val alarm = Alarm()
        assertEquals(listOf(Step.MUSIC), alarm.steps)
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertEquals("Klassischer Wecker", anzeige.titel)
        assertFalse(anzeige.ersatz)
        assertEquals("tone_classic.wav", gespielt(alarm, dateiDa = false))
    }

    @Test fun gewaehlteDateiWirdAngezeigtUndGespielt() {
        val alarm = Alarm(music = "/daten/music_1", musicName = "Morgenlied.mp3", musicQuelle = "datei")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = true)
        assertEquals("Morgenlied.mp3", anzeige.titel)
        assertEquals("/daten/music_1", gespielt(alarm, dateiDa = true))
    }

    @Test fun fehlendeDateiZeigtEhrlichDenErsatzton() {
        val alarm = Alarm(music = "/daten/weg", musicName = "Weg.mp3", musicQuelle = "datei")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertTrue(anzeige.ersatz)
        assertEquals("Klassischer Wecker", anzeige.titel)
        assertEquals("tone_classic.wav", gespielt(alarm, dateiDa = false))
    }
}
