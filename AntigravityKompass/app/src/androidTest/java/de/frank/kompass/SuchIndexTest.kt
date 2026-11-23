package de.frank.kompass

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.data.local.SucheDao
import de.frank.kompass.data.local.SucheFtsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sichert den Fehler ab, der die Suche abstürzen liess: Jedes Speichern legte im FTS-Index eine
 * weitere Zeile an, doppelte Treffer sprengten den Schlüssel der Trefferliste.
 *
 * Läuft auf dem Gerät gegen eine Datenbank im Arbeitsspeicher — die echten Daten der App werden
 * nicht berührt. Starten per `adb shell am instrument`, NICHT per connectedAndroidTest: das
 * deinstalliert die App danach samt aller eigenen Fragen.
 */
@RunWith(AndroidJUnit4::class)
class SuchIndexTest {

    private lateinit var datenbank: KompassDatabase
    private lateinit var suche: SucheDao

    @Before
    fun oeffne() {
        datenbank = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            KompassDatabase::class.java,
        ).build()
        suche = datenbank.sucheDao()
    }

    @After
    fun schliesse() = datenbank.close()

    private fun zeile(text: String) = SucheFtsEntity(
        quelleId = "slash:/compact",
        quelleArt = "eintrag",
        bereich = "slash",
        titel = "/compact",
        suchtext = text,
    )

    @Test
    fun zweimalSpeichernErgibtGenauEinenTreffer() = runBlocking {
        suche.ersetze(listOf(zeile("kontext verdichten")))
        suche.ersetze(listOf(zeile("kontext verdichten ausfuehrlich")))

        assertEquals(1, suche.suche("kontext*").size)
        assertEquals(1, suche.anzahl())
        assertEquals(0, suche.anzahlDoppelte())
    }

    @Test
    fun ersetzterTextWirdNichtMehrGefunden() = runBlocking {
        suche.ersetze(listOf(zeile("alter begriff")))
        suche.ersetze(listOf(zeile("neuer begriff")))

        assertEquals(0, suche.suche("alter*").size)
        assertEquals(1, suche.suche("neuer*").size)
    }

    @Test
    fun andereQuellenBleibenStehen() = runBlocking {
        suche.ersetze(listOf(zeile("kontext verdichten")))
        suche.ersetze(listOf(zeile("kontext").copy(quelleId = "slash:/clear", titel = "/clear")))
        suche.ersetze(listOf(zeile("kontext verdichten")))

        assertEquals(2, suche.suche("kontext*").size)
        assertEquals(0, suche.anzahlDoppelte())
    }
}
