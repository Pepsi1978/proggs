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
        /** Höchstens drei gleichzeitig offene Experimente (F-06). */
        const val MAX_OFFEN = 3

        /** Nach 15 Tagen wandert ein Tag ins Langzeit-Log (F-15). */
        const val TAGE_AUSFUEHRLICH = 15L

        // Merker für Hintergrundschritte, die ohne Netz ausgefallen sind (§6).
        private const val LOG_ROHSTOFF = "log"
        private const val VERDICHTUNG = "verdichtung"
        private const val ERKENNTNIS = "erkenntnis"
    }

    private val modellExperimente get() = CodexModell.aus(einstellungen.modellExperimente)
    private val effortExperimente get() = Effort.aus(einstellungen.effortExperimente)
    private val modellLogbuch get() = CodexModell.aus(einstellungen.modellLogbuch)
    private val effortLogbuch get() = Effort.aus(einstellungen.effortLogbuch)

    // --- Beobachtbare Ströme für die Oberfläche ------------------------------------------

    fun beobachteLage(tag: LocalDate) = db.lage().beobachte(tag)
    fun beobachteVorschlaege(tag: LocalDate) = db.vorschlaege().beobachte(tag)
    fun beobachteOffene() = db.experimente().beobachteOffene()
    fun beobachteZiele() = db.ziele().beobachte()
    fun beobachteMerkliste() = db.merkliste().beobachte()
    fun beobachteErkenntnisse() = db.erkenntnisse().beobachte()
    fun beobachteLogAusfuehrlich() = db.logbuch().beobachteAusfuehrlich()
    fun beobachteLogVerdichtet() = db.logbuch().beobachteVerdichtet()
    fun beobachteSelbstbild() = db.selbstbild().beobachte()
    fun beobachteGespraech(experimentId: Long) = db.gespraech().beobachte(experimentId)
    fun beobachteAuswertungen(tag: LocalDate) = db.auswertungen().beobachteTag(tag)
    fun beobachteAufgaben(experimentIds: List<Long>) = db.aufgaben().beobachte(experimentIds)

    // --- Kontext ------------------------------------------------------------------------

    /** Der vollständige Kontext in der Reihenfolge aus F-03 Schritt 1. */
    suspend fun kontext(tag: LocalDate = LocalDate.now(), mitMerkliste: Boolean = false): Kontext = Kontext(
        selbstbild = db.selbstbild().lies()?.text.orEmpty(),
        ziele = db.ziele().alle(),
        aktuellesLog = db.logbuch().alleAusfuehrlich(),
        langzeitLog = db.logbuch().alleVerdichtet(),
        erkenntnisse = db.erkenntnisse().alle(),
        laufende = db.experimente().offene(),
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
        if (db.experimente().anzahlOffene() >= MAX_OFFEN) return false
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

    // --- F-06 ---------------------------------------------------------------------------

    /**
     * F-06 — Experiment starten. Aus dem Vorschlag wird ein Experiment mit Startdatum,
     * Dauer, Stufe und der Aufgabenliste je Tag; die übrigen vier verschwinden.
     *
     * @return die Kennung, oder null wenn schon drei offen sind (dann ist die Auswahl gesperrt)
     */
    suspend fun starte(vorschlagId: Long, tag: LocalDate = LocalDate.now()): Long? {
        if (db.experimente().anzahlOffene() >= MAX_OFFEN) return null
        val vorschlag = db.vorschlaege().einer(vorschlagId) ?: return null

        val id = db.experimente().lege(
            Experiment(
                title = vorschlag.title,
                description = vorschlag.description,
                days = vorschlag.days,
                level = vorschlag.level,
                startedAt = tag,
                state = ExperimentZustand.OFFEN,
            ),
        )
        val jeTag = ausJson(vorschlag.tasksJson)
        val aufgaben = mutableListOf<Task>()
        jeTag.forEachIndexed { index, texte ->
            texte.forEachIndexed { nr, text ->
                aufgaben += Task(experimentId = id, dayIndex = index + 1, text = text, order = nr)
            }
        }
        if (aufgaben.isNotEmpty()) db.aufgaben().lege(aufgaben)

        // Die übrigen vier verschwinden.
        db.vorschlaege().verwerfeAlle(tag, Instant.now())

        // Ist der Vorschlag von der Merkliste gekommen, wird er dort entfernt.
        if (vorschlag.fromWatchlist) db.merkliste().loescheMitTitel(vorschlag.title)
        return id
    }

    // --- F-07 / F-08 --------------------------------------------------------------------

    /**
     * F-07 — die **eine** To-Do-Liste des Tages: je offenes Experiment sein Titel und
     * darunter dessen heutige Aufgaben. Nicht eine Liste je Experiment.
     */
    suspend fun tagesliste(tag: LocalDate = LocalDate.now()): List<Pair<Experiment, List<Task>>> =
        db.experimente().offene().map { experiment ->
            experiment to db.aufgaben().tagesaufgaben(experiment.id, tagNummer(experiment, tag))
        }

    /** Welcher Tag eines mehrtägigen Experiments heute ist — 1 = erster Tag. */
    fun tagNummer(experiment: Experiment, tag: LocalDate = LocalDate.now()): Int =
        (ChronoUnit.DAYS.between(experiment.startedAt, tag).toInt() + 1).coerceIn(1, experiment.days)

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

        db.auswertungen().lege(
            Evaluation(
                experimentId = experimentId,
                date = tag,
                ownText = eigenerText.trim(),
                isFinal = letzter,
            ),
        )

        val heutige = db.aufgaben().tagesaufgaben(experimentId, tagNummer(experiment, tag))
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

        schreibeLogbuchFort(
            "Auswertung zu „${experiment.title}“ ($stand)\nFrank: $eigenerText\nEinschätzung: $kiText",
        )

        if (letzter) {
            db.experimente().aendere(
                experiment.copy(state = ExperimentZustand.ABGESCHLOSSEN, closedAt = tag),
            )
            schreibeErkenntnisseFort(kiText)
        }
        return kiText
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
        val stoff = (nachlauf + neuerStoff).joinToString("\n\n")
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

    /** F-18 — eigenes Experiment anlegen. Dauer und Stufe schätzt die KI beim Speichern mit. */
    suspend fun legeEigenesAn(text: String) {
        val geschaetzt = aufgabenKi.schaetzeEigenes(text, modellExperimente, effortExperimente)
        db.merkliste().lege(
            WatchlistItem(
                title = geschaetzt.titel,
                description = geschaetzt.beschreibung,
                days = geschaetzt.tage,
                level = geschaetzt.stufe,
                tasksJson = alsJson(geschaetzt.aufgabenJeTag),
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
