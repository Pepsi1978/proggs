package de.frank.entropyreducer.presentation.dashboard4

/**
 * Stabile IDs aller verschiebbaren Karten im Biomarker-Screen.
 *
 * Frank-Wunsch 2026-05-10: Alle Daten-Karten im Biomarker-Bereich sollen per Drag & Drop frei
 * anordbar sein. Header (Sync-Info, Status-Bar, Datums-Auswahl) bleiben fix oben — verschiebbar ist
 * alles ab Gesamterholung abwaerts.
 *
 * Die Reihenfolge in [DEFAULT_ORDER] ist die Werks-Anordnung beim ersten Start (entspricht dem
 * bisherigen festen Layout). Der DataStore [BiomarkerCardOrder] persistiert die individuelle
 * Reihenfolge des Benutzers und ergaenzt automatisch neue IDs am Ende, falls in spaeteren Versionen
 * weitere Karten dazukommen.
 *
 * WICHTIG: Diese IDs sind STABILE Strings. Sie duerfen NIEMALS umbenannt werden, sonst verlieren
 * bestehende Benutzer ihre Reihenfolge. Neue Karten bekommen neue IDs und werden hinzugefuegt —
 * alte bleiben.
 */
object BiomarkerCardId {
    const val GESAMTERHOLUNG = "gesamterholung"
    // Frank-Wunsch 2026-05-16: Erholungs-Verlauf analog zum Tiefschlaf-Verlauf —
    // Balken-Graph der letzten 30 Tage Recovery-Score mit Ampel-Farben
    // (80-100 gruen, 60-79 gelb, <60 rot) und Bottom-Sheet mit Historie.
    const val RECOVERY_GRAPH = "recovery_graph"

    // Mini-Cards (Frank-Wunsch 2026-05-10): einzelne Mini-Karten im 2-Spalten-Grid,
    // die unabhaengig voneinander verschoben werden koennen. Frueher waren sie zu
    // einem festen 2x2-Grid (KEY_VALUE_GRID) zusammengefasst — jetzt sind es vier
    // eigenstaendige Items mit span=1, die in einem LazyVerticalGrid liegen.
    // Frank-Wunsch 2026-06-21: MINI_HRV ist jetzt eine volle Breite-Karte im
    // Erholungsverlauf-Pattern (Balken-Graph, Durchschnitt, Ampel-Faerbung).
    const val MINI_HRV = "mini_hrv"
    const val MINI_RHR = "mini_rhr"
    const val MINI_SLEEP_TOTAL = "mini_sleep_total"
    const val MINI_SLEEP_PERFORMANCE = "mini_sleep_performance"

    // Gewicht aus Health Connect (Zepp-Bridge, Frank-Wunsch 2026-05-10)
    const val MINI_WEIGHT = "mini_weight"
    // Koerperfett + Magermasse aus Health Connect (Frank-Wunsch 2026-05-10).
    // Smart-Scales liefern beides zusammen mit dem Gewicht.
    const val MINI_BODY_FAT = "mini_body_fat"
    const val MINI_LEAN_BODY_MASS = "mini_lean_body_mass"
    // Koerperwasser + Knochenmasse (Frank-Wunsch 2026-05-10, zweite Iteration —
    // analog zu "Muskelmasse" denkt: Health Connect hat keinen direkten
    // Muscle-Mass-Datentyp, aber Wasser und Knochen sind verfuegbar).
    const val MINI_BODY_WATER = "mini_body_water"
    const val MINI_BONE_MASS = "mini_bone_mass"
    // Muskelmasse: berechnet als LeanBodyMass - BoneMass (Annaeherung). Wenn
    // BoneMass null ist, fallback auf LeanBodyMass.
    const val MINI_MUSCLE_MASS = "mini_muscle_mass"

    // Herzfrequenz-Block
    const val HRV = "hrv"
    const val RHR = "rhr"

    // Frank-Wunsch 2026-05-18: VO2max als eigene Mini-Karte. Wert kommt nicht
    // aus dem Whoop-Snapshot, sondern wird im VM aus den juengsten VO2max-
    // faehigen Workouts (Laufen/Trail/Walk) berechnet — siehe vo2MaxHistory
    // im BiomarkerUiState und MiniVo2MaxCard im BiomarkerScreen.
    const val MINI_VO2MAX = "mini_vo2max"

    // Koerper-Block (Atmung, Sauerstoff, Hauttemperatur)
    const val RESPIRATORY = "respiratory"
    const val SPO2 = "spo2"
    const val SKIN_TEMP = "skin_temp"
    const val SKIN_TEMP_DELTA = "skin_temp_delta"

    // Schlaf-Block
    const val SLEEP_PERFORMANCE = "sleep_performance"
    const val SLEEP_TOTAL = "sleep_total"
    const val SLEEP_STAGES = "sleep_stages"
    // Frank-Wunsch 2026-05-13: Tiefschlaf-Verlauf als prozentualer Balken-Graph
    // direkt unter den Schlafphasen. Tap → Bottom-Sheet mit Historie + Vortag-Delta.
    const val SLEEP_DEEP_GRAPH = "sleep_deep_graph"
    // Frank-Wunsch 2026-05-23: REM-Schlaf-Verlauf — analog zu SLEEP_DEEP_GRAPH,
    // mit Ampel (0-15% rot, 15-20% gelb, >20% gruen). Liegt zwischen Tiefschlaf-
    // und Wachzeit-Verlauf.
    const val SLEEP_REM_GRAPH = "sleep_rem_graph"
    // Frank-Wunsch 2026-05-17: Wachzeit-Verlauf — analog zu SLEEP_DEEP_GRAPH,
    // aber mit invertierter Ampel (wenig Wachzeit = gruen). Direkt unter
    // dem Tiefschlaf-Verlauf platziert.
    const val SLEEP_WAKE_GRAPH = "sleep_wake_graph"
    const val SLEEP_RESTORATIVE = "sleep_restorative"
    const val SLEEP_RESTORATIVE_GRAPH = "sleep_restorative_graph"
    const val SLEEP_EFFICIENCY = "sleep_efficiency"
    const val SLEEP_CONSISTENCY = "sleep_consistency"
    const val SLEEP_DEBT = "sleep_debt"

    // Aktivitaet-Block
    const val KILOJOULES = "kilojoules"
    const val STRAIN = "strain"
    const val WORKOUTS_FOR_DAY = "workouts_for_day"

    // Analyse-Block
    // Frank-Wunsch 2026-06-21: HRV ↔ Schlafdauer-Korrelation entfernt — wird nicht mehr gebraucht.
    const val CORRELATION = "correlation"

    // Amazfit-Block (T-Rex 3)
    const val AMAZFIT_LAST_HERO = "amazfit_last_hero"
    const val AMAZFIT_TRAININGS = "amazfit_trainings"

    // Oura-Ring-Block (Frank-Wunsch 2026-05-10)
    const val OURA_READINESS = "oura_readiness"
    const val OURA_SLEEP_SCORE = "oura_sleep_score"
    const val OURA_ACTIVITY = "oura_activity"
    const val OURA_RESILIENCE = "oura_resilience"
    const val OURA_SLEEP_DETAIL = "oura_sleep_detail"

    /**
     * Werks-Reihenfolge — entspricht dem urspruenglichen Layout vor dem Drag & Drop-Feature. Wird
     * verwendet wenn der Benutzer noch keine eigene Reihenfolge gespeichert hat oder "Reihenfolge
     * zuruecksetzen" waehlt.
     */
    /**
     * Set aller IDs die als kleine Mini-Karten im 2-Spalten-Grid gerendert werden (span = 1). Alle
     * anderen IDs nehmen die volle Breite ein (span = 2). Frank- Wunsch 2026-05-10:
     * HRV/RHR/Schlaf/Performance koennen sich auch untereinander tauschen — und seit dem
     * Mini-Karten-Umbau gehoeren auch Erholsamer Schlaf und Hauttemperatur-Delta hier rein, damit
     * sie als halbe Spalte (nicht volle Breite) gerendert werden.
     */
    val MINI_CARD_IDS: Set<String> =
        setOf(
            MINI_RHR,
            // Frank-Wunsch 2026-08-07: MINI_SLEEP_PERFORMANCE ist jetzt eine volle
            // Breite-Karte im Erholungsverlauf-Pattern — daher NICHT mehr hier gelistet.
            MINI_SLEEP_TOTAL,
            MINI_VO2MAX,
            SLEEP_RESTORATIVE,
            SKIN_TEMP_DELTA,
            MINI_WEIGHT,
            MINI_BODY_FAT,
            MINI_LEAN_BODY_MASS,
            MINI_BODY_WATER,
            MINI_BONE_MASS,
            MINI_MUSCLE_MASS,
        )

    val DEFAULT_ORDER: List<String> =
        listOf(
            GESAMTERHOLUNG,
            RECOVERY_GRAPH,
            MINI_HRV,
            MINI_RHR,
            MINI_VO2MAX,
            MINI_SLEEP_TOTAL,
            MINI_SLEEP_PERFORMANCE,
            MINI_WEIGHT,
            MINI_BODY_FAT,
            MINI_LEAN_BODY_MASS,
            MINI_MUSCLE_MASS,
            // MINI_BODY_WATER + MINI_BONE_MASS bewusst NICHT mehr in der Werks-Reihenfolge —
            // Frank-Vorgabe 2026-05-10: 'Wasser und Knochen interessieren mich ueberhaupt nicht'.
            // Konstanten bleiben erhalten (Backward-Compat) — HIDDEN_CARD_IDS unten filtert
            // sie zusaetzlich aus bereits gespeicherten Drag&Drop-Reihenfolgen heraus.
            HRV,
            RHR,
            RESPIRATORY,
            SPO2,
            SKIN_TEMP,
            SKIN_TEMP_DELTA,
            SLEEP_PERFORMANCE,
            SLEEP_TOTAL,
            SLEEP_STAGES,
            SLEEP_DEEP_GRAPH,
            SLEEP_REM_GRAPH,
            SLEEP_WAKE_GRAPH,
            SLEEP_RESTORATIVE_GRAPH,
            SLEEP_RESTORATIVE,
            SLEEP_EFFICIENCY,
            SLEEP_CONSISTENCY,
            SLEEP_DEBT,
            KILOJOULES,
            STRAIN,
            // WORKOUTS_FOR_DAY: Frank-Vorgabe 2026-05-11 entfernt — die Whoop-Workouts
            // an diesem Tag sind im Biomarker-Screen redundant. Wird zusaetzlich in
            // HIDDEN_CARD_IDS gelistet damit es auch fuer bestehende User verschwindet.
            AMAZFIT_LAST_HERO,
            AMAZFIT_TRAININGS,
            OURA_READINESS,
            OURA_SLEEP_SCORE,
            OURA_RESILIENCE,
            // OURA_ACTIVITY bewusst NICHT mehr in der Werks-Reihenfolge —
            // Frank-Vorgabe 2026-05-10: 'brauche ich nicht da, vielen Dank'.
            // Konstante bleibt erhalten (Backward-Compat fuer User die die Karte
            // schon hatten). HIDDEN_CARD_IDS unten filtert sie zusaetzlich aus
            // bestehenden Drag&Drop-Reihenfolgen heraus, damit sie wirklich
            // ueberall verschwindet — auch bei Frank's bereits gespeicherter Order.
            // OURA_SLEEP_DETAIL bewusst NICHT mehr in der Werks-Reihenfolge —
            // Frank-Vorgabe 2026-05-10: vertraut fuer Schlafphasen nur Whoop.
            // Konstante bleibt erhalten (Backward-Compat fuer User die die Karte
            // schon hatten), wird aber bei Erstinstall nicht mehr angezeigt.
        )

    /**
     * Karten-IDs die der Screen ueberall ausblenden soll, auch wenn sie noch in der gespeicherten
     * Drag&Drop-Reihenfolge auftauchen. Frank-Vorgabe 2026-05-10: nachtraegliche Entfernung der
     * Oura-Activity- und Oura-SleepDetail-Karten ohne dass Frank manuell die Reihenfolge
     * zuruecksetzen muss.
     */
    val HIDDEN_CARD_IDS: Set<String> =
        setOf(
            OURA_ACTIVITY,
            OURA_SLEEP_DETAIL,
            MINI_BODY_WATER,
            MINI_BONE_MASS,
            WORKOUTS_FOR_DAY,
            CORRELATION,
        )
}
