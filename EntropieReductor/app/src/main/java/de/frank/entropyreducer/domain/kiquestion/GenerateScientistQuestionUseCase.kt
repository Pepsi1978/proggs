package de.frank.entropyreducer.domain.kiquestion

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.usecase.ScientistChatUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Generiert die "Forscher-Frage des Moments" — die proaktive Frage des Wissenschaftlers
 * im Forscher-Bereich, getrennt von der KI-Frage auf dem Aufgaben-Screen.
 *
 * Fokus: Wissensluecken, ungeklaerte Zusammenhaenge, ungenutzte Memory-Eintraege,
 * laufende Hypothesen die mehr Information brauchen. Nicht: aktive Aufgaben.
 *
 * Frank-Wunsch 2026-05-09: Kein statischer Fallback — wenn keine echte Frage
 * gebildet werden kann, wird gar keine angezeigt.
 */
class GenerateScientistQuestionUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val hypotheses: HypothesisRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val insightDao: InsightDao,
) {

    suspend operator fun invoke(avoidPreviousText: String? = null): KiQuestion? {
        val key = secrets.geminiApiKey
        if (key.isNullOrBlank()) return null

        val activeMemories = memories.getActive().first()
        val pendingMemoryProposals = memories.getProposals().first().filter { !it.isActive }
        val activeHypotheses = hypotheses.observeByStatus(HypothesisStatus.AKTIV).first()
        val proposedHypotheses = hypotheses.observeByStatus(HypothesisStatus.VORGESCHLAGEN).first()
        val confirmedInsights = insightDao.getConfirmed().first()
        val biomarker = biomarkerDao.getLatest().first()
        val fourteenDaysAgo = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val recentlyResolved = entries.getRecentlyResolved(fourteenDaysAgo).first()
        val resolutionMethods = recentlyResolved
            .mapNotNull { ScientistChatUseCase.extractResolutionMethod(it.aiNotes) }
            .take(8)

        // Wenn ueberhaupt keine Datenbasis vorliegt: nichts fragen.
        if (activeMemories.isEmpty() && pendingMemoryProposals.isEmpty() &&
            activeHypotheses.isEmpty() && proposedHypotheses.isEmpty() &&
            confirmedInsights.isEmpty() && resolutionMethods.isEmpty()
        ) {
            return null
        }

        val model = settings.geminiModelFlow.first()
        val context = buildContext(
            activeMemories = activeMemories,
            pendingProposals = pendingMemoryProposals,
            activeHypotheses = activeHypotheses,
            proposedHypotheses = proposedHypotheses,
            confirmedInsights = confirmedInsights,
            biomarker = biomarker,
            resolutionMethods = resolutionMethods,
            avoidPreviousText = avoidPreviousText,
        )

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(SYSTEM_PROMPT))),
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(context))),
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.85),
                ),
            )
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?.removePrefix("\"")?.removeSuffix("\"")
            if (text.isNullOrBlank()) {
                Diag.w(DiagnosticArea.AGENTIC, TAG, "Leere KI-Antwort — keine Forscher-Frage anzeigen")
                null
            } else {
                KiQuestion(
                    triggerKey = "scientist_${System.currentTimeMillis()}",
                    text = text,
                    rationale = "Forscher-Frage (Gemini $model)",
                    relatedEntryIds = emptyList(),
                )
            }
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "Forscher-Frage-Generierung fehlgeschlagen", t)
            null
        }
    }

    private fun buildContext(
        activeMemories: List<de.frank.entropyreducer.data.local.entities.MemoryEntryEntity>,
        pendingProposals: List<de.frank.entropyreducer.data.local.entities.MemoryEntryEntity>,
        activeHypotheses: List<de.frank.entropyreducer.data.local.entities.HypothesisEntity>,
        proposedHypotheses: List<de.frank.entropyreducer.data.local.entities.HypothesisEntity>,
        confirmedInsights: List<de.frank.entropyreducer.data.local.entities.InsightEntity>,
        biomarker: de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity?,
        resolutionMethods: List<String>,
        avoidPreviousText: String?,
    ): String = buildString {
        appendLine("KONTEXT FUER DIE PROAKTIVE FRAGE DES WISSENSCHAFTLERS:")
        appendLine()
        if (biomarker != null) {
            appendLine("Aktuelle Biomarker:")
            biomarker.recoveryScore?.let { appendLine("  Recovery: $it %") }
            biomarker.hrvMs?.let { appendLine("  HRV: ${"%.1f".format(it)} ms") }
            biomarker.restingHeartRate?.let { appendLine("  Ruhepuls: $it bpm") }
            biomarker.sleepPerformance?.let { appendLine("  Schlafqualitaet: $it %") }
            appendLine()
        }
        if (activeMemories.isNotEmpty()) {
            appendLine("Aktives Gedaechtnis (${activeMemories.size}):")
            activeMemories.take(15).forEach { appendLine("- ${it.content.take(150)}") }
            appendLine()
        }
        if (pendingProposals.isNotEmpty()) {
            appendLine("Memory-Vorschlaege noch ungelesen (${pendingProposals.size}):")
            pendingProposals.take(8).forEach { appendLine("- ${it.content.take(120)}") }
            appendLine()
        }
        if (activeHypotheses.isNotEmpty()) {
            appendLine("Laufende Experimente (${activeHypotheses.size}):")
            activeHypotheses.forEach { appendLine("- \"${it.title}\": ${it.description.take(120)}") }
            appendLine()
        }
        if (proposedHypotheses.isNotEmpty()) {
            appendLine("Vorgeschlagene, noch nicht gestartete Hypothesen (${proposedHypotheses.size}):")
            proposedHypotheses.take(8).forEach { appendLine("- \"${it.title}\"") }
            appendLine()
        }
        if (confirmedInsights.isNotEmpty()) {
            appendLine("Bestätigte Insights:")
            confirmedInsights.take(8).forEach { appendLine("- ${it.title} (conf=${it.confidence})") }
            appendLine()
        }
        if (resolutionMethods.isNotEmpty()) {
            appendLine("Bewaehrte Methoden des Nutzers (zuletzt erfolgreich):")
            resolutionMethods.forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("DEINE AUFGABE: Formuliere genau EINE proaktive Frage, die du als Forscher")
        appendLine("Frank stellst, um SEIN Modell der Welt scharfer zu machen. Frage genau dort,")
        appendLine("wo du als Forscher noch eine Wissensluecke siehst — wo Korrelationen unklar sind,")
        appendLine("wo Erfahrungen noch nicht verallgemeinert wurden, wo eine Hypothese mehr Information")
        appendLine("braeuchte um getestet zu werden.")
        appendLine()
        appendLine("REGELN:")
        appendLine("- Maximal 2 Saetze. Prägnant, klar.")
        appendLine("- Echtes Deutsch mit Umlauten ä ö ü ß. NIEMALS ae/oe/ue/ss als Ersatz.")
        appendLine("- Du-Anrede.")
        appendLine("- Kein Smalltalk, keine Begruessung — direkt die Frage.")
        appendLine("- Beziehe dich auf konkrete Inhalte oben (Memory, Hypothese, Insight, Methode).")
        appendLine("- Fokus auf Wissensluecken, nicht auf aktive Aufgaben.")
        appendLine("- Nur die Frage zurück, KEINE Anfuehrungszeichen, kein Vorspann.")
        if (!avoidPreviousText.isNullOrBlank()) {
            appendLine()
            appendLine("WICHTIG: Die letzte Forscher-Frage war:")
            appendLine("\"$avoidPreviousText\"")
            appendLine("Stelle eine ANDERE Frage zu einem ANDEREN Aspekt.")
        }
    }

    companion object {
        private const val TAG = "GenScientistQuestion"
        private const val SYSTEM_PROMPT = """Du bist der Forscher aus dem Entropie Reductor — ein wissenschaftlicher Assistent mit hoher Meta-Intelligenz. Du formulierst eine einzige prägnante, proaktive Frage an Frank, die seine Wissensbasis schaerft. Deine Antworten sind immer auf Deutsch mit echten Umlauten (ä, ö, ü, ß) und niemals mit ae/oe/ue/ss-Ersatz."""
    }
}
