package de.frank.aufgaben.ui.szene3d

import de.frank.aufgaben.ui.theme.Design
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class Szene3DChoreografieTest {

    @Test
    fun menschLaufwegHaeltSichtbarenAbstandZumStuhl() {
        val chairOpenX = 0.320f
        val chairOpenZ = -0.140f
        val enterPath = CubicPath2D(
            -2.48f, -0.24f,
            -1.05f, +0.56f,
            +0.18f, +0.82f,
            0.620f, +0.420f,
        )
        var minDistGesamt = Float.MAX_VALUE
        var minDistVorbei = Float.MAX_VALUE
        for (i in 0..200) {
            val u = i / 200f
            val x = enterPath.evalX(u)
            val z = enterPath.evalZ(u)
            val dist = hypot(x - chairOpenX, z - chairOpenZ)
            if (dist < minDistGesamt) minDistGesamt = dist
            if (u in 0.40f..0.82f && dist < minDistVorbei) minDistVorbei = dist
        }
        // Stuhlradius ~0.34 m: selbst am Einstieg vor der offenen Sitzfläche >= 0.60 m Abstand, beim Vorbeigehen >= 0.70 m
        assertTrue("Mindestabstand zum Stuhl zu klein: $minDistGesamt", minDistGesamt >= 0.60f)
        assertTrue("Abstand beim Vorbeigehen am Stuhl zu klein: $minDistVorbei", minDistVorbei >= 0.70f)
        assertTrue("Bogenlänge unplausibel: ${enterPath.totalLength}", enterPath.totalLength in 2.8f..4.5f)
    }

    @Test
    fun katzeGehtErstInsKoerbchenLinksObenDannZumMensch() {
        val basketX = -1.04f
        val basketZ = -1.24f
        val toBasket = CubicPath2D(
            -2.62f, -0.28f,
            -1.35f, +0.06f,
            -0.82f, -0.55f,
            basketX, basketZ,
        )
        val toHuman = CubicPath2D(
            basketX, basketZ,
            -0.52f, -0.45f,
            -0.02f, +0.34f,
            0.64f, +0.44f,
        )
        assertEquals(basketX, toBasket.evalX(1f), 0.001f)
        assertEquals(basketZ, toBasket.evalZ(1f), 0.001f)
        assertEquals(basketX, toHuman.evalX(0f), 0.001f)
        assertEquals(basketZ, toHuman.evalZ(0f), 0.001f)
        assertEquals(0.64f, toHuman.evalX(1f), 0.001f)
        assertEquals(0.44f, toHuman.evalZ(1f), 0.001f)

        // Prüfen, dass die Katze beim Weg zum Menschen nicht durch den angedockten Stuhl (0.468, -0.108) läuft
        var minChairDist = Float.MAX_VALUE
        for (i in 0..200) {
            val u = i / 200f
            val d = hypot(toHuman.evalX(u) - 0.468f, toHuman.evalZ(u) - (-0.108f))
            if (d < minChairDist) minChairDist = d
        }
        assertTrue("Katzenweg zu nah am Stuhl: $minChairDist", minChairDist >= 0.45f)
    }

    @Test
    fun phasenDeckenTagAbendKatzenpauseNachtUndMorgenAb() {
        assertEquals(5, SZENE_3D_PHASEN.size)
        assertEquals(0, phaseFuerZeit(0.0f))   // Tag
        assertEquals(0, phaseFuerZeit(14.2f))  // Standbild am Tag
        assertEquals(1, phaseFuerZeit(20.0f))  // Abend
        assertEquals(2, phaseFuerZeit(27.0f))  // Katzenpause
        assertEquals(3, phaseFuerZeit(42.0f))  // Dunkle Nacht
        assertEquals(4, phaseFuerZeit(51.0f))  // Sonnenaufgang & Morgen-Rückkehr
        assertEquals(4, phaseFuerZeit(65.5f))  // Langer Ausklang
        assertTrue(SZENE_3D_ZYKLUS > 60f)
        assertTrue(SZENE_3D_LOOP_START in 7.0f..9.0f)
    }

    @Test
    fun winkelInterpolationNimmtImmerKuerzestenWeg() {
        val midWrap = lerpAngleDeg(170f, -170f, 0.5f)
        assertEquals(180f, abs(midWrap), 0.01f)
        val midShort = lerpAngleDeg(26f, -60f, 0.5f)
        assertEquals(-17f, midShort, 0.01f)
    }

    @Test
    fun paletteEqualityVergleichtDesignUndDunkel() {
        val a = palette3DFuer(Design.ORANGE, false)
        val b = palette3DFuer(Design.ORANGE, false)
        val c = palette3DFuer(Design.ORANGE, true)
        val d = palette3DFuer(Design.KOSMOS, false)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assertNotEquals(a, d)
    }
}
