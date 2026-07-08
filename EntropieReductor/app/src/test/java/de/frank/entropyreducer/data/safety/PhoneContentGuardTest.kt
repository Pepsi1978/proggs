package de.frank.entropyreducer.data.safety

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhoneContentGuardTest {
    @Test
    fun `blockiert Kurzgeruest und Nachtschichtbibliothekar Artefakte`() {
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = "Kurzgerüst zum Thema",
                text = "Nachdem Frank mehrfach gefragt hat",
            )
        ).isTrue()
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = "Nachtschichtbibliothekar Fund",
                text = "Interne Arbeitsnotiz",
            )
        ).isTrue()
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = "Nachtschech Bibliothekar",
                text = "interner Plan",
            )
        ).isTrue()
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = "Nacht Schicht Bibliothekar",
                text = "interner Plan",
            )
        ).isTrue()
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = "Nachtschech-Bibliothekar",
                text = "interner Plan",
            )
        ).isTrue()
    }

    @Test
    fun `erlaubt normale Mentals auch mit Nachtschicht Bezug`() {
        assertThat(
            PhoneContentGuard.isSecondBrainWorkArtifact(
                title = null,
                text = "Nach der Nachtschicht ruhig bleiben und erst schlafen.",
            )
        ).isFalse()
    }
}
