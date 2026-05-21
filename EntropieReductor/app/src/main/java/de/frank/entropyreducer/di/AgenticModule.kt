package de.frank.entropyreducer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadEntropieEintraegeTool

/**
 * Hilt-Modul fuer Agentic-AI-Tools (Frank-Wunsch 2026-05-21).
 *
 * Jedes Tool wird per @Binds @IntoSet in das Set<AgenticTool> eingehaengt.
 * Die ToolRegistry konsumiert dieses Set und stellt es dem WorkflowRunner
 * zur Verfuegung.
 *
 * Hinzufuegen eines neuen Tools:
 * 1. Klasse implementiert AgenticTool mit @Inject constructor + @Singleton
 * 2. In dieses Modul eine neue @Binds @IntoSet abstract fun fuer das Tool
 *
 * Beim App-Start sammelt Hilt alle so registrierten Tools — keine zentrale
 * Liste zu pflegen, keine Reihenfolge wichtig.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AgenticModule {

    @Binds
    @IntoSet
    abstract fun bindReadEntropieEintraegeTool(impl: ReadEntropieEintraegeTool): AgenticTool

    // Weitere Tools werden in Etappen 6 und 7 hier registriert.
}
