package de.frank.jarvis.weckwort

import kotlin.math.max

/**
 * Merkt nach „Hey Jarvis“ am Pegel des Mikrofons, wann Frank fertig gesprochen hat, damit niemand tippen muss.
 *
 * Pegel ist der Wert aus `MicRecorder.pegel` (0 bis 1, Wurzel des Effektivwerts). Die Schwelle passt sich dem
 * Grundrauschen an (Auto, Wind, Lüfter): Der erste Wert nach dem Piepton und danach der leiseste Wert der letzten
 * Zeit bilden den Boden, Sprache liegt deutlich darüber. Der Piepton am Anfang zählt nicht mit.
 */
class SprechEnde(
    /** So lange nach dem Start wird nichts gewertet: Hier läuft der Piepton. */
    private val anlaufMs: Long = 250,
    /** Kommt bis hierhin keine Sprache, war es ein Fehlalarm oder Frank hat es sich anders überlegt. */
    private val wartenMs: Long = 7_000,
    /** So lange Stille nach dem letzten Wort beendet die Aufnahme. Großzügig, damit Frank mitten im Satz nachdenken kann. */
    private val stilleMs: Long = 4_500,
    /** Höchstdauer einer Frage. */
    private val hoechstMs: Long = 120_000,
) {
    enum class Lage { WEITER, FERTIG, NICHTS_GEHOERT }

    /** Grundrauschen; null bis zum ersten Wert nach dem Anlauf. */
    private var boden: Float? = null
    var gesprochen = false
        private set
    /** Wann (ms seit Start) zuletzt Sprache kam; für eine vorgezogene Mitschrift während der Nachdenkpause. */
    var letzteSpracheMs = 0L
        private set

    /** Ein neuer Pegelwert, [ms] seit dem Start der Aufnahme. */
    fun neu(pegel: Float, ms: Long): Lage {
        if (ms >= hoechstMs) return if (gesprochen) Lage.FERTIG else Lage.NICHTS_GEHOERT
        if (ms < anlaufMs) return Lage.WEITER
        // Boden fällt sofort auf leisere Werte und steigt nur langsam (über einige Sekunden) nach. Spricht Frank
        // schon beim ersten Wert, fällt er in der ersten Wortpause auf das echte Rauschen.
        val alt = boden
        boden = if (alt == null || pegel < alt) pegel else alt + (pegel - alt) * BODEN_ANSTIEG
        if (pegel > schwelle()) {
            gesprochen = true
            letzteSpracheMs = ms
        }
        if (!gesprochen) return if (ms >= wartenMs) Lage.NICHTS_GEHOERT else Lage.WEITER
        return if (ms - letzteSpracheMs >= stilleMs) Lage.FERTIG else Lage.WEITER
    }

    internal fun schwelle(): Float = max(MIN_SCHWELLE, (boden ?: 0f) * 1.5f + 0.05f)

    companion object {
        /** Darunter gilt nichts als Sprache, auch in sehr stiller Umgebung. */
        private const val MIN_SCHWELLE = 0.15f
        private const val BODEN_ANSTIEG = 0.01f
    }
}
