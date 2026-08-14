package de.frank.stacklabor.codex

/**
 * Auswahl der Codex-Modelle aus B-10 (F-18).
 *
 * [apiId] ist der Wert, der im Feld `model` an `chatgpt.com/backend-api/codex/responses` geht.
 * Vorgabe laut Spezifikation: `gpt-5.6-terra`.
 */
enum class CodexModell(val bezeichnung: String, val apiId: String) {
    SOL("GPT 5.6 Sol", "gpt-5.6-sol"),
    TERRA("GPT 5.6 Terra", "gpt-5.6-terra"),
    LUNA("GPT 5.6 Luna", "gpt-5.6-luna"),
    ;

    companion object {
        /** Vorgabe aus F-18. */
        val VORGABE: CodexModell = TERRA

        /** Liest einen gespeicherten Einstellungswert; unbekannte Werte fallen auf die Vorgabe. */
        fun ausWert(wert: String?): CodexModell {
            val gesucht = wert?.trim().orEmpty()
            if (gesucht.isEmpty()) return VORGABE
            return entries.firstOrNull {
                it.apiId.equals(gesucht, ignoreCase = true) || it.bezeichnung.equals(gesucht, ignoreCase = true)
            } ?: VORGABE
        }
    }
}

/**
 * Denkstufe (Reasoning-Effort) aus B-10.
 *
 * [apiWert] geht als `reasoning.effort` mit. Vorgabe laut Spezifikation: `high`.
 */
enum class Denkstufe(val bezeichnung: String, val apiWert: String) {
    NIEDRIG("Niedrig", "low"),
    MITTEL("Mittel", "medium"),
    HOCH("Hoch", "high"),
    SEHR_HOCH("Sehr hoch", "xhigh"),
    MAXIMAL("Maximal", "max"),
    ;

    companion object {
        /** Vorgabe aus F-18. */
        val VORGABE: Denkstufe = HOCH

        /** Liest einen gespeicherten Einstellungswert; unbekannte Werte fallen auf die Vorgabe. */
        fun ausWert(wert: String?): Denkstufe {
            val gesucht = wert?.trim().orEmpty()
            if (gesucht.isEmpty()) return VORGABE
            return entries.firstOrNull {
                it.apiWert.equals(gesucht, ignoreCase = true) || it.bezeichnung.equals(gesucht, ignoreCase = true)
            } ?: VORGABE
        }
    }
}

/**
 * Die drei Fehlerklassen, die die Oberfläche unterscheiden muss (F-12, F-13, F-02):
 *
 * - [NEU_ANMELDEN]: Anmeldung abgelaufen oder verweigert; Karte führt zu B-11.
 * - [KONTINGENT]: ChatGPT-Kontingent erschöpft; Knopf „Später erneut".
 * - [NETZ]: Verbindungs- oder Serverstörung; ein automatischer zweiter Versuch nach 3 Sekunden.
 */
enum class CodexFehlerArt { NEU_ANMELDEN, KONTINGENT, NETZ }

/**
 * Fehler aus der Codex-Anbindung.
 *
 * @param wiederholbar true bei Störungen, die sich von allein erledigen können (Serverfehler 5xx,
 *        Zeitüberschreitungen). Nur solche Anfragen werden automatisch wiederholt.
 */
class CodexFehler(
    val art: CodexFehlerArt,
    message: String,
    cause: Throwable? = null,
    val wiederholbar: Boolean = false,
) : Exception(message, cause)
