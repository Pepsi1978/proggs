package de.frank.entropyreducer.data.local

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.dao.HabitSuggestionDao
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.MentalSentenceDao
import de.frank.entropyreducer.data.local.dao.SpecialMentalSentenceDao
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao

/**
 * Hilt-@EntryPoint fuer die ID-Architektur Etappe 2c (Frank-Wunsch 2026-06-19).
 *
 * Die Ideen- und Vorschlags-Persistenz lebt als FREIE `internal fun ...(context: Context)`-Funktionen
 * (IdeenScreen.kt, SuggestionDataStores.kt). Damit diese ihren Room-DAO erreichen, OHNE dass ihre
 * Signaturen geaendert werden muessen (und damit ALLE Aufrufer — Compose-UI, Migrator, ViewModels,
 * SyncCoordinator, SyncEntriesUseCase — unveraendert weiterlaufen), holen sie den DAO ueber diesen
 * EntryPoint aus dem Application-Graphen.
 *
 * Best-Practice Hilt §3.6: `@EntryPoint` + `EntryPointAccessors` ist der offiziell vorgesehene Weg,
 * um aus Nicht-Hilt-Code (hier: top-level-Funktionen) in den DI-Graphen zu greifen — kein
 * Service-Locator, sauber an den Application-Component gebunden.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface IdeaTaskDaoEntryPoint {
    fun ideaDao(): IdeaDao

    fun taskSuggestionDao(): TaskSuggestionDao

    // ID-Architektur Etappe 3: Gewohnheiten + Gewohnheits-Vorschlaege.
    fun habitDao(): HabitDao

    fun habitSuggestionDao(): HabitSuggestionDao

    // ID-Architektur Etappe 4: Mental-Board-Saetze.
    fun mentalSentenceDao(): MentalSentenceDao

    // Frank-Wunsch 2026-07-15: Spezielle Mental-Saetze (eigener Vorlese-Bereich).
    fun specialMentalSentenceDao(): SpecialMentalSentenceDao

    // Frank-Wunsch 2026-06-20: fuer den chain-bewussten Vorschlags-Restore (countByRootId).
    fun entropyEntryDao(): EntropyEntryDao
}

/** Holt den [IdeaDao] aus dem Application-Graphen (fuer freie context-basierte Funktionen). */
internal fun ideaDaoFrom(context: Context): IdeaDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .ideaDao()

/** Holt den [TaskSuggestionDao] aus dem Application-Graphen. */
internal fun taskSuggestionDaoFrom(context: Context): TaskSuggestionDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .taskSuggestionDao()

/** Holt den [HabitDao] aus dem Application-Graphen (ID-Architektur Etappe 3). */
internal fun habitDaoFrom(context: Context): HabitDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .habitDao()

/** Holt den [HabitSuggestionDao] aus dem Application-Graphen (ID-Architektur Etappe 3). */
internal fun habitSuggestionDaoFrom(context: Context): HabitSuggestionDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .habitSuggestionDao()

/** Holt den [MentalSentenceDao] aus dem Application-Graphen (ID-Architektur Etappe 4). */
internal fun mentalSentenceDaoFrom(context: Context): MentalSentenceDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .mentalSentenceDao()

/** Holt den [SpecialMentalSentenceDao] aus dem Application-Graphen (Frank-Wunsch 2026-07-15). */
internal fun specialMentalSentenceDaoFrom(context: Context): SpecialMentalSentenceDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .specialMentalSentenceDao()

/** Holt den [EntropyEntryDao] aus dem Application-Graphen (fuer den chain-bewussten Vorschlags-Restore). */
internal fun entropyEntryDaoFrom(context: Context): EntropyEntryDao =
    EntryPointAccessors.fromApplication(
            context.applicationContext,
            IdeaTaskDaoEntryPoint::class.java,
        )
        .entropyEntryDao()
