package de.frank.jarvis.ui

import de.frank.jarvis.data.Sitzung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GespraechsNavigationTest {

    private val jetzt = 1_800_000_000_000L
    private fun lage(id: String, titel: String, app: String = "", stand: String = "", vorMin: Long = 1, auftrag: String = "") =
        SessionLage(Sitzung(id, titel, jetzt - 86_400_000L, jetzt - vorMin * 60_000L, 4), app, stand, jetzt - vorMin * 60_000L, auftrag)

    private val keinInhalt: (SessionLage) -> String = { "" }

    @Test fun andereProgrammierSessionWirdErkannt() {
        for (satz in listOf(
            "Geh in die andere Programmier-Session",
            "Ich möchte in die andere Programmier-Session",
            "Jarvis, wechsle in die andere Programmiersession",
            "Hey Jarvis, geh bitte in die andere Programmier Session",
        )) {
            val b = GespraechsNavigation.erkenne(satz)
            assertTrue(satz, b is NaviBefehl.Andere)
            assertTrue(satz, (b as NaviBefehl.Andere).programmiert)
            assertTrue(satz, b.stichworte.isEmpty())
        }
    }

    @Test fun themaUndZurueckWerdenErkannt() {
        val whoop = GespraechsNavigation.erkenne("Geh in die Whoop-Session")
        assertTrue(whoop is NaviBefehl.Thema)
        assertEquals(listOf("whoop"), (whoop as NaviBefehl.Thema).stichworte)
        assertTrue(GespraechsNavigation.erkenne("Ich will zur Whoop-Session") is NaviBefehl.Thema)
        assertTrue(GespraechsNavigation.erkenne("Gehe in die letzte Session zurück") is NaviBefehl.Zurueck)
        val letzteProg = GespraechsNavigation.erkenne("Wechsel zur letzten Programmier-Session")
        assertTrue(letzteProg is NaviBefehl.Zurueck && letzteProg.programmiert)
        assertTrue(GespraechsNavigation.erkenne("Öffne eine neue Session") is NaviBefehl.Neu)
    }

    @Test fun keinNavigationsbefehlFuerAuftraegeUndFragen() {
        for (satz in listOf(
            "Starte einen Programmierauftrag für Jarvis, der die Session-Navigation verbessert",
            "Bau in Jarvis eine Funktion ein, die die andere Session anzeigt",
            "Was war in der anderen Programmier-Session?",
            "Ist die andere Session schon fertig?",
            "Die andere Session hat das schon gemacht",
            "Sprachsession beenden",
        )) assertNull(satz, GespraechsNavigation.erkenne(satz))
    }

    @Test fun zweiLaufendeWechselnJeweilsZurAnderen() {
        val a = lage("a", "Session-Navigation verbessern", "Jarvis", "laeuft")
        val b = lage("b", "Whoop-Abruf reparieren", "Entropie Reductor", "frage", vorMin = 30)
        val lagen = listOf(a, b, lage("c", "Wetter morgen"))
        val befehl = GespraechsNavigation.erkenne("Geh in die andere Programmier-Session") as NaviBefehl.Andere
        val vonA = GespraechsNavigation.andereZiel(befehl, lagen, "a", "", keinInhalt, jetzt)
        assertEquals("b", (vonA as NaviZiel.Oeffnen).lage.sitzung.id)
        val vonB = GespraechsNavigation.andereZiel(befehl, lagen, "b", "", keinInhalt, jetzt)
        assertEquals("a", (vonB as NaviZiel.Oeffnen).lage.sitzung.id)
    }

    @Test fun abgeschlosseneUndVerwaisteSessionsZaehlenNicht() {
        val lagen = listOf(
            lage("a", "Aktuell", "Jarvis", "laeuft"),
            lage("b", "Fertig", "Jarvis", "fertig"),
            lage("c", "Problem", "Aufgaben", "fehler"),
            lage("d", "Liegen geblieben", "Wecker", "laeuft", vorMin = 7 * 60),
            lage("e", "Offen", "Geniale Ideen", "laeuft", vorMin = 5),
        )
        val befehl = NaviBefehl.Andere(emptyList(), true)
        assertEquals("e", (GespraechsNavigation.andereZiel(befehl, lagen, "a", "", keinInhalt, jetzt) as NaviZiel.Oeffnen).lage.sitzung.id)
        // Statusänderung: e meldet fertig, dann gibt es keine andere laufende mehr, und es wird nichts geöffnet.
        val danach = lagen.map { if (it.sitzung.id == "e") it.copy(stand = "fertig") else it }
        assertTrue(GespraechsNavigation.andereZiel(befehl, danach, "a", "", keinInhalt, jetzt) is NaviZiel.Keins)
    }

    @Test fun mehrereLaufendeNurBeiEchterMehrdeutigkeitNachfragen() {
        val lagen = listOf(
            lage("a", "Aktuell", "Jarvis", "laeuft"),
            lage("b", "Whoop-Abruf", "Entropie Reductor", "laeuft", auftrag = "Whoop Token erneuern"),
            lage("c", "Uhrzeiten", "Geniale Aufgaben", "laeuft", auftrag = "Uhrzeit 21 Uhr"),
        )
        val ohne = GespraechsNavigation.andereZiel(NaviBefehl.Andere(emptyList(), true), lagen, "a", "", keinInhalt, jetzt)
        assertEquals(2, (ohne as NaviZiel.Rueckfrage).kandidaten.size)
        val mitThema = GespraechsNavigation.erkenne("Geh in die andere Whoop-Programmier-Session") as NaviBefehl.Andere
        assertEquals("b", (GespraechsNavigation.andereZiel(mitThema, lagen, "a", "", keinInhalt, jetzt) as NaviZiel.Oeffnen).lage.sitzung.id)
        val ausKontext = GespraechsNavigation.andereZiel(NaviBefehl.Andere(emptyList(), true), lagen, "a", "Was macht eigentlich Geniale Aufgaben?", keinInhalt, jetzt)
        assertEquals("c", (ausKontext as NaviZiel.Oeffnen).lage.sitzung.id)
        // Antwort auf die Rückfrage über die App.
        val wahl = GespraechsNavigation.waehle("die Entropie", ohne.kandidaten)
        assertEquals("b", (wahl as NaviWahl.Treffer).sitzung.id)
    }

    @Test fun themenwechselBevorzugtLaufendeProgrammierSession() {
        val lagen = listOf(
            lage("a", "Wetter", vorMin = 1),
            lage("alt", "Whoop Schlafdaten besprochen", vorMin = 600),
            lage("neu", "Abruf reparieren", "Entropie Reductor", "laeuft", auftrag = "Whoop Anmeldung erneuern"),
        )
        val whoop = GespraechsNavigation.erkenne("Geh in die Whoop-Session, wo wir programmieren") as NaviBefehl.Thema
        assertEquals(listOf("neu"), GespraechsNavigation.themaZiel(whoop, lagen, "a", keinInhalt, jetzt).map { it.sitzung.id })
        val ohneProg = GespraechsNavigation.erkenne("Geh in die Whoop-Session") as NaviBefehl.Thema
        assertEquals(setOf("alt", "neu"), GespraechsNavigation.themaZiel(ohneProg, lagen, "a", keinInhalt, jetzt).map { it.sitzung.id }.toSet())
    }

    @Test fun zurueckZurLetztenSession() {
        val lagen = listOf(lage("a", "Jetzt"), lage("b", "Vorher", vorMin = 50), lage("c", "Neuer", vorMin = 2))
        assertEquals("b", GespraechsNavigation.zurueckZiel(NaviBefehl.Zurueck(), lagen, "a", "b", jetzt)?.sitzung?.id)
        assertEquals("c", GespraechsNavigation.zurueckZiel(NaviBefehl.Zurueck(), lagen, "a", null, jetzt)?.sitzung?.id)
    }

    // ---- GPT-Live: Werkzeugaufrufe, die Sessionbefehle sind ----

    @Test fun liveBenannterWechselOeffnetDieSession() {
        val lagen = listOf(lage("a", "Wetter morgen"), lage("w", "Whoop-Abruf reparieren", vorMin = 90), lage("k", "Kalender aufräumen", vorMin = 30))
        for (auftrag in listOf("Wechsle in die Whoop-Session", "Frank möchte in die Whoop-Session wechseln", "Jarvis, öffne bitte die Whoop-Session")) {
            val text = GespraechsNavigation.liveBefehl(auftrag, null, null)
            assertEquals(auftrag, auftrag, text)
            val b = GespraechsNavigation.erkenne(text!!) as NaviBefehl.Thema
            assertEquals(auftrag, listOf("whoop"), b.stichworte)
            assertEquals(auftrag, listOf("w"), GespraechsNavigation.themaZiel(b, lagen, "a", keinInhalt, jetzt).map { it.sitzung.id })
        }
    }

    @Test fun liveUmformulierterAuftragNimmtFranksWortlaut() {
        assertEquals("Wechsle in die Whoop-Session",
            GespraechsNavigation.liveBefehl("Frank fragt nach der Whoop-Unterhaltung", "Wechsle in die Whoop-Session", null))
    }

    @Test fun liveUnbekannterNameFindetNichts() {
        val lagen = listOf(lage("a", "Wetter morgen"), lage("w", "Whoop-Abruf reparieren"))
        val b = GespraechsNavigation.erkenne(GespraechsNavigation.liveBefehl("Wechsle in die Kochrezept-Session", null, null)!!) as NaviBefehl.Thema
        assertTrue(GespraechsNavigation.themaZiel(b, lagen, "a", keinInhalt, jetzt).isEmpty())
    }

    @Test fun liveMehrdeutigerNameFragtNachUndNimmtDieAuswahl() {
        val lagen = listOf(lage("a", "Wetter morgen"), lage("s", "Whoop Schlafdaten", vorMin = 60), lage("t", "Whoop Training", vorMin = 120))
        val b = GespraechsNavigation.erkenne("Wechsle in die Whoop-Session") as NaviBefehl.Thema
        val kandidaten = GespraechsNavigation.themaZiel(b, lagen, "a", keinInhalt, jetzt)
        assertEquals(setOf("s", "t"), kandidaten.map { it.sitzung.id }.toSet())
        // Franks Antwort auf die Rückfrage kommt im Live-Gespräch als neuer Werkzeugaufruf.
        val antwort = GespraechsNavigation.liveBefehl("Frank meint die zweite", null, kandidaten)
        assertEquals("Frank meint die zweite", antwort)
        assertEquals(kandidaten[1].sitzung.id, (GespraechsNavigation.waehle(antwort!!, kandidaten) as NaviWahl.Treffer).sitzung.id)
        assertTrue(GespraechsNavigation.waehle("Nein, abbrechen", kandidaten) is NaviWahl.Abbruch)
        // Eine andere Frage statt einer Auswahl bleibt ein normaler Werkzeugaufruf.
        assertNull(GespraechsNavigation.liveBefehl("Wie wird das Wetter morgen?", null, kandidaten))
    }

    @Test fun liveNormaleWerkzeugaufrufeBleibenBeiJarvis() {
        for (auftrag in listOf(
            "Wie sind meine Whoop-Werte heute?",
            "Stell einen Timer auf zehn Minuten",
            "Was steht morgen in meinem Kalender?",
            "Starte einen Programmierauftrag für Jarvis, der die Session-Navigation verbessert",
            "Was war in der anderen Programmier-Session?",
        )) {
            assertNull(auftrag, GespraechsNavigation.liveBefehl(auftrag, null, null))
            // Auch Franks Wortlaut macht aus einem Auftrag keinen Sessionwechsel.
            assertNull(auftrag, GespraechsNavigation.liveBefehl(auftrag, auftrag, null))
        }
        assertNull(GespraechsNavigation.liveBefehl("", null, null))
    }

    @Test fun mikrofonpfadErkenntSessionbefehleUnveraendert() {
        assertTrue(GespraechsNavigation.erkenne("Wechsle in die Whoop-Session") is NaviBefehl.Thema)
        assertTrue(GespraechsNavigation.erkenne("Geh zurück in die letzte Session") is NaviBefehl.Zurueck)
        assertTrue(GespraechsNavigation.erkenne("Öffne eine neue Session") is NaviBefehl.Neu)
        assertNull(GespraechsNavigation.erkenne("Wie sind meine Whoop-Werte heute?"))
    }
}
