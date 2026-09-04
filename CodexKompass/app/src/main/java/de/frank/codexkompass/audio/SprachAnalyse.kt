package de.frank.codexkompass.audio

import de.frank.codexkompass.observability.KompassLog
import kotlin.math.sqrt

/**
 * Ergebnis der Stille-Erkennung: welche 20-Millisekunden-Abschnitte laut genug für Sprache waren.
 */
class SprachAnalyse(
    val gesprochenMs: Int,
    private val rahmenMs: Int,
    private val lauteRahmen: BooleanArray,
) {

    /**
     * War in diesem Zeitfenster überhaupt jemand zu hören?
     *
     * Wird von Schicht 3 benutzt, um Segmente zu verwerfen, deren Zeitraum im Ton still war.
     * Ein leeres Ergebnis liefert bewusst `true`: Ohne Analyse darf nichts verworfen werden.
     */
    fun abschnittHatSprache(
        vonSekunde: Double,
        bisSekunde: Double,
        mindestAnteil: Double = MIND_ANTEIL,
    ): Boolean {
        if (lauteRahmen.isEmpty() || bisSekunde <= vonSekunde) return true
        val erster = (vonSekunde * 1_000.0 / rahmenMs).toInt().coerceIn(0, lauteRahmen.lastIndex)
        val letzter = (bisSekunde * 1_000.0 / rahmenMs).toInt().coerceIn(erster, lauteRahmen.lastIndex)
        var laute = 0
        for (index in erster..letzter) if (lauteRahmen[index]) laute++
        return laute.toDouble() / (letzter - erster + 1).toDouble() >= mindestAnteil
    }

    private companion object {
        /** Ein Zehntel laute Rahmen genügen — Sprache hat naturgemäss viele Pausen. */
        const val MIND_ANTEIL = 0.10
    }
}

/**
 * Schicht 1 der Halluzinations-Abwehr: Stille erkennen, BEVOR gesendet wird.
 *
 * Whisper erfindet bei Stille ganze Sätze. Der billigste und wirksamste Gegenzug ist, stille
 * Aufnahmen gar nicht erst hochzuladen — das spart zugleich Geld und Wartezeit.
 *
 * Die Analyse schlägt nie fehl: Kann sie den Ton nicht deuten, liefert sie `null`, und die
 * folgenden Schichten arbeiten ohne sie weiter. Ein Analysefehler darf niemals eine gültige
 * Aufnahme verwerfen.
 */
class SprachAnalysator {

    fun analysiere(wav: ByteArray?): SprachAnalyse? = runCatching { lies(wav) }
        .getOrElse { fehler ->
            KompassLog.warn("SprachAnalysator", "analysiere", "Tonanalyse fehlgeschlagen", mapOf("grund" to fehler.message))
            null
        }

    private fun lies(wav: ByteArray?): SprachAnalyse? {
        if (wav == null || wav.size <= WAV_KOPF_BYTES + 4) return null

        var rate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        if (rate <= 0) rate = ERSATZ_RATE

        val werteProRahmen = (rate * RAHMEN_MS / 1_000).coerceAtLeast(1)
        val bytesProRahmen = werteProRahmen * 2
        val rahmenAnzahl = (wav.size - WAV_KOPF_BYTES) / bytesProRahmen
        if (rahmenAnzahl <= 0) return null

        val laute = BooleanArray(rahmenAnzahl)
        for (rahmen in 0 until rahmenAnzahl) {
            val beginn = WAV_KOPF_BYTES + rahmen * bytesProRahmen
            var quadratsumme = 0.0
            for (wertIndex in 0 until werteProRahmen) {
                val byteIndex = beginn + wertIndex * 2
                val wert = (
                    (wav[byteIndex].toInt() and 0xFF) or (wav[byteIndex + 1].toInt() shl 8)
                    ).toShort()
                val normiert = wert / 32_768.0
                quadratsumme += normiert * normiert
            }
            laute[rahmen] = sqrt(quadratsumme / werteProRahmen) >= LAUTSTAERKE_SCHWELLE
        }

        return SprachAnalyse(
            gesprochenMs = laute.count { it } * RAHMEN_MS,
            rahmenMs = RAHMEN_MS,
            lauteRahmen = laute,
        )
    }

    companion object {
        const val RAHMEN_MS = 20
        const val LAUTSTAERKE_SCHWELLE = 0.015

        /** Weniger als das ist keine Sprache, sondern ein Fehlgriff auf den Knopf. */
        const val MIND_SPRACHE_MS = 150

        private const val WAV_KOPF_BYTES = 44
        private const val ERSATZ_RATE = 16_000
    }
}
