package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.staubsauger.ProscenicBedienung
import de.frank.jarvis.staubsauger.ProscenicSteuerung

/** Saugroboter Proscenic M7 Pro: Jarvis bedient dafür die Proscenic-App über die Bedienungshilfe „Jarvis Staubsauger“. */
class StaubsaugerFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "staubsauger"
    override val name = "Staubsauger"
    override val beschreibung = "Saugroboter Proscenic M7 Pro starten, Tiefenreinigung, anhalten, zur Station, orten, Staubsammlung, Akku und Zustand."
    override val hinweise =
        "Sagt Frank „Staubsauger“, „Saugroboter“, „M7 Pro“, „starte eine Reinigung“ oder „Tiefenreinigung“, nimm staubsauger. " +
            "Jarvis tippt dafür in der Proscenic-App; das dauert etwa 10 bis 20 Sekunden und braucht ein entsperrtes Handy. " +
            "Antworte danach kurz mit dem Ergebnis, etwa: „Tiefenreinigung läuft, Akku 100 Prozent.“"

    /** Öffnet eine andere App und tippt darin: nur auf Franks Wunsch im Gespräch, nie in Hintergrund-Agenten. */
    override val nurImGespraech: Boolean get() = true

    override fun stoerung(): String? =
        if (!ProscenicBedienung.eingeschaltet(context)) "Die Bedienungshilfe „Jarvis Staubsauger“ ist in den Android-Einstellungen aus." else null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "staubsauger",
            titel = "Saugroboter",
            beschreibung = "Jarvis: Saugroboter Proscenic M7 Pro über die Proscenic-App. „Starte eine Reinigung“ / „Staubsauger los“ → reinigen. " +
                "„Tiefenreinigung“ → tiefenreinigung. „Staubsauger stopp“ / „Halt den Staubsauger an“ → stoppen (ein bloßes „Stopp“ ohne Staubsauger gilt nicht dem Sauger). „Zurück zur Station“ / „Laden“ → station. " +
                "„Wo ist der Staubsauger?“ / „Gerät positionieren“ → positionieren. „Staub absaugen“ / „Ein-Klick-Staubsammlung“ → staubsammlung. " +
                "„Wie voll ist der Akku vom Staubsauger?“ / „Was macht der Staubsauger?“ → status.",
            schema = schema(
                "aktion" to text(
                    "reinigen, tiefenreinigung, stoppen, station, positionieren, staubsammlung oder status.",
                    ProscenicSteuerung.Aktion.entries.map { it.wort },
                ),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
            taetigkeit = "bedient den Saugroboter",
        ) { a ->
            val aktion = ProscenicSteuerung.Aktion.aus(a.optString("aktion"))
            if (aktion == null) Ergebnis("Unbekannte Aktion für den Saugroboter.", fehler = true)
            else ProscenicSteuerung.ausfuehren(context, aktion).let { Ergebnis(it.text, it.fehler) }
        },
    )
}
