package de.frank.entropyreducer.domain.model

/**
 * Sieben kanonische Entropie-Kategorien (siehe Spec §1).
 * Standard-Prioritaetshierarchie:
 * KOERPERLICH > MENTAL > ZEITLICH > EMOTIONAL > GESUNDHEITLICH > UMGEBUNG > SONSTIGES
 */
enum class EntropyCategory {
    KOERPERLICH,
    MENTAL,
    ZEITLICH,
    EMOTIONAL,
    GESUNDHEITLICH,
    UMGEBUNG,
    SONSTIGES,
}

enum class EntryStatus { OFFEN, IN_ARBEIT, REDUZIERT, ARCHIVIERT }

/**
 * Persistierte Aufgaben-Buckets. Die Enum-Namen bleiben für Backup-/DB-Kompatibilität erhalten,
 * fachlich stehen sie im Aufgaben-Reiter seit 2026-07-07 für Prioritätsbereiche:
 *  - HEUTE: Sehr hoch, 80-100
 *  - MORGEN: Hoch, 60-79
 *  - FREIBLOCK: Mittel, 40-59
 *  - GERING: Gering, 20-39
 *  - SPAETER: später, 0-19
 */
enum class TimeBucket { HEUTE, MORGEN, FREIBLOCK, GERING, SPAETER }

fun priorityBucketForScore(score: Double): TimeBucket {
    val clamped = score.coerceIn(0.0, 100.0)
    return when {
        clamped >= 80.0 -> TimeBucket.HEUTE
        clamped >= 60.0 -> TimeBucket.MORGEN
        clamped >= 40.0 -> TimeBucket.FREIBLOCK
        clamped >= 20.0 -> TimeBucket.GERING
        else -> TimeBucket.SPAETER
    }
}

fun defaultPriorityForBucket(bucket: TimeBucket): Double =
    when (bucket) {
        TimeBucket.HEUTE -> 90.0
        TimeBucket.MORGEN -> 70.0
        TimeBucket.FREIBLOCK -> 50.0
        TimeBucket.GERING -> 30.0
        TimeBucket.SPAETER -> 10.0
    }

enum class EntrySource {
    NUTZER_MIC,
    NUTZER_TEXT,
    SHARE_SHEET,
    KI_ERKANNT,
    BIOMARKER_AUTO,
    /** Wiederkehrende Aufgabe — automatisch aus RecurringTemplate erzeugt (Sprint 2). */
    RECURRING_TEMPLATE,
}

enum class MemorySource { MANUELL, KI_VORSCHLAG, AUS_PROFIL }

enum class ScientistRole { KI, NUTZER }

enum class HypothesisStatus { VORGESCHLAGEN, AKTIV, ABGEBROCHEN, ABGESCHLOSSEN }

enum class HypothesisOutcome { ERFOLGREICH, TEILWEISE_ERFOLGREICH, ERFOLGLOS, UNKLAR }

enum class StackType { MORGEN, ABEND, PRE_SPORT, SENOLYTIKA, SONDER }

enum class ShiftCode { TAGDIENST, NACHTDIENST, FREI, URLAUB, UNBEKANNT }
