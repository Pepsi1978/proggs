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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Zwischenstand, den die Oberfläche während des Laufs anzeigt. */
data class LaufFortschritt(
    val laeuft: Boolean = false,
    val schritt: String = "",
    val erledigt: Int = 0,
    val gesamt: Int = 0,
    val neuAnzahl: Int = 0,
    val entferntAnzahl: Int = 0,
    val geaendertAnzahl: Int = 0,
    val geloeschtAnzahl: Int = 0,
    val erklaertAnzahl: Int = 0,
    /** Wie viele Einträge noch auf ihre deutsche Erklärung warten. */
    val offeneErklaerungen: Int = 0,
    val neueNamen: List<String> = emptyList(),
    val entfernteNamen: List<String> = emptyList(),
    val geaenderteNamen: List<String> = emptyList(),
    val geloeschteNamen: List<String> = emptyList(),
    val gefundeneVersion: String = "",
    val fehler: String = "",
    val fertig: Boolean = false,
) {
    /** Hat sich überhaupt etwas geändert? Entscheidet, ob der Bericht sich zu lesen lohnt. */
    val hatAenderungen: Boolean
        get() = neuAnzahl > 0 || entferntAnzahl > 0 || geaendertAnzahl > 0 ||
            geloeschtAnzahl > 0 || erklaertAnzahl > 0
}

/**
 * Der Aktualisieren-Knopf.
 *
 * Der Ablauf:
 *  1. Die offiziellen Unterlagen und das Änderungsprotokoll holen.
 *  2. Namen und englische Beschreibungen daraus lesen — ohne Beteiligung des Modells, damit
 *     nichts erfunden und nichts übersehen wird.
 *  3. Mit dem Bestand vergleichen: Was ist neu, was ist verschwunden, was hat einen neuen
 *     offiziellen Text?
 *  4. Die Unterschiede SOFORT einspielen — neue Einträge zunächst mit ihrem englischen Text.
 *  5. Erst danach die deutschen Erklärungen einzeln nachziehen und jede einzeln speichern.
 *
 * Punkt vier und fünf sind die wichtigste Eigenschaft des Ablaufs. Früher lag alles bis zum
 * Ende des Laufs im Arbeitsspeicher: Ein Abbruch oder ein Netzfehler nach zweihundert
 * Erklärungen warf jede einzelne davon weg, und der nächste Lauf begann wieder bei null —
 * dieselben Einträge, dieselben Kosten. Jetzt ist nach jeder Erklärung ein Stand gesichert,
 * und ein neuer Lauf sammelt die Reste ein, statt sie noch einmal zu bezahlen.
 *
 * Erklärt wird ausschliesslich, was noch keine Erklärung hat. Ein Eintrag, der schon auf
 * Deutsch dasteht, wird nie erneut ans Modell geschickt — auch dann nicht, wenn sich sein
 * englischer Text geändert hat. Der neue Originaltext wird still nachgeführt; ausführlicher
 * erklären lässt sich der Eintrag von Hand.
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

    /**
     * Führt einen vollständigen Abgleich aus.
     *
     * @param erklaereAlles Übergeht die Rückfrage bei sehr vielen neuen Einträgen. Der Knopf
     *   „Alle jetzt erklären" im Bericht setzt das.
     */
    suspend fun fuehreAus(erklaereAlles: Boolean = false, melde: suspend (LaufFortschritt) -> Unit) {
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

            val slashGelesen = DokuParser.leseSlashBefehle(befehleMd)
            val settingsGelesen = DokuParser.leseEinstellungen(einstellungenMd)
            val variablenGelesen =
                if (variablenMd.isBlank()) emptyList() else DokuParser.leseVariablen(variablenMd)

            // Eine leere Ausbeute heisst fast sicher: Die Seite hat ihre Form geändert. Dann
            // gälte JEDER vorhandene Eintrag dieser Art als verschwunden — ein stiller
            // Totalschaden. Die Grenze gilt bewusst je Quelle: Eine gesunde Variablenliste
            // darf eine kaputte Befehlsliste nicht überdecken.
            pruefeAusbeute("Slash-Befehle", slashGelesen.size, MINDEST_SLASH)
            pruefeAusbeute("Einstellungen", settingsGelesen.size, MINDEST_EINSTELLUNGEN)

            // Kommen zu wenige Variablen zurück, werden sie behandelt wie eine ausgefallene
            // Liste: übersprungen. Sie deshalb alle als entfernt zu führen wäre falsch.
            val variablenBrauchbar = variablenGelesen.size >= MINDEST_VARIABLEN
            if (variablenGelesen.isNotEmpty() && !variablenBrauchbar) {
                KompassLog.warn(
                    "Aktualisierer",
                    "fuehreAus",
                    "Zu wenige Umgebungsvariablen gelesen — sie bleiben in diesem Lauf aussen vor",
                    mapOf("anzahl" to variablenGelesen.size),
                )
            }

            val gelesen = mapOf(
                Bereich.SLASH to slashGelesen,
                Bereich.CONFIG to settingsGelesen + if (variablenBrauchbar) variablenGelesen else emptyList(),
            )

            // --- Schritt 3: vergleichen ---------------------------------------------------
            val bestand = repository.ladeKomplett()
            val seedKennungen = repository.seedKennungen()
            val bereinigeAltlasten = !einstellungen.altlastenBereinigt
            val neueRoh = mutableListOf<RohEintrag>()
            val geaenderte = mutableListOf<EintragEntity>()
            val verschwundene = mutableListOf<EintragEntity>()
            val erfundene = mutableListOf<EintragEntity>()

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
                            kategorie = eintrag.kategorie.ifBlank { "Neu dazugekommen" },
                            art = eintrag.art.ifBlank { if (bereich == Bereich.SLASH) "Eingebaut" else "settings.json" },
                            kurz = eintrag.beschreibung.take(140),
                            englisch = eintrag.beschreibung,
                            // Bleibt leer: Genau daran erkennt der nächste Schritt, dass hier
                            // noch eine deutsche Erklärung fehlt.
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
                        // Das ist der Grund, warum ein Lauf keine Erklärungen wiederholt.
                        geaenderte += vorhanden.copy(
                            quelleEnglisch = eintrag.beschreibung,
                            zuletztGeaendert = System.currentTimeMillis(),
                        )
                    }
                }

                val fehlend = bekannt.values.filter { vorhanden ->
                    !vorhanden.entfernt &&
                        vorhanden.name !in gelesenNamen &&
                        // Umgebungsvariablen stehen nicht in der Einstellungsliste. Ohne diese
                        // Ausnahme gälten sie bei jedem Lauf als verschwunden.
                        !(!variablenBrauchbar && vorhanden.art == "Umgebungsvariable")
                }

                // Beim allerersten Lauf nach dem Auswertungsfehler wird aufgeräumt: Ein
                // fehlender Eintrag, den es in der Auslieferung nie gab, wurde damals erfunden.
                // Ihn als entfernt zu führen, würde im Klapp-Bereich dauerhaft Unsinn behaupten
                // und obendrein für jeden dieser Namen eine Nachfolgersuche kosten.
                //
                // Danach gilt die Regel NICHT mehr. Sonst würde jeder Eintrag, der nach der
                // Auslieferung dazukam und später aus Claude Code verschwindet, stillschweigend
                // gelöscht statt aufgehoben — der Bereich „Entfernte Einträge" wäre für alles
                // Neuere blind.
                if (bereinigeAltlasten) {
                    val (echtVerschwunden, nieDagewesen) = fehlend.partition { it.id in seedKennungen }
                    verschwundene += echtVerschwunden
                    erfundene += nieDagewesen
                } else {
                    verschwundene += fehlend
                }
            }

            stand = stand.copy(
                schritt = "Abgleich fertig",
                neuAnzahl = neueRoh.size,
                entferntAnzahl = verschwundene.size,
                geaendertAnzahl = geaenderte.size,
                geloeschtAnzahl = erfundene.size,
                neueNamen = neueRoh.map { it.name },
                entfernteNamen = verschwundene.map { it.name },
                geaenderteNamen = geaenderte.map { it.name },
                geloeschteNamen = erfundene.map { it.name },
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
                    "erfunden" to erfundene.size,
                ),
            )

            // --- Schritt 4: Unterschiede sofort sichern -----------------------------------
            stand = stand.copy(schritt = "Änderungen werden eingespielt")
            melde(stand)
            repository.spieleNeueEin(laufId, neueRoh)
            geaenderte.forEach { repository.sichereEintrag(it) }
            repository.loescheEintraege(erfundene.map { it.id })
            repository.raeumeNeuMarkierungen(laufId)

            if (bereinigeAltlasten) {
                repariereVergifteteErklaerungen(bestand, seedKennungen)
                einstellungen.altlastenBereinigt = true
            }

            // Verschwundene einzeln: die Nachfolgersuche fragt das Modell, und auch dieses
            // Ergebnis soll einen Abbruch überleben.
            //
            // Die Namensliste wird bewusst NACH dem Einspielen geholt: Ein Befehl, der durch
            // einen neuen ersetzt wurde, kann sonst nie auf seinen Nachfolger zeigen — der
            // stand beim Laden des Bestands ja noch gar nicht in der Datenbank.
            val namensliste = Prompts.namensliste(repository.ladeKomplett())
            var erledigt = 0
            for (verschwunden in verschwundene) {
                stand = stand.copy(
                    schritt = "Nachfolger wird gesucht: ${verschwunden.name}",
                    erledigt = erledigt,
                )
                melde(stand)
                repository.sichereEintrag(markiereAlsEntfernt(verschwunden, version, namensliste))
                erledigt += 1
            }

            // --- Schritt 5: fehlende Erklärungen nachziehen -------------------------------
            val offene = repository.ladeUnerklaerte()
            if (offene.isEmpty() || !codex.istVerbunden) {
                if (offene.isNotEmpty()) {
                    KompassLog.info(
                        "Aktualisierer",
                        "fuehreAus",
                        "Keine Anmeldung bei Codex — Erklärungen bleiben offen",
                        mapOf("offen" to offene.size),
                    )
                }
                schliesseAb(laufId, version, stand.copy(offeneErklaerungen = offene.size), melde)
                return
            }

            // Bei sehr vielen offenen Erklärungen wird nicht einfach losgelegt: Jede einzelne
            // ist eine Anfrage an das Modell. Wer 400 neue Einträge geschenkt bekommt, soll
            // vorher wissen, was das kostet, statt es hinterher an der Abrechnung zu merken.
            if (offene.size > SCHWELLE_RUECKFRAGE && !erklaereAlles) {
                KompassLog.info(
                    "Aktualisierer",
                    "fuehreAus",
                    "Viele offene Erklärungen — es wird nachgefragt",
                    mapOf("offen" to offene.size),
                )
                schliesseAb(laufId, version, stand.copy(offeneErklaerungen = offene.size), melde)
                return
            }

            val erklaert = erklaereListe(offene) { neuerStand ->
                stand = stand.copy(
                    schritt = neuerStand.schritt,
                    erledigt = neuerStand.erledigt,
                    gesamt = neuerStand.gesamt,
                    erklaertAnzahl = neuerStand.erklaertAnzahl,
                )
                melde(stand)
            }
            schliesseAb(
                laufId,
                version,
                stand.copy(
                    erklaertAnzahl = erklaert,
                    offeneErklaerungen = repository.anzahlUnerklaerte(),
                ),
                melde,
            )
        } catch (abbruch: CancellationException) {
            // Der Abbrechen-Knopf soll keinen Lauf-Datensatz auf „laeuft“ stehen lassen.
            //
            // NonCancellable ist hier nicht schmückend, sondern nötig: Die Datenbankzugriffe
            // von Room laufen intern über withContext, und das wirft in einer bereits
            // abgebrochenen Coroutine sofort beim Eintritt — der Datensatz bliebe unberührt
            // und stünde für immer auf „laeuft“.
            withContext(NonCancellable) {
                repository.ladeLauf(laufId)?.let {
                    repository.beendeLauf(
                        it.copy(
                            beendetAm = System.currentTimeMillis(),
                            status = "abgebrochen",
                            meldung = "Abgebrochen. Was bis dahin eingespielt wurde, ist gespeichert.",
                        ),
                    )
                }
            }
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

    /**
     * Holt die offenen Erklärungen nach, ohne die Unterlagen erneut abzugleichen.
     *
     * Das ist der zweite Weg in die Erklär-Schleife: der Knopf im Bericht, wenn beim Lauf
     * sehr viele neue Einträge zusammenkamen und der Benutzer entscheiden sollte.
     */
    suspend fun erklaereOffene(melde: suspend (LaufFortschritt) -> Unit) {
        var stand = LaufFortschritt(laeuft = true, schritt = "Offene Erklärungen werden geholt")
        melde(stand)
        try {
            if (!codex.istVerbunden) {
                melde(
                    stand.copy(
                        laeuft = false,
                        fehler = "Dafür braucht es die Anmeldung bei Codex. Du findest sie in " +
                            "den Einstellungen.",
                    ),
                )
                return
            }
            val offene = repository.ladeUnerklaerte()
            if (offene.isEmpty()) {
                melde(
                    LaufFortschritt(
                        laeuft = false,
                        fertig = true,
                        schritt = "Es war nichts mehr offen — alle Einträge sind auf Deutsch erklärt.",
                    ),
                )
                return
            }
            val erklaert = erklaereListe(offene) { neuerStand ->
                stand = neuerStand
                melde(stand)
            }
            val restlich = repository.anzahlUnerklaerte()
            melde(
                LaufFortschritt(
                    laeuft = false,
                    fertig = true,
                    erklaertAnzahl = erklaert,
                    offeneErklaerungen = restlich,
                    schritt = if (restlich == 0) {
                        "Fertig: ${zaehle(erklaert, "Erklärung", "Erklärungen")} nachgeholt."
                    } else {
                        "Fertig: ${zaehle(erklaert, "Erklärung", "Erklärungen")} nachgeholt, " +
                            "$restlich noch offen."
                    },
                ),
            )
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            val text = fehler.message ?: "Die Erklärungen konnten nicht geholt werden."
            KompassLog.error("Aktualisierer", "erklaereOffene", "Nachholen abgebrochen", mapOf("grund" to text))
            melde(stand.copy(laeuft = false, fehler = text))
        }
    }

    /**
     * Erklärt eine Liste von Einträgen und speichert jeden einzeln.
     *
     * Gibt zurück, wie viele wirklich eine deutsche Erklärung bekommen haben. Reisst die
     * Verbindung ab, hört die Schleife auf, statt hundertmal in denselben Fehler zu laufen —
     * das Bisherige ist dann gespeichert und der nächste Anlauf macht dort weiter.
     */
    private suspend fun erklaereListe(
        offene: List<EintragEntity>,
        melde: suspend (LaufFortschritt) -> Unit,
    ): Int {
        var erklaert = 0
        var fehlerInFolge = 0
        for ((index, eintrag) in offene.withIndex()) {
            melde(
                LaufFortschritt(
                    laeuft = true,
                    schritt = "Wird erklärt: ${eintrag.name}",
                    erledigt = index,
                    gesamt = offene.size,
                    erklaertAnzahl = erklaert,
                ),
            )
            val text = erklaereEinen(eintrag)
            if (text == null) {
                fehlerInFolge += 1
                if (fehlerInFolge >= ABBRUCH_NACH_FEHLERN) {
                    KompassLog.warn(
                        "Aktualisierer",
                        "erklaereListe",
                        "Zu viele Fehlschläge hintereinander — die Schleife hört auf",
                        mapOf("erklaert" to erklaert, "offen" to (offene.size - index)),
                    )
                    break
                }
                continue
            }
            fehlerInFolge = 0
            repository.sichereEintrag(text)
            erklaert += 1
        }
        return erklaert
    }

    /**
     * Lässt einen Eintrag erklären. Gibt `null` zurück, wenn es nicht geklappt hat — dann
     * bleibt die Erklärung leer und der Eintrag steht beim nächsten Mal wieder in der Schlange.
     */
    private suspend fun erklaereEinen(eintrag: EintragEntity): EintragEntity? = try {
        val antwort = codex.frage(
            anweisung = Prompts.neuerEintragAnweisung(eintrag.bereich),
            eingabe = Prompts.neuerEintragEingabe(
                eintrag.name,
                eintrag.quelleEnglisch,
                eintrag.seitVersion,
            ),
            modellId = einstellungen.modellId,
            denktiefe = einstellungen.denktiefe.apiValue,
        )
        val json = Prompts.leseJsonObjekt(antwort)
        val erklaerung = json?.optString("erklaerung").orEmpty()
        if (erklaerung.isBlank()) {
            KompassLog.warn(
                "Aktualisierer",
                "erklaereEinen",
                "Die Antwort enthielt keine Erklärung",
                mapOf("name" to eintrag.name),
            )
            null
        } else {
            eintrag.copy(
                kurz = json?.optString("kurz")?.takeIf(String::isNotBlank) ?: eintrag.kurz,
                kategorie = json?.optString("kategorie")?.takeIf(String::isNotBlank) ?: eintrag.kategorie,
                erklaerung = erklaerung,
                zuletztGeaendert = System.currentTimeMillis(),
            )
        }
    } catch (abbruch: CancellationException) {
        throw abbruch
    } catch (fehler: Exception) {
        // Ein einzelner missglückter Eintrag darf den Lauf nicht abbrechen. Er behält seine
        // englische Beschreibung und kommt beim nächsten Anlauf wieder dran.
        KompassLog.warn(
            "Aktualisierer",
            "erklaereEinen",
            "Erklärung fehlgeschlagen, englische Fassung bleibt stehen",
            mapOf("name" to eintrag.name, "grund" to fehler.message),
        )
        null
    }

    /**
     * Setzt Erklärungen zurück, die auf einer falsch gelesenen Beschreibung beruhen.
     *
     * Bis Fassung 0.3.1 holte die Auswertung auf der Einstellungsseite die letzte Spalte statt
     * der Beschreibung — als offizieller Text stand dort „Any file" oder „Managed". Wer damals
     * bei Codex angemeldet war, hat sich auf dieser Grundlage deutsche Erklärungen schreiben
     * lassen: plausibel klingend und inhaltlich erfunden. Der neue Ablauf würde sie nie
     * anfassen, weil sie nicht leer sind. Also werden sie hier geleert und landen dadurch in
     * der Warteschlange.
     *
     * Umgebungsvariablen bleiben aussen vor: Ihre Liste hat zwei Spalten, die letzte IST die
     * Beschreibung. Diese Erklärungen sind in Ordnung; sie zu verwerfen hiesse, dreihundert
     * Anfragen ohne Gewinn zu wiederholen.
     */
    private suspend fun repariereVergifteteErklaerungen(
        bestand: List<EintragEntity>,
        seedKennungen: Set<String>,
    ) {
        val betroffen = bestand.filter {
            it.bereich == Bereich.CONFIG.id &&
                it.art != "Umgebungsvariable" &&
                it.erklaerung.isNotBlank() &&
                it.id !in seedKennungen
        }
        if (betroffen.isEmpty()) return
        KompassLog.info(
            "Aktualisierer",
            "repariereVergifteteErklaerungen",
            "Erklärungen aus fehlerhaften Läufen werden verworfen",
            mapOf("anzahl" to betroffen.size),
        )
        betroffen.forEach {
            repository.sichereEintrag(
                it.copy(erklaerung = "", stufe = 0, zuletztGeaendert = System.currentTimeMillis()),
            )
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

    /** Schreibt den Lauf-Datensatz fest und meldet den fertigen Bericht an die Oberfläche. */
    private suspend fun schliesseAb(
        laufId: Long,
        version: String,
        stand: LaufFortschritt,
        melde: suspend (LaufFortschritt) -> Unit,
    ) {
        val meldung = baueMeldung(stand)
        repository.ladeLauf(laufId)?.let {
            repository.beendeLauf(
                it.copy(
                    beendetAm = System.currentTimeMillis(),
                    cliVersion = version,
                    neuAnzahl = stand.neuAnzahl,
                    entferntAnzahl = stand.entferntAnzahl,
                    geaendertAnzahl = stand.geaendertAnzahl,
                    status = "fertig",
                    meldung = meldung,
                ),
            )
        }
        melde(stand.copy(laeuft = false, fertig = true, schritt = meldung))
    }

    /**
     * Die Zusammenfassung in einem Satz.
     *
     * Die Namen selbst stehen im Bericht, den die Oberfläche daneben zeigt — hier steht nur,
     * wie viel wovon. Ein Lauf ohne Fund sagt das ausdrücklich, statt kommentarlos zu enden.
     */
    private fun baueMeldung(stand: LaufFortschritt): String {
        val teile = buildList {
            if (stand.neuAnzahl > 0) add(zaehle(stand.neuAnzahl, "neuer Eintrag", "neue Einträge"))
            if (stand.entferntAnzahl > 0) {
                add(zaehle(stand.entferntAnzahl, "entfernter Eintrag", "entfernte Einträge"))
            }
            if (stand.geaendertAnzahl > 0) {
                add(zaehle(stand.geaendertAnzahl, "geänderte Beschreibung", "geänderte Beschreibungen"))
            }
            if (stand.geloeschtAnzahl > 0) {
                add(zaehle(stand.geloeschtAnzahl, "Fehleintrag bereinigt", "Fehleinträge bereinigt"))
            }
            if (stand.erklaertAnzahl > 0) {
                add(zaehle(stand.erklaertAnzahl, "Erklärung geschrieben", "Erklärungen geschrieben"))
            }
        }
        val kern = if (teile.isEmpty()) {
            "Alles war schon auf dem neuesten Stand."
        } else {
            "Fertig: " + teile.joinToString(", ") + "."
        }
        return if (stand.offeneErklaerungen > 0) {
            "$kern Es warten noch ${stand.offeneErklaerungen} Einträge auf ihre deutsche Erklärung."
        } else {
            kern
        }
    }

    private fun zaehle(anzahl: Int, einzahl: String, mehrzahl: String): String =
        if (anzahl == 1) "1 $einzahl" else "$anzahl $mehrzahl"

    private fun pruefeAusbeute(was: String, anzahl: Int, grenze: Int) {
        if (anzahl >= grenze) return
        throw DokuFehler(
            "Aus den Unterlagen kamen nur $anzahl Einträge für „$was“ zurück. Das ist zu " +
                "wenig — wahrscheinlich hat sich der Aufbau der Seiten geändert. Es wurde " +
                "nichts verändert.",
        )
    }

    fun beende() = abruf.beende()

    private companion object {
        /**
         * Weniger als das kann keine echte Auskunft sein. Diese Untergrenzen sind die
         * Sicherung gegen einen stillen Totalschaden, wenn sich der Aufbau der Doku-Seiten
         * ändert — und sie gelten je Quelle, damit eine gesunde Liste eine kaputte nicht
         * überdeckt.
         */
        const val MINDEST_SLASH = 40
        const val MINDEST_EINSTELLUNGEN = 40
        const val MINDEST_VARIABLEN = 20

        /**
         * Ab so vielen offenen Erklärungen wird gefragt, statt losgelegt.
         *
         * Jede Erklärung ist eine Anfrage an das Modell. Ein Lauf, der vierhundert davon still
         * abarbeitet, ist die Art Überraschung, die man erst an der Abrechnung bemerkt.
         */
        const val SCHWELLE_RUECKFRAGE = 40

        /** So viele Fehlschläge hintereinander gelten als „die Verbindung ist weg". */
        const val ABBRUCH_NACH_FEHLERN = 5
    }
}
