package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusikAnzeigeTest {
    @Test fun eingebauterTonWirdOhneDateiAufgeloest() {
        val alarm = Alarm(tone = "chime", music = "", musicName = "")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertEquals(MusikArt.EINGEBAUT, anzeige.art)
        assertEquals(Tones.names.getValue("chime"), anzeige.titel)
        assertEquals("Eingebauter Weckton", anzeige.quelle)
        assertFalse(anzeige.ersatz)
        assertEquals(Tones.names.getValue("chime"), MusikAnzeige.kurz(alarm, dateiVorhanden = false))
    }

    @Test fun unbekannterEingebauterTonFaelltAufKlassischZurueck() {
        val alarm = Alarm(tone = "unbekannt", music = "")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertEquals(Tones.names.getValue("classic"), anzeige.titel)
        assertFalse(anzeige.ersatz)
    }

    @Test fun vorhandeneMp3ZeigtDateinamenUndQuelle() {
        val alarm = Alarm(music = "/data/alarm_audio/music_1", musicName = "Morgenmelodie.mp3", musicQuelle = "datei")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = true)
        assertEquals(MusikArt.MP3, anzeige.art)
        assertEquals("Morgenmelodie.mp3", anzeige.titel)
        assertEquals("MP3 / Audio-Datei", anzeige.quelle)
        assertFalse(anzeige.ersatz)
        assertEquals("MP3: Morgenmelodie.mp3", MusikAnzeige.kurz(alarm, dateiVorhanden = true))
    }

    @Test fun altbestandOhneMusicQuelleGiltAlsMp3() {
        val alarm = Alarm(music = "/data/alarm_audio/music_alt", musicName = "Lieblingssong.mp3", musicQuelle = "")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = true)
        assertEquals(MusikArt.MP3, anzeige.art)
        assertEquals("Lieblingssong.mp3", anzeige.titel)
        assertFalse(anzeige.ersatz)
    }

    @Test fun geraeteWecktonWirdAlsGeraetErkannt() {
        val alarm = Alarm(music = "/data/alarm_audio/music_2", musicName = "Morning Flower", musicQuelle = "geraet")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = true)
        assertEquals(MusikArt.GERAET, anzeige.art)
        assertEquals("Morning Flower", anzeige.titel)
        assertEquals("Geräte-Weckton", anzeige.quelle)
        assertFalse(anzeige.ersatz)
    }

    @Test fun fehlendeDateiMeldetVerstaendlichenErsatz() {
        val alarm = Alarm(music = "/nicht/mehr/da.mp3", musicName = "Urlaub.mp3", musicQuelle = "datei")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertTrue(anzeige.ersatz)
        assertEquals(MusikArt.MP3, anzeige.art)
        assertEquals(Tones.names.getValue("classic"), anzeige.titel)
        assertEquals("Urlaub.mp3", anzeige.fehlenderName)
        assertTrue(anzeige.quelle.contains("Urlaub.mp3"))
        assertEquals("Datei fehlt (Ersatzton)", MusikAnzeige.kurz(alarm, dateiVorhanden = false))
    }

    @Test fun fehlendeDateiOhneNameNutztSinnvollenPlatzhalter() {
        val alarm = Alarm(music = "/weg.mp3", musicName = "", musicQuelle = "datei")
        val anzeige = MusikAnzeige.von(alarm, dateiVorhanden = false)
        assertTrue(anzeige.ersatz)
        assertEquals("die gewählte Datei", anzeige.fehlenderName)
        assertTrue(anzeige.quelle.contains("die gewählte Datei"))
    }
}
