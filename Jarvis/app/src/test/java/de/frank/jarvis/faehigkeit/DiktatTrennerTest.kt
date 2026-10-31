package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.DiktatTrenner.TRENNER
import de.frank.jarvis.faehigkeit.DiktatTrenner.TRENNER_ALT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiktatTrennerTest {
    @Test fun einDiktatEndetDirektMitLeerzeichenSemikolonLeerzeichen() {
        assertEquals("Hallo Jarvis ; ", DiktatTrenner.anhaengen("", " Hallo Jarvis \n"))
        assertFalse(DiktatTrenner.anhaengen("", "Hallo Jarvis").contains('\n'))
    }

    @Test fun mehrereDiktateBleibenInlineGetrennt() {
        val drei = DiktatTrenner.anhaengen(DiktatTrenner.anhaengen(DiktatTrenner.anhaengen("", "Erstens."), "Zweitens"), "Drittens")
        assertEquals("Erstens. ; Zweitens ; Drittens ; ", drei)
        assertFalse(drei.contains('\n'))
    }

    @Test fun leereBeitraegeAendernNichts() {
        val e = "Erstens$TRENNER"
        assertEquals(e, DiktatTrenner.anhaengen(e, ""))
        assertEquals(e, DiktatTrenner.anhaengen(e, "  "))
        assertEquals(e, DiktatTrenner.anhaengen(e, ";"))
    }

    @Test fun getipptesBleibtUndKeinDoppelterTrenner() {
        val vorspann = ProgrammiererVorlage.einsetzen("")
        assertEquals("${vorspann.trimEnd()} Filter einbauen$TRENNER", DiktatTrenner.anhaengen(vorspann, "Filter einbauen"))
        assertEquals("Text ; Neu ; ", DiktatTrenner.anhaengen("Text ; ", "Neu"))
        assertEquals("Text ; Neu ; ", DiktatTrenner.anhaengen("Text ; ; ", "Neu"))
        // Ein Semikolon direkt am Wort ist kein Trenner: Der nächste Beitrag bekommt seinen eigenen.
        assertEquals("val x = 1; Neu ; ", DiktatTrenner.anhaengen("val x = 1;", "Neu"))
    }

    @Test fun alterMehrzeiligerTrennerWirdInlineFortgesetzt() {
        assertEquals("Eins ; Zwei ; ", DiktatTrenner.anhaengen("Eins$TRENNER_ALT", "Zwei"))
        assertEquals("Zwei ; ", DiktatTrenner.anhaengen(";\n\n", "Zwei"))
    }

    @Test fun abschliessenderTrennerGehtNichtMit() {
        assertEquals("Eins", DiktatTrenner.ohneSchluss("Eins$TRENNER"))
        assertEquals("Eins ; Zwei", DiktatTrenner.ohneSchluss("Eins${TRENNER}Zwei$TRENNER"))
        assertEquals("Eins", DiktatTrenner.ohneSchluss("Eins$TRENNER_ALT"))
        assertEquals("for (;;) x", DiktatTrenner.ohneSchluss("for (;;) x"))
        assertEquals("", DiktatTrenner.ohneSchluss(" ; "))
    }

    /** Was in der Blase steht, ist (ohne Schlusstrenner) genau das, was abgeschickt wird. */
    @Test fun darstellungUndGesendeterTextStimmen() {
        val entwurf = DiktatTrenner.anhaengen(DiktatTrenner.anhaengen("", "Knopf grün"), "Text größer")
        assertEquals("Knopf grün ; Text größer ; ", entwurf)
        assertEquals("Knopf grün ; Text größer", DiktatTrenner.ohneSchluss(entwurf))
    }

    @Test fun einzelneAufgabeMitSchlusstrennerBleibtEine() {
        assertFalse(DiktatTrenner.mehrteilig("Knopf grün machen$TRENNER"))
        assertEquals("Knopf grün machen", DiktatTrenner.auftrag("Knopf grün machen$TRENNER"))
    }

    @Test fun dreiAufgabenEinAuftrag() {
        val roh = "Aufgabe eins ; Aufgabe zwei ; Aufgabe drei ; "
        assertTrue(DiktatTrenner.mehrteilig(roh))
        assertEquals(listOf("Aufgabe eins", "Aufgabe zwei", "Aufgabe drei"), DiktatTrenner.abschnitte(roh))
        assertEquals("Aufgabe eins ; Aufgabe zwei ; Aufgabe drei", DiktatTrenner.auftrag(roh))
    }

    @Test fun alteZeilentrennerWerdenKompatibelErkannt() {
        val roh = "Aufgabe eins\n;\nAufgabe zwei\n\n  ;  \n\nAufgabe drei$TRENNER_ALT;\n"
        assertEquals(listOf("Aufgabe eins", "Aufgabe zwei", "Aufgabe drei"), DiktatTrenner.abschnitte(roh))
        assertEquals("Aufgabe eins ; Aufgabe zwei ; Aufgabe drei", DiktatTrenner.auftrag(roh))
        // Gemischt: alter Entwurf, danach neue Diktate.
        assertEquals("Alt ; Neu", DiktatTrenner.auftrag("Alt${TRENNER_ALT}Neu ; "))
    }

    @Test fun codeMitSemikolaKommtUnveraendertWiederZusammen() {
        val text = "Aufgabe eins ; Aufgabe zwei mit for (i = 0 ; i < n ; i++)"
        assertEquals(text, DiktatTrenner.auftrag(text))
        assertEquals("val x = 1; danach bauen", DiktatTrenner.auftrag("val x = 1; danach bauen"))
        // Zeilen in Code bleiben erhalten (kein Zusammenziehen ohne alte Trenner oder leere Abschnitte).
        assertEquals("SELECT a FROM t ;\n-- Kommentar", DiktatTrenner.auftrag("SELECT a FROM t ;\n-- Kommentar"))
    }

    @Test fun ohneTrennerUnveraendert() {
        val text = "Baue einen Filter ein.\nDer Knopf soll grün sein; danach bauen."
        assertFalse(DiktatTrenner.mehrteilig(text))
        assertEquals(text, DiktatTrenner.auftrag("  $text  "))
    }

    @Test fun leereAbschnitteFallenWeg() {
        assertEquals("Eins ; Zwei", DiktatTrenner.auftrag(" ; Eins ;  ; Zwei ; "))
        assertEquals("Eins ; Zwei", DiktatTrenner.auftrag(";\n\nEins$TRENNER_ALT;$TRENNER_ALT  \n;\nZwei"))
        assertEquals("Knopf grün", DiktatTrenner.auftrag(";\n\nKnopf grün\n;\n"))
        assertEquals("Eins ; })", DiktatTrenner.auftrag("Eins\n;\n})"))
    }

    @Test fun gespeichertBehaeltDenSchluss() {
        assertEquals("Eins ; Zwei ; ", DiktatTrenner.gespeichert("  Eins ; Zwei ;  "))
        assertEquals("Eins ; ", DiktatTrenner.gespeichert("Eins\n\n;\n\n"))
        assertEquals("Eins", DiktatTrenner.gespeichert(" Eins "))
        assertEquals("x;", DiktatTrenner.gespeichert("x;"))
        assertEquals("", DiktatTrenner.gespeichert(" ; "))
    }

    @Test fun kiFassungBekommtDenSchlussDerVorlage() {
        assertEquals("Neu ; ", DiktatTrenner.mitSchlussWie("Alt ; ", "Neu"))
        assertEquals("Neu ; ", DiktatTrenner.mitSchlussWie("Alt ; ", "Neu ;"))
        assertEquals("Neu", DiktatTrenner.mitSchlussWie("Alt", "Neu"))
    }

    @Test fun kiVerbesserungBehaeltJedeAufgabengrenze() = kotlinx.coroutines.runBlocking {
        val neu = DiktatTrenner.jeAbschnitt("erste aufgabe ; zweite aufgabe ; dritte aufgabe ; ") { t, _ -> "Neu: $t ; " }
        assertEquals("Neu: erste aufgabe ; Neu: zweite aufgabe ; Neu: dritte aufgabe ; ", neu.text)
        assertEquals(0, neu.ausgefallen)
    }

    @Test fun kiAntwortMitTrennerMachtKeineNeueAufgabe() = kotlinx.coroutines.runBlocking {
        assertEquals("x; y ; b", DiktatTrenner.jeAbschnitt("a ; b") { t, _ -> if (t == "a") "x ; y" else "" }.text)
        assertEquals("", DiktatTrenner.jeAbschnitt("a ; b") { _, _ -> "" }.text)
        for (antwort in listOf("x ;\ny", "x\n;\ny", "x ; ; y")) assertEquals(2, DiktatTrenner.abschnitte(DiktatTrenner.jeAbschnitt("a ; b") { t, _ -> if (t == "a") antwort else "b" }.text).size)
    }

    @Test fun kiFehlerInEinemAbschnittBehaeltDieUebrigen() = kotlinx.coroutines.runBlocking {
        val neu = DiktatTrenner.jeAbschnitt("a ; b") { t, _ -> if (t == "b") throw IllegalStateException("Netz") else "A" }
        assertEquals("A ; b", neu.text)
        assertEquals(1, neu.ausgefallen)
    }

    @Test fun kiBekommtGesamttextNurBeiMehrerenAbschnitten() = kotlinx.coroutines.runBlocking {
        var kontext: String? = "x"
        DiktatTrenner.jeAbschnitt("a") { t, g -> kontext = g; t }
        assertEquals(null, kontext)
        DiktatTrenner.jeAbschnitt("a ; b ; ") { t, g -> kontext = g; t }
        assertEquals("a ; b", kontext)
    }
}
