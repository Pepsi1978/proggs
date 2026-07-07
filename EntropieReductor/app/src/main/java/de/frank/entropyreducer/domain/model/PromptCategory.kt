package de.frank.entropyreducer.domain.model

/**
 * Kategorie eines [de.frank.entropyreducer.data.local.entities.SavedPromptEntity] (Frank-Wunsch
 * 2026-05-20). Prompts wirken nur in ihrem Bereich der App — Frank kann pro Bereich separate
 * Verhaltensregeln definieren statt eine globale Liste.
 *
 * Mapping zu UseCases / ViewModels:
 * - AUFGABEN → ProcessEntryUseCase (Aufgaben-Bewertung beim neuen Eintrag)
 * - ENTROPIE → Tagebuch-Bereich: Summary/Title/Improve
 * - THESEN → Thesen-Bereich: Summary/Title/Improve
 * - ANALYSE → GenerateAnalysisUseCase + GenerateReviewUseCase
 * - FORSCHER → ScientistChatUseCase + HypothesisChatUseCase
 * - CODEX → GenieCodexSynthesizer
 */
enum class PromptCategory(val label: String) {
    AUFGABEN("Aufgaben"),
    ENTROPIE("Entropie"),
    THESEN("Thesen"),
    ANALYSE("Analyse"),
    FORSCHER("Forscher"),
    CODEX("Codex"),
}
