package de.frank.codexkompass.audio

import de.frank.codexkompass.observability.KompassLog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Zerlegt eine zu große Aufnahme in Teile, die der Dienst noch annimmt (Referenz, Baustein F).
 *
 * Hintergrund: Groq lehnt zu große Uploads mit Fehler 413 ab, und dieser Fehler ist NICHT
 * wiederholbar — ein zu langes Diktat wäre damit vollständig verloren. Am 29.08.2026 ist genau
 * das passiert: 15,4 Minuten ergaben 29,5 MB, der Dienst lehnte ab, der ganze Text war weg.
 *
 * Geschnitten wird deshalb VOR dem Senden, und zwar an einer Sprechpause: Die leiseste Stelle
 * im letzten Zeitfenster vor dem Ziel wird gesucht, damit kein Wort mitten durchtrennt wird.
 * Fällt ein Teil später doch aus, gehen nur dessen Sekunden verloren statt der ganzen Aufnahme.
 */
object WavSchneider {

    private const val KOPF_BYTES = 44

    /** Ab dieser Grösse wird geschnitten. */
    const val SCHNITT_AB_BYTES = 20 * 1024 * 1024

    /**
     * Wie gross ein Teil im Verhältnis zur Grenze werden darf.
     *
     * Bewusst aus der Grenze abgeleitet statt fest eingetragen: Sonst laufen beide Werte beim
     * nächsten Anpassen auseinander, und die Teile wären entweder unnötig klein oder wieder
     * zu gross. Vier Fünftel lassen genug Abstand zur Ablehnung.
     */
    const val ZIEL_ANTEIL = 0.8

    /** In diesem Zeitfenster vor dem Ziel wird nach der Sprechpause gesucht. */
    const val PAUSENSUCHE_SEKUNDEN = 45

    /**
     * Liefert die Aufnahme als eine Liste sendefertiger WAV-Dateien.
     *
     * Ist sie klein genug, kommt sie unverändert als einziges Element zurück — der übliche Fall
     * darf keinen zusätzlichen Aufwand kosten.
     */
    fun teileWennNoetig(wav: ByteArray, grenzeBytes: Int = SCHNITT_AB_BYTES): List<ByteArray> {
        if (wav.size <= grenzeBytes) return listOf(wav)
        if (wav.size <= KOPF_BYTES) return listOf(wav)

        val rate = leseAbtastrate(wav)
        val pcm = wav.copyOfRange(KOPF_BYTES, wav.size)
        // Mindestens fünf Sekunden je Teil: Kürzere Stücke bringen die Erkennung aus dem Tritt,
        // weil ihr der Zusammenhang fehlt.
        val zielPcmBytes = ((grenzeBytes * ZIEL_ANTEIL).toInt() - KOPF_BYTES)
            .coerceAtLeast(rate * 2 * 5)

        val teile = mutableListOf<ByteArray>()
        var beginn = 0
        while (beginn < pcm.size) {
            val rohesEnde = (beginn + zielPcmBytes).coerceAtMost(pcm.size)
            val ende = if (rohesEnde >= pcm.size) {
                pcm.size
            } else {
                findeSprechpause(pcm, beginn, rohesEnde, rate)
            }
            teile += baueWav(pcm.copyOfRange(beginn, ende), rate)
            beginn = ende
        }

        KompassLog.info(
            "WavSchneider",
            "teileWennNoetig",
            "Aufnahme vor dem Senden geteilt",
            mapOf(
                "gesamtBytes" to wav.size,
                "teile" to teile.size,
                "sekundenGesamt" to (pcm.size / (rate * 2)),
            ),
        )
        return teile
    }

    /**
     * Sucht die leiseste Stelle im letzten Zeitfenster vor [rohesEnde].
     *
     * Gibt es keine brauchbare Stelle, wird an [rohesEnde] geschnitten. Ein hart geschnittenes
     * Wort ist unschön, aber immer noch besser als eine abgelehnte Aufnahme.
     */
    private fun findeSprechpause(pcm: ByteArray, beginn: Int, rohesEnde: Int, rate: Int): Int {
        val fensterBytes = PAUSENSUCHE_SEKUNDEN * rate * 2
        val suchStart = (rohesEnde - fensterBytes).coerceAtLeast(beginn + rate * 2)
        if (suchStart >= rohesEnde) return rohesEnde

        val rahmenBytes = (rate * SprachAnalysator.RAHMEN_MS / 1_000) * 2
        if (rahmenBytes <= 0) return rohesEnde

        var leisesteStelle = rohesEnde
        var leisesterWert = Double.MAX_VALUE
        var stelle = suchStart - (suchStart % 2)
        while (stelle + rahmenBytes <= rohesEnde) {
            val lautstaerke = rahmenLautstaerke(pcm, stelle, rahmenBytes)
            // "<=" statt "<": Bei gleich leisen Rahmen gewinnt der spätere. Das hält die Teile
            // so gross wie möglich und damit ihre Anzahl klein.
            if (lautstaerke <= leisesterWert) {
                leisesterWert = lautstaerke
                leisesteStelle = stelle
            }
            stelle += rahmenBytes
        }
        return if (leisesteStelle > beginn) leisesteStelle else rohesEnde
    }

    private fun rahmenLautstaerke(pcm: ByteArray, von: Int, laenge: Int): Double {
        var quadratsumme = 0.0
        var anzahl = 0
        var index = von
        val bis = (von + laenge).coerceAtMost(pcm.size - 1)
        while (index + 1 < bis) {
            val wert = ((pcm[index].toInt() and 0xFF) or (pcm[index + 1].toInt() shl 8)).toShort()
            val normiert = wert / 32_768.0
            quadratsumme += normiert * normiert
            anzahl++
            index += 2
        }
        return if (anzahl == 0) Double.MAX_VALUE else sqrt(quadratsumme / anzahl)
    }

    fun leseAbtastrate(wav: ByteArray): Int {
        if (wav.size < 28) return 16_000
        val rate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        return if (rate > 0) rate else 16_000
    }

    fun baueWav(pcm: ByteArray, rate: Int): ByteArray {
        val kanaele = 1
        val bits = 16
        val byteRate = rate * kanaele * bits / 8
        val kopf = ByteBuffer.allocate(KOPF_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(kanaele.toShort())
            putInt(rate)
            putInt(byteRate)
            putShort((kanaele * bits / 8).toShort())
            putShort(bits.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }.array()
        return kopf + pcm
    }
}
