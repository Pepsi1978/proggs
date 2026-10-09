package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.UeberFrank
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
            val bekannt = UeberFrank.alle(app)
            // Ohne Antwort bleibt der Tag offen: Der nächste Lauf versucht es noch einmal.
            val antwort = frage(LERNEN, "SCHON NOTIERT:\n" + UeberFrank.liste(bekannt).ifEmpty { "(noch nichts)" } + "\n\nTAGEBUCH DER LETZTEN TAGE:\n" + eintraege) ?: return
            val neu = antwort.lines().map { it.trim().trimStart('-', '•', '*').trim() }
                .filter { it.length in 12..300 && !it.startsWith("KEINE", ignoreCase = true) && bekannt.none { b -> b.text.equals(it, ignoreCase = true) } }.take(3)
            neu.forEach { UeberFrank.speichere(app, it) }
            if (neu.isNotEmpty()) Protokoll.melde(Quelle.JARVIS, "Aus dem Tagebuch gelernt", neu.joinToString(" "))
        }
        e.lernlaufTag = heute
    }

    private const val VERDICHTEN =
        "Du verdichtest Franks Tagebuch eines Monats zu einem Gedächtnis für seinen Assistenten Jarvis. Schreibe in höchstens 1200 Zeichen, in ganzen Sätzen und ohne Aufzählungszeichen, " +
            "was in diesem Monat für Frank wichtig war: was er gemacht und erlebt hat, woran er gearbeitet hat, Gesundheit und Training, Menschen, wiederkehrende Themen, Entscheidungen " +
            "und besondere Ereignisse mit Datum. Nur was in den Einträgen steht, sachlich und ohne Wertung. Antworte nur mit diesem Text."

    private const val LERNEN =
        "Du pflegst für Franks Assistenten Jarvis die Notizen darüber, wer Frank ist. Du bekommst, was schon notiert ist, und Franks Tagebuch der letzten Tage. " +
            "Finde höchstens drei NEUE dauerhafte Tatsachen über Frank, die noch nicht notiert sind: Ziele, Vorlieben und Abneigungen, was ihm wichtig ist, was er besitzt, Gewohnheiten, " +
            "Menschen in seinem Leben, Gesundheitliches von Dauer. Nicht: was er an einem einzelnen Tag gemacht hat, Stimmungen, einzelne Termine, Vermutungen. Im Zweifel nichts. " +
            "Schreibe jede Tatsache als einen kurzen, für sich allein verständlichen Satz über Frank in der dritten Person auf eine eigene Zeile, ohne Aufzählungszeichen. " +
            "Gibt es nichts Neues, antworte nur mit dem Wort KEINE."
}
