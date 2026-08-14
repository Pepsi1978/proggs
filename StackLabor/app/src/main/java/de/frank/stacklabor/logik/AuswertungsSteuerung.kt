package de.frank.stacklabor.logik

import de.frank.stacklabor.codex.AuswertungsAnfrage
import de.frank.stacklabor.codex.Auswertungsbereich
import de.frank.stacklabor.codex.CodexAntwort
import de.frank.stacklabor.codex.CodexDienst
import de.frank.stacklabor.codex.CodexFehler
import de.frank.stacklabor.codex.CodexFehlerArt
import de.frank.stacklabor.codex.CodexModell
import de.frank.stacklabor.codex.Denkstufe
import de.frank.stacklabor.codex.FrageAngabe
import de.frank.stacklabor.codex.MittelAngabe
import de.frank.stacklabor.codex.ZielAngabe
import de.frank.stacklabor.daten.Einstellungen
import de.frank.stacklabor.daten.StackLaborDatenbank
import de.frank.stacklabor.daten.StackRepository
import de.frank.stacklabor.daten.entitaeten.Bereich
import de.frank.stacklabor.daten.entitaeten.EintragMitMittel
import de.frank.stacklabor.daten.entitaeten.FrageAntwortE
import de.frank.stacklabor.daten.entitaeten.KonkurrenzArt
import de.frank.stacklabor.daten.entitaeten.KonkurrenzE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Der Zustand eines Auswertungslaufs — das, was die Oberfläche anzeigen muss.
 */
sealed interface Laufzustand {
    /** Nichts läuft. */
    data object Ruhe : Laufzustand

    /** Ein Lauf läuft; [text] wächst wortweise (M-13). */
    data class Laeuft(val text: String = "") : Laufzustand

    /** Fertig — die Ampeln sind bereits neu gerechnet. */
    data object Fertig : Laufzustand

    /** Etwas ging schief; die zuletzt gültigen Ampeln bleiben sichtbar. */
    data class Fehler(val art: CodexFehlerArt, val meldung: String) : Laufzustand
}

/**
 * **Die Kernfunktion der App: F-12 und F-13.**
 *
 * Sie stellt die Anfrage zusammen, schickt sie an Codex, nimmt die dünn besetzte
 * Bewertungstabelle entgegen und speichert sie. Danach rechnet die App **alle** Ampeln
 * lokal daraus (F-14) — deshalb wirken Häkchen und Umsortieren anschließend sofort,
 * kostenlos und ohne Netz.
 *
 * Was hier NICHT passiert: Ampeln von der KI übernehmen. Die KI liefert Beiträge je
 * Mittel × Ziel; die Ampel ist eine Rechnung, keine Meinung.
 */
class AuswertungsSteuerung(
    private val bereich: CoroutineScope,
    private val db: StackLaborDatenbank,
    private val repository: StackRepository,
    private val dienst: CodexDienst,
    private val einstellungen: Einstellungen,
) {
    private val _zustand = MutableStateFlow<Map<String, Laufzustand>>(emptyMap())

    /** Je Stack-Kennung ein Zustand; der Schlüssel `TAG` steht für die Gesamtprüfung. */
    val zustand: StateFlow<Map<String, Laufzustand>> = _zustand.asStateFlow()

    private var laufenderJob: Job? = null

    fun zustandVon(schluessel: String): Laufzustand = _zustand.value[schluessel] ?: Laufzustand.Ruhe

    private fun setze(schluessel: String, z: Laufzustand) {
        _zustand.value = _zustand.value.toMutableMap().apply { put(schluessel, z) }
    }

    /** Bricht einen laufenden Lauf ab. Der vorherige Stand bleibt unangetastet. */
    fun brichAb() {
        dienst.brichAb()
        laufenderJob?.cancel()
        laufenderJob = null
        _zustand.value = _zustand.value.mapValues { (_, z) ->
            if (z is Laufzustand.Laeuft) Laufzustand.Ruhe else z
        }
    }

    // ── F-12: einen Stack auswerten ──────────────────────────────────────────────

    fun werteStackAus(stackId: String) {
        if (zustandVon(stackId) is Laufzustand.Laeuft) {
            brichAb()
            return
        }
        laufenderJob = bereich.launch {
            setze(stackId, Laufzustand.Laeuft())
            val ergebnis = runCatching {
                val anfrage = baueAnfrage(stackId)
                val modell = CodexModell.ausWert(einstellungen.codexModell.first())
                val stufe = Denkstufe.ausWert(einstellungen.codexDenkstufe.first())
                dienst.werteStackAus(anfrage, modell, stufe) { stueck ->
                    val bisher = (zustandVon(stackId) as? Laufzustand.Laeuft)?.text.orEmpty()
                    setze(stackId, Laufzustand.Laeuft(bisher + stueck))
                }.getOrThrow() to (modell to stufe)
            }

            ergebnis.onSuccess { (antwort, wahl) ->
                speichere(stackId, Bereich.STACK, antwort, wahl.first, wahl.second)
                setze(stackId, Laufzustand.Fertig)
            }.onFailure { f ->
                setze(stackId, alsFehler(f))
            }
        }
    }

    // ── F-13: alle Stacks zusammen ───────────────────────────────────────────────

    fun pruefeAlleStacks() {
        val schluessel = "TAG"
        if (zustandVon(schluessel) is Laufzustand.Laeuft) {
            brichAb()
            return
        }
        laufenderJob = bereich.launch {
            setze(schluessel, Laufzustand.Laeuft())
            val ergebnis = runCatching {
                val anfrage = baueTagesAnfrage()
                val modell = CodexModell.ausWert(einstellungen.codexModell.first())
                val stufe = Denkstufe.ausWert(einstellungen.codexDenkstufe.first())
                dienst.werteStackAus(anfrage, modell, stufe) { stueck ->
                    val bisher = (zustandVon(schluessel) as? Laufzustand.Laeuft)?.text.orEmpty()
                    setze(schluessel, Laufzustand.Laeuft(bisher + stueck))
                }.getOrThrow() to (modell to stufe)
            }
            ergebnis.onSuccess { (antwort, wahl) ->
                speichere(null, Bereich.TAG, antwort, wahl.first, wahl.second)
                setze(schluessel, Laufzustand.Fertig)
            }.onFailure { f -> setze(schluessel, alsFehler(f)) }
        }
    }

    // ── F-02: Konkurrenzprüfung für frisch hinzugefügte Mittel ───────────────────

    /**
     * Läuft **nach** dem Hinzufügen, nie davor — das Eintippen bleibt flüssig.
     * Das Ergebnis wird am Eintrag gespeichert (`offenerHinweis`), damit es nicht verlorengeht,
     * wenn der Stack zwischenzeitlich verlassen wird.
     */
    fun pruefeNeueMittel(stackId: String, neueMittelIds: List<String>) {
        if (neueMittelIds.isEmpty()) return
        bereich.launch {
            runCatching {
                val anfrage = baueAnfrage(stackId)
                val modell = CodexModell.ausWert(einstellungen.codexModell.first())
                val stufe = Denkstufe.ausWert(einstellungen.codexDenkstufe.first())
                dienst.pruefeNeuesMittel(anfrage, neueMittelIds, modell, stufe).getOrThrow()
            }.onSuccess { antwort ->
                val eintraege = db.stackDao().eintraegeMitMittel(stackId).first()
                for (mittelId in neueMittelIds) {
                    val eintrag = eintraege.firstOrNull { it.eintrag.mittelId == mittelId } ?: continue
                    db.stackDao().setzeOffenenHinweis(eintrag.eintrag.id, hinweisText(antwort, mittelId))
                }
            }
        }
    }

    /** Der Satz, der im Schnipsel über dem Sockel steht. */
    private fun hinweisText(antwort: CodexAntwort, mittelId: String): String {
        val stuetzt = antwort.zellen.filter { it.nem == mittelId && it.wirkung.name.equals("STUETZT", true) }
        val stoert = antwort.zellen.filter { it.nem == mittelId && it.wirkung.name.equals("STOERT", true) }
        return buildString {
            if (stuetzt.isNotEmpty()) append("stützt ${stuetzt.size} Ziel(e)")
            if (stoert.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append("stört: ")
                append(stoert.joinToString("; ") { it.grund })
            }
            if (isEmpty()) append("Keine Konkurrenz gefunden — passt in den Stack.")
        }
    }

    // ── Anfragen zusammenstellen ─────────────────────────────────────────────────

    /**
     * Was mitgeschickt wird (F-12): Zeitpunkt und Einnahme-Hinweis des Stacks, je **aktivem**
     * Mittel die vollständigen Angaben, je Ziel Rang und Text, dazu die eigenen Fragen.
     * **Deaktivierte Mittel gehen nicht mit** — sie sind für diesen Stack abgewählt.
     */
    private suspend fun baueAnfrage(stackId: String): AuswertungsAnfrage {
        val stack = db.stackDao().stackEinmalig(stackId)
        val eintraege = db.stackDao().eintraegeMitMittel(stackId).first().filter { it.eintrag.aktiv }
        val ziele = db.zielDao().zieleDesStacksEinmalig(stackId)
        val fragen = db.frageDao().fragenDesStacksEinmalig(stackId)
        val variante = einstellungen.dosisVariante.first()
        val namen = eintraege.associate { it.eintrag.mittelId to it.mittel.name }

        return AuswertungsAnfrage(
            stackName = stack?.name.orEmpty(),
            zeitpunkt = stack?.zeitpunkt.orEmpty(),
            einnahmeHinweis = stack?.einnahmeHinweis.orEmpty(),
            mittel = eintraege.map { alsAngabe(it, variante, namen) },
            ziele = ziele.map { ZielAngabe(it.ziel.id, it.rang, it.ziel.text) },
            fragen = fragen.map { FrageAngabe(it.id, it.text) },
            bereich = Auswertungsbereich.STACK,
        )
    }

    /**
     * Für F-13 wird zusätzlich die **Tagesgesamtdosis je Wirkstoff** vorab gerechnet und
     * mitgeschickt — sie ist der Grund, warum diese Prüfung überhaupt existiert:
     * 3 × 155 mg Magnesium fallen in keinem einzelnen Stack auf.
     */
    private suspend fun baueTagesAnfrage(): AuswertungsAnfrage {
        val alleEintraege = db.stackDao().alleAktivenEintraegeMitMittel().first()
        val stacks = db.stackDao().alleStacks().first().associateBy { it.id }
        val variante = einstellungen.dosisVariante.first()
        val namen = alleEintraege.associate { it.eintrag.mittelId to it.mittel.name }

        val jeMittel = alleEintraege.groupBy { it.eintrag.mittelId }
        val mittelAngaben = jeMittel.map { (mittelId, liste) ->
            val erstes = liste.first()
            val summe = liste.sumOf { gesamtMenge(it, variante) }
            alsAngabe(erstes, variante, namen).copy(
                tagesgesamtdosis = "${zahl(summe)} ${einheitKurz(erstes)}",
                stacks = liste.mapNotNull { stacks[it.eintrag.stackId]?.name },
            )
        }

        // Für den Tageslauf gelten alle Ziele, die irgendwo aktiv sind.
        val alleZiele = db.zielDao().alleZuordnungen().first()
        val zielTexte = db.zielDao().alleZiele().first().associate { it.id to it.text }
        val zieleGesamt = alleZiele
            .groupBy { it.zielId }
            .map { (zielId, zuordnungen) ->
                ZielAngabe(zielId, zuordnungen.minOf { it.rang }, zielTexte[zielId].orEmpty())
            }
            .sortedBy { it.rang }

        val alleFragen = db.frageDao().alleFragenEinmalig()

        return AuswertungsAnfrage(
            stackName = "Alle Stacks zusammen",
            zeitpunkt = "über den ganzen Tag",
            einnahmeHinweis = "",
            mittel = mittelAngaben,
            ziele = zieleGesamt,
            fragen = alleFragen.map { FrageAngabe(it.id, it.text) },
            bereich = Auswertungsbereich.TAG,
        )
    }

    private fun alsAngabe(
        em: EintragMitMittel,
        variante: String,
        namen: Map<String, String>,
    ): MittelAngabe = MittelAngabe(
        id = em.eintrag.mittelId,
        name = em.mittel.name,
        gesamtdosis = "${zahl(gesamtMenge(em, variante))} ${einheitKurz(em)}",
        darreichungsform = em.mittel.darreichungsform.name.lowercase(),
        loeslichkeit = when (em.mittel.loeslichkeit.name) {
            "WASSER" -> "wasserlöslich"
            "FETT" -> "fettlöslich"
            else -> "wasser- und fettlöslich"
        },
        frequenz = if (em.eintrag.frequenzTage <= 1) "täglich" else "alle ${em.eintrag.frequenzTage} Tage",
        // Ausdrücklich mitgeschickt, damit die KI alternierende Paare NICHT als Konkurrenz
        // meldet — sonst gäbe es bei Pterostilben/Trans-Resveratrol täglich Fehlalarm.
        alterniertMit = em.eintrag.alterniertMit.split(',')
            .filter { it.isNotBlank() }
            .map { namen[it] ?: it },
        hersteller = em.mittel.hersteller,
        durchfallrisiko = em.mittel.durchfallrisiko,
        zusatztext = listOf(em.eintrag.zusatztext, em.mittel.beistoffe)
            .filter { it.isNotBlank() }.joinToString(" · "),
        gruppe = em.eintrag.gruppeId.orEmpty(),
    )

    private fun gesamtMenge(em: EintragMitMittel, variante: String): Double {
        val menge = if (variante == "B" && em.eintrag.dosisVarianteB != null) em.eintrag.dosisVarianteB
        else em.eintrag.mengeJeStueck
        return (menge ?: 0.0) * em.eintrag.stueckzahl
    }

    private fun einheitKurz(em: EintragMitMittel): String = when (em.eintrag.einheit.name) {
        "MG" -> "mg"; "UG" -> "µg"; "G" -> "g"; "ML" -> "ml"; "IE" -> "IE"; else -> "Stück"
    }

    private fun zahl(w: Double): String =
        if (w % 1.0 == 0.0) w.toInt().toString() else w.toString().replace('.', ',')

    // ── Ergebnis speichern ───────────────────────────────────────────────────────

    private suspend fun speichere(
        stackId: String?,
        bereichArt: Bereich,
        antwort: CodexAntwort,
        modell: CodexModell,
        stufe: Denkstufe,
    ) {
        val pruefsumme = if (stackId != null) repository.pruefsummeFuer(stackId) else ""
        repository.speichereBewertung(
            stackId = stackId,
            bereich = bereichArt,
            modell = modell.bezeichnung,
            denkstufe = stufe.bezeichnung,
            pruefsumme = pruefsumme,
            gesamt = antwort.gesamt,
            hinweise = antwort.hinweise,
            zellen = antwort.zellen.map {
                Zelle(
                    mittelId = it.nem,
                    zielId = it.ziel,
                    wirkung = if (it.wirkung.name.equals("STUETZT", true)) Wirkung.STUETZT else Wirkung.STOERT,
                    staerke = it.staerke.coerceIn(1, 3),
                    grund = it.grund,
                )
            },
            konkurrenzen = antwort.konkurrenzen.map {
                KonkurrenzE(
                    bewertungId = "",   // wird beim Speichern gesetzt
                    mittelA = it.mittelA,
                    mittelB = it.mittelB,
                    art = when (it.art.name.uppercase()) {
                        "AUFNAHME" -> KonkurrenzArt.AUFNAHME
                        "ZEITPUNKT" -> KonkurrenzArt.ZEITPUNKT
                        else -> KonkurrenzArt.WIRKUNG
                    },
                    schwere = it.schwere.coerceIn(1, 3),
                    grund = it.grund,
                )
            },
            antworten = antwort.antworten.map {
                FrageAntwortE(bewertungId = "", frageId = it.frage, text = it.text)
            },
        )
    }

    private fun alsFehler(f: Throwable): Laufzustand.Fehler = when (f) {
        is CodexFehler -> Laufzustand.Fehler(
            f.art,
            when (f.art) {
                CodexFehlerArt.NEU_ANMELDEN -> "Anmeldung abgelaufen."
                CodexFehlerArt.KONTINGENT -> "Kontingent erschöpft. " + (f.message ?: "")
                CodexFehlerArt.NETZ -> "Keine Verbindung."
            },
        )
        else -> Laufzustand.Fehler(CodexFehlerArt.NETZ, f.message ?: "Unbekannter Fehler")
    }
}
