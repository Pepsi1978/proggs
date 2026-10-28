package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.ablage.Prompts

/** Prompt-Speicher im Reiter „Prompts“ der Ablage: Frank legt vorformulierte Aufträge per Sprache ab und ruft sie später wieder ab. */
class PromptsFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "prompts"
    override val name = "Prompts"
    override val beschreibung = "Vorformulierte Aufträge (Prompts) nummeriert speichern, lesen, ändern und löschen."
    override val hinweise =
        "Sagt Frank „speicher folgendes als Prompt ab“ (auch „Promt“, „Prompt speichern“, „als Prompt ablegen“), nimm prompt_speichern mit aktion speichern: " +
            "Formuliere das Diktierte danach sinnvoll als klaren, vollständigen Auftrag in gutem Deutsch aus, behalte jeden Punkt, jeden Namen und jede Zahl, " +
            "berichtige Hörfehler und lass Füllwörter weg. Führe den Auftrag nicht aus, speichere ihn nur. Antworte danach kurz, etwa: „Als Prompt 3 gespeichert.“"

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "prompt_speichern",
            titel = "Prompt speichern",
            beschreibung = "Jarvis: Prompt-Speicher im Reiter „Prompts“ der Ablage (neue Prompts bekommen die kleinste freie Nummer). „Speicher folgendes als Prompt ab: …“ → aktion speichern " +
                "mit dem ausformulierten Text (nicht ausführen). „Ändere Prompt 2 in …“ → aktion aendern mit nummer und dem vollständigen neuen Text. „Lösche Prompt 2“ → aktion loeschen mit nummer, nur wenn Frank die Nummer ausdrücklich nennt.",
            schema = schema(
                "aktion" to text("speichern, aendern oder loeschen.", listOf("speichern", "aendern", "loeschen")),
                "text" to text("Beim Speichern und Ändern: der Prompt, sinnvoll ausformuliert als klarer Auftrag in gutem Deutsch."),
                "nummer" to zahl("Beim Ändern und Löschen: Nummer des Prompts."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            val nummer = a.optInt("nummer", 0)
            if (a.optString("aktion") == "loeschen") {
                Prompts.loesche(context, nummer)?.let { Ergebnis("Prompt ${it.nummer} gelöscht.") }
                    ?: Ergebnis("Prompt $nummer ließ sich nicht löschen (gibt es nicht oder Datei beschädigt).", fehler = true)
            } else if (a.optString("aktion") == "aendern") {
                val text = DiktatTrenner.ohneSchluss(a.optString("text")).trim()
                if (Prompts.aendere(context, nummer, text)) Ergebnis("Prompt $nummer geändert.")
                else Ergebnis("Prompt $nummer ließ sich nicht ändern (gibt es nicht, kein Text oder Datei beschädigt).", fehler = true)
            } else {
                val text = DiktatTrenner.ohneSchluss(a.optString("text")).trim()
                if (text.isEmpty()) Ergebnis("Kein Text zum Speichern angegeben.", fehler = true)
                else Prompts.speichere(context, text)?.let { Ergebnis("Als Prompt ${it.nummer} im Reiter Prompts der Ablage gespeichert.") }
                    ?: Ergebnis("Der Prompt ließ sich nicht speichern (Datei beschädigt, Details unter Aktivität).", fehler = true)
            }
        },
        Werkzeug(
            name = "prompts_lesen",
            titel = "Prompts lesen",
            beschreibung = "Jarvis: liest die gespeicherten Prompts aus dem Reiter „Prompts“ der Ablage. „Welche Prompts habe ich?“ → ohne nummer. „Lies Prompt 2 vor“ → nummer 2.",
            schema = schema("nummer" to zahl("Optional: nur diesen Prompt.")),
            nurLesen = true,
        ) { a ->
            val nummer = a.optInt("nummer", 0)
            val alle = Prompts.alle(context)
            when {
                Prompts.defekt(context) -> Ergebnis("Die Prompt-Datei ist beschädigt und wurde gesichert; Details unter Aktivität.", fehler = true)
                nummer > 0 -> alle.firstOrNull { it.nummer == nummer }?.let { Ergebnis("Prompt ${it.nummer}:\n${it.text}") }
                    ?: Ergebnis("Einen Prompt mit der Nummer $nummer gibt es nicht.", fehler = true)
                alle.isEmpty() -> Ergebnis("Noch keine Prompts gespeichert.")
                else -> Ergebnis(alle.joinToString("\n\n") { "Prompt ${it.nummer}:\n${it.text}" })
            }
        },
    )
}
