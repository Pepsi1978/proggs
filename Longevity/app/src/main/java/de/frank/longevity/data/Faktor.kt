package de.frank.longevity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/** Die Lebensbereiche, in die jeder Faktor fällt. */
enum class Kategorie(val anzeige: String, val emoji: String) {
    BEWEGUNG("Bewegung & Fitness", "🏃"),
    ERNAEHRUNG("Ernährung", "🥗"),
    SCHLAF("Schlaf & Rhythmus", "😴"),
    SUPPLEMENTE("Supplements", "💊"),
    GEIST("Geist & Stress", "🧘"),
    SOZIAL("Beziehungen", "🤝"),
    VORSORGE("Vorsorge & Medizin", "🩺"),
    GIFTE("Genussmittel & Gifte", "🚭"),
    UMWELT("Umwelt", "🌿"),
    SINN("Sinn & Lernen", "🎯"),
    ;

    companion object {
        fun von(name: String?): Kategorie =
            entries.firstOrNull { it.name.equals(name?.trim(), true) || it.anzeige.equals(name?.trim(), true) } ?: SINN
    }
}

/** Wie gut die Wirkung auf die Lebensdauer belegt ist. */
enum class Evidenz(val anzeige: String, val kurz: String, val staerke: Int) {
    BELEGT("Gut belegt", "belegt", 3),
    WAHRSCHEINLICH("Sehr wahrscheinlich", "wahrscheinlich", 2),
    LOGISCH("Logisch naheliegend", "logisch", 1),
    ;

    companion object {
        fun von(name: String?): Evidenz = entries.firstOrNull { it.name.equals(name?.trim(), true) } ?: WAHRSCHEINLICH
    }
}

/** Ein Punkt im Aufgabenplan eines Faktors — bei „Supplements“ z. B. ein einzelnes Mittel. */
data class Punkt(
    val titel: String,
    val text: String = "",
    val evidenz: String = Evidenz.WAHRSCHEINLICH.name,
    val erledigt: Boolean = false,
) {
    val ev: Evidenz get() = Evidenz.von(evidenz)
}

/** Eine Quelle zu einem Faktor (Studie, Metaanalyse, Leitlinie). */
data class Quelle(val titel: String, val jahr: String = "", val link: String = "")

/**
 * Ein Verhalten, das die Lebensdauer beeinflusst. [jahre] ist die grobe Schätzung gesunder Lebensjahre
 * gegenüber dem Unterlassen: positiv bei förderlichem Verhalten (Ausdauer +6), negativ bei schädlichem
 * (Rauchen −10). [rang] läuft lückenlos über die ganze Liste: oben die Plus-Faktoren nach Wichtigkeit,
 * darunter die Lebenszeit-Räuber, der schädlichste ganz unten (siehe [ordnen]). [wirkung] ist die Stärke
 * als 0–100-Punkte für Balken und Diagramme.
 */
@Entity(tableName = "faktoren")
data class Faktor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rang: Int = 0,
    val titel: String,
    val kurz: String = "",
    val kategorie: String = Kategorie.SINN.name,
    val evidenz: String = Evidenz.WAHRSCHEINLICH.name,
    val jahre: Float = 0f,
    val wirkung: Int = 50,
    val erklaerung: String = "",
    val begruendung: String = "",
    val ziel: String = "",
    val punkteJson: String = "[]",
    /** Vom Nutzer eingesprochen/eingetippt. */
    val eigen: Boolean = false,
    /** Die ursprüngliche Eingabe des Nutzers. */
    val notiz: String = "",
    /** Rang vor der letzten Aktualisierung — für die Pfeile ↑↓. */
    val vorherRang: Int? = null,
    /** Bei der letzten Aktualisierung/Auswertung neu hinzugekommen. */
    val neu: Boolean = false,
    /** Die KI hat Erklärung und Aufgabenplan schon vertieft. */
    val vertieft: Boolean = false,
    /** Neuer Faktor, den die KI bei der Aktualisierung vorschlägt — erscheint erst nach Bestätigung in der Liste. */
    val vorschlag: Boolean = false,
    /** Der Nutzer hat das Ziel komplett umgesetzt – bleibt auf seinem Platz, wird aber ausgegraut. */
    @ColumnInfo(defaultValue = "0") val zielErreicht: Boolean = false,
    val geaendertAm: Long = System.currentTimeMillis(),
    /** Quellen der KI (JSON-Liste aus Titel, Jahr, Link); null bei Altbestand. */
    val quellenJson: String? = null,
    /** Wann der Inhalt zuletzt von der KI geprüft bzw. neu geschrieben wurde. */
    val standVom: Long? = null,
    /** Hinweis der Gutachterin (z. B. Überschneidung, veraltete Aussage) – der Nutzer entscheidet. */
    val hinweis: String? = null,
    /** Vorschlag der Gutachterin: diesen Faktor mit dem Faktor dieser id zusammenlegen. */
    val zusammenMit: Long? = null,
    /** Geschätzte Wahrscheinlichkeit (0–100 %), dass der Effekt real ist; [jahre] ist der Erwartungswert daraus. */
    val wahrscheinlichkeit: Int? = null,
) {
    val kat: Kategorie get() = Kategorie.von(kategorie)
    /** Schädliches Verhalten, das Lebensjahre kostet – steht unter der Null-Linie. */
    val raeuber: Boolean get() = jahre < 0f
    val ev: Evidenz get() = Evidenz.von(evidenz)
    val punkte: List<Punkt> get() = punkteAusJson(punkteJson)
    val quellen: List<Quelle> get() = quellenAusJson(quellenJson)

    companion object {
        fun quellenAlsJson(liste: List<Quelle>): String = JSONArray().apply {
            liste.forEach { put(JSONObject().put("titel", it.titel).put("jahr", it.jahr).put("link", it.link)) }
        }.toString()

        fun quellenAusJson(json: String?): List<Quelle> = if (json.isNullOrBlank()) emptyList() else runCatching {
            val a = JSONArray(json)
            List(a.length()) { i -> a.getJSONObject(i).let { Quelle(it.optString("titel"), it.optString("jahr"), it.optString("link")) } }
                .filter { it.titel.isNotBlank() }
        }.getOrDefault(emptyList())

        /** Alte und neue Quellen vereinen, doppelte (gleicher Titel oder Link) nur einmal. */
        fun quellenMischen(alt: List<Quelle>, neu: List<Quelle>): List<Quelle> {
            fun schluessel(q: Quelle) = (q.link.ifBlank { q.titel }).lowercase().filter { it.isLetterOrDigit() }.take(60)
            return (neu + alt).distinctBy(::schluessel).take(24)
        }

        fun punkteAlsJson(liste: List<Punkt>): String = JSONArray().apply {
            liste.forEach {
                put(JSONObject().put("titel", it.titel).put("text", it.text).put("evidenz", it.evidenz).put("erledigt", it.erledigt))
            }
        }.toString()

        fun punkteAusJson(json: String): List<Punkt> = runCatching {
            val a = JSONArray(json)
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Punkt(o.optString("titel"), o.optString("text"), o.optString("evidenz", Evidenz.WAHRSCHEINLICH.name), o.optBoolean("erledigt"))
            }
        }.getOrDefault(emptyList())
    }
}

/**
 * Die eine Ordnung der Rangliste: oben alles, was Lebensjahre schenkt (in der bisherigen Reihenfolge),
 * darunter die Lebenszeit-Räuber nach verlorenen Jahren – knapp unter null zuerst, der schädlichste ganz unten.
 */
fun ordnen(liste: List<Faktor>): List<Faktor> {
    val (plus, minus) = liste.partition { !it.raeuber }
    return (plus + minus.sortedByDescending { it.jahre }).mapIndexed { i, x -> x.copy(rang = i + 1) }
}

/** Angezeigter Platz: oben 1, 2, 3 …; unter der Null-Linie −1 (knapp unter null) bis −n (ganz unten). */
fun Faktor.platz(liste: List<Faktor>): String =
    if (raeuber) "−" + (rang - liste.count { !it.vorschlag && !it.raeuber }).coerceAtLeast(1) else "$rang"

/** Kurzform für die Startinhalte. */
internal fun faktor(
    titel: String,
    kurz: String,
    kategorie: Kategorie,
    evidenz: Evidenz,
    jahre: Float,
    wirkung: Int,
    erklaerung: String,
    begruendung: String,
    ziel: String,
    vararg punkte: Punkt,
) = Faktor(
    titel = titel, kurz = kurz, kategorie = kategorie.name, evidenz = evidenz.name, jahre = jahre, wirkung = wirkung,
    erklaerung = erklaerung, begruendung = begruendung, ziel = ziel, punkteJson = Faktor.punkteAlsJson(punkte.toList()),
)

internal fun punkt(titel: String, text: String, evidenz: Evidenz = Evidenz.WAHRSCHEINLICH) = Punkt(titel, text, evidenz.name)
