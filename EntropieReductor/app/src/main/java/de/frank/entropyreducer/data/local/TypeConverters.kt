package de.frank.entropyreducer.data.local

import androidx.room.TypeConverter
import de.frank.entropyreducer.domain.model.ConfirmDecision
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.model.StackType
import de.frank.entropyreducer.domain.model.StepType
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.TriggerSource
import de.frank.entropyreducer.domain.model.TriggerType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters: speichert Enums als Strings und List<String> als JSON. Enums via name() —
 * robust gegen Reordering, solange Namen stabil bleiben.
 */
class EntropyTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }
    private val listStringSerializer = ListSerializer(String.serializer())

    // List<String> ↔ JSON-String
    @TypeConverter
    fun fromStringList(list: List<String>?): String =
        if (list.isNullOrEmpty()) "[]" else json.encodeToString(listStringSerializer, list)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else
            runCatching { json.decodeFromString(listStringSerializer, value) }
                .getOrDefault(emptyList())

    // Enums
    @TypeConverter fun fromCategory(c: EntropyCategory): String = c.name

    @TypeConverter
    fun toCategory(s: String): EntropyCategory =
        runCatching { EntropyCategory.valueOf(s) }.getOrDefault(EntropyCategory.SONSTIGES)

    // List<EntropyCategory> ↔ JSON-String. Frank-Wunsch 2026-05-09: Methoden im
    // Insight Board sollen mehrere Kategorien tragen koennen (z.B. Laufen wirkt
    // mental + koerperlich + emotional gleichzeitig). Speicherung als JSON-Array
    // der Enum-Namen — robust gegen Reordering, identisch zur fromStringList-Strategie.
    @TypeConverter
    fun fromCategoryList(list: List<EntropyCategory>?): String =
        if (list.isNullOrEmpty()) "[]"
        else json.encodeToString(listStringSerializer, list.map { it.name })

    @TypeConverter
    fun toCategoryList(value: String?): List<EntropyCategory> =
        if (value.isNullOrBlank()) emptyList()
        else
            runCatching {
                    json.decodeFromString(listStringSerializer, value).mapNotNull { name ->
                        runCatching { EntropyCategory.valueOf(name) }.getOrNull()
                    }
                }
                .getOrDefault(emptyList())

    @TypeConverter fun fromStatus(c: EntryStatus): String = c.name

    @TypeConverter
    fun toStatus(s: String): EntryStatus =
        runCatching { EntryStatus.valueOf(s) }.getOrDefault(EntryStatus.OFFEN)

    @TypeConverter fun fromTimeBucket(c: TimeBucket): String = c.name

    @TypeConverter
    fun toTimeBucket(s: String): TimeBucket =
        runCatching { TimeBucket.valueOf(s) }.getOrDefault(TimeBucket.FREIBLOCK)

    @TypeConverter fun fromEntrySource(c: EntrySource): String = c.name

    @TypeConverter
    fun toEntrySource(s: String): EntrySource =
        runCatching { EntrySource.valueOf(s) }.getOrDefault(EntrySource.NUTZER_TEXT)

    @TypeConverter fun fromMemorySource(c: MemorySource): String = c.name

    @TypeConverter
    fun toMemorySource(s: String): MemorySource =
        runCatching { MemorySource.valueOf(s) }.getOrDefault(MemorySource.MANUELL)

    @TypeConverter
    fun fromPromptCategory(c: de.frank.entropyreducer.domain.model.PromptCategory): String = c.name

    @TypeConverter
    fun toPromptCategory(s: String): de.frank.entropyreducer.domain.model.PromptCategory =
        runCatching { de.frank.entropyreducer.domain.model.PromptCategory.valueOf(s) }
            .getOrDefault(de.frank.entropyreducer.domain.model.PromptCategory.AUFGABEN)

    @TypeConverter fun fromScientistRole(c: ScientistRole): String = c.name

    @TypeConverter
    fun toScientistRole(s: String): ScientistRole =
        runCatching { ScientistRole.valueOf(s) }.getOrDefault(ScientistRole.NUTZER)

    @TypeConverter fun fromHypothesisStatus(c: HypothesisStatus): String = c.name

    @TypeConverter
    fun toHypothesisStatus(s: String): HypothesisStatus =
        runCatching { HypothesisStatus.valueOf(s) }.getOrDefault(HypothesisStatus.VORGESCHLAGEN)

    @TypeConverter fun fromHypothesisOutcome(c: HypothesisOutcome?): String? = c?.name

    @TypeConverter
    fun toHypothesisOutcome(s: String?): HypothesisOutcome? = s?.let {
        runCatching { HypothesisOutcome.valueOf(it) }.getOrNull()
    }

    @TypeConverter fun fromStackType(c: StackType): String = c.name

    @TypeConverter
    fun toStackType(s: String): StackType =
        runCatching { StackType.valueOf(s) }.getOrDefault(StackType.MORGEN)

    @TypeConverter fun fromShiftCode(c: ShiftCode): String = c.name

    @TypeConverter
    fun toShiftCode(s: String): ShiftCode =
        runCatching { ShiftCode.valueOf(s) }.getOrDefault(ShiftCode.UNBEKANNT)

    // Agentic-AI Converter (Frank-Wunsch 2026-05-21: Prompts als Agenten)

    @TypeConverter fun fromExecutionStatus(c: ExecutionStatus): String = c.name

    @TypeConverter
    fun toExecutionStatus(s: String): ExecutionStatus =
        runCatching { ExecutionStatus.valueOf(s) }.getOrDefault(ExecutionStatus.FAILED)

    @TypeConverter fun fromStepType(c: StepType): String = c.name

    @TypeConverter
    fun toStepType(s: String): StepType =
        runCatching { StepType.valueOf(s) }.getOrDefault(StepType.LLM_CALL)

    @TypeConverter fun fromConfirmDecision(c: ConfirmDecision?): String? = c?.name

    @TypeConverter
    fun toConfirmDecision(s: String?): ConfirmDecision? =
        s?.let { runCatching { ConfirmDecision.valueOf(it) }.getOrNull() }

    @TypeConverter fun fromTriggerSource(c: TriggerSource): String = c.name

    @TypeConverter
    fun toTriggerSource(s: String): TriggerSource =
        runCatching { TriggerSource.valueOf(s) }.getOrDefault(TriggerSource.MANUAL)

    @TypeConverter fun fromTriggerType(c: TriggerType): String = c.name

    @TypeConverter
    fun toTriggerType(s: String): TriggerType =
        runCatching { TriggerType.valueOf(s) }.getOrDefault(TriggerType.MANUAL)
}
