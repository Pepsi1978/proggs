package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.merkKontext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

/**
 * Das Gesamtbild, mit dem Jarvis jede Frage betrachtet: Franks Regeln, das Wissen über Frank, der Rahmen der
 * nächsten Tage, Termine, Aufgaben und die aktuelle Tagesauswertung (sie trägt Biodaten, Wetter, Trainings,
 * Tagebuch und Ideen schon gedeutet in sich). Ein Text für alle Wege: Der eigene Chat und die Agenten bekommen
 * ihn in die Anweisung, das Plugin holt ihn mit einem Aufruf (jarvis_kontext).
 */
object Gehirn {
    /** Wann das Plugin das Gesamtbild zuletzt geholt hat; danach richtet sich die Erinnerung in den Werkzeug-Ergebnissen. */
    @Volatile var zuletztGeholt = 0L

    suspend fun kontext(context: Context): String {
        val app = context.applicationContext
        val alle = Register.alle(app)
        val werkzeuge = alle.flatMap { it.werkzeuge }.associateBy { it.name }
        suspend fun rufe(name: String, argumente: JSONObject): String =
            runCatching { werkzeuge[name]?.ausfuehren(argumente)?.let { (if (it.fehler) "NICHT VERFÜGBAR: " else "") + it.text } }.getOrNull() ?: "NICHT VERFÜGBAR"
        val jetzt = LocalDateTime.now()
        val heute = LocalDate.now()
        return buildString {
            append("GESAMTBILD VON FRANK, Stand ").append(jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))).append(".\n")
            append("Das ist dein Hintergrundwissen für jede Frage. Betrachte Franks Frage in diesem Zusammenhang und überlege, was davon seine Antwort besser macht ")
            append("(zum Beispiel Dienst und Schlafzeiten, Erholung, Wetter, eine offene Aufgabe, etwas, das du über ihn weißt). Beziehe genau das ein, kurz und von dir aus. ")
            append("Was nichts mit der Frage zu tun hat, lässt du weg; zähle das Gesamtbild nie auf.\n")
            append("Fragt Frank, was jetzt am sinnvollsten ist, leite es aus seiner Lage ab: was für ein Tag es ist und wie viel freie Zeit bleibt, Schlaf und Erholung, ")
            append("seine Ziele gegenüber den aktuellen Werten, Wetter, offene Aufgaben, passende Ideen. Nenne ein bis drei konkrete Vorschläge mit kurzem Grund, den wichtigsten zuerst.\n\n")

            append(merkKontext(app).ifEmpty { "Frank hat noch keine Regeln festgelegt, und über ihn ist noch nichts notiert.\n" }).append('\n')

            append("RAHMEN DER NÄCHSTEN TAGE (fest gerechnet, verbindlich: Dienst, Schlaf- und freie Zeiten):\n")
            append(runCatching { alle.filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.rahmen(4)?.ifEmpty { null } }.getOrNull() ?: "NICHT VERFÜGBAR: Kalender nicht lesbar").append("\n\n")

            append("TERMINE HEUTE UND MORGEN (frisch gelesen):\n")
            append(rufe("kalender_lesen", JSONObject().put("von", heute.toString()).put("bis", heute.plusDays(1).toString()))).append("\n\n")

            append("AUFGABEN (frisch gelesen):\n")
            append("Heute und überfällig: ").append(rufe("aufgaben_lesen", JSONObject().put("bereich", "heute"))).append('\n')
            append("Morgen: ").append(rufe("aufgaben_lesen", JSONObject().put("bereich", "morgen"))).append("\n\n")

            append("TAGEBUCH (Franks eigene Einträge; daraus kennst du, was ihn beschäftigt):\n")
            append(runCatching { alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.gedaechtnis() }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            val auswertung = Tagesauswertung.neueste(app)
            if (auswertung == null) {
                append("TAGESAUSWERTUNG: Es liegt noch keine vor.\n")
            } else {
                val minuten = Duration.between(auswertung.zeitpunkt, jetzt).toMinutes()
                append("AKTUELLE TAGESAUSWERTUNG (von Jarvis geschrieben vor ").append(if (minuten < 90) "$minuten Minuten" else "${minuten / 60} Stunden")
                append("; Biodaten, Wetter, Trainings, Tagebuch und Ideen sind darin schon gedeutet, einzelne Werte liest du bei Bedarf frisch):\n")
                append(if (auswertung.mitKi) auswertung.text else auswertung.daten.substringBefore("== GENIALE IDEEN").trim()).append('\n')
            }
        }
    }
}
