package de.frank.aufgaben.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/** Die vier Prioritäten. [SPAETER] ist der Eingang: Jede neue Aufgabe landet zuerst dort. */
enum class Prioritaet(val anzeige: String, val rang: Int) {
    HOCH("Hoch", 0),
    MITTEL("Mittel", 1),
    GERING("Gering", 2),
    SPAETER("Später", 3);

    companion object {
        fun von(name: String?): Prioritaet = entries.firstOrNull { it.name == name } ?: SPAETER
        /** Die drei Stufen, die in Heute und Morgen gelten. */
        val tagesStufen = listOf(HOCH, MITTEL, GERING)
    }
}

enum class Wiederholung(val anzeige: String) {
    KEINE("Keine"),
    TAEGLICH("Täglich"),
    WERKTAGS("Werktags"),
    WOECHENTLICH("Wöchentlich"),
    MONATLICH("Monatlich");

    companion object {
        fun von(name: String?): Wiederholung = entries.firstOrNull { it.name == name } ?: KEINE
    }
}

data class Schritt(val text: String, val erledigt: Boolean)

@Entity(tableName = "aufgaben", indices = [Index("tag"), Index("erledigt")])
data class Aufgabe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val text: String = "",
    val prioritaet: String = Prioritaet.SPAETER.name,
    /** Geplanter Tag als Epochentag; null = ohne Tag (dann zählt nur die Priorität). */
    val tag: Long? = null,
    /** Uhrzeit in Minuten nach Mitternacht; null = ohne Termin. */
    val minuten: Int? = null,
    val dauer: Int = 30,
    val erinnerung: Boolean = false,
    val vorlauf: Int = 0,
    val wiederholung: String = Wiederholung.KEINE.name,
    val erledigt: Boolean = false,
    val erledigtAm: Long? = null,
    val erstellt: Long = System.currentTimeMillis(),
    val geaendert: Long = System.currentTimeMillis(),
    val titelVonKi: Boolean = false,
    /** Checkliste als JSON-Liste [{"t": Text, "e": erledigt}]. */
    val schritteJson: String = "",
    /** Erinnerung liest den Aufgabentext vor (sechs vorab erzeugte Stimmfassungen). */
    @ColumnInfo(defaultValue = "0") val vorlesen: Boolean = false,
    /** Erinnerung als Wecker: läuft weiter, bis man sie in der Benachrichtigung ausschaltet. */
    @ColumnInfo(defaultValue = "0") val alsWecker: Boolean = false,
) {
    val prio: Prioritaet get() = Prioritaet.von(prioritaet)
    val wdh: Wiederholung get() = Wiederholung.von(wiederholung)

    val schritte: List<Schritt>
        get() = if (schritteJson.isBlank()) emptyList() else runCatching {
            val array = JSONArray(schritteJson)
            (0 until array.length()).map { i ->
                array.getJSONObject(i).let { Schritt(it.optString("t"), it.optBoolean("e")) }
            }
        }.getOrDefault(emptyList())

    companion object {
        fun schritteAlsJson(schritte: List<Schritt>): String =
            if (schritte.isEmpty()) "" else JSONArray(schritte.map { JSONObject().put("t", it.text).put("e", it.erledigt) }).toString()
    }
}
