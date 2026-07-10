package de.frank.entropyreducer.presentation.tagebuch

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagebuchAreaTest {

    @Test
    fun `missing or unknown area remains entropy for backward compatibility`() {
        assertThat(tagebuchAreaFromStoredValue(null)).isEqualTo(TagebuchArea.ENTROPY)
        assertThat(tagebuchAreaFromStoredValue("unknown")).isEqualTo(TagebuchArea.ENTROPY)
    }

    @Test
    fun `learning area survives persisted value parsing`() {
        assertThat(tagebuchAreaFromStoredValue("LEARNING")).isEqualTo(TagebuchArea.LEARNING)
    }
}
