package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandyUseTest {
    @Test fun erkenntDenSkillInAllenSchreibweisen() {
        assertTrue(HandyUse.verlangt("${HandyUse.EINSTIEG} Finde die Einstellung."))
        assertTrue(HandyUse.verlangt("Bitte über Handy-Use prüfen"))
        assertTrue(HandyUse.verlangt("mit handy use nachsehen"))
    }

    @Test fun normalerAuftragVerlangtIhnNicht() {
        assertFalse(HandyUse.verlangt("Bau in die Wecker-App einen Knopf ein, der auf dem Handy gut benutzbar ist"))
        assertFalse(HandyUse.verlangt("Das Handy useful machen"))
    }

    @Test fun handyUseAuftragGehtNieInDieCloud() {
        assertEquals(listOf("pc"), HandyUse.erreichbar("Nutze handy-use", listOf("pc", "cloud")) { it == "pc" })
        assertEquals(emptyList<String>(), HandyUse.erreichbar("Nutze handy-use", listOf("cloud")) { it == "pc" })
    }

    @Test fun andereAuftraegeBehaltenAlleZiele() {
        assertEquals(listOf("pc", "cloud"), HandyUse.erreichbar("Bau einen Filter", listOf("pc", "cloud")) { it == "pc" })
    }
}
