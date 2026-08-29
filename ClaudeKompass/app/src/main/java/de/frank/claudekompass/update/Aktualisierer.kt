package de.frank.claudekompass.update

import de.frank.claudekompass.ai.CodexClient
import de.frank.claudekompass.ai.Prompts
import de.frank.claudekompass.data.EinstellungenStore
import de.frank.claudekompass.data.KompassRepository
import de.frank.claudekompass.data.RohEintrag
import de.frank.claudekompass.data.local.EintragEntity
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.observability.probe
import kotlinx.coroutines.CancellationException

/** Zwischenstand, den die Oberfläche während des Laufs anzeigt. */
data class LaufFortschritt(
    val laeuft: Boolean = false,
    val schritt: String = "",
    val erledigt: Int = 0,
    val gesamt: Int = 0,
    val neuAnzahl: Int = 0,
    val entferntAnzahl: Int = 0,
    val geaendertAnzahl: Int = 0,
    val gefundeneVersion: String = "",
    val fehler: String = "",
    val fertig: Boolean = false,
)

/**
 * Der Aktualisieren-Knopf.
 *
 * Der Ablauf in fünf Schritten:
 *  1. Die offiziellen Unterlagen und das Änderungsprotokoll holen.
 *  2. Namen und englische Beschreibungen daraus lesen — ohne Beteiligung des Modells, damit
 *     nichts erfunden und nichts übersehen wird.
 *  3. Mit dem Bestand vergleichen: Was ist neu, was ist verschwunden?
 *  4. Für jeden neuen Eintrag eine deutsche Erklärung erzeugen lassen; für jeden
 *     verschwundenen ermitteln, was seine Aufgabe übernommen hat.
 *  5. Einspielen. Neue Einträge tragen die Lauf-Nummer und werden dadurch hervorgehoben;
 *     die Markierungen des vorigen Laufs fallen weg.
 *
 * Ohne Anmeldung bei Codex läuft der Abgleich trotzdem: Neue Einträge kommen dann mit ihrer
 * englischen Beschreibung herein und lassen sich später einzeln erklären. Ein fehlender
 * Zugang darf nicht dazu führen, dass man von einer neuen Fassung gar nichts erfährt.
 */
class Aktualisierer(
    private val repository: KompassRepository,
    private val codex: CodexClient,
    private val einstellungen: EinstellungenStore,
) {

    private val abruf = DokuAbruf()

    suspend fun fuehreAus(melde: suspend (LaufFortschritt) -> Unit) {
        val laufId = repository.starteLauf()
        var stand = LaufFortschritt(laeuft = true, schritt = "Unterlagen werden geholt")
        melde(stand)

        try {
            // --- Schritt 1: Unterlagen holen ---------------------------------------------
            val changelog = abruf.hole(DokuAbruf.URL_CHANGELOG)
            val version = DokuParser.leseNeuesteVersion(changelog)
            probe(
                version.isNotBlank(),
                "Aus dem Änderungsprotokoll kam keine Versionsnummer",
                "Aktualisierer",
                "fuehreAus",
            )
            stand = stand.copy(schritt = "Fassung $version gefunden", gefundeneVersion = version)
            melde(stand)

            val befehleMd = abruf.hole(DokuAbruf.URL_BEFEHLE)
            val einstellungenMd = abruf.hole(DokuAbruf.URL_EINSTELLUNGEN)
            val variablenMd = runCatching { abruf.hole(DokuAbruf.URL_VARIABLEN) }.getOrElse {
                // Die Variablenliste ist der am ehesten verzichtbare Teil. Fällt sie aus,
                // läuft der Rest weiter, statt den ganzen Lauf scheitern zu lassen.
                KompassLog.warn(
                    "Aktualisierer",
                    "fuehreAus",
                    "Die Liste der Umgebungsvariablen kam nicht — der Rest läuft weiter",
                    mapOf("grund" to it.message),
                )
                ""
            }

            // --- Schritt 2: auswerten -----------------------------------------------------
            stand = stand.copy(schritt = "Unterlagen werden ausgewertet")
            melde(stand)
            val gelesen = buildMap<Bereich, List<GelesenerEintrag>> {
                put(Bereich.SLASH, DokuParser.leseSlashBefehle(befehleMd))
                put(
                    Bereich.CONFIG,
                    DokuParser.leseEinstellungen(einstellungenMd) +
                        if (variablenMd.isBlank()) emptyList() else DokuParser.leseVariablen(variablenMd),
                )
            }

            // Eine leere Ausbeute heisst fast sicher: Die Seite hat ihre Form geändert. Dann
            // gälte JEDER vorhandene Eintrag als verschwunden — das wäre ein stiller Totalschaden.
            // Deshalb wird hier abgebrochen, statt den Bestand zu leeren.
            val gesamtGelesen = gelesen.values.sumOf { it.size }
            if (gesamtGelesen < MINDEST_GELESEN) {
                throw DokuFehler(
                    "Aus den Unterlagen kamen nur $gesamtGelesen Einträge zurück. Das ist zu " +
                        "wenig — wahrscheinlich hat sich der Aufbau der Seiten geändert. " +
                        "Es wurde nichts verändert.",
                )
            }

            // --- Schritt 3: vergleichen ---------------------------------------------------
            val bestand = repository.ladeKomplett()
            val neueRoh = mutableListOf<RohEintrag>()
            val geaenderte = mutableListOf<EintragEntity>()
            val verschwundene = mutableListOf<EintragEntity>()

            for ((bereich, liste) in gelesen) {
                val bekannt = bestand.filter { it.bereich == bereich.id }.associateBy { it.name }
                val gelesenNamen = liste.map { it.name }.toSet()

                for (eintrag in liste) {
                    val vorhanden = bekannt[eintrag.name]
                    if (vorhanden == null) {
                        val (seit, beleg) = DokuParser.findeEinzug(
                            changelog,
                            eintrag.name,
                            bereich == Bereich.SLASH,
                        )
                        neueRoh += RohEintrag(
                            bereich = bereich,
                            name = eintrag.name,
                            kategorie = "Neu dazugekommen",
                            art = eintrag.art.ifBlank { if (bereich == Bereich.SLASH) "Eingebaut" else "settings.json" },
                            kurz = eintrag.beschreibung.take(140),
                            englisch = eintrag.beschreibung,
                            erklaerung = "",
                            seit = seit,
                            seitBeleg = beleg,
                            sortierName = eintrag.name.removePrefix("/").lowercase(),
                            entfernt = false,
                            entferntIn = "",
                            ersatz = "",
                        )
                    } else if (vorhanden.entfernt) {
                        // Ein Eintrag, den wir als entfernt geführt haben, steht wieder in der
                        // Unterlage. Dann war die Annahme falsch — zurück in den Bestand.
                        geaenderte += vorhanden.copy(
                            entfernt = false,
                            entferntInVersion = "",
                            zuletztGeaendert = System.currentTimeMillis(),
                        )
                    } else if (vorhanden.quelleEnglisch != eintrag.beschreibung &&
                        eintrag.beschreibung.isNotBlank()
                    ) {
                        // Die offizielle Beschreibung hat sich geändert. Die eigene deutsche
                        // Erklärung bleibt unangetastet — sie kann per Knopf vertieft werden.
                        geaenderte += vorhanden.copy(
                            quelleEnglisch = eintrag.beschreibung,
                            zuletztGeaendert = System.currentTimeMillis(),
                        )
                    }
                }

                verschwundene += bekannt.values.filter { vorhanden ->
                    !vorhanden.entfernt &&
                        vorhanden.name !in gelesenNamen &&
                        // Umgebungsvariablen stehen nicht in der Einstellungsliste. Ohne diese
                        // Ausnahme gälten sie bei jedem Lauf als verschwunden.
                        !(variablenMd.isBlank() && vorhanden.art == "Umgebungsvariable")
                }
            }

            stand = stand.copy(
                schritt = "Abgleich fertig",
                neuAnzahl = neueRoh.size,
                entferntAnzahl = verschwundene.size,
                geaendertAnzahl = geaenderte.size,
                gesamt = neueRoh.size + verschwundene.size,
            )
            melde(stand)
            KompassLog.info(
                "Aktualisierer",
                "fuehreAus",
                "Abgleich fertig",
                mapOf(
                    "version" to version,
                    "neu" to neueRoh.size,
                    "verschwunden" to verschwundene.size,
                    "geaendert" to geaenderte.size,
                ),
            )

            // --- Schritt 4: erklären lassen ----------------------------------------------
            val namensliste = Prompts.namensliste(bestand)
            val fertigeNeue = mutableListOf<RohEintrag>()
            var erledigt = 0

            for (roh in neueRoh) {
                stand = stand.copy(
                    schritt = "Neuer Eintrag wird erklärt: ${roh.name}",
                    erledigt = erledigt,
                )
                melde(stand)
                fertigeNeue += erklaereNeuen(roh)
                erledigt += 1
            }

            for (verschwunden in verschwundene) {
                stand = stand.copy(
                    schritt = "Nachfolger wird gesucht: ${verschwunden.name}",
                    erledigt = erledigt,
                )
                melde(stand)
                geaenderte += markiereAlsEntfernt(verschwunden, version, namensliste)
                erledigt += 1
            }

            // --- Schritt 5: einspielen ---------------------------------------------------
            stand = stand.copy(schritt = "Änderungen werden eingespielt", erledigt = erledigt)
            melde(stand)
            repository.spieleLaufEin(laufId, fertigeNeue, geaenderte)

            val lauf = repository.ladeLauf(laufId)
            if (lauf != null) {
                repository.beendeLauf(
                    lauf.copy(
                        beendetAm = System.currentTimeMillis(),
                        cliVersion = version,
                        neuAnzahl = fertigeNeue.size,
                        entferntAnzahl = verschwundene.size,
                        geaendertAnzahl = geaenderte.size,
                        status = "fertig",
                        meldung = baueMeldung(fertigeNeue.size, verschwundene.size, geaenderte.size),
                    ),
                )
            }
            melde(
                stand.copy(
                    laeuft = false,
                    fertig = true,
                    schritt = baueMeldung(fertigeNeue.size, verschwundene.size, geaenderte.size),
                    neuAnzahl = fertigeNeue.size,
                ),
            )
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            val text = fehler.message ?: "Die Aktualisierung ist fehlgeschlagen."
            KompassLog.error("Aktualisierer", "fuehreAus", "Lauf abgebrochen", mapOf("grund" to text))
            repository.ladeLauf(laufId)?.let {
                repository.beendeLauf(
                    it.copy(beendetAm = System.currentTimeMillis(), status = "fehler", meldung = text),
                )
            }
            melde(stand.copy(laeuft = false, fehler = text))
        }
    }

    /** Lässt einen neuen Eintrag erklären. Ohne Zugang bleibt die englische Fassung stehen. */
    private suspend fun erklaereNeuen(roh: RohEintrag): RohEintrag {
        if (!codex.istVerbunden) {
            return roh.copy(
                erklaerung = "Dieser Eintrag ist neu dazugekommen. Eine deutsche Erklärung fehlt " +
                    "noch, weil keine Verbindung zu Codex besteht.\n\n" +
                    "Die offizielle englische Beschreibung lautet:\n${roh.englisch}\n\n" +
                    "Meld dich in den Einstellungen bei Codex an und tipp dann auf " +
                    "„Ausführlicher“ — die Erklärung wird dann nachgeholt.",
            )
        }
        return try {
            val antwort = codex.frage(
                anweisung = Prompts.neuerEintragAnweisung(roh.bereich.id),
                eingabe = Prompts.neuerEintragEingabe(roh.name, roh.englisch, roh.seit),
                modellId = einstellungen.modellId,
                denktiefe = einstellungen.denktiefe.apiValue,
            )
            val json = Prompts.leseJsonObjekt(antwort)
            if (json == null) {
                KompassLog.warn(
                    "Aktualisierer",
                    "erklaereNeuen",
                    "Die Antwort war kein gültiges JSON — englische Fassung bleibt stehen",
                    mapOf("name" to roh.name),
                )
                return roh.copy(erklaerung = roh.englisch)
            }
            roh.copy(
                kurz = json.optString("kurz").takeIf(String::isNotBlank) ?: roh.kurz,
                kategorie = json.optString("kategorie").takeIf(String::isNotBlank) ?: roh.kategorie,
                erklaerung = json.optString("erklaerung").takeIf(String::isNotBlank) ?: roh.englisch,
            )
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            // Ein einzelner missglückter Eintrag darf den Lauf nicht abbrechen. Er kommt mit
            // seiner englischen Beschreibung herein und lässt sich später nachholen.
            KompassLog.warn(
                "Aktualisierer",
                "erklaereNeuen",
                "Erklärung fehlgeschlagen, englische Fassung bleibt stehen",
                mapOf("name" to roh.name, "grund" to fehler.message),
            )
            roh.copy(erklaerung = roh.englisch)
        }
    }

    private suspend fun markiereAlsEntfernt(
        eintrag: EintragEntity,
        version: String,
        namensliste: String,
    ): EintragEntity {
        val grundfassung = eintrag.copy(
            entfernt = true,
            entferntInVersion = version,
            ersatz = "Zu diesem Eintrag ist kein Nachfolger bekannt.",
            zuletztGeaendert = System.currentTimeMillis(),
        )
        if (!codex.istVerbunden) return grundfassung

        return try {
            val antwort = codex.frage(
                anweisung = Prompts.ersatzAnweisung(),
                eingabe = Prompts.ersatzEingabe(eintrag.name, eintrag.quelleEnglisch, namensliste),
                modellId = einstellungen.modellId,
                denktiefe = einstellungen.denktiefe.apiValue,
            )
            val json = Prompts.leseJsonObjekt(antwort) ?: return grundfassung
            grundfassung.copy(
                ersatz = json.optString("ersatz").takeIf(String::isNotBlank) ?: grundfassung.ersatz,
                erklaerung = json.optString("erklaerung").takeIf(String::isNotBlank)
                    ?: grundfassung.erklaerung,
            )
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn(
                "Aktualisierer",
                "markiereAlsEntfernt",
                "Nachfolger konnte nicht ermittelt werden",
                mapOf("name" to eintrag.name, "grund" to fehler.message),
            )
            grundfassung
        }
    }

    private fun baueMeldung(neu: Int, weg: Int, geaendert: Int): String = when {
        neu == 0 && weg == 0 && geaendert == 0 -> "Alles war schon auf dem neuesten Stand."
        else -> buildString {
            append("Fertig: ")
            append(if (neu == 1) "1 neuer Eintrag" else "$neu neue Einträge")
            append(", ")
            append(if (weg == 1) "1 entfernter" else "$weg entfernte")
            append(", ")
            append(if (geaendert == 1) "1 geänderter" else "$geaendert geänderte")
            append('.')
        }
    }

    fun beende() = abruf.beende()

    private companion object {
        /**
         * Weniger als das kann keine echte Auskunft sein — Claude Code hat weit über hundert
         * Befehle und Einstellungen. Diese Untergrenze ist die Sicherung gegen einen stillen
         * Totalschaden, wenn sich der Aufbau der Doku-Seiten ändert.
         */
        const val MINDEST_GELESEN = 40
    }
}
