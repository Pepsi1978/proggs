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
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Die vier Schichten gegen Whisper-Halluzinationen (Kanon: best-practices/desktop/groq-transkription.md), hier mit
 * der Zeitachse der Spracherkennung aus dem Audio-Testfall „Sprache, dann 7 s Fahrgeräusch“.
 */
class HalluzinationsSchichtenTest {
    private val filter = WhisperHallucinationFilter()
    private val analyzer = SpeechAnalyzer()

    /** Stimmhaft-Zeitachse wie aus MicRecorder, aus den echten Silero-Werten des Testfalls. */
    private fun stimmhaft(name: String): BooleanArray {
        val werte = javaClass.classLoader!!.getResourceAsStream("vad/$name.txt")!!.bufferedReader().readLines()
            .drop(1).filter { it.isNotBlank() }.map { it.split(' ')[0].toFloat() }
        return BooleanArray(werte.size) { werte[it] >= SprachZeitachse.STIMMHAFT }
    }

    /** 16-kHz-WAV mit lautem Rauschen (RMS ≈ 0,06 wie Fahrgeräusch) über [sek] Sekunden. */
    private fun rauschWav(sek: Double): ByteArray {
        val n = (sek * 16_000).toInt()
        val wav = ByteArray(44 + n * 2)
        wav[24] = 0x80.toByte(); wav[25] = 0x3E // 16000 Hz
        val zufall = Random(1)
        for (i in 0 until n) {
            val wert = (zufall.nextDouble(-1.0, 1.0) * 0.1 * 32_767).toInt()
            wav[44 + i * 2] = wert.toByte(); wav[45 + i * 2] = (wert shr 8).toByte()
        }
        return wav
    }

    private fun segment(von: Double, bis: Double, text: String, noSpeech: Double = 0.05, logprob: Double = -0.2, kompression: Double = 1.2) =
        GroqSegment(von, bis, text, noSpeech, logprob, kompression)

    @Test
    fun schicht1ReinesFahrgeraeuschGehtNichtHoch() {
        val wav = rauschWav(10.0)
        // Am Pegel gemessen ist im Auto alles „Sprache“ — die Schicht griff dort nie.
        assertTrue(analyzer.analyze(wav)!!.voicedMs >= SpeechAnalyzer.MIN_SPEECH_MS)
        // Mit der Spracherkennung bleibt reines Fahrgeräusch unter der Schwelle und wird vor dem Hochladen verworfen.
        val mitVad = analyzer.analyze(wav, stimmhaft("auto_nur_fahrt"))!!
        assertTrue("stimmhaft ${mitVad.voicedMs} ms", mitVad.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS)
    }

    @Test
    fun schicht2UnsicheresSegmentFaellt() {
        val antwort = GroqTranscriptionResponse(
            "Wie wird das Wetter? Danke.",
            listOf(segment(0.0, 2.0, " Wie wird das Wetter?"), segment(2.0, 4.0, " Danke.", noSpeech = 0.8, logprob = -1.4)),
        )
        assertEquals("Wie wird das Wetter?", filter.filter(antwort, null))
    }

    @Test
    fun schicht3AngehaengtesWortImFahrgeraeuschFaellt() {
        // Echte Sprache 0,76–6,61 s, danach nur Fahrgeräusch. Whisper hängt dort sicher klingend etwas an.
        val antwort = GroqTranscriptionResponse(
            "Wie wird das Wetter morgen in Neuenhagen? Und tschüss.",
            listOf(
                segment(0.7, 6.6, " Wie wird das Wetter morgen in Neuenhagen?"),
                segment(7.5, 10.5, " Und tschüss."),
            ),
        )
        val wav = rauschWav(13.6)
        // Vorher (Pegel): Im Fahrgeräusch gilt alles als laut, das Anhängsel bleibt stehen.
        assertEquals(
            "Wie wird das Wetter morgen in Neuenhagen? Und tschüss.",
            filter.filter(antwort, analyzer.analyze(wav)),
        )
        // Jetzt (Spracherkennung): Das Segment ohne Stimme fällt, der echte Satz bleibt.
        assertEquals(
            "Wie wird das Wetter morgen in Neuenhagen?",
            filter.filter(antwort, analyzer.analyze(wav, stimmhaft("auto_sprache_dann_fahrt"))),
        )
    }

    @Test
    fun schicht4FloskelNurInStille() {
        val nurFahrt = analyzer.analyze(rauschWav(10.0), stimmhaft("auto_nur_fahrt"))
        val mitSprache = analyzer.analyze(rauschWav(13.6), stimmhaft("auto_sprache_dann_fahrt"))
        val floskel = GroqTranscriptionResponse("Untertitel im Auftrag des ZDF.", null)
        assertEquals("", filter.filter(floskel, nurFahrt))
        assertEquals("", filter.filter(GroqTranscriptionResponse("Danke fürs Zuschauen!", null), nurFahrt))
        // Echt gesprochenes „Vielen Dank“ bleibt stehen: Keine Floskel wird pauschal entfernt.
        assertEquals("Vielen Dank.", filter.filter(GroqTranscriptionResponse("Vielen Dank.", null), mitSprache))
    }
}
