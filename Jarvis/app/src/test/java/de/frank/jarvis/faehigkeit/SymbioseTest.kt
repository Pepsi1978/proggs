package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbioseTest {
    private val vorgabe = SYMBIOSE_VORGABE.mapIndexed { i, t -> Regel(i + 1, t, "17.10.2026") }
    private fun fuerChatGpt(e: List<Regel>) = Symbiose.titel(e) + "\n" + Symbiose.liste(e)

    @Test fun textFuerChatGptPasstInsFeldMitLuft() {
        val text = fuerChatGpt(vorgabe)
        println("Symbiose-Text für ChatGPT: ${text.length} Zeichen")
        assertTrue("${text.length} Zeichen", text.length <= 3_950)
    }

    @Test fun beideFassungenNennenDieselbeKennungUndDieselbenRegeln() {
        val fassung = Symbiose.fassung(vorgabe)
        assertTrue(Symbiose.titel(vorgabe).contains("Fassung $fassung"))
        // Der Kontext-Kopf für Jarvis trägt dieselbe Fassung und das Stichwort, auf das Regel 10 verweist.
        val kopf = Symbiose.kopfTextFuerTest(vorgabe)
        assertTrue(kopf.startsWith("SYMBIOSE"))
        assertTrue(kopf.contains("Fassung $fassung"))
        assertTrue(kopf.contains("Stand 17.10.2026"))
        // Die Regeln selbst sind in beiden Texten zeichengleich.
        assertTrue(fuerChatGpt(vorgabe).endsWith(Symbiose.liste(vorgabe)))
    }

    @Test fun jedeAenderungAendertDieFassung() {
        val geaendert = vorgabe.map { if (it.id == 3) it.copy(text = it.text + " Neu.") else it }
        assertNotEquals(Symbiose.fassung(vorgabe), Symbiose.fassung(geaendert))
        assertEquals(Symbiose.fassung(vorgabe), Symbiose.fassung(vorgabe.reversed()))
    }

    @Test fun genannteWerkzeugeGibtEsWirklich() {
        // Werkzeuge, die der Text nennt; ein Tippfehler kostete ChatGPT im Sprachmodus einen Fehlversuch.
        val genannt = listOf(
            "jarvis_kontext", "gedaechtnis_suchen", "jarvis_merken", "idee_speichern", "agent_starten", "vorgaenge", "programmieren", "programmier_antwort",
            "aufgabe_anlegen", "kalender_eintragen", "dienstplan_lesen", "wecker_stellen", "timer", "stoppuhr", "biomarker_auswertung", "trainings_lesen",
            "wetter_lesen", "fahrzeit_lesen", "tagebuch_lesen", "stacks_lesen", "epigenetik_lesen", "mail_lesen", "mail_senden", "ablage_schreiben",
            "bild_erzeugen", "tagesauswertung_lesen", "einstellungen", "jarvis_status",
        )
        val text = Symbiose.liste(vorgabe)
        genannt.forEach { assertTrue("$it fehlt im Text", text.contains(it)) }
        // Gegenprobe gegen den Quelltext der Werkzeuge: jeder Name ist irgendwo als name = "…" definiert.
        val quellen = java.io.File("src/main/java/de/frank/jarvis").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        genannt.forEach { assertTrue("Werkzeug $it gibt es nicht", quellen.contains("name = \"$it\"") || quellen.contains("\"$it\"")) }
    }
}
