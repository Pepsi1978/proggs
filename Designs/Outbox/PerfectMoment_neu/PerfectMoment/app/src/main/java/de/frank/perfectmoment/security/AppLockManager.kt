package de.frank.perfectmoment.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.perfectmoment.data.settings.SecureSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(private val settings: SecureSettings) : DefaultLifecycleObserver {
    private val promptActive = AtomicBoolean(false)
    private val promptEnablesLock = AtomicBoolean(false)
    private val _locked = MutableStateFlow(settings.appLockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (settings.appLockEnabled) _locked.value = true
    }

    fun beginPrompt(enabling: Boolean): Boolean {
        if (!promptActive.compareAndSet(false, true)) return false
        promptEnablesLock.set(enabling)
        return true
    }

    fun finishAuthentication(success: Boolean) {
        val enabling = promptEnablesLock.getAndSet(false)
        promptActive.set(false)
        if (success && enabling) {
            settings.appLockEnabled = true
            _locked.value = false
        } else if (success && settings.appLockEnabled) {
            _locked.value = false
        }
    }

    fun disable() {
        promptActive.set(false)
        promptEnablesLock.set(false)
        settings.appLockEnabled = false
        _locked.value = false
    }
}
