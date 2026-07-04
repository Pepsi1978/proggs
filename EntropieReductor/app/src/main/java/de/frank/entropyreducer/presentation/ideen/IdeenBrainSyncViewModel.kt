package de.frank.entropyreducer.presentation.ideen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaConnector
import javax.inject.Inject

/**
 * Frank-Wunsch 2026-07-04: Beim Betreten des Ideen-Reiters SOFORT pruefen, ob im Second Brain neue
 * Ideen liegen, und sie einlesen (kein Warten auf den naechsten App-Start / keinen Timer).
 * Der Connector prueft intern Key + aktivierten Ideen-Schalter und bricht bei fehlendem Tunnel
 * still ab (mit Retry). App->Brain laeuft weiterhin sofort ueber den Ideen-Flow des Connectors.
 */
@HiltViewModel
class IdeenBrainSyncViewModel
@Inject
constructor(
    private val connector: SecondBrainIdeaConnector,
) : ViewModel() {
    fun pullNow(areaKey: String = "ideas") {
        connector.pullFromBrain(viewModelScope, areaKey)
        // Fix 2026-07-04: Beim Betreten des Ideen-Reiters nicht nur runterladen, sondern auch
        // ausstehende Uploads nachschieben — sonst bleibt eine unterwegs (ohne Netz) nicht
        // hochgeladene Idee liegen, bis die App als Prozess neu startet.
        connector.retryPendingUploads(viewModelScope, areaKey)
    }
}
