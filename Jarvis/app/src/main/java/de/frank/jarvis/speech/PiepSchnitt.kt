package de.frank.jarvis.speech

/**
 * Schneidet im Gesprächsmodus alles vor dem Ende des Bereit-Pieptons aus der Aufnahme: das Ende der eigenen Antwort
 * (Nachklang über Lautsprecher oder Bluetooth) und den Piepton selbst. Nur was Frank nach dem Piepton sagt, geht in die Mitschrift.
 *
 * Wo der Piepton am Mikrofon wirklich liegt, hängt vom Ausgang ab (Handy-Lautsprecher, Bluetooth mit Verzögerung). Deshalb
 * wird er im Ton selbst gesucht: TONE_PROP_BEEP ist 400 Hz + 1200 Hz, 35 ms. Je 10-ms-Fenster misst Goertzel den Anteil dieser
 * beiden Frequenzen an der Energie, beide müssen da sein. Als Piepton zählt nur ein kurzer Lauf solcher Fenster (20 bis 80 ms),
 * nach dem es deutlich leiser wird: Ein Vokal mit Obertönen bei 400 und 1200 Hz hält länger an und fällt nicht so ab.
 * Ab dem Beginn des Pieptons fallen höchstens Ton und kurzer Nachhall weg; nie mehr, damit kein Satzanfang fehlt.
 */
object PiepSchnitt {
    private const val KOPF = 44
    /** So lange nach der Marke darf der Piepton beginnen (Aufbau des Tongebers, Bluetooth-Verzögerung, Eingangspuffer). */
    private const val SUCHE_MS = 900
    /** Ab dem ersten Piepton-Fenster fällt so viel weg: 35 ms Ton, ein Fenster Raster, kurzer Nachhall. */
    private const val PIEP_UND_NACHHALL_MS = 75
    /** Anteil beider Piepton-Frequenzen an der Energie eines Fensters (reiner Ton ≈ 1) und Mindestanteil jeder einzelnen. */
    private const val ANTEIL = 0.45
    private const val ANTEIL_JE = 0.05
    /** Darunter ist ein Fenster zu leise, um überhaupt etwas zu sein (Effektivwert, 0 bis 1). */
    private const val MIN_PEGEL = 0.004
    /** Länge eines Piepton-Laufs in Fenstern (10 ms): 35 ms Ton, mit Nachhall höchstens 80 ms. */
    private const val LAUF_MIN = 2
    private const val LAUF_MAX = 8
    /** Nach dem Lauf muss binnen so vieler Fenster die Energie unter diesen Anteil fallen. */
    private const val ABFALL_FENSTER = 4
    private const val ABFALL_ANTEIL = 0.35
    /** 32-ms-Rahmen der Spracherkennung bei 16 kHz, 16 Bit. */
    private const val RAHMEN_BYTES = 1_024

    /** Ergebnis: geschnittene WAV, dazu die passende Stimmhaft-Zeitachse und wie viel Ton (ms) ab der Marke wegfiel. */
    class Ergebnis(val wav: ByteArray, val stimmhaft: BooleanArray?, val wegMs: Long, val piepGefunden: Boolean)

    /**
     * [abBytes]: PCM-Stelle, ab der die Aufnahme zählt; alles davor fällt weg, mit [suchen] danach der Piepton, soweit er
     * im Ton zu finden ist. [stimmhaft] je 32-ms-Rahmen ab Aufnahmebeginn (null ohne Spracherkennung).
     */
    fun schneiden(wav: ByteArray, abBytes: Int, stimmhaft: BooleanArray?, suchen: Boolean = true): Ergebnis {
        if (wav.size <= KOPF) return Ergebnis(wav, stimmhaft, 0, false)
        val kopf = java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val rate = kopf.getInt(24).takeIf { it > 0 } ?: 16_000
        val pcmLaenge = wav.size - KOPF
        val ab = abBytes.coerceIn(0, pcmLaenge) / 2 * 2
        val fb = rate / 100 * 2
        val piep = if (suchen) suche(wav, ab, minOf(pcmLaenge, ab + SUCHE_MS * rate / 1_000 * 2), pcmLaenge, fb, rate) else null
        val roh = if (piep != null) minOf(pcmLaenge, maxOf(piep.first + PIEP_UND_NACHHALL_MS * rate / 1_000 * 2, piep.second)) else ab
        // Auf ganze Rahmen der Spracherkennung abrunden (Stimmhaft-Zeitachse passt dann genau); lieber ein Rest Nachhall als ein fehlender Satzanfang.
        val schnitt = if (rate == 16_000) roh / RAHMEN_BYTES * RAHMEN_BYTES else roh / 2 * 2
        val rest = pcmLaenge - schnitt
        val neu = ByteArray(KOPF + rest)
        System.arraycopy(wav, 0, neu, 0, KOPF)
        System.arraycopy(wav, KOPF + schnitt, neu, KOPF, rest)
        java.nio.ByteBuffer.wrap(neu).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(4, neu.size - 8).putInt(40, rest)
        val achse = if (stimmhaft != null && rate == 16_000) {
            val weg = schnitt / RAHMEN_BYTES
            if (weg >= stimmhaft.size) BooleanArray(0) else stimmhaft.copyOfRange(weg, stimmhaft.size)
        } else null
        val wegMs = (schnitt - ab).coerceAtLeast(0).toLong() * 1_000 / 2 / rate
        return Ergebnis(neu, achse, wegMs, piep != null)
    }

    /** Erster Piepton-Lauf, der zwischen [von] und [bis] beginnt: Beginn und Ende in PCM-Bytes, sonst null. */
    private fun suche(wav: ByteArray, von: Int, bis: Int, pcmLaenge: Int, fb: Int, rate: Int): Pair<Int, Int>? {
        var pos = von
        while (pos + fb <= bis) {
            if (!istPiep(wav, pos, fb, rate)) { pos += fb; continue }
            var lauf = 1
            var summe = energie(wav, pos, fb)
            while (lauf <= LAUF_MAX && pos + (lauf + 1) * fb <= pcmLaenge && istPiep(wav, pos + lauf * fb, fb, rate)) {
                summe += energie(wav, pos + lauf * fb, fb); lauf++
            }
            if (lauf in LAUF_MIN..LAUF_MAX) {
                val mittel = summe / lauf
                val faellt = (0 until ABFALL_FENSTER).any { k ->
                    val p = pos + (lauf + k) * fb
                    p + fb <= pcmLaenge && energie(wav, p, fb) < mittel * ABFALL_ANTEIL
                }
                if (faellt) return pos to pos + lauf * fb
            }
            pos += lauf * fb
        }
        return null
    }

    private fun probe(wav: ByteArray, pcmPos: Int): Double {
        val o = KOPF + pcmPos
        return ((wav[o + 1].toInt() shl 8) or (wav[o].toInt() and 0xFF)).toShort() / 32_768.0
    }

    /** Mittlere Energie (Quadrat) eines Fensters. */
    private fun energie(wav: ByteArray, pos: Int, fb: Int): Double {
        var e = 0.0
        var i = 0
        while (i < fb) { val v = probe(wav, pos + i); e += v * v; i += 2 }
        return e / (fb / 2)
    }

    /** Ist dieses Fenster Piepton-Ton? Anteil von 400 Hz und 1200 Hz an der Energie (Goertzel), beide müssen da sein. */
    private fun istPiep(wav: ByteArray, pos: Int, fb: Int, rate: Int): Boolean {
        val n = fb / 2
        val werte = DoubleArray(n) { probe(wav, pos + it * 2) }
        val energie = werte.sumOf { it * it }
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
