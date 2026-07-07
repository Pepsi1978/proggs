package de.frank.entropyreducer.domain.usecase

/**
 * Frist-basierte Prio-Untergrenze (Frank-Wunsch 2026-05-23).
 *
 * Aus ProcessEntryUseCase als top-level Funktion ausgelagert, damit sie auch ohne Gemini-Call
 * direkt verwendet werden kann — wichtig fuer setDueDate(): die Prio muss SOFORT angepasst
 * werden wenn Frank die Frist aendert, unabhaengig vom optionalen KI-Rescore.
 *
 * Stufen:
 *  - ueberfaellig (dueAtMs < now)  → mindestens 98
 *  - weniger als 24h               → mindestens 95
 *  - 1-2 Tage Restzeit             → mindestens 85
 *  - 2-3 Tage Restzeit             → mindestens 75
 *  - 3-7 Tage Restzeit             → mindestens 65
 *  - mehr als 7 Tage / keine Frist → kein Floor, baseScore gilt
 */
fun computeDeadlineFloor(
    baseScore: Double,
    dueAtMs: Long?,
    nowMs: Long = System.currentTimeMillis(),
): Double {
    if (dueAtMs == null) return baseScore.coerceIn(0.0, 100.0)
    val remainingMs = dueAtMs - nowMs
    val hours = remainingMs / (60L * 60 * 1000)
    val floor =
        when {
            remainingMs < 0 -> 98.0
            hours < 24 -> 95.0
            hours < 48 -> 85.0
            hours < 72 -> 75.0
            hours < 24 * 7 -> 65.0
            else -> 0.0
        }
    return maxOf(baseScore, floor).coerceIn(0.0, 100.0)
}
