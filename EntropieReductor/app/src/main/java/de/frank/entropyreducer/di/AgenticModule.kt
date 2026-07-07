package de.frank.entropyreducer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.gates.ConfirmationGate
import de.frank.entropyreducer.domain.agentic.gates.UiConfirmationGate
import de.frank.entropyreducer.domain.agentic.tools.read.ReadAufgabenTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadBiomarkerTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadEntropieEintraegeTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadForscherSessionsTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadHypothesenTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadInsightsTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadMemoryTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadProfilTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadPromptExecutionsTool
import de.frank.entropyreducer.domain.agentic.tools.read.ReadThesenTool
import de.frank.entropyreducer.domain.agentic.tools.write.AddFollowupToAufgabeTool
import de.frank.entropyreducer.domain.agentic.tools.write.AddMemoryTool
import de.frank.entropyreducer.domain.agentic.tools.write.CreateAufgabeTool
import de.frank.entropyreducer.domain.agentic.tools.write.CreateForscherSessionTool
import de.frank.entropyreducer.domain.agentic.tools.write.CreateHypotheseTool
import de.frank.entropyreducer.domain.agentic.tools.write.CreateTagebuchEintragTool
import de.frank.entropyreducer.domain.agentic.tools.write.CreateTheseTool
import de.frank.entropyreducer.domain.agentic.tools.write.DeleteAufgabeTool
import de.frank.entropyreducer.domain.agentic.tools.write.UpdateAufgabeStatusTool
import de.frank.entropyreducer.domain.agentic.tools.write.UpdateProfilTool

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

    @Binds
    @IntoSet
    abstract fun bindReadThesenTool(impl: ReadThesenTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadAufgabenTool(impl: ReadAufgabenTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadMemoryTool(impl: ReadMemoryTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadProfilTool(impl: ReadProfilTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadInsightsTool(impl: ReadInsightsTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadHypothesenTool(impl: ReadHypothesenTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadBiomarkerTool(impl: ReadBiomarkerTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadForscherSessionsTool(impl: ReadForscherSessionsTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindReadPromptExecutionsTool(impl: ReadPromptExecutionsTool): AgenticTool

    // Write-Tools (Etappe 7) — alle haben isWriteTool=true und brauchen pro Prompt
    // einen Eintrag in prompt_tool_permissions mit granted=true

    @Binds
    @IntoSet
    abstract fun bindCreateAufgabeTool(impl: CreateAufgabeTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindCreateTagebuchEintragTool(impl: CreateTagebuchEintragTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindCreateTheseTool(impl: CreateTheseTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindUpdateAufgabeStatusTool(impl: UpdateAufgabeStatusTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindAddFollowupToAufgabeTool(impl: AddFollowupToAufgabeTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindDeleteAufgabeTool(impl: DeleteAufgabeTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindCreateHypotheseTool(impl: CreateHypotheseTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindCreateForscherSessionTool(impl: CreateForscherSessionTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindAddMemoryTool(impl: AddMemoryTool): AgenticTool

    @Binds
    @IntoSet
    abstract fun bindUpdateProfilTool(impl: UpdateProfilTool): AgenticTool

    // Gate-Bindings — UI-Variante mit StateFlow-Bridge (Frank 2026-05-21).
    // Im Background (z.B. WorkManager) bleibt das Verhalten gleich: wenn der
    // Prompt oder das Tool im Trust-Modus ist, wird AUTO_APPROVED ohne UI.
    // Sonst wartet das UI 60s auf Antwort, danach TIMED_OUT (≈ REJECTED).
    @Binds
    abstract fun bindConfirmationGate(impl: UiConfirmationGate): ConfirmationGate
}
