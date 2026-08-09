package de.frank.experimente.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Wie fordernd ein Experiment ist. Die KI dosiert aus der Chronik (F-03). */
enum class Stufe(val anzeige: String) {
    LEICHT("leicht"),
    MITTEL("mittel"),
    FORDERND("fordernd"),
}

/** Lebenslauf eines Experiments (01-FUNKTIONS-SPEC §4). */
enum class ExperimentZustand {
    OFFEN,
    ABGESCHLOSSEN,
    NICHT_UMGESETZT,
}

/** Woher ein Merklisten-Eintrag kommt. */
enum class MerkQuelle {
    GEMERKT,
    EIGEN,
    NICHT_UMGESETZT,
}

/** Wer in einem Gespräch spricht. */
enum class Sprecher {
    ICH,
    KI,
}

// ---------------------------------------------------------------------------
// Die zehn Einheiten aus 01-FUNKTIONS-SPEC.md §3
// ---------------------------------------------------------------------------

/** Das Selbstbild — genau ein Satz, `id` ist immer 1. */
@Entity(tableName = "selbstbild")
data class SelbstbildSatz(
    @PrimaryKey val id: Int = 1,
    val text: String = "",
    val geaendertAm: Instant = Instant.EPOCH,
)

/** Wünsche & Ziele (F-20). */
@Entity(tableName = "ziel")
data class Ziel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val angelegtAm: Instant,
    val geaendertAm: Instant,
)

/**
 * Ein Tag im Logbuch. Genau eines von [ausfuehrlich] / [verdichtet] ist gesetzt:
 * die ersten 15 Tage ausführlich, danach verdichtet (F-15).
 */
@Entity(tableName = "logtag")
data class LogTag(
    @PrimaryKey val datum: LocalDate,
    val ausfuehrlich: String? = null,
    val verdichtet: String? = null,
    val verdichtetAm: Instant? = null,
)

/** Ein Vorschlag des Tages (F-03). */
@Entity(tableName = "vorschlag")
data class Vorschlag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: LocalDate,
    val titel: String,
    val beschreibung: String,
    val tage: Int = 1,
    val stufe: Stufe,
    val vonMerkliste: Boolean = false,
    /** Aufgaben je Tag als JSON: `[["Tag1-Aufgabe1", …], ["Tag2-…"], …]` */
    val aufgabenJson: String,
    val verworfenAm: Instant? = null,
)

/** Ein laufendes oder abgeschlossenes Experiment (F-06, F-13). */
@Entity(tableName = "experiment")
data class Experiment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val beschreibung: String,
    val tage: Int = 1,
    val stufe: Stufe,
    val begonnenAm: LocalDate,
    val zustand: ExperimentZustand = ExperimentZustand.OFFEN,
    val beendetAm: LocalDate? = null,
)

/** Eine Aufgabe eines Experiments (F-07, F-08). */
@Entity(tableName = "aufgabe")
data class Aufgabe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    /** 1 = erster Tag des Experiments. */
    val tagNummer: Int = 1,
    val text: String,
    val erledigtAm: Instant? = null,
    val reihenfolge: Int,
)

/** Die Auswertung eines Tages zu einem Experiment (F-10, F-11). */
@Entity(tableName = "auswertung")
data class Auswertung(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val datum: LocalDate,
    /** Franks eigene Worte — Pflicht, nie durch die Haken ersetzt. */
    val eigenerText: String,
    /** Die Sicht der KI. Am Zwischentag kurz, am letzten Tag vollständig. */
    val kiText: String? = null,
    /** true am letzten Tag eines Experiments. */
    val abschliessend: Boolean = false,
)

/** Eine Runde im Gespräch zu einem Experiment (F-09). */
@Entity(tableName = "gespraech")
data class GespraechsRunde(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val sprecher: Sprecher,
    val text: String,
    val angelegtAm: Instant,
)

/** Ein Eintrag der Merkliste (F-05, F-18). */
@Entity(tableName = "merkliste")
data class MerkEintrag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val beschreibung: String,
    val tage: Int = 1,
    val stufe: Stufe,
    val aufgabenJson: String,
    val quelle: MerkQuelle,
    /** Was beim letzten Mal im Weg stand — nur bei [MerkQuelle.NICHT_UMGESETZT]. */
    val vermerk: String? = null,
    val angelegtAm: Instant,
)

/** Eine Erkenntnis (F-17). */
@Entity(tableName = "erkenntnis")
data class Erkenntnis(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val geaendertAm: Instant,
)

/** Die heutige Lage, bevor daraus Vorschläge werden (F-01). */
@Entity(tableName = "lage")
data class Lage(
    @PrimaryKey val datum: LocalDate,
    val text: String,
    val angelegtAm: Instant,
)
