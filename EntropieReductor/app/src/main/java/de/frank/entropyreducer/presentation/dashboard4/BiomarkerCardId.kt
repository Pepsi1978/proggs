package de.frank.entropyreducer.presentation.dashboard4

/**
 * Stabile IDs aller verschiebbaren Karten im Biomarker-Screen.
 *
 * Frank-Wunsch 2026-05-10: Alle Daten-Karten im Biomarker-Bereich sollen per
 * Drag & Drop frei anordbar sein. Header (Sync-Info, Status-Bar, Datums-Auswahl)
 * bleiben fix oben — verschiebbar ist alles ab Gesamterholung abwaerts.
 *
 * Die Reihenfolge in [DEFAULT_ORDER] ist die Werks-Anordnung beim ersten Start
 * (entspricht dem bisherigen festen Layout). Der DataStore [BiomarkerCardOrder]
 * persistiert die individuelle Reihenfolge des Benutzers und ergaenzt automatisch
 * neue IDs am Ende, falls in spaeteren Versionen weitere Karten dazukommen.
 *
 * WICHTIG: Diese IDs sind STABILE Strings. Sie duerfen NIEMALS umbenannt werden,
 * sonst verlieren bestehende Benutzer ihre Reihenfolge. Neue Karten bekommen
 * neue IDs und werden hinzugefuegt — alte bleiben.
 */
object BiomarkerCardId {
    const val GESAMTERHOLUNG = "gesamterholung"

    // Mini-Cards (Frank-Wunsch 2026-05-10): einzelne Mini-Karten im 2-Spalten-Grid,
    // die unabhaengig voneinander verschoben werden koennen. Frueher waren sie zu
    // einem festen 2x2-Grid (KEY_VALUE_GRID) zusammengefasst — jetzt sind es vier
    // eigenstaendige Items mit span=1, die in einem LazyVerticalGrid liegen.
    const val MINI_HRV = "mini_hrv"
    const val MINI_RHR = "mini_rhr"
    const val MINI_SLEEP_TOTAL = "mini_sleep_total"
    const val MINI_SLEEP_PERFORMANCE = "mini_sleep_performance"

    // Herzfrequenz-Block
    const val HRV = "hrv"
    const val RHR = "rhr"

    // Koerper-Block (Atmung, Sauerstoff, Hauttemperatur)
    const val RESPIRATORY = "respiratory"
    const val SPO2 = "spo2"
    const val SKIN_TEMP = "skin_temp"
    const val SKIN_TEMP_DELTA = "skin_temp_delta"

    // Schlaf-Block
    const val SLEEP_PERFORMANCE = "sleep_performance"
    const val SLEEP_TOTAL = "sleep_total"
    const val SLEEP_STAGES = "sleep_stages"
    const val SLEEP_RESTORATIVE = "sleep_restorative"
    const val SLEEP_EFFICIENCY = "sleep_efficiency"
    const val SLEEP_CONSISTENCY = "sleep_consistency"
    const val SLEEP_DEBT = "sleep_debt"

    // Aktivitaet-Block
    const val KILOJOULES = "kilojoules"
    const val STRAIN = "strain"
    const val WORKOUTS_FOR_DAY = "workouts_for_day"

    // Analyse-Block
    const val CORRELATION = "correlation"

    // Amazfit-Block (T-Rex 3)
    const val AMAZFIT_LAST_HERO = "amazfit_last_hero"
    const val AMAZFIT_TRAININGS = "amazfit_trainings"

    /**
     * Werks-Reihenfolge — entspricht dem urspruenglichen Layout vor dem
     * Drag & Drop-Feature. Wird verwendet wenn der Benutzer noch keine
     * eigene Reihenfolge gespeichert hat oder "Reihenfolge zuruecksetzen"
     * waehlt.
     */
    /**
     * Set aller IDs die als kleine Mini-Karten im 2-Spalten-Grid gerendert werden
     * (span = 1). Alle anderen IDs nehmen die volle Breite ein (span = 2). Frank-
     * Wunsch 2026-05-10: HRV/RHR/Schlaf/Performance koennen sich auch untereinander
     * tauschen — und seit dem Mini-Karten-Umbau gehoeren auch Erholsamer Schlaf
     * und Hauttemperatur-Delta hier rein, damit sie als halbe Spalte (nicht volle
     * Breite) gerendert werden.
     */
    val MINI_CARD_IDS: Set<String> = setOf(
        MINI_HRV,
        MINI_RHR,
        MINI_SLEEP_TOTAL,
        MINI_SLEEP_PERFORMANCE,
        SLEEP_RESTORATIVE,
        SKIN_TEMP_DELTA,
    )

    val DEFAULT_ORDER: List<String> = listOf(
        GESAMTERHOLUNG,
        MINI_HRV,
        MINI_RHR,
        MINI_SLEEP_TOTAL,
        MINI_SLEEP_PERFORMANCE,
        HRV,
        RHR,
        RESPIRATORY,
        SPO2,
        SKIN_TEMP,
        SKIN_TEMP_DELTA,
        SLEEP_PERFORMANCE,
        SLEEP_TOTAL,
        SLEEP_STAGES,
        SLEEP_RESTORATIVE,
        SLEEP_EFFICIENCY,
        SLEEP_CONSISTENCY,
        SLEEP_DEBT,
        KILOJOULES,
        STRAIN,
        WORKOUTS_FOR_DAY,
        CORRELATION,
        AMAZFIT_LAST_HERO,
        AMAZFIT_TRAININGS,
    )
}
