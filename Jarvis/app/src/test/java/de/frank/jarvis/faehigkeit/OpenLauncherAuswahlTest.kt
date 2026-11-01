package de.frank.jarvis.faehigkeit

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Kombination des echten Angebots (OpenLauncher/models.json, effort-stufen.json) muss aus dem Eingabefeld unverändert
 * wieder herauskommen: genau diese Werte gehen als slug, anbieter, effort, profil, modus und cli an den Rechner.
 */
class OpenLauncherAuswahlTest {
    private val angebot = OpenLauncherKatalog.Angebot(
        OpenLauncherKatalog.gruppenAus(File("../../OpenLauncher/models.json").readText()),
        OpenLauncherKatalog.stufenAus(File("../../OpenLauncher/effort-stufen.json").readText()),
    )

    @Test fun jedeKombinationKommtUnveraendertAn() {
        var anzahl = 0
        angebot.modelle.forEach { m ->
            angebot.efforts(m).ifEmpty { listOf("") }.forEach { effort ->
                OpenLauncherEfforts.PROFILE.forEach { profil ->
                    OpenLauncherEfforts.MODI.forEach { modus ->
                        m.clis.forEach { cli ->
                            val eingabe = ProgrammiererVorlage.FreieWahl(m.name, m.anbieterName, m.slug,
                                if (effort.isEmpty()) ProgrammiererVorlage.OHNE_EFFORT else OpenLauncherEfforts.name(effort),
                                profil.name, modus.name, OpenLauncherKatalog.CLI_NAMEN.getValue(cli))
                            val text = ProgrammiererVorlage.vorspannFrei(eingabe) + " Baue A ; Baue B"
                            assertEquals(text, eingabe, ProgrammiererVorlage.freieWahl(text))
                            assertEquals("Baue A ; Baue B", ProgrammiererVorlage.aufgabe(text))
                            assertEquals(m.name, ProgrammiererVorlage.modell(text))
                            val wahl = assertNotNullUndGib(angebot.wahlAus(eingabe), text)
                            assertEquals(text, m.slug, wahl.modell.slug)
                            assertEquals(text, m.anbieter, wahl.modell.anbieter)
                            assertEquals(text, effort, wahl.effort)
                            assertEquals(text, profil.wert, wahl.profil)
                            assertEquals(text, modus.wert, wahl.modus)
                            assertEquals(text, cli, wahl.cli)
                            anzahl++
                        }
                    }
                }
            }
        }
        println("OpenLauncher-Kombinationen geprüft: $anzahl")
        assertTrue(anzahl > 1000)
    }

    @Test fun schnellwahlTraegtModellUndCli() {
        ProgrammiererVorlage.MODELL_OPTIONEN.forEach { (modell, _) ->
            ProgrammiererVorlage.cliOptionen(modell).forEach { (cli, _) ->
                val text = ProgrammiererVorlage.vorspannFuer(modell, cli) + " Baue A"
                assertEquals(modell, ProgrammiererVorlage.modell(text))
                assertEquals(cli, ProgrammiererVorlage.cli(text))
                assertEquals("Baue A", ProgrammiererVorlage.aufgabe(text))
            }
        }
    }

    private fun assertNotNullUndGib(w: OpenLauncherKatalog.Wahl?, text: String): OpenLauncherKatalog.Wahl {
        assertNotNull(text, w)
        return w!!
    }
}
