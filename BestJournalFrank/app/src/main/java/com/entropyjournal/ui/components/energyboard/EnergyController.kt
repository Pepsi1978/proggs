package com.entropyjournal.ui.components.energyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Halt-Zustand und Frame-Loop fuer das Energy Board.
 * - Animiertes [phaseNanos] wird im Canvas-Lambda gelesen, NICHT in der Composition.
 * - Funken liegen in einer SnapshotStateList — Add/Remove triggert nur Canvas-Repaint.
 * - Fokus-Position wird von aussen gesetzt (Touch oder Scroll-Mitte).
 */
class EnergyController {
    val phaseNanos = mutableLongStateOf(0L)

    /** Funken-Liste — direkt aus drawBehind/Canvas lesen, nicht in Composition. */
    val sparks = mutableStateListOf<Spark>()

    /** Y-Position des aktuell fokussierten Punkts (in Canvas-Koordinaten). null = kein Fokus. */
    val focusY = mutableStateOf<Float?>(null)

    /** Touch-Position (null wenn nicht beruehrt). */
    val touchPos = mutableStateOf<Offset?>(null)

    private var lastSparkSpawnMs = 0L

    /**
     * Wird vom Frame-Loop aufgerufen — spawnt neue Funken am Fokuspunkt
     * und entfernt abgelaufene Funken.
     */
    fun tick(frameTimeMs: Long, canvasWidth: Float) {
        // Spawn-Quelle: Touch hat Vorrang vor Fokus
        val spawnY = touchPos.value?.y ?: focusY.value ?: return
        val spawnX = canvasWidth / 2f

        if (frameTimeMs - lastSparkSpawnMs > EnergyTheme.SPARK_SPAWN_INTERVAL_MS) {
            if (sparks.size < EnergyTheme.MAX_SPARKS) {
                sparks.add(makeSpark(spawnX, spawnY, frameTimeMs))
            }
            lastSparkSpawnMs = frameTimeMs
        }

        // Touch-Burst: bei aktivem Touch zusaetzliche Funken im selben Frame
        if (touchPos.value != null && Random.nextFloat() < 0.4f && sparks.size < EnergyTheme.MAX_SPARKS) {
            sparks.add(makeSpark(spawnX, spawnY, frameTimeMs))
        }

        // Tote Funken entfernen
        val iter = sparks.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            if (frameTimeMs - s.bornAtMs > s.lifetimeMs) iter.remove()
        }
    }

    private fun makeSpark(spawnX: Float, spawnY: Float, frameTimeMs: Long): Spark {
        val dir = if (Random.nextBoolean()) 1f else -1f
        val angle = Random.nextFloat() * 0.6f + 0.2f  // 0.2..0.8 rad — leicht schraeg
        val speed = Random.nextFloat() * 2.2f + 1.0f  // 1.0..3.2 px/frame
        return Spark(
            startX = spawnX,
            startY = spawnY,
            velX = dir * cos(angle) * speed,
            velY = -sin(angle) * speed * 0.6f - Random.nextFloat() * 1.5f,
            bornAtMs = frameTimeMs,
            lifetimeMs = (EnergyTheme.SPARK_LIFETIME_MS_MIN..EnergyTheme.SPARK_LIFETIME_MS_MAX).random(),
            len = Random.nextFloat() * 6f + 4f,
        )
    }
}

/**
 * Einfacher Funken — keine Compose-State-Felder drin, nur Daten.
 * Bewegt sich ballistisch von startX/startY weg.
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
 * Erzeugt einen [EnergyController] und startet den Frame-Loop.
 * Frame-Loop laeuft nur einmal (LaunchedEffect(Unit)).
 */
@Composable
fun rememberEnergyController(): EnergyController {
    val controller = remember { EnergyController() }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                controller.phaseNanos.longValue = nanos
                // Tick mit Millisekunden — canvasWidth wird in Canvas-Lambda gesetzt,
                // hier reicht 0f als Default. Spawn-Logik liest spawnY zuerst.
                controller.tick(nanos / 1_000_000L, 0f)
            }
        }
    }
    return controller
}

/**
 * Atem-Pulse mit drei ueberlagerten Sinuswellen (irrationale Frequenzverhaeltnisse via
 * goldenem Schnitt). Liefert 0f..1f, organisch wirkend.
 */
fun organicPulse(timeSec: Float): Float {
    val w1 = sin(timeSec * 0.7f)
    val w2 = sin(timeSec * 1.618f) * 0.3f
    val w3 = sin(timeSec * 2.7f) * 0.1f
    val raw = (w1 + w2 + w3 + 1.4f) / 2.8f
    return raw.coerceIn(0f, 1f).pow(0.7f)
}

/**
 * Glockenkurven-Falloff um den Fokus-Punkt — je weiter weg, desto schwaecher der Glow.
 * Sigma ist [EnergyTheme.FOCUS_FALLOFF_PX] / 2.
 */
fun focusGlow(distancePx: Float, sigma: Float = EnergyTheme.FOCUS_FALLOFF_PX / 2f): Float {
    val n = distancePx / sigma
    return kotlin.math.exp(-(n * n)).coerceIn(0f, 1f)
}
