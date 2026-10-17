package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerkDublettenTest {
    @Test fun wiederholungGiltAlsGleich() {
        assertTrue(Merkdatei.gleich("Frank fährt einen Toyota C-HR.", "  frank fährt einen toyota c-hr "))
        assertFalse(Merkdatei.gleich("Frank fährt einen Toyota C-HR.", "Frank fährt einen Toyota RAV4."))
    }

    @Test fun korrekturDesselbenEintragsIstSehrAehnlich() {
        assertTrue(Merkdatei.aehnlichkeit("Frank fährt einen Toyota C-HR.", "Frank fährt einen Toyota RAV4.") >= Merkdatei.AEHNLICH)
        assertTrue(Merkdatei.aehnlichkeit("Frank will seine VO2max auf 45 steigern.", "Frank will seine VO2max auf 46 steigern.") >= Merkdatei.AEHNLICH)
    }

    @Test fun verschiedeneThemenSindNichtAehnlich() {
        assertTrue(Merkdatei.aehnlichkeit("Frank fährt einen Toyota C-HR.", "Frank geht bei Regen nicht gern im Wald laufen.") < Merkdatei.AEHNLICH)
        assertTrue(Merkdatei.aehnlichkeit("Bei Fragen nach dem Schlaf immer HRV nennen.", "Frank ist Dortmund-Fan.") < Merkdatei.AEHNLICH)
        assertEquals(0.0, Merkdatei.aehnlichkeit("", "Frank ist Dortmund-Fan."), 0.0)
    }

    @Test fun entscheidungenSindEigeneArt() {
        assertEquals(Entscheidungen, Merkart.von("entscheidung")?.datei)
    }
}
