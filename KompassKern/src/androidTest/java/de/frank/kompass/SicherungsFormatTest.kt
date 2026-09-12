package de.frank.kompass

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.frank.kompass.data.Sicherung
import de.frank.kompass.data.SicherungsFehler
import de.frank.kompass.data.local.ChatNachrichtEntity
import de.frank.kompass.data.local.ChatSitzungEntity
import de.frank.kompass.data.local.EintragEntity
import de.frank.kompass.data.local.FrageEntity
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.data.model.SicherungsTeil
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sichert die eine Eigenschaft ab, an der das ganze Sicherungsmodul hängt: Was der Schreiber
 * in die Prüfsumme nimmt, muss der Leser in derselben Reihenfolge wieder hineinnehmen.
 *
 * Weicht das auch nur in einem Feld ab, gilt JEDE geschriebene Sicherung beim Zurücklesen als
 * beschädigt — und weil seit der Selbstprüfung genau daran das Sichern scheitert, hätte die App
 * ab diesem Moment gar keine Sicherung mehr geschrieben. Ein solcher Fehler war schon einmal
 * drin: Der Leser liess den Titel eines Gesprächs aus, den der Schreiber aufgenommen hatte.
 *
 * Läuft auf dem Gerät, weil `android.util.JsonWriter` und `JsonReader` verwendet werden; im
 * reinen JVM-Test gibt es die Klassen nur als Hülle.
 */
@RunWith(AndroidJUnit4::class)
class SicherungsFormatTest {

    private val eintraege = listOf(
        eintrag("slash:/help", "slash", "/help", "Zeigt die Hilfe.", stufe = 0),
        eintrag("slash:/undo", "slash", "/undo", "Nimmt zurück.", stufe = 2),
        eintrag("config:model", "config", "model", "Das Modell.", stufe = 1),
        eintrag("praxis:regeln", "praxis", "Regeln", "", stufe = 0),
        // Zeichen, die dem JSON und der Prüfsumme wehtun könnten.
        eintrag("slash:/sonder", "slash", "/sonder", "Anführung \" Backslash \\ Zeilen\numbruch ä ö ü ß 😀", stufe = 3),
    )

    private val fragen = listOf(
        FrageEntity(id = 1, eintragId = "slash:/help", frage = "Was tut das?", antwort = "Hilfe.", erstelltAm = 111L),
        FrageEntity(id = 2, eintragId = "slash:/undo", frage = "Und Git?", antwort = "Ja, nötig.", erstelltAm = 222L),
    )

    private val sitzungen = listOf(
        ChatSitzungEntity(id = 10, titel = "Erstes Gespräch", erstelltAm = 333L),
        ChatSitzungEntity(id = 11, titel = "Zweites", erstelltAm = 444L),
    )

    private val nachrichten = mapOf(
        10L to listOf(
            ChatNachrichtEntity(id = 1, sitzungId = 10, rolle = "user", text = "Hallo", erstelltAm = 1L),
            ChatNachrichtEntity(id = 2, sitzungId = 10, rolle = "assistant", text = "Guten Tag", erstelltAm = 2L),
        ),
        11L to listOf(
            ChatNachrichtEntity(id = 3, sitzungId = 11, rolle = "user", text = "Noch was", erstelltAm = 3L),
        ),
    )

    private val quelle = object : Sicherung.Quelle {
        override suspend fun eintraegeSeite(bereiche: List<String>, nachId: String): List<EintragEntity> =
            eintraege.filter { it.bereich in bereiche && it.id > nachId }
                .sortedBy { it.id }
                .take(Sicherung.SEITE)

        override suspend fun fragenSeite(nachId: Long): List<FrageEntity> =
            fragen.filter { it.id > nachId }.sortedBy { it.id }.take(Sicherung.SEITE)

        override suspend fun sitzungen(): List<ChatSitzungEntity> = sitzungen

        override suspend fun nachrichten(sitzungId: Long): List<ChatNachrichtEntity> =
            nachrichten[sitzungId].orEmpty()
    }

    private fun eintrag(id: String, bereich: String, name: String, erklaerung: String, stufe: Int) =
        EintragEntity(
            id = id,
            bereich = bereich,
            name = name,
            kurz = "kurz zu $name",
            erklaerung = erklaerung,
            stufe = stufe,
            seitVersion = "1.0.0",
            kategorie = "Test",
            art = "Eingebaut",
            quelleEnglisch = "English text for $name",
            sortierName = name.removePrefix("/").lowercase(),
        )

    private fun schreibe(umfang: Set<SicherungsTeil>): String {
        val ziel = StringWriter()
        runBlocking {
            Sicherung.schreibe(
                ziel = ziel,
                quelle = quelle,
                erstelltAm = "12.09.2026, 14:30",
                umfang = umfang,
                roomVersion = KompassDatabase.VERSION,
            )
        }
        return ziel.toString()
    }

    /** Der Kern: schreiben, lesen, und die Prüfsumme muss aufgehen. */
    @Test
    fun vollerUmfangGehtDurchDiePruefung() {
        val text = schreibe(SicherungsTeil.ALLE)
        val vorschau = runBlocking { Sicherung.pruefe(StringReader(text)) }

        assertEquals(5, vorschau.anzahl.eintraege)
        assertEquals(2, vorschau.anzahl.fragen)
        assertEquals(2, vorschau.anzahl.sitzungen)
        assertEquals(3, vorschau.anzahl.nachrichten)
        assertEquals(Sicherung.SCHEMA_VERSION, vorschau.schema)
        assertEquals(KompassDatabase.VERSION, vorschau.roomVersion)
        assertEquals(SicherungsTeil.ALLE, vorschau.umfang)
        assertEquals(3, vorschau.jeBereich[SicherungsTeil.SLASH])
        assertEquals(1, vorschau.jeBereich[SicherungsTeil.CONFIG])
        assertEquals(1, vorschau.jeBereich[SicherungsTeil.PRAXIS])
    }

    /** Jede Teilmenge muss für sich aufgehen — auch die ohne Einträge oder ohne Gespräche. */
    @Test
    fun jedeTeilmengeGehtDurchDiePruefung() {
        val proben = listOf(
            setOf(SicherungsTeil.SLASH),
            setOf(SicherungsTeil.FRAGEN),
            setOf(SicherungsTeil.GESPRAECHE),
            setOf(SicherungsTeil.SLASH, SicherungsTeil.GESPRAECHE),
            setOf(SicherungsTeil.CONFIG, SicherungsTeil.PRAXIS, SicherungsTeil.FRAGEN),
        )
        proben.forEach { umfang ->
            val text = schreibe(umfang)
            val vorschau = runBlocking { Sicherung.pruefe(StringReader(text)) }
            assertEquals("Umfang $umfang", umfang, vorschau.umfang)
        }
    }

    /** Was der Leser an die Senke gibt, muss dem entsprechen, was hineinging. */
    @Test
    fun dieSenkeBekommtAllesZurueck() {
        val text = schreibe(SicherungsTeil.ALLE)
        val gelesen = mutableListOf<Map<String, String>>()
        val geleseneFragen = mutableListOf<Triple<String, String, String>>()
        val geleseneSitzungen = mutableListOf<Pair<String, Int>>()

        runBlocking {
            Sicherung.spieleEin(
                StringReader(text),
                object : Sicherung.Senke {
                    override suspend fun eintrag(werte: Map<String, String>) {
                        gelesen += werte
                    }

                    override suspend fun frage(eintragId: String, frage: String, antwort: String, erstelltAm: Long) {
                        geleseneFragen += Triple(eintragId, frage, antwort)
                    }

                    override suspend fun sitzung(
                        titel: String,
                        erstelltAm: Long,
                        nachrichten: List<Triple<String, String, Long>>,
                    ) {
                        geleseneSitzungen += titel to nachrichten.size
                    }
                },
            )
        }

        assertEquals(5, gelesen.size)
        val sonder = gelesen.first { it["id"] == "slash:/sonder" }
        assertEquals(eintraege.last().erklaerung, sonder["erklaerung"])
        assertEquals("3", sonder["stufe"])
        assertEquals("false", sonder["entfernt"])
        assertEquals(listOf(Triple("slash:/help", "Was tut das?", "Hilfe.")), geleseneFragen.take(1))
        assertEquals(listOf("Erstes Gespräch" to 2, "Zweites" to 1), geleseneSitzungen)
    }

    /** Eine veränderte Datei darf nicht als heil durchgehen. */
    @Test
    fun veraenderterInhaltFaelltAuf() {
        val text = schreibe(SicherungsTeil.ALLE).replace("Zeigt die Hilfe.", "Zeigt etwas anderes.")
        try {
            runBlocking { Sicherung.pruefe(StringReader(text)) }
            fail("Eine veränderte Sicherung wurde als heil angesehen.")
        } catch (fehler: SicherungsFehler) {
            assertTrue(fehler.message.orEmpty(), fehler.message.orEmpty().contains("Prüfsumme"))
        }
    }

    /** Eine abgeschnittene Übertragung darf nicht als heil durchgehen. */
    @Test
    fun abgeschnitteneDateiFaelltAuf() {
        val text = schreibe(SicherungsTeil.ALLE)
        try {
            runBlocking { Sicherung.pruefe(StringReader(text.take(text.length / 2))) }
            fail("Eine abgeschnittene Sicherung wurde als heil angesehen.")
        } catch (fehler: SicherungsFehler) {
            assertTrue(fehler.message.orEmpty(), fehler.message.orEmpty().isNotBlank())
        }
    }

    /** Eine Sicherung aus einer neueren Datenbank wird abgelehnt, nicht halb eingelesen. */
    @Test
    fun neuereDatenbankWirdAbgelehnt() {
        val text = schreibe(SicherungsTeil.ALLE)
            .replace("\"roomVersion\": ${KompassDatabase.VERSION}", "\"roomVersion\": 99")
        try {
            runBlocking { Sicherung.pruefe(StringReader(text)) }
            fail("Eine Sicherung aus einer neueren Datenbank wurde angenommen.")
        } catch (fehler: SicherungsFehler) {
            assertTrue(fehler.message.orEmpty(), fehler.message.orEmpty().contains("neueren Datenbank"))
        }
    }

    /** Eine Sicherung einer anderen App gehört nicht hierher. */
    @Test
    fun fremdeAppWirdAbgelehnt() {
        val text = schreibe(SicherungsTeil.ALLE)
            .replace("\"${AppProfil.PRODUKT}\"", "\"Irgendeine andere App\"")
        try {
            runBlocking { Sicherung.pruefe(StringReader(text)) }
            fail("Die Sicherung einer fremden App wurde angenommen.")
        } catch (fehler: SicherungsFehler) {
            assertTrue(fehler.message.orEmpty(), fehler.message.orEmpty().contains("gehört nicht"))
        }
    }
}
