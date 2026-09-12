package de.frank.kompass.data.model

/** Die vier Bereiche, die unten in der Leiste stehen. */
enum class Bereich(val id: String, val titel: String) {
    SLASH("slash", "Slash-Befehle"),
    CONFIG("config", "Config"),
    PRAXIS("praxis", "Best Practices"),
    CHAT("chat", "Chat"),
    ;

    companion object {
        fun fromId(value: String): Bereich = entries.firstOrNull { it.id == value } ?: SLASH
    }
}

/**
 * Was in eine Sicherung kommt — jeder Punkt einzeln abwählbar.
 *
 * Die drei Wissensbereiche werden VOLLSTÄNDIG gesichert, nicht nur der selbst erarbeitete
 * Anteil. Das ist der Unterschied zu früher: Wer den Aktualisieren-Knopf benutzt hat, trägt
 * einen neueren Stand in der Datenbank als in der mitgelieferten Wissensbasis der App — auf
 * einem zweiten Gerät stand ohne das wieder nur der Auslieferungsstand.
 */
enum class SicherungsTeil(val id: String, val titel: String, val erklaerung: String) {
    SLASH("slash", "Slash-Befehle", "Der ganze Befehlskatalog mit allen Erklärungen."),
    CONFIG("config", "Config-Einstellungen", "Alle Einträge des Config-Bereichs."),
    PRAXIS("praxis", "Best Practices", "Alle Praxisartikel."),
    FRAGEN("fragen", "Eigene Fragen", "Die selbst gestellten Fragen samt Antworten."),
    GESPRAECHE("gespraeche", "Gespräche", "Die Chats mit ihren Nachrichten."),
    ;

    /** Der Wissensbereich dahinter, oder null bei den eigenen Inhalten. */
    val bereich: Bereich? get() = when (this) {
        SLASH -> Bereich.SLASH
        CONFIG -> Bereich.CONFIG
        PRAXIS -> Bereich.PRAXIS
        FRAGEN, GESPRAECHE -> null
    }

    companion object {
        fun fromId(value: String): SicherungsTeil? = entries.firstOrNull { it.id == value }

        /** Ohne eigene Wahl kommt alles mit — eine halbe Sicherung überrascht sonst später. */
        val ALLE: Set<SicherungsTeil> get() = entries.toSet()
    }
}

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

/** Ein Modell, das die Fragen beantwortet. Codex kennt Astra, Sol, Terra und Luna. */
data class KiModell(
    val label: String,
    val apiId: String,
) {
    companion object {
        /**
         * Die heute bekannten Modelle. Kommen später neue dazu, trägt der Nutzer sie in den
         * Einstellungen als eigenen Eintrag nach — die Liste ist deshalb bewusst nur der
         * Startbestand und keine abgeschlossene Aufzählung.
         */
        val bekannt = listOf(
            KiModell("GPT 5.6 Sol", "gpt-5.6-sol"),
            KiModell("GPT 5.6 Terra", "gpt-5.6-terra"),
            KiModell("GPT 5.6 Luna", "gpt-5.6-luna"),
            KiModell("GPT-6 Astra", "gpt-6-astra"),
        )
        val standard = bekannt[1]
    }
}

enum class Denktiefe(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
    MAX("Maximal", "max"),
    ULTRA("Ultra", "ultra"),
    ;

    companion object {
        // Lokaler Codex-Modellkatalog vom 07.09.2026; eigene Modelle behalten die bisherigen Stufen.
        fun fuerModell(modellId: String): List<Denktiefe> = when (modellId) {
            "gpt-6-astra", "gpt-5.6-sol", "gpt-5.6-terra" -> entries
            else -> entries.filter { it != ULTRA }
        }

        fun fromValue(value: String, modellId: String): Denktiefe =
            fuerModell(modellId).firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) } ?: MEDIUM
    }
}
