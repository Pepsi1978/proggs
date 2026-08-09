package de.frank.experimente.data.repo

import android.content.Context
import de.frank.experimente.ai.Antworten
import de.frank.experimente.ai.Aufgaben
import de.frank.experimente.ai.Gedaechtnis
import de.frank.experimente.ai.RohVorschlag
import de.frank.experimente.ai.alsFaden
import de.frank.experimente.ai.alsVerlauf
import de.frank.experimente.auth.CodexAuthManager
import de.frank.experimente.auth.CodexModel
import de.frank.experimente.auth.ReasoningEffort
import de.frank.experimente.data.local.Aufgabe
import de.frank.experimente.data.local.Auswertung
import de.frank.experimente.data.local.Erkenntnis
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.ExperimentZustand
import de.frank.experimente.data.local.ExperimenteDatenbank
import de.frank.experimente.data.local.GespraechsRunde
import de.frank.experimente.data.local.Lage
import de.frank.experimente.data.local.LogTag
import de.frank.experimente.data.local.MerkEintrag
import de.frank.experimente.data.local.MerkQuelle
import de.frank.experimente.data.local.SelbstbildSatz
import de.frank.experimente.data.local.Sprecher
import de.frank.experimente.data.local.Vorschlag
import de.frank.experimente.data.local.Ziel
import de.frank.experimente.data.settings.Einstellungen
import java.time.Instant
import java.time.LocalDate

/**
 * Alles Verhalten aus 01-FUNKTIONS-SPEC.md an einem Ort: Datenbank, Dienste und die
 * KI-Aufträge zusammengeführt.
 *
 * Regeln, die hier durchgesetzt werden:
 * - Höchstens **drei** gleichzeitig offene Experimente (F-06).
 * - Das aktuelle Log hält **15 Tage** ausführlich, danach wird verdichtet (F-15).
 * - Ein nicht umgesetztes Experiment wird **trotzdem** ausgewertet **und** landet zurück
 *   auf der Merkliste (F-13).
 */
class Ablage(
    context: Context,
    private val einstellungen: Einstellungen,
    val codex: CodexAuthManager,
) {
    private val db = ExperimenteDatenbank.hole(context)

    val selbstbildFlow = db.selbstbild().beobachte()
    val zieleFlow = db.ziele().beobachte()
    val logAusfuehrlichFlow = db.log().beobachteAusfuehrlich()
    val logLangzeitFlow = db.log().beobachteLangzeit()
    val merklisteFlow = db.merkliste().beobachte()
    val erkenntnisseFlow = db.erkenntnisse().beobachte()
    val offeneExperimenteFlow = db.experimente().beobachteOffene()

    fun lageFlow(datum: LocalDate) = db.lage().beobachte(datum)
    fun vorschlaegeFlow(datum: LocalDate) = db.vorschlaege().beobachteHeute(datum)
    fun gespraechFlow(experimentId: Long) = db.gespraeche().beobachte(experimentId)
    fun aufgabenFlow(experimentId: Long) = db.aufgaben().beobachteAlle(experimentId)
    fun auswertungenFlow(experimentId: Long) = db.auswertungen().beobachte(experimentId)

    // -----------------------------------------------------------------------
    // Modellwahl (F-22)
    // -----------------------------------------------------------------------

    private fun modellExperimente() = CodexModel.fromLabel(einstellungen.modellExperimente)
    private fun effortExperimente() = ReasoningEffort.fromLabel(einstellungen.effortExperimente)
    private fun modellLogbuch() = CodexModel.fromLabel(einstellungen.modellLogbuch)
    private fun effortLogbuch() = ReasoningEffort.fromLabel(einstellungen.effortLogbuch)

    // -----------------------------------------------------------------------
    // Das Gedächtnis (Grundlage jeder Anfrage)
    // -----------------------------------------------------------------------

    suspend fun gedaechtnis(heute: LocalDate): Gedaechtnis = Gedaechtnis(
        selbstbild = db.selbstbild().lies()?.text.orEmpty(),
        ziele = db.ziele().alle(),
        aktuellesLog = db.log().alleAusfuehrlich(),
        langzeitLog = db.log().alleLangzeit(),
        erkenntnisse = db.erkenntnisse().alle(),
        laufende = db.experimente().offene(),
        merkliste = db.merkliste().alle(),
        heutigeLage = db.lage().lies(heute)?.text.orEmpty(),
    )

    // -----------------------------------------------------------------------
    // F-01 — Lage
    // -----------------------------------------------------------------------

    suspend fun lageSpeichern(heute: LocalDate, text: String) {
        db.lage().schreib(Lage(datum = heute, text = text.trim(), angelegtAm = Instant.now()))
    }

    suspend fun lage(heute: LocalDate): String? = db.lage().lies(heute)?.text

    // -----------------------------------------------------------------------
    // F-02 — Text mit KI verbessern
    // -----------------------------------------------------------------------

    suspend fun textVerbessern(text: String, bisherige: List<String>): String =
        codex.improveWish(text, bisherige, modellExperimente(), effortExperimente())

    // -----------------------------------------------------------------------
    // F-03 / F-04 — fünf Vorschläge
    // -----------------------------------------------------------------------

    /** Bei drei offenen Experimenten kommen keine neuen Vorschläge (F-06). */
    suspend fun darfVorschlagen(): Boolean = db.experimente().anzahlOffene() < MAX_OFFEN

    suspend fun vorschlaegeErzeugen(heute: LocalDate, erneut: Boolean) {
        if (!darfVorschlagen()) return

        if (erneut) {
            db.vorschlaege().verwirfOffene(heute, Instant.now())
        }
        val verworfen = db.vorschlaege().alleDesTages(heute)
            .filter { it.verworfenAm != null }
            .map { it.titel }

        val g = gedaechtnis(heute)
        // Der Merklisten-Platz: der älteste Eintrag, den es heute noch nicht gab.
        val merkKandidat = g.merkliste.firstOrNull { it.titel !in verworfen }

        val roh = codex.frage(
            Aufgaben.vorschlaege(
                gedaechtnis = g,
                verworfeneTitel = verworfen,
                merklistenTitel = merkKandidat?.titel,
                model = modellExperimente(),
                effort = effortExperimente(),
            ),
        )
        val vorschlaege = Antworten.vorschlaege(roh).take(5)
        db.vorschlaege().schreibAlle(
            vorschlaege.map { v ->
                Vorschlag(
                    datum = heute,
                    titel = v.titel,
                    beschreibung = v.beschreibung,
                    tage = v.tage,
                    stufe = v.stufe,
                    vonMerkliste = v.vonMerkliste,
                    aufgabenJson = Antworten.aufgabenZuJson(v.aufgabenJeTag),
                )
            },
        )
        db.vorschlaege().raeumeAlteAuf(heute.minusDays(2))
    }

    // -----------------------------------------------------------------------
    // F-05 / F-18 / F-19 — Merkliste
    // -----------------------------------------------------------------------

    suspend fun merken(vorschlag: Vorschlag) {
        if (db.merkliste().mitTitel(vorschlag.titel) != null) return
        db.merkliste().schreib(
            MerkEintrag(
                titel = vorschlag.titel,
                beschreibung = vorschlag.beschreibung,
                tage = vorschlag.tage,
                stufe = vorschlag.stufe,
                aufgabenJson = vorschlag.aufgabenJson,
                quelle = MerkQuelle.GEMERKT,
                angelegtAm = Instant.now(),
            ),
        )
    }

    suspend fun istGemerkt(titel: String): Boolean = db.merkliste().mitTitel(titel) != null

    suspend fun eigeneIdeeAnlegen(idee: String, heute: LocalDate) {
        val roh = codex.frage(
            Aufgaben.eigeneIdee(idee, gedaechtnis(heute), modellExperimente(), effortExperimente()),
        )
        val v: RohVorschlag = Antworten.eigeneIdee(roh)
        db.merkliste().schreib(
            MerkEintrag(
                titel = v.titel,
                beschreibung = v.beschreibung,
                tage = v.tage,
                stufe = v.stufe,
                aufgabenJson = Antworten.aufgabenZuJson(v.aufgabenJeTag),
                quelle = MerkQuelle.EIGEN,
                angelegtAm = Instant.now(),
            ),
        )
    }

    suspend fun merkeintragLoeschen(eintrag: MerkEintrag) = db.merkliste().loesche(eintrag)

    // -----------------------------------------------------------------------
    // F-06 — Experiment starten
    // -----------------------------------------------------------------------

    suspend fun experimentStarten(vorschlag: Vorschlag, heute: LocalDate): Long? {
        if (db.experimente().anzahlOffene() >= MAX_OFFEN) return null

        val id = db.experimente().schreib(
            Experiment(
                titel = vorschlag.titel,
                beschreibung = vorschlag.beschreibung,
                tage = vorschlag.tage,
                stufe = vorschlag.stufe,
                begonnenAm = heute,
            ),
        )
        val jeTag = Antworten.aufgabenAusJson(vorschlag.aufgabenJson)
        db.aufgaben().schreibAlle(
            jeTag.flatMapIndexed { tagIndex, aufgaben ->
                aufgaben.mapIndexed { i, text ->
                    Aufgabe(
                        experimentId = id,
                        tagNummer = tagIndex + 1,
                        text = text,
                        reihenfolge = i,
                    )
                }
            },
        )
        // Die übrigen vier verschwinden — und ein Merklisten-Vorschlag ist verbraucht.
        db.vorschlaege().verwirfOffene(heute, Instant.now())
        if (vorschlag.vonMerkliste) {
            db.merkliste().mitTitel(vorschlag.titel)?.let { db.merkliste().loesche(it) }
        }
        return id
    }

    // -----------------------------------------------------------------------
    // F-07 / F-08 — Aufgaben des Tages
    // -----------------------------------------------------------------------

    /** Welcher Tag eines Experiments heute ist — 1-basiert, nie über [Experiment.tage]. */
    fun tagNummer(experiment: Experiment, heute: LocalDate): Int {
        val verstrichen = (heute.toEpochDay() - experiment.begonnenAm.toEpochDay()).toInt()
        return (verstrichen + 1).coerceIn(1, experiment.tage)
    }

    suspend fun aufgabenHeute(experiment: Experiment, heute: LocalDate): List<Aufgabe> =
        db.aufgaben().desTages(experiment.id, tagNummer(experiment, heute))

    suspend fun hakenUmschalten(aufgabe: Aufgabe) {
        db.aufgaben().aktualisiere(
            aufgabe.copy(erledigtAm = if (aufgabe.erledigtAm == null) Instant.now() else null),
        )
    }

    // -----------------------------------------------------------------------
    // F-09 — Gespräch
    // -----------------------------------------------------------------------

    suspend fun gespraechFortsetzen(experimentId: Long, frage: String, heute: LocalDate): String {
        val experiment = db.experimente().lies(experimentId)
            ?: throw IllegalStateException("Das Experiment gibt es nicht mehr.")

        db.gespraeche().schreib(
            GespraechsRunde(
                experimentId = experimentId,
                sprecher = Sprecher.ICH,
                text = frage.trim(),
                angelegtAm = Instant.now(),
            ),
        )

        val roh = codex.frage(
            Aufgaben.gespraech(
                gedaechtnis = gedaechtnis(heute),
                experimentTitel = experiment.titel,
                experimentBeschreibung = experiment.beschreibung,
                faden = db.gespraeche().zumExperiment(experimentId).alsFaden(),
                frage = frage.trim(),
                model = modellExperimente(),
                effort = effortExperimente(),
            ),
        )
        val antwort = Antworten.gespraech(roh)
        db.gespraeche().schreib(
            GespraechsRunde(
                experimentId = experimentId,
                sprecher = Sprecher.KI,
                text = antwort,
                angelegtAm = Instant.now(),
            ),
        )
        return antwort
    }

    // -----------------------------------------------------------------------
    // F-10 / F-11 / F-13 — Auswertung und Abschluss
    // -----------------------------------------------------------------------

    /**
     * Speichert Franks eigene Worte, lässt die KI auswerten und schließt das Experiment ab,
     * wenn es sein letzter Tag war.
     *
     * [nichtUmgesetzt] löst beides aus (F-13): Chronik-Eintrag **und** zurück auf die Merkliste.
     */
    suspend fun auswerten(
        experiment: Experiment,
        heute: LocalDate,
        eigenerText: String,
        nichtUmgesetzt: Boolean = false,
    ): String {
        val tag = tagNummer(experiment, heute)
        val abschliessend = nichtUmgesetzt || tag >= experiment.tage

        val aufgaben = db.aufgaben().desTages(experiment.id, tag)
        val erledigt = aufgaben.count { it.erledigtAm != null }
        val hakenStand = buildString {
            append("$erledigt von ${aufgaben.size} erledigt\n")
            aufgaben.forEach {
                append(if (it.erledigtAm != null) "[x] " else "[ ] ").append(it.text).append('\n')
            }
        }

        // Franks eigene Worte werden IMMER gespeichert — auch wenn die KI danach scheitert.
        db.auswertungen().schreib(
            Auswertung(
                experimentId = experiment.id,
                datum = heute,
                eigenerText = eigenerText.trim(),
                abschliessend = abschliessend,
            ),
        )

        val roh = codex.frage(
            Aufgaben.auswertung(
                gedaechtnis = gedaechtnis(heute),
                experimentTitel = experiment.titel,
                experimentBeschreibung = experiment.beschreibung,
                tagNummer = tag,
                tageGesamt = experiment.tage,
                abschliessend = abschliessend,
                hakenStand = hakenStand,
                faden = db.gespraeche().zumExperiment(experiment.id).alsFaden(),
                verlauf = db.auswertungen().zumExperiment(experiment.id).alsVerlauf(),
                eigenerText = eigenerText.trim(),
                model = modellExperimente(),
                effort = effortExperimente(),
            ),
        )
        val kiText = Antworten.auswertung(roh)

        db.auswertungen().desTages(experiment.id, heute)?.let {
            db.auswertungen().aktualisiere(it.copy(kiText = kiText))
        }

        if (abschliessend) {
            abschliessen(experiment, heute, nichtUmgesetzt)
            erkenntnisseFortschreiben(kiText)
        }
        return kiText
    }

    private suspend fun abschliessen(experiment: Experiment, heute: LocalDate, nichtUmgesetzt: Boolean) {
        db.experimente().aktualisiere(
            experiment.copy(
                zustand = if (nichtUmgesetzt) ExperimentZustand.NICHT_UMGESETZT
                else ExperimentZustand.ABGESCHLOSSEN,
                beendetAm = heute,
            ),
        )
        if (!nichtUmgesetzt) return

        // F-13: zusätzlich zurück auf die Merkliste, mit dem Vermerk, was im Weg stand.
        val grund = db.auswertungen().desTages(experiment.id, heute)?.eigenerText
        val jeTag = db.aufgaben().desTages(experiment.id, 1).map { it.text }
        db.merkliste().schreib(
            MerkEintrag(
                titel = experiment.titel,
                beschreibung = experiment.beschreibung,
                tage = experiment.tage,
                stufe = experiment.stufe,
                aufgabenJson = Antworten.aufgabenZuJson(listOf(jeTag)),
                quelle = MerkQuelle.NICHT_UMGESETZT,
                vermerk = grund,
                angelegtAm = Instant.now(),
            ),
        )
    }

    // -----------------------------------------------------------------------
    // F-14 / F-15 / F-17 — Logbuch, Verdichtung, Erkenntnisse
    // -----------------------------------------------------------------------

    suspend fun logbuchFortschreiben(heute: LocalDate, neuesMaterial: String) {
        val bisher = db.log().lies(heute)?.ausfuehrlich.orEmpty()
        val roh = codex.frage(
            Aufgaben.logbuch(bisher, neuesMaterial, modellLogbuch(), effortLogbuch()),
        )
        db.log().schreib(LogTag(datum = heute, ausfuehrlich = Antworten.logbuch(roh)))
    }

    /**
     * F-15 — läuft beim ersten Öffnen an einem neuen Kalendertag, **bevor** sonst etwas
     * geschieht. Der ausführliche Text wird erst gelöscht, wenn der verdichtete vorliegt.
     */
    suspend fun verdichtenFaellige(heute: LocalDate) {
        val grenze = heute.minusDays(TAGE_AUSFUEHRLICH.toLong())
        db.log().zuVerdichten(grenze).forEach { tag ->
            val roh = codex.frage(
                Aufgaben.verdichtung(
                    tag.datum.toString(), tag.ausfuehrlich.orEmpty(),
                    modellLogbuch(), effortLogbuch(),
                ),
            )
            db.log().schreib(
                tag.copy(
                    ausfuehrlich = null,
                    verdichtet = Antworten.verdichtung(roh),
                    verdichtetAm = Instant.now(),
                ),
            )
        }
        einstellungen.letzteVerdichtung = heute.toString()
    }

    fun verdichtungFaellig(heute: LocalDate): Boolean =
        einstellungen.letzteVerdichtung != heute.toString()

    private suspend fun erkenntnisseFortschreiben(neueAuswertung: String) {
        val bisher = db.erkenntnisse().alle()
        val roh = codex.frage(
            Aufgaben.erkenntnisse(bisher.map { it.text }, neueAuswertung, modellLogbuch(), effortLogbuch()),
        )
        val neu = Antworten.erkenntnisse(roh)
        if (neu.isEmpty()) return
        // Die KI liefert die vollständige Liste zurück — sie ersetzt die alte.
        db.erkenntnisse().leere()
        neu.forEach { db.erkenntnisse().schreib(Erkenntnis(text = it, geaendertAm = Instant.now())) }
    }

    // -----------------------------------------------------------------------
    // F-16 — Logbuch bearbeiten
    // -----------------------------------------------------------------------

    suspend fun logtagSpeichern(tag: LogTag) = db.log().schreib(tag)
    suspend fun logtagLoeschen(tag: LogTag) = db.log().loesche(tag)

    // -----------------------------------------------------------------------
    // F-20 / F-21 — Ziele und Selbstbild
    // -----------------------------------------------------------------------

    suspend fun zielSpeichern(ziel: Ziel) = db.ziele().schreib(ziel)
    suspend fun zielLoeschen(ziel: Ziel) = db.ziele().loesche(ziel)

    suspend fun selbstbildSpeichern(text: String) {
        db.selbstbild().schreib(SelbstbildSatz(text = text, geaendertAm = Instant.now()))
    }

    companion object {
        /** Höchstens drei gleichzeitig offene Experimente (F-06). */
        const val MAX_OFFEN = 3

        /** So viele Tage bleibt ein Logbuch-Eintrag ausführlich (F-15). */
        const val TAGE_AUSFUEHRLICH = 15
    }
}
