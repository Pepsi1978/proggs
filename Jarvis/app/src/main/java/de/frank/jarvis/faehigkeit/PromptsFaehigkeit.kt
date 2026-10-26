package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.ablage.Prompts

/** Prompt-Speicher im Reiter „Prompts“ der Ablage: Frank legt vorformulierte Aufträge per Sprache ab und ruft sie später wieder ab. */
class PromptsFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "prompts"
    override val name = "Prompts"
    override val beschreibung = "Vorformulierte Aufträge (Prompts) nummeriert speichern, lesen und löschen."
    override val hinweise =
        "Sagt Frank „speicher folgendes als Prompt ab“ (auch „Promt“, „Prompt speichern“, „als Prompt ablegen“), nimm prompts mit aktion speichern: " +
            "Formuliere das Diktierte danach sinnvoll als klaren, vollständigen Auftrag in gutem Deutsch aus, behalte jeden Punkt, jeden Namen und jede Zahl, " +
            "berichtige Hörfehler und lass Füllwörter weg. Führe den Auftrag nicht aus, speichere ihn nur. Antworte danach kurz, etwa: „Als Prompt 3 gespeichert.“"

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "prompts",
            titel = "Prompts",
            beschreibung = "Jarvis: Prompt-Speicher im Reiter „Prompts“ der Ablage (fortlaufend nummeriert). „Speicher folgendes als Prompt ab: …“ → aktion speichern " +
                "mit dem ausformulierten Text (nicht ausführen). „Welche Prompts habe ich?“ / „Lies Prompt 2 vor“ → lesen (optional nummer). „Lösche Prompt 2“ → loeschen mit nummer.",
            schema = schema(
                "aktion" to text("speichern, lesen oder loeschen.", listOf("speichern", "lesen", "loeschen")),
                "text" to text("Beim Speichern: der Prompt, sinnvoll ausformuliert als klarer Auftrag in gutem Deutsch."),
                "nummer" to zahl("Beim Lesen (optional) oder Löschen: Nummer des Prompts."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            val nummer = a.optInt("nummer", 0)
            when (a.optString("aktion")) {
                "speichern" -> {
                    val text = DiktatTrenner.ohneSchluss(a.optString("text")).trim()
                    if (text.isEmpty()) Ergebnis("Kein Text zum Speichern angegeben.", fehler = true)
                    else Prompts.speichere(context, text)?.let { Ergebnis("Als Prompt ${it.nummer} im Reiter Prompts der Ablage gespeichert.") }
                        ?: Ergebnis("Der Prompt ließ sich nicht speichern (Datei beschädigt, Details unter Aktivität).", fehler = true)
                }
                "loeschen" -> Prompts.loesche(context, nummer)?.let { Ergebnis("Prompt ${it.nummer} gelöscht.") }
                    ?: Ergebnis("Einen Prompt mit der Nummer $nummer gibt es nicht.", fehler = true)
                else -> {
                    val alle = Prompts.alle(context)
                    if (nummer > 0) alle.firstOrNull { it.nummer == nummer }?.let { Ergebnis("Prompt ${it.nummer}:\n${it.text}") }
                        ?: Ergebnis("Einen Prompt mit der Nummer $nummer gibt es nicht.", fehler = true)
                    else if (alle.isEmpty()) Ergebnis("Noch keine Prompts gespeichert.")
                    else Ergebnis(alle.joinToString("\n\n") { "Prompt ${it.nummer}:\n${it.text}" })
                }
            }
        },
    )
}
