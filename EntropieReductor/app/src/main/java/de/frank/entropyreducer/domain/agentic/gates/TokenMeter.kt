package de.frank.entropyreducer.domain.agentic.gates

import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.TokenUsageRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zweiter Gate in der Pipeline + Token-Tagesbuchhaltung. Hat zwei Aufgaben:
 *
 * 1. **Pre-Check (canRun)**: VOR jedem Gemini-Aufruf pruefen ob das
 *    optionale Tages-Token-Limit dieses Prompts erreicht ist. Wenn ja,
 *    Aufruf blockieren mit Status BLOCKED_BY_TOKEN_LIMIT.
 *
 * 2. **Post-Add (addUsage)**: NACH jedem Gemini-Aufruf das Token-Delta in
 *    die Tagesaggregation eintragen (inkrementelles Upsert in token_usage_daily).
 *
 * Token-Limit-Konfig:
 * - savedPrompts.tokenLimitPerDay: Int? — null bedeutet kein Limit
 * - Frank-Entscheidung 2026-05-21: kein globales Limit, kein Kategorie-Limit
 */
@Singleton
class TokenMeter
@Inject
constructor(
    private val promptRepo: PromptRepository,
    private val tokenUsageRepo: TokenUsageRepository,
) {

    /**
     * Pre-Check vor dem Gemini-Aufruf. Wenn der Prompt kein Limit hat, immer
     * erlaubt. Wenn ein Limit gesetzt ist, prueft gegen den heutigen Verbrauch.
     *
     * Die geschaetzten Tokens werden NICHT zum heutigen Verbrauch addiert — der
     * Pre-Check soll defensiv sein (wenn 50% des Limits gleich verbraucht waeren,
     * trotzdem Run zulassen). Erst der echte Verbrauch nach dem Run wird gezaehlt.
     * Falls der Run dann das Limit ueberschreitet, blockiert der naechste
     * Pre-Check.
     */
    suspend fun canRun(promptId: String): GateDecision {
        val prompt = promptRepo.getById(promptId)
            ?: return GateDecision.Denied(
                "Prompt nicht gefunden (id=$promptId). Workflow nicht startbar."
            )
        val limit = prompt.tokenLimitPerDay
        if (limit == null) {
            return GateDecision.Allowed("Kein Token-Limit fuer diesen Prompt gesetzt")
        }
        val todayUsed = tokenUsageRepo.getTodayTotalForPrompt(promptId)
        return if (todayUsed >= limit) {
            GateDecision.Denied(
                "Tages-Token-Limit erreicht ($todayUsed / $limit Tokens). " +
                    "Aenderbar in den Prompt-Einstellungen unter Token-Limit."
            )
        } else {
            GateDecision.Allowed("Token-Verbrauch heute: $todayUsed / $limit")
        }
    }

    /**
     * Pruefung WAEHREND des Loops nach einem Gemini-Aufruf — wenn das Limit
     * mittendrin gerissen wird, bricht der Loop ab. Identisch zu canRun.
     */
    suspend fun shouldContinue(promptId: String): GateDecision = canRun(promptId)

    /**
     * Token-Verbrauch in die Tagesaggregation eintragen. Sollte vom
     * ExecutionLogger nach jedem Gemini-Aufruf gerufen werden — entweder
     * inkrementell pro Loop-Schritt oder konsolidiert am Run-Ende.
     *
     * Wir nutzen den inkrementellen Pfad: jeder LLM_CALL-Step ruft das mit
     * dem Delta auf, damit der naechste shouldContinue-Check sofort den
     * aktuellen Stand sieht.
     */
    suspend fun addUsage(promptId: String, tokensInput: Int, tokensOutput: Int) {
        tokenUsageRepo.addTokens(promptId, tokensInput, tokensOutput)
    }

    /**
     * Convenience fuer's UI: gibt den heutigen Verbrauch zurueck (oder 0 wenn
     * nichts verbraucht).
     */
    suspend fun todayUsage(promptId: String): Int =
        tokenUsageRepo.getTodayTotalForPrompt(promptId)
}
