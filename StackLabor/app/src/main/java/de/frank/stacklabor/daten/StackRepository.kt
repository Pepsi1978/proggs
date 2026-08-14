package de.frank.stacklabor.daten

import android.content.Context
import de.frank.stacklabor.daten.entitaeten.BewertungE
import de.frank.stacklabor.daten.entitaeten.Bereich
import de.frank.stacklabor.daten.entitaeten.BewertungszelleE
import de.frank.stacklabor.daten.entitaeten.EigeneFrageE
import de.frank.stacklabor.daten.entitaeten.EintragMitMittel
import de.frank.stacklabor.daten.entitaeten.FrageAntwortE
import de.frank.stacklabor.daten.entitaeten.KonkurrenzE
import de.frank.stacklabor.daten.entitaeten.KonkurrenzArt
import de.frank.stacklabor.daten.entitaeten.Loeslichkeit
import de.frank.stacklabor.daten.entitaeten.MittelE
import de.frank.stacklabor.daten.entitaeten.StackE
import de.frank.stacklabor.daten.entitaeten.StackEintragE
import de.frank.stacklabor.daten.entitaeten.StackZielE
import de.frank.stacklabor.daten.entitaeten.ZielE
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.logik.Ampelrechnung
import de.frank.stacklabor.logik.Pruefsumme
import de.frank.stacklabor.logik.Zelle
import de.frank.stacklabor.logik.ZielImStack
import de.frank.stacklabor.logik.Zaehlung
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
import de.frank.stacklabor.daten.entitaeten.Wirkung as WirkungE

/**
 * Die eine Stelle, an der Daten und Rechnung zusammenkommen.
 *
 * Die Oberfläche fragt hier nach fertigen Bildern („dieser Stack mit seinen Ampeln") und
 * bekommt sie als `Flow`. Die Ampeln werden dabei **lokal** aus der gespeicherten
 * Bewertungstabelle gerechnet (F-14) — kein Netz, keine Kosten, Ergebnis in Millisekunden.
 */
class StackRepository(
    private val ctx: Context,
    private val db: StackLaborDatenbank,
    val einstellungen: Einstellungen,
) {
    private val stacks = db.stackDao()
    private val mittel = db.mittelDao()
    private val ziele = db.zielDao()
    private val bewertungen = db.bewertungDao()
    private val fragen = db.frageDao()

    // ── Bilder für die Oberfläche ────────────────────────────────────────────────

    /** Eine Stack-Karte auf B-01: Stammdaten, Anzahl, Sammelampel, Zählpunkte, Stand. */
    data class StackKarte(
        val stack: StackE,
        val anzahlMittel: Int,
        val sammelampel: Ampel,
        val zaehlung: Zaehlung,
        val stand: Bewertungsstand,
        val offenerHinweis: Boolean,
    )

    enum class Bewertungsstand { NIE, GUELTIG, VERALTET }

    /** Ein Mittel in der Liste auf B-02, fertig für die Anzeige. */
    data class MittelZeile(
        val eintrag: StackEintragE,
        val mittel: MittelE,
        val ampel: Ampel,
        val gestoerteZiele: List<Int>,
        val dosisText: String,
        val metaText: String,
    )

    /** Ein Ziel in der Liste, fertig für die Anzeige. */
    data class ZielZeile(
        val ziel: ZielE,
        val rang: Int,
        val ampel: Ampel,
        val begruendung: String,
    )

    val alleStacks: Flow<List<StackE>> = stacks.alleStacks()

    /**
     * Die Karten für B-01. Sie hängen an drei Quellen zugleich: den Stacks, allen aktiven
     * Einträgen und den Zellen der jüngsten Bewertungen. Ändert sich eine davon, rechnet
     * `combine` die Ampeln neu — genau das ist der Grund, warum ein Häkchen sofort wirkt.
     */
    val stackKarten: Flow<List<StackKarte>> = combine(
        stacks.alleStacks(),
        stacks.alleEintraegeMitMittel(),
        ziele.alleZuordnungen(),
        ziele.alleZiele(),
        bewertungen.zellenDerJuengstenBewertungen(),
    ) { alleStacks, alleEintraege, zuordnungen, alleZiele, alleZellen ->
        val zielText = alleZiele.associate { it.id to it.text }
        alleStacks.map { s ->
            val eintraege = alleEintraege.filter { it.eintrag.stackId == s.id }
            val aktive = eintraege.filter { it.eintrag.aktiv }.map { it.eintrag.mittelId }.toSet()
            val zieleDesStacks = zuordnungen
                .filter { it.stackId == s.id }
                .sortedBy { it.rang }
                .map { ZielImStack(it.zielId, zielText[it.zielId] ?: "", it.rang) }
            val zellen = alleZellen.map { it.alsZelle() }
            StackKarte(
                stack = s,
                anzahlMittel = eintraege.size,
                sammelampel = Ampelrechnung.ampelStack(zieleDesStacks, zellen, aktive),
                zaehlung = Ampelrechnung.zaehlung(zieleDesStacks, zellen, aktive),
                stand = Bewertungsstand.NIE,   // wird unten je Stack genauer bestimmt
                offenerHinweis = eintraege.any { it.eintrag.offenerHinweis.isNotBlank() },
            )
        }
    }

    /** Die Mittel-Liste eines Stacks, sortiert nach der gewählten Ansicht (F-06). */
    fun mittelZeilen(stackId: String): Flow<List<MittelZeile>> = combine(
        stacks.eintraegeMitMittel(stackId),
        ziele.zieleDesStacks(stackId),
        bewertungen.juengsteBewertungMitInhalt(stackId),
        einstellungen.sortieransicht,
        einstellungen.dosisVariante,
    ) { eintraege, zieleMitRang, bewertung, ansicht, variante ->
        val zieleImStack = zieleMitRang.map { ZielImStack(it.ziel.id, it.ziel.text, it.rang) }
        val zellen = bewertung?.zellen?.map { it.alsZelle() } ?: emptyList()

        val zeilen = eintraege.map { em ->
            MittelZeile(
                eintrag = em.eintrag,
                mittel = em.mittel,
                ampel = if (zellen.isEmpty()) Ampel.GRAU
                else Ampelrechnung.ampelMittel(em.eintrag.mittelId, zellen, zieleImStack),
                gestoerteZiele = Ampelrechnung.gestoerteZiele(em.eintrag.mittelId, zellen, zieleImStack),
                dosisText = dosisText(em, variante),
                metaText = metaText(em),
            )
        }
        sortiere(zeilen, ansicht)
    }

    /** Die Ziel-Liste eines Stacks mit Ampeln und Begründungen. */
    fun zielZeilen(stackId: String): Flow<List<ZielZeile>> = combine(
        ziele.zieleDesStacks(stackId),
        stacks.eintraegeMitMittel(stackId),
        bewertungen.juengsteBewertungMitInhalt(stackId),
    ) { zieleMitRang, eintraege, bewertung ->
        val aktive = eintraege.filter { it.eintrag.aktiv }.map { it.eintrag.mittelId }.toSet()
        val zellen = bewertung?.zellen?.map { it.alsZelle() } ?: emptyList()
        zieleMitRang.map { zr ->
            val ampel = if (zellen.isEmpty()) Ampel.GRAU
            else Ampelrechnung.ampelZiel(zr.ziel.id, zellen, aktive)
            ZielZeile(
                ziel = zr.ziel,
                rang = zr.rang,
                ampel = ampel,
                begruendung = if (ampel == Ampel.GRUEN) "" else begruendungFuerZiel(zr.ziel.id, zellen, aktive),
            )
        }
    }

    // ── Anzeige-Texte ────────────────────────────────────────────────────────────

    /** „2 × 80 mg = 160 mg" — genau so, wie die Quelle es meint (F-07 im Grilling). */
    private fun dosisText(em: EintragMitMittel, variante: String): String {
        val e = em.eintrag
        val menge = if (variante == "B" && e.dosisVarianteB != null) e.dosisVarianteB else e.mengeJeStueck
        if (menge == null) return "${e.stueckzahl} ${em.mittel.darreichungsform.anzeige()}"
        val einheit = e.einheit.anzeige()
        return if (e.stueckzahl > 1) {
            "${e.stueckzahl} × ${zahl(menge)} $einheit = ${zahl(menge * e.stueckzahl)} $einheit"
        } else {
            "${zahl(menge)} $einheit"
        }
    }

    /** Der Zusatz in Zeile 2: Darreichungsform nur, wenn nicht Kapsel; Frequenz nur, wenn nicht täglich. */
    private fun metaText(em: EintragMitMittel): String {
        val teile = mutableListOf<String>()
        if (em.eintrag.frequenzTage > 1) {
            teile += "alle ${em.eintrag.frequenzTage} Tage"
        } else if (em.mittel.darreichungsform != de.frank.stacklabor.daten.entitaeten.Darreichungsform.KAPSEL) {
            teile += em.mittel.darreichungsform.anzeige()
        }
        return teile.joinToString(" · ")
    }

    private fun zahl(w: Double): String =
        if (w % 1.0 == 0.0) w.toInt().toString() else w.toString().replace('.', ',')

    private fun begruendungFuerZiel(zielId: String, zellen: List<Zelle>, aktive: Set<String>): String =
        zellen.filter { it.zielId == zielId && it.mittelId in aktive && it.wirkung == de.frank.stacklabor.logik.Wirkung.STOERT }
            .maxByOrNull { it.staerke }
            ?.grund
            .orEmpty()

    /**
     * Sortierung der Mittel-Liste (F-06).
     *
     * In der Ansicht „Löslichkeit" stehen erst alle wasserlöslichen, dann „beides", dann die
     * fettlöslichen — innerhalb jeder Gruppe in der Einnahme-Reihenfolge. Die gespeicherte
     * Reihenfolge wird dabei **nie** verändert; sie ist eine echte Information.
     *
     * Kombi-Gruppen bleiben in beiden Ansichten zusammen: Sie werden nach der Löslichkeit
     * ihres ersten Mitglieds einsortiert (F-28).
     */
    private fun sortiere(zeilen: List<MittelZeile>, ansicht: String): List<MittelZeile> {
        if (ansicht == "EINNAHME") return zeilen.sortedBy { it.eintrag.reihenfolge }

        fun rangDerLoeslichkeit(l: Loeslichkeit) = when (l) {
            Loeslichkeit.WASSER -> 0
            Loeslichkeit.BEIDES -> 1
            Loeslichkeit.FETT -> 2
        }

        // Gruppen erhalten den Rang ihres ersten Mitglieds, damit sie nicht auseinandergerissen werden.
        val gruppenRang = zeilen
            .filter { it.eintrag.gruppeId != null }
            .groupBy { it.eintrag.gruppeId!! }
            .mapValues { (_, mitglieder) ->
                val erstes = mitglieder.minByOrNull { it.eintrag.reihenfolge }!!
                rangDerLoeslichkeit(erstes.mittel.loeslichkeit) to erstes.eintrag.reihenfolge
            }

        return zeilen.sortedWith(
            compareBy(
                { z -> gruppenRang[z.eintrag.gruppeId]?.first ?: rangDerLoeslichkeit(z.mittel.loeslichkeit) },
                { z -> gruppenRang[z.eintrag.gruppeId]?.second ?: z.eintrag.reihenfolge },
                { z -> z.eintrag.reihenfolge },
            ),
        )
    }

    // ── Änderungen ───────────────────────────────────────────────────────────────

    /** F-05: Häkchen. Ändert die Bewertung **nicht** — die Tabelle bleibt gültig. */
    suspend fun setzeAktiv(eintragId: String, aktiv: Boolean) = stacks.setzeAktiv(eintragId, aktiv)

    /** F-28: Das Gruppen-Häkchen schaltet alle Mitglieder. */
    suspend fun setzeAktivFuerGruppe(stackId: String, gruppeId: String, aktiv: Boolean) =
        stacks.setzeAktivFuerGruppe(stackId, gruppeId, aktiv)

    /** F-07: Einnahme-Reihenfolge. Ändert die Bewertung nicht. */
    suspend fun setzeReihenfolge(eintragIds: List<String>) = stacks.setzeReihenfolgen(eintragIds)

    /** F-10: Ziel-Priorität je Stack. Ändert die Bewertung nicht — nur die Gewichtung. */
    suspend fun setzeZielRaenge(stackId: String, zielIds: List<String>) =
        ziele.setzeRaenge(stackId, zielIds)

    /** F-09: Ziel für diesen Stack an- oder abwählen. Macht die Bewertung **veraltet**. */
    suspend fun waehleZiel(stackId: String, zielId: String) = ziele.waehleZiel(stackId, zielId)
    suspend fun entferneZiel(stackId: String, zielId: String) = ziele.entferneZuordnung(stackId, zielId)

    /** F-08 */
    suspend fun legeZielAn(text: String): String {
        val id = "ziel-" + UUID.randomUUID().toString().take(8)
        ziele.einfuegenZiel(ZielE(id, text.trim()))
        return id
    }

    suspend fun benenneZielUm(zielId: String, text: String) {
        ziele.zielEinmalig(zielId)?.let { ziele.aktualisierenZiel(it.copy(text = text.trim())) }
    }

    suspend fun loescheZiel(zielId: String) = ziele.loeschenZielMitZellen(zielId)

    /** F-11 */
    suspend fun legeFrageAn(stackId: String, text: String) {
        fragen.einfuegenFrage(
            EigeneFrageE("frage-" + UUID.randomUUID().toString().take(8), stackId, text.trim()),
        )
    }

    suspend fun loescheFrage(frageId: String) = fragen.loeschenFrage(frageId)

    /** F-01 */
    suspend fun legeStackAn(name: String, zeitpunkt: String, hinweis: String): String {
        val id = "stack-" + UUID.randomUUID().toString().take(8)
        stacks.einfuegenStack(
            StackE(id, name.trim(), zeitpunkt.trim(), hinweis.trim(), stacks.naechsteSortierung()),
        )
        return id
    }

    suspend fun aktualisiereStack(stack: StackE) = stacks.aktualisierenStack(stack)
    suspend fun loescheStack(stackId: String) = stacks.loeschenStack(stackId)

    /** F-04: Entfernen samt der Bewertungszellen dieses Mittels in diesem Stack. */
    suspend fun entferneEintrag(eintragId: String) {
        val e = stacks.eintragEinmalig(eintragId) ?: return
        stacks.loeschenEintrag(eintragId)
        bewertungen.loeschenZellenFuerMittelImStack(e.stackId, e.mittelId)
    }

    /** F-02: Das Mittel steht sofort in der Liste — die Prüfung läuft nach. */
    suspend fun fuegeMittelHinzu(
        stackId: String,
        mittelId: String,
        stueckzahl: Int,
        mengeJeStueck: Double?,
        einheit: de.frank.stacklabor.daten.entitaeten.Einheit,
    ): String {
        val vorhanden = stacks.eintragFuerMittel(stackId, mittelId)
        if (vorhanden != null) return vorhanden.id
        val id = "$stackId-$mittelId"
        stacks.einfuegenEintrag(
            StackEintragE(
                id = id, stackId = stackId, mittelId = mittelId,
                stueckzahl = stueckzahl, mengeJeStueck = mengeJeStueck, einheit = einheit,
                reihenfolge = stacks.naechsteReihenfolge(stackId),
            ),
        )
        return id
    }

    suspend fun aktualisiereEintrag(eintrag: StackEintragE) = stacks.aktualisierenEintrag(eintrag)
    suspend fun aktualisiereMittel(m: MittelE) = mittel.aktualisierenMittel(m)

    suspend fun legeMittelAn(name: String, loeslichkeit: Loeslichkeit): String {
        val id = "mittel-" + UUID.randomUUID().toString().take(8)
        mittel.einfuegenMittel(MittelE(id, name.trim(), loeslichkeit))
        return id
    }

    /** F-30 */
    suspend fun fuehreMittelZusammen(bleibend: String, verlierend: String) =
        mittel.zusammenfuehren(bleibend, verlierend)

    // ── Bewertung ────────────────────────────────────────────────────────────────

    /**
     * F-23: Passt die gespeicherte Bewertung noch zum Stack?
     *
     * Häkchen und Priorität gehen bewusst nicht in die Prüfsumme ein — sonst wäre jede
     * Bewertung nach dem ersten Probieren „veraltet", obwohl sie vollkommen gültig bleibt.
     */
    suspend fun pruefsummeFuer(stackId: String): String {
        val stack = stacks.stackEinmalig(stackId) ?: return ""
        val eintraege = stacks.eintraegeMitMittel(stackId).first()
        val zielIds = ziele.zieleDesStacksEinmalig(stackId).map { it.ziel.id }
        val fragenTexte = fragen.fragenDesStacksEinmalig(stackId).map { it.text }
        val variante = einstellungen.dosisVariante.first()
        return Pruefsumme.fuerStack(
            zeitpunkt = stack.zeitpunkt,
            einnahmeHinweis = stack.einnahmeHinweis,
            mittel = eintraege.map {
                Pruefsumme.MittelAnteil(
                    mittelId = it.eintrag.mittelId,
                    stueckzahl = it.eintrag.stueckzahl,
                    mengeJeStueck = it.eintrag.mengeJeStueck,
                    einheit = it.eintrag.einheit.name,
                    darreichungsform = it.mittel.darreichungsform.name,
                    loeslichkeit = it.mittel.loeslichkeit.name,
                    frequenzTage = it.eintrag.frequenzTage,
                    alterniertMit = it.eintrag.alterniertMit,
                    zusatztext = it.eintrag.zusatztext,
                    dosisVarianteB = it.eintrag.dosisVarianteB,
                )
            },
            zielIds = zielIds,
            eigeneFragen = fragenTexte,
            dosisVariante = variante,
        )
    }

    /** Speichert einen Auswertungslauf und schneidet die Historie auf fünf zurück (F-29). */
    suspend fun speichereBewertung(
        stackId: String?,
        bereich: Bereich,
        modell: String,
        denkstufe: String,
        pruefsumme: String,
        gesamt: String,
        hinweise: List<String>,
        zellen: List<Zelle>,
        konkurrenzen: List<KonkurrenzE>,
        antworten: List<FrageAntwortE>,
    ) {
        val id = "bew-" + UUID.randomUUID().toString()
        bewertungen.einfuegenBewertung(
            BewertungE(
                id = id, bereich = bereich, stackId = stackId,
                zeitpunkt = System.currentTimeMillis(),
                modell = modell, denkstufe = denkstufe, pruefsumme = pruefsumme,
                gesamt = gesamt, hinweise = hinweise.joinToString("\n"),
            ),
        )
        bewertungen.einfuegenZellen(
            zellen.map {
                BewertungszelleE(
                    bewertungId = id, mittelId = it.mittelId, zielId = it.zielId,
                    wirkung = if (it.wirkung == de.frank.stacklabor.logik.Wirkung.STUETZT) WirkungE.STUETZT else WirkungE.STOERT,
                    staerke = it.staerke, grund = it.grund,
                )
            },
        )
        bewertungen.einfuegenKonkurrenzen(konkurrenzen.map { it.copy(bewertungId = id) })
        bewertungen.einfuegenAntworten(antworten.map { it.copy(bewertungId = id) })
        if (stackId != null) bewertungen.entferneAlteLaeufe(stackId, 5) else bewertungen.entferneAlteTageslaeufe(5)
    }

    // ── Startbestand, Export, Import ─────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** F-21: Startbestand aus den Assets einlesen. */
    suspend fun leseStartbestandEin() {
        val text = ctx.assets.open("startbestand.json").bufferedReader().use { it.readText() }
        val b = json.decodeFromString<Startbestand>(text)

        mittel.einfuegenMittel(
            b.mittel.map {
                MittelE(
                    id = it.id, name = it.name,
                    loeslichkeit = Loeslichkeit.valueOf(it.loeslichkeit),
                    darreichungsform = de.frank.stacklabor.daten.entitaeten.Darreichungsform.valueOf(it.darreichungsform),
                    hersteller = it.hersteller, durchfallrisiko = it.durchfallrisiko,
                    beistoffe = it.beistoffe,
                )
            },
        )
        stacks.einfuegenStacks(
            b.stacks.map { StackE(it.id, it.name, it.zeitpunkt, it.einnahmeHinweis, it.sortierung) },
        )
        stacks.einfuegenEintraege(
            b.eintraege.map {
                StackEintragE(
                    id = it.id, stackId = it.stackId, mittelId = it.mittelId,
                    stueckzahl = it.stueckzahl, mengeJeStueck = it.mengeJeStueck,
                    einheit = de.frank.stacklabor.daten.entitaeten.Einheit.valueOf(it.einheit),
                    dosisVarianteB = it.dosisVarianteB, frequenzTage = it.frequenzTage,
                    alterniertMit = it.alterniertMit, aktiv = it.aktiv,
                    reihenfolge = it.reihenfolge, gruppeId = it.gruppeId, zusatztext = it.zusatztext,
                )
            },
        )
        ziele.einfuegenZiele(b.ziele.map { ZielE(it.id, it.text) })
        ziele.setzeZuordnungen(b.stackZiele.map { StackZielE(it.stackId, it.zielId, it.rang) })
        einstellungen.merkeStartbestandEingelesen()
    }

    /** Beim allerersten Start einmal einlesen. */
    suspend fun stelleStartbestandSicher() {
        if (!einstellungen.startbestandEingelesen.first()) leseStartbestandEin()
    }
}

/** Umwandlung der gespeicherten Zelle in die Rechengestalt. */
private fun BewertungszelleE.alsZelle(): Zelle = Zelle(
    mittelId = mittelId,
    zielId = zielId,
    wirkung = if (wirkung == WirkungE.STUETZT) de.frank.stacklabor.logik.Wirkung.STUETZT
    else de.frank.stacklabor.logik.Wirkung.STOERT,
    staerke = staerke,
    grund = grund,
)

private fun de.frank.stacklabor.daten.entitaeten.Darreichungsform.anzeige(): String = when (this) {
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.KAPSEL -> "Kapsel"
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.TABLETTE -> "Tablette"
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.LOEFFEL -> "Löffel"
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.TASSE -> "Tasse"
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.PULVER -> "Pulver"
    de.frank.stacklabor.daten.entitaeten.Darreichungsform.SONSTIGE -> "Stück"
}

private fun de.frank.stacklabor.daten.entitaeten.Einheit.anzeige(): String = when (this) {
    de.frank.stacklabor.daten.entitaeten.Einheit.MG -> "mg"
    de.frank.stacklabor.daten.entitaeten.Einheit.UG -> "µg"
    de.frank.stacklabor.daten.entitaeten.Einheit.G -> "g"
    de.frank.stacklabor.daten.entitaeten.Einheit.ML -> "ml"
    de.frank.stacklabor.daten.entitaeten.Einheit.IE -> "IE"
    de.frank.stacklabor.daten.entitaeten.Einheit.STUECK -> "Stück"
}

// ── Gestalt der Startbestand-Datei ───────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class Startbestand(
    val schemaVersion: Int = 1,
    val stand: String = "",
    val herkunft: String = "",
    val mittel: List<MittelJson> = emptyList(),
    val stacks: List<StackJson> = emptyList(),
    val eintraege: List<EintragJson> = emptyList(),
    val ziele: List<ZielJson> = emptyList(),
    val stackZiele: List<StackZielJson> = emptyList(),
)

@kotlinx.serialization.Serializable
data class MittelJson(
    val id: String, val name: String, val loeslichkeit: String,
    val darreichungsform: String, val hersteller: String = "",
    val durchfallrisiko: Boolean = false, val beistoffe: String = "",
)

@kotlinx.serialization.Serializable
data class StackJson(
    val id: String, val name: String, val zeitpunkt: String = "",
    val einnahmeHinweis: String = "", val sortierung: Int = 0,
)

@kotlinx.serialization.Serializable
data class EintragJson(
    val id: String, val stackId: String, val mittelId: String,
    val stueckzahl: Int = 1, val mengeJeStueck: Double? = null, val einheit: String = "MG",
    val dosisVarianteB: Double? = null, val frequenzTage: Int = 1,
    val alterniertMit: String = "", val aktiv: Boolean = true, val reihenfolge: Int = 0,
    val gruppeId: String? = null, val zusatztext: String = "",
)

@kotlinx.serialization.Serializable
data class ZielJson(val id: String, val text: String)

@kotlinx.serialization.Serializable
data class StackZielJson(val stackId: String, val zielId: String, val rang: Int)
