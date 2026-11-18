package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.kaffee.Kaffeemaschine
import org.json.JSONObject

/**
 * Kaffeemaschine De'Longhi PrimaDonna Soul: alles, was die Kaffee-Karte auf dem Startbildschirm kann, auch per Sprache.
 * „Mach mir einen Kaffee“ brüht Franks Standard (Long Black); Jarvis weckt die Maschine vorher selbst aus dem Standby.
 */
class KaffeeFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "kaffee"
    override val name = "Kaffeemaschine"
    override val beschreibung = "Kaffeemaschine De'Longhi PrimaDonna Soul: Kaffee und alle Getränke zubereiten, aufwecken, ausschalten, stoppen, Zustand, " +
        "Meldungen (Wassertank, Satzbehälter, Entkalken) und Einstellungen (automatisch aus, Wasserhärte)."
    override val hinweise =
        "Sagt Frank „mach mir einen Kaffee“, „mach mir meinen Kaffee“ oder „Kaffee bitte“ ohne Sorte, nimm kaffeemaschine mit aktion kaffee: " +
            "Das ist sein Standard, ein Long Black. Nennt er eine Sorte (Espresso, Cappuccino, Americano …), aktion getraenk mit name. " +
            "Jarvis weckt die Maschine selbst auf und wartet die Aufheiz-Spülung ab; das dauert bis zu zwei Minuten. Nicht nach der Tasse fragen. " +
            "Meldet das Werkzeug einen Fehler (Wassertank leer, offline …), sag Frank genau den Grund. Antworte danach kurz, etwa: „Dein Long Black läuft.“"

    override fun stoerung(): String? =
        if (!Kaffeemaschine.konfiguriert(context)) "Für die Kaffeemaschine fehlt die De'Longhi-Anmeldung unter Konten und APIs." else null

    private val aktionen = listOf("kaffee", "getraenk", "status", "aufwecken", "ausschalten", "stoppen", "einstellen", "getraenke")

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "kaffeemaschine",
            titel = "Kaffeemaschine",
            beschreibung = "Jarvis: Kaffeemaschine De'Longhi PrimaDonna Soul. Aktionen: kaffee (Franks Standard Long Black, „mach mir einen Kaffee“), " +
                "getraenk mit name (" + Kaffeemaschine.GETRAENKE.joinToString(", ") { it.name } + "), getraenke (Liste vorlesen), " +
                "status (bereit, Standby, Meldungen, Einstellungen), aufwecken (einschalten), ausschalten (Standby), stoppen (laufende Zubereitung abbrechen), " +
                "einstellen mit einstellung abschaltzeit (wert 15 Minuten, 30 Minuten, 1 Stunde, 3 Stunden; länger geht an der Soul nicht) " +
                "oder wasserhaerte (wert 1 bis 4).",
            schema = schema(
                "aktion" to text(aktionen.joinToString(", ") + ".", aktionen),
                "name" to text("Nur getraenk: Name des Getränks, etwa Espresso oder Cappuccino."),
                "einstellung" to text("Nur einstellen: abschaltzeit oder wasserhaerte.", listOf("abschaltzeit", "wasserhaerte")),
                "wert" to text("Nur einstellen: Abschaltzeit (15 Minuten, 30 Minuten, 1 Stunde, 3 Stunden) oder Wasserhärte 1 bis 4."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
            taetigkeit = "bedient die Kaffeemaschine",
        ) { a -> ausfuehren(a) },
    )

    private suspend fun ausfuehren(a: JSONObject): Ergebnis = try {
        when (val aktion = a.optString("aktion").trim().lowercase()) {
            "kaffee" -> Ergebnis("Dein " + Kaffeemaschine.bruehen(context, Kaffeemaschine.STANDARD))
            "getraenk" -> {
                val g = Kaffeemaschine.finden(a.optString("name"))
                    ?: return Ergebnis("RÜCKFRAGE: Welches Getränk? Möglich sind: ${Kaffeemaschine.GETRAENKE.joinToString(", ") { it.name }}.", true)
                Ergebnis(Kaffeemaschine.bruehen(context, g))
            }
            "getraenke" -> Ergebnis("Die Kaffeemaschine kann: ${Kaffeemaschine.GETRAENKE.joinToString(", ") { it.name }}. Standard ist der Long Black.")
            "status" -> {
                val z = Kaffeemaschine.zustand(context)
                val einst = Kaffeemaschine.EINSTELLUNGEN.mapNotNull { e -> z.einstellungen[e.schluessel]?.let { e.werte[it] }?.let { "${e.name}: $it" } }
                Ergebnis(
                    "Kaffeemaschine: ${z.text()}." + (if (z.alarme.isNotEmpty()) " Meldungen: ${z.alarme.joinToString(", ")}." else " Keine Meldungen.") +
                        (if (einst.isNotEmpty()) " " + einst.joinToString(", ") + "." else ""),
                )
            }
            "aufwecken" -> Ergebnis(Kaffeemaschine.aufwecken(context))
            "ausschalten" -> Ergebnis(Kaffeemaschine.ausschalten(context))
            "stoppen" -> Ergebnis(Kaffeemaschine.stoppen(context))
            "einstellen" -> einstellen(a)
            else -> Ergebnis("Unbekannte Aktion „$aktion“ für die Kaffeemaschine.", true)
        }
    } catch (e: Kaffeemaschine.Fehler) {
        Ergebnis(e.message ?: "Die Kaffeemaschine hat nicht reagiert.", true)
    } catch (e: Exception) {
        Ergebnis("Die Kaffeemaschine ist gerade nicht erreichbar (${e.javaClass.simpleName}).", true)
    }

    private suspend fun einstellen(a: JSONObject): Ergebnis {
        val e = Kaffeemaschine.EINSTELLUNGEN.firstOrNull { it.schluessel == a.optString("einstellung").trim().lowercase() }
            ?: return Ergebnis("RÜCKFRAGE: Welche Einstellung: automatisch aus (Abschaltzeit) oder Wasserhärte?", true)
        val w = a.optString("wert").trim().lowercase()
        val wert = when (e.schluessel) {
            "abschaltzeit" -> when {
                w.startsWith("15") -> 0
                w.startsWith("30") -> 1
                w.startsWith("1") || "eine stunde" in w -> 2
                w.startsWith("3") || "drei" in w || w.startsWith("12") || "länger" in w || "max" in w -> 3
                else -> null
            }
            else -> w.filter(Char::isDigit).toIntOrNull()?.minus(1)
        } ?: return Ergebnis("RÜCKFRAGE: Welchen Wert? Möglich: ${e.werte.values.joinToString(", ")}.", true)
        val text = Kaffeemaschine.einstellen(context, e, wert)
        val hinweis = if (e.schluessel == "abschaltzeit" && (w.startsWith("12") || "länger" in w)) " Länger als 3 Stunden kann die Soul nicht; ich wecke sie beim Kaffee aber selbst auf." else ""
        return Ergebnis(text + hinweis)
    }
}
