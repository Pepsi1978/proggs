package de.frank.entropyreducer.domain.kiquestion

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.EntryStatus
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Generiert die "KI-Frage des Moments" DYNAMISCH durch Gemini-API
 * (Frank-Wunsch 2026-05-08: "spontane Frage durch Meta-Intelligenz aus allen
 * aktuellen Aufgaben, keine vordefinierten Standardfragen").
 *
 * Gibt den vollstaendigen Kontext (offene Eintraege, Biomarker, Schicht heute/morgen)
 * an Gemini und laesst die KI eine personalisierte, kontextrelevante Frage formulieren.
 *
 * Fallback auf statischen [KiQuestionGenerator] wenn kein API-Key vorhanden ist
 * oder der Call fehlschlaegt — die Frage-Card soll nicht leer bleiben.
 */
class GenerateKiQuestionUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val staticGenerator: KiQuestionGenerator,
) {

    suspend operator fun invoke(avoidPreviousText: String? = null): KiQuestion? {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val openEntries = entries.getActive().first()
            .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
        val latest = biomarkerDao.getLatest().first()
        val todayCal = calendarDao.getDay(today.toString()).first()
        val tomorrowCal = calendarDao.getDay(tomorrow.toString()).first()

        val key = secrets.geminiApiKey
        if (key.isNullOrBlank() || openEntries.isEmpty()) {
            // Frank-Wunsch 2026-05-09: KEIN statischer Fallback. Wenn keine
            // echte KI-Frage gebildet werden kann, wird gar keine Frage angezeigt.
            return null
        }

        val model = settings.geminiModelFlow.first()
        val context = buildContext(openEntries, latest, todayCal, tomorrowCal, avoidPreviousText)
        val systemPrompt = SYSTEM_PROMPT
        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(context)),
                        ),
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
                Diag.w(DiagnosticArea.AGENTIC, TAG, "Leere KI-Antwort — keine Frage anzeigen (Frank-Wunsch 2026-05-09)")
                null
            } else {
                KiQuestion(
                    triggerKey = "ai_${System.currentTimeMillis()}",
                    text = text,
                    rationale = "KI-generiert (Gemini $model) basierend auf ${openEntries.size} offenen Eintraegen",
                    relatedEntryIds = openEntries.take(3).map { it.id },
                )
            }
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "KI-Frage-Generierung fehlgeschlagen — keine Frage anzeigen", t)
            null
        }
    }

    private fun buildContext(
        openEntries: List<de.frank.entropyreducer.data.local.entities.EntropyEntryEntity>,
        biomarker: de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity?,
        todayCal: de.frank.entropyreducer.data.local.entities.CalendarDayEntity?,
        tomorrowCal: de.frank.entropyreducer.data.local.entities.CalendarDayEntity?,
        avoidPreviousText: String? = null,
    ): String = buildString {
        appendLine("AKTUELLER ZUSTAND DES NUTZERS:")
        appendLine()
        appendLine("Heute: ${todayCal?.shiftCode ?: "unbekannt"} (${todayCal?.rawCalendarText.orEmpty()})")
        appendLine("Morgen: ${tomorrowCal?.shiftCode ?: "unbekannt"} (${tomorrowCal?.rawCalendarText.orEmpty()})")
        appendLine()
        if (biomarker != null) {
            appendLine("Whoop-Biomarker (heute):")
            biomarker.recoveryScore?.let { appendLine("  Recovery: $it %") }
            biomarker.hrvMs?.let { appendLine("  HRV: ${"%.1f".format(it)} ms") }
            biomarker.restingHeartRate?.let { appendLine("  Ruhepuls: $it bpm") }
            biomarker.sleepPerformance?.let { appendLine("  Schlafqualitaet: $it %") }
            biomarker.sleepTotalMinutes?.let { appendLine("  Schlafdauer: ${it / 60}h ${it % 60}min") }
            biomarker.dayStrain?.let { appendLine("  Strain: ${"%.1f".format(it)}") }
            appendLine()
        }
        appendLine("OFFENE EINTRAEGE (${openEntries.size} Stück, sortiert nach Prioritaet):")
        openEntries.sortedByDescending { it.priorityScore }.take(15).forEachIndexed { i, e ->
            appendLine("${i + 1}. [${e.category.name}] ${e.title} (Schweregrad ${e.severity}/10, Bucket ${e.timeBucket})")
            if (e.description.isNotBlank()) {
                appendLine("   Beschreibung: ${e.description.take(120)}")
            }
        }
        appendLine()
        appendLine("DEINE AUFGABE: Formuliere genau EINE personalisierte, intelligente Frage")
        appendLine("die Frank weiterbringt — nicht generisch, sondern konkret auf seine Situation")
        appendLine("und seine offenen Eintraege bezogen. Die Frage soll dazu führen dass Frank")
        appendLine("nachdenkt, eine Entscheidung trifft oder einen neuen Eintrag macht.")
        appendLine()
        appendLine("REGELN:")
        appendLine("- Maximal 2 Sätze. Prägnant, klar.")
        appendLine("- Echtes Deutsch mit Umlauten ä ö ü ß. NIEMALS ae/oe/ue/ss als Ersatz.")
        appendLine("- Du-Anrede.")
        appendLine("- Kein Smalltalk, keine Begrüßung — direkt die Frage.")
        appendLine("- Beziehe dich auf konkrete Einträge oder Werte aus dem Kontext oben.")
        appendLine("- Nur die Frage zurück, KEINE Anführungszeichen, kein Vorspann.")
        if (!avoidPreviousText.isNullOrBlank()) {
            appendLine()
            appendLine("WICHTIG: Die letzte Frage war:")
            appendLine("\"$avoidPreviousText\"")
            appendLine("Stelle eine ANDERE Frage zu einem ANDEREN Aspekt — nicht das gleiche Thema.")
        }
    }

    companion object {
        private const val TAG = "GenerateKiQuestion"
        private const val SYSTEM_PROMPT = """Du bist das Genie aus dem Entropie Reductor — ein Coach mit hoher Meta-Intelligenz, der Frank dabei hilft Entropie in seinem Leben zu reduzieren. Deine Antworten sind immer auf Deutsch mit echten Umlauten (ä, ö, ü, ß) und niemals mit ae/oe/ue/ss-Ersatz. Du formulierst eine einzige prägnante Frage, die Frank konkret und individuell weiterbringt."""
    }
}
