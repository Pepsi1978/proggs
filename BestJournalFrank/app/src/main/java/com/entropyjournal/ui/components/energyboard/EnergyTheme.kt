package com.entropyjournal.ui.components.energyboard

import androidx.compose.ui.graphics.Color

/**
 * Cyber-Amber Farbpalette fuer das Energy Board.
 * Kern fast weiss (Schwarzkoerperstrahlungs-Approximation),
 * Halo Bernstein/Amber, passt zum Dark-Theme-Orange #D36B00.
 */
object EnergyTheme {
    // Kern der Stromlinie und Funken — fast weiss mit warmem Stich
    val Core = Color(0xFFFFF5E0)

    // Erste Glow-Schicht (sichtbarste Farbe)
    val GlowInner = Color(0xFFFFB300)

    // Zweite Glow-Schicht (mittlere Reichweite)
    val GlowMid = Color(0xFFD36B00)

    // Aussere Halo (sehr weicher Schein)
    val GlowOuter = Color(0x40FF8C00)

    // Farbe der wandernden Energie-Pakete (heller Spot auf der Linie)
    val PacketPeak = Color(0xFFFFD700)

    // Light-Mode-Variante (gedaempfter — Hintergrund ist weiss/hell)
    val CoreLight = Color(0xFFFFA000)
    val GlowInnerLight = Color(0xFFFF8F00)
    val GlowMidLight = Color(0xFFE65100)
    val GlowOuterLight = Color(0x30E65100)
    val PacketPeakLight = Color(0xFFFFB300)

    // Konstanten fuer Animation
    const val PACKET_DURATION_MS = 2400L      // Wie lange braucht ein Energie-Paket vom Anfang bis Ende
    const val PULSE_PERIOD_MS = 4000f         // Atemrhythmus der Punkte (4s Periode = ~0.25Hz)
    const val MAX_SPARKS = 30                 // Maximum gleichzeitig aktiver Funken
    const val SPARK_LIFETIME_MS_MIN = 200L
    const val SPARK_LIFETIME_MS_MAX = 450L
    const val SPARK_SPAWN_INTERVAL_MS = 90L   // Wie oft am Fokuspunkt ein Funken entsteht
    const val FOCUS_FALLOFF_PX = 320f         // Reichweite des Fokus-Glows um den fokussierten Punkt
}
