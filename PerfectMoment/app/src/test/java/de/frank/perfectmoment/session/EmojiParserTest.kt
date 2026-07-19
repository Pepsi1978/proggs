package de.frank.perfectmoment.session

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiParserTest {
    @Test
    fun `trennt einfaches Emoji vom Text`() {
        assertEquals(
            EmojiParser.ParsedQuestion("🌅", "Wie fühlt sich ein schönes Leben an?"),
            EmojiParser.parse("🌅 Wie fühlt sich ein schönes Leben an?"),
        )
    }

    @Test
    fun `trennt ZWJ Emoji als ein Graphem`() {
        assertEquals(
            EmojiParser.ParsedQuestion("👨‍👩‍👧‍👦", "Was stärkt deine Familie?"),
            EmojiParser.parse("👨‍👩‍👧‍👦 Was stärkt deine Familie?"),
        )
    }

    @Test
    fun `trennt Flagge aus zwei Regionalindikatoren`() {
        assertEquals(
            EmojiParser.ParsedQuestion("🇩🇪", "Was bedeutet Heimat für dich?"),
            EmojiParser.parse("🇩🇪 Was bedeutet Heimat für dich?"),
        )
    }

    @Test
    fun `trennt Pfeil Emoji aus dem Validator Bereich`() {
        assertEquals(
            EmojiParser.ParsedQuestion("↗️", "Wohin möchtest du dich entwickeln?"),
            EmojiParser.parse("↗️ Wohin möchtest du dich entwickeln?"),
        )
    }

    @Test
    fun `verwendet Fallback ohne Emoji und behaelt Text`() {
        assertEquals(
            EmojiParser.ParsedQuestion("✨", "Was tut dir heute gut?"),
            EmojiParser.parse("Was tut dir heute gut?"),
        )
    }
}
