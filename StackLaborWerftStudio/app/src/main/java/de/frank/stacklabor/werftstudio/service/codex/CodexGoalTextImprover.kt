package de.frank.stacklabor.werftstudio.service.codex

import de.frank.stacklabor.werftstudio.data.preferences.EinstellungenStore
import de.frank.stacklabor.werftstudio.domain.model.mitDeutschenUmlauten
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class CodexGoalTextImprover(
    private val settings: EinstellungenStore,
    private val client: CodexResponsesClient,
) {
    suspend fun improve(text: String): String {
        val appSettings = settings.einstellungen.first()
        val model = CodexModel.entries.first { it.apiId == appSettings.codexModell }
        val reasoning = ReasoningEffort.entries.first { it.apiValue == appSettings.codexDenkstufe }
        val raw = client.request(goalTextPayload(text, model, reasoning))
        return runCatching { JSONObject(raw).getString("ziel") }
            .getOrElse { throw CodexException(CodexErrorKind.NETWORK, "Codex hat keinen gültigen Zieltext geliefert.", it) }
            .trim()
            .mitDeutschenUmlauten()
            .take(MAX_GOAL_LENGTH)
            .takeIf(String::isNotBlank)
            ?: throw CodexException(CodexErrorKind.NETWORK, "Codex hat einen leeren Zieltext geliefert.")
    }

    private fun goalTextPayload(text: String, model: CodexModel, reasoning: ReasoningEffort): JSONObject = JSONObject()
        .put("model", model.apiId)
        .put("stream", true)
        .put("store", false)
        .put(
            "instructions",
            "Formuliere den diktierten Text als ein klares, präzises deutsches Ziel. " +
                "Erhalte Absicht, Bedeutung und Person vollständig. Verbessere Grammatik, Satzbau, " +
                "Wortwahl und deutsche Rechtschreibung; löse Versprecher, Füllwörter und Wiederholungen auf. " +
                "Ergänze keine medizinischen Tatsachen, schwäche nichts ab und erkläre nichts. " +
                "Verwende echte Umlaute und ß. Das Ziel darf höchstens $MAX_GOAL_LENGTH Zeichen lang sein.",
        )
        .put(
            "input",
            JSONArray().put(JSONObject().put("role", "user").put("content", text.take(MAX_GOAL_LENGTH))),
        )
        .put("reasoning", JSONObject().put("effort", reasoning.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "verbessertes_ziel")
                    .put("strict", true)
                    .put(
                        "schema",
                        JSONObject()
                            .put("type", "object")
                            .put(
                                "properties",
                                JSONObject().put(
                                    "ziel",
                                    JSONObject().put("type", "string").put("maxLength", MAX_GOAL_LENGTH),
                                ),
                            )
                            .put("required", JSONArray().put("ziel"))
                            .put("additionalProperties", false),
                    ),
            ),
        )

    private companion object {
        const val MAX_GOAL_LENGTH = 200
    }
}
