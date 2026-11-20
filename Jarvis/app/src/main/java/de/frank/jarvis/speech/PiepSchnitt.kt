package de.frank.jarvis.speech

/**
 * Schneidet im Gesprächsmodus alles vor dem Ende des Bereit-Pieptons aus der Aufnahme: das Ende der eigenen Antwort
 * (Nachklang über Lautsprecher oder Bluetooth) und den Piepton selbst. Nur was Frank nach dem Piepton sagt, geht in die Mitschrift.
 *
 * Wo der Piepton am Mikrofon wirklich endet, hängt vom Ausgang ab (Handy-Lautsprecher, Bluetooth mit Verzögerung). Deshalb
 * wird er im Ton selbst gesucht: TONE_PROP_BEEP ist 400 Hz + 1200 Hz. Je 10-ms-Fenster misst Goertzel den Anteil dieser beiden
 * Frequenzen an der Energie, beide müssen da sein; Sprache ist nie so rein. Gesucht wird nur kurz nach dem Start des Pieptons; ab dem ersten Treffer fallen höchstens Ton und kurzer Nachhall weg.
 * Eine feste Zeit wäre entweder zu kurz (Bluetooth) oder schnitte bei sofortigem Sprechen den Satzanfang ab.
 */
object PiepSchnitt {
    private const val KOPF = 44
    /** Nur so lange nach dem Start des Pieptons darf er beginnen (Bluetooth verzögert); später ist es Frank, kein Piepton. */
    private const val SUCHE_MS = 450
    /** Ab dem ersten Piepton-Fenster fällt so viel weg: 35 ms Ton, ein Fenster Raster, kurzer Nachhall. Nie mehr, damit kein Satzanfang fehlt. */
    private const val PIEP_UND_NACHHALL_MS = 75
    /** Anteil beider Piepton-Frequenzen an der Energie eines Fensters (reiner Ton ≈ 1) und Mindestanteil jeder einzelnen. */
    private const val ANTEIL = 0.45
    private const val ANTEIL_JE = 0.05
    /** Darunter ist ein Fenster zu leise, um überhaupt etwas zu sein (Effektivwert, 0 bis 1). */
    private const val MIN_PEGEL = 0.004
    /** 32-ms-Rahmen der Spracherkennung bei 16 kHz, 16 Bit. */
    private const val RAHMEN_BYTES = 1_024

    /** Ergebnis: geschnittene WAV, dazu die passende Stimmhaft-Zeitachse und wie viel Ton (ms) wegfiel. */
    class Ergebnis(val wav: ByteArray, val stimmhaft: BooleanArray?, val wegMs: Long, val piepGefunden: Boolean)

    /**
     * [abBytes]: PCM-Stelle, an der der Piepton gestartet wurde; alles davor fällt weg, danach der Piepton, soweit er
     * im Ton zu finden ist. [stimmhaft] je 32-ms-Rahmen ab Aufnahmebeginn (null ohne Spracherkennung).
     */
    fun schneiden(wav: ByteArray, abBytes: Int, stimmhaft: BooleanArray?): Ergebnis {
        if (wav.size <= KOPF) return Ergebnis(wav, stimmhaft, 0, false)
        val kopf = java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val rate = kopf.getInt(24).takeIf { it > 0 } ?: 16_000
        val pcmLaenge = wav.size - KOPF
        val ab = abBytes.coerceIn(0, pcmLaenge) / 2 * 2
        val fenster = rate / 100
        val bisSuche = minOf(pcmLaenge, ab + SUCHE_MS * rate / 1_000 * 2)
        var erstesPiep = -1
        var pos = ab
        while (pos + fenster * 2 <= bisSuche) {
            if (istPiep(wav, KOPF + pos, fenster, rate)) { erstesPiep = pos; break }
            pos += fenster * 2
        }
        val gefunden = erstesPiep >= 0
        val roh = if (gefunden) minOf(pcmLaenge, erstesPiep + PIEP_UND_NACHHALL_MS * rate / 1_000 * 2) else ab
        // Auf ganze Rahmen der Spracherkennung aufrunden, damit die Stimmhaft-Zeitachse genau zur geschnittenen WAV passt.
        val schnitt = if (rate == 16_000) minOf(pcmLaenge, (roh + RAHMEN_BYTES - 1) / RAHMEN_BYTES * RAHMEN_BYTES) else roh / 2 * 2
        val rest = pcmLaenge - schnitt
        val neu = ByteArray(KOPF + rest)
        System.arraycopy(wav, 0, neu, 0, KOPF)
        System.arraycopy(wav, KOPF + schnitt, neu, KOPF, rest)
        java.nio.ByteBuffer.wrap(neu).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(4, neu.size - 8).putInt(40, rest)
        val achse = if (stimmhaft != null && rate == 16_000) {
            val weg = schnitt / RAHMEN_BYTES
            if (weg >= stimmhaft.size) BooleanArray(0) else stimmhaft.copyOfRange(weg, stimmhaft.size)
        } else null
        val wegMs = (roh - ab).toLong() * 1_000 / 2 / rate
        return Ergebnis(neu, achse, wegMs, gefunden)
    }

    /** Ist dieses Fenster der Piepton? Anteil von 400 Hz und 1200 Hz an der Energie (Goertzel). */
    private fun istPiep(wav: ByteArray, start: Int, n: Int, rate: Int): Boolean {
        var energie = 0.0
        val werte = DoubleArray(n)
        for (i in 0 until n) {
            val o = start + i * 2
            val v = ((wav[o + 1].toInt() shl 8) or (wav[o].toInt() and 0xFF)).toShort() / 32_768.0
            werte[i] = v
            energie += v * v
        }
        if (energie <= 0.0 || kotlin.math.sqrt(energie / n) < MIN_PEGEL) return false
        val bezug = energie * n / 2.0
        val tief = goertzel(werte, 400.0, rate) / bezug
        val hoch = goertzel(werte, 1_200.0, rate) / bezug
        return tief >= ANTEIL_JE && hoch >= ANTEIL_JE && tief + hoch >= ANTEIL
    }

    private fun goertzel(x: DoubleArray, freq: Double, rate: Int): Double {
        val k = 2.0 * kotlin.math.cos(2.0 * Math.PI * freq / rate)
        var s1 = 0.0
        var s2 = 0.0
        for (v in x) {
            val s = v + k * s1 - s2
            s2 = s1
            s1 = s
        }
        return s1 * s1 + s2 * s2 - k * s1 * s2
    }
}
