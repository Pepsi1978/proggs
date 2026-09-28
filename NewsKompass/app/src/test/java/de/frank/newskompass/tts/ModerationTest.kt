package de.frank.newskompass.tts

import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ModerationTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val morgen = ZonedDateTime.of(2026, 9, 28, 5, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val meldung = Meldung("m1", "Neues Modell", listOf("Erster Absatz."), emptyList(), null, false)

    private fun ausgabe(slot: String, id: String = "a$morgen") = Ausgabe(id, morgen, slot, emptyList())

    @Test
    fun vorlageNenntAusgabeTitelUndDatum() {
        val text = Moderation.blockText(ausgabe("Morgenausgabe"), Block("t", "KI-News", listOf(meldung), null), zone)
        assertEquals(
            listOf(
                "Willkommen zur Morgenausgabe von KI-News vom 28. September 2026.",
                "Neues Modell.",
                "Erster Absatz.",
                "Danke fürs Zuhören, das war die Morgenausgabe von KI-News vom 28. September 2026.",
            ),
            text.split("\n\n"),
        )
    }

    @Test
    fun passenderSatzDerKiGewinnt() {
        val block = Block(
            "t", "KI-News", listOf(meldung), null,
            anmoderation = "„Guten Morgen und willkommen zur Morgenausgabe von KI-News vom 28. September 2026“",
            abmoderation = "Das war die Morgenausgabe von KI-News vom 28. September 2026, danke fürs Zuhören!",
        )
        val a = ausgabe("Morgenausgabe")
        assertEquals("Guten Morgen und willkommen zur Morgenausgabe von KI-News vom 28. September 2026.", Moderation.anmoderation(a, block, zone))
        assertEquals("Das war die Morgenausgabe von KI-News vom 28. September 2026, danke fürs Zuhören!", Moderation.abmoderation(a, block, zone))
    }

    @Test
    fun falscheAusgabeOderFalscherTitelFallenAufVorlage() {
        val block = Block("t", "KI-News", listOf(meldung), null, anmoderation = "Willkommen zur Abendausgabe von KI-News.", abmoderation = "Das war Sport.")
        val a = ausgabe("Morgenausgabe")
        assertEquals("Willkommen zur Morgenausgabe von KI-News vom 28. September 2026.", Moderation.anmoderation(a, block, zone))
        assertEquals("Danke fürs Zuhören, das war die Morgenausgabe von KI-News vom 28. September 2026.", Moderation.abmoderation(a, block, zone))
    }

    @Test
    fun fehlendesOderFalschesDatumFaelltAufVorlage() {
        val block = Block(
            "t", "KI-News", listOf(meldung), null,
            anmoderation = "Willkommen zur Morgenausgabe von KI-News vom 27. September 2026.",
            abmoderation = "Das war die Morgenausgabe von KI-News, danke fürs Zuhören!",
        )
        val a = ausgabe("Morgenausgabe")
        assertEquals("Willkommen zur Morgenausgabe von KI-News vom 28. September 2026.", Moderation.anmoderation(a, block, zone))
        assertEquals("Danke fürs Zuhören, das war die Morgenausgabe von KI-News vom 28. September 2026.", Moderation.abmoderation(a, block, zone))
    }

    @Test
    fun frageUndRueckblickHabenEigeneSaetze() {
        val frage = Block("f", "Borussia Dortmund", listOf(meldung), null, frage = "Wie hat Dortmund gespielt?")
        assertEquals(
            "Hier ist die Antwort auf deine Frage zu Borussia Dortmund, Stand 28. September 2026.",
            Moderation.anmoderation(ausgabe("Morgenausgabe"), frage, zone),
        )
        val rueckblick = ausgabe("Rückblick September 2026 · 1. bis 28. September", id = "rueckblick-2026-09")
        assertEquals("Willkommen zum Rückblick September 2026 von KI-News.", Moderation.anmoderation(rueckblick, Block("t", "KI-News", listOf(meldung), null), zone))
    }

    @Test
    fun ohneMeldungenNurDerTitel() {
        assertEquals("KI-News.", Moderation.blockText(ausgabe("Morgenausgabe"), Block("t", "KI-News", emptyList(), null), zone))
    }

    @Test
    fun saeubereVerwirftZuLangeUndAdressen() {
        assertEquals("", Moderation.saeubere("Mehr unter https://example.com"))
        assertEquals("", Moderation.saeubere("x".repeat(Moderation.MAX_ZEICHEN + 1)))
        assertEquals("Hallo.", Moderation.saeubere("\n  **Hallo**  \nzweite Zeile"))
    }
}
