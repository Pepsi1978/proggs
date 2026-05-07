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

enum class TimeBucket { HEUTE, MORGEN, DIESE_WOCHE, DIESEN_MONAT, SPAETER }

enum class EntrySource { NUTZER_MIC, NUTZER_TEXT, SHARE_SHEET, KI_ERKANNT, BIOMARKER_AUTO }

enum class MemorySource { MANUELL, KI_VORSCHLAG, AUS_PROFIL }

enum class ScientistRole { KI, NUTZER }

enum class HypothesisStatus { VORGESCHLAGEN, AKTIV, ABGEBROCHEN, ABGESCHLOSSEN }

enum class HypothesisOutcome { ERFOLGREICH, TEILWEISE_ERFOLGREICH, ERFOLGLOS, UNKLAR }

enum class StackType { MORGEN, ABEND, PRE_SPORT, SENOLYTIKA, SONDER }

enum class ShiftCode { TAGDIENST, NACHTDIENST, FREI, URLAUB, UNBEKANNT }
