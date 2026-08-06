package de.frank.cortex.tts

/**
 * Der Text, den Frank waehrend der Stimm-Aufnahme vorliest.
 *
 * Alibaba nimmt hoechstens 60 Sekunden Referenz-Ton an, deshalb ist der Text auf ein
 * Wort-Budget zugeschnitten statt auf eine Satz-Anzahl: ruhig gelesen dauern [TARGET_WORDS]
 * Woerter etwa 50 Sekunden.
 */
object VoiceSampleScript {
    const val TARGET_WORDS = 105

    /**
     * Einfache, kurze Woerter — jeder soll den Text ohne Stolpern vorlesen koennen. Die Satzformen
     * wechseln aber bewusst (Aussage, Frage, Ausruf, kurz gegen lang): daraus lernt das Modell die
     * Betonung, und eine Probe aus lauter gleichen Saetzen klaenge spaeter monoton.
     */
    val lines: List<String> = listOf(
        "Ich lese diesen Text ruhig vor, damit meine Stimme später genauso klingt wie jetzt.",
        "Draußen ist es hell, und der Wind bewegt die Blätter vor dem Fenster.",
        "Ist das nicht ein guter Tag, um etwas Neues zu beginnen?",
        "Am Morgen trinke ich einen Kaffee, schaue kurz aus dem Fenster und denke an gar nichts.",
        "Manchmal reicht ein kurzer Satz. Manchmal braucht es einen langen, der in Ruhe zu Ende geht.",
        "Wie schön, dass wir uns endlich wiedersehen!",
        "Ich mag es, wenn ein Tag langsam beginnt und niemand mich drängt.",
        "Und jetzt lese ich noch diesen letzten Satz, ganz ruhig, so wie den ersten."
    )

    /** Wie viele Woerter der Vorlesetext hat — dient dem Abgleich mit [TARGET_WORDS]. */
    fun wordCount(): Int = lines.sumOf { line ->
        line.split(' ', '\n', '\t').count(String::isNotBlank)
    }
}
