package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.timer.Timer

/**
 * Timer direkt in Jarvis: „Starte einen Timer für eine Stunde“, „… für 33 Minuten“. Läuft exakt über den AlarmManager,
 * erscheint mit Restzeit auf der Startseite und klingelt am Ende mit dem Weckton.
 */
class TimerFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "timer"
    override val name = "Timer"
    override val beschreibung = "Timer starten, laufende Timer lesen und stoppen."
    override val hinweise =
        "Timer laufen in Jarvis selbst, auf die Sekunde genau, und klingeln am Ende. Sagt Frank „Timer“ oder „in X Minuten Bescheid sagen“, nimm timer, " +
            "nicht den Wecker und keine Aufgabe. Rechne die genannte Dauer in Stunden, Minuten und Sekunden um (eine Stunde = stunden 1, " +
            "anderthalb Stunden = stunden 1 und minuten 30). Antworte danach kurz mit Dauer und Uhrzeit, zu der der Timer klingelt."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "timer",
            titel = "Timer",
            beschreibung = "Jarvis: startet einen Timer in der Jarvis-App, liest die laufenden Timer oder stoppt einen. " +
                "Beispiele: „Starte einen Timer für 33 Minuten“ → aktion starten, minuten 33. „Timer für 2 Stunden“ → stunden 2. " +
                "„Wie lange läuft der Timer noch?“ → aktion lesen. „Stopp den Timer“ oder „Aus“, während er klingelt → aktion stoppen (ohne id: erst das Klingeln, sonst der zuerst ablaufende).",
            schema = schema(
                "aktion" to text("starten, lesen oder stoppen.", listOf("starten", "lesen", "stoppen")),
                "stunden" to zahl("Stunden der Dauer (beim Starten)."),
                "minuten" to zahl("Minuten der Dauer (beim Starten)."),
                "sekunden" to zahl("Sekunden der Dauer (beim Starten)."),
                "name" to text("Optionaler Name, z. B. Nudeln oder Pause."),
                "id" to zahl("Nummer des Timers zum Stoppen (aus lesen). Weglassen = der zuerst ablaufende; alle=true stoppt alle."),
                "alle" to schalter("true = beim Stoppen alle Timer anhalten."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            when (a.optString("aktion")) {
                "starten" -> {
                    val ms = (a.optLong("stunden") * 3600 + a.optLong("minuten") * 60 + a.optLong("sekunden")) * 1000
                    if (ms <= 0) Ergebnis("Keine Dauer angegeben. Frag Frank, wie lange der Timer laufen soll.", fehler = true)
                    else if (ms > 24 * 3_600_000L) Ergebnis("Ein Timer läuft höchstens 24 Stunden. Für Längeres einen Wecker stellen.", fehler = true)
                    else {
                        val t = Timer.starten(context, ms, a.optString("name"))
                        Ergebnis("Timer ${t.id} „${t.name}“ läuft: ${Timer.dauerText(ms)}, klingelt um ${uhr(t.ende)} Uhr. Er steht auf der Startseite von Jarvis.")
                    }
                }
                "stoppen" -> {
                    val alle = Timer.alle(context)
                    val klingelnd = Timer.klingelt.value
                    when {
                        klingelnd.isNotEmpty() && !a.has("id") && (!a.optBoolean("alle") || alle.isEmpty()) -> { Timer.klingelnAus(context, erledigt = true); Ergebnis("Klingeln ausgeschaltet (${klingelnd.joinToString { "„${it.name}“" }}).") }
                        alle.isEmpty() -> Ergebnis("Es läuft kein Timer.")
                        a.optBoolean("alle") -> { Timer.klingelnAus(context, erledigt = true); alle.forEach { Timer.stoppen(context, it.id) }; Ergebnis("Alle ${alle.size} Timer gestoppt.") }
                        else -> {
                            val t = if (a.has("id") && !a.isNull("id")) alle.firstOrNull { it.id == a.optInt("id") } else alle.first()
                            if (t == null) Ergebnis("Keinen Timer mit Nummer ${a.optInt("id")} gefunden. Laufend: " + liste(), fehler = true)
                            else { Timer.stoppen(context, t.id); Ergebnis("Timer ${t.id} „${t.name}“ gestoppt, es waren noch ${Timer.dauerText(t.ende - System.currentTimeMillis())} übrig.") }
                        }
                    }
                }
                else -> Ergebnis(liste())
            }
        },
    )

    private fun liste(): String {
        val alle = Timer.alle(context)
        if (alle.isEmpty()) return "Es läuft kein Timer."
        val jetzt = System.currentTimeMillis()
        return alle.joinToString("\n") { "- Timer ${it.id} „${it.name}“ (${Timer.dauerText(it.dauerMs)}): noch ${Timer.dauerText(it.ende - jetzt)}, klingelt um ${uhr(it.ende)} Uhr" }
    }

    private fun uhr(ms: Long): String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.GERMAN).format(java.util.Date(ms))
}
