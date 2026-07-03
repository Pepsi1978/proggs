package de.frank.entropyreducer.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Frank-Wunsch 2026-06-24: Mic-Overlay-Steuerung ueber alle Sub-Tab-Screens.
 *
 * Hintergrund (Root Cause): Bei der Umstellung der Sub-Reiter von unten (BottomBar)
 * nach oben (SubTabRow am Kopf) erhielt jeder Sub-Tab-Screen von AppNavGraph den
 * Parameter showBottomBar=false. Das schaltete die lokale CosmosBottomBar ab —
 * UND versehentlich auch das daran gekoppelte MicCaptureActions-Overlay. Der
 * zentrale Mic-Button in AppNavGraph toggelt den micActionsOpen-State, aber das
 * dazugehoerige Overlay wurde nirgends mehr gerendert -> Mikrofon reagierte auf
 * keinen Tap mehr (Frank-Regression 2026-06-24).
 *
 * Loesung (Defensiv, Funktionserhaltend): Dieser CompositionLocal kommuniziert
 * den Toggle-State zentral vom AppNavGraph an jeden Sub-Tab-Screen. Jeder Screen
 * liest statt seiner lokalen `var actionsExpanded/micActionsOpen`-Variable jetzt
 * `LocalMicActionsOpen.current` und zeigt sein eigenes (pro-Reiter-logisches)
 * Mic-Overlay an — sei es das native MicCaptureActions oder sein eigenes
 * Inline-Overlay wie TagebuchScreen/IdeenScreen mit Gemini-Review-Dialog. Die
 * Sub-Reiter-Optik oben bleibt unveraendert bestehen. Keine Funktionalitaet wurde
 * entfernt, alle pro-Reiter-Pfade (Gemini-Verbesserung, pendingTranscript,
 * onWriteClick, etc.) funktionieren weiter wie gewohnt.
 *
 * Poka-Yoke Stufe 2 (Guard): staticCompositionLocalOf wirft zur Compile-Zeit
 * (bzw. Composition) einen klaren Fehler, falls ein Screen vergisst, sich in den
 * Provider einzubinden — kein lautloses Versagen mehr.
 */
class MicActionsState(
    val isOpen: Boolean,
    val setOpen: (Boolean) -> Unit,
) {
    fun toggle() = setOpen(!isOpen)
    fun close() = setOpen(false)
}

val LocalMicActionsOpen = staticCompositionLocalOf<MicActionsState> {
    error("LocalMicActionsOpen nicht bereitgestellt — AppNavGraph muss MicActionsState via CompositionLocalProvider an die NavHost-Box durchreichen")
}