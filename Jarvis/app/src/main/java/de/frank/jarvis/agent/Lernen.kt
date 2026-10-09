package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.UeberFrank
import de.frank.jarvis.faehigkeit.Ziele
import java.time.LocalDate

/**
 * Jarvis lernt aus dem Tagebuch, im Anschluss an die Tagesauswertungs-Synchronisation. Zwei kleine Schritte:
 * 1. Verdichten: Ein Monat, der ganz hinter den letzten 14 Tagen liegt, wird zu einem kurzen Text – je Lauf einer,
 *    bis alle aufgeholt sind. So bleibt das Tagebuch im Gesamtbild, auch wenn es Tausende Einträge werden.
 * 2. Lernen: Einmal am Tag liest Jarvis die neuesten Einträge und notiert bis zu drei neue dauerhafte Tatsachen über Frank.
 * Das Tagebuch ist Franks eigener Text; nur deshalb darf dieser Lauf selbst Notizen anlegen.
 */
object Lernen {
    suspend fun laufe(context: Context) {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (!e.auswertungLernen) return
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return
        val tagebuch = Register.alle(app).filterIsInstance<TagebuchFaehigkeit>().firstOrNull() ?: return
        suspend fun frage(anweisung: String, text: String): String? =
            runCatching { agent.auth.streamChat(anweisung, listOf(ChatTurn("user", text)), e.modell, e.denkstufe).trim() }.getOrNull()

        tagebuch.unverdichteterMonat()?.let { (monat, eintraege) ->
            frage(VERDICHTEN, "Monat $monat\n\n$eintraege")?.takeIf { it.length > 80 }?.let {
                tagebuch.speichereVerdichtung(monat, it.take(1500))
                Protokoll.melde(Quelle.JARVIS, "Tagebuch verdichtet", "Monat $monat liegt jetzt als Kurzfassung im Gesamtbild.")
            }
        }

        val heute = LocalDate.now().toString()
        if (e.lernlaufTag == heute) return
        val eintraege = tagebuch.rueckblick(2, 4000)
        if (!eintraege.startsWith("Kein Tagebucheintrag")) {
            val bekannt = "ZIELE (art ziel):\n" + Ziele.liste(Ziele.alle(app)).ifEmpty { "(noch nichts)" } + "\n\nINFOS (art info):\n" + UeberFrank.liste(UeberFrank.alle(app)).ifEmpty { "(noch nichts)" }
            // Ohne Antwort bleibt der Tag offen: Der nächste Lauf versucht es noch einmal.
            val antwort = frage(LERNEN, "SCHON GEMERKT:\n$bekannt\n\nTAGEBUCH DER LETZTEN TAGE:\n$eintraege") ?: return
            // Nur Zeilen im verlangten Muster und nur Sätze über Frank: So landet keine Einleitung und kein „Nichts Neues“ im Gedächtnis.
            val gelernt = antwort.lines().mapNotNull { ZEILE.find(it.trim()) }.take(3).mapNotNull { treffer ->
                val datei = if (treffer.groupValues[1].uppercase() == "ZIEL") Ziele else UeberFrank
                val satz = treffer.groupValues[3].trim()
                if (satz.length !in 12..300 || !satz.startsWith("Frank") || datei.alle(app).any { it.text.equals(satz, ignoreCase = true) }) null
                else datei.speichere(app, satz, treffer.groupValues[2].toIntOrNull())?.text
            }
            if (gelernt.isNotEmpty()) Protokoll.melde(Quelle.JARVIS, "Aus dem Tagebuch gelernt", gelernt.joinToString(" "))
        }
        e.lernlaufTag = heute
    }

    private const val VERDICHTEN =
        "Du verdichtest Franks Tagebuch eines Monats zu einem Gedächtnis für seinen Assistenten Jarvis. Schreibe in höchstens 1200 Zeichen, in ganzen Sätzen und ohne Aufzählungszeichen, " +
            "was in diesem Monat für Frank wichtig war: was er gemacht und erlebt hat, woran er gearbeitet hat, Gesundheit und Training, Menschen, wiederkehrende Themen, Entscheidungen " +
            "und besondere Ereignisse mit Datum. Nur was in den Einträgen steht, sachlich und ohne Wertung. Antworte nur mit diesem Text."

    /** „ZIEL: Satz“, „INFO: Satz“ oder mit id zum Ersetzen „INFO 4: Satz“. */
    private val ZEILE = Regex("^(ZIEL|INFO)\\s*\\[?(\\d+)?\\]?\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    private const val LERNEN =
        "Du pflegst für Franks Assistenten Jarvis das Gedächtnis darüber, wer Frank ist. Du bekommst, was schon gemerkt ist (mit id in eckigen Klammern), und Franks Tagebuch der letzten Tage. " +
            "Finde höchstens drei Dinge, die das Gedächtnis besser machen: ein neues ZIEL (was Frank erreichen will und im Tagebuch eindeutig so sagt), eine neue INFO (dauerhafte Tatsache: " +
            "Vorlieben und Abneigungen, was ihm wichtig ist, Besitz, Gewohnheiten, Menschen in seinem Leben, Gesundheitliches von Dauer) oder einen vorhandenen Eintrag, den das Tagebuch überholt " +
            "oder widerlegt hat. Nicht: was er an einem einzelnen Tag gemacht hat, Stimmungen, einzelne Termine, Vermutungen. Im Zweifel nichts. " +
            "Antworte je Ding mit einer Zeile in genau dieser Form, der Satz in der dritten Person und mit „Frank“ beginnend: " +
            "„ZIEL: Frank will …“ oder „INFO: Frank …“ für Neues; „ZIEL 3: Frank …“ oder „INFO 7: Frank …“, um den Eintrag mit dieser id durch die neue Fassung zu ersetzen. " +
            "Gibt es nichts, antworte nur mit dem Wort KEINE."
}
