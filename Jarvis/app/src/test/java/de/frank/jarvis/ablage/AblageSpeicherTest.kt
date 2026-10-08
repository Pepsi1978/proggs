package de.frank.jarvis.ablage

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Prüft die Ablage mit echten Beispieldateien (src/test/resources/ablage): Migration der bisherigen Texte,
 * Übernahme, Typerkennung, Doppelschutz, gleichnamige Dateien, Einträge mit mehreren Anhängen, Neustart,
 * vollständige und abgebrochene Downloads samt Fortsetzen.
 */
class AblageSpeicherTest {
    private lateinit var wurzel: File
    private lateinit var s: AblageSpeicher

    @Before fun vorbereiten() {
        wurzel = Files.createTempDirectory("ablage").toFile()
        s = AblageSpeicher.fuer(wurzel)
    }

    @After fun aufraeumen() {
        AblageSpeicher.vergiss(wurzel)
        wurzel.deleteRecursively()
        Uebertragungen.erlaubeLokal = false
    }

    private fun probe(name: String): File = File(javaClass.classLoader!!.getResource("ablage/$name")!!.toURI())

    /** Simuliert einen App-Neustart: neue Instanz, alles frisch von der Platte. */
    private fun neustart(): AblageSpeicher { AblageSpeicher.vergiss(wurzel); return AblageSpeicher.fuer(wurzel).also { s = it } }

    @Test fun bestehendeTexteWerdenOhneVerlustUebernommen() {
        val alt = File(wurzel, "ablage").apply { mkdirs() }
        File(alt, "Recherche Wecker.md").writeText("# Alt\n\nInhalt bleibt.\n")
        File(alt, "Notiz.md").writeText("Zweite Datei\n")
        val liste = s.eintraege()
        assertEquals(2, liste.size)
        assertTrue(liste.all { it.hatText && !it.hatDateien })
        assertEquals("# Alt\n\nInhalt bleibt.\n", File(alt, "Recherche Wecker.md").readText())
        assertTrue(File(wurzel, "ablage-index.json").isFile)
        // Nach dem Neustart dieselben Einträge mit denselben Kennungen.
        val ids = liste.map { it.id }.toSet()
        assertEquals(ids, neustart().eintraege().map { it.id }.toSet())
        // Alte Schnittstelle: Finden über einen eindeutigen Teil des Titels.
        assertEquals("Recherche Wecker.md", s.finde("wecker")?.textDatei)
    }

    @Test fun textSchreibenWieBisher() {
        val (e, datei) = s.schreibeText("Plan: Woche/1", "Erster Teil")
        assertEquals("Plan Woche 1.md", datei.name)
        s.schreibeText("Plan: Woche/1", "Zweiter Teil", anhaengen = true)
        assertEquals("Erster Teil\n\n\nZweiter Teil\n", datei.readText())
        assertEquals(1, s.eintraege().size)
        assertEquals(e.id, s.eintraege()[0].id)
    }

    @Test fun bildMitFalschemNamenWirdAlsPngErkanntUndDoppelteZustellungErkannt() {
        val bytes = probe("infografik-din-a4.png").readBytes()
        val u1 = s.uebernimmBytes("Infografik Jarvis", bytes, "infografik", "application/octet-stream", kennung = "chatgpt-file_1")
        assertTrue(u1.neu)
        assertEquals("png", u1.anhang.endung)
        assertEquals("image/png", u1.anhang.mime)
        assertEquals(Art.BILD, u1.anhang.art)
        assertEquals("infografik.png", u1.anhang.originalName)
        assertEquals(bytes.size.toLong(), u1.anhang.groesse)
        assertArrayEquals(bytes, s.datei(u1.anhang).readBytes())
        // Gleiche Kennung (erneute Zustellung) und gleicher Inhalt im selben Eintrag: keine Kopie.
        val u2 = s.uebernimmBytes("Infografik Jarvis", bytes, "infografik.png", kennung = "chatgpt-file_1")
        val u3 = s.uebernimmBytes("Infografik Jarvis", bytes, "nochmal.png")
        assertFalse(u2.neu); assertFalse(u3.neu)
        assertEquals(1, s.eintraege().single().anhaenge.size)
        assertTrue(File(wurzel, "ablage-dateien/.teil").listFiles()!!.isEmpty())
    }

    @Test fun gleichnamigeDateienUeberschreibenSichNicht() {
        val a = probe("foto.jpg").readBytes()
        val b = probe("klein.webp").readBytes()
        val u1 = s.uebernimmBytes("Fotos", a, "bild.jpg")
        val u2 = s.uebernimmBytes("Fotos", b, "bild.jpg")
        assertNotEquals(u1.anhang.speicher, u2.anhang.speicher)
        assertArrayEquals(a, s.datei(u1.anhang).readBytes())
        assertArrayEquals(b, s.datei(u2.anhang).readBytes())
        // Inhalt geht vor Name: das WebP heißt jetzt passend.
        assertEquals("webp", u2.anhang.endung)
        assertEquals("bild.webp", u2.anhang.originalName)
    }

    @Test fun eintragMitTextPdfUndMehrerenBildernUeberstehtNeustart() {
        s.schreibeText("Recherche Schlaf", "# Bericht\n\nLesbarer Text.")
        s.uebernimm("Recherche Schlaf", kopieTeil("dokument-3-seiten.pdf"), "Bericht.pdf")
        s.uebernimm("Recherche Schlaf", kopieTeil("foto.jpg"), "Abbildung 1.jpg")
        s.uebernimm("Recherche Schlaf", kopieTeil("infografik-din-a4.png"), "Abbildung 2.png")
        val e = neustart().eintraege().single()
        assertTrue(e.hatText)
        assertEquals(3, e.anhaenge.size)
        assertEquals(setOf(Kategorie.TEXTE, Kategorie.DOKUMENTE, Kategorie.BILDER), e.kategorien)
        assertEquals(listOf(Art.PDF, Art.BILD, Art.BILD), e.anhaenge.map { it.art })
        e.anhaenge.forEach { assertTrue(s.datei(it).isFile) }
        assertTrue(s.text(e).contains("Lesbarer Text."))
    }

    @Test fun alleBeispieldateienWerdenErkannt() {
        val erwartet = mapOf(
            "infografik-din-a4.png" to Art.BILD, "foto.jpg" to Art.BILD, "klein.webp" to Art.BILD, "animation.gif" to Art.ANIMATION,
            "dokument-3-seiten.pdf" to Art.PDF, "ton.wav" to Art.AUDIO, "ton.mp3" to Art.AUDIO, "film.mp4" to Art.VIDEO,
            "bericht.docx" to Art.DOKUMENT, "daten.csv" to Art.TABELLE_TEXT, "einstellungen.json" to Art.DATEN, "unbekannt.xyz" to Art.SONSTIGE,
        )
        erwartet.forEach { (name, art) ->
            val u = s.uebernimm("Alle Formate", kopieTeil(name), name)
            assertEquals(name, art, u.anhang.art)
            assertEquals(name, probe(name).length(), u.anhang.groesse)
        }
        val e = neustart().eintraege().single()
        assertEquals(erwartet.size, e.anhaenge.size)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", e.anhaenge.first { it.endung == "docx" }.mime)
        assertEquals("application/octet-stream", e.anhaenge.first { it.endung == "xyz" }.mime)
    }

    @Test fun leereDateiWirdAbgelehnt() {
        val teil = s.neueTeilDatei().apply { writeBytes(ByteArray(0)) }
        try { s.uebernimm("Leer", teil, "leer.txt"); fail() } catch (e: AblageFehler) { assertTrue(e.message!!.contains("leer")) }
        assertTrue(s.eintraege().isEmpty())
    }

    @Test fun beschaedigtesVerzeichnisFaelltAufSicherungZurueck() {
        s.uebernimmBytes("A", probe("ton.wav").readBytes(), "ton.wav")
        s.uebernimmBytes("B", probe("ton.mp3").readBytes(), "ton.mp3")
        File(wurzel, "ablage-index.json").writeText("{kaputt")
        val liste = neustart().eintraege()
        // Die Sicherung ist die Fassung vor dem letzten Schreiben: Eintrag A ist sicher da.
        assertTrue(liste.any { it.titel == "A" })
    }

    @Test fun downloadVollstaendigUndMitTyp() {
        Uebertragungen.erlaubeLokal = true
        MockWebServer().use { server ->
            val bytes = probe("infografik-din-a4.png").readBytes()
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)).setHeader("Content-Type", "image/png"))
            server.start()
            val u = Uebertragungen(s, File(wurzel, "auftraege.json"))
            // So kommt ein ChatGPT-Dateiverweis an: download_url mit Zugangsparametern, file_name, mime_type, file_id.
            val url = server.url("/files/abc/raw?sig=GEHEIM123&exp=99").toString()
            val id = u.anlegen(DownloadAuftrag(url, "Infografik Jarvis-Funktionen", "Jarvis-Funktionen.png", "image/png", kennung = "chatgpt-file_9", herkunft = "ChatGPT-Datei"))
            val e = runBlocking { u.ausfuehren(id) }
            assertArrayEquals(bytes, s.datei(e.anhang).readBytes())
            assertEquals("Jarvis-Funktionen.png", e.anhang.originalName)
            assertFalse("Zugangsdaten dürfen nicht in der Herkunft stehen", e.anhang.herkunft.contains("GEHEIM"))
            assertTrue(u.liste.value.isEmpty())
            assertFalse(File(wurzel, "auftraege.json").readText().contains("\"id\":\"$id\""))
        }
    }

    @Test fun abgebrochenerDownloadErscheintNichtUndLaesstSichFortsetzen() {
        Uebertragungen.erlaubeLokal = true
        val bytes = probe("film.mp4").readBytes()
        MockWebServer().use { server ->
            // 1. Versuch: Verbindung bricht nach der Hälfte ab.
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)).setHeader("Content-Type", "video/mp4").setHeader("ETag", "\"v1\"").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
            server.start()
            val u = Uebertragungen(s, File(wurzel, "auftraege.json"))
            val id = u.anlegen(DownloadAuftrag(server.url("/film.mp4?token=GEHEIM").toString(), "Video", ""))
            try { runBlocking { u.ausfuehren(id) }; fail("Abbruch muss als Fehler enden") } catch (e: AblageFehler) {
                assertFalse(e.message!!.contains("GEHEIM"))
            }
            assertTrue("Unvollständige Datei darf nicht in der Ablage stehen", s.eintraege().isEmpty())
            assertEquals(UebertragungsZustand.FEHLER, u.liste.value.single().zustand)
            val geladen = File(s.teilOrdner, "$id.teil").length()
            assertTrue("Teil-Datei muss angefangen sein ($geladen)", geladen in 1 until bytes.size)
            // 2. Versuch: Server liefert den Rest (206).
            server.enqueue(MockResponse().setResponseCode(206).setBody(Buffer().write(bytes.copyOfRange(geladen.toInt(), bytes.size)))
                .setHeader("Content-Range", "bytes $geladen-${bytes.size - 1}/${bytes.size}"))
            val e = runBlocking { u.ausfuehren(id) }
            assertArrayEquals(bytes, s.datei(e.anhang).readBytes())
            assertEquals("film.mp4", e.anhang.originalName)
            assertEquals(Art.VIDEO, e.anhang.art)
            server.takeRequest()
            val zweite = server.takeRequest()
            assertEquals("bytes=$geladen-", zweite.getHeader("Range"))
            assertEquals("\"v1\"", zweite.getHeader("If-Range"))
        }
    }

    @Test fun unpassenderTeilbereichWirdVerworfen() {
        Uebertragungen.erlaubeLokal = true
        val bytes = probe("film.mp4").readBytes()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)).setHeader("ETag", "\"v1\"").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
            // Der Server antwortet mit einem Bereich, der nicht an der vorhandenen Länge beginnt.
            server.enqueue(MockResponse().setResponseCode(206).setBody(Buffer().write(bytes)).setHeader("Content-Range", "bytes 0-${bytes.size - 1}/${bytes.size}"))
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)).setHeader("ETag", "\"v1\""))
            server.start()
            val u = Uebertragungen(s, File(wurzel, "auftraege.json"))
            val id = u.anlegen(DownloadAuftrag(server.url("/film.mp4").toString(), "Video", "film.mp4"))
            runCatching { runBlocking { u.ausfuehren(id) } }
            try { runBlocking { u.ausfuehren(id) }; fail() } catch (e: AblageFehler) { assertTrue(e.message!!.contains("unpassenden Teil")) }
            assertFalse(File(s.teilOrdner, "$id.teil").exists())
            assertTrue(s.eintraege().isEmpty())
            val e = runBlocking { u.ausfuehren(id) }
            assertArrayEquals(bytes, s.datei(e.anhang).readBytes())
        }
    }

    @Test fun namenImEigenenNetzWerdenNichtAufgeloest() {
        listOf("localhost", "127.0.0.1").forEach {
            try { Uebertragungen.STANDARD_CLIENT.dns.lookup(it); fail(it) } catch (_: java.net.UnknownHostException) {}
        }
        listOf("10.1.2.3", "192.168.0.1", "169.254.1.1", "100.64.0.1", "fd00::1", "::1").forEach { assertTrue(it, Uebertragungen.istLokal(java.net.InetAddress.getByName(it))) }
        listOf("8.8.8.8", "2001:4860:4860::8888").forEach { assertFalse(it, Uebertragungen.istLokal(java.net.InetAddress.getByName(it))) }
    }

    @Test fun abgelaufenerLinkGibtVerstaendlicheMeldungOhneAdresse() {
        Uebertragungen.erlaubeLokal = true
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403))
            server.start()
            val u = Uebertragungen(s, File(wurzel, "auftraege.json"))
            val id = u.anlegen(DownloadAuftrag(server.url("/x?sig=GEHEIM").toString(), "X", "x.png"))
            try { runBlocking { u.ausfuehren(id) }; fail() } catch (e: AblageFehler) {
                assertTrue(e.message!!.contains("abgelaufen"))
                assertFalse(e.message!!.contains("GEHEIM"))
            }
            assertNotNull(u.liste.value.single().fehler)
            // Neustart: der fehlgeschlagene Auftrag bleibt zum Wiederholen erhalten.
            assertEquals(1, Uebertragungen(s, File(wurzel, "auftraege.json")).liste.value.size)
        }
    }

    @Test fun nurHttpsUndKeineAdressenImEigenenNetz() {
        listOf("http://example.com/a.png", "https://192.168.1.10/a.png", "https://localhost/a", "https://10.0.0.1/x", "ftp://example.com/a", "kein link").forEach {
            try { Uebertragungen.pruefeAdresse(it); fail(it) } catch (_: AblageFehler) {}
        }
        Uebertragungen.pruefeAdresse("https://files.example.com/a.png?sig=1")
    }

    private fun kopieTeil(name: String): File = s.neueTeilDatei().also { probe(name).copyTo(it, overwrite = true) }
}
