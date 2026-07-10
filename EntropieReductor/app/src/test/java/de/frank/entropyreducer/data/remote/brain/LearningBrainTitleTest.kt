package de.frank.entropyreducer.data.remote.brain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearningBrainTitleTest {

    @Test
    fun `same display title remains unique for each learning entry`() {
        val firstId = "59ced6c9-c6ef-4f62-9708-80b01675a547"
        val secondId = "48a27736-17bd-4303-b28c-fc8eb1df8640"
        val first = learningBrainTitle("Neue Erkenntnis", firstId)
        val second = learningBrainTitle("Neue Erkenntnis", secondId)

        assertThat(first).isNotEqualTo(second)
        assertThat(first).contains("Lernen")
        assertThat(first).endsWith(firstId)
    }

    @Test
    fun `technical learning title round trips to original identity and display title`() {
        val id = "59ced6c9-c6ef-4f62-9708-80b01675a547"

        val parsed = parseLearningBrainTitle(learningBrainTitle("Neue Erkenntnis", id))

        assertThat(parsed.displayTitle).isEqualTo("Neue Erkenntnis")
        assertThat(parsed.rowId).isEqualTo(id)
    }

    @Test
    fun `ordinary brain title remains unchanged and gets no imported identity`() {
        val parsed = parseLearningBrainTitle("Externes Wissen")

        assertThat(parsed.displayTitle).isEqualTo("Externes Wissen")
        assertThat(parsed.rowId).isNull()
    }
}
