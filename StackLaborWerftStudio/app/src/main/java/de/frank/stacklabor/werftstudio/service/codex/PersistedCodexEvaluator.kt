package de.frank.stacklabor.werftstudio.service.codex

import de.frank.stacklabor.werftstudio.data.preferences.EinstellungenStore
import de.frank.stacklabor.werftstudio.data.repository.StackLaborRepository
import de.frank.stacklabor.werftstudio.domain.model.Bewertung
import de.frank.stacklabor.werftstudio.domain.model.BewertungMitDetails
import de.frank.stacklabor.werftstudio.domain.model.BewertungsBereich
import de.frank.stacklabor.werftstudio.domain.model.Bewertungszelle
import de.frank.stacklabor.werftstudio.domain.model.FrageAntwort
import de.frank.stacklabor.werftstudio.domain.model.Konkurrenz
import de.frank.stacklabor.werftstudio.domain.model.KonkurrenzArt
import de.frank.stacklabor.werftstudio.domain.model.Wirkung
import de.frank.stacklabor.werftstudio.service.auth.CodexOAuthClient
import de.frank.stacklabor.werftstudio.service.work.DeferredCodexEvaluationRunner
import java.util.UUID
import java.security.MessageDigest
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class PersistedCodexEvaluator(
    private val repository: StackLaborRepository,
    private val settings: EinstellungenStore,
    private val oauth: CodexOAuthClient,
    private val service: CodexEvaluationService,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : DeferredCodexEvaluationRunner {
    override suspend fun isAuthenticationValid(): Boolean = runCatching {
        oauth.validAccessToken()
        true
    }.getOrDefault(false)

    override suspend fun runOnce(stackId: String) {
        evaluateStack(stackId = stackId)
    }

    suspend fun evaluateStack(
        stackId: String,
        evaluationId: String = UUID.randomUUID().toString(),
        persist: Boolean = true,
        onEvent: suspend (CodexStreamEvent) -> Unit = {},
    ): BewertungMitDetails {
        val inhalt = repository.ladeStackInhalt(stackId)
        val zielKatalog = repository.beobachteZiele().first().associateBy { it.id }
        val appSettings = settings.einstellungen.first()
        val model = CodexModel.entries.first { it.apiId == appSettings.codexModell }
        val reasoning = ReasoningEffort.entries.first { it.apiValue == appSettings.codexDenkstufe }
        val request = CodexEvaluationRequest(
            evaluationId = evaluationId,
            contextJson = contextJson(inhalt),
            goals = inhalt.ziele.mapNotNull { stackZiel ->
                zielKatalog[stackZiel.zielId]?.let { CodexGoal(it.id, stackZiel.rang, it.text) }
            },
            supplementIds = inhalt.eintraege.filter { it.aktiv }.mapTo(linkedSetOf()) { it.mittelId },
            questionIds = inhalt.fragen.mapTo(linkedSetOf()) { it.id },
            model = model,
            reasoningEffort = reasoning,
        )
        val checksum = repository.berechnePruefsumme(stackId, appSettings.dosisVariante)
        val outcome = service.evaluate(request, onEvent = onEvent)
        val details = when (outcome) {
            is CodexEvaluationOutcome.Success -> outcome.evaluation.toDomain(
                evaluationId = evaluationId,
                bereich = BewertungsBereich.STACK,
                stackId = stackId,
                checksum = checksum,
                model = model.apiId,
                reasoning = reasoning.apiValue,
            )
            is CodexEvaluationOutcome.InvalidJson -> BewertungMitDetails(
                bewertung = Bewertung(
                    id = evaluationId,
                    bereich = BewertungsBereich.STACK,
                    stackId = stackId,
                    zeitpunktEpochMillis = clockMillis(),
                    modell = model.apiId,
                    denkstufe = reasoning.apiValue,
                    pruefsumme = checksum,
                    gesamt = outcome.preservedNarrative,
                    hinweise = listOf("Antwortformat ungültig: ${outcome.reason}"),
                ),
                zellen = emptyList(),
                konkurrenzen = emptyList(),
                antworten = emptyList(),
            )
        }
        if (persist) repository.speichereBewertung(details)
        return details
    }

    suspend fun evaluateAllStacks(
        evaluationId: String = UUID.randomUUID().toString(),
        onEvent: suspend (CodexStreamEvent) -> Unit = {},
    ): BewertungMitDetails {
        val stacks = repository.beobachteStacks().first()
        val inhalte = stacks.map { repository.ladeStackInhalt(it.id) }
        val zielKatalog = repository.beobachteZiele().first().associateBy { it.id }
        val appSettings = settings.einstellungen.first()
        val model = CodexModel.entries.first { it.apiId == appSettings.codexModell }
        val reasoning = ReasoningEffort.entries.first { it.apiValue == appSettings.codexDenkstufe }
        val goals = inhalte.flatMap { it.ziele }
            .groupBy { it.zielId }
            .mapNotNull { (zielId, zuordnungen) ->
                zielKatalog[zielId]?.let { ziel ->
                    CodexGoal(ziel.id, zuordnungen.minOf { it.rang }, ziel.text)
                }
            }
            .sortedBy { it.rank }
        val checksumInput = stacks.map {
            repository.berechnePruefsumme(it.id, appSettings.dosisVariante)
        }.sorted().joinToString("\u0000")
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(checksumInput.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val request = CodexEvaluationRequest(
            evaluationId = evaluationId,
            contextJson = JSONObject()
                .put("bereich", "TAG")
                .put("stacks", JSONArray(inhalte.map { JSONObject(contextJson(it)) }))
                .toString(),
            goals = goals,
            supplementIds = inhalte.flatMap { it.eintraege }
                .filter { it.aktiv }
                .mapTo(linkedSetOf()) { it.mittelId },
            questionIds = inhalte.flatMap { it.fragen }.mapTo(linkedSetOf()) { it.id },
            model = model,
            reasoningEffort = reasoning,
        )
        val outcome = service.evaluate(request, onEvent = onEvent)
        val details = when (outcome) {
            is CodexEvaluationOutcome.Success -> outcome.evaluation.toDomain(
                evaluationId = evaluationId,
                bereich = BewertungsBereich.TAG,
                stackId = null,
                checksum = checksum,
                model = model.apiId,
                reasoning = reasoning.apiValue,
            )
            is CodexEvaluationOutcome.InvalidJson -> BewertungMitDetails(
                bewertung = Bewertung(
                    id = evaluationId,
                    bereich = BewertungsBereich.TAG,
                    stackId = null,
                    zeitpunktEpochMillis = clockMillis(),
                    modell = model.apiId,
                    denkstufe = reasoning.apiValue,
                    pruefsumme = checksum,
                    gesamt = outcome.preservedNarrative,
                    hinweise = listOf("Antwortformat ungültig: ${outcome.reason}"),
                ),
                zellen = emptyList(),
                konkurrenzen = emptyList(),
                antworten = emptyList(),
            )
        }
        repository.speichereBewertung(details)
        return details
    }

    private fun contextJson(inhalt: de.frank.stacklabor.werftstudio.domain.model.StackInhalt): String {
        val mittel = inhalt.mittel.associateBy { it.id }
        return JSONObject()
            .put(
                "stack",
                JSONObject()
                    .put("id", inhalt.stack.id)
                    .put("name", inhalt.stack.name)
                    .put("zeitpunkt", inhalt.stack.zeitpunkt)
                    .put("einnahme_hinweis", inhalt.stack.einnahmeHinweis),
            )
            .put(
                "mittel",
                JSONArray(inhalt.eintraege.filter { it.aktiv }.map { eintrag ->
                    val katalog = mittel.getValue(eintrag.mittelId)
                    JSONObject()
                        .put("id", katalog.id)
                        .put("name", katalog.name)
                        .put("aktiv", eintrag.aktiv)
                        .put("loeslichkeit", katalog.loeslichkeit.name)
                        .put("frequenz", eintrag.frequenzTyp.name)
                        .put("alle_n_tage", eintrag.alleNTage)
                        .put("zusatztext", eintrag.zusatztext)
                        .put("dosen", JSONArray(eintrag.dosen.map { dosis ->
                            JSONObject()
                                .put("variante", dosis.variante.name)
                                .put("stueckzahl", dosis.stueckzahl.toPlainString())
                                .put("menge_je_stueck", dosis.mengeJeStueck?.toPlainString())
                                .put("einheit", dosis.einheitText ?: dosis.einheit?.name)
                        }))
                        .put("alterniert_mit", JSONArray(eintrag.alternationsPartnerMittelIds))
                }),
            )
            .put("fragen", JSONArray(inhalt.fragen.map { JSONObject().put("id", it.id).put("text", it.text) }))
            .toString()
    }

    private fun CodexEvaluation.toDomain(
        evaluationId: String,
        bereich: BewertungsBereich,
        stackId: String?,
        checksum: String,
        model: String,
        reasoning: String,
    ): BewertungMitDetails = BewertungMitDetails(
        bewertung = Bewertung(
            id = evaluationId,
            bereich = bereich,
            stackId = stackId,
            zeitpunktEpochMillis = clockMillis(),
            modell = model,
            denkstufe = reasoning,
            pruefsumme = checksum,
            gesamt = narrative,
            hinweise = notices,
        ),
        zellen = cells.map {
            Bewertungszelle(
                evaluationId,
                it.supplementId,
                it.goalId,
                if (it.effect == CellEffect.SUPPORTS) Wirkung.STUETZT else Wirkung.STOERT,
                it.strength,
                it.reason,
            )
        },
        konkurrenzen = competitions.map {
            Konkurrenz(
                evaluationId,
                it.supplementAId,
                it.supplementBId,
                when (it.type) {
                    CompetitionType.ABSORPTION -> KonkurrenzArt.AUFNAHME
                    CompetitionType.EFFECT -> KonkurrenzArt.WIRKUNG
                    CompetitionType.TIMING -> KonkurrenzArt.ZEITPUNKT
                },
                it.severity,
                it.reason,
            )
        },
        antworten = answers.map { FrageAntwort(evaluationId, it.questionId, it.text) },
    )
}
