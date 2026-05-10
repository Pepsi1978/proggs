package de.frank.entropyreducer.presentation.settings.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Status der Health-Connect-Bruecke fuer die API-Schluessel-Settings-Karte
 * (Frank-Wunsch 2026-05-10): Frank will jederzeit die Berechtigungen erneut
 * setzen koennen — nicht nur ueber den Tap auf die Mini-Karte im Biomarker-
 * Bereich. Diese Karte zeigt Status + Knopf zum Erneut-Erteilen.
 */
data class HealthConnectApiUiState(
    val available: Boolean = false,
    val allGranted: Boolean = false,
    val grantedCount: Int = 0,
    val totalCount: Int = 0,
    val message: String? = null,
)

@HiltViewModel
class HealthConnectApiViewModel @Inject constructor(
    private val healthConnect: HealthConnectManager,
) : ViewModel() {
    private val _state = MutableStateFlow(HealthConnectApiUiState())
    val state: StateFlow<HealthConnectApiUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Liest aktuellen Permission-Stand neu — z.B. nach dem Permission-Dialog. */
    fun refresh() {
        viewModelScope.launch {
            val available = healthConnect.isAvailable()
            if (!available) {
                _state.value = HealthConnectApiUiState(
                    available = false,
                    message = "Health Connect ist auf diesem Geraet nicht verfuegbar.",
                )
                return@launch
            }
            val all = healthConnect.requiredReadPermissions()
            val grantedSet = healthConnect.allGrantedPermissions()
            val grantedAll = grantedSet.containsAll(all)
            _state.value = HealthConnectApiUiState(
                available = true,
                allGranted = grantedAll,
                grantedCount = grantedSet.intersect(all).size,
                totalCount = all.size,
                message = null,
            )
        }
    }

    fun setMessage(text: String?) {
        _state.value = _state.value.copy(message = text)
    }

    fun requiredPermissions(): Set<String> = healthConnect.requiredReadPermissions()
}
