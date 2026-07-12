package de.frank.cortex.audio

import de.frank.cortex.data.model.GroqSegment
import de.frank.cortex.data.model.GroqTranscriptionResponse
import de.frank.cortex.observability.CortexLog

/**
 * Whisper-Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2).
 *
 * Whisper (Groq large-v3 / large-v3-turbo) erfindet bei Stille Floskeln ("Vielen Dank",
 * "Untertitel des ZDF") und haengt nach End-Pausen Woerter an ("Ja") — beides MIT hoher
 * Confidence. Der kanonische Schutz (identisch im Terminal Voice Overlay, GroqWhisperClient.cs)
 * sind VIER funktionserhaltende Schichten. Echte (auch leise/kurze) Sprache bleibt IMMER:
 *
 *  1. Sprachgehalt-Vorfilter VOR dem Senden ([SpeechAnalyzer]) — < 150 ms echte Sprache gar
 *     nicht erst an Groq schicken (faengt reine Stille, die Schicht 2-4 nicht fangen: Floskeln
 *     kommen mit HOHER Confidence). Diese Schicht liegt im Aufrufer (ChatViewModel).
 *  2. verbose_json-Confidence-Gate — pro Segment verwerfen bei no_speech_prob > 0.6 UND
 *     avg_logprob < -1.0 (UND-Logik schuetzt leise Sprache), compression_ratio > 2.4
 *     (Repetition), Mini-Noise (< 0.4 s + no_speech > 0.6).
 *  3. Segment-Audio-Abgleich — Segment-Zeitfenster gegen die Voiced-Timeline der eigenen
 *     Aufnahme pruefen; < 10 % laute Frames = Trailing-Halluzination. Drift-Sicherung:
 *     verwirft Schicht 3 ALLE Segmente, die Schicht 2 liess, bleiben die Confidence-
 *     gefilterten erhalten (Whisper-Timestamps koennen driften — nie einen echten Satz verlieren).
 *  4. Floskel-Blocklist — letzter Filter an ALLEN Ausgaengen. Verwirft eine Outro-Floskel NUR
 *     bei drei gleichzeitigen Signalen: (1) Ausgabe kurz, (2) normalisierter EXAKTER Match,
 *     (3) Stille-Kontext (der ganze Clip war sprach-arm). Ein bewusst gesprochenes "Vielen Dank"
 *     (laute Zeit > Schwelle) bleibt.
 *
 * Leeres Ergebnis ("alles gefiltert") ist "" — der Aufrufer behandelt Blank als "nichts einfuegen".
 */
class WhisperHallucinationFilter {

    /** Schicht 2 (Confidence-Gate) + Schicht 3 (Segment-Audio-Abgleich) + Schicht 4 (Floskel-Blocklist). */
    fun filter(result: GroqTranscriptionResponse, analysis: SpeechAnalysis?): String {
        val segments = result.segments
        if (segments.isNullOrEmpty()) {
            // Ultrakurze Clips liefern oft KEINE Segmente — top-level Text durchreichen,
            // aber ZUERST durch Schicht 4 (sonst umgeht "Vielen Dank" bei Kurzclips den Schutz).
            return blockIfFloskel(result.text.trim(), analysis)
        }

        // Schicht 2: Confidence-Gate. UND-Logik (nie ODER) — echte leise Sprache hat zwar
        // erhoehtes no_speech_prob, aber gutes avg_logprob -> bleibt erhalten.
        val confident = segments.filter { seg ->
            val drop = isHallucination(seg)
            if (drop) {
                CortexLog.info(
                    "WhisperFilter", "layer2",
                    "Schicht 2: Segment verworfen ('${seg.text?.trim()?.take(60)}', " +
                        "no_speech=%.2f, logprob=%.2f, compression=%.2f)".format(
                            seg.no_speech_prob ?: 0.0, seg.avg_logprob ?: 0.0, seg.compression_ratio ?: 0.0,
                        ),
                )
            }
            !drop
        }
        if (confident.isEmpty()) {
            CortexLog.info("WhisperFilter", "layer2", "Schicht 2: alle ${segments.size} Segmente verworfen — leer")
            return ""
        }

        // Schicht 3: Segment-Audio-Abgleich gegen die Voiced-Timeline der Aufnahme.
        val aligned = if (analysis == null) {
            confident
        } else {
            confident.filter { seg ->
                val start = seg.start ?: return@filter true
                val end = seg.end ?: return@filter true
                val hasSpeech = analysis.segmentHasSpeech(start, end)
                if (!hasSpeech) {
                    CortexLog.warn(
                        "WhisperFilter", "layer3",
                        "Schicht 3: Segment ohne Schall im Zeitfenster verworfen " +
                            "('${seg.text?.trim()?.take(60)}', %.1f-%.1f s)".format(start, end),
                    )
                }
                hasSpeech
            }
        }

        // Drift-Sicherung: Whisper-Timestamps koennen driften. Wuerde Schicht 3 ALLES verwerfen,
        // obwohl Schicht 2 Segmente liess, behalten wir die Confidence-gefilterten (nie echten Satz verlieren).
        val kept = if (aligned.isEmpty()) {
            CortexLog.warn(
                "WhisperFilter", "layer3",
                "Schicht 3: haette ALLE ${confident.size} Segmente verworfen — Drift vermutet, Confidence-Ergebnis behalten",
            )
            confident
        } else {
            aligned
        }

        val text = kept.joinToString(" ") { it.text?.trim().orEmpty() }.trim()

        // Schicht 4: Floskel-Blocklist am finalen Ausgang.
        return blockIfFloskel(text, analysis)
    }

    /** Schicht 2: Halluzinations-Heuristik fuer EIN Segment. Regeln ODER-verknuepft; Stille-Regel intern UND. */
    private fun isHallucination(seg: GroqSegment): Boolean {
        val noSpeech = seg.no_speech_prob ?: 0.0
        val avgLogprob = seg.avg_logprob ?: 0.0
        val compression = seg.compression_ratio ?: 0.0
        if (noSpeech > NO_SPEECH_THRESHOLD && avgLogprob < AVG_LOGPROB_THRESHOLD) return true // Stille (UND!)
        if (compression > COMPRESSION_RATIO_THRESHOLD) return true                             // Repetition
        val dur = (seg.end ?: 0.0) - (seg.start ?: 0.0)
        if (dur > 0 && dur < MINI_NOISE_MAX_SEC && noSpeech > NO_SPEECH_THRESHOLD) return true  // Mini-Noise
        return false
    }

    /** Schicht 4 zentral: leert den Text, wenn er eine Stille-Floskel ist (sonst unveraendert). */
    private fun blockIfFloskel(text: String, analysis: SpeechAnalysis?): String {
        if (isBlocklistedFloskel(text, analysis)) {
            CortexLog.warn("WhisperFilter", "layer4", "Schicht 4: Floskel verworfen (\"$text\" — kurz + exakt + Stille-Kontext)")
            return ""
        }
        return text
    }

    /**
     * Schicht 4: true, wenn der Gesamttext eine Whisper-Outro-Floskel ist. Verwirft NUR bei drei
     * gleichzeitigen Signalen — kurz, normalisierter EXAKTER Blocklist-Match, Stille-Kontext.
     * Ohne Voiced-Timeline (analysis == null) wird NICHT verworfen (Kontext nicht messbar -> funktionserhaltend).
     */
    private fun isBlocklistedFloskel(text: String, analysis: SpeechAnalysis?): Boolean {
        if (text.isEmpty() || text.length > FLOSKEL_MAX_CHARS) return false
        if (analysis == null) return false // Stille-Kontext nicht messbar -> echte Sprache nie verlieren
        val norm = normalizeFloskel(text)
        if (norm.isEmpty()) return false
        if (norm.split(' ').filter { it.isNotEmpty() }.size > FLOSKEL_MAX_WORDS) return false
        if (norm !in FLOSKEL_BLOCKLIST) return false // exakter Match (== nicht contains)
        // Stille-Kontext: war der ganze Clip sprach-arm? Bewusst gesprochene Floskel hat mehr laute Zeit.
        return analysis.voicedMs < SILENCE_CONTEXT_MAX_VOICED_MS
    }

    /** lowercase, Satzzeichen/Ziffern entfernt (Umlaute bleiben), Whitespace kollabiert. */
    private fun normalizeFloskel(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) {
            when {
                c.isLetter() -> sb.append(c)
                c.isWhitespace() -> sb.append(' ')
                // Satzzeichen, Ziffern, Symbole weglassen
            }
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    companion object {
        /** Schicht 2: Stille-Gate nur in UND-Kombination mit [AVG_LOGPROB_THRESHOLD]. */
        private const val NO_SPEECH_THRESHOLD = 0.6
        private const val AVG_LOGPROB_THRESHOLD = -1.0

        /** Schicht 2: Repetitions-Halluzination ("danke danke danke …"). */
        private const val COMPRESSION_RATIO_THRESHOLD = 2.4

        /** Schicht 2: Mini-Noise — sehr kurze Segmente mit Stille-Verdacht. */
        private const val MINI_NOISE_MAX_SEC = 0.4

        /** Schicht 4: Floskel gilt nur bei kurzer Ausgabe + sprach-armem Clip. */
        private const val FLOSKEL_MAX_WORDS = 6
        private const val FLOSKEL_MAX_CHARS = 64
        private const val SILENCE_CONTEXT_MAX_VOICED_MS = 600

        /**
         * Normalisierte (lowercase, ohne Satzzeichen/Ziffern, Umlaute behalten) Whisper-Outro-Floskeln.
         * DE-Kern + EN-Kern aus dem HF-Dataset "whisper-hallucinations" (Almanach §2.4), 1:1 wie TVO/CVO.
         */
        private val FLOSKEL_BLOCKLIST: Set<String> = setOf(
            "vielen dank",
            "vielen dank fürs zuschauen",
            "vielen dank fuers zuschauen",
            "vielen dank für eure aufmerksamkeit",
            "vielen dank für ihre aufmerksamkeit",
            "vielen dank für die aufmerksamkeit",
            "bis zum nächsten mal",
            "bis zum nächsten video",
            "untertitel",
            "untertitel des zdf",
            "untertitelung des zdf für funk",
            "untertitel im auftrag des zdf für funk",
            "untertitel von stephanie geiges",
            "untertitel der amara org community",
            "der text ist nicht auf deutsch",
            "thank you",
            "thank you for watching",
            "thanks for watching",
            "please subscribe",
            "subtitles by the amara org community",
        )
    }
}
