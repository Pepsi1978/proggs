package com.entropyjournal.ui.components.energyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Halt-Zustand und Frame-Loop fuer das Energy Board.
 * - Animiertes [phaseNanos] wird im Canvas-Lambda gelesen, NICHT in der Composition.
 * - Funken und Quer-Blitze leben in SnapshotStateLists — Add/Remove triggert nur Repaint.
 * - V2: kein Touch-Tracking mehr — der Overlay greift keine Touch-Events ab, damit die
 *   LazyColumn frei scrollen kann.
 */
class EnergyController {
    val phaseNanos = mutableLongStateOf(0L)

    /** Aktive Funken — direkt aus drawBehind/Canvas lesen, nicht in Composition. */
    val sparks = mutableStateListOf<Spark>()

    /** Aktive Quer-Blitze (Mini-Lichtboegen die seitlich ausschiessen). */
    val arcs = mutableStateListOf<Arc>()

    /** Y-Position des fokussierten Punkts (Mitte des sichtbaren Bereichs, wird vom Overlay gesetzt). */
    val focusY = mutableStateOf<Float?>(null)

    /** Spawn-Punkte: alle sichtbaren Eintraege auf der Schiene. Wird vom Overlay pro Frame gesetzt. */
    val spawnPoints = mutableStateOf<List<Float>>(emptyList())

    /** Mitte der Linie horizontal — wird vom Overlay gesetzt. */
    val railX = mutableStateOf(0f)

    private var lastSparkSpawnMs = 0L
    private var lastArcSpawnMs = 0L

    /**
     * Wird vom Frame-Loop aufgerufen — spawnt neue Funken/Quer-Blitze und entfernt abgelaufene.
     * Spawn-Punkte sind alle sichtbaren Eintragspunkte; der Fokus-Punkt bekommt deutlich
     * mehr Funken als die anderen.
     */
    fun tick(frameTimeMs: Long) {
        val points = spawnPoints.value
        if (points.isEmpty()) {
            // Trotzdem alte Particles altern lassen
            cullExpired(frameTimeMs)
            return
        }

        val rx = railX.value
        val focus = focusY.value

        // Funken-Spawn — schnell und an mehreren Punkten
        if (frameTimeMs - lastSparkSpawnMs > EnergyTheme.SPARK_SPAWN_INTERVAL_MS) {
            // Aus dem Fokus-Punkt 2-3 Funken auf einmal, aus jedem anderen Punkt mit 25% Wahrscheinlichkeit einen
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

        // Quer-Blitze: seltener, aber visuell sehr deutlich
        if (frameTimeMs - lastArcSpawnMs > EnergyTheme.ARC_SPAWN_INTERVAL_MS) {
            val sourceY = if (focus != null && Random.nextFloat() < 0.6f) focus
            else points.random()
            arcs.add(makeArc(rx, sourceY, frameTimeMs))
            lastArcSpawnMs = frameTimeMs
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
    }

    private fun makeSpark(rx: Float, y: Float, frameTimeMs: Long, intense: Boolean): Spark {
        val dir = if (Random.nextBoolean()) 1f else -1f
        val angle = Random.nextFloat() * 0.8f + 0.1f  // 0.1..0.9 rad
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

    /**
     * Erzeugt einen Quer-Blitz: gezackte Linie, die seitlich von der Schiene ausschiesst,
     * 80-160px lang, mit 4-6 Knickpunkten.
     */
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
}

/**
 * Funken — bewegt sich ballistisch von startX/startY weg.
 */
data class Spark(
    val startX: Float,
    val startY: Float,
    val velX: Float,
    val velY: Float,
    val bornAtMs: Long,
    val lifetimeMs: Long,
    val len: Float,
)

/**
 * Quer-Blitz — gezackte Linie aus mehreren Punkten, kurz lebend.
 * points enthaelt Stuetz-Koordinaten (x, y) — direkt verbinden gibt einen Zackenblitz.
 */
data class Arc(
    val points: List<Pair<Float, Float>>,
    val bornAtMs: Long,
)

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

/**
 * Atem-Pulse mit drei ueberlagerten Sinuswellen. Liefert 0f..1f.
 */
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

/**
 * Pseudo-Random-Knister-Wert basierend auf Zeit + Position. Liefert 0.6f..1.0f, deterministisch
 * (gleiches t,y → gleicher Wert), aber chaotisch genug fuer Flackern.
 */
fun crackleNoise(timeSec: Float, y: Float): Float {
    val a = sin(timeSec * 12.34f + y * 0.07f)
    val b = sin(timeSec * 23.71f + y * 0.13f)
    val c = sin(timeSec * 41.0f + y * 0.21f)
    return 0.7f + 0.15f * (a + b * 0.6f + c * 0.4f) / 2f
}
