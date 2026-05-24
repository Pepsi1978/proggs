package de.frank.entropyreducer.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JournalMirrorDiffTest {

    @Test
    fun countsOnlyTrulyNewIds() {
        val existing = setOf(1L, 2L, 3L)
        val fetched = listOf(2L, 3L, 4L, 5L) // 4 und 5 sind neu
        assertThat(JournalMirrorDiff.newCount(existing, fetched)).isEqualTo(2)
    }

    @Test
    fun zeroWhenNothingNew() {
        assertThat(JournalMirrorDiff.newCount(setOf(1L, 2L), listOf(1L, 2L))).isEqualTo(0)
    }

    @Test
    fun deletionsDoNotCountAsNew() {
        // Quelle hat weniger als lokal -> 0 neue (Loeschungen zaehlen nicht als neu)
        assertThat(JournalMirrorDiff.newCount(setOf(1L, 2L, 3L), listOf(1L))).isEqualTo(0)
    }

    @Test
    fun allNewWhenLocalEmpty() {
        assertThat(JournalMirrorDiff.newCount(emptySet(), listOf(1L, 2L, 3L))).isEqualTo(3)
    }
}
