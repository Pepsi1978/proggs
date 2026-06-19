package de.frank.entropyreducer.data.local

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.dao.IdeaDao
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
