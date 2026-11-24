package de.frank.aufgaben.ui.szene3d

import de.frank.aufgaben.ui.theme.Design
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Szene3DChoreografieTest {

    private val enterPath = CubicPath2D(
        -2.65f, -0.36f,
        -1.35f, -0.36f,
        -0.05f, +0.96f,
        0.620f, +0.420f,
    )

    @Test
    fun menschLaufwegHaeltSichtbarenAbstandZumStuhl() {
        val chairOpenX = 0.320f
        val chairOpenZ = -0.140f
        var minDistGesamt = Float.MAX_VALUE
        var minDistVorbei = Float.MAX_VALUE
        for (i in 0..200) {
            val u = i / 200f
            val x = enterPath.evalX(u)
            val z = enterPath.evalZ(u)
            val dist = hypot(x - chairOpenX, z - chairOpenZ)
            if (dist < minDistGesamt) minDistGesamt = dist
            if (u in 0.45f..0.85f && dist < minDistVorbei) minDistVorbei = dist
        }
        assertTrue("Mindestabstand zum Stuhl zu klein: $minDistGesamt", minDistGesamt >= 0.60f)
        assertTrue("Abstand beim Vorbeigehen am Stuhl zu klein: $minDistVorbei", minDistVorbei >= 0.68f)
        assertTrue("Bogenlänge unplausibel: ${enterPath.totalLength}", enterPath.totalLength in 3.2f..4.6f)
    }

    @Test
    fun menschArmUndHandBeruehrenWederWandNochTuerrahmenBeimEintritt() {
        // Exakte 3D-Prüfung für beide Arme & Hände über den gesamten Eintritt (inkl. Armradius 0.055 m)
        val armRadius = 0.055f
        for (step in 0..400) {
            val uRaw = step / 400f
            val u = smoother(uRaw)
            val wx = enterPath.evalX(u)
            val wz = enterPath.evalZ(u)
            val yaw = enterPath.evalYawDeg(u)
            val dist = enterPath.distanceAt(u)
            val phaseRad = dist * (PI_F / 0.44f)
            val ramp = smoothPhase(uRaw, 0f, 0.14f) * (1f - smoothPhase(uRaw, 0.84f, 0.16f))
            val s = sin(phaseRad) * ramp
            val doorZone = smooth(phase(-wx, 1.65f, 0.45f))
            val armSwingScale = mix(1.0f, 0.68f, doorZone)
            val armOut = mix(7.0f, 4.2f, doorZone)

            val totalYawRad = degToRad(yaw + s * 6.5f - s * 8.5f * mix(1.0f, 0.72f, doorZone))
            val fwdX = cos(totalYawRad)
            val fwdZ = -sin(totalYawRad)
            val rightX = sin(totalYawRad)
            val rightZ = cos(totalYawRad)

            for (side in floatArrayOf(-1f, 1f)) {
                val shFwd = if (side < 0f) -s * 24f * armSwingScale + 2f else s * 24f * armSwingScale + 2f
                val elbDeg = 15f + kotlin.math.max(0f, if (side < 0f) -s else s) * 18f * armSwingScale + 3f
                // Schulter, Ellbogen und Hand in lokalen (fwd, right)-Koordinaten
                val elbFwd = 0.008f + 0.252f * sinDeg(shFwd)
                val handFwd = elbFwd + 0.255f * sinDeg(shFwd + elbDeg)
                val latElb = side * (0.172f + 0.252f * sinDeg(armOut))
                val latHand = side * (0.172f + 0.507f * sinDeg(armOut))

                val checkPoints = arrayOf(
                    Pair(wx + fwdX * 0.008f + rightX * (side * 0.172f), wz + fwdZ * 0.008f + rightZ * (side * 0.172f)),
                    Pair(wx + fwdX * elbFwd + rightX * latElb, wz + fwdZ * elbFwd + rightZ * latElb),
                    Pair(wx + fwdX * handFwd + rightX * latHand, wz + fwdZ * handFwd + rightZ * latHand),
                )
                for ((px, pz) in checkPoints) {
                    // Linke Wand (x in [-2.37, -2.15]) außerhalb der Türöffnung (z <= -0.82 oder z >= +0.08)
                    val inWallX = px + armRadius > -2.37f && px - armRadius < -2.15f
                    if (inWallX) {
                        assertFalse(
                            "Arm durchdringt vordere Wand bei u=$uRaw: ($px, $pz)",
                            pz + armRadius > 0.08f,
                        )
                        assertFalse(
                            "Arm durchdringt hintere Wand bei u=$uRaw: ($px, $pz)",
                            pz - armRadius < -0.82f,
                        )
                    }
                    // Türzarge vorne (x in [-2.17, -2.09], z in [+0.065, +0.135]) & hinten (z in [-0.875, -0.805])
                    val inJambX = px + armRadius > -2.17f && px - armRadius < -2.09f
                    if (inJambX) {
                        assertFalse(
                            "Arm durchdringt vorderen Türrahmen bei u=$uRaw: ($px, $pz)",
                            pz + armRadius > 0.065f,
                        )
                        assertFalse(
                            "Arm durchdringt hinteren Türrahmen bei u=$uRaw: ($px, $pz)",
                            pz - armRadius < -0.805f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun katzenPfotenDurchdringenBeimVerlassenUndBetretenNieDieKorbwand() {
        val basketX = -1.04f
        val basketZ = -1.24f
        val basketY = 0.068f
        val outX = -0.74f
        val outZ = -0.78f
        val rimTopY = 0.121f
        val basketRot = degToRad(18f)
        val cosB = cos(-basketRot)
        val sinB = sin(-basketRot)

        // Hilfsfunktion: prüft ob eine Pfote (wx, wy, wz) innerhalb der geflochtenen Korbwand steckt
        fun assertPawClearsBasketWall(label: String, wx: Float, wy: Float, wz: Float) {
            if (wy > rimTopY + 0.005f) return // sauber ÜBER dem gesamten Korbrand!
            val dx = wx - basketX
            val dz = wz - basketZ
            // In lokale Korb-Koordinaten drehen
            val lx = dx * cosB - dz * sinB
            val lz = dx * sinB + dz * cosB
            // Elliptischer Radius der Korbwand: Innenwand ~0.72, Außenrand ~1.05 bezogen auf (0.270, 0.235)
            val normR = hypot(lx / 0.270f, lz / 0.235f)
            assertFalse(
                "$label: Pfote ($wx, $wy, $wz) in Korbwand (normR=$normR, y=$wy <= rimTop=$rimTopY)",
                normR in 0.72f..1.08f,
            )
        }

        // 1) Aufstehen im Körbchen (24.05..24.65s) -> alle 4 Pfoten bleiben sauber IM Innenkissen
        val hopOutYaw = atan2(-(outZ - basketZ), outX - basketX) * (180f / PI_F)
        for (i in 0..60) {
            val p = i / 60f
            val yawRad = degToRad(lerpAngleDeg(-28f, hopOutYaw, smoother(p)))
            val cx = cos(yawRad); val sx = -sin(yawRad)
            val rx = sin(yawRad); val rz = cos(yawRad)
            for ((pxL, pzL) in listOf(Pair(0.122f, -0.052f), Pair(0.122f, 0.052f), Pair(-0.11f, -0.056f), Pair(-0.11f, 0.056f))) {
                val wx = basketX + cx * pxL + rx * pzL
                val wz = basketZ + sx * pxL + rz * pzL
                assertPawClearsBasketWall("Aufstehen p=$p", wx, basketY, wz)
            }
        }

        // 2) Bogensprung aus dem Körbchen (24.65..25.27s) -> keine Pfote in der Korbwand
        for (i in 0..100) {
            val k = i / 100f
            val arc = sin(k * PI_F)
            val catX = mix(basketX, outX, k)
            val catZ = mix(basketZ, outZ, k)
            val catY = mix(basketY, 0f, k) + arc * 0.25f
            val yawRad = degToRad(hopOutYaw)
            val cx = cos(yawRad); val sx = -sin(yawRad)
            val rx = sin(yawRad); val rz = cos(yawRad)
            val pawBottomY = catY + 0.008f * arc
            for ((pxL, pzL) in listOf(Pair(0.122f, -0.052f), Pair(0.122f, 0.052f), Pair(-0.11f, -0.056f), Pair(-0.11f, 0.056f))) {
                val wx = catX + cx * pxL + rx * pzL
                val wz = catZ + sx * pxL + rz * pzL
                assertPawClearsBasketWall("Ausstieg k=$k", wx, pawBottomY, wz)
            }
        }

        // 3) Weg vom Landepunkt zum Menschen weicht Stuhl sauber aus
        val toHuman = CubicPath2D(outX, outZ, -0.36f, -0.22f, +0.02f, +0.36f, 0.64f, 0.44f)
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
        assertEquals(0, phaseFuerZeit(0.0f))
        assertEquals(0, phaseFuerZeit(14.2f))
        assertEquals(1, phaseFuerZeit(20.0f))
        assertEquals(2, phaseFuerZeit(27.0f))
        assertEquals(3, phaseFuerZeit(42.0f))
        assertEquals(4, phaseFuerZeit(51.0f))
        assertEquals(4, phaseFuerZeit(65.5f))
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
