package com.entropyjournal.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Returns a lambda that performs haptic feedback only when the user has haptic enabled in settings.
 * Drop-in replacement: instead of `haptic.performHapticFeedback(type)` use `doHaptic(type)`.
 */
@Composable
fun rememberHapticAction(): (HapticFeedbackType) -> Unit {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember {
        val mk = androidx.security.crypto.MasterKeys.getOrCreate(
            androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
        )
        androidx.security.crypto.EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME,
            mk,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    return remember(haptic, prefs) {
        { type: HapticFeedbackType ->
            if (prefs.getBoolean(Constants.PREF_HAPTIC_ENABLED, true)) {
                haptic.performHapticFeedback(type)
            }
        }
    }
}
