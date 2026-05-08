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
 * Zeit-Buckets fuer Aufgaben (Frank-Wunsch 2026-05-09):
 *  - HEUTE: erscheint im HEUTE-Bucket, max 5 Eintraege sichtbar
 *  - MORGEN: erscheint morgen automatisch im HEUTE (Tag-Rollover beim App-Start)
 *  - FREIBLOCK: naechster freier Schicht-Block — passend fuer Schichtdienst
 *  - SPAETER: kein konkretes Datum, sammelt alles was nicht dringend ist
 *
 * Frank kann jeden Eintrag manuell einem Bucket zuweisen (manualBucket-Feld
 * in EntropyEntryEntity); ohne manuellen Override entscheidet die KI auf Basis
 * von priorityScore + Schichtkalender + geschaetzter Dauer.
 */
enum class TimeBucket { HEUTE, MORGEN, FREIBLOCK, SPAETER }

enum class EntrySource { NUTZER_MIC, NUTZER_TEXT, SHARE_SHEET, KI_ERKANNT, BIOMARKER_AUTO }

enum class MemorySource { MANUELL, KI_VORSCHLAG, AUS_PROFIL }

enum class ScientistRole { KI, NUTZER }

enum class HypothesisStatus { VORGESCHLAGEN, AKTIV, ABGEBROCHEN, ABGESCHLOSSEN }

enum class HypothesisOutcome { ERFOLGREICH, TEILWEISE_ERFOLGREICH, ERFOLGLOS, UNKLAR }

enum class StackType { MORGEN, ABEND, PRE_SPORT, SENOLYTIKA, SONDER }

enum class ShiftCode { TAGDIENST, NACHTDIENST, FREI, URLAUB, UNBEKANNT }
