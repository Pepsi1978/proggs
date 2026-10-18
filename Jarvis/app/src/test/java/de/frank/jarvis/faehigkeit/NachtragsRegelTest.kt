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
}
