package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.timer.Stoppuhr
import de.frank.jarvis.timer.StoppuhrStand

/** Stoppuhr in Jarvis: „Starte eine Stoppuhr“ startet sofort; sie bleibt auf der Startseite stehen, bis Frank sie zurücksetzt. */
class StoppuhrFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "stoppuhr"
    override val name = "Stoppuhr"
    override val beschreibung = "Stoppuhr starten, anhalten, fortsetzen, ablesen und zurücksetzen."
    override val hinweise =
        "Sagt Frank „Stoppuhr“ oder „Zeit stoppen“, nimm stoppuhr, nicht timer: Ein Timer zählt rückwärts, die Stoppuhr vorwärts. " +
            "„Starte eine Stoppuhr“ startet sofort von null. Antworte danach kurz, etwa: „Stoppuhr läuft.“ Beim Ablesen nenne die Zeit in Worten."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "stoppuhr",
            titel = "Stoppuhr",
            beschreibung = "Jarvis: Stoppuhr in der Jarvis-App. „Starte eine Stoppuhr“ → aktion starten (startet sofort von null). " +
                "„Stopp die Stoppuhr“ / „Halt die Stoppuhr an“ → anhalten. „Weiter“ → fortsetzen. „Wie lange läuft die Stoppuhr?“ → lesen. " +
                "„Setz die Stoppuhr zurück“ → zuruecksetzen.",
            schema = schema(
                "aktion" to text("starten, anhalten, fortsetzen, lesen oder zuruecksetzen.", listOf("starten", "anhalten", "fortsetzen", "lesen", "zuruecksetzen")),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            when (a.optString("aktion")) {
                "starten" -> Stoppuhr.starten(context).let { Ergebnis("Stoppuhr gestartet um ${uhr(it.gestartetUm)} Uhr. Sie steht auf der Startseite von Jarvis.") }
                "anhalten" -> {
                    val vorher = Stoppuhr.aktuell(context)
                    if (!vorher.aktiv) Ergebnis("Es läuft keine Stoppuhr.")
                    else Stoppuhr.anhalten(context).let { Ergebnis("Stoppuhr angehalten bei ${worte(it.gesamt())}. Sie bleibt auf der Startseite stehen.") }
                }
                "fortsetzen" -> Stoppuhr.fortsetzen(context).let { Ergebnis("Stoppuhr läuft weiter, bisher ${worte(it.gesamt())}.") }
                "zuruecksetzen" -> {
                    val vorher = Stoppuhr.aktuell(context)
                    Stoppuhr.zuruecksetzen(context)
                    Ergebnis(if (vorher.aktiv) "Stoppuhr zurückgesetzt, sie stand bei ${worte(vorher.gesamt())}." else "Es lief keine Stoppuhr.")
                }
                else -> Ergebnis(lesen(Stoppuhr.aktuell(context)))
            }
        },
    )

    private fun lesen(s: StoppuhrStand): String = when {
        !s.aktiv -> "Es läuft keine Stoppuhr."
        s.laeuft -> "Die Stoppuhr läuft seit ${uhr(s.gestartetUm)} Uhr: ${worte(s.gesamt())} (${Stoppuhr.anzeige(s.gesamt())})."
        else -> "Die Stoppuhr ist angehalten bei ${worte(s.gesamt())} (${Stoppuhr.anzeige(s.gesamt())})."
    }

    private fun worte(ms: Long): String = de.frank.jarvis.timer.Timer.dauerText(ms)
    private fun uhr(ms: Long?): String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.GERMAN).format(java.util.Date(ms ?: System.currentTimeMillis()))
}
