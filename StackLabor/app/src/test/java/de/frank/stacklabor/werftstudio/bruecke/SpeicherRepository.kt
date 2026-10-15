package de.frank.stacklabor.werftstudio.bruecke

import de.frank.stacklabor.werftstudio.data.repository.AmpelErgebnis
import de.frank.stacklabor.werftstudio.data.repository.ImportModus
import de.frank.stacklabor.werftstudio.data.repository.SeedImportErgebnis
import de.frank.stacklabor.werftstudio.data.repository.StackLaborRepository
import de.frank.stacklabor.werftstudio.data.repository.StackSortierstatus
import de.frank.stacklabor.werftstudio.domain.model.BewertungMitDetails
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Sortieransicht
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackInhalt
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Ziel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository im Speicher für die Brücken-Tests: dieselben Prüfungen, die für die Brücke zählen (ein Mittel je Stack nur
 * einmal, genau eine freie Dosis, Kaskaden beim Löschen), aber ohne Datenbank und ohne echte Nutzerdaten.
 */
class SpeicherRepository : StackLaborRepository {
    val stacks = mutableListOf<Stack>()
    val mittel = mutableListOf<Mittel>()
    val eintraege = mutableListOf<StackEintrag>()
    val ziele = mutableListOf<Ziel>()
    val stackZiele = mutableListOf<StackZiel>()
    val fragen = mutableListOf<EigeneFrage>()
    var schreibvorgaenge = 0
        private set

    override fun beobachteMittel(): Flow<List<Mittel>> = flowOf(mittel.toList())
    override fun beobachteStacks(): Flow<List<Stack>> = flowOf(stacks.sortedBy { it.sortierung })
    override fun beobachteEintraege(stackId: String): Flow<List<StackEintrag>> = flowOf(eintraege.filter { it.stackId == stackId }.sortedBy { it.reihenfolge })
    override fun beobachteAlleEintraege(): Flow<List<StackEintrag>> = flowOf(eintraege.sortedWith(compareBy({ it.stackId }, { it.reihenfolge })))
    override fun beobachteZiele(): Flow<List<Ziel>> = flowOf(ziele.toList())
    override fun beobachteStackZiele(stackId: String): Flow<List<StackZiel>> = flowOf(stackZiele.filter { it.stackId == stackId }.sortedBy { it.rang })
    override fun beobachteAlleStackZiele(): Flow<List<StackZiel>> = flowOf(stackZiele.toList())
    override fun beobachteFragen(stackId: String): Flow<List<EigeneFrage>> = flowOf(fragen.filter { it.stackId == stackId })
    override fun beobachteNeuesteBewertung(stackId: String): Flow<BewertungMitDetails?> = flowOf(null)
    override fun beobachteNeuesteTagBewertung(): Flow<BewertungMitDetails?> = flowOf(null)
    override fun beobachteHistorie(stackId: String): Flow<List<BewertungMitDetails>> = flowOf(emptyList())
    override fun beobachteSortieransicht(stackId: String): Flow<StackSortierstatus> = flowOf(StackSortierstatus())

    override suspend fun speichereMittel(mittel: Mittel) {
        require(mittel.name.isNotBlank() && mittel.name.length <= 80) { "Der Mittelname muss 1 bis 80 Zeichen haben." }
        schreibvorgaenge++
        this.mittel.removeAll { it.id == mittel.id }
        this.mittel += mittel
    }

    override suspend fun loescheMittel(mittelId: String, bestaetigt: Boolean): Int {
        val n = eintraege.count { it.mittelId == mittelId }
        check(n == 0 || bestaetigt) { "Das Mittel wird in $n Stacks verwendet." }
        schreibvorgaenge++
        mittel.removeAll { it.id == mittelId }
        eintraege.removeAll { it.mittelId == mittelId }
        return n
    }

    override suspend fun fuehreMittelZusammen(behalteneMittelId: String, zuErsetzendeMittelId: String) {
        schreibvorgaenge++
        val ziel = eintraege.filter { it.mittelId == behalteneMittelId }.map { it.stackId }.toSet()
        eintraege.replaceAll { if (it.mittelId == zuErsetzendeMittelId && it.stackId !in ziel) it.copy(mittelId = behalteneMittelId) else it }
        eintraege.removeAll { it.mittelId == zuErsetzendeMittelId }
        mittel.removeAll { it.id == zuErsetzendeMittelId }
    }

    override suspend fun speichereStack(stack: Stack) {
        require(stack.name.isNotBlank() && stack.name.length <= 60) { "Der Stackname muss 1 bis 60 Zeichen haben." }
        schreibvorgaenge++
        stacks.removeAll { it.id == stack.id }
        stacks += stack
    }

    override suspend fun loescheStack(stackId: String) {
        schreibvorgaenge++
        stacks.removeAll { it.id == stackId }
        eintraege.removeAll { it.stackId == stackId }
        stackZiele.removeAll { it.stackId == stackId }
        fragen.removeAll { it.stackId == stackId }
    }

    override suspend fun setzeStackGesperrt(stackId: String, gesperrt: Boolean) {
        stacks.replaceAll { if (it.id == stackId) it.copy(gesperrt = gesperrt) else it }
    }

    override suspend fun speichereEintrag(eintrag: StackEintrag) {
        require(eintrag.dosen.count { it.variante == DosisVariante.FREI } == 1) { "Genau eine freie Dosis ist erforderlich." }
        require(eintrag.dosen.all { it.stackEintragId == eintrag.id && it.stueckzahl.signum() > 0 }) { "Ungültige Stückzahl." }
        require(eintrag.dosen.all { it.mengeJeStueck == null || it.einheit != null || !it.einheitText.isNullOrBlank() }) { "Zu einer Menge gehört eine Einheit." }
        check(stacks.any { it.id == eintrag.stackId }) { "Der Stack fehlt." }
        check(mittel.any { it.id == eintrag.mittelId }) { "Das Mittel fehlt." }
        // Wie der eindeutige Index (stackId, mittelId) der Datenbank.
        check(eintraege.none { it.stackId == eintrag.stackId && it.mittelId == eintrag.mittelId && it.id != eintrag.id }) { "UNIQUE constraint failed" }
        schreibvorgaenge++
        eintraege.removeAll { it.id == eintrag.id }
        eintraege += eintrag
    }

    override suspend fun loescheEintrag(eintragId: String) {
        schreibvorgaenge++
        eintraege.removeAll { it.id == eintragId }
    }

    override suspend fun uebertrageMittel(quellStackId: String, zielStackId: String, mittelId: String, verschieben: Boolean) {
        val e = eintraege.first { it.stackId == quellStackId && it.mittelId == mittelId }
        val id = if (verschieben) e.id else "kopie-${e.id}"
        val reihenfolge = (eintraege.filter { it.stackId == zielStackId }.maxOfOrNull { it.reihenfolge } ?: 0) + 1
        if (verschieben) eintraege.remove(e)
        speichereEintrag(e.copy(id = id, stackId = zielStackId, reihenfolge = reihenfolge, gruppeId = null, dosen = e.dosen.map { it.copy(stackEintragId = id) }))
    }

    override suspend fun setzeEintragAktiv(eintragId: String, aktiv: Boolean) {
        schreibvorgaenge++
        eintraege.replaceAll { if (it.id == eintragId) it.copy(aktiv = aktiv) else it }
    }

    override suspend fun schalteEintragAktivUm(eintragId: String) {
        eintraege.replaceAll { if (it.id == eintragId) it.copy(aktiv = !it.aktiv) else it }
    }

    override suspend fun setzeOffenenHinweis(eintragId: String, hinweis: String?) {
        eintraege.replaceAll { if (it.id == eintragId) it.copy(offenerHinweis = hinweis) else it }
    }

    override suspend fun speichereZiel(ziel: Ziel) { schreibvorgaenge++; ziele.removeAll { it.id == ziel.id }; ziele += ziel }
    override suspend fun loescheZiel(zielId: String) { ziele.removeAll { it.id == zielId }; stackZiele.removeAll { it.zielId == zielId } }
    override suspend fun setzeStackZiel(stackZiel: StackZiel) {
        schreibvorgaenge++
        stackZiele.removeAll { it.stackId == stackZiel.stackId && it.zielId == stackZiel.zielId }
        stackZiele += stackZiel
    }
    override suspend fun entferneStackZiel(stackId: String, zielId: String) {
        schreibvorgaenge++
        stackZiele.removeAll { it.stackId == stackId && it.zielId == zielId }
    }
    override suspend fun speichereFrage(frage: EigeneFrage) { schreibvorgaenge++; fragen.removeAll { it.id == frage.id }; fragen += frage }
    override suspend fun loescheFrage(frageId: String) { schreibvorgaenge++; fragen.removeAll { it.id == frageId } }
    override suspend fun setzeSortieransicht(stackId: String, sortieransicht: Sortieransicht) = Unit

    override suspend fun sortiereStacks(stackIds: List<String>) {
        require(stackIds.toSet() == stacks.map { it.id }.toSet())
        schreibvorgaenge++
        stackIds.forEachIndexed { i, id -> stacks.replaceAll { if (it.id == id) it.copy(sortierung = i + 1) else it } }
    }

    override suspend fun sortiereEintraege(stackId: String, eintragIds: List<String>) {
        require(eintragIds.toSet() == eintraege.filter { it.stackId == stackId }.map { it.id }.toSet())
        schreibvorgaenge++
        eintragIds.forEachIndexed { i, id -> eintraege.replaceAll { if (it.id == id) it.copy(reihenfolge = i + 1) else it } }
    }

    override suspend fun sortiereZiele(stackId: String, zielIds: List<String>) {
        schreibvorgaenge++
        zielIds.forEachIndexed { i, id -> stackZiele.replaceAll { if (it.stackId == stackId && it.zielId == id) it.copy(rang = i + 1) else it } }
    }

    override suspend fun speichereBewertung(bewertung: BewertungMitDetails) = error("nicht benutzt")
    override suspend fun berechneAmpeln(stackId: String): AmpelErgebnis = error("nicht benutzt")
    override suspend fun berechnePruefsumme(stackId: String, dosisVariante: DosisVariante): String = error("nicht benutzt")
    override suspend fun istBewertungVeraltet(stackId: String, dosisVariante: DosisVariante): Boolean = false
    override suspend fun exportiere(): String = error("nicht benutzt")
    override suspend fun importiere(json: String, modus: ImportModus) = error("nicht benutzt")
    override suspend fun stelleRueckfallsicherungWiederHer() = error("nicht benutzt")
    override suspend fun importiereStartbestand(): SeedImportErgebnis = error("nicht benutzt")
    override suspend fun initialisiereStartbestandWennLeer(): SeedImportErgebnis? = null
    override suspend fun ladeStackInhalt(stackId: String): StackInhalt = error("nicht benutzt")
}
