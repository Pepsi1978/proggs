// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v2
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die drei Zusagen des Moduls, mit echten Werten von Silero VAD v5.1.2 (werkzeuge/vad_testfaelle.py):
 * Lüfter- und Fahrgeräusch halten die Aufnahme nicht offen, stille Halluzinationen fallen, und echte kurze
 * Äußerungen („Ja.“, „Stopp.“) bleiben vom Aufnahmeende bis zum fertigen Text erhalten. Dazu der Rückfall auf den
 * Pegel, wenn die Spracherkennung mitten in der Aufnahme ausfällt.
 */
class TranskriptionsSchutzTest {
    private class Fall(val werte: List<Float>, val pegel: List<Float>, val sprache: List<Pair<Long, Long>>) {
        val stimmhaft get() = BooleanArray(werte.size) { werte[it] >= SprachZeitachse.STIMMHAFT }
        val dauerSek get() = werte.size * SprachZeitachse.RAHMEN_MS / 1000.0
    }

    private fun lade(name: String): Fall {
        val zeilen = javaClass.classLoader!!.getResourceAsStream("vad/$name.txt")!!.bufferedReader().readLines()
        val sprache = zeilen.first().substringAfter("Sprache (s):").trim().split(' ').filter { it.isNotBlank() }.map {
            val (a, b) = it.split('-').map { s -> (s.toDouble() * 1000).toLong() }
            a to b
        }
        val daten = zeilen.drop(1).filter { it.isNotBlank() }.map { it.split(' ') }
        return Fall(daten.map { it[0].toFloat() }, daten.map { it[1].toFloat() }, sprache)
    }

    /** Wie auf dem Handy: je 32 ms ein Rahmen in die Zeitachse, alle 50 ms ein Blick von SprechEnde. */
    private fun abspielen(
        fall: Fall,
        ende: SprechEnde = SprechEnde(),
        vadAusfallAbMs: Long = Long.MAX_VALUE,
    ): Triple<SprechEnde.Lage, Long, SprachZeitachse> {
        var naechster = 0
        val achse = SprachZeitachse { fall.werte[naechster - 1] }
        val leer = ByteArray(1024)
        var ms = 50L
        while (ms <= fall.werte.size * 32L) {
            while ((naechster + 1) * 32L <= ms && naechster < fall.werte.size) {
                naechster++
                achse.pcm(leer, leer.size)
            }
            val pegel = fall.pegel[(naechster - 1).coerceAtLeast(0)]
            // MicRecorder setzt die Zeitachse bei einem Fehler auf null; ab dann zählt der Pegel.
            val rahmen = if (ms < vadAusfallAbMs) achse.sprachRahmen else null
            val lage = ende.neu(pegel, ms, rahmen)
            if (lage != SprechEnde.Lage.WEITER) return Triple(lage, ms, achse)
            ms += 50
        }
        return Triple(SprechEnde.Lage.WEITER, ms, achse)
    }

    /** Eine WAV-Hülle der passenden Länge; mit Zeitachse zählt für die Analyse nur die Stimme, nicht der Inhalt. */
    private fun wav(sek: Double): ByteArray = ByteArray(44 + (sek * 16_000).toInt() * 2).also {
        it[24] = 0x80.toByte(); it[25] = 0x3E // 16000 Hz
    }

    private fun segment(von: Double, bis: Double, text: String) = GroqSegment(von, bis, text, 0.05, -0.2, 1.2)

    private val filter = WhisperHallucinationFilter()
    private val analyzer = SpeechAnalyzer()

    // ---- Geräusch hält die Aufnahme nicht offen ----

    @Test
    fun reinerLuefterHaeltNichtsOffen() {
        val (lage, ms, achse) = abspielen(lade("luefter_nur"))
        assertEquals(SprechEnde.Lage.NICHTS_GEHOERT, lage)
        assertEquals(7_000L, ms)
        assertEquals(0L, achse.sprachRahmen)
    }

    @Test
    fun spracheAmLuefterEndetNachViereinhalbSekunden() {
        val fall = lade("luefter_sprache_dann_luefter")
        val (lage, ms, _) = abspielen(fall)
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        val nachSprache = ms - fall.sprache.last().second
        assertTrue("Ende $nachSprache ms nach dem letzten Wort", nachSprache in 4_500L..5_100L)
    }

    // ---- Stille Halluzinationen fallen ----

    @Test
    fun reinerLuefterGehtNichtHochUndFloskelFaellt() {
        val fall = lade("luefter_nur")
        val analyse = analyzer.analyze(wav(fall.dauerSek), fall.stimmhaft)!!
        // Schicht 1: weniger als 150 ms Stimme, die Aufnahme ginge gar nicht erst an Groq.
        assertTrue("stimmhaft ${analyse.voicedMs} ms", analyse.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS)
        // Schicht 4: Käme doch etwas zurück, fällt die typische Stille-Floskel.
        assertEquals("", filter.filter(GroqTranscriptionResponse("Untertitel im Auftrag des ZDF.", null), analyse))
        assertEquals("", filter.filter(GroqTranscriptionResponse("Vielen Dank fürs Zuschauen!", null), analyse))
    }

    @Test
    fun angehaengtesWortImLuefterrauschenFaellt() {
        val fall = lade("luefter_sprache_dann_luefter")
        val analyse = analyzer.analyze(wav(fall.dauerSek), fall.stimmhaft)
        val antwort = GroqTranscriptionResponse(
            "Wie wird das Wetter morgen in Neuenhagen? Ja.",
            listOf(segment(0.7, 3.0, " Wie wird das Wetter morgen in Neuenhagen?"), segment(6.0, 8.0, " Ja.")),
        )
        // Schicht 3: Das sicher klingende „Ja.“ liegt im reinen Lüfterrauschen und fällt, der echte Satz bleibt.
        assertEquals("Wie wird das Wetter morgen in Neuenhagen?", filter.filter(antwort, analyse))
    }

    // ---- Echte kurze Äußerungen bleiben ----

    private fun kurzeAeusserungBleibt(name: String, text: String) {
        val fall = lade(name)
        val ende = SprechEnde()
        val (lage, ms, _) = abspielen(fall, ende)
        // Das Aufnahmeende erkennt das eine Wort als Sprache und wartet danach die 4,5 s ab.
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue(ende.gesprochen)
        val nachSprache = ms - fall.sprache.last().second
        assertTrue("$name: Ende $nachSprache ms nach dem Wort", nachSprache in 4_500L..5_100L)
        // Schicht 1 lässt es hochgehen …
        val analyse = analyzer.analyze(wav(fall.dauerSek), fall.stimmhaft)!!
        assertTrue("$name: stimmhaft ${analyse.voicedMs} ms", analyse.voicedMs >= SpeechAnalyzer.MIN_SPEECH_MS)
        // … und Schicht 2 bis 4 lassen den Text stehen.
        val von = fall.sprache.first().first / 1000.0
        val bis = fall.sprache.last().second / 1000.0
        val antwort = GroqTranscriptionResponse(text, listOf(segment(von - 0.1, bis + 0.1, " $text")))
        assertEquals(text, filter.filter(antwort, analyse))
    }

    @Test
    fun kurzesJaImRuhigenRaumBleibt() = kurzeAeusserungBleibt("kurz_ja_ruhig", "Ja.")

    @Test
    fun kurzesStoppImFahrendenAutoBleibt() = kurzeAeusserungBleibt("kurz_stopp_auto", "Stopp.")

    // ---- Rückfall bei Ausfall der Spracherkennung ----

    @Test
    fun ausfallDerSpracherkennungFaelltAufDenPegelZurueck() {
        // Ruhiger Raum, die Spracherkennung fällt eine halbe Sekunde nach dem Satz aus. Ab dann zählt der Pegel; im
        // ruhigen Raum liegt er unter der Schwelle, die Aufnahme endet also trotzdem nach der Pause.
        val fall = lade("ruhig_leise")
        val ausfall = fall.sprache.last().second + 500
        val ende = SprechEnde()
        val (lage, ms, _) = abspielen(fall, ende, vadAusfallAbMs = ausfall)
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue(ende.gesprochen)
        assertTrue("Ende bei $ms ms", ms >= fall.sprache.last().second + 4_500)
    }

    @Test
    fun ohneZeitachseAnalysiertDerPegel() {
        // Ohne Spracherkennung (Modell fehlt, andere Abtastrate) misst Schicht 1 wie früher den Pegel.
        val stille = analyzer.analyze(wav(3.0), null)!!
        assertEquals(0, stille.voicedMs)
        assertFalse(stille.segmentHasSpeech(0.5, 2.5))
    }
}
