package de.frank.genialeideen.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Offen, umgesetzt — oder ein Entwurf, der beim Verlassen des Erfassen-Bildschirms von allein
 * gesichert wurde, damit halbfertige Ideen nicht verloren gehen.
 */
enum class IdeenStatus { OFFEN, UMGESETZT, ENTWURF }

/** Die vom Nutzer gewählte Art einer Kategorie. */
enum class Kategorieart { MENTAL, PRAKTISCH }

@Entity(tableName = "ideen")
data class IdeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val text: String,
    /** Gespeichert als Name des [IdeenStatus]. */
    val status: String = IdeenStatus.OFFEN.name,
    /** Die gezogene Reihenfolge innerhalb der jeweiligen Liste. Kleiner heisst weiter oben. */
    val reihenfolge: Int = 0,
    val angelegtAm: Long = System.currentTimeMillis(),
    val geaendertAm: Long = System.currentTimeMillis(),
    val umgesetztAm: Long? = null,
    /** Pfad der Sprachaufnahme im App-Verzeichnis, falls die Idee eingesprochen wurde. */
    val aufnahmePfad: String? = null,
    /** Das ungeglättete Diktat — „Text glätten" überschreibt nie unwiderruflich (Baustein O.3). */
    val originalText: String? = null,
    /** Die Kategorie, in der die Idee zusätzlich zu ihrer Liste auftaucht (Baustein P). */
    @ColumnInfo(defaultValue = "NULL") val kategorieId: Long? = null,
)

/** Eine manuell angelegte Kategorie. Die Idee bleibt trotzdem in ihrer Liste stehen (Baustein P). */
@Entity(tableName = "kategorien", indices = [Index(value = ["name", "art"], unique = true)])
data class KategorieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reihenfolge: Int = 0,
    @ColumnInfo(defaultValue = "'MENTAL'") val art: Kategorieart = Kategorieart.MENTAL,
)

/** Die Spiegeltabelle für die Volltextsuche (Baustein K). */
@Fts4(contentEntity = IdeeEntity::class)
@Entity(tableName = "ideen_fts")
data class IdeeFts(
    val titel: String,
    val text: String,
)

@Entity(
    tableName = "nachrichten",
    foreignKeys = [
        ForeignKey(
            entity = IdeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["ideeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ideeId")],
)
data class NachrichtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideeId: Long,
    /** `user` oder `assistant`. */
    val rolle: String,
    val text: String,
    val zeitpunkt: Long = System.currentTimeMillis(),
    /** true, wenn die Antwort abgebrochen wurde und unvollständig ist (Baustein O.2). */
    @ColumnInfo(defaultValue = "0") val unvollstaendig: Boolean = false,
)

/** Die letzten Suchanfragen (Baustein K). */
@Entity(tableName = "suchverlauf")
data class SuchanfrageEntity(
    @PrimaryKey val anfrage: String,
    val zeitpunkt: Long = System.currentTimeMillis(),
)
