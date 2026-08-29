package de.frank.claudekompass

import de.frank.claudekompass.ai.geraeteCodeGruppen
import de.frank.claudekompass.audio.WavSchneider
import de.frank.claudekompass.data.local.baueSuchAnfrage
import de.frank.claudekompass.data.local.normalisiereFuerSuche
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Der Anmeldecode darf unter keinen Umständen gekürzt werden. */
class GeraeteCodeTest {

    @Test
    fun neunZeichenWerdenVierPlusFuenf() {
        assertEquals(listOf("ABCD", "EFGHJ"), geraeteCodeGruppen("ABCDEFGHJ"))
    }

    @Test
    fun serverTrennungGewinnt() {
        assertEquals(listOf("AB", "CDE", "FG"), geraeteCodeGruppen("AB-CDE-FG"))
    }

    @Test
    fun kurzerCodeBleibtAmStueck() {
        assertEquals(listOf("AB12"), geraeteCodeGruppen("AB12"))
    }

    @Test
    fun keinZeichenGehtVerloren() {
        // Der Server bestimmt die Länge. Eine eigene Annahme darüber hiesse: unvollständiger
        // Code auf dem Bildschirm und eine Anmeldung, die scheitert.
        val lang = "ABCDEFGHJKLMNP"
        val gruppen = geraeteCodeGruppen(lang)
        assertEquals(lang.length, gruppen.sumOf { it.length })
    }

    @Test
    fun leererCodeZeigtPlatzhalter() {
        assertEquals(2, geraeteCodeGruppen("").size)
    }
}

/** Die Suche muss Umlaute und Schreibweisen gleich behandeln. */
class SuchNormalisierungTest {

    @Test
    fun umlauteWerdenAufgeloest() {
        assertEquals("ueber", normalisiereFuerSuche("über"))
        assertEquals("ueber", normalisiereFuerSuche("Ueber"))
        assertEquals("massstab", normalisiereFuerSuche("Maßstab"))
    }

    @Test
    fun sonderzeichenTrennenWoerter() {
        assertEquals("code review level", normalisiereFuerSuche("/code-review [level]"))
    }

    @Test
    fun anfrageBekommtSternchenJeWort() {
        assertEquals("suchtext:komp* suchtext:kontext*", baueSuchAnfrage("Komp Kontext"))
    }

    @Test
    fun leereAnfrageBleibtLeer() {
        // Eine leere Anfrage an die Volltextsuche würde eine Ausnahme werfen — der Aufrufer
        // erkennt an der leeren Zeichenkette, dass er gar nicht erst suchen soll.
        assertEquals("", baueSuchAnfrage("   "))
        assertEquals("", baueSuchAnfrage("!?-"))
    }
}

/** Eine zu grosse Aufnahme muss VOR dem Senden geteilt werden — 413 ist nicht wiederholbar. */
class WavSchneiderTest {

    private fun baueAufnahme(sekunden: Int, rate: Int = 16_000): ByteArray {
        val pcm = ByteArray(sekunden * rate * 2)
        // Ein wechselndes Muster mit leisen Stellen, damit die Pausensuche etwas zu finden hat.
        for (index in pcm.indices step 2) {
            val laut = (index / (rate * 2)) % 2 == 0
            val wert = if (laut) 8000 else 5
            pcm[index] = (wert and 0xFF).toByte()
            pcm[index + 1] = ((wert shr 8) and 0xFF).toByte()
        }
        return WavSchneider.baueWav(pcm, rate)
    }

    @Test
    fun kleineAufnahmeBleibtUngeteilt() {
        val klein = baueAufnahme(10)
        val teile = WavSchneider.teileWennNoetig(klein)
        assertEquals(1, teile.size)
        assertTrue(teile[0].contentEquals(klein))
    }

    @Test
    fun grosseAufnahmeWirdGeteilt() {
        // Bei 16 kHz mono sind 20 MB rund elf Minuten. Mit einer kleinen Grenze lässt sich das
        // im Test nachstellen, ohne minutenlange Aufnahmen zu erzeugen.
        val gross = baueAufnahme(30)
        val teile = WavSchneider.teileWennNoetig(gross, grenzeBytes = 200_000)
        assertTrue("Es muss geteilt worden sein", teile.size > 1)
        assertTrue("Jeder Teil braucht einen gültigen Kopf", teile.all { it.size > 44 })
        assertTrue(
            "Jeder Teil muss als RIFF beginnen",
            teile.all { String(it.copyOfRange(0, 4), Charsets.US_ASCII) == "RIFF" },
        )
    }

    @Test
    fun keinTonGehtVerloren() {
        val gross = baueAufnahme(30)
        val teile = WavSchneider.teileWennNoetig(gross, grenzeBytes = 200_000)
        val summeNutzdaten = teile.sumOf { it.size - 44 }
        assertEquals(gross.size - 44, summeNutzdaten)
    }

    @Test
    fun abtastrateBleibtErhalten() {
        val gross = baueAufnahme(30, rate = 24_000)
        val teile = WavSchneider.teileWennNoetig(gross, grenzeBytes = 200_000)
        assertTrue(teile.all { WavSchneider.leseAbtastrate(it) == 24_000 })
    }
}
