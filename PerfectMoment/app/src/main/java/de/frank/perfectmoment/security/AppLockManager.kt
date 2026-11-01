package de.frank.perfectmoment.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.perfectmoment.data.settings.SecureSettings
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wofür der Fingerabdruck gerade abgefragt wird. */
enum class LockPrompt { UNLOCK_APP, ENABLE_LOCK, DISABLE_LOCK, UNLOCK_KEYS }

class AppLockManager(private val settings: SecureSettings) : DefaultLifecycleObserver {
    private val pendingPrompt = AtomicReference<LockPrompt?>(null)
    private val _locked = MutableStateFlow(settings.appLockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Die API-Schlüssel brauchen zusätzlich zur entsperrten App eine eigene Freigabe. */
    private val _keysUnlocked = MutableStateFlow(false)
    val keysUnlocked: StateFlow<Boolean> = _keysUnlocked.asStateFlow()

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (settings.appLockEnabled) _locked.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        lockKeys()
    }

    fun beginPrompt(prompt: LockPrompt): Boolean = pendingPrompt.compareAndSet(null, prompt)

    fun finishAuthentication(success: Boolean) {
        val prompt = pendingPrompt.getAndSet(null)
        if (!success) return
        when (prompt) {
            LockPrompt.ENABLE_LOCK -> {
                settings.appLockEnabled = true
                _keysUnlocked.value = false
                _locked.value = false
            }
            LockPrompt.UNLOCK_APP -> if (settings.appLockEnabled) _locked.value = false
            LockPrompt.DISABLE_LOCK -> disable()
            LockPrompt.UNLOCK_KEYS -> if (settings.appLockEnabled && !_locked.value) _keysUnlocked.value = true
            null -> Unit
        }
    }

    /** Eine neu aufgebaute Activity bekommt das Ergebnis der alten Abfrage nicht mehr; sonst bliebe sie ewig offen. */
    fun cancelPendingPrompt() {
        pendingPrompt.set(null)
    }

    fun lockKeys() {
        // Eine noch laufende Abfrage darf die Freigabe danach nicht nachträglich erteilen.
        pendingPrompt.compareAndSet(LockPrompt.UNLOCK_KEYS, null)
        _keysUnlocked.value = false
    }

    private fun disable() {
        settings.appLockEnabled = false
        _keysUnlocked.value = false
        _locked.value = false
    }
}
