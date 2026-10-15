package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammierregelnTest {
    private val regeln = "FRANKS PROGRAMMIERREGELN (…):\n[1] Vor jeder Änderung Projekt und Paketkennung prüfen.\n[2] Nie einen eigenen Keystore anlegen.\n"
    private val pc = "FORTSCHRITT (AUFTRAG-ID: ABCDE): PC"
    private val cloud = "FORTSCHRITT (AUFTRAG-ID: ABCDE): Cloud"

    @Test fun programmierregelnSindEigeneDateiImKontextUndAlsArtMerkbar() {
        // merkKontext() baut aus genau dieser Liste: steht die Datei hier, steht sie bei jeder Frage, im Plugin, bei Agenten und in der Tagesauswertung.
        assertTrue(Programmierregeln in merkdateien)
        assertEquals(listOf(Regeln, Ziele, UeberFrank, Programmierregeln), merkdateien)
        assertEquals(Programmierregeln, Merkart.von("programmierregel")?.datei)
        // Die bestehenden Dateien bleiben unverändert erreichbar.
        assertEquals(Regeln, Merkart.von("regel")?.datei)
        assertEquals(Ziele, Merkart.von("ziel")?.datei)
        assertEquals(UeberFrank, Merkart.von("info")?.datei)
    }

    @Test fun listeGibtJedenEintragVollstaendigWieder() {
        val lang = "Beim Programmieren immer " + "sehr ".repeat(250) + "gründlich bleiben."
        val text = Programmierregeln.liste(listOf(Regel(2, "Zweite Regel.", ""), Regel(1, lang, "")))
        assertEquals("[1] $lang\n[2] Zweite Regel.", text)
    }

    @Test fun pcUndCloudBekommenDieRegelnVollstaendigVorDerFortschrittsAnweisung() {
        for (fortschritt in listOf(pc, cloud)) {
            val text = ProgrammierFaehigkeit.auftragMitRegeln("Baue einen Filter in Geniale Aufgaben ein.", regeln, fortschritt)
            assertTrue(text.startsWith("Baue einen Filter in Geniale Aufgaben ein."))
            assertTrue(text.contains(regeln.trim()))
            assertTrue(text.indexOf(regeln.trim()) < text.indexOf(fortschritt))
            assertTrue(text.endsWith(fortschritt))
        }
    }

    @Test fun ohneProgrammierregelnBleibtDerAuftragWieBisher() {
        assertEquals("Auftrag\n\n$pc", ProgrammierFaehigkeit.auftragMitRegeln("Auftrag", "", pc))
        assertFalse(ProgrammierFaehigkeit.auftragMitRegeln("Auftrag", "  ", pc).contains("Programmierregeln"))
    }

    @Test fun zuLangerAuftragWirdGekuerztNieDieRegeln() {
        val vieleRegeln = "FRANKS PROGRAMMIERREGELN:\n" + (1..40).joinToString("\n") { "[$it] " + "x".repeat(1_400) }
        val text = ProgrammierFaehigkeit.auftragMitRegeln("A".repeat(80_000), vieleRegeln, cloud)
        assertTrue(text.length <= ProgrammierFaehigkeit.HOECHSTENS)
        assertTrue(text.contains(vieleRegeln))
        assertTrue(text.endsWith(cloud))
        assertTrue(text.contains("(gekürzt)"))
    }
}
