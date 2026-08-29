package de.frank.genialeideen.audio

import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.text.UmlautKorrektur
import kotlin.math.min

/**
 * Nimmt eine fertige WAV-Aufnahme und macht Text daraus (Baustein F).
 *
 * Kern ist der **413-Schnitt**: Groq lehnt zu grosse Uploads ab, und ein 413 ist nicht
 * wiederholbar — ein 15-Minuten-Diktat wäre sonst komplett verloren. Darum wird alles über
 * [SCHNITT_AB_BYTES] vorher in Teile von rund [ZIEL_TEIL_BYTES] geschnitten, jeder Teil einzeln
 * transkribiert und die Texte werden zusammengesetzt. Fällt ein Teil aus, gehen nur dessen
 * Sekunden verloren.
 */
class Diktat(private val transcriber: GroqTranscriber) {

    data class Ergebnis(val text: String, val teileGesamt: Int, val teileFehlend: Int)

    suspend fun transkribiere(wav: ByteArray): Ergebnis {
        val teile = schneide(wav)
        if (teile.size > 1) {
            IdeenLog.info(
                "Diktat",
                "transkribiere",
                "Aufnahme geschnitten",
                mapOf("bytes" to wav.size, "teile" to teile.size),
            )
        }
        val stuecke = mutableListOf<String>()
        var fehlend = 0
        teile.forEach { teil ->
            runCatching { transcriber.transcribe(teil) }
                .onSuccess { text -> if (text.isNotBlank()) stuecke += text.trim() }
                .onFailure { fehler ->
                    fehlend++
                    IdeenLog.warn(
                        "Diktat",
                        "transkribiere",
                        "Ein Teil kam nicht durch",
                        mapOf("art" to fehler.javaClass.simpleName),
                    )
                }
        }
        if (stuecke.isEmpty() && fehlend > 0) {
            throw GroqTranscriptionException(
                "Die Aufnahme konnte nicht übertragen werden. Prüf den Groq-Schlüssel in den Einstellungen.",
            )
        }
        // Baustein M.2: Der Weg von Whisper bis ins Textfeld bleibt UTF-8; wo doch
        // Ersatzschreibung ankommt, greift die Wörterbuch-Korrektur.
        val zusammen = UmlautKorrektur.korrigiere(stuecke.joinToString(" ")) { ersetzung ->
            IdeenLog.debug(
                "Diktat",
                "transkribiere",
                "Umlaut korrigiert",
                mapOf("vorher" to ersetzung.vorher, "nachher" to ersetzung.nachher),
            )
        }
        return Ergebnis(zusammen.trim(), teile.size, fehlend)
    }

    /**
     * Schneidet die WAV-Daten an einer Sprechpause im letzten 45-Sekunden-Fenster vor der
     * Zielgrösse. Findet sich keine Pause, wird an der Zielgrösse geschnitten — eine harte
     * Kante ist besser als ein abgelehnter Upload.
     */
    internal fun schneide(wav: ByteArray): List<ByteArray> {
        if (wav.size <= SCHNITT_AB_BYTES) return listOf(wav)
        val kopf = wav.copyOfRange(0, KOPF_BYTES.coerceAtMost(wav.size))
        val daten = wav.copyOfRange(kopf.size, wav.size)
        val teile = mutableListOf<ByteArray>()
        var anfang = 0
        while (anfang < daten.size) {
            val maxEnde = min(anfang + ZIEL_TEIL_BYTES, daten.size)
            val ende = if (maxEnde >= daten.size) daten.size else findePause(daten, anfang, maxEnde)
            teile += kopf + daten.copyOfRange(anfang, ende)
            anfang = ende
        }
        return teile.map(::korrigiereWavLaenge)
    }

    /** Sucht rückwärts vom Ende her das leiseste 20-ms-Fenster im letzten Suchbereich. */
    private fun findePause(daten: ByteArray, anfang: Int, maxEnde: Int): Int {
        val suchAnfang = (maxEnde - SUCHFENSTER_BYTES).coerceAtLeast(anfang + 1)
        var bestesEnde = maxEnde
        var bestesMass = Long.MAX_VALUE
        var position = suchAnfang
        while (position + RAHMEN_BYTES <= maxEnde) {
            var summe = 0L
            var index = position
            while (index + 1 < position + RAHMEN_BYTES) {
                val wert = ((daten[index + 1].toInt() shl 8) or (daten[index].toInt() and 0xFF)).toShort()
                summe += kotlin.math.abs(wert.toInt()).toLong()
                index += 2
            }
            if (summe < bestesMass) {
                bestesMass = summe
                bestesEnde = position + RAHMEN_BYTES
            }
            position += RAHMEN_BYTES
        }
        // Der Schnitt liegt immer auf einer geraden Byte-Grenze, sonst zerfällt das 16-Bit-Muster.
        return (bestesEnde - bestesEnde % 2).coerceIn(anfang + 2, maxEnde)
    }

    /** Trägt die tatsächliche Länge in den WAV-Kopf ein, sonst spielt kein Dienst den Teil ab. */
    private fun korrigiereWavLaenge(teil: ByteArray): ByteArray {
        if (teil.size < KOPF_BYTES) return teil
        val datenLaenge = teil.size - KOPF_BYTES
        schreibeLittleEndian(teil, 4, teil.size - 8)
        schreibeLittleEndian(teil, 40, datenLaenge)
        return teil
    }

    private fun schreibeLittleEndian(ziel: ByteArray, offset: Int, wert: Int) {
        ziel[offset] = (wert and 0xFF).toByte()
        ziel[offset + 1] = ((wert shr 8) and 0xFF).toByte()
        ziel[offset + 2] = ((wert shr 16) and 0xFF).toByte()
        ziel[offset + 3] = ((wert shr 24) and 0xFF).toByte()
    }

    companion object {
        /** Ab hier wird geschnitten — deutlich unter der Grenze, ab der Groq mit 413 antwortet. */
        const val SCHNITT_AB_BYTES = 20 * 1024 * 1024

        /** Zielgrösse je Teil. */
        const val ZIEL_TEIL_BYTES = 16 * 1024 * 1024

        /** Ein WAV-Kopf aus [MicRecorder] ist genau 44 Byte lang. */
        const val KOPF_BYTES = 44

        /** 45 Sekunden bei 16 kHz mono 16 bit. */
        const val SUCHFENSTER_BYTES = 45 * 16_000 * 2

        /** 20 ms bei 16 kHz mono 16 bit. */
        const val RAHMEN_BYTES = 640
    }
}
