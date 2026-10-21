package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.SuchRegel.Weg
import de.frank.jarvis.mcp.Integration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuchRegelTest {
    private val quellen by lazy {
        java.io.File("src/main/java/de/frank/jarvis").walkTopDown().filter { it.extension == "kt" }.associate { it.path to it.readText() }
    }

    // 1. Normale Frage mit aktuellem Informationsbedarf: direkte Websuche, kein Recherche-Agent
    @Test fun aktuelleVerfuegbarkeitsfrageIstKeinRechercheWunsch() {
        val frage = "Ist das neueste Modern Warfare mit DMZ in Deutschland schon spielbar?"
        assertFalse(SuchRegel.ausdruecklich(frage))
        assertFalse(SuchRegel.darfStarten("Recherche", frage))
        // Auch im Live-Gespräch ohne Satzzeichen und klein geschrieben.
        assertFalse(SuchRegel.darfStarten("recherche", "ist modern warfare mit dmz in deutschland schon spielbar"))
    }

    @Test fun internetbedarfUnsicherheitOderDauerAlleinStartenKeinenAgenten() {
        listOf(
            "Wann erscheint das nächste iPhone?",
            "Was kostet gerade ein Liter Diesel in Kassel? Ich bin mir nicht sicher.",
            "Schau mal im Internet, ob der Baumarkt heute offen hat, das kann ruhig etwas dauern.",
        ).forEach { assertFalse(it, SuchRegel.darfStarten("Recherche", it)) }
    }

    @Test fun gesuchtWirdImGespraechZuerstMitDerWebsucheDesModells() {
        assertEquals(listOf(Weg.MODELL, Weg.TAVILY), SuchRegel.wege(imAgenten = false, modell = true, tavily = true))
        // Fehlt die Anmeldung, springt Tavily ein; Agenten behalten Tavily zuerst und das Modell als Ersatz.
        assertEquals(listOf(Weg.TAVILY), SuchRegel.wege(imAgenten = false, modell = false, tavily = true))
        assertEquals(listOf(Weg.TAVILY, Weg.MODELL), SuchRegel.wege(imAgenten = true, modell = true, tavily = true))
        assertEquals(listOf(Weg.MODELL), SuchRegel.wege(imAgenten = true, modell = true, tavily = false))
    }

    @Test fun ohneDirekteSucheWirdEhrlichGemeldetStattEinesAgenten() {
        assertTrue(SuchRegel.wege(imAgenten = false, modell = false, tavily = false).isEmpty())
        assertTrue(SuchRegel.EHRLICH.contains("keinen Agenten"))
        // Die Störungsanzeige der App bekommt nur den Satz für Frank, keine Anweisung ans Modell.
        assertFalse(SuchRegel.KEINE_SUCHE.contains("Agent"))
        assertTrue(SuchRegel.NICHT_GESTARTET.contains("web_suche"))
    }

    @Test fun dasGespraechHatDieWebsucheDieTagesauswertungNicht() {
        assertTrue(SuchRegel.mitWebsuche(mitInternet = false, imGespraech = true))
        assertTrue(SuchRegel.mitWebsuche(mitInternet = true, imGespraech = false))
        assertFalse(SuchRegel.mitWebsuche(mitInternet = false, imGespraech = false))
        // Der Filter im Jarvis-Agenten nutzt genau diese Regel mit dem Gespräch (lernen) als imGespraech.
        assertTrue(quellen.values.any { it.contains("SuchRegel.mitWebsuche(mitInternet, imGespraech = lernen)") })
    }

    // 2. Ausdrückliche Aufforderung: Der Recherche-Agent startet weiterhin
    @Test fun ausdruecklicheRechercheStartetDenAgenten() {
        listOf(
            "Recherchiere ausführlich, ob das neue Modern Warfare mit DMZ in Deutschland spielbar ist.",
            "Mach mir eine Recherche zu den besten Laufschuhen für Anfänger.",
            "Lass einen Agenten nachforschen, wie man eine Sauna baut.",
            "recherchier mal gründlich die neuen Blutwerte",
        ).forEach { assertTrue(it, SuchRegel.darfStarten("Recherche", it)) }
    }

    @Test fun zustimmungAufEinAngebotZaehltAlsWunsch() {
        // Im Chat stehen Franks vorige Frage und seine Antwort auf das Angebot zeilenweise in den Worten.
        val angebot = "Laut Hersteller ja. Soll ich dazu eine ausführliche Recherche starten?"
        assertTrue(SuchRegel.darfStarten("Recherche", "Ja, mach das bitte.", vorher = "Ist DMZ in Deutschland schon spielbar?", angebot = angebot))
        assertTrue(SuchRegel.darfStarten("Recherche", "Gerne", angebot = angebot))
        // Ein „Ja“ auf eine andere Frage oder ein „Super, danke“ ist keine Zustimmung zu einem Agenten.
        assertFalse(SuchRegel.darfStarten("Recherche", "Ja", angebot = "Soll ich den Termin eintragen?"))
        assertFalse(SuchRegel.darfStarten("Recherche", "Super, danke", angebot = angebot))
        assertFalse(SuchRegel.darfStarten("Recherche", "Gerne"))
        assertFalse(SuchRegel.zustimmung("Ja, aber wann erscheint es?"))
        assertFalse(SuchRegel.darfStarten("Recherche", "Und wann erscheint das nächste Update?", vorher = "Ja"))
    }

    @Test fun verneinteRechercheStartetKeinenAgenten() {
        listOf(
            "Keine Recherche, schau nur kurz nach, ob DMZ spielbar ist.",
            "Ohne Agent bitte, nur eine kurze Antwort.",
            "Recherchieren brauchst du nicht, nur kurz im Netz schauen.",
            "Bitte nicht gleich recherchieren lassen.",
        ).forEach { assertFalse(it, SuchRegel.darfStarten("Recherche", it)) }
    }

    @Test fun neuerAusdruecklicherWunschGiltAuchNachEinerVerneintenFrage() {
        // Im Chat stehen Franks letzte zwei Nachrichten in den Worten; das „keine Recherche“ von vorhin hebt den neuen Wunsch nicht auf.
        assertTrue(SuchRegel.darfStarten("Recherche", "Okay, jetzt recherchiere das bitte ausführlich.", vorher = "Keine Recherche, schau nur kurz, ob DMZ spielbar ist."))
        assertFalse(SuchRegel.darfStarten("Recherche", "Ist DMZ in Deutschland spielbar?", vorher = "Recherchiere bitte nicht."))
    }

    @Test fun einWunschVonVorhinGiltNichtFuerDieNaechsteFrage() {
        // Codex-Befund: Nach „Recherchiere …“ ist die nächste normale Frage kein neuer Auftrag.
        assertFalse(SuchRegel.darfStarten("Recherche", "Was kostet Diesel gerade?", vorher = "Recherchiere ausführlich die besten Laufschuhe."))
        assertFalse(SuchRegel.darfStarten("Recherche", "wann öffnet der baumarkt heute", vorher = "Recherchiere ausführlich die besten Laufschuhe."))
        // Eine kurze Ergänzung auf die Rückfrage zur verlangten Recherche gehört noch dazu.
        assertTrue(SuchRegel.darfStarten("Recherche", "Für Deutschland.", vorher = "Recherchiere ausführlich die Rechtslage für Drohnen."))
        // Eine kurze Absage oder eine eingesprochene Ja/Nein-Frage ohne Fragezeichen ist keine Ergänzung.
        listOf("Nein, lass gut sein.", "Stopp", "Nicht nötig", "Vergiss es", "Danke, das reicht.", "ist der baumarkt heute offen").forEach {
            assertFalse(it, SuchRegel.darfStarten("Recherche", it, vorher = "Recherchiere ausführlich die Rechtslage für Drohnen."))
        }
    }

    @Test fun eineBlosseErwaehnungIstKeinWunsch() {
        // Codex-Befund: Nur die Erwähnung von Recherche oder ein Wort wie „Agentur“ reicht nicht.
        listOf(
            "Was ist eine systematische Recherche?",
            "Welche Agentur ist dafür geeignet?",
            "Wann hat die Arbeitsagentur heute offen?",
            "Hast du das schon recherchiert?",
            "Was macht die Recherche?",
            "Brauche ich dafür eine Recherche?",
            "Wie lange hat der Agent gestern gebraucht?",
        ).forEach { assertFalse(it, SuchRegel.darfStarten("Recherche", it)) }
        listOf(
            "Kannst du recherchieren, welche Laufschuhe gut sind?",
            "Starte eine Recherche zu Vitamin D.",
            "Forsch mal nach, wer das Spiel entwickelt.",
            "Kannst du das bitte recherchieren?",
            "Starte einen Agenten zu DMZ.",
            "Kannst du einen Agenten starten?",
            "Ich hätte gern eine Recherche über Laufschuhe.",
            // Eine Verneinung zählt nur direkt am Wort.
            "Keine Ahnung, recherchiere das bitte.",
            "Das weiß ich nicht genau, recherchiere bitte ausführlich.",
            "Recherchiere nicht nur den Preis, sondern auch die Verfügbarkeit.",
        ).forEach { assertTrue(it, SuchRegel.darfStarten("Recherche", it)) }
    }

    @Test fun andereAgentenNurAufWunschUnbekannteWorteNichtGesperrt() {
        assertTrue(SuchRegel.darfStarten("Machbarkeit", "Prüf mal, ob die Idee machbar ist."))
        assertTrue(SuchRegel.darfStarten("Code-Analyse", "Analysiere den Code von Aufgaben."))
        // Kein Ausweichen auf einen anderen oder neu angelegten Agenten bei einer normalen Frage.
        val frage = "Ist das neueste Modern Warfare mit DMZ in Deutschland schon spielbar?"
        assertFalse(SuchRegel.darfStarten("Machbarkeit", frage))
        assertFalse(SuchRegel.darfStarten("Websuche-Agent", frage))
        // ChatGPT über MCP und Live ohne Mitschrift: Franks Worte fehlen, die Regel gilt dort über die Anweisungen.
        assertTrue(SuchRegel.darfStarten("Recherche", null))
    }

    // 3. Widersprüchliche Kontextvorgaben: die alte pauschale Vorgabe ist überall ersetzt, die neue geht vor
    @Test fun diePauschaleAgentenVorgabeStehtNirgendsMehr() {
        val alt = listOf("alles, was Internet braucht", "Versuche so etwas nie selbst im Gespräch", "eigene Recherche ohnehin", "lange Arbeit mit agent_starten")
        quellen.forEach { (pfad, text) -> alt.forEach { assertFalse("„$it“ steht noch in $pfad", text.contains(it)) } }
        // Die alte Symbiose-Regel 6 steht nur noch als Erkennungstext für die Ersetzung da.
        val mitAlterRegel = quellen.filterValues { it.contains("Recherchen und alles über eine Minute gibst du mit agent_starten") }.keys
        assertEquals(1, mitAlterRegel.size)
        assertTrue(mitAlterRegel.first().endsWith("RegelnFaehigkeit.kt"))
    }

    @Test fun dieNeueRegelGehtAnAlleWegeUndAelterenVorgabenVor() {
        assertTrue(SuchRegel.REGEL.contains("geht älteren oder anderslautenden Vorgaben vor, auch der Symbiose"))
        assertTrue(SuchRegel.REGEL.contains("Zuerst Franks Apps"))
        // ChatGPT über MCP: steht in den Integrationsregeln, die die Symbiose für genau diese Regel zurückstellen.
        assertTrue(Integration.REGELN.contains(SuchRegel.REGEL))
        assertTrue(Integration.REGELN.contains("außer der Regel WEBSUCHE STATT AGENT"))
        // Eigener Chat, klassischer Sprachmodus und Live über jarvis_fragen: fester Teil der Anweisung des Jarvis-Agenten.
        assertTrue(quellen.filterKeys { it.endsWith("JarvisAgent.kt") }.values.single().contains("SuchRegel.REGEL"))
        // Live-Denk-Modell und Live-Sprachmodelle schicken aktuelle Fragen an Jarvis.
        assertTrue(quellen.filterKeys { it.endsWith("LiveGespraech.kt") }.values.single().contains("Recherche-Agenten startet er nur"))
        assertTrue(quellen.filterKeys { it.endsWith("AppViewModel.kt") }.values.single().contains("für aktuelle Fragen aus dem Internet"))
    }

    @Test fun unveraenderteAlteSymbioseRegelWirdErsetztFranksFassungNicht() {
        val (alt, neu) = Symbiose.ERSETZTE_VORGABEN.single()
        assertEquals(SYMBIOSE_LANGE_ARBEIT_ALT, alt)
        assertEquals(SYMBIOSE_SUCHE, neu)
        assertTrue(SYMBIOSE_VORGABE.contains(SYMBIOSE_SUCHE))
        assertFalse(SYMBIOSE_VORGABE.contains(SYMBIOSE_LANGE_ARBEIT_ALT))
        assertTrue(neu.contains("agent_starten nur, wenn ich ausdrücklich Recherche verlange"))
        // Die neue Fassung ändert die Prüfsumme: ChatGPT erkennt die alte Kopie und bittet Frank einmal, neu zu kopieren.
        val vorher = SYMBIOSE_VORGABE.map { if (it == SYMBIOSE_SUCHE) SYMBIOSE_LANGE_ARBEIT_ALT else it }.mapIndexed { i, t -> Regel(i + 1, t, "17.10.2026") }
        val nachher = SYMBIOSE_VORGABE.mapIndexed { i, t -> Regel(i + 1, t, "21.10.2026") }
        assertFalse(Symbiose.fassung(vorher) == Symbiose.fassung(nachher))
    }
}
