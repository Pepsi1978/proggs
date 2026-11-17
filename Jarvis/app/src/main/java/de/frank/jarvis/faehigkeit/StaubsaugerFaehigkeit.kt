package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.staubsauger.ProscenicBedienung
import de.frank.jarvis.staubsauger.ProscenicCloud
import de.frank.jarvis.staubsauger.ProscenicMehr
import de.frank.jarvis.staubsauger.ProscenicSteuerung
import de.frank.jarvis.staubsauger.SaugerAusgang
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saugroboter Proscenic M7 Pro: alles, was die Staubsauger-Karte auf dem Startbildschirm kann, auch per Sprache.
 * Befehle laufen unsichtbar über die Proscenic-Cloud (Konten, Proscenic); ohne Anmeldung tippt Jarvis die Grundbefehle in der Proscenic-App.
 */
class StaubsaugerFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "staubsauger"
    override val name = "Staubsauger"
    override val beschreibung = "Saugroboter Proscenic M7 Pro: Reinigung, Tiefenreinigung, Pause, Station, orten, Staubsammlung, Wischen, Saugstufe, " +
        "Lautstärke, Leuchte, automatische Staubsammlung, Zeitpläne, Sperrzonen, Verlauf, Akku, Zustand und Erreichbarkeit."
    override val hinweise =
        "Sagt Frank „Staubsauger“, „Saugroboter“, „M7 Pro“, „starte eine Reinigung“ oder „Tiefenreinigung“, nimm staubsauger. " +
            "Jarvis steuert den Roboter unsichtbar über die Proscenic-Cloud; das dauert meist 5 bis 15 Sekunden, bei langsamem Internet länger, " +
            "und Frank kann Jarvis währenddessen weiter benutzen. Ist nicht eindeutig, welche Aktion oder welcher Wert gemeint ist " +
            "(etwa „mach ihn leiser“: Lautstärke oder Saugstufe? „Zeitplan ändern“ ohne Uhrzeit oder Tage?), frag einmal kurz nach, bevor du das Werkzeug aufrufst. " +
            "Beginnt die Antwort des Werkzeugs mit RÜCKFRAGE, stell Frank genau diese Frage. " +
            "Meldet das Werkzeug einen Fehler, sag Frank den genannten Grund. Antworte danach kurz mit dem Ergebnis, etwa: „Tiefenreinigung läuft, Akku 100 Prozent.“"

    /** Öffnet ohne Cloud-Anmeldung eine andere App und tippt darin: nur auf Franks Wunsch im Gespräch, nie in Hintergrund-Agenten. */
    override val nurImGespraech: Boolean get() = true

    override fun stoerung(): String? =
        if (!ProscenicCloud.konfiguriert(context) && !ProscenicBedienung.eingeschaltet(context))
            "Für den Staubsauger fehlt die Proscenic-Anmeldung unter Konten (oder die Bedienungshilfe „Jarvis Staubsauger“)." else null

    private val aktionen = listOf(
        "reinigen", "tiefenreinigung", "stoppen", "pause", "fortsetzen", "station", "positionieren", "staubsammlung", "wischen", "status",
        "erreichbar", "saugstufe", "lautstaerke", "leuchte", "staubsammlung_automatik",
        "zeitplaene", "zeitplan_anlegen", "zeitplan_schalten", "zeitplan_loeschen", "sperrzonen", "sperrzone_loeschen", "verlauf",
    )

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "staubsauger",
            titel = "Saugroboter",
            beschreibung = "Jarvis: Saugroboter Proscenic M7 Pro. Aktionen: " +
                "reinigen („Staubsauger los“), tiefenreinigung, stoppen oder pause („Staubsauger stopp“, „pausier den Sauger“; ein bloßes „Stopp“ gilt nicht dem Sauger), " +
                "fortsetzen („mach weiter“), station („zurück zur Station“, „laden“), positionieren („wo ist der Staubsauger?“), staubsammlung („Staub absaugen“), " +
                "wischen (Y-förmiges Wischen), status (Akku, Zustand, Saugstufe), erreichbar (ist er mit der Cloud verbunden?), " +
                "saugstufe mit wert leise, normal oder stark, lautstaerke mit wert 0 bis 100 (Prozent), leuchte mit wert an oder aus, " +
                "staubsammlung_automatik (Absaugen nach dem Andocken) mit wert aus, jedes, jedes2 oder jedes3, " +
                "zeitplaene (vorlesen), zeitplan_anlegen mit uhrzeit (HH:MM), tage (z. B. „Mo,Mi,Fr“ oder „täglich“) und optional wert = Saugstufe, " +
                "zeitplan_schalten mit nummer und wert an oder aus, zeitplan_loeschen mit nummer, sperrzonen (vorlesen), sperrzone_loeschen mit nummer, " +
                "verlauf (letzte Reinigungen und Statistik). Neue Sperrzonen zeichnet Frank auf der Karte im Startbildschirm.",
            schema = schema(
                "aktion" to text(aktionen.joinToString(", ") + ".", aktionen),
                "wert" to text("Wert zur Aktion: Saugstufe (leise, normal, stark), Lautstärke 0–100, an/aus, aus/jedes/jedes2/jedes3."),
                "uhrzeit" to text("Nur zeitplan_anlegen: Startzeit HH:MM."),
                "tage" to text("Nur zeitplan_anlegen: Wochentage, etwa „Mo,Mi,Fr“, „werktags“, „wochenende“ oder „täglich“."),
                "nummer" to zahl("Nummer des Zeitplans oder der Sperrzone, wie zuvor vorgelesen (ab 1)."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
            taetigkeit = "bedient den Saugroboter",
        ) { a -> ausfuehren(a).let { Ergebnis(it.text, it.fehler) } },
    )

    private suspend fun ausfuehren(a: JSONObject): SaugerAusgang {
        val aktion = a.optString("aktion").trim().lowercase()
        val wert = a.optString("wert").trim().lowercase()
        ProscenicSteuerung.Aktion.aus(aktion)?.let { return ProscenicSteuerung.ausfuehren(context, it) }
        if (aktion == "pause") return ProscenicSteuerung.ausfuehren(context, ProscenicSteuerung.Aktion.STOPPEN)
        if (aktion !in aktionen) return SaugerAusgang("Unbekannte Aktion für den Saugroboter.", true)
        if (!ProscenicCloud.konfiguriert(context))
            return SaugerAusgang("Dafür braucht Jarvis die Proscenic-Anmeldung unter Einstellungen, Konten und APIs.", true)
        return when (aktion) {
            "fortsetzen" -> ProscenicMehr.pauseOderWeiter(context, weiter = true)
            "erreichbar" -> when (ProscenicMehr.erreichbar(context)) {
                true -> SaugerAusgang("Der M7 Pro ist erreichbar und mit der Proscenic-Cloud verbunden.")
                false -> SaugerAusgang(de.frank.jarvis.staubsauger.ProscenicCloud.OFFLINE_TEXT, true)
                null -> SaugerAusgang("Die Proscenic-Cloud ist gerade nicht erreichbar.", true)
            }
            "saugstufe" -> when {
                wert.startsWith("lei") || wert == "quiet" -> ProscenicMehr.saugstufe(context, "quiet")
                wert.startsWith("norm") || wert == "auto" || wert == "mittel" -> ProscenicMehr.saugstufe(context, "auto")
                wert.startsWith("stark") || wert.startsWith("leistung") || wert == "strong" || wert == "max" -> ProscenicMehr.saugstufe(context, "strong")
                else -> frage("Welche Saugstufe: leise, normal oder stark?")
            }
            "lautstaerke" -> wert.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 0..100 }
                ?.let { ProscenicMehr.lautstaerke(context, (it + 5) / 10) } ?: frage("Wie laut soll der Roboter sein, von 0 bis 100 Prozent?")
            "leuchte" -> when (wert) {
                "an", "ein", "einschalten", "on", "1" -> ProscenicMehr.led(context, true)
                "aus", "ausschalten", "off", "0" -> ProscenicMehr.led(context, false)
                else -> frage("Soll die Leuchte am Roboter an oder aus?")
            }
            "staubsammlung_automatik" -> when (wert) {
                "aus", "0", "nie" -> ProscenicMehr.staubFrequenz(context, 0)
                "jedes", "1", "immer", "jedes mal", "jedesmal" -> ProscenicMehr.staubFrequenz(context, 1)
                "jedes2", "2", "jedes zweite", "jedes 2." -> ProscenicMehr.staubFrequenz(context, 2)
                "jedes3", "3", "jedes dritte", "jedes 3." -> ProscenicMehr.staubFrequenz(context, 3)
                else -> frage("Soll die Station nach dem Andocken nie, jedes Mal, jedes zweite oder jedes dritte Mal absaugen?")
            }
            "verlauf" -> ProscenicMehr.verlauf(context)?.let { v ->
                val f = SimpleDateFormat("EEEE, d. MMMM, HH:mm", Locale.GERMANY)
                val kopf = listOfNotNull(v.anzahl?.let { "$it Reinigungen" }, v.gesamtFlaeche?.let { "$it Quadratmeter" }, v.gesamtMin?.let { "${it / 60} Stunden" }).joinToString(", ")
                SaugerAusgang("Insgesamt: $kopf. Zuletzt: " + v.laeufe.take(5).joinToString("; ") { "${f.format(Date(it.start * 1000))} Uhr, ${it.dauerSek / 60} Minuten, ${it.flaeche} Quadratmeter" } + ".")
            } ?: SaugerAusgang("Den Verlauf konnte ich gerade nicht laden.", true)
            else -> mitLive(aktion, a, wert)
        }
    }

    private fun frage(text: String) = SaugerAusgang("RÜCKFRAGE: $text Frag Frank das und ruf das Werkzeug danach mit dem genauen Wert erneut auf.", true)

    private val tagNamen = mapOf(1 to "Montag", 2 to "Dienstag", 3 to "Mittwoch", 4 to "Donnerstag", 5 to "Freitag", 6 to "Samstag", 0 to "Sonntag")

    private fun tageLesen(t: String): List<Int>? {
        val s = t.lowercase()
        if (s.isBlank()) return null
        if ("täglich" in s || "taeglich" in s || "jeden tag" in s || "alle" in s) return listOf(1, 2, 3, 4, 5, 6, 0)
        if ("werktag" in s) return listOf(1, 2, 3, 4, 5)
        if ("wochenend" in s) return listOf(6, 0)
        val kurz = listOf("mo" to 1, "di" to 2, "mi" to 3, "do" to 4, "fr" to 5, "sa" to 6, "so" to 0)
        val teile = s.split(',', ' ', ';', '/').map { it.trim() }.filter { it.isNotEmpty() && it != "und" }
        return teile.mapNotNull { w -> kurz.firstOrNull { w.startsWith(it.first) }?.second }.distinct().ifEmpty { null }
    }

    private fun zeitplanText(i: Int, z: ProscenicMehr.Zeitplan) =
        "${i + 1}. ${"%02d:%02d".format(z.startSek / 3600, z.startSek % 3600 / 60)} Uhr, " +
            listOf(1, 2, 3, 4, 5, 6, 0).filter { it in z.tage }.joinToString(", ") { tagNamen[it]!! }.ifBlank { "einmalig" } +
            ", Saugstufe ${mapOf("quiet" to "leise", "auto" to "normal", "strong" to "stark")[z.stufe] ?: z.stufe}" + if (z.aktiv) "" else " (ausgeschaltet)"

    /** Aktionen, die erst den aktuellen Stand (Zeitpläne, Zonen) brauchen. */
    private suspend fun mitLive(aktion: String, a: JSONObject, wert: String): SaugerAusgang {
        val live = ProscenicMehr.live(context, mitKarte = true)
            ?: return if (ProscenicMehr.erreichbar(context) == false) SaugerAusgang(de.frank.jarvis.staubsauger.ProscenicCloud.OFFLINE_TEXT, true)
                else SaugerAusgang("Den aktuellen Stand des M7 Pro konnte ich gerade nicht abrufen; versuch es gleich noch einmal.", true)
        // Ohne frisch gelesene Zeitpläne bzw. Zonen nichts vorlesen und vor allem nichts zurückschreiben: Sonst würde eine leere
        // Liste die vorhandenen Zeitpläne oder Sperrzonen auf dem Roboter überschreiben.
        val brauchtZeitplaene = aktion.startsWith("zeitplan")
        if ((brauchtZeitplaene && !live.zeitplaeneGelesen) || (aktion == "sperrzone_loeschen" && !live.zonenGelesen))
            return if (ProscenicMehr.erreichbar(context) == false) SaugerAusgang(de.frank.jarvis.staubsauger.ProscenicCloud.OFFLINE_TEXT, true)
            else SaugerAusgang("Der M7 Pro hat ${if (brauchtZeitplaene) "seine Zeitpläne" else "seine Sperrzonen"} gerade nicht geschickt; versuch es gleich noch einmal.", true)
        val nummer = a.optInt("nummer", 0)
        val sperren = live.zonen.filter { it.sperre }
        return when (aktion) {
            "zeitplaene" -> if (live.zeitplaene.isEmpty()) SaugerAusgang("Es sind keine Zeitpläne angelegt.")
                else SaugerAusgang("Zeitpläne: " + live.zeitplaene.mapIndexed(::zeitplanText).joinToString("; ") + ".")
            "zeitplan_anlegen" -> {
                val zeit = Regex("(\\d{1,2})[:.]?(\\d{2})?").find(a.optString("uhrzeit"))
                val h = zeit?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..23 }
                val m = zeit?.groupValues?.get(2)?.ifEmpty { "0" }?.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
                val tage = tageLesen(a.optString("tage"))
                if (h == null || tage == null) return frage("Um wie viel Uhr und an welchen Tagen soll der Roboter reinigen?")
                val stufe = when { wert.startsWith("lei") -> "quiet"; wert.startsWith("stark") || wert.startsWith("leistung") -> "strong"; else -> "auto" }
                val neu = live.zeitplaene + ProscenicMehr.Zeitplan(true, h * 3600 + m * 60, tage, stufe, JSONObject())
                ProscenicMehr.zeitplaeneSpeichern(context, live.zeitZone, live.zeitZoneSek, neu)
                    .let { if (it.fehler) it else SaugerAusgang("Zeitplan angelegt: " + zeitplanText(neu.size - 1, neu.last()).substringAfter(". ") + ".") }
            }
            "zeitplan_schalten", "zeitplan_loeschen" -> {
                val i = nummer - 1
                if (i !in live.zeitplaene.indices) return frage("Welcher Zeitplan? " + live.zeitplaene.mapIndexed(::zeitplanText).joinToString("; ").ifBlank { "Es gibt keinen." })
                val neu = live.zeitplaene.toMutableList()
                if (aktion == "zeitplan_loeschen") neu.removeAt(i)
                else neu[i] = neu[i].copy(aktiv = when (wert) { "an", "ein", "1" -> true; "aus", "0" -> false; else -> return frage("Soll der Zeitplan an oder aus?") })
                ProscenicMehr.zeitplaeneSpeichern(context, live.zeitZone, live.zeitZoneSek, neu)
                    .let { if (it.fehler) it else SaugerAusgang(if (aktion == "zeitplan_loeschen") "Zeitplan ${i + 1} gelöscht." else "Zeitplan ${i + 1} ist jetzt ${if (neu[i].aktiv) "an" else "aus"}.") }
            }
            "sperrzonen" -> if (sperren.isEmpty()) SaugerAusgang("Es sind keine Sperrzonen angelegt.")
                else SaugerAusgang("Sperrzonen: " + sperren.mapIndexed { i, z -> "${i + 1}. " + raumVon(z, live) }.joinToString("; ") + ". Neue Sperrzonen zeichnest du auf der Karte im Startbildschirm.")
            "sperrzone_loeschen" -> {
                val i = nummer - 1
                val ziel = sperren.getOrNull(i) ?: return frage("Welche Sperrzone? " + sperren.mapIndexed { n, z -> "${n + 1}. " + raumVon(z, live) }.joinToString("; ").ifBlank { "Es gibt keine." })
                val mapId = live.mapId ?: return SaugerAusgang("Die Karte des Roboters ist gerade nicht lesbar.", true)
                ProscenicMehr.zonenSpeichern(context, mapId, live.zonen.filter { it !== ziel })
                    .let { if (it.fehler) it else SaugerAusgang("Sperrzone ${i + 1} gelöscht.") }
            }
            else -> SaugerAusgang("Unbekannte Aktion für den Saugroboter.", true)
        }
    }

    /** Beschreibt eine Sperrzone über den Raum, in dem ihre Mitte liegt. */
    private fun raumVon(z: ProscenicMehr.Zone, live: ProscenicMehr.Live): String {
        val e = z.ecken
        if (e.isEmpty()) return "ohne Lage"
        val mx = e.sumOf { it.first } / e.size; val my = e.sumOf { it.second } / e.size
        val raum = live.zonen.filter { !it.sperre }.firstOrNull { r -> r.ecken.let { k -> k.isNotEmpty() && mx in k.minOf { it.first }..k.maxOf { it.first } && my in k.minOf { it.second }..k.maxOf { it.second } } }
        val b = (e.maxOf { it.first } - e.minOf { it.first }) / 100 / 10.0; val h = (e.maxOf { it.second } - e.minOf { it.second }) / 100 / 10.0
        return (raum?.name?.let { "im Raum $it" } ?: "ohne Raum") + ", etwa $b mal $h Meter"
    }
}
