package de.frank.jarvis.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SprachBefehleTest {

    @Test fun ausdruecklichesEndeGiltImmer() {
        for (satz in listOf(
            "Sprachsession beenden",
            "Die Sprachsession bitte stoppen.",
            "Jarvis, beende die Sprachsession jetzt",
            "Kannst du die Sprachsession beenden?",
            "Sprach-Session stopp",
            "Mach das Mikrofon aus",
            "Schalt bitte das Mikro aus",
            "Mikrofon schließen",
            "Okay, Gesprächsmodus beenden",
            "Voice Session beenden",
        )) {
            assertTrue(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = false))
            assertTrue(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = true))
        }
    }

    @Test fun kurzeEndwuenscheNurInDerSprachschleife() {
        for (satz in listOf("Bitte beenden jetzt", "Beenden", "Session beenden", "Jetzt bitte stoppen", "Hör auf", "Danke, das war's", "Stopp", "Die Sitzung beenden bitte")) {
            assertTrue(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = true))
        }
        for (satz in listOf("Bitte beenden jetzt", "Session beenden", "Hör auf")) {
            assertFalse(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = false))
        }
    }

    @Test fun normaleFragenUndAndereBefehleBeendenNicht() {
        for (satz in listOf(
            "Wie beende ich die Sprachsession?",
            "Beende die Sprachsession nicht",
            "Was ist eine Sprachsession?",
            "Stopp den Timer",
            "Stoppuhr stoppen",
            "Mach den Wecker aus",
            "Programmier-Session beenden",
            "Beende den Programmierauftrag",
            "Beende die App",
            "Hör zu",
            "Zu",
            "Wie wird das Wetter morgen?",
            "Starte einen Programmierauftrag, der die Sprachsession beenden kann",
        )) {
            assertFalse(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = true))
            assertFalse(satz, SprachBefehle.istSessionEnde(satz, imGespraechsmodus = false))
        }
    }
}
