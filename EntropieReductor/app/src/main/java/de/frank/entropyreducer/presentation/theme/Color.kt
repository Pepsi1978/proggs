package de.frank.entropyreducer.presentation.theme

import androidx.compose.ui.graphics.Color
import de.frank.entropyreducer.domain.model.EntropyCategory

/** Neon Cosmos Palette — siehe Spec §3 und Referenzbilder 11-30. */
object CosmosColors {
    // Hintergruende — Dark
    val BgDark = Color(0xFF0A0E1A)
    val BgDarkMid = Color(0xFF0F1729)
    val BgDarkAccent = Color(0xFF1A1F38)

    // Hintergruende — Light (Off-White mit dezenter Galaxy-Andeutung)
    val BgLight = Color(0xFFF5F7FB)
    val BgLightMid = Color(0xFFE8EDFA)
    val BgLightAccent = Color(0xFFDCE3F5)

    // Glas-Flaechen
    val GlassDark = Color(0x14FFFFFF)             // weisses Overlay alpha 0.08
    val GlassDarkBorder = Color(0x29FFFFFF)       // weisses Overlay alpha 0.16 für 1dp Border
    val GlassLight = Color(0xCCFFFFFF)            // weiss alpha 0.80
    val GlassLightBorder = Color(0x14000000)      // schwarz alpha 0.08

    // Akzente (gleich in beiden Modi)
    val AccentPrimary = Color(0xFF22D3EE)         // Cyan
    val AccentSecondary = Color(0xFFA78BFA)       // Violett
    val Success = Color(0xFF34D399)               // Mintgruen
    val Warning = Color(0xFFFBBF24)               // Bernstein
    val Critical = Color(0xFFF87171)              // Korallrot

    // Text
    val TextPrimaryDark = Color(0xFFF8FAFC)
    val TextSecondaryDark = Color(0xFF94A3B8)
    val TextPrimaryLight = Color(0xFF0F172A)
    val TextSecondaryLight = Color(0xFF475569)

    // Statusbalken-Stops (von 0% nach 100%)
    val StatusRed = Color(0xFFF87171)             // 0-20
    val StatusOrange = Color(0xFFFB923C)          // 20-40
    val StatusYellow = Color(0xFFFBBF24)          // 40-60
    val StatusLightGreen = Color(0xFF86EFAC)      // 60-80
    val StatusGreen = Color(0xFF22C55E)           // 80-100

    // Prioritaets-Farbskala fuer die Prio-Zahl auf den Aufgabenkarten.
    // Hohe Werte = wichtig = warm (Rot/Orange), niedrige Werte = unwichtig = kuehl (Blau/Gruen).
    // Hinweis: Diese Skala ist BEWUSST anders herum als StatusRed/Green oben — beim Schweregrad
    // ist Rot schlecht, bei der Prio-Zahl ist Rot wichtig.
    val PriorityRed = Color(0xFFEF4444)           // 80-100  (sehr wichtig)
    val PriorityOrange = Color(0xFFFB923C)        // 60-80
    val PriorityYellow = Color(0xFFFBBF24)        // 40-60
    val PriorityBlue = Color(0xFF60A5FA)          // 20-40
    val PriorityGreen = Color(0xFF22C55E)         // 0-20    (geringste Prio)

    // Kategorie-Farben
    val CatPhysical = Color(0xFFF87171)
    val CatMental = Color(0xFFA78BFA)
    val CatTemporal = Color(0xFFFBBF24)
    val CatEmotional = Color(0xFFF472B6)
    val CatHealth = Color(0xFF34D399)
    val CatEnvironment = Color(0xFF22D3EE)
    val CatOther = Color(0xFF94A3B8)
}

/** Liefert die Akzentfarbe einer Kategorie — für Pillen und Highlights. */
fun EntropyCategory.color(): Color = when (this) {
    EntropyCategory.KOERPERLICH -> CosmosColors.CatPhysical
    EntropyCategory.MENTAL -> CosmosColors.CatMental
    EntropyCategory.ZEITLICH -> CosmosColors.CatTemporal
    EntropyCategory.EMOTIONAL -> CosmosColors.CatEmotional
    EntropyCategory.GESUNDHEITLICH -> CosmosColors.CatHealth
    EntropyCategory.UMGEBUNG -> CosmosColors.CatEnvironment
    EntropyCategory.SONSTIGES -> CosmosColors.CatOther
}

/** Lokalisiertes Anzeige-Label. */
fun EntropyCategory.label(): String = when (this) {
    EntropyCategory.KOERPERLICH -> "Koerperlich"
    EntropyCategory.MENTAL -> "Mental"
    EntropyCategory.ZEITLICH -> "Zeitlich"
    EntropyCategory.EMOTIONAL -> "Emotional"
    EntropyCategory.GESUNDHEITLICH -> "Gesundheitlich"
    EntropyCategory.UMGEBUNG -> "Umgebung"
    EntropyCategory.SONSTIGES -> "Sonstiges"
}
