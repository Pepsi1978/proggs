package de.frank.jarvis.faehigkeit

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

/**
 * Die eigene Ablage von Jarvis: Textdateien, die Jarvis selbst anlegt, liest und wieder löscht —
 * Recherchen, Ausarbeitungen, Notizen zu laufenden Vorhaben. Liegt im privaten Speicher der App.
 */
object Ablage {
    private fun ordner(context: Context) = File(context.filesDir, "ablage").apply { mkdirs() }

    /** Macht aus einem Titel einen sicheren Dateinamen, ohne Pfadzeichen. */
    private fun dateiname(titel: String): String =
        titel.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex("\\s+"), " ").take(80).trim().ifEmpty { "Notiz" }

    fun alle(context: Context): List<File> = ordner(context).listFiles { f -> f.isFile && f.name.endsWith(".md") }?.sortedByDescending { it.lastModified() }.orEmpty()

    fun finde(context: Context, titel: String): File? {
        val gesucht = dateiname(titel.removeSuffix(".md")).lowercase(Locale.GERMAN)
        val dateien = alle(context)
        return dateien.firstOrNull { it.nameWithoutExtension.lowercase(Locale.GERMAN) == gesucht }
            ?: dateien.filter { gesucht in it.nameWithoutExtension.lowercase(Locale.GERMAN) }.singleOrNull()
    }

    /** Legt eine Datei an oder ersetzt sie. Mit [anhaengen] wird der Text ans Ende gesetzt. */
    fun schreibe(context: Context, titel: String, inhalt: String, anhaengen: Boolean = false): File {
        val datei = finde(context, titel) ?: File(ordner(context), dateiname(titel) + ".md")
        if (anhaengen && datei.exists()) datei.appendText("\n\n" + inhalt.trim() + "\n") else datei.writeText(inhalt.trim() + "\n")
        return datei
    }

    fun datum(datei: File): String = Instant.ofEpochMilli(datei.lastModified()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.GERMAN))
}

class AblageFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "ablage"
    override val name = "Jarvis-Ablage"
    override val beschreibung = "Eigene Textdateien von Jarvis: Recherchen, Ausarbeitungen und Notizen anlegen, lesen und löschen."
    override val hinweise =
        "Die Ablage ist das eigene Gedächtnis von Jarvis für längere Texte: Ergebnisse von Agenten (zum Beispiel Recherchen), Ausarbeitungen zu Ideen, Notizen zu Vorhaben. " +
            "Lege dort ab, was Frank später wieder braucht, mit einem sprechenden Titel. Ist ein Vorhaben erledigt und Frank braucht die Datei nicht mehr, lösche sie nach Rückfrage."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "ablage_liste",
            titel = "Ablage ansehen",
            beschreibung = "Jarvis: listet die Dateien in der eigenen Ablage von Jarvis (Recherchen, Ausarbeitungen, Notizen) mit Datum und Größe. " +
                "Nutze es für „Was hast du recherchiert?“, „Welche Unterlagen liegen vor?“.",
            schema = schema("suche" to text("Optional: nur Dateien, deren Titel oder Inhalt dieses Wort enthält.")),
            nurLesen = true,
        ) { a ->
            val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
            val dateien = Ablage.alle(context).filter { suche.isEmpty() || suche in it.name.lowercase(Locale.GERMAN) || suche in it.readText().lowercase(Locale.GERMAN) }
            if (dateien.isEmpty()) Ergebnis("Die Ablage ist leer" + (if (suche.isEmpty()) "." else " für „$suche“."))
            else Ergebnis(dateien.joinToString("\n", "${dateien.size} Datei(en) in der Ablage:\n") { "- ${it.nameWithoutExtension} (${Ablage.datum(it)}, ${it.length() / 1000 + 1} kB)" })
        },
        Werkzeug(
            name = "ablage_lesen",
            titel = "Ablage-Datei lesen",
            beschreibung = "Jarvis: liest eine Datei aus der Ablage von Jarvis, zum Beispiel eine fertige Recherche. Der Titel muss nicht exakt sein, ein eindeutiger Teil genügt.",
            schema = schema("titel" to text("Titel der Datei oder ein eindeutiger Teil davon."), pflicht = listOf("titel")),
            nurLesen = true,
        ) { a ->
            val datei = Ablage.finde(context, a.optString("titel"))
            if (datei == null) Ergebnis("Keine eindeutige Datei für „${a.optString("titel")}“. Mit ablage_liste nachsehen.", fehler = true)
            else Ergebnis("Datei „${datei.nameWithoutExtension}“ (Stand ${Ablage.datum(datei)}):\n\n" + datei.readText().take(60_000))
        },
        Werkzeug(
            name = "ablage_schreiben",
            titel = "In die Ablage schreiben",
            beschreibung = "Jarvis: legt eine Textdatei in der Ablage von Jarvis an oder ersetzt sie (mit anhaengen wird ergänzt). Für Ergebnisse, die Frank später wieder braucht: " +
                "Recherchen, Ausarbeitungen, Pläne, Zusammenfassungen eines Gesprächs.",
            schema = schema(
                "titel" to text("Sprechender Titel, zum Beispiel „Recherche Wecker-App Machbarkeit“."),
                "inhalt" to text("Der vollständige Text."),
                "anhaengen" to schalter("true = an eine vorhandene Datei gleichen Titels anhängen statt ersetzen."),
                pflicht = listOf("titel", "inhalt"),
            ),
            nurLesen = false,
        ) { a ->
            if (a.optString("inhalt").isBlank()) Ergebnis("Der Inhalt ist leer.", fehler = true)
            else Ablage.schreibe(context, a.optString("titel"), a.optString("inhalt"), a.optBoolean("anhaengen")).let { Ergebnis("Abgelegt als „${it.nameWithoutExtension}“ (${it.length() / 1000 + 1} kB).") }
        },
        Werkzeug(
            name = "ablage_loeschen",
            titel = "Ablage-Datei löschen",
            beschreibung = "Jarvis: löscht eine Datei aus der Ablage von Jarvis. Nur wenn Frank das verlangt oder bestätigt hat, etwa weil ein Vorhaben erledigt ist.",
            schema = schema("titel" to text("Titel der Datei oder ein eindeutiger Teil davon."), pflicht = listOf("titel")),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            val datei = Ablage.finde(context, a.optString("titel"))
            if (datei == null) Ergebnis("Keine eindeutige Datei für „${a.optString("titel")}“.", fehler = true)
            else { val name = datei.nameWithoutExtension; datei.delete(); Ergebnis("Gelöscht: „$name“.") }
        },
    )
}
