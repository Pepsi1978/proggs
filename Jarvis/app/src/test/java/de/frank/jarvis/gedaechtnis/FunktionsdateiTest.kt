package de.frank.jarvis.gedaechtnis

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunktionsdateiTest {
    private val text = """
        ## Alltag

        - Timer (`timer`): klingeln wie ein Wecker.
        - Stoppuhr (`stoppuhr`) mit Benachrichtigung.

        ## Sprache

        - Vorlesen mit Edge und Qwen.
    """.trimIndent() + "\n"

    private fun op(aktion: String, bereich: String = "", alt: String = "", neu: String = "", beleg: String = "Version 1.8.200") =
        FunktionsOperation(aktion, bereich, alt, neu, beleg)

    private fun zaehle(t: String, teil: String) = t.lines().count { it.contains(teil) }

    // ---- Erstbefüllung und Versionsstand

    @Test fun erstbefuellungLiestGeprueftenStandUndEntferntDieMarke() {
        val (code, name, inhalt) = Funktionsdatei.erstbefuellung("<!-- stand: 197 1.8.186 -->\n## Alltag\n\n- Timer")
        assertEquals(197, code)
        assertEquals("1.8.186", name)
        assertTrue(inhalt.startsWith("## Alltag"))
    }

    @Test fun ohneMarkeIstDerStandUnbekannt() {
        val (code, _, inhalt) = Funktionsdatei.erstbefuellung("## Alltag\n- Timer")
        assertEquals(0, code)
        assertTrue(inhalt.contains("Timer"))
    }

    @Test fun nurEineNeuereInstallierteVersionLoestPflegeAus() {
        // Ein angenommener oder fertig gemeldeter Auftrag ändert die installierte Version nicht: keine Pflege.
        assertFalse(Funktionsdatei.pflegeFaellig(geprueft = 197, installiert = 197))
        assertFalse(Funktionsdatei.pflegeFaellig(geprueft = 198, installiert = 197))
        assertTrue(Funktionsdatei.pflegeFaellig(geprueft = 197, installiert = 199))
    }

    @Test fun offeneVersionenSindGenauDieInstalliertenUeberDemGeprueftenStand() {
        val log = (195..201).map { Versionseintrag(it, "1.8.$it", "18.10.2026, 20:00 Uhr", "Notiz $it") }
        assertEquals(listOf(198, 199), Funktionsdatei.offeneVersionen(log, geprueft = 197, installiert = 199).map { it.code })
        assertTrue(Funktionsdatei.offeneVersionen(log, geprueft = 199, installiert = 199).isEmpty())
    }

    @Test fun versionslogWirdGelesenUndKaputtesLogErgibtNichts() {
        val json = JSONObject().put("format", 1).put("eintraege", JSONArray()
            .put(JSONObject().put("versionCode", 196).put("versionName", "1.8.185").put("stand", "x").put("notiz", "A"))
            .put(JSONObject().put("versionCode", 197).put("versionName", "1.8.186").put("stand", "y").put("notiz", "B"))).toString()
        assertEquals(listOf(196, 197), Funktionsdatei.versionslog(json).map { it.code })
        assertTrue(Funktionsdatei.versionslog("kein json").isEmpty())
    }

    // ---- Pflege-Operationen

    @Test fun neueFunktionWirdEinmalImPassendenBereichErgaenzt() {
        val r = Funktionsdatei.wende(text, listOf(op("neu", "Alltag", neu = "Erinnerungen per Sprache (`erinnern`).")))
        val zeilen = r.text.lines()
        val i = zeilen.indexOfFirst { it.contains("Erinnerungen per Sprache") }
        assertTrue(i > zeilen.indexOf("## Alltag") && i < zeilen.indexOf("## Sprache"))
        assertEquals(1, r.angewandt.size)
        // Dieselbe Funktion ein zweites Mal: keine Dopplung.
        val zweimal = Funktionsdatei.wende(r.text, listOf(op("neu", "Alltag", neu = "- erinnerungen per sprache (`erinnern`)")))
        assertEquals(1, zaehle(zweimal.text, "Erinnerungen per Sprache"))
        assertTrue(zweimal.angewandt.isEmpty())
    }

    @Test fun neuerBereichEntstehtWennKeinerPasst() {
        val r = Funktionsdatei.wende(text, listOf(op("neu", "Fotos", neu = "Fotos beschreiben.")))
        assertTrue(r.text.contains("## Fotos\n\n- Fotos beschreiben."))
    }

    @Test fun geaenderteFunktionWirdErsetztStattAngehaengt() {
        val r = Funktionsdatei.wende(text, listOf(op("ersetzen", alt = "Vorlesen mit Edge und Qwen.", neu = "Vorlesen mit Edge, Qwen und Google Chirp.")))
        assertEquals(0, zaehle(r.text, "Vorlesen mit Edge und Qwen"))
        assertEquals(1, zaehle(r.text, "Vorlesen mit Edge, Qwen und Google Chirp."))
        assertEquals(text.lines().size, r.text.lines().size)
    }

    @Test fun ersetzenAufSchonVorhandeneFassungLaesstNurEineZeileStehen() {
        val r = Funktionsdatei.wende(text, listOf(op("ersetzen", alt = "Timer (`timer`): klingeln wie ein Wecker.", neu = "Stoppuhr (`stoppuhr`) mit Benachrichtigung.")))
        assertEquals(0, zaehle(r.text, "Timer (`timer`)"))
        assertEquals(1, zaehle(r.text, "Stoppuhr"))
    }

    @Test fun entfernteFunktionWirdGestrichenUndLeererBereichFaelltWeg() {
        val r = Funktionsdatei.wende(text, listOf(op("entfernen", alt = "- Vorlesen mit Edge und Qwen.")))
        assertFalse(r.text.contains("Vorlesen"))
        assertFalse(r.text.contains("## Sprache"))
        assertTrue(r.text.contains("## Alltag"))
    }

    @Test fun vonHandGeaenderteAnkerzeileWirdNichtUeberschrieben() {
        // Frank hat die Zeile inzwischen umformuliert; die Operation des Modells bezog sich auf die alte Fassung.
        val vonHand = text.replace("Vorlesen mit Edge und Qwen.", "Vorlesen mit Edge, Qwen und meiner eigenen Stimme.")
        val r = Funktionsdatei.wende(vonHand, listOf(op("ersetzen", alt = "Vorlesen mit Edge und Qwen.", neu = "Vorlesen mit Edge.")))
        assertEquals(vonHand.trim(), r.text.trim())
        assertEquals(1, r.uebersprungen.size)
    }

    @Test fun franksEigeneZeilenBleibenUndWerdenNurVorgeschlagen() {
        val manuelle = setOf(Funktionsdatei.normal("- Vorlesen mit Edge und Qwen."))
        val r = Funktionsdatei.wende(text, listOf(
            op("ersetzen", alt = "Vorlesen mit Edge und Qwen.", neu = "Vorlesen mit Edge."),
            op("entfernen", alt = "Vorlesen mit Edge und Qwen."),
        ), manuelle)
        assertEquals(text.trim(), r.text.trim())
        assertEquals(2, r.hinweise.size)
        assertTrue(r.hinweise.first().contains("Vorlesen mit Edge."))
    }

    @Test fun manuelleZeilenWerdenErkanntUndVerschwindenMitDemText() {
        val neu = text.replace("- Vorlesen mit Edge und Qwen.", "- Vorlesen mit Edge, Qwen und Chirp.") + "\n- Eigene Notiz.\n"
        val manuelle = Funktionsdatei.manuelleZeilenNach(text, neu, emptySet())
        assertEquals(setOf(Funktionsdatei.normal("Vorlesen mit Edge, Qwen und Chirp."), Funktionsdatei.normal("Eigene Notiz.")), manuelle)
        // Streicht Frank die Zeile später wieder, ist sie auch nicht mehr geschützt.
        val danach = Funktionsdatei.manuelleZeilenNach(neu, neu.replace("- Eigene Notiz.\n", ""), manuelle)
        assertEquals(setOf(Funktionsdatei.normal("Vorlesen mit Edge, Qwen und Chirp.")), danach)
    }

    @Test fun unlesbareOderUnbekannteOperationenAendernNichts() {
        assertTrue(Funktionsdatei.operationen(null).isEmpty())
        assertTrue(Funktionsdatei.operationen(JSONObject().put("operationen", "kaputt")).isEmpty())
        val ops = Funktionsdatei.operationen(JSONObject().put("operationen", JSONArray()
            .put(JSONObject().put("aktion", "loeschen_alles").put("alt", "x"))
            .put(JSONObject().put("aktion", "NEU").put("bereich", "Alltag").put("neu", "Eine\nZeile"))))
        assertEquals(1, ops.size)
        assertEquals("neu", ops.single().aktion)
        assertEquals("Eine Zeile", ops.single().neu)
        assertEquals(text.trim(), Funktionsdatei.wende(text, emptyList()).text.trim())
    }

    @Test fun leereOderUnvollstaendigeOperationenWerdenUebersprungen() {
        val r = Funktionsdatei.wende(text, listOf(op("neu", "Alltag", neu = " "), op("ersetzen", alt = "Stoppuhr (`stoppuhr`) mit Benachrichtigung.", neu = ""), op("entfernen", alt = "")))
        assertEquals(text.trim(), r.text.trim())
        assertEquals(3, r.uebersprungen.size)
    }

    @Test fun sternPunktWirdSauberErsetzt() {
        val mitStern = text.replace("- Vorlesen mit Edge und Qwen.", "* Vorlesen - mit Edge und Qwen.")
        val r = Funktionsdatei.wende(mitStern, listOf(op("ersetzen", alt = "* Vorlesen - mit Edge und Qwen.", neu = "Vorlesen mit Edge.")))
        assertTrue(r.text.lines().contains("- Vorlesen mit Edge."))
        assertFalse(r.text.contains("Qwen"))
    }

    @Test fun bereichsnameDesModellsTrifftDenVorhandenenBereich() {
        val r = Funktionsdatei.wende(text, listOf(op("neu", "## Sprache", neu = "Diktat per Groq."), op("neu", "Sprache und Stimme", neu = "Weckwort.")))
        assertEquals(1, zaehle(r.text, "## Sprache"))
        assertFalse(r.text.contains("## ##"))
        val zeilen = r.text.lines()
        assertTrue(zeilen.indexOf("- Weckwort.") > zeilen.indexOf("## Sprache"))
    }

    @Test fun rueckgaengigMachtZurueckgeholteZeilenNichtZuFranksEigenen() {
        // So rechnet Rückgängig: nur behalten, was noch dasteht, nichts hinzufügen.
        val eigene = setOf(Funktionsdatei.normal("Stoppuhr (`stoppuhr`) mit Benachrichtigung."), Funktionsdatei.normal("verschwundene zeile"))
        assertEquals(setOf(Funktionsdatei.normal("Stoppuhr (`stoppuhr`) mit Benachrichtigung.")), Funktionsdatei.manuelleZeilenNach(text, text, eigene))
        assertTrue(Funktionsdatei.manuelleZeilenNach(text, text, emptySet()).isEmpty())
    }

    // ---- Speichern und Lesen

    @Test fun standUeberstehtSpeichernUndLaden() {
        val s = FunktionsStand(text, 4, 198, "1.8.187", 1_000L, 2_000L, setOf("eigene zeile"), listOf("Vorschlag: x"), "selbst gepflegt nach Versionslog")
        val geladen = FunktionsStand.aus(s.text, JSONObject(s.metaJson().toString()))
        assertEquals(s, geladen)
    }

    @Test fun kopfNenntOffeneVersionenUndGrenzen() {
        val s = FunktionsStand(text, 1, 197, "1.8.186", 0L, 0L, emptySet(), emptyList(), "Erstbefüllung aus dem Code")
        val kopf = Funktionsdatei.kopf(s, 199, "1.8.188", listOf(Versionseintrag(198, "1.8.187", "", "Neuer Filter")), { "Zeit" })
        assertTrue(kopf.contains("versionCode 197"))
        assertTrue(kopf.contains("NOCH NICHT EINGEARBEITET"))
        assertTrue(kopf.contains("1.8.187: Neuer Filter"))
        assertTrue(kopf.contains("nicht nachweislich fehlend"))
        assertFalse(Funktionsdatei.kopf(s, 197, "1.8.186", emptyList(), { "Zeit" }).contains("NOCH NICHT"))
    }

    @Test fun bereichLaesstSichGezieltLesen() {
        assertEquals("## Sprache\n\n- Vorlesen mit Edge und Qwen.", Funktionsdatei.bereiche(text, "sprach"))
        assertNull(Funktionsdatei.bereiche(text, "Fotos"))
    }
}
