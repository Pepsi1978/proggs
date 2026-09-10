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
        return improveText(
            text = text,
            previousVersions = previousVersions,
            outputField = "ziel",
            schemaName = "verbessertes_ziel",
            instructions = "Formuliere den diktierten Text als ein klares, präzises deutsches Ziel. " +
                "Erhalte Absicht, Bedeutung, sämtliche Einzelheiten und Person vollständig. " +
                "Verbessere Grammatik, Satzbau, Wortwahl und deutsche Rechtschreibung; " +
                "löse Versprecher, Füllwörter und Wiederholungen auf. Kürze den Inhalt nicht. " +
                "Ergänze keine medizinischen Tatsachen, schwäche nichts ab und erkläre nichts. " +
                "Verwende echte Umlaute und ß. Es gibt keine Zeichen-, Wort- oder Zeilenbegrenzung.",
        )
    }

    suspend fun improveAdditionalInformation(text: String, previousVersions: List<String>): String {
        return improveText(
            text = text,
            previousVersions = previousVersions,
            outputField = "informationen",
            schemaName = "verbesserte_zusatzinformationen",
            instructions = "Formuliere die zusätzlichen Informationen für eine gemeinsame Stack-Auswertung klar und präzise auf Deutsch. " +
                "Erhalte Bedeutung, sämtliche Einzelheiten, Zeitangaben, Einnahmebedingungen und persönliche Beobachtungen vollständig. " +
                "Verbessere Grammatik, Satzbau, Wortwahl und deutsche Rechtschreibung; " +
                "löse Versprecher, Füllwörter und Wiederholungen auf. Kürze den Inhalt nicht. " +
                "Ergänze keine medizinischen Tatsachen, schwäche nichts ab und erkläre nichts. " +
                "Verwende echte Umlaute und ß. Es gibt keine Zeichen-, Wort- oder Zeilenbegrenzung.",
        )
    }

    private suspend fun improveText(
        text: String,
        previousVersions: List<String>,
        outputField: String,
        schemaName: String,
        instructions: String,
    ): String {
        val appSettings = settings.einstellungen.first()
        val model = CodexModel.entries.first { it.apiId == appSettings.codexModell }
        val reasoning = ReasoningEffort.entries.first { it.apiValue == appSettings.codexDenkstufe }
        val raw = client.request(textPayload(text, previousVersions, model, reasoning, outputField, schemaName, instructions))
        return runCatching { JSONObject(raw).getString(outputField) }
            .getOrElse { throw CodexException(CodexErrorKind.NETWORK, "Codex hat keinen gültigen verbesserten Text geliefert.", it) }
            .trim()
            .mitDeutschenUmlauten()
            .takeIf(String::isNotBlank)
            ?: throw CodexException(CodexErrorKind.NETWORK, "Codex hat einen leeren verbesserten Text geliefert.")
    }

    private fun textPayload(
        text: String,
        previousVersions: List<String>,
        model: CodexModel,
        reasoning: ReasoningEffort,
        outputField: String,
        schemaName: String,
        baseInstructions: String,
    ): JSONObject = JSONObject()
        .put("model", model.apiId)
        .put("stream", true)
        .put("store", false)
        .put(
            "instructions",
            buildString {
                append(baseInstructions)
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
                    .put("name", schemaName)
                    .put("strict", true)
                    .put(
                        "schema",
                        JSONObject()
                            .put("type", "object")
                            .put(
                                "properties",
                                JSONObject().put(
                                    outputField,
                                    JSONObject().put("type", "string"),
                                ),
                            )
                            .put("required", JSONArray().put(outputField))
                            .put("additionalProperties", false),
                    ),
            ),
        )
}
