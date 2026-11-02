package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandyUseTest {
    @Test fun erkenntDenAuftragInAllenSchreibweisen() {
        assertTrue(HandyUse.verlangt("${HandyUse.EINSTIEG} Finde die Einstellung."))
        assertTrue(HandyUse.verlangt("Bitte über Handy-Use prüfen"))
        assertTrue(HandyUse.verlangt("mit handy use nachsehen"))
        assertTrue(HandyUse.verlangt("Verwende den Skill Handy-Use, um WLAN zu prüfen"))
    }

    @Test fun normalerAuftragVerlangtIhnNicht() {
        assertFalse(HandyUse.verlangt("Bau in die Wecker-App einen Knopf ein, der auf dem Handy gut benutzbar ist"))
        assertFalse(HandyUse.verlangt("Das Handy useful machen"))
    }

    @Test fun arbeitAmSkillSelbstDarfInDieCloud() {
        assertFalse(HandyUse.verlangt("Ändere im Skill handy-use die Wartezeit"))
        assertFalse(HandyUse.verlangt("Bau in Jarvis einen Handy-Use-Hinweis ein"))
    }
}
