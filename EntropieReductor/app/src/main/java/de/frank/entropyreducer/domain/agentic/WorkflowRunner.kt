package de.frank.entropyreducer.domain.agentic

import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.data.remote.FunctionCall
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.remote.Tool
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.PromptToolPermissionRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.agentic.gates.ConfirmationGate
import de.frank.entropyreducer.domain.agentic.gates.ConfirmationRequest
import de.frank.entropyreducer.domain.agentic.gates.GateDecision
import de.frank.entropyreducer.domain.agentic.gates.PermissionGate
import de.frank.entropyreducer.domain.agentic.gates.TokenMeter
import de.frank.entropyreducer.domain.agentic.trigger.ChainTriggerNotifier
import de.frank.entropyreducer.domain.model.ConfirmDecision
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.StepType
import de.frank.entropyreducer.domain.model.TriggerSource
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Herz des Agentic-AI-Geruests: orchestriert die ReAct-Schleife (Reason + Act)
 * zwischen Gemini und den AgenticTools. Pro Run:
 *
 *  1. Pre-Checks (Prompt existiert? API-Key? Token-Budget?)
 *  2. ExecutionLogger.start(...)
 *  3. Loop:
 *     a. Gemini.generateContent(model, tools, contents)
 *     b. Falls Text-Antwort und keine FunctionCall -> Run finished
 *     c. Sonst: pro FunctionCall durch PermissionGate -> ConfirmationGate ->
 *        Tool.execute -> ExecutionLogger
 *     d. FunctionResponses zurueck an Gemini, naechste Iteration
 *  4. Abbruch-Bedingungen: max 20 Tool-Calls, max 100k Tokens, Token-Limit
 *     ueberschritten, Hard-Timeout, Permission-Block ohne moeglichen Workaround
 *  5. ExecutionLogger.complete / .fail
 *
 * Returnt einen Flow<WorkflowEvent> fuer Live-UI-Updates. Der Flow ist Cold —
 * jeder Aufruf von `run()` startet einen frischen Run.
 */
@Singleton
class WorkflowRunner
@Inject
constructor(
    private val gemini: GeminiApi,
    private val toolRegistry: ToolRegistry,
    private val permissionGate: PermissionGate,
    private val confirmationGate: ConfirmationGate,
    private val tokenMeter: TokenMeter,
    private val executionLogger: ExecutionLogger,
    private val promptRepo: PromptRepository,
    private val permissionRepo: PromptToolPermissionRepository,
    private val secrets: EncryptedSecretsStore,
    private val chainNotifier: ChainTriggerNotifier,
) {
    private val promptRunMutexes = ConcurrentHashMap<String, Mutex>()

    fun run(
        promptId: String,
        userInputContext: String? = null,
        triggerSource: TriggerSource = TriggerSource.MANUAL,
    ): Flow<WorkflowEvent> = flow {
        val executionId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        var tokensInputTotal = 0
        var tokensOutputTotal = 0
        var toolCallCount = 0
        var stepIndex = 0
        var executionStarted = false

        try {
        withTimeout(MAX_RUN_DURATION_MILLIS) {
        promptRunMutexes.computeIfAbsent(promptId) { Mutex() }.withLock {
        // 1. Prompt laden
        val prompt = promptRepo.getById(promptId)
        if (prompt == null) {
            emit(
                WorkflowEvent.Finished(
                    executionId = executionId,
                    status = ExecutionStatus.FAILED,
                    errorMessage = "Prompt mit id='$promptId' nicht gefunden.",
                )
            )
            return@withTimeout
        }

        // 2. API-Key vorhanden?
        val apiKey = secrets.geminiApiKey
        if (apiKey.isNullOrBlank()) {
            emit(
                WorkflowEvent.Finished(
                    executionId = executionId,
                    status = ExecutionStatus.FAILED,
                    errorMessage = "Kein Gemini-API-Key hinterlegt. Bitte in den Einstellungen eintragen.",
                )
            )
            return@withTimeout
        }

        // 3. Pre-Token-Check
        val tokenPreCheck = tokenMeter.canRun(promptId)
        if (tokenPreCheck is GateDecision.Denied) {
            executionLogger.start(
                executionId = executionId,
                prompt = prompt,
                userInputContext = userInputContext,
                startedAt = startedAt,
                modelUsed = prompt.model,
                triggerSource = triggerSource,
            )
            executionLogger.fail(
                executionId = executionId,
                status = ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                errorMessage = tokenPreCheck.reason,
            )
            emit(
                WorkflowEvent.Finished(
                    executionId = executionId,
                    status = ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                    errorMessage = tokenPreCheck.reason,
                )
            )
            return@withTimeout
        }

        // 4. Run-Start logging
        executionLogger.start(
            executionId = executionId,
            prompt = prompt,
            userInputContext = userInputContext,
            startedAt = startedAt,
            modelUsed = prompt.model,
            triggerSource = triggerSource,
        )
        executionStarted = true
        emit(
            WorkflowEvent.Started(
                executionId = executionId,
                promptId = promptId,
                promptName = prompt.name,
                modelUsed = prompt.model,
            )
        )

        // 5. Tool-Deklarationen aufbauen
        val declarations =
            toolRegistry.functionDeclarationsForPrompt(promptId, permissionRepo)
        val tools = if (declarations.isEmpty()) null else listOf(Tool(declarations))

        // 6. Initial-Contents
        val systemInstruction =
            GeminiContent(parts = listOf(GeminiPart.text(prompt.content)))
        val contents = mutableListOf<GeminiContent>()
        val initialUserText =
            userInputContext?.takeIf { it.isNotBlank() }
                ?: "Bitte fuehre die im System-Prompt beschriebene Aufgabe aus."
        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart.text(initialUserText))))

        var finalAnswer: String? = null
        var loopAborted = false

            workflowLoop@ while (toolCallCount < MAX_TOOL_CALLS) {
                // Timeout-Check (defensiv ohne withTimeout um Flow-Semantics nicht zu stoeren)
                if (System.currentTimeMillis() - startedAt > MAX_RUN_DURATION_MILLIS) {
                    executionLogger.fail(
                        executionId,
                        ExecutionStatus.BLOCKED_BY_TIMEOUT,
                        "Lauf-Timeout nach ${MAX_RUN_DURATION_MILLIS / 1000}s",
                        tokensInputTotal,
                        tokensOutputTotal,
                        toolCallCount,
                    )
                    emit(
                        WorkflowEvent.Finished(
                            executionId,
                            ExecutionStatus.BLOCKED_BY_TIMEOUT,
                            errorMessage = "Lauf-Timeout",
                            tokensTotal = tokensInputTotal + tokensOutputTotal,
                            toolCallCount = toolCallCount,
                            durationMillis = System.currentTimeMillis() - startedAt,
                        )
                    )
                    loopAborted = true
                    break
                }

                // a) Gemini aufrufen
                val llmStepIndex = stepIndex++
                emit(
                    WorkflowEvent.StepStarted(
                        executionId,
                        llmStepIndex,
                        StepType.LLM_CALL,
                    )
                )

                val response =
                    try {
                        gemini.generateContent(
                            model = prompt.model,
                            apiKey = apiKey,
                            request =
                                GeminiRequest(
                                    systemInstruction = systemInstruction,
                                    contents = contents,
                                    tools = tools,
                                    generationConfig =
                                        GeminiGenerationConfig(
                                            temperature = 0.3,
                                            // Direktive 3 Loop-1-Fix (LOW-2):
                                            // Gemini-Output-Cap damit ein einzelner
                                            // Call das Token-Budget nicht sprengt.
                                            maxOutputTokens = 8192,
                                        ),
                                ),
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (e: Exception) {
                        executionLogger.fail(
                            executionId,
                            ExecutionStatus.FAILED,
                            "Gemini-Aufruf fehlgeschlagen: ${e.message ?: e::class.simpleName}",
                            tokensInputTotal,
                            tokensOutputTotal,
                            toolCallCount,
                        )
                        emit(
                            WorkflowEvent.Finished(
                                executionId,
                                ExecutionStatus.FAILED,
                                errorMessage = e.message,
                                tokensTotal = tokensInputTotal + tokensOutputTotal,
                                toolCallCount = toolCallCount,
                                durationMillis = System.currentTimeMillis() - startedAt,
                            )
                        )
                        loopAborted = true
                        break
                    }

                val usage = response.usageMetadata
                val tokensIn = usage?.promptTokenCount ?: 0
                val tokensOut = usage?.candidatesTokenCount ?: 0
                tokensInputTotal += tokensIn
                tokensOutputTotal += tokensOut
                tokenMeter.addUsage(promptId, tokensIn, tokensOut)

                val candidate = response.candidates?.firstOrNull()
                val responseParts = candidate?.content?.parts ?: emptyList()
                val textOutput =
                    responseParts.mapNotNull { it.text }.joinToString("\n").ifBlank { null }
                val functionCalls = responseParts.mapNotNull { it.functionCall }

                executionLogger.logStep(
                    PromptExecutionStepEntity(
                        id = UUID.randomUUID().toString(),
                        executionId = executionId,
                        stepIndex = llmStepIndex,
                        stepType = StepType.LLM_CALL,
                        timestamp = System.currentTimeMillis(),
                        llmTextOutput = textOutput,
                    )
                )
                emit(
                    WorkflowEvent.LlmStepCompleted(
                        executionId,
                        llmStepIndex,
                        textOutput,
                        tokensIn,
                        tokensOut,
                    )
                )

                // Max-Tokens-Pro-Run-Hartlimit
                if (tokensInputTotal + tokensOutputTotal > MAX_TOKENS_PER_RUN) {
                    executionLogger.fail(
                        executionId,
                        ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                        "Max-Token-Limit pro Run erreicht ($MAX_TOKENS_PER_RUN)",
                        tokensInputTotal,
                        tokensOutputTotal,
                        toolCallCount,
                    )
                    emit(
                        WorkflowEvent.Finished(
                            executionId,
                            ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                            errorMessage = "Max-Token-Limit pro Run erreicht",
                            tokensTotal = tokensInputTotal + tokensOutputTotal,
                            toolCallCount = toolCallCount,
                            durationMillis = System.currentTimeMillis() - startedAt,
                        )
                    )
                    loopAborted = true
                    break
                }

                // Tages-Token-Limit nach jedem LLM-Call neu checken
                val tokenMidCheck = tokenMeter.shouldContinue(promptId)
                if (tokenMidCheck is GateDecision.Denied) {
                    executionLogger.fail(
                        executionId,
                        ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                        tokenMidCheck.reason,
                        tokensInputTotal,
                        tokensOutputTotal,
                        toolCallCount,
                    )
                    emit(
                        WorkflowEvent.Finished(
                            executionId,
                            ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                            errorMessage = tokenMidCheck.reason,
                            tokensTotal = tokensInputTotal + tokensOutputTotal,
                            toolCallCount = toolCallCount,
                            durationMillis = System.currentTimeMillis() - startedAt,
                        )
                    )
                    loopAborted = true
                    break
                }

                // b) Falls keine Function-Calls: Run beendet
                if (functionCalls.isEmpty()) {
                    if (textOutput == null) {
                        val errorMessage = "Gemini hat weder Text noch Tool-Aufruf geliefert."
                        executionLogger.fail(
                            executionId,
                            ExecutionStatus.FAILED,
                            errorMessage,
                            tokensInputTotal,
                            tokensOutputTotal,
                            toolCallCount,
                        )
                        emit(
                            WorkflowEvent.Finished(
                                executionId,
                                ExecutionStatus.FAILED,
                                errorMessage = errorMessage,
                                tokensTotal = tokensInputTotal + tokensOutputTotal,
                                toolCallCount = toolCallCount,
                                durationMillis = System.currentTimeMillis() - startedAt,
                            )
                        )
                        loopAborted = true
                    } else {
                        finalAnswer = textOutput
                    }
                    break
                }

                // c) Function-Call-Antwort von Gemini als "model"-Content in Verlauf einfuegen
                contents.add(GeminiContent(role = "model", parts = responseParts))

                // d) Pro Function-Call: Gates + Execute
                val toolResponseParts = mutableListOf<GeminiPart>()
                for (call in functionCalls) {
                    if (toolCallCount >= MAX_TOOL_CALLS) break@workflowLoop
                    // Jeder vom Modell angeforderte Aufruf zaehlt, auch unbekannte,
                    // abgelehnte oder fehlgeschlagene Tools. Sonst koennte das Modell
                    // das Hartlimit mit ungueltigen Aufrufen unbegrenzt umgehen.
                    toolCallCount++
                    val responsePart = processFunctionCall(
                        executionId = executionId,
                        promptId = promptId,
                        userInputContext = userInputContext,
                        startedAt = startedAt,
                        stepIndexProvider = { stepIndex++ },
                        call = call,
                        triggerSource = triggerSource,
                        emit = { evt -> emit(evt) },
                    )
                    toolResponseParts.add(responsePart)
                }

                contents.add(GeminiContent(role = "function", parts = toolResponseParts))
            }

            // Loop ist normal beendet (oder break aus Function-Empty)
            if (!loopAborted) {
                if (toolCallCount >= MAX_TOOL_CALLS && finalAnswer == null) {
                    executionLogger.fail(
                        executionId,
                        ExecutionStatus.FAILED,
                        "Max-Tool-Call-Limit erreicht ($MAX_TOOL_CALLS) ohne finale Antwort.",
                        tokensInputTotal,
                        tokensOutputTotal,
                        toolCallCount,
                    )
                    emit(
                        WorkflowEvent.Finished(
                            executionId,
                            ExecutionStatus.FAILED,
                            errorMessage = "Max-Tool-Call-Limit erreicht",
                            tokensTotal = tokensInputTotal + tokensOutputTotal,
                            toolCallCount = toolCallCount,
                            durationMillis = System.currentTimeMillis() - startedAt,
                        )
                    )
                } else {
                    val finishedAt = System.currentTimeMillis()
                    executionLogger.complete(
                        executionId = executionId,
                        finalAnswer = finalAnswer,
                        tokensInput = tokensInputTotal,
                        tokensOutput = tokensOutputTotal,
                        toolCallCount = toolCallCount,
                        finishedAt = finishedAt,
                    )
                    emit(
                        WorkflowEvent.Finished(
                            executionId,
                            ExecutionStatus.SUCCESS,
                            finalAnswer = finalAnswer,
                            tokensTotal = tokensInputTotal + tokensOutputTotal,
                            toolCallCount = toolCallCount,
                            durationMillis = finishedAt - startedAt,
                        )
                    )
                    // Frank-Wunsch 2026-05-21: Chain-Trigger nach Erfolg.
                    // Fire-and-forget — der Chain-Notifier laeuft in seinem
                    // eigenen Scope. Falls Kette: weitere Prompts werden im
                    // Hintergrund mit TriggerSource.CHAINED gestartet.
                    chainNotifier.notifySuccess(promptId)
                }
            }
            }
            }
        } catch (t: Throwable) {
            // Direktive 3 Loop-3-Fix (war L3-2-Bug): CancellationException
            // wird auch vom catch(Throwable) gefangen, der naechste suspending
            // executionLogger.fail()-Aufruf wuerde sie wieder werfen ohne dass
            // der Status persistiert wird → Eintrag bleibt RUNNING in DB.
            // NonCancellable-Scope schuetzt den Logging-Pfad, CancellationException
            // wird sauber als CANCELLED persistiert UND rethrown (Kotlin-Pflicht).
            val isTimeout = t is TimeoutCancellationException
            val isCancellation = t is CancellationException
            val status =
                when {
                    isTimeout -> ExecutionStatus.BLOCKED_BY_TIMEOUT
                    isCancellation -> ExecutionStatus.CANCELLED
                    else -> ExecutionStatus.FAILED
                }
            val errorMsg =
                when {
                    isTimeout -> "Lauf-Timeout nach ${MAX_RUN_DURATION_MILLIS / 1000}s"
                    isCancellation -> "Run abgebrochen"
                    else -> "Unerwarteter Fehler: ${t.message ?: t::class.simpleName}"
                }
            withContext(NonCancellable) {
                if (executionStarted) {
                    executionLogger.fail(
                        executionId,
                        status,
                        errorMsg,
                        tokensInputTotal,
                        tokensOutputTotal,
                        toolCallCount,
                    )
                }
            }
            // Bei externer Cancellation ist auch der Collector abgebrochen; ein emit
            // wuerde die Cancellation nur erneut werfen. Das eigene Timeout ist nach
            // Verlassen von withTimeout dagegen wieder in einem aktiven Kontext.
            if (!isCancellation || isTimeout) {
                emit(
                    WorkflowEvent.Finished(
                        executionId,
                        status,
                        errorMessage = errorMsg,
                        tokensTotal = tokensInputTotal + tokensOutputTotal,
                        toolCallCount = toolCallCount,
                        durationMillis = System.currentTimeMillis() - startedAt,
                    )
                )
            }
            if (isCancellation && !isTimeout) throw t
        }
    }

    /**
     * Einzelnen Function-Call abarbeiten: PermissionGate -> ConfirmationGate ->
     * Tool.execute. Returnt die Antwort-Part die an Gemini zurueckgeschickt wird
     * (entweder das Tool-Result oder eine Fehler-Begruendung damit Gemini
     * sinnvoll weiterreagieren kann).
     */
    private suspend fun processFunctionCall(
        executionId: String,
        promptId: String,
        userInputContext: String?,
        startedAt: Long,
        stepIndexProvider: () -> Int,
        call: FunctionCall,
        triggerSource: TriggerSource,
        emit: suspend (WorkflowEvent) -> Unit,
    ): GeminiPart {
        val tool = toolRegistry.byName(call.name)
        if (tool == null) {
            val idx = stepIndexProvider()
            executionLogger.logStep(
                PromptExecutionStepEntity(
                    id = UUID.randomUUID().toString(),
                    executionId = executionId,
                    stepIndex = idx,
                    stepType = StepType.BLOCKED,
                    timestamp = System.currentTimeMillis(),
                    toolName = call.name,
                    error = "Tool nicht registriert",
                )
            )
            emit(
                WorkflowEvent.Blocked(
                    executionId,
                    idx,
                    call.name,
                    "Tool '${call.name}' nicht registriert.",
                )
            )
            return GeminiPart.response(
                call.name,
                errorJson("unknown_tool", "Tool '${call.name}' ist nicht registriert."),
            )
        }

        // Permission-Gate
        val permDecision = permissionGate.check(promptId, tool)
        if (permDecision is GateDecision.Denied) {
            val idx = stepIndexProvider()
            executionLogger.logStep(
                PromptExecutionStepEntity(
                    id = UUID.randomUUID().toString(),
                    executionId = executionId,
                    stepIndex = idx,
                    stepType = StepType.BLOCKED,
                    timestamp = System.currentTimeMillis(),
                    toolName = call.name,
                    error = permDecision.reason,
                )
            )
            emit(WorkflowEvent.Blocked(executionId, idx, call.name, permDecision.reason))
            return GeminiPart.response(
                call.name,
                errorJson("permission_denied", permDecision.reason),
            )
        }

        // Confirmation-Gate (nur fuer Write-Tools)
        if (tool.isWriteTool) {
            // Direktive 3 Loop-1-Fix (MED-2): Background-Runs koennen keinen
            // User-Confirm zeigen → isBackground=true damit der Gate sofort
            // REJECTED zurueckgibt statt 60s zu warten. Frank kann Background-
            // Write-Tools nur per Trust-Modus erlauben.
            val isBackground = triggerSource != TriggerSource.MANUAL
            val confirmReq =
                ConfirmationRequest(
                    promptId = promptId,
                    executionId = executionId,
                    tool = tool,
                    args = call.args,
                    isBackground = isBackground,
                )
            val confirmIdx = stepIndexProvider()
            emit(WorkflowEvent.ConfirmationRequested(executionId, confirmReq))
            val confirmResult = confirmationGate.request(confirmReq)
            executionLogger.logStep(
                PromptExecutionStepEntity(
                    id = UUID.randomUUID().toString(),
                    executionId = executionId,
                    stepIndex = confirmIdx,
                    stepType = StepType.USER_CONFIRM,
                    timestamp = System.currentTimeMillis(),
                    toolName = call.name,
                    toolArgsJson = call.args.toString(),
                    confirmDecision = confirmResult.decision,
                )
            )
            if (
                confirmResult.decision == ConfirmDecision.REJECTED ||
                    confirmResult.decision == ConfirmDecision.TIMED_OUT
            ) {
                return GeminiPart.response(
                    call.name,
                    errorJson(
                        "user_rejected",
                        confirmResult.rejectReason
                            ?: "Vom Nutzer abgelehnt (oder Timeout).",
                    ),
                )
            }
        }

        // Tool ausfuehren
        val ctx =
            ToolContext(
                promptId = promptId,
                executionId = executionId,
                stepIndex = stepIndexProvider(),
                startedAt = startedAt,
                userInputContext = userInputContext,
            )
        val result = tool.execute(call.args, ctx)
        val toolStepIdx = ctx.stepIndex

        return when (result) {
            is ToolResult.Success -> {
                // Direktive 3 Loop-5-Fix (war L5-2-Bug): Tool-Results koennen
                // persoenliche Daten enthalten (z.B. read_profil gibt Frank's
                // Lebensumstaende, read_entropie_eintraege Tagebuch-Texte).
                // Wir speichern eine GEKUERZTE Version im Audit-Log — die
                // vollen Daten gehen weiter an Gemini (per response()), aber
                // landen nicht dauerhaft in der DB. 2000 Zeichen reichen fuer
                // Debugging-Zwecke + Audit-Nachvollziehbarkeit.
                val fullResultJson = result.data.toString()
                val storedResultJson =
                    if (fullResultJson.length > AUDIT_RESULT_MAX_CHARS)
                        fullResultJson.take(AUDIT_RESULT_MAX_CHARS) +
                            "… [getrimmt — Original ${fullResultJson.length} Zeichen]"
                    else fullResultJson
                executionLogger.logStep(
                    PromptExecutionStepEntity(
                        id = UUID.randomUUID().toString(),
                        executionId = executionId,
                        stepIndex = toolStepIdx,
                        stepType = StepType.TOOL_CALL,
                        timestamp = System.currentTimeMillis(),
                        toolName = call.name,
                        toolArgsJson = call.args.toString().take(AUDIT_ARGS_MAX_CHARS),
                        toolResultJson = storedResultJson,
                        createdEntityIds = result.createdEntityIds,
                        updatedEntityIds = result.updatedEntityIds,
                        deletedEntityIds = result.deletedEntityIds,
                    )
                )
                emit(
                    WorkflowEvent.ToolStepCompleted(
                        executionId,
                        toolStepIdx,
                        call.name,
                        success = true,
                        resultPreview = result.data.toString().take(200),
                    )
                )
                GeminiPart.response(call.name, result.data)
            }
            is ToolResult.Failure -> {
                executionLogger.logStep(
                    PromptExecutionStepEntity(
                        id = UUID.randomUUID().toString(),
                        executionId = executionId,
                        stepIndex = toolStepIdx,
                        stepType = StepType.TOOL_CALL,
                        timestamp = System.currentTimeMillis(),
                        toolName = call.name,
                        toolArgsJson = call.args.toString(),
                        error = result.message,
                    )
                )
                emit(
                    WorkflowEvent.ToolStepCompleted(
                        executionId,
                        toolStepIdx,
                        call.name,
                        success = false,
                        error = result.message,
                    )
                )
                GeminiPart.response(
                    call.name,
                    errorJson("tool_failure", result.message),
                )
            }
        }
    }

    private fun errorJson(code: String, message: String): JsonObject =
        buildJsonObject {
            put("error", code)
            put("message", message)
        }

    companion object {
        /** Frank-TODO 2026-05-21: max 20 Tool-Calls pro Run. */
        const val MAX_TOOL_CALLS = 20

        /** Frank-TODO 2026-05-21: max 100k Tokens pro Run als Run-Away-Schutz. */
        const val MAX_TOKENS_PER_RUN = 100_000

        /** Frank-TODO 2026-05-21: max 5 Minuten Laufzeit pro Run. */
        const val MAX_RUN_DURATION_MILLIS = 5L * 60L * 1000L

        /**
         * Loop-5-Fix (L5-2): maximale Zeichen die im Audit-Log fuer
         * toolResultJson und toolArgsJson gespeichert werden. Verhindert dass
         * persoenliche Daten (z.B. Profil-Text, Tagebucheintraege) dauerhaft
         * im prompt_execution_steps stehen. Vollwert geht an Gemini, gekuerzt
         * an die DB.
         */
        private const val AUDIT_RESULT_MAX_CHARS = 2000
        private const val AUDIT_ARGS_MAX_CHARS = 1000
    }
}
