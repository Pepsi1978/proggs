package de.frank.newskompass.data.model

import de.frank.newskompass.ai.CodexModell

/** Anbieter, der einen Text vorliest. */
enum class TtsAnbieter(val id: String, val label: String) {
    GOOGLE("google", "Google Chirp 3 HD"),
    EDGE("edge", "Microsoft Edge"),
    QWEN("qwen", "Alibaba (eigene Stimme)"),
    ;

    companion object {
        fun fromId(value: String): TtsAnbieter = entries.firstOrNull { it.id == value } ?: GOOGLE
    }
}

enum class Geschlecht { WEIBLICH, MAENNLICH }

data class Stimme(
    val id: String,
    val name: String,
    val geschlecht: Geschlecht,
)

enum class DesignModus(val id: String, val label: String) {
    SYSTEM("system", "System"),
    HELL("hell", "Hell"),
    DUNKEL("dunkel", "Dunkel"),
    ;

    companion object {
        fun fromId(value: String?): DesignModus = entries.firstOrNull { it.id == value } ?: SYSTEM
    }
}

/** Woher die Bilder der Meldungen kommen. */
enum class BildModus(val id: String, val label: String, val erklaerung: String) {
    FOTO_SONST_KI("foto_ki", "Foto, sonst KI", "Das Titelbild der Quelle; fehlt es, malt Codex eine Illustration."),
    NUR_KI("ki", "Immer KI-Illustration", "Codex malt zu jeder Meldung ein eigenes Bild."),
    NUR_FOTO("foto", "Nur Fotos", "Nur die Titelbilder der Quellen, keine KI-Bilder."),
    KEINE("keine", "Keine Bilder", "Nur Text — am schnellsten."),
    ;

    companion object {
        fun fromId(value: String?): BildModus = entries.firstOrNull { it.id == value } ?: FOTO_SONST_KI
    }
}

/**
 * Wie ausführlich jede Meldung geschrieben wird, unabhängig je Thema oder für freie Sprachfragen.
 * [regel] ist der Absatz-Auftrag im Redaktions-Prompt.
 */
enum class Ausfuehrlichkeit(val id: String, val label: String, val maxAbsaetze: Int) {
    KOMPAKT("kompakt", "Das Wichtigste in Kürze", 2),
    KURZ("absaetze_3", "Kurz erklärt", 3),
    STANDARD("standard", "Mit Hintergrund", 4),
    VERTIEFT("absaetze_5", "Vertieft", 5),
    AUSFUEHRLICH("ausfuehrlich", "Ausführlich", 6),
    SEHR_AUSFUEHRLICH("absaetze_7", "Sehr ausführlich", 7),
    UMFASSEND("absaetze_8", "Umfassend", 8),
    DETAILLIERT("absaetze_9", "Im Detail", 9),
    MAXIMAL("absaetze_10", "Maximale Tiefe", 10),
    ;

    val umfang: String get() = if (this == KOMPAKT) "1–2 kurze Absätze" else "$maxAbsaetze Absätze"

    val erklaerung: String get() = if (this == KOMPAKT) {
        "1–2 kurze Absätze pro Meldung — das Wichtigste in Kürze."
    } else {
        "$maxAbsaetze Absätze pro Meldung: Hintergrund, konkrete Zahlen, wer was sagt, Einordnung und Ausblick. " +
            if (maxAbsaetze >= 7) "Mit mehr Vorgeschichte, Details und unterschiedlichen Perspektiven, soweit belegt."
            else "Die Tiefe wächst mit der Absatzanzahl."
    }

    val regel: String get() = if (this == KOMPAKT) {
        "Jede Meldung hat 1 bis 2 kurze Absätze mit je 2 bis 3 Sätzen, jeder Absatz höchstens 350 Zeichen. " +
            "Der erste Absatz sagt das Wichtigste; einen zweiten nur, wenn er Wesentliches ergänzt."
    } else {
        "Schreibe pro Meldung $maxAbsaetze Absätze mit je ${if (maxAbsaetze <= 4) "2 bis 4" else "3 bis 5"} Sätzen, " +
            "jeder Absatz höchstens ${if (maxAbsaetze <= 4) 500 else 800} Zeichen. Der erste Absatz sagt das Wichtigste. " +
            "Die weiteren Absätze liefern, soweit belegt: den Hintergrund und die Vorgeschichte; konkrete Zahlen, Beträge und Daten, sprechbar ausgeschrieben; " +
            "die Positionen der beteiligten Seiten, also wer was sagt und wer widerspricht; die Einordnung, was das bedeutet und für wen; " +
            "und was als Nächstes zu erwarten ist. Bei vielen Absätzen vertiefe diese Aspekte mit zusätzlichen belegten Details und Perspektiven. " +
            "Erfinde nichts und wiederhole nichts, nur um die Absatzanzahl zu erreichen; fehlen belegte Inhalte, liefere weniger Absätze. " +
            "Niemals mehr als $maxAbsaetze Absätze. Jeder Absatz ist ein abgeschlossener Gedanke, der sich einzeln gut vorlesen lässt."
    }

    companion object {
        fun fromId(value: String?): Ausfuehrlichkeit = entries.firstOrNull { it.id == value } ?: STANDARD
        fun fromAbsaetze(anzahl: Int): Ausfuehrlichkeit = entries.first { it.maxAbsaetze == anzahl.coerceIn(2, 10) }
    }
}

/**
 * Ein Nachrichtenthema aus den Einstellungen — ein Stichwort, eine Frage oder ein ganzer Satz.
 *
 * [maxMeldungen] ist eine harte Obergrenze, [minMeldungen] nur ein Ziel: Gibt es nicht genug
 * belegtes Neues, bleibt der Block kürzer.
 */
@androidx.compose.runtime.Immutable
data class Thema(
    val id: String,
    val text: String,
    val minMeldungen: Int = STANDARD_MIN,
    val maxMeldungen: Int = STANDARD_MAX,
    /**
     * Wann dieses Thema automatisch neu recherchiert wird, als Minuten seit Mitternacht,
     * aufsteigend sortiert. Leer heißt: nur per Hand.
     */
    val uhrzeiten: List<Int> = STANDARD_UHRZEITEN,
    /** An welchen Tagen die [uhrzeiten] gelten — täglich, alle x Tage, wöchentlich, monatlich oder jährlich. */
    val rhythmus: Rhythmus = Rhythmus(),
    /** Kurze Überschrift aus höchstens drei Wörtern, von der KI aus [text] gebildet. Leer = noch keine. */
    val ueberschrift: String = "",
    /** Der [text], zu dem [ueberschrift] gebildet wurde — weicht er ab, ist die Überschrift veraltet. */
    val ueberschriftFuer: String = "",
    /** Eigene Länge für manuelle und automatische Recherchen dieses Themas. */
    val ausfuehrlichkeit: Ausfuehrlichkeit = Ausfuehrlichkeit.STANDARD,
) {
    /** Die einzeilige Überschrift für die zugeklappte Karte: die der KI oder ersatzweise die ersten Wörter. */
    fun kopfzeile(): String =
        ueberschrift.takeIf { it.isNotBlank() && ueberschriftFuer == text }
            ?: kurzfassung(text).ifBlank { "Neues Thema" }

    /** Braucht dieses Thema eine neue KI-Überschrift? */
    val ueberschriftVeraltet: Boolean get() = text.isNotBlank() && (ueberschrift.isBlank() || ueberschriftFuer != text)

    /** Bringt alle Werte in die Grenzen, sorgt für min ≤ max und sortiert die Uhrzeiten. */
    fun normiert(): Thema {
        val max = maxMeldungen.coerceIn(GRENZE_MIN, GRENZE_MAX)
        return copy(
            minMeldungen = minMeldungen.coerceIn(GRENZE_MIN, max),
            maxMeldungen = max,
            uhrzeiten = uhrzeiten.map { it.coerceIn(0, MINUTEN_PRO_TAG - 1) }.distinct().sorted(),
            rhythmus = rhythmus.normiert(),
        )
    }

    companion object {
        const val GRENZE_MIN = 1
        const val GRENZE_MAX = 15
        const val STANDARD_MIN = 4
        const val STANDARD_MAX = 7

        /** Eine gesprochene Frage steht in keiner Themenliste und bekommt einen festen, kleinen Rahmen. */
        const val FRAGE_MIN = 1
        const val FRAGE_MAX = 4

        const val MINUTEN_PRO_TAG = 24 * 60

        /** Bisheriger fester Zeitplan: 5 und 17 Uhr. */
        val STANDARD_UHRZEITEN: List<Int> = listOf(5 * 60, 17 * 60)

        const val UEBERSCHRIFT_WOERTER = 3

        /** Die ersten drei Wörter ohne Satzzeichen am Rand — Ersatz, solange die KI keine Überschrift geliefert hat. */
        fun kurzfassung(text: String): String =
            text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(UEBERSCHRIFT_WOERTER)
                .joinToString(" ") { it.trim(',', '.', ';', ':', '!', '?', '„', '“', '"', '(', ')') }.trim()

        /** Säubert die Antwort der KI: eine Zeile, ohne Anführungszeichen und Schlusspunkt, höchstens drei Wörter. */
        fun saeubereUeberschrift(roh: String): String =
            roh.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
                .removePrefix("Überschrift:").trim()
                .trim('"', '„', '“', '\'', '*', '#', ' ', '.')
                .split(Regex("\\s+")).filter { it.isNotBlank() }.take(UEBERSCHRIFT_WOERTER).joinToString(" ")
                .take(40).trim()

        /** „05:00“ für die Oberfläche. */
        fun uhrzeitText(minuten: Int): String = "%02d:%02d".format(minuten / 60, minuten % 60)
    }
}

data class Meldung(
    val id: String,
    val titel: String,
    val absaetze: List<String>,
    val quellen: List<String>,
    val bildDatei: String?,
    val bildIstKi: Boolean,
    /** Sprechbare Zeitangabe, etwa „heute früh“ oder „gestern Abend“. */
    val wann: String = "",
    /** Folgemeldung zu etwas, das schon in einer früheren Ausgabe stand. */
    val istUpdate: Boolean = false,
) {
    /** Was der Lautsprecher vorliest: Überschrift als eigener Absatz, dann der Text. */
    val vorleseText: String get() = (listOf("$titel.") + absaetze).joinToString("\n\n")
}

data class Block(
    val themaId: String,
    val titel: String,
    val meldungen: List<Meldung>,
    val fehler: String?,
    /**
     * Gesprochene Frage vom Mikrofon-Knopf. Solch ein Block ist ein Thema nur für diesen einen
     * Moment: Er hängt unten an der Ausgabe und steht in keiner Themenliste.
     */
    val frage: String? = null,
    /** Begrüßungssatz der KI, mit dem das Vorlesen des Blocks beginnt. Leer = Vorlage aus [de.frank.newskompass.tts.Moderation]. */
    val anmoderation: String = "",
    /** Schlusssatz der KI, mit dem das Vorlesen des Blocks endet. Leer = Vorlage. */
    val abmoderation: String = "",
)

data class Ausgabe(
    val id: String,
    val erstelltUm: Long,
    val slot: String,
    val bloecke: List<Block>,
) {
    /** Kam aus dem Zeitplan (oder per Hand) — nicht nur ein Behälter für gesprochene Fragen. */
    val istRegulaer: Boolean get() = bloecke.isEmpty() || bloecke.any { it.frage == null }
}

object Denkstufen {
    fun label(stufe: String): String = when (stufe) {
        "none" -> "Aus"
        "minimal" -> "Minimal"
        "low" -> "Niedrig"
        "medium" -> "Mittel"
        "high" -> "Hoch"
        "xhigh" -> "Sehr hoch"
        "max" -> "Maximal"
        "ultra" -> "Ultra"
        else -> stufe
    }

    private val bisUltra = listOf("low", "medium", "high", "xhigh", "max", "ultra")
    private val bisMax = bisUltra.dropLast(1)

    /** Startbestand aus dem Codex-Katalog vom 25.09.2026, falls der Live-Abruf scheitert. */
    val bekannteModelle = listOf(
        CodexModell("gpt-6-astra", "GPT-6 Astra", bisUltra, "medium"),
        CodexModell("gpt-6-sol", "GPT-6 Sol", bisUltra, "medium"),
        CodexModell("gpt-6-luna", "GPT-6 Luna", bisMax, "medium"),
        CodexModell("gpt-5.6-sol", "GPT-5.6 Sol", bisUltra, "medium"),
        CodexModell("gpt-5.6-terra", "GPT-5.6 Terra", bisUltra, "medium"),
        CodexModell("gpt-5.6-luna", "GPT-5.6 Luna", bisMax, "medium"),
        CodexModell("gpt-5.5", "GPT-5.5", listOf("low", "medium", "high", "xhigh"), "medium"),
    )
}
