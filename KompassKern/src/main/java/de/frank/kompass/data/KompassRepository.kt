package de.frank.kompass.data

import android.content.Context
import kotlinx.coroutines.flow.first
import de.frank.kompass.data.local.AktualisierungEntity
import de.frank.kompass.data.local.ChatNachrichtEntity
import de.frank.kompass.data.local.ChatSitzungEntity
import de.frank.kompass.data.local.EintragEntity
import de.frank.kompass.data.local.ErklaerungHistorieEntity
import de.frank.kompass.data.local.FrageEntity
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.data.local.SuchTreffer
import de.frank.kompass.data.local.SucheFtsEntity
import de.frank.kompass.data.local.SuchVerlaufEntity
import de.frank.kompass.data.local.baueSuchAnfrage
import de.frank.kompass.data.local.normalisiereFuerSuche
import de.frank.kompass.data.model.Bereich
import de.frank.kompass.observability.KompassLog
import de.frank.kompass.observability.probe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Was ein Einspielen angelegt hat — die Grundlage fürs Zurücknehmen.
 *
 * Weil das Einspielen nur ergänzt und nie überschreibt, reicht diese Liste aus, um es
 * vollständig rückgängig zu machen: Was vorher dastand, wurde nie angefasst.
 */
class Einspielspur {
    val neueEintraege = mutableSetOf<String>()
    val gefuellteErklaerungen = mutableSetOf<String>()
    val neueFragen = mutableListOf<Long>()
    val neueSitzungen = mutableListOf<Long>()

    val leer: Boolean
        get() = neueEintraege.isEmpty() && gefuellteErklaerungen.isEmpty() &&
            neueFragen.isEmpty() && neueSitzungen.isEmpty()
}

/** Was beim Einspielen einer Sicherung dazukam und was schon da war. */
data class EinspielBericht(
    val erklaerungen: Int,
    val eintraege: Int,
    val fragen: Int,
    val gespraeche: Int,
    val uebersprungen: Int,
) {
    fun alsText(): String {
        val teile = buildList {
            if (erklaerungen > 0) add("$erklaerungen Erklärungen")
            if (eintraege > 0) add("$eintraege Einträge")
            if (fragen > 0) add("$fragen Fragen")
            if (gespraeche > 0) add("$gespraeche Gespräche")
        }
        val kern = if (teile.isEmpty()) {
            "Sicherung eingespielt: Es war schon alles da."
        } else {
            "Sicherung eingespielt: " + teile.joinToString(", ") + " neu."
        }
        return if (uebersprungen > 0) "$kern $uebersprungen schon vorhanden oder übersprungen." else kern
    }

    /** Dieselbe Bilanz in kurz — für die Zeile neben dem Zurück-Pfeil. */
    fun alsKurztext(): String = buildList {
        if (eintraege > 0) add("$eintraege Einträge")
        if (erklaerungen > 0) add("$erklaerungen Erklärungen")
        if (fragen > 0) add("$fragen Fragen")
        if (gespraeche > 0) add("$gespraeche Gespräche")
    }.joinToString(", ").ifEmpty { "nichts" }
}

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
        if (eintraege.anzahl() > 0) {
            ergaenzeFehlendeAusSeed(context)
            raeumeSuchIndexAuf()
            return false
        }
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

    /**
     * Nach einem App-Update: Einträge, die der neue Seed kennt, die Datenbank aber nicht,
     * kommen dazu. Vorhandene Einträge bleiben unberührt — eigene Erklärungen gehen vor.
     */
    private suspend fun ergaenzeFehlendeAusSeed(context: Context) {
        val vorhanden = eintraege.ladeKomplett().map { it.id }.toSet()
        val fehlend = SeedLader.ladeAlles(context).filter { it.id !in vorhanden }
        if (fehlend.isEmpty()) return
        val entitaeten = fehlend.map { it.zuEntity() }
        eintraege.setze(entitaeten)
        indiziereEintraege(entitaeten)
        KompassLog.info("Repository", "ergaenzeFehlendeAusSeed", "Seed-Einträge ergänzt", mapOf("anzahl" to fehlend.size))
    }

    /** Ältere Fassungen haben Indexzeilen verdoppelt; einmal neu aufbauen, wenn das vorliegt. */
    private suspend fun raeumeSuchIndexAuf() {
        if (suche.anzahlDoppelte() > 0) baueSuchIndexNeu()
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
        beiAenderung("Erklärung geändert")
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
     * unterscheiden, der aus Codex CLI entfernt wurde.
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
        // Erst die fertige Antwort meldet sich, nicht jedes Teilstück beim Schreiben.
        if (fehler.isBlank()) beiAenderung("Frage beantwortet")
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
        val nachricht = ChatNachrichtEntity(sitzungId = sitzungId, rolle = rolle, text = text)
        val id = chat.fuegeEin(nachricht)
        chat.beruehre(sitzungId)
        if (text.isNotBlank()) indiziereNachricht(nachricht.copy(id = id))
        beiAenderung("Neue Nachricht")
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
        return runCatching {
            suche.suche(anfrage).distinctBy { it.quelleArt to it.quelleId }
        }.getOrElse { fehler ->
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
        suche.ersetze(
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
        suche.ersetze(
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
        suche.ersetze(
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

    suspend fun ladeFrage(id: Long): FrageEntity? = fragen.lade(id)

    suspend fun ladeNachricht(id: Long): ChatNachrichtEntity? = chat.ladeNachricht(id)

    // --- Sicherung: Quelle und Senke ---------------------------------------------------

    /**
     * Wird gerufen, wenn etwas Eigenes entstanden ist — daran hängt die selbsttätige Sicherung.
     *
     * Als Haken statt als feste Abhängigkeit: Das Repository soll nichts über Ordner, Dateien
     * und Sicherungen wissen müssen. Wer nicht sichert, setzt ihn nicht.
     */
    var beiAenderung: (String) -> Unit = {}

    // Die Senke des Einspielens meldet sich hier bewusst NICHT: Eine selbsttätige Sicherung
    // gleich nach dem Einspielen würde genau die Datei überschreiben, aus der eben gelesen
    // wurde — und mit ihr die Möglichkeit, das Einspielen zurückzunehmen.

    /** Woher die Sätze beim Sichern kommen — seitenweise, nie der ganze Bestand auf einmal. */
    val sicherungsQuelle = object : Sicherung.Quelle {
        override suspend fun eintraegeSeite(bereiche: List<String>, nachId: String) =
            eintraege.ladeSeite(bereiche, nachId, Sicherung.SEITE)

        override suspend fun fragenSeite(nachId: Long) = fragen.ladeSeite(nachId, Sicherung.SEITE)

        override suspend fun sitzungen() = chat.ladeSitzungen()

        override suspend fun nachrichten(sitzungId: Long) = chat.ladeNachrichten(sitzungId)
    }

    /** Wie viel eine Sicherung dieses Umfangs enthalten würde — ohne sie zu schreiben. */
    suspend fun sicherungsUmfangZaehlen(bereiche: List<String>, mitFragen: Boolean, mitGespraechen: Boolean) =
        SicherungsAnzahl(
            eintraege = if (bereiche.isEmpty()) 0 else eintraege.anzahlIn(bereiche),
            fragen = if (mitFragen) fragen.anzahl() else 0,
            // Zählen statt laden: Es geht nur um die Zahl für die Anzeige.
            sitzungen = if (mitGespraechen) chat.anzahlSitzungen() else 0,
            nachrichten = if (mitGespraechen) chat.anzahlNachrichten() else 0,
        )

    /**
     * Führt eine Sicherung mit dem Bestand zusammen — beliebig oft, ohne zu verdoppeln.
     *
     * Ergänzt wird nur, was hier fehlt. Ein Eintrag, den es schon gibt, bleibt unangetastet:
     * weder sein Text noch seine Angaben werden überschrieben. Der einzige Zusatz ist eine
     * Erklärung für einen Eintrag, der noch gar keine hat — dort geht nichts verloren.
     *
     * Das ist bewusst strenger als früher: Bis dahin ersetzte jede eingespielte Erklärung die
     * vorhandene (die alte wanderte in die Historie). Wer nach der Sicherung weitergearbeitet
     * hatte, fand seine neuere Fassung danach nur noch im Verlauf wieder.
     *
     * Alles, was dabei entsteht, wird mitgeschrieben — daraus wird das Zurücknehmen.
     */
    inner class EinspielSenke : Sicherung.Senke {
        private val jetzt = System.currentTimeMillis()
        val spur = Einspielspur()
        var uebersprungen = 0
            private set

        /**
         * Was schon da ist — einmal geladen und danach fortgeschrieben.
         *
         * Vorher fragte jede einzelne Frage die ganze Tabelle ab und jedes Gespräch sämtliche
         * Nachrichten aller Gespräche. Bei ein paar hundert Sätzen wird daraus ein Lauf, der
         * quadratisch wächst und die App zum Stehen bringt — ausgerechnet beim Einspielen,
         * also dann, wenn ohnehin gerade etwas schiefgegangen ist.
         */
        private var bekannteFragen: MutableSet<Triple<String, String, String>>? = null
        private var bekannteGespraeche: MutableSet<List<Pair<String, String>>>? = null

        private suspend fun fragenSchluessel(): MutableSet<Triple<String, String, String>> =
            bekannteFragen ?: fragen.beobachteAlle().first()
                .mapTo(mutableSetOf()) { Triple(it.eintragId, it.frage, it.antwort) }
                .also { bekannteFragen = it }

        private suspend fun gespraechsSchluessel(): MutableSet<List<Pair<String, String>>> =
            bekannteGespraeche ?: chat.ladeAlleNachrichten()
                .groupBy { it.sitzungId }
                .values
                .mapTo(mutableSetOf()) { liste -> liste.map { it.rolle to it.text } }
                .also { bekannteGespraeche = it }

        override suspend fun eintrag(werte: Map<String, String>) {
            val id = werte["id"].orEmpty()
            val text = werte["erklaerung"].orEmpty()
            if (id.isBlank()) {
                uebersprungen += 1
                return
            }
            val vorhanden = eintraege.lade(id)
            if (vorhanden == null) {
                val name = werte["name"].orEmpty()
                val bereich = werte["bereich"].orEmpty()
                if (name.isBlank() || bereich.isBlank()) {
                    uebersprungen += 1
                    return
                }
                eintraege.setze(
                    listOf(
                        EintragEntity(
                            id = id,
                            bereich = bereich,
                            name = name,
                            kurz = werte["kurz"].orEmpty(),
                            erklaerung = text,
                            stufe = werte["stufe"]?.toIntOrNull() ?: 0,
                            seitVersion = werte["seitVersion"].orEmpty(),
                            kategorie = werte["kategorie"].orEmpty(),
                            art = werte["art"].orEmpty(),
                            entfernt = werte["entfernt"] == "true",
                            entferntInVersion = werte["entferntInVersion"].orEmpty(),
                            ersatz = werte["ersatz"].orEmpty(),
                            quelleEnglisch = werte["quelleEnglisch"].orEmpty(),
                            sortierName = werte["sortierName"].orEmpty()
                                .ifBlank { name.removePrefix("/").lowercase() },
                        ),
                    ),
                )
                spur.neueEintraege += id
                return
            }
            // Vorhandenes bleibt stehen. Nur eine Lücke wird gefüllt: ein Eintrag ohne jede
            // Erklärung bekommt die aus der Sicherung. Alles andere wird übergangen.
            if (text.isBlank() || vorhanden.erklaerung.isNotBlank()) {
                uebersprungen += 1
                return
            }
            eintraege.aktualisiere(
                vorhanden.copy(
                    erklaerung = text,
                    stufe = werte["stufe"]?.toIntOrNull() ?: vorhanden.stufe,
                    zuletztGeaendert = jetzt,
                ),
            )
            spur.gefuellteErklaerungen += id
        }

        override suspend fun frage(eintragId: String, frage: String, antwort: String, erstelltAm: Long) {
            // Nur zu Einträgen, die es hier gibt — sonst würde der Fremdschlüssel greifen.
            if (eintragId.isBlank() || frage.isBlank() || eintraege.lade(eintragId) == null) {
                uebersprungen += 1
                return
            }
            val bekannt = fragenSchluessel()
            val schluessel = Triple(eintragId, frage, antwort)
            if (schluessel in bekannt) {
                uebersprungen += 1
                return
            }
            bekannt += schluessel
            val id = fragen.fuegeEin(
                FrageEntity(
                    eintragId = eintragId,
                    frage = frage,
                    antwort = antwort,
                    erstelltAm = erstelltAm.takeIf { it > 0 } ?: jetzt,
                ),
            )
            spur.neueFragen += id
        }

        override suspend fun sitzung(
            titel: String,
            erstelltAm: Long,
            nachrichten: List<Triple<String, String, Long>>,
        ) {
            if (nachrichten.isEmpty()) {
                uebersprungen += 1
                return
            }
            // Ein Gespräch gilt als vorhanden, wenn es eines mit genau denselben Nachrichten gibt.
            val bekannt = gespraechsSchluessel()
            val kennung = nachrichten.map { it.first to it.second }
            if (kennung in bekannt) {
                uebersprungen += 1
                return
            }
            bekannt += kennung
            val sitzungId = chat.lege(
                ChatSitzungEntity(
                    titel = titel.ifBlank { "Eingespielt" },
                    erstelltAm = erstelltAm.takeIf { it > 0 } ?: jetzt,
                ),
            )
            nachrichten.forEach { (rolle, text, wann) ->
                chat.fuegeEin(
                    ChatNachrichtEntity(
                        sitzungId = sitzungId,
                        rolle = rolle,
                        text = text,
                        erstelltAm = wann.takeIf { it > 0 } ?: jetzt,
                    ),
                )
            }
            spur.neueSitzungen += sitzungId
        }

        fun bericht() = EinspielBericht(
            erklaerungen = spur.gefuellteErklaerungen.size,
            eintraege = spur.neueEintraege.size,
            fragen = spur.neueFragen.size,
            gespraeche = spur.neueSitzungen.size,
            uebersprungen = uebersprungen,
        )
    }

    /**
     * Nimmt ein Einspielen wieder zurück.
     *
     * Entfernt genau das, was dabei entstanden ist — nicht mehr. Weil das Einspielen nur
     * ergänzt und nie überschreibt, genügt das: Was vorher dastand, wurde nie angefasst.
     */
    suspend fun nimmEinspielenZurueck(spur: Einspielspur): Int {
        var zurueck = 0
        spur.neueSitzungen.forEach { id ->
            chat.loescheSitzung(id)
            zurueck += 1
        }
        // Zuerst die eingespielten Fragen. Danach hängen an den eingespielten Einträgen nur
        // noch Fragen, die inzwischen selbst gestellt wurden.
        spur.neueFragen.forEach { id ->
            fragen.loesche(id)
            zurueck += 1
        }
        spur.gefuellteErklaerungen.forEach { id ->
            eintraege.lade(id)?.let { eintrag ->
                eintraege.aktualisiere(eintrag.copy(erklaerung = "", stufe = 0))
                zurueck += 1
            }
        }
        if (spur.neueEintraege.isNotEmpty()) {
            // An einem Eintrag hängen die Fragen mit CASCADE. Ihn zu löschen nähme jede Frage
            // mit — auch eine, die nach dem Einspielen selbst gestellt wurde. Ein
            // Zurücknehmen darf nichts wegnehmen, was nicht aus der Sicherung kam; solche
            // Einträge bleiben deshalb stehen.
            val alle = spur.neueEintraege.toList()
            val behalten = fragen.eintraegeMitFragen(alle).toSet()
            val loeschbar = alle.filterNot { it in behalten }
            if (loeschbar.isNotEmpty()) {
                eintraege.loesche(loeschbar)
                zurueck += loeschbar.size
            }
            if (behalten.isNotEmpty()) {
                KompassLog.info(
                    "Repository",
                    "nimmEinspielenZurueck",
                    "Einträge blieben stehen, weil eigene Fragen daran hängen",
                    mapOf("anzahl" to behalten.size),
                )
            }
        }
        baueSuchIndexNeu()
        KompassLog.info("Repository", "nimmEinspielenZurueck", "Einspielen zurückgenommen", mapOf("anzahl" to zurueck))
        return zurueck
    }

    suspend fun schliesseEinspielenAb() = baueSuchIndexNeu()

    companion object {
        const val ART_EINTRAG = "eintrag"
        const val ART_FRAGE = "frage"
        const val ART_CHAT = "chat"
    }
}
