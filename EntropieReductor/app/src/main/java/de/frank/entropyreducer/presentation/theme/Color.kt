package de.frank.entropyreducer.presentation.theme

import androidx.compose.ui.graphics.Color
import de.frank.entropyreducer.domain.model.EntropyCategory

/**
 * "Glut"-Palette (2026-06-12) — warmes, tiefes Anthrazit + Orange-Glut. Quelle der Wahrheit:
 * docs/mockup-neues-design.html (1:1-Vorlage, Hell + Dunkel). Ersetzt die alte "Neon
 * Cosmos"-Palette (Galaxy-Navy + Cyan).
 *
 * WICHTIG: Akzente sind jetzt THEME-ABHAENGIG (Dark heller, Light tiefer) — in Composables IMMER
 * ueber LocalCosmos.current.accent/accentTasks/... zugreifen. Die statischen Werte hier sind die
 * DARK-Varianten (+ explizite *Light-Pendants) fuer Nicht-Compose-Kontexte (Widget, Notifications).
 */
object CosmosColors {
    // Hintergruende — Dark (warmes Anthrazit; Mockup --bg / card2 / --bg2)
    val BgDark = Color(0xFF0B0A08)
    val BgDarkMid = Color(0xFF12100D) // Screen-Hintergrund
    val BgDarkAccent = Color(0xFF26211B) // Dialoge / Sheets (Mockup card2)
    val BgDarkBar = Color(0xFF191613) // BottomBar / erhoehte Flaechen (Mockup bg2)

    // Hintergruende — Light (warmes Papier-Weiss)
    val BgLight = Color(0xFFFAF7F3) // Screen-Hintergrund
    val BgLightMid = Color(0xFFF3EFE9)
    val BgLightAccent = Color(0xFFF1ECE5) // BottomBar / erhoehte Flaechen (Mockup bg2)

    // Karten — solide und warm statt "Glas" (Namen bleiben fuer Kompatibilitaet)
    val GlassDark = Color(0xFF1D1A16) // Mockup --card dark
    val GlassDarkBorder = Color(0x16FFF0E0) // warmweisser 1dp-Rand, alpha ~0.085
    val GlassLight = Color(0xFFFFFFFF) // Mockup --card light: reines Weiss
    val GlassLightBorder = Color(0x171E1812) // warmschwarzer Rand, alpha ~0.09

    // Akzente — DARK-Werte (Light-Pendants darunter; theme-aware via LocalCosmos)
    val AccentPrimary = Color(0xFFF97316) // Orange-Glut (Primaer)
    val AccentSecondary = Color(0xFFA78BFA) // Violett (Forscher/KI)
    val Success = Color(0xFF34C77B) // Smaragd
    val Warning = Color(0xFFFBBF24) // Bernstein
    val Critical = Color(0xFFF87171) // Korallrot

    // Akzente — LIGHT-Pendants (Mockup [data-theme=light])
    val AccentPrimaryLight = Color(0xFFEA580C)
    val AccentSecondaryLight = Color(0xFF7C3AED)
    val SuccessLight = Color(0xFF0F9D58)
    val WarningLight = Color(0xFFD97706)
    val CriticalLight = Color(0xFFDC2626)

    // Tab-Farbklassen (Bottom-Bar Sub-Modi) — Dark / Light
    val TabTasks = Color(0xFFF97316)
    val TabTasksLight = Color(0xFFEA580C)
    val TabTasksSub = Color(0xFF60A5FA)     // Blau — Sub-Tabs unter Aufgaben
    val TabTasksSubLight = Color(0xFF3B82F6) // Blau (Light)
    val TabAnalyse = Color(0xFF34C77B)
    val TabAnalyseLight = Color(0xFF0F9D58)
    val TabForscher = Color(0xFFA78BFA)
    val TabForscherLight = Color(0xFF7C3AED)
    val TabBio = Color(0xFFFB7185)
    val TabBioLight = Color(0xFFE11D48)

    // Offizielle WHOOP-Recovery-Farben (Frank-Wunsch 2026-06-01). Kraeftiger als die
    // Cosmos-Pastelltoene oben — bewusst die Original-Whoop-Toene fuer alle
    // Erholungs-/Recovery-Anzeigen. Schwellen laut WHOOP: gruen 67-100 %, gelb 34-66 %,
    // rot 0-33 %. Siehe whoopRecoveryColor() unten. FUNKTIONAL — nicht Teil von Glut.
    val WhoopRecoveryGreen = Color(0xFF16EC06)
    val WhoopRecoveryYellow = Color(0xFFFFDE00)
    val WhoopRecoveryRed = Color(0xFFFF0026)

    // Text — warme Toene statt kaltem Blaugrau
    val TextPrimaryDark = Color(0xFFF5F0E8)
    val TextSecondaryDark = Color(0xFFA89F93)
    val TextPrimaryLight = Color(0xFF221C15)
    val TextSecondaryLight = Color(0xFF6F665B)

    // Statusbalken-Stops (von 0% nach 100%) — FUNKTIONAL, unveraendert
    val StatusRed = Color(0xFFF87171) // 0-20
    val StatusOrange = Color(0xFFFB923C) // 20-40
    val StatusYellow = Color(0xFFFBBF24) // 40-60
    val StatusLightGreen = Color(0xFF86EFAC) // 60-80
    val StatusGreen = Color(0xFF22C55E) // 80-100

    // Prioritaets-Farbskala fuer die Prio-Zahl auf den Aufgabenkarten.
    // Hohe Werte = wichtig = warm (Rot/Orange), niedrige Werte = unwichtig = kuehl (Gruen/Blau).
    // Hinweis: Diese Skala ist BEWUSST anders herum als StatusRed/Green oben — beim Schweregrad
    // ist Rot schlecht, bei der Prio-Zahl ist Rot wichtig.
    // Frank-Wunsch 2026-05-10: geringste Prio ist Blau (kuehlste Farbe), Gruen liegt davor.
    // FUNKTIONAL, unveraendert.
    val PriorityRed = Color(0xFFEF4444) // 80-100  (sehr wichtig)
    val PriorityOrange = Color(0xFFFB923C) // 60-80
    val PriorityYellow = Color(0xFFFBBF24) // 40-60
    val PriorityGreen = Color(0xFF22C55E) // 20-40
    val PriorityBlue = Color(0xFF60A5FA) // 0-20    (geringste Prio)

    // Bucket-Toenung fuer Aufgabenkarten (Frank-Wunsch 2026-05-10).
    // Volle Saettigung — die Alpha wird beim Anwenden gesetzt (siehe bucketCardTint
    // in TasksScreen.kt), damit die Toenung in beiden Modi nur "ganz leicht" ist.
    //  - HEUTE      = Rosa/Pink (Frank-Aenderung 2026-05-10: vorher Orange,
    //                 unterschied sich aber zu wenig vom Gelb der Morgen-Karten;
    //                 Rosa bleibt warm/dringend, ist aber klar abgegrenzt)
    //  - MORGEN     = Gelb (vorbereitend)
    //  - FREIBLOCK  = Gruen (Schichtblock, ruhig)
    //  - SPAETER    = Blau (kuehl, kein Datum)
    // FUNKTIONAL, unveraendert.
    val BucketHeuteTint = Color(0xFFF472B6) // Tailwind pink-400 — klar von Gelb unterschieden
    val BucketMorgenTint = Color(0xFFFBBF24)
    val BucketFreiblockTint = Color(0xFF22C55E)
    val BucketSpaeterTint = Color(0xFF60A5FA)

    // Kategorie-Farben — FUNKTIONAL, unveraendert
    val CatPhysical = Color(0xFFF87171)
    val CatMental = Color(0xFFA78BFA)
    val CatTemporal = Color(0xFFFBBF24)
    val CatEmotional = Color(0xFFF472B6)
    val CatHealth = Color(0xFF34D399)
    val CatEnvironment = Color(0xFF22D3EE)
    val CatOther = Color(0xFF94A3B8)
}

/**
 * Offizielle WHOOP-Recovery-Ampel (Frank-Wunsch 2026-06-01). Eine einzige Quelle der Wahrheit fuer
 * ALLE Recovery-/Erholungs-Faerbungen (Gesamterholung-Ring, Erholungsverlauf, Detail-Werte,
 * Historie). Schwellen exakt nach WHOOP:
 * - 67-100 % → Gruen
 * - 34-66 % → Gelb
 * - 0-33 % → Rot
 */
fun whoopRecoveryColor(percent: Double): Color =
    when {
        percent >= 67.0 -> CosmosColors.WhoopRecoveryGreen
        percent >= 34.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryRed
    }

/** Liefert die Akzentfarbe einer Kategorie — für Pillen und Highlights. */
fun EntropyCategory.color(): Color =
    when (this) {
        EntropyCategory.KOERPERLICH -> CosmosColors.CatPhysical
        EntropyCategory.MENTAL -> CosmosColors.CatMental
        EntropyCategory.ZEITLICH -> CosmosColors.CatTemporal
        EntropyCategory.EMOTIONAL -> CosmosColors.CatEmotional
        EntropyCategory.GESUNDHEITLICH -> CosmosColors.CatHealth
        EntropyCategory.UMGEBUNG -> CosmosColors.CatEnvironment
        EntropyCategory.SONSTIGES -> CosmosColors.CatOther
    }

/** Lokalisiertes Anzeige-Label. */
fun EntropyCategory.label(): String =
    when (this) {
        EntropyCategory.KOERPERLICH -> "Körperlich"
        EntropyCategory.MENTAL -> "Mental"
        EntropyCategory.ZEITLICH -> "Zeitlich"
        EntropyCategory.EMOTIONAL -> "Emotional"
        EntropyCategory.GESUNDHEITLICH -> "Gesundheitlich"
        EntropyCategory.UMGEBUNG -> "Umgebung"
        EntropyCategory.SONSTIGES -> "Sonstiges"
    }
