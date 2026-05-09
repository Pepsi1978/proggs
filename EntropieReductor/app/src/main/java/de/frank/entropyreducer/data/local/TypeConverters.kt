package de.frank.entropyreducer.data.local

import androidx.room.TypeConverter
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.model.StackType
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters: speichert Enums als Strings und List<String> als JSON.
 * Enums via name() — robust gegen Reordering, solange Namen stabil bleiben.
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
        else runCatching { json.decodeFromString(listStringSerializer, value) }.getOrDefault(emptyList())

    // Enums
    @TypeConverter
    fun fromCategory(c: EntropyCategory): String = c.name
    @TypeConverter
    fun toCategory(s: String): EntropyCategory =
        runCatching { EntropyCategory.valueOf(s) }.getOrDefault(EntropyCategory.SONSTIGES)

    // List<EntropyCategory> ↔ JSON-String. Frank-Wunsch 2026-05-09: Methoden im
    // Insight Board sollen mehrere Kategorien tragen koennen (z.B. Laufen wirkt
    // mental + koerperlich + emotional gleichzeitig). Speicherung als JSON-Array
    // der Enum-Namen — robust gegen Reordering, identisch zur fromStringList-Strategie.
    @TypeConverter
    fun fromCategoryList(list: List<EntropyCategory>?): String =
        if (list.isNullOrEmpty()) "[]" else json.encodeToString(listStringSerializer, list.map { it.name })

    @TypeConverter
    fun toCategoryList(value: String?): List<EntropyCategory> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching {
            json.decodeFromString(listStringSerializer, value)
                .mapNotNull { name -> runCatching { EntropyCategory.valueOf(name) }.getOrNull() }
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromStatus(c: EntryStatus): String = c.name
    @TypeConverter
    fun toStatus(s: String): EntryStatus =
        runCatching { EntryStatus.valueOf(s) }.getOrDefault(EntryStatus.OFFEN)

    @TypeConverter
    fun fromTimeBucket(c: TimeBucket): String = c.name
    @TypeConverter
    fun toTimeBucket(s: String): TimeBucket =
        runCatching { TimeBucket.valueOf(s) }.getOrDefault(TimeBucket.HEUTE)

    @TypeConverter
    fun fromEntrySource(c: EntrySource): String = c.name
    @TypeConverter
    fun toEntrySource(s: String): EntrySource =
        runCatching { EntrySource.valueOf(s) }.getOrDefault(EntrySource.NUTZER_TEXT)

    @TypeConverter
    fun fromMemorySource(c: MemorySource): String = c.name
    @TypeConverter
    fun toMemorySource(s: String): MemorySource =
        runCatching { MemorySource.valueOf(s) }.getOrDefault(MemorySource.MANUELL)

    @TypeConverter
    fun fromScientistRole(c: ScientistRole): String = c.name
    @TypeConverter
    fun toScientistRole(s: String): ScientistRole =
        runCatching { ScientistRole.valueOf(s) }.getOrDefault(ScientistRole.NUTZER)

    @TypeConverter
    fun fromHypothesisStatus(c: HypothesisStatus): String = c.name
    @TypeConverter
    fun toHypothesisStatus(s: String): HypothesisStatus =
        runCatching { HypothesisStatus.valueOf(s) }.getOrDefault(HypothesisStatus.VORGESCHLAGEN)

    @TypeConverter
    fun fromHypothesisOutcome(c: HypothesisOutcome?): String? = c?.name
    @TypeConverter
    fun toHypothesisOutcome(s: String?): HypothesisOutcome? =
        s?.let { runCatching { HypothesisOutcome.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromStackType(c: StackType): String = c.name
    @TypeConverter
    fun toStackType(s: String): StackType =
        runCatching { StackType.valueOf(s) }.getOrDefault(StackType.MORGEN)

    @TypeConverter
    fun fromShiftCode(c: ShiftCode): String = c.name
    @TypeConverter
    fun toShiftCode(s: String): ShiftCode =
        runCatching { ShiftCode.valueOf(s) }.getOrDefault(ShiftCode.UNBEKANNT)
}
