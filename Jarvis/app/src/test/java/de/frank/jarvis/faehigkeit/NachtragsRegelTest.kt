package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.NachtragsRegel.Ziel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NachtragsRegelTest {
    private val bekannte = listOf("BD2XW", "K7M3Q", "6U6M6")

    // 1. Ausdrücklicher Nachtrag in eine eindeutig bezeichnete Sitzung
    @Test fun ausdruecklicherNachtragInDieGleicheSessionGehtAnDenEinzigenAuftragDesGespraechs() {
        val worte = "Schick einen Nachtrag in die gleiche Session an den Programmierer: der Knopf soll grün sein."
        assertTrue(NachtragsRegel.ausdruecklich(worte))
        assertEquals(Ziel.Auftrag("BD2XW"), NachtragsRegel.ziel("", worte, bekannte, listOf("BD2XW")))
    }

    @Test fun genannteNummerBezeichnetGenauDieseSitzungAuchUeberDasPlugin() {
        val worte = "Schick bitte einen Nachtrag an Auftrag k7m3q, er soll noch die Texte prüfen."
        assertEquals(Ziel.Auftrag("K7M3Q"), NachtragsRegel.ziel("", worte, bekannte, emptyList()))
        assertEquals(Ziel.Auftrag("K7M3Q"), NachtragsRegel.ziel("K7M3Q", worte, bekannte, emptyList()))
    }

    @Test fun pluginMitAuftragsnummerUndAusdruecklichemWunsch() {
        assertEquals(Ziel.Auftrag("BD2XW"), NachtragsRegel.ziel("BD2XW", "Sag dem Programmierer noch, dass er die Farben anpasst.", bekannte, emptyList()))
    }

    // 2. Mehrdeutiges Ziel: Rückfrage, keine Zustellung
    @Test fun mehrdeutigesZielFuehrtZurNachfrage() {
        val worte = "Schick einen Nachtrag in die gleiche Session."
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("", worte, bekannte, listOf("BD2XW", "K7M3Q")))
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_ZIEL), NachtragsRegel.ziel("", worte, bekannte, emptyList()))
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("", "Nachtrag an BD2XW oder K7M3Q", bekannte, emptyList()))
    }

    @Test fun widersprichtDieAuftragsnummerFranksBezeichnungWirdNachgefragt() {
        assertEquals(Ziel.Nachfrage(NachtragsRegel.WIDERSPRUCH), NachtragsRegel.ziel("K7M3Q", "Nachtrag an BD2XW", bekannte, emptyList()))
        // „Die gleiche Session“ meint das Gespräch; eine fremde auftrag_id des Modells gilt nicht.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.WIDERSPRUCH), NachtragsRegel.ziel("K7M3Q", "Schick einen Nachtrag in die gleiche Session", bekannte, listOf("BD2XW")))
    }

    @Test fun nummerNurVomModellWaehltImGespraechNieSelbstDieSitzung() {
        val worte = "Sag dem Programmierer noch, der Knopf soll rot werden."
        // Zwei Aufträge im Gespräch, Franks Worte nennen keinen: Die Wahl des Modells zählt nicht.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("BD2XW", worte, bekannte, listOf("BD2XW", "K7M3Q")))
        // Ein Auftrag aus einem anderen Gespräch: Widerspruch, auch ohne „gleiche Session“.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.WIDERSPRUCH), NachtragsRegel.ziel("6U6M6", worte, bekannte, listOf("BD2XW")))
        // Genau der einzige Auftrag des Gesprächs: zugestellt.
        assertEquals(Ziel.Auftrag("BD2XW"), NachtragsRegel.ziel("BD2XW", worte, bekannte, listOf("BD2XW")))
        // Mehrere Aufträge, aber nur einer läuft noch: Der ist eindeutig gemeint, ohne Nachfrage.
        assertEquals(Ziel.Auftrag("K7M3Q"), NachtragsRegel.ziel("K7M3Q", worte, bekannte, listOf("BD2XW", "K7M3Q"), laufendImGespraech = listOf("K7M3Q")))
        assertEquals(Ziel.Auftrag("K7M3Q"), NachtragsRegel.ziel("", worte, bekannte, listOf("BD2XW", "K7M3Q"), laufendImGespraech = listOf("K7M3Q")))
        // Das Modell wählt den beendeten statt des einzigen laufenden: fragen.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("BD2XW", worte, bekannte, listOf("BD2XW", "K7M3Q"), laufendImGespraech = listOf("K7M3Q")))
        // Nennt Frank die Nummer selbst, gilt sie auch bei mehreren Aufträgen.
        assertEquals(Ziel.Auftrag("K7M3Q"), NachtragsRegel.ziel("K7M3Q", "Nachtrag an K7M3Q: der Knopf soll rot werden", bekannte, listOf("BD2XW", "K7M3Q")))
    }

    // 3. Bloß thematisch verwandter Wunsch ohne Nachtragserlaubnis
    @Test fun thematischVerwandterWunschIstKeinNachtrag() {
        val worte = "Programmiere in Jarvis noch einen Filter für die Live-Einstellungen."
        assertFalse(NachtragsRegel.ausdruecklich(worte))
        // Auch wenn das Modell selbst eine passende Sitzung ausgesucht hat: nichts senden.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_WUNSCH), NachtragsRegel.ziel("BD2XW", worte, bekannte, listOf("BD2XW")))
        assertFalse(NachtragsRegel.ausdruecklich("Schick den Auftrag an den Programmierer"))
        assertFalse(NachtragsRegel.ausdruecklich("Schick diesen Auftrag an den Programmierer"))
        assertFalse(NachtragsRegel.ausdruecklich("Lass das programmieren"))
        assertFalse(NachtragsRegel.ausdruecklich(""))
    }

    // 4. Mehrere Aufträge derselben App
    @Test fun mehrereAuftraegeDerselbenAppStartenNeuOhneUmleitung() {
        // Ein neuer Auftrag aus einem Gespräch, das schon Aufträge hat, bekommt eine eigene Session statt sich anzuhängen.
        assertTrue(NachtragsRegel.eigenesGespraech(ausChatGpt = false, neu = false, auftraegeImGespraech = 2))
        assertFalse(NachtragsRegel.eigenesGespraech(ausChatGpt = false, neu = false, auftraegeImGespraech = 0))
        // Ein Nachtrag ohne Nummer in ein Gespräch mit zwei Aufträgen derselben App: fragen.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("", "Nachtrag in diese Sitzung", bekannte, listOf("BD2XW", "6U6M6")))
    }

    // 5. Bereits fertige Sitzung
    @Test fun fertigeSitzungAmPcBekommtDenNachtragDieBeendeteCloudSitzungNicht() {
        assertTrue(NachtragsRegel.zustellbar(cloud = false, stand = "fertig"))
        assertTrue(NachtragsRegel.zustellbar(cloud = true, stand = "laeuft"))
        assertFalse(NachtragsRegel.zustellbar(cloud = true, stand = "fertig"))
        assertFalse(NachtragsRegel.zustellbar(cloud = true, stand = "fehler"))
        // Ein fertiger Auftrag ist nie von selbst das Ziel: ohne ausdrücklichen Wunsch keine Zustellung.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_WUNSCH), NachtragsRegel.ziel("BD2XW", "Und mach den Knopf noch größer", bekannte, listOf("BD2XW")))
    }

    // 6. Ausdrücklich neue, getrennte Session
    @Test fun ausdruecklichNeueGetrennteSessionIstNieEinNachtrag() {
        assertFalse(NachtragsRegel.ausdruecklich("Starte das in einer neuen, getrennten Session, kein Nachtrag."))
        assertFalse(NachtragsRegel.ausdruecklich("Mach dafür einen eigenen Auftrag."))
        assertFalse(NachtragsRegel.ausdruecklich("Neuer eigenständiger Auftrag in einer NEUEN, getrennten Jarvis-Programmier-Session, ausdrücklich KEIN Nachtrag zum bisherigen Live-Einstellungen-Auftrag."))
        assertTrue(NachtragsRegel.eigenesGespraech(ausChatGpt = false, neu = true, auftraegeImGespraech = 0))
        assertTrue(NachtragsRegel.eigenesGespraech(ausChatGpt = true, neu = false, auftraegeImGespraech = 0))
        // „Kein neuer Auftrag, sondern ein Nachtrag“ ist dagegen ausdrücklich.
        assertTrue(NachtragsRegel.ausdruecklich("Kein neuer Auftrag, schick das als Nachtrag in die laufende Sitzung."))
    }

    @Test fun eingesprocheneVariantenWerdenErkannt() {
        assertTrue(NachtragsRegel.ausdruecklich("schick einen nach trag in die selbe sitzung"))
        assertTrue(NachtragsRegel.ausdruecklich("Gib das in die bestehende Programmier-Session"))
        assertTrue(NachtragsRegel.ausdruecklich("sag dem programmierer noch dass das menü kleiner sein soll"))
    }

    // Regression 21.10.2026 (Auftrag 9ZHDP): Franks wörtliche Äußerungen aus dem Gespräch, die alle mit „kein_wunsch“ blockiert wurden.
    private val fuenfteAufgabe = "bitte als nachtrag noch eine fünfte aufgabe hinzufügen und zwar auf meinem rechner selber habe ich mal reingeguckt wie " +
        "das ganze aussieht diese programmierauftrag … Der zweite Auftrag, neuer ausdrücklich getrennter Auftrag auf Franks erreichbarem Rechner, aktuell " +
        "Macbook Air und so weiter. … Kann das bitte überprüft werden und korrigiert werden?"
    private val rueckfrage = "Die Übergabe wurde trotz deines ausdrücklichen Nachtragswunsches von Jarvis’ Prüfung blockiert; es wurde nichts gesendet. " +
        "Bestätige bitte noch einmal: Soll die Leerzeilen-Prüfung als fünfte Aufgabe in die laufende Sitzung mit den vier Jarvis-Aufgaben?"
    private val neueSession = "Dann startet diesen Programmierauftrag in einer neuen Programmier-Session."
    private val inDieserNeuen = "Und gib gleichzeitig in dieser neuen Programmier-Session die Aufgabe weiter, dass Nachträge irgendwie nicht akzeptiert " +
        "werden und geblockiert werden. Das soll nicht sein. Das bitte überprüfen, woran das liegt und fixen lassen."
    private val alle = listOf("QBKWZ", "74XGF", "9ZHDP")

    @Test fun ausdruecklicherNachtragGiltAuchWennDerTextEinenNeuenAuftragZitiert() {
        // Früher: „neuer ausdrücklich getrennter Auftrag“ (zitierter Titel eines anderen Auftrags) blockierte das „als Nachtrag“ am Anfang.
        assertTrue(NachtragsRegel.ausdruecklich(fuenfteAufgabe))
        val worte = NachtragsRegel.pruefWorte(fuenfteAufgabe, null, null)
        assertEquals(Ziel.Auftrag("QBKWZ"), NachtragsRegel.ziel("QBKWZ", worte, alle, listOf("QBKWZ"), listOf("QBKWZ")))
    }

    @Test fun bestaetigendesJaNachRueckfrageGiltAlsAusdruecklich() {
        assertTrue(NachtragsRegel.istBestaetigung("Ja, bitte!"))
        // Mit der Rückfrage, auf die Frank antwortet (auch ohne seine vorige Nachricht) und mit seiner vorigen Nachricht.
        for (worte in listOf(NachtragsRegel.pruefWorte("Ja, bitte!", null, rueckfrage), NachtragsRegel.pruefWorte("Ja, bitte!", fuenfteAufgabe, rueckfrage)))
            assertEquals(Ziel.Auftrag("QBKWZ"), NachtragsRegel.ziel("QBKWZ", worte, alle, listOf("QBKWZ"), listOf("QBKWZ")))
        // Ein Ja auf eine Frage, die keinen Nachtrag anbietet, oder auf eine Entweder-oder-Frage, erlaubt keinen.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_WUNSCH), NachtragsRegel.ziel("QBKWZ",
            NachtragsRegel.pruefWorte("Ja", "Programmiere in Jarvis noch einen Filter.", "Soll ich das als neuen Auftrag starten?"), alle, listOf("QBKWZ")))
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_WUNSCH), NachtragsRegel.ziel("QBKWZ",
            NachtragsRegel.pruefWorte("Ja", null, "Soll das ein neuer Auftrag werden oder ein Nachtrag in die laufende Sitzung?"), alle, listOf("QBKWZ")))
        // „Bitte programmiere …“ ist kein Ja, sondern ein neuer Wunsch.
        assertFalse(NachtragsRegel.istBestaetigung("Bitte programmiere noch einen Filter"))
        assertFalse(NachtragsRegel.istBestaetigung("Ja, aber in einer neuen Session"))
    }

    @Test fun nachtragInDieseNeueSessionGehtAnDieZuletztAusDemGespraechGestartete() {
        // Die vorige Nachricht („in einer neuen Programmier-Session“) zählt nicht mehr mit; „dieser neuen Session“ ist ein Verweis.
        val worte = NachtragsRegel.pruefWorte(inDieserNeuen, neueSession, "Die Leerzeilen-Prüfung läuft jetzt als eigener Auftrag.")
        assertTrue(NachtragsRegel.ausdruecklich(worte))
        assertTrue(NachtragsRegel.verweistAufNeue(worte))
        assertEquals(Ziel.Auftrag("74XGF"), NachtragsRegel.ziel("74XGF", worte, alle, listOf("QBKWZ"), listOf("QBKWZ"), gestartetAusGespraech = listOf("74XGF")))
        assertEquals(Ziel.Auftrag("74XGF"), NachtragsRegel.ziel("", worte, alle, listOf("QBKWZ"), listOf("QBKWZ"), gestartetAusGespraech = listOf("74XGF")))
        // Das Modell wählt die alte Sitzung des Gesprächs statt der neuen: fragen.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("QBKWZ", worte, alle, listOf("QBKWZ"), listOf("QBKWZ"), gestartetAusGespraech = listOf("74XGF")))
        // Ohne gemerkte Herkunft (vor dieser Version gestartet) bleibt die fremde Nummer ein Widerspruch: nichts senden.
        assertEquals(Ziel.Nachfrage(NachtragsRegel.WIDERSPRUCH), NachtragsRegel.ziel("74XGF", worte, alle, listOf("QBKWZ"), listOf("QBKWZ")))
        // Ein neuer Auftrag ist weiterhin kein Nachtrag.
        assertFalse(NachtragsRegel.ausdruecklich(neueSession))
        assertFalse(NachtragsRegel.ausdruecklich("Dann übermittele auch das als neuen Programmierauftrag, dass irgendwie Nachträge blockiert werden."))
    }

    @Test fun mehrdeutigeZieleBeiMehrerenSitzungen() {
        val worte = "Schick das als Nachtrag in die laufende Sitzung."
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("", worte, alle, listOf("QBKWZ", "9ZHDP"), listOf("QBKWZ", "9ZHDP")))
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("QBKWZ", worte, alle, listOf("QBKWZ", "9ZHDP"), listOf("QBKWZ", "9ZHDP")))
        // Zwei aus dem Gespräch gestartete: „diese neue Session“ meint die zuletzt gestartete; wählt das Modell die ältere, fragen.
        val neue = "Nachtrag in diese neue Session: der Knopf soll rot werden."
        assertEquals(Ziel.Auftrag("9ZHDP"), NachtragsRegel.ziel("9ZHDP", neue, alle, listOf("QBKWZ"), gestartetAusGespraech = listOf("74XGF", "9ZHDP")))
        assertEquals(Ziel.Nachfrage(NachtragsRegel.MEHRDEUTIG), NachtragsRegel.ziel("74XGF", neue, alle, listOf("QBKWZ"), gestartetAusGespraech = listOf("74XGF", "9ZHDP")))
    }

    @Test fun fehlendeAutorisierungBleibtGesperrt() {
        assertEquals(Ziel.Nachfrage(NachtragsRegel.KEIN_WUNSCH), NachtragsRegel.ziel("QBKWZ", NachtragsRegel.pruefWorte("Ja, bitte!", null, null), alle, listOf("QBKWZ")))
        assertFalse(NachtragsRegel.ausdruecklich("Nicht in die laufende Sitzung, sondern als neuen Auftrag."))
        assertFalse(NachtragsRegel.ausdruecklich("Bitte kein Nachtrag, mach das getrennt."))
        // Ein früheres „Nachtrag“ erlaubt keinen neuen, bloß thematisch passenden Wunsch.
        assertEquals("Programmiere in Jarvis noch einen Filter.", NachtragsRegel.pruefWorte("Programmiere in Jarvis noch einen Filter.", fuenfteAufgabe, rueckfrage))
        assertFalse(NachtragsRegel.ausdruecklich(NachtragsRegel.pruefWorte("Programmiere in Jarvis noch einen Filter.", fuenfteAufgabe, rueckfrage)))
    }

    @Test fun nichtErreichbareSitzungUndDoppelteUebermittlung() {
        assertFalse(NachtragsRegel.zustellbar(cloud = true, stand = "fertig"))
        assertTrue(NachtragsRegel.zustellbar(cloud = false, stand = "laeuft"))
        val jetzt = 1_000_000_000L
        assertTrue(NachtragsRegel.doppelt("Der Knopf  soll ROT werden", jetzt - 60_000L, "der knopf soll rot werden", jetzt))
        assertFalse(NachtragsRegel.doppelt("Der Knopf soll rot werden", jetzt - 31 * 60_000L, "Der Knopf soll rot werden", jetzt))
        assertFalse(NachtragsRegel.doppelt("Der Knopf soll rot werden", jetzt - 60_000L, "Der Knopf soll blau werden", jetzt))
    }

    // Befunde des Code-Reviews: kein Weg für unaufgeforderte Nachträge.
    @Test fun neuWunschIrgendwoSperrtAusserNachAusdruecklicherAnweisungAmAnfang() {
        assertFalse(NachtragsRegel.ausdruecklich("Programmiere in Jarvis: Nachträge werden geblockt. Das bitte in einer neuen Session."))
        assertFalse(NachtragsRegel.ausdruecklich("Nachtrag in die laufende Sitzung – nein, doch lieber eine neue Session"))
        assertFalse(NachtragsRegel.ausdruecklich("Auf keinen Fall in die laufende Sitzung, sondern als neuen Auftrag"))
        assertFalse(NachtragsRegel.ausdruecklich("Schick das bitte nicht einfach so als Nachtrag"))
        assertFalse(NachtragsRegel.ausdruecklich("Keinesfalls als Nachtrag"))
        assertFalse(NachtragsRegel.ausdruecklich("Mach das bitte in einer anderen Session"))
        // „Die neue Session“ ohne „diese“ bleibt ein Neu-Wunsch.
        assertFalse(NachtragsRegel.ausdruecklich("Schick das in die neue Sitzung"))
        assertFalse(NachtragsRegel.verweistAufNeue("Schick das in die neue Sitzung"))
    }

    @Test fun jaZaehltNurZurLetztenEindeutigenNachtragsFrage() {
        val ja = "Ja"
        // Entweder-oder, oder die Frage bietet etwas anderes an: kein Nachtrag, auch nicht über Franks vorige Nachricht.
        for (frage in listOf("Soll ich das als Nachtrag in die laufende Sitzung schicken oder als neuen Auftrag starten?",
            "Der Nachtrag wurde blockiert. Soll ich stattdessen einen neuen Auftrag starten?",
            "Der Nachtrag wurde blockiert. Soll ich das neu starten?",
            "Fertig. Zusätzliches Code-Review?"))
            assertFalse(frage, NachtragsRegel.ausdruecklich(NachtragsRegel.pruefWorte(ja, "Als Nachtrag: Filter einbauen", frage)))
        assertFalse(NachtragsRegel.istBestaetigung("Danke"))
        assertFalse(NachtragsRegel.ausdruecklich(NachtragsRegel.pruefWorte("Danke", null, "Soll das als Nachtrag in die laufende Sitzung?")))
        assertTrue(NachtragsRegel.ausdruecklich(NachtragsRegel.pruefWorte(ja, null, "Alles klar. Soll das als Nachtrag in die laufende Sitzung?")))
    }
}
