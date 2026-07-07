package de.frank.entropyreducer.domain.model

/**
 * Status einer Prompt-Ausfuehrung (PromptExecutionEntity).
 *
 * Lifecycle: RUNNING → SUCCESS | FAILED | CANCELLED | INTERRUPTED | BLOCKED_BY_*
 *
 * INTERRUPTED ist der Status fuer Ausfuehrungen die durch App-Crash oder erzwungenes Beenden
 * mittendrin unterbrochen wurden — beim naechsten App-Start markiert sie der ExecutionLogger.
 */
enum class ExecutionStatus(val label: String) {
    RUNNING("Laeuft"),
    SUCCESS("Erfolgreich"),
    FAILED("Fehlgeschlagen"),
    CANCELLED("Abgebrochen"),
    INTERRUPTED("Unterbrochen"),
    BLOCKED_BY_TOKEN_LIMIT("Token-Limit erreicht"),
    BLOCKED_BY_PERMISSION("Berechtigung fehlt"),
    BLOCKED_BY_TIMEOUT("Zeitlimit ueberschritten"),
    BLOCKED_BY_USER_REJECT("Vom Nutzer abgelehnt"),
}

/**
 * Typ eines Schritts im ReAct-Loop (PromptExecutionStepEntity).
 *
 * - LLM_CALL: Gemini wurde aufgerufen, Ausgabe in `llmTextOutput`
 * - TOOL_CALL: Ein Tool wurde ausgefuehrt, Args/Result in `toolArgsJson`/`toolResultJson`
 * - USER_CONFIRM: Confirm-Dialog gezeigt, Antwort in `confirmDecision`
 * - BLOCKED: Schritt wurde von einem Gate blockiert, Grund in `error`
 */
enum class StepType {
    LLM_CALL,
    TOOL_CALL,
    USER_CONFIRM,
    BLOCKED,
}

/**
 * Entscheidung beim Confirm-Dialog (vor Write-Tool-Aufruf).
 *
 * - APPROVED: Nutzer hat zugestimmt
 * - REJECTED: Nutzer hat abgelehnt
 * - AUTO_APPROVED: Trust-Modus war aktiv, kein Dialog gezeigt
 * - TIMED_OUT: 60s ohne Antwort → automatisch abgelehnt
 */
enum class ConfirmDecision {
    APPROVED,
    REJECTED,
    AUTO_APPROVED,
    TIMED_OUT,
}

/**
 * Wodurch wurde eine Prompt-Ausfuehrung gestartet?
 *
 * - MANUAL: Nutzer hat den Ausfuehren-Knopf gedrueckt
 * - SCHEDULED: Cron-Trigger (z.B. Sonntag 19:00) ist gefeuert
 * - EVENT: Event-Bedingung wurde erfuellt (z.B. "3 neue Entropie-Eintraege in 24h")
 * - CHAINED: Vorgaenger-Prompt war erfolgreich, dieser kettet sich an
 */
enum class TriggerSource {
    MANUAL,
    SCHEDULED,
    EVENT,
    CHAINED,
}

/**
 * Trigger-Typ fuer Auto-Ausfuehrung (PromptTriggerEntity, Stufe 3).
 *
 * - MANUAL: kein Auto-Trigger, nur per Knopfdruck
 * - CRON: zeitgesteuert (cronExpression im Quartz-Format, z.B. "0 19 * * SUN")
 * - EVENT: ausgeloest durch App-Ereignis (DSL-String in eventCondition)
 * - CHAIN: ausgeloest nach Erfolg eines anderen Prompts (chainAfterPromptId)
 */
enum class TriggerType {
    MANUAL,
    CRON,
    EVENT,
    CHAIN,
}
