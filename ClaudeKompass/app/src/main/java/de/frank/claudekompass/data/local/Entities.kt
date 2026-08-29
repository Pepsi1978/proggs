package de.frank.claudekompass.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ein Nachschlage-Eintrag: ein Slash-Befehl, eine Config-Einstellung oder eine Best Practice.
 *
 * Alle drei Bereiche liegen bewusst in EINER Tabelle. Sie verhalten sich fuer den Benutzer
 * identisch (vorlesen, nachfragen, ausfuehrlicher erklaeren, zurueck) — getrennte Tabellen
 * haetten denselben Code dreimal erzwungen und damit drei Stellen zum Auseinanderlaufen.
 */
@Entity(
    tableName = "eintraege",
    indices = [Index("bereich"), Index("sortierName"), Index("entfernt")],
)
data class EintragEntity(
    @PrimaryKey val id: String,
    /** `slash`, `config` oder `praxis`. */
    val bereich: String,
    /** Wie der Eintrag heisst: `/compact`, `autoCompactEnabled`, „Hooks statt Gedaechtnis". */
    val name: String,
    /** Ein Satz fuer die Liste. */
    val kurz: String,
    /** Die aktuell angezeigte Erklaerung. Wird durch „Ausfuehrlicher" ersetzt. */
    val erklaerung: String,
    /** Stufe der aktuellen Erklaerung: 0 = Grundfassung, 1..n = jeweils ausfuehrlicher. */
    val stufe: Int = 0,
    /** In welcher Claude-Code-Version der Eintrag dazukam. Leer, solange unbekannt. */
    val seitVersion: String = "",
    val kategorie: String = "",
    /** Gruppe innerhalb des Bereichs, z. B. `Eingebaut` oder `Mitgelieferter Skill`. */
    val art: String = "",
    /** Wurde der Eintrag aus Claude Code entfernt? Dann steht er im Klapp-Bereich ganz unten. */
    val entfernt: Boolean = false,
    val entferntInVersion: String = "",
    /** Was die Aufgabe uebernommen hat — oder der Hinweis, dass es keinen Ersatz gibt. */
    val ersatz: String = "",
    /**
     * Markierung „in diesem Aktualisierungslauf neu dazugekommen".
     * Beim naechsten Lauf faellt sie weg: dann gehoert der Eintrag zum Bestand.
     */
    val neuImLauf: Long = 0L,
    /** Der englische Originaltext aus der offiziellen Dokumentation, als Beleg. */
    val quelleEnglisch: String = "",
    /** Kleingeschriebener Name ohne fuehrenden Schraegstrich — dient der Sortierung. */
    val sortierName: String,
    val zuletztGeaendert: Long = System.currentTimeMillis(),
)

/**
 * Eine frühere Fassung einer Erklaerung.
 *
 * Der Zurueck-Pfeil braucht sie: „Ausfuehrlicher" schreibt die alte Fassung hierhin, bevor die
 * neue in den Eintrag wandert. Ohne diese Tabelle waere die kurze Fassung unwiederbringlich weg.
 */
@Entity(
    tableName = "erklaerung_historie",
    indices = [Index("eintragId")],
    foreignKeys = [
        ForeignKey(
            entity = EintragEntity::class,
            parentColumns = ["id"],
            childColumns = ["eintragId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ErklaerungHistorieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eintragId: String,
    val stufe: Int,
    val text: String,
    val erstelltAm: Long = System.currentTimeMillis(),
)

/** Eine per Mikrofon gestellte Frage samt Antwort, dauerhaft am Eintrag gespeichert. */
@Entity(
    tableName = "fragen",
    indices = [Index("eintragId")],
    foreignKeys = [
        ForeignKey(
            entity = EintragEntity::class,
            parentColumns = ["id"],
            childColumns = ["eintragId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FrageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eintragId: String,
    val frage: String,
    val antwort: String,
    /** true, solange die Antwort noch laeuft — die Liste zeigt dann „wird beantwortet". */
    val laeuft: Boolean = false,
    val fehler: String = "",
    val erstelltAm: Long = System.currentTimeMillis(),
)

/** Ein Gespraech im Chat-Bereich. Neue entstehen ueber das Plus. */
@Entity(tableName = "chat_sitzungen")
data class ChatSitzungEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val erstelltAm: Long = System.currentTimeMillis(),
    val zuletztAm: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chat_nachrichten",
    indices = [Index("sitzungId")],
    foreignKeys = [
        ForeignKey(
            entity = ChatSitzungEntity::class,
            parentColumns = ["id"],
            childColumns = ["sitzungId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatNachrichtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sitzungId: Long,
    /** `benutzer` oder `ki`. */
    val rolle: String,
    val text: String,
    val fehler: String = "",
    val erstelltAm: Long = System.currentTimeMillis(),
)

/** Ein Durchlauf des Aktualisieren-Knopfs. */
@Entity(tableName = "aktualisierungen")
data class AktualisierungEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gestartetAm: Long = System.currentTimeMillis(),
    val beendetAm: Long = 0L,
    /** Die Claude-Code-Version, fuer die dieser Lauf den Stand gehoben hat. */
    val cliVersion: String = "",
    val neuAnzahl: Int = 0,
    val entferntAnzahl: Int = 0,
    val geaendertAnzahl: Int = 0,
    /** `laeuft`, `fertig` oder `fehler`. */
    val status: String = "laeuft",
    val meldung: String = "",
)
