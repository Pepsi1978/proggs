package de.frank.genialeideen.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.genialeideen.data.settings.SecureSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sperrt die App beim Kaltstart und nach einer wählbaren Zeit im Hintergrund (Baustein I).
 * Es wird kein eigenes Passwort erfunden — nur das Verfahren des Geräts, immer mit
 * PIN-Rückfall.
 */
class AppLockManager(private val settings: SecureSettings) : DefaultLifecycleObserver {
    private val promptActive = AtomicBoolean(false)
    private val promptEnablesLock = AtomicBoolean(false)
    private val _locked = MutableStateFlow(settings.appLockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var imHintergrundSeit = 0L

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        imHintergrundSeit = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!settings.appLockEnabled) return
        val verzoegerung = settings.appLockDelayMinutes
        // -1 heisst „nie" — dann sperrt nur der Kaltstart, den der Anfangswert oben abdeckt.
        if (verzoegerung < 0) return
        val warZeitGenug = imHintergrundSeit == 0L ||
            System.currentTimeMillis() - imHintergrundSeit >= verzoegerung * 60_000L
        if (warZeitGenug) _locked.value = true
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
            imHintergrundSeit = 0L
        }
    }

    fun disable() {
        promptActive.set(false)
        promptEnablesLock.set(false)
        settings.appLockEnabled = false
        _locked.value = false
    }
}
