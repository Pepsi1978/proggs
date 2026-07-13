package de.frank.entropyreducer.presentation

import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PriorityRampTest {

    @Test
    fun `each priority midpoint remains in its assigned color family`() {
        val veryHigh = priorityRampColor(90.0).toArgb()
        val high = priorityRampColor(70.0).toArgb()
        val medium = priorityRampColor(50.0).toArgb()
        val low = priorityRampColor(30.0).toArgb()
        val later = priorityRampColor(10.0).toArgb()

        assertThat(red(veryHigh)).isGreaterThan(green(veryHigh))
        assertThat(red(high)).isGreaterThan(green(high))
        assertThat(green(high)).isGreaterThan(blue(high))
        assertThat(red(medium)).isGreaterThan(green(medium))
        assertThat(green(medium)).isGreaterThan(blue(medium))
        assertThat(green(low)).isGreaterThan(red(low))
        assertThat(green(low)).isGreaterThan(blue(low))
        assertThat(blue(later)).isGreaterThan(green(later))
        assertThat(green(later)).isGreaterThan(red(later))
    }

    private fun red(color: Int): Int = color shr 16 and 0xFF

    private fun green(color: Int): Int = color shr 8 and 0xFF

    private fun blue(color: Int): Int = color and 0xFF
}
