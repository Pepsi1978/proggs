package de.frank.entropyreducer.presentation.widget

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.domain.model.TimeBucket
import org.junit.Test

class WidgetExpandStateTest {

    @Test
    fun `each priority section starts open and can be toggled independently`() {
        TimeBucket.entries.forEach { bucket ->
            assertThat(WidgetExpandState.isExpanded(bucket.name)).isTrue()

            WidgetExpandState.toggle(bucket.name)
            assertThat(WidgetExpandState.isExpanded(bucket.name)).isFalse()

            WidgetExpandState.toggle(bucket.name)
            assertThat(WidgetExpandState.isExpanded(bucket.name)).isTrue()
        }
    }
}
