package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wächter gegen den leisen, halligen Satzanfang: Pausen, Vorlauf und Wachhalter dürfen nie aus digitalen Nullen
 * bestehen, sonst schaltet der Lautsprecher-Verstärker in der Pause ab und blendet den nächsten Satz leise ein.
 */
class PausenRauschenTest {
    @get:Rule val ordner = TemporaryFolder()

    private fun samples(millis: Long, rate: Int = Tones.STANDARD_RATE): ShortArray {
        val bytes = Tones.stille(ordner.root, millis, rate).readBytes()
        val daten = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return ShortArray(daten.remaining()).also { daten.get(it) }
    }

    @Test fun pauseIstNichtDigitalStill() {
        val werte = samples(2000)
        assertEquals(22050 * 2, werte.size)
        // Fast alle Werte ungleich null: keine stillen Strecken, die der Verstärker als „aus“ werten könnte.
        assertTrue(werte.count { it.toInt() != 0 } > werte.size * 0.9)
    }

    @Test fun pauseBleibtLeise() {
        // Tiefton −40 dBFS plus Rauschen: nie lauter als etwa −39 dBFS.
        assertTrue(samples(1500).all { kotlin.math.abs(it.toInt()) <= 360 })
    }

    @Test fun pauseHatDieAbtastrateDerSprache() {
        // Google/Edge liefern 24 kHz: gleiches Format, damit der Übergang wirklich lückenlos ist.
        assertEquals(24000 * 2, samples(2000, 24000).size)
    }

    @Test fun pauseEndetImNulldurchgang() {
        // Ganze Schwingungen: kein Knacken am Übergang und in der Dauerschleife.
        val werte = samples(Tones.VORLAUF_MS)
        assertTrue(kotlin.math.abs(werte.first().toInt()) <= 30 && kotlin.math.abs(werte.last().toInt()) <= 40)
    }

    @Test fun vorlaufHatDieVorlaufLaenge() {
        assertEquals((22050 * Tones.VORLAUF_MS / 1000).toInt(), samples(Tones.VORLAUF_MS).size)
    }

    @Test fun neueErzeugungNutztNeuenDateinamen() {
        // Eine alte Nullen-Datei aus dem Cache darf nie weiterbenutzt werden.
        assertTrue(Tones.stille(ordner.root, 3000).name.endsWith("_v3.wav"))
    }
}
