package de.frank.stacklabor.werftstudio.service.codex

class CodexEvaluationService(private val client: CodexResponsesClient) {
    suspend fun evaluate(
        request: CodexEvaluationRequest,
        allowNetworkRetry: Boolean = true,
        onEvent: suspend (CodexStreamEvent) -> Unit = {},
    ): CodexEvaluationOutcome {
        require(request.evaluationId.isNotBlank())
        require(request.contextJson.isNotBlank()) { "Kontextdaten fehlen." }
        onEvent(CodexStreamEvent.Stage("Prüfe Wechselwirkungen …"))
        val partials = mutableListOf<CodexEvaluation>()
        val narratives = mutableListOf<String>()
        // Ohne Ziele laeuft genau eine Anfrage: dann wird die Zusammenstellung selbst
        // beurteilt statt gegen Ziele gewichtet. Ein leeres chunked() wuerde gar nichts fragen.
        val chunks = if (request.goals.isEmpty()) listOf(emptyList()) else request.goals.chunked(MAX_GOALS_PER_REQUEST)
        for ((chunkIndex, goals) in chunks.withIndex()) {
            val includeStackConclusion = request.scope == CodexEvaluationScope.STACK && chunkIndex == chunks.lastIndex
            onEvent(
                CodexStreamEvent.Stage(
                    if (goals.isEmpty()) {
                        "Prüfe Konkurrenzen, Reihenfolge und Verträglichkeit …"
                    } else {
                        "Gewichte Ziele ${chunkIndex * MAX_GOALS_PER_REQUEST + 1}–${chunkIndex * MAX_GOALS_PER_REQUEST + goals.size} …"
                    },
                ),
            )
            val raw = client.request(
                payload = codexPayload(request, goals, includeStackConclusion = includeStackConclusion),
                operationId = "${request.evaluationId}:$chunkIndex",
                allowNetworkRetry = allowNetworkRetry,
                onEvent = onEvent,
            )
            val parsed = tryParse(request, goals, raw)
            if (parsed != null) {
                partials += parsed
                narratives += parsed.narrative
                continue
            }

            onEvent(CodexStreamEvent.Stage("Repariere Antwortformat …"))
            val repairedRaw = runCatching {
                client.request(
                    payload = codexPayload(
                        request,
                        goals,
                        repairRawOutput = raw,
                        includeStackConclusion = includeStackConclusion,
                    ),
                    operationId = "${request.evaluationId}:$chunkIndex:repair",
                    allowNetworkRetry = false,
                    onEvent = onEvent,
                )
            }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                return CodexEvaluationOutcome.InvalidJson(
                    preservedNarrative = (narratives + CodexJson.bestEffortNarrative(raw)).filter(String::isNotBlank)
                        .joinToString("\n\n"),
                    rawOutput = raw,
                    reason = error.message ?: "JSON-Reparatur fehlgeschlagen.",
                )
            }
            val repaired = tryParse(request, goals, repairedRaw)
                ?: return CodexEvaluationOutcome.InvalidJson(
                    preservedNarrative = (narratives + CodexJson.bestEffortNarrative(repairedRaw.ifBlank { raw }))
                        .filter(String::isNotBlank)
                        .joinToString("\n\n"),
                    rawOutput = repairedRaw,
                    reason = parseError(request, goals, repairedRaw),
                )
            partials += repaired
            narratives += repaired.narrative
        }
        return CodexEvaluationOutcome.Success(merge(partials))
    }

    private fun tryParse(
        request: CodexEvaluationRequest,
        goals: List<CodexGoal>,
        raw: String,
    ): CodexEvaluation? = runCatching {
        CodexJson.parse(raw, request.supplementIds, goals.mapTo(mutableSetOf()) { it.id }, request.questionIds)
    }.getOrNull()

    private fun parseError(request: CodexEvaluationRequest, goals: List<CodexGoal>, raw: String): String =
        runCatching {
            CodexJson.parse(raw, request.supplementIds, goals.mapTo(mutableSetOf()) { it.id }, request.questionIds)
            "Unbekannter JSON-Fehler."
        }.exceptionOrNull()?.message ?: "Unbekannter JSON-Fehler."

    private fun merge(parts: List<CodexEvaluation>): CodexEvaluation {
        val competitions = linkedMapOf<String, Competition>()
        val answers = linkedMapOf<String, QuestionAnswer>()
        for (part in parts) {
            for (competition in part.competitions) {
                val key = listOf(competition.supplementAId, competition.supplementBId).sorted().joinToString("\u0000")
                competitions.putIfAbsent(key, competition)
            }
            for (answer in part.answers) answers.putIfAbsent(answer.questionId, answer)
        }
        return CodexEvaluation(
            cells = parts.flatMap { it.cells },
            competitions = competitions.values.toList(),
            answers = answers.values.toList(),
            narrative = parts.map { it.narrative }.filter(String::isNotBlank).joinToString("\n\n"),
            notices = parts.flatMap { it.notices }.distinct(),
        )
    }

    private companion object {
        const val MAX_GOALS_PER_REQUEST = 12
    }
}
