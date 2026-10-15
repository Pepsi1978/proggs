package de.frank.epigenetik.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.Normalizer
import java.util.Locale

/** Was recherchiert wird. */
enum class Typ(val anzeige: String, val emoji: String) {
    MITTEL("Mittel", "💊"),
    WIRKUNG("Wirkung", "🧬"),
    ZIEL("Ziel", "🎯"),
    STACK("Stack-Analyse", "🧪"),
    FRAGE("Frage", "💬");

    companion object {
        fun von(s: String?): Typ = entries.firstOrNull { it.name.equals(s, true) || it.anzeige.equals(s, true) } ?: MITTEL
    }
}

/** Art eines Mittels: Hauptfokus Supplements, dann Nahrung, dann Verhalten. */
enum class Kategorie(val anzeige: String, val mehrzahl: String, val emoji: String) {
    SUPPLEMENT("Nahrungsergänzung", "Nahrungsergänzungsmittel", "💊"),
    NAHRUNG("Nahrung", "Nahrungsmittel", "🥦"),
    VERHALTEN("Verhalten", "Verhaltensweisen", "🏃");

    companion object {
        fun von(s: String?): Kategorie {
            val t = s.orEmpty().lowercase(Locale.GERMAN)
            return entries.firstOrNull { it.name.equals(s, true) } ?: when {
                t.startsWith("nahrungserg") || t.startsWith("supp") -> SUPPLEMENT
                t.startsWith("nahrung") || t.startsWith("lebensm") || t.startsWith("food") -> NAHRUNG
                t.startsWith("verhalt") || t.startsWith("lebensst") -> VERHALTEN
                else -> SUPPLEMENT
            }
        }
    }
}

enum class Status { WARTET, LAEUFT, FERTIG, FEHLER }

/**
 * Eine Recherche: ein Mittel, eine Wirkung, ein Ziel, die Stack-Analyse oder eine Frage.
 * Das vollständige Ergebnis steht als JSON in [json]; die Schalter eines Mittels liegen zusätzlich
 * normalisiert in [SchalterWirkung], damit Rückwärtssuche und Atlas ohne neue Recherche funktionieren.
 */
@Entity(tableName = "recherche", indices = [Index("schluessel"), Index("typ")])
data class Recherche(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typ: String,
    val kategorie: String = "",
    val name: String,
    val schluessel: String,
    /** Weitere Namen, normalisiert und mit | getrennt (z. B. „nr|nicotinamide riboside“). */
    val aliase: String = "",
    val status: String = Status.WARTET.name,
    val fehler: String = "",
    val kurz: String = "",
    /** Epigenetisches Potenzial 0–100. */
    val potenzial: Int = 0,
    val evidenz: String = "",
    val json: String = "",
    val favorit: Boolean = false,
    val imStack: Boolean = false,
    val notiz: String = "",
    /** app oder jarvis */
    val quelle: String = "app",
    val gelesen: Boolean = false,
    val erstellt: Long = System.currentTimeMillis(),
    val aktualisiert: Long = System.currentTimeMillis(),
) {
    val typEnum: Typ get() = Typ.von(typ)
    val kategorieEnum: Kategorie get() = Kategorie.von(kategorie)
    val statusEnum: Status get() = runCatching { Status.valueOf(status) }.getOrDefault(Status.WARTET)
    val fertig: Boolean get() = statusEnum == Status.FERTIG
    val offen: Boolean get() = statusEnum == Status.WARTET || statusEnum == Status.LAEUFT
    val emoji: String get() = if (typEnum == Typ.MITTEL) kategorieEnum.emoji else typEnum.emoji
}

/** Ein Schalter, den ein Mittel beeinflusst (aus der Recherche des Mittels). */
@Entity(tableName = "schalter_wirkung", primaryKeys = ["rechercheId", "schalter"], indices = [Index("schalter")])
data class SchalterWirkung(
    val rechercheId: Long,
    val schalter: String,
    /** aktiviert | hemmt | moduliert */
    val richtung: String,
    val staerke: Int,
    val evidenz: String,
    val mechanismus: String,
)

/** Normalisiert Namen für Vergleiche: klein, ohne Akzente/Satzzeichen, Umlaute aufgelöst. */
fun normalisiere(s: String): String {
    val klein = s.lowercase(Locale.GERMAN).trim()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    val ohne = Normalizer.normalize(klein, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return ohne.replace(Regex("[^a-z0-9+]+"), " ").trim().replace(Regex("\\s+"), " ")
}
