package de.frank.entropyreducer.data.local.entities

/**
 * Zentrale Herkunfts-Typ-Codes (`originType`) der ID-Architektur (Spec 2026-06-19 §3.1).
 *
 * Als String in `originType` gespeichert (Backup-stabil, kein Enum-Ordinal) — aber hier zentral
 * definiert, damit es EINE Quelle der Wahrheit gibt statt verstreuter String-Literale "IDEA"/
 * "TASK_SUGGESTION" (Tippfehler-Schutz). Die Kette laeuft: IDEA -> TASK_SUGGESTION/HABIT_SUGGESTION
 * -> TASK/HABIT; spaeter (Stufe 5) auch ENTROPY/THESE/JOURNAL -> IDEA.
 */
object OriginType {
    const val IDEA = "IDEA"
    const val TASK_SUGGESTION = "TASK_SUGGESTION"
    const val HABIT_SUGGESTION = "HABIT_SUGGESTION"
    const val TASK = "TASK"
    const val HABIT = "HABIT"
    const val ENTROPY = "ENTROPY"
    const val LEARNING = "LEARNING"
    const val THESE = "THESE"
    const val JOURNAL = "JOURNAL"

    // Backfill (Frank-Wunsch 2026-06-20): Mental-Saetze als Self-Root-Typ. Altbestand-Eintraege ohne
    // echte Herkunft werden „selbst-verwurzelt" (rootId=eigene id) — damit hat jeder Eintrag eine
    // gueltige ID statt herkunftslos zu sein.
    const val MENTAL = "MENTAL"
}
