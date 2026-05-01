package com.entropyjournal.ui.components.energyboard

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generator fuer fraktale Lichtenberg-Baeume.
 *
 * Algorithmus: Rekursiver Random-Walk mit exponentiell abfallender Branch-Probability
 * und asymmetrischer Faecher-Aufteilung.
 *
 * Quelle: Researcher B (2026-05-01) — der Sweet Spot zwischen Look und Performance.
 * Referenzen: Paul Bourke L-Systems, Envato Tuts+ 2D Lightning, UNC Physically Based
 * Animation of Lightning.
 */
object LightningTreeGenerator {

    /** Ein Segment des Blitz-Baumes — sortiert nach Generation fuer Layered-Render. */
    data class Segment(
        val start: Offset,
        val end: Offset,
        val generation: Int,
        val intensity: Float,   // 1.0 = volle Helligkeit, faellt mit Generation
    )

    private const val MAX_GEN = 4
    private const val LENGTH_DECAY = 0.62f
    private const val INTENSITY_DECAY = 0.72f
    private val BRANCH_PROB = floatArrayOf(0.85f, 0.60f, 0.35f, 0.15f)
    private val MIN_CHILDREN = intArrayOf(1, 1, 1, 1)
    private val MAX_CHILDREN = intArrayOf(3, 3, 2, 2)
    private val ANGLE_JITTER_MIN = floatArrayOf(0.15f, 0.25f, 0.30f, 0.35f)
    private val ANGLE_JITTER_MAX = floatArrayOf(0.45f, 0.65f, 0.80f, 0.90f)
    private const val SEGMENTS_PER_BRANCH = 6
    private const val DISPLACEMENT_STRENGTH = 0.25f

    /**
     * Erzeugt einen vollstaendigen fraktalen Blitz-Baum vom Ursprung aus.
     *
     * @param origin Startpunkt des Hauptastes
     * @param initialAngle Richtung des Hauptastes in Bogenmass (0 = nach rechts, PI/2 = nach unten)
     * @param initialLength Laenge des Hauptastes in Pixeln
     * @param rng Random-Quelle — fuer Determinismus oder Variation
     */
    fun generate(
        origin: Offset,
        initialAngle: Float,
        initialLength: Float = 120f,
        rng: Random = Random.Default,
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        growBranch(
            start = origin,
            angle = initialAngle,
            length = initialLength,
            generation = 0,
            intensity = 1.0f,
            segments = segments,
            rng = rng,
        )
        return segments
    }

    private fun growBranch(
        start: Offset,
        angle: Float,
        length: Float,
        generation: Int,
        intensity: Float,
        segments: MutableList<Segment>,
        rng: Random,
    ) {
        if (generation >= MAX_GEN) return
        if (length < 6f) return

        val endX = start.x + cos(angle) * length
        val endY = start.y + sin(angle) * length
        val pathPoints = generateJaggedPath(start, Offset(endX, endY), generation, rng)

        for (i in 0 until pathPoints.size - 1) {
            segments.add(
                Segment(
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    generation = generation,
                    intensity = intensity,
                )
            )
        }

        val actualEnd = pathPoints.last()
        val branchProb = BRANCH_PROB.getOrElse(generation) { 0f }
        if (rng.nextFloat() > branchProb) return

        val minC = MIN_CHILDREN.getOrElse(generation) { 1 }
        val maxC = MAX_CHILDREN.getOrElse(generation) { 2 }
        val numChildren = rng.nextInt(minC, maxC + 1)

        val spread = (PI * 0.6).toFloat()
        val angleStep = if (numChildren > 1) spread / (numChildren - 1) else 0f
        val startAngle = angle - spread / 2f

        for (i in 0 until numChildren) {
            val childBase = if (numChildren > 1) startAngle + i * angleStep else angle
            val jitterMin = ANGLE_JITTER_MIN.getOrElse(generation) { 0.2f }
            val jitterMax = ANGLE_JITTER_MAX.getOrElse(generation) { 0.8f }
            val jitter = rng.nextFloat() * (jitterMax - jitterMin) + jitterMin
            val sign = if (rng.nextBoolean()) 1f else -1f
            val childAngle = childBase + sign * jitter

            // Asymmetrie: dominanter Hauptast bleibt laenger, Seitenaeste kuerzer
            val isMainContinuation = i == numChildren / 2
            val lengthFactor = if (isMainContinuation) 0.92f else 0.55f
            val childLength = length * LENGTH_DECAY * lengthFactor / LENGTH_DECAY

            growBranch(
                start = actualEnd,
                angle = childAngle,
                length = childLength * LENGTH_DECAY,
                generation = generation + 1,
                intensity = intensity * INTENSITY_DECAY,
                segments = segments,
                rng = rng,
            )
        }
    }

    private fun generateJaggedPath(
        from: Offset,
        to: Offset,
        generation: Int,
        rng: Random,
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        points.add(from)

        val dx = (to.x - from.x) / SEGMENTS_PER_BRANCH
        val dy = (to.y - from.y) / SEGMENTS_PER_BRANCH
        val perpX = -dy
        val perpY = dx
        val segLen = sqrt(dx * dx + dy * dy)

        val displacementFactor = DISPLACEMENT_STRENGTH *
            (1f - generation * 0.15f).coerceAtLeast(0.3f)

        for (i in 1 until SEGMENTS_PER_BRANCH) {
            val t = i.toFloat() / SEGMENTS_PER_BRANCH
            val baseX = from.x + t * (to.x - from.x)
            val baseY = from.y + t * (to.y - from.y)
            val displacement = (rng.nextFloat() * 2f - 1f) * segLen * displacementFactor
            val ox = perpX * displacement / sqrt(perpX * perpX + perpY * perpY).coerceAtLeast(0.0001f)
            val oy = perpY * displacement / sqrt(perpX * perpX + perpY * perpY).coerceAtLeast(0.0001f)
            points.add(Offset(baseX + ox, baseY + oy))
        }
        points.add(to)
        return points
    }
}

/** Helper fuer Float-Potenzierung. */
private fun Float.fpow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()
