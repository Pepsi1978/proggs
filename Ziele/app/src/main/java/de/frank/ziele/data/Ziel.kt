package de.frank.ziele.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

/** Wo ein Ziel steht. Nur aktive Ziele liest der Wecker vor. */
enum class ZielStatus(val anzeige: String, val emoji: String) {
    AKTIV("Aktiv", "🎯"),
    PAUSIERT("Pausiert", "⏸️"),
    ERREICHT("Erreicht", "🏆");

    companion object {
        fun von(name: String?): ZielStatus = entries.firstOrNull { it.name == name } ?: AKTIV
    }
}

/** Die zwei Gruppen der Vorschläge. */
enum class Kategorie(val anzeige: String, val beschreibung: String) {
    NOTWENDIG("Notwendig", "Ohne diese Aufgaben wird das Ziel nicht erreicht."),
    UNTERSTUETZEND("Unterstützend", "Nicht zwingend, aber sie machen den Weg leichter und sicherer.");

    companion object {
        fun von(name: String?): Kategorie = entries.firstOrNull { it.name.equals(name?.trim(), true) }
            ?: if (name?.lowercase()?.startsWith("unter") == true) UNTERSTUETZEND else NOTWENDIG
    }
}

/** Grobe Aufwandsklasse; die Minuten schätzt die KI dazu. */
enum class Aufwand(val anzeige: String, val rang: Int) {
    KLEIN("Klein", 0),
    MITTEL("Mittel", 1),
    GROSS("Groß", 2);

    companion object {
        fun von(name: String?): Aufwand = when (name?.trim()?.lowercase()) {
            "klein", "gering", "niedrig", "small", "low" -> KLEIN
            "gross", "groß", "hoch", "large", "high" -> GROSS
            else -> MITTEL
        }
    }
}

@Entity(tableName = "ziele", indices = [Index("status")])
data class Ziel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    /** Was genau erreicht werden soll, möglichst messbar. */
    val beschreibung: String = "",
    /** Warum das Ziel wichtig ist: hält bei Durststrecken und schärft die Vorschläge. */
    val warum: String = "",
    /** Zieldatum als Epochentag, null = ohne Frist. */
    val frist: Long? = null,
    val status: String = ZielStatus.AKTIV.name,
    /** Emoji der Karte (die KI schlägt eins vor, Frank kann es ändern). */
    val symbol: String = "🎯",
    /** Reihenfolge in der Liste (kleiner = weiter oben). */
    val rang: Int = 0,
    /** Recherche-Bericht der letzten Auswertung: wie man das Ziel am besten erreicht. */
    val bericht: String = "",
    /** Kurzfassung der Strategie (ein bis drei Sätze) für Karte, Wecker und Vorlesen. */
    val strategie: String = "",
    val ausgewertetAm: Long? = null,
    val auswertungModell: String = "",
    val erstellt: Long = System.currentTimeMillis(),
    val geaendert: Long = System.currentTimeMillis(),
    val erreichtAm: Long? = null,
) {
    val zustand: ZielStatus get() = ZielStatus.von(status)
}

@Entity(tableName = "vorschlaege", indices = [Index("zielId")])
data class Vorschlag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zielId: Long,
    /** Kurzer Schlüssel der KI (v1, v2 …), über den Voraussetzungen aufeinander zeigen. */
    val schluessel: String,
    val titel: String,
    val text: String = "",
    /** Warum diese Aufgabe auf das Ziel einzahlt. */
    val begruendung: String = "",
    val kategorie: String = Kategorie.NOTWENDIG.name,
    /** 1 (gering) bis 5 (entscheidend) für die Zielerreichung. */
    val wichtigkeit: Int = 3,
    val aufwand: String = Aufwand.MITTEL.name,
    /** Geschätzte Minuten für die ganze Aufgabe. */
    val minuten: Int = 60,
    /** Schlüssel der Vorschläge, die vorher erledigt sein müssen, als JSON-Liste. */
    val voraussetzungenJson: String = "",
    /** Empfohlene Phase: 1 = sofort, 2 = bald, 3 = später. */
    val phase: Int = 2,
    /** Woran man merkt, dass die Aufgabe erledigt ist. */
    val erfolg: String = "",
    /** Wird regelmäßig wiederholt (z. B. dreimal pro Woche laufen). */
    val wiederkehrend: Boolean = false,
    /** Id der Aufgabe in Geniale Aufgaben, sobald übernommen. */
    val aufgabeId: Long? = null,
    val uebernommenAm: Long? = null,
    /** Stand aus Geniale Aufgaben beim letzten Abgleich. */
    val erledigt: Boolean = false,
    /** Frank hat den Vorschlag verworfen: bleibt unten ausgegraut, damit die nächste Auswertung ihn nicht wiederholt. */
    val verworfen: Boolean = false,
    /** Frank hat den Vorschlag angenommen: gefällt ihm, er will ihn behalten und machen. Nicht erledigt, nicht übernommen. */
    val angenommen: Boolean = false,
    val angenommenAm: Long? = null,
    /** Frank hat die Aufgabe selbst formuliert (kein KI-Vorschlag): grüner Punkt, bleibt bei jeder Auswertung, belegt keinen Platz. */
    val eigen: Boolean = false,
    val erstellt: Long = System.currentTimeMillis(),
) {
    val gruppe: Kategorie get() = Kategorie.von(kategorie)
    val aufwandKlasse: Aufwand get() = Aufwand.von(aufwand)
    val voraussetzungen: List<String>
        get() = if (voraussetzungenJson.isBlank()) emptyList() else runCatching {
            val a = JSONArray(voraussetzungenJson)
            (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

    /** Hebel: viel Wirkung bei wenig Aufwand steht oben. */
    val hebel: Double get() = wichtigkeit.toDouble() / (1.0 + aufwandKlasse.rang + minuten / 240.0)

    companion object {
        fun liste(werte: List<String>): String = if (werte.isEmpty()) "" else JSONArray(werte).toString()
    }
}

/**
 * Ein Vorschlag, den Frank abgelehnt hat (anders als Ausblenden: endgültig). Er verschwindet aus der Liste,
 * bleibt hier aber gemerkt, damit keine Auswertung und kein Nachfüllen ihn je wieder vorschlägt, auch nicht umformuliert.
 */
@Entity(tableName = "abgelehnt", indices = [Index("zielId")])
data class Abgelehnt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zielId: Long,
    /** Schlüssel des früheren Vorschlags: wird nicht neu vergeben, damit alte Voraussetzungen nicht umgedeutet werden. */
    val schluessel: String,
    val titel: String,
    val text: String = "",
    val erstellt: Long = System.currentTimeMillis(),
)

/** Ein Beitrag im Gespräch zur Auswertung: Franks Rückmeldung oder die Antwort des Aufgaben-Agenten. */
@Entity(tableName = "rueckmeldungen", indices = [Index("zielId")])
data class Rueckmeldung(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zielId: Long,
    val vonFrank: Boolean,
    val text: String,
    val erstellt: Long = System.currentTimeMillis(),
    /** Eine Auswertung hat Franks Anregungen daraus schon in Aufgaben umgesetzt (oder bewusst nicht). */
    val verarbeitet: Boolean = false,
)
