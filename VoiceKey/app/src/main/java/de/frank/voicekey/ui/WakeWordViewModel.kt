package de.frank.voicekey.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.data.WakeWord
import de.frank.voicekey.data.WakeWordRepository
import de.frank.voicekey.obs.Obs
import de.frank.voicekey.service.WakeWordService
import de.frank.voicekey.trigger.AssistantLauncher
import de.frank.voicekey.trigger.AssistantStopper
import de.frank.voicekey.wake.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ModelUiState {
    data object Missing : ModelUiState

    data class Downloading(val percent: Int) : ModelUiState

    data object Ready : ModelUiState

    data object Error : ModelUiState
}

data class PermissionsState(
    val mic: Boolean = false,
    val notifications: Boolean = false,
    val overlay: Boolean = false,
    val battery: Boolean = false,
    val assistKill: Boolean = false,
) {
    val total: Int
        get() = 5

    val grantedCount: Int
        get() = listOf(mic, notifications, overlay, battery, assistKill).count { it }

    val allGranted: Boolean
        get() = grantedCount == total
}

data class UiState(
    val words: List<WakeWord> = emptyList(),
    val serviceEnabled: Boolean = false,
    val models: Map<WakeLang, ModelUiState> = emptyMap(),
    val permissions: PermissionsState = PermissionsState(),
    val batteryUnrestricted: Boolean = false,
    val targetAvailable: Boolean = true,
)

class WakeWordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WakeWordRepository(application)

    private val modelStates =
        MutableStateFlow(
            WakeLang.entries.associateWith { lang ->
                if (ModelManager.isReady(application, lang)) ModelUiState.Ready
                else ModelUiState.Missing as ModelUiState
            }
        )
    private val permissions = MutableStateFlow(PermissionsState())
    private val targetAvailable = MutableStateFlow(true)

    val uiState: StateFlow<UiState> =
        combine(
                repository.words,
                repository.serviceEnabled,
                modelStates,
                permissions,
                targetAvailable,
            ) { words, enabled, models, perms, target ->
                UiState(
                    words = words,
                    serviceEnabled = enabled,
                    models = models,
                    permissions = perms,
                    batteryUnrestricted = perms.battery,
                    targetAvailable = target,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        ensureModels()
        // Nach App-Update/Neuinstallation: Dienst wieder hochfahren, wenn der Schalter an ist
        // (der Foreground-Service ueberlebt das Update nicht von selbst).
        viewModelScope.launch {
            if (repository.serviceEnabled.first()) {
                Obs.i("WakeWordViewModel", "init", "Schalter ist an — starte Dienst nach App-Start")
                WakeWordService.start(getApplication())
            }
        }
    }

    /** Modelle beim ersten Start automatisch herunterladen (DE + EN). */
    fun ensureModels() {
        val context = getApplication<Application>()
        WakeLang.entries.forEach { lang ->
            if (ModelManager.isReady(context, lang)) {
                setModelState(lang, ModelUiState.Ready)
                return@forEach
            }
            if (modelStates.value[lang] is ModelUiState.Downloading) return@forEach
            setModelState(lang, ModelUiState.Downloading(0))
            viewModelScope.launch {
                val ok =
                    ModelManager.ensureModel(context, lang) { percent ->
                        setModelState(lang, ModelUiState.Downloading(percent))
                    }
                setModelState(lang, if (ok) ModelUiState.Ready else ModelUiState.Error)
            }
        }
    }

    private fun setModelState(lang: WakeLang, state: ModelUiState) {
        modelStates.value = modelStates.value.toMutableMap().apply { put(lang, state) }
    }

    /**
     * Wird von der Activity in onResume aufgerufen — Berechtigungen koennen sich extern aendern.
     */
    fun refreshPermissions(context: Context) {
        val power = context.getSystemService(PowerManager::class.java)
        permissions.value =
            PermissionsState(
                mic = granted(context, Manifest.permission.RECORD_AUDIO),
                notifications = granted(context, Manifest.permission.POST_NOTIFICATIONS),
                overlay = Settings.canDrawOverlays(context),
                battery = power.isIgnoringBatteryOptimizations(context.packageName),
                assistKill = isAccessibilityEnabled(context),
            )
        targetAvailable.value = AssistantLauncher.isTargetAvailable(context)
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        if (de.frank.voicekey.a11y.VoiceKeyAccessibilityService.isEnabled) return true
        val enabled =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
        return enabled.contains("${context.packageName}/") &&
            enabled.contains("VoiceKeyAccessibilityService")
    }

    /**
     * Not-Aus: beendet die ChatGPT-Voice-Session wirklich (Headsethook an ChatGPTs Media-Session).
     * Braucht KEINE Bedienungshilfe — wirkt immer, solange eine Session laeuft.
     */
    fun endAssistant(context: Context) {
        AssistantStopper.endVoiceSession(context, "Not-Aus-Knopf (App)")
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun setServiceEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
            if (enabled) WakeWordService.start(context) else WakeWordService.stop(context)
            Obs.checkpoint(
                step = "Dienst-Schalter",
                intent = "Schalter startet/stoppt das Wake-Word-Lauschen",
                expected = if (enabled) "gestartet" else "gestoppt",
                actual = if (enabled) "gestartet" else "gestoppt",
            )
        }
    }

    fun testTrigger(context: Context) {
        AssistantLauncher.launch(context)
    }

    fun addWord(text: String, lang: WakeLang, favorit: Boolean) {
        viewModelScope.launch { repository.addWord(text, lang, favorit) }
    }

    fun updateWord(id: String, newText: String) {
        viewModelScope.launch { repository.updateWord(id, newText) }
    }

    fun removeWord(id: String) {
        viewModelScope.launch { repository.removeWord(id) }
    }

    fun toggleFavorit(id: String) {
        viewModelScope.launch { repository.toggleFavorit(id) }
    }
}
