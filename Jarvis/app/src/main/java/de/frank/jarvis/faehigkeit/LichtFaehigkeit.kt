package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.licht.LedStreifen
import de.frank.jarvis.licht.LedStreifen.Modus
import de.frank.jarvis.licht.LichtAutomatik
import org.json.JSONObject

/**
 * Franks LED-Streifen per Sprache (Bluetooth direkt, ohne KS-Lite-App; Handy und Mac gleichzeitig). Die Arbeit macht [LedStreifen],
 * die Automatik [LichtAutomatik]; ihre Schalter stehen im Einstellungskatalog (Bereich „LED-Licht“).
 */
class LichtFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "licht"
    override val name = "Licht"
    override val beschreibung = "Franks LED-Streifen in der Wohnung (Bluetooth): an, aus, Farbe, Helligkeit, Szenen und Mikrofon-Modi, alle Streifen immer gleich."
    override val hinweise =
        "Sagt Frank „mach das Licht an“, „Licht an“, „mach die LEDs an“, „LED-Licht an“ oder „Licht aus“, nimm licht mit aktion an oder aus: " +
            "Es schaltet alle LED-Streifen der Wohnung gleichzeitig, an immer mit dem zuletzt gewählten Zustand (Farbe, Helligkeit, Modus), auf allen Streifen gleich. " +
            "„Mach das Licht blau“ → aktion farbe mit farbe; „heller/dunkler/auf 30 Prozent“ → aktion helligkeit; „Szene bunter Sprung“, „mach einen Farbverlauf“ → aktion szene mit szene (Name oder 1 bis 23), " +
            "optional tempo 0 bis 100; „Licht nach Musik“, „Disco-Modus“ → aktion mikrofon mit modus (Klassisch, Sanft, Dynamisch, Disco) und optional empfindlichkeit 1 bis 7. " +
            "„Wie ist das Licht eingestellt?“ → aktion zustand. Die Automatik (abends an, um 22 bzw. 19 Uhr aus, aus beim Verlassen des Hauses) stellt Frank über das Werkzeug einstellungen, Bereich LED-Licht. " +
            "Melde danach kurz, wie viele Streifen geschaltet wurden; nennt das Ergebnis nicht erreichte Streifen oder einen Grund, sag das genau so."

    override fun stoerung(): String? = if (!LedStreifen.erlaubt(context)) "Für das Licht fehlt Jarvis das Recht „Geräte in der Nähe“ (Bluetooth)." else null

    private val aktionen = listOf("an", "aus", "farbe", "helligkeit", "szene", "mikrofon", "zustand")

    override val werkzeuge = listOf(
        Werkzeug(
            name = "licht",
            titel = "Licht schalten",
            beschreibung = "Jarvis: schaltet alle LED-Streifen in Franks Wohnung per Bluetooth direkt (ohne KS-Lite-App, Handy und Mac gleichzeitig), immer alle im selben Zustand. " +
                "Aktionen: an (letzter Zustand), aus, farbe, helligkeit (1 bis 100), szene (1 bis 23 oder Name, mit tempo), mikrofon (Modus nach Geräusch, mit empfindlichkeit), zustand (nur lesen).",
            schema = schema(
                "aktion" to text(aktionen.joinToString(", ") + ".", aktionen),
                "farbe" to text("Nur farbe: " + LedStreifen.FARBEN.keys.joinToString(", ") + " oder #RRGGBB."),
                "helligkeit" to text("1 bis 100 Prozent, oder heller/dunkler."),
                "szene" to text("Nur szene: " + LedStreifen.SZENEN.mapIndexed { i, n -> "${i + 1} $n" }.joinToString(", ") + "."),
                "tempo" to text("Nur szene: Tempo 0 bis 100."),
                "modus" to text("Nur mikrofon: " + LedStreifen.RHYTHMEN.joinToString(", ") + "."),
                "empfindlichkeit" to text("Nur mikrofon: 1 bis 7."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
            taetigkeit = "schaltet das Licht",
        ) { a -> ausfuehren(a) },
    )

    private fun zahl(t: String) = Regex("\\d+").find(t)?.value?.toIntOrNull()

    private suspend fun ausfuehren(a: JSONObject): Ergebnis {
        val alt = LedStreifen.zustand(context) ?: LedStreifen.Zustand()
        val hText = a.optString("helligkeit").trim().lowercase()
        val hell = when {
            hText.isEmpty() -> alt.hell
            "hell" in hText || "mehr" in hText -> alt.hell + 25
            "dunk" in hText || "weniger" in hText -> alt.hell - 25
            "max" in hText || "voll" in hText -> 100
            else -> zahl(hText) ?: return Ergebnis("RÜCKFRAGE: Welche Helligkeit, 1 bis 100 Prozent?", true)
        }.coerceIn(1, 100)
        val b = when (a.optString("aktion").trim().lowercase()) {
            "an", "ein", "einschalten", "on" -> LedStreifen.an(context)
            "aus", "ausschalten", "off" -> LedStreifen.aus(context)
            "farbe" -> {
                val rgb = LedStreifen.farbe(a.optString("farbe")) ?: return Ergebnis("RÜCKFRAGE: Welche Farbe? Möglich: ${LedStreifen.FARBEN.keys.joinToString(", ")} oder #RRGGBB.", true)
                LedStreifen.setzen(context, alt.copy(modus = Modus.FARBE, rgb = rgb, hell = hell))
            }
            "helligkeit" -> LedStreifen.setzen(context, alt.copy(modus = if (alt.modus == Modus.RHYTHMUS) Modus.FARBE else alt.modus, hell = hell))
            "szene" -> {
                val s = LedStreifen.szene(a.optString("szene")) ?: return Ergebnis("RÜCKFRAGE: Welche Szene? ${LedStreifen.SZENEN.mapIndexed { i, n -> "${i + 1} $n" }.joinToString(", ")}.", true)
                LedStreifen.setzen(context, alt.copy(modus = Modus.SZENE, szene = s, hell = hell, tempo = zahl(a.optString("tempo"))?.coerceIn(0, 100) ?: alt.tempo))
            }
            "mikrofon" -> {
                val m = a.optString("modus").trim().lowercase()
                val r = LedStreifen.RHYTHMEN.indexOfFirst { it.lowercase() == m }.takeIf { it >= 0 }?.plus(1) ?: zahl(m)?.takeIf { it in 1..4 } ?: alt.rhythmus
                LedStreifen.setzen(context, alt.copy(modus = Modus.RHYTHMUS, rhythmus = r, empfindlichkeit = zahl(a.optString("empfindlichkeit"))?.coerceIn(1, 7) ?: alt.empfindlichkeit))
            }
            "zustand" -> {
                val f = LichtAutomatik.fenster(context)
                val uhr = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                return Ergebnis(
                    "Eingestellt: ${LedStreifen.zustand(context)?.text() ?: "noch nichts gemerkt (beim nächsten Einschalten liest Jarvis die Streifen)"}. " +
                        (f?.let { "Automatik heute (${it.art}): an ab ${it.an.format(uhr)} Uhr, aus um ${it.aus.format(uhr)} Uhr." } ?: "Heute schaltet die Automatik nicht.") +
                        (LedStreifen.letzterBericht?.let { " Zuletzt: ${it.text}" } ?: ""),
                )
            }
            else -> return Ergebnis("RÜCKFRAGE: Licht an, aus, eine Farbe, eine Helligkeit, eine Szene oder ein Mikrofon-Modus?", true)
        }
        return Ergebnis(b.text, b.fehler)
    }
}
