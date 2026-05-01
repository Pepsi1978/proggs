package com.entropyjournal.ui.components.energyboard

import androidx.compose.ui.graphics.Color

/**
 * Arctic-Lightning Farbpalette — kaltes, blaues elektrisches Hochspannungs-Look.
 * Kern fast weiss (Schwarzkoerperstrahlungs-Approximation echter elektrischer Boegen),
 * Halo Neon-Cyan, Aussensaum tiefes Elektrik-Blau, Violett als Akzent fuer Sub-Verzweigungen.
 */
object EnergyTheme {
    // Kern aller hellen Strukturen — Eis-Weiss mit minimalem Blau-Stich
    val Core = Color(0xFFEAF7FF)

    // Erste Glow-Schicht — Neon-Cyan (sichtbarste Elektrizitaets-Farbe)
    val GlowInner = Color(0xFF00E5FF)

    // Zweite Glow-Schicht — Elektrik-Blau (mittlere Reichweite)
    val GlowMid = Color(0xFF0288D1)

    // Aussere Halo — tiefes Nachtblau, sehr weicher Schein
    val GlowOuter = Color(0x55003B7A)

    // Spitze des wandernden Energie-Pakets — heller als Cyan, fast weiss
    val PacketPeak = Color(0xFFB3F0FF)

    // Akzent fuer Verzweigungen / Quer-Blitze (Violett-Hauch wie echte Plasma-Boegen)
    val Accent = Color(0xFF7C4DFF)

    // Light-Mode-Variante (gedaempfter — Hintergrund hell, sonst zu unsichtbar)
    val CoreLight = Color(0xFF0277BD)
    val GlowInnerLight = Color(0xFF0288D1)
    val GlowMidLight = Color(0xFF01579B)
    val GlowOuterLight = Color(0x4001579B)
    val PacketPeakLight = Color(0xFF039BE5)
    val AccentLight = Color(0xFF512DA8)

    // Animation-Konstanten — V2 deutlich aggressiver fuer "richtigen Strom"
    const val PACKET_DURATION_MS = 1100L      // Schnelleres Wandern (war 2400)
    const val MAX_SPARKS = 80                 // Deutlich mehr (war 30)
    const val SPARK_LIFETIME_MS_MIN = 180L
    const val SPARK_LIFETIME_MS_MAX = 380L
    const val SPARK_SPAWN_INTERVAL_MS = 35L   // Viel schneller (war 90)
    const val FOCUS_FALLOFF_PX = 320f
    const val ARC_LIFETIME_MS = 220L          // Quer-Blitze: kurze gezackte Mini-Bogen
    const val ARC_SPAWN_INTERVAL_MS = 220L    // Wie oft ein Quer-Blitz entsteht
    const val LINE_JITTER_PX = 6f             // Wie stark die Hauptlinie zickzackt
    const val LINE_SEGMENT_COUNT = 24         // In wieviele Segmente die Linie unterteilt wird
}
