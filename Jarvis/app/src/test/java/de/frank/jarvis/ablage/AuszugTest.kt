package de.frank.jarvis.ablage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuszugTest {
    private fun probe(name: String): File = File(javaClass.classLoader!!.getResource("ablage/$name")!!.toURI())

    @Test fun docxTextauszug() {
        val text = Auszug.office(probe("bericht.docx"), "docx")
        assertNotNull(text)
        assertTrue(text!!.contains("Überschrift Bericht"))
        assertTrue(text.contains("Umlauten äöü & Zeichen."))
        assertEquals(2, text.lines().count { it.isNotBlank() })
    }

    @Test fun keineOfficeDateiErgibtNull() {
        assertNull(Auszug.office(probe("unbekannt.xyz"), "docx"))
    }

    @Test fun csvMitSemikolonUndAnfuehrungszeichen() {
        val t = Auszug.tabelle(probe("daten.csv").readText(), tsv = false)
        assertEquals(listOf("Name", "Wert", "Notiz"), t.zeilen[0])
        assertEquals(listOf("HRV", "43", "mit ; Semikolon"), t.zeilen[1])
        assertEquals(listOf("Puls", "140", "Zeile mit \"Anführung\""), t.zeilen[2])
        assertFalse(t.gekuerzt)
    }

    @Test fun grosseTabelleWirdNurInDerVorschauGekuerzt() {
        val t = Auszug.tabelle((1..2000).joinToString("\n") { "a$it,b$it" }, tsv = false, maxZeilen = 500)
        assertEquals(500, t.zeilen.size)
        assertTrue(t.gekuerzt)
    }

    @Test fun jsonHuebschUndHervorgehoben() {
        val j = Auszug.jsonHuebsch(probe("einstellungen.json").readText())!!
        assertTrue(j.contains("\n"))
        val b = Auszug.hervorheben(j, "json")
        assertTrue(b.any { it.klasse == Auszug.Klasse.MARKE && j.substring(it.von, it.bis) == "\"modell\"" })
        assertTrue(b.any { it.klasse == Auszug.Klasse.SCHLUESSELWORT && j.substring(it.von, it.bis) == "true" })
    }

    @Test fun adresseInZeichenketteIstKeinKommentar() {
        val code = "val url = \"https://example.com\" // echter Kommentar"
        val b = Auszug.hervorheben(code, "kt")
        val kommentar = b.single { it.klasse == Auszug.Klasse.KOMMENTAR }
        assertEquals("// echter Kommentar", code.substring(kommentar.von, kommentar.bis))
    }

    @Test fun langeDateiWirdFuerDieVorschauGekuerzt() {
        val f = File.createTempFile("lang", ".txt").apply { writeText("Zeile\n".repeat(200_000)) }
        val v = Auszug.lies(f, 1000)
        assertTrue(v.gekuerzt)
        assertTrue(v.text.length <= 1000)
        assertEquals(1_200_000L, f.length())
        f.delete()
    }

    @Test fun sichererDateiname() {
        assertEquals("b.png", AblageSpeicher.sichererName("../../a/b", "png"))
        assertEquals("Datei.pdf", AblageSpeicher.sichererName("", "pdf"))
        assertEquals("foto.jpeg", AblageSpeicher.sichererName("foto.jpeg", "jpg"))
        assertEquals("x.png", AblageSpeicher.sichererName("x.jpg", "png"))
    }
}
