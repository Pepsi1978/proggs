package com.entropyjournal.ui.components.energyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Halt-Zustand und Frame-Loop fuer das Energy Board.
 */
class EnergyController {
    val phaseNanos = mutableLongStateOf(0L)
    val sparks = mutableStateListOf<Spark>()
    val arcs = mutableStateListOf<Arc>()
    val lichtenbergs = mutableStateListOf<Lichtenberg>()

    /** Y-Position des fokussierten Punkts (Mitte des sichtbaren Bereichs). */
    val focusY = mutableStateOf<Float?>(null)

    /** Spawn-Punkte: alle sichtbaren Eintraege auf der Schiene. */
    val spawnPoints = mutableStateOf<List<Float>>(emptyList())

    /** Bounds der aktuell aktiven Karte (vom Overlay gesetzt). */
    val activeCardBounds = mutableStateOf<Rect?>(null)

    /** Mitte der Linie horizontal. */
    val railX = mutableStateOf(0f)

    private var lastSparkSpawnMs = 0L
    private var lastArcSpawnMs = 0L
    private var lastLichtenbergMs = 0L

    fun tick(frameTimeMs: Long) {
        val points = spawnPoints.value
        val rx = railX.value
        val focus = focusY.value
        val cardBounds = activeCardBounds.value

        // Funken
        if (points.isNotEmpty() && frameTimeMs - lastSparkSpawnMs > EnergyTheme.SPARK_SPAWN_INTERVAL_MS) {
            focus?.let { fy ->
                if (sparks.size < EnergyTheme.MAX_SPARKS) {
                    repeat(2 + Random.nextInt(2)) {
                        if (sparks.size < EnergyTheme.MAX_SPARKS) {
                            sparks.add(makeSpark(rx, fy, frameTimeMs, intense = true))
                        }
                    }
                }
            }
            points.forEach { y ->
                if (focus == null || (y - focus).let { d -> d * d > 200f * 200f }) {
                    if (Random.nextFloat() < 0.45f && sparks.size < EnergyTheme.MAX_SPARKS) {
                        sparks.add(makeSpark(rx, y, frameTimeMs, intense = false))
                    }
                }
            }
            lastSparkSpawnMs = frameTimeMs
        }

        // Quer-Blitze: jetzt gezielt zur aktiven Karte hin gerichtet
        if (frameTimeMs - lastArcSpawnMs > EnergyTheme.ARC_SPAWN_INTERVAL_MS && cardBounds != null) {
            val sourceY = focus ?: cardBounds.center.y
            arcs.add(makeArcTowardsCard(rx, sourceY, cardBounds, frameTimeMs))
            lastArcSpawnMs = frameTimeMs
        } else if (frameTimeMs - lastArcSpawnMs > EnergyTheme.ARC_SPAWN_INTERVAL_MS && points.isNotEmpty()) {
            val sourceY = focus ?: points.random()
            arcs.add(makeArc(rx, sourceY, frameTimeMs))
            lastArcSpawnMs = frameTimeMs
        }

        // Lichtenberg-Cluster um die aktive Karte
        if (cardBounds != null && frameTimeMs - lastLichtenbergMs > EnergyTheme.LICHTENBERG_SPAWN_INTERVAL_MS) {
            val cluster = makeLichtenbergCluster(rx, focus ?: cardBounds.center.y, cardBounds, frameTimeMs)
            lichtenbergs.addAll(cluster)
            lastLichtenbergMs = frameTimeMs
        }

        cullExpired(frameTimeMs)
    }

    private fun cullExpired(frameTimeMs: Long) {
        val it = sparks.iterator()
        while (it.hasNext()) {
            val s = it.next()
            if (frameTimeMs - s.bornAtMs > s.lifetimeMs) it.remove()
        }
        val ai = arcs.iterator()
        while (ai.hasNext()) {
            val a = ai.next()
            if (frameTimeMs - a.bornAtMs > EnergyTheme.ARC_LIFETIME_MS) ai.remove()
        }
        val li = lichtenbergs.iterator()
        while (li.hasNext()) {
            val l = li.next()
            if (frameTimeMs - l.bornAtMs > EnergyTheme.LICHTENBERG_LIFETIME_MS) li.remove()
        }
    }

    private fun makeSpark(rx: Float, y: Float, frameTimeMs: Long, intense: Boolean): Spark {
        val dir = if (Random.nextBoolean()) 1f else -1f
        val angle = Random.nextFloat() * 0.8f + 0.1f
        val baseSpeed = if (intense) 2.4f else 1.4f
        val speed = Random.nextFloat() * baseSpeed + 1.2f
        return Spark(
            startX = rx,
            startY = y,
            velX = dir * cos(angle) * speed,
            velY = (Random.nextFloat() * 2f - 1f) * speed * 0.5f,
            bornAtMs = frameTimeMs,
            lifetimeMs = (EnergyTheme.SPARK_LIFETIME_MS_MIN..EnergyTheme.SPARK_LIFETIME_MS_MAX).random(),
            len = if (intense) Random.nextFloat() * 8f + 6f else Random.nextFloat() * 5f + 3f,
        )
    }

    /** Random-Quer-Blitz (wenn keine Karte aktiv). */
    private fun makeArc(rx: Float, y: Float, frameTimeMs: Long): Arc {
        val dir = if (Random.nextBoolean()) 1f else -1f
        val totalLen = 80f + Random.nextFloat() * 80f
        val segments = 4 + Random.nextInt(3)
        val points = ArrayList<Pair<Float, Float>>(segments + 1)
        points.add(rx to y)
        var x = rx
        var cy = y
        for (i in 1..segments) {
            val progress = i.toFloat() / segments
            val targetX = rx + dir * totalLen * progress
            val jitter = (Random.nextFloat() * 18f - 9f)
            x = targetX + (Random.nextFloat() * 12f - 6f)
            cy = y + jitter * progress
            points.add(x to cy)
        }
        return Arc(points = points, bornAtMs = frameTimeMs)
    }

    /**
     * Quer-Blitz der zur aktiven Karte gerichtet ist — zielt auf einen zufaelligen Punkt
     * innerhalb der Card-Bounds. Damit zucken die Blitze nicht ins Leere sondern wirken
     * wie sie die Karte mit Energie aufladen.
     */
    private fun makeArcTowardsCard(rx: Float, sourceY: Float, cardBounds: Rect, frameTimeMs: Long): Arc {
        val targetX = cardBounds.left + Random.nextFloat() * cardBounds.width
        val targetY = cardBounds.top + Random.nextFloat() * cardBounds.height
        val segments = 4 + Random.nextInt(3)
        val points = ArrayList<Pair<Float, Float>>(segments + 1)
        points.add(rx to sourceY)
        for (i in 1..segments) {
            val progress = i.toFloat() / segments
            val baseX = rx + (targetX - rx) * progress
            val baseY = sourceY + (targetY - sourceY) * progress
            val jitter = (1f - progress).pow(1.2f) * 18f  // Jitter nimmt zum Ziel hin ab
            val jx = (Random.nextFloat() * 2f - 1f) * jitter
            val jy = (Random.nextFloat() * 2f - 1f) * jitter
            points.add((baseX + jx) to (baseY + jy))
        }
        return Arc(points = points, bornAtMs = frameTimeMs)
    }

    /**
     * Erzeugt einen Cluster aus mehreren Lichtenberg-Pfaden die von der Stromlinie
     * zu zufaelligen Punkten am Rand der Karte verzweigen. Jeder Pfad ist via
     * Midpoint-Displacement fraktal aufgebaut.
     */
    private fun makeLichtenbergCluster(
        rx: Float,
        sourceY: Float,
        cardBounds: Rect,
        frameTimeMs: Long,
    ): List<Lichtenberg> {
        val n = (EnergyTheme.LICHTENBERG_BRANCHES_MIN..EnergyTheme.LICHTENBERG_BRANCHES_MAX).random()
        val result = ArrayList<Lichtenberg>(n)
        repeat(n) {
            // Ziel: ein zufaelliger Punkt im Bereich der Karte (auch leicht ausserhalb fuer Halo-Effekt)
            val padX = cardBounds.width * 0.05f
            val padY = cardBounds.height * 0.1f
            val tx = cardBounds.left - padX + Random.nextFloat() * (cardBounds.width + 2 * padX)
            val ty = cardBounds.top - padY + Random.nextFloat() * (cardBounds.height + 2 * padY)
            val path = generateLichtenbergPath(
                Offset(rx, sourceY),
                Offset(tx, ty),
                EnergyTheme.LICHTENBERG_DEPTH,
                EnergyTheme.LICHTENBERG_DISPLACE_PX,
            )
            result.add(Lichtenberg(points = path, bornAtMs = frameTimeMs))
        }
        return result
    }
}

/** Funken — bewegt sich ballistisch von startX/startY weg. */
data class Spark(
    val startX: Float,
    val startY: Float,
    val velX: Float,
    val velY: Float,
    val bornAtMs: Long,
    val lifetimeMs: Long,
    val len: Float,
)

/** Quer-Blitz — gezackte Linie aus mehreren Punkten, kurz lebend. */
data class Arc(
    val points: List<Pair<Float, Float>>,
    val bornAtMs: Long,
)

/** Lichtenberg-Pfad — fraktal verzweigte Mini-Lightning-Bolt. */
data class Lichtenberg(
    val points: List<Offset>,
    val bornAtMs: Long,
)

/**
 * Generiert einen fraktalen Pfad zwischen start und end via Midpoint-Displacement.
 * depth = Rekursionstiefe (Anzahl der Verdoppelungen), displace = max. Versatz pro Mittelpunkt.
 */
fun generateLichtenbergPath(start: Offset, end: Offset, depth: Int, displace: Float): List<Offset> {
    var points = mutableListOf(start, end)
    var disp = displace
    repeat(depth) {
        val newPoints = mutableListOf<Offset>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            newPoints.add(p1)
            val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            val ox = (Random.nextFloat() * 2f - 1f) * disp
            val oy = (Random.nextFloat() * 2f - 1f) * disp
            newPoints.add(Offset(mid.x + ox, mid.y + oy))
        }
        newPoints.add(points.last())
        points = newPoints
        disp *= 0.55f  // Subdivisionen werden glatter
    }
    return points
}

/**
 * Erzeugt einen [EnergyController] und startet den Frame-Loop.
 */
@Composable
fun rememberEnergyController(): EnergyController {
    val controller = remember { EnergyController() }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                controller.phaseNanos.longValue = nanos
                controller.tick(nanos / 1_000_000L)
            }
        }
    }
    return controller
}

/** Atem-Pulse mit drei ueberlagerten Sinuswellen. Liefert 0f..1f. */
fun organicPulse(timeSec: Float): Float {
    val w1 = sin(timeSec * 0.7f)
    val w2 = sin(timeSec * 1.618f) * 0.3f
    val w3 = sin(timeSec * 2.7f) * 0.1f
    val raw = (w1 + w2 + w3 + 1.4f) / 2.8f
    return raw.coerceIn(0f, 1f).pow(0.7f)
}

/** Glockenkurven-Falloff um den Fokus-Punkt. */
fun focusGlow(distancePx: Float, sigma: Float = EnergyTheme.FOCUS_FALLOFF_PX / 2f): Float {
    val n = distancePx / sigma
    return kotlin.math.exp(-(n * n)).coerceIn(0f, 1f)
}

/** Pseudo-Random-Knister-Wert, deterministisch pro Zeit+Position. */
fun crackleNoise(timeSec: Float, y: Float): Float {
    val a = sin(timeSec * 12.34f + y * 0.07f)
    val b = sin(timeSec * 23.71f + y * 0.13f)
    val c = sin(timeSec * 41.0f + y * 0.21f)
    return 0.7f + 0.15f * (a + b * 0.6f + c * 0.4f) / 2f
}
