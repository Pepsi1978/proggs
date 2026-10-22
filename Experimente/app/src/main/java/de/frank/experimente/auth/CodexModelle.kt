package de.frank.experimente.auth

/**
 * Modelle und Effort-Stufen für F-22.
 *
 * Werte und Bezeichnungen exakt wie `CodexModels.kt` in PerfectMoment — das Funktions-Spec
 * verlangt das ausdrücklich, damit dieselbe Anmeldung dieselben Modelle anspricht.
 */
enum class CodexModell(val bezeichnung: String, val apiId: String, val anbieter: KiAnbieter = KiAnbieter.OPENAI) {
    ASTRA("GPT-6 Astra", "gpt-6-astra"),
    SOL("GPT 5.6 Sol", "gpt-5.6-sol"),
    TERRA("GPT 5.6 Terra", "gpt-5.6-terra"),
    LUNA("GPT 5.6 Luna", "gpt-5.6-luna"),
    OPUS("Claude Opus 5.5", "claude-opus-5-5", KiAnbieter.ANTHROPIC),
    FABLE("Claude Fable 5.1", "claude-fable-5-1", KiAnbieter.ANTHROPIC),
    SONNET("Claude Sonnet 5.5", "claude-sonnet-5-5", KiAnbieter.ANTHROPIC),
    HAIKU("Claude Haiku 5.5", "claude-haiku-5-5", KiAnbieter.ANTHROPIC),
    ;

    val istAnthropic: Boolean get() = anbieter == KiAnbieter.ANTHROPIC

    /** Anthropic kennt die Stufen low bis max, aber kein Ultra. */
    val unterstuetzteEfforts: List<Effort>
        get() = when (this) {
            ASTRA, SOL, TERRA -> Effort.entries.toList()
            LUNA, OPUS, FABLE, SONNET, HAIKU -> Effort.entries.filter { it != Effort.ULTRA }
        }

    fun normalisiereEffort(effort: Effort, fallback: Effort = Effort.MITTEL): Effort =
        effort.takeIf { it in unterstuetzteEfforts } ?: fallback

    companion object {
        fun aus(wert: String): CodexModell = entries.firstOrNull {
            it.apiId.equals(wert.trim(), true) || it.bezeichnung.equals(wert.trim(), true)
        } ?: TERRA
    }
}

enum class Effort(val bezeichnung: String, val apiWert: String) {
    NIEDRIG("Niedrig", "low"),
    MITTEL("Mittel", "medium"),
    HOCH("Hoch", "high"),
    SEHR_HOCH("Sehr hoch", "xhigh"),
    MAXIMAL("Maximal", "max"),
    ULTRA("Ultra", "ultra"),
    ;

    companion object {
        fun aus(wert: String): Effort = entries.firstOrNull {
            it.apiWert.equals(wert.trim(), true) || it.bezeichnung.equals(wert.trim(), true)
        } ?: MITTEL
    }
}

/** Was bei der Geräteanmeldung anzuzeigen ist (F-24). */
data class Geraetecode(val benutzercode: String, val adresse: String)

enum class FehlerArt { ANMELDUNG, KONTINGENT, NETZ }

/**
 * @param wiederholbar true bei Störungen, die sich von allein erledigen können
 *        (Serverfehler 5xx, Zeitüberschreitungen) — solche Anfragen werden wiederholt.
 */
class CodexFehler(
    val art: FehlerArt,
    meldung: String,
    ursache: Throwable? = null,
    val wiederholbar: Boolean = false,
) : Exception(meldung, ursache)
