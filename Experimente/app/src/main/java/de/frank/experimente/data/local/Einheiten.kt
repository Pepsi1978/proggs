package de.frank.experimente.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Das Datenmodell aus `Specs/Experimente/v2/01-FUNKTIONS-SPEC.md` §3.
 *
 * Feldnamen, Typen, Pflichtfelder und Standardwerte exakt wie spezifiziert.
 */

/** Stufe eines Experiments — sichtbar auf jeder Karte (F-03). */
enum class Stufe { LEICHT, MITTEL, FORDERND }

/** F-13: umgesetzt, nicht umgesetzt, oder noch offen. */
enum class ExperimentZustand { OFFEN, ABGESCHLOSSEN, NICHT_UMGESETZT }

/** Wer im Gespräch spricht (F-09). */
enum class Rolle { ICH, KI }

/** Woher ein Merklisten-Eintrag kommt (F-05, F-18, F-13). */
enum class Quelle { GEMERKT, EIGEN, NICHT_UMGESETZT }

/** Das Selbstbild — genau ein Satz, `id` ist immer 1 (F-21). */
@Entity(tableName = "selbstbild")
data class SelfImage(
    @PrimaryKey val id: Int = 1,
    val text: String = "",
    val updatedAt: Instant,
)

/** Wünsche & Ziele (F-20). Gehen als voller Text in jede Vorschlagsanfrage ein. */
@Entity(tableName = "ziele")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Ein Tag im Logbuch (F-14, F-15).
 *
 * Genau eines von `detailText` / `compactText` ist gesetzt: ausführlich solange der Tag
 * jünger als 15 Tage ist, danach verdichtet. Er fällt nie heraus.
 */
@Entity(tableName = "logtage")
data class LogDay(
    @PrimaryKey val date: LocalDate,
    val detailText: String? = null,
    val compactText: String? = null,
    val compactedAt: Instant? = null,
)

/** Ein Vorschlag des Tages (F-03, F-04). */
@Entity(tableName = "vorschlaege")
data class Suggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val description: String,
    val days: Int = 1,
    val level: Stufe,
    val fromWatchlist: Boolean = false,
    val tasksJson: String,
    val discardedAt: Instant? = null,
)

/** Ein laufendes oder abgeschlossenes Experiment (F-06, F-13). */
@Entity(tableName = "experimente")
data class Experiment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val days: Int = 1,
    val level: Stufe,
    val startedAt: LocalDate,
    val state: ExperimentZustand = ExperimentZustand.OFFEN,
    val closedAt: LocalDate? = null,
)

/**
 * Eine Aufgabe eines Experiments (F-07, F-08).
 *
 * `order` heißt in SQL anders, weil `ORDER` ein Schlüsselwort ist — der Spec-Name bleibt
 * am Kotlin-Feld erhalten.
 */
@Entity(tableName = "aufgaben")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val dayIndex: Int = 1,
    val text: String,
    val doneAt: Instant? = null,
    @ColumnInfo(name = "order_index") val order: Int,
)

/** Die Auswertung eines Tages zu einem Experiment (F-10, F-11). */
@Entity(tableName = "auswertungen")
data class Evaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val date: LocalDate,
    val ownText: String,
    val aiText: String? = null,
    val isFinal: Boolean = false,
)

/** Eine Runde im Gespräch (F-09). Der Faden gehört genau einem Experiment. */
@Entity(tableName = "gespraech")
data class ChatTurn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val role: Rolle,
    val text: String,
    val createdAt: Instant,
)

/** Ein Eintrag der Merkliste (F-05, F-18, F-19). */
@Entity(tableName = "merkliste")
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val days: Int = 1,
    val level: Stufe,
    val tasksJson: String,
    val source: Quelle,
    val note: String? = null,
    val createdAt: Instant,
)

/** Eine Erkenntnis (F-17). Trägt das Datum ihrer letzten Änderung. */
@Entity(tableName = "erkenntnisse")
data class Insight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val updatedAt: Instant,
)

/**
 * Die heutige Lage (F-01).
 *
 * **Lücke im Funktions-Spec:** §3 listet zehn Einheiten, F-01 schreibt aber eine
 * `SituationEntry`, die dort nicht steht. Sie wird hier als elfte Einheit angelegt, weil
 * F-03 ohne sie keinen Kontext hat. Das Spec selbst wurde nicht geändert — die Lücke steht
 * im Logbuch (Lauf 01, V-36).
 *
 * Pro Tag genau eine; eine erneute Eingabe am selben Tag ersetzt die vorige.
 */
@Entity(tableName = "lage")
data class Lage(
    @PrimaryKey val date: LocalDate,
    val text: String,
    val createdAt: Instant,
)
