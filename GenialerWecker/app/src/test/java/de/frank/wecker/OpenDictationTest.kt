package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenDictationTest {
    @Test fun roundTripKeepsAllFields() {
        val value = OpenDictation("id-1", "Frühschicht", "Heute zuerst „Ideen“ lesen.\nZweite Zeile", 2, 1_789_000_000_000)
        assertEquals(value, OpenDictation.parse(value.json()))
    }

    @Test fun damagedOrUnknownEntriesAreRejected() {
        assertThrows(Exception::class.java) { OpenDictation.parse("{kaputt") }
        assertThrows(Exception::class.java) { OpenDictation.parse("""{"v":2,"draftId":"a","draftName":"n","text":"t","missing":0,"createdAt":1}""") }
        assertThrows(Exception::class.java) { OpenDictation.parse("""{"v":1,"draftId":"","draftName":"n","text":"t","missing":0,"createdAt":1}""") }
        assertThrows(Exception::class.java) { OpenDictation.parse("""{"v":1,"draftId":"a","draftName":"n","text":" ","missing":0,"createdAt":1}""") }
        assertThrows(Exception::class.java) { OpenDictation.parse("""{"v":1,"draftId":"a","draftName":"n","text":"t","missing":-1,"createdAt":1}""") }
        assertThrows(Exception::class.java) { OpenDictation.parse("""{"v":1,"draftId":"a","text":"t","missing":0,"createdAt":1}""") }
    }
}
