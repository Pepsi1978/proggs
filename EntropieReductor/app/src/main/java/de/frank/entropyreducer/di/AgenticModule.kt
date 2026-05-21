package de.frank.entropyreducer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.gates.AutoApproveConfirmationGate
import de.frank.entropyreducer.domain.agentic.gates.ConfirmationGate
import de.frank.entropyreducer.domain.agentic.tools.read.ReadEntropieEintraegeTool

/**
 * Hilt-Modul fuer Agentic-AI-Tools + Gates (Frank-Wunsch 2026-05-21).
 *
 * Jedes Tool wird per @Binds @IntoSet in das Set<AgenticTool> eingehaengt.
 * Die ToolRegistry konsumiert dieses Set und stellt es dem WorkflowRunner
 * zur Verfuegung.
 *
 * Hinzufuegen eines neuen Tools:
 * 1. Klasse implementiert AgenticTool mit @Inject constructor + @Singleton
 * 2. In dieses Modul eine neue @Binds @IntoSet abstract fun fuer das Tool
 *
 * ConfirmationGate: Default-Binding ist AutoApprove (fuer Background-Runs +
 * Trust-Modus). Die UI-Variante wird in Etappe 9 in einem eigenen Modul (oder
 * via @Provides im UI-Layer) hinzugefuegt und kann diese Default-Bindung
 * ueberschreiben.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AgenticModule {

    // Tool-Bindings (Set<AgenticTool> via Multibindings)
    @Binds
    @IntoSet
    abstract fun bindReadEntropieEintraegeTool(impl: ReadEntropieEintraegeTool): AgenticTool

    // Weitere Tools werden in Etappen 6 und 7 hier registriert.

    // Gate-Bindings — Default-Implementations (UI-Variante ueberschreibt in Etappe 9)
    @Binds
    abstract fun bindConfirmationGate(impl: AutoApproveConfirmationGate): ConfirmationGate
}
