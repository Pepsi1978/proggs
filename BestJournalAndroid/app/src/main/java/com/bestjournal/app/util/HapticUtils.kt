package com.bestjournal.app.util

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Returns a lambda that performs haptic feedback only when the user has haptic enabled in settings.
 * Uses the View-based Android API for stronger feedback than the Compose defaults. Drop-in
 * replacement: instead of `haptic.performHapticFeedback(type)` use `doHaptic(type)`.
 *
 * Upgrade mapping (one level stronger): TextHandleMove → LONG_PRESS (medium) LongPress → REJECT
 * (API 30+) / KEYBOARD_PRESS (API 27-29)
 */
@Composable
fun rememberHapticAction(): (HapticFeedbackType) -> Unit {
    val view = LocalView.current
    val context = LocalContext.current
    val prefs = remember { EncryptedPrefsProvider.get(context) }
    return remember(view, prefs) {
        { type: HapticFeedbackType ->
            if (prefs.getBoolean(Constants.PREF_HAPTIC_ENABLED, true)) {
                val constant =
                    when (type) {
                        HapticFeedbackType.TextHandleMove -> HapticFeedbackConstants.LONG_PRESS
                        HapticFeedbackType.LongPress -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                HapticFeedbackConstants.REJECT
                            } else {
                                HapticFeedbackConstants.KEYBOARD_PRESS
                            }
                        }
                        else -> HapticFeedbackConstants.LONG_PRESS
                    }
                view.performHapticFeedback(constant)
            }
        }
    }
}
