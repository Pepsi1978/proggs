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
    suspend fun improve(text: String, previousVersions: List<String>): String {
        val appSettings = settings.einstellungen.first()
        val model = CodexModel.entries.first { it.apiId == appSettings.codexModell }
        val reasoning = ReasoningEffort.entries.first { it.apiValue == appSettings.codexDenkstufe }
        val raw = client.request(goalTextPayload(text, previousVersions, model, reasoning))
        return runCatching { JSONObject(raw).getString("ziel") }
            .getOrElse { throw CodexException(CodexErrorKind.NETWORK, "Codex hat keinen gültigen Zieltext geliefert.", it) }
            .trim()
            .mitDeutschenUmlauten()
            .takeIf(String::isNotBlank)
            ?: throw CodexException(CodexErrorKind.NETWORK, "Codex hat einen leeren Zieltext geliefert.")
    }

    private fun goalTextPayload(
        text: String,
        previousVersions: List<String>,
        model: CodexModel,
        reasoning: ReasoningEffort,
    ): JSONObject = JSONObject()
        .put("model", model.apiId)
        .put("stream", true)
        .put("store", false)
        .put(
            "instructions",
            buildString {
                append("Formuliere den diktierten Text als ein klares, präzises deutsches Ziel. ")
                append("Erhalte Absicht, Bedeutung, sämtliche Einzelheiten und Person vollständig. ")
                append("Verbessere Grammatik, Satzbau, Wortwahl und deutsche Rechtschreibung; ")
                append("löse Versprecher, Füllwörter und Wiederholungen auf. Kürze den Inhalt nicht. ")
                append("Ergänze keine medizinischen Tatsachen, schwäche nichts ab und erkläre nichts. ")
                append("Verwende echte Umlaute und ß. Es gibt keine Zeichen-, Wort- oder Zeilenbegrenzung.")
                if (previousVersions.isNotEmpty()) {
                    append("\n\nDiese Fassungen wurden bereits vorgeschlagen. Liefere bei gleichem Inhalt eine deutlich andere ")
                    append("Formulierung mit anderem Aufbau und anderer Wortwahl:\n")
                    append(JSONArray(previousVersions).toString())
                }
            },
        )
        .put(
            "input",
            JSONArray().put(JSONObject().put("role", "user").put("content", text)),
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
                                    JSONObject().put("type", "string"),
                                ),
                            )
                            .put("required", JSONArray().put("ziel"))
                            .put("additionalProperties", false),
                    ),
            ),
        )
}
