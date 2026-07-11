package de.frank.entropyreducer.presentation.mental

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PausableCountdownTest {
    @Test
    fun `pausierte Zeit wird nicht von der Restzeit abgezogen`() {
        var now = 0L
        val countdown = PausableCountdown(totalMs = 60_000L, nowMs = { now })

        now = 30_000L
        countdown.pause()
        now = 7_230_000L

        assertThat(countdown.remainingMs()).isEqualTo(30_000L)

        countdown.resume()
        now += 29_999L
        assertThat(countdown.isExpired()).isFalse()
        now += 1L
        assertThat(countdown.isExpired()).isTrue()
    }
}
