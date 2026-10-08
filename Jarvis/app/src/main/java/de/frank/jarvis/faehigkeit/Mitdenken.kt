package de.frank.jarvis.faehigkeit

import android.content.Context
import java.time.LocalDate
import java.util.Locale

/**
 * Jarvis denkt mit: Zu einem Tag und einer Uhrzeit trägt er zusammen, was Franks Vorhaben berührt —
 * Dienst und Schlafzeiten, andere Termine und, bei Vorhaben im Freien, das Wetter.
 *
 * Gerechnet wird nach festen Regeln aus Kalender und gespeicherter Wettervorhersage, ohne Modell und ohne Netz.
 * Die Hinweise hängen an den Antworten der schreibenden Werkzeuge (Aufgabe, Termin, Wecker); das Sprachmodell
 * entscheidet, was davon es Frank sagt.
 */
object Mitdenken {
    private val DRAUSSEN = Regex(
        "lauf|jogg|trail|wald|fahrrad|\\brad\\b|radfahr|spazier|wander|garten|rasen|mäh|draußen|draussen|grill|\\bpark\\b|\\bsee\\b|baden|terrasse|balkon|auto waschen|fenster putz|angeln|zelten|im freien",
        RegexOption.IGNORE_CASE,
    )

    fun istDraussen(text: String): Boolean = DRAUSSEN.containsMatchIn(text)

    /**
     * Die Hinweise zu einem Zeitpunkt. [minuten] = Uhrzeit in Minuten nach Mitternacht oder null, wenn nur der Tag feststeht.
     * [vorhaben] ist Franks Text; er entscheidet, ob das Wetter eine Rolle spielt.
     */
    fun hinweise(context: Context, tag: LocalDate, minuten: Int?, vorhaben: String, mitWetter: Boolean = true): List<String> {
        val liste = mutableListOf<String>()
        val alle = Register.alle(context)
        val lage = runCatching { alle.filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.lage(tag) }.getOrNull()
        if (lage != null) {
            if (minuten != null) {
                lage.belegt.firstOrNull { minuten >= it.von && minuten < it.bis }?.let { liste += "Zu dieser Zeit passt es laut Dienstplan nicht: ${it.grund}." }
                    ?: run { if (lage.arbeitet) liste += "An dem Tag: ${lage.dienstText}." }
                lage.termine.filter { it.von != null && minuten < (it.bis ?: (it.von + 60)) && minuten + 60 > it.von }.take(3).forEach { t ->
                    liste += "Zur selben Zeit steht im Kalender: ${t.titel} (${uhr(t.von!!)}–${uhr(t.bis ?: (t.von + 60))} Uhr)."
                }
            } else {
                if (lage.arbeitet) liste += "An dem Tag: ${lage.dienstText}."
                else lage.belegt.firstOrNull()?.let { liste += "An dem Tag zu beachten: ${it.grund}." }
            }
            lage.termine.filter { it.von == null }.take(3).takeIf { it.isNotEmpty() }?.let { ganztags -> liste += "Am selben Tag im Kalender: " + ganztags.joinToString(", ") { it.titel } + "." }
        }
        if (mitWetter && minuten != null && istDraussen(vorhaben)) {
            val wetter = alle.filterIsInstance<WetterFaehigkeit>().firstOrNull()
            runCatching { wetter?.zurZeit(tag, minuten) }.getOrNull()?.let { liste += "Wetter dann: " + wetter!!.einschaetzung(it) }
        }
        return liste
    }

    /**
     * Fertiger Anhang für die Antwort eines schreibenden Werkzeugs. [datum] als JJJJ-MM-TT, [uhrzeit] als HH:MM;
     * beides darf fehlen oder unlesbar sein, dann gibt es nichts mitzudenken.
     */
    fun anhang(context: Context, datum: String?, uhrzeit: String?, vorhaben: String): String {
        val tag = runCatching { LocalDate.parse(datum.orEmpty().take(10)) }.getOrNull() ?: return ""
        val minuten = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)").find(uhrzeit.orEmpty())?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
        val liste = runCatching { hinweise(context, tag, minuten, vorhaben) }.getOrDefault(emptyList())
        if (liste.isEmpty()) return ""
        return "\nMITGEDACHT (sage Frank davon, was für sein Vorhaben wichtig ist, in einem Satz; Unwichtiges weglassen):\n" + liste.joinToString("\n") { "- $it" }
    }

    private fun uhr(minuten: Int): String = String.format(Locale.GERMAN, "%d:%02d", minuten / 60 % 24, minuten % 60)
}
