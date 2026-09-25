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

/** Ein Nachrichtenthema aus den Einstellungen — ein Stichwort, eine Frage oder ein ganzer Satz. */
data class Thema(val id: String, val text: String)

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
