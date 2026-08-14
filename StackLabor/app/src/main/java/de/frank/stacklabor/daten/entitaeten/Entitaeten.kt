package de.frank.stacklabor.daten.entitaeten

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// ── Aufzaehlungen ───────────────────────────────────────────────────────────────

/** Loeslichkeit eines Mittels — bestimmt die Gruppierung in der Ansicht "Loeslichkeit" (F-06). */
enum class Loeslichkeit {
    WASSER,
    FETT,
    BEIDES,
}

/** Darreichungsform aus dem Katalog. Wird bei der Tagesgesamtdosis (F-13) nicht vermischt. */
enum class Darreichungsform {
    KAPSEL,
    TABLETTE,
    LOEFFEL,
    TASSE,
    PULVER,
    SONSTIGE,
}

/** Mengeneinheit der Stack-Dosis. */
enum class Einheit {
    MG,
    UG,
    G,
    ML,
    IE,
    STUECK,
}

/** Geltungsbereich einer Bewertung: ein einzelner Stack (F-12) oder der ganze Tag (F-13). */
enum class Bereich {
    STACK,
    TAG,
}

/** Urteil einer Bewertungszelle. Neutrale Paare werden gar nicht erst gespeichert (duenn besetzt). */
enum class Wirkung {
    STUETZT,
    STOERT,
}

/** Art einer gemeldeten Konkurrenz zwischen zwei Mitteln. */
enum class KonkurrenzArt {
    AUFNAHME,
    WIRKUNG,
    ZEITPUNKT,
}

// ── Entitaeten ──────────────────────────────────────────────────────────────────

/** Katalog-Mittel — gilt app-weit, unabhaengig von einzelnen Stacks (F-30). */
@Entity(
    tableName = "mittel",
    indices = [Index(value = ["name"])],
)
data class MittelE(
    @PrimaryKey val id: String,
    val name: String,
    val loeslichkeit: Loeslichkeit = Loeslichkeit.WASSER,
    val darreichungsform: Darreichungsform = Darreichungsform.KAPSEL,
    val hersteller: String = "",
    val durchfallrisiko: Boolean = false,
    val beistoffe: String = "",
)

/** Ein Stack (z. B. "Morgens") — Klammer um seine Eintraege, Ziele und Fragen (F-01). */
@Entity(
    tableName = "stack",
    indices = [Index(value = ["sortierung"])],
)
data class StackE(
    @PrimaryKey val id: String,
    val name: String,
    val zeitpunkt: String = "",
    val einnahmeHinweis: String = "",
    val sortierung: Int = 0,
)

/**
 * Ein Mittel innerhalb eines Stacks samt Stack-Dosis (F-02, F-03).
 *
 * `frequenzTage` bildet das Spec-Feld `frequenz` ab: 1 = TAEGLICH, n = ALLE_N_TAGE(n).
 * `alterniertMit` ist eine Komma-getrennte Liste von `mittelId`.
 */
@Entity(
    tableName = "stack_eintrag",
    foreignKeys = [
        ForeignKey(
            entity = StackE::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelE::class,
            parentColumns = ["id"],
            childColumns = ["mittelId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["stackId"]),
        Index(value = ["mittelId"]),
        Index(value = ["stackId", "reihenfolge"]),
        Index(value = ["gruppeId"]),
        // Ein Mittel darf in EINEM Stack nur einmal vorkommen (F-02, Regeln/Grenzen).
        Index(value = ["stackId", "mittelId"], unique = true),
    ],
)
data class StackEintragE(
    @PrimaryKey val id: String,
    val stackId: String,
    val mittelId: String,
    val stueckzahl: Int = 1,
    val mengeJeStueck: Double? = null,
    val einheit: Einheit = Einheit.MG,
    val dosisVarianteB: Double? = null,
    val frequenzTage: Int = 1,
    val alterniertMit: String = "",
    val aktiv: Boolean = true,
    val reihenfolge: Int = 0,
    val gruppeId: String? = null,
    val zusatztext: String = "",
    val offenerHinweis: String = "",
)

/** Global gepflegtes Ziel (F-08). Der Rang haengt am Stack, nicht am Ziel. */
@Entity(
    tableName = "ziel",
    indices = [Index(value = ["text"])],
)
data class ZielE(
    @PrimaryKey val id: String,
    val text: String,
)

/** Zuordnung Stack ↔ Ziel mit stack-eigenem Rang (F-09, F-10). */
@Entity(
    tableName = "stack_ziel",
    primaryKeys = ["stackId", "zielId"],
    foreignKeys = [
        ForeignKey(
            entity = StackE::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ZielE::class,
            parentColumns = ["id"],
            childColumns = ["zielId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["zielId"]),
        Index(value = ["stackId", "rang"]),
    ],
)
data class StackZielE(
    val stackId: String,
    val zielId: String,
    val rang: Int = 0,
)

/** Eigene KI-Frage, gehoert zu genau einem Stack (F-11). */
@Entity(
    tableName = "eigene_frage",
    foreignKeys = [
        ForeignKey(
            entity = StackE::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["stackId"])],
)
data class EigeneFrageE(
    @PrimaryKey val id: String,
    val stackId: String,
    val text: String,
)

/**
 * Ein abgeschlossener Auswertungslauf (F-12 bereich = STACK, F-13 bereich = TAG).
 *
 * Bei bereich = TAG ist `stackId` leer (null). Die Fremdschluessel-Regel CASCADE greift nur bei
 * gesetzter stackId — ein NULL-Wert verletzt sie nicht.
 * `hinweise` haelt die Warnungszeilen, je eine pro Zeilenumbruch.
 */
@Entity(
    tableName = "bewertung",
    foreignKeys = [
        ForeignKey(
            entity = StackE::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["stackId", "zeitpunkt"]),
        Index(value = ["bereich", "zeitpunkt"]),
    ],
)
data class BewertungE(
    @PrimaryKey val id: String,
    val bereich: Bereich,
    val stackId: String? = null,
    val zeitpunkt: Long,
    val modell: String,
    val denkstufe: String,
    val pruefsumme: String,
    val gesamt: String = "",
    val hinweise: String = "",
)

/**
 * Eine nicht-neutrale Mittel-Ziel-Zelle (F-12, Antwortformat).
 *
 * Bewusst ohne Fremdschluessel auf Mittel und Ziel: eine Bewertung ist eine Momentaufnahme und
 * soll nicht still verschwinden, wenn der Katalog aufgeraeumt wird. Das gezielte Loeschen regeln
 * F-04 und F-08 ueber eigene Abfragen.
 */
@Entity(
    tableName = "bewertungszelle",
    foreignKeys = [
        ForeignKey(
            entity = BewertungE::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bewertungId"]),
        Index(value = ["mittelId"]),
        Index(value = ["zielId"]),
    ],
)
data class BewertungszelleE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bewertungId: String,
    val mittelId: String,
    val zielId: String,
    val wirkung: Wirkung,
    @ColumnInfo(name = "staerke") val staerke: Int,
    val grund: String = "",
)

/** Gemeldete Konkurrenz zwischen zwei Mitteln eines Laufs (F-12, F-13). */
@Entity(
    tableName = "konkurrenz",
    foreignKeys = [
        ForeignKey(
            entity = BewertungE::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bewertungId"]),
        Index(value = ["mittelA"]),
        Index(value = ["mittelB"]),
    ],
)
data class KonkurrenzE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bewertungId: String,
    val mittelA: String,
    val mittelB: String,
    val art: KonkurrenzArt,
    val schwere: Int,
    val grund: String = "",
)

/** Antwort der KI auf eine eigene Frage (F-11, F-12). */
@Entity(
    tableName = "frage_antwort",
    foreignKeys = [
        ForeignKey(
            entity = BewertungE::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bewertungId"]),
        Index(value = ["frageId"]),
    ],
)
data class FrageAntwortE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bewertungId: String,
    val frageId: String,
    val text: String,
)

// ── Verbundene Lesegestalten ────────────────────────────────────────────────────

/** Stack-Eintrag samt Katalog-Stammdaten — die Liste auf B-02 braucht beides zusammen. */
data class EintragMitMittel(
    @Embedded val eintrag: StackEintragE,
    @Relation(parentColumn = "mittelId", entityColumn = "id") val mittel: MittelE,
)

/** Ein Lauf mit allem, was daran haengt (B-07, B-09, B-15). */
data class BewertungMitInhalt(
    @Embedded val bewertung: BewertungE,
    @Relation(parentColumn = "id", entityColumn = "bewertungId") val zellen: List<BewertungszelleE>,
    @Relation(parentColumn = "id", entityColumn = "bewertungId") val konkurrenzen: List<KonkurrenzE>,
    @Relation(parentColumn = "id", entityColumn = "bewertungId") val antworten: List<FrageAntwortE>,
)

/** Katalog-Zeile auf B-14: Mittel plus "in N Stacks". */
data class MittelMitAnzahl(
    @Embedded val mittel: MittelE,
    val anzahlStacks: Int,
)

/** Ziel-Zeile auf B-03: Ziel plus "in N Stacks verwendet". */
data class ZielMitAnzahl(
    @Embedded val ziel: ZielE,
    val anzahlStacks: Int,
)

/** Ziel im Zusammenhang eines Stacks — der Rang gehoert zur Zuordnung, nicht zum Ziel. */
data class ZielMitRang(
    @Embedded val ziel: ZielE,
    val rang: Int,
)
