package com.entropyjournal.ui.components.energyboard

import androidx.compose.ui.graphics.Color

/**
 * Arctic-Lightning Farbpalette — kaltes, blaues elektrisches Hochspannungs-Look.
 */
object EnergyTheme {
    // Kern aller hellen Strukturen — Eis-Weiss mit minimalem Blau-Stich
    val Core = Color(0xFFEAF7FF)

    // Erste Glow-Schicht — Neon-Cyan
    val GlowInner = Color(0xFF00E5FF)

    // Zweite Glow-Schicht — Elektrik-Blau
    val GlowMid = Color(0xFF0288D1)

    // Aussere Halo — tiefes Nachtblau
    val GlowOuter = Color(0x55003B7A)

    // Spitze des wandernden Energie-Pakets
    val PacketPeak = Color(0xFFB3F0FF)

    // Akzent fuer Verzweigungen / Quer-Blitze (Violett-Hauch)
    val Accent = Color(0xFF7C4DFF)

    // Zarte Innen-Fluess fuer aktive Karte (sehr transparent — nur leichte Tönung)
    val CardWash = Color(0x1A00E5FF)

    // Light-Mode-Variante
    val CoreLight = Color(0xFF0277BD)
    val GlowInnerLight = Color(0xFF0288D1)
    val GlowMidLight = Color(0xFF01579B)
    val GlowOuterLight = Color(0x4001579B)
    val PacketPeakLight = Color(0xFF039BE5)
    val AccentLight = Color(0xFF512DA8)
    val CardWashLight = Color(0x140288D1)

    // Animation-Konstanten
    const val PACKET_DURATION_MS = 1100L
    const val MAX_SPARKS = 80
    const val SPARK_LIFETIME_MS_MIN = 180L
    const val SPARK_LIFETIME_MS_MAX = 380L
    const val SPARK_SPAWN_INTERVAL_MS = 35L
    const val FOCUS_FALLOFF_PX = 320f

    // Quer-Blitze: kurze gezackte Mini-Boegen die aus der Schiene zucken
    const val ARC_LIFETIME_MS = 220L
    const val ARC_SPAWN_INTERVAL_MS = 180L

    // Hauptlinie zackiger
    const val LINE_JITTER_PX = 8f
    const val LINE_SEGMENT_COUNT = 28

    // Lichtenberg-Pattern: fraktale Verzweigungen rund um die aktive Karte
    const val LICHTENBERG_LIFETIME_MS = 380L
    const val LICHTENBERG_SPAWN_INTERVAL_MS = 280L
    const val LICHTENBERG_BRANCHES_MIN = 5
    const val LICHTENBERG_BRANCHES_MAX = 9
    const val LICHTENBERG_DEPTH = 3        // Rekursionstiefe der Midpoint-Displacement
    const val LICHTENBERG_DISPLACE_PX = 22f // Maximaler Offset pro Mittelpunkt

    // Punkte (Dots auf der Schiene)
    const val DOT_OUTER_RADIUS_PX = 26f    // Aeusserer Glow-Radius (war 16)
    const val DOT_INNER_RADIUS_PX = 11f
    const val DOT_CORE_RADIUS_PX = 4.5f
    const val DOT_RAY_COUNT = 6            // Wieviele Strahlen ums Punkt zucken
    const val DOT_RAY_LENGTH_PX = 14f
}
