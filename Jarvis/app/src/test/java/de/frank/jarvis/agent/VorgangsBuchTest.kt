package de.frank.jarvis.agent

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VorgangsBuchTest {
    private val ordner: File = Files.createTempDirectory("vorgaenge").toFile()
    private val datei = File(ordner, "vorgaenge.json")
    private var jetzt = 1_000_000L
    private fun buch() = VorgangsBuch(datei, uhr = { jetzt })

    @Test fun annahmeLiefertKennungUndWiederholungStartetNichtsDoppelt() {
        val b = buch()
        var reserviert = 0
        val erst = b.annehmen("Recherche", "Wie baut man ein Hochbeet?") { reserviert++; true }
        assertTrue(erst is Annahme.Neu)
        val v = (erst as Annahme.Neu).vorgang
        assertEquals(Vorgang.ANGENOMMEN, v.stand)
        // Wiederholung nach Verbindungsabbruch, anderer Leerraum und Großschreibung: derselbe Vorgang, keine zweite Reservierung.
        jetzt += 30_000
        val zweit = b.annehmen("recherche", "  wie baut man  ein Hochbeet? ") { reserviert++; true }
        assertTrue(zweit is Annahme.Doppelt)
        assertEquals(v.id, (zweit as Annahme.Doppelt).vorgang.id)
        assertEquals(1, reserviert)
    }

    @Test fun keinPlatzLegtNichtsAn() {
        val b = buch()
        assertTrue(b.annehmen("Recherche", "A") { false } is Annahme.KeinPlatz)
        assertTrue(b.liste(alle = true).isEmpty())
    }

    @Test fun ergebnisUeberlebtNeustartUndBleibtBisZurBestaetigung() {
        val b = buch()
        val v = (b.annehmen("Recherche", "Thema X") { true } as Annahme.Neu).vorgang
        b.gestartet(v.id)
        b.schritt(v.id, "sucht im Internet")
        jetzt += 600_000
        b.beende(v.id, Vorgang.FERTIG, "Recherche – Thema X – 17.10. 12.00", null)
        // Neuer Prozess: dieselbe Datei, neues Buch.
        val nachNeustart = buch()
        val gelesen = nachNeustart.finde(v.id)!!
        assertEquals(Vorgang.FERTIG, gelesen.stand)
        assertEquals("Recherche – Thema X – 17.10. 12.00", gelesen.ergebnis)
        // Abrufen ist nicht mitteilen: der Vorgang bleibt in der Liste der neuen.
        nachNeustart.abgerufen(listOf(v.id))
        assertTrue(nachNeustart.liste().any { it.id == v.id })
        assertTrue(nachNeustart.finde(v.id)!!.abgerufen > 0)
        // Wiederholter Abruf ändert nichts und startet nichts.
        nachNeustart.abgerufen(listOf(v.id))
        assertEquals(1, nachNeustart.liste(alle = true).size)
        assertEquals(listOf(v.id), nachNeustart.mitgeteilt(listOf(v.id)).map { it.id })
        assertFalse(nachNeustart.liste().any { it.id == v.id })
        assertTrue(nachNeustart.liste(alle = true).any { it.id == v.id })
    }

    @Test fun offenerLaufEinesFrueherenProzessesGiltAlsAbgebrochen() {
        val b = buch()
        val v = (b.annehmen("Recherche", "Thema Y") { true } as Annahme.Neu).vorgang
        b.gestartet(v.id)
        val neu = buch()
        val gelesen = neu.finde(v.id)!!
        assertEquals(Vorgang.ABGEBROCHEN, gelesen.stand)
        assertTrue(gelesen.grund.contains("neu gestartet"))
        // Ein spätes „läuft“ aus dem alten Lauf überschreibt das Ende nicht.
        neu.schritt(v.id, "spät")
        assertEquals(Vorgang.ABGEBROCHEN, neu.finde(v.id)!!.stand)
        // Nach einem Abbruch darf derselbe Auftrag neu angenommen werden.
        assertTrue(neu.annehmen("Recherche", "Thema Y") { true } is Annahme.Neu)
    }

    @Test fun nieAngelaufenerAuftragGiltNachDreiMinutenAlsGescheitert() {
        val b = buch()
        val v = (b.annehmen("Recherche", "Thema Z") { true } as Annahme.Neu).vorgang
        jetzt += VorgangsBuch.START_MS + 1
        assertEquals(Vorgang.FEHLGESCHLAGEN, b.finde(v.id)!!.stand)
    }

    @Test fun mitgeteiltBestaetigtKeineOffenenVorgaenge() {
        val b = buch()
        val v = (b.annehmen("Recherche", "läuft noch") { true } as Annahme.Neu).vorgang
        assertTrue(b.mitgeteilt(listOf(v.id, "VXXXX")).isEmpty())
        assertTrue(b.finde(v.id)!!.offen)
    }

    // Ausgabeformat und Ablage (Auftrag 9FGNN): Ein PDF entsteht nur aus dem ausdrücklichen Schalter, nie aus einer Wortsuche im Auftrag.

    @Test fun normaleRechercheOhneWunschIstTextOhneAblage() {
        val v = (buch().annehmen("Recherche", "Recherchiere die besten Hochbeet-Erden") { true } as Annahme.Neu).vorgang
        assertFalse(v.pdf)
        assertFalse(v.ablage)
    }

    @Test fun verneintesPdfImAuftragErzeugtKeinPdf() {
        listOf("Kein PDF erforderlich, nur Text.", "Bitte nicht als PDF", "Analysiere PDF_WUNSCH in Agenten.kt, nur Text").forEach { auftrag ->
            val v = (buch().annehmen("Code-Analyse", auftrag) { true } as Annahme.Neu).vorgang
            assertFalse(auftrag, v.pdf)
        }
    }

    @Test fun pdfWunschOhneAblageUndAblageOhnePdfSindUnabhaengig() {
        val b = buch()
        val nurPdf = (b.annehmen("Recherche", "Thema als PDF", ablage = false, pdf = true) { true } as Annahme.Neu).vorgang
        assertTrue(nurPdf.pdf); assertFalse(nurPdf.ablage)
        val nurText = (b.annehmen("Recherche", "Recherchiere und lege das als Text in der Ablage ab", ablage = true, pdf = false) { true } as Annahme.Neu).vorgang
        assertFalse(nurText.pdf); assertTrue(nurText.ablage)
        val beides = (b.annehmen("Recherche", "Recherche als PDF in die Ablage", ablage = true, pdf = true) { true } as Annahme.Neu).vorgang
        assertTrue(beides.pdf); assertTrue(beides.ablage)
        // Nach einem Neustart (neu gelesen) bleiben die Wünsche erhalten.
        val neu = VorgangsBuch(datei, uhr = { jetzt })
        assertTrue(neu.finde(nurPdf.id)!!.pdf); assertFalse(neu.finde(nurPdf.id)!!.ablage)
        assertFalse(neu.finde(nurText.id)!!.pdf); assertTrue(neu.finde(nurText.id)!!.ablage)
        assertTrue(neu.finde(beides.id)!!.pdf); assertTrue(neu.finde(beides.id)!!.ablage)
    }

    @Test fun alterEintragOhnePdfFeldIstKeinPdf() {
        val alt = org.json.JSONObject().put("id", "A1B2C").put("agent", "Recherche").put("auftrag", "als PDF").put("erstellt", 1L).put("stand", Vorgang.FERTIG)
        assertFalse(Vorgang.aus(alt).pdf)
    }
}
