package de.frank.stacklabor.werftstudio.bruecke

import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Beispielbefehle aus dem Auftrag Ende zu Ende gegen die Brückenlogik, mit erfundenen Testdaten im Speicher.
 * Franks echte Einnahmedaten werden dabei nie berührt.
 */
class JarvisStackDienstTest {
    private val repo = SpeicherRepository()
    private var zaehler = 0
    private val neueMittel = mutableListOf<String>()
    private val dienst = JarvisStackDienst(repo, neueId = { "neu-${++zaehler}" }, nachNeuemMittel = { neueMittel += it.name })

    init {
        listOf(
            Stack("a1", "Nach Aufstehen 1", "Direkt nach dem Aufstehen", "nur mit Wasser", 1),
            Stack("a2", "Nach Aufstehen 2", "60 Minuten nach dem Aufstehen", "mit Olivenöl", 2),
            Stack("sp1", "Sport 1", "45 Minuten vor dem Sport", "", 3),
            Stack("sp2", "Sport 2", "direkt nach dem Sport", "mit Wasser", 4),
            Stack("sp3", "Sport 3", "", "", 5),
            Stack("vs1", "Vor dem Schlafen 1", "2 Stunden vor dem Schlafen", "", 6),
            Stack("vs2", "Vor dem Schlafen 2", "direkt vor dem Schlafen", "", 7, gesperrt = true),
        ).forEach { repo.stacks += it }
        repo.mittel += Mittel("vitc", "Vitamin C", darreichungsform = Darreichungsform.KAPSEL)
        repo.mittel += Mittel("ashwa", "Ashwagandha KSM-66", Loeslichkeit.FETT, Darreichungsform.KAPSEL)
        repo.mittel += Mittel("mag", "Magnesium", darreichungsform = Darreichungsform.TABLETTE)
        repo.eintraege += eintrag("e1", "sp2", "vitc", 1, aktiv = true, stueck = "2", je = "500", einheit = Einheit.MG)
        repo.eintraege += eintrag("e2", "sp2", "mag", 2, aktiv = false, stueck = "1", je = "400", einheit = Einheit.MG)
        repo.eintraege += eintrag("e3", "vs2", "mag", 1, aktiv = true, stueck = "1", je = "200", einheit = Einheit.MG)
    }

    private fun eintrag(id: String, stack: String, mittel: String, nr: Int, aktiv: Boolean, stueck: String, je: String?, einheit: Einheit?) = StackEintrag(
        id = id, stackId = stack, mittelId = mittel, aktiv = aktiv, reihenfolge = nr,
        dosen = listOf(StackEintragDosis(id, DosisVariante.FREI, BigDecimal(stueck), je?.let(::BigDecimal), einheit)),
    )

    private fun rufe(methode: String, vararg felder: Pair<String, Any>): JSONObject = runBlocking {
        dienst.verarbeite(methode, JSONObject().apply { felder.forEach { put(it.first, it.second) } })
    }

    private fun eintragVon(stack: String, mittelName: String): StackEintrag? {
        val m = repo.mittel.firstOrNull { it.name == mittelName } ?: return null
        return repo.eintraege.firstOrNull { it.stackId == stack && it.mittelId == m.id }
    }

    @Test
    fun beispielAshwagandhaInSport2ZweiGrammZweiKapseln() {
        val antwort = rufe("eintrag", "stack" to "Sportmodus 2", "mittel" to "Ashwagandha", "aktion" to "hinzufuegen", "menge" to "2", "einheit" to "Gramm", "stueckzahl" to "2")
        assertNull(antwort.opt("fehler"), antwort.toString())
        val e = assertNotNull(eintragVon("sp2", "Ashwagandha KSM-66"))
        val frei = e.dosen.single { it.variante == DosisVariante.FREI }
        assertEquals(0, frei.stueckzahl.compareTo(BigDecimal(2)))
        assertEquals(0, frei.mengeJeStueck!!.compareTo(BigDecimal.ONE))
        assertEquals(Einheit.G, frei.einheit)
        assertTrue(e.aktiv)
        assertEquals(3, e.reihenfolge)
        assertTrue(antwort.getString("text").contains("2 Kapseln à 1 g = 2 g"), antwort.getString("text"))
        assertTrue(antwort.getString("text").startsWith("Gespeichert und nachgelesen"))
        // Vorhandenes Katalog-Mittel benutzt, kein Doppel angelegt.
        assertEquals(1, repo.mittel.count { it.name.startsWith("Ashwagandha") })
    }

    @Test
    fun beispielGlycinInSport2SechsGrammLegtMittelNurAufWunschAn() {
        val ohne = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Glycin", "aktion" to "hinzufuegen", "menge" to "6", "einheit" to "Gramm")
        assertTrue(ohne.getString("fehler").contains("noch nicht im Mittel-Katalog"), ohne.toString())
        assertTrue(ohne.optBoolean("rueckfrage"))
        assertNull(repo.mittel.firstOrNull { it.name == "Glycin" })

        val mit = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Glycin", "aktion" to "hinzufuegen", "menge" to "6", "einheit" to "Gramm", "neues_mittel" to true)
        assertNull(mit.opt("fehler"), mit.toString())
        val e = assertNotNull(eintragVon("sp2", "Glycin"))
        val frei = e.dosen.single()
        assertEquals(0, frei.stueckzahl.compareTo(BigDecimal.ONE))
        assertEquals(0, frei.mengeJeStueck!!.compareTo(BigDecimal(6)))
        assertEquals(Einheit.G, frei.einheit)
        // Keine Darreichungsform erfunden, Löslichkeit bestimmt die KI der App.
        assertEquals(Darreichungsform.SONSTIGE, repo.mittel.single { it.name == "Glycin" }.darreichungsform)
        assertEquals(listOf("Glycin"), neueMittel)
        assertTrue(mit.getString("text").contains("Glycin — 6 g"), mit.getString("text"))
    }

    @Test
    fun wiederholterAufrufLegtNichtsDoppeltAn() {
        val felder = arrayOf<Pair<String, Any>>("stack" to "Sport 2", "mittel" to "Glycin", "aktion" to "hinzufuegen", "menge" to "6", "einheit" to "g", "neues_mittel" to true)
        rufe("eintrag", *felder)
        val schreibenVorher = repo.schreibvorgaenge
        val zweiter = rufe("eintrag", *felder)
        assertNull(zweiter.opt("fehler"), zweiter.toString())
        assertTrue(zweiter.getString("text").contains("nichts doppelt"), zweiter.getString("text"))
        assertEquals(schreibenVorher, repo.schreibvorgaenge)
        assertEquals(1, repo.mittel.count { it.name == "Glycin" })
        assertEquals(1, repo.eintraege.count { it.stackId == "sp2" && it.mittelId == repo.mittel.single { m -> m.name == "Glycin" }.id })
        // 6000 mg sind dieselbe Dosis wie 6 g: ebenfalls kein Doppel und keine Änderung.
        val inMg = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Glycin", "aktion" to "hinzufuegen", "menge" to "6000", "einheit" to "mg")
        assertNull(inMg.opt("fehler"), inMg.toString())
        assertEquals(schreibenVorher, repo.schreibvorgaenge)
    }

    @Test
    fun andereDosisBeiVorhandenemMittelWirdNichtStillGeaendert() {
        val antwort = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Vitamin C", "aktion" to "hinzufuegen", "menge" to "2", "einheit" to "g")
        assertTrue(antwort.getString("fehler").contains("schon in Sport 2"), antwort.toString())
        assertEquals(0, eintragVon("sp2", "Vitamin C")!!.dosen.single().mengeJeStueck!!.compareTo(BigDecimal(500)))

        val geaendert = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Vitamin C", "aktion" to "aendern", "menge" to "2", "einheit" to "g", "stueckzahl" to "2")
        assertNull(geaendert.opt("fehler"), geaendert.toString())
        val d = eintragVon("sp2", "Vitamin C")!!.dosen.single()
        assertEquals(0, d.mengeJeStueck!!.compareTo(BigDecimal.ONE))
        assertEquals(Einheit.G, d.einheit)
        assertTrue(geaendert.getString("text").contains("Vorher:") && geaendert.getString("text").contains("Jetzt:"))
    }

    @Test
    fun nurStueckzahlBeimAendernFuehrtZurRueckfrage() {
        val antwort = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Vitamin C", "aktion" to "aendern", "stueckzahl" to "3")
        assertTrue(antwort.optBoolean("rueckfrage"), antwort.toString())
        assertEquals(0, eintragVon("sp2", "Vitamin C")!!.dosen.single().stueckzahl.compareTo(BigDecimal(2)))
        val beibehalten = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Vitamin C", "aktion" to "aendern", "stueckzahl" to "3", "menge_je_stueck_beibehalten" to true)
        assertNull(beibehalten.opt("fehler"), beibehalten.toString())
        assertTrue(beibehalten.getString("text").contains("3 Kapseln à 500 mg = 1500 mg"), beibehalten.getString("text"))
    }

    @Test
    fun fehlendeDosisWirdErfragt() {
        val antwort = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Ashwagandha", "aktion" to "hinzufuegen")
        assertTrue(antwort.getString("fehler").contains("fehlt die Dosis"), antwort.toString())
        assertNull(eintragVon("sp2", "Ashwagandha KSM-66"))
        val ohneEinheit = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Ashwagandha", "aktion" to "hinzufuegen", "menge" to "2")
        assertTrue(ohneEinheit.getString("fehler").contains("Einheit"), ohneEinheit.toString())
    }

    @Test
    fun mehrdeutigerStackFuehrtZurRueckfrageOhneAenderung() {
        val schreibenVorher = repo.schreibvorgaenge
        val antwort = rufe("eintrag", "stack" to "Sport", "mittel" to "Ashwagandha", "aktion" to "hinzufuegen", "menge" to "2", "einheit" to "g")
        assertTrue(antwort.optBoolean("rueckfrage"))
        assertEquals(listOf("Sport 1", "Sport 2", "Sport 3"), (antwort.get("kandidaten") as JSONArray).let { a -> (0 until a.length()).map(a::getString) })
        assertEquals(schreibenVorher, repo.schreibvorgaenge)
    }

    @Test
    fun aktivierenUndDeaktivierenMitNachlesen() {
        val an = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Magnesium", "aktion" to "aktivieren")
        assertNull(an.opt("fehler"), an.toString())
        assertTrue(eintragVon("sp2", "Magnesium")!!.aktiv)
        val nochmal = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Magnesium", "aktion" to "aktivieren")
        assertTrue(nochmal.getString("text").contains("war schon aktiv"))
        val aus = rufe("eintrag", "stack" to "Sport 2", "mittel" to "Magnesium", "aktion" to "deaktivieren")
        assertTrue(aus.getString("text").contains("INAKTIV"), aus.getString("text"))
        assertFalse(eintragVon("sp2", "Magnesium")!!.aktiv)
    }

    @Test
    fun auslesenTrenntAktivUndInaktivUndZeigtBeschreibungUndReihenfolge() {
        val text = rufe("stack", "stack" to "Sport-Stack 2").getString("text")
        assertTrue(text.contains("Zeitpunkt: direkt nach dem Sport"), text)
        assertTrue(text.contains("Einnahmehinweis: mit Wasser"), text)
        assertTrue(text.contains("(2, davon 1 aktiv)"), text)
        val vitamin = text.lines().first { it.contains("Vitamin C") }
        val magnesium = text.lines().first { it.contains("Magnesium") }
        assertTrue(vitamin.contains("1. Vitamin C — 2 Kapseln à 500 mg = 1000 mg") && vitamin.contains("· aktiv"), vitamin)
        assertTrue(magnesium.contains("2. Magnesium") && magnesium.contains("INAKTIV"), magnesium)

        val alle = rufe("stacks", "voll" to true).getString("text")
        assertTrue(alle.contains("kein Nachweis einer Einnahme"))
        assertTrue(alle.indexOf("Vor dem Schlafen 1") < alle.indexOf("Vor dem Schlafen 2"))
        assertTrue(alle.indexOf("Nach Aufstehen 1") < alle.indexOf("Sport 1"))
        assertTrue(alle.contains("Gesperrt in Stack-Labor"))
    }

    @Test
    fun gesperrterStackBleibtUnveraendert() {
        val schreibenVorher = repo.schreibvorgaenge
        val antwort = rufe("eintrag", "stack" to "Vor dem Schlafen 2", "mittel" to "Magnesium", "aktion" to "deaktivieren")
        assertTrue(antwort.getString("fehler").contains("gesperrt"), antwort.toString())
        assertTrue(repo.eintraege.single { it.id == "e3" }.aktiv)
        val loeschen = rufe("loeschen", "was" to "stack", "stack" to "Vor dem Schlafen 2", "bestaetigt" to true)
        assertTrue(loeschen.getString("fehler").contains("gesperrt"))
        assertEquals(schreibenVorher, repo.schreibvorgaenge)
    }

    @Test
    fun loeschenNurNachBestaetigung() {
        val frage = rufe("loeschen", "was" to "mittel_aus_stack", "stack" to "Sport 2", "mittel" to "Vitamin C")
        assertTrue(frage.optBoolean("bestaetigung_noetig"), frage.toString())
        assertNotNull(eintragVon("sp2", "Vitamin C"))
        val ok = rufe("loeschen", "was" to "mittel_aus_stack", "stack" to "Sport 2", "mittel" to "Vitamin C", "bestaetigt" to true)
        assertNull(ok.opt("fehler"), ok.toString())
        assertNull(eintragVon("sp2", "Vitamin C"))
        assertTrue(ok.getString("text").contains("Zum Wiederherstellen"))
    }

    @Test
    fun stackAnlegenOhneDoppelUndReihenfolgeAendern() {
        val neu = rufe("stack_speichern", "stack" to "Sport 4", "neu" to true, "zeitpunkt" to "nach dem Laufen")
        assertNull(neu.opt("fehler"), neu.toString())
        assertEquals(1, repo.stacks.count { it.name == "Sport 4" })
        val nochmal = rufe("stack_speichern", "stack" to "sport vier", "neu" to true)
        assertTrue(nochmal.getString("text").contains("gibt es schon"))
        assertEquals(1, repo.stacks.count { it.name == "Sport 4" })

        val reihenfolge = rufe("stack_speichern", "stack" to "Sport 2", "mittel_reihenfolge" to JSONArray(listOf("Magnesium")))
        assertNull(reihenfolge.opt("fehler"), reihenfolge.toString())
        assertEquals(1, repo.eintraege.single { it.id == "e2" }.reihenfolge)
        assertEquals(2, repo.eintraege.single { it.id == "e1" }.reihenfolge)
    }

    @Test
    fun leereFelderLoeschenKeineBeschreibung() {
        rufe("stack_speichern", "stack" to "Sport 2", "zeitpunkt" to "", "einnahme_hinweis" to "", "ziel_hinzufuegen" to "Erholung")
        val s = repo.stacks.single { it.id == "sp2" }
        assertEquals("direkt nach dem Sport", s.zeitpunkt)
        assertEquals("mit Wasser", s.einnahmeHinweis)
        assertEquals(1, repo.stackZiele.count { it.stackId == "sp2" })
    }

    @Test
    fun mittelImKatalogAnlegenErkenntTippfehler() {
        val tippfehler = rufe("mittel_speichern", "mittel" to "Ashwaganda KSM-66", "neu" to true)
        assertTrue(tippfehler.optBoolean("rueckfrage"), tippfehler.toString())
        val gleich = rufe("mittel_speichern", "mittel" to "vitamin-c", "neu" to true)
        assertTrue(gleich.getString("text").contains("nichts doppelt"))
        assertEquals(3, repo.mittel.size)
        val neu = rufe("mittel_speichern", "mittel" to "Kreatin", "neu" to true, "darreichungsform" to "Pulver", "loeslichkeit" to "wasser")
        assertNull(neu.opt("fehler"), neu.toString())
        assertEquals(Darreichungsform.PULVER, repo.mittel.single { it.name == "Kreatin" }.darreichungsform)
        // Löslichkeit ausdrücklich genannt: keine KI-Bestimmung.
        assertTrue(neueMittel.isEmpty())
    }
}
