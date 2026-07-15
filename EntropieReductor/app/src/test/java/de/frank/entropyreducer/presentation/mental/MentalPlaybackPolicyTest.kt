package de.frank.entropyreducer.presentation.mental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentalPlaybackPolicyTest {
    @Test
    fun `wiederholt Mentals solange automatische Abschaltung nicht erreicht ist`() {
        assertTrue(shouldRepeatMentalPlayback(autoStopReached = false))
    }

    @Test
    fun `beendet Mentals sobald automatische Abschaltung erreicht ist`() {
        assertFalse(shouldRepeatMentalPlayback(autoStopReached = true))
    }
}
