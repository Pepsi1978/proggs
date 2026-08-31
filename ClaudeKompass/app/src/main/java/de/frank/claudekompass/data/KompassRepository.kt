package de.frank.claudekompass.data

import android.content.Context
import de.frank.claudekompass.data.local.AktualisierungEntity
import de.frank.claudekompass.data.local.ChatNachrichtEntity
import de.frank.claudekompass.data.local.ChatSitzungEntity
import de.frank.claudekompass.data.local.EintragEntity
import de.frank.claudekompass.data.local.ErklaerungHistorieEntity
import de.frank.claudekompass.data.local.FrageEntity
import de.frank.claudekompass.data.local.KompassDatabase
import de.frank.claudekompass.data.local.SuchTreffer
import de.frank.claudekompass.data.local.SucheFtsEntity
import de.frank.claudekompass.data.local.SuchVerlaufEntity
import de.frank.claudekompass.data.local.baueSuchAnfrage
import de.frank.claudekompass.data.local.normalisiereFuerSuche
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.observability.probe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Die eine Stelle, an der Daten gelesen und geschrieben werden.
 *
 * Die Oberfläche fasst die Datenbank nie direkt an. Das hält die Regeln an einem Ort — etwa
 * die, dass bei jeder Änderung an einem Eintrag auch der Suchindex nachgezogen wird. Vergäße
 * man das an einer Stelle, würde die Suche still veraltete Ergebnisse liefern.
 */
class KompassRepository(context: Context) {

    private val appContext = context.applicationContext
    private val datenbank = KompassDatabase.hole(context)
    private val eintraege = datenbank.eintragDao()
    private val erklaerungen = datenbank.erklaerungDao()
    private val fragen = datenbank.frageDao()
    private val chat = datenbank.chatDao()
    private val laeufe = datenbank.aktualisierungDao()
    private val suche = datenbank.sucheDao()

    // --- Erstbefüllung ---------------------------------------------------------------------

    /**
     * Füllt die Datenbank beim ersten Start aus den Beigaben.
     *
     * Läuft nur, wenn noch nichts da ist. Ein späterer Aufruf darf die eigenen Ergänzungen
     * — ausführlichere Erklärungen, gestellte Fragen — auf keinen Fall überschreiben.
     */
    suspend fun befuelleWennLeer(context: Context): Boolean {
        if (eintraege.anzahl() > 0) return false
        val roh = SeedLader.ladeAlles(context)
        if (roh.isEmpty()) {
            KompassLog.error("Repository", "befuelleWennLeer", "Keine Wissensbasis gefunden — die App bliebe leer")
            return false
        }
        eintraege.setze(roh.map { it.zuEntity() })
        indiziereEintraege(roh.map { it.zuEntity() })
        probe(
            eintraege.anzahl() == roh.size,
            "Nicht alle Einträge sind in der Datenbank angekommen",
            "Repository",
            "befuelleWennLeer",
            mapOf("erwartet" to roh.size, "vorhanden" to eintraege.anzahl()),
        )
        KompassLog.info("Repository", "befuelleWennLeer", "Wissensbasis eingespielt", mapOf("eintraege" to roh.size))
        return true
    }

    // --- Einträge -------------------------------------------------------------------------

    fun beobachteAktive(bereich: Bereich): Flow<List<EintragEntity>> =
        eintraege.beobachteAktive(bereich.id)

    fun beobachteEntfernte(bereich: Bereich): Flow<List<EintragEntity>> =
        eintraege.beobachteEntfernte(bereich.id)

    suspend fun ladeEintrag(id: String): EintragEntity? = eintraege.lade(id)

    suspend fun ladeAlle(bereich: Bereich): List<EintragEntity> = eintraege.ladeAlle(bereich.id)

    suspend fun ladeKomplett(): List<EintragEntity> = eintraege.ladeKomplett()

    /** Alle Einträge, deren deutsche Erklärung noch aussteht. */
    suspend fun ladeUnerklaerte(): List<EintragEntity> = eintraege.ladeUnerklaerte()

    suspend fun anzahlUnerklaerte(): Int = eintraege.anzahlUnerklaerte()

    /** Schreibt einen einzelnen Eintrag zurueck und zieht den Suchindex mit. */
    suspend fun sichereEintrag(eintrag: EintragEntity) {
        eintraege.aktualisiere(eintrag)
        indiziereEintraege(listOf(eintrag))
    }

    /**
     * Löscht Einträge endgültig und räumt den Suchindex mit auf.
     *
     * Gedacht für Namen, die ein früherer, fehlerhafter Lauf erfunden hat.
     */
    suspend fun loescheEintraege(ids: List<String>) {
        if (ids.isEmpty()) return
        eintraege.loesche(ids)
        ids.forEach { suche.entferne(it, ART_EINTRAG) }
        KompassLog.info("Repository", "loescheEintraege", "Einträge entfernt", mapOf("anzahl" to ids.size))
    }

    /**
     * Die Kennungen aus der mitgelieferten Wissensbasis.
     *
     * Sie beantworten die Frage „Stand dieser Eintrag schon in der Auslieferung?". Nur wer das
     * weiss, kann einen erfundenen Eintrag aus einem früheren Lauf von einem echten
     * unterscheiden, der aus Claude Code entfernt wurde.
     */
    fun seedKennungen(): Set<String> = zwischengespeicherteSeedKennungen ?: run {
        val kennungen = SeedLader.ladeAlles(appContext).map { it.id }.toSet()
        zwischengespeicherteSeedKennungen = kennungen
        kennungen
    }

    @Volatile
    private var zwischengespeicherteSeedKennungen: Set<String>? = null

    /**
     * Ersetzt die Erklärung durch eine ausführlichere und hebt die alte für den Zurück-Pfeil auf.
     *
     * Die Reihenfolge ist wichtig: erst sichern, dann ersetzen. Andersherum wäre die kurze
     * Fassung im Fehlerfall unwiederbringlich weg.
     */
    suspend fun vertiefeErklaerung(id: String, neueErklaerung: String): Boolean {
        val eintrag = eintraege.lade(id) ?: return false
        erklaerungen.sichere(
            ErklaerungHistorieEntity(
                eintragId = id,
                stufe = eintrag.stufe,
                text = eintrag.erklaerung,
            ),
        )
        val neu = eintrag.copy(
            erklaerung = neueErklaerung,
            stufe = eintrag.stufe + 1,
            zuletztGeaendert = System.currentTimeMillis(),
        )
        eintraege.aktualisiere(neu)
        indiziereEintraege(listOf(neu))
        KompassLog.info(
            "Repository",
            "vertiefeErklaerung",
            "Erklärung vertieft",
            mapOf("id" to id, "neueStufe" to neu.stufe, "zeichen" to neueErklaerung.length),
        )
        return true
    }

    /** Holt die vorherige Fassung zurück. Liefert false, wenn es keine gibt. */
    suspend fun machErklaerungRueckgaengig(id: String): Boolean {
        val eintrag = eintraege.lade(id) ?: return false
        val vorherige = erklaerungen.letzte(id) ?: return false
        val neu = eintrag.copy(
            erklaerung = vorherige.text,
            stufe = vorherige.stufe,
            zuletztGeaendert = System.currentTimeMillis(),
        )
        eintraege.aktualisiere(neu)
        erklaerungen.loesche(vorherige.id)
        indiziereEintraege(listOf(neu))
        KompassLog.info(
            "Repository",
            "machErklaerungRueckgaengig",
            "Frühere Erklärung wiederhergestellt",
            mapOf("id" to id, "stufe" to vorherige.stufe),
        )
        return true
    }

    fun beobachteHistorieAnzahlen(): Flow<Map<String, Int>> =
        erklaerungen.beobachteAlleAnzahlen().map { liste ->
            liste.associate { it.eintragId to it.anzahl }
        }

    // --- Fragen ---------------------------------------------------------------------------

    fun beobachteFragen(eintragId: String): Flow<List<FrageEntity>> = fragen.beobachte(eintragId)

    fun beobachteAlleFragen(): Flow<List<FrageEntity>> = fragen.beobachteAlle()

    suspend fun starteFrage(eintragId: String, frage: String): Long {
        val id = fragen.fuegeEin(FrageEntity(eintragId = eintragId, frage = frage, antwort = "", laeuft = true))
        KompassLog.info("Repository", "starteFrage", "Frage gestellt", mapOf("eintrag" to eintragId, "id" to id))
        return id
    }

    suspend fun beendeFrage(id: Long, antwort: String, fehler: String = "") {
        val vorhandene = fragen.lade(id) ?: return
        val fertig = vorhandene.copy(antwort = antwort, laeuft = false, fehler = fehler)
        fragen.aktualisiere(fertig)
        indiziereFrage(fertig)
    }

    suspend fun aktualisiereFrageText(id: Long, text: String) {
        val vorhandene = fragen.lade(id) ?: return
        fragen.aktualisiere(vorhandene.copy(antwort = text))
    }

    suspend fun loescheFrage(id: Long) {
        fragen.loesche(id)
        suche.entferne(id.toString(), ART_FRAGE)
    }

    // --- Gespräche ------------------------------------------------------------------------

    fun beobachteSitzungen(): Flow<List<ChatSitzungEntity>> = chat.beobachteSitzungen()

    fun beobachteNachrichten(sitzungId: Long): Flow<List<ChatNachrichtEntity>> =
        chat.beobachteNachrichten(sitzungId)

    suspend fun ladeNachrichten(sitzungId: Long): List<ChatNachrichtEntity> =
        chat.ladeNachrichten(sitzungId)

    suspend fun legeSitzung(titel: String): Long = chat.lege(ChatSitzungEntity(titel = titel))

    suspend fun benenneSitzungUm(id: Long, titel: String) {
        val sitzung = chat.ladeSitzung(id) ?: return
        chat.aktualisiere(sitzung.copy(titel = titel))
    }

    suspend fun loescheSitzung(id: Long) {
        chat.loescheSitzung(id)
        // Die Nachrichten verschwinden über die Fremdschlüssel-Regel mit. Der Suchindex kennt
        // diese Regel nicht — er wird deshalb hier von Hand nachgezogen.
        neuIndiziereChat()
    }

    suspend fun fuegeNachrichtEin(sitzungId: Long, rolle: String, text: String): Long {
        val id = chat.fuegeEin(ChatNachrichtEntity(sitzungId = sitzungId, rolle = rolle, text = text))
        chat.beruehre(sitzungId)
        return id
    }

    suspend fun aktualisiereNachricht(id: Long, text: String, fehler: String = "") {
        val nachricht = chat.ladeNachricht(id) ?: return
        val neu = nachricht.copy(text = text, fehler = fehler)
        chat.aktualisiere(neu)
        indiziereNachricht(neu)
    }

    // --- Aktualisierungsläufe ---------------------------------------------------------------

    fun beobachteLetztenErfolg(): Flow<AktualisierungEntity?> = laeufe.beobachteLetztenErfolg()

    fun beobachteVerlauf(): Flow<List<AktualisierungEntity>> = laeufe.beobachteVerlauf()

    suspend fun starteLauf(): Long = laeufe.starte(AktualisierungEntity())

    suspend fun ladeLauf(id: Long): AktualisierungEntity? = laeufe.lade(id)

    suspend fun beendeLauf(lauf: AktualisierungEntity) = laeufe.aktualisiere(lauf)

    /**
     * Spielt das Ergebnis eines Laufs ein.
     *
     * Neue Einträge bekommen die Lauf-Nummer und werden dadurch farblich hervorgehoben. Alle
     * Markierungen früherer Läufe fallen weg — was beim letzten Mal neu war, gehört jetzt zum
     * Bestand. Genau so war es gewünscht.
     */
    suspend fun spieleLaufEin(laufId: Long, neue: List<RohEintrag>, geaenderte: List<EintragEntity>) {
        if (neue.isNotEmpty()) eintraege.setze(neue.map { it.zuEntity(neuImLauf = laufId) })
        geaenderte.forEach { eintraege.aktualisiere(it) }
        eintraege.entferneAlteNeuMarkierungen(laufId)
        indiziereEintraege(neue.map { it.zuEntity(laufId) } + geaenderte)
    }

    /**
     * Spielt die frisch gefundenen Einträge ein, noch bevor sie erklärt sind.
     *
     * Das ist die entscheidende Reihenfolge: Erst sind die Namen sicher in der Datenbank, dann
     * werden die Erklärungen einzeln nachgezogen. Vorher lag alles bis zum Ende des Laufs im
     * Arbeitsspeicher — ein Abbruch oder ein Netzfehler nach zweihundert Erklärungen warf jede
     * einzelne davon weg, und der nächste Lauf fing wieder bei null an.
     */
    suspend fun spieleNeueEin(laufId: Long, neue: List<RohEintrag>) {
        if (neue.isEmpty()) return
        val entitaeten = neue.map { it.zuEntity(neuImLauf = laufId) }
        eintraege.setze(entitaeten)
        indiziereEintraege(entitaeten)
    }

    /** Nimmt die Hervorhebung von allem, was nicht aus dem laufenden Durchgang stammt. */
    suspend fun raeumeNeuMarkierungen(laufId: Long) = eintraege.entferneAlteNeuMarkierungen(laufId)

    // --- Suche ----------------------------------------------------------------------------

    /**
     * Sucht über alle Inhalte.
     *
     * Eine leere Anfrage liefert bewusst eine leere Liste, statt eine ungültige Suchanfrage an
     * die Datenbank zu schicken — die würde eine Ausnahme werfen.
     */
    suspend fun suche(eingabe: String): List<SuchTreffer> {
        val anfrage = baueSuchAnfrage(eingabe)
        if (anfrage.isBlank()) return emptyList()
        return runCatching { suche.suche(anfrage) }.getOrElse { fehler ->
            KompassLog.warn("Repository", "suche", "Suche fehlgeschlagen", mapOf("grund" to fehler.message))
            emptyList()
        }
    }

    fun beobachteSuchVerlauf(): Flow<List<SuchVerlaufEntity>> = suche.beobachteVerlauf()

    suspend fun merkeSuchAnfrage(anfrage: String) {
        val sauber = anfrage.trim()
        if (sauber.length < 2) return
        suche.merkeAnfrage(SuchVerlaufEntity(anfrage = sauber))
        suche.kuerzeVerlauf()
    }

    suspend fun leereSuchVerlauf() = suche.leereVerlauf()

    suspend fun loescheSuchAnfrage(anfrage: String) = suche.loescheAnfrage(anfrage)

    /** Baut den Index vollständig neu — der Notausgang, wenn er nicht mehr stimmt. */
    suspend fun baueSuchIndexNeu() {
        suche.leereArt(ART_EINTRAG)
        suche.leereArt(ART_FRAGE)
        suche.leereArt(ART_CHAT)
        indiziereEintraege(eintraege.ladeKomplett())
        neuIndiziereChat()
        KompassLog.info("Repository", "baueSuchIndexNeu", "Suchindex neu aufgebaut", mapOf("eintraege" to suche.anzahl()))
    }

    private suspend fun indiziereEintraege(liste: List<EintragEntity>) {
        if (liste.isEmpty()) return
        suche.indiziere(
            liste.map { eintrag ->
                SucheFtsEntity(
                    quelleId = eintrag.id,
                    quelleArt = ART_EINTRAG,
                    bereich = eintrag.bereich,
                    titel = eintrag.name,
                    suchtext = normalisiereFuerSuche(
                        listOf(
                            eintrag.name,
                            eintrag.kurz,
                            eintrag.erklaerung,
                            eintrag.kategorie,
                            eintrag.quelleEnglisch,
                            eintrag.ersatz,
                        ).joinToString(" "),
                    ),
                )
            },
        )
    }

    private suspend fun indiziereFrage(frage: FrageEntity) {
        suche.indiziere(
            listOf(
                SucheFtsEntity(
                    quelleId = frage.id.toString(),
                    quelleArt = ART_FRAGE,
                    bereich = frage.eintragId.substringBefore(':'),
                    titel = frage.frage,
                    suchtext = normalisiereFuerSuche("${frage.frage} ${frage.antwort}"),
                ),
            ),
        )
    }

    private suspend fun indiziereNachricht(nachricht: ChatNachrichtEntity) {
        suche.indiziere(
            listOf(
                SucheFtsEntity(
                    quelleId = nachricht.id.toString(),
                    quelleArt = ART_CHAT,
                    bereich = Bereich.CHAT.id,
                    titel = nachricht.text.take(80),
                    suchtext = normalisiereFuerSuche(nachricht.text),
                ),
            ),
        )
    }

    /**
     * Baut den Chat-Teil des Index neu auf.
     *
     * Nötig, weil das Löschen eines Gespräches seine Nachrichten über die Fremdschlüssel-Regel
     * mitnimmt — davon erfährt der Suchindex nichts. Ohne diesen Neuaufbau blieben Treffer
     * stehen, die ins Leere führen.
     */
    private suspend fun neuIndiziereChat() {
        suche.leereArt(ART_CHAT)
        val nachrichten = chat.ladeAlleNachrichten()
        if (nachrichten.isEmpty()) return
        suche.indiziere(
            nachrichten.map { nachricht ->
                SucheFtsEntity(
                    quelleId = nachricht.id.toString(),
                    quelleArt = ART_CHAT,
                    bereich = Bereich.CHAT.id,
                    titel = nachricht.text.take(80),
                    suchtext = normalisiereFuerSuche(nachricht.text),
                )
            },
        )
    }

    companion object {
        const val ART_EINTRAG = "eintrag"
        const val ART_FRAGE = "frage"
        const val ART_CHAT = "chat"
    }
}
