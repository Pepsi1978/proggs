package de.frank.stacklabor.werftstudio.ui.model

import de.frank.stacklabor.werftstudio.service.tts.TtsConfiguration
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute

enum class Appearance { Light, Dark }

enum class DoseVariant { Frei, Dienst }

enum class SortMode { Loeslichkeit, Einnahme }

enum class SignalState { Green, Yellow, Red, Gray }

enum class Solubility { Water, Fat, Both }

enum class EvaluationState { Valid, Stale, Running, Offline, Reauth, Quota, NetworkError }

enum class PlaybackState { Ready, Playing, Paused }

enum class CodexLoginState { Waiting, Success, Expired, Denied, NetworkError }

data class SignalCounts(
    val green: Int,
    val yellow: Int,
    val red: Int,
)

data class StackSummaryUi(
    val id: String,
    val name: String,
    val meta: String,
    val medicineCount: Int,
    val signal: SignalState,
    val counts: SignalCounts,
)

data class MedicineUi(
    val id: String,
    val name: String,
    val dose: String,
    val reason: String = "",
    val solubility: Solubility = Solubility.Water,
    val signal: SignalState = SignalState.Green,
    val active: Boolean = true,
    val catalogMeta: String = "",
    val manufacturer: String = "",
    val form: String = "Kapsel",
    val diarrheaRisk: Boolean = false,
    val excipients: String = "",
    val pieces: String = "1",
    val amount: String = "",
    val unit: String = "",
    val frequency: String = "Täglich",
    val secondDose: Boolean = false,
    val together: Boolean = false,
    val note: String = "",
    val alternates: String = "",
) {
    val warningReason: String
        get() = reason.takeIf {
            it.split(" · ").map(String::trim).any { part ->
                part.isNotEmpty() &&
                    !part.startsWith("stützt Ziel") &&
                    !part.startsWith("Keine Konkurrenz") &&
                    !part.startsWith("Konkurrenzprüfung läuft") &&
                    !part.startsWith("Konkurrenzprüfung nicht möglich")
            }
        }.orEmpty()
}

data class GoalUi(
    val id: String,
    val rank: Int,
    val text: String,
    val signal: SignalState,
    val selected: Boolean = true,
    val reason: String = "",
    val usage: String = "in 4 Stacks verwendet",
)

data class QuestionUi(val id: String, val text: String)

data class RelationshipUi(
    val id: String,
    val rank: Int,
    val title: String,
    val signal: SignalState,
    val reason: String,
)

data class EvaluationRunUi(
    val id: String,
    val date: String,
    val model: String,
    val counts: SignalCounts,
    val selectedForComparison: Boolean = false,
)

data class HistoryChangeUi(
    val title: String,
    val from: SignalState,
    val to: SignalState,
)

data class SettingRowUi(val id: String, val label: String, val value: String = "")

data class DoseSummaryUi(val name: String, val dose: String, val meta: String)

data class CompetitionUi(val title: String, val detail: String)

data class StackLaborUiState(
    val loading: Boolean = true,
    val appearance: Appearance = Appearance.Light,
    val reducedMotion: Boolean = false,
    val doseVariant: DoseVariant = DoseVariant.Frei,
    val sortMode: SortMode = SortMode.Einnahme,
    /** In der Löslichkeits-Ansicht: stehen die fettlöslichen oben? */
    val solubilityFatFirst: Boolean = true,
    val searchQuery: String = "",
    val evaluationState: EvaluationState = EvaluationState.Valid,
    val allStacksEvaluationState: EvaluationState = EvaluationState.Stale,
    val playbackState: PlaybackState = PlaybackState.Ready,
    val codexLoginState: CodexLoginState = CodexLoginState.Waiting,
    val selectedStackId: String? = null,
    val stackName: String = "",
    val stackTime: String = "",
    val stackNote: String = "",
    val medicineName: String = "",
    val evaluationParagraphs: List<String> = emptyList(),
    val streamedEvaluationText: String = "",
    val evaluationMeta: String = "Noch nicht ausgewertet",
    val latestEvaluationId: String? = null,
    val spokenParagraphIndex: Int? = null,
    val questions: List<QuestionUi> = emptyList(),
    val stacks: List<StackSummaryUi> = emptyList(),
    val medicines: List<MedicineUi> = emptyList(),
    val catalogMedicines: List<MedicineUi> = emptyList(),
    val goals: List<GoalUi> = emptyList(),
    val breakdownItems: List<RelationshipUi> = emptyList(),
    val history: List<EvaluationRunUi> = emptyList(),
    val historyComparison: String = "",
    val historyChanges: List<HistoryChangeUi> = emptyList(),
    val dailyDoses: List<DoseSummaryUi> = emptyList(),
    val competitions: List<CompetitionUi> = emptyList(),
    val allStacksEvaluationId: String? = null,
    val ttsProviderLabel: String = "Microsoft Edge",
    val ttsVoiceLabel: String = "Seraphina",
    /** Alle Stimmen des gerade gewählten Anbieters, fertig beschriftet. */
    val ttsVoiceOptions: List<String> = emptyList(),
    val clonedVoicesLoading: Boolean = false,
    val clonedVoicesHint: String = "",
    val googleApiKeyValue: String = "",
    val qwenApiKeyValue: String = "",
    val googleApiKeyLabel: String = "Nicht hinterlegt",
    val qwenApiKeyLabel: String = "Nicht hinterlegt",
    val ttsSpeedLabel: String = "1,00×",
    val ttsPauseLabel: String = "Kurz",
    val ttsTimeoutLabel: String = "30 Min.",
    val ttsUsageLabel: String = "0 Zeichen",
    val codexAccountLabel: String = "Nicht angemeldet",
    val codexModelLabel: String = "Terra",
    val codexReasoningLabel: String = "Hoch",
    val codexDeviceCode: String = "",
    val codexVerificationUrl: String = "https://auth.openai.com/codex/device",
    val versionName: String = "",
    val versionBumpedAt: String = "",
)

sealed interface StackLaborUiEffect {
    data object OpenImportDocument : StackLaborUiEffect
    data class CreateExportDocument(val json: String) : StackLaborUiEffect
    /** Legt eine lesbare Textdatei an — für den Stack-Export. */
    data class CreateTextDocument(val fileName: String, val text: String) : StackLaborUiEffect
    data class OpenUrl(val url: String) : StackLaborUiEffect
    data class PlayTts(val text: String, val configuration: TtsConfiguration, val startParagraph: Int = 0) : StackLaborUiEffect
    data object PauseTts : StackLaborUiEffect
    data object ResumeTts : StackLaborUiEffect
    data object StopTts : StackLaborUiEffect
    data class Message(val text: String) : StackLaborUiEffect
}

sealed interface StackLaborEvent {
    data class RouteChanged(val route: StackLaborRoute) : StackLaborEvent
    data object ToggleAppearance : StackLaborEvent
    data class ChangeDoseVariant(val variant: DoseVariant) : StackLaborEvent
    data class ChangeSortMode(val mode: SortMode) : StackLaborEvent
    data class ChangeSearch(val query: String) : StackLaborEvent
    data class ToggleMedicine(val stackId: String, val medicineId: String) : StackLaborEvent
    data class RemoveMedicineFromStack(val stackId: String, val medicineId: String) : StackLaborEvent
    data class ReorderMedicine(val stackId: String, val medicineId: String, val targetIndex: Int) : StackLaborEvent
    /**
     * Übernimmt eine per Drag and Drop gezogene Reihenfolge vollständig — die Liste enthält
     * alle sichtbaren Mittel in der neuen Abfolge.
     */
    data class ApplyMedicineOrder(val stackId: String, val medicineIds: List<String>) : StackLaborEvent
    data class ApplyStackOrder(val stackIds: List<String>) : StackLaborEvent
    data class EditGoal(val goalId: String, val text: String) : StackLaborEvent
    data class ToggleGoal(val stackId: String, val goalId: String) : StackLaborEvent
    data class ReorderGoal(val stackId: String, val goalId: String, val targetRank: Int) : StackLaborEvent
    data class UpdateStackName(val value: String) : StackLaborEvent
    data class UpdateStackTime(val value: String) : StackLaborEvent
    data class UpdateStackNote(val value: String) : StackLaborEvent
    data class UpdateMedicineField(val field: String, val value: String) : StackLaborEvent
    data class SelectHistoryRun(val stackId: String, val runId: String) : StackLaborEvent
    data class SelectSetting(val settingId: String) : StackLaborEvent
    data class SelectSettingValue(val settingId: String, val value: String) : StackLaborEvent
    /** Speichert einen selbst hinterlegten Schlüssel; leerer Wert holt den aus dem SK-Ordner zurück. */
    data class SaveApiKey(val keyId: String, val value: String) : StackLaborEvent
    /** Holt die geklonten Alibaba-Stimmen frisch aus dem Konto. */
    data object LoadClonedVoices : StackLaborEvent
    data class RemoveQuestion(val questionId: String) : StackLaborEvent
    data class SaveStack(val stackId: String?) : StackLaborEvent
    data class DeleteStack(val stackId: String) : StackLaborEvent
    data class SaveMedicine(val medicineId: String?, val stackId: String?) : StackLaborEvent
    data class DeleteMedicine(val medicineId: String) : StackLaborEvent
    data class AddMedicineToStack(val stackId: String, val medicineId: String) : StackLaborEvent
    data class AddGoal(val text: String) : StackLaborEvent
    data class DeleteGoal(val goalId: String) : StackLaborEvent
    data class SaveQuestion(val stackId: String, val questionId: String?, val text: String) : StackLaborEvent
    data object Undo : StackLaborEvent
    data object EvaluateStack : StackLaborEvent
    data object EvaluateAll : StackLaborEvent
    data object RetryEvaluation : StackLaborEvent
    data object CancelEvaluation : StackLaborEvent
    data object ScheduleRetry : StackLaborEvent
    data object ExportData : StackLaborEvent
    /** Schreibt einen einzelnen Stack als Textdatei mit allen Angaben heraus. */
    data class ExportStack(val stackId: String) : StackLaborEvent
    /** Schreibt alle Stacks nacheinander in eine Textdatei. */
    data object ExportAllStacks : StackLaborEvent
    data object ImportData : StackLaborEvent
    data class ImportDocument(val json: String) : StackLaborEvent
    data object ImportSeedData : StackLaborEvent
    data object RestoreBackup : StackLaborEvent
    data object OpenCodexPage : StackLaborEvent
    data object RetryCodexLogin : StackLaborEvent
    data object Play : StackLaborEvent
    /** Liest ab dem angetippten Absatz weiter — bei langen Auswertungen der schnellste Weg. */
    data class PlayFromParagraph(val index: Int) : StackLaborEvent
    data object Pause : StackLaborEvent
    data object Stop : StackLaborEvent
    data class MergeMedicines(val keepId: String, val replaceId: String) : StackLaborEvent
}

val previewStacks = listOf(
    StackSummaryUi("stack-1", "Morgen-Stack Teil 1", "06:00 · nüchtern", 12, SignalState.Red, SignalCounts(3, 1, 1)),
    StackSummaryUi("stack-2", "Morgen-Stack Teil 2", "08:00 · zum Frühstück", 8, SignalState.Green, SignalCounts(5, 0, 0)),
    StackSummaryUi("stack-3", "Mittags-Stack", "12:30 · mit einer Mahlzeit", 9, SignalState.Yellow, SignalCounts(4, 1, 0)),
    StackSummaryUi("stack-4", "Nachmittags-Stack", "16:00 · Noch nicht ausgewertet", 6, SignalState.Gray, SignalCounts(0, 0, 0)),
    StackSummaryUi("stack-5", "Abend-Stack", "19:00 · zum Abendessen", 11, SignalState.Red, SignalCounts(2, 2, 1)),
    StackSummaryUi("stack-6", "Nacht-Stack", "22:30 · vor dem Schlafen", 7, SignalState.Green, SignalCounts(5, 0, 0)),
)

val previewMedicines = listOf(
    MedicineUi("pea", "PEA (Palmitoylethanolamid)", "2 × 80 mg = 160 mg", solubility = Solubility.Water),
    MedicineUi("alcar", "Acetyl-L-Carnitin (ALCAR)", "1 × 500 mg = 500 mg", "stört 3, 7", Solubility.Water, SignalState.Red),
    MedicineUi("nr", "Nicotinamid-Ribosid (NR)", "1 × 300 mg = 300 mg", "stört 4", Solubility.Water, SignalState.Yellow),
    MedicineUi("magnesium", "Magnesiumbisglycinat", "2 × 100 mg = 200 mg", solubility = Solubility.Water),
    MedicineUi("d3k2", "Vitamin D3 + K2", "1 × 2000 IE = 2000 IE", "stört 2", Solubility.Fat, SignalState.Yellow),
    MedicineUi("omega3", "Omega-3", "2 × 500 mg = 1000 mg", "stört 5", Solubility.Fat, SignalState.Red),
    MedicineUi("ala", "Alpha-Liponsäure", "1 × 300 mg = 300 mg", "nicht bedient", Solubility.Both, SignalState.Gray, active = false),
    MedicineUi("q10", "Coenzym Q10", "1 × 100 mg = 100 mg", solubility = Solubility.Fat),
    MedicineUi("quercetin", "Quercetin", "1 × 250 mg = 250 mg", "stört 1", Solubility.Both, SignalState.Yellow),
    MedicineUi("kreatin", "Kreatin-Monohydrat", "1 × 3 g = 3 g · Pulver", solubility = Solubility.Water),
)

val previewCatalogMedicines = listOf(
    MedicineUi("alcar", "Acetyl-L-Carnitin (ALCAR)", "", catalogMeta = "in 3 Stacks · Kapsel · Thorne"),
    MedicineUi("ala", "Alpha-Liponsäure", "", catalogMeta = "in 2 Stacks · Kapsel · Sunday Natural"),
    MedicineUi("berberin", "Berberin", "", catalogMeta = "in 1 Stack · Kapsel · Thorne"),
    MedicineUi("q10", "Coenzym Q10", "", catalogMeta = "in 2 Stacks · Kapsel · Pure Encapsulations"),
    MedicineUi("curcumin", "Curcumin", "", catalogMeta = "in 1 Stack · Kapsel · Sunday Natural"),
    MedicineUi("magnesium", "Magnesiumbisglycinat", "", catalogMeta = "in 4 Stacks · Pulver · Sunday Natural"),
    MedicineUi("nr", "Nicotinamid-Ribosid (NR)", "", catalogMeta = "in 2 Stacks · Kapsel · Thorne"),
    MedicineUi("omega3", "Omega-3", "", catalogMeta = "in 3 Stacks · Kapsel · Norsan"),
    MedicineUi("pea", "PEA (Palmitoylethanolamid)", "", catalogMeta = "in 3 Stacks · Kapsel · Thorne"),
    MedicineUi("quercetin", "Quercetin", "", catalogMeta = "in 1 Stack · Kapsel · Pure Encapsulations"),
    MedicineUi("resveratrol", "Resveratrol", "", catalogMeta = "in 1 Stack · Kapsel · Sunday Natural"),
    MedicineUi("b12", "Vitamin B12", "", catalogMeta = "in 2 Stacks · Tropfen · Sunday Natural"),
    MedicineUi("d3", "Vitamin D3", "", catalogMeta = "in 3 Stacks · Kapsel · Thorne"),
    MedicineUi("zink", "Zink", "", catalogMeta = "in 2 Stacks · Kapsel · Pure Encapsulations"),
)

val previewGoals = listOf(
    GoalUi("goal-1", 1, "Entzündungen senken", SignalState.Green),
    GoalUi("goal-2", 2, "Mitochondrien unterstützen", SignalState.Yellow, reason = "Die aktuelle Einnahme stützt dieses Ziel nur teilweise. Dosis und Zeitpunkt erneut prüfen."),
    GoalUi("goal-3", 3, "Schlafqualität verbessern", SignalState.Green),
    GoalUi("goal-4", 4, "Stressresilienz erhöhen", SignalState.Red, reason = "Mehrere Mittel stören dieses Ziel. Die betroffenen Kombinationen sollten aufgeschlüsselt werden."),
    GoalUi("goal-5", 5, "Kognitive Leistung erhalten", SignalState.Green),
    GoalUi("goal-6", 6, "Magen und Darm schonen", SignalState.Yellow, reason = "Ein Mittel kann den Magen reizen. Einnahmehinweis beachten."),
    GoalUi("goal-7", 7, "Energie am Tag stabilisieren", SignalState.Gray),
    GoalUi("goal-8", 8, "Regeneration fördern", SignalState.Green),
    GoalUi("goal-9", 9, "Nebenwirkungen vermeiden", SignalState.Green),
)

val previewQuestions = listOf(
    QuestionUi("question-1", "Welche Mittel konkurrieren um dieselben Aufnahmewege?"),
    QuestionUi("question-2", "Ist die Reihenfolge für einen nüchternen Magen sinnvoll?"),
)

val previewHistory = listOf(
    EvaluationRunUi("run-1", "14.08. · 12:28", "Terra · hoch", SignalCounts(4, 1, 0)),
    EvaluationRunUi("run-2", "13.08. · 19:42", "Terra · hoch", SignalCounts(3, 2, 0)),
    EvaluationRunUi("run-3", "12.08. · 08:16", "Terra · hoch", SignalCounts(3, 1, 1)),
    EvaluationRunUi("run-4", "10.08. · 18:05", "Terra · hoch", SignalCounts(2, 2, 1)),
    EvaluationRunUi("run-5", "08.08. · 12:31", "Terra · hoch", SignalCounts(4, 0, 1)),
)

val previewEvaluationParagraphs = listOf(
    "Die aktuelle Zusammenstellung wurde anhand der priorisierten Ziele und der angegebenen Tagesdosen geprüft. Insgesamt zeigt sich ein überwiegend stützendes Bild. Zwei Punkte benötigen Aufmerksamkeit, weil sie die gewünschte Wirkung einzelner Mittel abschwächen können.",
    "Die wasserlöslichen Mittel sind über den Einnahmezeitraum sinnvoll verteilt. Die angegebenen Mengen liegen in der vorgesehenen Tagesdosis. Für die fettlöslichen Bestandteile ist die Einnahme zusammen mit einer Mahlzeit zweckmäßig.",
    "Bei der Einnahme-Reihenfolge fällt auf, dass mehrere Mittel um dieselben Aufnahmewege konkurrieren. Ein größerer Abstand zwischen diesen Gruppen kann die Verfügbarkeit verbessern, ohne die Gesamtmenge zu verändern.",
    "Das erste priorisierte Ziel wird von mehreren Bestandteilen gestützt. Die stärksten Beiträge entstehen durch die Kombination der morgens vorgesehenen Mittel. Für dieses Ziel ist derzeit kein klar störender Bestandteil hinterlegt.",
    "Beim zweiten Ziel ist die Bewertung gemischt. Die Mehrzahl der Mittel bleibt neutral, während ein Bestandteil wegen seiner angegebenen Menge gelb bewertet wird. Die Aufschlüsselung zeigt, welche Zuordnung dafür ausschlaggebend ist.",
    "Für das dritte Ziel ergibt sich eine überwiegend grüne Bewertung. Die zusammen einzunehmende Gruppe bleibt dabei als Einheit erhalten. Eine Änderung der Reihenfolge innerhalb dieser Gruppe ist nicht erforderlich.",
    "Die Frequenzen wurden mit den Tagesmengen verrechnet. Mittel, die nur an einzelnen Tagen vorgesehen sind, werden nicht als tägliche Belastung behandelt. Dadurch bleibt die Gesamtsicht von den einzelnen Einnahmetagen getrennt.",
    "Die Bewertung beschreibt den gespeicherten Stand zum angegebenen Zeitpunkt. Änderungen an Mitteln, Dosen, Frequenzen oder Zielen markieren diese Auswertung als veraltet und erfordern eine neue Prüfung.",
)
