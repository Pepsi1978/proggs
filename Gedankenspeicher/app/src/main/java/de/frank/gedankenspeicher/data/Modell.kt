package de.frank.gedankenspeicher.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * **Das Datenmodell aus `01-FUNKTIONS-SPEC.md` §3.**
 *
 * Notizen und KI-Antworten liegen in zwei Tabellen, erscheinen im Verlauf aber gemeinsam
 * und nach `erstelltAm` sortiert (siehe [Verlaufseintrag]). Getrennt sind sie, weil eine
 * KI-Antwort andere Felder trägt als eine Notiz — und weil die Grenze für die nächste
 * Auswertung (F-09) genau der Zeitstempel der letzten KI-Antwort ist.
 */

@Entity(tableName = "sitzung")
data class Sitzung(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String = "Neue Sitzung",
    /** Ein von Hand vergebener Titel wird nie wieder von der KI überschrieben (F-12). */
    val titelVonHand: Boolean = false,
    val erstelltAm: Long,
    val zuletztGeoeffnet: Long,
)

/** Der Zustand einer Notiz (`01-FUNKTIONS-SPEC.md` §4). */
enum class Notizzustand {
    /** Die Aufnahme läuft noch. Dieser Zustand überlebt das Schließen der App nicht. */
    AUFNEHMEND,
    TRANSKRIBIERT_GERADE,
    WARTET_AUF_TRANSKRIPTION,
    TRANSKRIPTION_FEHLGESCHLAGEN,
    NICHTS_VERSTANDEN,
    KEIN_SCHLUESSEL,
    FERTIG,
}

enum class Notizquelle { GESPROCHEN, GETIPPT }

@Entity(
    tableName = "notiz",
    foreignKeys = [
        ForeignKey(
            entity = Sitzung::class,
            parentColumns = ["id"],
            childColumns = ["sitzungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sitzungId"), Index("erstelltAm")],
)
data class Notiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sitzungId: Long,
    val erstelltAm: Long,
    val text: String = "",
    /**
     * Der wirklich gesprochene Wortlaut, gesichert vor der ersten Verbesserung (F-07).
     * Bleibt danach unangetastet — deshalb ist eine Notiz nur einmal verbesserbar.
     */
    val textOriginal: String? = null,
    val ueberschrift: String? = null,
    val ueberschriftVonHand: Boolean = false,
    val quelle: Notizquelle,
    val zustand: Notizzustand,
    /** Nur gesetzt, solange die Aufnahme noch auf ihre Transkription wartet (F-04). */
    val audioPfad: String? = null,
    val istVerbessert: Boolean = false,
    val versucheTranskription: Int = 0,
)

@Entity(
    tableName = "ki_antwort",
    foreignKeys = [
        ForeignKey(
            entity = Sitzung::class,
            parentColumns = ["id"],
            childColumns = ["sitzungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sitzungId"), Index("erstelltAm")],
)
data class KiAntwort(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sitzungId: Long,
    val erstelltAm: Long,
    val rueckfrage: String,
    val antwortDesNutzers: String,
    val text: String,
    val profilName: String,
    val modell: String,
    val effort: String,
    val websucheAn: Boolean,
    val ganzeSitzung: Boolean = false,
)

@Entity(tableName = "auswertungsprofil")
data class Auswertungsprofil(
    @PrimaryKey val nummer: Int,
    val name: String,
    val anweisung: String,
    @ColumnInfo(name = "istAktiv") val istAktiv: Boolean,
)

/**
 * Ein Eintrag im Verlauf — entweder eine Notiz oder eine KI-Antwort.
 *
 * Der Verlauf ist eine einzige Liste; die Trennung in zwei Tabellen darf man ihm nicht
 * ansehen. Deshalb wird sie hier wieder zusammengeführt statt in der Oberfläche.
 */
sealed interface Verlaufseintrag {
    val zeit: Long

    data class NotizEintrag(val notiz: Notiz) : Verlaufseintrag {
        override val zeit: Long get() = notiz.erstelltAm
    }

    data class AntwortEintrag(val antwort: KiAntwort) : Verlaufseintrag {
        override val zeit: Long get() = antwort.erstelltAm
    }
}

/** Ein Suchtreffer (F-14) — mit der Sitzung, in der er steckt. */
data class Suchtreffer(
    val sitzungId: Long,
    val sitzungstitel: String,
    val notizId: Long,
    val ueberschrift: String?,
    val text: String,
    val erstelltAm: Long,
    val istKiAntwort: Boolean,
)
