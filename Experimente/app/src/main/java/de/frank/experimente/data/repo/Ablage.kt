package de.frank.experimente.data.repo

import de.frank.experimente.ai.Aufgaben
import de.frank.experimente.ai.Kontext
import de.frank.experimente.ai.alsFaden
import de.frank.experimente.auth.CodexModell
import de.frank.experimente.auth.Effort
import de.frank.experimente.data.local.ChatTurn
import de.frank.experimente.data.local.Evaluation
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.ExperimenteDatenbank
import de.frank.experimente.data.local.ExperimentZustand
import de.frank.experimente.data.local.Goal
import de.frank.experimente.data.local.Herkunft
import de.frank.experimente.data.local.Insight
import de.frank.experimente.data.local.Lage
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.data.local.Quelle
import de.frank.experimente.data.local.Rolle
import de.frank.experimente.data.local.SelfImage
import de.frank.experimente.data.local.Stufe
import de.frank.experimente.data.local.Suggestion
import de.frank.experimente.data.local.Task
import de.frank.experimente.data.local.WatchlistItem
import de.frank.experimente.data.settings.Einstellungen
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Die Ablage führt Datenzugriff und Abläufe zusammen — hier steht, was beim Antippen
 * wirklich passiert. Die Reihenfolge der Schritte folgt dem Funktions-Spec; sie wird
 * nicht zusammengefasst und nicht umsortiert.
 */
class Ablage(
    private val db: ExperimenteDatenbank,
    private val einstellungen: Einstellungen,
    private val aufgabenKi: Aufgaben,
) {
    companion object {
        /**
         * Höchstens drei gleichzeitig **laufende** Experimente (F-06, F-37).
         * Anstehende zählen nicht mit — sie sind unbegrenzt (F-34).
         */
        const val MAX_LAUFEND = 3

        /** Nach 15 Tagen wandert ein Tag ins Langzeit-Log (F-15). */
        const val TAGE_AUSFUEHRLICH = 15L

        // Merker für Hintergrundschritte, die ohne Netz ausgefallen sind (§6).
        private const val LOG_ROHSTOFF = "log"
        private const val VERDICHTUNG = "verdichtung"
        private const val ERKENNTNIS = "erkenntnis"

        /** F-35 ohne Netz: die Aufgabenliste wird beim nächsten Lauf nachgetragen. */
        private const val EIGENES = "eigenes"

        /** Eine Verlängerung, deren neue Tage noch keine Aufgaben haben. */
        private const val AUFGABEN = "aufgaben"

        /**
         * So lange darf ein Experiment höchstens laufen. Keine fachliche Grenze — eine
         * Schranke gegen Vertipper („70" statt „7"), damit nicht 70 Tagesspalten entstehen.
         */
        const val MAX_TAGE = 60
    }

    private val modellExperimente get() = CodexModell.aus(einstellungen.modellExperimente)
    private val effortExperimente get() = Effort.aus(einstellungen.effortExperimente)
    private val modellLogbuch get() = CodexModell.aus(einstellungen.modellLogbuch)
    private val effortLogbuch get() = Effort.aus(einstellungen.effortLogbuch)

    // --- Beobachtbare Ströme für die Oberfläche ------------------------------------------

    fun beobachteLage(tag: LocalDate) = db.lage().beobachte(tag)
    fun beobachteVorschlaege(tag: LocalDate) = db.vorschlaege().beobachte(tag)

    /** B-10, Abschnitt „Läuft“ (F-34). */
    fun beobachteLaufende() = db.experimente().beobachteLaufende()

    /** B-10, Abschnitt „Steht an“ (F-34). */
    fun beobachteAnstehende() = db.experimente().beobachteAnstehende()
    fun beobachteZiele() = db.ziele().beobachte()
    fun beobachteMerkliste() = db.merkliste().beobachte()
    fun beobachteErkenntnisse() = db.erkenntnisse().beobachte()
    fun beobachteLogAusfuehrlich() = db.logbuch().beobachteAusfuehrlich()
    fun beobachteLogVerdichtet() = db.logbuch().beobachteVerdichtet()
    fun beobachteSelbstbild() = db.selbstbild().beobachte()
    fun beobachteGespraech(experimentId: Long) = db.gespraech().beobachte(experimentId)
    fun beobachteAuswertungen(tag: LocalDate) = db.auswertungen().beobachteTag(tag)

    /** B-03 — die bisherigen Auswertungen eines Experiments, Tag für Tag. */
    fun beobachteAuswertungenZu(experimentId: Long) = db.auswertungen().beobachteZumExperiment(experimentId)

    /** B-07, Reiter *Auswertungen* — alle, im vollen Wortlaut, die jüngste zuerst. */
    fun beobachteAlleAuswertungen() = db.auswertungen().beobachteAlleMitTitel()

    /** Ein Experiment unabhängig von seinem Zustand — auch ein abgeschlossenes. */
    fun beobachteExperiment(experimentId: Long) = db.experimente().beobachteEines(experimentId)
    fun beobachteAufgaben(experimentIds: List<Long>) = db.aufgaben().beobachte(experimentIds)

    // --- Kontext ------------------------------------------------------------------------

    /** Der vollständige Kontext in der Reihenfolge aus F-03 Schritt 1. */
    suspend fun kontext(tag: LocalDate = LocalDate.now(), mitMerkliste: Boolean = false): Kontext = Kontext(
        selbstbild = db.selbstbild().lies()?.text.orEmpty(),
        ziele = db.ziele().alle(),
        aktuellesLog = db.logbuch().alleAusfuehrlich(),
        langzeitLog = db.logbuch().alleVerdichtet(),
        erkenntnisse = db.erkenntnisse().alle(),
        laufende = db.experimente().laufende(),
        heutigeLage = db.lage().anTag(tag)?.text,
        merkliste = if (mitMerkliste) db.merkliste().alle() else emptyList(),
    )

    // --- F-01 ---------------------------------------------------------------------------

    /**
     * F-01 — die heutige Lage festhalten. Pro Tag genau eine; eine erneute Eingabe am
     * selben Tag ersetzt die vorige und erzeugt neue Vorschläge.
     */
    suspend fun speichereLage(text: String, tag: LocalDate = LocalDate.now()) {
        db.lage().schreibe(Lage(date = tag, text = text.trim(), createdAt = Instant.now()))
    }

    // Drei schmale Fragen für den Zustand von B-01 — ohne dafür den vollen Kontext zu bauen.

    /** Die Lage eines Tages, oder `null`. */
    suspend fun lageAmTag(tag: LocalDate = LocalDate.now()): String? = db.lage().anTag(tag)?.text

    /** Stehen für diesen Tag noch nicht verworfene Vorschläge bereit? */
    suspend fun hatVorschlaege(tag: LocalDate = LocalDate.now()): Boolean =
        db.vorschlaege().aktuelle(tag).isNotEmpty()

    /** Wie viele Experimente laufen gerade? */
    suspend fun anzahlLaufende(): Int = db.experimente().anzahlLaufende()

    // --- F-02 ---------------------------------------------------------------------------

    suspend fun verbessere(text: String, bisherige: List<String>): String =
        aufgabenKi.verbessere(text, bisherige, modellExperimente, effortExperimente)

    // --- F-03 / F-04 --------------------------------------------------------------------

    /**
     * F-03 — fünf Vorschläge erzeugen. Sind bereits drei Experimente offen, wird nicht
     * ausgeführt (dann geht B-01 von `LAGE_STEHT` direkt nach `LAEUFT`).
     *
     * @return true wenn Vorschläge entstanden sind
     */
    suspend fun erzeugeVorschlaege(tag: LocalDate = LocalDate.now()): Boolean {
        if (db.experimente().anzahlLaufende() >= MAX_LAUFEND) return false
        val roh = aufgabenKi.fuenfVorschlaege(
            kontext = kontext(tag, mitMerkliste = true),
            verworfeneTitel = db.vorschlaege().verworfeneTitel(tag),
            modell = modellExperimente,
            effort = effortExperimente,
        )
        if (roh.isEmpty()) return false
        db.vorschlaege().lege(
            roh.map {
                Suggestion(
                    date = tag,
                    title = it.titel,
                    description = it.beschreibung,
                    days = it.tage,
                    level = it.stufe,
                    fromWatchlist = it.vonMerkliste,
                    tasksJson = alsJson(it.aufgabenJeTag),
                )
            },
        )
        return true
    }

    /**
     * F-04 — andere Vorschläge. Die alten gehen zuerst als „gesehen und verworfen“ in die
     * Anfrage und bleiben bis Mitternacht ausgeschlossen. Schlägt es fehl, bleiben die
     * alten fünf stehen.
     */
    suspend fun aktualisiereVorschlaege(tag: LocalDate = LocalDate.now()): Boolean {
        val alte = db.vorschlaege().aktuelle(tag)
        if (alte.isEmpty()) return erzeugeVorschlaege(tag)
        val roh = aufgabenKi.fuenfVorschlaege(
            kontext = kontext(tag, mitMerkliste = true),
            verworfeneTitel = db.vorschlaege().verworfeneTitel(tag) + alte.map { it.title },
            modell = modellExperimente,
            effort = effortExperimente,
        )
        if (roh.isEmpty()) return false
        db.vorschlaege().verwerfeAlle(tag, Instant.now())
        db.vorschlaege().lege(
            roh.map {
                Suggestion(
                    date = tag,
                    title = it.titel,
                    description = it.beschreibung,
                    days = it.tage,
                    level = it.stufe,
                    fromWatchlist = it.vonMerkliste,
                    tasksJson = alsJson(it.aufgabenJeTag),
                )
            },
        )
        return true
    }

    // --- F-05 ---------------------------------------------------------------------------

    /** F-05 — Vorschlag vollständig auf die Merkliste kopieren. Nicht doppelt merkbar. */
    suspend fun merke(vorschlag: Suggestion): Boolean {
        if (db.merkliste().zaehleMitTitel(vorschlag.title) > 0) return false
        db.merkliste().lege(
            WatchlistItem(
                title = vorschlag.title,
                description = vorschlag.description,
                days = vorschlag.days,
                level = vorschlag.level,
                tasksJson = vorschlag.tasksJson,
                source = Quelle.GEMERKT,
                createdAt = Instant.now(),
            ),
        )
        return true
    }

    suspend fun istGemerkt(titel: String): Boolean = db.merkliste().zaehleMitTitel(titel) > 0

    // --- F-36 / F-35: in den Monitor ----------------------------------------------------

    /**
     * F-36 — einen KI-Vorschlag in den Monitor übernehmen. Aus dem `Suggestion` wird ein
     * `Experiment` im Zustand `ANSTEHEND` mit der vollständigen Aufgabenliste je Tag.
     *
     * **Übernehmen ist nicht starten:** es zählt nicht gegen die Grenze von drei, die greift
     * erst bei F-37. Die übrigen Vorschläge bleiben stehen — Frank kann mehrere hintereinander
     * übernehmen. Ein Vorschlag lässt sich nicht doppelt übernehmen.
     *
     * @return die Kennung, oder null wenn der Vorschlag schon im Monitor steht
     */
    suspend fun uebernimm(vorschlagId: Long): Long? {
        val vorschlag = db.vorschlaege().einer(vorschlagId) ?: return null
        if (db.experimente().zaehleImMonitor(vorschlag.title) > 0) return null

        val id = legeAnstehendAn(
            titel = vorschlag.title,
            beschreibung = vorschlag.description,
            tage = vorschlag.days,
            stufe = vorschlag.level,
            herkunft = if (vorschlag.fromWatchlist) Herkunft.MERKLISTE else Herkunft.KI_VORSCHLAG,
            aufgabenJeTag = ausJson(vorschlag.tasksJson),
        )
        // Kam der Vorschlag von der Merkliste, wird er dort entfernt.
        if (vorschlag.fromWatchlist) db.merkliste().loescheMitTitel(vorschlag.title)
        return id
    }

    /**
     * F-35 — ein eigenes Experiment anlegen. Dauer, Stufe und Aufgabenliste ergänzt die KI
     * beim Speichern.
     *
     * **Ohne Netz wird trotzdem gespeichert** — mit dem eingetippten Text, Dauer 1 und Stufe
     * *mittel*. Die Aufgabenliste wird beim nächsten erfolgreichen Lauf nachgetragen. Es geht
     * nichts verloren.
     */
    suspend fun legeEigenesImMonitorAn(text: String, tage: Int? = null): Long {
        val sauber = text.trim()
        val gewaehlt = tage?.coerceIn(1, MAX_TAGE)
        return try {
            val geschaetzt = aufgabenKi.schaetzeEigenes(
                sauber, modellExperimente, effortExperimente, gewaehlt,
            )
            legeAnstehendAn(
                titel = geschaetzt.titel,
                beschreibung = geschaetzt.beschreibung,
                tage = gewaehlt ?: geschaetzt.tage,
                stufe = geschaetzt.stufe,
                herkunft = Herkunft.EIGEN,
                aufgabenJeTag = geschaetzt.aufgabenJeTag,
            )
        } catch (fehler: Exception) {
            einstellungen.merkeAusstehend("$EIGENES:$sauber")
            legeAnstehendAn(
                titel = sauber.lineSequence().first().take(80),
                beschreibung = sauber,
                // Auch ohne Netz gilt die gewählte Dauer — sie kommt von Frank, nicht vom Netz.
                tage = gewaehlt ?: 1,
                stufe = Stufe.MITTEL,
                herkunft = Herkunft.EIGEN,
                aufgabenJeTag = emptyList(),
            )
        }
    }

    /** Neue Karten kommen **oben** im Abschnitt „Steht an“ an (F-35 Schritt 4). */
    private suspend fun legeAnstehendAn(
        titel: String,
        beschreibung: String,
        tage: Int,
        stufe: Stufe,
        herkunft: Herkunft,
        aufgabenJeTag: List<List<String>>,
    ): Long {
        val id = db.experimente().lege(
            Experiment(
                title = titel,
                description = beschreibung,
                days = tage,
                level = stufe,
                origin = herkunft,
                addedAt = Instant.now(),
                order = (db.experimente().kleinsterRang() ?: 0) - 1,
                startedAt = null,
                state = ExperimentZustand.ANSTEHEND,
            ),
        )
        val aufgaben = mutableListOf<Task>()
        aufgabenJeTag.forEachIndexed { index, texte ->
            texte.forEachIndexed { nr, text ->
                aufgaben += Task(experimentId = id, dayIndex = index + 1, text = text, order = nr)
            }
        }
        if (aufgaben.isNotEmpty()) db.aufgaben().lege(aufgaben)
        return id
    }

    /** F-36 — steht dieser Titel schon im Monitor? Der Knopf zeigt dann den übernommenen Zustand. */
    suspend fun stehtImMonitor(titel: String): Boolean = db.experimente().zaehleImMonitor(titel) > 0

    // --- F-37 / F-06: starten -----------------------------------------------------------

    /**
     * F-37 — ein anstehendes Experiment starten. Der Zustand wechselt auf `LAEUFT`,
     * `startedAt` wird auf heute gesetzt, die Aufgaben werden auf die Tage verteilt.
     *
     * @return true wenn gestartet wurde; false wenn schon drei laufen (dann ist der Knopf
     *   gesperrt und die Karte bleibt anstehend)
     */
    suspend fun starte(experimentId: Long, tag: LocalDate = LocalDate.now()): Boolean {
        if (db.experimente().anzahlLaufende() >= MAX_LAUFEND) return false
        val experiment = db.experimente().einer(experimentId) ?: return false
        if (experiment.state != ExperimentZustand.ANSTEHEND) return false
        db.experimente().aendere(
            experiment.copy(state = ExperimentZustand.LAEUFT, startedAt = tag),
        )
        return true
    }

    /**
     * F-06 — „Jetzt starten“ auf einer Vorschlagskarte: der Abkürzungsweg, der F-36 und F-37
     * in einem Zug ausführt.
     *
     * Die übrigen vier Vorschläge **bleiben stehen** und lassen sich weiterhin in den Monitor
     * übernehmen — das ist der Unterschied zur ersten Fassung, in der sie verschwanden.
     *
     * @return die Kennung, oder null wenn schon drei laufen
     */
    suspend fun starteSofort(vorschlagId: Long, tag: LocalDate = LocalDate.now()): Long? {
        if (db.experimente().anzahlLaufende() >= MAX_LAUFEND) return null
        val id = uebernimm(vorschlagId) ?: return null
        return if (starte(id, tag)) id else null
    }

    // --- F-38 / F-39 --------------------------------------------------------------------

    /**
     * F-38 — die Reihenfolge unter „Steht an“ ändern. Nur dieser Abschnitt ist sortierbar;
     * laufende Experimente ordnen sich nach ihrem Startzeitpunkt.
     */
    suspend fun sortiere(experimentId: Long, nachIndex: Int) {
        val liste = db.experimente().anstehende().toMutableList()
        val von = liste.indexOfFirst { it.id == experimentId }
        if (von < 0) return
        val nach = nachIndex.coerceIn(0, liste.size - 1)
        if (von == nach) return
        liste.add(nach, liste.removeAt(von))
        liste.forEachIndexed { rang, satz -> db.experimente().setzeRang(satz.id, rang) }
    }

    /**
     * F-39 — ein anstehendes Experiment aus dem Monitor nehmen. Zwei Wege:
     * `aufMerkliste = true` erhält es vollständig auf der Merkliste (B-05),
     * `false` entfernt es endgültig.
     *
     * Gilt **nur für anstehende**. Ein laufendes wird über „Nicht umgesetzt“ (F-13) beendet,
     * nicht gelöscht — sonst ginge seine Geschichte verloren.
     */
    suspend fun nimmAusMonitor(experimentId: Long, aufMerkliste: Boolean) {
        val experiment = db.experimente().einer(experimentId) ?: return
        if (experiment.state != ExperimentZustand.ANSTEHEND) return
        if (aufMerkliste) {
            val jeTag = db.aufgaben().alleZu(experimentId)
                .groupBy { it.dayIndex }
                .toSortedMap()
                .map { (_, aufgaben) -> aufgaben.sortedBy { it.order }.map { it.text } }
            db.merkliste().lege(
                WatchlistItem(
                    title = experiment.title,
                    description = experiment.description,
                    days = experiment.days,
                    level = experiment.level,
                    tasksJson = alsJson(jeTag),
                    source = if (experiment.origin == Herkunft.EIGEN) Quelle.EIGEN else Quelle.GEMERKT,
                    createdAt = Instant.now(),
                ),
            )
        }
        db.aufgaben().loescheZu(experimentId)
        db.experimente().loesche(experiment)
    }

    // --- F-07 / F-08 --------------------------------------------------------------------

    /**
     * F-07 — die **eine** To-Do-Liste des Tages: je laufendes Experiment sein Titel und
     * darunter dessen heutige Aufgaben. Nicht eine Liste je Experiment. Sie steht seit dieser
     * Fassung unter dem Abschnitt „Läuft“ im Monitor.
     */
    suspend fun tagesliste(tag: LocalDate = LocalDate.now()): List<Pair<Experiment, List<Task>>> =
        db.experimente().laufende().map { experiment ->
            experiment to db.aufgaben().tagesaufgaben(experiment.id, tagNummer(experiment, tag))
        }

    /**
     * Welcher Tag eines mehrtägigen Experiments heute ist — 1 = erster Tag.
     * Ein anstehendes Experiment hat noch keinen Starttag; für es gilt Tag 1.
     */
    fun tagNummer(experiment: Experiment, tag: LocalDate = LocalDate.now()): Int {
        val start = experiment.startedAt ?: return 1
        return (ChronoUnit.DAYS.between(start, tag).toInt() + 1).coerceIn(1, experiment.days)
    }

    fun istLetzterTag(experiment: Experiment, tag: LocalDate = LocalDate.now()): Boolean =
        tagNummer(experiment, tag) >= experiment.days

    /** F-08 — Haken setzen oder zurücknehmen. Der Stand übersteht einen Neustart. */
    suspend fun schalteHaken(aufgabeId: Long) {
        val aufgabe = db.aufgaben().eine(aufgabeId) ?: return
        db.aufgaben().setzeHaken(aufgabeId, if (aufgabe.doneAt == null) Instant.now() else null)
    }

    // --- F-09 ---------------------------------------------------------------------------

    /**
     * F-09 — eine Runde im Gespräch: Franks Frage wird gespeichert, die Antwort geholt,
     * gespeichert und zurückgegeben (das Vorlesen macht die Oberfläche).
     */
    suspend fun sprich(experimentId: Long, frage: String): String {
        val experiment = db.experimente().einer(experimentId)
            ?: throw IllegalStateException("Das Experiment gibt es nicht mehr.")
        val faden = db.gespraech().faden(experimentId)
        db.gespraech().lege(
            ChatTurn(experimentId = experimentId, role = Rolle.ICH, text = frage.trim(), createdAt = Instant.now()),
        )
        val antwort = aufgabenKi.gespraechsantwort(
            kontext = kontext(),
            experiment = experiment,
            faden = faden.alsFaden(),
            frage = frage,
            modell = modellExperimente,
            effort = effortExperimente,
        )
        db.gespraech().lege(
            ChatTurn(experimentId = experimentId, role = Rolle.KI, text = antwort, createdAt = Instant.now()),
        )
        schreibeLogbuchFort("Gespräch zu „${experiment.title}“:\nFrank: $frage\nAntwort: $antwort")
        return antwort
    }

    // --- F-10 / F-11 / F-13 -------------------------------------------------------------

    /**
     * F-10 — Franks eigene Auswertung festhalten, danach F-11 (die KI-Auswertung).
     *
     * Am letzten Tag entsteht die vollständige Auswertung; danach laufen F-13 (Abschluss)
     * und F-17 (Erkenntnisse). An einem Zwischentag nur ein kurzer Zwischenstand.
     *
     * Franks Text bleibt in jedem Fall gespeichert — auch wenn die KI nicht antwortet.
     */
    suspend fun werteAus(experimentId: Long, eigenerText: String, tag: LocalDate = LocalDate.now()): String? {
        val experiment = db.experimente().einer(experimentId) ?: return null
        val letzter = istLetzterTag(experiment, tag)
        val tagNr = tagNummer(experiment, tag)
        val sauber = eigenerText.trim()

        // Die Auswertung des Tages wird FORTGESCHRIEBEN, nicht ein zweites Mal angelegt —
        // sonst stünden nach einem zweiten Anlauf zwei Zeilen zum selben Tag in der Ablage.
        val bisherige = db.auswertungen().anTag(experimentId, tag)
        if (bisherige == null) {
            db.auswertungen().lege(
                Evaluation(
                    experimentId = experimentId,
                    date = tag,
                    ownText = sauber,
                    isFinal = letzter,
                    createdAt = Instant.now(),
                ),
            )
        } else {
            db.auswertungen().aendere(
                bisherige.copy(
                    ownText = sauber,
                    aiText = null,
                    isFinal = letzter,
                    createdAt = Instant.now(),
                ),
            )
        }

        // **Der eingesprochene Text gehört in den Gesprächsfaden — sofort, vor dem Netz.**
        //
        // Vorher lag er allein in `auswertungen`, und diese Tabelle zeigt kein Bildschirm an:
        // nach „Weiter" war das Gesagte für Frank spurlos verschwunden. Es ging auch nicht in
        // den Faden ein, den jede spätere Anfrage mitbekommt — eingesprochen, aber wirkungslos.
        //
        // Er wird VOR dem KI-Aufruf geschrieben. Fällt das Netz aus, ist er trotzdem da.
        db.gespraech().lege(
            ChatTurn(
                experimentId = experimentId,
                role = Rolle.ICH,
                text = "Auswertung Tag $tagNr:\n$sauber",
                createdAt = Instant.now(),
            ),
        )

        val heutige = db.aufgaben().tagesaufgaben(experimentId, tagNr)
        val stand = if (heutige.isEmpty()) {
            "keine Aufgaben für heute"
        } else {
            val fertig = heutige.count { it.doneAt != null }
            "$fertig von ${heutige.size} abgehakt:\n" +
                heutige.joinToString("\n") { "- [${if (it.doneAt != null) "x" else " "}] ${it.text}" }
        }

        val kiText = aufgabenKi.auswertung(
            kontext = kontext(tag),
            experiment = experiment,
            faden = db.gespraech().faden(experimentId).alsFaden(),
            hakenStand = stand,
            eigenerText = eigenerText,
            istLetzterTag = letzter,
            modell = modellExperimente,
            effort = effortExperimente,
        )

        db.auswertungen().anTag(experimentId, tag)?.let {
            db.auswertungen().aendere(it.copy(aiText = kiText))
        }

        // Die Einschätzung gehört in denselben Faden wie Franks Text — im Gespräch stehen
        // beide danach untereinander und gehen in jede weitere Anfrage ein.
        if (kiText.isNotBlank()) {
            db.gespraech().lege(
                ChatTurn(
                    experimentId = experimentId,
                    role = Rolle.KI,
                    text = kiText,
                    createdAt = Instant.now(),
                ),
            )
        }

        schreibeLogbuchFort(
            "Auswertung zu „${experiment.title}“ ($stand)\nFrank: $sauber\nEinschätzung: $kiText",
            tag,
        )

        // **Die Auswertung schließt das Experiment NICHT ab.**
        //
        // Vorher tat sie genau das: am letzten Tag setzte sie den Zustand auf ABGESCHLOSSEN,
        // still, im selben Zug. Wer bei „Tag 2 von 2" erzählte, wie es gelaufen ist, hatte
        // sein Experiment damit beendet — auch wenn er es fortführen wollte. Erzählen ist
        // nicht beenden. Über das Ende entscheidet Frank auf B-03, nicht der Kalender.
        return kiText
    }

    /**
     * F-13 — das Experiment tatsächlich abschließen. Erst hier entstehen die Erkenntnisse.
     *
     * Getrennt von [werteAus], damit die Auswertung eines Tages nie das ganze Experiment
     * beendet.
     */
    suspend fun schliesseAb(experimentId: Long, tag: LocalDate = LocalDate.now()) {
        val experiment = db.experimente().einer(experimentId) ?: return
        db.experimente().aendere(
            experiment.copy(state = ExperimentZustand.ABGESCHLOSSEN, closedAt = tag),
        )
        val letzteAuswertung = db.auswertungen().anTag(experimentId, tag)
        db.auswertungen().anTag(experimentId, tag)?.let {
            db.auswertungen().aendere(it.copy(isFinal = true))
        }
        val stoff = letzteAuswertung?.aiText?.takeIf { it.isNotBlank() }
            ?: letzteAuswertung?.ownText
        if (!stoff.isNullOrBlank()) schreibeErkenntnisseFort(stoff)
    }

    /**
     * Das Experiment läuft weiter, obwohl sein letzter Tag erreicht ist — Frank verlängert es
     * um weitere Tage. Die Aufgaben für die neuen Tage liefert die KI nach.
     */
    suspend fun fuehreFort(experimentId: Long, neueTage: Int) {
        aendereDauer(experimentId, neueTage)
    }

    /**
     * Die Dauer eines Experiments ändern — beim Anlegen **und** mittendrin.
     *
     * Die KI schätzte die Dauer bisher allein, und sie ließ sich danach nirgends berichtigen:
     * aus „die nächsten sechs, sieben Tage" wurden zwei, und dabei blieb es. Jetzt entscheidet
     * Frank, jederzeit.
     *
     * **Beim Kürzen wird nichts gelöscht.** Die Aufgaben der wegfallenden Tage bleiben in der
     * Ablage stehen; wird später wieder verlängert, sind sie unverändert da. Beim Verlängern
     * liefert die KI die fehlenden Tage nach — geht das gerade nicht, bleibt es vorgemerkt.
     */
    suspend fun aendereDauer(experimentId: Long, neueTage: Int) {
        val experiment = db.experimente().einer(experimentId) ?: return
        val tage = neueTage.coerceIn(1, MAX_TAGE)
        if (tage == experiment.days) return
        db.experimente().aendere(experiment.copy(days = tage))
        if (tage > experiment.days) ergaenzeAufgaben(experimentId)
    }

    /**
     * Für Tage ohne Aufgaben welche nachliefern — nach einer Verlängerung, oder wenn ein
     * eigenes Experiment ohne Netz angelegt wurde.
     */
    suspend fun ergaenzeAufgaben(experimentId: Long) {
        val experiment = db.experimente().einer(experimentId) ?: return
        val vorhanden = db.aufgaben().alleZu(experimentId)
        val belegteTage = vorhanden.map { it.dayIndex }.toSet()
        val fehlende = (1..experiment.days).filterNot { it in belegteTage }
        if (fehlende.isEmpty()) return

        val bisher = vorhanden.groupBy { it.dayIndex }.toSortedMap()
            .map { (tag, liste) -> tag to liste.sortedBy { it.order }.map { it.text } }
        try {
            val neue = aufgabenKi.weitereTage(
                experiment = experiment,
                bisherJeTag = bisher,
                fehlendeTage = fehlende,
                modell = modellExperimente,
                effort = effortExperimente,
            )
            val zuLegen = mutableListOf<Task>()
            neue.forEach { (tagNr, texte) ->
                texte.forEachIndexed { nr, text ->
                    zuLegen += Task(experimentId = experimentId, dayIndex = tagNr, text = text, order = nr)
                }
            }
            if (zuLegen.isNotEmpty()) db.aufgaben().lege(zuLegen)
        } catch (fehler: Exception) {
            // Vorgemerkt statt verloren: der Nachlauf holt es beim nächsten Mal.
            einstellungen.merkeAusstehend("$AUFGABEN:$experimentId")
        }
    }

    /**
     * F-13, Weg „Nicht umgesetzt“: Der Vorgang wandert in die Chronik **und** das Experiment
     * kommt zurück auf die Merkliste — mit dem Vermerk, was im Weg stand.
     */
    suspend fun nichtUmgesetzt(experimentId: Long, grund: String, tag: LocalDate = LocalDate.now()) {
        val experiment = db.experimente().einer(experimentId) ?: return
        db.experimente().aendere(
            experiment.copy(state = ExperimentZustand.NICHT_UMGESETZT, closedAt = tag),
        )
        // Die Oberfläche sagt „der Eintrag steht im Logbuch" — bis hierher stand er dort nie.
        schreibeLogbuchFort(
            "„${experiment.title}“ wurde nicht umgesetzt." +
                grund.trim().takeIf { it.isNotBlank() }?.let { "\nGrund: $it" }.orEmpty(),
            tag,
        )
        val aufgaben = db.aufgaben().tagesaufgaben(experimentId, 1)
        db.merkliste().lege(
            WatchlistItem(
                title = experiment.title,
                description = experiment.description,
                days = experiment.days,
                level = experiment.level,
                tasksJson = alsJson(listOf(aufgaben.map { it.text })),
                source = Quelle.NICHT_UMGESETZT,
                note = grund.trim().takeIf { it.isNotBlank() },
                createdAt = Instant.now(),
            ),
        )
    }

    // --- F-14 / F-15 / F-16 -------------------------------------------------------------

    /**
     * F-14 — den heutigen Eintrag fortschreiben. Kein Netz: der Rohstoff bleibt gespeichert
     * und wird beim nächsten erfolgreichen Lauf nachgetragen. Es geht nichts verloren.
     */
    suspend fun schreibeLogbuchFort(neuerStoff: String, tag: LocalDate = LocalDate.now()) {
        val bisheriger = db.logbuch().tag(tag)
        val nachlauf = einstellungen.ausstehend.filter { it.startsWith("$LOG_ROHSTOFF:") }
            .map { it.removePrefix("$LOG_ROHSTOFF:") }
        // Ein leerer Anstoß aus dem Nachlauf darf keinen leeren Merker erzeugen.
        val teile = (nachlauf + neuerStoff).filter { it.isNotBlank() }
        if (teile.isEmpty()) return
        val stoff = teile.joinToString("\n\n")
        try {
            val text = aufgabenKi.logbuchFortschreiben(
                tag = tag,
                bisheriger = bisheriger?.detailText,
                neuerStoff = stoff,
                modell = modellLogbuch,
                effort = effortLogbuch,
            )
            db.logbuch().schreibe(
                (bisheriger ?: LogDay(date = tag)).copy(detailText = text, compactText = null),
            )
            nachlauf.forEach { einstellungen.erledigeAusstehend("$LOG_ROHSTOFF:$it") }
        } catch (fehler: Exception) {
            // Der Rohstoff bleibt erhalten und läuft beim nächsten Mal mit.
            einstellungen.merkeAusstehend("$LOG_ROHSTOFF:$neuerStoff")
        }
    }

    /**
     * F-15 — Tagesverdichtung, beim ersten Öffnen an einem neuen Kalendertag.
     *
     * Der ausführliche Text wird **erst** gelöscht, wenn der verdichtete vorliegt. Ohne Netz
     * bleibt der Tag ausführlich stehen — kein Datenverlust.
     *
     * @return Anzahl der verdichteten Tage
     */
    suspend fun verdichteFaellige(heute: LocalDate = LocalDate.now()): Int {
        val grenze = heute.minusDays(TAGE_AUSFUEHRLICH)
        val faellige = db.logbuch().zuVerdichten(grenze)
        var gezaehlt = 0
        for (tag in faellige) {
            val ausfuehrlich = tag.detailText ?: continue
            try {
                val kurz = aufgabenKi.verdichte(tag.date, ausfuehrlich, modellLogbuch, effortLogbuch)
                if (kurz.isBlank()) continue
                db.logbuch().schreibe(
                    tag.copy(detailText = null, compactText = kurz, compactedAt = Instant.now()),
                )
                gezaehlt++
            } catch (fehler: Exception) {
                // Verschoben, nicht verloren: der Tag bleibt ausführlich stehen.
                einstellungen.merkeAusstehend("$VERDICHTUNG:${tag.date}")
                break
            }
        }
        einstellungen.verdichtetAm = heute.toString()
        return gezaehlt
    }

    /** Läuft F-15 heute noch aus? */
    fun verdichtungFaellig(heute: LocalDate = LocalDate.now()): Boolean =
        einstellungen.verdichtetAm != heute.toString()

    /**
     * §6 — die ohne Netz liegengebliebenen Schritte nachholen.
     *
     * Der Merker wurde an fünf Stellen gesetzt, und überall stand im Kommentar „läuft beim
     * nächsten Start nach, es geht nichts verloren". **Nachgeholt hat es nie jemand:** die
     * Einträge sammelten sich in den Einstellungen an, ohne dass sie eine Funktion je wieder
     * gelesen hätte. Ein eigenes Experiment ohne Netz blieb für immer ohne Aufgabenliste,
     * und eine Erkenntnis, die einmal am Netz scheiterte, entstand nie.
     *
     * Der Lauf ist absichtlich anspruchslos: was jetzt nicht geht, bleibt stehen und kommt
     * beim nächsten Mal wieder dran. Kein Eintrag wird verworfen, ohne erledigt zu sein.
     */
    suspend fun holeNach(heute: LocalDate = LocalDate.now()) {
        for (eintrag in einstellungen.ausstehend.toList()) {
            try {
                when {
                    eintrag.startsWith("$EIGENES:") -> {
                        val text = eintrag.removePrefix("$EIGENES:")
                        // Die Karte steht längst im Monitor — nachzutragen ist ihre
                        // Aufgabenliste, und nur wenn sie noch fehlt.
                        val geschaetzt = aufgabenKi.schaetzeEigenes(text, modellExperimente, effortExperimente)
                        val karte = db.experimente().anstehende()
                            .firstOrNull { it.description == text || it.title == text.lineSequence().first().take(80) }
                        if (karte != null && db.aufgaben().alleZu(karte.id).isEmpty()) {
                            val aufgaben = mutableListOf<Task>()
                            geschaetzt.aufgabenJeTag.forEachIndexed { index, texte ->
                                texte.forEachIndexed { nr, zeile ->
                                    aufgaben += Task(experimentId = karte.id, dayIndex = index + 1, text = zeile, order = nr)
                                }
                            }
                            if (aufgaben.isNotEmpty()) db.aufgaben().lege(aufgaben)
                            db.experimente().aendere(
                                karte.copy(
                                    title = geschaetzt.titel,
                                    description = geschaetzt.beschreibung,
                                    days = geschaetzt.tage,
                                    level = geschaetzt.stufe,
                                ),
                            )
                        }
                        einstellungen.erledigeAusstehend(eintrag)
                    }

                    eintrag.startsWith("$AUFGABEN:") -> {
                        val id = eintrag.removePrefix("$AUFGABEN:").toLongOrNull()
                        einstellungen.erledigeAusstehend(eintrag)
                        if (id != null) ergaenzeAufgaben(id)
                    }

                    eintrag.startsWith("$ERKENNTNIS:") -> {
                        val auswertung = eintrag.removePrefix("$ERKENNTNIS:")
                        // Erst den Merker lösen, dann schreiben: sonst legt ein erneuter
                        // Fehlschlag denselben Eintrag ein zweites Mal an.
                        einstellungen.erledigeAusstehend(eintrag)
                        schreibeErkenntnisseFort(auswertung)
                    }

                    eintrag.startsWith("$VERDICHTUNG:") -> {
                        // Die Verdichtung arbeitet die fälligen Tage ohnehin von vorn ab.
                        einstellungen.erledigeAusstehend(eintrag)
                        verdichteFaellige(heute)
                    }

                    eintrag.startsWith("$LOG_ROHSTOFF:") -> {
                        // Der Rohstoff läuft beim nächsten Fortschreiben von selbst mit —
                        // hier wird er angestoßen, damit er nicht auf einen Anlass wartet.
                        schreibeLogbuchFort("", heute)
                    }
                }
            } catch (fehler: Exception) {
                // Bleibt stehen und kommt beim nächsten Mal wieder dran.
                break
            }
        }
    }

    /** F-16 — einen Logbuch-Eintrag ändern. Gilt für beide Reiter. */
    suspend fun aendereLogtag(tag: LogDay, neuerText: String) {
        db.logbuch().schreibe(
            if (tag.detailText != null) tag.copy(detailText = neuerText) else tag.copy(compactText = neuerText),
        )
    }

    /** F-16 — einen Logbuch-Eintrag löschen. Endgültig. */
    suspend fun loescheLogtag(tag: LogDay) = db.logbuch().loesche(tag)

    // --- F-17 ---------------------------------------------------------------------------

    /** F-17 — Erkenntnisse fortschreiben. Ohne Netz wird beim nächsten Lauf nachgeholt. */
    suspend fun schreibeErkenntnisseFort(neueAuswertung: String) {
        try {
            val bisherige = db.erkenntnisse().alle()
            val neue = aufgabenKi.erkenntnisseFortschreiben(
                bisherige = bisherige,
                neueAuswertung = neueAuswertung,
                modell = modellLogbuch,
                effort = effortLogbuch,
            )
            if (neue.isEmpty()) return
            // Die KI gibt die vollständige Liste zurück; sie ersetzt die alte, statt anzuhängen.
            bisherige.forEach { db.erkenntnisse().loesche(it) }
            neue.forEach { db.erkenntnisse().lege(Insight(text = it, updatedAt = Instant.now())) }
        } catch (fehler: Exception) {
            einstellungen.merkeAusstehend("$ERKENNTNIS:$neueAuswertung")
        }
    }

    // --- F-18 / F-19 --------------------------------------------------------------------

    /**
     * F-18 — eigenes Experiment anlegen. Dauer und Stufe schätzt die KI beim Speichern mit.
     *
     * **Ohne Netz wird trotzdem gespeichert**, genau wie beim Weg in den Monitor (F-35). Vorher
     * warf diese Stelle den Fehler nach oben durch und legte gar nichts an: eine eingesprochene
     * Idee war nach der Störungsmeldung nirgends abgelegt.
     */
    suspend fun legeEigenesAn(text: String, tage: Int? = null) {
        val sauber = text.trim()
        val gewaehlt = tage?.coerceIn(1, MAX_TAGE)
        val geschaetzt = runCatching {
            aufgabenKi.schaetzeEigenes(sauber, modellExperimente, effortExperimente, gewaehlt)
        }.getOrNull()
        db.merkliste().lege(
            WatchlistItem(
                title = geschaetzt?.titel ?: sauber.lineSequence().first().take(80),
                description = geschaetzt?.beschreibung ?: sauber,
                days = gewaehlt ?: geschaetzt?.tage ?: 1,
                level = geschaetzt?.stufe ?: Stufe.MITTEL,
                tasksJson = alsJson(geschaetzt?.aufgabenJeTag ?: emptyList()),
                source = Quelle.EIGEN,
                createdAt = Instant.now(),
            ),
        )
    }

    /** F-19 — Merklisten-Eintrag löschen. Endgültig. */
    suspend fun loescheMerkliste(eintrag: WatchlistItem) = db.merkliste().loesche(eintrag)

    // --- F-20 / F-21 --------------------------------------------------------------------

    /** F-20 — Ziel anlegen. Keine Mengenbegrenzung. */
    suspend fun legeZielAn(text: String) {
        val jetzt = Instant.now()
        db.ziele().lege(Goal(text = text.trim(), createdAt = jetzt, updatedAt = jetzt))
    }

    suspend fun aendereZiel(ziel: Goal, neuerText: String) =
        db.ziele().aendere(ziel.copy(text = neuerText.trim(), updatedAt = Instant.now()))

    suspend fun loescheZiel(ziel: Goal) = db.ziele().loesche(ziel)

    /** F-21 — Selbstbild pflegen. Keine Längenbegrenzung, ein Fließtext. */
    suspend fun speichereSelbstbild(text: String) =
        db.selbstbild().schreibe(SelfImage(id = 1, text = text, updatedAt = Instant.now()))

    /**
     * F-21 — das gespeicherte Selbstbild direkt aus der Ablage.
     *
     * B-09 las es vorher aus dem beobachteten Strom. Der beginnt aber bei `null` und wird
     * erst kurz darauf gefüllt — beim Betreten des Bildschirms stand deshalb regelmäßig ein
     * leeres Feld da, obwohl der Text längst gespeichert war.
     */
    suspend fun liesSelbstbild(): String = db.selbstbild().lies()?.text.orEmpty()

    // --- Aufgaben je Tag als JSON -------------------------------------------------------

    private fun alsJson(jeTag: List<List<String>>): String {
        val aussen = JSONArray()
        jeTag.forEach { tag ->
            val innen = JSONArray()
            tag.forEach(innen::put)
            aussen.put(innen)
        }
        return aussen.toString()
    }

    internal fun ausJson(json: String): List<List<String>> = runCatching {
        val aussen = JSONArray(json)
        (0 until aussen.length()).map { i ->
            val innen = aussen.optJSONArray(i) ?: JSONArray()
            (0 until innen.length()).mapNotNull { innen.optString(it).takeIf(String::isNotBlank) }
        }
    }.getOrDefault(emptyList())

    /** Stufe als Wort, wie sie auf der Karte steht. */
    fun stufenwort(stufe: Stufe): String = when (stufe) {
        Stufe.LEICHT -> "leicht"
        Stufe.MITTEL -> "mittel"
        Stufe.FORDERND -> "fordernd"
    }
}
